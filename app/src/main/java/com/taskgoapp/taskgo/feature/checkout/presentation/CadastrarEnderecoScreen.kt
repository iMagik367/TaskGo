package com.taskgoapp.taskgo.feature.checkout.presentation

import android.Manifest
import android.location.Address
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskgoapp.taskgo.core.location.LocationManager
import dagger.hilt.android.EntryPointAccessors
import com.taskgoapp.taskgo.core.permissions.PermissionHandler
import com.taskgoapp.taskgo.core.permissions.rememberMultiplePermissionsLauncher
import com.taskgoapp.taskgo.core.theme.*
import com.taskgoapp.taskgo.core.validation.CepService
import com.taskgoapp.taskgo.core.validation.DocumentValidator
import com.taskgoapp.taskgo.core.model.Result
import com.taskgoapp.taskgo.core.model.onSuccess
import com.taskgoapp.taskgo.core.model.onFailure
import com.taskgoapp.taskgo.core.model.getDataOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import javax.inject.Inject

@Composable
fun CadastrarEnderecoScreen(
    onBackClick: () -> Unit,
    onSave: () -> Unit,
    variant: String? = null,
    viewModel: AddressFormViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Observar sucesso e navegar de volta
    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            onSave()
        }
    }
    
    // Serviços de validação e busca
    val cepService = remember { CepService() }
    val documentValidator = remember { DocumentValidator() }
    
    var rua by remember { mutableStateOf("") }
    var numero by remember { mutableStateOf("") }
    var bairro by remember { mutableStateOf("") }
    // REMOVIDO: cidade e estado - serão obtidos automaticamente do perfil do usuário
    var cep by remember { mutableStateOf("") }
    var complemento by remember { mutableStateOf("") }
    var nomeEndereco by remember { mutableStateOf("") }
    var isLoadingLocation by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var cepError by remember { mutableStateOf<String?>(null) }
    var isLoadingCep by remember { mutableStateOf(false) }
    
    val isError = variant == "invalid"
    val isEmpty = variant == "empty"
    
    // LocationManager via Hilt EntryPoint
    val locationManager = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext as com.taskgoapp.taskgo.TaskGoApp,
            com.taskgoapp.taskgo.di.LocationManagerEntryPoint::class.java
        ).locationManager()
    }
    
    // Verificar permissão de localização
    val hasLocationPermission = remember {
        PermissionHandler.hasLocationPermission(context)
    }
    
    // Launcher para permissão de localização
    val locationPermissionLauncher = rememberMultiplePermissionsLauncher(
        onAllPermissionsGranted = {
            isLoadingLocation = true
            locationError = null
            scope.launch {
                try {
                    // Verificar permissão antes de usar
                    if (PermissionHandler.hasLocationPermission(context)) {
                        try {
                            val location = locationManager.getCurrentLocationGuaranteed()
                            val address = locationManager.getAddressGuaranteed(location.latitude, location.longitude)
                            rua = address.thoroughfare ?: ""
                            numero = address.subThoroughfare ?: ""
                            bairro = address.subLocality ?: address.featureName ?: ""
                            // REMOVIDO: cidade e estado - serão obtidos automaticamente do perfil
                            cep = address.postalCode ?: ""
                            locationError = null
                        } catch (e: Exception) {
                            locationError = "Erro ao obter localização: ${e.message}"
                        }
                    } else {
                        locationError = "Permissão de localização não concedida"
                    }
                } catch (e: SecurityException) {
                    locationError = "Permissão de localização negada"
                } catch (e: Exception) {
                    locationError = "Erro ao obter localização: ${e.message}"
                } finally {
                    isLoadingLocation = false
                }
            }
        },
        onPermissionDenied = {
            locationError = "Permissão de localização negada"
            isLoadingLocation = false
        }
    )
    
    fun loadCurrentLocation() {
        if (hasLocationPermission) {
            isLoadingLocation = true
            locationError = null
            scope.launch {
                try {
                    // Verificar permissão antes de usar
                    if (PermissionHandler.hasLocationPermission(context)) {
                        try {
                            val location = locationManager.getCurrentLocationGuaranteed()
                            val address = locationManager.getAddressGuaranteed(location.latitude, location.longitude)
                            rua = address.thoroughfare ?: ""
                            numero = address.subThoroughfare ?: ""
                            bairro = address.subLocality ?: address.featureName ?: ""
                            // REMOVIDO: cidade e estado - serão obtidos automaticamente do perfil
                            cep = address.postalCode ?: ""
                            locationError = null
                        } catch (e: Exception) {
                            locationError = "Erro ao obter localização: ${e.message}"
                        }
                    } else {
                        locationError = "Permissão de localização não concedida"
                    }
                } catch (e: SecurityException) {
                    locationError = "Permissão de localização negada"
                } catch (e: Exception) {
                    locationError = "Erro ao obter localização: ${e.message}"
                } finally {
                    isLoadingLocation = false
                }
            }
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Cadastrar Endereço",
            style = FigmaSectionTitle,
            color = TaskGoTextBlack
        )
        
        Spacer(Modifier.height(18.dp))
        
        // Botão para usar localização atual
        OutlinedButton(
            onClick = { loadCurrentLocation() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoadingLocation
        ) {
            if (isLoadingLocation) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
                Text("Obtendo localização...")
            } else {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Usar localização atual")
            }
        }
        
        locationError?.let { error ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        Spacer(Modifier.height(16.dp))
        
        OutlinedTextField(
            value = nomeEndereco,
            onValueChange = { nomeEndereco = it },
            label = { Text("Nome do endereço (ex: Casa, Trabalho)") },
            placeholder = { Text("Casa") },
            modifier = Modifier.fillMaxWidth()
        )
        
        OutlinedTextField(
            value = rua,
            onValueChange = { rua = it },
            label = { Text("Rua*") },
            isError = isEmpty && rua.isEmpty() || isError && rua == "" || false,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = numero,
            onValueChange = { numero = it },
            label = { Text("Número*") },
            isError = isEmpty && numero.isEmpty(),
            modifier = Modifier.fillMaxWidth()
        )
        
        OutlinedTextField(
            value = complemento,
            onValueChange = { complemento = it },
            label = { Text("Complemento") },
            placeholder = { Text("Apto, Bloco, etc.") },
            modifier = Modifier.fillMaxWidth()
        )
        
        OutlinedTextField(
            value = bairro,
            onValueChange = { bairro = it },
            label = { Text("Bairro*") },
            isError = isEmpty && bairro.isEmpty(),
            modifier = Modifier.fillMaxWidth()
        )
        // REMOVIDO: Campos de Cidade e Estado
        // Localização será obtida automaticamente do perfil do usuário
        OutlinedTextField(
            value = cep,
            onValueChange = { newValue ->
                val cleanValue = newValue.replace(Regex("[^0-9]"), "")
                if (cleanValue.length <= 8) {
                    cep = cleanValue
                    cepError = null
                    
                    if (cleanValue.length == 8) {
                        cep = documentValidator.formatCep(cleanValue)
                        
                        scope.launch {
                            isLoadingCep = true
                            cepError = null
                            
                            delay(500)
                            
                            cepService.searchCep(cleanValue).fold(
                                onSuccess = { cepResult ->
                                    rua = cepResult.logradouro
                                    bairro = cepResult.bairro
                                    // REMOVIDO: cidade e estado - serão obtidos automaticamente do perfil
                                    isLoadingCep = false
                                },
                                onFailure = { error ->
                                    cepError = error.message ?: "CEP não encontrado"
                                    isLoadingCep = false
                                }
                            )
                        }
                    }
                }
            },
            label = { Text("CEP*") },
            placeholder = { Text("00000-000") },
            trailingIcon = {
                if (isLoadingCep) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
            },
            isError = (isEmpty && cep.isEmpty()) || (isError && cep == "30100-000") || cepError != null,
            supportingText = {
                when {
                    cepError != null -> Text(cepError!!, color = MaterialTheme.colorScheme.error)
                    isError -> Text("CEP inválido", color = MaterialTheme.colorScheme.error)
                    else -> null
                }
            },
            enabled = !isLoadingCep,
            modifier = Modifier.fillMaxWidth()
        )
        // Mostrar erro do ViewModel
        uiState.error?.let { error ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                viewModel.saveAddress(
                    name = nomeEndereco.ifEmpty { "Endereço" },
                    street = rua,
                    number = numero,
                    complement = complemento.takeIf { it.isNotEmpty() },
                    neighborhood = bairro,
                    district = bairro,
                    // REMOVIDO: city e state - serão obtidos automaticamente do perfil
                    cep = cep.replace(Regex("[^0-9]"), ""),
                    zipCode = cep.replace(Regex("[^0-9]"), "")
                )
            },
            enabled = !uiState.isLoading && rua.isNotEmpty() && numero.isNotEmpty() && cep.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
                Text("Salvando...")
            } else {
                Text("Salvar")
            }
        }
        OutlinedButton(
            onClick = onBackClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancelar")
        }
    }
}
