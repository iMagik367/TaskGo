package com.taskgoapp.taskgo.feature.reviews.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.taskgoapp.taskgo.core.design.AppTopBar
import com.taskgoapp.taskgo.core.design.reviews.ReviewCard
import com.taskgoapp.taskgo.core.design.reviews.ReviewSummaryCard
import com.taskgoapp.taskgo.core.model.ReviewType
import com.taskgoapp.taskgo.core.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewsScreen(
    targetId: String,
    type: ReviewType,
    targetName: String,
    onNavigateBack: () -> Unit,
    onNavigateToCreateReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: ReviewsViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(targetId, type) {
        viewModel.loadReviews(targetId, type)
    }
    
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Avaliações - $targetName",
                onBackClick = onNavigateBack
            )
        },
        floatingActionButton = {
            if (uiState.canReview) {
                FloatingActionButton(
                    onClick = onNavigateToCreateReview,
                    containerColor = TaskGoGreen
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Avaliar",
                        tint = androidx.compose.ui.graphics.Color.White
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Resumo de avaliações
            if (uiState.summary.totalReviews > 0) {
                item {
                    ReviewSummaryCard(summary = uiState.summary)
                }
            }
            
            // Lista de avaliações
            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (uiState.reviews.isEmpty()) {
                item {
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
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Nenhuma avaliação ainda",
                                style = FigmaProductName,
                                color = TaskGoTextGray,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Seja o primeiro a avaliar!",
                                style = FigmaProductDescription,
                                color = TaskGoTextGray
                            )
                        }
                    }
                }
            } else {
                items(uiState.reviews) { review ->
                    ReviewCard(
                        review = review,
                        onHelpfulClick = {
                            viewModel.markAsHelpful(review.id)
                        }
                    )
                }
            }
        }
    }
}

