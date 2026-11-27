package dev.lurexh.halo.vpn

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

object VpnStarter {
    fun quickConnect(context: Context) {
        val app = context.applicationContext
        val store = ProfileStore(app)
        val engine = store.engine()
        val openVpnServer = if (engine == "openvpn") {
            val catalog = CatalogRepository.load(app)
            CatalogRepository.lastServer(app) ?: CatalogRepository.fastest(catalog.servers)
        } else {
            null
        }

        when (engine) {
            "openvpn" -> {
                if (openVpnServer != null) connectOpenVpn(app, openVpnServer) else connectWarp(app)
            }
            else -> connectWarp(app)
        }
    }

    fun connectOpenVpn(context: Context, server: Server) {
        val app = context.applicationContext
        CatalogRepository.setLastServer(app, server)
        stopHaloService(app)
        OpenVpnEngine.connect(app, server, ProfileStore(app).adblockEnabled())
    }

    fun connectWarp(context: Context) {
        val app = context.applicationContext
        OpenVpnEngine.disconnect(app)
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
        Thread {
            val result = ProfileStore(app).warp()
            result.onSuccess { profile ->
                ProfileStore(app).setActive(profile.id)
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
                runCatching {
                    ContextCompat.startForegroundService(
                        app,
                        Intent(app, HaloVpnService::class.java)
                            .setAction(HaloVpnService.ACTION_START),
                    )
                }.onFailure { error ->
                    VpnController.update {
                        it.copy(
                            status = VpnStatus.ERROR,
                            message = error.message ?: "Could not start WARP",
                        )
                    }
                }
            }.onFailure { error ->
                VpnController.update {
                    it.copy(
                        status = VpnStatus.ERROR,
                        engine = "warp",
                        message = "WARP failed: ${error.message}",
                    )
                }
            }
        }.apply {
            name = "halo-warp"
            isDaemon = true
        }.start()
    }

    fun stop(context: Context) {
        val app = context.applicationContext
        OpenVpnEngine.disconnect(app)
        runCatching {
            app.stopService(Intent(app, HaloVpnService::class.java))
        }
        VpnController.update {
            it.copy(status = VpnStatus.DISCONNECTED, message = null)
        }
    }

    private fun stopHaloService(context: Context) {
        runCatching {
            context.stopService(Intent(context, HaloVpnService::class.java))
        }
    }
}
