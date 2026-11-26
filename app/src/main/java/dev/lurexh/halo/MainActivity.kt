package dev.lurexh.halo

import android.content.Intent
import android.net.Uri
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
import dev.lurexh.halo.vpn.HaloVpnService
import dev.lurexh.halo.vpn.Profile
import dev.lurexh.halo.vpn.ProfileStore
import dev.lurexh.halo.vpn.VpnController
import dev.lurexh.halo.vpn.VpnStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class MainActivity : ComponentActivity() {

    private lateinit var store: ProfileStore

    private var profiles by mutableStateOf<List<Profile>>(emptyList())
    private var activeId by mutableStateOf<String?>(null)
    private var latencies by mutableStateOf<Map<String, Int>>(emptyMap())
    private var adblock by mutableStateOf(true)
    private var autoconnect by mutableStateOf(false)
    private var busy by mutableStateOf(false)
    private var rules by mutableStateOf("")

    private val vpnPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) startVpn()
    }

    private val importFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
            }
            if (text.isNullOrBlank()) {
                VpnController.update { it.copy(message = "Could not read that file") }
            } else {
                handleImport(text)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = ProfileStore(this)
        reload()
        adblock = store.adblockEnabled()
        autoconnect = store.autoconnect()
        rules = store.customRules()

        setContent {
            HaloTheme {
                val state = VpnController.state.collectAsState().value

                LaunchedEffect(state.status) {
                    if (state.status != VpnStatus.CONNECTED) return@LaunchedEffect
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
                    profiles = profiles,
                    activeId = activeId,
                    latencies = latencies,
                    adblock = adblock,
                    autoconnect = autoconnect,
                    busy = busy,
                    initialRules = rules,
                    onToggle = {
                        when (state.status) {
                            VpnStatus.CONNECTED, VpnStatus.CONNECTING -> stopVpn()
                            else -> requestStart()
                        }
                    },
                    onQuickConnect = { quickConnect() },
                    onSelect = { profile -> selectProfile(profile) },
                    onDelete = { profile -> deleteProfile(profile) },
                    onImportFile = { importFile.launch(arrayOf("*/*")) },
                    onImportText = { text -> handleImport(text) },
                    onAddWarp = { addWarp(false) },
                    onProbe = { probeAll() },
                    onAdblock = { enabled ->
                        adblock = enabled
                        store.setAdblock(enabled)
                    },
                    onAutoconnect = { enabled ->
                        autoconnect = enabled
                        store.setAutoconnect(enabled)
                    },
                    onSaveRules = { text ->
                        rules = text
                        store.setCustomRules(text)
                        VpnController.update { it.copy(message = "Rules saved \u2014 reconnect to apply") }
                    },
                )
            }
        }
    }

    private fun reload() {
        profiles = store.list()
        activeId = store.active()?.id
    }

    private fun selectProfile(profile: Profile) {
        store.setActive(profile.id)
        reload()
        connectSelected()
    }

    private fun deleteProfile(profile: Profile) {
        store.remove(profile.id)
        reload()
        VpnController.update { it.copy(message = "Removed ${profile.name}") }
    }

    private fun handleImport(text: String) {
        lifecycleScope.launch {
            busy = true
            val result = withContext(Dispatchers.IO) { store.importText(text, "Imported") }
            result.onSuccess { profile ->
                store.setActive(profile.id)
                reload()
                VpnController.update { it.copy(message = "Imported ${profile.name}") }
            }.onFailure { error ->
                VpnController.update { it.copy(message = error.message ?: "Import failed") }
            }
            busy = false
        }
    }

    private fun addWarp(connect: Boolean) {
        lifecycleScope.launch {
            busy = true
            VpnController.update { it.copy(message = "Registering WARP\u2026") }
            val result = withContext(Dispatchers.IO) { store.warp() }
            result.onSuccess { profile ->
                store.setActive(profile.id)
                reload()
                VpnController.update { it.copy(message = "WARP ready") }
                if (connect) connectSelected()
            }.onFailure { error ->
                VpnController.update { it.copy(message = "WARP failed: ${error.message}") }
            }
            busy = false
        }
    }

    private fun quickConnect() {
        if (busy) return
        val list = store.list()
        if (list.isEmpty()) {
            addWarp(true)
            return
        }
        lifecycleScope.launch {
            busy = true
            VpnController.update { it.copy(message = "Finding the fastest server\u2026") }
            val results = withContext(Dispatchers.IO) {
                list.map { it to Native.probe(it.endpoint) }
            }
            latencies = results.associate { it.first.id to it.second }
            val best = results.filter { it.second >= 0 }.minByOrNull { it.second }?.first ?: list.first()
            store.setActive(best.id)
            reload()
            VpnController.update { it.copy(message = "Fastest: ${best.name}") }
            connectSelected()
            busy = false
        }
    }

    private fun probeAll() {
        if (busy) return
        val list = store.list()
        if (list.isEmpty()) return
        lifecycleScope.launch {
            busy = true
            val results = withContext(Dispatchers.IO) {
                list.map { it to Native.probe(it.endpoint) }
            }
            latencies = results.associate { it.first.id to it.second }
            busy = false
        }
    }

    private fun connectSelected() {
        val status = VpnController.state.value.status
        if (status == VpnStatus.CONNECTED || status == VpnStatus.CONNECTING) {
            lifecycleScope.launch {
                stopVpn()
                delay(600)
                requestStart()
            }
        } else {
            requestStart()
        }
    }

    private fun requestStart() {
        val prepare = VpnService.prepare(this)
        if (prepare != null) {
            vpnPermission.launch(prepare)
        } else {
            startVpn()
        }
    }

    private fun startVpn() {
        val intent = Intent(this, HaloVpnService::class.java).setAction(HaloVpnService.ACTION_START)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopVpn() {
        val intent = Intent(this, HaloVpnService::class.java).setAction(HaloVpnService.ACTION_STOP)
        startService(intent)
    }
}
