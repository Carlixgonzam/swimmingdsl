package swimming

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import swimming.model.AnalysisResult
import swimming.service.RascalService
import swimming.ui.AnalysisPanel
import swimming.ui.EditorPanel
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

@Composable
fun SwimmingDslApp() {
    val rascalService = remember { RascalService() }
    val scope = rememberCoroutineScope()

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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(listOf(Primary, Secondary))
                )
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Swimming DSL",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Desktop Mode - powered by Rascal",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = "Programa y analiza sesiones de entrenamiento de natación",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 5.dp)
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            EditorPanel(
                code = code,
                onCodeChange = { code = it },
                onAnalyze = { doAnalyze(it) },
                isLoading = isLoading,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            AnalysisPanel(
                result = analysisResult,
                isLoading = isLoading,
                errorMessage = errorMessage,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
    }
}
