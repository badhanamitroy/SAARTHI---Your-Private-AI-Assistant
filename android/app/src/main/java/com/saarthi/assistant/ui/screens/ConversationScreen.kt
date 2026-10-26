package com.saarthi.assistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.saarthi.assistant.R
import com.saarthi.assistant.core.HistoryItem
import com.saarthi.assistant.ui.components.MicState
import com.saarthi.assistant.ui.components.SaarthiEmblem
import com.saarthi.assistant.ui.components.SaarthiIcons
import com.saarthi.assistant.ui.components.TextCommandInput
import com.saarthi.assistant.ui.theme.DarkBlueGrey
import com.saarthi.assistant.ui.theme.ElectricCyan
import com.saarthi.assistant.ui.theme.ElevatedSurface
import com.saarthi.assistant.ui.theme.MidnightNavy
import com.saarthi.assistant.ui.theme.SoftViolet
import com.saarthi.assistant.ui.theme.SurfaceBorderDark

@Composable
fun ConversationScreen(
    historyItems: List<HistoryItem>,
    typedText: String,
    onTypedTextChange: (String) -> Unit,
    onSendText: (String) -> Unit,
    micState: MicState,
    onMicClick: () -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var showClearDialog by remember { mutableStateOf(false) }

    // Scroll to latest when new items arrive
    LaunchedEffect(historyItems.size) {
        if (historyItems.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Conversation Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SaarthiEmblem(size = 32.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(id = R.string.section_conversation),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (historyItems.isNotEmpty()) {
                IconButton(
                    onClick = { showClearDialog = true }
                ) {
                    Icon(
                        imageVector = SaarthiIcons.Trash,
                        contentDescription = stringResource(id = R.string.btn_clear_history),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Messages List
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (historyItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        SaarthiEmblem(size = 64.dp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(id = R.string.greeting_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(id = R.string.empty_conversation),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    reverseLayout = true // Newest at bottom
                ) {
                    items(historyItems, key = { it.id }) { item ->
                        ConversationItem(item = item)
                    }
                }
            }
        }

        // Processing indicator (if listening or processing)
        if (micState == MicState.LISTENING || micState == MicState.PROCESSING) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = ElectricCyan
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (micState == MicState.LISTENING) {
                        stringResource(id = R.string.mic_listening)
                    } else {
                        stringResource(id = R.string.mic_processing)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricCyan
                )
            }
        }

        // Persistent Bottom Composer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkBlueGrey)
                .border(1.dp, SurfaceBorderDark)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Voice Input Trigger
            IconButton(
                onClick = onMicClick,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (micState == MicState.LISTENING) ElectricCyan else ElevatedSurface
                    )
            ) {
                Icon(
                    imageVector = SaarthiIcons.Mic,
                    contentDescription = stringResource(id = R.string.mic_tap_to_speak),
                    tint = if (micState == MicState.LISTENING) MidnightNavy else ElectricCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Text Input
            TextCommandInput(
                value = typedText,
                onValueChange = onTypedTextChange,
                onSend = onSendText,
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(text = stringResource(id = R.string.dialog_clear_title)) },
            text = { Text(text = stringResource(id = R.string.dialog_clear_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        onClearHistory()
                    }
                ) {
                    Text(text = stringResource(id = R.string.btn_confirm), color = ElectricCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
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
fun ConversationItem(item: HistoryItem) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // User Message Bubble (Right aligned)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                    .background(ElevatedSurface)
                    .border(1.dp, SurfaceBorderDark, RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column {
                    Text(
                        text = item.query,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Saarthi Message Bubble (Left aligned)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            SaarthiEmblem(size = 28.dp)
            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .widthIn(max = 290.dp)
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                    .background(DarkBlueGrey)
                    .border(1.dp, SurfaceBorderDark, RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column {
                    Text(
                        text = item.response,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (!item.appLabel.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "● ${item.appLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan
                        )
                    }
                }
            }
        }
    }
}
