package br.com.taskgo.taskgo.feature.settings.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.taskgoapp.R
import com.example.taskgoapp.core.design.*
import com.example.taskgoapp.feature.profile.presentation.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    onBackClick: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var editedName by remember(state.name) { mutableStateOf(state.name) }
    var editedEmail by remember(state.email) { mutableStateOf(state.email) }
    var editedPhone by remember(state.phone) { mutableStateOf(state.phone) }
    var editedCity by remember(state.city) { mutableStateOf(state.city) }
    var editedProfession by remember(state.profession) { mutableStateOf(state.profession) }
    var editedType by remember(state.accountType) { mutableStateOf(state.accountType) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.onAvatarSelected(uri.toString())
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
                        .background(MaterialTheme.colorScheme.surfaceVariant),
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
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = editedName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(text = editedProfession, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Formulário
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = stringResource(R.string.account_full_name), style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(painter = painterResource(TGIcons.Profile), contentDescription = null) }
                )

                Text(text = stringResource(R.string.account_email), style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = editedEmail,
                    onValueChange = { editedEmail = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(painter = painterResource(TGIcons.Info), contentDescription = null) }
                )

                Text(text = stringResource(R.string.profile_phone_label), style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = editedPhone,
                    onValueChange = { editedPhone = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(painter = painterResource(TGIcons.Phone), contentDescription = null) }
                )

                Text(text = stringResource(R.string.profile_city_label), style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = editedCity,
                    onValueChange = { editedCity = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(painter = painterResource(TGIcons.Info), contentDescription = null) }
                )

                Text(text = stringResource(R.string.profile_account_type_label), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = editedType == AccountType.PRESTADOR,
                        onClick = { editedType = AccountType.PRESTADOR },
                        label = { Text(stringResource(R.string.profile_account_type_provider)) }
                    )
                    FilterChip(
                        selected = editedType == AccountType.VENDEDOR,
                        onClick = { editedType = AccountType.VENDEDOR },
                        label = { Text(stringResource(R.string.profile_account_type_seller)) }
                    )
                    FilterChip(
                        selected = editedType == AccountType.CLIENTE,
                        onClick = { editedType = AccountType.CLIENTE },
                        label = { Text(stringResource(R.string.profile_account_type_client)) }
                    )
                }

                // Alterar foto dentro do formulário
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = stringResource(R.string.account_change_photo), style = MaterialTheme.typography.bodyLarge)
                        Icon(painter = painterResource(TGIcons.Arrow), contentDescription = null)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Botão Salvar Alterações
            Button(
                onClick = {
                    viewModel.onNameChange(editedName)
                    viewModel.onEmailChange(editedEmail)
                    viewModel.onPhoneChange(editedPhone)
                    viewModel.onCityChange(editedCity)
                    viewModel.onProfessionChange(editedProfession)
                    viewModel.onAccountTypeChange(editedType)
                    viewModel.save()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(stringResource(R.string.settings_save_changes))
            }
        }
    }
}


