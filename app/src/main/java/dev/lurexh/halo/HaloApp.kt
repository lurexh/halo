package dev.lurexh.halo

import android.app.Application

class HaloApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Thread { BlocklistUpdater.ensure(this) }.apply {
            name = "halo-blocklist"
            isDaemon = true
        }.start()
    }
}
