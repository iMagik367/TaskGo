package com.taskgoapp.taskgo.feature.settings.presentation

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.taskgoapp.taskgo.core.theme.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.taskgoapp.taskgo.core.design.*
import com.taskgoapp.taskgo.core.security.LGPDComplianceManager
import com.taskgoapp.taskgo.data.firebase.FirebaseFunctionsService
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun PrivacyScreen(
    onBackClick: () -> Unit,
    onNavigateToPrivacyPolicy: () -> Unit = {},
    onNavigateToTermsOfService: () -> Unit = {},
    onNavigateToConsentHistory: () -> Unit = {},
    viewModel: SettingsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val settings by viewModel.state.collectAsState()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var saveJob by remember { mutableStateOf<Job?>(null) }
    var isSyncingFromRemote by remember { mutableStateOf(true) }
    
    val lgpdManager = remember(context) { 
        com.taskgoapp.taskgo.core.security.LGPDComplianceManager(
            context,
            com.taskgoapp.taskgo.core.firebase.FirestoreHelper.getInstance()
        )
    }
    val functionsService = remember {
        FirebaseFunctionsService(FirebaseFunctions.getInstance())
    }
    // Estados locais - inicializar apenas uma vez
    var locationSharingEnabled by remember { mutableStateOf(true) }
    var profileVisibilityEnabled by remember { mutableStateOf(true) }
    var contactInfoSharingEnabled by remember { mutableStateOf(false) }
    var analyticsEnabled by remember { mutableStateOf(true) }
    var personalizedAdsEnabled by remember { mutableStateOf(false) }
    var dataCollectionEnabled by remember { mutableStateOf(true) }
    var thirdPartySharingEnabled by remember { mutableStateOf(false) }
    
    // Inicializar apenas uma vez quando dados estiverem disponíveis
    var hasInitialized by remember { mutableStateOf(false) }
    LaunchedEffect(
        settings.locationSharingEnabled,
        settings.profileVisible,
        settings.contactInfoSharingEnabled,
        settings.analyticsEnabled,
        settings.personalizedAdsEnabled,
        settings.dataCollectionEnabled,
        settings.thirdPartySharingEnabled
    ) {
        isSyncingFromRemote = true
        locationSharingEnabled = settings.locationSharingEnabled
        profileVisibilityEnabled = settings.profileVisible
        contactInfoSharingEnabled = settings.contactInfoSharingEnabled
        analyticsEnabled = settings.analyticsEnabled
        personalizedAdsEnabled = settings.personalizedAdsEnabled
        dataCollectionEnabled = settings.dataCollectionEnabled
        thirdPartySharingEnabled = settings.thirdPartySharingEnabled
        hasInitialized = true
        isSyncingFromRemote = false
    }
    
    fun queueSave() {
        if (!hasInitialized || isSyncingFromRemote) return
        saveJob?.cancel()
        saveJob = coroutineScope.launch {
            delay(800)
            viewModel.savePrivacySettings(
                locationSharing = locationSharingEnabled,
                profileVisible = profileVisibilityEnabled,
                contactInfoSharing = contactInfoSharingEnabled,
                analytics = analyticsEnabled,
                personalizedAds = personalizedAdsEnabled,
                dataCollection = dataCollectionEnabled,
                thirdPartySharing = thirdPartySharingEnabled
            )
        }
    }
    
    LaunchedEffect(
        locationSharingEnabled,
        profileVisibilityEnabled,
        contactInfoSharingEnabled,
        analyticsEnabled,
        personalizedAdsEnabled,
        dataCollectionEnabled,
        thirdPartySharingEnabled
    ) {
        queueSave()
    }
    
    // Salvar quando sair da tela (garantir salvamento final)
    DisposableEffect(Unit) {
        onDispose {
            saveJob?.cancel()
            viewModel.savePrivacySettings(
                locationSharing = locationSharingEnabled,
                profileVisible = profileVisibilityEnabled,
                contactInfoSharing = contactInfoSharingEnabled,
                analytics = analyticsEnabled,
                personalizedAds = personalizedAdsEnabled,
                dataCollection = dataCollectionEnabled,
                thirdPartySharing = thirdPartySharingEnabled
            )
        }
    }
    
    // Permissão de localização
    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    
    // Flag para evitar solicitações contínuas de permissão
    var hasRequestedLocationPermission by remember { mutableStateOf(false) }
    
    // Solicitar permissão apenas uma vez quando locationSharingEnabled for habilitado
    LaunchedEffect(locationSharingEnabled) {
        if (locationSharingEnabled && 
            locationPermission.status !is PermissionStatus.Granted && 
            !hasRequestedLocationPermission) {
            hasRequestedLocationPermission = true
            locationPermission.launchPermissionRequest()
        } else if (!locationSharingEnabled) {
            hasRequestedLocationPermission = false
        }
    }
    
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Privacidade",
                subtitle = "Controle como suas informações são utilizadas",
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Privacy Overview
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = TaskGoBackgroundGray
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Sua Privacidade é Importante",
                        style = FigmaSectionTitle,
                        color = TaskGoTextBlack,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Controle como suas informações são usadas e compartilhadas no TaskGo. Você pode alterar essas configurações a qualquer momento.",
                        style = FigmaProductDescription,
                        color = TaskGoTextBlack
                    )
                }
            }
            
            // Location Privacy
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Localização",
                        style = FigmaSectionTitle,
                        color = TaskGoTextBlack,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = TaskGoGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Compartilhar Localização",
                                    style = FigmaProductName,
                                    color = TaskGoTextBlack,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Permitir que prestadores vejam sua localização para serviços próximos",
                                    style = FigmaStatusText,
                                    color = TaskGoTextGray,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Switch(
                            checked = locationSharingEnabled,
                            onCheckedChange = { locationSharingEnabled = it }
                        )
                    }
                    
                    if (locationSharingEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedButton(
                            onClick = {
                                // Abrir configurações de localização do Android
                                val intent = android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    android.util.Log.e("PrivacyScreen", "Erro ao abrir configurações: ${e.message}", e)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Configurações de Localização")
                        }
                    }
                }
            }
            
            // Profile Privacy
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Perfil e Visibilidade",
                        style = FigmaSectionTitle,
                        color = TaskGoTextBlack,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = TaskGoGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Perfil Público",
                                    style = FigmaProductName,
                                    color = TaskGoTextBlack,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Permitir que outros usuários vejam seu perfil",
                                    style = FigmaStatusText,
                                    color = TaskGoTextGray,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Switch(
                            checked = profileVisibilityEnabled,
                            onCheckedChange = { profileVisibilityEnabled = it }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContactPhone,
                                contentDescription = null,
                                tint = TaskGoGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Compartilhar Contato",
                                    style = FigmaProductName,
                                    color = TaskGoTextBlack,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Permitir que prestadores vejam seu telefone e e-mail",
                                    style = FigmaStatusText,
                                    color = TaskGoTextGray,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Switch(
                            checked = contactInfoSharingEnabled,
                            onCheckedChange = { contactInfoSharingEnabled = it }
                        )
                    }
                }
            }
            
            // Data Collection
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Coleta de Dados",
                        style = FigmaSectionTitle,
                        color = TaskGoTextBlack,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = null,
                                tint = TaskGoGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Analytics e Métricas",
                                    style = FigmaProductName,
                                    color = TaskGoTextBlack,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Coletar dados para melhorar o aplicativo",
                                    style = FigmaStatusText,
                                    color = TaskGoTextGray,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Switch(
                            checked = analyticsEnabled,
                            onCheckedChange = { analyticsEnabled = it }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DataUsage,
                                contentDescription = null,
                                tint = TaskGoGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Coleta de Dados",
                                    style = FigmaProductName,
                                    color = TaskGoTextBlack,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Armazenar informações sobre seu uso do aplicativo",
                                    style = FigmaStatusText,
                                    color = TaskGoTextGray,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Switch(
                            checked = dataCollectionEnabled,
                            onCheckedChange = { dataCollectionEnabled = it }
                        )
                    }
                }
            }
            
            // Advertising
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Publicidade",
                        style = FigmaSectionTitle,
                        color = TaskGoTextBlack,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = TaskGoGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Publicidade Personalizada",
                                    style = FigmaProductName,
                                    color = TaskGoTextBlack,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Mostrar anúncios baseados em seus interesses",
                                    style = FigmaStatusText,
                                    color = TaskGoTextGray,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Switch(
                            checked = personalizedAdsEnabled,
                            onCheckedChange = { personalizedAdsEnabled = it }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = TaskGoGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Compartilhamento com Terceiros",
                                    style = FigmaProductName,
                                    color = TaskGoTextBlack,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Permitir que parceiros acessem dados para publicidade",
                                    style = FigmaStatusText,
                                    color = TaskGoTextGray,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Switch(
                            checked = thirdPartySharingEnabled,
                            onCheckedChange = { thirdPartySharingEnabled = it }
                        )
                    }
                }
            }
            
            // Privacy Actions
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Ações de Privacidade",
                        style = FigmaSectionTitle,
                        color = TaskGoTextBlack,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    var isExporting by remember { mutableStateOf(false) }
                    var exportMessage by remember { mutableStateOf<String?>(null) }
                    
                    OutlinedButton(
                        onClick = {
                            val user = currentUser
                            if (user != null) {
                                isExporting = true
                                coroutineScope.launch {
                                    try {
                                        val result = lgpdManager.exportUserData(user.uid)
                                        result.fold(
                                            onSuccess = { export ->
                                                exportMessage = "Dados exportados com sucesso! Total: ${export.data.size} itens"
                                                isExporting = false
                                            },
                                            onFailure = { error ->
                                                exportMessage = "Erro ao exportar: ${error.message}"
                                                isExporting = false
                                                // Tratar erro específico do Secure Token API bloqueado
                                                if (error.message?.contains("SecureToken") == true || error.message?.contains("securetoken") == true) {
                                                    android.util.Log.w("PrivacyScreen", "Firebase Secure Token API bloqueado. Verifique configurações do Google Cloud.")
                                                }
                                            }
                                        )
                                    } catch (e: Exception) {
                                        exportMessage = "Erro: ${e.message}"
                                        isExporting = false
                                        if (e.message?.contains("SecureToken") == true || e.message?.contains("securetoken") == true) {
                                            android.util.Log.w("PrivacyScreen", "Firebase Secure Token API bloqueado. Verifique configurações do Google Cloud.")
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isExporting && currentUser != null
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp))
                        } else {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isExporting) "Exportando..." else "Baixar Meus Dados")
                    }
                    
                    if (exportMessage != null) {
                        Text(
                            text = exportMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (exportMessage!!.contains("sucesso")) TaskGoGreen else MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    var showDeleteConfirmation by remember { mutableStateOf(false) }
                    
                    OutlinedButton(
                        onClick = { showDeleteConfirmation = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = TaskGoError
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = TaskGoError
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Excluir Meus Dados", color = TaskGoError)
                    }
                    
                    if (showDeleteConfirmation) {
                        AlertDialog(
                            onDismissRequest = { showDeleteConfirmation = false },
                            title = { Text("Confirmar Exclusão", color = TaskGoError) },
                            text = { 
                                Text(
                                    "Tem certeza que deseja excluir permanentemente todos os seus dados? Esta ação não pode ser desfeita.",
                                    color = TaskGoTextBlack
                                )
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        val user = currentUser
                                        if (user != null) {
                                            coroutineScope.launch {
                                                try {
                                                    // Chamar Cloud Function para excluir conta completamente (inclui Firebase Auth)
                                                    val result = functionsService.deleteUserAccount()
                                                    result.fold(
                                                        onSuccess = { data ->
                                                            val message = data["message"] as? String ?: "Conta excluída com sucesso"
                                                            exportMessage = message
                                                            showDeleteConfirmation = false
                                                            // Fazer logout após exclusão bem-sucedida
                                                            auth.signOut()
                                                            // Navegar para login será feito pela navegação
                                                        },
                                                        onFailure = { error ->
                                                            exportMessage = "Erro ao excluir conta: ${error.message}"
                                                            showDeleteConfirmation = false
                                                            android.util.Log.e("PrivacyScreen", "Erro ao excluir conta", error)
                                                        }
                                                    )
                                                } catch (e: Exception) {
                                                    exportMessage = "Erro: ${e.message}"
                                                    showDeleteConfirmation = false
                                                    android.util.Log.e("PrivacyScreen", "Erro ao excluir conta", e)
                                                }
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = TaskGoError
                                    )
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
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedButton(
                        onClick = onNavigateToPrivacyPolicy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Policy,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Política de Privacidade")
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedButton(
                        onClick = onNavigateToTermsOfService,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Termos de Uso")
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedButton(
                        onClick = onNavigateToConsentHistory,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Histórico de Consentimentos")
                    }
                }
            }
            
            // Privacy Status
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = TaskGoSurfaceGray
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Status da Privacidade",
                        style = FigmaSectionTitle,
                        color = TaskGoTextBlack,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val privacyScore = calculatePrivacyScore(
                        locationSharingEnabled,
                        profileVisibilityEnabled,
                        contactInfoSharingEnabled,
                        analyticsEnabled,
                        personalizedAdsEnabled,
                        dataCollectionEnabled,
                        thirdPartySharingEnabled
                    )
                    
                    Text(
                        text = "Sua privacidade está ${getPrivacyStatus(privacyScore)}",
                        style = FigmaProductDescription,
                        color = TaskGoTextGray
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = getPrivacyColor(privacyScore),
                        progress = { privacyScore / 100f }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "$privacyScore%",
                        style = FigmaProductName,
                        color = TaskGoTextBlack,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
        }
    }
}

private fun calculatePrivacyScore(
    locationSharing: Boolean,
    profileVisibility: Boolean,
    contactInfoSharing: Boolean,
    analytics: Boolean,
    personalizedAds: Boolean,
    dataCollection: Boolean,
    thirdPartySharing: Boolean
): Int {
    var score = 100
    
    if (locationSharing) score -= 15
    if (profileVisibility) score -= 10
    if (contactInfoSharing) score -= 20
    if (analytics) score -= 10
    if (personalizedAds) score -= 15
    if (dataCollection) score -= 10
    if (thirdPartySharing) score -= 20
    
    return maxOf(score, 0)
}

private fun getPrivacyStatus(score: Int): String {
    return when {
        score >= 80 -> "muito bem protegida"
        score >= 60 -> "bem protegida"
        score >= 40 -> "moderadamente protegida"
        score >= 20 -> "pouco protegida"
        else -> "muito exposta"
    }
}

private fun getPrivacyColor(score: Int): Color {
    return when {
        score >= 80 -> TaskGoSuccessGreen
        score >= 60 -> Color(0xFF8BC34A)
        score >= 40 -> TaskGoAmber
        score >= 20 -> TaskGoOrange
        else -> Color(0xFFF44336)
    }
}




