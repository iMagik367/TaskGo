package com.taskgoapp.taskgo.feature.orders.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.taskgoapp.taskgo.core.design.AppTopBar
import com.taskgoapp.taskgo.core.theme.*
import com.taskgoapp.taskgo.data.firestore.models.PurchaseOrderFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShipmentScreen(
    orderId: String,
    onBackClick: () -> Unit,
    onShipmentConfirmed: () -> Unit,
    viewModel: ShipmentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(orderId) {
        viewModel.loadOrder(orderId)
    }
    
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Confirmar Envio",
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.order == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Pedido não encontrado", color = TaskGoTextGray)
            }
        } else {
            val order = uiState.order!!
            val isSameCity = uiState.isSameCity
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Informações do Pedido
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = TaskGoSurface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Pedido #${order.orderNumber}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TaskGoTextBlack
                        )
                        Text(
                            text = "Total: R$ ${String.format("%.2f", order.total)}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TaskGoTextBlack
                        )
                        if (order.deliveryAddressDetails != null) {
                            Text(
                                text = "Endereço: ${order.deliveryAddressDetails.city}, ${order.deliveryAddressDetails.state}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TaskGoTextGray
                            )
                        }
                    }
                }
                
                if (isSameCity) {
                    // Pedido na mesma cidade - confirmação simples
                    SameCityShipmentSection(
                        onConfirmDelivery = { deliveryTime ->
                            scope.launch {
                                viewModel.confirmSameCityDelivery(orderId, deliveryTime)
                                onShipmentConfirmed()
                            }
                        }
                    )
                } else {
                    // Pedido entre cidades - rastreamento
                    CrossCityShipmentSection(
                        trackingCode = uiState.trackingCode,
                        trackingUrl = uiState.trackingUrl,
                        carrierName = uiState.carrierName,
                        shippingMethod = uiState.shippingMethod,
                        onTrackingCodeChange = { viewModel.updateTrackingCode(it) },
                        onTrackingUrlChange = { viewModel.updateTrackingUrl(it) },
                        onCarrierChange = { viewModel.updateCarrier(it) },
                        onShippingMethodChange = { viewModel.updateShippingMethod(it) },
                        onConfirmShipment = {
                            scope.launch {
                                viewModel.confirmShipment(orderId)
                                onShipmentConfirmed()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SameCityShipmentSection(
    onConfirmDelivery: (String) -> Unit
) {
    var deliveryTime by remember { mutableStateOf("") }
    var showTimePicker by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = TaskGoSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = TaskGoGreen
                )
                Text(
                    text = "Pedido na Mesma Cidade",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TaskGoTextBlack
                )
            }
            
            Text(
                text = "Este pedido será entregue na mesma cidade. Confirme quando o pedido chegou ao cliente.",
                style = MaterialTheme.typography.bodyMedium,
                color = TaskGoTextGray
            )
            
            OutlinedTextField(
                value = deliveryTime,
                onValueChange = { deliveryTime = it },
                label = { Text("Horário de Chegada (ex: 14:30)") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.Schedule, contentDescription = null)
                },
                placeholder = { Text("HH:MM") }
            )
            
            Button(
                onClick = { 
                    if (deliveryTime.isNotBlank()) {
                        onConfirmDelivery(deliveryTime)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = deliveryTime.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TaskGoGreen
                )
            ) {
                Text("Confirmar Entrega")
            }
        }
    }
}

@Composable
private fun CrossCityShipmentSection(
    trackingCode: String,
    trackingUrl: String?,
    carrierName: String?,
    shippingMethod: String?,
    onTrackingCodeChange: (String) -> Unit,
    onTrackingUrlChange: (String) -> Unit,
    onCarrierChange: (String) -> Unit,
    onShippingMethodChange: (String) -> Unit,
    onConfirmShipment: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = TaskGoSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalShipping,
                    contentDescription = null,
                    tint = TaskGoGreen
                )
                Text(
                    text = "Pedido Entre Cidades",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TaskGoTextBlack
                )
            }
            
            Text(
                text = "Este pedido será enviado para outra cidade. Informe os dados de rastreamento.",
                style = MaterialTheme.typography.bodyMedium,
                color = TaskGoTextGray
            )
            
            // Seleção de transportadora
            Text(
                text = "Transportadora",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = TaskGoTextBlack
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = shippingMethod == "CORREIOS",
                    onClick = { onShippingMethodChange("CORREIOS") },
                    label = { Text("Correios") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = shippingMethod == "OTHER",
                    onClick = { onShippingMethodChange("OTHER") },
                    label = { Text("Outra") },
                    modifier = Modifier.weight(1f)
                )
            }
            
            if (shippingMethod == "CORREIOS") {
                // Código de rastreamento dos Correios
                OutlinedTextField(
                    value = trackingCode,
                    onValueChange = onTrackingCodeChange,
                    label = { Text("Código de Rastreamento") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.QrCode, contentDescription = null)
                    },
                    placeholder = { Text("Ex: AA123456789BR") },
                    singleLine = true
                )
            } else if (shippingMethod == "OTHER") {
                // Outra transportadora
                OutlinedTextField(
                    value = carrierName ?: "",
                    onValueChange = onCarrierChange,
                    label = { Text("Nome da Transportadora") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.LocalShipping, contentDescription = null)
                    },
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = trackingCode,
                    onValueChange = onTrackingCodeChange,
                    label = { Text("Código de Rastreamento") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.QrCode, contentDescription = null)
                    },
                    placeholder = { Text("Código de rastreamento") },
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = trackingUrl ?: "",
                    onValueChange = onTrackingUrlChange,
                    label = { Text("URL de Rastreamento") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.Link, contentDescription = null)
                    },
                    placeholder = { Text("https://...") },
                    singleLine = true
                )
            }
            
            Button(
                onClick = onConfirmShipment,
                modifier = Modifier.fillMaxWidth(),
                enabled = trackingCode.isNotBlank() && shippingMethod != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TaskGoGreen
                )
            ) {
                Text("Confirmar Envio")
            }
        }
    }
}

@dagger.hilt.android.lifecycle.HiltViewModel
class ShipmentViewModel @Inject constructor(
    private val firestoreOrdersRepository: com.taskgoapp.taskgo.data.repository.FirestoreOrdersRepositoryImpl,
    private val functionsService: com.taskgoapp.taskgo.data.firebase.FirebaseFunctionsService
) : androidx.lifecycle.ViewModel() {
    
    private val _uiState = kotlinx.coroutines.flow.MutableStateFlow(ShipmentUiState())
    val uiState: kotlinx.coroutines.flow.StateFlow<ShipmentUiState> = _uiState.asStateFlow()
    
    suspend fun loadOrder(orderId: String) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        try {
            val order = firestoreOrdersRepository.getPurchaseOrder(orderId)
            if (order != null) {
                // Verificar se é mesma cidade comparando cidade do vendedor com cidade do cliente
                val isSameCity = checkIfSameCity(order)
                _uiState.value = _uiState.value.copy(
                    order = order,
                    isSameCity = isSameCity,
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        } catch (e: Exception) {
            android.util.Log.e("ShipmentVM", "Erro ao carregar pedido: ${e.message}", e)
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
    
    private suspend fun checkIfSameCity(order: PurchaseOrderFirestore): Boolean {
        // TODO: Implementar lógica para verificar se vendedor e cliente estão na mesma cidade
        // Por enquanto, retorna false (assume que é entre cidades)
        return false
    }
    
    fun updateTrackingCode(code: String) {
        _uiState.value = _uiState.value.copy(trackingCode = code)
    }
    
    fun updateTrackingUrl(url: String) {
        _uiState.value = _uiState.value.copy(trackingUrl = url)
    }
    
    fun updateCarrier(name: String) {
        _uiState.value = _uiState.value.copy(carrierName = name)
    }
    
    fun updateShippingMethod(method: String) {
        _uiState.value = _uiState.value.copy(shippingMethod = method)
    }
    
    suspend fun confirmShipment(orderId: String) {
        val state = _uiState.value
        try {
            // Criar documento de envio no Firestore
            val shipmentData = hashMapOf<String, Any>(
                "purchaseOrderId" to orderId,
                "orderId" to orderId, // Mantido para compatibilidade
                "sellerId" to (state.order?.storeId ?: ""),
                "clientId" to (state.order?.clientId ?: ""),
                "isLocalDelivery" to false,
                "isSameCity" to false, // Mantido para compatibilidade
                "shippingMethod" to (state.shippingMethod ?: "CORREIOS"),
                "carrier" to (state.shippingMethod ?: "Correios"),
                "carrierName" to (state.carrierName ?: (state.shippingMethod ?: "Correios")),
                "trackingCode" to state.trackingCode,
                "status" to "SHIPPED"
            )
            if (state.trackingUrl != null) shipmentData["trackingUrl"] = state.trackingUrl
            if (state.carrierName != null) shipmentData["carrierName"] = state.carrierName
            shipmentData["shippedAt"] = com.google.firebase.firestore.FieldValue.serverTimestamp()
            shipmentData["createdAt"] = com.google.firebase.firestore.FieldValue.serverTimestamp()
            shipmentData["updatedAt"] = com.google.firebase.firestore.FieldValue.serverTimestamp()
            
            val shipmentRef = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("shipments")
                .add(shipmentData)
                .await()
            
            // Atualizar status do pedido
            firestoreOrdersRepository.updatePurchaseOrderStatus(orderId, "SHIPPED")
            
            // Transferir pagamento para o vendedor após confirmação de envio
            val transferResult = functionsService.transferPaymentToSeller(orderId)
            transferResult.onSuccess {
                android.util.Log.d("ShipmentVM", "Pagamento transferido com sucesso para o vendedor")
            }.onFailure { error ->
                android.util.Log.e("ShipmentVM", "Erro ao transferir pagamento: ${error.message}", error)
                // Não lançar exceção aqui - o envio foi confirmado, apenas logar o erro
            }
        } catch (e: Exception) {
            android.util.Log.e("ShipmentVM", "Erro ao confirmar envio: ${e.message}", e)
            throw e
        }
    }
    
    suspend fun confirmSameCityDelivery(orderId: String, deliveryTime: String) {
        try {
            val shipmentData = hashMapOf<String, Any>(
                "purchaseOrderId" to orderId,
                "orderId" to orderId, // Mantido para compatibilidade
                "sellerId" to (_uiState.value.order?.storeId ?: ""),
                "clientId" to (_uiState.value.order?.clientId ?: ""),
                "isLocalDelivery" to true,
                "isSameCity" to true, // Mantido para compatibilidade
                "shippingMethod" to "SAME_CITY",
                "carrier" to "Local",
                "carrierName" to "Entrega Local",
                "status" to "DELIVERED",
                "deliveryTime" to deliveryTime
            )
            shipmentData["deliveredAt"] = com.google.firebase.firestore.FieldValue.serverTimestamp()
            shipmentData["createdAt"] = com.google.firebase.firestore.FieldValue.serverTimestamp()
            shipmentData["updatedAt"] = com.google.firebase.firestore.FieldValue.serverTimestamp()
            
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("shipments")
                .add(shipmentData)
                .await()
            
            // Atualizar status do pedido
            firestoreOrdersRepository.updatePurchaseOrderStatus(orderId, "DELIVERED")
            
            // Transferir pagamento para o vendedor após confirmação de entrega local
            val transferResult = functionsService.transferPaymentToSeller(orderId)
            transferResult.onSuccess {
                android.util.Log.d("ShipmentVM", "Pagamento transferido com sucesso para o vendedor")
            }.onFailure { error ->
                android.util.Log.e("ShipmentVM", "Erro ao transferir pagamento: ${error.message}", error)
                // Não lançar exceção aqui - a entrega foi confirmada, apenas logar o erro
            }
        } catch (e: Exception) {
            android.util.Log.e("ShipmentVM", "Erro ao confirmar entrega: ${e.message}", e)
            throw e
        }
    }
}

data class ShipmentUiState(
    val order: PurchaseOrderFirestore? = null,
    val isSameCity: Boolean = false,
    val trackingCode: String = "",
    val trackingUrl: String? = null,
    val carrierName: String? = null,
    val shippingMethod: String? = null,
    val isLoading: Boolean = false
)

