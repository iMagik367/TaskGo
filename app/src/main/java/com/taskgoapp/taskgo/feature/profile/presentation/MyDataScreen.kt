package com.taskgoapp.taskgo.feature.profile.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taskgoapp.taskgo.R
import com.taskgoapp.taskgo.core.design.AppTopBar
import com.taskgoapp.taskgo.core.design.TGIcons
import com.taskgoapp.taskgo.core.data.models.User
import com.taskgoapp.taskgo.core.data.models.AccountType
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyDataScreen(
    onNavigateBack: () -> Unit
) {
    val user = remember { 
        User(
            id = 1L,
            name = "João Silva",
            email = "joao@email.com",
            phone = "(11) 99999-9999",
            accountType = AccountType.CLIENT,
            timeOnTaskGo = "2 anos",
            rating = 4.8,
            reviewCount = 156,
            city = "São Paulo"
        )
    }
    
    var isEditing by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf("") }
    var editedEmail by remember { mutableStateOf("") }
    var editedPhone by remember { mutableStateOf("") }
    var showChangePhotoDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showSaveSuccess by remember { mutableStateOf(false) }
    
    LaunchedEffect(user) {
        editedName = user.name
        editedEmail = user.email
        editedPhone = user.phone
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
                onChangePhoto = { showChangePhotoDialog = true }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Personal Information
            PersonalInfoSection(
                user = user,
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
            AccountInfoSection(user = user)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Action Buttons
            ActionButtonsSection(
                isEditing = isEditing,
                onEditClick = { isEditing = true },
                onSaveClick = {
                    // TODO: Implement save changes
                    isEditing = false
                    showSaveSuccess = true
                },
                onCancelClick = {
                    isEditing = false
                    editedName = user.name
                    editedEmail = user.email
                    editedPhone = user.phone
                },
                onChangePassword = { showChangePasswordDialog = true }
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
                    Icon(
                        painter = painterResource(TGIcons.Profile),
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.primaryContainer
                    )
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
    user: User,
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
                    value = user.name
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                InfoRow(
                    label = stringResource(R.string.profile_email_label),
                    value = user.email
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                InfoRow(
                    label = stringResource(R.string.profile_phone_label),
                    value = user.phone
                )
            }
        }
    }
}

@Composable
private fun AccountInfoSection(user: User) {
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
                value = when (user.accountType) {
                    AccountType.PROVIDER -> stringResource(R.string.profile_account_type_provider)
                    AccountType.SELLER -> stringResource(R.string.profile_account_type_seller)
                    AccountType.CLIENT -> stringResource(R.string.profile_account_type_client)
                }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            InfoRow(
                label = stringResource(R.string.profile_time_on_taskgo_label),
                value = user.timeOnTaskGo
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            InfoRow(
                label = stringResource(R.string.profile_rating_label),
                value = "${user.rating}/5.0 (${user.reviewCount} avaliações)"
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            InfoRow(
                label = stringResource(R.string.profile_city_label),
                value = user.city
            )
        }
    }
}

@Composable
private fun ActionButtonsSection(
    isEditing: Boolean,
    onEditClick: () -> Unit,
    onSaveClick: () -> Unit,
    onCancelClick: () -> Unit,
    onChangePassword: () -> Unit
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
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.profile_save))
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
