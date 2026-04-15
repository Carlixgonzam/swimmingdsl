package swimming.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import swimming.model.AnalysisResult
import swimming.ui.components.*
import swimming.ui.theme.*

@Composable
fun AnalysisPanel(
    result: AnalysisResult?,
    isLoading: Boolean,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
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
                Text("Análisis", fontWeight = FontWeight.SemiBold, color = TextColor, fontSize = 16.sp)
            }
            Box(modifier = Modifier.padding(20.dp)) {
                Crossfade(
                    targetState = when {
                        isLoading -> "loading"
                        errorMessage != null -> "error"
                        result != null && result.success -> "success"
                        result != null && !result.success -> "error_result"
                        else -> "empty"
                    },
                    animationSpec = tween(ANIM_MEDIUM)
                ) { state ->
                    when (state) {
                        "loading" -> LoadingState()
                        "error" -> ErrorState(errorMessage ?: "")
                        "success" -> AnalysisContent(result!!)
                        "error_result" -> ErrorState(result?.error ?: "Error desconocido")
                        else -> EmptyState()
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    SwimmerLoadingAnimation(
        message = "Analizando con Rascal...",
        modifier = Modifier.padding(20.dp)
    )
}

@Composable
private fun ErrorState(message: String) {
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
            Text("Error", fontWeight = FontWeight.Bold, color = ErrorColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text(message, color = ErrorColor, fontSize = 14.sp)
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("\uD83C\uDFCA", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Escribe código y presiona \"Analizar\"\npara ver los resultados",
            color = TextLight,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun AnalysisContent(result: AnalysisResult) {
    val scrollState = rememberScrollState()
    // Stagger animation for children
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(result) { visible = true }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(ANIM_MEDIUM)) + slideInVertically(tween(ANIM_MEDIUM)) { -20 }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        SuccessColor.copy(alpha = 0.1f),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(15.dp)
            ) {
                Text("\u2713 Análisis completado con Rascal", color = SuccessColor, fontWeight = FontWeight.Medium)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            StatCard(
                value = "${result.totalDistance}",
                label = "metros (${result.distanceKm} km)",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                value = "${result.sessionCount}",
                label = "sesión(es)",
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            StatCard(
                value = result.time.totalFormatted,
                label = "tiempo total",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                value = "${result.rest.periods}",
                label = "descansos",
                modifier = Modifier.weight(1f)
            )
        }

        // Styles section
        // Reemplazar el bloque "Styles section" existente:
        if (result.styles.isNotEmpty()) {
            AnalysisSection("Estilos") {
                val total = result.styles.values.sum().toFloat()
                val barColors = listOf(Primary, Secondary, Color(0xFF0077B6), Color(0xFF90E0EF))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    result.styles.entries.forEachIndexed { i, (style, count) ->
                        val pct = if (total > 0) count / total else 0f
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                style.replaceFirstChar { it.uppercase() },
                                fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                                color = TextColor,
                                modifier = Modifier.width(80.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f).height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(BorderLight)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(pct)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(barColors.getOrElse(i) { Primary })
                                )
                            }
                            Text(
                                "${(pct * 100).toInt()}%",
                                fontSize = 10.sp, color = MutedText,
                                fontFamily = MonospaceFont,
                                modifier = Modifier.width(28.dp)
                            )
                        }
                    }
                }
            }
        }

        // Intensities section
        if (result.intensities.isNotEmpty()) {
            AnalysisSection("Intensidades") {
                FlowTags {
                    result.intensities.forEach { (intensity, count) ->
                        TagChip("$intensity: $count", intensityTagType(intensity))
                    }
                }
            }
        }

        // Equipment section
        if (result.equipment.isNotEmpty()) {
            AnalysisSection("Equipamiento") {
                FlowTags {
                    result.equipment.forEach { (eq, count) ->
                        TagChip("$eq: $count", TagType.EQUIPMENT)
                    }
                }
            }
        }

        // Drills section
        if (result.drills.isNotEmpty()) {
            AnalysisSection("Drills") {
                FlowTags {
                    result.drills.forEach { (drill, count) ->
                        TagChip("$drill: $count", TagType.DRILL)
                    }
                }
            }
        }

        // Time breakdown
        AnalysisSection("Tiempo") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("⏸ Descanso: ${result.time.restFormatted}", fontSize = 14.sp, color = TextColor)
            }
        }
    }
}

@Composable
private fun AnalysisSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            color = Primary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        HorizontalDivider(color = Primary, thickness = 2.dp, modifier = Modifier.padding(bottom = 10.dp))
        content()
    }
}

@Composable
private fun FlowTags(content: @Composable RowScope.() -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        content()
    }
}
