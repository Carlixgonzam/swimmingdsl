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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.skia.Image
import swimming.agent.CoachAgent
import swimming.agent.DSLTranslatorAgent
import swimming.agent.OptimizerAgent
import swimming.model.AnalysisResult
import swimming.model.UserProfile
import swimming.service.LLMService
import swimming.service.RascalService
import swimming.ui.*
import swimming.ui.theme.*
import java.awt.Taskbar
import java.io.File
import javax.imageio.ImageIO

fun main() {
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
        val json = remember { Json { ignoreUnknownKeys = true } }
        val profileFile = remember {
            File(System.getProperty("user.home"), ".swimmingdsl/profile.json")
        }
        var showOnboarding by remember { mutableStateOf(!profileFile.exists()) }

        Window(
            onCloseRequest = ::exitApplication,
            title = "Swimming DSL - Desktop",
            state = state,
            icon = iconPainter
        ) {
            if (showOnboarding) {
                OnboardingScreen(onComplete = { profile ->
                    profileFile.parentFile.mkdirs()
                    profileFile.writeText(json.encodeToString(profile))
                    showOnboarding = false
                })
            } else {
                val profile = remember {
                    try { json.decodeFromString<UserProfile>(profileFile.readText()) }
                    catch (_: Exception) { UserProfile() }
                }
                SwimmingDslApp(profile = profile)
            }
        }
    }
}

enum class AppTab(val label: String) {
    DASHBOARD("Dashboard"),
    EDITOR("Editor"),
    TRANSLATOR("Traductor IA"),
    COACH("Coach IA"),
    OPTIMIZER("Optimizador IA")
}

@Composable
fun SwimmingDslApp(profile: UserProfile = UserProfile()) {
    val json = remember { Json { ignoreUnknownKeys = true } }
    val rascalService = remember { RascalService() }
    val llmService = remember { LLMService() }
    val translatorAgent = remember { DSLTranslatorAgent(llmService, rascalService, userProfile = profile) }
    val coachAgent = remember { CoachAgent(llmService) }
    val optimizerAgent = remember { OptimizerAgent(llmService, rascalService) }
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(AppTab.DASHBOARD) }
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

    // Session history persistence
    val historyFile = remember {
        File(System.getProperty("user.home"), ".swimmingdsl/history.json")
    }
    var sessionHistory by remember {
        mutableStateOf<List<AnalysisResult>>(
            try { json.decodeFromString(historyFile.readText()) }
            catch (_: Exception) { emptyList() }
        )
    }

    val doAnalyze: (String) -> Unit = { codeToAnalyze ->
        scope.launch {
            isLoading = true
            errorMessage = null
            val result = rascalService.analyze(codeToAnalyze)
            analysisResult = result
            if (result.success) {
                sessionHistory = sessionHistory + result
                historyFile.parentFile.mkdirs()
                historyFile.writeText(json.encodeToString(sessionHistory))
            } else {
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
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp)
                .background(WaterDark)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Swimming DSL",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Desktop \u2022 Rascal + IA",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                )
            }
        }

        // Sidebar + content
        Row(Modifier.fillMaxSize()) {
            SidebarNav(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                modifier = Modifier.width(200.dp).fillMaxHeight()
            )

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
                        AppTab.DASHBOARD -> {
                            DashboardPanel(
                                sessionHistory = sessionHistory,
                                profile = profile,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }
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
                    // Show AnalysisPanel on the right for all tabs except DASHBOARD
                    if (tab != AppTab.DASHBOARD) {
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
    }
}
