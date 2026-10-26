package com.saarthi.assistant.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.saarthi.assistant.R
import com.saarthi.assistant.core.HelpCategory
import com.saarthi.assistant.core.HelpRegistry
import com.saarthi.assistant.ui.theme.DarkBlueGrey
import com.saarthi.assistant.ui.theme.ElectricCyan
import com.saarthi.assistant.ui.theme.ElevatedSurface
import com.saarthi.assistant.ui.theme.SoftViolet
import com.saarthi.assistant.ui.theme.SurfaceBorderDark

@Composable
fun HelpSection(
    onExampleClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var expandedCategoryId by remember { mutableStateOf<String?>("battery") }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ElevatedSurface)
            .border(1.dp, SurfaceBorderDark, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = SaarthiIcons.Help,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = stringResource(id = R.string.section_help),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(id = R.string.help_subtitle),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HelpRegistry.categories.forEach { category ->
                    val isExpanded = expandedCategoryId == category.id
                    HelpCategoryAccordionItem(
                        category = category,
                        isExpanded = isExpanded,
                        onToggle = {
                            expandedCategoryId = if (isExpanded) null else category.id
                        },
                        onExampleClick = onExampleClick
                    )
                }
            }
        }
    }
}

@Composable
private fun HelpCategoryAccordionItem(
    category: HelpCategory,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onExampleClick: (String) -> Unit
) {
    val icon = when (category.iconName) {
        "Battery" -> SaarthiIcons.Battery
        "Flashlight" -> SaarthiIcons.FlashlightOn
        "Timer" -> SaarthiIcons.Timer
        "Wifi" -> SaarthiIcons.Wifi
        "Bluetooth" -> SaarthiIcons.Bluetooth
        "MobileData" -> SaarthiIcons.MobileData
        "Volume" -> SaarthiIcons.VolumeUp
        "Brightness" -> SaarthiIcons.Brightness
        "Calculator" -> SaarthiIcons.Calculator
        "Clock" -> SaarthiIcons.Clock
        "Apps" -> SaarthiIcons.Tools
        else -> SaarthiIcons.Help
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkBlueGrey)
            .border(1.dp, SurfaceBorderDark, RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle)
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = category.title,
                    tint = if (isExpanded) ElectricCyan else SoftViolet,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = category.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = if (isExpanded) "▲" else "▼",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    category.examples.forEach { example ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(ElevatedSurface)
                                .padding(8.dp)
                        ) {
                            Column {
                                if (example.description.isNotBlank()) {
                                    Text(
                                        text = example.description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ElectricCyan
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "EN: \"${example.english}\"",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onExampleClick(example.english) }
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "BN-En: \"${example.banglish}\"",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onExampleClick(example.banglish) }
                                    )
                                    Text(
                                        text = "বাংলা: \"${example.bangla}\"",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onExampleClick(example.bangla) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
