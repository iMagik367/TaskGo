package com.taskgoapp.taskgo.feature.checkout.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.taskgoapp.taskgo.core.design.PrimaryButton
import com.taskgoapp.taskgo.core.theme.FigmaSectionTitle
import com.taskgoapp.taskgo.core.theme.FigmaStatusText
import com.taskgoapp.taskgo.core.theme.TaskGoGreen
import com.taskgoapp.taskgo.core.theme.TaskGoPriceDark
import com.taskgoapp.taskgo.core.theme.TaskGoTextGray

@Composable
fun PaymentSuccessScreen(
    totalAmount: Double,
    orderId: String,
    trackingCode: String?,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = TaskGoGreen,
            modifier = Modifier.height(96.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Pedido confirmado!",
            style = FigmaSectionTitle,
            color = TaskGoPriceDark
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Código: $orderId  •  Total: R$ %.2f".format(totalAmount),
            style = FigmaStatusText,
            color = TaskGoTextGray
        )
        if (!trackingCode.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Rastreio: $trackingCode",
                style = FigmaStatusText,
                color = TaskGoTextGray
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        PrimaryButton(
            text = "OK",
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

