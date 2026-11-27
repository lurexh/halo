package dev.lurexh.halo.ui.render

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import dev.lurexh.halo.vpn.CountryGeo
import dev.lurexh.halo.vpn.Server
import dev.lurexh.halo.vpn.VpnStatus

@Composable
fun HaloGlobe(
    status: VpnStatus,
    servers: List<Server>,
    activeCode: String?,
    onCountryTap: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var failed by remember { mutableStateOf(false) }
    val reducedMotion = remember {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }

    val available = remember(servers) { servers.map { it.countryCode }.toSet() }
    val nodes = remember(available, activeCode) {
        CountryGeo.all.mapNotNull { (code, geo) ->
            GlobeNode(
                code = code,
                lat = geo.lat,
                lon = geo.lon,
                available = code in available,
                active = code == activeCode,
            )
        }
    }
    val activeNode = remember(nodes, activeCode) { nodes.firstOrNull { it.code == activeCode } }

    if (failed) {
        HaloGlobeFallback(
            status = status,
            servers = servers,
            activeCode = activeCode,
            onCountryTap = onCountryTap,
            modifier = modifier,
        )
    } else {
        AndroidView(
            modifier = modifier,
            factory = { viewContext ->
                HaloGlobeView(viewContext) { failed = true }.also { view ->
                    view.onCountryTap = onCountryTap
                }
            },
            update = { view ->
                view.onCountryTap = onCountryTap
                view.setScene(
                    GlobeScene(
                        status = status,
                        nodes = nodes,
                        activeNode = activeNode,
                        reducedMotion = reducedMotion,
                    ),
                )
            },
            onRelease = { view -> view.dispose() },
        )
    }
}
