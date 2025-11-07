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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskgoapp.taskgo.core.design.TGIcons
import com.taskgoapp.taskgo.core.theme.TaskGoGreen
import com.taskgoapp.taskgo.core.theme.TaskGoTextBlack
import com.taskgoapp.taskgo.core.theme.TaskGoTextGray
import com.taskgoapp.taskgo.core.theme.TaskGoBackgroundWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProductScreen(
    productId: String,
    onBackClick: () -> Unit,
    onProductUpdated: () -> Unit,
    onProductDeleted: () -> Unit
) {
    // Dados vêm do Firestore - iniciar vazios
    var productName by remember { mutableStateOf("") }
    var productPrice by remember { mutableStateOf("") }
    var productDescription by remember { mutableStateOf("") }
    var selectedImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var isUpdating by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TaskGoBackgroundWhite)
    ) {
        // Top Bar
        TopAppBar(
            title = {
                Text(
                    text = "Editar Produto",
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
            actions = {
                IconButton(
                    onClick = { showDeleteDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Excluir produto",
                        tint = Color(0xFFDC3545)
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
                    
                    Text(
                        text = "Adicione ou substitua as fotos",
                        color = TaskGoTextGray,
                        fontSize = 12.sp
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Grid de imagens
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(selectedImages.size + 1) { index ->
                            if (index < selectedImages.size) {
                                // Imagem existente
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
                onValueChange = { 
                    // Formatar preço
                    val filtered = it.filter { char -> char.isDigit() || char == ',' || char == '.' }
                    productPrice = filtered
                },
                label = { 
                    Text(
                        "Preço",
                        color = TaskGoTextGray,
                        fontSize = 14.sp
                    ) 
                },
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
            
            // Botões de ação
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { onBackClick() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TaskGoTextBlack
                    )
                ) {
                    Text(
                        text = "Cancelar",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Button(
                    onClick = { 
                        isUpdating = true
                        // Simular atualização do produto
                        onProductUpdated()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TaskGoGreen
                    ),
                    enabled = !isUpdating && productName.isNotEmpty() && productPrice.isNotEmpty()
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(
                            text = "Alterar",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
    
    // Dialog de confirmação de exclusão
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = "Excluir Produto",
                    color = TaskGoTextBlack,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Tem certeza que deseja excluir este produto? Esta ação não pode ser desfeita.",
                    color = TaskGoTextGray
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { 
                        showDeleteDialog = false
                        onProductDeleted()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFDC3545)
                    )
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}
