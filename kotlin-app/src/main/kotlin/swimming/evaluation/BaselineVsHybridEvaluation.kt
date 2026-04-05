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

data class CompPrompt(val id: Int, val category: String, val prompt: String)

@Serializable
data class BaselineResult(
    val promptId: Int,
    val category: String,
    val prompt: String,
    val valid: Boolean,
    val dslCode: String? = null,
    val error: String? = null,
    val latencyMs: Long = 0
)

@Serializable
data class HybridResult(
    val promptId: Int,
    val category: String,
    val prompt: String,
    val valid: Boolean,
    val attempts: Int,
    val dslCode: String? = null,
    val error: String? = null,
    val latencyMs: Long = 0
)

@Serializable
data class PerPromptRow(
    val promptId: Int,
    val category: String,
    val prompt: String,
    val baselineValid: Boolean,
    val hybridValid: Boolean,
    val hybridAttempts: Int,
    val baselineLatencyMs: Long,
    val hybridLatencyMs: Long
)

@Serializable
data class SummaryMetrics(
    val totalPrompts: Int,
    val validCount: Int,
    val validityRate: Double,
    val failureRate: Double,
    val avgLatencyMs: Double,
    val avgAttempts: Double = 1.0
)

@Serializable
data class CategoryBreakdown(
    val category: String,
    val baselineValidityRate: Double,
    val hybridValidityRate: Double,
    val hybridAvgAttempts: Double
)

@Serializable
data class ComparisonOutput(
    val totalPrompts: Int,
    val baseline: SummaryMetrics,
    val hybrid: SummaryMetrics,
    val byCategory: List<CategoryBreakdown>,
    val perPrompt: List<PerPromptRow>
)

// ═══════════════════════════════════════════════════════════════
//  System Prompt (identical to DSLTranslatorAgent.SYSTEM_PROMPT)
//  Duplicated here because the original is private.
// ═══════════════════════════════════════════════════════════════

private val DSL_SYSTEM_PROMPT = """
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

// ═══════════════════════════════════════════════════════════════
//  Dataset — 30 prompts across 3 complexity tiers
// ═══════════════════════════════════════════════════════════════

val COMPARISON_DATASET = listOf(
    // ── Simple (10): single exercise, basic parameters ─────────
    CompPrompt(1, "simple", "Create a 500 meter freestyle easy session"),
    CompPrompt(2, "simple", "I want to swim 400 meters backstroke at moderate intensity"),
    CompPrompt(3, "simple", "Make a 1000 meter breaststroke session"),
    CompPrompt(4, "simple", "Generate a 200 meter butterfly sprint at hard intensity"),
    CompPrompt(5, "simple", "I need a 600 meter easy kick workout"),
    CompPrompt(6, "simple", "Create a 1000 meter freestyle session at moderate pace of 110"),
    CompPrompt(7, "simple", "Build a 300 meter backstroke easy session"),
    CompPrompt(8, "simple", "I want a 500 meter hard freestyle swim with pace 80"),
    CompPrompt(9, "simple", "Make a 800 meter easy endurance freestyle session"),
    CompPrompt(10, "simple", "Generate a 400 meter moderate breaststroke workout"),

    // ── Medium (10): structured sessions, intervals ────────────
    CompPrompt(11, "medium",
        "Create a 1500 meter session with a 400m easy freestyle warmup, " +
        "8x100m freestyle hard intervals with 15 second rest, and a 200m easy cooldown"),
    CompPrompt(12, "medium",
        "I want interval training with 6 times 100 meter freestyle hard with 20 seconds rest, " +
        "followed by 200m easy backstroke"),
    CompPrompt(13, "medium",
        "Build a structured session with warmup, main set of mixed freestyle " +
        "and backstroke intervals, and cooldown totaling 2000 meters"),
    CompPrompt(14, "medium",
        "Design a pyramid workout: swim 50m, 100m, 200m, 100m, 50m all freestyle moderate"),
    CompPrompt(15, "medium",
        "Make a training session with 400m freestyle easy warmup, sprint intervals " +
        "in the main set, and 300m easy cooldown"),
    CompPrompt(16, "medium",
        "I need a session with 4x200m backstroke moderate with 30 seconds rest " +
        "and 4x100m freestyle hard with 15 seconds rest"),
    CompPrompt(17, "medium",
        "Create a session with 500m easy freestyle warmup, " +
        "10x50m hard freestyle with 10 seconds rest, and 200m easy cooldown"),
    CompPrompt(18, "medium",
        "Build a progressive distance workout: 4x50m then 4x100m then 4x200m " +
        "freestyle with 20 second rest between each set"),
    CompPrompt(19, "medium",
        "Design a session with warmup 300m easy freestyle, " +
        "kick intervals 6x50m hard with 15 second rest, and 200m easy cooldown"),
    CompPrompt(20, "medium",
        "Make a 1 hour endurance session in freestyle with 400m warmup, " +
        "long main set of continuous swimming, and 300m cooldown"),

    // ── Complex (10): equipment, drills, targets, mixed ────────
    CompPrompt(21, "complex",
        "Create a technique session: warmup 400m easy freestyle, " +
        "main with drill catchup 200m, drill fingertip 200m, " +
        "4x50m drill sculling with 20 second rest, cooldown 200m easy freestyle"),
    CompPrompt(22, "complex",
        "Build a session using fins for 300m easy warmup, " +
        "paddles for 6x100m freestyle hard with 15 second rest, " +
        "and pullbuoy for 200m easy cooldown"),
    CompPrompt(23, "complex",
        "I want a mixed session: 400m warmup easy freestyle, " +
        "4x100m freestyle hard rest 15s, 4x50m kick hard with board rest 10s, " +
        "drill onesided 200m moderate, 200m cooldown easy freestyle"),
    CompPrompt(24, "complex",
        "Create a pyramid: 50m easy, 100m moderate, 200m hard, " +
        "200m hard, 100m moderate, 50m easy all freestyle with pace and 20 second rest"),
    CompPrompt(25, "complex",
        "Build a 2500m competition prep session with structured warmup, " +
        "main set of butterfly and freestyle intervals, kick sets, " +
        "and drill work in the cooldown"),
    CompPrompt(26, "complex",
        "Design a recovery session using snorkel for 400m easy warmup, " +
        "pullbuoy for 4x100m easy backstroke rest 30s, " +
        "and fins for 200m easy cooldown"),
    CompPrompt(27, "complex",
        "Create a technique session with five drill types: " +
        "catchup, onesided, fingertip, sixKick, sculling each 100m easy, " +
        "with 400m freestyle warmup and 200m backstroke cooldown"),
    CompPrompt(28, "complex",
        "Build an advanced interval session: warmup 400m easy freestyle, " +
        "main 8x100m freestyle hard pace 70 target 1:10 rest 15s with paddles, " +
        "then 4x50m butterfly hard rest 20s, cooldown 300m easy with fins"),
    CompPrompt(29, "complex",
        "Design a multi-component session: warmup 200m easy freestyle and " +
        "200m kick with board, main with 6x100m breaststroke moderate pace 100 " +
        "rest 20s and drill sculling 4x50m rest 15s, cooldown 200m easy with snorkel"),
    CompPrompt(30, "complex",
        "Create a full 3000m session: warmup 400m easy swim and 200m drill catchup, " +
        "main 8x100m freestyle hard pace 75 rest 15s and 4x50m butterfly moderate " +
        "rest 20s, cooldown 200m easy backstroke with fins")
)

// ═══════════════════════════════════════════════════════════════
//  Phase 1 — Baseline: single LLM call, no retry
// ═══════════════════════════════════════════════════════════════

suspend fun runBaseline(
    llmService: LLMService,
    rascalService: RascalService,
    ep: CompPrompt
): BaselineResult {
    val t0 = System.currentTimeMillis()

    val dslCode = try {
        val response = llmService.chat(
            DSL_SYSTEM_PROMPT,
            listOf(ChatMessage("user", ep.prompt)),
            temperature = 0.2
        )
        response.trim()
            .removePrefix("```swim").removePrefix("```").removeSuffix("```").trim()
    } catch (e: Exception) {
        return BaselineResult(
            promptId = ep.id, category = ep.category, prompt = ep.prompt,
            valid = false, error = "LLM error: ${e.message}",
            latencyMs = System.currentTimeMillis() - t0
        )
    }

    val analysis = rascalService.analyze(dslCode)
    val elapsed = System.currentTimeMillis() - t0

    return BaselineResult(
        promptId = ep.id, category = ep.category, prompt = ep.prompt,
        valid = analysis.success,
        dslCode = dslCode,
        error = if (!analysis.success) analysis.error else null,
        latencyMs = elapsed
    )
}

// ═══════════════════════════════════════════════════════════════
//  Phase 2 — Hybrid: validation loop via DSLTranslatorAgent
// ═══════════════════════════════════════════════════════════════

suspend fun runHybrid(
    translator: DSLTranslatorAgent,
    ep: CompPrompt
): HybridResult {
    val t0 = System.currentTimeMillis()
    val result = translator.translate(ep.prompt)
    val elapsed = System.currentTimeMillis() - t0

    return HybridResult(
        promptId = ep.id, category = ep.category, prompt = ep.prompt,
        valid = result.success,
        attempts = result.attempts,
        dslCode = result.dslCode,
        error = result.error,
        latencyMs = elapsed
    )
}

// ═══════════════════════════════════════════════════════════════
//  Metrics
// ═══════════════════════════════════════════════════════════════

fun computeBaselineMetrics(results: List<BaselineResult>): SummaryMetrics {
    val n = results.size
    val valid = results.count { it.valid }
    return SummaryMetrics(
        totalPrompts = n, validCount = valid,
        validityRate = pct(valid, n),
        failureRate = pct(n - valid, n),
        avgLatencyMs = results.map { it.latencyMs.toDouble() }.average()
    )
}

fun computeHybridMetrics(results: List<HybridResult>): SummaryMetrics {
    val n = results.size
    val valid = results.count { it.valid }
    return SummaryMetrics(
        totalPrompts = n, validCount = valid,
        validityRate = pct(valid, n),
        failureRate = pct(n - valid, n),
        avgLatencyMs = results.map { it.latencyMs.toDouble() }.average(),
        avgAttempts = results.map { it.attempts.toDouble() }.average()
    )
}

fun buildCategoryBreakdown(
    baseline: List<BaselineResult>,
    hybrid: List<HybridResult>
): List<CategoryBreakdown> {
    val categories = listOf("simple", "medium", "complex")
    return categories.map { cat ->
        val bCat = baseline.filter { it.category == cat }
        val hCat = hybrid.filter { it.category == cat }
        CategoryBreakdown(
            category = cat,
            baselineValidityRate = pct(bCat.count { it.valid }, bCat.size),
            hybridValidityRate = pct(hCat.count { it.valid }, hCat.size),
            hybridAvgAttempts = if (hCat.isNotEmpty())
                hCat.map { it.attempts.toDouble() }.average() else 0.0
        )
    }
}

fun buildPerPrompt(
    baseline: List<BaselineResult>,
    hybrid: List<HybridResult>
): List<PerPromptRow> = baseline.zip(hybrid).map { (b, h) ->
    PerPromptRow(
        promptId = b.promptId, category = b.category, prompt = b.prompt,
        baselineValid = b.valid, hybridValid = h.valid,
        hybridAttempts = h.attempts,
        baselineLatencyMs = b.latencyMs, hybridLatencyMs = h.latencyMs
    )
}

private fun pct(n: Int, d: Int): Double =
    if (d > 0) n.toDouble() / d * 100.0 else 0.0

// ═══════════════════════════════════════════════════════════════
//  Console Output
// ═══════════════════════════════════════════════════════════════

fun printComparisonTable(bm: SummaryMetrics, hm: SummaryMetrics) {
    val w1 = 22; val w2 = 14; val w3 = 14
    val sep = "+" + "-".repeat(w1) + "+" + "-".repeat(w2) + "+" + "-".repeat(w3) + "+"
    println()
    println(sep)
    println("| %-${w1 - 2}s | %-${w2 - 2}s | %-${w3 - 2}s |".format("Metric", "LLM-only", "Hybrid"))
    println(sep)
    println("| %-${w1 - 2}s | %${w2 - 3}s%% | %${w3 - 3}s%% |".format(
        "Validity Rate", "%.1f".format(bm.validityRate), "%.1f".format(hm.validityRate)))
    println("| %-${w1 - 2}s | %${w2 - 3}s%% | %${w3 - 3}s%% |".format(
        "Failure Rate", "%.1f".format(bm.failureRate), "%.1f".format(hm.failureRate)))
    println("| %-${w1 - 2}s | %${w2 - 2}s | %${w3 - 2}s |".format(
        "Avg Attempts", "%.1f".format(bm.avgAttempts), "%.2f".format(hm.avgAttempts)))
    println("| %-${w1 - 2}s | %${w2 - 2}s | %${w3 - 2}s |".format(
        "Avg Latency (ms)", "%.0f".format(bm.avgLatencyMs), "%.0f".format(hm.avgLatencyMs)))
    println(sep)

    val improvement = hm.validityRate - bm.validityRate
    println()
    println("Improvement (Hybrid over Baseline): %+.1f pp".format(improvement))
    val corrected = hm.validCount - bm.validCount
    if (corrected > 0) {
        println("Programs corrected by retry loop: $corrected / ${bm.totalPrompts}")
    }
}

fun printCategoryTable(categories: List<CategoryBreakdown>) {
    println()
    val sep = "+----------+----------------+----------------+----------------+"
    println(sep)
    println("| %-8s | %-14s | %-14s | %-14s |".format(
        "Category", "Baseline (%)", "Hybrid (%)", "Hybrid Avg Att"))
    println(sep)
    for (c in categories) {
        println("| %-8s | %13.1f%% | %13.1f%% | %14.2f |".format(
            c.category, c.baselineValidityRate, c.hybridValidityRate, c.hybridAvgAttempts))
    }
    println(sep)
}

fun printPerPromptTable(rows: List<PerPromptRow>) {
    println()
    val sep = "+----+----------+----------+----------+---------+----------+----------+"
    println(sep)
    println("| %2s | %-8s | %-8s | %-8s | %-7s | %-8s | %-8s |".format(
        "#", "Category", "Baseline", "Hybrid", "Attempt", "Lat(B)ms", "Lat(H)ms"))
    println(sep)
    for (r in rows) {
        println("| %2d | %-8s | %-8s | %-8s | %4d    | %8d | %8d |".format(
            r.promptId,
            r.category,
            if (r.baselineValid) "VALID" else "INVALID",
            if (r.hybridValid) "VALID" else "INVALID",
            r.hybridAttempts,
            r.baselineLatencyMs,
            r.hybridLatencyMs
        ))
    }
    println(sep)
}

// ═══════════════════════════════════════════════════════════════
//  Export
// ═══════════════════════════════════════════════════════════════

fun exportComparison(
    outputDir: File,
    bm: SummaryMetrics,
    hm: SummaryMetrics,
    categories: List<CategoryBreakdown>,
    perPrompt: List<PerPromptRow>
) {
    val json = Json { prettyPrint = true; encodeDefaults = true }
    outputDir.mkdirs()

    val output = ComparisonOutput(
        totalPrompts = bm.totalPrompts,
        baseline = bm, hybrid = hm,
        byCategory = categories, perPrompt = perPrompt
    )
    File(outputDir, "comparison_results.json").writeText(json.encodeToString(output))

    File(outputDir, "comparison_per_prompt.csv").writeText(buildString {
        appendLine("prompt_id,category,baseline_valid,hybrid_valid,hybrid_attempts,baseline_latency_ms,hybrid_latency_ms")
        for (r in perPrompt) {
            appendLine("${r.promptId},${r.category},${r.baselineValid},${r.hybridValid},${r.hybridAttempts},${r.baselineLatencyMs},${r.hybridLatencyMs}")
        }
    })
}

// ═══════════════════════════════════════════════════════════════
//  Main
// ═══════════════════════════════════════════════════════════════

fun main() = runBlocking {
    val n = COMPARISON_DATASET.size
    println("╔══════════════════════════════════════════════════════════╗")
    println("║     Baseline vs Hybrid — Comparative Evaluation        ║")
    println("║     $n prompts · 2 approaches                          ║")
    println("╚══════════════════════════════════════════════════════════╝")
    println()

    val llmService = LLMService()
    val rascalService = RascalService()
    val translator = DSLTranslatorAgent(llmService, rascalService)

    // ── Phase 1: Baseline (LLM-only, single generation) ───────
    println("=== Phase 1: Baseline (single LLM call, no retries) ===")
    println()

    val baselineResults = mutableListOf<BaselineResult>()
    for ((i, ep) in COMPARISON_DATASET.withIndex()) {
        print("[${i + 1}/$n] #${ep.id} (${ep.category}) ... ")
        val result = runBaseline(llmService, rascalService, ep)
        baselineResults.add(result)
        val tag = if (result.valid) "VALID" else "INVALID"
        println("$tag  (${result.latencyMs}ms)")
        if (i < n - 1) delay(1_000)
    }

    // ── Phase 2: Hybrid (validation loop, max 3 attempts) ─────
    println()
    println("=== Phase 2: Hybrid (validate + retry, max 3 attempts) ===")
    println()

    val hybridResults = mutableListOf<HybridResult>()
    for ((i, ep) in COMPARISON_DATASET.withIndex()) {
        print("[${i + 1}/$n] #${ep.id} (${ep.category}) ... ")
        val result = runHybrid(translator, ep)
        hybridResults.add(result)
        val tag = if (result.valid) "VALID" else "INVALID"
        val att = if (result.attempts > 1) "${result.attempts} attempts" else "1 attempt"
        println("$tag  ($att, ${result.latencyMs}ms)")
        if (i < n - 1) delay(1_000)
    }

    // ── Compute & Display ─────────────────────────────────────
    val bMetrics = computeBaselineMetrics(baselineResults)
    val hMetrics = computeHybridMetrics(hybridResults)
    val categories = buildCategoryBreakdown(baselineResults, hybridResults)
    val perPrompt = buildPerPrompt(baselineResults, hybridResults)

    printComparisonTable(bMetrics, hMetrics)
    printCategoryTable(categories)
    printPerPromptTable(perPrompt)

    // ── Export ─────────────────────────────────────────────────
    val outputDir = File("evaluation_output")
    exportComparison(outputDir, bMetrics, hMetrics, categories, perPrompt)
    println()
    println("Results exported to evaluation_output/")
}
