package swimming

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.launch
import org.jetbrains.skia.Image
import swimming.agent.CoachAgent
import swimming.agent.DSLTranslatorAgent
import swimming.agent.OptimizerAgent
import swimming.model.AnalysisResult
import swimming.service.LLMService
import swimming.service.RascalService
import swimming.ui.*
import swimming.ui.theme.*
import java.awt.Taskbar
import javax.imageio.ImageIO

fun main() {
    // macOS Dock icon
    try {
        val iconStream = object {}.javaClass.getResourceAsStream("/app_icon.png")
        if (iconStream != null) {
            val awtImage = ImageIO.read(iconStream)
            if (Taskbar.isTaskbarSupported()) {
                Taskbar.getTaskbar().iconImage = awtImage
            }
        }
    } catch (_: Exception) { }
    application {
        val state = rememberWindowState(size = DpSize(1400.dp, 900.dp))
        val iconPainter = remember {
            val bytes = object {}.javaClass.getResourceAsStream("/app_icon.png")?.readBytes()
            if (bytes != null) {
                BitmapPainter(Image.makeFromEncoded(bytes).toComposeImageBitmap())
            } else null
        }
        Window(
            onCloseRequest = ::exitApplication,
            title = "Swimming DSL - Desktop",
            state = state,
            icon = iconPainter
        ) {
            SwimmingDslApp()
        }
    }
}

private enum class AppTab(val label: String, val icon: String) {
    EDITOR("Editor", "\uD83D\uDCDD"),
    TRANSLATOR("Traductor IA", "\uD83C\uDF10"),
    COACH("Coach IA", "\uD83C\uDFCB\uFE0F"),
    OPTIMIZER("Optimizador IA", "\u26A1")
}

@Composable
fun SwimmingDslApp() {
    val rascalService = remember { RascalService() }
    val llmService = remember { LLMService() }
    val translatorAgent = remember { DSLTranslatorAgent(llmService, rascalService) }
    val coachAgent = remember { CoachAgent(llmService) }
    val optimizerAgent = remember { OptimizerAgent(llmService, rascalService) }
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(AppTab.EDITOR) }
    var code by remember {
        mutableStateOf(
            """session morning {
  swim 400 m freestyle easy pace 120
  4 x swim 100 m freestyle hard pace 90 rest 20 s
  swim 200 m backstroke moderate pace 130
  3 x kick 50 m hard rest 15 s
}"""
        )
    }
    var analysisResult by remember { mutableStateOf<AnalysisResult?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val doAnalyze: (String) -> Unit = { codeToAnalyze ->
        scope.launch {
            isLoading = true
            errorMessage = null
            val result = rascalService.analyze(codeToAnalyze)
            analysisResult = result
            if (!result.success) {
                errorMessage = result.error
            }
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Header with gradient + depth
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp)
                .background(
                    Brush.linearGradient(
                        listOf(PrimaryDark, Primary, Secondary)
                    )
                )
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Animated wave emoji
                    val infiniteTransition = rememberInfiniteTransition()
                    val wave by infiniteTransition.animateFloat(
                        initialValue = -4f,
                        targetValue = 4f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = EaseInOutSine),
                            repeatMode = RepeatMode.Reverse
                        )
                    )
                    Text(
                        text = "\uD83C\uDFCA",
                        fontSize = 28.sp,
                        modifier = Modifier.graphicsLayer { translationY = wave }
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Swimming DSL",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Desktop • Rascal + IA",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .padding(horizontal = 14.dp, vertical = 5.dp)
                    )
                }
                // Tab navigation with hover + animated indicator
                Spacer(modifier = Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    AppTab.entries.forEach { tab ->
                        val isSelected = selectedTab == tab
                        val interactionSource = remember { MutableInteractionSource() }
                        val isHovered by interactionSource.collectIsHoveredAsState()

                        val bgAlpha by animateFloatAsState(
                            targetValue = when {
                                isSelected -> 0.28f
                                isHovered -> 0.15f
                                else -> 0f
                            },
                            animationSpec = tween(ANIM_FAST)
                        )
                        val textAlpha by animateFloatAsState(
                            targetValue = if (isSelected || isHovered) 1f else 0.6f,
                            animationSpec = tween(ANIM_FAST)
                        )
                        val scale by animateFloatAsState(
                            targetValue = if (isSelected) 1.02f else 1f,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .graphicsLayer { scaleX = scale; scaleY = scale }
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = bgAlpha))
                                .hoverable(interactionSource)
                                .clickable { selectedTab = tab }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = tab.icon,
                                fontSize = 15.sp,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            Text(
                                text = tab.label,
                                color = Color.White.copy(alpha = textAlpha),
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Animated content based on selected tab
        Crossfade(
            targetState = selectedTab,
            animationSpec = tween(ANIM_MEDIUM),
            modifier = Modifier.fillMaxSize()
        ) { tab ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                when (tab) {
                    AppTab.EDITOR -> {
                        EditorPanel(
                            code = code,
                            onCodeChange = { code = it },
                            onAnalyze = { doAnalyze(it) },
                            isLoading = isLoading,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                    AppTab.TRANSLATOR -> {
                        TranslatorPanel(
                            translatorAgent = translatorAgent,
                            onCodeGenerated = { generatedCode ->
                                code = generatedCode
                            },
                            onAnalyze = { doAnalyze(it) },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                    AppTab.COACH -> {
                        CoachPanel(
                            coachAgent = coachAgent,
                            analysisResult = analysisResult,
                            currentCode = code,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                    AppTab.OPTIMIZER -> {
                        OptimizerPanel(
                            optimizerAgent = optimizerAgent,
                            onLoadSession = { sessionCode ->
                                code = sessionCode
                                selectedTab = AppTab.EDITOR
                                doAnalyze(sessionCode)
                            },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
                AnalysisPanel(
                    result = analysisResult,
                    isLoading = isLoading,
                    errorMessage = errorMessage,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        }
    }
}
