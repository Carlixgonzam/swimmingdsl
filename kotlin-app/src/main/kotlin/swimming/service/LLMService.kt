package swimming.service

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
data class ChatMessage(val role: String, val content: String)

class LLMService {

    private val apiKey: String by lazy {
        System.getenv("GEMINI_API_KEY")
            ?: error("GEMINI_API_KEY environment variable is not set. Please set it to a valid Gemini API key.")
    }

    private val model = "gemini-2.5-flash"

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        engine {
            requestTimeout = 60_000
        }
    }

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun chat(systemPrompt: String, messages: List<ChatMessage>): String =
        withContext(Dispatchers.IO) {
            val body = buildJsonObject {
                // System instruction
                putJsonObject("system_instruction") {
                    putJsonArray("parts") {
                        addJsonObject { put("text", systemPrompt) }
                    }
                }
                // Conversation messages (Gemini uses "model" instead of "assistant")
                putJsonArray("contents") {
                    for (msg in messages) {
                        addJsonObject {
                            put("role", if (msg.role == "assistant") "model" else msg.role)
                            putJsonArray("parts") {
                                addJsonObject { put("text", msg.content) }
                            }
                        }
                    }
                }
                putJsonObject("generationConfig") {
                    put("maxOutputTokens", 2000)
                }
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            println("[LLM] Calling Gemini model=$model")
            val response = client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(body.toString())
            }

            val responseText = response.bodyAsText()
            println("[LLM] Response status: ${response.status}")
            println("[LLM] Response (first 500 chars): ${responseText.take(500)}")
            val responseJson = json.parseToJsonElement(responseText).jsonObject

            // Check for error
            responseJson["error"]?.let { error ->
                val errorMsg = error.jsonObject["message"]?.jsonPrimitive?.content ?: responseText
                throw RuntimeException("Gemini API error: $errorMsg")
            }

            val candidates = responseJson["candidates"]?.jsonArray
                ?: throw RuntimeException("No candidates in response: $responseText")
            val parts = candidates[0].jsonObject["content"]?.jsonObject?.get("parts")?.jsonArray
                ?: throw RuntimeException("No parts in response: $responseText")
            parts[0].jsonObject["text"]?.jsonPrimitive?.content
                ?: throw RuntimeException("No text in parts: $responseText")
        }
}
