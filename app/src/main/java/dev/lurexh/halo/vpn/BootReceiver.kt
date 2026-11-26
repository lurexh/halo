package dev.lurexh.halo.vpn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val store = ProfileStore(context)
        if (!store.autoconnect()) return
        if (store.active() == null) return
        runCatching {
            ContextCompat.startForegroundService(
                context,
                Intent(context, HaloVpnService::class.java).setAction(HaloVpnService.ACTION_START),
            )
        }
    }
}
