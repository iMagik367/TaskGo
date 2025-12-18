package com.taskgoapp.taskgo.feature.settings.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.taskgoapp.taskgo.core.design.AppTopBar
import com.taskgoapp.taskgo.core.security.ConsentRecord
import com.taskgoapp.taskgo.core.security.ConsentType
import com.taskgoapp.taskgo.core.security.LGPDComplianceManager
import com.taskgoapp.taskgo.core.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsentHistoryScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val lgpdManager = remember(context) {
        LGPDComplianceManager(
            context,
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
        )
    }
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    var consentHistory by remember { mutableStateOf<List<ConsentRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    
    val persistentScope = remember {
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
    
    LaunchedEffect(Unit) {
        if (currentUser != null) {
            isLoading = true
            error = null
            persistentScope.launch {
                val result = lgpdManager.getConsentHistory(currentUser.uid)
                result.fold(
                    onSuccess = { history ->
                        consentHistory = history
                        isLoading = false
                    },
                    onFailure = { e ->
                        error = "Erro ao carregar histórico: ${e.message}"
                        isLoading = false
                    }
                )
            }
        } else {
            error = "Usuário não autenticado"
            isLoading = false
        }
    }
    
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Histórico de Consentimentos",
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Erro",
                        tint = TaskGoError,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = error ?: "Erro desconhecido",
                        color = TaskGoError,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            consentHistory.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Sem histórico",
                        tint = TaskGoTextGray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Nenhum consentimento registrado",
                        color = TaskGoTextGray,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "Seus consentimentos de dados",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TaskGoTextDark,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Total: ${consentHistory.size} registro(s)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TaskGoTextGray,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                    
                    items(consentHistory) { consent ->
                        ConsentHistoryCard(consent = consent)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsentHistoryCard(
    consent: ConsentRecord,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val dateString = dateFormat.format(Date(consent.timestamp))
    
    val consentTypeLabel = when (consent.type) {
        ConsentType.ANALYTICS -> "Analytics"
        ConsentType.MARKETING -> "Marketing"
        ConsentType.PERSONALIZATION -> "Personalização"
        ConsentType.LOCATION -> "Localização"
        ConsentType.CAMERA -> "Câmera"
        ConsentType.CONTACTS -> "Contatos"
        ConsentType.STORAGE -> "Armazenamento"
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (consent.granted) {
                TaskGoGreen.copy(alpha = 0.1f)
            } else {
                TaskGoError.copy(alpha = 0.1f)
            }
        )
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
                Text(
                    text = consentTypeLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TaskGoTextDark
                )
                
                Surface(
                    color = if (consent.granted) TaskGoGreen else TaskGoError,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (consent.granted) "Aceito" else "Negado",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            
            if (consent.purpose.isNotBlank()) {
                Text(
                    text = "Finalidade: ${consent.purpose}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TaskGoTextGray
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = "Data",
                    tint = TaskGoTextGray,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.bodySmall,
                    color = TaskGoTextGray
                )
            }
        }
    }
}

