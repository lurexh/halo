package dev.lurexh.halo.vpn

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class Profile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val privateKey: String,
    val peerPublicKey: String,
    val presharedKey: String? = null,
    val endpoint: String,
    val addresses: List<String>,
    val dns: List<String> = listOf("1.1.1.1"),
    val mtu: Int = 1420,
    val allowedIps: List<String> = listOf("0.0.0.0/0", "::/0"),
    val keepalive: Int = 0,
    val reserved: List<Int> = emptyList(),
    val source: String = "import",
    val blocklistFile: String? = null,
    val allowlistFile: String? = null,
) {
    fun tunnelJson(): String = JSONObject().apply {
        put("private_key", privateKey)
        put("peer_public_key", peerPublicKey)
        if (!presharedKey.isNullOrBlank()) put("preshared_key", presharedKey)
        put("endpoint", endpoint)
        put("mtu", mtu)
        put("keepalive", keepalive)
        put("allowed_ips", JSONArray(allowedIps))
        if (reserved.size == 3) put("reserved", JSONArray(reserved))
        blocklistFile?.let { put("blocklist_file", it) }
        allowlistFile?.let { put("allowlist_file", it) }
    }.toString()

    fun toJson(): String = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("private_key", privateKey)
        put("peer_public_key", peerPublicKey)
        if (!presharedKey.isNullOrBlank()) put("preshared_key", presharedKey)
        put("endpoint", endpoint)
        put("addresses", JSONArray(addresses))
        put("dns", JSONArray(dns))
        put("mtu", mtu)
        put("allowed_ips", JSONArray(allowedIps))
        put("keepalive", keepalive)
        put("reserved", JSONArray(reserved))
        put("source", source)
        blocklistFile?.let { put("blocklist_file", it) }
        allowlistFile?.let { put("allowlist_file", it) }
    }.toString()

    companion object {
        fun fromJson(o: JSONObject): Profile {
            val dnsArr = o.optJSONArray("dns")
            val dns = when {
                dnsArr != null -> (0 until dnsArr.length()).map { dnsArr.getString(it) }
                else -> listOf(o.optString("dns")).filter { it.isNotBlank() }.ifEmpty { listOf("1.1.1.1") }
            }
            val addrArr = o.optJSONArray("addresses")
            val addresses = if (addrArr != null) {
                (0 until addrArr.length()).map { addrArr.getString(it) }
            } else {
                listOf(o.optString("address")).filter { it.isNotBlank() }
            }
            val allowedArr = o.optJSONArray("allowed_ips")
            val allowed = if (allowedArr != null) {
                (0 until allowedArr.length()).map { allowedArr.getString(it) }
            } else {
                listOf("0.0.0.0/0", "::/0")
            }
            val resArr = o.optJSONArray("reserved")
            val reserved = if (resArr != null) (0 until resArr.length()).map { resArr.getInt(it) } else emptyList()
            return Profile(
                id = o.optString("id").ifBlank { UUID.randomUUID().toString() },
                name = o.optString("name").ifBlank { "WireGuard" },
                privateKey = o.optString("private_key"),
                peerPublicKey = o.optString("peer_public_key"),
                presharedKey = o.optString("preshared_key").takeIf { it.isNotBlank() && it != "null" },
                endpoint = o.optString("endpoint"),
                addresses = addresses,
                dns = dns,
                mtu = o.optInt("mtu", 1420),
                allowedIps = allowed,
                keepalive = o.optInt("keepalive", 0),
                reserved = reserved,
                source = o.optString("source").ifBlank { "import" },
                blocklistFile = o.optString("blocklist_file").takeIf { it.isNotBlank() && it != "null" },
                allowlistFile = o.optString("allowlist_file").takeIf { it.isNotBlank() && it != "null" },
            )
        }
    }
}
