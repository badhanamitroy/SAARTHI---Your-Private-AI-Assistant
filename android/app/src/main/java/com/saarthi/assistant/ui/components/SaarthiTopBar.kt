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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.saarthi.assistant.R
import com.saarthi.assistant.core.NetworkStatus
import com.saarthi.assistant.ui.theme.ElectricCyan
import com.saarthi.assistant.ui.theme.ElevatedSurface
import com.saarthi.assistant.ui.theme.SuccessGreen
import com.saarthi.assistant.ui.theme.SurfaceBorderDark

@Composable
fun SaarthiTopBar(
    networkStatus: NetworkStatus,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Emblem + App Name + Subtitle
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            SaarthiEmblem(size = 36.dp)

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = stringResource(id = R.string.assistant_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(id = R.string.assistant_subtitle),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Right: Honest Connectivity Badge + Settings Action
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Connectivity Pill Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(ElevatedSurface)
                    .border(1.dp, SurfaceBorderDark, RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(
                                if (networkStatus == NetworkStatus.ONLINE) ElectricCyan else SuccessGreen
                            )
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (networkStatus == NetworkStatus.ONLINE) {
                            stringResource(id = R.string.status_online)
                        } else {
                            stringResource(id = R.string.status_offline_ready)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = SaarthiIcons.Settings,
                    contentDescription = stringResource(id = R.string.nav_settings),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
