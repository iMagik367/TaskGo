package com.taskgoapp.taskgo.feature.auth.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import com.taskgoapp.taskgo.feature.auth.presentation.SignupViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import com.taskgoapp.taskgo.core.model.UserType
import com.taskgoapp.taskgo.core.model.AccountType
import com.taskgoapp.taskgo.core.design.TGIcons
import com.taskgoapp.taskgo.core.design.EnhancedOutlinedTextField
import com.taskgoapp.taskgo.core.design.OutlinedTextFieldWithValue
import com.taskgoapp.taskgo.core.theme.TaskGoGreen
import com.taskgoapp.taskgo.core.theme.TaskGoTextBlack
import com.taskgoapp.taskgo.core.theme.TaskGoTextGray
import com.taskgoapp.taskgo.core.theme.TaskGoBackgroundWhite
import com.taskgoapp.taskgo.core.theme.TaskGoSurface
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.res.stringResource
import com.taskgoapp.taskgo.R
import com.taskgoapp.taskgo.core.validation.CepService
import com.taskgoapp.taskgo.core.validation.DocumentValidator
import com.taskgoapp.taskgo.core.validation.ValidationResult
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@Composable
fun SignUpScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToDocumentVerification: () -> Unit = {},
    onBackClick: () -> Unit
) {
    val viewModel: SignupViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    
    // Injetar serviços via Hilt (criar instâncias locais por enquanto)
    val cepService = remember { CepService() }
    val documentValidator = remember { DocumentValidator() }
    val governmentValidator = remember { com.taskgoapp.taskgo.core.validation.GovernmentDocumentValidator() }
    
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var cpf by remember { mutableStateOf(TextFieldValue("")) }
    var cnpj by remember { mutableStateOf(TextFieldValue("")) }
    var rg by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf(TextFieldValue("")) }
    var biometricEnabled by remember { mutableStateOf(false) }
    var twoFactorEnabled by remember { mutableStateOf(false) }
    var selectedAccountType by remember { mutableStateOf(AccountType.CLIENTE) }
    // Seleção de tipo de documento para Parceiro (CPF ou CNPJ)
    var documentType by remember { mutableStateOf<String?>(null) } // "CPF" ou "CNPJ" ou null
    
    // Categorias de serviço selecionadas para Parceiro
    var selectedServiceCategories by remember { mutableStateOf<Set<String>>(emptySet()) }
    
    // Observar categorias de serviço do ViewModel
    val serviceCategories by viewModel.serviceCategories.collectAsState()
    
    // Campos de endereço
    var zipCode by remember { mutableStateOf(TextFieldValue("")) }
    var street by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var complement by remember { mutableStateOf("") }
    var neighborhood by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("Brasil") }
    
    // Estados de validação e loading
    var cpfError by remember { mutableStateOf<String?>(null) }
    var cnpjError by remember { mutableStateOf<String?>(null) }
    var rgError by remember { mutableStateOf<String?>(null) }
    var cepError by remember { mutableStateOf<String?>(null) }
    var isLoadingCep by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    
    // Validador de senha
    val passwordValidator = remember { com.taskgoapp.taskgo.core.validation.PasswordValidator() }

    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TaskGoBackgroundWhite)
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) {
                focusManager.clearFocus()
                keyboardController?.hide()
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            // Logo TaskGo Horizontal
            Image(
                painter = painterResource(id = TGIcons.TaskGoLogoHorizontal),
                contentDescription = "TaskGo Logo",
                modifier = Modifier.size(120.dp, 40.dp)
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Título
            Text(
                text = "Cadastro",
                style = MaterialTheme.typography.headlineMedium,
                color = TaskGoTextBlack,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Campo Nome
            com.taskgoapp.taskgo.core.design.EnhancedOutlinedTextField(
                value = name,
                onValueChange = { newValue -> name = newValue },
                label = { 
                    Text(
                        "Nome",
                        color = TaskGoTextGray,
                        fontSize = 14.sp
                    ) 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp),
                keyboardType = KeyboardType.Text
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Campo E-mail
            com.taskgoapp.taskgo.core.design.EnhancedOutlinedTextField(
                value = email,
                onValueChange = { newValue -> email = newValue },
                label = { 
                    Text(
                        "E-mail",
                        color = TaskGoTextGray,
                        fontSize = 14.sp
                    ) 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp),
                keyboardType = KeyboardType.Email
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Campo Telefone
            OutlinedTextFieldWithValue(
                value = phone,
                onValueChange = { newValue: TextFieldValue ->
                    val cleanValue = newValue.text.replace(Regex("[^0-9]"), "")
                    if (cleanValue.length <= 11) {
                        phone = com.taskgoapp.taskgo.core.utils.TextFormatters.formatPhoneWithCursor(newValue)
                    } else {
                        phone = newValue
                    }
                },
                label = { 
                    Text(
                        "Telefone",
                        color = TaskGoTextGray,
                        fontSize = 14.sp
                    ) 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Seleção de Tipo de Conta
            Text(
                text = "Tipo de Conta",
                style = MaterialTheme.typography.titleMedium,
                color = TaskGoTextBlack,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedAccountType = AccountType.PARCEIRO },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedAccountType == AccountType.PARCEIRO) TaskGoGreen.copy(alpha = 0.1f) else TaskGoSurface
                    ),
                    border = if (selectedAccountType == AccountType.PARCEIRO) BorderStroke(2.dp, TaskGoGreen) else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedAccountType == AccountType.PARCEIRO,
                            onClick = { selectedAccountType = AccountType.PARCEIRO },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = TaskGoGreen
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Parceiro",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TaskGoTextBlack,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Oferecer serviços e vender produtos",
                                style = MaterialTheme.typography.bodySmall,
                                color = TaskGoTextGray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedAccountType = AccountType.CLIENTE },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedAccountType == AccountType.CLIENTE) TaskGoGreen.copy(alpha = 0.1f) else TaskGoSurface
                    ),
                    border = if (selectedAccountType == AccountType.CLIENTE) BorderStroke(2.dp, TaskGoGreen) else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedAccountType == AccountType.CLIENTE,
                            onClick = { selectedAccountType = AccountType.CLIENTE },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = TaskGoGreen
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Cliente",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TaskGoTextBlack,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Contratar serviços e comprar produtos",
                                style = MaterialTheme.typography.bodySmall,
                                color = TaskGoTextGray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
            
            // Mostrar checkboxes de tipos de serviço quando Parceiro for selecionado
            if (selectedAccountType == AccountType.PARCEIRO && serviceCategories.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Selecione os tipos de serviço que você oferece",
                    style = MaterialTheme.typography.titleSmall,
                    color = TaskGoTextBlack,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    serviceCategories.forEach { category ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedServiceCategories.contains(category.name),
                                onCheckedChange = { checked ->
                                    selectedServiceCategories = if (checked) {
                                        selectedServiceCategories + category.name
                                    } else {
                                        selectedServiceCategories - category.name
                                    }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = TaskGoGreen
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TaskGoTextBlack,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Para Parceiro: checkboxes para selecionar CPF ou CNPJ
            if (selectedAccountType == AccountType.PARCEIRO) {
                Text(
                    text = "Tipo de Documento",
                    style = MaterialTheme.typography.titleSmall,
                    color = TaskGoTextBlack,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Checkbox CPF
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { 
                                documentType = "CPF"
                                cnpj = TextFieldValue("") // Limpar CNPJ quando selecionar CPF
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = documentType == "CPF",
                            onCheckedChange = { checked ->
                                if (checked) {
                                    documentType = "CPF"
                                    cnpj = TextFieldValue("") // Limpar CNPJ
                                } else {
                                    documentType = null
                                }
                            },
                            colors = CheckboxDefaults.colors(checkedColor = TaskGoGreen)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("CPF", style = MaterialTheme.typography.bodyMedium)
                    }
                    
                    // Checkbox CNPJ
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { 
                                documentType = "CNPJ"
                                cpf = TextFieldValue("") // Limpar CPF quando selecionar CNPJ
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = documentType == "CNPJ",
                            onCheckedChange = { checked ->
                                if (checked) {
                                    documentType = "CNPJ"
                                    cpf = TextFieldValue("") // Limpar CPF
                                } else {
                                    documentType = null
                                }
                            },
                            colors = CheckboxDefaults.colors(checkedColor = TaskGoGreen)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("CNPJ", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Campo CPF (apenas se Parceiro selecionou CPF ou se Cliente)
            if (selectedAccountType != AccountType.PARCEIRO || documentType == "CPF") {
                OutlinedTextFieldWithValue(
                    value = cpf,
                    onValueChange = { newValue ->
                        val cleanValue = newValue.text.replace(Regex("[^0-9]"), "")
                        if (cleanValue.length <= 11) {
                            cpfError = null // Limpar erro ao começar a editar
                            
                            // Formata progressivamente preservando posição do cursor
                            cpf = com.taskgoapp.taskgo.core.utils.TextFormatters.formatCpfWithCursor(newValue)
                            
                            // Valida usando validador avançado quando tiver 11 dígitos
                            if (cleanValue.length == 11) {
                                scope.launch {
                                    val cpfText = cpf.text.replace(Regex("[^0-9]"), "")
                                    val validation = governmentValidator.validateCpfAdvanced(cpfText)
                                    cpfError = when (validation) {
                                        is com.taskgoapp.taskgo.core.validation.DocumentValidationResult.Invalid -> validation.message
                                        is com.taskgoapp.taskgo.core.validation.DocumentValidationResult.Suspicious -> validation.message
                                        is com.taskgoapp.taskgo.core.validation.DocumentValidationResult.Error -> validation.message
                                        else -> null
                                    }
                                }
                            }
                        }
                    },
                    label = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "CPF",
                                color = TaskGoTextGray,
                                fontSize = 14.sp
                            )
                            Text(
                                " (Obrigatório)",
                                color = TaskGoTextGray,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    },
                    placeholder = { Text("000.000.000-00", color = TaskGoTextGray) },
                    isError = cpfError != null,
                    supportingText = if (cpfError != null) {
                        { Text(cpfError ?: "", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    enabled = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 72.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Campo CNPJ (apenas se Parceiro selecionou CNPJ)
            if (selectedAccountType == AccountType.PARCEIRO && documentType == "CNPJ") {
                OutlinedTextFieldWithValue(
                    value = cnpj,
                    onValueChange = { newValue ->
                        val cleanValue = newValue.text.replace(Regex("[^0-9]"), "")
                        if (cleanValue.length <= 14) {
                            cnpjError = null // Limpar erro ao começar a editar
                            
                            // Formata progressivamente preservando posição do cursor
                            cnpj = com.taskgoapp.taskgo.core.utils.TextFormatters.formatCnpjWithCursor(newValue)
                            
                            // Valida quando tiver 14 dígitos
                            if (cleanValue.length == 14) {
                                scope.launch {
                                    val cnpjText = cnpj.text.replace(Regex("[^0-9]"), "")
                                    val validation = documentValidator.validateCnpj(cnpjText)
                                    cnpjError = if (validation is ValidationResult.Invalid) validation.message else null
                                }
                            }
                        }
                    },
                    label = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "CNPJ",
                                color = TaskGoTextGray,
                                fontSize = 14.sp
                            )
                            Text(
                                " (Obrigatório)",
                                color = TaskGoTextGray,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    },
                    placeholder = { Text("00.000.000/0000-00", color = TaskGoTextGray) },
                    isError = cnpjError != null,
                    supportingText = if (cnpjError != null) {
                        { Text(cnpjError ?: "", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    enabled = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 72.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Campo RG com validação e formatação
            var rgTextFieldValue by remember { mutableStateOf(TextFieldValue(rg)) }
            
            // Sincronizar rgTextFieldValue com rg quando rg mudar externamente
            LaunchedEffect(rg) {
                if (rgTextFieldValue.text.replace(Regex("[^0-9A-Za-z]"), "") != rg.replace(Regex("[^0-9A-Za-z]"), "")) {
                    rgTextFieldValue = TextFieldValue(rg)
                }
            }
            
            OutlinedTextFieldWithValue(
                value = rgTextFieldValue,
                onValueChange = { newValue ->
                    val cleanValue = newValue.text.replace(Regex("[^0-9A-Za-z]"), "")
                    // Permitir até 12 caracteres alfanuméricos (formato brasileiro permite até 12 dígitos)
                    if (cleanValue.length <= 12) {
                        rgError = null // Limpar erro ao começar a editar
                        
                        // Formata progressivamente preservando posição do cursor
                        rgTextFieldValue = com.taskgoapp.taskgo.core.utils.TextFormatters.formatRgWithCursor(newValue)
                        rg = rgTextFieldValue.text.replace(Regex("[^0-9A-Za-z]"), "").uppercase()
                        
                        // Valida quando tiver tamanho mínimo (removendo formatação para contar dígitos)
                        val cleanRgDigits = rgTextFieldValue.text.replace(Regex("[^0-9A-Za-z]"), "")
                        if (cleanRgDigits.length >= 6) {
                            // Valida o RG formatado (com pontos e hífen) ou sem formatação
                            val rgToValidate = rgTextFieldValue.text.uppercase()
                            val validation = documentValidator.validateRg(rgToValidate, state.takeIf { it.isNotEmpty() })
                            rgError = if (validation is ValidationResult.Invalid) validation.message else null
                        } else {
                            rgError = null
                        }
                    }
                },
                label = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "RG",
                            color = TaskGoTextGray,
                            fontSize = 14.sp
                        )
                        Text(
                            " (Obrigatório)",
                            color = TaskGoTextGray,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                },
                placeholder = { Text("00.000.000-0", color = TaskGoTextGray) },
                isError = rgError != null,
                supportingText = if (rgError != null) {
                    { Text(rgError ?: "", color = MaterialTheme.colorScheme.error) }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Seção de Endereço
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Endereço",
                    style = MaterialTheme.typography.titleMedium,
                    color = TaskGoTextBlack,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = " (Obrigatório)",
                    style = MaterialTheme.typography.titleMedium,
                    color = TaskGoTextBlack,
                    fontWeight = FontWeight.Normal
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Campo CEP com busca automática de endereço
            OutlinedTextFieldWithValue(
                value = zipCode,
                onValueChange = { newValue: TextFieldValue ->
                    // Formata preservando cursor
                    zipCode = com.taskgoapp.taskgo.core.utils.TextFormatters.formatCepWithCursor(newValue)
                    val cleanValue = zipCode.text.replace(Regex("[^0-9]"), "")
                    
                    if (cleanValue.length <= 8) {
                        cepError = null
                        
                        // Busca endereço quando tiver 8 dígitos
                        if (cleanValue.length == 8) {
                            
                            // Busca endereço automaticamente
                            scope.launch {
                                isLoadingCep = true
                                cepError = null
                                
                                delay(500) // Delay para evitar múltiplas requisições
                                
                                cepService.searchCep(cleanValue).fold(
                                    onSuccess = { cepResult ->
                                        street = cepResult.logradouro
                                        neighborhood = cepResult.bairro
                                        city = cepResult.localidade
                                        state = cepResult.uf
                                        cepResult.complemento?.let { complement = it }
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
                label = { 
                    Text(
                        "CEP",
                        color = TaskGoTextGray,
                        fontSize = 14.sp
                    ) 
                },
                placeholder = { Text("00000-000", color = TaskGoTextGray) },
                singleLine = true,
                trailingIcon = {
                    if (isLoadingCep) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                },
                isError = cepError != null,
                supportingText = if (cepError != null) {
                    { Text(cepError ?: "", color = MaterialTheme.colorScheme.error) }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (cepError != null) MaterialTheme.colorScheme.error else TaskGoGreen,
                    unfocusedBorderColor = if (cepError != null) MaterialTheme.colorScheme.error else Color(0xFFD9D9D9),
                    focusedLabelColor = TaskGoTextGray,
                    unfocusedLabelColor = TaskGoTextGray,
                    cursorColor = TaskGoGreen
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = !isLoadingCep,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Campo Rua
            EnhancedOutlinedTextField(
                value = street,
                onValueChange = { newValue -> street = newValue },
                label = { 
                    Text(
                        "Rua",
                        color = TaskGoTextGray,
                        fontSize = 14.sp
                    ) 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp),
                keyboardType = KeyboardType.Text,
                minLines = 1,
                maxLines = 3
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Campo Número e Complemento
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EnhancedOutlinedTextField(
                    value = number,
                    onValueChange = { newValue -> number = newValue },
                    label = { 
                        Text(
                            "Número",
                            color = TaskGoTextGray,
                            fontSize = 14.sp
                        ) 
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 72.dp),
                    keyboardType = KeyboardType.Number
                )
                
                EnhancedOutlinedTextField(
                    value = complement,
                    onValueChange = { newValue -> complement = newValue },
                    label = { 
                        Text(
                            "Complemento",
                            color = TaskGoTextGray,
                            fontSize = 14.sp
                        ) 
                    },
                    placeholder = { Text("Apto, Bloco, etc.", color = TaskGoTextGray) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 72.dp),
                    keyboardType = KeyboardType.Text,
                    minLines = 1,
                    maxLines = 3
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Campo Bairro
            EnhancedOutlinedTextField(
                value = neighborhood,
                onValueChange = { newValue -> neighborhood = newValue },
                label = { 
                    Text(
                        "Bairro",
                        color = TaskGoTextGray,
                        fontSize = 14.sp
                    ) 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp),
                keyboardType = KeyboardType.Text,
                minLines = 1,
                maxLines = 3
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Campo Cidade e Estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EnhancedOutlinedTextField(
                    value = city,
                    onValueChange = { newValue -> city = newValue },
                    label = { 
                        Text(
                            "Cidade",
                            color = TaskGoTextGray,
                            fontSize = 14.sp
                        ) 
                    },
                    modifier = Modifier
                        .weight(2f)
                        .heightIn(min = 72.dp),
                    keyboardType = KeyboardType.Text,
                    minLines = 1,
                    maxLines = 3
                )
                
                EnhancedOutlinedTextField(
                    value = state,
                    onValueChange = { newValue -> state = newValue },
                    label = { 
                        Text(
                            "Estado",
                            color = TaskGoTextGray,
                            fontSize = 14.sp
                        ) 
                    },
                    placeholder = { Text("SP", color = TaskGoTextGray) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 72.dp),
                    keyboardType = KeyboardType.Text
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextFieldWithValue(
                value = birthDate,
                onValueChange = { newValue: TextFieldValue ->
                    try {
                        val cleanValue = newValue.text
                        val cleanOnlyNumbers = cleanValue.replace(Regex("[^0-9]"), "")
                        
                        // Validação segura para evitar crash
                        if (cleanOnlyNumbers.isEmpty()) {
                            birthDate = TextFieldValue("")
                        } else if (cleanOnlyNumbers.length <= 8) {
                            // Valida antes de formatar
                            if (com.taskgoapp.taskgo.core.utils.TextFormatters.isValidDateInput(cleanOnlyNumbers)) {
                                birthDate = com.taskgoapp.taskgo.core.utils.TextFormatters.formatDateWithCursor(newValue)
                            } else {
                                // Se inválido, mantém o valor anterior (não atualiza)
                                // Não faz nada, previne crash
                            }
                        }
                    } catch (e: Exception) {
                        // Em caso de qualquer exceção, mantém o valor anterior
                        android.util.Log.e("SignUpScreen", "Erro ao processar data: ${e.message}", e)
                        // Não atualiza birthDate, mantém valor anterior
                    }
                },
                label = { 
                    Text(
                        "Data de Nascimento (Opcional)",
                        color = TaskGoTextGray,
                        fontSize = 14.sp
                    ) 
                },
                placeholder = {
                    Text("DD/MM/AAAA", color = TaskGoTextGray)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TaskGoGreen,
                    unfocusedBorderColor = Color(0xFFD9D9D9),
                    focusedLabelColor = TaskGoTextGray,
                    unfocusedLabelColor = TaskGoTextGray,
                    cursorColor = TaskGoGreen
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Checkbox: Habilitar Biometria
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = biometricEnabled,
                    onCheckedChange = { biometricEnabled = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = TaskGoGreen
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Habilitar login com biometria",
                    color = TaskGoTextBlack,
                    fontSize = 14.sp
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Checkbox: Habilitar 2FA
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = twoFactorEnabled,
                        onCheckedChange = { twoFactorEnabled = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = TaskGoGreen
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Habilitar autenticação de duas etapas",
                        color = TaskGoTextBlack,
                        fontSize = 14.sp
                    )
                }
                
                // Instruções de senha abaixo do checkbox
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "A senha deve conter:",
                    color = TaskGoTextGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 48.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Column(
                    modifier = Modifier.padding(start = 48.dp)
                ) {
                    Text(
                        text = "• Mínimo de 8 caracteres",
                        color = TaskGoTextGray,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "• Pelo menos 1 número (0-9)",
                        color = TaskGoTextGray,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "• Pelo menos 1 letra maiúscula (A-Z)",
                        color = TaskGoTextGray,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "• Pelo menos 1 caractere especial (!@#$%&*()_+-=[]{}|;:,.<>?)",
                        color = TaskGoTextGray,
                        fontSize = 12.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Campo Senha
            EnhancedOutlinedTextField(
                value = password,
                onValueChange = { newPassword ->
                    password = newPassword
                    passwordError = null // Limpar erro ao começar a editar
                    
                    // Validar senha em tempo real
                    if (newPassword.isNotEmpty()) {
                        val validation = passwordValidator.validate(newPassword)
                        passwordError = when (validation) {
                            is ValidationResult.Invalid -> validation.message
                            else -> null
                        }
                    }
                    
                    // Se houver confirmação de senha, validar novamente
                    if (confirmPassword.isNotEmpty() && confirmPassword != newPassword) {
                        confirmPasswordError = "As senhas não coincidem"
                    } else if (confirmPassword.isNotEmpty() && confirmPassword == newPassword) {
                        confirmPasswordError = null
                    }
                },
                label = { 
                    Text(
                        "Senha",
                        color = TaskGoTextGray,
                        fontSize = 14.sp
                    ) 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Done
                ),
                isError = passwordError != null,
                supportingText = if (passwordError != null) {
                    { Text(passwordError ?: "", color = MaterialTheme.colorScheme.error) }
                } else null,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (passwordVisible) "Ocultar senha" else "Mostrar senha",
                            tint = TaskGoTextGray
                        )
                    }
                }
            )
            
            // Indicador de força da senha
            if (password.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                com.taskgoapp.taskgo.core.design.PasswordStrengthIndicator(
                    password = password,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Campo Confirmar Senha
            EnhancedOutlinedTextField(
                value = confirmPassword,
                onValueChange = { newConfirmPassword ->
                    confirmPassword = newConfirmPassword
                    
                    // Validar se as senhas coincidem
                    if (newConfirmPassword.isNotEmpty()) {
                        if (newConfirmPassword != password) {
                            confirmPasswordError = "As senhas não coincidem"
                        } else {
                            confirmPasswordError = null
                        }
                    } else {
                        confirmPasswordError = null
                    }
                },
                label = { 
                    Text(
                        "Confirmar Senha",
                        color = TaskGoTextGray,
                        fontSize = 14.sp
                    ) 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Done
                ),
                isError = confirmPasswordError != null,
                supportingText = if (confirmPasswordError != null) {
                    { Text(confirmPasswordError ?: "", color = MaterialTheme.colorScheme.error) }
                } else null,
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (confirmPasswordVisible) "Ocultar senha" else "Mostrar senha",
                            tint = TaskGoTextGray
                        )
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Botão Cadastrar (usar o viewModel já criado acima)
            
            // Variáveis para validação e submissão
            val phoneText = phone.text
            val zipCodeText = zipCode.text
            val birthDateText = birthDate.text
            val cpfText = cpf.text.replace(Regex("[^0-9]"), "") // Remove formatação do CPF
            val cnpjText = cnpj.text.replace(Regex("[^0-9]"), "") // Remove formatação do CNPJ
            val rgText = rgTextFieldValue.text.replace(Regex("[^0-9A-Za-z]"), "") // Remove formatação do RG
            val isAddressComplete = zipCodeText.isNotEmpty() && street.isNotEmpty() && 
                                   number.isNotEmpty() && neighborhood.isNotEmpty() && 
                                   city.isNotEmpty() && state.isNotEmpty()
            
            Button(
                onClick = { 
                    android.util.Log.d("SignUpScreen", "Botão Cadastrar clicado")
                    
                    // Criar objeto Address (obrigatório)
                    val address = com.taskgoapp.taskgo.core.model.Address(
                        street = street,
                        number = number,
                        complement = complement.takeIf { it.isNotEmpty() },
                        neighborhood = neighborhood,
                        city = city,
                        state = state,
                        country = country,
                        zipCode = zipCodeText,
                        cep = zipCodeText
                    )
                    
                    // Parse birthDate se fornecido
                    val parsedBirthDate = try {
                        if (birthDateText.isNotEmpty()) {
                            val parts = birthDateText.split("/")
                            if (parts.size == 3) {
                                val day = parts[0].toInt()
                                val month = parts[1].toInt() - 1 // Calendar months are 0-based
                                val year = parts[2].toInt()
                                java.util.Calendar.getInstance().apply {
                                    set(year, month, day)
                                }.time
                            } else null
                        } else null
                    } catch (e: Exception) {
                        android.util.Log.e("SignUpScreen", "Erro ao parsear data de nascimento: ${e.message}", e)
                        null
                    }
                    
                    // Mapear AccountType para UserType (temporário, para compatibilidade)
                    val userType = when (selectedAccountType) {
                        AccountType.PARCEIRO -> com.taskgoapp.taskgo.core.model.UserType.PROVIDER // Parceiro mapeia para PROVIDER
                        AccountType.PRESTADOR -> com.taskgoapp.taskgo.core.model.UserType.PROVIDER // Legacy
                        AccountType.VENDEDOR -> com.taskgoapp.taskgo.core.model.UserType.PROVIDER // Legacy - agora é provider também
                        AccountType.CLIENTE -> com.taskgoapp.taskgo.core.model.UserType.CLIENT
                    }
                    
                    // Converter Set<String> para List<String> para preferredCategories
                    val preferredCategoriesList = if (selectedAccountType == AccountType.PARCEIRO && selectedServiceCategories.isNotEmpty()) {
                        selectedServiceCategories.toList()
                    } else null
                    
                    viewModel.signup(
                        name = name,
                        email = email,
                        phone = phoneText,
                        password = password,
                        userType = userType,
                        accountType = selectedAccountType,
                        cpf = if (selectedAccountType == AccountType.PARCEIRO && documentType == "CNPJ") null else cpfText.takeIf { it.isNotEmpty() },
                        cnpj = if (selectedAccountType == AccountType.PARCEIRO && documentType == "CNPJ") cnpjText.takeIf { it.isNotEmpty() } else null,
                        rg = rgTextFieldValue.text.uppercase(),
                        birthDate = parsedBirthDate,
                        address = address,
                        biometricEnabled = biometricEnabled,
                        twoFactorEnabled = twoFactorEnabled,
                        twoFactorMethod = if (twoFactorEnabled) "email" else null,
                        preferredCategories = preferredCategoriesList
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TaskGoGreen
                ),
                enabled = !uiState.isLoading && 
                         name.isNotEmpty() && 
                         email.isNotEmpty() && 
                         phoneText.isNotEmpty() && 
                         cpfText.length == 11 &&
                         cpfError == null &&
                         rgText.length >= 6 &&
                         rgError == null &&
                         isAddressComplete &&
                         password.isNotEmpty() && 
                         confirmPassword.isNotEmpty() && 
                         password == confirmPassword &&
                         passwordError == null &&
                         confirmPasswordError == null &&
                         // Validação adicional: RG formatado deve ser válido (XX.XXX.XXX-X = 9 dígitos ou mais)
                         (rgText.isEmpty() || (rgText.length >= 6 && rgText.length <= 12))
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = "Cadastrar",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Mensagem de erro
            uiState.errorMessage?.let { errorMsg ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMsg,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Navegação após sucesso - ir para tela de verificação de documentos
            LaunchedEffect(uiState.isSuccess) {
                if (uiState.isSuccess) {
                    android.util.Log.d("SignUpScreen", "Cadastro bem-sucedido, navegando para verificação de documentos...")
                    kotlinx.coroutines.delay(500) // Pequeno delay para garantir que tudo foi salvo
                    onNavigateToDocumentVerification() // Navega para identity_verification
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Divisor
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFD9D9D9),
                    thickness = 1.dp
                )
                Text(
                    text = "ou",
                    color = TaskGoTextGray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFD9D9D9),
                    thickness = 1.dp
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Link para Login
            Text(
                text = "Já tenho uma conta",
                color = TaskGoGreen,
                fontSize = 14.sp,
                modifier = Modifier.clickable { onNavigateToLogin() }
            )
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

