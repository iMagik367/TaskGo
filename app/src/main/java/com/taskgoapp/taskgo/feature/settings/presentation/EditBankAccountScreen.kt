package com.taskgoapp.taskgo.feature.settings.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBankAccountScreen(
    accountId: String,
    onBackClick: () -> Unit,
    viewModel: BankAccountViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    
    // Carregar conta ao entrar na tela
    LaunchedEffect(accountId) {
        val account = uiState.accounts.find { it.id == accountId }
        if (account != null) {
            viewModel.showEditDialog(account)
        }
    }
    
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Editar Conta Bancária",
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        if (uiState.editingAccount == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = TaskGoGreen)
            }
        } else {
            EditBankAccountForm(
                account = uiState.editingAccount!!,
                onSave = { account ->
                    scope.launch {
                        viewModel.saveAccount(account)
                        if (uiState.error == null) {
                            onBackClick()
                        }
                    }
                },
                onCancel = onBackClick,
                isLoading = uiState.isLoading,
                error = uiState.error,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditBankAccountForm(
    account: BankAccount,
    onSave: (BankAccount) -> Unit,
    onCancel: () -> Unit,
    isLoading: Boolean,
    error: String?,
    modifier: Modifier = Modifier
) {
    val documentValidator = remember { DocumentValidator() }
    
    // Estados para banco
    var selectedBank by remember(account) { 
        mutableStateOf(
            if (account.bankCode.isNotEmpty()) {
                BrazilianBanks.getBankByCode(account.bankCode)
            } else null
        )
    }
    var bankExpanded by remember { mutableStateOf(false) }
    var bankSearchQuery by remember { mutableStateOf("") }
    val filteredBanks = remember(bankSearchQuery) {
        if (bankSearchQuery.isBlank()) {
            BrazilianBanks.banks.take(50)
        } else {
            BrazilianBanks.searchBanks(bankSearchQuery).take(50)
        }
    }
    
    // Estados para outros campos
    var agency by remember { mutableStateOf(account.agency) }
    var accountNumber by remember { mutableStateOf(account.account) }
    var accountType by remember { mutableStateOf(account.accountType) }
    var holderName by remember { mutableStateOf(account.accountHolderName) }
    var holderDocument by remember { 
        mutableStateOf(
            TextFieldValue(
                text = account.accountHolderDocument,
                selection = TextRange(account.accountHolderDocument.length)
            )
        )
    }
    var documentType by remember { mutableStateOf(account.accountHolderDocumentType) }
    var isDefault by remember { mutableStateOf(account.isDefault) }
    
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
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Mensagem de erro
        error?.let {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = it,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        
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
        
        // Botões de ação
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancelar")
            }
            Button(
                onClick = {
                    val cleanDocument = holderDocument.text.replace(Regex("[^0-9]"), "")
                    val updatedAccount = account.copy(
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
                    onSave(updatedAccount)
                },
                enabled = canSave && !isLoading,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TaskGoGreen,
                    disabledContainerColor = TaskGoTextGray.copy(alpha = 0.3f)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = TaskGoBackgroundWhite
                    )
                } else {
                    Text("Salvar")
                }
            }
        }
    }
}
