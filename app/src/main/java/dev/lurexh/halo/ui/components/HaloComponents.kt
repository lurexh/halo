package dev.lurexh.halo.ui.components

import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.lurexh.halo.ui.theme.HaloBorder
import dev.lurexh.halo.ui.theme.HaloCyan
import dev.lurexh.halo.ui.theme.HaloSurface
import dev.lurexh.halo.ui.theme.HaloSurfaceHigh
import dev.lurexh.halo.ui.theme.HaloTokens
import dev.lurexh.halo.ui.theme.Proton
import dev.lurexh.halo.ui.theme.ProtonDeep
import dev.lurexh.halo.ui.theme.TextMuted
import dev.lurexh.halo.ui.theme.TextPrimary
import dev.lurexh.halo.ui.theme.haloStatusColor
import dev.lurexh.halo.ui.theme.haloStatusLabel
import dev.lurexh.halo.vpn.VpnStatus
import kotlinx.coroutines.delay
import kotlin.math.max

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    accent: Color = HaloCyan,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(HaloTokens.CardRadius)
    Box(modifier) {
        Column(
            modifier = Modifier
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            HaloSurface.copy(alpha = 0.82f),
                            HaloSurfaceHigh.copy(alpha = 0.62f),
                        ),
                    ),
                )
                .border(1.dp, HaloBorder.copy(alpha = 0.9f), shape)
                .padding(HaloTokens.CardPad),
            content = content,
        )
        if (Build.VERSION.SDK_INT >= 31) {
            Box(
                Modifier
                    .matchParentSize()
                    .clip(shape)
                    .blur(18.dp)
                    .background(Color.Transparent),
            )
        }
        Box(
            Modifier
                .matchParentSize()
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(accent.copy(alpha = 0.07f), Color.Transparent),
                    ),
                ),
        )
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        color = TextMuted,
        fontSize = HaloTokens.Label,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.7.sp,
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp),
    )
}

@Composable
fun ThinDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp)
            .height(1.dp)
            .background(HaloBorder.copy(alpha = 0.6f)),
    )
}

@Composable
fun StatusChip(status: VpnStatus) {
    val color = haloStatusColor(status)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(HaloTokens.PillRadius))
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.30f), RoundedCornerShape(HaloTokens.PillRadius))
            .padding(horizontal = 11.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = haloStatusLabel(status).uppercase(),
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.1.sp,
        )
    }
}

@Composable
fun GradientButton(
    text: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val shimmer by rememberInfiniteTransition().animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
    )
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(enabled = enabled, onClick = onClick)
            .heightIn(min = 52.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Proton, ProtonDeep, HaloCyan),
                ),
                cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx()),
            )
            val x = size.width * shimmer
            drawLine(
                color = Color.White.copy(alpha = 0.20f),
                start = Offset(x, 0f),
                end = Offset(x + 96.dp.toPx(), size.height),
                strokeWidth = 34.dp.toPx(),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(9.dp))
            Text(text, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}

@Composable
fun MetricCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    accent: Color = HaloCyan,
) {
    GlassPanel(modifier = modifier, accent = accent) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, color = TextMuted, fontSize = 9.sp, letterSpacing = 1.sp)
        }
        Spacer(Modifier.height(7.dp))
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun ThroughputGraph(
    rx: Long,
    tx: Long,
    modifier: Modifier = Modifier,
) {
    val rxHistory = remember { mutableStateListOf<Float>() }
    val txHistory = remember { mutableStateListOf<Float>() }
    var lastRx by remember { mutableStateOf(rx) }
    var lastTx by remember { mutableStateOf(tx) }

    LaunchedEffect(rx, tx) {
        val deltaRx = (rx - lastRx).coerceAtLeast(0L).toFloat()
        val deltaTx = (tx - lastTx).coerceAtLeast(0L).toFloat()
        lastRx = rx
        lastTx = tx
        rxHistory.add(deltaRx)
        txHistory.add(deltaTx)
        if (rxHistory.size > 48) {
            rxHistory.removeAt(0)
            txHistory.removeAt(0)
        }
        delay(16)
    }

    Canvas(modifier) {
        val count = max(rxHistory.size, 2)
        val peak = max(1f, (rxHistory + txHistory).maxOrNull() ?: 1f)
        val w = size.width
        val h = size.height

        for (i in 0..3) {
            val y = h * i / 3f
            drawLine(
                color = HaloBorder.copy(alpha = 0.55f),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 0.7.dp.toPx(),
            )
        }

        fun drawSeries(values: List<Float>, color: Color) {
            if (values.size < 2) return
            val points = values.mapIndexed { index, value ->
                Offset(
                    x = w * index / (count - 1).toFloat(),
                    y = h - (value / peak) * h * 0.86f - h * 0.07f,
                )
            }
            val fill = Path().apply {
                moveTo(points.first().x, h)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, h)
                close()
            }
            drawPath(fill, color.copy(alpha = 0.10f))
            val line = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(line, color.copy(alpha = 0.85f))
        }

        drawSeries(rxHistory, HaloCyan)
        drawSeries(txHistory, dev.lurexh.halo.ui.theme.HaloPink)
    }
}

@Composable
fun EngineOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(19.dp)
                .clip(CircleShape)
                .border(1.5.dp, if (selected) HaloCyan else HaloBorder, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(HaloCyan),
                )
            }
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
            Text(subtitle, color = TextMuted, fontSize = 11.sp)
        }
        if (selected) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = HaloCyan, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.Medium)
            Text(subtitle, color = TextMuted, fontSize = 11.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = HaloCyan, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.Medium)
            Text(subtitle, color = TextMuted, fontSize = 11.sp)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = TextMuted)
    }
}
