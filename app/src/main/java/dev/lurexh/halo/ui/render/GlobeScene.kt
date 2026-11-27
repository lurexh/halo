package dev.lurexh.halo.ui.render

import dev.lurexh.halo.vpn.VpnStatus

data class GlobeNode(
    val code: String,
    val lat: Float,
    val lon: Float,
    val available: Boolean,
    val active: Boolean,
)

data class GlobeScene(
    val status: VpnStatus = VpnStatus.DISCONNECTED,
    val nodes: List<GlobeNode> = emptyList(),
    val activeNode: GlobeNode? = null,
    val reducedMotion: Boolean = false,
)

data class ProjectedNode(
    val code: String,
    val x: Float,
    val y: Float,
    val depth: Float,
)
