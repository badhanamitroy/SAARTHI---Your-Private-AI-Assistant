package com.saarthi.assistant.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.saarthi.assistant.R
import com.saarthi.assistant.ui.theme.DarkBlueGrey
import com.saarthi.assistant.ui.theme.ElectricCyan
import com.saarthi.assistant.ui.theme.ElevatedSurface
import com.saarthi.assistant.ui.theme.ErrorRed
import com.saarthi.assistant.ui.theme.MidnightNavy
import com.saarthi.assistant.ui.theme.SoftViolet
import com.saarthi.assistant.ui.theme.SuccessGreen

enum class MicState {
    IDLE,
    LISTENING,
    PROCESSING,
    RESPONDING,
    PERMISSION_DENIED
}

@Composable
fun VoiceInteractionArea(
    micState: MicState,
    onMicClick: () -> Unit,
    onCancelClick: () -> Unit,
    onStopSpeakingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_waves")

    val pulseScale by if (micState == MicState.LISTENING) {
        infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "mic_scale"
        )
    } else {
        remember { androidx.compose.runtime.mutableFloatStateOf(1.0f) }
    }

    val glowColor = when (micState) {
        MicState.IDLE -> ElectricCyan.copy(alpha = 0.25f)
        MicState.LISTENING -> ElectricCyan.copy(alpha = 0.65f)
        MicState.PROCESSING -> SoftViolet.copy(alpha = 0.65f)
        MicState.RESPONDING -> SuccessGreen.copy(alpha = 0.65f)
        MicState.PERMISSION_DENIED -> ErrorRed.copy(alpha = 0.45f)
    }

    val buttonBgBrush = when (micState) {
        MicState.IDLE -> Brush.linearGradient(listOf(ElevatedSurface, DarkBlueGrey))
        MicState.LISTENING -> Brush.linearGradient(listOf(ElectricCyan, SoftViolet))
        MicState.PROCESSING -> Brush.linearGradient(listOf(SoftViolet, DarkBlueGrey))
        MicState.RESPONDING -> Brush.linearGradient(listOf(SuccessGreen, ElevatedSurface))
        MicState.PERMISSION_DENIED -> Brush.linearGradient(listOf(ElevatedSurface, DarkBlueGrey))
    }

    val iconTint = when (micState) {
        MicState.LISTENING -> MidnightNavy
        MicState.IDLE -> ElectricCyan
        MicState.PERMISSION_DENIED -> ErrorRed
        else -> MaterialTheme.colorScheme.onBackground
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Outer Mic Target Container
        Box(
            modifier = Modifier
                .size(88.dp),
            contentAlignment = Alignment.Center
        ) {
            // Animated Ambient Aura Ring
            if (micState == MicState.LISTENING) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(glowColor)
                )
            }

            // Primary Mic Button
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(buttonBgBrush)
                    .border(
                        width = 1.5.dp,
                        color = when (micState) {
                            MicState.LISTENING -> Color.White.copy(alpha = 0.6f)
                            MicState.PERMISSION_DENIED -> ErrorRed
                            else -> ElectricCyan.copy(alpha = 0.4f)
                        },
                        shape = CircleShape
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onMicClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (micState == MicState.PROCESSING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = ElectricCyan,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Icon(
                        imageVector = if (micState == MicState.RESPONDING) SaarthiIcons.Stop else SaarthiIcons.Mic,
                        contentDescription = stringResource(id = R.string.mic_tap_to_speak),
                        tint = iconTint,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // State Text Label
        val stateLabel = when (micState) {
            MicState.IDLE -> stringResource(id = R.string.mic_tap_to_speak)
            MicState.LISTENING -> stringResource(id = R.string.mic_listening)
            MicState.PROCESSING -> stringResource(id = R.string.mic_processing)
            MicState.RESPONDING -> stringResource(id = R.string.mic_responding)
            MicState.PERMISSION_DENIED -> stringResource(id = R.string.mic_permission_needed)
        }

        Text(
            text = stateLabel,
            style = MaterialTheme.typography.labelLarge,
            color = when (micState) {
                MicState.LISTENING -> ElectricCyan
                MicState.PERMISSION_DENIED -> ErrorRed
                MicState.RESPONDING -> SuccessGreen
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )

        // Cancel / Stop Actions
        AnimatedVisibility(
            visible = micState == MicState.LISTENING || micState == MicState.RESPONDING,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                if (micState == MicState.LISTENING) {
                    OutlinedButton(
                        onClick = onCancelClick,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.mic_cancel),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                } else if (micState == MicState.RESPONDING) {
                    OutlinedButton(
                        onClick = onStopSpeakingClick,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.mic_stop_speaking),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}
