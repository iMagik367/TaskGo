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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.taskgoapp.taskgo.core.design.AppTopBar
import com.taskgoapp.taskgo.core.theme.*
import com.taskgoapp.taskgo.data.firestore.models.ServiceFirestore
import com.taskgoapp.taskgo.data.firestore.models.OrderFirestore
import com.taskgoapp.taskgo.feature.home.presentation.FilterTypeChip

enum class ServiceTab {
    SOLICITADOS, EM_ANDAMENTO, CONCLUIDOS, CANCELADOS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeusServicosScreen(
    onBackClick: () -> Unit,
    onCriarServico: () -> Unit,
    onEditarServico: (String) -> Unit,
    onViewService: (String) -> Unit = { serviceId -> onEditarServico(serviceId) },
    onOrderClick: ((String) -> Unit)? = null,
    viewModel: MyServicesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(ServiceTab.SOLICITADOS) }

    LaunchedEffect(Unit) {
        Log.d("MeusServicosScreen", "=== Iniciando MeusServicosScreen ===")
    }

    // Filtrar ordens baseado na aba selecionada
    val filteredOrders = when (selectedTab) {
        ServiceTab.SOLICITADOS -> uiState.orders.filter { order ->
            order.status.lowercase() in listOf("pending", "proposed")
        }
        ServiceTab.EM_ANDAMENTO -> uiState.orders.filter { order ->
            order.status.lowercase() in listOf("accepted", "payment_pending", "paid", "in_progress")
        }
        ServiceTab.CONCLUIDOS -> uiState.orders.filter { order ->
            order.status.lowercase() == "completed"
        }
        ServiceTab.CANCELADOS -> uiState.orders.filter { order ->
            order.status.lowercase() == "cancelled"
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Meus Serviços",
                onBackClick = onBackClick
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Abas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ServiceTabChip(
                    text = "Solicitados",
                    selected = selectedTab == ServiceTab.SOLICITADOS,
                    onClick = { selectedTab = ServiceTab.SOLICITADOS },
                    modifier = Modifier.weight(1f)
                )
                ServiceTabChip(
                    text = "Em Andamento",
                    selected = selectedTab == ServiceTab.EM_ANDAMENTO,
                    onClick = { selectedTab = ServiceTab.EM_ANDAMENTO },
                    modifier = Modifier.weight(1f)
                )
                ServiceTabChip(
                    text = "Concluídos",
                    selected = selectedTab == ServiceTab.CONCLUIDOS,
                    onClick = { selectedTab = ServiceTab.CONCLUIDOS },
                    modifier = Modifier.weight(1f)
                )
                ServiceTabChip(
                    text = "Cancelados",
                    selected = selectedTab == ServiceTab.CANCELADOS,
                    onClick = { selectedTab = ServiceTab.CANCELADOS },
                    modifier = Modifier.weight(1f)
                )
            }

            // Conteúdo
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
                filteredOrders.isEmpty() -> {
                    EmptyOrdersState(
                        tab = selectedTab,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(
                            items = filteredOrders,
                            key = { it.id }
                        ) { order ->
                            ServiceOrderItemCard(
                                order = order,
                                onServiceClick = { 
                                    if (order.serviceId.isNotEmpty()) {
                                        onViewService(order.serviceId)
                                    }
                                },
                                onOrderClick = onOrderClick
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceOrderItemCard(
    order: OrderFirestore,
    onServiceClick: () -> Unit,
    onOrderClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { 
                // Se onOrderClick está disponível, usar para navegar para detalhes da ordem
                // Caso contrário, usar onServiceClick como fallback
                if (onOrderClick != null && order.id.isNotEmpty()) {
                    onOrderClick(order.id)
                } else {
                    onServiceClick()
                }
            },
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
                        text = order.category ?: "Serviço",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TaskGoTextDark
                    )
                    if (order.dueDate != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Prazo: ${order.dueDate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TaskGoTextGray
                        )
                    }
                }
                
                // Status chip
                StatusChip(status = order.status)
            }
            
            Text(
                text = order.details.ifEmpty { "Sem descrição" },
                style = MaterialTheme.typography.bodyMedium,
                color = TaskGoTextDark,
                maxLines = 3
            )
            
            if (order.location.isNotEmpty()) {
                Text(
                    text = "📍 ${order.location}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TaskGoTextGray
                )
            }
            
            if (order.budget > 0) {
                Text(
                    text = "Orçamento: R$ ${String.format("%.2f", order.budget)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = TaskGoGreen
                )
            }
            
            if (order.proposalDetails != null) {
                Text(
                    text = "Proposta: R$ ${String.format("%.2f", order.proposalDetails.price)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = TaskGoGreen
                )
            }
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val (color, backgroundColor, text) = when (status.lowercase()) {
        "pending" -> Triple(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primaryContainer,
            "Pendente"
        )
        "proposed" -> Triple(
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.secondaryContainer,
            "Proposta Enviada"
        )
        "accepted" -> Triple(
            TaskGoGreen,
            TaskGoGreen.copy(alpha = 0.1f),
            "Aceita"
        )
        "payment_pending", "paid" -> Triple(
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.tertiaryContainer,
            "Pagamento"
        )
        "in_progress" -> Triple(
            TaskGoGreen,
            TaskGoGreen.copy(alpha = 0.1f),
            "Em Andamento"
        )
        "completed" -> Triple(
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.tertiaryContainer,
            "Concluído"
        )
        "cancelled" -> Triple(
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.errorContainer,
            "Cancelada"
        )
        else -> Triple(
            TaskGoTextGray,
            TaskGoTextGray.copy(alpha = 0.1f),
            status
        )
    }
    
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
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
private fun ServiceTabChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier.height(40.dp),
        label = {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) TaskGoBackgroundWhite else TaskGoTextBlack,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = TaskGoGreen,
            selectedLabelColor = TaskGoBackgroundWhite,
            containerColor = TaskGoSurfaceGray,
            labelColor = TaskGoTextBlack
        )
    )
}

@Composable
private fun EmptyOrdersState(
    tab: ServiceTab,
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
            text = when (tab) {
                ServiceTab.SOLICITADOS -> "📋"
                ServiceTab.EM_ANDAMENTO -> "⚙️"
                ServiceTab.CONCLUIDOS -> "✅"
                ServiceTab.CANCELADOS -> "❌"
            },
            style = MaterialTheme.typography.displayMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = when (tab) {
                ServiceTab.SOLICITADOS -> "Nenhum serviço solicitado"
                ServiceTab.EM_ANDAMENTO -> "Nenhum serviço em andamento"
                ServiceTab.CONCLUIDOS -> "Nenhum serviço concluído"
                ServiceTab.CANCELADOS -> "Nenhum serviço cancelado"
            },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TaskGoTextBlack
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = when (tab) {
                ServiceTab.SOLICITADOS -> "Aguardando solicitações de clientes"
                ServiceTab.EM_ANDAMENTO -> "Você ainda não tem serviços em andamento"
                ServiceTab.CONCLUIDOS -> "Histórico de serviços concluídos aparecerá aqui"
                ServiceTab.CANCELADOS -> "Serviços cancelados aparecerão aqui"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = TaskGoTextGray
        )
    }
}
