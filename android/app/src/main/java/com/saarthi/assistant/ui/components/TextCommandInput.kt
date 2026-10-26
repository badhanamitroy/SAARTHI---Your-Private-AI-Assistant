package com.saarthi.assistant.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.saarthi.assistant.R
import com.saarthi.assistant.ui.theme.DarkBlueGrey
import com.saarthi.assistant.ui.theme.ElectricCyan
import com.saarthi.assistant.ui.theme.ElevatedSurface
import com.saarthi.assistant.ui.theme.MidnightNavy
import com.saarthi.assistant.ui.theme.SurfaceBorderDark

@Composable
fun TextCommandInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val canSend = value.isNotBlank()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(ElevatedSurface)
            .border(1.dp, SurfaceBorderDark, RoundedCornerShape(26.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.input_placeholder),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground
                ),
                cursorBrush = SolidColor(ElectricCyan),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (canSend) {
                            onSend(value)
                        }
                    }
                )
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Send Button
        IconButton(
            onClick = {
                if (canSend) {
                    onSend(value)
                }
            },
            enabled = canSend,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    if (canSend) ElectricCyan else DarkBlueGrey
                )
        ) {
            Icon(
                imageVector = SaarthiIcons.Send,
                contentDescription = stringResource(id = R.string.btn_send),
                tint = if (canSend) MidnightNavy else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
