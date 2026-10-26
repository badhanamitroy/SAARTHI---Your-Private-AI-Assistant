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
import com.saarthi.assistant.ui.theme.DarkBlueGrey
import com.saarthi.assistant.ui.theme.ElectricCyan
import com.saarthi.assistant.ui.theme.ElevatedSurface
import com.saarthi.assistant.ui.theme.SoftViolet
import com.saarthi.assistant.ui.theme.SurfaceBorderDark

data class QuickActionItem(
    val id: String,
    val titleRes: Int,
    val icon: ImageVector,
    val accentColor: androidx.compose.ui.graphics.Color = ElectricCyan,
    val onClick: () -> Unit
)

@Composable
fun QuickActionsGrid(
    onCameraClick: () -> Unit,
    onCalculatorClick: () -> Unit,
    onClockClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onTimeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val actionsRow1 = listOf(
        QuickActionItem("camera", R.string.action_camera, SaarthiIcons.Camera, ElectricCyan, onCameraClick),
        QuickActionItem("calculator", R.string.action_calculator, SaarthiIcons.Calculator, SoftViolet, onCalculatorClick),
        QuickActionItem("clock", R.string.action_clock, SaarthiIcons.Clock, ElectricCyan, onClockClick)
    )

    val actionsRow2 = listOf(
        QuickActionItem("time", R.string.action_time, SaarthiIcons.Clock, SoftViolet, onTimeClick),
        QuickActionItem("calendar", R.string.action_calendar, SaarthiIcons.Calendar, ElectricCyan, onCalendarClick),
        QuickActionItem("settings", R.string.action_settings, SaarthiIcons.Settings, SoftViolet, onSettingsClick)
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (action in actionsRow1) {
                QuickActionButton(
                    action = action,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (action in actionsRow2) {
                QuickActionButton(
                    action = action,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun QuickActionButton(
    action: QuickActionItem,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(ElevatedSurface)
            .border(1.dp, SurfaceBorderDark, RoundedCornerShape(14.dp))
            .clickable(onClick = action.onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkBlueGrey),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = stringResource(id = action.titleRes),
                    tint = action.accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(id = action.titleRes),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}
