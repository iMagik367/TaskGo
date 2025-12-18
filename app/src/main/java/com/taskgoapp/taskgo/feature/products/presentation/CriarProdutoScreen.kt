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
import com.taskgoapp.taskgo.core.security.DocumentVerificationBlock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CriarProdutoScreen(
    onBackClick: () -> Unit,
    onProductCreated: () -> Unit,
    onNavigateToIdentityVerification: () -> Unit = {},
    viewModel: ProductFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isVerified by viewModel.isVerified.collectAsState()
    
    var nome by remember { mutableStateOf("") }
    var preco by remember { mutableStateOf("") }
    var endereco by remember { mutableStateOf("") }
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var showSuccessMessage by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Criar Produto",
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            DocumentVerificationBlock(
                isVerified = isVerified,
                onVerifyClick = onNavigateToIdentityVerification
            ) {
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

                    // Nome do produto
                    Column {
                        Text(
                            text = "Nome do produto",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = nome,
                            onValueChange = { nome = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("Ex: Furadeira") }
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
                            prefix = { Text("R$ ") },
                            placeholder = { Text("250,00") }
                        )
                    }

                    // Endereço
                    Column {
                        Text(
                            text = "Endereço",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = endereco,
                            onValueChange = { endereco = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("Rua das Palmeiras, 123 São Paulo, SP") }
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Botão criar produto
                    Button(
                        onClick = { 
                            viewModel.onTitleChange(nome)
                            viewModel.onPriceChange(preco)
                            viewModel.onDescriptionChange("")
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
                        ),
                        enabled = nome.isNotBlank() && preco.isNotBlank() && endereco.isNotBlank()
                    ) {
                        Text(
                            text = "Criar Produto",
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
                    text = { Text("Produto criado com sucesso!") },
                    confirmButton = {
                        TextButton(onClick = { 
                            showSuccessMessage = false
                            onProductCreated()
                        }) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }
}
