package dev.lurexh.halo.vpn

import android.content.Context
import org.json.JSONObject
import java.io.File

class ProfileStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("halo", Context.MODE_PRIVATE)

    fun active(): Profile? {
        val raw = prefs.getString(KEY_ACTIVE, null) ?: return null
        return runCatching { decode(raw) }.getOrNull()
    }

    fun save(profile: Profile) {
        prefs.edit().putString(KEY_ACTIVE, profile.toJson()).apply()
    }

    fun importWireGuard(text: String, name: String): Profile {
        var privateKey = ""
        var address = ""
        var dns = "1.1.1.1"
        var mtu = 1280
        var peerKey = ""
        var endpoint = ""
        var allowed = listOf("0.0.0.0/0")
        var inPeer = false

        for (rawLine in text.lineSequence()) {
            val line = rawLine.trim()
            if (line.startsWith("[")) {
                inPeer = line.equals("[Peer]", ignoreCase = true)
                continue
            }
            val idx = line.indexOf('=')
            if (idx < 0) continue
            val key = line.substring(0, idx).trim().lowercase()
            val value = line.substring(idx + 1).trim()
            when (key) {
                "privatekey" -> privateKey = value
                "address" -> address = value.substringBefore(',').trim()
                "dns" -> dns = value.substringBefore(',').trim()
                "mtu" -> mtu = value.toIntOrNull() ?: mtu
                "publickey" -> if (inPeer) peerKey = value
                "endpoint" -> if (inPeer) endpoint = value
                "allowedips" -> if (inPeer) allowed = value.split(',').map { it.trim() }
            }
        }

        val (ip, prefix) = address.split('/').let {
            it[0] to (it.getOrNull(1)?.toIntOrNull() ?: 32)
        }
        val profile = Profile(
            name = name,
            privateKey = privateKey,
            peerPublicKey = peerKey,
            endpoint = endpoint,
            address = ip,
            prefix = prefix,
            dns = dns,
            mtu = mtu,
            allowedIps = allowed,
            blocklistFile = blocklistPath(appContext),
        )
        save(profile)
        return profile
    }

    private fun decode(raw: String): Profile {
        val o = JSONObject(raw)
        val allowedArray = o.optJSONArray("allowed_ips")
        val allowed = buildList {
            if (allowedArray != null) for (i in 0 until allowedArray.length()) add(allowedArray.getString(i))
        }.ifEmpty { listOf("0.0.0.0/0") }
        val blocklist = if (o.has("blocklist_file") && !o.isNull("blocklist_file")) {
            o.getString("blocklist_file")
        } else {
            blocklistPath(appContext)
        }
        return Profile(
            name = o.getString("name"),
            privateKey = o.getString("private_key"),
            peerPublicKey = o.getString("peer_public_key"),
            endpoint = o.getString("endpoint"),
            address = o.getString("address"),
            prefix = o.optInt("prefix", 32),
            dns = o.optString("dns", "1.1.1.1"),
            mtu = o.optInt("mtu", 1280),
            allowedIps = allowed,
            blocklistFile = blocklist,
        )
    }

    companion object {
        private const val KEY_ACTIVE = "active_profile"

        fun blocklistPath(context: Context): String =
            File(context.applicationContext.filesDir, "blocklist.txt").absolutePath
    }
}
