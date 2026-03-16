package swimming.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import swimming.ui.theme.MonospaceFont
import swimming.ui.theme.MutedText
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SwimmerLoadingAnimation(
    message: String,
    modifier: Modifier = Modifier
) {
    // Pool colors — hardcoded hex, not theme variables (physical scene)
    val water = Color(0xFF0096C7)
    val lane = Color(0xFFFFFFFF).copy(alpha = 0.25f)
    val edge = Color(0xFF023E8A)
    val floor = Color(0xFF0077B6)
    val skin = Color(0xFFF4C48B)
    val suit = Color(0xFF0A3D54)
    val cap = Color(0xFFFF6B35)
    val splash = Color(0xFFADE8F4).copy(alpha = 0.7f)

    val inf = rememberInfiniteTransition()

    // swimX: 0→1 linear 2800ms Restart — swimmer crosses pool
    val swimX by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // strokeAngle: 0→360 linear 500ms Restart — arm rotation
    val strokeAngle by inf.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // splashAlpha: 0.3→0.9 500ms Reverse — splash pulse
    val splashAlpha by inf.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // shimmer: 0→1 linear 1800ms Restart — tile shimmer offset
    val shimmer by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            val w = size.width
            val h = size.height

            // 1. Draw pool floor
            drawRect(color = floor, topLeft = Offset.Zero, size = Size(w, h))

            // 2. Water overlay
            drawRect(color = water, topLeft = Offset.Zero, size = Size(w, h))

            // 3. Tile grid: lines every 20dp offset by shimmer
            val tileSize = 20.dp.toPx()
            val shimmerOffset = shimmer * tileSize
            val tileLineColor = Color.White.copy(alpha = 0.08f)
            var x = -tileSize + shimmerOffset
            while (x < w + tileSize) {
                drawLine(tileLineColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
                x += tileSize
            }
            var y = 0f
            while (y < h) {
                drawLine(tileLineColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                y += tileSize
            }

            // 4. Lane ropes: 2 rows of circles at h/3 and 2h/3
            val laneY1 = h / 3f
            val laneY2 = 2f * h / 3f
            drawLaneRope(lane, laneY1, w)
            drawLaneRope(lane, laneY2, w)

            // 5. Pool edges: 4dp rects top and bottom
            val edgeH = 4.dp.toPx()
            drawRect(color = edge, topLeft = Offset.Zero, size = Size(w, edgeH))
            drawRect(color = edge, topLeft = Offset(0f, h - edgeH), size = Size(w, edgeH))

            // Swimmer position
            val swimmerX = w * 0.05f + swimX * (w * 0.9f)
            val swimmerY = h / 2f

            // 6. Splash trail: 5 fading circles behind swimmer
            for (i in 1..5) {
                val trailX = swimmerX - i * 12.dp.toPx()
                if (trailX > 0f) {
                    val alpha = splashAlpha * (1f - i * 0.18f)
                    drawCircle(
                        color = splash.copy(alpha = alpha.coerceIn(0f, 1f)),
                        radius = (4.dp.toPx() - i * 0.5f).coerceAtLeast(1f),
                        center = Offset(trailX, swimmerY + (i % 2) * 3f)
                    )
                }
            }

            // 7. Swimmer (top-down view)
            val bodyW = 24.dp.toPx()
            val bodyH = 10.dp.toPx()
            val headR = 5.dp.toPx()
            val armLen = 14.dp.toPx()

            // Body: RoundRect, suit color
            drawRoundRect(
                color = suit,
                topLeft = Offset(swimmerX - bodyW / 2, swimmerY - bodyH / 2),
                size = Size(bodyW, bodyH),
                cornerRadius = CornerRadius(4.dp.toPx())
            )

            // Head: Circle, cap color
            drawCircle(
                color = cap,
                radius = headR,
                center = Offset(swimmerX + bodyW / 2 + headR * 0.8f, swimmerY)
            )

            // Goggles: 2 white dots
            drawCircle(
                color = Color.White,
                radius = 1.5.dp.toPx(),
                center = Offset(swimmerX + bodyW / 2 + headR * 1.2f, swimmerY - 2.dp.toPx())
            )
            drawCircle(
                color = Color.White,
                radius = 1.5.dp.toPx(),
                center = Offset(swimmerX + bodyW / 2 + headR * 1.2f, swimmerY + 2.dp.toPx())
            )

            // Arms
            val angleRad = Math.toRadians(strokeAngle.toDouble())
            val rightArmEnd = Offset(
                swimmerX + (armLen * cos(angleRad)).toFloat(),
                swimmerY + (armLen * sin(angleRad)).toFloat()
            )
            val leftArmEnd = Offset(
                swimmerX + (armLen * cos(angleRad + Math.PI)).toFloat(),
                swimmerY + (armLen * sin(angleRad + Math.PI)).toFloat()
            )

            drawLine(
                color = skin,
                start = Offset(swimmerX, swimmerY),
                end = rightArmEnd,
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = skin,
                start = Offset(swimmerX, swimmerY),
                end = leftArmEnd,
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Splash dot at arm entry when angle < 60 or > 300
            if (strokeAngle < 60f || strokeAngle > 300f) {
                drawCircle(
                    color = splash.copy(alpha = splashAlpha),
                    radius = 3.dp.toPx(),
                    center = rightArmEnd
                )
            }

            // Legs: 2 small RoundRects offset by sin(strokeAngle*3) flutter
            val legFlutter = sin(Math.toRadians((strokeAngle * 3).toDouble())).toFloat() * 4.dp.toPx()
            val legW = 8.dp.toPx()
            val legH = 3.dp.toPx()
            drawRoundRect(
                color = skin,
                topLeft = Offset(swimmerX - bodyW / 2 - legW, swimmerY - legH / 2 + legFlutter),
                size = Size(legW, legH),
                cornerRadius = CornerRadius(2.dp.toPx())
            )
            drawRoundRect(
                color = skin,
                topLeft = Offset(swimmerX - bodyW / 2 - legW, swimmerY - legH / 2 - legFlutter),
                size = Size(legW, legH),
                cornerRadius = CornerRadius(2.dp.toPx())
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            fontSize = 12.sp,
            fontFamily = MonospaceFont,
            color = MutedText
        )
    }
}

private fun DrawScope.drawLaneRope(color: Color, y: Float, width: Float) {
    val circleR = 3.dp.toPx()
    val spacing = 12.dp.toPx()
    var cx = spacing
    while (cx < width) {
        drawCircle(color = color, radius = circleR, center = Offset(cx, y))
        cx += spacing
    }
}
