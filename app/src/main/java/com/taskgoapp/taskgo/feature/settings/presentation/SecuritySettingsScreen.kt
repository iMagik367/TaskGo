package com.taskgoapp.taskgo.feature.settings.presentation

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.taskgoapp.taskgo.core.biometric.BiometricManager
import com.taskgoapp.taskgo.core.biometric.BiometricStatus
import com.taskgoapp.taskgo.core.design.TGIcons
import com.taskgoapp.taskgo.core.theme.*
import com.taskgoapp.taskgo.data.local.datastore.PreferencesManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(
    onBackClick: () -> Unit,
    onNavigateToIdentityVerification: () -> Unit,
    viewModel: SecuritySettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? androidx.fragment.app.FragmentActivity
    val biometricManager = remember { BiometricManager(context) }
    val biometricStatus = remember { biometricManager.isBiometricAvailable() }
    val biometricAvailable = biometricStatus == BiometricStatus.AVAILABLE
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Segurança",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(TGIcons.Back),
                            contentDescription = "Voltar",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TaskGoGreen
                )
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
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "Verificar identidade",
                                tint = TaskGoGreen
                            )
                        }
                    }
                }
            }
            
            // Biometria
            if (biometricAvailable) {
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
                                    text = "Login com Biometria",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TaskGoTextBlack
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Use sua biometria para fazer login",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TaskGoTextGray
                                )
                            }
                            Switch(
                                checked = uiState.biometricEnabled,
                                onCheckedChange = { 
                                    if (activity != null) {
                                        viewModel.toggleBiometric(activity, biometricManager)
                                    }
                                }
                            )
                        }
                    }
                }
            }
            
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
                                text = "Adicione uma camada extra de segurança",
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
                        Text(
                            text = "Método: ${uiState.twoFactorMethod ?: "Email"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TaskGoTextGray
                        )
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
                        text = "• Mantenha seus dados atualizados\n• Use senhas fortes\n• Ative a autenticação de duas etapas\n• Verifique sua identidade",
                        style = MaterialTheme.typography.bodySmall,
                        color = TaskGoTextGray
                    )
                }
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
    private val preferencesManager: PreferencesManager
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
    
    fun toggleBiometric(
        activity: Activity,
        biometricManager: BiometricManager
    ) {
        val currentState = _uiState.value.biometricEnabled
        
        if (!currentState) {
            // Habilitar biometria
            biometricManager.authenticate(
                activity = activity,
                title = "Habilitar Biometria",
                subtitle = "Autentique-se para habilitar o login biométrico",
                onSuccess = {
                    viewModelScope.launch {
                        preferencesManager.updateBiometricEnabled(true)
                        _uiState.value = _uiState.value.copy(biometricEnabled = true)
                    }
                },
                onError = { error ->
                    _uiState.value = _uiState.value.copy(errorMessage = error)
                },
                onCancel = {
                    // Usuário cancelou, não fazer nada
                }
            )
        } else {
            // Desabilitar biometria
            viewModelScope.launch {
                preferencesManager.updateBiometricEnabled(false)
                _uiState.value = _uiState.value.copy(biometricEnabled = false)
            }
        }
    }
    
    fun toggleTwoFactor(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.updateTwoFactorEnabled(enabled)
            if (enabled) {
                preferencesManager.updateTwoFactorMethod("email")
            } else {
                preferencesManager.updateTwoFactorMethod(null)
            }
            _uiState.value = _uiState.value.copy(
                twoFactorEnabled = enabled,
                twoFactorMethod = if (enabled) "email" else null
            )
        }
    }
}

