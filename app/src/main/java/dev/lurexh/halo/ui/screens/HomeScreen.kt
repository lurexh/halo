package dev.lurexh.halo.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.lurexh.halo.ui.components.GlassPanel
import dev.lurexh.halo.ui.components.GradientButton
import dev.lurexh.halo.ui.components.HaloTopBar
import dev.lurexh.halo.ui.components.MetricCard
import dev.lurexh.halo.ui.components.StatusChip
import dev.lurexh.halo.ui.components.ThroughputGraph
import dev.lurexh.halo.ui.render.HaloGlobe
import dev.lurexh.halo.ui.render.ReactorOrb
import dev.lurexh.halo.ui.theme.ConnectedGreen
import dev.lurexh.halo.ui.theme.Danger
import dev.lurexh.halo.ui.theme.HaloCyan
import dev.lurexh.halo.ui.theme.HaloTokens
import dev.lurexh.halo.ui.theme.TextMuted
import dev.lurexh.halo.ui.theme.TextPrimary
import dev.lurexh.halo.ui.theme.haloStatusColor
import dev.lurexh.halo.ui.theme.haloStatusLabel
import dev.lurexh.halo.vpn.Countries
import dev.lurexh.halo.vpn.Server
import dev.lurexh.halo.vpn.VpnState
import dev.lurexh.halo.vpn.VpnStatus
import java.util.Locale

@Composable
fun HomeScreen(
    state: VpnState,
    servers: List<Server>,
    selectedServer: Server?,
    refreshing: Boolean,
    busy: Boolean,
    onToggle: () -> Unit,
    onQuickConnect: () -> Unit,
    onRefresh: () -> Unit,
    onCountryTap: (String) -> Unit,
) {
    val activeCode = when {
        state.status == VpnStatus.CONNECTED && state.engine == "openvpn" ->
            selectedServer?.countryCode?.takeIf { it.length == 2 } ?: state.exitCountry.takeIf { it.length == 2 }
        state.status == VpnStatus.CONNECTED && state.exitCountry.length == 2 -> state.exitCountry
        else -> null
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = HaloTokens.ScreenPad),
    ) {
        Spacer(Modifier.height(18.dp))
        HaloTopBar(
            title = "HALO",
            subtitle = "Private by default",
            refreshing = refreshing,
            onRefresh = onRefresh,
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusChip(state.status)
            Spacer(Modifier.weight(1f))
            Text("${servers.size} relays", color = TextMuted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(4.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .height(356.dp),
            contentAlignment = Alignment.Center,
        ) {
            HaloGlobe(
                status = state.status,
                servers = servers,
                activeCode = activeCode,
                onCountryTap = onCountryTap,
                modifier = Modifier.fillMaxSize(),
            )
            ReactorOrb(status = state.status, onClick = onToggle)
        }

        Text(
            text = haloStatusLabel(state.status),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = haloStatusColor(state.status),
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            letterSpacing = 0.4.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = when {
                state.status == VpnStatus.CONNECTED -> state.profileName
                state.status == VpnStatus.CONNECTING -> "Negotiating a secure route\u2026"
                else -> "Tap the core to protect this device"
            },
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = TextMuted,
            fontSize = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        state.message?.let { message ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = message,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = if (state.status == VpnStatus.ERROR) Danger else Color(0xFFFFC857),
                fontSize = 12.sp,
            )
        }
        Spacer(Modifier.height(16.dp))

        GradientButton(
            text = if (busy) "Preparing\u2026" else "Smart connect",
            icon = Icons.Rounded.Bolt,
            enabled = !busy,
            onClick = onQuickConnect,
        )
        Spacer(Modifier.height(14.dp))

        GlassPanel(Modifier.fillMaxWidth(), accent = HaloCyan) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("LIVE THROUGHPUT", color = TextMuted, fontSize = 9.sp, letterSpacing = 1.4.sp)
                Spacer(Modifier.weight(1f))
                Text("${state.blocked} blocked", color = ConnectedGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            ThroughputGraph(
                rx = state.rxBytes,
                tx = state.txBytes,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
            )
        }
        Spacer(Modifier.height(12.dp))

        ExitCard(state)
        Spacer(Modifier.height(12.dp))
        StatsRow(state)
        Spacer(Modifier.height(12.dp))
        ActiveRouteCard(state, selectedServer)
        Spacer(Modifier.height(26.dp))
    }
}

@Composable
private fun ExitCard(state: VpnState) {
    val connected = state.status == VpnStatus.CONNECTED
    GlassPanel(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("EXIT IP", color = TextMuted, fontSize = 10.sp, letterSpacing = 1.4.sp)
                Spacer(Modifier.height(5.dp))
                Text(
                    text = if (connected) state.exitIp.ifBlank { "\u2014" } else "Not connected",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("LOCATION", color = TextMuted, fontSize = 10.sp, letterSpacing = 1.4.sp)
                Spacer(Modifier.height(5.dp))
                Text(
                    text = if (connected) {
                        state.exitCountryName.ifBlank { state.exitCountry.ifBlank { "\u2014" } }
                    } else {
                        "\u2014"
                    },
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Catalog-reported exit. Public relays can observe metadata.",
            color = TextMuted,
            fontSize = 10.sp,
        )
    }
}

@Composable
private fun StatsRow(state: VpnState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
    ) {
        MetricCard(
            label = "DOWN",
            value = humanBytes(state.rxBytes),
            icon = Icons.Rounded.Download,
            modifier = Modifier.weight(1f),
            accent = HaloCyan,
        )
        MetricCard(
            label = "UP",
            value = humanBytes(state.txBytes),
            icon = Icons.Rounded.Upload,
            modifier = Modifier.weight(1f),
            accent = dev.lurexh.halo.ui.theme.HaloPink,
        )
        MetricCard(
            label = "BLOCKED",
            value = state.blocked.toString(),
            icon = Icons.Rounded.Shield,
            modifier = Modifier.weight(1f),
            accent = ConnectedGreen,
        )
    }
}

@Composable
private fun ActiveRouteCard(state: VpnState, selectedServer: Server?) {
    GlassPanel(Modifier.fillMaxWidth(), accent = ConnectedGreen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(HaloCyan.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.Icon(
                    Icons.Rounded.Public,
                    contentDescription = null,
                    tint = HaloCyan,
                    modifier = Modifier.size(19.dp),
                )
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text("ACTIVE ROUTE", color = TextMuted, fontSize = 9.sp, letterSpacing = 1.2.sp)
                Spacer(Modifier.height(3.dp))
                Text(
                    text = if (state.status == VpnStatus.CONNECTED) state.profileName else "Smart connect ready",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(state.protocol, color = HaloCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        if (selectedServer != null && state.engine == "openvpn") {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "${Countries.flag(selectedServer.countryCode)} ${selectedServer.label} \u00b7 ${selectedServer.pingLabel} \u00b7 ${selectedServer.speedLabel}",
                color = TextMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun humanBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var index = 0
    while (value >= 1024.0 && index < units.lastIndex) {
        value /= 1024.0
        index++
    }
    return String.format(Locale.US, if (index == 0) "%.0f %s" else "%.1f %s", value, units[index])
}
