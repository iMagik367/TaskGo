package com.taskgoapp.taskgo.feature.products.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskgoapp.taskgo.R
import com.taskgoapp.taskgo.core.design.AppTopBar
import com.taskgoapp.taskgo.core.theme.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import com.taskgoapp.taskgo.core.data.models.CartItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    onNavigateToCheckout: () -> Unit,
    onNavigateToProductDetail: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToProducts: () -> Unit = {},
    variant: String? = null
) {
    val isLoading = variant == "loading"
    val isError = variant == "error"
    val isSelectingAddress = variant == "selection_address"
    fun getCartItems(): Flow<List<com.taskgoapp.taskgo.core.data.models.CartItem>> = flow {
        delay(200)
        if (variant == "filled" || isSelectingAddress) {
            emit(
                listOf(
                    com.taskgoapp.taskgo.core.data.models.CartItem(
                        id = 1L,
                        product = com.taskgoapp.taskgo.core.data.models.Product(
                            id = 1L,
                            name = "Guarda Roupa",
                            description = "Guarda-roupa 2 portas",
                            price = 750.0,
                            category = "Móveis"
                        ),
                        quantity = 1
                    )
                )
            )
        } else emit(emptyList())
    }
    val cartItems by getCartItems().collectAsStateWithLifecycle(initialValue = emptyList())
    
    var showRemoveDialog by remember { mutableStateOf<CartItem?>(null) }
    val total = cartItems.sumOf { it.quantity * it.product.price }
    val deliveryFee = if (total > 0) 15.0 else 0.0
    val finalTotal = total + deliveryFee

    // Loading state
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // Error state
    if (isError) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = TaskGoError
                )
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Erro ao carregar o carrinho",
                    style = FigmaProductName,
                    color = TaskGoError
                )
                Text(
                    text = "Tente novamente em instantes.",
                    style = FigmaProductDescription,
                    color = TaskGoTextGray
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onNavigateBack,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TaskGoGreen
                    )
                ) {
                    Text(
                        "Tentar Novamente",
                        style = FigmaButtonText,
                        color = Color.White
                    )
                }
            }
        }
        return
    }

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
                if (isSelectingAddress) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(18.dp)) {
                                Text("Entregar em:", style = FigmaProductName, color = TaskGoTextBlack)
                                Text("[Endereço de entrega]", style = FigmaProductDescription, color = TaskGoTextGray)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                item {
                    Text(
                        text = stringResource(R.string.cart_items_title, cartItems.size),
                        style = FigmaProductName,
                        color = TaskGoTextBlack
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                items(cartItems.size) { index ->
                    val cartItem = cartItems[index]
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
                    Card(
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.cart_summary_title),
                                style = FigmaProductName,
                                color = TaskGoTextBlack
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
                            HorizontalDivider(color = TaskGoDivider, modifier = Modifier.padding(vertical = 8.dp))
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
                        tint = TaskGoTextGray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.cart_empty_title),
                        style = FigmaSectionTitle,
                        color = TaskGoTextGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.cart_empty_message),
                        style = FigmaProductDescription,
                        color = TaskGoTextGray,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onNavigateToProducts,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TaskGoGreen
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.cart_start_shopping),
                            style = FigmaButtonText,
                            color = Color.White
                        )
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
                    .background(TaskGoSurfaceGray),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = TaskGoTextGray
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Product Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = cartItem.product.name,
                    style = FigmaProductName,
                    color = TaskGoTextBlack,
                    maxLines = 2
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "R$ ${String.format("%.2f", cartItem.product.price)}",
                    style = FigmaPrice,
                    color = TaskGoPriceGreen
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
                            contentDescription = stringResource(R.string.cart_decrease_quantity),
                            tint = TaskGoGreen
                        )
                    }
                    
                    Text(
                        text = cartItem.quantity.toString(),
                        modifier = Modifier.padding(horizontal = 12.dp),
                        style = FigmaProductName,
                        color = TaskGoTextBlack
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
                    tint = TaskGoError
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
                    style = FigmaProductDescription,
                    color = TaskGoTextGray
                )
                Text(
                    text = "R$ ${String.format("%.2f", total)}",
                    style = FigmaPrice,
                    color = TaskGoPriceGreen
                )
            }
            
            Button(
                onClick = onCheckout,
                modifier = Modifier.height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TaskGoGreen
                )
            ) {
                Text(
                    stringResource(R.string.cart_checkout),
                    style = FigmaButtonText,
                    color = Color.White
                )
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
            style = if (isProminent) FigmaProductName else FigmaProductDescription,
            color = if (isProminent) TaskGoTextBlack else TaskGoTextGray
        )
        Text(
            text = value,
            style = if (isProminent) FigmaPrice else FigmaProductDescription,
            color = if (isProminent) TaskGoPriceGreen else TaskGoTextBlack
        )
    }
}
