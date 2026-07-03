package com.pocketcodeagent.data.network

import com.pocketcodeagent.data.model.Provider
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.BufferedReader
import java.io.IOException
import java.util.concurrent.TimeUnit

object ApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()
    private val gson = Gson()

    private fun getFullUrl(baseUrl: String): String {
        val trimmed = baseUrl.trim()
        return when {
            trimmed.endsWith("/chat/completions") -> trimmed
            trimmed.endsWith("/") -> "${trimmed}chat/completions"
            else -> "$trimmed/chat/completions"
        }
    }

    suspend fun testProvider(provider: Provider): Result<String> {
        val fullUrl = getFullUrl(provider.baseUrl)
        val messages = listOf(
            mapOf("role" to "user", "content" to "Hello, quick test. Reply with exactly 'OK'.")
        )
        val requestBodyMap = mapOf(
            "model" to provider.modelName,
            "messages" to messages,
            "max_tokens" to 5,
            "stream" to false
        )
        val requestBodyJson = gson.toJson(requestBodyMap)

        val requestBuilder = Request.Builder()
            .url(fullUrl)
            .post(requestBodyJson.toRequestBody(mediaType))
            .addHeader("Authorization", "Bearer ${provider.apiKey}")
            .addHeader("Content-Type", "application/json")

        // Add custom headers
        for ((key, value) in provider.customHeaders) {
            requestBuilder.addHeader(key, value)
        }

        // Add special headers for OpenRouter
        if (provider.name.lowercase().contains("openrouter")) {
            requestBuilder.addHeader("HTTP-Referer", "https://github.com/mertgoevse-wq/PocketCodeAgent")
            requestBuilder.addHeader("X-Title", "PocketCodeAgent")
        }

        return try {
            val response = client.newCall(requestBuilder.build()).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val responseJson = gson.fromJson(body, JsonObject::class.java)
                val choices = responseJson.getAsJsonArray("choices")
                if (choices != null && choices.size() > 0) {
                    val content = choices.get(0).asJsonObject
                        .getAsJsonObject("message")
                        .get("content").asString
                    Result.success(content.trim())
                } else {
                    Result.failure(IOException("Invalid response body structure: $body"))
                }
            } else {
                Result.failure(IOException("HTTP Error ${response.code}: ${response.body?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun chatCompletionStream(
        provider: Provider,
        messages: List<Map<String, String>>,
        onChunk: (String) -> Unit
    ): Result<String> {
        val fullUrl = getFullUrl(provider.baseUrl)
        val requestBodyMap = mapOf(
            "model" to provider.modelName,
            "messages" to messages,
            "stream" to true
        )
        val requestBodyJson = gson.toJson(requestBodyMap)

        val requestBuilder = Request.Builder()
            .url(fullUrl)
            .post(requestBodyJson.toRequestBody(mediaType))
            .addHeader("Authorization", "Bearer ${provider.apiKey}")
            .addHeader("Content-Type", "application/json")

        for ((key, value) in provider.customHeaders) {
            requestBuilder.addHeader(key, value)
        }

        if (provider.name.lowercase().contains("openrouter")) {
            requestBuilder.addHeader("HTTP-Referer", "https://github.com/mertgoevse-wq/PocketCodeAgent")
            requestBuilder.addHeader("X-Title", "PocketCodeAgent")
        }

        return try {
            val response = client.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful) {
                return Result.failure(IOException("HTTP Error ${response.code}: ${response.body?.string()}"))
            }

            val body = response.body ?: return Result.failure(IOException("Empty response body"))
            val reader = BufferedReader(body.charStream())
            val fullContent = StringBuilder()
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: break
                if (currentLine.startsWith("data: ")) {
                    val data = currentLine.substring(6).trim()
                    if (data == "[DONE]") continue
                    try {
                        val chunkJson = gson.fromJson(data, JsonObject::class.java)
                        val choices = chunkJson.getAsJsonArray("choices")
                        if (choices != null && choices.size() > 0) {
                            val delta = choices.get(0).asJsonObject.getAsJsonObject("delta")
                            if (delta != null && delta.has("content")) {
                                val contentChunk = delta.get("content").asString
                                fullContent.append(contentChunk)
                                onChunk(contentChunk)
                            }
                        }
                    } catch (e: Exception) {
                        // Suppress parsing errors for malformed SSE chunks
                    }
                }
            }
            Result.success(fullContent.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
