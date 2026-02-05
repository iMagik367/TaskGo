package com.taskgoapp.taskgo.feature.checkout.presentation

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.wallet.PaymentData
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.taskgoapp.taskgo.core.model.PaymentType
import com.taskgoapp.taskgo.core.payment.GooglePayManager
import com.taskgoapp.taskgo.core.theme.*

@Composable
fun PaymentMethodScreen(
    onNavigateBack: () -> Unit,
    onPaymentMethodSelected: (String) -> Unit,
    variant: String? = null, // null=padrão, 'two_options'
    price: String = "0.00", // Preço para Google Pay
    viewModel: CheckoutViewModel
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val googlePayManager = remember { GooglePayManager(context) }
    var googlePayAvailable by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Verificar disponibilidade do Google Pay
    LaunchedEffect(Unit) {
        googlePayManager.isReadyToPay().addOnSuccessListener { available ->
            googlePayAvailable = available
        }.addOnFailureListener {
            googlePayAvailable = false
        }
    }
    
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    val options = if(variant == "two_options") 
        listOf("Cartão de Crédito", "Cartão de Débito") 
    else 
        listOf("Pix", "Cartão de Crédito", "Cartão de Débito")
    
    // Inicializar com o método já selecionado, se houver
    var selected by remember(uiState.selectedPaymentMethod) { 
        mutableStateOf(
            uiState.selectedPaymentMethod?.let {
                when (it.type) {
                    PaymentType.PIX -> "Pix"
                    PaymentType.CREDIT_CARD -> "Cartão de Crédito"
                    PaymentType.DEBIT_CARD -> "Cartão de Débito"
                    else -> options.first()
                }
            } ?: options.first()
        )
    }
    
    // Atualizar seleção quando o ViewModel mudar
    LaunchedEffect(uiState.selectedPaymentMethod?.type) {
        uiState.selectedPaymentMethod?.let {
            selected = when (it.type) {
                PaymentType.PIX -> "Pix"
                PaymentType.CREDIT_CARD -> "Cartão de Crédito"
                PaymentType.DEBIT_CARD -> "Cartão de Débito"
                else -> options.first()
            }
        }
    }
    
    Column(Modifier.fillMaxSize().padding(32.dp)) {
        Text("Método de pagamento", style = FigmaSectionTitle, color = TaskGoTextBlack)
        Spacer(Modifier.height(14.dp))
        
        // Google Pay Button (se disponível) - Melhorado visualmente
        if (googlePayAvailable && activity != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable {
                        googlePayManager.loadPaymentData(
                            activity = activity,
                            price = price,
                            currencyCode = "BRL",
                            onSuccess = { paymentData ->
                                try {
                                    val paymentInfo = googlePayManager.extractPaymentInfo(paymentData)
                                    // O token será processado no backend via processGooglePayPayment
                                    onPaymentMethodSelected("Google Pay")
                                } catch (e: Exception) {
                                    // Erro ao processar pagamento
                                }
                            },
                            onError = { exception ->
                                // Tratar erro
                            }
                        )
                    },
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF000000) // Preto do Google Pay
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                border = BorderStroke(1.dp, TaskGoBorder),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo do Google Pay (texto estilizado)
                    Text(
                        text = "G",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color(0xFF4285F4),
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Text(
                        text = "oogle",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color(0xFFEA4335),
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Text(
                        text = " Pay",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
        }
        
        options.forEach { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical=6.dp)
                    .clickable { selected = item },
                colors = CardDefaults.cardColors(
                    containerColor = TaskGoBackgroundWhite
                ),
                border = BorderStroke(1.dp, TaskGoBorder)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected=selected==item, 
                        onClick={ selected = item }
                    )
                    Text(item, Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        Button(
            onClick = {
                // Mapear o método selecionado para PaymentType e salvar no ViewModel
                val paymentType = when (selected) {
                    "Pix" -> PaymentType.PIX
                    "Cartão de Crédito" -> PaymentType.CREDIT_CARD
                    "Cartão de Débito" -> PaymentType.DEBIT_CARD
                    "Google Pay" -> PaymentType.CREDIT_CARD // Google Pay usa cartão de crédito
                    else -> PaymentType.PIX
                }
                
                // Buscar o método de pagamento correspondente no ViewModel
                val method = uiState.availablePaymentMethods.firstOrNull { 
                    it.type == paymentType && 
                    (paymentType != PaymentType.PIX || it.lastFourDigits == "PIX")
                }
                
                if (method != null) {
                    // Método encontrado, selecionar
                    viewModel.selectPaymentMethod(method)
                } else if (paymentType == PaymentType.PIX) {
                    // Para PIX, sempre criar um método padrão se não existir
                    val pixMethod = com.taskgoapp.taskgo.core.model.PaymentMethod(
                        id = 0L,
                        type = PaymentType.PIX,
                        lastFourDigits = "PIX",
                        cardholderName = "",
                        expiryDate = "",
                        isDefault = false
                    )
                    viewModel.selectPaymentMethod(pixMethod)
                } else {
                    // Para cartões, tentar encontrar qualquer cartão do tipo
                    val anyCard = uiState.availablePaymentMethods.firstOrNull { it.type == paymentType }
                    anyCard?.let { viewModel.selectPaymentMethod(it) }
                }
                
                // Pequeno delay para garantir que o estado seja atualizado antes de navegar
                scope.launch {
                    delay(100)
                    onPaymentMethodSelected(selected)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ){ Text("Selecionar") }
        OutlinedButton(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth()) { Text("Voltar") }
    }
}
