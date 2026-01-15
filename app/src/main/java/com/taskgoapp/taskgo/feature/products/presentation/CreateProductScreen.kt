package com.taskgoapp.taskgo.feature.products.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.taskgoapp.taskgo.core.design.TGIcons
import com.taskgoapp.taskgo.core.security.DocumentVerificationBlock
import com.taskgoapp.taskgo.core.security.DocumentVerificationManager
import com.taskgoapp.taskgo.core.theme.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CreateProductViewModel @Inject constructor(
    private val documentVerificationManager: DocumentVerificationManager
) : ViewModel() {
    private val _isVerified = MutableStateFlow(false)
    val isVerified: StateFlow<Boolean> = _isVerified.asStateFlow()
    
    init {
        viewModelScope.launch {
            _isVerified.value = documentVerificationManager.hasDocumentsVerified()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProductScreen(
    onBackClick: () -> Unit,
    onProductCreated: () -> Unit,
    onNavigateToIdentityVerification: () -> Unit = {},
    viewModel: CreateProductViewModel = hiltViewModel()
) {
    val isVerified by viewModel.isVerified.collectAsState()
    
    DocumentVerificationBlock(
        isVerified = isVerified,
        onVerifyClick = onNavigateToIdentityVerification
    ) {
        var productName by remember { mutableStateOf("") }
        var productPrice by remember { mutableStateOf("") }
        var productDescription by remember { mutableStateOf("") }
        var selectedImages by remember { mutableStateOf<List<String>>(emptyList()) }
        var isCreating by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(TaskGoBackgroundWhite)
        ) {
            // Top Bar
            TopAppBar(
                title = {
                    Text(
                        text = "Criar Produto",
                        color = TaskGoTextBlack,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = TaskGoTextBlack
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TaskGoBackgroundWhite
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Seção de fotos
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Fotos do Produto",
                            color = TaskGoTextBlack,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        if (selectedImages.isEmpty()) {
                            // Placeholder para adicionar fotos
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .border(
                                        width = 2.dp,
                                        color = Color(0xFFD9D9D9),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { 
                                        // TODO: Implementar seleção de imagens
                                        selectedImages = listOf("image1", "image2", "image3")
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CameraAlt,
                                        contentDescription = "Adicionar foto",
                                        tint = TaskGoTextGray,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Adicione fotos",
                                        color = TaskGoTextGray,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        } else {
                            // Grid de imagens selecionadas
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(selectedImages.size + 1) { index ->
                                    if (index < selectedImages.size) {
                                        // Imagem selecionada
                                        Box(
                                            modifier = Modifier
                                                .size(80.dp)
                                                .background(
                                                    color = Color(0xFFF0F0F0),
                                                    shape = RoundedCornerShape(8.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Foto ${index + 1}",
                                                color = TaskGoTextGray,
                                                fontSize = 10.sp
                                            )
                                        }
                                    } else {
                                        // Botão para adicionar mais fotos
                                        Box(
                                            modifier = Modifier
                                                .size(80.dp)
                                                .border(
                                                    width = 2.dp,
                                                    color = Color(0xFFD9D9D9),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable { 
                                                    // TODO: Implementar seleção de mais imagens
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Add,
                                                contentDescription = "Adicionar mais fotos",
                                                tint = TaskGoTextGray,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Campo Nome do Produto
                OutlinedTextField(
                    value = productName,
                    onValueChange = { productName = it },
                    label = { 
                        Text(
                            "Nome do Produto",
                            color = TaskGoTextGray,
                            fontSize = 14.sp
                        ) 
                    },
                    placeholder = { Text("Ex: Furadeira") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TaskGoGreen,
                        unfocusedBorderColor = Color(0xFFD9D9D9),
                        focusedLabelColor = TaskGoTextGray,
                        unfocusedLabelColor = TaskGoTextGray,
                        cursorColor = TaskGoGreen
                    ),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Text)
                )
                
                // Campo Preço
                OutlinedTextField(
                    value = productPrice,
                    onValueChange = { newValue ->
                        productPrice = com.taskgoapp.taskgo.core.utils.TextFormatters.formatPrice(newValue)
                    },
                    label = { 
                        Text(
                            "Preço",
                            color = TaskGoTextGray,
                            fontSize = 14.sp
                        ) 
                    },
                    placeholder = { Text("Ex: 250,00") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TaskGoGreen,
                        unfocusedBorderColor = Color(0xFFD9D9D9),
                        focusedLabelColor = TaskGoTextGray,
                        unfocusedLabelColor = TaskGoTextGray,
                        cursorColor = TaskGoGreen
                    ),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                
                // Campo Descrição
                OutlinedTextField(
                    value = productDescription,
                    onValueChange = { productDescription = it },
                    label = { 
                        Text(
                            "Descrição",
                            color = TaskGoTextGray,
                            fontSize = 14.sp
                        ) 
                    },
                    placeholder = { Text("Descreva o produto...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TaskGoGreen,
                        unfocusedBorderColor = Color(0xFFD9D9D9),
                        focusedLabelColor = TaskGoTextGray,
                        unfocusedLabelColor = TaskGoTextGray,
                        cursorColor = TaskGoGreen
                    ),
                    maxLines = 4
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Botão Criar Produto
                Button(
                    onClick = { 
                        isCreating = true
                        // Simular criação do produto
                        onProductCreated()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TaskGoGreen
                    ),
                    enabled = !isCreating && productName.isNotEmpty() && productPrice.isNotEmpty()
                ) {
                    if (isCreating) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            text = "Criar Produto",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
