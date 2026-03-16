package swimming.agent

import swimming.model.UserProfile
import swimming.service.ChatMessage
import swimming.service.LLMService
import swimming.service.RascalService

data class TranslationResult(
    val success: Boolean,
    val dslCode: String? = null,
    val error: String? = null,
    val attempts: Int = 0
)

class DSLTranslatorAgent(
    private val llmService: LLMService,
    private val rascalService: RascalService,
    private val userProfile: UserProfile = UserProfile()
) {

    companion object {
        private const val MAX_RETRIES = 3

        private val SYSTEM_PROMPT = """
You are a Swimming DSL code generator. You receive a natural language description of a swimming training session and generate ONLY valid DSL code. No explanations, no markdown, no comments — just the raw DSL code.

Here is the COMPLETE grammar of the DSL:

PROGRAM:
  One or more sessions.

SESSION:
  session <name> { <blocks or sections> }
  Sections (optional structure): warmup { ... } main { ... } cooldown { ... }

BLOCKS:
  Exercise — a single exercise
  Interval — N x Exercise rest N s

EXERCISES:
  swim <distance> m [<style>] [<intensity>] [pace <number>] [with <equipment>] [target <min>:<sec>]
  kick <distance> m [<intensity>] [with <equipment>]
  drill <drillType> <distance> m [<intensity>]

STYLES: freestyle | backstroke | breaststroke | butterfly
INTENSITIES: easy | moderate | hard
EQUIPMENT: fins | paddles | board | pullbuoy | snorkel
DRILL TYPES: catchup | onesided | fingertip | sixKick | sculling

INTERVALS:
  <N> x <exercise> rest <seconds> s

EXAMPLES OF VALID CODE:

session morning {
  swim 400 m freestyle easy pace 120
  4 x swim 100 m freestyle hard pace 90 rest 20 s
  swim 200 m backstroke moderate pace 130
  3 x kick 50 m hard rest 15 s
}

session structured {
  warmup {
    swim 400 m freestyle easy pace 120
  }
  main {
    8 x swim 100 m freestyle hard pace 75 rest 15 s
    4 x swim 200 m backstroke moderate pace 110 rest 30 s
  }
  cooldown {
    swim 200 m easy pace 140
  }
}

IMPORTANT RULES:
- Distance is always in meters with the "m" unit
- Pace is seconds per 100m
- Rest in intervals uses "s" for seconds
- Session names must be a single word (alphanumeric, no spaces)
- "swim" requires at least distance and "m"; style, intensity, pace, equipment are optional
- "kick" requires at least distance and "m"
- "drill" requires drillType, distance and "m"
- Intervals use "N x <exercise> rest N s" syntax
- Output ONLY the DSL code, nothing else
""".trimIndent()
    }

    suspend fun translate(userRequest: String): TranslationResult {
        val messages = mutableListOf(
            ChatMessage("user", userRequest)
        )

        for (attempt in 1..MAX_RETRIES) {
            val response = try {
                llmService.chat(SYSTEM_PROMPT, messages, temperature = 0.2)
            } catch (e: Exception) {
                return TranslationResult(
                    success = false,
                    error = "Error calling LLM: ${e.message}",
                    attempts = attempt
                )
            }

            val dslCode = response.trim()
                .removePrefix("```swim").removePrefix("```").removeSuffix("```").trim()

            val analysisResult = rascalService.analyze(dslCode)

            if (analysisResult.success) {
                return TranslationResult(
                    success = true,
                    dslCode = dslCode,
                    attempts = attempt
                )
            }


            if (attempt == MAX_RETRIES) {
                return TranslationResult(
                    success = false,
                    dslCode = dslCode,
                    error = "Failed after $MAX_RETRIES attempts. Last error: ${analysisResult.error}",
                    attempts = attempt
                )
            }


            messages.add(ChatMessage("assistant", dslCode))
            messages.add(
                ChatMessage(
                    "user",
                    "Este código generó el error: ${analysisResult.error}. Corrígelo respetando la gramática."
                )
            )
        }

        return TranslationResult(success = false, error = "Unexpected error", attempts = MAX_RETRIES)
    }
}
