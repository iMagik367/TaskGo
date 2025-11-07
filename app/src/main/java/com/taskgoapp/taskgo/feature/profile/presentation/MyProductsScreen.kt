package com.taskgoapp.taskgo.feature.profile.presentation

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

@Composable
fun MyProductsScreen(
    onNavigateBack: () -> Unit
) {
    val products = listOf(
        ProductItem(
            id = 1,
            title = "Furadeira sem fio",
            price = 299.99,
            date = "15/12/2024",
            status = "Entregue",
            quantity = 1
        ),
        ProductItem(
            id = 2,
            title = "Guarda Roupa 6 Portas",
            price = 899.99,
            date = "10/12/2024",
            status = "Em trânsito",
            quantity = 1
        ),
        ProductItem(
            id = 3,
            title = "Smartphone Galaxy A54",
            price = 1899.99,
            date = "05/12/2024",
            status = "Entregue",
            quantity = 1
        )
    )
    
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Meus Produtos",
                onBackClick = onNavigateBack
            )
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
                    text = "Histórico de Compras",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            items(products) { product ->
                Card(
                    modifier = Modifier.fillMaxWidth()
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
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = product.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Quantidade: ${product.quantity}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            AssistChip(
                                onClick = { },
                                label = { Text(product.status) }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Data: ${product.date}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Valor: R$ %.2f".format(product.price),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            Row {
                                IconButton(onClick = { /* Ver detalhes */ }) {
                                    Icon(Icons.Default.Visibility, contentDescription = "Ver detalhes")
                                }
                                IconButton(onClick = { /* Rastrear */ }) {
                                    Icon(Icons.Default.LocationOn, contentDescription = "Rastrear")
                                }
                                IconButton(onClick = { /* Avaliar */ }) {
                                    Icon(Icons.Default.Star, contentDescription = "Avaliar")
                                }
                            }
                        }
                    }
                }
            }
            
            if (products.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Nenhum produto encontrado",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Faça sua primeira compra",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

data class ProductItem(
    val id: Int,
    val title: String,
    val price: Double,
    val date: String,
    val status: String,
    val quantity: Int
)
