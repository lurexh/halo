package dev.lurexh.halo.ui.render

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import dev.lurexh.halo.ui.theme.ConnectedGreen
import dev.lurexh.halo.ui.theme.HaloBorder
import dev.lurexh.halo.ui.theme.HaloCyan
import dev.lurexh.halo.ui.theme.Proton
import dev.lurexh.halo.vpn.CountryGeo
import dev.lurexh.halo.vpn.Server
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RelayRadar(
    servers: List<Server>,
    activeCode: String?,
    modifier: Modifier = Modifier,
) {
    val sweep by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
    )
    val blips = remember(servers) {
        servers.mapNotNull { server ->
            CountryGeo.centroid(server.countryCode)?.let { geo ->
                val latRad = Math.toRadians(geo.lat.toDouble())
                val lonRad = Math.toRadians(geo.lon.toDouble())
                Offset(
                    x = (cos(latRad) * sin(lonRad)).toFloat(),
                    y = (-sin(latRad)).toFloat(),
                )
            }
        }.distinctBy { "${it.x.round2()}:${it.y.round2()}" }
    }

    Canvas(modifier) {
        val center = this.center
        val radius = size.minDimension * 0.42f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Proton.copy(alpha = 0.16f), Color.Transparent),
                center = center,
                radius = radius * 1.4f,
            ),
            radius = radius * 1.4f,
            center = center,
        )
        drawCircle(
            color = HaloCyan.copy(alpha = 0.20f),
            radius = radius,
            center = center,
            style = Stroke(width = 1.dp.toPx()),
        )
        repeat(3) { index ->
            drawCircle(
                color = HaloBorder.copy(alpha = 0.7f),
                radius = radius * (index + 1) / 3f,
                center = center,
                style = Stroke(width = 0.8.dp.toPx()),
            )
        }
        drawLine(
            color = HaloBorder.copy(alpha = 0.6f),
            start = Offset(center.x - radius, center.y),
            end = Offset(center.x + radius, center.y),
            strokeWidth = 0.8.dp.toPx(),
        )
        drawLine(
            color = HaloBorder.copy(alpha = 0.6f),
            start = Offset(center.x, center.y - radius),
            end = Offset(center.x, center.y + radius),
            strokeWidth = 0.8.dp.toPx(),
        )

        val angle = Math.toRadians(sweep.toDouble()).toFloat()
        val wedge = Path().apply {
            moveTo(center.x, center.y)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(
                    center.x - radius,
                    center.y - radius,
                    center.x + radius,
                    center.y + radius,
                ),
                startAngleDegrees = sweep - 34f,
                sweepAngleDegrees = 34f,
                forceMoveTo = false,
            )
            close()
        }
        drawPath(
            wedge,
            Brush.radialGradient(
                colors = listOf(HaloCyan.copy(alpha = 0.22f), Color.Transparent),
                center = center,
                radius = radius,
            ),
        )
        drawLine(
            color = HaloCyan.copy(alpha = 0.5f),
            start = center,
            end = Offset(
                center.x + (cos(angle) * radius).toFloat(),
                center.y + (sin(angle) * radius).toFloat(),
            ),
            strokeWidth = 1.2.dp.toPx(),
        )

        blips.forEach { point ->
            val px = center.x + point.x * radius * 0.92f
            val py = center.y + point.y * radius * 0.92f
            drawCircle(
                color = HaloCyan.copy(alpha = 0.65f),
                radius = 2.1.dp.toPx(),
                center = Offset(px, py),
            )
        }
        activeCode?.let { code ->
            CountryGeo.centroid(code)?.let { geo ->
                val latRad = Math.toRadians(geo.lat.toDouble())
                val lonRad = Math.toRadians(geo.lon.toDouble())
                val px = center.x + (cos(latRad) * sin(lonRad)).toFloat() * radius * 0.92f
                val py = center.y + (-sin(latRad)).toFloat() * radius * 0.92f
                drawCircle(
                    color = ConnectedGreen.copy(alpha = 0.22f),
                    radius = 8.dp.toPx(),
                    center = Offset(px, py),
                )
                drawCircle(
                    color = ConnectedGreen,
                    radius = 3.dp.toPx(),
                    center = Offset(px, py),
                )
            }
        }
    }
}

private fun Float.round2(): Int = (this * 100f).toInt()
