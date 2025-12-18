package com.taskgoapp.taskgo.feature.auth.presentation

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import com.taskgoapp.taskgo.feature.auth.presentation.LoginViewModel
import com.taskgoapp.taskgo.data.repository.GoogleSignInHelper
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskgoapp.taskgo.core.design.TGIcons
import com.taskgoapp.taskgo.core.theme.TaskGoGreen
import com.taskgoapp.taskgo.core.theme.TaskGoTextBlack
import com.taskgoapp.taskgo.core.theme.TaskGoTextGray
import com.taskgoapp.taskgo.core.theme.TaskGoBackgroundWhite
import com.taskgoapp.taskgo.core.theme.FigmaTitleLarge
import com.taskgoapp.taskgo.core.theme.FigmaProductDescription
import com.taskgoapp.taskgo.core.theme.FigmaButtonText
import com.taskgoapp.taskgo.core.biometric.BiometricManager

@Composable
fun LoginPersonScreen(
    onNavigateToStoreLogin: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToForgotPassword: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    val loginViewModel: LoginViewModel = hiltViewModel()
    val loginUiState = loginViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? androidx.fragment.app.FragmentActivity
    val biometricManager = remember { 
        BiometricManager(context)
    }
    val googleSignInHelper = remember { 
        GoogleSignInHelper(context.applicationContext)
    }
    
    // Verificar disponibilidade de biometria
    val biometricStatus = remember { 
        biometricManager.isBiometricAvailable()
    }
    val biometricAvailable = biometricStatus == com.taskgoapp.taskgo.core.biometric.BiometricStatus.AVAILABLE
    
    // Verificar se biometria está disponível e se activity é FragmentActivity
    val canUseBiometric = biometricAvailable && activity != null
    
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("LoginPersonScreen", "Resultado do Google Sign-In recebido")
        val account = googleSignInHelper.getSignInResultFromIntent(result.data)
        if (account != null) {
            Log.d("LoginPersonScreen", "Conta Google obtida: ${account.email}")
            account.idToken?.let { idToken ->
                Log.d("LoginPersonScreen", "ID Token obtido, iniciando login...")
                loginViewModel.signInWithGoogle(idToken)
            } ?: run {
                Log.e("LoginPersonScreen", "ID Token é null")
                // Atualizar UI state com erro manualmente
                // Como o loginViewModel não tem um método para setar erro diretamente,
                // vamos apenas logar o erro
            }
        } else {
            Log.e("LoginPersonScreen", "Falha ao obter conta do Google Sign-In")
        }
    }

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
                text = "Login",
                style = FigmaTitleLarge,
                color = TaskGoTextBlack
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Botão de Biometria (se disponível)
            if (canUseBiometric) {
                OutlinedButton(
                    onClick = {
                        if (activity != null) {
                            loginViewModel.loginWithBiometric(
                                biometricManager = biometricManager,
                                activity = activity,
                                onBiometricNotAvailable = {
                                    // Não há email salvo, mostrar mensagem
                                }
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TaskGoGreen
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        width = 1.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(TaskGoGreen)
                    )
                ) {
                    Icon(
                        painter = painterResource(TGIcons.Phone), // Usando ícone temporário, pode ser substituído
                        contentDescription = "Biometria",
                        tint = TaskGoGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Entrar com Biometria",
                        style = FigmaButtonText,
                        color = TaskGoGreen
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
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
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Campo E-mail
            com.taskgoapp.taskgo.core.design.EnhancedOutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("E-mail", style = FigmaProductDescription, color = TaskGoTextGray) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                keyboardType = KeyboardType.Email
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
            Button(
                onClick = { 
                    Log.d("LoginPersonScreen", "Botão Entrar clicado para: $email")
                    loginViewModel.login(email, password)
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
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Navegação após sucesso (login com email ou Google)
            LaunchedEffect(loginUiState.value.isSuccess) {
                if (loginUiState.value.isSuccess) {
                    Log.d("LoginPersonScreen", "Login bem-sucedido, navegando para home...")
                    // Aguardar um pouco para garantir que tudo está sincronizado
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
            
            // Botão Entrar com Google
            OutlinedButton(
                onClick = { 
                    googleSignInLauncher.launch(googleSignInHelper.getSignInIntent())
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TaskGoTextBlack
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD9D9D9))
            ) {
                Icon(
                    painter = painterResource(TGIcons.Google),
                    contentDescription = "Google",
                    modifier = Modifier.size(24.dp),
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Entrar com Google",
                    style = FigmaButtonText,
                    color = TaskGoTextBlack
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Botão Criar Conta
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
                    text = "Criar Conta",
                    style = FigmaButtonText
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Link para Login de Prestador
            Text(
                text = "Sou prestador",
                color = TaskGoGreen,
                fontSize = 14.sp,
                modifier = Modifier.clickable { onNavigateToStoreLogin() }
            )
        }
    }
}
