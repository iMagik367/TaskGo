package com.taskgoapp.taskgo.feature.services.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.taskgoapp.taskgo.R
import com.taskgoapp.taskgo.core.design.*
import com.taskgoapp.taskgo.core.security.DocumentVerificationBlock
import com.taskgoapp.taskgo.core.security.DocumentVerificationManager
import com.taskgoapp.taskgo.core.theme.*
import com.taskgoapp.taskgo.data.firebase.FirebaseFunctionsService
import com.taskgoapp.taskgo.data.repository.FirestoreUserRepository
import com.taskgoapp.taskgo.domain.repository.CategoriesRepository
import com.taskgoapp.taskgo.core.model.fold
import com.taskgoapp.taskgo.core.model.isSuccess
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class CreateOrderUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

@HiltViewModel
class CreateWorkOrderViewModel @Inject constructor(
    private val documentVerificationManager: DocumentVerificationManager,
    private val firebaseFunctionsService: FirebaseFunctionsService,
    private val categoriesRepository: CategoriesRepository,
    private val userRepository: FirestoreUserRepository,
    private val authRepository: com.taskgoapp.taskgo.data.repository.FirebaseAuthRepository
) : ViewModel() {
    private val _isVerified = MutableStateFlow(false)
    val isVerified: StateFlow<Boolean> = _isVerified.asStateFlow()
    
    private val _uiState = MutableStateFlow(CreateOrderUiState())
    val uiState: StateFlow<CreateOrderUiState> = _uiState.asStateFlow()
    
    val serviceCategories = categoriesRepository.observeServiceCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    init {
        viewModelScope.launch {
            _isVerified.value = documentVerificationManager.hasDocumentsVerified()
        }
    }
    
    suspend fun createOrder(
        category: String,
        description: String,
        location: String,
        budget: Double?,
        dueDate: String?
    ): com.taskgoapp.taskgo.core.model.Result<String> {
        return try {
            android.util.Log.d("CreateWorkOrderVM", "🔵 Criando ordem de serviço...")
            android.util.Log.d("CreateWorkOrderVM", "   category: $category")
            android.util.Log.d("CreateWorkOrderVM", "   description: ${description.take(50)}...")
            android.util.Log.d("CreateWorkOrderVM", "   location: $location")
            android.util.Log.d("CreateWorkOrderVM", "   budget: $budget")
            android.util.Log.d("CreateWorkOrderVM", "   dueDate: $dueDate")
            
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, success = false)
            
            val result = firebaseFunctionsService.createOrder(
                serviceId = null,
                category = category,
                details = description,
                location = location,
                budget = budget,
                dueDate = dueDate
            )
            
            android.util.Log.d("CreateWorkOrderVM", "   Resultado da CF: isSuccess=${result.isSuccess}")
            
            return result.fold(
                onSuccess = { data: Map<String, Any> ->
                    val orderId = data["orderId"] as? String
                    if (orderId != null) {
                        android.util.Log.d("CreateWorkOrderVM", "✅ Ordem criada com sucesso: orderId=$orderId")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            success = true,
                            error = null
                        )
                        com.taskgoapp.taskgo.core.model.Result.Success(orderId)
                    } else {
                        android.util.Log.e("CreateWorkOrderVM", "❌ Resposta inválida: orderId não encontrado")
                        android.util.Log.e("CreateWorkOrderVM", "   Dados recebidos: $data")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Erro ao criar ordem: resposta inválida"
                        )
                        com.taskgoapp.taskgo.core.model.Result.Error(Exception("Resposta inválida"))
                    }
                },
                onFailure = { exception: Throwable ->
                    val error = exception.message ?: "Erro desconhecido"
                    android.util.Log.e("CreateWorkOrderVM", "❌ Erro ao criar ordem: $error", exception)
                    android.util.Log.e("CreateWorkOrderVM", "   Exception type: ${exception.javaClass.simpleName}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error
                    )
                    com.taskgoapp.taskgo.core.model.Result.Error(exception)
                }
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = e.message ?: "Erro ao criar ordem"
            )
            com.taskgoapp.taskgo.core.model.Result.Error(e)
        }
    }
    
    suspend fun getUserAddress(): String? {
        return try {
            val currentUser = authRepository.getCurrentUser()
            if (currentUser != null) {
                val user = userRepository.getUser(currentUser.uid)
                val address = user?.address
                if (address != null) {
                    val parts = mutableListOf<String>()
                    if (address.street.isNotEmpty()) parts.add(address.street)
                    if (address.number.isNotEmpty()) parts.add(address.number)
                    if (address.neighborhood.isNotEmpty()) parts.add(address.neighborhood)
                    if (address.city.isNotEmpty()) parts.add(address.city)
                    if (address.state.isNotEmpty()) parts.add(address.state)
                    parts.joinToString(", ")
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateWorkOrderScreen(
    onBackClick: () -> Unit,
    onWorkOrderCreated: () -> Unit,
    onNavigateToIdentityVerification: () -> Unit = {},
    viewModel: CreateWorkOrderViewModel = hiltViewModel()
) {
    val isVerified by viewModel.isVerified.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val categories by viewModel.serviceCategories.collectAsState()
    
    var selectedCategories by remember { mutableStateOf<Set<String>>(emptySet()) }
    var description by remember { mutableStateOf("") }
    // REMOVIDO: address, city e state - localização será obtida automaticamente do perfil do usuário
    var budget by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf<Date?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")) }
    
    // Show success dialog
    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            onWorkOrderCreated()
        }
    }
    
    DocumentVerificationBlock(
        isVerified = isVerified,
        onVerifyClick = onNavigateToIdentityVerification
    ) {
        Scaffold(
            topBar = {
                AppTopBar(
                    title = stringResource(R.string.services_create_order_title),
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
                Text(
                    text = stringResource(R.string.services_create_order_subtitle),
                    style = FigmaProductDescription,
                    color = TaskGoTextGray
                )
                
                // Category Selection (Checkboxes)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = TaskGoBackgroundWhite),
                    border = BorderStroke(1.dp, TaskGoBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Categorias do Serviço *",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Selecione todas as categorias relacionadas ao serviço que você precisa",
                            style = MaterialTheme.typography.bodySmall,
                            color = TaskGoTextGray
                        )
                        categories.forEach { category ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        selectedCategories = if (selectedCategories.contains(category.name)) {
                                            selectedCategories - category.name
                                        } else {
                                            selectedCategories + category.name
                                        }
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Checkbox(
                                    checked = selectedCategories.contains(category.name),
                                    onCheckedChange = { 
                                        selectedCategories = if (selectedCategories.contains(category.name)) {
                                            selectedCategories - category.name
                                        } else {
                                            selectedCategories + category.name
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição do Serviço *") },
                    placeholder = { Text("Descreva o serviço que você precisa...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 8,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TaskGoGreen,
                        unfocusedBorderColor = TaskGoBorder
                    )
                )
                
                // REMOVIDO: Campo de Endereço
                // Localização será obtida automaticamente do perfil do usuário
                
                // REMOVIDO: Campos de Cidade e Estado
                // A localização será obtida automaticamente do perfil do usuário
                // O app detecta automaticamente quando o usuário muda de cidade
                
                // Budget
                OutlinedTextField(
                    value = budget,
                    onValueChange = { 
                        // Only allow numbers and decimal point
                        if (it.matches(Regex("^\\d*\\.?\\d*$"))) {
                            budget = it
                        }
                    },
                    label = { Text("Orçamento (R$)") },
                    placeholder = { Text("0.00") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Text("R$", color = TaskGoTextGray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TaskGoGreen,
                        unfocusedBorderColor = TaskGoBorder
                    )
                )
                
                // Due Date
                OutlinedTextField(
                    value = dueDate?.let { dateFormat.format(it) } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Data Desejada") },
                    placeholder = { Text("Selecione uma data") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Selecionar data")
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TaskGoGreen,
                        unfocusedBorderColor = TaskGoBorder
                    )
                )
                
                // Error message
                uiState.error?.let { error ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = TaskGoBackgroundWhite
                        ),
                        border = BorderStroke(1.dp, TaskGoBorder)
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Submit Button
                PrimaryButton(
                    text = if (uiState.isLoading) "Criando..." else stringResource(R.string.services_create_order_submit),
                    onClick = {
                        // Validation
                        if (selectedCategories.isEmpty()) {
                            return@PrimaryButton
                        }
                        if (description.isBlank()) {
                            return@PrimaryButton
                        }
                        
                        // CRÍTICO: Localização será obtida automaticamente do perfil do usuário pelo backend
                        // Não enviar location - backend usa getUserLocation() do perfil
                        val location = "" // Backend ignora e usa perfil do usuário
                        val budgetValue = budget.toDoubleOrNull()
                        val dueDateString = dueDate?.let { 
                            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(it) 
                        }
                        
                        // Usar a primeira categoria selecionada como categoria principal
                        val primaryCategory = selectedCategories.first()
                        
                        viewModel.viewModelScope.launch {
                            viewModel.createOrder(
                                category = primaryCategory,
                                description = description,
                                location = location,
                                budget = budgetValue,
                                dueDate = dueDateString
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading && selectedCategories.isNotEmpty()
                )
            }
        }
    }
    
    // Date Picker - Simple text input for now
    // In a production app, you might want to use a proper date picker library
    var dateInput by remember { mutableStateOf("") }
    
    LaunchedEffect(showDatePicker) {
        if (showDatePicker) {
            dateInput = dueDate?.let { dateFormat.format(it) } ?: ""
        }
    }
    
    if (showDatePicker) {
        AlertDialog(
            onDismissRequest = { showDatePicker = false },
            title = { Text("Selecionar Data") },
            text = {
                Column {
                    Text(
                        text = "Digite a data no formato DD/MM/AAAA",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = dateInput,
                        onValueChange = { 
                            // Format input as DD/MM/YYYY
                            val filtered = it.filter { char -> char.isDigit() }
                            val formatted = when {
                                filtered.isEmpty() -> ""
                                filtered.length <= 2 -> filtered
                                filtered.length <= 4 -> {
                                    if (filtered.length >= 2) {
                                        "${filtered.substring(0, 2)}/${filtered.substring(2)}"
                                    } else {
                                        filtered
                                    }
                                }
                                else -> {
                                    if (filtered.length >= 2) {
                                        val day = filtered.substring(0, 2)
                                        val month = if (filtered.length >= 4) {
                                            filtered.substring(2, 4)
                                        } else {
                                            filtered.substring(2)
                                        }
                                        val year = if (filtered.length > 4) {
                                            filtered.substring(4, minOf(8, filtered.length))
                                        } else {
                                            ""
                                        }
                                        if (year.isNotEmpty()) {
                                            "$day/$month/$year"
                                        } else {
                                            "$day/$month"
                                        }
                                    } else {
                                        filtered
                                    }
                                }
                            }
                            dateInput = formatted
                        },
                        placeholder = { Text("DD/MM/AAAA") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        try {
                            val parsedDate = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).parse(dateInput)
                            if (parsedDate != null && parsedDate.time >= System.currentTimeMillis()) {
                                dueDate = parsedDate
                                showDatePicker = false
                            }
                        } catch (e: Exception) {
                            // Invalid date format
                        }
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}


