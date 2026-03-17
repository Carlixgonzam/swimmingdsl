package swimming.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import swimming.model.AnalysisResult
import swimming.model.UserProfile
import swimming.ui.theme.*

// Chart palette — hardcoded hex (physical scene)
private val BarHigh        = Color(0xFF06A77D)
private val BarMid         = Color(0xFF48CAE4)
private val BarLow         = Color(0xFF0096C7)
private val StyleFreestyle   = Color(0xFF00B4D8)
private val StyleBackstroke  = Color(0xFF48CAE4)
private val StyleBreaststroke= Color(0xFF0077B6)
private val StyleButterfly   = Color(0xFFF4A261)
private val IntensityEasyBg      = Color(0xFFE0F7EE)
private val IntensityEasyStroke  = Color(0xFF06A77D)
private val IntensityModerateBg     = Color(0xFFFFF3E0)
private val IntensityModerateStroke = Color(0xFFF4A261)
private val IntensityHardBg    = Color(0xFFFDE8E8)
private val IntensityHardStroke= Color(0xFFE05252)
private val WeekGreen = Color(0xFF06A77D)

private val STYLE_COLORS = mapOf(
    "freestyle"    to StyleFreestyle,
    "backstroke"   to StyleBackstroke,
    "breaststroke" to StyleBreaststroke,
    "butterfly"    to StyleButterfly
)

@Composable
fun DashboardPanel(
    sessionHistory: List<AnalysisResult>,
    profile: UserProfile,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Tu progreso", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = LaneDark)
            Text(
                "${profile.level.replaceFirstChar { it.uppercase() }} · ${profile.availableMinutes} min/sesión",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = LaneDark,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Primary.copy(alpha = 0.1f))
                    .padding(vertical = 4.dp, horizontal = 10.dp)
            )
        }

        // ← CAMBIO: RelayRaceAnimation como tira decorativa (4 nadadores en carriles)
        RelayRaceAnimation(
            message = "",
            modifier = Modifier.fillMaxWidth().height(90.dp)
        )

        if (sessionHistory.isEmpty()) {
            // ← CAMBIO: DivingAnimation en empty state
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DivingAnimation(
                    message = "Analiza tu primera sesión para ver estadísticas"
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("No hay datos aún", color = MutedText, fontSize = 13.sp)
            }
        } else {
            MetricsRow(sessionHistory)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ChartCard("Distancia por sesión (m)", Modifier.weight(1f)) {
                    DistanceBarChart(sessionHistory)
                }
                ChartCard("Distribución de estilos", Modifier.weight(1f)) {
                    StyleDonutChart(sessionHistory)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ChartCard("Intensidad por sesión", Modifier.weight(1f)) {
                    IntensityTimeline(sessionHistory)
                }
                ChartCard("Progreso semanal (km)", Modifier.weight(1f)) {
                    WeeklyProgressBars(sessionHistory)
                }
            }
        }
    }
}

@Composable
private fun MetricsRow(history: List<AnalysisResult>) {
    val totalKm = history.sumOf { it.totalDistance } / 1000.0
    val totalSeconds = history.sumOf { it.time.totalSeconds }
    val hours = totalSeconds / 3600
    val mins = (totalSeconds % 3600) / 60
    val avgDist = if (history.isEmpty()) 0 else history.sumOf { it.totalDistance } / history.size

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MetricCard("${history.size}", "sesiones", Modifier.weight(1f))
        MetricCard("${"%.1f".format(totalKm)}", "km totales", Modifier.weight(1f))
        MetricCard("$hours:${mins.toString().padStart(2, '0')}", "tiempo total", Modifier.weight(1f))
        MetricCard("$avgDist", "m promedio", Modifier.weight(1f))
    }
}

@Composable
private fun MetricCard(value: String, label: String, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Background)
            .border(0.5.dp, BorderLight, RoundedCornerShape(10.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = LaneDark)
        Text(label, fontSize = 10.sp, color = MutedText, fontFamily = MonospaceFont)
    }
}

@Composable
private fun ChartCard(title: String, modifier: Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Background)
            .border(0.5.dp, BorderLight, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = LaneDark)
        content()
    }
}

@Composable
private fun DistanceBarChart(history: List<AnalysisResult>) {
    val data = history.takeLast(12)
    val maxDist = data.maxOfOrNull { it.totalDistance }?.toFloat() ?: 1f

    val animatedBars = data.mapIndexed { index, result ->
        val fraction = result.totalDistance / maxDist
        key(history.size) {
            animateFloatAsState(
                targetValue = fraction,
                animationSpec = tween(600, delayMillis = index * 50)
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxWidth().height(100.dp)) {
        val count = data.size
        if (count == 0) return@Canvas
        val gap = 4.dp.toPx()
        val barWidth = (size.width - gap * (count - 1)) / count
        val maxH = size.height - 16.dp.toPx()

        data.forEachIndexed { i, result ->
            val fraction = animatedBars[i].value
            val barH = fraction * maxH
            val x = i * (barWidth + gap)
            val color = when {
                result.totalDistance >= maxDist * 0.8f -> BarHigh
                result.totalDistance >= maxDist * 0.6f -> BarMid
                else -> BarLow
            }
            drawRoundRect(
                color = color,
                topLeft = Offset(x, maxH - barH),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
            )
            drawContext.canvas.nativeCanvas.apply {
                val paint = org.jetbrains.skia.Paint().apply {
                    this.color = org.jetbrains.skia.Color.makeARGB(153, 90, 143, 160)
                }
                val font = org.jetbrains.skia.Font(null, 8.dp.toPx())
                val label = "S${i + 1}"
                val textWidth = font.measureTextWidth(label)
                drawString(label, x + (barWidth - textWidth) / 2, size.height, font, paint)
            }
        }
    }
}

@Composable
private fun StyleDonutChart(history: List<AnalysisResult>) {
    val aggregated = mutableMapOf<String, Int>()
    history.forEach { result ->
        result.styles.forEach { (style, count) ->
            aggregated[style] = (aggregated[style] ?: 0) + count
        }
    }
    val total = aggregated.values.sum().toFloat().coerceAtLeast(1f)
    val entries = aggregated.entries.sortedByDescending { it.value }

    val animatedSweeps = entries.mapIndexed { index, entry ->
        val target = (entry.value / total) * 360f
        key(history.size) {
            animateFloatAsState(
                targetValue = target,
                animationSpec = tween(800, delayMillis = index * 100)
            )
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(90.dp)) {
            val strokeW = 14.dp.toPx()
            val arcSize = size.width - strokeW
            var startAngle = -90f
            entries.forEachIndexed { i, entry ->
                val color = STYLE_COLORS[entry.key] ?: BarMid
                val sweep = animatedSweeps[i].value
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(strokeW / 2, strokeW / 2),
                    size = Size(arcSize, arcSize),
                    style = Stroke(width = strokeW)
                )
                startAngle += sweep
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            entries.forEach { entry ->
                val pct = ((entry.value / total) * 100).toInt()
                val color = STYLE_COLORS[entry.key] ?: BarMid
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "${entry.key.replaceFirstChar { it.uppercase() }} $pct%",
                        fontSize = 10.sp, color = MutedText, fontFamily = MonospaceFont
                    )
                }
            }
        }
    }
}

@Composable
private fun IntensityTimeline(history: List<AnalysisResult>) {
    val data = history.takeLast(12)

    Canvas(modifier = Modifier.fillMaxWidth().height(80.dp)) {
        val count = data.size
        if (count == 0) return@Canvas
        val gap = 4.dp.toPx()
        val barWidth = (size.width - gap * (count - 1)) / count
        val maxH = size.height - 16.dp.toPx()

        data.forEachIndexed { i, result ->
            val easy     = result.intensities["easy"]     ?: 0
            val moderate = result.intensities["moderate"] ?: 0
            val hard     = result.intensities["hard"]     ?: 0
            val totalI   = (easy + moderate + hard).coerceAtLeast(1)
            val hardPct  = hard.toFloat() / totalI
            val dominant = when {
                hard >= moderate && hard >= easy -> "hard"
                moderate >= easy -> "moderate"
                else -> "easy"
            }
            val (bgColor, strokeColor) = when (dominant) {
                "hard"     -> IntensityHardBg to IntensityHardStroke
                "moderate" -> IntensityModerateBg to IntensityModerateStroke
                else       -> IntensityEasyBg to IntensityEasyStroke
            }
            val barH = (0.3f + hardPct * 0.7f) * maxH
            val x = i * (barWidth + gap)
            drawRoundRect(bgColor, Offset(x, maxH - barH), Size(barWidth, barH),
                CornerRadius(3.dp.toPx()))
            drawRoundRect(strokeColor, Offset(x, maxH - barH), Size(barWidth, barH),
                CornerRadius(3.dp.toPx()), style = Stroke(1.dp.toPx()))
            drawContext.canvas.nativeCanvas.apply {
                val paint = org.jetbrains.skia.Paint().apply {
                    this.color = org.jetbrains.skia.Color.makeARGB(153, 90, 143, 160)
                }
                val font = org.jetbrains.skia.Font(null, 8.dp.toPx())
                val label = "S${i + 1}"
                val tw = font.measureTextWidth(label)
                drawString(label, x + (barWidth - tw) / 2, size.height, font, paint)
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 4.dp)) {
        LegendDot(IntensityEasyStroke, "Easy")
        LegendDot(IntensityModerateStroke, "Moderate")
        LegendDot(IntensityHardStroke, "Hard")
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 9.sp, color = MutedText, fontFamily = MonospaceFont)
    }
}

@Composable
private fun WeeklyProgressBars(history: List<AnalysisResult>) {
    val sessionsPerWeek = 3
    val weeks = history.chunked(sessionsPerWeek)
    val weekKms = weeks.map { week -> week.sumOf { it.totalDistance } / 1000.0 }
    val maxKm = weekKms.maxOrNull()?.toFloat()?.coerceAtLeast(0.1f) ?: 0.1f

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        weekKms.forEachIndexed { index, km ->
            val isLast = index == weekKms.lastIndex
            val fraction by key(history.size) {
                animateFloatAsState(
                    targetValue = (km.toFloat() / maxKm).coerceIn(0f, 1f),
                    animationSpec = tween(600, delayMillis = index * 100)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Semana ${index + 1}",
                    fontSize = 10.sp, color = MutedText, fontFamily = MonospaceFont,
                    modifier = Modifier.width(64.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f).height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(BorderLight)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isLast) WeekGreen else Primary)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "${"%.1f".format(km)} km",
                    fontSize = 10.sp, color = LaneDark, fontFamily = MonospaceFont,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.width(48.dp)
                )
            }
        }
    }
}