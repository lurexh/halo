package dev.lurexh.halo.vpn

import android.content.Context
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Catalog(
    val servers: List<Server>,
    val updatedAt: Long,
)

object CatalogRepository {
    private const val API = "https://www.vpngate.net/api/iphone/"
    private const val FILE = "vpngate_catalog.json"
    private const val PREFS = "halo_catalog"
    private const val KEY_LAST = "last_server_id"
    private const val MAX_AGE_MS = 12L * 60L * 60L * 1000L

    fun load(context: Context): Catalog {
        val file = File(context.applicationContext.filesDir, FILE)
        if (!file.exists()) return Catalog(emptyList(), 0L)
        return runCatching {
            val root = JSONObject(file.readText())
            val arr = root.optJSONArray("servers") ?: JSONArray()
            val servers = buildList {
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    Server.fromJson(item)?.let { add(it) }
                }
            }
            Catalog(servers, root.optLong("updated_at"))
        }.getOrDefault(Catalog(emptyList(), 0L))
    }

    fun refresh(context: Context, force: Boolean = false): Result<Catalog> = runCatching {
        val cached = load(context)
        val fresh = cached.servers.isNotEmpty() && System.currentTimeMillis() - cached.updatedAt < MAX_AGE_MS
        if (!force && fresh) {
            cached
        } else {
            val connection = (URL(API).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 35_000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Halo/1.0 (Android)")
                setRequestProperty("Accept", "text/csv,text/plain,*/*")
            }
            try {
                if (connection.responseCode !in 200..299) {
                    error("Server catalog returned HTTP ${connection.responseCode}")
                }
                val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val now = System.currentTimeMillis()
                val servers = parse(body, now)
                if (servers.isEmpty()) error("Server catalog is empty")
                val catalog = Catalog(servers, now)
                save(context, catalog)
                catalog
            } finally {
                connection.disconnect()
            }
        }
    }

    fun refreshIfStale(context: Context): Result<Catalog> = refresh(context, force = false)

    fun lastServer(context: Context): Server? {
        val id = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LAST, null)
        val servers = load(context).servers
        return servers.firstOrNull { it.id == id } ?: fastest(servers)
    }

    fun setLastServer(context: Context, server: Server) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST, server.id)
            .apply()
    }

    fun fastest(servers: List<Server>, countryCode: String? = null): Server? =
        servers.asSequence()
            .filter { countryCode == null || it.countryCode.equals(countryCode, ignoreCase = true) }
            .filter { it.config.isNotBlank() }
            .sortedWith(
                compareByDescending<Server> { it.speedBps }
                    .thenBy { if (it.pingMs in 1..9_999) it.pingMs else 9_999 }
                    .thenByDescending { it.score },
            )
            .firstOrNull()

    fun grouped(servers: List<Server>): Map<String, List<Server>> =
        servers.groupBy { it.countryCode }.mapValues { (_, list) ->
            list.sortedWith(
                compareByDescending<Server> { it.speedBps }
                    .thenBy { if (it.pingMs in 1..9_999) it.pingMs else 9_999 },
            )
        }

    fun formatUpdated(timestamp: Long): String {
        if (timestamp <= 0L) return "never"
        return SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date(timestamp))
    }

    private fun save(context: Context, catalog: Catalog) {
        val file = File(context.applicationContext.filesDir, FILE)
        val root = JSONObject().apply {
            put("updated_at", catalog.updatedAt)
            put("servers", JSONArray().apply {
                catalog.servers.forEach { put(it.toJson()) }
            })
        }
        val temp = File(file.parentFile, "$FILE.tmp")
        temp.writeText(root.toString())
        if (!temp.renameTo(file)) {
            file.writeText(root.toString())
            temp.delete()
        }
    }

    private fun parse(body: String, now: Long): List<Server> {
        val out = ArrayList<Server>()
        val seen = HashSet<String>()
        body.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEach
            if (line.startsWith("*") || line.startsWith("#")) return@forEach
            val fields = splitCsv(line)
            if (fields.size < 15) return@forEach

            val hostName = fields[0].trim()
            val ip = fields[1].trim()
            val score = fields[2].toLongOrNull() ?: 0L
            val ping = fields[3].toIntOrNull() ?: 9_999
            val speed = fields[4].toLongOrNull() ?: 0L
            val countryName = fields[5].trim()
            val countryCode = fields[6].trim().uppercase(Locale.US)
            val sessions = fields[7].toIntOrNull() ?: 0
            val operator = fields[12].trim()
            val encoded = fields[14].trim()
            if (countryCode.length != 2 || encoded.isEmpty()) return@forEach

            val config = runCatching {
                String(Base64.decode(encoded, Base64.DEFAULT), Charsets.UTF_8)
            }.getOrNull()?.takeIf { it.contains("remote ") || it.contains("client") } ?: return@forEach

            val remote = Regex("""(?m)^\s*remote\s+(\S+)(?:\s+(\d+))?""").find(config)
            val host = remote?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() } ?: ip
            val port = remote?.groupValues?.getOrNull(2)?.toIntOrNull() ?: 443
            if (host.isBlank()) return@forEach

            val id = "$countryCode-$host-$port-$ip"
            if (!seen.add(id)) return@forEach

            out += Server(
                id = id,
                name = hostName.ifBlank { "Public relay" },
                countryCode = countryCode,
                countryName = countryName.ifBlank { countryCode },
                host = host,
                ip = ip,
                port = port,
                pingMs = ping,
                speedBps = speed,
                score = score,
                sessions = sessions,
                operator = operator,
                config = config,
                updatedAt = now,
            )
        }
        return out
    }

    private fun splitCsv(line: String): List<String> {
        val fields = ArrayList<String>()
        val current = StringBuilder()
        var quoted = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && quoted && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"')
                    i++
                }
                c == '"' -> quoted = !quoted
                c == ',' && !quoted -> {
                    fields += current.toString()
                    current.clear()
                }
                else -> current.append(c)
            }
            i++
        }
        fields += current.toString()
        return fields
    }
}
