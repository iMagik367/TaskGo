package com.taskgoapp.taskgo.feature.settings.presentation

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.taskgoapp.taskgo.core.design.AppTopBar
import com.taskgoapp.taskgo.core.theme.*
import com.taskgoapp.taskgo.data.local.datastore.PreferencesManager
import com.taskgoapp.taskgo.core.security.LGPDComplianceManager
import com.taskgoapp.taskgo.core.security.PDFExporter
import com.google.firebase.auth.FirebaseAuth
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.taskgoapp.taskgo.data.repository.FirestoreUserRepository
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(
    onBackClick: () -> Unit,
    onNavigateToIdentityVerification: () -> Unit,
    onNavigateToConsentHistory: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {}, // Callback para navegar para login após logout
    viewModel: SecuritySettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    
    // Usar um escopo persistente para operações que precisam continuar após o composable sair
    val persistentScope = remember {
        CoroutineScope(
            SupervisorJob() + Dispatchers.IO
        )
    }
    
    val lgpdManager = remember(context) {
        com.taskgoapp.taskgo.core.security.LGPDComplianceManager(
            context,
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
        )
    }
    val pdfExporter = remember(context) {
        com.taskgoapp.taskgo.core.security.PDFExporter(context)
    }
    
    var showChangeMethodDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showExportMessage by remember { mutableStateOf<String?>(null) }
    var isExporting by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Segurança",
                subtitle = "Autenticação de duas etapas e proteção de dados",
                onBackClick = onBackClick,
                backgroundColor = TaskGoGreen,
                titleColor = Color.White,
                backIconColor = Color.White
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(TaskGoBackgroundWhite)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Verificação de Identidade
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Verificação de Identidade",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TaskGoTextBlack
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Envie seus documentos para verificação",
                                style = MaterialTheme.typography.bodySmall,
                                color = TaskGoTextGray
                            )
                        }
                        IconButton(onClick = onNavigateToIdentityVerification) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Verificar identidade",
                                tint = TaskGoGreen
                            )
                        }
                    }
                }
            }
            
            // Biometria removida - login apenas no primeiro acesso
            
            // Autenticação de Duas Etapas
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Autenticação de Duas Etapas",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TaskGoTextBlack
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Receba um código por email ou telefone ao fazer login",
                                style = MaterialTheme.typography.bodySmall,
                                color = TaskGoTextGray
                            )
                        }
                        Switch(
                            checked = uiState.twoFactorEnabled,
                            onCheckedChange = { viewModel.toggleTwoFactor(it) }
                        )
                    }
                    
                    if (uiState.twoFactorEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Método: ${uiState.twoFactorMethod ?: "Email ou Telefone"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TaskGoTextGray
                            )
                            TextButton(onClick = { showChangeMethodDialog = true }) {
                                Text("Alterar", color = TaskGoGreen)
                            }
                        }
                    }
                }
            }
            
            // Gerenciamento de Proteção de Dados (LGPD)
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Proteção de Dados (LGPD)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TaskGoTextBlack
                    )
                    
                    // Exportar dados
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Exportar Meus Dados",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = TaskGoTextBlack
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Baixe uma cópia dos seus dados",
                                style = MaterialTheme.typography.bodySmall,
                                color = TaskGoTextGray
                            )
                        }
                        TextButton(
                            onClick = {
                                if (currentUser != null) {
                                    isExporting = true
                                    persistentScope.launch {
                                        try {
                                            // Primeiro, obter os dados
                                            val dataResult = lgpdManager.exportUserData(currentUser.uid)
                                            dataResult.fold(
                                                onSuccess = { export ->
                                                    // Gerar PDF
                                                    val pdfResult = pdfExporter.exportUserDataToPDF(
                                                        currentUser.uid,
                                                        export.data
                                                    )
                                                    pdfResult.fold(
                                                        onSuccess = { pdfFile ->
                                                            // Compartilhar o arquivo
                                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                                val uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                                                    FileProvider.getUriForFile(
                                                                        context,
                                                                        "${context.packageName}.fileprovider",
                                                                        pdfFile
                                                                    )
                                                                } else {
                                                                    Uri.fromFile(pdfFile)
                                                                }
                                                                setDataAndType(uri, "application/pdf")
                                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                            }
                                                            context.startActivity(Intent.createChooser(intent, "Abrir PDF com"))
                                                            
                                                            showExportMessage = "PDF exportado com sucesso!\nLocalização: ${pdfFile.absolutePath}"
                                                            isExporting = false
                                                        },
                                                        onFailure = { error ->
                                                            showExportMessage = "Erro ao gerar PDF: ${error.message}"
                                                            isExporting = false
                                                        }
                                                    )
                                                },
                                                onFailure = { error ->
                                                    showExportMessage = "Erro ao obter dados: ${error.message}"
                                                    isExporting = false
                                                    // Tratar erro específico do Secure Token API bloqueado
                                                    if (error.message?.contains("SecureToken") == true || error.message?.contains("securetoken") == true) {
                                                        android.util.Log.w("SecuritySettingsScreen", "Firebase Secure Token API bloqueado. Verifique configurações do Google Cloud.")
                                                    }
                                                }
                                            )
                                        } catch (e: Exception) {
                                            showExportMessage = "Erro: ${e.message}"
                                            isExporting = false
                                            if (e.message?.contains("SecureToken") == true || e.message?.contains("securetoken") == true) {
                                                android.util.Log.w("SecuritySettingsScreen", "Firebase Secure Token API bloqueado. Verifique configurações do Google Cloud.")
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = !isExporting && currentUser != null
                        ) {
                            if (isExporting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            } else {
                                Text("Exportar", color = TaskGoGreen)
                            }
                        }
                    }
                    
                    HorizontalDivider()
                    
                    // Excluir conta
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Excluir Minha Conta",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = TaskGoError
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Remover permanentemente todos os seus dados",
                                style = MaterialTheme.typography.bodySmall,
                                color = TaskGoTextGray
                            )
                        }
                        TextButton(onClick = { showDeleteConfirmation = true }) {
                            Text("Excluir", color = TaskGoError)
                        }
                    }
                    
                    HorizontalDivider()
                    
                    // Histórico de consentimentos
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Histórico de Consentimentos",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = TaskGoTextBlack
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Visualize seus consentimentos de dados",
                                style = MaterialTheme.typography.bodySmall,
                                color = TaskGoTextGray
                            )
                        }
                        IconButton(onClick = onNavigateToConsentHistory) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Ver histórico",
                                tint = TaskGoGreen
                            )
                        }
                    }
                }
            }
            
            // Informações
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE3F2FD)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = TaskGoGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Dicas de Segurança",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TaskGoTextBlack
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Mantenha seus dados atualizados\n• Use senhas fortes\n• Ative a autenticação de duas etapas\n• Verifique sua identidade\n• Revise regularmente suas configurações de privacidade",
                        style = MaterialTheme.typography.bodySmall,
                        color = TaskGoTextGray
                    )
                }
            }
            
            // Diálogos e mensagens
            if (showChangeMethodDialog) {
                AlertDialog(
                    onDismissRequest = { showChangeMethodDialog = false },
                    title = { 
                        Text(
                            "Alterar Método de 2FA",
                            fontWeight = FontWeight.Bold,
                            color = TaskGoTextBlack
                        ) 
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Escolha o método de autenticação de duas etapas:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TaskGoTextGray
                            )
                            
                            data class TwoFactorMethod(
                                val key: String,
                                val icon: androidx.compose.ui.graphics.vector.ImageVector,
                                val title: String,
                                val description: String
                            )
                            
                            val methods = listOf(
                                TwoFactorMethod("email", Icons.Default.Email, "Email", "Código enviado por email"),
                                TwoFactorMethod("sms", Icons.Default.Phone, "Telefone", "Código enviado por SMS"),
                                TwoFactorMethod("email e telefone", Icons.Default.VerifiedUser, "Email e Telefone", "Código enviado por ambos")
                            )
                            
                            methods.forEach { method ->
                                val methodKey = method.key
                                val icon = method.icon
                                val title = method.title
                                val description = method.description
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.changeTwoFactorMethod(methodKey)
                                            showChangeMethodDialog = false
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (uiState.twoFactorMethod == methodKey) 
                                            TaskGoGreen.copy(alpha = 0.1f) 
                                        else 
                                            MaterialTheme.colorScheme.surface
                                    ),
                                    border = if (uiState.twoFactorMethod == methodKey) 
                                        BorderStroke(2.dp, TaskGoGreen) 
                                    else null
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = uiState.twoFactorMethod == methodKey,
                                            onClick = {
                                                viewModel.changeTwoFactorMethod(methodKey)
                                                showChangeMethodDialog = false
                                            },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = TaskGoGreen
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (uiState.twoFactorMethod == methodKey) 
                                                TaskGoGreen 
                                            else 
                                                TaskGoTextGray,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = title,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium,
                                                color = TaskGoTextBlack
                                            )
                                            Text(
                                                text = description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TaskGoTextGray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showChangeMethodDialog = false }) {
                            Text("Fechar", color = TaskGoGreen)
                        }
                    }
                )
            }
            
            if (showDeleteConfirmation) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmation = false },
                    title = { Text("Confirmar Exclusão", color = TaskGoError) },
                    text = {
                        Text(
                            "Tem certeza que deseja excluir permanentemente sua conta? Esta ação não pode ser desfeita.",
                            color = TaskGoTextBlack
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (currentUser != null) {
                                    persistentScope.launch {
                                        try {
                                            // 1. Chamar Cloud Function para deletar dados
                                            val functionsService = com.taskgoapp.taskgo.data.firebase.FirebaseFunctionsService(
                                                com.google.firebase.functions.FirebaseFunctions.getInstance()
                                            )
                                            val deleteResult = functionsService.deleteUserAccount()
                                            
                                            // Fazer logout IMEDIATAMENTE antes mesmo de aguardar resultado
                                            // Isso garante que o usuário seja deslogado mesmo se houver erro na função
                                            auth.signOut()
                                            
                                            // Aguardar um pouco para garantir que o signOut foi processado
                                            kotlinx.coroutines.delay(500)
                                            
                                            // Navegar para login após logout
                                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                showDeleteConfirmation = false
                                                showExportMessage = null
                                                // Navegar para login - o MainActivity irá detectar que não há usuário autenticado
                                                onNavigateToLogin()
                                            }
                                            
                                            deleteResult.fold(
                                                onSuccess = { data ->
                                                    val message = data["message"] as? String ?: "Conta excluída com sucesso"
                                                    android.util.Log.d("SecuritySettingsScreen", message)
                                                    // Mensagem não precisa ser mostrada, pois já navegamos para login
                                                },
                                                onFailure = { error ->
                                                    android.util.Log.e("SecuritySettingsScreen", "Erro ao excluir conta", error)
                                                    // Logout já foi feito e navegação já ocorreu, apenas logamos o erro
                                                }
                                            )
                                        } catch (e: Exception) {
                                            showExportMessage = "Erro: ${e.message}"
                                            showDeleteConfirmation = false
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = TaskGoError)
                        ) {
                            Text("Excluir", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirmation = false }) {
                            Text("Cancelar")
                        }
                    }
                )
            }
            
            if (showExportMessage != null) {
                AlertDialog(
                    onDismissRequest = { showExportMessage = null },
                    title = { Text("Exportação de Dados") },
                    text = { Text(showExportMessage!!) },
                    confirmButton = {
                        TextButton(onClick = { showExportMessage = null }) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }
}

data class SecuritySettingsUiState(
    val biometricEnabled: Boolean = false,
    val twoFactorEnabled: Boolean = false,
    val twoFactorMethod: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class SecuritySettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val firestoreUserRepository: FirestoreUserRepository,
    private val auth: FirebaseAuth
) : ViewModel() {
    
    private val _uiState = kotlinx.coroutines.flow.MutableStateFlow(SecuritySettingsUiState())
    val uiState: kotlinx.coroutines.flow.StateFlow<SecuritySettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            val biometricEnabled = preferencesManager.biometricEnabled.first()
            val twoFactorEnabled = preferencesManager.twoFactorEnabled.first()
            val twoFactorMethod = preferencesManager.twoFactorMethod.first()
            
            _uiState.value = _uiState.value.copy(
                biometricEnabled = biometricEnabled,
                twoFactorEnabled = twoFactorEnabled,
                twoFactorMethod = twoFactorMethod
            )
        }
    }
    
    // Biometria removida - login apenas no primeiro acesso
    
    fun toggleTwoFactor(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesManager.updateTwoFactorEnabled(enabled)
                val method = if (enabled) "email" else null
                preferencesManager.updateTwoFactorMethod(method)
                
                // Salvar no Firestore
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    val user = firestoreUserRepository.getUser(currentUser.uid)
                    if (user != null) {
                        val updatedUser = user.copy(
                            twoFactorEnabled = enabled,
                            twoFactorMethod = method
                        )
                        firestoreUserRepository.updateUser(updatedUser)
                    }
                }
                
                _uiState.value = _uiState.value.copy(
                    twoFactorEnabled = enabled,
                    twoFactorMethod = method
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Erro ao atualizar 2FA: ${e.message}"
                )
            }
        }
    }
    
    fun changeTwoFactorMethod(method: String) {
        viewModelScope.launch {
            try {
                preferencesManager.updateTwoFactorMethod(method)
                
                // Salvar no Firestore
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    val user = firestoreUserRepository.getUser(currentUser.uid)
                    if (user != null) {
                        val updatedUser = user.copy(
                            twoFactorMethod = method
                        )
                        firestoreUserRepository.updateUser(updatedUser)
                    }
                }
                
                _uiState.value = _uiState.value.copy(
                    twoFactorMethod = method
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Erro ao alterar método: ${e.message}"
                )
            }
        }
    }
}

