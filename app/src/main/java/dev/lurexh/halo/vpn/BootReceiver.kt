package dev.lurexh.halo.vpn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        runCatching { CatalogScheduler.schedule(context) }
        if (!ProfileStore(context).autoconnect()) return
        runCatching { VpnStarter.quickConnect(context) }
    }
}
