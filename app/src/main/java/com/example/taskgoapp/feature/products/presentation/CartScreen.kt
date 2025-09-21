package com.example.taskgoapp.feature.products.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskgoapp.R
import com.example.taskgoapp.core.design.AppTopBar
import com.example.taskgoapp.core.data.repositories.MarketplaceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import com.example.taskgoapp.core.data.models.CartItem
import com.example.taskgoapp.core.data.models.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    onNavigateToCheckout: () -> Unit,
    onNavigateToProductDetail: (Long) -> Unit,
    onNavigateBack: () -> Unit
) {

    fun getCartItems(): Flow<List<com.example.taskgoapp.core.data.models.CartItem>> = flow {
        delay(200)
        emit(emptyList())
    }
    val cartItems by getCartItems().collectAsStateWithLifecycle(initialValue = emptyList())
    
    var showRemoveDialog by remember { mutableStateOf<CartItem?>(null) }
    
    val total = cartItems.sumOf { it.quantity * it.product.price }
    val deliveryFee = if (total > 0) 15.0 else 0.0
    val finalTotal = total + deliveryFee

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.cart_title),
                onBackClick = onNavigateBack
            )
        },
        bottomBar = {
            if (cartItems.isNotEmpty()) {
                CartBottomBar(
                    total = finalTotal,
                    itemCount = cartItems.size,
                    onCheckout = onNavigateToCheckout
                )
            }
        }
    ) { paddingValues ->
        if (cartItems.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = stringResource(R.string.cart_items_title, cartItems.size),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                items(cartItems) { cartItem ->
                    CartItemCard(
                        cartItem = cartItem,
                        onQuantityIncrease = { /* TODO: Implement quantity increase */ },
                        onQuantityDecrease = { /* TODO: Implement quantity decrease */ },
                        onRemove = { showRemoveDialog = cartItem },
                        onClick = { onNavigateToProductDetail(cartItem.product.id) }
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Order Summary
                    Card(
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.cart_summary_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            SummaryRow(
                                label = stringResource(R.string.cart_subtotal),
                                value = "R$ ${String.format("%.2f", total)}"
                            )
                            
                            SummaryRow(
                                label = stringResource(R.string.cart_delivery),
                                value = "R$ ${String.format("%.2f", deliveryFee)}"
                            )
                            
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            
                            SummaryRow(
                                label = stringResource(R.string.cart_total),
                                value = "R$ ${String.format("%.2f", finalTotal)}",
                                isProminent = true
                            )
                        }
                    }
                }
            }
        } else {
            // Empty Cart
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = stringResource(R.string.cart_empty_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = stringResource(R.string.cart_empty_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = onNavigateBack
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.cart_start_shopping))
                    }
                }
            }
        }
    }
    
    // Remove Item Dialog
    showRemoveDialog?.let { item ->
        AlertDialog(
            onDismissRequest = { showRemoveDialog = null },
            title = { Text(stringResource(R.string.cart_remove_title)) },
            text = { Text(stringResource(R.string.cart_remove_message, item.product.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        // TODO: Implement remove from cart
                        showRemoveDialog = null
                    }
                ) {
                    Text(stringResource(R.string.cart_remove_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRemoveDialog = null }
                ) {
                    Text(stringResource(R.string.cart_remove_cancel))
                }
            }
        )
    }
}

@Composable
private fun CartItemCard(
    cartItem: CartItem,
    onQuantityIncrease: () -> Unit,
    onQuantityDecrease: () -> Unit,
    onRemove: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Product Image
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Product Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = cartItem.product.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "R$ ${String.format("%.2f", cartItem.product.price)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Quantity Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onQuantityDecrease,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = stringResource(R.string.cart_decrease_quantity)
                        )
                    }
                    
                    Text(
                        text = cartItem.quantity.toString(),
                        modifier = Modifier.padding(horizontal = 12.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    IconButton(
                        onClick = onQuantityIncrease,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.cart_increase_quantity)
                        )
                    }
                }
            }
            
            // Remove Button
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.cart_remove_item),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun CartBottomBar(
    total: Double,
    itemCount: Int,
    onCheckout: () -> Unit
) {
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.cart_total),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "R$ ${String.format("%.2f", total)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Button(
                onClick = onCheckout,
                modifier = Modifier.height(48.dp)
            ) {
                Text(stringResource(R.string.cart_checkout))
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    isProminent: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (isProminent) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isProminent) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = value,
            style = if (isProminent) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isProminent) FontWeight.Bold else FontWeight.Normal,
            color = if (isProminent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
