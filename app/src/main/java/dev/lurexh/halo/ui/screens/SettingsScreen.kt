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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.lurexh.halo.ui.components.ActionRow
import dev.lurexh.halo.ui.components.EngineOption
import dev.lurexh.halo.ui.components.GlassPanel
import dev.lurexh.halo.ui.components.HaloTopBar
import dev.lurexh.halo.ui.components.SectionLabel
import dev.lurexh.halo.ui.components.SwitchRow
import dev.lurexh.halo.ui.components.ThinDivider
import dev.lurexh.halo.ui.theme.HaloCyan
import dev.lurexh.halo.ui.theme.HaloPink
import dev.lurexh.halo.ui.theme.HaloTokens
import dev.lurexh.halo.ui.theme.Proton
import dev.lurexh.halo.ui.theme.TextMuted
import dev.lurexh.halo.ui.theme.TextPrimary
import dev.lurexh.halo.ui.theme.haloStatusColor
import dev.lurexh.halo.ui.theme.haloStatusLabel
import dev.lurexh.halo.vpn.CatalogRepository
import dev.lurexh.halo.vpn.Server
import dev.lurexh.halo.vpn.VpnState

@Composable
fun SettingsScreen(
    state: VpnState,
    servers: List<Server>,
    catalogUpdatedAt: Long,
    refreshing: Boolean,
    adblock: Boolean,
    autoconnect: Boolean,
    engine: String,
    onRefresh: () -> Unit,
    onAdblock: (Boolean) -> Unit,
    onAutoconnect: (Boolean) -> Unit,
    onEngine: (String) -> Unit,
) {
    var showLogs by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = HaloTokens.ScreenPad),
    ) {
        Spacer(Modifier.height(18.dp))
        HaloTopBar(
            title = "Settings",
            subtitle = "No account. No tracking.",
            refreshing = refreshing,
            onRefresh = onRefresh,
        )
        Spacer(Modifier.height(16.dp))

        SectionLabel("ENGINE")
        GlassPanel(Modifier.fillMaxWidth(), accent = HaloCyan) {
            EngineOption(
                title = "Smart",
                subtitle = "WARP first, country relays when pinned",
                selected = engine == "smart",
                onClick = { onEngine("smart") },
            )
            ThinDivider()
            EngineOption(
                title = "WARP",
                subtitle = "Cloudflare edge, no country pinning",
                selected = engine == "warp",
                onClick = { onEngine("warp") },
            )
            ThinDivider()
            EngineOption(
                title = "OpenVPN",
                subtitle = "Country relays from the live catalog",
                selected = engine == "openvpn",
                onClick = { onEngine("openvpn") },
            )
        }
        Spacer(Modifier.height(16.dp))

        SectionLabel("PROTECTION")
        GlassPanel(Modifier.fillMaxWidth(), accent = HaloPink) {
            SwitchRow(
                title = "DNS adblock",
                subtitle = "Filters ad and tracker domains",
                checked = adblock,
                onCheckedChange = onAdblock,
            )
            ThinDivider()
            SwitchRow(
                title = "Auto-connect",
                subtitle = "Reconnect after reboot",
                checked = autoconnect,
                onCheckedChange = onAutoconnect,
            )
        }
        Spacer(Modifier.height(16.dp))

        SectionLabel("SERVER CATALOG")
        GlassPanel(Modifier.fillMaxWidth(), accent = Proton) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "${servers.size} public relays",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Updated ${CatalogRepository.formatUpdated(catalogUpdatedAt)} \u00b7 every 12h",
                        color = TextMuted,
                        fontSize = 12.sp,
                    )
                }
                IconButton(onClick = onRefresh, enabled = !refreshing) {
                    if (refreshing) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh catalog")
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "New relays are added automatically. If a country has no relay, Halo says so instead of pretending.",
                color = TextMuted,
                fontSize = 12.sp,
                lineHeight = 17.sp,
            )
        }
        Spacer(Modifier.height(16.dp))

        SectionLabel("DIAGNOSTICS")
        GlassPanel(Modifier.fillMaxWidth(), accent = HaloCyan) {
            ActionRow(
                icon = Icons.Rounded.Description,
                title = "Connection logs",
                subtitle = "Core + OpenVPN output",
                onClick = { showLogs = true },
            )
        }
        Spacer(Modifier.height(16.dp))

        SectionLabel("ABOUT")
        GlassPanel(Modifier.fillMaxWidth(), accent = Proton) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Proton, HaloPink))),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Shield, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Halo 1.0.0", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Text("GPLv2 \u00b7 ics-openvpn + boringtun", color = TextMuted, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Free public relays are operated by volunteers. They can see your source IP, destinations, timing and volume. Encryption hides content, not metadata. Use self-hosted or audited paid servers when privacy is critical.",
                color = TextMuted,
                fontSize = 12.sp,
                lineHeight = 18.sp,
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = "${haloStatusLabel(state.status)} \u00b7 ${state.protocol}",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = haloStatusColor(state.status),
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(26.dp))
    }

    if (showLogs) {
        LogsDialog(onDismiss = { showLogs = false })
    }
}
