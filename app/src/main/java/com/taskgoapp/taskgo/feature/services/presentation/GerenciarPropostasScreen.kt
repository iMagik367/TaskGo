package com.taskgoapp.taskgo.feature.services.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taskgoapp.taskgo.core.design.AppTopBar
import com.taskgoapp.taskgo.core.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GerenciarPropostasScreen(
    onBackClick: () -> Unit,
    onVerProposta: (String) -> Unit
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Gerenciar Propostas",
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProposalItem(initials = "RS", titulo = "Montagem de Móveis", pessoa = "Rodrigo Silva", preco = "R$ 150,00", status = "Pendente", onClickVer = { onVerProposta("1") })
            HorizontalDivider(thickness = 0.5.dp, color = TaskGoDivider)
            ProposalItem(initials = "MS", titulo = "Limpeza Residencial", pessoa = "Maria Santos", preco = "R$ 80,00", status = "Aceita", onClickVer = { onVerProposta("2") })
        }
    }
}

@Composable
private fun ProposalItem(
    initials: String,
    titulo: String,
    pessoa: String,
    preco: String,
    status: String,
    onClickVer: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClickVer() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .background(
                    color = TaskGoBackgroundGray,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                style = FigmaProductName,
                color = TaskGoTextBlack,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = titulo, style = FigmaProductName, color = TaskGoTextBlack, fontWeight = FontWeight.Medium)
            Text(text = pessoa, style = FigmaProductDescription, color = TaskGoTextGray)
            Text(text = preco, style = FigmaPrice, color = TaskGoPriceGreen, fontWeight = FontWeight.Bold)
            Text(text = status, style = FigmaStatusText, color = TaskGoGreen)
        }

        OutlinedButton(
            onClick = onClickVer,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent
            ),
            border = BorderStroke(1.dp, TaskGoGreen)
        ) { 
            Text("Ver", style=FigmaButtonText, color=TaskGoGreen) 
        }
    }
}
