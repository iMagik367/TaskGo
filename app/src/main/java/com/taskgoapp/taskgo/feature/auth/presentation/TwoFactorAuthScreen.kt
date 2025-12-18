package com.taskgoapp.taskgo.feature.auth.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.taskgoapp.taskgo.core.design.AppTopBar
import com.taskgoapp.taskgo.core.theme.*

/**
 * Tela de verificação de código de autenticação de duas etapas (2FA)
 * Exibida após login quando o usuário tem 2FA ativado
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TwoFactorAuthScreen(
    onVerificationSuccess: () -> Unit,
    onVerificationFailed: () -> Unit,
    viewModel: TwoFactorAuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var code by remember { mutableStateOf("") }
    var isResending by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Verificação de Código",
                subtitle = "Digite o código enviado para ${uiState.verificationMethod}",
                onBackClick = onVerificationFailed,
                backgroundColor = TaskGoGreen,
                titleColor = Color.White,
                subtitleColor = Color.White,
                backIconColor = Color.White
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.weight(1f))
            
            // Ícone de segurança
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = TaskGoGreen
            )
            
            Text(
                text = "Autenticação de Duas Etapas",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TaskGoTextBlack,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Enviamos um código de verificação de 6 dígitos para:\n${uiState.verificationMethod}",
                style = MaterialTheme.typography.bodyMedium,
                color = TaskGoTextGray,
                textAlign = TextAlign.Center
            )
            
            // Campo de código
            OutlinedTextField(
                value = code,
                onValueChange = { newValue ->
                    if (newValue.length <= 6 && newValue.all { it.isDigit() }) {
                        code = newValue
                        if (newValue.length == 6) {
                            viewModel.verifyCode(newValue)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Código de verificação") },
                placeholder = { Text("000000") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                singleLine = true,
                isError = uiState.error != null,
                supportingText = uiState.error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
            )
            
            if (uiState.isLoading) {
                CircularProgressIndicator()
            }
            
            // Botão de reenvio
            TextButton(
                onClick = {
                    isResending = true
                    viewModel.resendCode()
                },
                enabled = !isResending && !uiState.isLoading
            ) {
                Text(
                    text = if (isResending) "Reenviando..." else "Reenviar código",
                    color = TaskGoGreen
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Botão de verificar
            Button(
                onClick = { viewModel.verifyCode(code) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = code.length == 6 && !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TaskGoGreen
                )
            ) {
                Text(
                    text = "Verificar",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
    
    // Navegar quando verificação for bem-sucedida
    LaunchedEffect(uiState.isVerified) {
        if (uiState.isVerified) {
            onVerificationSuccess()
        }
    }
}

