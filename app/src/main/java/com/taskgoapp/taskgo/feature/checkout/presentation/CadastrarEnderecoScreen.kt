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
import androidx.hilt.navigation.compose.hiltViewModel
import com.taskgoapp.taskgo.core.location.LocationManager
import com.taskgoapp.taskgo.core.permissions.PermissionHandler
import com.taskgoapp.taskgo.core.permissions.rememberMultiplePermissionsLauncher
import com.taskgoapp.taskgo.core.theme.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@Composable
fun CadastrarEnderecoScreen(
    onBackClick: () -> Unit,
    onSave: () -> Unit,
    variant: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var rua by remember { mutableStateOf("") }
    var numero by remember { mutableStateOf("") }
    var bairro by remember { mutableStateOf("") }
    var cidade by remember { mutableStateOf("") }
    var estado by remember { mutableStateOf("") }
    var cep by remember { mutableStateOf("") }
    var isLoadingLocation by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }
    
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
            onValueChange = { cep = it },
            label = { Text("CEP*") },
            isError = isEmpty && cep.isEmpty() || isError && cep == "30100-000",
            supportingText = { if (isError) Text("CEP inválido") else null },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                if (!isEmpty && !isError) onSave()
            },
            enabled = !isEmpty && !isError,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Salvar")
        }
        OutlinedButton(
            onClick = onBackClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancelar")
        }
    }
}
