package dev.lurexh.halo.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.lurexh.halo.core.Native
import dev.lurexh.halo.ui.theme.HaloCyan
import dev.lurexh.halo.ui.theme.HaloSurface
import dev.lurexh.halo.ui.theme.TextMuted
import dev.lurexh.halo.ui.theme.TextPrimary
import dev.lurexh.halo.vpn.OpenVpnEngine

@Composable
fun LogsDialog(onDismiss: () -> Unit) {
    val logs = remember {
        buildString {
            val openVpn = runCatching { OpenVpnEngine.logs() }.getOrDefault("")
            if (openVpn.isNotBlank()) {
                append(openVpn)
                append("\n\n")
            }
            append(runCatching { Native.logs() }.getOrDefault(""))
        }.ifBlank { "No logs yet." }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row {
                Icon(Icons.Rounded.Info, contentDescription = null, tint = HaloCyan)
                Spacer(Modifier.width(8.dp))
                Text("Connection logs", color = TextPrimary)
            }
        },
        text = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = logs,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = HaloCyan)
            }
        },
        containerColor = HaloSurface,
    )
}
