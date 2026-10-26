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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.saarthi.assistant.R
import com.saarthi.assistant.core.HistoryItem
import com.saarthi.assistant.ui.theme.DarkBlueGrey
import com.saarthi.assistant.ui.theme.ElectricCyan
import com.saarthi.assistant.ui.theme.ElevatedSurface
import com.saarthi.assistant.ui.theme.SoftViolet
import com.saarthi.assistant.ui.theme.SurfaceBorderDark

@Composable
fun RecentActivitySection(
    historyItems: List<HistoryItem>,
    onItemClick: (HistoryItem) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showConfirmDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.section_recent_activity),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (historyItems.isNotEmpty()) {
                Text(
                    text = stringResource(id = R.string.btn_clear_history),
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricCyan,
                    modifier = Modifier
                        .clickable { showConfirmDialog = true }
                        .padding(4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (historyItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ElevatedSurface)
                    .border(1.dp, SurfaceBorderDark, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.empty_recent_activity),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Show latest 3 items on Home screen
                for (item in historyItems.take(3)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ElevatedSurface)
                            .border(1.dp, SurfaceBorderDark, RoundedCornerShape(12.dp))
                            .clickable { onItemClick(item) }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.query,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.response,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item.formattedTime,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Text(text = stringResource(id = R.string.dialog_clear_title))
            },
            text = {
                Text(text = stringResource(id = R.string.dialog_clear_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        onClearHistory()
                    }
                ) {
                    Text(
                        text = stringResource(id = R.string.btn_confirm),
                        color = ElectricCyan
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmDialog = false }
                ) {
                    Text(text = stringResource(id = R.string.btn_cancel))
                }
            },
            containerColor = ElevatedSurface,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
