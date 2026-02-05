package com.taskgoapp.taskgo.core.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.taskgoapp.taskgo.core.model.AccountType
import com.taskgoapp.taskgo.core.data.models.ServiceCategory
import com.taskgoapp.taskgo.core.theme.*
import com.taskgoapp.taskgo.core.validation.DocumentValidator
import com.taskgoapp.taskgo.feature.auth.presentation.LoginViewModel
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.taskgoapp.taskgo.core.utils.TextFormatters
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Icon
import com.taskgoapp.taskgo.core.design.EnhancedOutlinedTextField
import androidx.compose.material3.ExperimentalMaterial3Api

/**
 * Dados coletados do dialog de seleção de tipo de conta
 */
data class AccountTypeSelectionData(
    val accountType: AccountType,
    val selectedCategories: Set<String> = emptySet(),
    val cpf: String? = null,
    val cnpj: String? = null,
    val rg: String? = null,
    val documentType: String? = null, // "CPF" ou "CNPJ"
    val state: String? = null,
    val city: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountTypeSelectionDialog(
    onAccountTypeSelected: (AccountTypeSelectionData) -> Unit,
    onDismiss: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    var selectedAccountType by remember { 
        mutableStateOf(AccountType.CLIENTE) 
    }
    
    // Estados para documentos
    var documentType by remember { mutableStateOf<String?>(null) } // "CPF" ou "CNPJ"
    var cpf by remember { mutableStateOf(TextFieldValue("")) }
    var cnpj by remember { mutableStateOf(TextFieldValue("")) }
    var rg by remember { mutableStateOf("") }
    
    // Estados de validação
    var cpfError by remember { mutableStateOf<String?>(null) }
    var cnpjError by remember { mutableStateOf<String?>(null) }
    var rgError by remember { mutableStateOf<String?>(null) }
    
    // Categorias de serviço
    var selectedCategories by remember { mutableStateOf<Set<String>>(emptySet()) }
    val serviceCategories by viewModel.serviceCategories.collectAsState()
    
    // Campos de localização (cidade e estado)
    var selectedState by remember { mutableStateOf<String?>(null) }
    var selectedCity by remember { mutableStateOf<String?>(null) }
    val availableStates = com.taskgoapp.taskgo.core.data.BrazilianCities.allStates
    val availableCities = remember(selectedState) {
        selectedState?.let { com.taskgoapp.taskgo.core.data.BrazilianCities.getCitiesForState(it) } ?: emptyList()
    }
    
    // Validar se pode continuar
    val canContinue = when (selectedAccountType) {
        AccountType.PARCEIRO -> {
            selectedState != null &&
            selectedCity != null &&
            documentType != null && 
            ((documentType == "CPF" && cpf.text.isNotBlank() && cpfError == null) ||
             (documentType == "CNPJ" && cnpj.text.isNotBlank() && cnpjError == null)) &&
            rg.isNotBlank() && rgError == null &&
            selectedCategories.isNotEmpty()
        }
        AccountType.CLIENTE -> {
            selectedState != null && selectedCity != null
        }
        else -> true
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = TaskGoBackgroundWhite
            ),
            border = BorderStroke(1.dp, TaskGoBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Selecione o tipo de conta",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TaskGoTextBlack,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Escolha o tipo de conta que melhor descreve você:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TaskGoTextGray
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AccountTypeOption(
                        accountType = AccountType.CLIENTE,
                        title = "Cliente",
                        description = "Contratar serviços e comprar produtos",
                        isSelected = selectedAccountType == AccountType.CLIENTE,
                        onClick = { 
                            selectedAccountType = AccountType.CLIENTE
                            documentType = null
                            cpf = TextFieldValue("")
                            cnpj = TextFieldValue("")
                            rg = ""
                            selectedCategories = emptySet()
                        }
                    )
                    
                    AccountTypeOption(
                        accountType = AccountType.PARCEIRO,
                        title = "Parceiro",
                        description = "Oferecer serviços e vender produtos",
                        isSelected = selectedAccountType == AccountType.PARCEIRO,
                        onClick = { selectedAccountType = AccountType.PARCEIRO }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Campos de Localização (Estado e Cidade) - Obrigatórios para todos
                Text(
                    text = "Localização",
                    style = MaterialTheme.typography.titleMedium,
                    color = TaskGoTextBlack,
                    fontWeight = FontWeight.Bold
                )
                
                // Select Box de Estado
                var expandedState by remember { mutableStateOf(false) }
                val stateText: String = (selectedState ?: "").ifEmpty { "Selecione o estado" }
                ExposedDropdownMenuBox(
                    expanded = expandedState,
                    onExpandedChange = { expandedState = !expandedState }
                ) {
                    OutlinedTextField(
                        value = stateText,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Estado (Obrigatório)", color = TaskGoTextGray, fontSize = 14.sp) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedState) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TaskGoGreen,
                            unfocusedBorderColor = TaskGoTextGray,
                            focusedLabelColor = TaskGoTextGray,
                            unfocusedLabelColor = TaskGoTextGray
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expandedState,
                        onDismissRequest = { expandedState = false }
                    ) {
                        availableStates.forEach { state ->
                            DropdownMenuItem(
                                text = { Text(state) },
                                onClick = {
                                    selectedState = state
                                    selectedCity = null
                                    expandedState = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Select Box de Cidade
                var expandedCity by remember { mutableStateOf(false) }
                val cityText: String = (selectedCity ?: "").ifEmpty { if (selectedState == null) "Selecione o estado primeiro" else "Selecione a cidade" }
                val isCityEnabled = selectedState != null && availableCities.isNotEmpty()
                ExposedDropdownMenuBox(
                    expanded = expandedCity && isCityEnabled,
                    onExpandedChange = { if (isCityEnabled) expandedCity = !expandedCity }
                ) {
                    OutlinedTextField(
                        value = cityText,
                        onValueChange = {},
                        readOnly = true,
                        enabled = isCityEnabled,
                        label = { Text("Cidade (Obrigatório)", color = TaskGoTextGray, fontSize = 14.sp) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCity && isCityEnabled) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TaskGoGreen,
                            unfocusedBorderColor = TaskGoTextGray,
                            focusedLabelColor = TaskGoTextGray,
                            unfocusedLabelColor = TaskGoTextGray,
                            disabledBorderColor = TaskGoTextGray.copy(alpha = 0.5f),
                            disabledLabelColor = TaskGoTextGray.copy(alpha = 0.5f)
                        )
                    )
                    if (isCityEnabled) {
                        ExposedDropdownMenu(
                            expanded = expandedCity,
                            onDismissRequest = { expandedCity = false }
                        ) {
                            availableCities.forEach { city ->
                                DropdownMenuItem(
                                    text = { Text(city) },
                                    onClick = {
                                        selectedCity = city
                                        expandedCity = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Campos adicionais para Parceiro
                if (selectedAccountType == AccountType.PARCEIRO) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Documentos Obrigatórios",
                        style = MaterialTheme.typography.titleMedium,
                        color = TaskGoTextBlack,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Seleção de tipo de documento
                    Text(
                        text = "Tipo de Documento *",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TaskGoTextBlack,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilterChip(
                            selected = documentType == "CPF",
                            onClick = { 
                                documentType = "CPF"
                                cnpj = TextFieldValue("")
                                cnpjError = null
                            },
                            label = { Text("CPF") },
                            modifier = Modifier.weight(1f)
                        )
                        
                        FilterChip(
                            selected = documentType == "CNPJ",
                            onClick = { 
                                documentType = "CNPJ"
                                cpf = TextFieldValue("")
                                cpfError = null
                            },
                            label = { Text("CNPJ") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    // Campo CPF
                    if (documentType == "CPF") {
                        OutlinedTextField(
                            value = cpf,
                            onValueChange = { newValue ->
                                val formatted = TextFormatters.formatCpfWithCursor(newValue)
                                cpf = formatted
                                val validator = DocumentValidator()
                                val validation = validator.validateCpf(formatted.text.replace(Regex("[^0-9]"), ""))
                                cpfError = if (validation is com.taskgoapp.taskgo.core.validation.ValidationResult.Invalid) {
                                    validation.message
                                } else null
                            },
                            label = { Text("CPF *") },
                            placeholder = { Text("000.000.000-00") },
                            isError = cpfError != null,
                            supportingText = if (cpfError != null) {
                                { Text(cpfError ?: "", color = MaterialTheme.colorScheme.error) }
                            } else null,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                    
                    // Campo CNPJ
                    if (documentType == "CNPJ") {
                        OutlinedTextField(
                            value = cnpj,
                            onValueChange = { newValue ->
                                val formatted = TextFormatters.formatCnpjWithCursor(newValue)
                                cnpj = formatted
                                val validator = DocumentValidator()
                                val validation = validator.validateCnpj(formatted.text.replace(Regex("[^0-9]"), ""))
                                cnpjError = if (validation is com.taskgoapp.taskgo.core.validation.ValidationResult.Invalid) {
                                    validation.message
                                } else null
                            },
                            label = { Text("CNPJ *") },
                            placeholder = { Text("00.000.000/0000-00") },
                            isError = cnpjError != null,
                            supportingText = if (cnpjError != null) {
                                { Text(cnpjError ?: "", color = MaterialTheme.colorScheme.error) }
                            } else null,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                    
                    // Campo RG
                    OutlinedTextField(
                        value = rg,
                        onValueChange = { newValue ->
                            rg = newValue
                            if (newValue.isNotBlank() && newValue.length < 5) {
                                rgError = "RG deve ter pelo menos 5 caracteres"
                            } else {
                                rgError = null
                            }
                        },
                        label = { Text("RG *") },
                        placeholder = { Text("00.000.000-0") },
                        isError = rgError != null,
                        supportingText = if (rgError != null) {
                            { Text(rgError ?: "", color = MaterialTheme.colorScheme.error) }
                        } else null,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Categorias de Serviço
                    Text(
                        text = "Categorias de Serviço *",
                        style = MaterialTheme.typography.titleMedium,
                        color = TaskGoTextBlack,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "Selecione pelo menos uma categoria de serviço que você oferece:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TaskGoTextGray
                    )
                    
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(serviceCategories) { category ->
                            CategoryCheckbox(
                                category = category,
                                isSelected = selectedCategories.contains(category.name),
                                onToggle = { 
                                    if (selectedCategories.contains(category.name)) {
                                        selectedCategories = selectedCategories - category.name
                                    } else {
                                        selectedCategories = selectedCategories + category.name
                                    }
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TaskGoTextBlack
                        ),
                        border = BorderStroke(1.dp, TaskGoTextGray)
                    ) {
                        Text("Cancelar")
                    }
                    
                    Button(
                        onClick = { 
                            val data = AccountTypeSelectionData(
                                accountType = selectedAccountType,
                                selectedCategories = selectedCategories,
                                cpf = if (documentType == "CPF") cpf.text.replace(Regex("[^0-9]"), "") else null,
                                cnpj = if (documentType == "CNPJ") cnpj.text.replace(Regex("[^0-9]"), "") else null,
                                rg = rg.takeIf { it.isNotBlank() },
                                documentType = documentType,
                                state = selectedState,
                                city = selectedCity
                            )
                            onAccountTypeSelected(data)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TaskGoGreen
                        ),
                        enabled = canContinue
                    ) {
                        Text("Continuar", color = androidx.compose.ui.graphics.Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountTypeOption(
    accountType: AccountType,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = TaskGoBackgroundWhite
        ),
        border = BorderStroke(1.dp, TaskGoBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = TaskGoGreen
                )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TaskGoTextBlack,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TaskGoTextGray
                )
            }
        }
    }
}

@Composable
private fun CategoryCheckbox(
    category: ServiceCategory,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = TaskGoGreen
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = category.name,
            style = MaterialTheme.typography.bodyMedium,
            color = TaskGoTextBlack
        )
    }
}