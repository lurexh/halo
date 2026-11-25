package dev.lurexh.halo.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.lurexh.halo.core.Native
import dev.lurexh.halo.ui.theme.ConnectedGreen
import dev.lurexh.halo.ui.theme.DeepSpace
import dev.lurexh.halo.ui.theme.HaloCyan
import dev.lurexh.halo.ui.theme.HaloPink
import dev.lurexh.halo.ui.theme.HaloPurple
import dev.lurexh.halo.ui.theme.TextMuted
import dev.lurexh.halo.ui.theme.TextPrimary
import dev.lurexh.halo.ui.theme.Void
import dev.lurexh.halo.vpn.VpnState
import dev.lurexh.halo.vpn.VpnStatus

@Composable
fun HaloScreen(state: VpnState, onToggle: () -> Unit) {
    var showSheet by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Void, DeepSpace, Void))),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(64.dp))
            Text(
                text = "HALO",
                style = MaterialTheme.typography.headlineMedium,
                letterSpacing = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            )
            Text(
                text = "private · fast · yours",
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted,
            )

            Spacer(Modifier.weight(1f))
            HaloOrb(status = state.status, onClick = onToggle)
            Spacer(Modifier.height(36.dp))
            Text(
                text = statusLabel(state.status),
                style = MaterialTheme.typography.headlineSmall,
                color = when (state.status) {
                    VpnStatus.CONNECTED -> ConnectedGreen
                    VpnStatus.ERROR -> HaloPink
                    else -> TextPrimary
                },
            )
            Text(text = state.profile, color = TextMuted, style = MaterialTheme.typography.bodyMedium)
            state.message?.let { Text(text = it, color = HaloPink, style = MaterialTheme.typography.bodySmall) }

            Spacer(Modifier.weight(1f))
            DetailsPill(onClick = { showSheet = true })
            Spacer(Modifier.height(28.dp))
        }

        if (showSheet) {
            DetailsSheet(state = state, onDismiss = { showSheet = false })
        }
    }
}

@Composable
private fun HaloOrb(status: VpnStatus, onClick: () -> Unit) {
    val active = status == VpnStatus.CONNECTED || status == VpnStatus.CONNECTING
    val transition = rememberInfiniteTransition(label = "halo-orb")
    val pulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    val scale = if (active) pulse else 1f

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(280.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        HaloPurple.copy(alpha = 0.55f),
                        HaloCyan.copy(alpha = 0.16f),
                        Color.Transparent,
                    ),
                ),
            )
            .clickable(onClick = onClick),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(168.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(HaloPurple, HaloPink))),
        ) {
            Icon(
                imageVector = if (active) Icons.Rounded.Bolt else Icons.Rounded.PowerSettingsNew,
                contentDescription = if (active) "Disconnect" else "Connect",
                tint = Color.White,
                modifier = Modifier.size(60.dp),
            )
        }
    }
}

@Composable
private fun DetailsPill(onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.06f))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Icon(Icons.Rounded.Shield, contentDescription = null, tint = HaloCyan, modifier = Modifier.size(18.dp))
        Text(text = "Details & adblock", color = TextPrimary, style = MaterialTheme.typography.labelLarge)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailsSheet(state: VpnState, onDismiss: () -> Unit) {
    var adblock by remember { mutableStateOf(true) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = DeepSpace) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
        ) {
            Text("Connection", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            Spacer(Modifier.height(16.dp))
            StatRow("Protocol", state.profile)
            StatRow("Status", statusLabel(state.status))
            StatRow("Download", humanBytes(state.rxBytes))
            StatRow("Upload", humanBytes(state.txBytes))

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            Spacer(Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            ) {
                Icon(Icons.Rounded.Shield, contentDescription = null, tint = HaloCyan)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("DNS adblock", color = TextPrimary)
                    Text(
                        "Block ads & trackers inside the tunnel",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                    )
                }
                Switch(checked = adblock, onCheckedChange = { adblock = it })
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = "Rust core · boringtun · ${Native.version()}",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
            )
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = TextMuted, style = MaterialTheme.typography.bodyMedium)
        Text(value, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun statusLabel(status: VpnStatus): String = when (status) {
    VpnStatus.DISCONNECTED -> "Disconnected"
    VpnStatus.CONNECTING -> "Connecting…"
    VpnStatus.CONNECTED -> "Protected"
    VpnStatus.ERROR -> "Error"
}

private fun humanBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = listOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var i = 0
    while (value >= 1024 && i < units.lastIndex) {
        value /= 1024
        i++
    }
    return String.format("%.1f %s", value, units[i])
}
