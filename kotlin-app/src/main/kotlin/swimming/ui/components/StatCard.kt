package swimming.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import swimming.ui.theme.*

@Composable
fun StatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val elevation by animateDpAsState(
        targetValue = if (isHovered) 12.dp else 3.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
    )
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.04f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
    )

    Box(
    modifier = modifier
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .shadow(elevation, RoundedCornerShape(12.dp))
        .clip(RoundedCornerShape(12.dp))
        .background(Color.White)
        .border(0.5.dp, BorderLight, RoundedCornerShape(12.dp))
        .hoverable(interactionSource)
        .padding(16.dp),
    contentAlignment = Alignment.Center
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = LaneDark, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(label, color = MutedText, fontSize = 13.sp)
    }
}
}
