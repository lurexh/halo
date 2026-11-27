package dev.lurexh.halo.ui.render

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.lurexh.halo.ui.theme.HaloCyan
import dev.lurexh.halo.ui.theme.Proton
import dev.lurexh.halo.ui.theme.ProtonDeep
import dev.lurexh.halo.ui.theme.haloStatusColor
import dev.lurexh.halo.ui.theme.haloStatusLabel
import dev.lurexh.halo.vpn.VpnStatus
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ReactorOrb(
    status: VpnStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val color = haloStatusColor(status)
    val transition = rememberInfiniteTransition()
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
    )
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
    )

    Box(
        modifier = modifier
            .size(144.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .semantics(mergeDescendants = true) {
                contentDescription = "${haloStatusLabel(status)}. Tap to ${if (status == VpnStatus.CONNECTED) "disconnect" else "connect"}."
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(160.dp)) {
            val center = this.center
            val radius = size.minDimension * 0.31f

            drawCircle(
                color = color.copy(alpha = 0.05f + pulse * 0.05f),
                radius = radius * (1.70f + pulse * 0.12f),
                center = center,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.20f), Color.Transparent),
                    center = center,
                    radius = radius * 1.8f,
                ),
                radius = radius * 1.8f,
                center = center,
            )

            drawArc(
                color = HaloCyan.copy(alpha = 0.22f),
                startAngle = spin,
                sweepAngle = 92f,
                useCenter = false,
                topLeft = Offset(center.x - radius * 1.42f, center.y - radius * 1.42f),
                size = androidx.compose.ui.geometry.Size(radius * 2.84f, radius * 2.84f),
                style = Stroke(width = 1.3.dp.toPx()),
            )
            drawArc(
                color = Proton.copy(alpha = 0.28f),
                startAngle = -spin * 0.72f,
                sweepAngle = 120f,
                useCenter = false,
                topLeft = Offset(center.x - radius * 1.18f, center.y - radius * 1.18f),
                size = androidx.compose.ui.geometry.Size(radius * 2.36f, radius * 2.36f),
                style = Stroke(width = 1.dp.toPx()),
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.06f),
                radius = radius,
                center = center,
                style = Stroke(width = 1.dp.toPx()),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.30f), color.copy(alpha = 0.95f), ProtonDeep),
                    center = Offset(center.x - radius * 0.24f, center.y - radius * 0.28f),
                    radius = radius,
                ),
                radius = radius * 0.78f,
                center = center,
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.16f),
                radius = radius * 0.24f,
                center = Offset(center.x - radius * 0.18f, center.y - radius * 0.24f),
            )

            repeat(8) { index ->
                val angle = Math.toRadians((spin + index * 45f).toDouble())
                val inner = radius * 1.02f
                val outer = radius * (1.20f + 0.05f * sin(spin * 0.05f + index))
                drawLine(
                    color = color.copy(alpha = 0.35f),
                    start = Offset(center.x + cos(angle).toFloat() * inner, center.y + sin(angle).toFloat() * inner),
                    end = Offset(center.x + cos(angle).toFloat() * outer, center.y + sin(angle).toFloat() * outer),
                    strokeWidth = 1.2.dp.toPx(),
                )
            }
        }
    }
}
