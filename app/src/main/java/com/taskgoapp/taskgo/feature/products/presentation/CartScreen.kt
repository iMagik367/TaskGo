package com.taskgoapp.taskgo.feature.products.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
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
    onNavigateToProductDetail: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToProducts: () -> Unit = {},
    variant: String? = null,
    viewModel: CartViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isLoading = variant == "loading" || uiState.isLoading
    val isError = variant == "error" || uiState.error != null
    val isSelectingAddress = variant == "selection_address"
    val cartItems = uiState.cartItems
    val total = uiState.total
    val deliveryFee = uiState.deliveryFee
    val finalTotal = uiState.finalTotal
    
    var showRemoveDialog by remember { mutableStateOf<com.taskgoapp.taskgo.core.data.models.CartItem?>(null) }

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
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = TaskGoBackgroundWhite
                            ),
                            border = BorderStroke(1.dp, TaskGoBorder)
                        ) {
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
                    val cartItemWithId = cartItems[index]
                    val cartItem = cartItemWithId.uiCartItem
                    CartItemCard(
                        cartItem = cartItem,
                        onQuantityIncrease = { viewModel.increaseQuantity(cartItemWithId.productId) },
                        onQuantityDecrease = { viewModel.decreaseQuantity(cartItemWithId.productId) },
                        onRemove = { showRemoveDialog = cartItem },
                        onClick = { onNavigateToProductDetail(cartItemWithId.productId) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = TaskGoBackgroundWhite
                        ),
                        border = BorderStroke(1.dp, TaskGoBorder)
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
                        // Encontrar o productId do item removido
                        uiState.cartItems.find { it.uiCartItem == item }?.let { cartItemWithId ->
                            viewModel.removeItem(cartItemWithId.productId)
                        }
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = TaskGoBackgroundWhite
        ),
        border = BorderStroke(1.dp, TaskGoBorder)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Product Image
            val imageUri = cartItem.product.imageUrl
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(TaskGoSurfaceGray),
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null && imageUri.isNotBlank()) {
                    coil.compose.AsyncImage(
                        model = coil.request.ImageRequest.Builder(LocalContext.current)
                            .data(imageUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = cartItem.product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = TaskGoTextGray
                )
                }
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
