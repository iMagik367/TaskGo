package com.example.taskgoapp.feature.auth.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.example.taskgoapp.R
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.taskgoapp.feature.auth.presentation.AuthViewModel
import androidx.compose.runtime.collectAsState

@Composable
fun LoginStoreScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToSignup: () -> Unit,
    onSwitchToPersonLogin: () -> Unit,
    onForgotPassword: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isStoreSelected by remember { mutableStateOf(true) }
    
    val uiState by viewModel.uiState.collectAsState()

    val context = LocalContext.current
    val callbackManager = remember { CallbackManager.Factory.create() }

    // Facebook Login callback
    DisposableEffect(Unit) {
        val callback = object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {
                val accessToken = result.accessToken.token
                viewModel.socialLogin("facebook", accessToken)
            }
            override fun onCancel() {}
            override fun onError(error: FacebookException) {}
        }
        LoginManager.getInstance().registerCallback(callbackManager, callback)
        onDispose {
            LoginManager.getInstance().unregisterCallback(callbackManager)
        }
    }

    // Google Sign-In launcher
    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken("YOUR_GOOGLE_CLIENT_ID.apps.googleusercontent.com") // Substitua pelo clientId correto
            .build()
        GoogleSignIn.getClient(context, gso)
    }
    val googleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken != null) {
                viewModel.socialLogin("google", idToken)
            }
        } catch (e: Exception) {
            // TODO: Tratar erro
        }
    }
    
    // Observar mudanças no estado de login
    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            onLoginSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        // Logo
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_check),
                contentDescription = "Checkmark",
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "TaskGo",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Account Type Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(
                    color = Color(0xFFF5F5F5),
                    shape = RoundedCornerShape(8.dp)
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pessoa Física
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .background(
                        color = if (!isStoreSelected) Color.White else Color.Transparent,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable { isStoreSelected = false },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Pessoa Física",
                    color = if (!isStoreSelected) Color(0xFF333333) else Color(0xFF666666),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Loja
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .background(
                        color = if (isStoreSelected) Color.White else Color.Transparent,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable { isStoreSelected = true },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Loja",
                    color = if (isStoreSelected) Color(0xFF333333) else Color(0xFF666666),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Email Field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-mail") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Forgot Password Link
        Text(
            text = "Esqueci minha senha",
            color = Color(0xFF007AFF),
            fontSize = 14.sp,
            modifier = Modifier
                .align(Alignment.End)
                .clickable { onForgotPassword() }
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Password Field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Senha") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Error Message
        uiState.error?.let { error ->
            Text(
                text = error,
                color = androidx.compose.ui.graphics.Color.Red,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
        }


        // Login Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(
                    color = if (uiState.isLoading) androidx.compose.ui.graphics.Color.Gray else androidx.compose.ui.graphics.Color(0xFF00BD48),
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable { 
                    if (!uiState.isLoading && email.isNotBlank() && password.isNotBlank()) {
                        viewModel.login(email, password)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(color = androidx.compose.ui.graphics.Color.White)
            } else {
                Text(
                    text = "Entrar",
                    color = androidx.compose.ui.graphics.Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Google Sign-In Button
        Button(
            onClick = {
                googleLauncher.launch(googleSignInClient.signInIntent)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_google_logo),
                contentDescription = "Google",
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Entrar com Google", color = Color.Black)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Facebook Login Button
        Button(
            onClick = {
                LoginManager.getInstance().logInWithReadPermissions(
                    context as android.app.Activity,
                    listOf("email", "public_profile")
                )
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2))
        ) {
            Text("Entrar com Facebook", color = Color.White)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Create Account Link
        Text(
            text = "Criar Conta",
            color = Color(0xFF333333),
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.clickable { onNavigateToSignup() }
        )
    }
}

