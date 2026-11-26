package dev.lurexh.halo.vpn

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class VpnStatus { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

data class VpnState(
    val status: VpnStatus = VpnStatus.DISCONNECTED,
    val profileName: String = "No profile",
    val profileId: String? = null,
    val rxBytes: Long = 0,
    val txBytes: Long = 0,
    val blocked: Long = 0,
    val message: String? = null,
)

object VpnController {
    private val _state = MutableStateFlow(VpnState())
    val state = _state.asStateFlow()

    fun update(transform: (VpnState) -> VpnState) {
        _state.value = transform(_state.value)
    }
}
