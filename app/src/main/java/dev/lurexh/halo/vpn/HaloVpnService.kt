package dev.lurexh.halo.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import dev.lurexh.halo.core.Native

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
        val profile = ProfileStore(this).active()
        if (profile == null) {
            fail("No profile imported")
            return
        }

        VpnController.update { it.copy(status = VpnStatus.CONNECTING, profile = profile.name, message = null) }

        val builder = Builder()
            .setSession("Halo VPN")
            .setMtu(profile.mtu)
            .addAddress(profile.address, profile.prefix)
            .addDnsServer(profile.dns)
            .addRoute("0.0.0.0", 0)

        try {
            builder.addDisallowedApplication(packageName)
        } catch (_: PackageManager.NameNotFoundException) {
        }

        val pfd = builder.establish()
        if (pfd == null) {
            fail("VPN permission denied")
            return
        }
        tun = pfd

        val rc = Native.startTunnel(pfd.fd, profile.toJson())
        if (rc != 0) {
            fail("Core start failed (code $rc)")
            return
        }

        startForeground(NOTIF_ID, buildNotification(profile.name))
        VpnController.update { it.copy(status = VpnStatus.CONNECTED) }
    }

    private fun stopTunnel() {
        Native.stopTunnel()
        runCatching { tun?.close() }
        tun = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        VpnController.update { VpnState(status = VpnStatus.DISCONNECTED) }
        stopSelf()
    }

    private fun fail(message: String) {
        Native.stopTunnel()
        runCatching { tun?.close() }
        tun = null
        VpnController.update { it.copy(status = VpnStatus.ERROR, message = message) }
        stopSelf()
    }

    override fun onRevoke() {
        stopTunnel()
        super.onRevoke()
    }

    override fun onDestroy() {
        Native.stopTunnel()
        runCatching { tun?.close() }
        tun = null
        super.onDestroy()
    }

    private fun buildNotification(profile: String): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Connection", NotificationManager.IMPORTANCE_LOW),
            )
        }
        val open = android.app.PendingIntent.getActivity(
            this,
            0,
            Intent(this, dev.lurexh.halo.MainActivity::class.java),
            android.app.PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Halo VPN")
            .setContentText("Connected · $profile")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
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
