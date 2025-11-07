package com.taskgoapp.taskgo.feature.checkout.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.taskgoapp.taskgo.core.design.AppTopBar
import com.taskgoapp.taskgo.core.design.PrimaryButton
import com.taskgoapp.taskgo.core.theme.FigmaProductDescription
import com.taskgoapp.taskgo.core.theme.FigmaProductName
import com.taskgoapp.taskgo.core.theme.TaskGoTextBlack
import com.taskgoapp.taskgo.core.theme.TaskGoTextGray

@Composable
fun CardDetailsScreen(
    onPaymentSuccess: () -> Unit,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppTopBar(title = "Dados do Cartão", onBackClick = onBackClick)

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Digite os dados do cartão", style = FigmaProductName, color = TaskGoTextBlack)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Número, nome, validade e CVV", style = FigmaProductDescription, color = TaskGoTextGray)

        Spacer(modifier = Modifier.height(24.dp))

        PrimaryButton(
            text = "Pagar com cartão",
            onClick = onPaymentSuccess,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

