package com.taskgoapp.taskgo.feature.products.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskgoapp.taskgo.core.design.TGIcons
import com.taskgoapp.taskgo.core.theme.TaskGoGreen
import com.taskgoapp.taskgo.core.theme.TaskGoTextBlack
import com.taskgoapp.taskgo.core.theme.TaskGoTextGray
import com.taskgoapp.taskgo.core.theme.TaskGoBackgroundWhite
import com.taskgoapp.taskgo.core.theme.FigmaSectionTitle
import com.taskgoapp.taskgo.core.theme.FigmaProductDescription
import com.taskgoapp.taskgo.core.theme.FigmaPrice

data class Product(
    val id: String,
    val name: String,
    val price: Double,
    val description: String,
    val imageUrl: String? = null,
    val isActive: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageProductsScreen(
    onBackClick: () -> Unit,
    onCreateProduct: () -> Unit,
    onEditProduct: (String) -> Unit,
    onDeleteProduct: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    // Lista vazia - dados vêm do Firestore
    val products = remember { emptyList<Product>() }
    
    val filteredProducts = remember(products, searchQuery) {
        if (searchQuery.isEmpty()) {
            products
        } else {
            products.filter { 
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TaskGoBackgroundWhite)
    ) {
        // Top Bar
        TopAppBar(
            title = {
                Text(
                    text = "Gerenciar Produtos",
                    style = FigmaSectionTitle,
                    color = TaskGoTextBlack
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
                IconButton(onClick = onCreateProduct) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Criar produto",
                        tint = TaskGoGreen
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Barra de pesquisa
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar produtos...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Buscar",
                        tint = TaskGoTextGray
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TaskGoGreen,
                    unfocusedBorderColor = Color(0xFFD9D9D9),
                    cursorColor = TaskGoGreen
                )
            )
            
            // Título da seção
            Text(
                text = "Gerencie seus produtos",
                style = FigmaProductDescription,
                color = TaskGoTextBlack
            )
            
            // Lista de produtos
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Nenhum produto cadastrado",
                            color = TaskGoTextGray,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Crie seu primeiro produto para começar",
                            color = TaskGoTextGray,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredProducts) { product ->
                        ProductCard(
                            product = product,
                            onEditClick = { onEditProduct(product.id) },
                            onDeleteClick = { onDeleteProduct(product.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header com nome e status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = product.name,
                    style = FigmaSectionTitle,
                    color = TaskGoTextBlack
                )
                
                // Status badge
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (product.isActive) Color(0xFFD4EDDA) else Color(0xFFF8D7DA)
                    )
                ) {
                    Text(
                        text = if (product.isActive) "Ativo" else "Inativo",
                        color = if (product.isActive) Color(0xFF155724) else Color(0xFF721C24),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Preço
            Text(
                text = "R$ ${String.format("%.2f", product.price)}",
                style = FigmaPrice,
                color = TaskGoGreen
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Descrição
            Text(
                text = product.description,
                style = FigmaProductDescription,
                color = TaskGoTextGray
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Botões de ação
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFDC3545)
                    )
                ) {
                    Text(
                        text = "Excluir",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Button(
                    onClick = onEditClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TaskGoGreen
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Editar",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Editar",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

