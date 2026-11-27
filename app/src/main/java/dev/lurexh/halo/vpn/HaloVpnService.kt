package dev.lurexh.halo.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import dev.lurexh.halo.MainActivity
import dev.lurexh.halo.R
import dev.lurexh.halo.core.Native
import org.json.JSONArray
import org.json.JSONObject

class HaloVpnService : VpnService() {

    private var tun: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTunnel()
            ACTION_STOP -> stopTunnel()
        }
        return START_STICKY
    }

    private fun startTunnel() {
        closeTunnel()

        val connecting = buildNotification("Connecting")
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, connecting, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, connecting)
        }

        val store = ProfileStore(this)
        val profile = store.active()
        if (profile == null) {
            fail("No profile imported")
            return
        }

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
                rxBytes = 0,
                txBytes = 0,
                blocked = 0,
                message = null,
            )
        }

        val builder = Builder()
            .setSession("Halo VPN")
            .setMtu(profile.mtu.coerceIn(1280, 1500))

        var added = 0
        for (addr in profile.addresses) {
            val (ip, prefix) = splitCidr(addr) ?: continue
            runCatching { builder.addAddress(ip, prefix) }.onSuccess { added++ }
        }
        if (added == 0) {
            fail("Profile has no usable Address")
            return
        }

        val dns = profile.dns.filter { it.isNotBlank() }.ifEmpty { listOf("1.1.1.1") }
        dns.forEach { runCatching { builder.addDnsServer(it) } }

        builder.addRoute("0.0.0.0", 0)
        builder.addRoute("::", 0)

        try {
            builder.addDisallowedApplication(packageName)
        } catch (_: PackageManager.NameNotFoundException) {
        }

        val pfd = runCatching { builder.establish() }.getOrNull()
        if (pfd == null) {
            fail("VPN permission denied")
            return
        }
        tun = pfd

        val config = JSONObject(profile.tunnelJson()).apply {
            if (store.adblockEnabled()) {
                val rules = store.customRules()
                    .lineSequence()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && !it.startsWith("#") }
                    .toList()
                put("blocklists", JSONArray(rules))
                profile.blocklistFile?.let { put("blocklist_file", it) }
                profile.allowlistFile?.let { put("allowlist_file", it) }
            } else {
                put("blocklists", JSONArray())
                remove("blocklist_file")
                remove("allowlist_file")
            }
        }.toString()

        val rc = runCatching { Native.startTunnel(pfd.fd, config) }.getOrElse { error ->
            fail("Core unavailable: ${error.message}")
            return
        }
        if (rc != 0) {
            fail("Core start failed (code $rc)")
            return
        }

        getSystemService(NotificationManager::class.java).notify(NOTIF_ID, buildNotification(profile.name))
        VpnController.update {
            it.copy(
                status = VpnStatus.CONNECTED,
                engine = "warp",
                protocol = "WARP",
                profileName = "WARP Auto",
                exitIp = "Cloudflare edge",
                exitCountry = "",
                exitCountryName = "Cloudflare",
                message = null,
            )
        }
    }

    private fun closeTunnel() {
        runCatching { tun?.close() }
        tun = null
        runCatching { Native.stopTunnel() }
    }

    private fun stopTunnel() {
        closeTunnel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        if (VpnController.state.value.engine == "warp") {
            VpnController.update {
                it.copy(
                    status = VpnStatus.DISCONNECTED,
                    engine = "warp",
                    exitIp = "",
                    exitCountry = "",
                    exitCountryName = "",
                    message = null,
                )
            }
        }
        stopSelf()
    }

    private fun fail(message: String) {
        closeTunnel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        if (VpnController.state.value.engine == "warp") {
            VpnController.update {
                it.copy(status = VpnStatus.ERROR, engine = "warp", message = message)
            }
        }
        stopSelf()
    }

    override fun onRevoke() {
        stopTunnel()
        super.onRevoke()
    }

    override fun onDestroy() {
        closeTunnel()
        super.onDestroy()
    }

    private fun splitCidr(value: String): Pair<String, Int>? {
        val v = value.trim()
        if (v.isEmpty()) return null
        val idx = v.lastIndexOf('/')
        return if (idx < 0) {
            Pair(v, if (v.contains(':')) 128 else 32)
        } else {
            val ip = v.substring(0, idx)
            val prefix = v.substring(idx + 1).toIntOrNull() ?: return null
            Pair(ip, prefix)
        }
    }

    private fun buildNotification(profile: String): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Connection", NotificationManager.IMPORTANCE_LOW),
            )
        }
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("Halo VPN")
            .setContentText("Connected \u00b7 $profile")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(open)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val ACTION_START = "dev.lurexh.halo.START"
        const val ACTION_STOP = "dev.lurexh.halo.STOP"
        private const val CHANNEL_ID = "halo_connection"
        private const val NOTIF_ID = 1337
    }
}
