package swimming.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import swimming.service.RascalService
import swimming.ui.components.SwimmerLoadingAnimation
import swimming.ui.theme.SuccessColor
import swimming.util.adjustGeneratedDistance

private val rascalService = RascalService()

private val GOALS = listOf(
    "endurance" to "Resistencia (Endurance)",
    "speed" to "Velocidad (Speed)",
    "technique" to "Técnica (Technique)",
    "recovery" to "Recuperación (Recovery)"
)

private val STYLE_OPTIONS = listOf(
    "freestyle" to "Freestyle",
    "backstroke" to "Backstroke",
    "breaststroke" to "Breaststroke",
    "butterfly" to "Butterfly"
)

// Distance adjustment is now in swimming.util.adjustGeneratedDistance

@Composable
fun GeneratorPanel(
    onCodeGenerated: (String) -> Unit,
    onAnalyze: (String) -> Unit,
    isLoading: Boolean
) {
    val scope = rememberCoroutineScope()
    var goalExpanded by remember { mutableStateOf(false) }
    var selectedGoal by remember { mutableStateOf(GOALS[0]) }
    var distance by remember { mutableStateOf("3000") }
    var duration by remember { mutableStateOf("60") }
    var selectedStyles by remember { mutableStateOf(setOf("freestyle")) }
    var generating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        // Goal selector
        Text("Objetivo", fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
        Box {
            OutlinedButton(
                onClick = { goalExpanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(selectedGoal.second)
            }
            DropdownMenu(
                expanded = goalExpanded,
                onDismissRequest = { goalExpanded = false }
            ) {
                GOALS.forEach { goal ->
                    DropdownMenuItem(
                        text = { Text(goal.second) },
                        onClick = {
                            selectedGoal = goal
                            goalExpanded = false
                        }
                    )
                }
            }
        }

        // Distance
        Text("Distancia Total (metros)", fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
        OutlinedTextField(
            value = distance,
            onValueChange = { distance = it.filter { c -> c.isDigit() } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(6.dp)
        )

        // Styles checkboxes
        Text("Estilos", fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            STYLE_OPTIONS.forEach { (value, label) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = value in selectedStyles,
                        onCheckedChange = { checked ->
                            selectedStyles = if (checked) selectedStyles + value
                            else selectedStyles - value
                        }
                    )
                    Text(label, fontSize = 14.sp)
                }
            }
        }

        // Duration
        Text("Duración (minutos)", fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
        OutlinedTextField(
            value = duration,
            onValueChange = { duration = it.filter { c -> c.isDigit() } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(6.dp)
        )

        // Error message
        errorMessage?.let { err ->
            Text(err, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }

        // Generate button
        Button(
            onClick = {
                scope.launch {
                    generating = true
                    errorMessage = null
                    val styles = selectedStyles.toList().ifEmpty { listOf("freestyle") }
                    val dist = distance.toIntOrNull() ?: 3000
                    val dur = duration.toIntOrNull() ?: 60
                    val result = rascalService.generate(selectedGoal.first, dist, styles, dur)
                    generating = false
                    if (result.success && result.code != null) {
                        val adjustedCode = adjustGeneratedDistance(result.code, dist)
                        onCodeGenerated(adjustedCode)
                        onAnalyze(adjustedCode)
                    } else {
                        errorMessage = result.error ?: "Error al generar sesión"
                    }
                }
            },
            enabled = !generating && !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = SuccessColor),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(if (generating) "Generando..." else "Generar Sesi\u00f3n")
        }

        if (generating) {
            SwimmerLoadingAnimation("Generando sesi\u00f3n...")
        }
    }
}
