package com.taskgoapp.taskgo.feature.checkout.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskgoapp.taskgo.core.design.AppTopBar
import com.taskgoapp.taskgo.core.theme.*
import androidx.compose.ui.unit.size

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartaoCreditoScreen(
    onBackClick: () -> Unit,
    isAlt: Boolean = false,
    viewModel: CardFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    var nome by remember { mutableStateOf("") }
    var numeroCartao by remember { mutableStateOf("") }
    var validade by remember { mutableStateOf("") }
    var cvc by remember { mutableStateOf("") }
    
    // Observar sucesso e navegar de volta
    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            onBackClick()
        }
    }

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
                    onValueChange = { 
                        // Formatar número do cartão (adicionar espaços a cada 4 dígitos)
                        val cleanValue = it.replace(Regex("[^0-9]"), "")
                        numeroCartao = cleanValue.chunked(4).joinToString(" ")
                    },
                    placeholder = { Text("0000 0000 0000 0000") },
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
                        onValueChange = { 
                            // Formatar validade (MM/AA)
                            val cleanValue = it.replace(Regex("[^0-9]"), "")
                            validade = when {
                                cleanValue.length <= 2 -> cleanValue
                                else -> "${cleanValue.take(2)}/${cleanValue.drop(2).take(2)}"
                            }
                        },
                        placeholder = { Text("MM/AA") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = cvc,
                        onValueChange = { 
                            // Limitar CVC a 3-4 dígitos
                            val cleanValue = it.replace(Regex("[^0-9]"), "")
                            cvc = cleanValue.take(4)
                        },
                        placeholder = { Text("CVC") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }

            // Mostrar erro do ViewModel
            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))

            // Save button
            Button(
                onClick = {
                    // Parse validade (MM/AA)
                    val validadeParts = validade.split("/")
                    val expMonth = validadeParts.getOrNull(0)?.toIntOrNull() ?: 0
                    val expYear = if (validadeParts.getOrNull(1)?.length == 2) {
                        2000 + (validadeParts[1].toIntOrNull() ?: 0)
                    } else {
                        validadeParts.getOrNull(1)?.toIntOrNull() ?: 0
                    }
                    
                    viewModel.saveCard(
                        holder = nome,
                        cardNumber = numeroCartao.replace(Regex("[^0-9]"), ""),
                        expMonth = expMonth,
                        expYear = expYear,
                        cvc = cvc,
                        type = "Crédito"
                    )
                },
                enabled = !uiState.isLoading && nome.isNotEmpty() && 
                         numeroCartao.replace(Regex("[^0-9]"), "").length >= 13 &&
                         validade.length >= 5 && cvc.length >= 3,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TaskGoGreen
                )
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Salvando...", style = FigmaButtonText, color = Color.White)
                } else {
                    Text(
                        text = if (isAlt) "Salvar Cartão" else "Salvar",
                        style = FigmaButtonText,
                        color = Color.White
                    )
                }
            }
        }
    }
}

