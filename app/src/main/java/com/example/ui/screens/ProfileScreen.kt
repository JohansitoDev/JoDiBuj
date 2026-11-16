package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.AvatarCatalog
import com.example.data.LevelSystem
import com.example.data.UserProfile
import com.example.notifications.DailyReminderManager
import com.example.notifications.ReminderSettings
import com.example.ui.components.AvatarSelectionDialog
import com.example.ui.components.DuoButton
import com.example.ui.components.DuoCleanInput
import com.example.ui.components.DuoOutlinedButton
import com.example.ui.components.DuoProgressBar
import com.example.ui.components.PlayerAvatar
import com.example.ui.components.iconForAvatarKey
import com.example.ui.components.parseColorSafe
import com.example.ui.theme.BorderGray
import com.example.ui.theme.CardinalRed
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.FeatherGreen
import com.example.ui.theme.FeatherGreenDark
import com.example.ui.theme.MacawBlue
import com.example.ui.theme.MacawBlueDark
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    profileState: UiState<UserProfile?>,
    isBusy: Boolean,
    isGuestMode: Boolean = false,
    onConnectGoogle: () -> Unit = {},
    onSaveProfile: (username: String, displayName: String, bio: String, icon: String, color: String) -> Unit,
    onSignOut: () -> Unit,
    onFeedbackMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val reminderSettings by DailyReminderManager.observeSettings(context)
        .collectAsStateWithLifecycle(initialValue = ReminderSettings())

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            scope.launch {
                DailyReminderManager.saveAndSchedule(
                    context,
                    enabled = true,
                    hour = reminderSettings.hour,
                    minute = reminderSettings.minute,
                )
            }
            DailyReminderManager.showDailyNotificationNow(context)
            onFeedbackMessage("¡Notificaciones diarias activadas!")
        } else {
            onFeedbackMessage("Activa el permiso de notificaciones en Ajustes para recibir recordatorios.")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureWhite)
            .testTag("profile_screen"),
        contentAlignment = Alignment.TopCenter,
    ) {
        val profile = (profileState as? UiState.Success)?.data
        if (profileState is UiState.Loading || profile == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MacawBlue)
            }
        } else {
            var username by remember(profile.userId) { mutableStateOf(profile.username) }
            var displayName by remember(profile.userId) { mutableStateOf(profile.displayName) }
            var bio by remember(profile.userId) { mutableStateOf(profile.bio) }
            var avatarIcon by remember(profile.userId) { mutableStateOf(profile.avatarIcon) }
            var avatarColor by remember(profile.userId) { mutableStateOf(profile.avatarColor) }
            var showAvatarDialog by remember { mutableStateOf(false) }

            LaunchedEffect(profile.username, profile.displayName, profile.bio) {
                if (username.isBlank()) username = profile.username
                if (displayName.isBlank()) displayName = profile.displayName
            }

            val level = profile.level.toInt().coerceAtLeast(1)

            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier.clickable { showAvatarDialog = true },
                    contentAlignment = Alignment.BottomEnd,
                ) {
                    PlayerAvatar(
                        iconKey = avatarIcon,
                        colorHex = avatarColor,
                        size = 96.dp,
                    )
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(MacawBlue)
                            .border(1.5.dp, PureWhite, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Edit,
                            contentDescription = "Cambiar Avatar",
                            tint = PureWhite,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = profile.username,
                    style = MaterialTheme.typography.headlineMedium,
                    color = DeepNavy,
                )
                Text(
                    text = "Nivel $level · ${LevelSystem.titleForLevel(level)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )

                Spacer(Modifier.height(14.dp))
                DuoProgressBar(
                    progress = LevelSystem.xpProgressInLevel(profile.totalPoints),
                    fillColor = FeatherGreen,
                )
                Spacer(Modifier.height(16.dp))

                // Detailed Stats Grid (Ganados, Perdidos, Jugados, WinRate)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        ProfileStatBox(label = "Nivel Actual", value = "$level", modifier = Modifier.weight(1f))
                        ProfileStatBox(label = "Puntos XP", value = "${profile.totalPoints}", modifier = Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        ProfileStatBox(label = "Ganados 🏆", value = "${profile.wins}", modifier = Modifier.weight(1f))
                        ProfileStatBox(label = "Perdidos ❌", value = "${profile.calculatedLosses}", modifier = Modifier.weight(1f))
                        ProfileStatBox(label = "Jugados 🎮", value = "${profile.gamesPlayed}", modifier = Modifier.weight(1f))
                    }
                }

                Spacer(Modifier.height(22.dp))

                if (showAvatarDialog) {
                    AvatarSelectionDialog(
                        initialIconKey = avatarIcon,
                        initialColorHex = avatarColor,
                        onDismiss = { showAvatarDialog = false },
                        onAvatarSelected = { newIcon, newColor ->
                            avatarIcon = newIcon
                            avatarColor = newColor
                        },
                    )
                }

                Spacer(Modifier.height(22.dp))

                Text(
                    text = stringResource(R.string.profile_edit_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = DeepNavy,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))

                // Clean inputs directly on white background (no card behind)
                DuoCleanInput(
                    value = username,
                    onValueChange = { username = it.take(24) },
                    label = stringResource(R.string.input_username_label),
                    leadingIcon = Icons.Rounded.AlternateEmail,
                    testTag = "profile_username_input",
                )
                Spacer(Modifier.height(12.dp))
                DuoCleanInput(
                    value = displayName,
                    onValueChange = { displayName = it.take(40) },
                    label = stringResource(R.string.input_display_name_label),
                    leadingIcon = Icons.Rounded.Person,
                    testTag = "profile_displayname_input",
                )
                Spacer(Modifier.height(12.dp))
                DuoCleanInput(
                    value = bio,
                    onValueChange = { bio = it.take(120) },
                    label = stringResource(R.string.input_bio_label),
                    leadingIcon = Icons.Rounded.Edit,
                    testTag = "profile_bio_input",
                )

                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    AvatarCatalog.icons.forEach { (key, label) ->
                        val isSel = avatarIcon == key
                        Box(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(if (isSel) parseColorSafe(avatarColor) else SoftSurface)
                                .border(1.5.dp, if (isSel) DeepNavy else BorderGray, CircleShape)
                                .clickable { avatarIcon = key },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = iconForAvatarKey(key),
                                contentDescription = label,
                                tint = if (isSel) PureWhite else TextSecondary,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    AvatarCatalog.colors.forEach { hex ->
                        val c = parseColorSafe(hex)
                        val isSel = avatarColor == hex
                        Box(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(if (isSel) 3.dp else 0.dp, if (isSel) DeepNavy else c, CircleShape)
                                .clickable { avatarColor = hex },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSel) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = PureWhite, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                DuoButton(
                    onClick = { onSaveProfile(username, displayName, bio, avatarIcon, avatarColor) },
                    containerColor = FeatherGreen,
                    lipColor = FeatherGreenDark,
                    isLoading = isBusy,
                    testTag = "save_profile_button",
                ) {
                    Text(stringResource(R.string.btn_save_profile), style = MaterialTheme.typography.labelLarge)
                }

                Spacer(Modifier.height(26.dp))

                // Daily Push Notifications Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(SoftSurface)
                        .border(1.5.dp, BorderGray, RoundedCornerShape(20.dp))
                        .padding(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                Icons.Rounded.NotificationsActive,
                                contentDescription = null,
                                tint = MacawBlue,
                                modifier = Modifier.size(28.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.notifications_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = DeepNavy,
                                )
                                Text(
                                    text = stringResource(R.string.notifications_subtitle),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                )
                            }
                        }
                        Switch(
                            checked = reminderSettings.enabled,
                            onCheckedChange = { wantEnabled ->
                                if (wantEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val granted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS,
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (!granted) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        return@Switch
                                    }
                                }
                                scope.launch {
                                    DailyReminderManager.saveAndSchedule(
                                        context,
                                        enabled = wantEnabled,
                                        hour = reminderSettings.hour,
                                        minute = reminderSettings.minute,
                                    )
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = PureWhite, checkedTrackColor = FeatherGreen),
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Hora diaria del recordatorio:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(9 to "09:00", 14 to "14:00", 19 to "19:00", 21 to "21:00").forEach { (hour, label) ->
                            val isSel = reminderSettings.hour == hour
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) MacawBlue else PureWhite)
                                    .border(1.5.dp, if (isSel) MacawBlueDark else BorderGray, RoundedCornerShape(12.dp))
                                    .clickable {
                                        scope.launch {
                                            DailyReminderManager.saveAndSchedule(
                                                context,
                                                enabled = true,
                                                hour = hour,
                                                minute = 0,
                                            )
                                        }
                                        onFeedbackMessage("Recordatorio diario programado a las $label")
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isSel) PureWhite else TextPrimary,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    DuoOutlinedButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS,
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                val shown = DailyReminderManager.showDailyNotificationNow(context)
                                onFeedbackMessage(
                                    if (shown) "¡Notificación diaria enviada!" else "Permiso de notificaciones requerido."
                                )
                            }
                        },
                        accentColor = MacawBlue,
                        height = 46.dp,
                        testTag = "test_notification_button",
                    ) {
                        Text(stringResource(R.string.btn_test_notification), style = MaterialTheme.typography.labelLarge)
                    }
                }

                Spacer(Modifier.height(22.dp))

                if (isGuestMode) {
                    DuoButton(
                        onClick = onConnectGoogle,
                        containerColor = MacawBlue,
                        lipColor = MacawBlueDark,
                        testTag = "profile_connect_google_button",
                    ) {
                        Text(stringResource(R.string.btn_connect_google), style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(Modifier.height(12.dp))
                }

                DuoOutlinedButton(
                    onClick = onSignOut,
                    accentColor = CardinalRed,
                    testTag = "sign_out_button",
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isGuestMode) "Salir de modo invitado" else stringResource(R.string.btn_sign_out),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ProfileStatBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(PureWhite)
            .border(1.5.dp, BorderGray, RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = DeepNavy,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
        )
    }
}
