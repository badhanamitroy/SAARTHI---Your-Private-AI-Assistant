package com.saarthi.assistant.ui.screens

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.saarthi.assistant.R
import com.saarthi.assistant.core.BatteryStatusResult
import com.saarthi.assistant.core.MathEvaluationResult
import com.saarthi.assistant.core.SafeMathParser
import com.saarthi.assistant.core.SystemSettingType
import com.saarthi.assistant.core.TimerController
import com.saarthi.assistant.core.TimerSnapshot
import com.saarthi.assistant.ui.components.HardwareControlsCard
import com.saarthi.assistant.ui.components.HelpSection
import com.saarthi.assistant.ui.components.SaarthiEmblem
import com.saarthi.assistant.ui.components.SaarthiIcons
import com.saarthi.assistant.ui.components.SystemSettingsCard
import com.saarthi.assistant.ui.components.TimerCard
import com.saarthi.assistant.ui.theme.DarkBlueGrey
import com.saarthi.assistant.ui.theme.ElectricCyan
import com.saarthi.assistant.ui.theme.ElevatedSurface
import com.saarthi.assistant.ui.theme.ErrorRed
import com.saarthi.assistant.ui.theme.MidnightNavy
import com.saarthi.assistant.ui.theme.SoftViolet
import com.saarthi.assistant.ui.theme.SuccessGreen
import com.saarthi.assistant.ui.theme.SurfaceBorderDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ToolsScreen(
    onCameraClick: () -> Unit,
    onCalculatorClick: () -> Unit,
    onClockClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onSettingsClick: () -> Unit,
    isBgRunning: Boolean,
    isBgPaused: Boolean,
    isVoiceFeedbackEnabled: Boolean,
    bgStatusText: String,
    onToggleBgService: () -> Unit,
    onPauseResumeBgService: () -> Unit,
    onToggleVoiceFeedback: () -> Unit,
    batteryStatus: BatteryStatusResult? = null,
    isFlashlightOn: Boolean = false,
    isFlashAvailable: Boolean = true,
    onCheckBatteryClick: () -> Unit = {},
    onSetFlashlightClick: (Boolean) -> Unit = {},
    timerSnapshot: TimerSnapshot = TimerController.snapshot.value,
    onStartTimer: (Long) -> Unit = {},
    onPauseTimer: () -> Unit = {},
    onResumeTimer: () -> Unit = {},
    onCancelTimer: () -> Unit = {},
    onOpenSetting: (SystemSettingType) -> Unit = {},
    onHelpExampleClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var mathInput by remember { mutableStateOf("") }
    var mathResult by remember { mutableStateOf("") }
    var mathError by remember { mutableStateOf(false) }

    val currentTime = remember {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
    }
    val currentDate = remember {
        SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date())
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Tools Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SaarthiEmblem(size = 32.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = stringResource(id = R.string.tools_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(id = R.string.tools_subtitle),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Live Time & Date Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Time Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ElevatedSurface)
                        .border(1.dp, SurfaceBorderDark, RoundedCornerShape(14.dp))
                        .clickable(onClick = onClockClick)
                        .padding(14.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = SaarthiIcons.Clock,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(id = R.string.action_time),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = currentTime,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                // Date Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ElevatedSurface)
                        .border(1.dp, SurfaceBorderDark, RoundedCornerShape(14.dp))
                        .clickable(onClick = onCalendarClick)
                        .padding(14.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = SaarthiIcons.Calendar,
                                contentDescription = null,
                                tint = SoftViolet,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(id = R.string.action_date),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = currentDate,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Hardware Controls Card (Battery & Flashlight)
            HardwareControlsCard(
                batteryStatus = batteryStatus,
                isFlashlightOn = isFlashlightOn,
                isFlashAvailable = isFlashAvailable,
                onCheckBatteryClick = onCheckBatteryClick,
                onSetFlashlightClick = onSetFlashlightClick
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Countdown Timer Card
            TimerCard(
                snapshot = timerSnapshot,
                onStartTimer = onStartTimer,
                onPauseTimer = onPauseTimer,
                onResumeTimer = onResumeTimer,
                onCancelTimer = onCancelTimer
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Safe System Settings Shortcuts Card
            SystemSettingsCard(
                onOpenSetting = onOpenSetting
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Inline Math Solver Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ElevatedSurface)
                    .border(1.dp, SurfaceBorderDark, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = SaarthiIcons.Calculator,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(id = R.string.action_math),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Text(
                            text = stringResource(id = R.string.action_calculator),
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan,
                            modifier = Modifier
                                .clickable(onClick = onCalculatorClick)
                                .padding(4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = mathInput,
                            onValueChange = {
                                mathInput = it
                                mathError = false
                            },
                            placeholder = {
                                Text(
                                    text = stringResource(id = R.string.tool_math_placeholder),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (mathInput.isNotBlank()) {
                                        when (val res = SafeMathParser.evaluate(mathInput)) {
                                            is MathEvaluationResult.Success -> {
                                                mathResult = res.formatted
                                                mathError = false
                                            }
                                            is MathEvaluationResult.Error -> {
                                                mathResult = res.message
                                                mathError = true
                                            }
                                        }
                                    }
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricCyan,
                                unfocusedBorderColor = SurfaceBorderDark,
                                focusedContainerColor = DarkBlueGrey,
                                unfocusedContainerColor = DarkBlueGrey
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (mathInput.isNotBlank()) {
                                    when (val res = SafeMathParser.evaluate(mathInput)) {
                                        is MathEvaluationResult.Success -> {
                                            mathResult = res.formatted
                                            mathError = false
                                        }
                                        is MathEvaluationResult.Error -> {
                                            mathResult = res.message
                                            mathError = true
                                        }
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                            enabled = mathInput.isNotBlank()
                        ) {
                            Text(
                                text = "=",
                                style = MaterialTheme.typography.titleMedium,
                                color = MidnightNavy
                            )
                        }
                    }

                    if (mathResult.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkBlueGrey)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = if (mathError) mathResult else "Result: $mathResult",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (mathError) ErrorRed else SuccessGreen
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Direct Native Launchers
            Text(
                text = "Native App Launchers",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ToolLaunchCard(
                    title = stringResource(id = R.string.action_camera),
                    description = stringResource(id = R.string.tool_camera_desc),
                    icon = SaarthiIcons.Camera,
                    accentColor = ElectricCyan,
                    onClick = onCameraClick
                )

                ToolLaunchCard(
                    title = stringResource(id = R.string.action_calculator),
                    description = stringResource(id = R.string.tool_calc_desc),
                    icon = SaarthiIcons.Calculator,
                    accentColor = SoftViolet,
                    onClick = onCalculatorClick
                )

                ToolLaunchCard(
                    title = stringResource(id = R.string.action_clock),
                    description = stringResource(id = R.string.tool_clock_desc),
                    icon = SaarthiIcons.Clock,
                    accentColor = ElectricCyan,
                    onClick = onClockClick
                )

                ToolLaunchCard(
                    title = stringResource(id = R.string.action_calendar),
                    description = stringResource(id = R.string.tool_calendar_desc),
                    icon = SaarthiIcons.Calendar,
                    accentColor = SoftViolet,
                    onClick = onCalendarClick
                )

                ToolLaunchCard(
                    title = stringResource(id = R.string.action_settings),
                    description = stringResource(id = R.string.tool_settings_desc),
                    icon = SaarthiIcons.Settings,
                    accentColor = ElectricCyan,
                    onClick = onSettingsClick
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Help & Command Guide Section
            HelpSection(
                onExampleClick = onHelpExampleClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Background Assistant Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ElevatedSurface)
                    .border(1.dp, SurfaceBorderDark, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = stringResource(id = R.string.section_background_assistant),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isBgRunning) {
                            if (isBgPaused) "State: Paused" else "State: Active ($bgStatusText)"
                        } else {
                            "State: Inactive (Stopped)"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isBgRunning && !isBgPaused) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onToggleBgService,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isBgRunning) ErrorRed else ElectricCyan
                            )
                        ) {
                            Text(
                                text = if (isBgRunning) "Stop Service" else "Start Service",
                                color = MidnightNavy,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        if (isBgRunning) {
                            OutlinedButton(
                                onClick = onPauseResumeBgService,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = if (isBgPaused) "Resume" else "Pause",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onToggleVoiceFeedback,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isVoiceFeedbackEnabled) SaarthiIcons.VolumeUp else SaarthiIcons.VolumeOff,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isVoiceFeedbackEnabled) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isVoiceFeedbackEnabled) "Voice Feedback: ON" else "Voice Feedback: MUTED",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ToolLaunchCard(
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ElevatedSurface)
            .border(1.dp, SurfaceBorderDark, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkBlueGrey),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

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

            Text(
                text = "Launch →",
                style = MaterialTheme.typography.labelSmall,
                color = ElectricCyan
            )
        }
    }
}
