package swimming.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import swimming.ui.theme.MonospaceFont
import swimming.ui.theme.MutedText
import kotlin.math.*

// ── Pool colors — hardcoded, physical scene ──────────────────────────────
private val PoolFloor   = Color(0xFF0077B6)
private val PoolEdge    = Color(0xFF023E8A)
private val PoolLane    = Color(0xFFFFFFFF).copy(alpha = 0.22f)
private val TileColor   = Color(0xFF00B4D8).copy(alpha = 0.12f)
private val Skin        = Color(0xFFF4C48B)
private val Suit        = Color(0xFF0A3D54)
private val CapOrange   = Color(0xFFFF6B35)
private val Splash      = Color(0xFFADE8F4).copy(alpha = 0.70f)

// ── Shared top-down swimmer ───────────────────────────────────────────────
private fun DrawScope.drawSwimmerTopDown(
    cx: Float, cy: Float,
    strokeAngle: Float, splashAlpha: Float,
    capColor: Color = CapOrange
) {
    val bw = 26.dp.toPx(); val bh = 10.dp.toPx()
    // Splash trail
    for (i in 4 downTo 0) {
        val tx = cx - i * 11.dp.toPx()
        if (tx < 0f || tx > size.width) continue
        val a = (1f - i / 5f) * splashAlpha * 0.55f
        drawCircle(Splash.copy(alpha = a), radius = (3f - i * 0.4f).dp.toPx(), center = Offset(tx, cy))
    }
    // Body
    drawRoundRect(Suit, topLeft = Offset(cx - bw / 2, cy - bh / 2),
        size = Size(bw, bh), cornerRadius = CornerRadius(4.dp.toPx()))
    // Cap
    drawCircle(capColor, radius = 5.dp.toPx(), center = Offset(cx + bw / 2 - 2.dp.toPx(), cy))
    // Goggles
    drawCircle(Color.White.copy(0.85f), 1.5.dp.toPx(), Offset(cx + bw / 2, cy - 2.dp.toPx()))
    drawCircle(Color.White.copy(0.85f), 1.5.dp.toPx(), Offset(cx + bw / 2, cy + 2.dp.toPx()))
    // Arms
    fun arm(angle: Float) {
        val rad = Math.toRadians(angle.toDouble())
        val ax = cx + 3.dp.toPx() + cos(rad).toFloat() * 14.dp.toPx()
        val ay = cy + sin(rad).toFloat() * 7.dp.toPx()
        drawLine(Skin, Offset(cx + 3.dp.toPx(), cy), Offset(ax, ay),
            strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
        if (angle % 360f < 60f || angle % 360f > 300f)
            drawCircle(Splash.copy(alpha = splashAlpha * 0.8f), 2.5.dp.toPx(), Offset(ax, ay))
    }
    arm(strokeAngle); arm(strokeAngle + 180f)
    // Flutter kick
    val kick = sin(Math.toRadians((strokeAngle * 3).toDouble())).toFloat() * 3.dp.toPx()
    drawRoundRect(Suit.copy(0.8f),
        topLeft = Offset(cx - bw / 2 - 10.dp.toPx(), cy - 2.5.dp.toPx() + kick),
        size = Size(9.dp.toPx(), 2.5.dp.toPx()), cornerRadius = CornerRadius(1.dp.toPx()))
    drawRoundRect(Suit.copy(0.8f),
        topLeft = Offset(cx - bw / 2 - 10.dp.toPx(), cy + kick),
        size = Size(9.dp.toPx(), 2.5.dp.toPx()), cornerRadius = CornerRadius(1.dp.toPx()))
}

// ── Shared pool background ────────────────────────────────────────────────
private fun DrawScope.drawPoolBackground(shimOffset: Float) {
    drawRect(PoolFloor)
    val ts = 18.dp.toPx()
    val style = Stroke(0.5f)
    var x = -(shimOffset % ts)
    while (x < size.width) {
        drawLine(TileColor, Offset(x, 0f), Offset(x, size.height))
        x += ts
    }
    var y = 0f
    while (y < size.height) {
        drawLine(TileColor, Offset(0f, y), Offset(size.width, y))
        y += ts
    }
}

private fun DrawScope.drawLaneRopes(laneCount: Int) {
    for (i in 1 until laneCount) {
        val y = size.height * i / laneCount
        var x = 0f
        while (x < size.width) {
            drawCircle(PoolLane, radius = 3.dp.toPx(), center = Offset(x, y))
            x += 12.dp.toPx()
        }
    }
    drawRect(PoolEdge, size = Size(size.width, 4.dp.toPx()))
    drawRect(PoolEdge, topLeft = Offset(0f, size.height - 4.dp.toPx()),
        size = Size(size.width, 4.dp.toPx()))
}

// ═══════════════════════════════════════════════════════════════════
// ANIMATION 1 — PoolLaneAnimation (top-down freestyle, 1 swimmer)
// ═══════════════════════════════════════════════════════════════════
@Composable
fun PoolLaneAnimation(
    message: String = "Procesando...",
    modifier: Modifier = Modifier
) {
    val inf = rememberInfiniteTransition()
    val swimX    by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(2800, easing = LinearEasing)))
    val stroke   by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(500, easing = LinearEasing)))
    val splashA  by inf.animateFloat(0.3f, 0.9f, infiniteRepeatable(tween(250), RepeatMode.Reverse))
    val shim     by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing)))

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Canvas(Modifier.fillMaxWidth().height(100.dp)) {
            drawPoolBackground(shim * 18.dp.toPx())
            drawLaneRopes(3)
            val sx = swimX * (size.width + 60.dp.toPx()) - 30.dp.toPx()
            val sy = size.height / 2f
            if (sx > -30.dp.toPx()) drawSwimmerTopDown(sx, sy, stroke, splashA)
        }
        if (message.isNotEmpty())
            Text(message, fontSize = 12.sp, fontFamily = MonospaceFont, color = MutedText)
    }
}

// Backward-compatible wrapper — all existing call sites keep working
@Composable
fun SwimmerLoadingAnimation(
    message: String = "Procesando...",
    modifier: Modifier = Modifier
) = PoolLaneAnimation(message, modifier)

// ═══════════════════════════════════════════════════════════════════
// ANIMATION 2 — UnderwaterAnimation (side profile, bubbles, rays)
// ═══════════════════════════════════════════════════════════════════
@Composable
fun UnderwaterAnimation(
    message: String = "Procesando...",
    modifier: Modifier = Modifier
) {
    val inf = rememberInfiniteTransition()
    val swimX   by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(3200, easing = LinearEasing)))
    val stroke  by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(520, easing = LinearEasing)))
    val shim    by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing)))

    // 8 bubbles with independent phases — use different initial values to stagger
    val bubblePhases = (0..7).map { i ->
        inf.animateFloat(
            initialValue = (i * 0.125f) % 1f,  // stagger start position 0..0.875
            targetValue = (i * 0.125f + 1f) % 2f,
            animationSpec = infiniteRepeatable(tween(1200 + i * 150, easing = LinearEasing))
        )
    }
    val bubblePhaseValues = bubblePhases.map { (it.value % 1f + 1f) % 1f }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Canvas(Modifier.fillMaxWidth().height(100.dp)) {
            val w = size.width; val h = size.height

            // Water background — two tones
            drawRect(Color(0xFF0096C7), size = Size(w, h * 0.5f))
            drawRect(Color(0xFF0077B6), topLeft = Offset(0f, h * 0.5f), size = Size(w, h * 0.5f))

            // Tile grid
            val ts = 18.dp.toPx()
            val so = shim * ts
            var tx = -(so % ts); while (tx < w) { drawLine(TileColor, Offset(tx,0f), Offset(tx,h)); tx+=ts }
            var ty = 0f; while (ty < h) { drawLine(TileColor, Offset(0f,ty), Offset(w,ty)); ty+=ts }

            // Light rays from top
            val rayStyle = Stroke(12.dp.toPx())
            for (i in 0..2) {
                val rx = w * (0.2f + i * 0.3f)
                drawLine(Color.White.copy(0.07f),
                    Offset(rx, 0f), Offset(rx + h * 0.26f, h),
                    strokeWidth = 12.dp.toPx(), cap = StrokeCap.Round)
            }

            // Side-profile swimmer
            val sx = swimX * (w + 60.dp.toPx()) - 30.dp.toPx()
            val sy = h * 0.42f
            val armAngle = sin(Math.toRadians(stroke.toDouble())).toFloat()

            // Body horizontal
            drawRoundRect(Suit, topLeft = Offset(sx - 15.dp.toPx(), sy - 5.dp.toPx()),
                size = Size(30.dp.toPx(), 10.dp.toPx()), cornerRadius = CornerRadius(5.dp.toPx()))
            // Head
            drawCircle(CapOrange, 7.dp.toPx(), Offset(sx + 16.dp.toPx(), sy))
            // Goggles
            drawRoundRect(Color.White.copy(0.8f),
                topLeft = Offset(sx + 19.dp.toPx(), sy - 1.5.dp.toPx()),
                size = Size(4.dp.toPx(), 3.dp.toPx()), cornerRadius = CornerRadius(1.dp.toPx()))

            // Arms (side view — one forward, one back)
            val fwdAngle = Math.toRadians((armAngle * 60 - 30).toDouble())
            val bkAngle  = Math.toRadians((armAngle * -60 + 150).toDouble())
            listOf(fwdAngle, bkAngle).forEach { ang ->
                val ax = sx + cos(ang).toFloat() * 16.dp.toPx()
                val ay = sy + sin(ang).toFloat() * 6.dp.toPx()
                drawLine(Skin, Offset(sx, sy - 2.dp.toPx()), Offset(ax, ay),
                    strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
            }

            // Flutter legs
            val legKick = sin(Math.toRadians((stroke * 3).toDouble())).toFloat() * 3.dp.toPx()
            drawRoundRect(Suit.copy(0.8f),
                topLeft = Offset(sx - 24.dp.toPx(), sy - 2.dp.toPx() + legKick),
                size = Size(11.dp.toPx(), 2.5.dp.toPx()), cornerRadius = CornerRadius(1.dp.toPx()))
            drawRoundRect(Suit.copy(0.8f),
                topLeft = Offset(sx - 24.dp.toPx(), sy + 1.dp.toPx() - legKick),
                size = Size(11.dp.toPx(), 2.5.dp.toPx()), cornerRadius = CornerRadius(1.dp.toPx()))

            // Bubbles
            val bubbleXPositions = listOf(0.1f,0.2f,0.35f,0.45f,0.55f,0.65f,0.75f,0.88f)
            val bubbleRadii      = listOf(3f,2f,4f,2.5f,3.5f,2f,4f,3f)
            bubblePhaseValues.forEachIndexed { i, phase ->
                val bx = w * bubbleXPositions[i] + sin(phase * 2 * PI).toFloat() * 4.dp.toPx()
                val by = h - phase * (h + 10.dp.toPx())
                if (by > -5f) {
                    val alpha = (0.3f + (1f - phase) * 0.4f).coerceIn(0f, 0.7f)
                    drawCircle(Color.White.copy(alpha), bubbleRadii[i].dp.toPx(), Offset(bx, by))
                }
            }

            // Edges
            drawRect(PoolEdge, size = Size(w, 4.dp.toPx()))
            drawRect(PoolEdge, topLeft = Offset(0f, h - 4.dp.toPx()), size = Size(w, 4.dp.toPx()))
        }
        if (message.isNotEmpty())
            Text(message, fontSize = 12.sp, fontFamily = MonospaceFont, color = MutedText)
    }
}

// ═══════════════════════════════════════════════════════════════════
// ANIMATION 3 — DivingAnimation (diver + splash cycle)
// ═══════════════════════════════════════════════════════════════════
@Composable
fun DivingAnimation(
    message: String = "Generando...",
    modifier: Modifier = Modifier
) {
    val inf = rememberInfiniteTransition()
    val cycle by inf.animateFloat(0f, 1f,
        infiniteRepeatable(tween(3000, easing = LinearEasing)))

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Canvas(Modifier.fillMaxWidth().height(140.dp)) {
            val w = size.width; val h = size.height
            val cx = w / 2f
            val waterY = h * 0.65f
            val frac = cycle.coerceIn(0f, 1f)

            // Pool water below waterY
            drawRect(Color(0xFF0096C7), topLeft = Offset(0f, waterY), size = Size(w, h - waterY))
            // Sky/deck above
            drawRect(Color(0xFFF0F7FA), size = Size(w, waterY))

            // Tile shimmer on water
            val ts = 18.dp.toPx()
            var tx = 0f; while (tx < w) { drawLine(TileColor, Offset(tx, waterY), Offset(tx, h)); tx += ts }

            // Diving board (always visible)
            drawRoundRect(Color(0xFF0A3D54),
                topLeft = Offset(cx - 35.dp.toPx(), 16.dp.toPx()),
                size = Size(70.dp.toPx(), 6.dp.toPx()), cornerRadius = CornerRadius(3.dp.toPx()))
            // Board support
            drawLine(Color(0xFF0A3D54).copy(0.5f),
                Offset(cx, 22.dp.toPx()), Offset(cx, 32.dp.toPx()), strokeWidth = 3.dp.toPx())

            when {
                // Phase A — standing on board (0–30%)
                frac < 0.30f -> {
                    val figY = 16.dp.toPx() - 2.dp.toPx()
                    drawCircle(CapOrange, 6.dp.toPx(), Offset(cx, figY - 14.dp.toPx()))
                    drawLine(Suit, Offset(cx, figY - 8.dp.toPx()), Offset(cx, figY - 26.dp.toPx() + 8.dp.toPx()),
                        strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
                    drawLine(Skin, Offset(cx, figY - 18.dp.toPx()), Offset(cx - 10.dp.toPx(), figY - 12.dp.toPx()),
                        strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    drawLine(Skin, Offset(cx, figY - 18.dp.toPx()), Offset(cx + 10.dp.toPx(), figY - 12.dp.toPx()),
                        strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    drawLine(Suit, Offset(cx, figY - 8.dp.toPx()), Offset(cx - 5.dp.toPx(), figY),
                        strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    drawLine(Suit, Offset(cx, figY - 8.dp.toPx()), Offset(cx + 5.dp.toPx(), figY),
                        strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
                }

                // Phase B — falling and rotating (30–60%)
                frac < 0.60f -> {
                    val t = (frac - 0.30f) / 0.30f
                    val startY = 16.dp.toPx()
                    val fallY = startY + t * (waterY - startY - 10.dp.toPx())
                    val rotation = t * 90f
                    val armAngle = 45f * (1f - t * 0.8f)

                    translate(cx, fallY) {
                        rotate(rotation) {
                            drawCircle(CapOrange, 6.dp.toPx(), Offset(0f, -14.dp.toPx()))
                            drawLine(Suit, Offset(0f, -8.dp.toPx()), Offset(0f, 6.dp.toPx()),
                                strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
                            val ar = Math.toRadians(armAngle.toDouble())
                            drawLine(Skin, Offset(0f, -4.dp.toPx()),
                                Offset(-cos(ar).toFloat() * 10.dp.toPx(), -4.dp.toPx() + sin(ar).toFloat() * 6.dp.toPx()),
                                strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
                            drawLine(Skin, Offset(0f, -4.dp.toPx()),
                                Offset(cos(ar).toFloat() * 10.dp.toPx(), -4.dp.toPx() + sin(ar).toFloat() * 6.dp.toPx()),
                                strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
                        }
                    }
                }

                // Phase C — splash (60–80%)
                frac < 0.80f -> {
                    val t = (frac - 0.60f) / 0.20f
                    for (i in 0..5) {
                        val angle = Math.toRadians((i * 60.0))
                        val maxR = 8.dp.toPx() + i * 2.dp.toPx()
                        val r = t * maxR
                        val alpha = (0.8f * (1f - t)).coerceIn(0f, 0.8f)
                        drawCircle(Splash.copy(alpha), r,
                            Offset(cx + cos(angle).toFloat() * r * 0.7f,
                                waterY + sin(angle).toFloat() * r * 0.3f))
                    }
                    // Entry line
                    drawLine(Color.White.copy(0.4f * (1f - t)),
                        Offset(cx, waterY - 4.dp.toPx()), Offset(cx, waterY + 12.dp.toPx() * t),
                        strokeWidth = 2.dp.toPx())
                }

                // Phase D — ripples (80–100%)
                else -> {
                    val t = (frac - 0.80f) / 0.20f
                    for (i in 0..3) {
                        val delay = i * 0.15f
                        val rt = ((t - delay) / (1f - delay)).coerceIn(0f, 1f)
                        val r = rt * (20.dp.toPx() + i * 8.dp.toPx())
                        val alpha = (0.25f * (1f - rt)).coerceIn(0f, 0.25f)
                        drawArc(Color.White.copy(alpha), 180f, 180f, false,
                            topLeft = Offset(cx - r, waterY - r * 0.3f),
                            size = Size(r * 2, r * 0.6f),
                            style = Stroke(1.5.dp.toPx()))
                    }
                }
            }

            // Water surface line
            drawLine(Color.White.copy(0.35f), Offset(0f, waterY), Offset(w, waterY),
                strokeWidth = 1.5.dp.toPx())
            // Pool edges
            drawRect(PoolEdge, size = Size(w, 4.dp.toPx()))
            drawRect(PoolEdge, topLeft = Offset(0f, h - 4.dp.toPx()), size = Size(w, 4.dp.toPx()))
        }
        if (message.isNotEmpty())
            Text(message, fontSize = 12.sp, fontFamily = MonospaceFont, color = MutedText)
    }
}

// ═══════════════════════════════════════════════════════════════════
// ANIMATION 4 — RelayRaceAnimation (4 swimmers racing in lanes)
// ═══════════════════════════════════════════════════════════════════
@Composable
fun RelayRaceAnimation(
    message: String = "Optimizando...",
    modifier: Modifier = Modifier
) {
    val inf = rememberInfiniteTransition()

    val durations = listOf(2600, 3100, 2400, 2900)
    val capColors = listOf(
        Color(0xFFFF6B35), Color(0xFFFF3B6B), Color(0xFFFFD700), Color(0xFF48CAE4)
    )
    val speeds = listOf(0.072f, 0.065f, 0.080f, 0.068f)

    val swimXs = durations.map { dur ->
        inf.animateFloat(0f, 1f, infiniteRepeatable(tween(dur, easing = LinearEasing)))
    }
    val swimXValues = swimXs.map { it.value }

    val stroke by inf.animateFloat(0f, 360f,
        infiniteRepeatable(tween(480, easing = LinearEasing)))
    val splashA by inf.animateFloat(0.3f, 0.9f,
        infiniteRepeatable(tween(260), RepeatMode.Reverse))
    val shim by inf.animateFloat(0f, 1f,
        infiniteRepeatable(tween(1800, easing = LinearEasing)))

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Canvas(Modifier.fillMaxWidth().height(120.dp)) {
            val w = size.width; val h = size.height
            val laneH = h / 4f

            drawPoolBackground(shim * 18.dp.toPx())
            drawLaneRopes(4)

            // Find leader
            val leaderIdx = swimXValues.indices.maxByOrNull { swimXValues[it] } ?: 0

            swimXValues.forEachIndexed { i, xFrac ->
                val sy = laneH * i + laneH / 2f
                val sx = xFrac * (w + 60.dp.toPx()) - 30.dp.toPx()
                val strokeAngle = stroke * speeds[i] / speeds[0]

                if (sx > -30.dp.toPx()) {
                    drawSwimmerTopDown(sx, sy, strokeAngle % 360f, splashA, capColors[i])
                }

                // Leader triangle above lane
                if (i == leaderIdx && sx > 10.dp.toPx() && sx < w - 10.dp.toPx()) {
                    val triSize = 6.dp.toPx()
                    val triY = sy - laneH / 2f + 4.dp.toPx()
                    val path = Path().apply {
                        moveTo(sx, triY)
                        lineTo(sx - triSize, triY + triSize * 1.5f)
                        lineTo(sx + triSize, triY + triSize * 1.5f)
                        close()
                    }
                    drawPath(path, Color(0xFFFFD700))
                }
            }
        }
        if (message.isNotEmpty())
            Text(message, fontSize = 12.sp, fontFamily = MonospaceFont, color = MutedText)
    }
}