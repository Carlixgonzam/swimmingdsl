package swimming.agent

import kotlinx.serialization.json.Json
import swimming.model.AnalysisResult
import swimming.service.ChatMessage
import swimming.service.LLMService

class CoachAgent(
    private val llmService: LLMService
) {
    private val conversationHistory = mutableListOf<ChatMessage>()
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    companion object {
        private const val BASE_SYSTEM_PROMPT = """Eres un entrenador experto de natación con años de experiencia entrenando nadadores de todos los niveles, desde principiantes hasta competidores.

Tu rol:
- Dar feedback práctico y específico sobre sesiones de entrenamiento
- Mencionar métricas concretas del análisis (distancia, tiempos, estilos, intensidades, descansos)
- Sugerir ajustes concretos cuando sea necesario
- Adaptar tus consejos al nivel del nadador según lo que se observa en la sesión
- Responder en el mismo idioma que el usuario (español o inglés)

Pautas de entrenamiento que conoces:
- Una sesión balanceada tiene calentamiento (15-20%), parte principal (60-70%) y vuelta a la calma (10-15%)
- Los descansos deben ser proporcionales a la intensidad: más descanso para series rápidas
- La variedad de estilos es buena para evitar lesiones y trabajar diferentes grupos musculares
- El pace (segundos por 100m) indica la velocidad: menor pace = más rápido
- Para principiantes: 1500-2500m total, pace 120-150s/100m
- Para intermedios: 2500-4000m total, pace 90-120s/100m
- Para avanzados: 4000-6000m total, pace 70-95s/100m

Si el usuario pide cambios al código, describe exactamente qué cambiarías en el DSL.
Sé conciso pero completo en tus respuestas."""
    }

    fun resetConversation() {
        conversationHistory.clear()
    }

    suspend fun chat(
        userMessage: String,
        analysisResult: AnalysisResult?,
        currentCode: String?
    ): String {
        val systemPrompt = buildString {
            append(BASE_SYSTEM_PROMPT)
            if (analysisResult != null && analysisResult.success) {
                append("\n\n--- ANÁLISIS DE LA SESIÓN ACTUAL ---\n")
                append("Distancia total: ${analysisResult.totalDistance}m (${analysisResult.distanceKm}km)\n")
                append("Sesiones: ${analysisResult.sessionCount}\n")
                append("Estilos: ${analysisResult.styles}\n")
                append("Intensidades: ${analysisResult.intensities}\n")
                append("Tiempo nado: ${analysisResult.time.swimFormatted}\n")
                append("Tiempo descanso: ${analysisResult.time.restFormatted}\n")
                append("Tiempo total: ${analysisResult.time.totalFormatted}\n")
                append("Descansos: ${analysisResult.rest.periods} períodos, promedio ${analysisResult.rest.average}s\n")
                if (analysisResult.equipment.isNotEmpty()) {
                    append("Equipamiento: ${analysisResult.equipment}\n")
                }
                if (analysisResult.drills.isNotEmpty()) {
                    append("Drills: ${analysisResult.drills}\n")
                }
            }
            if (currentCode != null) {
                append("\n--- CÓDIGO DSL ACTUAL ---\n")
                append(currentCode)
                append("\n")
            }
        }

        conversationHistory.add(ChatMessage("user", userMessage))

        val response = try {
            llmService.chat(systemPrompt, conversationHistory)
        } catch (e: Exception) {
            conversationHistory.removeLastOrNull()
            throw e
        }

        conversationHistory.add(ChatMessage("assistant", response))
        return response
    }
}
