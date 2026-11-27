package dev.lurexh.halo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.lurexh.halo.ui.components.GlassPanel
import dev.lurexh.halo.ui.components.HaloTopBar
import dev.lurexh.halo.ui.components.SectionLabel
import dev.lurexh.halo.ui.render.RelayRadar
import dev.lurexh.halo.ui.theme.ConnectedGreen
import dev.lurexh.halo.ui.theme.HaloCyan
import dev.lurexh.halo.ui.theme.HaloTokens
import dev.lurexh.halo.ui.theme.TextMuted
import dev.lurexh.halo.ui.theme.TextPrimary
import dev.lurexh.halo.vpn.CatalogRepository
import dev.lurexh.halo.vpn.Countries
import dev.lurexh.halo.vpn.Country
import dev.lurexh.halo.vpn.Server

@Composable
fun CountriesScreen(
    servers: List<Server>,
    selectedServer: Server?,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    onQuickConnect: () -> Unit,
    onConnectWarp: () -> Unit,
    onConnectCountry: (String) -> Unit,
    onConnectServer: (Server) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var sheet by remember { mutableStateOf<Country?>(null) }
    val grouped = remember(servers) { CatalogRepository.grouped(servers) }
    val countries = remember(query, grouped) {
        Countries.all().filter { country ->
            query.isBlank() ||
                country.name.contains(query, ignoreCase = true) ||
                country.code.contains(query, ignoreCase = true)
        }
    }
    val fastest = remember(servers) { CatalogRepository.fastest(servers) }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = HaloTokens.ScreenPad)) {
            Spacer(Modifier.height(18.dp))
            HaloTopBar(
                title = "Countries",
                subtitle = "Auto-updated every 12 hours",
                refreshing = refreshing,
                onRefresh = onRefresh,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search a country") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            )
            Spacer(Modifier.height(12.dp))

            GlassPanel(
                Modifier
                    .fillMaxWidth()
                    .height(186.dp),
                accent = HaloCyan,
            ) {
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        SectionLabel("RELAY CONSTELLATION")
                        Text(
                            "${servers.size} live exits",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Available countries glow; pinned routes move to the active arc.",
                            color = TextMuted,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                        )
                    }
                    RelayRadar(
                        servers = servers,
                        activeCode = selectedServer?.countryCode,
                        modifier = Modifier
                            .weight(0.9f)
                            .height(146.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            FastestCard(fastest, if (fastest == null) onRefresh else onQuickConnect)
            Spacer(Modifier.height(10.dp))
            WarpCard(onConnectWarp)
            Spacer(Modifier.height(16.dp))
            SectionLabel("ALL COUNTRIES")
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = HaloTokens.ScreenPad),
            contentPadding = PaddingValues(top = 6.dp, bottom = 24.dp),
        ) {
            items(countries, key = { it.code }) { country ->
                val relays = grouped[country.code].orEmpty()
                CountryRow(
                    country = country,
                    relays = relays,
                    selected = selectedServer?.countryCode == country.code,
                    onClick = { if (relays.isNotEmpty()) sheet = country },
                )
            }
        }
    }

    val current = sheet
    if (current != null) {
        CountryDialog(
            country = current,
            servers = grouped[current.code].orEmpty(),
            onDismiss = { sheet = null },
            onFastest = {
                onConnectCountry(current.code)
                sheet = null
            },
            onServer = { server ->
                onConnectServer(server)
                sheet = null
            },
        )
    }
}

@Composable
private fun FastestCard(server: Server?, onClick: () -> Unit) {
    GlassPanel(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        accent = Color(0xFFFFC857),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(Color(0xFFFFC857).copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Bolt, contentDescription = null, tint = Color(0xFFFFC857))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Fastest available", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(3.dp))
                Text(
                    text = server?.let { "${Countries.flag(it.countryCode)} ${it.label} \u00b7 ${it.pingLabel}" }
                        ?: "No relay cached yet \u2014 tap to refresh",
                    color = TextMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
            Text("AUTO", color = Color(0xFFFFC857), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun WarpCard(onClick: () -> Unit) {
    GlassPanel(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        accent = HaloCyan,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(HaloCyan.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Cloud, contentDescription = null, tint = HaloCyan)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("WARP Auto", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(3.dp))
                Text("Cloudflare edge \u00b7 no account", color = TextMuted, fontSize = 12.sp)
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = TextMuted)
        }
    }
}

@Composable
private fun CountryRow(
    country: Country,
    relays: List<Server>,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val available = relays.isNotEmpty()
    val best = relays.firstOrNull()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .background(if (selected) HaloCyan.copy(alpha = 0.10f) else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(enabled = available, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 11.dp)
            .alpha(if (available) 1f else 0.42f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(Countries.flag(country.code), fontSize = 22.sp)
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = country.name,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            Text(
                text = if (available) "${relays.size} relay${if (relays.size == 1) "" else "s"}" else "No public relay",
                color = TextMuted,
                fontSize = 11.sp,
            )
        }
        if (best != null) {
            Column(horizontalAlignment = Alignment.End) {
                Text(best.pingLabel, color = ConnectedGreen, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Text(best.speedLabel, color = TextMuted, fontSize = 10.sp)
            }
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(17.dp))
        }
    }
}
