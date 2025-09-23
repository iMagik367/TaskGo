package br.com.taskgo.taskgo.feature.services.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.taskgoapp.core.design.AppTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesScreen(
    onBackClick: (() -> Unit)? = null,
    onNavigateToCreateWorkOrder: () -> Unit,
    onNavigateToProposals: () -> Unit,
    onNavigateToMyServices: () -> Unit
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Serviços",
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Explore serviços, crie ordens e gerencie propostas.",
                style = MaterialTheme.typography.bodyLarge
            )

            // Minimal placeholder. The feature-specific lists/cards can be reinstated here.
        }
    }
}


