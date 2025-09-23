package br.com.taskgo.taskgo.feature.products.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.taskgoapp.R
import com.example.taskgoapp.core.accessibility.AccessibilityStrings
import com.example.taskgoapp.core.design.AppTopBar
import androidx.compose.ui.semantics.semantics
import com.example.taskgoapp.core.accessibility.contentDescription
import com.example.taskgoapp.core.accessibility.testTag
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: String,
    onBackClick: () -> Unit,
    onAddToCart: () -> Unit,
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
                title = uiState.product?.name ?: stringResource(R.string.products_title),
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
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun ProductDetailContent(
    product: com.example.taskgoapp.core.data.models.Product,
    onAddToCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Product Image
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(product.imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = AccessibilityStrings.productImage(product.name),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .semantics {
                    testTag("product_image")
                }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Product Title
        Text(
            text = product.name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics {
                testTag("product_title")
            }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Product Price
        Text(
            text = "R$ ${String.format("%.2f", product.price)}",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics {
                testTag("product_price")
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Seller Info
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.products_seller),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = product.seller?.name ?: "Vendedor não informado",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Product Description
        if (product.description != null) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.products_description),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = product.description,
                        style = MaterialTheme.typography.bodyLarge
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
                    contentDescription(AccessibilityStrings.addToCart(product.name))
                    testTag("add_to_cart_button")
                }
        ) {
            Text(stringResource(R.string.products_add_to_cart))
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
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics {
                contentDescription(AccessibilityStrings.errorState())
                testTag("error_title")
            }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
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
                }
            ) {
                Text(stringResource(R.string.ui_retry))
            }
            
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.semantics {
                    contentDescription(AccessibilityStrings.CANCEL_BUTTON)
                    testTag("dismiss_button")
                }
            ) {
                Text(stringResource(R.string.ui_ok))
            }
        }
    }
}