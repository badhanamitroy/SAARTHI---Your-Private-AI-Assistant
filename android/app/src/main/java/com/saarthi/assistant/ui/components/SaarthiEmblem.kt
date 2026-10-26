package com.saarthi.assistant.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.saarthi.assistant.ui.theme.DarkBlueGrey
import com.saarthi.assistant.ui.theme.ElectricCyan
import com.saarthi.assistant.ui.theme.ElevatedSurface
import com.saarthi.assistant.ui.theme.MidnightNavy
import com.saarthi.assistant.ui.theme.SoftViolet

@Composable
fun SaarthiEmblem(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    isListening: Boolean = false,
    isProcessing: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "emblem_anim")

    val pulseScale by if (isListening) {
        infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(1100, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_scale"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(1.0f) }
    }

    val glowAlpha by if (isListening || isProcessing) {
        infiniteTransition.animateFloat(
            initialValue = 0.35f,
            targetValue = 0.85f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glow_alpha"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0.4f) }
    }

    val gradientBorder = Brush.linearGradient(
        colors = listOf(
            ElectricCyan.copy(alpha = glowAlpha),
            SoftViolet.copy(alpha = glowAlpha)
        ),
        start = Offset(0f, 0f),
        end = Offset(100f, 100f)
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(pulseScale),
        contentAlignment = Alignment.Center
    ) {
        // Outer Squircle
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(size * 0.28f))
                .background(ElevatedSurface)
                .border(
                    width = 1.5.dp,
                    brush = gradientBorder,
                    shape = RoundedCornerShape(size * 0.28f)
                ),
            contentAlignment = Alignment.Center
        ) {
            // Geometric Stylized "S" + Spark Emblem
            Canvas(modifier = Modifier.size(size * 0.58f)) {
                val w = this.size.width
                val h = this.size.height

                // Flowing geometric dual arcs
                val path1 = Path().apply {
                    moveTo(w * 0.80f, h * 0.24f)
                    cubicTo(w * 0.40f, h * 0.10f, w * 0.15f, h * 0.35f, w * 0.50f, h * 0.50f)
                    cubicTo(w * 0.85f, h * 0.65f, w * 0.60f, h * 0.90f, w * 0.20f, h * 0.76f)
                }

                drawPath(
                    path = path1,
                    brush = Brush.linearGradient(
                        listOf(ElectricCyan, SoftViolet)
                    ),
                    style = Stroke(
                        width = w * 0.12f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Central intelligence spark dot
                drawCircle(
                    color = ElectricCyan,
                    radius = w * 0.08f,
                    center = Offset(w * 0.5f, h * 0.5f)
                )

                // Top right subtle node
                drawCircle(
                    color = SoftViolet,
                    radius = w * 0.05f,
                    center = Offset(w * 0.78f, h * 0.24f)
                )
            }
        }
    }
}
