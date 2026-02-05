package com.taskgoapp.taskgo.feature.orders.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taskgoapp.taskgo.core.theme.*

@Composable
fun ResumoPedidoScreen(
    orderId: String,
    onBackClick: () -> Unit,
    onIrParaPedidos: () -> Unit,
    variant: String? = null // null padrão, 'discount', 'voucher'
) {
    val produtos = listOf(
        "Guarda-roupa 2 portas" to 750.0,
        "Colchão ortopédico" to 350.0
    )
    val desconto = if (variant == "discount") 100.0 else 0.0
    val voucher = if (variant == "voucher") 50.0 else 0.0
    val subtotal = produtos.sumOf { it.second }
    val total = subtotal + 15 - desconto - voucher
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Resumo do Pedido", style = FigmaSectionTitle, color = TaskGoTextBlack)
        Spacer(Modifier.height(14.dp))
        Card(
            Modifier.fillMaxWidth(), 
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = TaskGoBackgroundWhite
            ),
            border = BorderStroke(1.dp, TaskGoBorder)
        ) {
            Column(Modifier.padding(14.dp)) {
                Text("Produtos", style = FigmaProductDescription, color = TaskGoTextGrayLight)
                produtos.forEach { (nome, valor) ->
                    Row {
                        Text(nome, Modifier.weight(1f), color = TaskGoTextBlack)
                        Text("R$ %.2f".format(valor), color = TaskGoPriceGreen)
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text("Subtotal: R$ %.2f".format(subtotal), style = FigmaPrice)
                Text("Taxa de entrega: R$ 15.00", color = TaskGoTextGray)
                if(desconto > 0) Text("Desconto: -R$ %.2f".format(desconto), color = TaskGoSuccess)
                if(voucher > 0) Text("Voucher aplicado: -R$ %.2f".format(voucher), color = TaskGoAccent)
                Text("Total: R$ %.2f".format(total), style = FigmaSectionTitle, color = TaskGoPriceGreen, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(18.dp))
        Card(
            Modifier.fillMaxWidth(), 
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = TaskGoBackgroundWhite
            ),
            border = BorderStroke(1.dp, TaskGoBorder)
        ) {
            Column(Modifier.padding(14.dp)) {
                Text("Endereço de entrega:", color = TaskGoTextGrayLight, style = FigmaProductDescription)
                Text("[Endereço de entrega]", color = TaskGoTextBlack, style = FigmaProductName)
            }
        }
        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.align(Alignment.End)) {
            Button(onClick = onIrParaPedidos) {
                Text("Finalizar pedido")
            }
            OutlinedButton(onClick = onBackClick) {
                Text("Voltar")
            }
        }
    }
}
