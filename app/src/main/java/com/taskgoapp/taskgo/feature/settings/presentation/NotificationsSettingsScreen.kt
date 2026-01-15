package com.taskgoapp.taskgo.feature.settings.presentation

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.taskgoapp.taskgo.core.design.*
import com.taskgoapp.taskgo.R
import com.taskgoapp.taskgo.core.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun NotificationsSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.state.collectAsStateWithLifecycle()
    // Estados locais - inicializar apenas uma vez
    var pushNotifications by remember { mutableStateOf(false) }
    var soundEnabled by remember { mutableStateOf(true) }
    var showOnLockScreen by remember { mutableStateOf(true) }
    var promotionalNotifications by remember { mutableStateOf(true) }
    var emailNotifications by remember { mutableStateOf(false) }
    var smsNotifications by remember { mutableStateOf(false) }
    
    // Inicializar apenas uma vez quando dados estiverem disponíveis
    var hasInitialized by remember { mutableStateOf(false) }
    LaunchedEffect(settings.pushEnabled, settings.soundEnabled) {
        if (!hasInitialized) {
            pushNotifications = settings.pushEnabled
            soundEnabled = settings.soundEnabled
            showOnLockScreen = settings.lockscreenEnabled
            promotionalNotifications = settings.promosEnabled
            emailNotifications = settings.emailNotificationsEnabled
            smsNotifications = settings.smsNotificationsEnabled
            hasInitialized = true
        }
    }
    
    val coroutineScope = rememberCoroutineScope()
    var saveJob by remember { mutableStateOf<Job?>(null) }

    fun queueSave() {
        if (!hasInitialized) return
        saveJob?.cancel()
        saveJob = coroutineScope.launch {
            delay(800)
            viewModel.saveNotifications(
                promos = promotionalNotifications,
                sound = soundEnabled,
                push = pushNotifications,
                lockscreen = showOnLockScreen,
                email = emailNotifications,
                sms = smsNotifications
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            saveJob?.cancel()
            viewModel.saveNotifications(
                promos = promotionalNotifications,
                sound = soundEnabled,
                push = pushNotifications,
                lockscreen = showOnLockScreen,
                email = emailNotifications,
                sms = smsNotifications
            )
        }
    }
    
    // Permissão de notificações
    val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }
    
    var hasRequestedNotificationPermission by remember { mutableStateOf(false) }
    
    // Solicitar permissão quando a tela abrir se ainda não tiver permissão
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && 
            notificationPermission != null && 
            notificationPermission.status !is PermissionStatus.Granted && 
            !hasRequestedNotificationPermission) {
            hasRequestedNotificationPermission = true
            kotlinx.coroutines.delay(500) // Pequeno delay para melhor UX
            notificationPermission.launchPermissionRequest()
        }
    }
    
    // Solicitar permissão quando pushNotifications for habilitado
    LaunchedEffect(pushNotifications) {
        if (pushNotifications && 
            notificationPermission != null && 
            notificationPermission.status !is PermissionStatus.Granted && 
            !hasRequestedNotificationPermission) {
            hasRequestedNotificationPermission = true
            notificationPermission.launchPermissionRequest()
        } else if (!pushNotifications) {
            hasRequestedNotificationPermission = false
        }
    }
    
    LaunchedEffect(pushNotifications, soundEnabled, showOnLockScreen, promotionalNotifications, emailNotifications, smsNotifications) {
        if (hasInitialized) {
            queueSave()
        }
    }
    
    // Atualizar estado após salvar (sincronizar com backend)
    LaunchedEffect(settings.pushEnabled, settings.soundEnabled, settings.lockscreenEnabled, settings.promosEnabled, settings.emailNotificationsEnabled, settings.smsNotificationsEnabled) {
        if (hasInitialized) {
            pushNotifications = settings.pushEnabled
            soundEnabled = settings.soundEnabled
            showOnLockScreen = settings.lockscreenEnabled
            promotionalNotifications = settings.promosEnabled
            emailNotifications = settings.emailNotificationsEnabled
            smsNotifications = settings.smsNotificationsEnabled
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.settings_notifications),
                subtitle = "Escolha como deseja ser notificado",
                onBackClick = onNavigateBack,
                backgroundColor = TaskGoGreen,
                titleColor = TaskGoBackgroundWhite,
                backIconColor = TaskGoBackgroundWhite
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // General Notifications
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.notifications_general),
                        style = FigmaSectionTitle,
                        color = TaskGoTextBlack,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.notifications_push),
                                style = FigmaProductDescription,
                                color = TaskGoTextBlack
                            )
                            Text(
                                text = stringResource(R.string.notifications_push_desc),
                                style = FigmaStatusText,
                                color = TaskGoTextGray
                            )
                        }
                        Switch(
                            checked = pushNotifications,
                            onCheckedChange = { pushNotifications = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.notifications_email),
                                style = FigmaProductDescription,
                                color = TaskGoTextBlack
                            )
                            Text(
                                text = stringResource(R.string.notifications_email_desc),
                                style = FigmaStatusText,
                                color = TaskGoTextGray
                            )
                        }
                        Switch(
                            checked = emailNotifications,
                            onCheckedChange = { emailNotifications = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.notifications_sms),
                                style = FigmaProductDescription,
                                color = TaskGoTextBlack
                            )
                            Text(
                                text = stringResource(R.string.notifications_sms_desc),
                                style = FigmaStatusText,
                                color = TaskGoTextGray
                            )
                        }
                        Switch(
                            checked = smsNotifications,
                            onCheckedChange = { smsNotifications = it }
                        )
                    }
                }
            }

            // Notification Behavior
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.notifications_behavior),
                        style = FigmaSectionTitle,
                        color = TaskGoTextBlack,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.notifications_sound),
                                style = FigmaProductDescription,
                                color = TaskGoTextBlack
                            )
                            Text(
                                text = stringResource(R.string.notifications_sound_desc),
                                style = FigmaStatusText,
                                color = TaskGoTextGray
                            )
                        }
                        Switch(
                            checked = soundEnabled,
                            onCheckedChange = { soundEnabled = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.notifications_locked_screen),
                                style = FigmaProductDescription,
                                color = TaskGoTextBlack
                            )
                            Text(
                                text = stringResource(R.string.notifications_locked_screen_desc),
                                style = FigmaStatusText,
                                color = TaskGoTextGray
                            )
                        }
                        Switch(
                            checked = showOnLockScreen,
                            onCheckedChange = { showOnLockScreen = it }
                        )
                    }
                }
            }

            // Specific Notifications
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.notifications_types),
                        style = FigmaSectionTitle,
                        color = TaskGoTextBlack,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.notifications_promotions),
                                style = FigmaProductDescription,
                                color = TaskGoTextBlack
                            )
                            Text(
                                text = stringResource(R.string.notifications_promotions_desc),
                                style = FigmaStatusText,
                                color = TaskGoTextGray
                            )
                        }
                        Switch(
                            checked = promotionalNotifications,
                            onCheckedChange = { promotionalNotifications = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Notification Schedule
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.notifications_schedule),
                            style = FigmaProductDescription,
                            color = TaskGoTextBlack,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringResource(R.string.notifications_schedule_desc),
                            style = FigmaStatusText,
                            color = TaskGoTextGray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}


