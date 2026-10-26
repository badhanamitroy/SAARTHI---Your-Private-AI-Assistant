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
import com.saarthi.assistant.ui.theme.SurfaceBorderDark

enum class SaarthiTab {
    HOME,
    CHAT,
    TOOLS,
    SETTINGS
}

data class TabItem(
    val tab: SaarthiTab,
    val titleRes: Int,
    val icon: ImageVector
)

@Composable
fun SaarthiBottomBar(
    currentTab: SaarthiTab,
    onTabSelected: (SaarthiTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        TabItem(SaarthiTab.HOME, R.string.nav_home, SaarthiIcons.Home),
        TabItem(SaarthiTab.CHAT, R.string.nav_chat, SaarthiIcons.Chat),
        TabItem(SaarthiTab.TOOLS, R.string.nav_tools, SaarthiIcons.Tools),
        TabItem(SaarthiTab.SETTINGS, R.string.nav_settings, SaarthiIcons.Settings)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(DarkBlueGrey)
            .border(
                width = 1.dp,
                color = SurfaceBorderDark,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            .padding(horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (item in tabs) {
                val isSelected = currentTab == item.tab
                val tint = if (isSelected) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .clickable { onTabSelected(item.tab) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = stringResource(id = item.titleRes),
                            tint = tint,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = stringResource(id = item.titleRes),
                            style = MaterialTheme.typography.labelSmall,
                            color = tint
                        )
                    }
                }
            }
        }
    }
}
