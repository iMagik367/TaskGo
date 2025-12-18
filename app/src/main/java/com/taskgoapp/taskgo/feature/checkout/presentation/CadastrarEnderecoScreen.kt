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
import com.taskgoapp.taskgo.core.permissions.PermissionHandler
import com.taskgoapp.taskgo.core.permissions.rememberMultiplePermissionsLauncher
import com.taskgoapp.taskgo.core.theme.*
import com.taskgoapp.taskgo.core.validation.CepService
import com.taskgoapp.taskgo.core.validation.DocumentValidator
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
    var cidade by remember { mutableStateOf("") }
    var estado by remember { mutableStateOf("") }
    var cep by remember { mutableStateOf("") }
    var complemento by remember { mutableStateOf("") }
    var nomeEndereco by remember { mutableStateOf("") }
    var isLoadingLocation by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var cepError by remember { mutableStateOf<String?>(null) }
    var isLoadingCep by remember { mutableStateOf(false) }
    
    val isError = variant == "invalid"
    val isEmpty = variant == "empty"
    
    // LocationManager via Hilt (precisamos injetar diretamente via contexto por enquanto)
    val locationManager = remember {
        LocationManager(context)
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
                    val address = locationManager.getCurrentAddress()
                    address?.let {
                        rua = it.thoroughfare ?: ""
                        numero = it.subThoroughfare ?: ""
                        bairro = it.subLocality ?: it.featureName ?: ""
                        cidade = it.locality ?: ""
                        estado = it.adminArea ?: ""
                        cep = it.postalCode ?: ""
                        locationError = null
                    } ?: run {
                        locationError = "Não foi possível obter o endereço da localização"
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
                    val address = locationManager.getCurrentAddress()
                    address?.let {
                        rua = it.thoroughfare ?: ""
                        numero = it.subThoroughfare ?: ""
                        bairro = it.subLocality ?: it.featureName ?: ""
                        cidade = it.locality ?: ""
                        estado = it.adminArea ?: ""
                        cep = it.postalCode ?: ""
                        locationError = null
                    } ?: run {
                        locationError = "Não foi possível obter o endereço da localização"
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
        OutlinedTextField(
            value = cidade,
            onValueChange = { cidade = it },
            label = { Text("Cidade*") },
            isError = isEmpty && cidade.isEmpty(),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = estado,
            onValueChange = { estado = it },
            label = { Text("Estado*") },
            isError = isEmpty && estado.isEmpty(),
            modifier = Modifier.fillMaxWidth()
        )
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
                                    cidade = cepResult.localidade
                                    estado = cepResult.uf
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
                    city = cidade,
                    state = estado,
                    cep = cep.replace(Regex("[^0-9]"), ""),
                    zipCode = cep.replace(Regex("[^0-9]"), "")
                )
            },
            enabled = !uiState.isLoading && rua.isNotEmpty() && numero.isNotEmpty() && 
                     cidade.isNotEmpty() && estado.isNotEmpty() && cep.isNotEmpty(),
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
