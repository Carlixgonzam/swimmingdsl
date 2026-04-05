package swimming.evaluation

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import swimming.agent.DSLTranslatorAgent
import swimming.service.ChatMessage
import swimming.service.LLMService
import swimming.service.RascalService
import java.io.File

// ═══════════════════════════════════════════════════════════════
//  Data Classes
// ═══════════════════════════════════════════════════════════════

data class EvalPrompt(val id: Int, val category: String, val prompt: String)

@Serializable
data class PromptResultA(
    val promptId: Int,
    val category: String,
    val prompt: String,
    val success: Boolean,
    val attempts: Int,
    val finalDsl: String? = null,
    val error: String? = null,
    val durationMs: Long = 0
)

@Serializable
data class PromptResultB(
    val promptId: Int,
    val category: String,
    val prompt: String,
    val success: Boolean,
    val resolvedBy: String, // "translator", "optimizer", "fallback", "none"
    val translatorAttempts: Int,
    val finalDsl: String? = null,
    val error: String? = null,
    val durationMs: Long = 0
)

@Serializable
data class CategoryMetrics(
    val total: Int,
    val successes: Int,
    val successRate: Double,
    val avgAttempts: Double
)

@Serializable
data class MetricsA(
    val totalPrompts: Int,
    val firstAttemptSuccesses: Int,
    val firstAttemptRate: Double,
    val totalSuccesses: Int,
    val successRate: Double,
    val failures: Int,
    val failureRate: Double,
    val avgAttempts: Double,
    val byCategory: Map<String, CategoryMetrics>
)

@Serializable
data class MetricsB(
    val totalPrompts: Int,
    val endToEndSuccesses: Int,
    val endToEndRate: Double,
    val resolvedByTranslator: Int,
    val translatorRate: Double,
    val resolvedByOptimizer: Int,
    val optimizerRate: Double,
    val resolvedByFallback: Int,
    val fallbackRate: Double,
    val improvementOverA: Double
)

@Serializable
data class EvaluationOutput(
    val experimentA: ExperimentAOutput,
    val experimentB: ExperimentBOutput
)

@Serializable
data class ExperimentAOutput(val metrics: MetricsA, val results: List<PromptResultA>)

@Serializable
data class ExperimentBOutput(val metrics: MetricsB, val results: List<PromptResultB>)

// ═══════════════════════════════════════════════════════════════
//  Dataset — 30 prompts in 3 difficulty tiers
// ═══════════════════════════════════════════════════════════════

val EVAL_DATASET = listOf(
    // ── Simple (10): single exercise type, basic parameters ────
    EvalPrompt(1, "simple", "Create a 500 meter freestyle easy session"),
    EvalPrompt(2, "simple", "I want a 400 meter backstroke workout at moderate intensity"),
    EvalPrompt(3, "simple", "Make a simple 800 meter breaststroke session"),
    EvalPrompt(4, "simple", "Generate a 200 meter butterfly hard sprint session"),
    EvalPrompt(5, "simple", "I need a 600 meter easy kick workout"),
    EvalPrompt(6, "simple", "Create a 1000 meter freestyle session at moderate pace of 110 seconds per 100m"),
    EvalPrompt(7, "simple", "Build me a 300 meter backstroke easy warmup session"),
    EvalPrompt(8, "simple", "I want a short 400 meter easy swim session"),
    EvalPrompt(9, "simple", "Make a 500 meter hard freestyle session with pace 80"),
    EvalPrompt(10, "simple", "Give me a 1000 meter easy endurance swim in freestyle"),

    // ── Medium (10): structured sessions, intervals, pace ──────
    EvalPrompt(11, "medium",
        "Create a 1500 meter session with warmup of 400m easy freestyle, " +
        "main set of 8x100m freestyle hard with 15 second rest, and 200m easy cooldown"),
    EvalPrompt(12, "medium",
        "I want interval training: 6 times 100 meter freestyle hard with 20 seconds rest, " +
        "then finish with 200m easy backstroke"),
    EvalPrompt(13, "medium",
        "Build a structured 2000 meter session with warmup, " +
        "main set of mixed freestyle and backstroke intervals, and cooldown"),
    EvalPrompt(14, "medium",
        "Design a pyramid workout: swim 50m, 100m, 200m, 100m, 50m all freestyle at moderate intensity"),
    EvalPrompt(15, "medium",
        "Make a 45 minute moderate training session with 400m warmup, " +
        "sprint intervals in the main set, and 300m cooldown"),
    EvalPrompt(16, "medium",
        "I need a session with 4x200m backstroke moderate with 30 seconds rest " +
        "and 4x100m freestyle hard with 15 seconds rest"),
    EvalPrompt(17, "medium",
        "Create a session starting with 500m easy warmup, " +
        "then 10x50m hard freestyle with 10 seconds rest, then 200m easy cooldown"),
    EvalPrompt(18, "medium",
        "Build a progressive distance workout: 4x50m then 4x100m then 4x200m " +
        "all freestyle with 20 second rest between each set"),
    EvalPrompt(19, "medium",
        "Design a session with warmup 300m easy freestyle, " +
        "main with kick intervals 6x50m hard with 15 second rest, and 200m easy cooldown"),
    EvalPrompt(20, "medium",
        "Make a 1 hour endurance session mostly freestyle, " +
        "include a 400m warmup, a long main set of continuous swimming, and 300m cooldown"),

    // ── Hard (10): equipment, drills, targets, mixed constructs ─
    EvalPrompt(21, "hard",
        "Create a technique session with warmup 400m easy freestyle, " +
        "then drills: catchup 200m, fingertip 200m, sculling 4x50m with 20 second rest, " +
        "and cooldown 200m easy"),
    EvalPrompt(22, "hard",
        "Build a session using fins for the warmup 300m easy, " +
        "paddles for main set 6x100m freestyle hard with 15 second rest, " +
        "and pullbuoy for cooldown 200m easy"),
    EvalPrompt(23, "hard",
        "Design a session with target times: warmup 400m freestyle easy, " +
        "main 8x100m freestyle hard pace 75 with target 1:15 and 15 second rest, " +
        "cooldown 200m easy"),
    EvalPrompt(24, "hard",
        "I want a mixed session: 400m warmup easy freestyle, " +
        "4x100m freestyle hard rest 15s, 4x50m kick hard with board rest 10s, " +
        "drill onesided 200m moderate, 200m cooldown easy"),
    EvalPrompt(25, "hard",
        "Create a complex pyramid: 50m easy, 100m moderate, 200m hard, " +
        "200m hard, 100m moderate, 50m easy all freestyle with pace and 20 second rest"),
    EvalPrompt(26, "hard",
        "Build a 2500 meter competition preparation session with structured warmup, " +
        "main set of butterfly and freestyle intervals, kick sets, and drill work in the cooldown"),
    EvalPrompt(27, "hard",
        "Design a recovery session using snorkel for 400m easy warmup, " +
        "pullbuoy for 4x100m easy backstroke rest 30s, " +
        "and fins for 200m easy cooldown"),
    EvalPrompt(28, "hard",
        "Create a technique focused session with all five drill types: " +
        "catchup, onesided, fingertip, sixKick, and sculling each 100m easy as the main set, " +
        "with 400m freestyle warmup and 200m backstroke cooldown"),
    EvalPrompt(29, "hard",
        "Build an advanced interval session: warmup 400m easy, " +
        "main has 8x100m freestyle hard pace 70 target 1:10 rest 15s with paddles, " +
        "then 4x50m butterfly hard rest 20s, cooldown 300m easy with fins"),
    EvalPrompt(30, "hard",
        "I need a multi-component session: warmup with 200m easy swim and 200m kick with board, " +
        "main with 6x100m breaststroke moderate pace 100 rest 20s " +
        "and drill sculling 4x50m rest 15s, cooldown 200m easy with snorkel")
)

// ═══════════════════════════════════════════════════════════════
//  LLM Repair Step (Optimizer substitute for single prompts)
// ═══════════════════════════════════════════════════════════════
//
// The existing OptimizerAgent generates multi-week training plans,
// so it doesn't fit single-prompt repair. Instead we do a focused
// LLM call with repair instructions + the previous error.

private val REPAIR_SYSTEM_PROMPT = """
You are a Swimming DSL code repair agent. You receive a natural language swimming session description 
and a previous parse error. Generate ONLY corrected, valid DSL code.

Grammar rules:
- session <name> { ... }
- Sections (optional): warmup { ... } main { ... } cooldown { ... }
- swim <distance> m [<style>] [<intensity>] [pace <number>] [with <equipment>] [target <min>:<sec>]
- kick <distance> m [<intensity>] [with <equipment>]
- drill <drillType> <distance> m [<intensity>]
- Intervals: <N> x <exercise> rest <N> s
- Styles: freestyle | backstroke | breaststroke | butterfly
- Intensities: easy | moderate | hard
- Equipment: fins | paddles | board | pullbuoy | snorkel
- Drills: catchup | onesided | fingertip | sixKick | sculling

IMPORTANT: Distance always uses "m". Rest always uses "s". Session names are single alphanumeric words.
Output ONLY the DSL code, nothing else.
""".trimIndent()

suspend fun attemptRepair(
    llmService: LLMService,
    rascalService: RascalService,
    originalPrompt: String,
    previousError: String
): String? {
    val messages = listOf(
        ChatMessage("user",
            "Original request: $originalPrompt\n\n" +
            "Previous attempt failed with error: $previousError\n\n" +
            "Generate a corrected DSL program for this request. Output ONLY the DSL code."
        )
    )

    return try {
        val response = llmService.chat(REPAIR_SYSTEM_PROMPT, messages, temperature = 0.1)
        val dslCode = response.trim()
            .removePrefix("```swim").removePrefix("```").removeSuffix("```").trim()
        val analysis = rascalService.analyze(dslCode)
        if (analysis.success) dslCode else null
    } catch (_: Exception) {
        null
    }
}

// ═══════════════════════════════════════════════════════════════
//  Parameter Extraction for Rascal Fallback Generator
// ═══════════════════════════════════════════════════════════════

data class FallbackParams(val goal: String, val distance: Int, val styles: List<String>, val duration: Int)

fun extractParamsFromPrompt(prompt: String): FallbackParams {
    val lower = prompt.lowercase()

    val goal = when {
        "technique" in lower || "drill" in lower -> "technique"
        "sprint" in lower || "speed" in lower || "competition" in lower -> "speed"
        "recovery" in lower -> "recovery"
        else -> "endurance"
    }

    // Find the largest distance mentioned (likely the total)
    val distances = Regex("(\\d{2,5})\\s*(?:m|meter)").findAll(lower)
        .map { it.groupValues[1].toInt() }
        .toList()
    val distance = distances.maxOrNull() ?: 2000

    val styles = mutableListOf<String>()
    if ("freestyle" in lower || "free" in lower) styles.add("freestyle")
    if ("backstroke" in lower) styles.add("backstroke")
    if ("breaststroke" in lower) styles.add("breaststroke")
    if ("butterfly" in lower) styles.add("butterfly")
    if (styles.isEmpty()) styles.add("freestyle")

    val durationMatch = Regex("(\\d+)\\s*(min|hour|minute)").find(lower)
    val duration = when {
        durationMatch != null -> {
            val value = durationMatch.groupValues[1].toInt()
            if ("hour" in durationMatch.groupValues[2]) value * 60 else value
        }
        else -> 60
    }

    return FallbackParams(goal, distance, styles, duration)
}

// ═══════════════════════════════════════════════════════════════
//  Metrics Computation
// ═══════════════════════════════════════════════════════════════

fun computeMetricsA(results: List<PromptResultA>): MetricsA {
    val total = results.size
    val firstAttemptSuccesses = results.count { it.success && it.attempts == 1 }
    val totalSuccesses = results.count { it.success }
    val failures = total - totalSuccesses
    val avgAttempts = if (totalSuccesses > 0)
        results.filter { it.success }.map { it.attempts.toDouble() }.average()
    else 0.0

    val byCategory = results.groupBy { it.category }.mapValues { (_, catResults) ->
        val catSuccesses = catResults.count { it.success }
        CategoryMetrics(
            total = catResults.size,
            successes = catSuccesses,
            successRate = pct(catSuccesses, catResults.size),
            avgAttempts = if (catSuccesses > 0)
                catResults.filter { it.success }.map { it.attempts.toDouble() }.average()
            else 0.0
        )
    }

    return MetricsA(
        totalPrompts = total,
        firstAttemptSuccesses = firstAttemptSuccesses,
        firstAttemptRate = pct(firstAttemptSuccesses, total),
        totalSuccesses = totalSuccesses,
        successRate = pct(totalSuccesses, total),
        failures = failures,
        failureRate = pct(failures, total),
        avgAttempts = avgAttempts,
        byCategory = byCategory
    )
}

fun computeMetricsB(results: List<PromptResultB>, metricsA: MetricsA): MetricsB {
    val total = results.size
    val successes = results.count { it.success }
    val byTranslator = results.count { it.resolvedBy == "translator" }
    val byOptimizer = results.count { it.resolvedBy == "optimizer" }
    val byFallback = results.count { it.resolvedBy == "fallback" }

    return MetricsB(
        totalPrompts = total,
        endToEndSuccesses = successes,
        endToEndRate = pct(successes, total),
        resolvedByTranslator = byTranslator,
        translatorRate = pct(byTranslator, total),
        resolvedByOptimizer = byOptimizer,
        optimizerRate = pct(byOptimizer, total),
        resolvedByFallback = byFallback,
        fallbackRate = pct(byFallback, total),
        improvementOverA = pct(successes, total) - metricsA.successRate
    )
}

private fun pct(num: Int, den: Int): Double =
    if (den > 0) num.toDouble() / den * 100.0 else 0.0

// ═══════════════════════════════════════════════════════════════
//  Console Output
// ═══════════════════════════════════════════════════════════════

fun printResultsA(m: MetricsA) {
    println()
    println("╔══════════════════════════════════════════════════════════╗")
    println("║          EXPERIMENT A: DSLTranslatorAgent Only          ║")
    println("╠══════════════════════════════════════════════════════════╣")
    println("║  Total prompts:            %3d                          ║".format(m.totalPrompts))
    println("║  First-attempt success:  %5.1f%%   (%d/%d)               ║".format(m.firstAttemptRate, m.firstAttemptSuccesses, m.totalPrompts))
    println("║  Success (≤3 attempts):  %5.1f%%   (%d/%d)               ║".format(m.successRate, m.totalSuccesses, m.totalPrompts))
    println("║  Avg attempts (success): %5.2f                          ║".format(m.avgAttempts))
    println("║  Failures:               %5.1f%%   (%d/%d)               ║".format(m.failureRate, m.failures, m.totalPrompts))
    println("╠──────────────────────────────────────────────────────────╣")
    println("║  By Category:                                            ║")
    for ((cat, cm) in m.byCategory.toSortedMap()) {
        println("║    %-8s  %5.1f%% success (%d/%d)  avg %.2f attempts     ║".format(
            cat, cm.successRate, cm.successes, cm.total, cm.avgAttempts))
    }
    println("╚══════════════════════════════════════════════════════════╝")
}

fun printResultsB(m: MetricsB) {
    println()
    println("╔══════════════════════════════════════════════════════════╗")
    println("║          EXPERIMENT B: Full Pipeline                    ║")
    println("╠══════════════════════════════════════════════════════════╣")
    println("║  End-to-end success:     %5.1f%%   (%d/%d)               ║".format(m.endToEndRate, m.endToEndSuccesses, m.totalPrompts))
    println("║  Resolved by Translator: %5.1f%%   (%d/%d)               ║".format(m.translatorRate, m.resolvedByTranslator, m.totalPrompts))
    println("║  Resolved by Optimizer:  %5.1f%%   (%d/%d)               ║".format(m.optimizerRate, m.resolvedByOptimizer, m.totalPrompts))
    println("║  Resolved by Fallback:   %5.1f%%   (%d/%d)               ║".format(m.fallbackRate, m.resolvedByFallback, m.totalPrompts))
    println("║  Improvement over A:     %+5.1f pp                       ║".format(m.improvementOverA))
    println("╚══════════════════════════════════════════════════════════╝")
}

fun printRepresentativeExamples(resultsA: List<PromptResultA>, resultsB: List<PromptResultB>) {
    println()
    println("══════════════════════════════════════════════════")
    println("  Representative Examples")
    println("══════════════════════════════════════════════════")

    // Case 1: success on first attempt
    val firstTrySuccess = resultsA.firstOrNull { it.success && it.attempts == 1 }
    if (firstTrySuccess != null) {
        println()
        println("── Case 1: Success on First Attempt (prompt #${firstTrySuccess.promptId}) ──")
        println("Prompt:   ${firstTrySuccess.prompt}")
        println("Attempts: 1")
        println("DSL:")
        firstTrySuccess.finalDsl?.lines()?.forEach { println("  $it") }
    }

    // Case 2: corrected after ParseError
    val corrected = resultsA.firstOrNull { it.success && it.attempts > 1 }
    if (corrected != null) {
        println()
        println("── Case 2: Corrected After ParseError (prompt #${corrected.promptId}) ──")
        println("Prompt:   ${corrected.prompt}")
        println("Attempts: ${corrected.attempts}")
        println("DSL:")
        corrected.finalDsl?.lines()?.forEach { println("  $it") }
    }

    // Case 3: fallback or optimizer rescue
    val fallback = resultsB.firstOrNull { it.resolvedBy == "fallback" }
    val optimizer = resultsB.firstOrNull { it.resolvedBy == "optimizer" }
    val rescueCase = fallback ?: optimizer
    if (rescueCase != null) {
        println()
        println("── Case 3: ${if (rescueCase.resolvedBy == "fallback") "Required Fallback" else "Resolved by Optimizer"} (prompt #${rescueCase.promptId}) ──")
        println("Prompt:     ${rescueCase.prompt}")
        println("Translator: ${rescueCase.translatorAttempts} attempts (all failed)")
        println("Resolved:   ${rescueCase.resolvedBy}")
        println("DSL:")
        rescueCase.finalDsl?.lines()?.forEach { println("  $it") }
    }

    // If no case 2 or 3 found, note it
    if (corrected == null) println("\n(No retried-and-corrected case found — all succeeded on first attempt or failed entirely)")
    if (rescueCase == null) println("\n(No fallback/optimizer case — translator resolved all prompts)")
}

// ═══════════════════════════════════════════════════════════════
//  File Export
// ═══════════════════════════════════════════════════════════════

fun exportResults(
    outputDir: File,
    resultsA: List<PromptResultA>,
    resultsB: List<PromptResultB>,
    metricsA: MetricsA,
    metricsB: MetricsB
) {
    val json = Json { prettyPrint = true; encodeDefaults = true }
    outputDir.mkdirs()

    // Full JSON
    val output = EvaluationOutput(
        experimentA = ExperimentAOutput(metricsA, resultsA),
        experimentB = ExperimentBOutput(metricsB, resultsB)
    )
    File(outputDir, "evaluation_results.json").writeText(json.encodeToString(output))

    // CSV — Experiment A
    File(outputDir, "experiment_a.csv").writeText(buildString {
        appendLine("prompt_id,category,success,attempts,duration_ms")
        for (r in resultsA) {
            appendLine("${r.promptId},${r.category},${r.success},${r.attempts},${r.durationMs}")
        }
    })

    // CSV — Experiment B
    File(outputDir, "experiment_b.csv").writeText(buildString {
        appendLine("prompt_id,category,success,resolved_by,translator_attempts,duration_ms")
        for (r in resultsB) {
            appendLine("${r.promptId},${r.category},${r.success},${r.resolvedBy},${r.translatorAttempts},${r.durationMs}")
        }
    })
}

// ═══════════════════════════════════════════════════════════════
//  Main Entry Point
// ═══════════════════════════════════════════════════════════════

fun main() = runBlocking {
    println("╔══════════════════════════════════════════════════════════╗")
    println("║          SwimmingDSL — Empirical Evaluation             ║")
    println("║          ${EVAL_DATASET.size} prompts · 2 experiments                    ║")
    println("╚══════════════════════════════════════════════════════════╝")
    println()

    val llmService = LLMService()
    val rascalService = RascalService()
    val translator = DSLTranslatorAgent(llmService, rascalService)

    // ───────────────────────────────────────────────────────────
    //  EXPERIMENT A — DSLTranslatorAgent Only
    // ───────────────────────────────────────────────────────────
    println("=== Running Experiment A: DSLTranslatorAgent ===")
    println()

    val resultsA = mutableListOf<PromptResultA>()

    for ((index, ep) in EVAL_DATASET.withIndex()) {
        print("[${index + 1}/${EVAL_DATASET.size}] #${ep.id} (${ep.category}) ... ")

        val t0 = System.currentTimeMillis()
        val result = translator.translate(ep.prompt)
        val elapsed = System.currentTimeMillis() - t0

        resultsA.add(PromptResultA(
            promptId = ep.id,
            category = ep.category,
            prompt = ep.prompt,
            success = result.success,
            attempts = result.attempts,
            finalDsl = result.dslCode,
            error = result.error,
            durationMs = elapsed
        ))

        val icon = if (result.success) "OK" else "FAIL"
        println("$icon  (${result.attempts} attempt${if (result.attempts != 1) "s" else ""}, ${elapsed}ms)")

        if (index < EVAL_DATASET.size - 1) delay(1_000)
    }

    // ───────────────────────────────────────────────────────────
    //  EXPERIMENT B — Full Pipeline
    //  Reuses Experiment A results; only runs extra steps for failures.
    // ───────────────────────────────────────────────────────────
    println()
    println("=== Running Experiment B: Full Pipeline ===")
    println()

    val resultsB = mutableListOf<PromptResultB>()

    for ((index, rA) in resultsA.withIndex()) {
        val ep = EVAL_DATASET[index]

        // If translator already succeeded, carry forward
        if (rA.success) {
            resultsB.add(PromptResultB(
                promptId = ep.id, category = ep.category, prompt = ep.prompt,
                success = true, resolvedBy = "translator",
                translatorAttempts = rA.attempts, finalDsl = rA.finalDsl,
                durationMs = rA.durationMs
            ))
            println("[${index + 1}/${EVAL_DATASET.size}] #${ep.id} -> translator OK")
            continue
        }

        // Step 2: optimizer (LLM repair)
        print("[${index + 1}/${EVAL_DATASET.size}] #${ep.id} -> translator FAIL -> optimizer ... ")
        val t0 = System.currentTimeMillis()

        val repaired = attemptRepair(llmService, rascalService, ep.prompt, rA.error ?: "Parse error")

        if (repaired != null) {
            val elapsed = System.currentTimeMillis() - t0
            resultsB.add(PromptResultB(
                promptId = ep.id, category = ep.category, prompt = ep.prompt,
                success = true, resolvedBy = "optimizer",
                translatorAttempts = rA.attempts, finalDsl = repaired,
                durationMs = rA.durationMs + elapsed
            ))
            println("OK")
            delay(1_000)
            continue
        }

        // Step 3: Rascal native fallback
        print("FAIL -> fallback ... ")
        delay(1_000)

        val params = extractParamsFromPrompt(ep.prompt)
        val fallback = rascalService.generate(params.goal, params.distance, params.styles, params.duration)
        val elapsed = System.currentTimeMillis() - t0

        if (fallback.success && fallback.code != null) {
            resultsB.add(PromptResultB(
                promptId = ep.id, category = ep.category, prompt = ep.prompt,
                success = true, resolvedBy = "fallback",
                translatorAttempts = rA.attempts, finalDsl = fallback.code,
                durationMs = rA.durationMs + elapsed
            ))
            println("OK")
        } else {
            resultsB.add(PromptResultB(
                promptId = ep.id, category = ep.category, prompt = ep.prompt,
                success = false, resolvedBy = "none",
                translatorAttempts = rA.attempts, error = fallback.error,
                durationMs = rA.durationMs + elapsed
            ))
            println("FAIL")
        }

        if (index < EVAL_DATASET.size - 1) delay(1_000)
    }

    // ───────────────────────────────────────────────────────────
    //  Results
    // ───────────────────────────────────────────────────────────
    val metricsA = computeMetricsA(resultsA)
    val metricsB = computeMetricsB(resultsB, metricsA)

    printResultsA(metricsA)
    printResultsB(metricsB)
    printRepresentativeExamples(resultsA, resultsB)

    val outputDir = File("evaluation_output")
    exportResults(outputDir, resultsA, resultsB, metricsA, metricsB)
    println()
    println("Results exported to evaluation_output/")
}
