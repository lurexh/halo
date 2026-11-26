package dev.lurexh.halo.vpn

import android.content.Intent
import android.graphics.drawable.Icon
import android.net.VpnService
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import dev.lurexh.halo.R

class HaloTileService : TileService() {

    override fun onStartListening() {
        refresh()
    }

    private fun refresh() {
        val tile = qsTile ?: return
        val connected = VpnController.state.value.status == VpnStatus.CONNECTED
        tile.state = if (connected) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "Halo VPN"
        tile.icon = Icon.createWithResource(this, R.drawable.ic_notification)
        tile.updateTile()
    }

    override fun onClick() {
        val connected = VpnController.state.value.status == VpnStatus.CONNECTED
        if (connected) {
            startService(Intent(this, HaloVpnService::class.java).setAction(HaloVpnService.ACTION_STOP))
        } else {
            val prepare = VpnService.prepare(this)
            if (prepare != null) {
                startActivityAndCollapse(prepare)
            } else {
                ContextCompat.startForegroundService(
                    this,
                    Intent(this, HaloVpnService::class.java).setAction(HaloVpnService.ACTION_START),
                )
            }
        }
        refresh()
    }
}
