package com.taskgoapp.taskgo.feature.services.presentation

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.taskgoapp.taskgo.core.design.AppTopBar
import com.taskgoapp.taskgo.core.theme.*
import com.taskgoapp.taskgo.data.firestore.models.ServiceFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeusServicosScreen(
    onBackClick: () -> Unit,
    onCriarServico: () -> Unit,
    onEditarServico: (String) -> Unit,
    onViewService: (String) -> Unit = { serviceId -> onEditarServico(serviceId) },
    viewModel: MyServicesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        Log.d("MeusServicosScreen", "=== Iniciando MeusServicosScreen ===")
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Meus Serviços",
                onBackClick = onBackClick
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCriarServico,
                containerColor = TaskGoGreen
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Criar Serviço",
                    tint = Color.White
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = TaskGoGreen)
                    }
                }
                uiState.error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = uiState.error ?: "Erro desconhecido",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.refreshServices() },
                            colors = ButtonDefaults.buttonColors(containerColor = TaskGoGreen)
                        ) {
                            Text("Tentar Novamente")
                        }
                    }
                }
                uiState.services.isEmpty() -> {
                    EmptyServicesState(
                        onCreateService = onCriarServico,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = uiState.services,
                            key = { it.id }
                        ) { service ->
                            ServiceItemCard(
                                service = service,
                                onEditClick = { onEditarServico(service.id) },
                                onDeleteClick = {
                                    viewModel.deleteService(service.id)
                                },
                                onServiceClick = { onViewService(service.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceItemCard(
    service: ServiceFirestore,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onServiceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onServiceClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = service.title.ifEmpty { "Sem título" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TaskGoTextDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "R$ ${String.format("%.2f", service.price)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TaskGoGreen
                    )
                }
                
                // Status chip
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (service.active) 
                        TaskGoGreen.copy(alpha = 0.1f) 
                    else 
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = if (service.active) "Ativo" else "Inativo",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (service.active) TaskGoGreen else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Service image preview
            if (service.images.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(service.images.first())
                            .crossfade(true)
                            .build(),
                        contentDescription = "Imagem do serviço ${service.title}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
            }
            
            Text(
                text = service.description.ifEmpty { "Sem descrição" },
                style = MaterialTheme.typography.bodyMedium,
                color = TaskGoTextDark,
                maxLines = 3
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (service.images.isNotEmpty()) {
                    Text(
                        text = "📷 ${service.images.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TaskGoTextGray
                    )
                }
                if (service.videos.isNotEmpty()) {
                    Text(
                        text = "🎥 ${service.videos.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TaskGoTextGray
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Excluir",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Excluir")
                }
                
                Button(
                    onClick = onEditClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TaskGoGreen
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Editar")
                }
            }
        }
    }
}

@Composable
private fun EmptyServicesState(
    onCreateService: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "📋",
            style = MaterialTheme.typography.displayMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Nenhum serviço cadastrado",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TaskGoTextBlack
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Comece adicionando seu primeiro serviço",
            style = MaterialTheme.typography.bodyMedium,
            color = TaskGoTextGray
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onCreateService,
            colors = ButtonDefaults.buttonColors(containerColor = TaskGoGreen),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Adicionar Serviço")
        }
    }
}
