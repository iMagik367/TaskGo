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
import com.taskgoapp.taskgo.core.model.UserType
import com.taskgoapp.taskgo.core.model.AccountType
import com.taskgoapp.taskgo.core.design.TGIcons
import com.taskgoapp.taskgo.core.design.EnhancedOutlinedTextField
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
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var cpf by remember { mutableStateOf("") }
    var rg by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var biometricEnabled by remember { mutableStateOf(false) }
    var twoFactorEnabled by remember { mutableStateOf(false) }
    var selectedAccountType by remember { mutableStateOf(AccountType.CLIENTE) }
    
    // Campos de endereço
    var zipCode by remember { mutableStateOf("") }
    var street by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var complement by remember { mutableStateOf("") }
    var neighborhood by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("Brasil") }
    
    // Estados de validação e loading
    var cpfError by remember { mutableStateOf<String?>(null) }
    var rgError by remember { mutableStateOf<String?>(null) }
    var cepError by remember { mutableStateOf<String?>(null) }
    var isLoadingCep by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TaskGoBackgroundWhite)
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
                onValueChange = { name = it },
                label = { 
                    Text(
                        "Nome",
                        color = TaskGoTextGray,
                        fontSize = 14.sp
                    ) 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                keyboardType = KeyboardType.Text
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Campo E-mail
            com.taskgoapp.taskgo.core.design.EnhancedOutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { 
                    Text(
                        "E-mail",
                        color = TaskGoTextGray,
                        fontSize = 14.sp
                    ) 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                keyboardType = KeyboardType.Email
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Campo Telefone
            EnhancedOutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { 
                    Text(
                        "Telefone",
                        color = TaskGoTextGray,
                        fontSize = 14.sp
                    ) 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
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
                        .clickable { selectedAccountType = AccountType.PRESTADOR },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedAccountType == AccountType.PRESTADOR) TaskGoGreen.copy(alpha = 0.1f) else TaskGoSurface
                    ),
                    border = if (selectedAccountType == AccountType.PRESTADOR) BorderStroke(2.dp, TaskGoGreen) else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedAccountType == AccountType.PRESTADOR,
                            onClick = { selectedAccountType = AccountType.PRESTADOR },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = TaskGoGreen
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Prestador de Serviços",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TaskGoTextBlack
                        )
                    }
                }
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedAccountType = AccountType.VENDEDOR },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedAccountType == AccountType.VENDEDOR) TaskGoGreen.copy(alpha = 0.1f) else TaskGoSurface
                    ),
                    border = if (selectedAccountType == AccountType.VENDEDOR) BorderStroke(2.dp, TaskGoGreen) else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedAccountType == AccountType.VENDEDOR,
                            onClick = { selectedAccountType = AccountType.VENDEDOR },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = TaskGoGreen
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Vendedor",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TaskGoTextBlack
                        )
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
                        Text(
                            text = "Cliente",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TaskGoTextBlack
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Campo CPF com validação e formatação
            EnhancedOutlinedTextField(
                value = cpf,
                onValueChange = { newValue ->
                    // Remove caracteres não numéricos
                    val cleanValue = newValue.replace(Regex("[^0-9]"), "")
                    if (cleanValue.length <= 11) {
                        // Sempre permitir edição, mesmo com erro
                        cpf = cleanValue
                        cpfError = null // Limpar erro ao começar a editar
                        
                        // Formata automaticamente quando tiver 11 dígitos
                        if (cleanValue.length == 11) {
                            val formatted = documentValidator.formatCpf(cleanValue)
                            cpf = formatted
                            // Valida usando validador avançado
                            scope.launch {
                                val validation = governmentValidator.validateCpfAdvanced(formatted)
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
                    Text(
                        "CPF (Opcional)",
                        color = TaskGoTextGray,
                        fontSize = 14.sp
                    ) 
                },
                placeholder = { Text("000.000.000-00", color = TaskGoTextGray) },
                isError = cpfError != null,
                supportingText = cpfError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                enabled = true, // Sempre habilitado para permitir correção
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Campo RG com validação e formatação
            OutlinedTextField(
                value = rg,
                onValueChange = { newValue ->
                    rg = newValue.uppercase()
                    // Valida quando o campo perder o foco ou tiver tamanho mínimo
                    if (rg.length >= 6) {
                        val validation = documentValidator.validateRg(rg, state.takeIf { it.isNotEmpty() })
                        rgError = if (validation is ValidationResult.Invalid) validation.message else null
                    } else {
                        rgError = null
                    }
                },
                label = { 
                    Text(
                        "RG (Opcional)",
                        color = TaskGoTextGray,
                        fontSize = 14.sp
                    ) 
                },
                placeholder = { Text("00.000.000-0", color = TaskGoTextGray) },
                isError = rgError != null,
                supportingText = rgError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (rgError != null) MaterialTheme.colorScheme.error else TaskGoGreen,
                    unfocusedBorderColor = if (rgError != null) MaterialTheme.colorScheme.error else Color(0xFFD9D9D9),
                    focusedLabelColor = TaskGoTextGray,
                    unfocusedLabelColor = TaskGoTextGray,
                    cursorColor = TaskGoGreen
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Seção de Endereço
            Text(
                text = "Endereço (Opcional)",
                style = MaterialTheme.typography.titleMedium,
                color = TaskGoTextBlack,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Campo CEP com busca automática de endereço
            OutlinedTextField(
                value = zipCode,
                onValueChange = { newValue ->
                    // Remove caracteres não numéricos
                    val cleanValue = newValue.replace(Regex("[^0-9]"), "")
                    if (cleanValue.length <= 8) {
                        zipCode = cleanValue
                        cepError = null
                        
                        // Formata automaticamente quando tiver 8 dígitos
                        if (cleanValue.length == 8) {
                            zipCode = documentValidator.formatCep(cleanValue)
                            
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (cepError != null) MaterialTheme.colorScheme.error else TaskGoGreen,
                    unfocusedBorderColor = if (cepError != null) MaterialTheme.colorScheme.error else Color(0xFFD9D9D9),
                    focusedLabelColor = TaskGoTextGray,
                    unfocusedLabelColor = TaskGoTextGray,
                    cursorColor = TaskGoGreen
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = !isLoadingCep
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Campo Rua
            OutlinedTextField(
                value = street,
                onValueChange = { street = it },
                label = { 
                    Text(
                        "Rua",
                        color = TaskGoTextGray,
                        fontSize = 14.sp
                    ) 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TaskGoGreen,
                    unfocusedBorderColor = Color(0xFFD9D9D9),
                    focusedLabelColor = TaskGoTextGray,
                    unfocusedLabelColor = TaskGoTextGray,
                    cursorColor = TaskGoGreen
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Campo Número e Complemento
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { 
                        Text(
                            "Número",
                            color = TaskGoTextGray,
                            fontSize = 14.sp
                        ) 
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TaskGoGreen,
                        unfocusedBorderColor = Color(0xFFD9D9D9),
                        focusedLabelColor = TaskGoTextGray,
                        unfocusedLabelColor = TaskGoTextGray,
                        cursorColor = TaskGoGreen
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                
                OutlinedTextField(
                    value = complement,
                    onValueChange = { complement = it },
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
                        .height(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TaskGoGreen,
                        unfocusedBorderColor = Color(0xFFD9D9D9),
                        focusedLabelColor = TaskGoTextGray,
                        unfocusedLabelColor = TaskGoTextGray,
                        cursorColor = TaskGoGreen
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Campo Bairro
            OutlinedTextField(
                value = neighborhood,
                onValueChange = { neighborhood = it },
                label = { 
                    Text(
                        "Bairro",
                        color = TaskGoTextGray,
                        fontSize = 14.sp
                    ) 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TaskGoGreen,
                    unfocusedBorderColor = Color(0xFFD9D9D9),
                    focusedLabelColor = TaskGoTextGray,
                    unfocusedLabelColor = TaskGoTextGray,
                    cursorColor = TaskGoGreen
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Campo Cidade e Estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { 
                        Text(
                            "Cidade",
                            color = TaskGoTextGray,
                            fontSize = 14.sp
                        ) 
                    },
                    modifier = Modifier
                        .weight(2f)
                        .height(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TaskGoGreen,
                        unfocusedBorderColor = Color(0xFFD9D9D9),
                        focusedLabelColor = TaskGoTextGray,
                        unfocusedLabelColor = TaskGoTextGray,
                        cursorColor = TaskGoGreen
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )
                
                OutlinedTextField(
                    value = state,
                    onValueChange = { state = it },
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
                        .height(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TaskGoGreen,
                        unfocusedBorderColor = Color(0xFFD9D9D9),
                        focusedLabelColor = TaskGoTextGray,
                        unfocusedLabelColor = TaskGoTextGray,
                        cursorColor = TaskGoGreen
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = birthDate,
                onValueChange = { birthDate = it },
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
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TaskGoGreen,
                    unfocusedBorderColor = Color(0xFFD9D9D9),
                    focusedLabelColor = TaskGoTextGray,
                    unfocusedLabelColor = TaskGoTextGray,
                    cursorColor = TaskGoGreen
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Campo Senha
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { 
                    Text(
                        "Senha",
                        color = TaskGoTextGray,
                        fontSize = 14.sp
                    ) 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TaskGoGreen,
                    unfocusedBorderColor = Color(0xFFD9D9D9),
                    focusedLabelColor = TaskGoTextGray,
                    unfocusedLabelColor = TaskGoTextGray,
                    cursorColor = TaskGoGreen
                ),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Campo Confirmar Senha
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { 
                    Text(
                        "Confirmar Senha",
                        color = TaskGoTextGray,
                        fontSize = 14.sp
                    ) 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TaskGoGreen,
                    unfocusedBorderColor = Color(0xFFD9D9D9),
                    focusedLabelColor = TaskGoTextGray,
                    unfocusedLabelColor = TaskGoTextGray,
                    cursorColor = TaskGoGreen
                ),
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
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
            
            // Botão Cadastrar
            val signupViewModel: SignupViewModel = hiltViewModel()
            val signupUiState = signupViewModel.uiState.collectAsState()
            
            Button(
                onClick = { 
                    android.util.Log.d("SignUpScreen", "Botão Cadastrar clicado")
                    
                    // Criar objeto Address se houver dados de endereço
                    val address = if (street.isNotEmpty() || city.isNotEmpty() || zipCode.isNotEmpty()) {
                        com.taskgoapp.taskgo.core.model.Address(
                            street = street,
                            number = number,
                            complement = complement.takeIf { it.isNotEmpty() },
                            neighborhood = neighborhood,
                            city = city,
                            state = state,
                            country = country,
                            zipCode = zipCode,
                            cep = zipCode
                        )
                    } else null
                    
                    // Parse birthDate se fornecido
                    val parsedBirthDate = try {
                        if (birthDate.isNotEmpty()) {
                            val parts = birthDate.split("/")
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
                        AccountType.PRESTADOR -> com.taskgoapp.taskgo.core.model.UserType.PROVIDER
                        AccountType.VENDEDOR -> com.taskgoapp.taskgo.core.model.UserType.CLIENT // Vendedor usa CLIENT temporariamente
                        AccountType.CLIENTE -> com.taskgoapp.taskgo.core.model.UserType.CLIENT
                    }
                    
                    signupViewModel.signup(
                        name = name,
                        email = email,
                        phone = phone,
                        password = password,
                        userType = userType,
                        accountType = selectedAccountType,
                        cpf = cpf.takeIf { it.isNotEmpty() },
                        rg = rg.takeIf { it.isNotEmpty() },
                        birthDate = parsedBirthDate,
                        address = address,
                        biometricEnabled = biometricEnabled,
                        twoFactorEnabled = twoFactorEnabled,
                        twoFactorMethod = if (twoFactorEnabled) "email" else null
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TaskGoGreen
                ),
                enabled = !signupUiState.value.isLoading && 
                         name.isNotEmpty() && 
                         email.isNotEmpty() && 
                         phone.isNotEmpty() && 
                         password.isNotEmpty() && 
                         confirmPassword.isNotEmpty() && 
                         password == confirmPassword
            ) {
                if (signupUiState.value.isLoading) {
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
            signupUiState.value.errorMessage?.let { errorMsg ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMsg,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Navegação após sucesso - ir para verificação de documentos
            LaunchedEffect(signupUiState.value.isSuccess) {
                if (signupUiState.value.isSuccess) {
                    android.util.Log.d("SignUpScreen", "Cadastro bem-sucedido, navegando para verificação de documentos...")
                    onNavigateToDocumentVerification()
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

