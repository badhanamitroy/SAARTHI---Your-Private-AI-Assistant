package com.saarthi.assistant.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.saarthi.assistant.R
import com.saarthi.assistant.ui.theme.DarkBlueGrey
import com.saarthi.assistant.ui.theme.ElectricCyan
import com.saarthi.assistant.ui.theme.ElevatedSurface
import com.saarthi.assistant.ui.theme.SoftViolet
import com.saarthi.assistant.ui.theme.SurfaceBorderDark

@Composable
fun AssistantResponseCard(
    userQuery: String,
    responseText: String,
    statusText: String,
    modifier: Modifier = Modifier
) {
    if (responseText.isBlank() && userQuery.isBlank()) {
        return
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ElevatedSurface)
            .border(1.dp, SurfaceBorderDark, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column {
            // User Query (if present)
            if (userQuery.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(SoftViolet)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = userQuery,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Saarthi Response
            if (responseText.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.Top
                ) {
                    SaarthiEmblem(size = 24.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = responseText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            // Status label if relevant (e.g. speaking, error)
            if (statusText.isNotBlank() && statusText != "Ready") {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricCyan,
                    modifier = Modifier.padding(start = 34.dp)
                )
            }
        }
    }
}
