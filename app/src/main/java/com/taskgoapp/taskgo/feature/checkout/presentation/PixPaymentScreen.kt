package com.taskgoapp.taskgo.feature.checkout.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.background
import androidx.compose.ui.unit.dp
import com.taskgoapp.taskgo.core.theme.*

@Composable
fun PixPaymentScreen(
    totalAmount: Double,
    onPaymentSuccess: () -> Unit,
    onBackClick: () -> Unit,
    variant: String? = null // null=padrão, 'waiting', 'error', 'success'
        ) {
            Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
        when(variant) {
            "success" -> {
                Icon(Icons.Default.CheckCircle, null, tint = TaskGoSuccess, modifier = Modifier.size(60.dp))
                Spacer(Modifier.height(14.dp))
                Text("Pagamento aprovado!", style = FigmaTitleLarge, color = TaskGoSuccess)
                Text("Seu pagamento via Pix foi confirmado.", style = FigmaProductDescription, color = TaskGoTextGray, modifier = Modifier.padding(top = 4.dp, bottom = 18.dp))
                Button(onClick = onPaymentSuccess, modifier = Modifier.fillMaxWidth()) { Text("Continuar") }
            }
            "waiting" -> {
                Icon(Icons.Default.HourglassEmpty, null, tint = TaskGoWarning, modifier = Modifier.size(60.dp))
                Spacer(Modifier.height(14.dp))
                Text("Aguardando pagamento...", style = FigmaTitleLarge, color = TaskGoWarning)
                Text("Após o pagamento via Pix, a confirmação será automática.", style = FigmaProductDescription, color = TaskGoTextGray, modifier = Modifier.padding(top=4.dp, bottom=18.dp))
            }
            "error" -> {
                Icon(Icons.Default.ErrorOutline, null, tint = TaskGoError, modifier = Modifier.size(60.dp))
                Spacer(Modifier.height(14.dp))
                Text("Pagamento não realizado", style = FigmaTitleLarge, color = TaskGoError)
                Text("Não foi possível processar o pagamento via Pix. Tente novamente.", style = FigmaProductDescription, color = TaskGoTextGray, modifier = Modifier.padding(top=4.dp, bottom=18.dp))
                Button(onClick = onBackClick, modifier = Modifier.fillMaxWidth()) { Text("Tentar novamente") }
            }
            else -> {
                Text("Pagamento via Pix", style = FigmaSectionTitle, color = TaskGoTextBlack)
                Spacer(Modifier.height(16.dp))
                Box(
                    Modifier.size(180.dp).background(TaskGoSurfaceGrayBg), contentAlignment = Alignment.Center
                ) {
                    // Aqui entraria o imageResource real do QR gerado
                    Text("[QR CODE]", color = TaskGoTextGrayLight)
                }
                Spacer(Modifier.height(20.dp))
                Text("Total: R$ %.2f".format(totalAmount), style = FigmaPrice, color = TaskGoPriceGreen)
                Text("Escaneie para pagar ou copie a chave aleatória Pix:", style = FigmaProductDescription, color = TaskGoTextGray, modifier=Modifier.padding(top=8.dp))
                // Simula chave Pix
                Text("0002012636BR.GOV.BCB.PIX...", style = FigmaProductDescription, color = TaskGoTextBlack, modifier = Modifier.padding(vertical = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {}, modifier = Modifier.weight(1f)) { Text("Copiar chave") }
                    OutlinedButton(onClick = onBackClick, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                }
            }
        }
    }
}
