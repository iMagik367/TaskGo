package com.taskgoapp.taskgo.feature.checkout.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taskgoapp.taskgo.core.design.AppTopBar
import com.taskgoapp.taskgo.core.design.PrimaryButton
import com.taskgoapp.taskgo.core.theme.*

data class Address(
    val id: Int,
    val name: String,
    val street: String,
    val number: String,
    val complement: String?,
    val neighborhood: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val isDefault: Boolean = false
)

@Composable
fun AddressBookScreen(
    onNavigateBack: () -> Unit,
    onAddressSelected: (Address) -> Unit
) {
    var selectedAddress by remember { mutableStateOf<Address?>(null) }
    
    val addresses = listOf(
        Address(
            id = 1,
            name = "Casa",
            street = "Rua das Flores",
            number = "123",
            complement = "Apto 45",
            neighborhood = "Centro",
            city = "São Paulo",
            state = "SP",
            zipCode = "01234-567",
            isDefault = true
        ),
        Address(
            id = 2,
            name = "Trabalho",
            street = "Av. Paulista",
            number = "1000",
            complement = "Sala 101",
            neighborhood = "Bela Vista",
            city = "São Paulo",
            state = "SP",
            zipCode = "01310-100"
        )
    )
    
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Endereços",
                onBackClick = onNavigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* Adicionar novo endereço */ }
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
            
            items(addresses) { address ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        selectedAddress = address
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
                            if (address.isDefault) {
                                AssistChip(
                                    onClick = { },
                                    label = { Text("Padrão") }
                                )
                            }
                            RadioButton(
                                selected = selectedAddress?.id == address.id,
                                onClick = { selectedAddress = address }
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
                            text = "${address.neighborhood}, ${address.city} - ${address.state}",
                            style = FigmaProductDescription,
                            color = TaskGoTextGray
                        )
                        Text(
                            text = "CEP: ${address.zipCode}",
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
                    onClick = { selectedAddress?.let { onAddressSelected(it) } },
                    enabled = selectedAddress != null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

