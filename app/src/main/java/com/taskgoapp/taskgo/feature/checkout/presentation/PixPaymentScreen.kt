package com.taskgoapp.taskgo.feature.checkout.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.background
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.size
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskgoapp.taskgo.core.theme.*

@Composable
fun PixPaymentScreen(
    orderId: String,
    totalAmount: Double,
    onPaymentSuccess: () -> Unit,
    onBackClick: () -> Unit,
    variant: String? = null, // null=padrão, 'waiting', 'error', 'success'
    viewModel: PixPaymentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Criar pagamento PIX quando a tela carregar
    LaunchedEffect(orderId) {
        if (uiState.pixKey == null && !uiState.isLoading && uiState.error == null) {
            viewModel.createPixPayment(orderId)
        }
    }
    
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
                if (uiState.isLoading) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Gerando pagamento PIX...", style = FigmaProductDescription, color = TaskGoTextGray)
                } else if (uiState.error != null) {
                    Icon(Icons.Default.ErrorOutline, null, tint = TaskGoError, modifier = Modifier.size(60.dp))
                    Spacer(Modifier.height(14.dp))
                    Text("Erro ao gerar pagamento", style = FigmaTitleLarge, color = TaskGoError)
                    Text(uiState.error ?: "Erro desconhecido", style = FigmaProductDescription, color = TaskGoTextGray, modifier = Modifier.padding(top=4.dp, bottom=18.dp))
                    Button(onClick = { viewModel.createPixPayment(orderId) }, modifier = Modifier.fillMaxWidth()) { Text("Tentar novamente") }
                    OutlinedButton(onClick = onBackClick, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") }
                } else {
                    Text("Pagamento via Pix", style = FigmaSectionTitle, color = TaskGoTextBlack)
                    Spacer(Modifier.height(16.dp))
                    
                    // Mostrar QR Code
                    val qrBitmap = uiState.qrCodeBitmap
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR Code PIX",
                            modifier = Modifier.size(250.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier.size(250.dp).background(TaskGoSurfaceGrayBg),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    
                    Spacer(Modifier.height(20.dp))
                    Text("Total: R$ %.2f".format(uiState.amount), style = FigmaPrice, color = TaskGoPriceGreen)
                    Text("Escaneie para pagar ou copie a chave aleatória Pix:", style = FigmaProductDescription, color = TaskGoTextGray, modifier=Modifier.padding(top=8.dp))
                    
                    // Mostrar chave PIX
                    val pixKey = uiState.pixKey
                    if (pixKey != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = TaskGoBackgroundWhite
                            ),
                            border = BorderStroke(1.dp, TaskGoBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = pixKey,
                                    style = FigmaProductDescription,
                                    color = TaskGoTextBlack,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { viewModel.copyPixKey() }) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copiar chave",
                                        tint = TaskGoGreen
                                    )
                                }
                            }
                        }
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.copyPixKey() },
                            modifier = Modifier.weight(1f),
                            enabled = uiState.pixKey != null
                        ) {
                            Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Copiar chave")
                        }
                        OutlinedButton(onClick = onBackClick, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                    }
                }
            }
        }
    }
}
