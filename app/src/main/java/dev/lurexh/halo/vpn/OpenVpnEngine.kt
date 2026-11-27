package dev.lurexh.halo.vpn

import android.content.Context
import android.content.Intent
import de.blinkt.openvpn.core.ConfigParser
import de.blinkt.openvpn.core.ConnectionStatus
import de.blinkt.openvpn.core.OpenVPNService
import de.blinkt.openvpn.core.ProfileManager
import de.blinkt.openvpn.core.VPNLaunchHelper
import de.blinkt.openvpn.core.VpnStatus as OpenVpnStatus
import java.io.StringReader

object OpenVpnEngine {
    @Volatile
    private var initialized = false

    @Volatile
    private var appContext: Context? = null

    private val stateListener = object : OpenVpnStatus.StateListener {
        override fun updateState(
            state: String?,
            logmessage: String?,
            localizedResId: Int,
            level: ConnectionStatus?,
            intent: Intent?,
        ) {
            if (VpnController.state.value.engine != "openvpn") return
            val status = when (level) {
                ConnectionStatus.LEVEL_CONNECTED -> VpnStatus.CONNECTED
                ConnectionStatus.LEVEL_CONNECTING_SERVER_REPLIED,
                ConnectionStatus.LEVEL_CONNECTING_NO_SERVER_REPLY_YET,
                ConnectionStatus.LEVEL_START,
                ConnectionStatus.LEVEL_VPNPAUSED -> VpnStatus.CONNECTING
                ConnectionStatus.LEVEL_AUTH_FAILED,
                ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT -> VpnStatus.ERROR
                ConnectionStatus.LEVEL_NONETWORK -> VpnStatus.ERROR
                ConnectionStatus.LEVEL_NOTCONNECTED -> VpnStatus.DISCONNECTED
                else -> null
            } ?: return

            val message = when {
                status == VpnStatus.ERROR && !logmessage.isNullOrBlank() -> logmessage
                level == ConnectionStatus.LEVEL_NONETWORK -> "No network"
                level == ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT -> "Server requested credentials"
                status == VpnStatus.DISCONNECTED -> null
                else -> VpnController.state.value.message
            }
            VpnController.update {
                it.copy(
                    status = status,
                    engine = "openvpn",
                    message = message,
                )
            }
        }

        override fun setConnectedVPN(uuid: String?) = Unit
    }

    private val byteListener = OpenVpnStatus.ByteCountListener { inBytes, outBytes, _, _ ->
        if (VpnController.state.value.engine == "openvpn") {
            VpnController.update {
                it.copy(rxBytes = inBytes, txBytes = outBytes)
            }
        }
    }

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            appContext = context.applicationContext
            OpenVpnStatus.addStateListener(stateListener)
            OpenVpnStatus.addByteCountListener(byteListener)
            initialized = true
        }
    }

    fun connect(context: Context, server: Server, adblock: Boolean): Result<Unit> =
        runCatching {
            init(context)
            val parser = ConfigParser()
            parser.parseConfig(StringReader(server.config))
            val profile = parser.convertProfile()
            profile.mName = server.label
            profile.mUseDefaultRoute = true
            profile.mUseDefaultRoutev6 = true
            profile.mOverrideDNS = true
            profile.mDNS1 = if (adblock) "94.140.14.14" else "1.1.1.1"
            profile.mDNS2 = if (adblock) "94.140.15.15" else "1.0.0.1"
            profile.mAllowLocalLAN = true

            val manager = ProfileManager.getInstance(context.applicationContext)
            manager.addProfile(profile)
            ProfileManager.saveProfile(context.applicationContext, profile)
            manager.saveProfileList(context.applicationContext)

            VpnController.update {
                it.copy(
                    status = VpnStatus.CONNECTING,
                    engine = "openvpn",
                    protocol = "OpenVPN",
                    profileName = server.label,
                    profileId = server.id,
                    exitIp = server.ip,
                    exitCountry = server.countryCode,
                    exitCountryName = server.countryName,
                    rxBytes = 0,
                    txBytes = 0,
                    blocked = 0,
                    message = "Connecting to ${server.countryName}\u2026",
                )
            }
            VPNLaunchHelper.startOpenVpn(profile, context.applicationContext, "Halo", true)
        }.onFailure { error ->
            VpnController.update {
                it.copy(
                    status = VpnStatus.ERROR,
                    engine = "openvpn",
                    message = error.message ?: "OpenVPN could not start",
                )
            }
        }
    }

    fun disconnect(context: Context) {
        val wasOpenVpn = VpnController.state.value.engine == "openvpn"
        runCatching {
            context.startService(
                Intent(context, OpenVPNService::class.java)
                    .setAction(OpenVPNService.DISCONNECT_VPN),
            )
        }
        if (wasOpenVpn) {
            VpnController.update {
                it.copy(status = VpnStatus.DISCONNECTED, message = null)
            }
        }
    }

    fun logs(): String {
        val context = appContext ?: return ""
        return runCatching {
            OpenVpnStatus.getlogbuffer()
                .takeLast(80)
                .joinToString("\n") { it.getString(context) }
        }.getOrDefault("")
    }
}
