package swimming.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import swimming.model.UserProfile
import swimming.ui.theme.*
import kotlin.math.roundToInt

private data class LevelOption(
    val key: String,
    val label: String,
    val range: String,
    val iconBg: Color
)

private val LEVELS = listOf(
    LevelOption("beginner",     "Principiante", "1500–2500m", Color(0xFFE0F7EE)),
    LevelOption("intermediate", "Intermedio",   "2500–4000m", Color(0xFFE8F9FD)),
    LevelOption("advanced",     "Avanzado",     "4000–6000m", Color(0xFFFFF3E0))
)

private val EQUIPMENT_OPTIONS = listOf(
    "fins"      to "Fins",
    "paddles"   to "Paddles",
    "board"     to "Board",
    "pullbuoy"  to "Pullbuoy",
    "snorkel"   to "Snorkel"
)

@Composable
fun OnboardingScreen(onComplete: (UserProfile) -> Unit) {
    var selectedLevel     by remember { mutableStateOf("intermediate") }
    var selectedEquipment by remember { mutableStateOf(setOf<String>()) }
    var sliderValue       by remember { mutableStateOf(60f) }

    Row(modifier = Modifier.fillMaxSize()) {

        // ── LEFT COLUMN ───────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(WaterDark)
                .padding(32.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Logo
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Primary)
                ) {
                    val w = size.width; val h = size.height
                    val wavePath = Path().apply {
                        moveTo(0f, h * 0.55f)
                        cubicTo(w * 0.25f, h * 0.35f, w * 0.5f, h * 0.7f, w, h * 0.5f)
                        lineTo(w, h); lineTo(0f, h); close()
                    }
                    drawPath(wavePath, Color.White.copy(alpha = 0.4f), style = Fill)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("SwimmingDSL", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }

            // ← FIXED: SwimmerLoadingAnimation is now in swimming.ui (same package)
            // No import needed — same package
            PoolLaneAnimation(
                message = "Tu entrenador personal",
                modifier = Modifier.weight(1f).fillMaxWidth()
            )

            Text(
                "Powered by Rascal MPL",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 11.sp,
                fontFamily = MonospaceFont
            )
        }

        // ── RIGHT COLUMN ──────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("Cuéntanos sobre ti", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = LaneDark)
            Text("Personaliza tu experiencia de entrenamiento", fontSize = 13.sp, color = MutedText)

            // Level selector
            Text("Nivel", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = LaneDark)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LEVELS.forEach { level ->
                    val isActive = selectedLevel == level.key
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                width = if (isActive) 2.dp else 1.5.dp,
                                color = if (isActive) Primary else BorderLight,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .background(if (isActive) Primary.copy(alpha = 0.06f) else Color.Transparent)
                            .clickable { selectedLevel = level.key }
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(level.iconBg)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(level.label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LaneDark)
                        Text(level.range, fontSize = 10.sp, color = MutedText)
                    }
                }
            }

            // Equipment chips
            Text("Equipamiento", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = LaneDark)
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EQUIPMENT_OPTIONS.forEach { (value, label) ->
                    val isActive = value in selectedEquipment
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .border(
                                width = 1.dp,
                                color = if (isActive) Primary else BorderLight,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .background(if (isActive) Primary.copy(alpha = 0.1f) else Color.Transparent)
                            .clickable {
                                selectedEquipment = if (isActive)
                                    selectedEquipment - value
                                else
                                    selectedEquipment + value
                            }
                            .padding(vertical = 5.dp, horizontal = 10.dp)
                    ) {
                        Text(
                            label,
                            fontSize = 11.sp,
                            fontFamily = MonospaceFont,
                            color = if (isActive) LaneDark else MutedText
                        )
                    }
                }
            }

            // Time slider
            Text("Tiempo por sesión", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = LaneDark)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("30", fontSize = 10.sp, color = MutedText)
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 30f..120f,
                    steps = 8,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Primary,
                        activeTrackColor = Primary
                    )
                )
                Text(
                    "${sliderValue.roundToInt()} min",
                    fontSize = 12.sp,
                    fontFamily = MonospaceFont,
                    fontWeight = FontWeight.Medium,
                    color = LaneDark
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    onComplete(
                        UserProfile(
                            level = selectedLevel,
                            availableMinutes = sliderValue.roundToInt(),
                            preferredStyles = listOf("freestyle"),
                            hasEquipment = selectedEquipment.toList()
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    "Comenzar entrenamiento →",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}