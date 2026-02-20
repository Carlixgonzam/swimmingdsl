package swimming.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

    Text(
        text = text,
        color = fg,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 5.dp)
    )
}

fun intensityTagType(intensity: String): TagType = when (intensity) {
    "easy" -> TagType.INTENSITY_EASY
    "moderate" -> TagType.INTENSITY_MODERATE
    "hard" -> TagType.INTENSITY_HARD
    else -> TagType.INTENSITY_EASY
}
