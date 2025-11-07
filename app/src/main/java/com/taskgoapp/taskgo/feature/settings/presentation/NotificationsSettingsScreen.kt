package com.taskgoapp.taskgo.feature.settings.presentation

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
import com.taskgoapp.taskgo.core.design.*
import com.taskgoapp.taskgo.R
import com.taskgoapp.taskgo.core.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.state.collectAsState()
    var pushNotifications by remember { mutableStateOf(settings.pushEnabled) }
    var soundEnabled by remember { mutableStateOf(settings.soundEnabled) }
    var showOnLockScreen by remember { mutableStateOf(settings.lockscreenEnabled) }
    var promotionalNotifications by remember { mutableStateOf(settings.promosEnabled) }
    var emailNotifications by remember { mutableStateOf(false) }
    var smsNotifications by remember { mutableStateOf(false) }
    var showSaveSuccess by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.settings_notifications),
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

            // Save Button
            PrimaryButton(
                text = stringResource(R.string.notifications_save_changes),
                onClick = {
                    viewModel.saveNotifications(
                        promos = promotionalNotifications,
                        sound = soundEnabled,
                        push = pushNotifications,
                        lockscreen = showOnLockScreen
                    )
                    showSaveSuccess = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Success Snackbar
    if (showSaveSuccess) {
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { showSaveSuccess = false }) {
                    Text("OK")
                }
            }
        ) {
            Text(stringResource(R.string.notifications_settings_saved))
        }
    }
}


