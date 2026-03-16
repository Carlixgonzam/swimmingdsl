package swimming.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import swimming.agent.CoachAgent
import swimming.model.AnalysisResult
import swimming.ui.components.SwimmerLoadingAnimation
import swimming.ui.theme.*

data class ChatBubble(val role: String, val content: String)

@Composable
fun CoachPanel(
    coachAgent: CoachAgent,
    analysisResult: AnalysisResult?,
    currentCode: String?,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var userInput by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var chatHistory by remember { mutableStateOf(listOf<ChatBubble>()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    Card(
        modifier = modifier.shadow(4.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Background)
                    .padding(horizontal = 20.dp, vertical = 15.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Coach IA", fontWeight = FontWeight.SemiBold, color = TextColor, fontSize = 16.sp)
                OutlinedButton(
                    onClick = {
                        coachAgent.resetConversation()
                        chatHistory = emptyList()
                        errorMessage = null
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Nueva conversación", fontSize = 12.sp)
                }
            }

            // Context indicator
            if (analysisResult != null && analysisResult.success) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 5.dp)
                        .background(SuccessColor.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        "Contexto: ${analysisResult.totalDistance}m, ${analysisResult.styles.keys.joinToString(", ")}, ${analysisResult.time.totalFormatted}",
                        fontSize = 12.sp,
                        color = SuccessColor
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 5.dp)
                        .background(WarningColor.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        "Analiza una sesión primero para dar contexto al coach",
                        fontSize = 12.sp,
                        color = WarningColor
                    )
                }
            }

            // Chat messages area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (chatHistory.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Pregunta al coach sobre tu sesión de entrenamiento",
                            color = TextLight,
                            fontSize = 14.sp
                        )
                    }
                }
                chatHistory.forEachIndexed { _, bubble ->
                    val isUser = bubble.role == "user"
                    // Animate each new message
                    var messageVisible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { messageVisible = true }

                    AnimatedVisibility(
                        visible = messageVisible,
                        enter = fadeIn(tween(ANIM_MEDIUM)) +
                                slideInHorizontally(tween(ANIM_MEDIUM)) { if (isUser) 40 else -40 }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            if (!isUser) {
                                Text("\uD83C\uDFCB\uFE0F", fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp, end = 6.dp))
                            }
                            Box(
                                modifier = Modifier
                                    .widthIn(max = 500.dp)
                                    .shadow(2.dp, RoundedCornerShape(
                                        topStart = if (isUser) 14.dp else 4.dp,
                                        topEnd = if (isUser) 4.dp else 14.dp,
                                        bottomStart = 14.dp,
                                        bottomEnd = 14.dp
                                    ))
                                    .clip(RoundedCornerShape(
                                        topStart = if (isUser) 14.dp else 4.dp,
                                        topEnd = if (isUser) 4.dp else 14.dp,
                                        bottomStart = 14.dp,
                                        bottomEnd = 14.dp
                                    ))
                                    .background(
                                        if (isUser) Primary.copy(alpha = 0.12f)
                                        else CardBackground
                                    )
                                    .border(
                                        1.dp,
                                        if (isUser) Primary.copy(alpha = 0.15f) else BorderColor.copy(alpha = 0.5f),
                                        RoundedCornerShape(
                                            topStart = if (isUser) 14.dp else 4.dp,
                                            topEnd = if (isUser) 4.dp else 14.dp,
                                            bottomStart = 14.dp,
                                            bottomEnd = 14.dp
                                        )
                                    )
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = bubble.content,
                                    fontSize = 14.sp,
                                    color = TextColor,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }

                if (isSending) {
                    SwimmerLoadingAnimation("Coach pensando...")
                }

                errorMessage?.let {
                    Text(it, color = ErrorColor, fontSize = 13.sp)
                }
            }

            // Auto-scroll when new messages
            LaunchedEffect(chatHistory.size, isSending) {
                scrollState.animateScrollTo(scrollState.maxValue)
            }

            // Input area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Background)
                    .padding(15.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(45.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardBackground)
                        .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                ) {
                    BasicTextField(
                        value = userInput,
                        onValueChange = { userInput = it },
                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 12.dp),
                        textStyle = TextStyle(fontSize = 14.sp, color = TextColor),
                        cursorBrush = SolidColor(TextColor),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (userInput.isEmpty()) {
                                Text(
                                    "¿Está bien balanceada? ¿Cómo la mejorarías?",
                                    color = TextLight.copy(alpha = 0.5f),
                                    fontSize = 14.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                }
                val sendInteraction = remember { MutableInteractionSource() }
                val sendHovered by sendInteraction.collectIsHoveredAsState()
                val sendScale by animateFloatAsState(
                    targetValue = if (sendHovered) 1.05f else 1f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                )
                Button(
                    onClick = {
                        val message = userInput.trim()
                        if (message.isNotBlank()) {
                            userInput = ""
                            chatHistory = chatHistory + ChatBubble("user", message)
                            scope.launch {
                                isSending = true
                                errorMessage = null
                                try {
                                    val response = coachAgent.chat(message, analysisResult, currentCode)
                                    chatHistory = chatHistory + ChatBubble("assistant", response)
                                } catch (e: Exception) {
                                    errorMessage = "Error: ${e.message}"
                                }
                                isSending = false
                            }
                        }
                    },
                    enabled = !isSending && userInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(8.dp),
                    interactionSource = sendInteraction,
                    modifier = Modifier
                        .graphicsLayer { scaleX = sendScale; scaleY = sendScale }
                        .hoverable(sendInteraction)
                ) {
                    Text("Enviar")
                }
            }
        }
    }
}
