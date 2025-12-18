package com.taskgoapp.taskgo.feature.auth.presentation

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.taskgoapp.taskgo.core.biometric.BiometricManager
import com.taskgoapp.taskgo.core.biometric.BiometricStatus
import com.taskgoapp.taskgo.core.theme.TaskGoGreen
import com.taskgoapp.taskgo.core.theme.TaskGoTextDark
import com.taskgoapp.taskgo.core.theme.TaskGoTextGray

@Composable
fun BiometricAuthScreen(
    onAuthSuccess: () -> Unit,
    onAuthFailed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val viewModel: BiometricAuthViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    
    val biometricManager = remember { BiometricManager(context) }
    val biometricStatus = remember { biometricManager.isBiometricAvailable() }
    val biometricAvailable = biometricStatus == BiometricStatus.AVAILABLE
    
    Log.d("BiometricAuthScreen", "=== Iniciando BiometricAuthScreen ===")
    Log.d("BiometricAuthScreen", "BiometricStatus: $biometricStatus, Available: $biometricAvailable")
    Log.d("BiometricAuthScreen", "UIState - isLoading: ${uiState.isLoading}, hasSavedCredentials: ${uiState.hasSavedCredentials}")
    
    // Tentar autenticação biométrica automaticamente quando a tela aparecer
    LaunchedEffect(Unit) {
        Log.d("BiometricAuthScreen", "LaunchedEffect - Iniciando autenticação automática")
        
        if (!uiState.hasSavedCredentials) {
            Log.d("BiometricAuthScreen", "Nenhuma credencial salva, navegando para login")
            onAuthFailed()
            return@LaunchedEffect
        }
        
        // Pequeno delay para garantir que a UI está pronta
        kotlinx.coroutines.delay(500)
        
        if (biometricAvailable && activity != null && !uiState.isLoading) {
            Log.d("BiometricAuthScreen", "Iniciando autenticação biométrica automaticamente")
            viewModel.authenticateWithBiometric(activity, biometricManager) {
                Log.d("BiometricAuthScreen", "Autenticação biométrica bem-sucedida")
                onAuthSuccess()
            }
        } else if (!biometricAvailable) {
            Log.d("BiometricAuthScreen", "Biometria não disponível, mostrando opção de senha")
        }
    }
    
    // Observar mudanças no estado de autenticação
    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) {
            Log.d("BiometricAuthScreen", "Usuário autenticado, navegando para home")
            onAuthSuccess()
        }
    }
    
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(64.dp),
                    color = TaskGoGreen
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Autenticando...",
                    style = MaterialTheme.typography.titleMedium,
                    color = TaskGoTextDark
                )
            } else if (biometricAvailable && uiState.hasSavedCredentials) {
                // Mostrar instruções para biometria
                Text(
                    text = "Autenticação Biométrica",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TaskGoTextDark
                )
                Text(
                    text = "Use sua biometria ou senha para continuar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TaskGoTextGray
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Botão para tentar biometria novamente
                Button(
                    onClick = {
                        if (activity != null) {
                            viewModel.authenticateWithBiometric(activity, biometricManager) {
                                onAuthSuccess()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TaskGoGreen
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tentar Biometria Novamente")
                }
                
                // Botão para usar senha
                TextButton(
                    onClick = {
                        viewModel.showPasswordDialog()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Usar Senha")
                }
            } else if (!biometricAvailable && uiState.hasSavedCredentials) {
                // Mostrar opção de senha se não tiver biometria
                Text(
                    text = "Autenticação",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TaskGoTextDark
                )
                Text(
                    text = "Digite sua senha para continuar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TaskGoTextGray
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        viewModel.showPasswordDialog()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TaskGoGreen
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Digitar Senha")
                }
            }
            
            if (uiState.error != null) {
                Text(
                    text = uiState.error ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
    
    // Dialog de senha
    if (uiState.showPasswordDialog) {
        PasswordDialog(
            onDismiss = { viewModel.hidePasswordDialog() },
            onPasswordEntered = { password ->
                viewModel.authenticateWithPassword(password) {
                    onAuthSuccess()
                }
            },
            savedEmail = uiState.savedEmail
        )
    }
}

@Composable
private fun PasswordDialog(
    onDismiss: () -> Unit,
    onPasswordEntered: (String) -> Unit,
    savedEmail: String?
) {
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Digite sua senha") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (savedEmail != null) {
                    Text(
                        text = "Email: $savedEmail",
                        style = MaterialTheme.typography.bodySmall,
                        color = TaskGoTextGray
                    )
                }
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Senha") },
                    singleLine = true,
                    visualTransformation = if (showPassword) {
                        androidx.compose.ui.text.input.VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                                contentDescription = if (showPassword) "Ocultar senha" else "Mostrar senha"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (password.isNotBlank()) {
                        onPasswordEntered(password)
                    }
                },
                enabled = password.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TaskGoGreen
                )
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

