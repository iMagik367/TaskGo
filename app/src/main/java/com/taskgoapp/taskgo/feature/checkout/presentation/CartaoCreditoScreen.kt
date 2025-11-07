package com.taskgoapp.taskgo.feature.checkout.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.taskgoapp.taskgo.core.design.AppTopBar
import com.taskgoapp.taskgo.core.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartaoCreditoScreen(
    onBackClick: () -> Unit,
    isAlt: Boolean = false
) {
    var nome by remember { mutableStateOf("Lucas Almeida") }
    var numeroCartao by remember { mutableStateOf("") }
    var validade by remember { mutableStateOf("") }
    var cvc by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (isAlt) "Cartão (Crédito)" else "Cartão de Crédito",
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
            val fieldContainer: @Composable (@Composable () -> Unit) -> Unit = { content ->
                if (isAlt) {
                    Card { Column(Modifier.padding(16.dp)) { content() } }
                } else {
                    Column { content() }
                }
            }

            // Nome
            fieldContainer {
                Text(
                    text = if (isAlt) "NOME COMPLETO" else "Nome",
                    style = FigmaProductDescription,
                    color = TaskGoTextGray
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
            fieldContainer {
                Text(
                    text = if (isAlt) "DADOS DO CARTÃO" else "Detalhes do cartão",
                    style = FigmaProductDescription,
                    color = TaskGoTextGray
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Número
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
                    containerColor = TaskGoGreen
                )
            ) {
                Text(
                    text = if (isAlt) "Salvar Cartão" else "Salvar",
                    style = FigmaButtonText,
                    color = Color.White
                )
            }
        }
    }
}

