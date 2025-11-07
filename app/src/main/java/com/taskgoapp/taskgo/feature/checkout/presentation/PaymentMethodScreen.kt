package com.taskgoapp.taskgo.feature.checkout.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.taskgoapp.taskgo.core.theme.*

@Composable
fun PaymentMethodScreen(
    onNavigateBack: () -> Unit,
    onPaymentMethodSelected: (String) -> Unit,
    variant: String? = null // null=padrão, 'two_options'
) {
    // Temporariamente desabilitado Google Pay para debug
    // val context = LocalContext.current
    // val activity = context as? Activity
    // val googlePayManager = remember { GooglePayManager(context) }
    // var googlePayAvailable by remember { mutableStateOf(false) }
    // var showGooglePay by remember { mutableStateOf(false) }
    
    // // Verificar disponibilidade do Google Pay
    // LaunchedEffect(Unit) {
    //     googlePayManager.isReadyToPay().addOnSuccessListener { available ->
    //         googlePayAvailable = available
    //     }
    // }
    
    val options = if(variant == "two_options") 
        listOf("Cartão de Crédito", "Cartão de Débito") 
    else 
        listOf("Pix", "Cartão de Crédito", "Cartão de Débito")
    
    var selected by remember { mutableStateOf(options.first()) }
    
    // Temporariamente desabilitado Google Pay para debug
    // val googlePayLauncher = rememberLauncherForActivityResult(
    //     contract = ActivityResultContracts.StartActivityForResult()
    // ) { result ->
    //     val data = result.data
    //     if (result.resultCode == Activity.RESULT_OK && data != null) {
    //         val paymentData = PaymentData.getFromIntent(data)
    //         paymentData?.let {
    //             // Processar pagamento com Google Pay
    //             onPaymentMethodSelected("Google Pay")
    //         }
    //     }
    // }
    Column(Modifier.fillMaxSize().padding(32.dp)) {
        Text("Método de pagamento", style = FigmaSectionTitle, color = TaskGoTextBlack)
        Spacer(Modifier.height(14.dp))
        options.forEach { item ->
            Card(modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical=6.dp)
                    .clickable { selected = item },
                colors= CardDefaults.cardColors(
                    containerColor = if(selected==item) TaskGoGreen.copy(alpha=0.1f) else Color.White)) {
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
            onClick = { onPaymentMethodSelected(selected) },
            modifier = Modifier.fillMaxWidth()
        ){ Text("Selecionar") }
        OutlinedButton(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth()) { Text("Voltar") }
    }
}
