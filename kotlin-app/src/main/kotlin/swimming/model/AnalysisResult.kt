package swimming.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
data class RestInfo(
    val periods: Int = 0,
    val totalSeconds: Int = 0,
    val average: Int = 0
) {
    val totalFormatted: String get() = formatTime(totalSeconds)
}

@Serializable
data class TimeInfo(
    val swimSeconds: Int = 0,
    val restSeconds: Int = 0,
    val totalSeconds: Int = 0
) {
    val swimFormatted: String get() = formatTime(swimSeconds)
    val restFormatted: String get() = formatTime(restSeconds)
    val totalFormatted: String get() = formatTime(totalSeconds)
}

@Serializable
data class AnalysisResult(
    val success: Boolean = false,
    val error: String? = null,
    val sessionCount: Int = 0,
    val sessionNames: List<String> = emptyList(),
    val totalDistance: Int = 0,
    val distanceKm: Double = 0.0,
    val styles: Map<String, Int> = emptyMap(),
    val intensities: Map<String, Int> = emptyMap(),
    val equipment: Map<String, Int> = emptyMap(),
    val drills: Map<String, Int> = emptyMap(),
    val rest: RestInfo = RestInfo(),
    val time: TimeInfo = TimeInfo()
)

@Serializable
data class GenerateResult(
    val success: Boolean = false,
    val error: String? = null,
    val code: String? = null,
    val goal: String? = null,
    val distance: Int = 0
)

fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "$mins:${secs.toString().padStart(2, '0')}"
}
