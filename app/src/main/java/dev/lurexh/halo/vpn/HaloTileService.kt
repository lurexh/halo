package dev.lurexh.halo.vpn

import android.graphics.drawable.Icon
import android.net.VpnService
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
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
        val status = VpnController.state.value.status
        if (status == VpnStatus.CONNECTED || status == VpnStatus.CONNECTING) {
            VpnStarter.stop(this)
        } else {
            val prepare = VpnService.prepare(this)
            if (prepare != null) {
                startActivityAndCollapse(prepare)
            } else {
                VpnStarter.quickConnect(this)
            }
        }
        refresh()
    }
}
