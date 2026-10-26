package com.saarthi.assistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.saarthi.assistant.R
import com.saarthi.assistant.core.BatteryStatusResult
import com.saarthi.assistant.core.HistoryItem
import com.saarthi.assistant.core.NetworkStatus
import com.saarthi.assistant.core.TimerState
import com.saarthi.assistant.ui.components.AssistantResponseCard
import com.saarthi.assistant.ui.components.DeviceHardwareSummaryRow
import com.saarthi.assistant.ui.components.MicState
import com.saarthi.assistant.ui.components.QuickActionsGrid
import com.saarthi.assistant.ui.components.RecentActivitySection
import com.saarthi.assistant.ui.components.SaarthiEmblem
import com.saarthi.assistant.ui.components.SaarthiTopBar
import com.saarthi.assistant.ui.components.TextCommandInput
import com.saarthi.assistant.ui.components.TimerCard
import com.saarthi.assistant.ui.components.VoiceInteractionArea
import com.saarthi.assistant.ui.theme.DarkBlueGrey
import com.saarthi.assistant.ui.theme.ElevatedSurface
import com.saarthi.assistant.ui.theme.SurfaceBorderDark

@Composable
fun HomeScreen(
    networkStatus: NetworkStatus,
    micState: MicState,
    typedText: String,
    onTypedTextChange: (String) -> Unit,
    onSendText: (String) -> Unit,
    onMicClick: () -> Unit,
    onCancelListening: () -> Unit,
    onStopSpeaking: () -> Unit,
    lastUserQuery: String,
    lastAssistantResponse: String,
    statusText: String,
    historyItems: List<HistoryItem>,
    onHistoryItemClick: (HistoryItem) -> Unit,
    onClearHistory: () -> Unit,
    onCameraClick: () -> Unit,
    onCalculatorClick: () -> Unit,
    onClockClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onTimeClick: () -> Unit,
    batteryStatus: BatteryStatusResult? = null,
    isFlashlightOn: Boolean = false,
    isFlashAvailable: Boolean = true,
    onCheckBatteryClick: () -> Unit = {},
    onToggleFlashlightClick: () -> Unit = {},
    timerSnapshot: com.saarthi.assistant.core.TimerSnapshot = com.saarthi.assistant.core.TimerController.snapshot.value,
    onPauseTimer: () -> Unit = {},
    onResumeTimer: () -> Unit = {},
    onCancelTimer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        SaarthiTopBar(
            networkStatus = networkStatus,
            onSettingsClick = onSettingsClick
        )

        // Scrollable Home Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Assistant Identity Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(ElevatedSurface)
                    .border(1.dp, SurfaceBorderDark, RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SaarthiEmblem(
                        size = 48.dp,
                        isListening = micState == MicState.LISTENING,
                        isProcessing = micState == MicState.PROCESSING
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = stringResource(id = R.string.greeting_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(id = R.string.greeting_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Voice Interaction Area
            VoiceInteractionArea(
                micState = micState,
                onMicClick = onMicClick,
                onCancelClick = onCancelListening,
                onStopSpeakingClick = onStopSpeaking
            )

            // Compact Text Command Input
            TextCommandInput(
                value = typedText,
                onValueChange = onTypedTextChange,
                onSend = onSendText,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            // Latest Response (if present)
            if (lastAssistantResponse.isNotBlank() || lastUserQuery.isNotBlank()) {
                AssistantResponseCard(
                    userQuery = lastUserQuery,
                    responseText = lastAssistantResponse,
                    statusText = statusText,
                    modifier = Modifier.padding(bottom = 14.dp)
                )
            }

            // Active Timer Banner (shows when timer is running/paused/ringing)
            if (timerSnapshot.state != TimerState.IDLE) {
                TimerCard(
                    snapshot = timerSnapshot,
                    onStartTimer = {},
                    onPauseTimer = onPauseTimer,
                    onResumeTimer = onResumeTimer,
                    onCancelTimer = onCancelTimer,
                    modifier = Modifier.padding(bottom = 14.dp)
                )
            }

            // Device Hardware Essentials (Battery & Flashlight)
            DeviceHardwareSummaryRow(
                batteryStatus = batteryStatus,
                isFlashlightOn = isFlashlightOn,
                isFlashAvailable = isFlashAvailable,
                onCheckBatteryClick = onCheckBatteryClick,
                onToggleFlashlightClick = onToggleFlashlightClick,
                modifier = Modifier.padding(bottom = 14.dp)
            )

            // Quick Actions Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = stringResource(id = R.string.section_quick_actions),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            QuickActionsGrid(
                onCameraClick = onCameraClick,
                onCalculatorClick = onCalculatorClick,
                onClockClick = onClockClick,
                onCalendarClick = onCalendarClick,
                onSettingsClick = onSettingsClick,
                onTimeClick = onTimeClick,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Recent Activity Section
            RecentActivitySection(
                historyItems = historyItems,
                onItemClick = onHistoryItemClick,
                onClearHistory = onClearHistory,
                modifier = Modifier.padding(bottom = 20.dp)
            )
        }
    }
}
