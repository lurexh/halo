package dev.lurexh.halo

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.core.content.ContextCompat
import dev.lurexh.halo.ui.HaloScreen
import dev.lurexh.halo.ui.theme.HaloTheme
import dev.lurexh.halo.vpn.HaloVpnService
import dev.lurexh.halo.vpn.VpnController
import dev.lurexh.halo.vpn.VpnStatus

class MainActivity : ComponentActivity() {

    private val vpnPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) startVpn()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            HaloTheme {
                val state = VpnController.state.collectAsState().value
                HaloScreen(
                    state = state,
                    onToggle = {
                        when (state.status) {
                            VpnStatus.CONNECTED, VpnStatus.CONNECTING -> stopVpn()
                            else -> requestStart()
                        }
                    },
                )
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
