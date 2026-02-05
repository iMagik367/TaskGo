package com.taskgoapp.taskgo.feature.checkout.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taskgoapp.taskgo.core.design.PrimaryButton
import com.taskgoapp.taskgo.core.theme.FigmaPrice
import com.taskgoapp.taskgo.core.theme.FigmaProductDescription
import com.taskgoapp.taskgo.core.theme.FigmaProductName
import com.taskgoapp.taskgo.core.theme.FigmaStatusText
import com.taskgoapp.taskgo.core.theme.TaskGoBackgroundWhite
import com.taskgoapp.taskgo.core.theme.TaskGoBorder
import com.taskgoapp.taskgo.core.theme.TaskGoGreen
import com.taskgoapp.taskgo.core.theme.TaskGoTextBlack
import com.taskgoapp.taskgo.core.theme.TaskGoTextGray
import com.taskgoapp.taskgo.feature.checkout.presentation.CheckoutCartItem

@Composable
fun SelectedFieldCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = TaskGoBackgroundWhite
        ),
        border = BorderStroke(1.dp, TaskGoBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, style = FigmaProductName, color = TaskGoTextBlack)
                TextButton(onClick = onAction) {
                    Text("Alterar")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TaskGoGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = value, style = FigmaProductName, color = TaskGoTextBlack)
                    Text(text = subtitle, style = FigmaProductDescription, color = TaskGoTextGray)
                }
            }
        }
    }
}

@Composable
fun SummaryCard(
    subtotal: Double,
    shipping: Double,
    total: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = TaskGoBackgroundWhite
        ),
        border = BorderStroke(1.dp, TaskGoBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Resumo do pedido", style = FigmaProductName, color = TaskGoTextBlack)
            SummaryRow("Subtotal", subtotal)
            SummaryRow("Frete", shipping, highlightFree = true)
            HorizontalDivider()
            SummaryRow("Total", total, prominent = true)
        }
    }
}

@Composable
fun SummaryRow(
    label: String,
    value: Double,
    highlightFree: Boolean = false,
    prominent: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = if (prominent) FigmaProductName else FigmaProductDescription,
            color = TaskGoTextGray
        )
        Text(
            text = if (highlightFree && value == 0.0) "Grátis" else "R$ %.2f".format(value),
            style = if (prominent) FigmaPrice else FigmaProductDescription,
            color = when {
                prominent -> TaskGoTextBlack
                highlightFree && value == 0.0 -> TaskGoGreen
                else -> TaskGoTextBlack
            },
            fontWeight = if (prominent) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun CheckoutCartItemRow(item: CheckoutCartItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = FigmaProductDescription,
                color = TaskGoTextBlack
            )
            Text(
                text = "Qtd: ${item.quantity}",
                style = FigmaStatusText,
                color = TaskGoTextGray
            )
        }
        Text(
            text = "R$ %.2f".format(item.price * item.quantity),
            style = FigmaProductDescription,
            color = TaskGoTextBlack,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun EmptyCheckoutState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = null,
                tint = TaskGoTextGray,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Seu carrinho está vazio",
                style = FigmaProductName,
                color = TaskGoTextGray
            )
        }
    }
}

@Composable
fun CheckoutBottomBar(
    total: Double,
    enabled: Boolean,
    isLoading: Boolean,
    onCheckout: () -> Unit
) {
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Total", style = FigmaProductDescription, color = TaskGoTextGray)
                Text(
                    text = "R$ %.2f".format(total),
                    style = FigmaProductName,
                    color = TaskGoTextBlack,
                    fontWeight = FontWeight.Bold
                )
            }
            PrimaryButton(
                text = if (isLoading) "Processando..." else "Continuar",
                enabled = enabled && !isLoading,
                onClick = onCheckout,
                modifier = Modifier.height(48.dp)
            )
        }
    }
}

