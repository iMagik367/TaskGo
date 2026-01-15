package com.taskgoapp.taskgo.core.design

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.taskgoapp.taskgo.core.theme.TaskGoGreen

/**
 * Botão flutuante expansível que exibe ações rápidas
 * Quando fechado: círculo verde
 * Quando aberto: retângulo verde vertical com ícones de ações
 */
@Composable
fun ExpandingFAB(
    onMessagesClick: () -> Unit,
    onSearchClick: () -> Unit,
    onCartClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    // Animações
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 45f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "rotation"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isExpanded) 0.8f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    Box(
        modifier = modifier
            .padding(16.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        // Botão expandido (retângulo vertical com ícones)
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) + fadeIn(),
            exit = shrinkVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(TaskGoGreen)
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Mensagens (topo)
                IconButton(
                    onClick = {
                        isExpanded = false
                        onMessagesClick()
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    TGIcon(
                        iconRes = TGIcons.Messages,
                        contentDescription = "Mensagens",
                        size = TGIcons.Sizes.Medium,
                        tint = Color.White
                    )
                }
                
                // Busca
                IconButton(
                    onClick = {
                        isExpanded = false
                        onSearchClick()
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    TGIcon(
                        iconRes = TGIcons.Search,
                        contentDescription = "Busca",
                        size = TGIcons.Sizes.Medium,
                        tint = Color.White
                    )
                }
                
                // Carrinho
                IconButton(
                    onClick = {
                        isExpanded = false
                        onCartClick()
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    TGIcon(
                        iconRes = TGIcons.Cart,
                        contentDescription = "Carrinho",
                        size = TGIcons.Sizes.Medium,
                        tint = Color.White
                    )
                }
                
                // Notificações (base)
                IconButton(
                    onClick = {
                        isExpanded = false
                        onNotificationsClick()
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    TGIcon(
                        iconRes = TGIcons.Bell,
                        contentDescription = "Notificações",
                        size = TGIcons.Sizes.Medium,
                        tint = Color.White
                    )
                }
            }
        }
        
        // Botão principal (círculo verde) - aparece quando fechado
        AnimatedVisibility(
            visible = !isExpanded,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(TaskGoGreen)
                    .clickable { isExpanded = true }
            )
        }
    }
}

