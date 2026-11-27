package dev.lurexh.halo

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import dev.lurexh.halo.core.Native
import dev.lurexh.halo.ui.HaloRoot
import dev.lurexh.halo.ui.theme.HaloTheme
import dev.lurexh.halo.vpn.CatalogRepository
import dev.lurexh.halo.vpn.HaloVpnService
import dev.lurexh.halo.vpn.OpenVpnEngine
import dev.lurexh.halo.vpn.ProfileStore
import dev.lurexh.halo.vpn.Server
import dev.lurexh.halo.vpn.VpnController
import dev.lurexh.halo.vpn.VpnStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class MainActivity : ComponentActivity() {

    private lateinit var store: ProfileStore

    private var servers by mutableStateOf<List<Server>>(emptyList())
    private var catalogUpdatedAt by mutableStateOf(0L)
    private var selectedServer by mutableStateOf<Server?>(null)
    private var refreshing by mutableStateOf(false)
    private var adblock by mutableStateOf(true)
    private var autoconnect by mutableStateOf(false)
    private var engine by mutableStateOf("smart")
    private var busy by mutableStateOf(false)
    private var pendingAction: (() -> Unit)? = null

    private val vpnPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val action = pendingAction
        pendingAction = null
        if (result.resultCode == RESULT_OK) {
            action?.invoke()
        } else {
            VpnController.update { it.copy(message = "VPN permission was not granted") }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = ProfileStore(this)
        adblock = store.adblockEnabled()
        autoconnect = store.autoconnect()
        engine = store.engine()
        loadCatalog(refresh = false)

        setContent {
            HaloTheme {
                val state = VpnController.state.collectAsState().value

                LaunchedEffect(state.status, state.engine) {
                    if (state.status != VpnStatus.CONNECTED || state.engine != "warp") return@LaunchedEffect
                    while (true) {
                        val parsed = runCatching { JSONObject(Native.statsJson()) }.getOrNull()
                        if (parsed != null) {
                            VpnController.update {
                                it.copy(
                                    rxBytes = parsed.optLong("rx"),
                                    txBytes = parsed.optLong("tx"),
                                    blocked = parsed.optLong("blocked"),
                                )
                            }
                        }
                        delay(1000)
                    }
                }

                HaloRoot(
                    state = state,
                    servers = servers,
                    catalogUpdatedAt = catalogUpdatedAt,
                    refreshing = refreshing,
                    selectedServer = selectedServer,
                    adblock = adblock,
                    autoconnect = autoconnect,
                    engine = engine,
                    busy = busy,
                    onToggle = {
                        when (state.status) {
                            VpnStatus.CONNECTED, VpnStatus.CONNECTING -> stopVpn()
                            else -> requestQuickConnect()
                        }
                    },
                    onQuickConnect = { requestQuickConnect() },
                    onConnectWarp = { withVpnPermission { connectWarp() } },
                    onConnectServer = { server -> withVpnPermission { connectServer(server) } },
                    onConnectCountry = { code -> requestCountry(code) },
                    onCountryTap = { code -> requestCountry(code) },
                    onRefresh = { loadCatalog(refresh = true) },
                    onAdblock = { enabled ->
                        adblock = enabled
                        store.setAdblock(enabled)
                        if (state.status == VpnStatus.CONNECTED) {
                            VpnController.update { it.copy(message = "Reconnect to apply DNS filtering") }
                        }
                    },
                    onAutoconnect = { enabled ->
                        autoconnect = enabled
                        store.setAutoconnect(enabled)
                    },
                    onEngine = { value ->
                        engine = value
                        store.setEngine(value)
                        if (state.status == VpnStatus.CONNECTED) {
                            VpnController.update { it.copy(message = "Reconnect to switch engine") }
                        }
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val catalog = withContext(Dispatchers.IO) { CatalogRepository.load(this@MainActivity) }
            if (catalog.servers.isNotEmpty()) {
                servers = catalog.servers
                catalogUpdatedAt = catalog.updatedAt
            }
            selectedServer = withContext(Dispatchers.IO) { CatalogRepository.lastServer(this@MainActivity) }
        }
    }

    private fun loadCatalog(refresh: Boolean) {
        lifecycleScope.launch {
            refreshing = true
            val result = withContext(Dispatchers.IO) {
                if (refresh) {
                    CatalogRepository.refresh(this@MainActivity)
                } else {
                    CatalogRepository.refreshIfStale(this@MainActivity)
                }
            }
            val catalog = result.getOrNull()
            if (catalog != null) {
                servers = catalog.servers
                catalogUpdatedAt = catalog.updatedAt
            } else {
                val cached = withContext(Dispatchers.IO) { CatalogRepository.load(this@MainActivity) }
                servers = cached.servers
                catalogUpdatedAt = cached.updatedAt
                if (servers.isEmpty()) {
                    VpnController.update {
                        it.copy(message = "Server catalog unavailable: ${result.exceptionOrNull()?.message}")
                    }
                }
            }
            selectedServer = withContext(Dispatchers.IO) { CatalogRepository.lastServer(this@MainActivity) }
            refreshing = false
        }
    }

    private fun requestQuickConnect() {
        if (busy) return
        withVpnPermission {
            when (store.engine()) {
                "openvpn" -> {
                    val best = CatalogRepository.fastest(servers)
                    if (best != null) connectServer(best) else connectWarp()
                }
                else -> connectWarp()
            }
        }
    }

    private fun requestCountry(code: String) {
        val best = CatalogRepository.fastest(servers, code)
        if (best == null) {
            VpnController.update { it.copy(message = "No public relay in that country yet") }
            return
        }
        withVpnPermission { connectServer(best) }
    }

    private fun withVpnPermission(action: () -> Unit) {
        val prepare = VpnService.prepare(this)
        if (prepare != null) {
            pendingAction = action
            vpnPermission.launch(prepare)
        } else {
            action()
        }
    }

    private fun connectServer(server: Server) {
        selectedServer = server
        CatalogRepository.setLastServer(this, server)
        stopService(Intent(this, HaloVpnService::class.java))
        OpenVpnEngine.connect(this, server, adblock)
    }

    private fun connectWarp() {
        lifecycleScope.launch {
            busy = true
            OpenVpnEngine.disconnect(this@MainActivity)
            stopService(Intent(this@MainActivity, HaloVpnService::class.java))
            VpnController.update {
                it.copy(
                    status = VpnStatus.CONNECTING,
                    engine = "warp",
                    protocol = "WARP",
                    profileName = "WARP Auto",
                    exitIp = "Cloudflare edge",
                    exitCountry = "",
                    exitCountryName = "Cloudflare",
                    message = "Registering WARP\u2026",
                )
            }
            val result = withContext(Dispatchers.IO) { store.warp() }
            result.onSuccess { profile ->
                store.setActive(profile.id)
                VpnController.update {
                    it.copy(
                        status = VpnStatus.CONNECTING,
                        engine = "warp",
                        protocol = "WARP",
                        profileName = "WARP Auto",
                        profileId = profile.id,
                        exitIp = "Cloudflare edge",
                        exitCountry = "",
                        exitCountryName = "Cloudflare",
                        message = "Connecting WARP\u2026",
                    )
                }
                startHaloService()
            }.onFailure { error ->
                VpnController.update {
                    it.copy(
                        status = VpnStatus.ERROR,
                        engine = "warp",
                        message = "WARP failed: ${error.message}",
                    )
                }
            }
            busy = false
        }
    }

    private fun startHaloService() {
        runCatching {
            ContextCompat.startForegroundService(
                this,
                Intent(this, HaloVpnService::class.java).setAction(HaloVpnService.ACTION_START),
            )
        }.onFailure { error ->
            VpnController.update {
                it.copy(status = VpnStatus.ERROR, message = error.message ?: "Could not start VPN")
            }
        }
    }

    private fun stopVpn() {
        OpenVpnEngine.disconnect(this)
        runCatching {
            stopService(Intent(this, HaloVpnService::class.java))
        }
        VpnController.update {
            it.copy(status = VpnStatus.DISCONNECTED, message = null)
        }
    }
}
