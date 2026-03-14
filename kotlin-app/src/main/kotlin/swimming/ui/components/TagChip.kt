package swimming.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import swimming.ui.theme.*

enum class TagType {
    STYLE, INTENSITY_EASY, INTENSITY_MODERATE, INTENSITY_HARD, EQUIPMENT, DRILL
}

@Composable
fun TagChip(
    text: String,
    type: TagType,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = when (type) {
        TagType.STYLE -> TagStyleBg to TagStyleText
        TagType.INTENSITY_EASY -> TagIntensityEasyBg to TagIntensityEasyText
        TagType.INTENSITY_MODERATE -> TagIntensityModerateBg to TagIntensityModerateText
        TagType.INTENSITY_HARD -> TagIntensityHardBg to TagIntensityHardText
        TagType.EQUIPMENT -> TagEquipmentBg to TagEquipmentText
        TagType.DRILL -> TagDrillBg to TagDrillText
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.08f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
    )

    Text(
        text = text,
        color = fg,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.dp, if (isHovered) fg.copy(alpha = 0.3f) else bg, RoundedCornerShape(20.dp))
            .hoverable(interactionSource)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    )
}

fun intensityTagType(intensity: String): TagType = when (intensity) {
    "easy" -> TagType.INTENSITY_EASY
    "moderate" -> TagType.INTENSITY_MODERATE
    "hard" -> TagType.INTENSITY_HARD
    else -> TagType.INTENSITY_EASY
}
