package dev.lurexh.halo.vpn

import org.json.JSONObject

data class Server(
    val id: String,
    val name: String,
    val countryCode: String,
    val countryName: String,
    val host: String,
    val ip: String,
    val port: Int,
    val pingMs: Int,
    val speedBps: Long,
    val score: Long,
    val sessions: Int,
    val operator: String,
    val config: String,
    val source: String = "vpngate",
    val updatedAt: Long = 0L,
) {
    val label: String
        get() = if (countryName.isBlank()) name else "$countryName \u00b7 $name"

    val pingLabel: String
        get() = if (pingMs in 1..9_999) "$pingMs ms" else "\u2014"

    val speedLabel: String
        get() = if (speedBps <= 0) "\u2014" else "${speedBps / 1_000_000} Mbps"

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("country_code", countryCode)
        put("country_name", countryName)
        put("host", host)
        put("ip", ip)
        put("port", port)
        put("ping_ms", pingMs)
        put("speed_bps", speedBps)
        put("score", score)
        put("sessions", sessions)
        put("operator", operator)
        put("config", config)
        put("source", source)
        put("updated_at", updatedAt)
    }

    companion object {
        fun fromJson(obj: JSONObject): Server? {
            val id = obj.optString("id")
            val config = obj.optString("config")
            if (id.isBlank() || config.isBlank()) return null
            return Server(
                id = id,
                name = obj.optString("name", "Public relay"),
                countryCode = obj.optString("country_code").uppercase(),
                countryName = obj.optString("country_name", "Unknown"),
                host = obj.optString("host"),
                ip = obj.optString("ip"),
                port = obj.optInt("port", 443),
                pingMs = obj.optInt("ping_ms", 9_999),
                speedBps = obj.optLong("speed_bps"),
                score = obj.optLong("score"),
                sessions = obj.optInt("sessions"),
                operator = obj.optString("operator"),
                config = config,
                source = obj.optString("source", "vpngate"),
                updatedAt = obj.optLong("updated_at"),
            )
        }
    }
}
