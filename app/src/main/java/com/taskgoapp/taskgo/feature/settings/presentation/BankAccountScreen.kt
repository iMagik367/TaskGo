package com.taskgoapp.taskgo.feature.settings.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.taskgoapp.taskgo.core.design.AppTopBar
import com.taskgoapp.taskgo.core.design.OutlinedTextFieldWithValue
import com.taskgoapp.taskgo.core.theme.*
import com.taskgoapp.taskgo.core.data.BrazilianBanks
import com.taskgoapp.taskgo.core.utils.TextFormatters
import com.taskgoapp.taskgo.core.validation.DocumentValidator
import com.taskgoapp.taskgo.core.validation.ValidationResult
import com.taskgoapp.taskgo.data.firestore.models.BankAccount
import com.taskgoapp.taskgo.data.repository.FirestoreBankAccountRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.viewModelScope
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankAccountScreen(
    onBackClick: () -> Unit,
    onEditAccount: (String) -> Unit = { /* Fallback para dialog */ },
    viewModel: BankAccountViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Contas Bancárias",
                onBackClick = onBackClick
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                containerColor = TaskGoGreen
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Adicionar Conta",
                    tint = TaskGoBackgroundWhite
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
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
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.reload() },
                            colors = ButtonDefaults.buttonColors(containerColor = TaskGoGreen)
                        ) {
                            Text("Tentar Novamente")
                        }
                    }
                }
                uiState.accounts.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Nenhuma conta bancária cadastrada",
                                style = MaterialTheme.typography.titleMedium,
                                color = TaskGoTextGray
                            )
                            Text(
                                text = "Toque no botão + para adicionar uma conta",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TaskGoTextGray
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.accounts) { account ->
                            BankAccountCard(
                                account = account,
                                onEdit = { 
                                    // Navegar para tela de edição
                                    onEditAccount(account.id)
                                },
                                onDelete = { 
                                    scope.launch {
                                        viewModel.deleteAccount(account.id)
                                    }
                                },
                                onSetDefault = {
                                    scope.launch {
                                        viewModel.setDefaultAccount(account.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Dialog para adicionar/editar conta
    if (uiState.showDialog) {
        BankAccountDialog(
            account = uiState.editingAccount,
            onDismiss = { viewModel.hideDialog() },
            onSave = { account ->
                scope.launch {
                    viewModel.saveAccount(account)
                }
            }
        )
    }
}

@Composable
private fun BankAccountCard(
    account: BankAccount,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = TaskGoBackgroundWhite
        ),
        border = BorderStroke(1.dp, TaskGoBorder)
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
                        text = account.bankName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TaskGoTextBlack
                    )
                    Text(
                        text = "${account.accountType} • Ag: ${account.agency} • Conta: ${account.account}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TaskGoTextGray
                    )
                    Text(
                        text = account.accountHolderName,
                        style = MaterialTheme.typography.bodySmall,
                        color = TaskGoTextGray
                    )
                }
                if (account.isDefault) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = TaskGoGreen.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "Padrão",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = TaskGoGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!account.isDefault) {
                    OutlinedButton(
                        onClick = onSetDefault,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TaskGoGreen
                        )
                    ) {
                        Text("Definir como Padrão")
                    }
                }
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TaskGoTextBlack
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
                OutlinedButton(
                    onClick = onDelete,
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
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BankAccountDialog(
    account: BankAccount?,
    onDismiss: () -> Unit,
    onSave: (BankAccount) -> Unit
) {
    val documentValidator = remember { DocumentValidator() }
    
    // Estados para banco
    var selectedBank by remember(account) { 
        mutableStateOf(
            if (account?.bankCode?.isNotEmpty() == true) {
                BrazilianBanks.getBankByCode(account.bankCode)
            } else null
        )
    }
    var bankExpanded by remember { mutableStateOf(false) }
    var bankSearchQuery by remember { mutableStateOf("") }
    val filteredBanks = remember(bankSearchQuery) {
        if (bankSearchQuery.isBlank()) {
            BrazilianBanks.banks.take(50) // Limitar a 50 para performance
        } else {
            BrazilianBanks.searchBanks(bankSearchQuery).take(50)
        }
    }
    
    // Estados para outros campos
    var agency by remember { mutableStateOf(account?.agency ?: "") }
    var accountNumber by remember { mutableStateOf(account?.account ?: "") }
    var accountType by remember { mutableStateOf(account?.accountType ?: "CHECKING") }
    var holderName by remember { mutableStateOf(account?.accountHolderName ?: "") }
    var holderDocument by remember(account?.accountHolderDocument, account?.accountHolderDocumentType) { 
        mutableStateOf(
            TextFieldValue(
                text = account?.accountHolderDocument ?: "",
                selection = TextRange(account?.accountHolderDocument?.length ?: 0)
            )
        )
    }
    var documentType by remember { mutableStateOf(account?.accountHolderDocumentType ?: "CPF") }
    var isDefault by remember { mutableStateOf(account?.isDefault ?: false) }
    
    // Validações
    var agencyError by remember { mutableStateOf<String?>(null) }
    var accountNumberError by remember { mutableStateOf<String?>(null) }
    var holderNameError by remember { mutableStateOf<String?>(null) }
    var documentError by remember { mutableStateOf<String?>(null) }
    
    // Atualizar banco quando selecionado
    LaunchedEffect(selectedBank) {
        if (selectedBank != null) {
            bankSearchQuery = ""
        }
    }
    
    // Validar documento quando mudar
    LaunchedEffect(holderDocument.text, documentType) {
        val cleanDoc = holderDocument.text.replace(Regex("[^0-9]"), "")
        if (cleanDoc.isNotEmpty()) {
            val validation = if (documentType == "CPF") {
                documentValidator.validateCpf(holderDocument.text)
            } else {
                documentValidator.validateCnpj(holderDocument.text)
            }
            documentError = if (validation is ValidationResult.Invalid) validation.message else null
        } else {
            documentError = null
        }
    }
    
    // Validar agência
    val validateAgency: (String) -> Unit = { value ->
        val clean = value.replace(Regex("[^0-9]"), "")
        agencyError = when {
            clean.isEmpty() -> "Agência é obrigatória"
            clean.length < 4 -> "Agência deve ter pelo menos 4 dígitos"
            clean.length > 5 -> "Agência deve ter no máximo 5 dígitos"
            else -> null
        }
    }
    
    // Validar conta
    val validateAccount: (String) -> Unit = { value ->
        val clean = value.replace(Regex("[^0-9]"), "")
        accountNumberError = when {
            clean.isEmpty() -> "Número da conta é obrigatório"
            clean.length < 5 -> "Conta deve ter pelo menos 5 dígitos"
            clean.length > 12 -> "Conta deve ter no máximo 12 dígitos"
            else -> null
        }
    }
    
    // Validar nome
    val validateName: (String) -> Unit = { value ->
        holderNameError = when {
            value.isBlank() -> "Nome do titular é obrigatório"
            value.length < 3 -> "Nome deve ter pelo menos 3 caracteres"
            else -> null
        }
    }
    
    // Formatação do documento
    val onDocumentChange: (TextFieldValue) -> Unit = { newValue ->
        val formatted = if (documentType == "CPF") {
            TextFormatters.formatCpfWithCursor(newValue)
        } else {
            TextFormatters.formatCnpjWithCursor(newValue)
        }
        holderDocument = formatted
    }
    
    // Verificar se pode salvar
    val canSave = selectedBank != null &&
            agencyError == null &&
            accountNumberError == null &&
            holderNameError == null &&
            documentError == null &&
            agency.isNotBlank() &&
            accountNumber.isNotBlank() &&
            holderName.isNotBlank() &&
            holderDocument.text.replace(Regex("[^0-9]"), "").isNotEmpty()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = if (account == null) "Adicionar Conta Bancária" else "Editar Conta Bancária",
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Seleção de Banco
                ExposedDropdownMenuBox(
                    expanded = bankExpanded,
                    onExpandedChange = { bankExpanded = it }
                ) {
                    OutlinedTextField(
                        value = if (selectedBank != null) "${selectedBank!!.code} - ${selectedBank!!.name}" else bankSearchQuery,
                        onValueChange = { 
                            bankSearchQuery = it
                            bankExpanded = true
                            if (it.isBlank()) {
                                selectedBank = null
                            }
                        },
                        label = { Text("Banco *") },
                        placeholder = { Text("Busque por nome ou código (ex: 001, Itaú)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bankExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        readOnly = selectedBank != null,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (selectedBank == null && agency.isNotBlank()) MaterialTheme.colorScheme.error else TaskGoGreen,
                            unfocusedBorderColor = if (selectedBank == null && agency.isNotBlank()) MaterialTheme.colorScheme.error else TaskGoTextGray
                        ),
                        singleLine = true
                    )
                    
                    ExposedDropdownMenu(
                        expanded = bankExpanded,
                        onDismissRequest = { bankExpanded = false },
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        if (filteredBanks.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Nenhum banco encontrado") },
                                onClick = { bankExpanded = false }
                            )
                        } else {
                            filteredBanks.forEach { bank ->
                                DropdownMenuItem(
                                    text = { Text("${bank.code} - ${bank.name}") },
                                    onClick = {
                                        selectedBank = bank
                                        bankExpanded = false
                                        bankSearchQuery = ""
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Agência
                OutlinedTextField(
                    value = agency,
                    onValueChange = { 
                        agency = it.filter { char -> char.isDigit() }
                        validateAgency(agency)
                    },
                    label = { Text("Agência *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = agencyError != null,
                    supportingText = agencyError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
                )
                
                // Número da Conta
                OutlinedTextField(
                    value = accountNumber,
                    onValueChange = { 
                        accountNumber = it.filter { char -> char.isDigit() }
                        validateAccount(accountNumber)
                    },
                    label = { Text("Número da Conta *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = accountNumberError != null,
                    supportingText = accountNumberError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
                )
                
                // Tipo de Conta
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = accountType == "CHECKING",
                        onClick = { accountType = "CHECKING" },
                        label = { Text("Conta Corrente") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = accountType == "SAVINGS",
                        onClick = { accountType = "SAVINGS" },
                        label = { Text("Poupança") },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Nome do Titular
                OutlinedTextField(
                    value = holderName,
                    onValueChange = { 
                        holderName = it
                        validateName(holderName)
                    },
                    label = { Text("Nome do Titular *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = holderNameError != null,
                    supportingText = holderNameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
                )
                
                // Tipo de Documento
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = documentType == "CPF",
                        onClick = { 
                            documentType = "CPF"
                            holderDocument = TextFieldValue("")
                            documentError = null
                        },
                        label = { Text("CPF") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = documentType == "CNPJ",
                        onClick = { 
                            documentType = "CNPJ"
                            holderDocument = TextFieldValue("")
                            documentError = null
                        },
                        label = { Text("CNPJ") },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Documento (CPF/CNPJ)
                OutlinedTextFieldWithValue(
                    value = holderDocument,
                    onValueChange = onDocumentChange,
                    label = { Text(if (documentType == "CPF") "CPF *" else "CNPJ *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = documentError != null,
                    supportingText = documentError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
                )
                
                // Conta Padrão
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isDefault,
                        onCheckedChange = { isDefault = it }
                    )
                    Text(
                        text = "Definir como conta padrão",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanDocument = holderDocument.text.replace(Regex("[^0-9]"), "")
                    // CRÍTICO: Preservar o ID ao editar, criar novo ID ao adicionar
                    val newAccount = if (account != null) {
                        // Editando - preservar ID e outros campos
                        account.copy(
                            bankName = selectedBank?.name ?: "",
                            bankCode = selectedBank?.code ?: "",
                            agency = agency,
                            account = accountNumber,
                            accountType = accountType,
                            accountHolderName = holderName,
                            accountHolderDocument = cleanDocument,
                            accountHolderDocumentType = documentType,
                            isDefault = isDefault
                        )
                    } else {
                        // Criando nova conta
                        BankAccount(
                            id = "", // Será gerado pelo Firestore
                            bankName = selectedBank?.name ?: "",
                            bankCode = selectedBank?.code ?: "",
                            agency = agency,
                            account = accountNumber,
                            accountType = accountType,
                            accountHolderName = holderName,
                            accountHolderDocument = cleanDocument,
                            accountHolderDocumentType = documentType,
                            isDefault = isDefault
                        )
                    }
                    android.util.Log.d("BankAccountDialog", "Salvando conta - ID: ${newAccount.id}, isNew: ${account == null}")
                    onSave(newAccount)
                },
                enabled = canSave,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TaskGoGreen,
                    disabledContainerColor = TaskGoTextGray.copy(alpha = 0.3f)
                )
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

data class BankAccountUiState(
    val accounts: List<BankAccount> = emptyList(),
    val isLoading: Boolean = false,
    val showDialog: Boolean = false,
    val editingAccount: BankAccount? = null,
    val error: String? = null
)

@dagger.hilt.android.lifecycle.HiltViewModel
class BankAccountViewModel @Inject constructor(
    private val repository: FirestoreBankAccountRepository
) : androidx.lifecycle.ViewModel() {
    
    private val _uiState = kotlinx.coroutines.flow.MutableStateFlow(BankAccountUiState())
    val uiState: kotlinx.coroutines.flow.StateFlow<BankAccountUiState> = _uiState.asStateFlow()
    
    init {
        reload()
    }
    
    fun reload() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                repository.observeUserBankAccounts().collect { accounts ->
                    _uiState.value = _uiState.value.copy(
                        accounts = accounts,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("BankAccountVM", "Erro ao observar contas bancárias: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Erro ao carregar contas bancárias",
                    accounts = emptyList()
                )
            }
        }
    }
    
    fun showAddDialog() {
        _uiState.value = _uiState.value.copy(
            showDialog = true,
            editingAccount = null
        )
    }
    
    fun showEditDialog(account: BankAccount) {
        _uiState.value = _uiState.value.copy(
            showDialog = true,
            editingAccount = account
        )
    }
    
    fun hideDialog() {
        _uiState.value = _uiState.value.copy(
            showDialog = false,
            editingAccount = null
        )
    }
    
    suspend fun saveAccount(account: BankAccount) {
        _uiState.value = _uiState.value.copy(error = null, isLoading = true)
        android.util.Log.d("BankAccountVM", "Salvando conta bancária - ID: ${account.id}, isNew: ${account.id.isBlank()}")
        
        repository.saveBankAccount(account).fold(
            onSuccess = { accountId ->
                android.util.Log.d("BankAccountVM", "Conta bancária salva com sucesso: $accountId")
                _uiState.value = _uiState.value.copy(isLoading = false)
                hideDialog()
                reload()
            },
            onFailure = { e ->
                android.util.Log.e("BankAccountVM", "Erro ao salvar conta: ${e.message}", e)
                android.util.Log.e("BankAccountVM", "Stack trace:", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Erro ao salvar conta bancária"
                )
            }
        )
    }
    
    suspend fun deleteAccount(accountId: String) {
        _uiState.value = _uiState.value.copy(error = null)
        repository.deleteBankAccount(accountId).fold(
            onSuccess = {},
            onFailure = { e ->
                android.util.Log.e("BankAccountVM", "Erro ao deletar conta: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Erro ao deletar conta bancária"
                )
            }
        )
        reload()
    }
    
    suspend fun setDefaultAccount(accountId: String) {
        val account = _uiState.value.accounts.find { it.id == accountId }
        if (account != null) {
            saveAccount(account.copy(isDefault = true))
        }
    }
}

