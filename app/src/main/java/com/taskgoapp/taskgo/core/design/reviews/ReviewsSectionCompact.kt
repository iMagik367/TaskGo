package com.taskgoapp.taskgo.core.design.reviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.taskgoapp.taskgo.core.model.ReviewType
import com.taskgoapp.taskgo.core.theme.*
import com.taskgoapp.taskgo.feature.reviews.presentation.ReviewsViewModel

/**
 * Componente compacto de avaliações para telas de detalhes
 */
@Composable
fun ReviewsSectionCompact(
    targetId: String,
    type: ReviewType,
    onNavigateToReviews: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: ReviewsViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(targetId, type) {
        viewModel.loadReviews(targetId, type)
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onNavigateToReviews() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Avaliações",
                        style = FigmaProductName,
                        color = TaskGoTextBlack,
                        fontWeight = FontWeight.Bold
                    )
                    if (uiState.summary.totalReviews > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RatingStarsDisplay(
                                rating = uiState.summary.averageRating,
                                starSize = 16.dp,
                                showRating = true
                            )
                            Text(
                                text = "(${uiState.summary.totalReviews})",
                                style = FigmaProductDescription,
                                color = TaskGoTextGray
                            )
                        }
                    } else {
                        Text(
                            text = "Nenhuma avaliação ainda",
                            style = FigmaProductDescription,
                            color = TaskGoTextGray
                        )
                    }
                }
                
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Ver todas as avaliações",
                    tint = TaskGoTextGray
                )
            }
            
            // Mostrar algumas avaliações recentes
            if (uiState.reviews.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.reviews.take(2).forEach { review ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = review.reviewerName,
                                    style = FigmaStatusText,
                                    color = TaskGoTextBlack,
                                    fontWeight = FontWeight.Medium
                                )
                                review.comment?.let { comment ->
                                    Text(
                                        text = comment.take(80) + if (comment.length > 80) "..." else "",
                                        style = FigmaProductDescription,
                                        color = TaskGoTextGray,
                                        maxLines = 2
                                    )
                                }
                            }
                            RatingStarsDisplay(
                                rating = review.rating.toDouble(),
                                starSize = 12.dp,
                                showRating = false
                            )
                        }
                        if (review != uiState.reviews.take(2).last()) {
                            HorizontalDivider(color = TaskGoDivider)
                        }
                    }
                }
            }
        }
    }
}

