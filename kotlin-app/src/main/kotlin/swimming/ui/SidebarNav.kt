package swimming.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import swimming.AppTab
import swimming.ui.theme.*

@Composable
fun SidebarNav(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(LaneDark)
            .padding(vertical = 20.dp)
    ) {
        // Nav items
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            AppTab.entries.forEach { tab ->
                val isSelected = selectedTab == tab
                val interactionSource = remember { MutableInteractionSource() }
                val isHovered by interactionSource.collectIsHoveredAsState()

                val bgAlpha by animateFloatAsState(
                    targetValue = when {
                        isSelected -> 0.2f
                        isHovered -> 0.1f
                        else -> 0f
                    },
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(0.dp))
                        .background(Primary.copy(alpha = bgAlpha))
                        .hoverable(interactionSource)
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Small colored dot indicator
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Secondary
                                else Color.White.copy(alpha = 0.3f)
                            )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = tab.label,
                        color = if (isSelected) Secondary else Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // Bottom decoration: 6 vertical bars of different heights
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            val heights = listOf(0.4f, 0.7f, 0.55f, 0.9f, 0.35f, 0.65f)
            heights.forEach { fraction ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(fraction)
                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                )
            }
        }
    }
}
