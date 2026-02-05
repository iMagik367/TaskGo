package com.taskgoapp.taskgo.feature.reviews.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.taskgoapp.taskgo.core.design.AppTopBar
import com.taskgoapp.taskgo.core.design.reviews.RatingStars
import com.taskgoapp.taskgo.core.model.ReviewType
import com.taskgoapp.taskgo.core.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReviewScreen(
    targetId: String,
    type: ReviewType,
    targetName: String,
    orderId: String? = null,
    onNavigateBack: () -> Unit,
    onReviewCreated: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: CreateReviewViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    
    var rating by remember { mutableStateOf(0) }
    var comment by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        viewModel.initialize(targetId, type, orderId)
    }
    
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Avaliar $targetName",
                onBackClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Informações do target
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = TaskGoBackgroundWhite
                ),
                border = BorderStroke(1.dp, TaskGoBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = targetName,
                        style = FigmaProductName,
                        color = TaskGoTextBlack,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when (type) {
                            ReviewType.PRODUCT -> "Produto"
                            ReviewType.SERVICE -> "Serviço"
                            ReviewType.PARTNER -> "Parceiro"
                        },
                        style = FigmaProductDescription,
                        color = TaskGoTextGray
                    )
                }
            }
            
            // Seleção de estrelas
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = TaskGoBackgroundWhite
                ),
                border = BorderStroke(1.dp, TaskGoBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Como foi sua experiência?",
                        style = FigmaProductName,
                        color = TaskGoTextBlack,
                        fontWeight = FontWeight.Bold
                    )
                    
                    RatingStars(
                        rating = rating,
                        onRatingChange = { rating = it },
                        enabled = true,
                        starSize = 48.dp
                    )
                    
                    if (rating > 0) {
                        Text(
                            text = when (rating) {
                                1 -> "Péssimo"
                                2 -> "Ruim"
                                3 -> "Regular"
                                4 -> "Bom"
                                5 -> "Excelente"
                                else -> ""
                            },
                            style = FigmaProductDescription,
                            color = TaskGoTextGray
                        )
                    }
                }
            }
            
            // Campo de comentário
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = TaskGoBackgroundWhite
                ),
                border = BorderStroke(1.dp, TaskGoBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Compartilhe sua opinião (opcional)",
                        style = FigmaProductName,
                        color = TaskGoTextBlack,
                        fontWeight = FontWeight.Medium
                    )
                    
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = "Conte mais sobre sua experiência...",
                                color = TaskGoTextGray
                            )
                        },
                        minLines = 4,
                        maxLines = 8,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TaskGoGreen,
                            unfocusedBorderColor = TaskGoBorder
                        )
                    )
                }
            }
            
            // Badge de compra verificada (se aplicável)
            if (orderId != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = TaskGoBackgroundWhite
                    ),
                    border = BorderStroke(1.dp, TaskGoBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = TaskGoGreen
                        )
                        Text(
                            text = "Compra verificada",
                            style = FigmaProductDescription,
                            color = TaskGoGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Botão de enviar
            Button(
                onClick = {
                    if (rating > 0) {
                        viewModel.createReview(rating, comment.takeIf { it.isNotBlank() })
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = rating > 0 && !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TaskGoGreen
                )
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "Enviar Avaliação",
                        style = FigmaButtonText,
                        color = Color.White
                    )
                }
            }
            
            // Mensagem de erro
            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = FigmaProductDescription
                )
            }
        }
    }
    
    // Navegar de volta quando avaliação for criada
    LaunchedEffect(uiState.reviewCreated) {
        if (uiState.reviewCreated) {
            onReviewCreated()
        }
    }
}

