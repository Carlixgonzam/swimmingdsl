package swimming.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Card header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Background)
                    .padding(horizontal = 20.dp, vertical = 15.dp)
            ) {
                Text("Análisis", fontWeight = FontWeight.SemiBold, color = TextColor)
            }

            // Content
            Box(modifier = Modifier.padding(20.dp)) {
                when {
                    isLoading -> LoadingState()
                    errorMessage != null -> ErrorState(errorMessage)
                    result != null && result.success -> AnalysisContent(result)
                    result != null && !result.success -> ErrorState(result.error ?: "Error desconocido")
                    else -> EmptyState()
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = Primary)
        Spacer(modifier = Modifier.height(15.dp))
        Text("Analizando con Rascal...", color = TextLight)
    }
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
    Box(
        modifier = Modifier.fillMaxWidth().padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Escribe código y presiona \"Analizar\" para ver los resultados",
            color = TextLight,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun AnalysisContent(result: AnalysisResult) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Success banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    SuccessColor.copy(alpha = 0.1f),
                    RoundedCornerShape(8.dp)
                )
                .padding(15.dp)
        ) {
            Text("✓ Análisis completado con Rascal", color = SuccessColor, fontWeight = FontWeight.Medium)
        }

        // Stats grid
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
        if (result.styles.isNotEmpty()) {
            AnalysisSection("Estilos") {
                FlowTags {
                    result.styles.forEach { (style, count) ->
                        TagChip("$style: $count", TagType.STYLE)
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
                Text("🏊 Nado: ${result.time.swimFormatted}", fontSize = 14.sp, color = TextColor)
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
