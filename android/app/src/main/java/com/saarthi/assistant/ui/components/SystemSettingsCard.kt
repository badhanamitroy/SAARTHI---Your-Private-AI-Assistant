package com.saarthi.assistant.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.saarthi.assistant.R
import com.saarthi.assistant.core.SystemSettingType
import com.saarthi.assistant.ui.theme.DarkBlueGrey
import com.saarthi.assistant.ui.theme.ElectricCyan
import com.saarthi.assistant.ui.theme.ElevatedSurface
import com.saarthi.assistant.ui.theme.SoftViolet
import com.saarthi.assistant.ui.theme.SurfaceBorderDark

@Composable
fun SystemSettingsCard(
    onOpenSetting: (SystemSettingType) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ElevatedSurface)
            .border(1.dp, SurfaceBorderDark, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = SaarthiIcons.Settings,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = stringResource(id = R.string.section_system_controls),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(id = R.string.system_controls_desc),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SystemSettingRow(
                    title = stringResource(id = R.string.setting_wifi),
                    description = "Wi-Fi connections & Internet panel",
                    icon = SaarthiIcons.Wifi,
                    tint = ElectricCyan,
                    onClick = { onOpenSetting(SystemSettingType.WIFI) }
                )

                SystemSettingRow(
                    title = stringResource(id = R.string.setting_bluetooth),
                    description = "Pair & manage Bluetooth devices",
                    icon = SaarthiIcons.Bluetooth,
                    tint = SoftViolet,
                    onClick = { onOpenSetting(SystemSettingType.BLUETOOTH) }
                )

                SystemSettingRow(
                    title = stringResource(id = R.string.setting_mobile_data),
                    description = "SIM & mobile data connectivity",
                    icon = SaarthiIcons.MobileData,
                    tint = ElectricCyan,
                    onClick = { onOpenSetting(SystemSettingType.MOBILE_DATA) }
                )

                SystemSettingRow(
                    title = stringResource(id = R.string.setting_sound),
                    description = "Volume, ringtones, and media panel",
                    icon = SaarthiIcons.VolumeUp,
                    tint = SoftViolet,
                    onClick = { onOpenSetting(SystemSettingType.SOUND) }
                )

                SystemSettingRow(
                    title = stringResource(id = R.string.setting_brightness),
                    description = "Display brightness & screen timeout",
                    icon = SaarthiIcons.Brightness,
                    tint = ElectricCyan,
                    onClick = { onOpenSetting(SystemSettingType.BRIGHTNESS) }
                )
            }
        }
    }
}

@Composable
private fun SystemSettingRow(
    title: String,
    description: String,
    icon: ImageVector,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkBlueGrey)
            .border(1.dp, SurfaceBorderDark, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ElevatedSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = tint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = stringResource(id = R.string.setting_panel_hint),
                style = MaterialTheme.typography.labelSmall,
                color = ElectricCyan
            )
        }
    }
}
