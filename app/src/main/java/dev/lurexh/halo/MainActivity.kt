package dev.lurexh.halo

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.lifecycleScope
import androidx.core.content.ContextCompat
import dev.lurexh.halo.core.Native
import dev.lurexh.halo.ui.HaloScreen
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

    private val vpnPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) startVpn()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            HaloTheme {
                val state = VpnController.state.collectAsState().value

                LaunchedEffect(state.status) {
                    if (state.status != VpnStatus.CONNECTED) return@LaunchedEffect
                    while (true) {
                        val parsed = runCatching { JSONObject(Native.statsJson()) }.getOrNull()
                        if (parsed != null) {
                            VpnController.update {
                                it.copy(rxBytes = parsed.optLong("rx"), txBytes = parsed.optLong("tx"))
                            }
                        }
                        delay(1000)
                    }
                }

                HaloScreen(
                    state = state,
                    onToggle = {
                        when (state.status) {
                            VpnStatus.CONNECTED, VpnStatus.CONNECTING -> stopVpn()
                            else -> requestStart()
                        }
                    },
                    onCreateWarp = { createWarpProfile() },
                )
            }
        }
    }

    private fun createWarpProfile() {
        VpnController.update { it.copy(message = "Registering WARP…") }
        lifecycleScope.launch {
            val raw = withContext(Dispatchers.IO) { Native.registerWarp() }
            val obj = runCatching { JSONObject(raw) }.getOrNull()
            val error = obj?.optString("error")?.takeIf { it.isNotBlank() }
            if (obj == null || error != null) {
                VpnController.update { it.copy(message = error ?: "WARP registration failed") }
                return@launch
            }
            val profile = Profile(
                name = "WARP",
                privateKey = obj.getString("private_key"),
                peerPublicKey = obj.getString("peer_public_key"),
                endpoint = obj.getString("endpoint"),
                address = obj.getString("address"),
                prefix = obj.optInt("prefix", 32),
                dns = obj.optString("dns", "1.1.1.1"),
                mtu = obj.optInt("mtu", 1280),
                allowedIps = listOf("0.0.0.0/0"),
                blocklistFile = ProfileStore.blocklistPath(this@MainActivity),
            )
            ProfileStore(this@MainActivity).save(profile)
            VpnController.update {
                it.copy(profile = "WARP", message = "WARP profile ready — tap the orb to connect")
            }
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
