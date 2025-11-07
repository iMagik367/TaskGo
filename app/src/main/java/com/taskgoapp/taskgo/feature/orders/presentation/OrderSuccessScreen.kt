package com.taskgoapp.taskgo.feature.orders.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taskgoapp.taskgo.core.theme.*

@Composable
fun OrderSuccessScreen(
    orderId: String,
    totalAmount: Double,
    address: String?,
    onHome: () -> Unit,
    onViewOrder: () -> Unit,
    variant: String? = null // null=success, 'pending', 'error'
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            when (variant) {
                "pending" -> {
                    Icon(
                        imageVector = Icons.Filled.HourglassEmpty,
                        contentDescription = "Aguardando processamento",
                        tint = TaskGoWarning,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        "Aguardando confirmação",
                        style = FigmaTitleLarge,
                        color = TaskGoWarning
                    )
                    Text(
                        "Seu pedido está sendo processado. Assim que confirmado, enviaremos uma notificação.",
                        style = FigmaProductDescription,
                        color = TaskGoTextGray,
                        modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
                        fontWeight = FontWeight.Normal
                    )
                }
                "error" -> {
                    Icon(
                        imageVector = Icons.Filled.ErrorOutline,
                        contentDescription = "Erro ao processar",
                        tint = TaskGoError,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        "Algo deu errado",
                        style = FigmaTitleLarge,
                        color = TaskGoError
                    )
                    Text(
                        "Não foi possível processar seu pedido no momento. Tente novamente ou fale com o suporte.",
                        style = FigmaProductDescription,
                        color = TaskGoTextGray,
                        modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
                        fontWeight = FontWeight.Normal
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Pedido realizado com sucesso",
                        tint = TaskGoSuccess,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        "Pedido finalizado com sucesso!",
                        style = FigmaTitleLarge,
                        color = TaskGoSuccess
                    )
                    Text(
                        "Seu pedido foi confirmado e entraremos em contato para envio e acompanhamento.",
                        style = FigmaProductDescription,
                        color = TaskGoTextGray,
                        modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
                        fontWeight = FontWeight.Normal
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            Text(
                "Pedido #$orderId",
                style = FigmaSectionTitle,
                color = TaskGoTextBlack
            )
            Row(
                horizontalArrangement = Arrangement.SpaceAround,
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
            ) {
                Column(horizontalAlignment = Alignment.Start, modifier = Modifier.weight(1f)) {
                    Text("Total", style = FigmaProductDescription, color = TaskGoTextGrayLight)
                    Text("R$ %.2f".format(totalAmount), style = FigmaPrice, color = TaskGoPriceGreen, fontWeight = FontWeight.Bold)
                }
                if (address != null) {
                    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.weight(2f)) {
                        Text("Endereço", style = FigmaProductDescription, color = TaskGoTextGrayLight)
                        Text(address, style = FigmaProductName, color = TaskGoTextBlack)
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onHome) {
                    Text("Voltar para Início")
                }
                if (variant != "error") {
                    OutlinedButton(onClick = onViewOrder) {
                        Text("Ver Pedido")
                    }
                }
            }
        }
    }
}

