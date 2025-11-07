package com.taskgoapp.taskgo.feature.products.presentation

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.taskgoapp.taskgo.core.design.AppTopBar
import com.taskgoapp.taskgo.core.design.ImageEditor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarProdutoScreen(
    onBackClick: () -> Unit,
    viewModel: ProductFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var nome by remember { mutableStateOf("Guarda Roupa") }
    var descricao by remember { mutableStateOf("Guarda-roupa de 3 portas com espelho") }
    var preco by remember { mutableStateOf("750,00") }
    var categoria by remember { mutableStateOf("Móveis") }
    var selectedImageUris by remember { mutableStateOf(listOf<Uri>()) }
    var showSuccessMessage by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Editar Produto",
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
            // Seletor de fotos
            ImageEditor(
                selectedImageUris = selectedImageUris,
                onImagesChanged = { uris -> selectedImageUris = uris },
                maxImages = 5,
                placeholderText = "Adicione fotos do produto"
            )

            // Nome
            Column {
                Text(
                    text = "Nome",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Descrição
            Column {
                Text(
                    text = "Descrição",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = descricao,
                    onValueChange = { descricao = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
            }

            // Preço
            Column {
                Text(
                    text = "Preço",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = preco,
                    onValueChange = { preco = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    prefix = { Text("R$ ") }
                )
            }

            // Categoria
            Column {
                Text(
                    text = "Categoria",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = categoria,
                    onValueChange = { categoria = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Save button
            Button(
                onClick = { 
                    viewModel.onTitleChange(nome)
                    viewModel.onPriceChange(preco)
                    viewModel.onDescriptionChange(descricao)
                    viewModel.onSellerNameChange("Usuário")
                    selectedImageUris.forEach { uri ->
                        viewModel.addImage(uri.toString())
                    }
                    viewModel.save()
                    showSuccessMessage = true
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Salvar Alterações",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // Success message
    if (showSuccessMessage) {
        AlertDialog(
            onDismissRequest = { showSuccessMessage = false },
            title = { Text("Sucesso") },
            text = { Text("Produto salvo com sucesso!") },
            confirmButton = {
                TextButton(onClick = { showSuccessMessage = false }) {
                    Text("OK")
                }
            }
        )
    }
}
