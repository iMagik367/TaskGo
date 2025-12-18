package com.taskgoapp.taskgo.feature.settings.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.taskgoapp.taskgo.core.design.AppTopBar
import com.taskgoapp.taskgo.core.theme.*
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
            if (uiState.accounts.isEmpty() && !uiState.isLoading) {
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
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.accounts) { account ->
                        BankAccountCard(
                            account = account,
                            onEdit = { viewModel.showEditDialog(account) },
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
            containerColor = if (account.isDefault) TaskGoGreen.copy(alpha = 0.1f) else TaskGoSurface
        )
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

@Composable
private fun BankAccountDialog(
    account: BankAccount?,
    onDismiss: () -> Unit,
    onSave: (BankAccount) -> Unit
) {
    var bankName by remember { mutableStateOf(account?.bankName ?: "") }
    var bankCode by remember { mutableStateOf(account?.bankCode ?: "") }
    var agency by remember { mutableStateOf(account?.agency ?: "") }
    var accountNumber by remember { mutableStateOf(account?.account ?: "") }
    var accountType by remember { mutableStateOf(account?.accountType ?: "CHECKING") }
    var holderName by remember { mutableStateOf(account?.accountHolderName ?: "") }
    var holderDocument by remember { mutableStateOf(account?.accountHolderDocument ?: "") }
    var documentType by remember { mutableStateOf(account?.accountHolderDocumentType ?: "CPF") }
    var isDefault by remember { mutableStateOf(account?.isDefault ?: false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (account == null) "Adicionar Conta Bancária" else "Editar Conta Bancária") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    label = { Text("Nome do Banco") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = bankCode,
                    onValueChange = { bankCode = it },
                    label = { Text("Código do Banco") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = agency,
                    onValueChange = { agency = it },
                    label = { Text("Agência") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = accountNumber,
                    onValueChange = { accountNumber = it },
                    label = { Text("Conta") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
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
                
                OutlinedTextField(
                    value = holderName,
                    onValueChange = { holderName = it },
                    label = { Text("Nome do Titular") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = documentType == "CPF",
                        onClick = { documentType = "CPF" },
                        label = { Text("CPF") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = documentType == "CNPJ",
                        onClick = { documentType = "CNPJ" },
                        label = { Text("CNPJ") },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                OutlinedTextField(
                    value = holderDocument,
                    onValueChange = { holderDocument = it },
                    label = { Text(if (documentType == "CPF") "CPF" else "CNPJ") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
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
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newAccount = (account ?: BankAccount()).copy(
                        bankName = bankName,
                        bankCode = bankCode,
                        agency = agency,
                        account = accountNumber,
                        accountType = accountType,
                        accountHolderName = holderName,
                        accountHolderDocument = holderDocument,
                        accountHolderDocumentType = documentType,
                        isDefault = isDefault
                    )
                    onSave(newAccount)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = TaskGoGreen
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
    val editingAccount: BankAccount? = null
)

@dagger.hilt.android.lifecycle.HiltViewModel
class BankAccountViewModel @Inject constructor(
    private val repository: FirestoreBankAccountRepository
) : androidx.lifecycle.ViewModel() {
    
    private val _uiState = kotlinx.coroutines.flow.MutableStateFlow(BankAccountUiState())
    val uiState: kotlinx.coroutines.flow.StateFlow<BankAccountUiState> = _uiState.asStateFlow()
    
    init {
        viewModelScope.launch {
            repository.observeUserBankAccounts().collect { accounts ->
                _uiState.value = _uiState.value.copy(accounts = accounts, isLoading = false)
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
        repository.saveBankAccount(account).fold(
            onSuccess = {
                hideDialog()
            },
            onFailure = { e ->
                android.util.Log.e("BankAccountVM", "Erro ao salvar conta: ${e.message}", e)
            }
        )
    }
    
    suspend fun deleteAccount(accountId: String) {
        repository.deleteBankAccount(accountId).fold(
            onSuccess = {},
            onFailure = { e ->
                android.util.Log.e("BankAccountVM", "Erro ao deletar conta: ${e.message}", e)
            }
        )
    }
    
    suspend fun setDefaultAccount(accountId: String) {
        val account = _uiState.value.accounts.find { it.id == accountId }
        if (account != null) {
            saveAccount(account.copy(isDefault = true))
        }
    }
}

