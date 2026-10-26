package com.saarthi.assistant.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.saarthi.assistant.R
import com.saarthi.assistant.core.TimerSnapshot
import com.saarthi.assistant.core.TimerState
import com.saarthi.assistant.ui.theme.DarkBlueGrey
import com.saarthi.assistant.ui.theme.ElectricCyan
import com.saarthi.assistant.ui.theme.ElevatedSurface
import com.saarthi.assistant.ui.theme.ErrorRed
import com.saarthi.assistant.ui.theme.MidnightNavy
import com.saarthi.assistant.ui.theme.SoftViolet
import com.saarthi.assistant.ui.theme.SuccessGreen
import com.saarthi.assistant.ui.theme.SurfaceBorderDark

@Composable
fun TimerCard(
    snapshot: TimerSnapshot,
    onStartTimer: (Long) -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onCancelTimer: () -> Unit,
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
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = SaarthiIcons.Timer,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.section_timer),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Status Badge
                val (badgeText, badgeColor) = when (snapshot.state) {
                    TimerState.IDLE -> Pair(stringResource(id = R.string.timer_no_active), MaterialTheme.colorScheme.onSurfaceVariant)
                    TimerState.RUNNING -> Pair(stringResource(id = R.string.timer_running), ElectricCyan)
                    TimerState.PAUSED -> Pair(stringResource(id = R.string.timer_paused), SoftViolet)
                    TimerState.FINISHED -> Pair(stringResource(id = R.string.timer_finished), SuccessGreen)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkBlueGrey)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Display / Countdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (snapshot.state == TimerState.IDLE) "00:00" else snapshot.formattedRemaining,
                        style = MaterialTheme.typography.headlineMedium,
                        color = when (snapshot.state) {
                            TimerState.RUNNING -> ElectricCyan
                            TimerState.PAUSED -> SoftViolet
                            TimerState.FINISHED -> SuccessGreen
                            TimerState.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    if (snapshot.state != TimerState.IDLE && snapshot.totalSeconds > 0) {
                        Text(
                            text = "of ${snapshot.formattedTotal}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Control Action Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (snapshot.state) {
                        TimerState.RUNNING -> {
                            Button(
                                onClick = onPauseTimer,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SoftViolet)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.timer_btn_pause),
                                    color = MidnightNavy,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            OutlinedButton(
                                onClick = onCancelTimer,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.timer_btn_cancel),
                                    color = ErrorRed,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }

                        TimerState.PAUSED -> {
                            Button(
                                onClick = onResumeTimer,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.timer_btn_resume),
                                    color = MidnightNavy,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            OutlinedButton(
                                onClick = onCancelTimer,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.timer_btn_cancel),
                                    color = ErrorRed,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }

                        TimerState.FINISHED -> {
                            Button(
                                onClick = onCancelTimer,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                            ) {
                                Text(
                                    text = "Dismiss",
                                    color = MidnightNavy,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }

                        TimerState.IDLE -> {
                            // When idle, show quick preset buttons below
                        }
                    }
                }
            }

            // Progress bar if active
            if (snapshot.state == TimerState.RUNNING || snapshot.state == TimerState.PAUSED) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { snapshot.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (snapshot.state == TimerState.RUNNING) ElectricCyan else SoftViolet,
                    trackColor = DarkBlueGrey
                )
            }

            // Quick Preset Buttons (visible whenever Idle or optionally available)
            if (snapshot.state == TimerState.IDLE) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Quick Presets:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PresetChip(text = stringResource(id = R.string.timer_quick_30s), seconds = 30L, onClick = onStartTimer)
                    PresetChip(text = stringResource(id = R.string.timer_quick_1m), seconds = 60L, onClick = onStartTimer)
                    PresetChip(text = stringResource(id = R.string.timer_quick_2m), seconds = 120L, onClick = onStartTimer)
                    PresetChip(text = stringResource(id = R.string.timer_quick_5m), seconds = 300L, onClick = onStartTimer)
                }
            }
        }
    }
}

@Composable
private fun PresetChip(
    text: String,
    seconds: Long,
    onClick: (Long) -> Unit
) {
    OutlinedButton(
        onClick = { onClick(seconds) },
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.height(34.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = ElectricCyan
        )
    }
}
