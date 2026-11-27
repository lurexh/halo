package dev.lurexh.halo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.lurexh.halo.ui.theme.HaloCyan
import dev.lurexh.halo.ui.theme.HaloSurface
import dev.lurexh.halo.ui.theme.TextMuted
import dev.lurexh.halo.ui.theme.TextPrimary
import dev.lurexh.halo.vpn.Countries
import dev.lurexh.halo.vpn.Country
import dev.lurexh.halo.vpn.Server

@Composable
fun CountryDialog(
    country: Country,
    servers: List<Server>,
    onDismiss: () -> Unit,
    onFastest: () -> Unit,
    onServer: (Server) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "${Countries.flag(country.code)}  ${country.name}",
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            if (servers.isEmpty()) {
                Text("No public relay in this country yet.", color = TextMuted)
            } else {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(servers, key = { it.id }) { server ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onServer(server) }
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    server.name,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    "${server.pingLabel} \u00b7 ${server.speedLabel}",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                )
                            }
                            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = HaloCyan)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onFastest) {
                Text("Fastest", color = HaloCyan)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextMuted)
            }
        },
        containerColor = HaloSurface,
    )
}
