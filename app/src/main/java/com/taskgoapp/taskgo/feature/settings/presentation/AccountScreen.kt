package com.taskgoapp.taskgo.feature.settings.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.hilt.navigation.compose.hiltViewModel
import com.taskgoapp.taskgo.R
import com.taskgoapp.taskgo.core.design.*
import com.taskgoapp.taskgo.core.model.AccountType
import com.taskgoapp.taskgo.feature.profile.presentation.ProfileViewModel
import com.taskgoapp.taskgo.data.repository.FirestoreAccountChangeRepository
import com.google.firebase.auth.FirebaseAuth
import com.taskgoapp.taskgo.core.theme.*
import com.taskgoapp.taskgo.core.validation.CepService
import com.taskgoapp.taskgo.core.validation.DocumentValidator
import com.taskgoapp.taskgo.core.validation.ValidationResult
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    onBackClick: () -> Unit,
    onNavigateToLogin: () -> Unit = {},
    onNavigateToBankAccounts: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val accountChangeRepository = remember { FirestoreAccountChangeRepository(com.google.firebase.firestore.FirebaseFirestore.getInstance()) }
    val firebaseAuth = remember { FirebaseAuth.getInstance() }
    
    // Estados para diálogo de solicitação de mudança
    var showChangeAccountDialog by remember { mutableStateOf(false) }
    var selectedNewAccountType by remember { mutableStateOf<AccountType?>(null) }
    var isSubmittingRequest by remember { mutableStateOf(false) }
    var requestSuccessMessage by remember { mutableStateOf<String?>(null) }
    var requestErrorMessage by remember { mutableStateOf<String?>(null) }
    
    // Usar um escopo persistente para operações que precisam continuar após o composable sair
    val persistentScope = remember {
        CoroutineScope(
            SupervisorJob() + Dispatchers.IO
        )
    }
    
    // Serviços de validação e busca
    val cepService = remember { CepService() }
    val documentValidator = remember { DocumentValidator() }
    
    // Estados locais - inicializar apenas uma vez quando dados carregarem
    var editedName by remember { mutableStateOf("") }
    var editedEmail by remember { mutableStateOf("") }
    var editedPhone by remember { mutableStateOf("") }
    var editedCity by remember { mutableStateOf("") }
    var editedProfession by remember { mutableStateOf("") }
    var editedType by remember { mutableStateOf(AccountType.CLIENTE) } // Somente leitura agora
    
    // Novos campos
    var editedCpf by remember { mutableStateOf("") }
    var editedRg by remember { mutableStateOf("") }
    var editedState by remember { mutableStateOf("") }
    var editedCountry by remember { mutableStateOf("Brasil") }
    var editedStreet by remember { mutableStateOf("") }
    var editedNumber by remember { mutableStateOf("") }
    var editedNeighborhood by remember { mutableStateOf("") }
    var editedZipCode by remember { mutableStateOf("") }
    var editedComplement by remember { mutableStateOf("") }
    
    // Inicializar apenas uma vez quando dados estiverem disponíveis
    var hasInitialized by remember { mutableStateOf(false) }
    LaunchedEffect(state.id, state.name) {
        if (!hasInitialized && state.id.isNotEmpty() && state.name.isNotEmpty()) {
            editedName = state.name
            editedEmail = state.email
            editedPhone = state.phone
            editedCity = state.city
            editedProfession = state.profession
            editedType = state.accountType
            editedCpf = state.cpf
            editedRg = state.rg
            editedState = state.state
            editedCountry = state.country.ifEmpty { "Brasil" }
            editedStreet = state.street
            editedNumber = state.number
            editedNeighborhood = state.neighborhood
            editedZipCode = state.zipCode
            editedComplement = state.complement
            hasInitialized = true
        }
    }
    
    // Estados de validação e loading
    var cpfError by remember { mutableStateOf<String?>(null) }
    var rgError by remember { mutableStateOf<String?>(null) }
    var cepError by remember { mutableStateOf<String?>(null) }
    var isLoadingCep by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    var pendingPhotoPickerAction by remember { mutableStateOf(false) }
    
    val hasImagePermission = remember {
        com.taskgoapp.taskgo.core.permissions.PermissionHandler.hasImageReadPermission(context)
    }
    
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.onAvatarSelected(uri.toString())
    }
    
    val imagePermissionLauncher = com.taskgoapp.taskgo.core.permissions.rememberImageReadPermissionLauncher(
        onPermissionGranted = {
            pendingPhotoPickerAction = true
        },
        onPermissionDenied = {
            // Permissão negada
        }
    )
    
    // Executar ação do photoPicker quando permissão for concedida
    LaunchedEffect(pendingPhotoPickerAction) {
        if (pendingPhotoPickerAction && com.taskgoapp.taskgo.core.permissions.PermissionHandler.hasImageReadPermission(context)) {
            photoPicker.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            pendingPhotoPickerAction = false
        }
    }
    
    // Função helper para salvar com retry e backoff
    suspend fun retrySaveWithBackoff(block: suspend () -> Unit) {
        var retryCount = 0
        val maxRetries = 3
        var delayMs = 1000L
        
        while (retryCount < maxRetries) {
            try {
                block()
                return // Sucesso, sair do loop
            } catch (e: Exception) {
                retryCount++
                val isSecureTokenError = e.message?.contains("SecureToken", ignoreCase = true) == true ||
                                        e.message?.contains("securetoken", ignoreCase = true) == true
                
                if (isSecureTokenError) {
                    android.util.Log.w("AccountScreen", "Erro do Secure Token API (configuração do Google Cloud necessária): ${e.message}")
                    // Não tentar novamente para erros de Secure Token API
                    break
                }
                
                if (retryCount < maxRetries) {
                    android.util.Log.w("AccountScreen", "Tentativa $retryCount/$maxRetries falhou, tentando novamente em ${delayMs}ms: ${e.message}")
                    delay(delayMs)
                    delayMs *= 2 // Backoff exponencial
                } else {
                    android.util.Log.e("AccountScreen", "Erro ao salvar após $maxRetries tentativas: ${e.message}", e)
                }
            }
        }
    }
    
    // Debounce para salvar automaticamente após 2 segundos de inatividade
    var saveJob by remember { mutableStateOf<Job?>(null) }
    
    LaunchedEffect(editedName, editedEmail, editedPhone, editedCity, editedProfession, 
                   editedCpf, editedRg, editedState, editedCountry, editedStreet, editedNumber, 
                   editedNeighborhood, editedZipCode, editedComplement) {
        if (hasInitialized) {
            // Cancelar job anterior
            saveJob?.cancel()
            
            // Atualizar ViewModel imediatamente
            viewModel.onNameChange(editedName)
            viewModel.onEmailChange(editedEmail)
            viewModel.onPhoneChange(editedPhone)
            viewModel.onCityChange(editedCity)
            viewModel.onProfessionChange(editedProfession)
            // Não alterar accountType aqui - apenas exibir o atual
            viewModel.onCpfChange(editedCpf)
            viewModel.onRgChange(editedRg)
            viewModel.onStateChange(editedState)
            viewModel.onCountryChange(editedCountry)
            viewModel.onStreetChange(editedStreet)
            viewModel.onNumberChange(editedNumber)
            viewModel.onNeighborhoodChange(editedNeighborhood)
            viewModel.onZipCodeChange(editedZipCode)
            viewModel.onComplementChange(editedComplement)
            
            // Criar novo job com debounce aumentado
            saveJob = persistentScope.launch {
                delay(2000) // 2 segundos de debounce para evitar cancelamentos
                retrySaveWithBackoff {
                    viewModel.save()
                    android.util.Log.d("AccountScreen", "Dados da conta salvos automaticamente")
                }
            }
        }
    }
    
    // Salvar quando sair da tela (garantir salvamento final)
    DisposableEffect(Unit) {
        onDispose {
            // Cancelar job pendente
            saveJob?.cancel()
            
            // Atualizar ViewModel com os valores finais
            viewModel.onNameChange(editedName)
            viewModel.onEmailChange(editedEmail)
            viewModel.onPhoneChange(editedPhone)
            viewModel.onCityChange(editedCity)
            viewModel.onProfessionChange(editedProfession)
            // Não alterar accountType aqui - apenas exibir o atual
            viewModel.onCpfChange(editedCpf)
            viewModel.onRgChange(editedRg)
            viewModel.onStateChange(editedState)
            viewModel.onCountryChange(editedCountry)
            viewModel.onStreetChange(editedStreet)
            viewModel.onNumberChange(editedNumber)
            viewModel.onNeighborhoodChange(editedNeighborhood)
            viewModel.onZipCodeChange(editedZipCode)
            viewModel.onComplementChange(editedComplement)
            
            // Salvar no Firebase usando escopo persistente com retry
            persistentScope.launch {
                retrySaveWithBackoff {
                    viewModel.save()
                    android.util.Log.d("AccountScreen", "Dados da conta salvos ao sair da tela")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.settings_account),
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Cabeçalho com avatar, nome e função
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(TaskGoSurfaceGray)
                        .clickable {
                            if (hasImagePermission) {
                                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            } else {
                                imagePermissionLauncher.launch(com.taskgoapp.taskgo.core.permissions.PermissionHandler.getImageReadPermission())
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (!state.avatarUri.isNullOrBlank()) {
                        AsyncImage(
                            model = state.avatarUri,
                            contentDescription = stringResource(R.string.cd_user_avatar),
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            painter = painterResource(TGIcons.Profile),
                            contentDescription = stringResource(R.string.cd_user_avatar),
                            tint = TaskGoTextGray,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Toque para alterar foto",
                    style = FigmaProductDescription,
                    color = TaskGoTextGray,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = editedName, style = FigmaProductName, color = TaskGoTextBlack, fontWeight = FontWeight.Bold)
                Text(text = editedProfession, style = FigmaProductDescription, color = TaskGoTextGray)
            }

            // Formulário completo
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Informações Pessoais
                Text(
                    text = "Informações Pessoais",
                    style = FigmaProductName,
                    color = TaskGoTextBlack,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(text = "Nome Completo", style = FigmaButtonText, color = TaskGoTextBlack)
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(painter = painterResource(TGIcons.Profile), contentDescription = null) }
                )

                Text(text = "Email", style = FigmaButtonText, color = TaskGoTextBlack)
                OutlinedTextField(
                    value = editedEmail,
                    onValueChange = { editedEmail = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(painter = painterResource(TGIcons.Info), contentDescription = null) }
                )

                Text(text = "Telefone", style = FigmaButtonText, color = TaskGoTextBlack)
                OutlinedTextField(
                    value = editedPhone,
                    onValueChange = { editedPhone = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(painter = painterResource(TGIcons.Phone), contentDescription = null) }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "CPF", style = FigmaButtonText, color = TaskGoTextBlack)
                        OutlinedTextField(
                            value = editedCpf,
                            onValueChange = { newValue ->
                                val cleanValue = newValue.replace(Regex("[^0-9]"), "")
                                if (cleanValue.length <= 11) {
                                    editedCpf = cleanValue
                                    if (cleanValue.length == 11) {
                                        editedCpf = documentValidator.formatCpf(cleanValue)
                                        val validation = documentValidator.validateCpf(editedCpf)
                                        cpfError = if (validation is ValidationResult.Invalid) validation.message else null
                                    } else {
                                        cpfError = null
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("000.000.000-00") },
                            isError = cpfError != null,
                            supportingText = cpfError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "RG", style = FigmaButtonText, color = TaskGoTextBlack)
                        OutlinedTextField(
                            value = editedRg,
                            onValueChange = { newValue ->
                                editedRg = newValue.uppercase()
                                if (editedRg.length >= 6) {
                                    val validation = documentValidator.validateRg(editedRg, editedState.takeIf { it.isNotEmpty() })
                                    rgError = if (validation is ValidationResult.Invalid) validation.message else null
                                } else {
                                    rgError = null
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("00.000.000-0") },
                            isError = rgError != null,
                            supportingText = rgError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
                        )
                    }
                }

                // Endereço
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Endereço",
                    style = FigmaProductName,
                    color = TaskGoTextBlack,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
                )

                Text(text = "CEP", style = FigmaButtonText, color = TaskGoTextBlack)
                OutlinedTextField(
                    value = editedZipCode,
                    onValueChange = { newValue ->
                        val cleanValue = newValue.replace(Regex("[^0-9]"), "")
                        if (cleanValue.length <= 8) {
                            editedZipCode = cleanValue
                            cepError = null
                            
                            if (cleanValue.length == 8) {
                                editedZipCode = documentValidator.formatCep(cleanValue)
                                
                                scope.launch {
                                    isLoadingCep = true
                                    cepError = null
                                    
                                    delay(500)
                                    
                                    cepService.searchCep(cleanValue).fold(
                                        onSuccess = { cepResult ->
                                            editedStreet = cepResult.logradouro
                                            editedNeighborhood = cepResult.bairro
                                            editedCity = cepResult.localidade
                                            editedState = cepResult.uf
                                            cepResult.complemento?.let { editedComplement = it }
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
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("00000-000") },
                    trailingIcon = {
                        if (isLoadingCep) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    },
                    isError = cepError != null,
                    supportingText = cepError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    enabled = !isLoadingCep
                )

                Text(text = "Rua", style = FigmaButtonText, color = TaskGoTextBlack)
                OutlinedTextField(
                    value = editedStreet,
                    onValueChange = { editedStreet = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(2f)) {
                        Text(text = "Número", style = FigmaButtonText, color = TaskGoTextBlack)
                        OutlinedTextField(
                            value = editedNumber,
                            onValueChange = { editedNumber = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    Column(modifier = Modifier.weight(3f)) {
                        Text(text = "Complemento", style = FigmaButtonText, color = TaskGoTextBlack)
                        OutlinedTextField(
                            value = editedComplement,
                            onValueChange = { editedComplement = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("Apto, Bloco, etc.") }
                        )
                    }
                }

                Text(text = "Bairro", style = FigmaButtonText, color = TaskGoTextBlack)
                OutlinedTextField(
                    value = editedNeighborhood,
                    onValueChange = { editedNeighborhood = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(2f)) {
                        Text(text = "Cidade", style = FigmaButtonText, color = TaskGoTextBlack)
                        OutlinedTextField(
                            value = editedCity,
                            onValueChange = { editedCity = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Estado", style = FigmaButtonText, color = TaskGoTextBlack)
                        OutlinedTextField(
                            value = editedState,
                            onValueChange = { editedState = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("UF") }
                        )
                    }
                }

                Text(text = "País", style = FigmaButtonText, color = TaskGoTextBlack)
                OutlinedTextField(
                    value = editedCountry,
                    onValueChange = { editedCountry = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Tipo de Conta Atual (somente leitura)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tipo de Conta Atual",
                    style = FigmaProductName,
                    color = TaskGoTextBlack,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = TaskGoSurface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (editedType) {
                                AccountType.PRESTADOR -> stringResource(R.string.profile_account_type_provider)
                                AccountType.VENDEDOR -> stringResource(R.string.profile_account_type_seller)
                                AccountType.CLIENTE -> stringResource(R.string.profile_account_type_client)
                            },
                            style = FigmaProductName,
                            color = TaskGoTextBlack
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Botão para gerenciar contas bancárias (apenas para vendedores)
                if (state.accountType == AccountType.VENDEDOR) {
                    OutlinedButton(
                        onClick = { onNavigateToBankAccounts() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TaskGoGreen
                        ),
                        border = BorderStroke(1.dp, TaskGoGreen)
                    ) {
                        Text(
                            text = "Gerenciar Contas Bancárias",
                            style = FigmaButtonText,
                            color = TaskGoGreen
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Botão para solicitar mudança de modo de conta
                OutlinedButton(
                    onClick = { showChangeAccountDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TaskGoGreen
                    ),
                    border = BorderStroke(1.dp, TaskGoGreen)
                ) {
                    Text(
                        text = "Solicitar Mudança de Modo de Conta",
                        style = FigmaButtonText,
                        color = TaskGoGreen
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Botão Sair da Conta
                OutlinedButton(
                    onClick = {
                        // Fazer logout
                        firebaseAuth.signOut()
                        // Navegar para tela de login
                        onNavigateToLogin()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Text(
                        text = stringResource(R.string.settings_logout),
                        style = FigmaButtonText,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
    
    // Diálogo para solicitar mudança de modo de conta
    if (showChangeAccountDialog) {
        AlertDialog(
            onDismissRequest = { 
                showChangeAccountDialog = false
                selectedNewAccountType = null
                requestSuccessMessage = null
                requestErrorMessage = null
            },
            title = {
                Text(
                    text = "Solicitar Mudança de Modo de Conta",
                    style = FigmaProductName,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Selecione o novo modo de conta desejado. A mudança será processada após 1 dia útil.",
                        style = FigmaProductDescription,
                        color = TaskGoTextGray
                    )
                    
                    if (requestSuccessMessage != null) {
                        Text(
                            text = requestSuccessMessage!!,
                            style = FigmaProductDescription,
                            color = TaskGoGreen
                        )
                    }
                    
                    if (requestErrorMessage != null) {
                        Text(
                            text = requestErrorMessage!!,
                            style = FigmaProductDescription,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Opção Prestador
                        if (editedType != AccountType.PRESTADOR) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedNewAccountType = AccountType.PRESTADOR },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedNewAccountType == AccountType.PRESTADOR) TaskGoGreen.copy(alpha = 0.1f) else TaskGoSurface
                                ),
                                border = if (selectedNewAccountType == AccountType.PRESTADOR) BorderStroke(2.dp, TaskGoGreen) else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedNewAccountType == AccountType.PRESTADOR,
                                        onClick = { selectedNewAccountType = AccountType.PRESTADOR }
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = stringResource(R.string.profile_account_type_provider),
                                        style = FigmaProductName,
                                        color = TaskGoTextBlack
                                    )
                                }
                            }
                        }
                        
                        // Opção Vendedor
                        if (editedType != AccountType.VENDEDOR) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedNewAccountType = AccountType.VENDEDOR },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedNewAccountType == AccountType.VENDEDOR) TaskGoGreen.copy(alpha = 0.1f) else TaskGoSurface
                                ),
                                border = if (selectedNewAccountType == AccountType.VENDEDOR) BorderStroke(2.dp, TaskGoGreen) else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedNewAccountType == AccountType.VENDEDOR,
                                        onClick = { selectedNewAccountType = AccountType.VENDEDOR }
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = stringResource(R.string.profile_account_type_seller),
                                        style = FigmaProductName,
                                        color = TaskGoTextBlack
                                    )
                                }
                            }
                        }
                        
                        // Opção Cliente
                        if (editedType != AccountType.CLIENTE) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedNewAccountType = AccountType.CLIENTE },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedNewAccountType == AccountType.CLIENTE) TaskGoGreen.copy(alpha = 0.1f) else TaskGoSurface
                                ),
                                border = if (selectedNewAccountType == AccountType.CLIENTE) BorderStroke(2.dp, TaskGoGreen) else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedNewAccountType == AccountType.CLIENTE,
                                        onClick = { selectedNewAccountType = AccountType.CLIENTE }
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = stringResource(R.string.profile_account_type_client),
                                        style = FigmaProductName,
                                        color = TaskGoTextBlack
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val userId = firebaseAuth.currentUser?.uid
                        if (userId != null && selectedNewAccountType != null) {
                            isSubmittingRequest = true
                            requestErrorMessage = null
                            requestSuccessMessage = null
                            
                            scope.launch {
                                val result = accountChangeRepository.createAccountChangeRequest(
                                    userId = userId,
                                    currentAccountType = editedType.name,
                                    requestedAccountType = selectedNewAccountType!!.name
                                )
                                
                                isSubmittingRequest = false
                                
                                result.fold(
                                    onSuccess = {
                                        requestSuccessMessage = "Solicitação enviada com sucesso! A mudança será processada após 1 dia útil."
                                        // Fechar diálogo após 2 segundos
                                        kotlinx.coroutines.delay(2000)
                                        showChangeAccountDialog = false
                                        selectedNewAccountType = null
                                        requestSuccessMessage = null
                                    },
                                    onFailure = { exception ->
                                        requestErrorMessage = "Erro ao enviar solicitação: ${exception.message}"
                                    }
                                )
                            }
                        } else {
                            requestErrorMessage = "Por favor, selecione um novo modo de conta"
                        }
                    },
                    enabled = !isSubmittingRequest && selectedNewAccountType != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TaskGoGreen
                    )
                ) {
                    if (isSubmittingRequest) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White
                        )
                    } else {
                        Text("Enviar Solicitação", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showChangeAccountDialog = false
                        selectedNewAccountType = null
                        requestSuccessMessage = null
                        requestErrorMessage = null
                    }
                ) {
                    Text("Cancelar", color = TaskGoTextGray)
                }
            }
        )
    }
    
}


