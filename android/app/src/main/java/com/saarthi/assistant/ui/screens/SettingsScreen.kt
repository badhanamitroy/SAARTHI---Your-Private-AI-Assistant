package com.saarthi.assistant.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.saarthi.assistant.R
import com.saarthi.assistant.ui.components.SaarthiEmblem
import com.saarthi.assistant.ui.components.SaarthiIcons
import com.saarthi.assistant.ui.theme.DarkBlueGrey
import com.saarthi.assistant.ui.theme.ElectricCyan
import com.saarthi.assistant.ui.theme.ElevatedSurface
import com.saarthi.assistant.ui.theme.ErrorRed
import com.saarthi.assistant.ui.theme.MidnightNavy
import com.saarthi.assistant.ui.theme.SoftViolet
import com.saarthi.assistant.ui.theme.SuccessGreen
import com.saarthi.assistant.ui.theme.SurfaceBorderDark

@Composable
fun SettingsScreen(
    isVoiceFeedbackEnabled: Boolean,
    onToggleVoiceFeedback: () -> Unit,
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    hasAudioPermission: Boolean,
    hasNotificationPermission: Boolean,
    onRequestPermissions: () -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showClearConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Settings Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SaarthiEmblem(size = 32.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(id = R.string.settings_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Scrollable Settings Sections
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Section: Assistant Preferences
            SettingsSectionHeader(title = stringResource(id = R.string.pref_group_assistant))

            SettingsCard {
                SettingsSwitchRow(
                    title = stringResource(id = R.string.pref_voice_feedback),
                    description = stringResource(id = R.string.pref_voice_feedback_desc),
                    checked = isVoiceFeedbackEnabled,
                    onCheckedChange = { onToggleVoiceFeedback() }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section: Appearance
            SettingsSectionHeader(title = stringResource(id = R.string.pref_group_appearance))

            SettingsCard {
                SettingsSwitchRow(
                    title = stringResource(id = R.string.pref_theme),
                    description = if (isDarkTheme) {
                        stringResource(id = R.string.pref_theme_dark)
                    } else {
                        stringResource(id = R.string.pref_theme_light)
                    },
                    checked = isDarkTheme,
                    onCheckedChange = { onToggleDarkTheme(it) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section: Privacy & Data
            SettingsSectionHeader(title = stringResource(id = R.string.pref_group_privacy))

            SettingsCard {
                Column {
                    // Privacy Disclosure
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = SaarthiIcons.Shield,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(id = R.string.pref_privacy_policy),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(id = R.string.pref_privacy_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Permissions Info Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = stringResource(id = R.string.pref_permissions),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Mic: ${if (hasAudioPermission) "Granted" else "Needed"} | Notifications: ${if (hasNotificationPermission) "Granted" else "Optional"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (hasAudioPermission) SuccessGreen else ErrorRed
                            )
                        }

                        if (!hasAudioPermission) {
                            Text(
                                text = "Grant",
                                style = MaterialTheme.typography.labelMedium,
                                color = ElectricCyan,
                                modifier = Modifier
                                    .clickable { onRequestPermissions() }
                                    .padding(6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Clear History Action Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showClearConfirm = true }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = SaarthiIcons.Trash,
                                contentDescription = null,
                                tint = ErrorRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(id = R.string.btn_clear_history),
                                style = MaterialTheme.typography.bodyMedium,
                                color = ErrorRed
                            )
                        }

                        Text(
                            text = "Clear →",
                            style = MaterialTheme.typography.labelSmall,
                            color = ErrorRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section: About
            SettingsSectionHeader(title = stringResource(id = R.string.pref_group_about))

            SettingsCard {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SaarthiEmblem(size = 32.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(id = R.string.app_name),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = stringResource(id = R.string.pref_app_version),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(id = R.string.tagline),
                        style = MaterialTheme.typography.bodySmall,
                        color = ElectricCyan
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // System Settings direct link
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "System App Info & Permissions",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Open →",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(text = stringResource(id = R.string.dialog_clear_title)) },
            text = { Text(text = stringResource(id = R.string.dialog_clear_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirm = false
                        onClearHistory()
                    }
                ) {
                    Text(text = stringResource(id = R.string.btn_confirm), color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(text = stringResource(id = R.string.btn_cancel))
                }
            },
            containerColor = ElevatedSurface,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = ElectricCyan,
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ElevatedSurface)
            .border(1.dp, SurfaceBorderDark, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MidnightNavy,
                checkedTrackColor = ElectricCyan,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = DarkBlueGrey
            )
        )
    }
}
