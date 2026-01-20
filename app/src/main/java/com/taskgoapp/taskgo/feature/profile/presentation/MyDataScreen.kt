package com.taskgoapp.taskgo.feature.profile.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll

import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.taskgoapp.taskgo.core.theme.TaskGoError
import com.taskgoapp.taskgo.R
import com.taskgoapp.taskgo.core.design.AppTopBar
import com.taskgoapp.taskgo.core.design.TGIcons
import com.taskgoapp.taskgo.core.model.AccountType
import com.taskgoapp.taskgo.core.theme.TaskGoGreen
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyDataScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Evitar renderização prematura que cause crash por estado nulo
    if (uiState.isLoading && uiState.name.isBlank() && uiState.email.isBlank()) {
        Scaffold(
            topBar = {
                AppTopBar(
                    title = stringResource(R.string.profile_my_data),
                    onBackClick = onNavigateBack
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = TaskGoGreen)
            }
        }
        return
    }
    // Se ainda não carregou o perfil, mostrar carregando (evita crash por estado vazio)
    if (uiState.isLoading && uiState.name.isBlank() && uiState.email.isBlank()) {
        Scaffold(
            topBar = {
                AppTopBar(
                    title = stringResource(R.string.profile_my_data),
                    onBackClick = onNavigateBack
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = TaskGoGreen)
            }
        }
        return
    }
    
    var isEditing by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf("") }
    var editedEmail by remember { mutableStateOf("") }
    var editedPhone by remember { mutableStateOf("") }
    var showChangePhotoDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showSaveSuccess by remember { mutableStateOf(false) }
    var showDeleteAccountConfirmation by remember { mutableStateOf(false) }
    
    LaunchedEffect(uiState) {
        if (!isEditing) {
            editedName = uiState.name
            editedEmail = uiState.email
            editedPhone = uiState.phone
        }
    }
    
    // Calcular tempo no TaskGo baseado em createdAt
    val timeOnTaskGo = remember(uiState.createdAt) {
        uiState.createdAt?.let { createdAt ->
            val now = Date()
            val diff = now.time - createdAt.time
            val days = diff / (1000 * 60 * 60 * 24)
            when {
                days < 30 -> "${days} dias"
                days < 365 -> "${days / 30} meses"
                else -> "${days / 365} anos"
            }
        } ?: "Recém cadastrado"
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.profile_my_data),
                onBackClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Profile Photo Section
            ProfilePhotoSection(
                avatarUri = uiState.avatarUri,
                onChangePhoto = { showChangePhotoDialog = true }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Personal Information
            PersonalInfoSection(
                uiState = uiState,
                isEditing = isEditing,
                editedName = editedName,
                editedEmail = editedEmail,
                editedPhone = editedPhone,
                onNameChange = { editedName = it },
                onEmailChange = { editedEmail = it },
                onPhoneChange = { editedPhone = it }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Account Information
            AccountInfoSection(
                uiState = uiState,
                timeOnTaskGo = timeOnTaskGo
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Action Buttons
            ActionButtonsSection(
                isEditing = isEditing,
                isSaving = uiState.isSaving,
                onEditClick = { isEditing = true },
                onSaveClick = {
                    viewModel.onNameChange(editedName)
                    viewModel.onEmailChange(editedEmail)
                    viewModel.onPhoneChange(editedPhone)
                    viewModel.save()
                    isEditing = false
                    showSaveSuccess = true
                },
                onCancelClick = {
                    isEditing = false
                    editedName = uiState.name
                    editedEmail = uiState.email
                    editedPhone = uiState.phone
                },
                onChangePassword = { showChangePasswordDialog = true },
                onDeleteAccount = { showDeleteAccountConfirmation = true }
            )
        }
    }
    
    // Change Photo Dialog
    if (showChangePhotoDialog) {
        AlertDialog(
            onDismissRequest = { showChangePhotoDialog = false },
            title = { Text(stringResource(R.string.profile_change_photo_title)) },
            text = { Text(stringResource(R.string.profile_change_photo_message)) },
            confirmButton = {
                TextButton(
                    onClick = { showChangePhotoDialog = false }
                ) {
                    Text(stringResource(R.string.profile_change_photo_confirm))
                }
            }
        )
    }
    
    // Change Password Dialog
    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showChangePasswordDialog = false }
        )
    }
    
    // Delete Account Confirmation Dialog
    if (showDeleteAccountConfirmation) {
        DeleteAccountDialog(
            onDismiss = { showDeleteAccountConfirmation = false },
            onConfirm = {
                showDeleteAccountConfirmation = false
                // Navegar para tela de configurações de segurança onde está o botão de excluir
                // Ou implementar a exclusão diretamente aqui
            }
        )
    }
    
    // Save Success Snackbar
    if (showSaveSuccess) {
        LaunchedEffect(Unit) {
            delay(2000)
            showSaveSuccess = false
        }
        
        Snackbar(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(stringResource(R.string.profile_save_success))
        }
    }
}

@Composable
private fun ProfilePhotoSection(
    avatarUri: String?,
    onChangePhoto: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Photo
            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUri != null && avatarUri.isNotBlank()) {
                        coil.compose.AsyncImage(
                            model = avatarUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Icon(
                            painter = painterResource(TGIcons.Profile),
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                            tint = MaterialTheme.colorScheme.primaryContainer
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stringResource(R.string.profile_photo_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedButton(
                onClick = onChangePhoto,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(
                    painter = painterResource(TGIcons.Edit),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.profile_change_photo))
            }
        }
    }
}

@Composable
private fun PersonalInfoSection(
    uiState: ProfileState,
    isEditing: Boolean,
    editedName: String,
    editedEmail: String,
    editedPhone: String,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.profile_personal_info_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isEditing) {
                // Editable fields
                OutlinedTextField(
                    value = editedName,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.profile_name_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = editedEmail,
                    onValueChange = onEmailChange,
                    label = { Text(stringResource(R.string.profile_email_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = editedPhone,
                    onValueChange = onPhoneChange,
                    label = { Text(stringResource(R.string.profile_phone_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // Read-only fields
                InfoRow(
                    label = stringResource(R.string.profile_name_label),
                    value = uiState.name.ifBlank { "Não informado" }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                InfoRow(
                    label = stringResource(R.string.profile_email_label),
                    value = uiState.email.ifBlank { "Não informado" }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                InfoRow(
                    label = stringResource(R.string.profile_phone_label),
                    value = uiState.phone.ifBlank { "Não informado" }
                )
            }
        }
    }
}

@Composable
private fun AccountInfoSection(
    uiState: ProfileState,
    timeOnTaskGo: String
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.profile_account_info_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            InfoRow(
                label = stringResource(R.string.profile_account_type_label),
                value = when (uiState.accountType) {
                    AccountType.PARCEIRO -> "Parceiro"
                    AccountType.PRESTADOR -> "Prestador"
                    AccountType.VENDEDOR -> "Vendedor"
                    AccountType.CLIENTE -> "Cliente"
                }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            InfoRow(
                label = stringResource(R.string.profile_time_on_taskgo_label),
                value = timeOnTaskGo
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            InfoRow(
                label = stringResource(R.string.profile_rating_label),
                value = if (uiState.rating != null) {
                    "${String.format("%.1f", uiState.rating)}/5.0"
                } else {
                    "Sem avaliações"
                }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            InfoRow(
                label = stringResource(R.string.profile_city_label),
                value = uiState.city.ifBlank { "Não informado" }
            )
        }
    }
}

@Composable
private fun ActionButtonsSection(
    isEditing: Boolean,
    isSaving: Boolean,
    onEditClick: () -> Unit,
    onSaveClick: () -> Unit,
    onCancelClick: () -> Unit,
    onChangePassword: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isEditing) {
            // Save and Cancel buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancelClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.profile_cancel))
                }
                
                Button(
                    onClick = onSaveClick,
                    modifier = Modifier.weight(1f),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(stringResource(R.string.profile_save))
                    }
                }
            }
        } else {
            // Edit button
            Button(
                onClick = onEditClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(TGIcons.Edit),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.profile_edit_info))
            }
        }
        
        // Change Password button
        OutlinedButton(
            onClick = onChangePassword,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                painter = painterResource(TGIcons.Edit),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.profile_change_password))
        }
        
        // Delete Account button
        OutlinedButton(
            onClick = onDeleteAccount,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.DeleteForever,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Excluir Minha Conta",
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DeleteAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val functionsService = remember {
        com.taskgoapp.taskgo.data.firebase.FirebaseFunctionsService(
            com.google.firebase.functions.FirebaseFunctions.getInstance()
        )
    }
    var isDeleting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        title = { 
            Text(
                "Confirmar Exclusão",
                color = TaskGoError
            ) 
        },
        text = {
            Column {
                Text(
                    "Tem certeza que deseja excluir permanentemente sua conta? Esta ação não pode ser desfeita e todos os seus dados serão removidos.",
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        errorMessage!!,
                        color = TaskGoError,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (currentUser != null && !isDeleting) {
                        isDeleting = true
                        errorMessage = null
                        scope.launch {
                            try {
                                val deleteResult = functionsService.deleteUserAccount()
                                deleteResult.fold(
                                    onSuccess = {
                                        // Fazer logout após exclusão bem-sucedida
                                        auth.signOut()
                                        onConfirm()
                                    },
                                    onFailure = { error ->
                                        errorMessage = "Erro ao excluir conta: ${error.message}"
                                        isDeleting = false
                                    }
                                )
                            } catch (e: Exception) {
                                errorMessage = "Erro: ${e.message}"
                                isDeleting = false
                            }
                        }
                    }
                },
                enabled = !isDeleting && currentUser != null,
                colors = ButtonDefaults.textButtonColors(contentColor = TaskGoError)
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = TaskGoError
                    )
                } else {
                    Text("Excluir", color = TaskGoError)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDeleting
            ) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    viewModel: com.taskgoapp.taskgo.feature.auth.presentation.AuthViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val changeState = viewModel.changePasswordState.collectAsState().value

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_change_password_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text(stringResource(R.string.profile_current_password)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text(stringResource(R.string.profile_new_password)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text(stringResource(R.string.profile_confirm_password)) },
                    modifier = Modifier.fillMaxWidth()
                )

                if (changeState.error != null) {
                    Text(changeState.error, color = MaterialTheme.colorScheme.error)
                }
                if (changeState.success) {
                    Text("Senha alterada com sucesso!", color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (newPassword == confirmPassword && newPassword.isNotBlank() && currentPassword.isNotBlank()) {
                        viewModel.changePassword(currentPassword, newPassword)
                    }
                },
                enabled = !changeState.isLoading && newPassword == confirmPassword && newPassword.isNotBlank() && currentPassword.isNotBlank()
            ) {
                if (changeState.isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                else Text(stringResource(R.string.profile_change_password_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.profile_change_password_cancel))
            }
        }
    )
}
