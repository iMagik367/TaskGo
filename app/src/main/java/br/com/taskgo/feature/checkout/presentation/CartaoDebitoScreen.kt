package br.com.taskgo.taskgo.feature.checkout.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.taskgoapp.core.design.AppTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartaoDebitoScreen(
    onBackClick: () -> Unit
) {
    var nome by remember { mutableStateOf("Lucas Almeida") }
    var numeroCartao by remember { mutableStateOf("") }
    var validade by remember { mutableStateOf("") }
    var cvc by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Cartão de Débito",
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
            // Nome
            Column {
                Text(
                    text = "Nome",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Detalhes do cartão
            Column {
                Text(
                    text = "Detalhes do cartão",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Card container
                OutlinedTextField(
                    value = numeroCartao,
                    onValueChange = { numeroCartao = it },
                    placeholder = { Text("Número do cartão") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Validade e CVC row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = validade,
                        onValueChange = { validade = it },
                        placeholder = { Text("Validade") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = cvc,
                        onValueChange = { cvc = it },
                        placeholder = { Text("CVC") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Save button
            Button(
                onClick = { /* TODO: Salvar cartão */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Salvar",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
