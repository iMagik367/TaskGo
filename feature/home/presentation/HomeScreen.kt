package br.com.taskgo.feature.home.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.taskgo.feature.auth.presentation.AuthViewModel

@Composable
fun HomeScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onSignOut: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Bem-vindo, ${state.userData?.name}!",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Button(
            onClick = {
                viewModel.signOut()
                onSignOut()
            }
        ) {
            Text("Sair")
        }
    }
}