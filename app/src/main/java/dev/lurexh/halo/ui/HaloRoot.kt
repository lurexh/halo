package dev.lurexh.halo.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.lurexh.halo.core.Native
import dev.lurexh.halo.ui.theme.ConnectedGreen
import dev.lurexh.halo.ui.theme.Danger
import dev.lurexh.halo.ui.theme.HaloBorder
import dev.lurexh.halo.ui.theme.HaloCyan
import dev.lurexh.halo.ui.theme.HaloPink
import dev.lurexh.halo.ui.theme.HaloSurface
import dev.lurexh.halo.ui.theme.Proton
import dev.lurexh.halo.ui.theme.ProtonDeep
import dev.lurexh.halo.ui.theme.TextMuted
import dev.lurexh.halo.ui.theme.TextPrimary
import dev.lurexh.halo.ui.theme.Void
import dev.lurexh.halo.vpn.Profile
import dev.lurexh.halo.vpn.VpnState
import dev.lurexh.halo.vpn.VpnStatus

private val Amber = Color(0xFFFFC857)

@Composable
fun HaloRoot(
    state: VpnState,
    profiles: List<Profile>,
    activeId: String?,
    latencies: Map<String, Int>,
    adblock: Boolean,
    autoconnect: Boolean,
    busy: Boolean,
    initialRules: String,
    onToggle: () -> Unit,
    onQuickConnect: () -> Unit,
    onSelect: (Profile) -> Unit,
    onDelete: (Profile) -> Unit,
    onImportFile: () -> Unit,
    onImportText: (String) -> Unit,
    onAddWarp: () -> Unit,
    onProbe: () -> Unit,
    onAdblock: (Boolean) -> Unit,
    onAutoconnect: (Boolean) -> Unit,
    onSaveRules: (String) -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(0) }
    var showPaste by rememberSaveable { mutableStateOf(false) }
    var showRules by rememberSaveable { mutableStateOf(false) }
    var showLogs by rememberSaveable { mutableStateOf(false) }
    var rules by remember { mutableStateOf(initialRules) }
    val active = profiles.firstOrNull { it.id == activeId }

    Scaffold(
        containerColor = Void,
        bottomBar = { HaloNav(tab) { tab = it } },
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .background(Brush.verticalGradient(listOf(Void, Color(0xFF0B0E18), Void))),
        ) {
            when (tab) {
                0 -> HomeTab(
                    state = state,
                    active = active,
                    latencies = latencies,
                    busy = busy,
                    onToggle = onToggle,
                    onQuickConnect = onQuickConnect,
                    onOpenServers = { tab = 1 },
                )
                1 -> ServersTab(
                    profiles = profiles,
                    activeId = activeId,
                    latencies = latencies,
                    busy = busy,
                    onSelect = onSelect,
                    onDelete = onDelete,
                    onImportFile = onImportFile,
                    onPaste = { showPaste = true },
                    onAddWarp = onAddWarp,
                    onProbe = onProbe,
                )
                else -> SettingsTab(
                    state = state,
                    adblock = adblock,
                    autoconnect = autoconnect,
                    onAdblock = onAdblock,
                    onAutoconnect = onAutoconnect,
                    onEditRules = { showRules = true },
                    onLogs = { showLogs = true },
                    onProbe = onProbe,
                )
            }
        }
    }

    if (showPaste) {
        PasteDialog(onDismiss = { showPaste = false }, onConfirm = { onImportText(it) })
    }
    if (showRules) {
        RulesDialog(
            initial = rules,
            onDismiss = { showRules = false },
            onSave = {
                rules = it
                onSaveRules(it)
            },
        )
    }
    if (showLogs) {
        LogsDialog(onDismiss = { showLogs = false })
    }
}

@Composable
private fun HaloNav(tab: Int, onTab: (Int) -> Unit) {
    NavigationBar(containerColor = HaloSurface, tonalElevation = 0.dp) {
        NavigationBarItem(
            selected = tab == 0,
            onClick = { onTab(0) },
            icon = { Icon(Icons.Rounded.Home, contentDescription = null) },
            label = { Text("Home") },
        )
        NavigationBarItem(
            selected = tab == 1,
            onClick = { onTab(1) },
            icon = { Icon(Icons.Rounded.Public, contentDescription = null) },
            label = { Text("Servers") },
        )
        NavigationBarItem(
            selected = tab == 2,
            onClick = { onTab(2) },
            icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
            label = { Text("Settings") },
        )
    }
}

@Composable
private fun HomeTab(
    state: VpnState,
    active: Profile?,
    latencies: Map<String, Int>,
    busy: Boolean,
    onToggle: () -> Unit,
    onQuickConnect: () -> Unit,
    onOpenServers: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "HALO",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 8.sp,
                    color = TextPrimary,
                )
                Text("private \u00b7 fast \u00b7 yours", style = MaterialTheme.typography.labelMedium, color = TextMuted)
            }
            StatusBadge(state.status)
        }

        Spacer(Modifier.height(22.dp))
        ConnectOrb(state.status, onToggle)
        Spacer(Modifier.height(18.dp))

        Text(
            text = statusLabel(state.status),
            style = MaterialTheme.typography.headlineSmall,
            color = statusColor(state.status),
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = active?.name ?: "No server selected",
            color = TextMuted,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        state.message?.let {
            Spacer(Modifier.height(6.dp))
            Text(
                text = it,
                color = if (state.status == VpnStatus.ERROR) Danger else HaloCyan,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(Modifier.height(18.dp))
        Button(
            onClick = onQuickConnect,
            enabled = !busy,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            if (busy) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
            } else {
                Icon(Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = if (active == null) "Quick connect" else "Quick connect \u00b7 fastest",
                fontWeight = FontWeight.SemiBold,
            )
        }

        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("Download", humanBytes(state.rxBytes), Modifier.weight(1f))
            StatCard("Upload", humanBytes(state.txBytes), Modifier.weight(1f))
            StatCard("Blocked", state.blocked.toString(), Modifier.weight(1f))
        }

        Spacer(Modifier.height(18.dp))
        SectionHeader("Active server", if (active != null) "Change" else "Browse", onOpenServers)
        if (active == null) {
            EmptyCard("No server yet", "Import a WireGuard config or add WARP to get started.")
        } else {
            ServerCard(active, latencies[active.id], selected = true, onClick = onOpenServers)
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun ConnectOrb(status: VpnStatus, onClick: () -> Unit) {
    val active = status == VpnStatus.CONNECTED || status == VpnStatus.CONNECTING
    val transition = rememberInfiniteTransition(label = "halo-orb")
    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    val scale = if (active) pulse else 1f
    val ring = statusColor(status)

    Box(Modifier.size(250.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(ring.copy(alpha = 0.32f), Color.Transparent))),
        )
        Box(
            modifier = Modifier
                .size(210.dp)
                .clip(CircleShape)
                .border(1.dp, ring.copy(alpha = 0.45f), CircleShape),
        )
        Box(
            modifier = Modifier
                .size(176.dp)
                .clip(CircleShape)
                .border(1.dp, ring.copy(alpha = 0.22f), CircleShape),
        )
        Box(
            modifier = Modifier
                .size(142.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(if (active) ring else Proton, ProtonDeep)))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = if (active) Icons.Rounded.Bolt else Icons.Rounded.PowerSettingsNew,
                    contentDescription = if (active) "Disconnect" else "Connect",
                    tint = Color.White,
                    modifier = Modifier.size(44.dp),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (active) "TAP TO STOP" else "TAP TO START",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.sp,
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(status: VpnStatus) {
    val (text, color) = when (status) {
        VpnStatus.CONNECTED -> "PROTECTED" to ConnectedGreen
        VpnStatus.CONNECTING -> "CONNECTING" to Amber
        VpnStatus.ERROR -> "ERROR" to Danger
        else -> "OFFLINE" to TextMuted
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(6.dp))
        Text(text, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(HaloSurface)
            .border(1.dp, HaloBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Text(
            text = value,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
        )
        Spacer(Modifier.height(2.dp))
        Text(label, color = TextMuted, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun SectionHeader(title: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        if (action != null) {
            Text(
                text = action,
                color = Proton,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.clickable(onClick = onAction),
            )
        }
    }
}

@Composable
private fun ServersTab(
    profiles: List<Profile>,
    activeId: String?,
    latencies: Map<String, Int>,
    busy: Boolean,
    onSelect: (Profile) -> Unit,
    onDelete: (Profile) -> Unit,
    onImportFile: () -> Unit,
    onPaste: () -> Unit,
    onAddWarp: () -> Unit,
    onProbe: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp),
    ) {
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Servers",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onProbe, enabled = !busy) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Test latency", tint = if (busy) TextMuted else HaloCyan)
            }
        }
        Text(
            text = "${profiles.size} configured \u00b7 tap to connect",
            color = TextMuted,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = onImportFile, enabled = !busy, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("File")
            }
            FilledTonalButton(onClick = onPaste, enabled = !busy, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Paste")
            }
            FilledTonalButton(onClick = onAddWarp, enabled = !busy, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.Cloud, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("WARP")
            }
        }
        Spacer(Modifier.height(14.dp))
        if (profiles.isEmpty()) {
            EmptyCard("Nothing here yet", "Import a .conf, paste a WireGuard config, or add a free WARP tunnel.")
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(profiles, key = { it.id }) { profile ->
                    ServerRow(
                        profile = profile,
                        latency = latencies[profile.id],
                        selected = profile.id == activeId,
                        onSelect = onSelect,
                        onDelete = onDelete,
                    )
                }
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Public relays are unverified and can be slow or offline. Configs you import stay on this device.",
                        color = TextMuted,
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun ServerRow(
    profile: Profile,
    latency: Int?,
    selected: Boolean,
    onSelect: (Profile) -> Unit,
    onDelete: (Profile) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Proton.copy(alpha = 0.16f) else HaloSurface)
            .border(1.dp, if (selected) Proton.copy(alpha = 0.5f) else HaloBorder, RoundedCornerShape(16.dp))
            .clickable { onSelect(profile) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Proton.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Public, contentDescription = null, tint = HaloCyan, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = profile.name,
                color = TextPrimary,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                SourceBadge(profile.source)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = profile.endpoint,
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        LatencyChip(latency)
        if (selected) {
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = ConnectedGreen, modifier = Modifier.size(18.dp))
        }
        IconButton(onClick = { onDelete(profile) }) {
            Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ServerCard(profile: Profile, latency: Int?, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Proton.copy(alpha = 0.16f) else HaloSurface)
            .border(1.dp, if (selected) Proton.copy(alpha = 0.5f) else HaloBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Proton.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Public, contentDescription = null, tint = HaloCyan, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = profile.name,
                color = TextPrimary,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = profile.endpoint,
                color = TextMuted,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        LatencyChip(latency)
    }
}

@Composable
private fun SourceBadge(source: String) {
    val label = when (source) {
        "warp" -> "WARP"
        "import" -> "IMPORTED"
        else -> source.uppercase()
    }
    Text(
        text = label,
        color = Proton,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Proton.copy(alpha = 0.14f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun LatencyChip(latency: Int?) {
    val (text, color) = when {
        latency == null -> "\u2014" to TextMuted
        latency < 0 -> "fail" to Danger
        latency < 120 -> "${latency}ms" to ConnectedGreen
        latency < 250 -> "${latency}ms" to Amber
        else -> "${latency}ms" to HaloPink
    }
    Text(text, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
}

@Composable
private fun SettingsTab(
    state: VpnState,
    adblock: Boolean,
    autoconnect: Boolean,
    onAdblock: (Boolean) -> Unit,
    onAutoconnect: (Boolean) -> Unit,
    onEditRules: () -> Unit,
    onLogs: () -> Unit,
    onProbe: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp),
    ) {
        Spacer(Modifier.height(18.dp))
        Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(Modifier.height(16.dp))
        SettingRow(
            icon = Icons.Rounded.Shield,
            title = "DNS adblock",
            subtitle = "Sinkhole ads & trackers inside the tunnel",
            checked = adblock,
            onChange = onAdblock,
        )
        Spacer(Modifier.height(10.dp))
        SettingRow(
            icon = Icons.Rounded.Bolt,
            title = "Auto-connect",
            subtitle = "Reconnect when the device boots",
            checked = autoconnect,
            onChange = onAutoconnect,
        )
        Spacer(Modifier.height(10.dp))
        ActionRow(
            icon = Icons.Rounded.Shield,
            title = "Custom block rules",
            subtitle = "hosts, ||domain^, @@allow, # comments",
            onClick = onEditRules,
        )
        Spacer(Modifier.height(10.dp))
        ActionRow(
            icon = Icons.Rounded.Speed,
            title = "Re-test all servers",
            subtitle = "Measure TCP handshake latency",
            onClick = onProbe,
        )
        Spacer(Modifier.height(10.dp))
        ActionRow(
            icon = Icons.Rounded.List,
            title = "Core logs",
            subtitle = "Recent tunnel events",
            onClick = onLogs,
        )

        Spacer(Modifier.height(18.dp))
        Text("About", color = TextMuted, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(HaloSurface)
                .border(1.dp, HaloBorder, RoundedCornerShape(16.dp))
                .padding(16.dp),
        ) {
            InfoLine("Version", Native.version())
            InfoLine("Blocked requests", state.blocked.toString())
            InfoLine("Core", if (Native.available) "loaded" else "unavailable")
            InfoLine("Engine", "Rust \u00b7 boringtun \u00b7 WireGuard")
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Halo is free software. No accounts, no telemetry. Imported configs never leave your device.",
                color = TextMuted,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(HaloSurface)
            .border(1.dp, HaloBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = HaloCyan, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.Medium)
            Text(subtitle, color = TextMuted, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ActionRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(HaloSurface)
            .border(1.dp, HaloBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = HaloCyan, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.Medium)
            Text(subtitle, color = TextMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = TextMuted, style = MaterialTheme.typography.bodySmall)
        Text(value, color = TextPrimary, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun EmptyCard(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(HaloSurface)
            .border(1.dp, HaloBorder, RoundedCornerShape(16.dp))
            .padding(18.dp),
    ) {
        Text(title, color = TextPrimary, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Text(body, color = TextMuted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun PasteDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Paste WireGuard config") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                label = { Text("Config") },
                placeholder = { Text("[Interface]\nPrivateKey = ...") },
            )
        },
        confirmButton = {
            TextButton(onClick = {
                if (text.isNotBlank()) {
                    onConfirm(text)
                    onDismiss()
                }
            }) { Text("Import") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun RulesDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom block rules") },
        text = {
            Column {
                Text(
                    text = "One rule per line. Supports hosts format, ||domain^, @@allow, and # comments.",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    label = { Text("Rules") },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(text)
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun LogsDialog(onDismiss: () -> Unit) {
    val logs = remember { Native.logs().ifBlank { "No log entries yet." } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Core logs") },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Void)
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp),
            ) {
                Text(
                    text = logs,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

private fun statusLabel(status: VpnStatus): String = when (status) {
    VpnStatus.DISCONNECTED -> "Disconnected"
    VpnStatus.CONNECTING -> "Connecting\u2026"
    VpnStatus.CONNECTED -> "Protected"
    VpnStatus.ERROR -> "Error"
}

private fun statusColor(status: VpnStatus): Color = when (status) {
    VpnStatus.CONNECTED -> ConnectedGreen
    VpnStatus.CONNECTING -> Amber
    VpnStatus.ERROR -> Danger
    else -> Proton
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
