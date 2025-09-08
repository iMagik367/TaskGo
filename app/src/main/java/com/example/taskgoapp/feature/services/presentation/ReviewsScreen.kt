package com.example.taskgoapp.feature.services.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.taskgoapp.R
import com.example.taskgoapp.core.design.*
import com.example.taskgoapp.core.data.models.Review
import com.example.taskgoapp.core.data.models.Provider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewsScreen(
    onBackClick: () -> Unit,
    onProposalAccepted: (Long) -> Unit
) {
    val reviews = remember { 
        listOf(
            Review(
                id = 1L,
                provider = Provider(
                    id = 1L,
                    name = "Carlos Amaral",
                    profession = "Montador de Móveis",
                    rating = 4.9,
                    reviewCount = 250,
                    serviceCount = "100+ serviços",
                    city = "São Paulo"
                ),
                rating = 5.0,
                comment = "Excelente trabalho, muito profissional e rápido!",
                reviewer = com.example.taskgoapp.core.data.models.User(
                    id = 1L,
                    name = "João Silva",
                    email = "joao@email.com",
                    phone = "11999999999",
                    accountType = com.example.taskgoapp.core.data.models.AccountType.CLIENT,
                    timeOnTaskGo = "2 anos",
                    rating = 4.8,
                    reviewCount = 156,
                    city = "São Paulo"
                ),
                date = java.time.LocalDateTime.now().minusDays(5)
            ),
            Review(
                id = 2L,
                provider = Provider(
                    id = 2L,
                    name = "Ana Paula",
                    profession = "Arquiteta",
                    rating = 4.7,
                    reviewCount = 180,
                    serviceCount = "80+ serviços",
                    city = "Rio de Janeiro"
                ),
                rating = 4.0,
                comment = "Bom trabalho, mas demorou um pouco mais que o esperado.",
                reviewer = com.example.taskgoapp.core.data.models.User(
                    id = 2L,
                    name = "Maria Santos",
                    email = "maria@email.com",
                    phone = "11888888888",
                    accountType = com.example.taskgoapp.core.data.models.AccountType.CLIENT,
                    timeOnTaskGo = "1 ano",
                    rating = 4.6,
                    reviewCount = 89,
                    city = "Rio de Janeiro"
                ),
                date = java.time.LocalDateTime.now().minusDays(8)
            )
        )
    }
    
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.reviews_title),
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.reviews_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (reviews.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.reviews_empty),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stringResource(R.string.reviews_empty_message),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(reviews) { review ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = review.provider.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                RatingBar(
                                    rating = review.rating.toFloat(),
                                    showCount = false
                                )
                            }
                                Text(
                                    text = review.provider.profession,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = review.comment,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}


