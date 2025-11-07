package com.taskgoapp.taskgo.feature.services.presentation
import com.taskgoapp.taskgo.core.theme.*

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taskgoapp.taskgo.core.design.AppTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvaliarPrestadorScreen(
    onBackClick: () -> Unit
) {
    var rating by remember { mutableStateOf(5) }
    var review by remember { mutableStateOf("Excelente trabalho montando a estante!") }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Avaliar Prestador",
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Provider info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Rodrigo Silva",
                    style = FigmaProductName,
                    color = TaskGoTextBlack,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Montador de Móveis",
                    style = FigmaProductDescription,
                    color = TaskGoTextGray
                )
            }

            // Rating stars
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(5) { index ->
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Estrela ${index + 1}",
                        modifier = Modifier.size(32.dp),
                        tint = if (index < rating) TaskGoStarYellow else Color.Gray
                    )
                }
            }

            // Review text field
            OutlinedTextField(
                value = review,
                onValueChange = { review = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 6,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TaskGoDivider,
                    unfocusedBorderColor = TaskGoDivider
                )
            )

            Spacer(modifier = Modifier.weight(1f))

            // Send button
            Button(
                onClick = { /* TODO: Enviar avaliação */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TaskGoGreen
                )
            ) {
                Text(
                    text = "Enviar",
                    style = FigmaButtonText,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
