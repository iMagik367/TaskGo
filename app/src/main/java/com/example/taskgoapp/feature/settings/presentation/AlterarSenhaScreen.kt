package com.example.taskgoapp.feature.settings.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.taskgoapp.core.design.AppTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlterarSenhaScreen(
    onBackClick: () -> Unit
) {
    var novaSenha by remember { mutableStateOf("") }
    var repitaSenha by remember { mutableStateOf("") }

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
                    text = "Nova senha",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = novaSenha,
                    onValueChange = { novaSenha = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            Column {
                Text(
                    text = "Repita a senha",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = repitaSenha,
                    onValueChange = { repitaSenha = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFCC0000)
                )
            ) {
                Text(
                    text = "Salvar Alterações",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
