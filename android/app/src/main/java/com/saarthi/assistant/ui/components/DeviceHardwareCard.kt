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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.saarthi.assistant.R
import com.saarthi.assistant.core.BatteryChargingState
import com.saarthi.assistant.core.BatteryStatusResult
import com.saarthi.assistant.ui.theme.DarkBlueGrey
import com.saarthi.assistant.ui.theme.ElectricCyan
import com.saarthi.assistant.ui.theme.ElevatedSurface
import com.saarthi.assistant.ui.theme.ErrorRed
import com.saarthi.assistant.ui.theme.SoftViolet
import com.saarthi.assistant.ui.theme.SuccessGreen
import com.saarthi.assistant.ui.theme.SurfaceBorderDark

/**
 * Compact side-by-side hardware tiles for HomeScreen.
 * Displays real-time battery status and quick flashlight toggle.
 */
@Composable
fun DeviceHardwareSummaryRow(
    batteryStatus: BatteryStatusResult?,
    isFlashlightOn: Boolean,
    isFlashAvailable: Boolean,
    onCheckBatteryClick: () -> Unit,
    onToggleFlashlightClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pct = batteryStatus?.percentage
    val isCharging = batteryStatus?.chargingState == BatteryChargingState.CHARGING ||
            batteryStatus?.chargingState == BatteryChargingState.FULL

    val batteryColor = when {
        isCharging -> SuccessGreen
        pct == null -> SoftViolet
        pct > 50 -> SuccessGreen
        pct > 20 -> ElectricCyan
        else -> ErrorRed
    }

    val batteryStateText = when (batteryStatus?.chargingState) {
        BatteryChargingState.CHARGING -> stringResource(R.string.battery_charging)
        BatteryChargingState.FULL -> stringResource(R.string.battery_full)
        BatteryChargingState.DISCHARGING -> stringResource(R.string.battery_discharging)
        BatteryChargingState.NOT_CHARGING -> stringResource(R.string.battery_not_charging)
        else -> if (pct != null) "$pct%" else stringResource(R.string.battery_unknown)
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Battery Tile
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(ElevatedSurface)
                .border(1.dp, SurfaceBorderDark, RoundedCornerShape(14.dp))
                .clickable(onClick = onCheckBatteryClick)
                .padding(12.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isCharging) SaarthiIcons.BatteryCharging else SaarthiIcons.Battery,
                        contentDescription = stringResource(R.string.hardware_battery_title),
                        tint = batteryColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.action_battery),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (pct != null) "$pct%" else "--",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = batteryStateText,
                    style = MaterialTheme.typography.labelSmall,
                    color = batteryColor
                )
            }
        }

        // Flashlight Tile
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(if (isFlashlightOn) DarkBlueGrey else ElevatedSurface)
                .border(
                    1.dp,
                    if (isFlashlightOn) ElectricCyan else SurfaceBorderDark,
                    RoundedCornerShape(14.dp)
                )
                .clickable(enabled = isFlashAvailable, onClick = onToggleFlashlightClick)
                .padding(12.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isFlashlightOn) SaarthiIcons.FlashlightOn else SaarthiIcons.FlashlightOff,
                        contentDescription = stringResource(R.string.hardware_flashlight_title),
                        tint = if (!isFlashAvailable) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        else if (isFlashlightOn) ElectricCyan else SoftViolet,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.action_flashlight),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (!isFlashAvailable) stringResource(R.string.hardware_no_flash)
                    else if (isFlashlightOn) stringResource(R.string.hardware_flashlight_on)
                    else stringResource(R.string.hardware_flashlight_off),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isFlashlightOn) ElectricCyan else MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = if (!isFlashAvailable) "Unavailable"
                    else if (isFlashlightOn) "Tap to Turn Off"
                    else "Tap to Turn On",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isFlashlightOn) ElectricCyan.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Detailed Hardware Controls Card for ToolsScreen.
 * Provides explicit ON and OFF buttons for flashlight and battery refresh action.
 */
@Composable
fun HardwareControlsCard(
    batteryStatus: BatteryStatusResult?,
    isFlashlightOn: Boolean,
    isFlashAvailable: Boolean,
    onCheckBatteryClick: () -> Unit,
    onSetFlashlightClick: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val pct = batteryStatus?.percentage
    val isCharging = batteryStatus?.chargingState == BatteryChargingState.CHARGING ||
            batteryStatus?.chargingState == BatteryChargingState.FULL

    val batteryColor = when {
        isCharging -> SuccessGreen
        pct == null -> SoftViolet
        pct > 50 -> SuccessGreen
        pct > 20 -> ElectricCyan
        else -> ErrorRed
    }

    val batteryDetail = when (batteryStatus?.chargingState) {
        BatteryChargingState.CHARGING -> {
            val src = when (batteryStatus.pluggedSource) {
                com.saarthi.assistant.core.PluggedSource.AC -> " (AC)"
                com.saarthi.assistant.core.PluggedSource.USB -> " (USB)"
                com.saarthi.assistant.core.PluggedSource.WIRELESS -> " (Wireless)"
                else -> ""
            }
            "Charging$src"
        }
        BatteryChargingState.FULL -> "Fully Charged (100%)"
        BatteryChargingState.DISCHARGING -> "Discharging"
        BatteryChargingState.NOT_CHARGING -> "Plugged in, not charging"
        else -> if (pct != null) "Battery: $pct%" else "Status unavailable"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ElevatedSurface)
            .border(1.dp, SurfaceBorderDark, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = "Device Hardware Essentials",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Battery section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkBlueGrey.copy(alpha = 0.4f))
                    .clickable(onClick = onCheckBatteryClick)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(batteryColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCharging) SaarthiIcons.BatteryCharging else SaarthiIcons.Battery,
                        contentDescription = null,
                        tint = batteryColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.hardware_battery_title),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        if (pct != null) {
                            Text(
                                text = "$pct%",
                                style = MaterialTheme.typography.titleSmall,
                                color = batteryColor
                            )
                        }
                    }
                    Text(
                        text = batteryDetail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "Check",
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricCyan
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Flashlight section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkBlueGrey.copy(alpha = 0.4f))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isFlashlightOn) ElectricCyan.copy(alpha = 0.2f)
                                else SoftViolet.copy(alpha = 0.12f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFlashlightOn) SaarthiIcons.FlashlightOn else SaarthiIcons.FlashlightOff,
                            contentDescription = null,
                            tint = if (!isFlashAvailable) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            else if (isFlashlightOn) ElectricCyan else SoftViolet,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.hardware_flashlight_title),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (!isFlashAvailable) stringResource(R.string.hardware_no_flash)
                            else if (isFlashlightOn) "State: ON"
                            else "State: OFF",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (!isFlashAvailable) MaterialTheme.colorScheme.onSurfaceVariant
                            else if (isFlashlightOn) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isFlashAvailable) {
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Explicit ON button
                        Button(
                            onClick = { onSetFlashlightClick(true) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFlashlightOn) ElectricCyan else DarkBlueGrey,
                                contentColor = if (isFlashlightOn) Color.Black else Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.hardware_turn_on),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        // Explicit OFF button
                        Button(
                            onClick = { onSetFlashlightClick(false) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isFlashlightOn) SoftViolet.copy(alpha = 0.25f) else DarkBlueGrey,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.hardware_turn_off),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
