package swimming.agent

import swimming.model.AnalysisResult
import swimming.service.ChatMessage
import swimming.service.LLMService
import swimming.service.RascalService
import swimming.util.adjustGeneratedDistance

data class OptimizationConfig(
    val goal: String,
    val weeks: Int,
    val sessionsPerWeek: Int,
    val baseDistance: Int,
    val styles: List<String>,
    val maxMinutes: Int
)

data class SessionPlan(
    val week: Int,
    val sessionNumber: Int,
    val code: String,
    val analysis: AnalysisResult
)

data class OptimizationResult(
    val success: Boolean,
    val sessions: List<SessionPlan> = emptyList(),
    val error: String? = null
)

class OptimizerAgent(
    private val llmService: LLMService,
    private val rascalService: RascalService
) {
    companion object {
        private const val MAX_RASCAL_CALLS = 10

        private val SYSTEM_PROMPT = """
You are a swimming training plan optimizer. Given a training goal and parameters, you generate a progressive training plan as individual DSL sessions.

You must output ONLY the DSL code for a single session, no explanations.

The DSL grammar:
- session <name> { <blocks or sections> }
- Sections: warmup { ... } main { ... } cooldown { ... }
- swim <distance> m [<style>] [<intensity>] [pace <number>] [with <equipment>]
- kick <distance> m [<intensity>] [with <equipment>]
- drill <drillType> <distance> m [<intensity>]
- Intervals: <N> x <exercise> rest <N> s
- Styles: freestyle | backstroke | breaststroke | butterfly
- Intensities: easy | moderate | hard
- Equipment: fins | paddles | board | pullbuoy | snorkel
- Drills: catchup | onesided | fingertip | sixKick | sculling

Progression guidelines per goal:
- endurance: increase distance 5-10% per week, keep pace moderate, long main sets
- speed: increase intensity and decrease rest over weeks, shorter faster intervals
- technique: more drills and equipment, moderate distance, focus on form
- recovery: low intensity, varied styles, shorter sessions, more rest

Output ONLY the DSL code.
""".trimIndent()
    }

    suspend fun optimize(
        config: OptimizationConfig,
        onProgress: suspend (String) -> Unit
    ): OptimizationResult {
        val sessions = mutableListOf<SessionPlan>()
        var rascalCallCount = 0
        val totalSessions = config.weeks * config.sessionsPerWeek

        for (week in 1..config.weeks) {
            for (sessionNum in 1..config.sessionsPerWeek) {
                val globalSessionNum = (week - 1) * config.sessionsPerWeek + sessionNum
                onProgress("Generando sesión $globalSessionNum de $totalSessions (semana $week)...")

                if (rascalCallCount >= MAX_RASCAL_CALLS) {
                    onProgress("Límite de llamadas a Rascal alcanzado ($MAX_RASCAL_CALLS). Plan parcial generado.")
                    return OptimizationResult(success = true, sessions = sessions)
                }

                val progressionFactor = 1.0 + (week - 1) * 0.07
                val targetDistance = (config.baseDistance * progressionFactor).toInt()
                val targetMinutes = config.maxMinutes

                val prompt = buildString {
                    append("Generate a swimming session with these parameters:\n")
                    append("- Goal: ${config.goal}\n")
                    append("- Week: $week of ${config.weeks} (progression factor: ${"%.0f".format(progressionFactor * 100)}%)\n")
                    append("- Session: $sessionNum of ${config.sessionsPerWeek} this week\n")
                    append("- Target distance: ~${targetDistance}m\n")
                    append("- Max duration: ${targetMinutes} minutes\n")
                    append("- Preferred styles: ${config.styles.joinToString(", ")}\n")
                    append("- Session name: week${week}_session${sessionNum}\n")
                    if (sessions.isNotEmpty()) {
                        val lastSession = sessions.last()
                        append("- Previous session had ${lastSession.analysis.totalDistance}m. ")
                        append("Make this one progressively ${if (config.goal == "recovery") "lighter" else "harder"}.\n")
                    }
                    append("\nUse warmup/main/cooldown structure. Output ONLY the DSL code.")
                }

                val messages = listOf(ChatMessage("user", prompt))

                val response = try {
                    llmService.chat(SYSTEM_PROMPT, messages)
                } catch (e: Exception) {
                    return OptimizationResult(
                        success = false,
                        sessions = sessions,
                        error = "Error calling LLM for session $globalSessionNum: ${e.message}"
                    )
                }

                val dslCode = response.trim()
                    .removePrefix("```swim").removePrefix("```").removeSuffix("```").trim()

                onProgress("Analizando sesión $globalSessionNum...")
                rascalCallCount++
                val analysis = rascalService.analyze(dslCode)

                if (analysis.success) {
                    sessions.add(
                        SessionPlan(
                            week = week,
                            sessionNumber = sessionNum,
                            code = dslCode,
                            analysis = analysis
                        )
                    )
                } else {
                    // Try one fix with Rascal generate as fallback
                    if (rascalCallCount < MAX_RASCAL_CALLS) {
                        onProgress("Sesión $globalSessionNum inválida, usando generador Rascal como fallback...")
                        rascalCallCount++
                        val fallback = rascalService.generate(
                            config.goal, targetDistance, config.styles, targetMinutes
                        )
                        if (fallback.success && fallback.code != null) {
                            val adjustedCode = adjustGeneratedDistance(fallback.code, targetDistance)
                            rascalCallCount++
                            val fallbackAnalysis = rascalService.analyze(adjustedCode)
                            if (fallbackAnalysis.success) {
                                sessions.add(
                                    SessionPlan(
                                        week = week,
                                        sessionNumber = sessionNum,
                                        code = adjustedCode,
                                        analysis = fallbackAnalysis
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        return OptimizationResult(success = true, sessions = sessions)
    }
}
