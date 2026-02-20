package swimming.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import swimming.examples.DSL_EXAMPLES
import swimming.ui.theme.*

// Keywords for syntax highlighting
private val KEYWORDS = setOf("session", "warmup", "main", "cooldown", "swim", "kick", "drill", "with", "pace", "rest", "x")
private val STYLES = setOf("freestyle", "backstroke", "breaststroke", "butterfly", "medley")
private val INTENSITIES = setOf("easy", "moderate", "hard", "sprint")
private val EQUIPMENT = setOf("fins", "paddles", "board", "pullbuoy", "snorkel")
private val DRILLS = setOf("catchup", "fingertip", "sculling", "onesided", "sixkick", "tarzan")
private val UNITS = setOf("m", "km", "s", "min")

fun highlightDsl(text: String): AnnotatedString {
    return buildAnnotatedString {
        val words = Regex("""(\w+|[{}]|\d+|[^\w\s{}]+|\s+)""").findAll(text)
        for (match in words) {
            val word = match.value
            val style = when {
                word in KEYWORDS -> SpanStyle(color = SyntaxKeyword, fontWeight = FontWeight.Bold)
                word in STYLES -> SpanStyle(color = SyntaxStyle)
                word in INTENSITIES -> SpanStyle(color = SyntaxIntensity)
                word in EQUIPMENT -> SpanStyle(color = SyntaxEquipment)
                word in DRILLS -> SpanStyle(color = SyntaxDrill)
                word in UNITS -> SpanStyle(color = SyntaxUnit)
                word.all { it.isDigit() } && word.isNotEmpty() -> SpanStyle(color = SyntaxNumber)
                word == "{" || word == "}" -> SpanStyle(color = SyntaxPunctuation)
                else -> SpanStyle(color = EditorText)
            }
            withStyle(style) { append(word) }
        }
    }
}

class DslSyntaxHighlight : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(highlightDsl(text.text), OffsetMapping.Identity)
    }
}

@Composable
fun EditorPanel(
    code: String,
    onCodeChange: (String) -> Unit,
    onAnalyze: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Código, 1 = Generador
    var examplesExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Card header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Background)
                    .padding(horizontal = 20.dp, vertical = 15.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Editor", fontWeight = FontWeight.SemiBold, color = TextColor)

                // Example selector
                Box {
                    OutlinedButton(
                        onClick = { examplesExpanded = true },
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Cargar ejemplo...", fontSize = 14.sp)
                    }
                    DropdownMenu(
                        expanded = examplesExpanded,
                        onDismissRequest = { examplesExpanded = false }
                    ) {
                        DSL_EXAMPLES.forEach { example ->
                            DropdownMenuItem(
                                text = { Text(example.name) },
                                onClick = {
                                    onCodeChange(example.code)
                                    examplesExpanded = false
                                    selectedTab = 0
                                }
                            )
                        }
                    }
                }
            }

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = CardBackground,
                contentColor = Primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Código") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Generador") }
                )
            }

            // Tab content
            when (selectedTab) {
                0 -> EditorTab(
                    code = code,
                    onCodeChange = onCodeChange,
                    onAnalyze = onAnalyze,
                    isLoading = isLoading
                )
                1 -> GeneratorPanel(
                    onCodeGenerated = { generatedCode ->
                        onCodeChange(generatedCode)
                        selectedTab = 0
                    },
                    onAnalyze = onAnalyze,
                    isLoading = isLoading
                )
            }
        }
    }
}

@Composable
private fun EditorTab(
    code: String,
    onCodeChange: (String) -> Unit,
    onAnalyze: () -> Unit,
    isLoading: Boolean
) {
    Column(modifier = Modifier.padding(20.dp)) {
        // Editor with syntax highlighting
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(EditorBg)
                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
        ) {
            BasicTextField(
                value = code,
                onValueChange = onCodeChange,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(15.dp),
                textStyle = TextStyle(
                    fontFamily = MonospaceFont,
                    fontSize = 14.sp,
                    color = EditorText,
                    lineHeight = 21.sp
                ),
                cursorBrush = SolidColor(EditorText),
                visualTransformation = remember { DslSyntaxHighlight() },
                decorationBox = { innerTextField ->
                    if (code.isEmpty()) {
                        Text(
                            "Escribe tu código .swim aquí...",
                            color = EditorText.copy(alpha = 0.4f),
                            fontFamily = MonospaceFont,
                            fontSize = 14.sp
                        )
                    }
                    innerTextField()
                }
            )
        }

        Spacer(modifier = Modifier.height(15.dp))

        // Buttons
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onAnalyze,
                enabled = !isLoading && code.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (isLoading) "Analizando..." else "▶ Analizar")
            }
            OutlinedButton(
                onClick = { onCodeChange("") },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("✕ Limpiar")
            }
        }
    }
}
