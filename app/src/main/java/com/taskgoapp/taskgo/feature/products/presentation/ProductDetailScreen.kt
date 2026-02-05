package com.taskgoapp.taskgo.feature.products.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taskgoapp.taskgo.core.theme.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.taskgoapp.taskgo.R
import com.taskgoapp.taskgo.core.accessibility.AccessibilityStrings
import com.taskgoapp.taskgo.core.design.AppTopBar
import androidx.compose.ui.semantics.semantics
import com.taskgoapp.taskgo.core.accessibility.contentDescription
import com.taskgoapp.taskgo.core.accessibility.testTag
import androidx.compose.ui.res.stringResource
import com.taskgoapp.taskgo.core.model.Product
import com.taskgoapp.taskgo.core.model.ReviewType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: String,
    onBackClick: () -> Unit,
    onAddToCart: () -> Unit,
    variant: String? = null,
    onNavigateToReviews: ((String) -> Unit)? = null,
    viewModel: ProductDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(productId) {
        viewModel.loadProduct(productId)
    }
    
    // Handle success message
    LaunchedEffect(uiState.showAddToCartSuccess) {
        if (uiState.showAddToCartSuccess) {
            // Show snackbar or navigate
            onAddToCart()
            viewModel.dismissAddToCartSuccess()
        }
    }
    
    Scaffold(
        topBar = {
            AppTopBar(
                title = uiState.product?.title ?: stringResource(R.string.products_title),
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.semantics {
                            contentDescription(AccessibilityStrings.loadingState())
                        }
                    )
                }
            }
            
            uiState.error != null -> {
                ErrorState(
                    error = uiState.error!!,
                    onRetry = { viewModel.loadProduct(productId) },
                    onDismiss = { viewModel.dismissError() },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            
            uiState.product != null -> {
                ProductDetailContent(
                    product = uiState.product!!,
                    onAddToCart = { viewModel.addToCart(productId) },
                    variant = variant,
                    onNavigateToReviews = onNavigateToReviews,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun ProductDetailContent(
    product: Product,
    onAddToCart: () -> Unit,
    variant: String? = null,
    onNavigateToReviews: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (variant == "gallery") {
            GallerySection(product)
            Spacer(modifier = Modifier.height(16.dp))
        }
        // Product Image
        if (product.imageUris.isNotEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(product.imageUris.first())
                    .crossfade(true)
                    .build(),
                contentDescription = AccessibilityStrings.productImage(product.title),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .semantics {
                        testTag("product_image")
                    }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Product Title
        Text(
            text = product.title,
            style = FigmaTitleLarge,
            color = TaskGoTextBlack,
            modifier = Modifier.semantics {
                testTag("product_title")
            }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Product Price
        Text(
            text = "R$ ${String.format("%.2f", product.price)}",
            style = FigmaPrice,
            color = TaskGoPriceGreen,
            modifier = Modifier.semantics {
                testTag("product_price")
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Seller Info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = TaskGoBackgroundWhite
            ),
            border = BorderStroke(1.dp, TaskGoBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.products_seller),
                    style = FigmaProductName,
                    color = TaskGoTextBlack
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = product.sellerName ?: "Vendedor não informado",
                    style = FigmaProductDescription,
                    color = TaskGoTextGray
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Seção de Avaliações
        if (onNavigateToReviews != null) {
            com.taskgoapp.taskgo.core.design.reviews.ReviewsSectionCompact(
                targetId = product.id,
                type = com.taskgoapp.taskgo.core.model.ReviewType.PRODUCT,
                onNavigateToReviews = { onNavigateToReviews(product.id) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
        } else if (variant == "reviews" && onNavigateToReviews != null) {
            ReviewsSection(
                productId = product.id,
                onNavigateToReviews = onNavigateToReviews
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Product Description
        if (product.description != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = TaskGoBackgroundWhite
                ),
                border = BorderStroke(1.dp, TaskGoBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.products_description),
                        style = FigmaProductName,
                        color = TaskGoTextBlack
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = product.description,
                        style = FigmaProductDescription,
                        color = TaskGoTextGray
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Add to Cart Button
        Button(
            onClick = onAddToCart,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription(AccessibilityStrings.addToCart(product.title))
                    testTag("add_to_cart_button")
                },
            colors = ButtonDefaults.buttonColors(
                containerColor = TaskGoGreen
            )
        ) {
            Text(
                stringResource(R.string.products_add_to_cart),
                style = FigmaButtonText,
                color = Color.White
            )
        }
    }
}

@Composable
private fun ReviewsSection(
    productId: String,
    onNavigateToReviews: (String) -> Unit
) {
    // Usar o componente real de avaliações conectado ao backend
    com.taskgoapp.taskgo.core.design.reviews.ReviewsSectionCompact(
        targetId = productId,
        type = com.taskgoapp.taskgo.core.model.ReviewType.PRODUCT,
        onNavigateToReviews = { onNavigateToReviews(productId) },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun GallerySection(product: Product) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val images = if (product.imageUris.isNotEmpty()) {
            product.imageUris.take(3)
        } else {
            emptyList()
        }
        images.forEach { uri ->
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(uri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.size(96.dp)
            )
        }
    }
}

@Composable
private fun ErrorState(
    error: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.ui_error),
            style = FigmaSectionTitle,
            color = TaskGoError,
            modifier = Modifier.semantics {
                contentDescription(AccessibilityStrings.errorState())
                testTag("error_title")
            }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = error,
            style = FigmaProductDescription,
            color = TaskGoTextGray,
            modifier = Modifier.semantics {
                testTag("error_message")
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onRetry,
                modifier = Modifier.semantics {
                    contentDescription(AccessibilityStrings.BACK_BUTTON)
                    testTag("retry_button")
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = TaskGoGreen
                )
            ) {
                Text(
                    stringResource(R.string.ui_retry),
                    style = FigmaButtonText,
                    color = Color.White
                )
            }
            
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.semantics {
                    contentDescription(AccessibilityStrings.CANCEL_BUTTON)
                    testTag("dismiss_button")
                }
            ) {
                Text(
                    stringResource(R.string.ui_ok),
                    style = FigmaButtonText
                )
            }
        }
    }
}
