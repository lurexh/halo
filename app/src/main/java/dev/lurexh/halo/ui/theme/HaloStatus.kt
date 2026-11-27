package dev.lurexh.halo.ui.theme

import androidx.compose.ui.graphics.Color
import dev.lurexh.halo.vpn.VpnStatus

fun haloStatusLabel(status: VpnStatus): String = when (status) {
    VpnStatus.CONNECTED -> "Protected"
    VpnStatus.CONNECTING -> "Connecting"
    VpnStatus.ERROR -> "Attention"
    VpnStatus.DISCONNECTED -> "Unprotected"
}

fun haloStatusColor(status: VpnStatus): Color = when (status) {
    VpnStatus.CONNECTED -> ConnectedGreen
    VpnStatus.CONNECTING -> Color(0xFFFFC857)
    VpnStatus.ERROR -> Danger
    VpnStatus.DISCONNECTED -> TextMuted
}
