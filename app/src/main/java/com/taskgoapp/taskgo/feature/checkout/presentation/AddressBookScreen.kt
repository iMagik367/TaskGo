package com.taskgoapp.taskgo.feature.checkout.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.runtime.rememberCoroutineScope
import com.taskgoapp.taskgo.core.design.AppTopBar
import com.taskgoapp.taskgo.core.design.PrimaryButton
import com.taskgoapp.taskgo.core.model.Address
import com.taskgoapp.taskgo.core.theme.*

@Composable
fun AddressBookScreen(
    onNavigateBack: () -> Unit,
    onAddressSelected: (String) -> Unit,
    onAddAddress: () -> Unit = {},
    viewModel: CheckoutViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    
    // Inicializar com o endereço já selecionado, se houver
    var selectedAddressId by remember { 
        mutableStateOf<String?>(uiState.selectedAddress?.id) 
    }
    
    val addresses = uiState.availableAddresses
    
    // Atualizar seleção quando o ViewModel mudar
    LaunchedEffect(uiState.selectedAddress?.id) {
        uiState.selectedAddress?.id?.let { selectedAddressId = it }
    }
    
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Endereços",
                onBackClick = onNavigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddAddress
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar endereço")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Escolha o endereço de entrega",
                    style = FigmaProductName,
                    color = TaskGoTextBlack
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            if (addresses.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = TaskGoSurface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = TaskGoTextGray
                            )
                            Text(
                                text = "Nenhum endereço cadastrado",
                                style = FigmaProductName,
                                color = TaskGoTextBlack
                            )
                            Text(
                                text = "Adicione um endereço para continuar com a compra",
                                style = FigmaProductDescription,
                                color = TaskGoTextGray
                            )
                        }
                    }
                }
            }
            
            items(addresses) { address ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        selectedAddressId = address.id
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = address.name,
                                style = FigmaProductName,
                                color = TaskGoTextBlack
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            RadioButton(
                                selected = selectedAddressId == address.id,
                                onClick = { selectedAddressId = address.id }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${address.street}, ${address.number}",
                            style = FigmaProductDescription,
                            color = TaskGoTextGray
                        )
                        if (!address.complement.isNullOrEmpty()) {
                            Text(
                                text = address.complement,
                                style = FigmaProductDescription,
                                color = TaskGoTextGray
                            )
                        }
                        Text(
                            text = "${address.neighborhood.ifEmpty { address.district }}, ${address.city} - ${address.state}",
                            style = FigmaProductDescription,
                            color = TaskGoTextGray
                        )
                        Text(
                            text = "CEP: ${address.zipCode.ifEmpty { address.cep }}",
                            style = FigmaProductDescription,
                            color = TaskGoTextGray
                        )
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
                PrimaryButton(
                    text = "Continuar",
                    onClick = {
                        selectedAddressId?.let { addressId ->
                            // Salva a seleção no ViewModel ANTES de navegar
                            addresses.firstOrNull { it.id == addressId }?.let { address ->
                                viewModel.selectAddress(address)
                                // Pequeno delay para garantir que o estado seja atualizado
                                scope.launch {
                                    delay(100)
                                    onAddressSelected(addressId)
                                }
                            } ?: run {
                                onAddressSelected(addressId)
                            }
                        }
                    },
                    enabled = selectedAddressId != null && addresses.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

