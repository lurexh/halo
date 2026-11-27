package dev.lurexh.halo.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.lurexh.halo.ui.components.HaloBottomBar
import dev.lurexh.halo.ui.render.HaloBackdrop
import dev.lurexh.halo.ui.screens.CountriesScreen
import dev.lurexh.halo.ui.screens.HomeScreen
import dev.lurexh.halo.ui.screens.SettingsScreen
import dev.lurexh.halo.ui.theme.Void
import dev.lurexh.halo.vpn.Server
import dev.lurexh.halo.vpn.VpnState

@Composable
fun HaloRoot(
    state: VpnState,
    servers: List<Server>,
    catalogUpdatedAt: Long,
    refreshing: Boolean,
    selectedServer: Server?,
    adblock: Boolean,
    autoconnect: Boolean,
    engine: String,
    busy: Boolean,
    onToggle: () -> Unit,
    onQuickConnect: () -> Unit,
    onConnectWarp: () -> Unit,
    onConnectServer: (Server) -> Unit,
    onConnectCountry: (String) -> Unit,
    onCountryTap: (String) -> Unit,
    onRefresh: () -> Unit,
    onAdblock: (Boolean) -> Unit,
    onAutoconnect: (Boolean) -> Unit,
    onEngine: (String) -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(0) }

    Scaffold(
        containerColor = Void,
        bottomBar = { HaloBottomBar(tab) { tab = it } },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            HaloBackdrop(Modifier.fillMaxSize())
            when (tab) {
                0 -> HomeScreen(
                    state = state,
                    servers = servers,
                    selectedServer = selectedServer,
                    refreshing = refreshing,
                    busy = busy,
                    onToggle = onToggle,
                    onQuickConnect = onQuickConnect,
                    onRefresh = onRefresh,
                    onCountryTap = onCountryTap,
                )

                1 -> CountriesScreen(
                    servers = servers,
                    selectedServer = selectedServer,
                    refreshing = refreshing,
                    onRefresh = onRefresh,
                    onQuickConnect = onQuickConnect,
                    onConnectWarp = onConnectWarp,
                    onConnectCountry = onConnectCountry,
                    onConnectServer = onConnectServer,
                )

                else -> SettingsScreen(
                    state = state,
                    servers = servers,
                    catalogUpdatedAt = catalogUpdatedAt,
                    refreshing = refreshing,
                    adblock = adblock,
                    autoconnect = autoconnect,
                    engine = engine,
                    onRefresh = onRefresh,
                    onAdblock = onAdblock,
                    onAutoconnect = onAutoconnect,
                    onEngine = onEngine,
                )
            }
        }
    }
}
