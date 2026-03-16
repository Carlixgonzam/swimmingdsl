package swimming.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import swimming.agent.OptimizationConfig
import swimming.agent.OptimizationResult
import swimming.agent.OptimizerAgent
import swimming.ui.components.SwimmerLoadingAnimation
import swimming.ui.theme.*

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

@Composable
fun OptimizerPanel(
    optimizerAgent: OptimizerAgent,
    onLoadSession: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var goalExpanded by remember { mutableStateOf(false) }
    var selectedGoal by remember { mutableStateOf(GOALS[0]) }
    var weeks by remember { mutableStateOf("3") }
    var sessionsPerWeek by remember { mutableStateOf("2") }
    var baseDistance by remember { mutableStateOf("2000") }
    var maxMinutes by remember { mutableStateOf("45") }
    var selectedStyles by remember { mutableStateOf(setOf("freestyle")) }
    var isOptimizing by remember { mutableStateOf(false) }
    var progressMessage by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<OptimizationResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                Text("Optimizador IA", fontWeight = FontWeight.SemiBold, color = TextColor, fontSize = 16.sp)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Goal selector
                Text("Objetivo", fontSize = 14.sp, fontWeight = FontWeight.Medium)
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

                // Config fields
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Semanas", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        OutlinedTextField(
                            value = weeks,
                            onValueChange = { weeks = it.filter { c -> c.isDigit() } },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(6.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Sesiones/semana", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        OutlinedTextField(
                            value = sessionsPerWeek,
                            onValueChange = { sessionsPerWeek = it.filter { c -> c.isDigit() } },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(6.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Distancia base (m)", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        OutlinedTextField(
                            value = baseDistance,
                            onValueChange = { baseDistance = it.filter { c -> c.isDigit() } },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(6.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Máx minutos", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        OutlinedTextField(
                            value = maxMinutes,
                            onValueChange = { maxMinutes = it.filter { c -> c.isDigit() } },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(6.dp)
                        )
                    }
                }

                // Styles
                Text("Estilos", fontSize = 14.sp, fontWeight = FontWeight.Medium)
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
                            Text(label, fontSize = 13.sp)
                        }
                    }
                }

                // Generate button
                Button(
                    onClick = {
                        scope.launch {
                            isOptimizing = true
                            errorMessage = null
                            result = null
                            val config = OptimizationConfig(
                                goal = selectedGoal.first,
                                weeks = weeks.toIntOrNull() ?: 3,
                                sessionsPerWeek = sessionsPerWeek.toIntOrNull() ?: 2,
                                baseDistance = baseDistance.toIntOrNull() ?: 2000,
                                styles = selectedStyles.toList().ifEmpty { listOf("freestyle") },
                                maxMinutes = maxMinutes.toIntOrNull() ?: 45
                            )
                            val optimizationResult = optimizerAgent.optimize(config) { msg ->
                                progressMessage = msg
                            }
                            result = optimizationResult
                            if (!optimizationResult.success) {
                                errorMessage = optimizationResult.error
                            }
                            progressMessage = null
                            isOptimizing = false
                        }
                    },
                    enabled = !isOptimizing,
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessColor),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isOptimizing) "Optimizando..." else "Generar Plan de Entrenamiento")
                }

                // Progress indicator
                if (isOptimizing) {
                    SwimmerLoadingAnimation(progressMessage ?: "Optimizando...")
                }

                // Error
                errorMessage?.let {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ErrorColor.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                            .padding(15.dp)
                    ) {
                        Text(it, color = ErrorColor, fontSize = 14.sp)
                    }
                }

                // Results
                result?.let { res ->
                    if (res.sessions.isNotEmpty()) {
                        Text(
                            "Plan generado: ${res.sessions.size} sesiones",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Primary,
                            modifier = Modifier.padding(top = 10.dp)
                        )

                        // Summary
                        val totalDist = res.sessions.sumOf { it.analysis.totalDistance }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(15.dp)
                        ) {
                            Box(
                                modifier = Modifier.weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Primary.copy(alpha = 0.1f))
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("${totalDist}m", fontWeight = FontWeight.Bold, color = Primary)
                                    Text("Total", fontSize = 12.sp, color = TextLight)
                                }
                            }
                            Box(
                                modifier = Modifier.weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Secondary.copy(alpha = 0.1f))
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("${res.sessions.size}", fontWeight = FontWeight.Bold, color = Secondary)
                                    Text("Sesiones", fontSize = 12.sp, color = TextLight)
                                }
                            }
                        }

                        // Individual sessions
                        res.sessions.forEach { session ->
                            val sessionInteraction = remember { MutableInteractionSource() }
                            val sessionHovered by sessionInteraction.collectIsHoveredAsState()
                            val sessionElevation by animateDpAsState(
                                targetValue = if (sessionHovered) 6.dp else 1.dp,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                            )
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(sessionElevation, RoundedCornerShape(10.dp))
                                    .hoverable(sessionInteraction),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (sessionHovered) CardHover else Background
                                )
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "\uD83D\uDCC5 Semana ${session.week} - Sesión ${session.sessionNumber}",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = TextColor
                                        )
                                        val loadInteraction = remember { MutableInteractionSource() }
                                        val loadHovered by loadInteraction.collectIsHoveredAsState()
                                        val loadScale by animateFloatAsState(
                                            targetValue = if (loadHovered) 1.05f else 1f,
                                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                        )
                                        OutlinedButton(
                                            onClick = { onLoadSession(session.code) },
                                            shape = RoundedCornerShape(6.dp),
                                            interactionSource = loadInteraction,
                                            modifier = Modifier
                                                .graphicsLayer { scaleX = loadScale; scaleY = loadScale }
                                                .hoverable(loadInteraction)
                                        ) {
                                            Text("\u25B6 Cargar", fontSize = 12.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                                        Text(
                                            "\uD83C\uDFCA ${session.analysis.totalDistance}m",
                                            fontSize = 13.sp,
                                            color = Primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            "\u23F1 ${session.analysis.time.totalFormatted}",
                                            fontSize = 13.sp,
                                            color = TextLight
                                        )
                                        Text(
                                            session.analysis.styles.keys.joinToString(", "),
                                            fontSize = 13.sp,
                                            color = TextLight
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
