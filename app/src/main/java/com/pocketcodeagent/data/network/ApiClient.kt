package com.pocketcodeagent.data.network

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.pocketcodeagent.data.model.Provider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

data class ProviderConnectionResult(
    val success: Boolean,
    val providerName: String,
    val modelName: String,
    val httpCode: Int? = null,
    val answer: String? = null,
    val sanitizedError: String? = null
)

private class ProviderApiException(
    override val message: String,
    val httpCode: Int? = null
) : IOException(message)

object ApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()
    private val gson = Gson()

    private fun normalizedBaseUrl(baseUrl: String): String {
        val trimmed = baseUrl.trim().trimEnd('/')
        return if (trimmed.endsWith("/chat/completions")) {
            trimmed.removeSuffix("/chat/completions")
        } else {
            trimmed
        }
    }

    private fun chatUrl(baseUrl: String): String {
        return "${normalizedBaseUrl(baseUrl)}/chat/completions"
    }

    private fun modelsUrl(baseUrl: String): String {
        return "${normalizedBaseUrl(baseUrl)}/models"
    }

    private fun providerLabel(provider: Provider): String {
        return "${provider.displayName} / model ${provider.modelName}"
    }

    private fun requireProviderReady(provider: Provider) {
        when {
            !provider.enabled -> throw ProviderApiException("${provider.displayName}: Provider ist deaktiviert.")
            provider.baseUrl.isBlank() -> throw ProviderApiException("${provider.displayName}: Base URL fehlt.")
            provider.modelName.isBlank() -> throw ProviderApiException("${provider.displayName}: Modell fehlt.")
            provider.apiKey.isBlank() -> throw ProviderApiException("${provider.displayName}: Provider nicht konfiguriert - API-Key fehlt.")
        }
    }

    private fun requireProviderAuth(provider: Provider) {
        when {
            !provider.enabled -> throw ProviderApiException("${provider.displayName}: Provider ist deaktiviert.")
            provider.baseUrl.isBlank() -> throw ProviderApiException("${provider.displayName}: Base URL fehlt.")
            provider.apiKey.isBlank() -> throw ProviderApiException("${provider.displayName}: Provider nicht konfiguriert - API-Key fehlt.")
        }
    }

    private fun sanitizedText(raw: String?, provider: Provider): String {
        if (raw.isNullOrBlank()) return ""
        var text = raw
        if (provider.apiKey.isNotBlank()) {
            text = text.replace(provider.apiKey, "[redacted]")
        }
        text = text
            .replace(Regex("(?i)Bearer\\s+[A-Za-z0-9._\\-]+"), "Bearer [redacted]")
            .replace(Regex("(?i)(api[_-]?key|authorization|token|secret)\"?\\s*[:=]\\s*\"?[^\"]+"), "$1=[redacted]")
        return text.take(800)
    }

    private fun extractProviderError(rawBody: String?, provider: Provider): String? {
        if (rawBody.isNullOrBlank()) return null
        return try {
            val root = JsonParser.parseString(rawBody).asJsonObject
            val error = root.get("error")
            val message = when {
                error is JsonObject && error.has("message") -> error.get("message").asString
                root.has("message") -> root.get("message").asString
                root.has("detail") -> root.get("detail").toString()
                else -> null
            }
            sanitizedText(message, provider).take(220)
        } catch (_: Exception) {
            sanitizedText(rawBody, provider).take(220)
        }
    }

    private fun httpError(provider: Provider, code: Int, rawBody: String?): ProviderApiException {
        val label = providerLabel(provider)
        val baseMessage = when (code) {
            400 -> "HTTP 400 - Anfrage ungueltig. Modellname, Endpoint oder Parameter pruefen."
            401 -> "HTTP 401 - API-Key ungueltig oder abgelaufen."
            403 -> "HTTP 403 - Zugriff verweigert. Konto, Modellzugriff oder API-Limits pruefen."
            404 -> "HTTP 404 - Modell oder Endpoint nicht gefunden."
            408 -> "HTTP 408 - Timeout beim Provider."
            409 -> "HTTP 409 - Provider konnte die Anfrage nicht verarbeiten."
            422 -> "HTTP 422 - Anfrageformat vom Provider abgelehnt."
            429 -> "HTTP 429 - Rate-Limit erreicht. Kurz warten oder anderes Modell waehlen."
            in 500..599 -> "HTTP $code - Serverfehler beim Provider."
            else -> "HTTP $code - Provider-Anfrage fehlgeschlagen."
        }
        val providerMessage = extractProviderError(rawBody, provider)
            ?.takeIf { it.isNotBlank() && code !in setOf(401, 403, 404) }
        val message = if (providerMessage != null) {
            "$label: $baseMessage $providerMessage"
        } else {
            "$label: $baseMessage"
        }
        return ProviderApiException(message, code)
    }

    private fun throwableMessage(provider: Provider, throwable: Throwable): ProviderApiException {
        return when (throwable) {
            is ProviderApiException -> throwable
            is SocketTimeoutException -> ProviderApiException("${provider.displayName}: Timeout - Internet oder Provider pruefen.")
            is UnknownHostException -> ProviderApiException("${provider.displayName}: Netzwerk nicht erreichbar - Internet pruefen.")
            else -> {
                val detail = throwable.message?.takeIf { it.isNotBlank() } ?: throwable.javaClass.simpleName
                ProviderApiException("${provider.displayName}: Anfrage fehlgeschlagen - ${sanitizedText(detail, provider)}")
            }
        }
    }

    private fun buildChatBody(
        provider: Provider,
        messages: List<Map<String, String>>,
        temperature: Double,
        maxTokens: Int,
        stream: Boolean
    ): String {
        val requestBodyMap = linkedMapOf<String, Any>(
            "model" to provider.modelName,
            "messages" to messages,
            "temperature" to temperature,
            "max_tokens" to maxTokens,
            "stream" to stream
        )
        return gson.toJson(requestBodyMap)
    }

    private fun buildRequest(provider: Provider, url: String, bodyJson: String? = null): Request {
        val builder = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${provider.apiKey}")
            .addHeader("Content-Type", "application/json")

        if (bodyJson != null) {
            builder.post(bodyJson.toRequestBody(mediaType))
        } else {
            builder.get()
        }

        for ((key, value) in provider.customHeaders) {
            if (!key.equals("Authorization", ignoreCase = true)) {
                builder.addHeader(key, value)
            }
        }

        if (provider.name.lowercase().contains("openrouter") ||
            provider.baseUrl.lowercase().contains("openrouter.ai")
        ) {
            if (provider.customHeaders.keys.none { it.equals("HTTP-Referer", ignoreCase = true) }) {
                builder.addHeader("HTTP-Referer", "https://github.com/mertgoevse-wq/PocketCodeAgent")
            }
            if (provider.customHeaders.keys.none { it.equals("X-Title", ignoreCase = true) }) {
                builder.addHeader("X-Title", "PocketCodeAgent")
            }
        }

        return builder.build()
    }

    private fun extractContentFromChoice(choice: JsonObject): String? {
        val delta = choice.getAsJsonObject("delta")
        val message = choice.getAsJsonObject("message")
        return contentToString(delta?.get("content"))
            ?: contentToString(message?.get("content"))
            ?: contentToString(choice.get("text"))
            ?: extractFromNestedOutput(choice.getAsJsonArray("output"))
            ?: extractFromNestedContent(choice.getAsJsonArray("content"))
    }

    private fun extractFromNestedOutput(output: JsonArray?): String? {
        if (output == null || output.size() == 0) return null
        val first = output[0]?.asJsonObject ?: return null
        val content = first.getAsJsonArray("content") ?: first.getAsJsonArray("parts")
        if (content != null && content.size() > 0) {
            return contentToString(content[0])
        }
        return contentToString(first.get("text"))
            ?: contentToString(first.get("output"))
    }

    private fun extractFromNestedContent(content: JsonArray?): String? {
        if (content == null || content.size() == 0) return null
        var result = ""
        for (i in 0 until content.size()) {
            val item = content[i]
            if (item.isJsonObject) {
                val obj = item.asJsonObject
                val text = contentToString(obj.get("text")) ?: contentToString(obj.get("value"))
                if (text != null) result += text
            } else if (item.isJsonPrimitive) {
                result += item.asString
            }
        }
        return result.takeIf { it.isNotEmpty() }
    }

    private fun contentToString(element: JsonElement?): String? {
        if (element == null || element.isJsonNull) return null
        return when {
            element.isJsonPrimitive -> element.asString
            element.isJsonArray -> element.asJsonArray.mapNotNull { part ->
                if (part.isJsonObject) {
                    val obj = part.asJsonObject
                    contentToString(obj.get("text")) ?: contentToString(obj.get("content"))
                } else {
                    contentToString(part)
                }
            }.joinToString("")
            else -> element.toString()
        }?.takeIf { it.isNotEmpty() }
    }

    private fun extractCompletionContent(root: JsonObject, provider: Provider): String {
        val error = root.getAsJsonObject("error")
        if (error != null) {
            val message = error.get("message")?.asString ?: "Provider meldet einen Fehler."
            throw ProviderApiException("${providerLabel(provider)}: ${sanitizedText(message, provider)}")
        }

        // Format A/B: choices[] with message/delta content
        val choices = root.getAsJsonArray("choices")
        if (choices != null && choices.size() > 0) {
            val content = extractContentFromChoice(choices[0].asJsonObject)
            if (!content.isNullOrBlank()) return content
        }

        // Format C: text at top level
        root.get("text")?.asString?.takeIf { it.isNotBlank() }?.let { return it }

        // Format D: output array at top level
        val output = root.getAsJsonArray("output")
        if (output != null) {
            val nested = extractFromNestedOutput(output)
            if (!nested.isNullOrBlank()) return nested
        }

        // Generic fallbacks: content/response/message at top level
        root.get("content")?.asString?.takeIf { it.isNotBlank() }?.let { return it }
        root.get("response")?.asString?.takeIf { it.isNotBlank() }?.let { return it }
        root.get("result")?.asString?.takeIf { it.isNotBlank() }?.let { return it }

        // Unknown format: include sanitized response schema without secrets
        val snippet = sanitizedText(gson.toJson(root).take(300), provider)
        throw ProviderApiException(
            "${providerLabel(provider)}: Antwortformat unerwartet. HTTP war erfolgreich, Parser konnte keinen Text finden. Schema: $snippet"
        )
    }

    private fun parseJsonObject(raw: String, provider: Provider): JsonObject {
        return try {
            JsonParser.parseString(raw).asJsonObject
        } catch (_: Exception) {
            // Fallback: try to extract any text from non-JSON responses
            tryExtractTextFromRaw(raw)?.let { text ->
                return JsonObject().apply { addProperty("content", sanitizedText(text, provider)) }
            }
            val snippet = sanitizedText(raw.take(200), provider)
            throw ProviderApiException(
                "${providerLabel(provider)}: Parserfehler - Antwort war kein gültiges JSON. Raw: $snippet"
            )
        }
    }

    private fun tryExtractTextFromRaw(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null

        // Try to find a reasonable text block using simple string extraction
        val patterns = listOf(
            "\"content\"" to "content",
            "\"text\"" to "text",
            "\"message\"" to "message",
            "\"response\"" to "response"
        )
        for ((keyPattern, _) in patterns) {
            val escapedPattern = Regex(
                keyPattern + "\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"",
                setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
            )
            val match = escapedPattern.find(trimmed)
            if (match != null) {
                val text = match.groupValues[1]
                    .replace("\\\"", "\"")
                    .replace("\\n", "\n")
                    .replace("\\t", "\t")
                if (text.isNotBlank() && text.length > 1) return text.take(2000)
            }
        }

        // Last resort: if the raw text is plain text (not surrounded by JSON braces)
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[") && trimmed.length > 1 && trimmed.length < 3000) {
            return trimmed
        }

        return null
    }

    suspend fun testProvider(provider: Provider): ProviderConnectionResult {
        return withContext(Dispatchers.IO) {
            try {
                requireProviderReady(provider)
                val messages = listOf(
                    mapOf("role" to "user", "content" to "Reply with OK only.")
                )
                val request = buildRequest(
                    provider = provider,
                    url = chatUrl(provider.baseUrl),
                    bodyJson = buildChatBody(
                        provider = provider,
                        messages = messages,
                        temperature = 0.0,
                        maxTokens = 8,
                        stream = false
                    )
                )

                client.newCall(request).execute().use { response ->
                    val bodyString = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        val error = httpError(provider, response.code, bodyString)
                        return@withContext ProviderConnectionResult(
                            success = false,
                            providerName = provider.displayName,
                            modelName = provider.modelName,
                            httpCode = error.httpCode,
                            sanitizedError = error.message
                        )
                    }
                    val content = extractCompletionContent(parseJsonObject(bodyString, provider), provider)
                    val trimmed = content.trim()
                    val okTest = trimmed.lowercase().let { t ->
                        t == "ok" || t == "ok." || t == "okay" || t.contains("ok")
                    }
                    ProviderConnectionResult(
                        success = true,
                        providerName = provider.displayName,
                        modelName = provider.modelName,
                        httpCode = response.code,
                        answer = if (okTest && trimmed.length <= 10) "OK" else trimmed.take(80)
                    )
                }
            } catch (throwable: Throwable) {
                val error = throwableMessage(provider, throwable)
                ProviderConnectionResult(
                    success = false,
                    providerName = provider.displayName,
                    modelName = provider.modelName,
                    httpCode = error.httpCode,
                    sanitizedError = error.message
                )
            }
        }
    }

    suspend fun listModels(provider: Provider): Result<List<String>> {
        return withContext(Dispatchers.IO) {
            try {
                requireProviderAuth(provider)
                val request = buildRequest(provider, modelsUrl(provider.baseUrl))
                client.newCall(request).execute().use { response ->
                    val bodyString = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(httpError(provider, response.code, bodyString))
                    }
                    val root = parseJsonObject(bodyString, provider)
                    val data = root.getAsJsonArray("data")
                        ?: return@withContext Result.failure(
                            ProviderApiException("${provider.displayName}: Modellliste konnte nicht gelesen werden.")
                        )
                    val models = data.mapNotNull { item ->
                        if (item.isJsonObject) item.asJsonObject.get("id")?.asString else null
                    }.filter { it.isNotBlank() }.distinct()

                    if (models.isEmpty()) {
                        Result.failure(ProviderApiException("${provider.displayName}: Modellliste ist leer."))
                    } else {
                        Result.success(models)
                    }
                }
            } catch (throwable: Throwable) {
                Result.failure(throwableMessage(provider, throwable))
            }
        }
    }

    suspend fun chatCompletionStream(
        provider: Provider,
        messages: List<Map<String, String>>,
        onChunk: (String) -> Unit
    ): Result<String> {
        return try {
            requireProviderReady(provider)
            val streamingResult = if (provider.supportsStreaming) {
                executeStreamingChat(provider, messages, onChunk)
            } else {
                Result.failure(ProviderApiException("${provider.displayName}: Streaming nicht aktiviert."))
            }

            if (streamingResult.isSuccess) {
                streamingResult
            } else {
                val streamError = streamingResult.exceptionOrNull()
                if (streamError is ProviderApiException && streamError.httpCode in setOf(401, 403, 404)) {
                    streamingResult
                } else {
                    executeNonStreamingChat(provider, messages, onChunk)
                }
            }
        } catch (throwable: Throwable) {
            Result.failure(throwableMessage(provider, throwable))
        }
    }

    private suspend fun executeNonStreamingChat(
        provider: Provider,
        messages: List<Map<String, String>>,
        onChunk: (String) -> Unit
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val request = buildRequest(
                    provider = provider,
                    url = chatUrl(provider.baseUrl),
                    bodyJson = buildChatBody(
                        provider = provider,
                        messages = messages,
                        temperature = 0.7,
                        maxTokens = 2048,
                        stream = false
                    )
                )
                client.newCall(request).execute().use { response ->
                    val bodyString = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(httpError(provider, response.code, bodyString))
                    }
                    val content = extractCompletionContent(parseJsonObject(bodyString, provider), provider)
                    onChunk(content)
                    Result.success(content)
                }
            } catch (throwable: Throwable) {
                Result.failure(throwableMessage(provider, throwable))
            }
        }
    }

    private suspend fun executeStreamingChat(
        provider: Provider,
        messages: List<Map<String, String>>,
        onChunk: (String) -> Unit
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val request = buildRequest(
                    provider = provider,
                    url = chatUrl(provider.baseUrl),
                    bodyJson = buildChatBody(
                        provider = provider,
                        messages = messages,
                        temperature = 0.7,
                        maxTokens = 2048,
                        stream = true
                    )
                )
                client.newCall(request).execute().use { response ->
                    val body = response.body
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(httpError(provider, response.code, body?.string()))
                    }
                    if (body == null) {
                        return@withContext Result.failure(
                            ProviderApiException("${providerLabel(provider)}: Leere Antwort vom Provider.")
                        )
                    }

                    val fullContent = StringBuilder()
                    val rawJsonBuffer = StringBuilder()
                    var hasSeenDataPrefix = false

                    BufferedReader(body.charStream()).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            val currentLine = line.orEmpty().trim()
                            if (currentLine.isBlank()) continue

                            if (currentLine.startsWith("data:")) {
                                hasSeenDataPrefix = true
                                val data = currentLine.removePrefix("data:").trim()
                                if (data == "[DONE]") break

                                val root = parseJsonObject(data, provider)
                                val content = extractStreamingContent(root, provider)
                                if (content.isNotEmpty()) {
                                    fullContent.append(content)
                                    onChunk(content)
                                }
                            } else if (!hasSeenDataPrefix) {
                                rawJsonBuffer.append(currentLine)
                            }
                        }
                    }

                    if (fullContent.isEmpty() && rawJsonBuffer.isNotBlank()) {
                        val content = extractCompletionContent(parseJsonObject(rawJsonBuffer.toString(), provider), provider)
                        fullContent.append(content)
                        onChunk(content)
                    }

                    if (fullContent.isEmpty()) {
                        val snippet = sanitizedText(rawJsonBuffer.toString().take(300), provider)
                        return@withContext Result.failure(
                            ProviderApiException(
                                "${providerLabel(provider)}: Streaming lieferte keinen Inhalt. Raw: $snippet"
                            )
                        )
                    }

                    Result.success(fullContent.toString())
                }
            } catch (throwable: Throwable) {
                Result.failure(throwableMessage(provider, throwable))
            }
        }
    }

    private fun extractStreamingContent(root: JsonObject, provider: Provider): String {
        val error = root.getAsJsonObject("error")
        if (error != null) {
            val message = error.get("message")?.asString ?: "Provider meldet einen Fehler."
            throw ProviderApiException("${providerLabel(provider)}: ${sanitizedText(message, provider)}")
        }
        val choices = root.getAsJsonArray("choices")
        if (choices != null && choices.size() > 0) {
            return extractContentFromChoice(choices[0].asJsonObject).orEmpty()
        }
        // Non-standard streaming: try top-level text
        root.get("text")?.asString?.takeIf { it.isNotBlank() }?.let { return it }
        root.get("content")?.asString?.takeIf { it.isNotBlank() }?.let { return it }
        return ""
    }
}
