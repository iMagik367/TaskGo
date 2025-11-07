package com.taskgoapp.taskgo.feature.checkout.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.taskgoapp.taskgo.core.theme.*

@Composable
fun FormaPagamentoScreen(
    onBackClick: () -> Unit,
    onPix: () -> Unit,
    onCartaoCredito: () -> Unit,
    onCartaoDebito: () -> Unit,
    variant: String? = null // null=padrão, 'pix_only'
) {
    val options = if(variant == "pix_only") listOf("Pix") else listOf("Pix", "Cartão de Crédito", "Cartão de Débito")
    var selected by remember { mutableStateOf(options.first()) }
    Column(Modifier.fillMaxSize().padding(32.dp)) {
        Text("Forma de pagamento", style = FigmaSectionTitle, color = TaskGoTextBlack)
        Spacer(Modifier.height(18.dp))
        options.forEach {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { selected = it }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = selected == it, onClick = { selected = it })
                Text(it, Modifier.weight(1f), color = TaskGoTextBlack)
            }
        }
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = {
                when(selected){
                    "Pix" -> onPix()
                    "Cartão de Crédito" -> onCartaoCredito()
                    "Cartão de Débito" -> onCartaoDebito()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ){ Text("Continuar") }
        OutlinedButton(onClick = onBackClick, modifier = Modifier.fillMaxWidth()) { Text("Voltar") }
    }
}
