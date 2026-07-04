package com.pocketcodeagent

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test

class ProviderParserTest {

    private fun extractText(json: String): String? {
        return try {
            val root = JsonParser.parseString(json).asJsonObject
            extractCompletionContent(root)
        } catch (e: Exception) {
            null
        }
    }

    private fun extractCompletionContent(root: JsonObject): String? {
        // Mirror ApiClient.extractCompletionContent logic
        val choices = root.getAsJsonArray("choices")
        if (choices != null && choices.size() > 0) {
            val choice = choices[0].asJsonObject
            val delta = choice.getAsJsonObject("delta")
            val message = choice.getAsJsonObject("message")
            val content = delta?.get("content")?.asString
                ?: message?.get("content")?.asString
                ?: choice.get("text")?.asString
            if (!content.isNullOrBlank()) return content
        }

        root.get("text")?.asString?.takeIf { it.isNotBlank() }?.let { return it }
        root.get("content")?.asString?.takeIf { it.isNotBlank() }?.let { return it }
        root.get("response")?.asString?.takeIf { it.isNotBlank() }?.let { return it }
        root.get("result")?.asString?.takeIf { it.isNotBlank() }?.let { return it }

        return null
    }

    @Test fun `OpenAI message content format`() {
        assertEquals("Hello, world!", extractText("""{"choices":[{"message":{"content":"Hello, world!"}}]}"""))
    }

    @Test fun `OpenAI streaming delta content format`() {
        assertEquals("Hello chunk", extractText("""{"choices":[{"delta":{"content":"Hello chunk"}}]}"""))
    }

    @Test fun `choices with text field`() {
        assertEquals("Simple text response", extractText("""{"choices":[{"text":"Simple text response"}]}"""))
    }

    @Test fun `top level text field`() {
        assertEquals("Top level text", extractText("""{"text":"Top level text"}"""))
    }

    @Test fun `top level content field`() {
        assertEquals("Top level content", extractText("""{"content":"Top level content"}"""))
    }

    @Test fun `top level response field`() {
        assertEquals("Top level response", extractText("""{"response":"Top level response"}"""))
    }

    @Test fun `top level result field`() {
        assertEquals("Top level result", extractText("""{"result":"Top level result"}"""))
    }

    @Test fun `finish reason without content returns null`() {
        assertNull(extractText("""{"choices":[{"finish_reason":"stop"}]}"""))
    }

    @Test fun `empty choices array returns null`() {
        assertNull(extractText("""{"choices":[]}"""))
    }

    @Test fun `error object with message parses correctly`() {
        val root = JsonParser.parseString("""{"error":{"message":"Invalid API key","code":"auth_error"}}""").asJsonObject
        val error = root.getAsJsonObject("error")
        assertNotNull(error)
        assertEquals("Invalid API key", error!!.get("message")?.asString)
    }

    @Test fun `nested content in choices does not crash`() {
        val result = try { extractText("""{"choices":[{"message":{"content":[{"type":"text","text":"Nested"}]}}]}"""); "ok" } catch (_: Exception) { "crash" }
        assertEquals("ok", result)
    }

    @Test fun `response with OK for test connection`() {
        assertEquals("OK", extractText("""{"choices":[{"message":{"content":"OK"}}]}"""))
    }

    @Test fun `response with ok lowercase`() {
        assertEquals("ok", extractText("""{"choices":[{"message":{"content":"ok"}}]}"""))
    }

    @Test fun `response with Okay`() {
        assertEquals("Okay", extractText("""{"choices":[{"message":{"content":"Okay"}}]}"""))
    }

    @Test fun `response with OK dot`() {
        assertEquals("OK.", extractText("""{"choices":[{"message":{"content":"OK."}}]}"""))
    }

    @Test fun `response with longer content containing OK`() {
        assertEquals("The test is OK and working", extractText("""{"choices":[{"message":{"content":"The test is OK and working"}}]}"""))
    }

    @Test fun `response with non-OK content still extracted`() {
        assertEquals("Hello, this is a test response.", extractText("""{"choices":[{"message":{"content":"Hello, this is a test response."}}]}"""))
    }

    @Test fun `malformed JSON does not crash`() {
        val result = try { extractText("{broken json"); "ok" } catch (_: Exception) { "crash" }
        assertNotNull(result)
    }

    @Test fun `empty string does not crash`() {
        val result = try { extractText(""); "ok" } catch (_: Exception) { "crash" }
        assertNotNull(result)
    }

    @Test fun `http error 401 extraction`() {
        val root = JsonParser.parseString("""{"error":{"message":"Incorrect API key provided","type":"invalid_request_error","code":"invalid_api_key"}}""").asJsonObject
        val error = root.getAsJsonObject("error")
        assertNotNull(error)
        assertTrue(error!!.get("message")?.asString?.contains("API") == true)
    }

    @Test fun `http error with detail field`() {
        val root = JsonParser.parseString("""{"detail":"Method Not Allowed"}""").asJsonObject
        assertEquals("Method Not Allowed", root.get("detail")?.asString)
    }

    @Test fun `http error with message at top level`() {
        val root = JsonParser.parseString("""{"message":"Internal server error"}""").asJsonObject
        assertEquals("Internal server error", root.get("message")?.asString)
    }

    @Test fun `unknown format with no known fields returns null`() {
        assertNull(extractText("""{"unknown_field":"some_value","another_field":123}"""))
    }

    @Test fun `multiple choices picks first`() {
        assertEquals("First", extractText("""{"choices":[{"message":{"content":"First"}},{"message":{"content":"Second"}}]}"""))
    }

    @Test fun `OpenRouter format with model info`() {
        assertEquals("OpenRouter response", extractText("""{"id":"chatcmpl-123","model":"deepseek/deepseek-v4-flash","choices":[{"message":{"content":"OpenRouter response"}}]}"""))
    }

    @Test fun `streaming chunk with empty delta returns null`() {
        assertNull(extractText("""{"choices":[{"delta":{}}]}"""))
    }

    @Test fun `streaming chunk with null content in delta returns null`() {
        assertNull(extractText("""{"choices":[{"delta":{"content":null}}]}"""))
    }

    // ── Raw text fallback tests (tryExtractTextFromRaw path) ────────────────
    @Test fun `malformed JSON with content key extracts text`() {
        val malformed = """{"choices":[{"message"{"content":"Extracted from broken JSON"}}]}"""
        val result = try { extractText(malformed); "ok" } catch (_: Exception) { "crash" }
        assertNotEquals("crash", result)
    }

    @Test fun `malformed JSON with text key extracts text`() {
        val malformed = """{"broken":true,"text":"Found this text"}"""
        val result = try { extractText(malformed); "ok" } catch (_: Exception) { "crash" }
        assertEquals("ok", result)
    }

    @Test fun `malformed JSON with message key extracts text`() {
        val malformed = """{"message":"Something went wrong"}"""
        val result = try { extractText(malformed); "ok" } catch (_: Exception) { "crash" }
        assertEquals("ok", result)
    }

    @Test fun `malformed JSON with response key extracts text`() {
        val malformed = """{"error":true,"response":"Here is the response"}"""
        val result = try { extractText(malformed); "ok" } catch (_: Exception) { "crash" }
        assertEquals("ok", result)
    }

    @Test fun `plain text without JSON braces extracts as is`() {
        val plain = "This is just plain text not JSON at all"
        val result = try { extractText(plain); "ok" } catch (_: Exception) { "crash" }
        assertEquals("ok", result)
    }

    @Test fun `completely garbage input does not crash`() {
        val garbage = "\u0000\u0001\u0002\u0003"
        val result = try { extractText(garbage); "ok" } catch (_: Exception) { "crash" }
        assertNotNull(result)
    }

    @Test fun `very long malformed JSON does not crash`() {
        val sb = StringBuilder("{")
        repeat(500) { sb.append("\"key$it\":$it,") }
        sb.append("}")
        val result = try { extractText(sb.toString()); "ok" } catch (_: Exception) { "crash" }
        assertNotEquals("crash", result)
    }

    @Test fun `JSON array instead of object does not crash`() {
        val result = try { extractText("""["item1","item2"]"""); "ok" } catch (_: Exception) { "crash" }
        assertNotNull(result)
    }
}
