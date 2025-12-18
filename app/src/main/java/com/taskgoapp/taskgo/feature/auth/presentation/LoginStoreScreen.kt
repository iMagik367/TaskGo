package com.taskgoapp.taskgo.feature.auth.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import com.taskgoapp.taskgo.feature.auth.presentation.LoginViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
import kotlinx.coroutines.delay
import com.taskgoapp.taskgo.core.design.TGIcons
import com.taskgoapp.taskgo.core.design.EnhancedOutlinedTextField
import com.taskgoapp.taskgo.core.theme.TaskGoGreen
import com.taskgoapp.taskgo.core.theme.TaskGoTextBlack
import com.taskgoapp.taskgo.core.theme.TaskGoTextGray
import com.taskgoapp.taskgo.core.theme.TaskGoBackgroundWhite
import com.taskgoapp.taskgo.core.theme.FigmaTitleLarge
import com.taskgoapp.taskgo.core.theme.FigmaProductDescription
import com.taskgoapp.taskgo.core.theme.FigmaButtonText

@Composable
fun LoginStoreScreen(
    onNavigateToPersonLogin: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToForgotPassword: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TaskGoBackgroundWhite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))
            
            // Logo TaskGo Horizontal
            Image(
                painter = painterResource(id = TGIcons.TaskGoLogoHorizontal),
                contentDescription = "TaskGo Logo",
                modifier = Modifier.size(120.dp, 40.dp)
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Título
            Text(
                text = "Prestador",
                style = FigmaTitleLarge,
                color = TaskGoTextBlack
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Campo CPF/CNPJ (em vez de email)
            var documentError by remember { mutableStateOf<String?>(null) }
            val documentValidator = remember { com.taskgoapp.taskgo.core.validation.DocumentValidator() }
            
            EnhancedOutlinedTextField(
                value = email, // Reutilizando variável email para CPF/CNPJ
                onValueChange = { newValue ->
                    // Remove caracteres não numéricos
                    val cleanValue = newValue.replace(Regex("[^0-9]"), "")
                    if (cleanValue.length <= 14) {
                        email = cleanValue
                        // Formata e valida quando tiver tamanho completo
                        if (cleanValue.length == 11) {
                            email = documentValidator.formatCpf(cleanValue)
                            val validation = documentValidator.validateCpf(email)
                            documentError = if (validation is com.taskgoapp.taskgo.core.validation.ValidationResult.Invalid) validation.message else null
                        } else if (cleanValue.length == 14) {
                            email = documentValidator.formatCnpj(cleanValue)
                            val validation = documentValidator.validateCnpj(email)
                            documentError = if (validation is com.taskgoapp.taskgo.core.validation.ValidationResult.Invalid) validation.message else null
                        } else {
                            documentError = null
                        }
                    }
                },
                label = { Text("CPF ou CNPJ", style = FigmaProductDescription, color = TaskGoTextGray) },
                placeholder = { Text("000.000.000-00 ou 00.000.000/0000-00", color = TaskGoTextGray) },
                isError = documentError != null,
                supportingText = documentError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Campo Senha
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Senha", style = FigmaProductDescription, color = TaskGoTextGray) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TaskGoGreen,
                    unfocusedBorderColor = Color(0xFFD9D9D9),
                    focusedLabelColor = TaskGoTextGray,
                    unfocusedLabelColor = TaskGoTextGray,
                    cursorColor = TaskGoGreen
                ),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                textStyle = androidx.compose.ui.text.TextStyle(
                    lineHeight = androidx.compose.ui.unit.TextUnit(24f, androidx.compose.ui.unit.TextUnitType.Sp)
                ),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (passwordVisible) "Ocultar senha" else "Mostrar senha",
                            tint = TaskGoTextGray
                        )
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Esqueci minha senha
            Text(
                text = "Esqueci minha senha",
                color = TaskGoGreen,
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable { onNavigateToForgotPassword() }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Botão Entrar
            val loginViewModel: LoginViewModel = hiltViewModel()
            val loginUiState = loginViewModel.uiState.collectAsState()
            
            Button(
                onClick = { 
                    // Validar CPF/CNPJ antes de fazer login
                    val cleanDocument = email.replace(Regex("[^0-9]"), "")
                    if (cleanDocument.length == 11 || cleanDocument.length == 14) {
                        loginViewModel.loginWithDocument(email, password)
                    } else {
                        // Se não for CPF/CNPJ válido, tentar como email (fallback)
                        loginViewModel.login(email, password)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TaskGoGreen
                ),
                enabled = !loginUiState.value.isLoading && email.isNotEmpty() && password.isNotEmpty()
            ) {
                if (loginUiState.value.isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = "Entrar",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Mensagem de erro
            loginUiState.value.errorMessage?.let { errorMsg ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMsg,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Navegação após sucesso
            LaunchedEffect(loginUiState.value.isSuccess) {
                if (loginUiState.value.isSuccess) {
                    Log.d("LoginStoreScreen", "Login bem-sucedido, navegando para home...")
                    kotlinx.coroutines.delay(300)
                    onNavigateToHome()
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Divisor
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFD9D9D9),
                    thickness = 1.dp
                )
                Text(
                    text = "ou",
                    style = FigmaProductDescription,
                    color = TaskGoTextGray,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFD9D9D9),
                    thickness = 1.dp
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Botão Cadastrar
            OutlinedButton(
                onClick = { onNavigateToSignUp() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TaskGoTextBlack
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    width = 1.dp
                )
            ) {
                Text(
                    text = "Cadastrar",
                    style = FigmaButtonText
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Link para Login de Cliente
            Text(
                text = "Sou cliente",
                color = TaskGoGreen,
                fontSize = 14.sp,
                modifier = Modifier.clickable { onNavigateToPersonLogin() }
            )
        }
    }
}