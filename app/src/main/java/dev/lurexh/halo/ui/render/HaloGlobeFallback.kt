package dev.lurexh.halo.ui.render

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import dev.lurexh.halo.ui.theme.ConnectedGreen
import dev.lurexh.halo.ui.theme.HaloCyan
import dev.lurexh.halo.ui.theme.Proton
import dev.lurexh.halo.vpn.CountryGeo
import dev.lurexh.halo.vpn.Server
import dev.lurexh.halo.vpn.VpnStatus
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun HaloGlobeFallback(
    status: VpnStatus,
    servers: List<Server>,
    activeCode: String?,
    onCountryTap: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(42000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
    )
    val hits = remember { mutableListOf<Triple<String, Float, Float>>() }
    val available = remember(servers) { servers.map { it.countryCode }.toSet() }
    val accent = when (status) {
        VpnStatus.CONNECTED -> ConnectedGreen
        VpnStatus.CONNECTING -> Color(0xFFFFC857)
        VpnStatus.ERROR -> Color(0xFFFF5C7A)
        else -> HaloCyan
    }

    Canvas(
        modifier.pointerInput(available) {
            detectTapGestures { offset ->
                hits.minByOrNull { (_, x, y) ->
                    val dx = x - offset.x
                    val dy = y - offset.y
                    dx * dx + dy * dy
                }?.let { (code, _, _) -> onCountryTap(code) }
            }
        },
    ) {
        val center = this.center
        val radius = size.minDimension * 0.38f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.22f), Color.Transparent),
                center = center,
                radius = radius * 1.8f,
            ),
            radius = radius * 1.8f,
            center = center,
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF1B2450), Color(0xFF070A14)),
                center = Offset(center.x - radius * 0.3f, center.y - radius * 0.34f),
                radius = radius * 1.4f,
            ),
            radius = radius,
            center = center,
        )
        drawCircle(
            color = accent.copy(alpha = 0.28f),
            radius = radius,
            center = center,
            style = Stroke(width = 1.2.dp.toPx()),
        )

        for (index in -2..2) {
            val y = center.y + radius * index / 3f
            val half = sqrt((radius * radius - (y - center.y) * (y - center.y)).coerceAtLeast(0f))
            drawOval(
                color = HaloCyan.copy(alpha = 0.12f),
                topLeft = Offset(center.x - half, y - radius * 0.05f),
                size = androidx.compose.ui.geometry.Size(half * 2f, radius * 0.10f),
                style = Stroke(width = 0.8.dp.toPx()),
            )
        }
        repeat(6) { index ->
            rotate(rotation + index * 30f, center) {
                drawOval(
                    color = HaloCyan.copy(alpha = 0.10f),
                    topLeft = Offset(center.x - radius * 0.42f, center.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 0.84f, radius * 2f),
                    style = Stroke(width = 0.8.dp.toPx()),
                )
            }
        }

        hits.clear()
        CountryGeo.all.forEach { (code, geo) ->
            val lat = Math.toRadians(geo.lat.toDouble())
            val lon = Math.toRadians((geo.lon + rotation).toDouble())
            val x = cos(lat) * sin(lon)
            val z = cos(lat) * cos(lon)
            if (z < -0.12) return@forEach
            val y = -sin(lat)
            val point = Offset(
                center.x + (x * radius * 0.92f).toFloat(),
                center.y + (y * radius * 0.92f).toFloat(),
            )
            val active = code == activeCode
            val availableHere = code in available
            val color = when {
                active -> ConnectedGreen
                availableHere -> accent
                else -> Color(0xFF6B7391)
            }
            val alpha = if (availableHere || active) 0.9f else 0.22f
            drawCircle(color = color.copy(alpha = alpha * 0.20f), radius = 7.dp.toPx(), center = point)
            drawCircle(color = color.copy(alpha = alpha), radius = 2.4.dp.toPx(), center = point)
            if (availableHere) {
                hits.add(Triple(code, point.x, point.y))
            }
        }
    }
}
