package dev.lurexh.halo

import android.app.Application
import dev.lurexh.halo.vpn.BlocklistUpdater
import dev.lurexh.halo.vpn.CatalogScheduler
import dev.lurexh.halo.vpn.OpenVpnEngine

class HaloApp : Application() {
    override fun onCreate() {
        super.onCreate()
        runCatching { OpenVpnEngine.init(this) }
        runCatching { CatalogScheduler.schedule(this) }
        Thread { BlocklistUpdater.ensure(this) }.apply {
            name = "halo-blocklist"
            isDaemon = true
        }.start()
    }
}
