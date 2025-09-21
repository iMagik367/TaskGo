package com.example.taskgoapp.feature.settings.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.taskgoapp.feature.auth.presentation.AuthViewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.taskgoapp.core.design.AppTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlterarSenhaScreen(
    onBackClick: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var novaSenha by remember { mutableStateOf("") }
    var repitaSenha by remember { mutableStateOf("") }
    var currentSenha by remember { mutableStateOf("") }
    val changeState = viewModel.changePasswordState.collectAsState().value

    Scaffold(
        topBar = { 
            AppTopBar(
                title = "Alterar Senha",
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Text(
                    text = "Senha atual",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = currentSenha,
                    onValueChange = { currentSenha = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Senha atual") }
                )
            }

            Column {
                Text(
                    text = "Nova senha",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = novaSenha,
                    onValueChange = { novaSenha = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Nova senha") }
                )
            }

            Column {
                Text(
                    text = "Repita a nova senha",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = repitaSenha,
                    onValueChange = { repitaSenha = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Repita a nova senha") }
                )
            }

            if (changeState.error != null) {
                Text(changeState.error, color = MaterialTheme.colorScheme.error)
            }

            if (changeState.success) {
                Text("Senha alterada com sucesso!", color = MaterialTheme.colorScheme.primary)
            }

            Button(
                onClick = {
                    if (novaSenha == repitaSenha && novaSenha.isNotBlank() && currentSenha.isNotBlank()) {
                        viewModel.changePassword(currentSenha, novaSenha)
                    }
                },
                enabled = !changeState.isLoading && novaSenha == repitaSenha && novaSenha.isNotBlank() && currentSenha.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFCC0000)
                )
            ) {
                if (changeState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text(
                        text = "Salvar Alterações",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
