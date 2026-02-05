package com.taskgoapp.taskgo.feature.chatai.presentation

import android.Manifest
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.taskgoapp.taskgo.core.theme.*
import kotlinx.coroutines.delay

/**
 * Tela de conversação por voz similar ao ChatGPT
 * Permite gravar áudio, converter para texto, enviar para AI e receber resposta em voz
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun VoiceChatScreen(
    chatId: String,
    onDismiss: () -> Unit,
    viewModel: ChatAIViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val recordAudioPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    
    // Inicializar chat quando a tela abrir
    LaunchedEffect(chatId) {
        viewModel.initializeChat(chatId)
    }
    
    // Atualizar amplitude do áudio para animação
    val amplitude by produceState(initialValue = 0.0) {
        while (true) {
            if (uiState.isRecording) {
                value = viewModel.getAudioAmplitude()
            }
            delay(50) // Atualizar a cada 50ms para animação suave
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // TOP BAR - Fechar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Título
                Text(
                    text = "Conversação por Voz",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Grave uma mensagem e receba a resposta em voz",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Status da gravação/fala
                when {
                    uiState.isLoading && !uiState.isRecording -> {
                        // Processando/IA está respondendo
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                color = Color.White,
                                strokeWidth = 3.dp
                            )
                            Text(
                                text = "AI está processando...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White
                            )
                        }
                    }
                    uiState.isSpeaking -> {
                        // AI está falando
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Animação de ondas sonoras
                            WaveformAnimation(
                                modifier = Modifier.size(120.dp)
                            )
                            Text(
                                text = "AI está falando...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White
                            )
                        }
                    }
                    uiState.messages.isNotEmpty() -> {
                        // Mostrar última mensagem
                        val lastMessage = uiState.messages.last()
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp)
                        ) {
                            if (lastMessage.isFromAi) {
                                Text(
                                    text = "Resposta:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Surface(
                                    color = Color.White.copy(alpha = 0.15f),
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = lastMessage.text,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.White,
                                        modifier = Modifier.padding(16.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                Text(
                                    text = "Você disse:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Surface(
                                    color = TaskGoGreen.copy(alpha = 0.3f),
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = lastMessage.text,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.White,
                                        modifier = Modifier.padding(16.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Mensagens da conversação (histórico)
                if (uiState.messages.size > 1) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.messages.takeLast(3).forEach { message ->
                            Text(
                                text = if (message.isFromAi) "AI: ${message.text.take(50)}..." else "Você: ${message.text.take(50)}...",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.6f),
                                maxLines = 1
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Botão de gravação principal (grande, central)
                VoiceRecordButton(
                    isRecording = uiState.isRecording,
                    amplitude = amplitude.toFloat(),
                    onRecordClick = {
                        if (recordAudioPermission.status is PermissionStatus.Granted) {
                            if (uiState.isRecording) {
                                // Parar gravação e processar
                                viewModel.stopAudioRecordingAndSend()
                            } else {
                                // Iniciar gravação
                                viewModel.startAudioRecording()
                            }
                        } else {
                            recordAudioPermission.launchPermissionRequest()
                        }
                    },
                    modifier = Modifier.size(120.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Instruções
                Text(
                    text = if (uiState.isRecording) {
                        "Gravando... Toque novamente para enviar"
                    } else if (uiState.isSpeaking) {
                        "Toque no botão para interromper"
                    } else if (uiState.messages.isEmpty()) {
                        "Toque no botão para começar a gravar"
                    } else {
                        "Toque para gravar uma nova mensagem"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
    
    // Quando receber resposta da AI, falar automaticamente
    LaunchedEffect(uiState.messages.size) {
        val lastMessage = uiState.messages.lastOrNull()
        if (lastMessage != null && lastMessage.isFromAi && !uiState.isSpeaking && !uiState.isLoading) {
            // Aguardar um pouco antes de falar
            delay(500)
            viewModel.speakResponse(lastMessage.text)
        }
    }
    
    // Mostrar erro se houver
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            delay(3000)
            viewModel.clearError()
        }
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = TaskGoBackgroundWhite
            ),
            border = BorderStroke(1.dp, TaskGoBorder)
        ) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/**
 * Botão de gravação animado (similar ao ChatGPT)
 */
@Composable
private fun VoiceRecordButton(
    isRecording: Boolean,
    amplitude: Float,
    onRecordClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animação de pulso quando está gravando
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    // Animação de rotação suave
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isRecording) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Círculo externo pulsante (quando está gravando)
        if (isRecording) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(scale)
                    .clip(CircleShape)
                    .background(Color.Red.copy(alpha = 0.3f))
            )
        }
        
        // Círculo do botão
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    if (isRecording) Color.Red else TaskGoGreen
                )
                .clickable(onClick = onRecordClick),
            contentAlignment = Alignment.Center
        ) {
            // Ícone interno
            Icon(
                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = if (isRecording) "Parar gravação" else "Gravar áudio",
                tint = Color.White,
                modifier = Modifier
                    .size(48.dp)
                    .then(
                        if (isRecording) {
                            Modifier.rotate(rotation * 0.1f) // Rotação suave
                        } else {
                            Modifier
                        }
                    )
            )
        }
        
        // Indicador de amplitude quando está gravando
        if (isRecording && amplitude > 0.1f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(1f + (amplitude * 0.3f)) // Escala baseada na amplitude
                    .clip(CircleShape)
                    .background(
                        Color.White.copy(alpha = amplitude * 0.4f)
                    )
            )
        }
    }
}

/**
 * Animação de ondas sonoras (quando AI está falando)
 */
@Composable
private fun WaveformAnimation(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    
    val wave1 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave1"
    )
    
    val wave2 by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave2"
    )
    
    val wave3 by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave3"
    )
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(5) { index ->
            val scale = when (index) {
                0, 4 -> wave1
                1, 3 -> wave2
                else -> wave3
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
                    .scale(scaleX = 1f, scaleY = scale)
                    .clip(MaterialTheme.shapes.small)
                    .background(Color.White.copy(alpha = 0.8f))
            )
        }
    }
}
