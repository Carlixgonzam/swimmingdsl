package swimming.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import swimming.agent.DSLTranslatorAgent
import swimming.agent.TranslationResult
import swimming.ui.theme.*

@Composable
fun TranslatorPanel(
    translatorAgent: DSLTranslatorAgent,
    onCodeGenerated: (String) -> Unit,
    onAnalyze: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var userInput by remember { mutableStateOf("") }
    var isTranslating by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<TranslationResult?>(null) }

    Card(
        modifier = modifier.shadow(4.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Background)
                    .padding(horizontal = 20.dp, vertical = 15.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("\uD83C\uDF10", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Traductor IA",
                        fontWeight = FontWeight.SemiBold,
                        color = TextColor,
                        fontSize = 16.sp
                    )
                }
            }

            Column(
                modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                Text(
                    "Describe tu sesión de entrenamiento en lenguaje natural:",
                    fontSize = 14.sp,
                    color = TextColor
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(EditorBg)
                        .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                ) {
                    BasicTextField(
                        value = userInput,
                        onValueChange = { userInput = it },
                        modifier = Modifier.fillMaxSize().padding(15.dp),
                        textStyle = TextStyle(
                            fontSize = 14.sp,
                            color = EditorText,
                            lineHeight = 21.sp
                        ),
                        cursorBrush = SolidColor(EditorText),
                        decorationBox = { innerTextField ->
                            if (userInput.isEmpty()) {
                                Text(
                                    "Ej: quiero nadar 2km de velocidad con mariposa y libre, unos 45 minutos",
                                    color = EditorText.copy(alpha = 0.4f),
                                    fontSize = 14.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                }

                Button(
                    onClick = {
                        scope.launch {
                            isTranslating = true
                            result = null
                            val translationResult = translatorAgent.translate(userInput)
                            result = translationResult
                            if (translationResult.success && translationResult.dslCode != null) {
                                onCodeGenerated(translationResult.dslCode)
                                onAnalyze(translationResult.dslCode)
                            }
                            isTranslating = false
                        }
                    },
                    enabled = !isTranslating && userInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isTranslating) "Traduciendo..." else "Traducir a DSL")
                }

                if (isTranslating) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Primary,
                            strokeWidth = 2.dp
                        )
                        Text("Generando código DSL con IA...", color = TextLight, fontSize = 13.sp)
                    }
                }

                result?.let { res ->
                    if (res.success && res.dslCode != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    SuccessColor.copy(alpha = 0.1f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(15.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "Código generado (intento ${res.attempts})",
                                    color = SuccessColor,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    res.dslCode,
                                    fontFamily = MonospaceFont,
                                    fontSize = 13.sp,
                                    color = TextColor
                                )
                            }
                        }
                    } else if (!res.success) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    ErrorColor.copy(alpha = 0.08f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(15.dp)
                        ) {
                            Column {
                                Text(
                                    "Error tras ${res.attempts} intento(s)",
                                    fontWeight = FontWeight.Bold,
                                    color = ErrorColor
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    res.error ?: "Error desconocido",
                                    color = ErrorColor,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
