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

@Serializable
data class GeminiResponse(val candidates: List<Candidate> = emptyList())

@Serializable
data class Candidate(val content: GeminiContent = GeminiContent(), val finishReason: String = "")

@Serializable
data class GeminiContent(val parts: List<GeminiPart> = emptyList())

@Serializable
data class GeminiPart(val text: String = "")

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

    suspend fun chat(systemPrompt: String, messages: List<ChatMessage>, temperature: Double = 0.7): String =
        withContext(Dispatchers.IO) {
            val body = buildJsonObject {
                putJsonObject("systemInstruction") {
                    putJsonArray("parts") {
                        addJsonObject { put("text", systemPrompt) }
                    }
                }
                putJsonArray("contents") {
                    for (msg in messages) {
                        addJsonObject {
                            put("role", if (msg.role == "assistant") "model" else "user")
                            putJsonArray("parts") {
                                addJsonObject { put("text", msg.content) }
                            }
                        }
                    }
                }
                putJsonObject("generationConfig") {
                    put("maxOutputTokens", 8192)
                    put("temperature", temperature) 
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
            responseJson["error"]?.let { error ->
                val errorMsg = error.jsonObject["message"]?.jsonPrimitive?.content ?: responseText
                throw RuntimeException("Gemini API error: $errorMsg")
            }


            val geminiResponse = json.decodeFromString<GeminiResponse>(responseText)
            if (geminiResponse.candidates.isEmpty()) {
                throw RuntimeException("No candidates in response: $responseText")
            }

            val candidate = geminiResponse.candidates[0]


            if (candidate.finishReason == "MAX_TOKENS") {
                println("LLM WARNING: la respuesta fue truncada (finishReason=MAX_TOKENS)")
            }

            val text = candidate.content.parts.firstOrNull()?.text
                ?: throw RuntimeException("No text in parts: $responseText")
            text
        }
}
