package com.taskgoapp.taskgo.feature.feed.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskgoapp.taskgo.core.theme.*

@Composable
fun RadiusFilterDialog(
    currentRadius: Double,
    onRadiusChanged: (Double) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var sliderValue by remember(currentRadius) { 
        mutableStateOf(currentRadius.toFloat().coerceIn(10f, 100f))
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Raio de Busca",
                style = MaterialTheme.typography.titleLarge,
                color = TaskGoTextBlack
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "${sliderValue.toInt()} km",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = TaskGoGreen
                )
                
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 10f..100f,
                    steps = 8, // Incrementos de 10km (10, 20, 30, ..., 100)
                    colors = SliderDefaults.colors(
                        thumbColor = TaskGoGreen,
                        activeTrackColor = TaskGoGreen
                    )
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "10 km",
                        style = MaterialTheme.typography.bodySmall,
                        color = TaskGoTextGray
                    )
                    Text(
                        text = "100 km",
                        style = MaterialTheme.typography.bodySmall,
                        color = TaskGoTextGray
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onRadiusChanged(sliderValue.toDouble())
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = TaskGoGreen
                )
            ) {
                Text("Aplicar", color = TaskGoBackgroundWhite)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TaskGoTextGray)
            }
        },
        modifier = modifier
    )
}
