package dev.lurexh.halo.vpn

import org.json.JSONArray
import org.json.JSONObject

data class Profile(
    val name: String,
    val privateKey: String,
    val peerPublicKey: String,
    val endpoint: String,
    val address: String,
    val prefix: Int = 32,
    val dns: String = "1.1.1.1",
    val mtu: Int = 1280,
    val allowedIps: List<String> = listOf("0.0.0.0/0"),
) {
    fun toJson(): String = JSONObject().apply {
        put("name", name)
        put("private_key", privateKey)
        put("peer_public_key", peerPublicKey)
        put("endpoint", endpoint)
        put("address", address)
        put("prefix", prefix)
        put("dns", dns)
        put("mtu", mtu)
        put("allowed_ips", JSONArray(allowedIps))
    }.toString()
}
