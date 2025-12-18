package com.taskgoapp.taskgo.feature.checkout.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskgoapp.taskgo.core.theme.*

@Composable
fun CartaoDebitoScreen(
    onBackClick: () -> Unit,
    isAlt: Boolean = false,
    viewModel: CardFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    var nome by remember { mutableStateOf("") }
    var numero by remember { mutableStateOf("") }
    var validade by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    
    // Observar sucesso e navegar de volta
    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            onBackClick()
        }
    }
    
    val enabled = nome.isNotBlank() && numero.replace(Regex("[^0-9]"), "").length >= 13 && 
                 validade.length >= 5 && cvv.length >= 3
    val bg = if(isAlt) TaskGoSurfaceGrayBg else TaskGoSurface
    Column(
        Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(if(isAlt)"Adicionar cartão de débito" else "Cartão de débito", style = FigmaSectionTitle)
        Spacer(Modifier.height(14.dp))
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors=CardDefaults.cardColors(containerColor = bg)) {
            Column(Modifier.padding(14.dp)) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome no cartão") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = numero,
                    onValueChange = {
                        val cleanValue = it.replace(Regex("[^0-9]"), "")
                        numero = cleanValue.chunked(4).joinToString(" ")
                    },
                    label = { Text("Número do cartão") },
                    placeholder = { Text("0000 0000 0000 0000") },
                    singleLine = true,
                    visualTransformation = VisualTransformation.None,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = validade,
                        onValueChange = {
                            val cleanValue = it.replace(Regex("[^0-9]"), "")
                            validade = when {
                                cleanValue.length <= 2 -> cleanValue
                                else -> "${cleanValue.take(2)}/${cleanValue.drop(2).take(2)}"
                            }
                        },
                        label = { Text("Validade (MM/AA)") },
                        placeholder = { Text("MM/AA") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = cvv,
                        onValueChange = {
                            if(it.length <= 3) cvv = it.filter { ch -> ch.isDigit() }
                        },
                        label = { Text("CVV") },
                        singleLine = true,
                        visualTransformation = VisualTransformation.None,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        // Mostrar erro do ViewModel
        uiState.error?.let { error ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = error,
                color = TaskGoError,
                style = FigmaProductDescription
            )
        }
        
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = {
                val validadeParts = validade.split("/")
                val expMonth = validadeParts.getOrNull(0)?.toIntOrNull() ?: 0
                val expYear = if (validadeParts.getOrNull(1)?.length == 2) {
                    2000 + (validadeParts[1].toIntOrNull() ?: 0)
                } else {
                    validadeParts.getOrNull(1)?.toIntOrNull() ?: 0
                }
                
                viewModel.saveCard(
                    holder = nome,
                    cardNumber = numero.replace(Regex("[^0-9]"), ""),
                    expMonth = expMonth,
                    expYear = expYear,
                    cvc = cvv,
                    type = "Débito"
                )
            },
            enabled = enabled && !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
                Text("Salvando...")
            } else {
                Text("Confirmar")
            }
        }
        OutlinedButton(onClick=onBackClick, modifier=Modifier.fillMaxWidth()) { Text("Voltar") }
    }
}
