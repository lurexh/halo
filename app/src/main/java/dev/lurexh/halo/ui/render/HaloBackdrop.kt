package dev.lurexh.halo.ui.render

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import dev.lurexh.halo.ui.theme.HaloCyan
import dev.lurexh.halo.ui.theme.HaloPink
import dev.lurexh.halo.ui.theme.Proton
import dev.lurexh.halo.ui.theme.Void
import kotlin.math.sin

@Composable
fun HaloBackdrop(modifier: Modifier = Modifier) {
    val drift by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
    )
    val breathe by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
    )
    val sweep by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
    )

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val center = this.center

        drawRect(
            brush = Brush.verticalGradient(
                listOf(Void, Color(0xFF0A0C16), Color(0xFF08060F)),
            ),
        )

        repeat(4) { index ->
            val phase = (drift + index * 0.23f) % 1f
            val cx = w * (0.12f + 0.76f * phase)
            val cy = h * (0.10f + 0.18f * index + 0.05f * breathe)
            val radius = w * (0.55f + 0.08f * index)
            val tint = when (index % 3) {
                0 -> Proton
                1 -> HaloCyan
                else -> HaloPink
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(tint.copy(alpha = 0.13f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = radius,
                ),
                radius = radius,
                center = Offset(cx, cy),
            )
        }

        repeat(3) { index ->
            val offset = (drift + index * 0.31f) % 1f
            val path = Path().apply {
                moveTo(-w * 0.1f, h * (0.08f + index * 0.12f))
                cubicTo(
                    w * 0.25f, h * (0.40f + offset * 0.10f),
                    w * 0.62f, h * (0.02f + index * 0.08f),
                    w * 1.1f, h * (0.24f + index * 0.10f),
                )
                lineTo(w * 1.1f, h * 1.1f)
                lineTo(-w * 0.1f, h * 1.1f)
                close()
            }
            val tint = when (index) {
                0 -> Proton
                1 -> HaloCyan
                else -> HaloPink
            }
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(tint.copy(alpha = 0.09f), Color.Transparent),
                    startY = h * 0.05f,
                    endY = h * 0.9f,
                ),
            )
        }

        val gridStep = 44.dp.toPx()
        var x = 0f
        while (x <= w) {
            drawLine(
                color = HaloCyan.copy(alpha = 0.035f),
                start = Offset(x, 0f),
                end = Offset(x, h),
                strokeWidth = 0.6.dp.toPx(),
            )
            x += gridStep
        }
        var y = 0f
        while (y <= h) {
            drawLine(
                color = HaloCyan.copy(alpha = 0.035f),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 0.6.dp.toPx(),
            )
            y += gridStep
        }

        val horizon = h * 0.63f
        for (line in 0..7) {
            val t = line / 7f
            val py = horizon + (h - horizon) * (t * t)
            drawLine(
                color = Proton.copy(alpha = 0.10f * (1f - t * 0.5f)),
                start = Offset(0f, py),
                end = Offset(w, py),
                strokeWidth = 0.7.dp.toPx(),
            )
        }
        for (line in -6..6) {
            drawLine(
                color = Proton.copy(alpha = 0.08f),
                start = Offset(w * 0.5f, horizon),
                end = Offset(w * 0.5f + line * w * 0.15f, h),
                strokeWidth = 0.7.dp.toPx(),
            )
        }

        repeat(64) { index ->
            val px = frac(sin(index * 12.9898f) * 43758.5453f) * w
            val py = frac(sin(index * 78.233f) * 12345.678f) * h
            val twinkle = 0.25f + 0.75f * frac(sin((index + drift * 3f) * 39.346f) * 24634.6345f)
            drawCircle(
                color = Color.White.copy(alpha = 0.16f * twinkle),
                radius = (0.6f + twinkle) * 0.7.dp.toPx(),
                center = Offset(px, py),
            )
        }

        val scanY = sweep * h
        drawLine(
            color = HaloCyan.copy(alpha = 0.08f),
            start = Offset(0f, scanY),
            end = Offset(w, scanY),
            strokeWidth = 1.2.dp.toPx(),
        )

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Void.copy(alpha = 0.82f)),
                center = center,
                radius = maxOf(w, h) * 0.78f,
            ),
        )

        drawCircle(
            color = HaloCyan.copy(alpha = 0.05f * breathe),
            radius = maxOf(w, h) * 0.42f,
            center = center,
            style = Stroke(width = 0.8.dp.toPx()),
        )
    }
}

private fun frac(value: Float): Float = value - kotlin.math.floor(value)
