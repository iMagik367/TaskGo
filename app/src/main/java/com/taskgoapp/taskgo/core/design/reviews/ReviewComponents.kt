package com.taskgoapp.taskgo.core.design.reviews

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taskgoapp.taskgo.core.theme.*

/**
 * Componente de estrelas de avaliação interativo
 */
@Composable
fun RatingStars(
    rating: Int,
    onRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    starSize: androidx.compose.ui.unit.Dp = 32.dp
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        (1..5).forEach { index ->
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Estrela $index",
                modifier = Modifier
                    .size(starSize)
                    .then(
                        if (enabled) {
                            Modifier.clickable { onRatingChange(index) }
                        } else {
                            Modifier
                        }
                    ),
                tint = if (index <= rating) Color(0xFFFFD700) else Color.Gray.copy(alpha = 0.3f)
            )
        }
    }
}

/**
 * Componente de estrelas de avaliação apenas para exibição
 */
@Composable
fun RatingStarsDisplay(
    rating: Double,
    modifier: Modifier = Modifier,
    starSize: androidx.compose.ui.unit.Dp = 16.dp,
    showRating: Boolean = true
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val fullStars = rating.toInt()
        val hasHalfStar = rating - fullStars >= 0.5
        
        (1..5).forEach { index ->
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(starSize),
                tint = when {
                    index <= fullStars -> Color(0xFFFFD700)
                    index == fullStars + 1 && hasHalfStar -> Color(0xFFFFD700).copy(alpha = 0.5f)
                    else -> Color.Gray.copy(alpha = 0.3f)
                }
            )
        }
        
        if (showRating) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = String.format("%.1f", rating),
                style = MaterialTheme.typography.bodyMedium,
                color = TaskGoTextGray,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Card de avaliação individual
 */
@Composable
fun ReviewCard(
    review: com.taskgoapp.taskgo.core.model.Review,
    onHelpfulClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Avatar, nome, data e verificação
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(TaskGoSurfaceGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = review.reviewerName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = TaskGoTextGray
                        )
                    }
                    
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = review.reviewerName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = TaskGoTextBlack
                            )
                            if (review.verifiedPurchase) {
                                Badge(
                                    containerColor = TaskGoGreen.copy(alpha = 0.2f),
                                    contentColor = TaskGoGreen
                                ) {
                                    Text(
                                        text = "Compra verificada",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                        Text(
                            text = formatDate(review.createdAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = TaskGoTextGray
                        )
                    }
                }
                
                // Rating
                RatingStarsDisplay(
                    rating = review.rating.toDouble(),
                    starSize = 16.dp,
                    showRating = false
                )
            }
            
            // Comentário
            review.comment?.let { comment ->
                Text(
                    text = comment,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TaskGoTextBlack
                )
            }
            
            // Fotos (se houver)
            if (review.photoUrls.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    review.photoUrls.take(3).forEach { photoUrl ->
                        // Placeholder para imagem - em produção, usar AsyncImage
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(TaskGoSurfaceGray)
                        )
                    }
                }
            }
            
            // Footer: Útil
            if (onHelpfulClick != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onHelpfulClick) {
                        Text(
                            text = "Útil (${review.helpfulCount})",
                            style = MaterialTheme.typography.bodySmall,
                            color = TaskGoTextGray
                        )
                    }
                }
            }
        }
    }
}

/**
 * Resumo de avaliações com distribuição
 */
@Composable
fun ReviewSummaryCard(
    summary: com.taskgoapp.taskgo.core.model.ReviewSummary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Média e total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = String.format("%.1f", summary.averageRating),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = TaskGoTextBlack
                    )
                    RatingStarsDisplay(
                        rating = summary.averageRating,
                        starSize = 20.dp,
                        showRating = false
                    )
                }
                Text(
                    text = "${summary.totalReviews} avaliações",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TaskGoTextGray
                )
            }
            
            // Distribuição de avaliações
            if (summary.ratingDistribution.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    (5 downTo 1).forEach { stars ->
                        val count = summary.ratingDistribution[stars] ?: 0
                        val percentage = if (summary.totalReviews > 0) {
                            (count.toFloat() / summary.totalReviews.toFloat()) * 100f
                        } else 0f
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "$stars",
                                style = MaterialTheme.typography.bodySmall,
                                color = TaskGoTextGray,
                                modifier = Modifier.width(16.dp)
                            )
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(12.dp)
                            )
                            LinearProgressIndicator(
                                progress = { percentage / 100f },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(8.dp),
                                color = Color(0xFFFFD700),
                                trackColor = Color.Gray.copy(alpha = 0.2f)
                            )
                            Text(
                                text = "$count",
                                style = MaterialTheme.typography.bodySmall,
                                color = TaskGoTextGray,
                                modifier = Modifier.width(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "Agora"
        diff < 3_600_000 -> "${diff / 60_000} min atrás"
        diff < 86_400_000 -> "${diff / 3_600_000} h atrás"
        diff < 2_592_000_000 -> "${diff / 86_400_000} dias atrás"
        else -> {
            val date = java.util.Date(timestamp)
            val format = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("pt", "BR"))
            format.format(date)
        }
    }
}

