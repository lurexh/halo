package dev.lurexh.halo.vpn

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Downloads a hosts-format blocklist (StevenBlack's unified hosts, MIT) into
 * app storage. Rust reads the same file at tunnel start and sinkholes matches.
 */
object BlocklistUpdater {
    private const val URL_LISTS = "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts"
    private const val MAX_AGE_MS = 24L * 60 * 60 * 1000
    private const val MIN_SIZE = 10_000L

    fun ensure(context: Context) {
        val target = File(context.filesDir, "blocklist.txt")
        if (!target.exists()) seed(context, target)
        val stale = System.currentTimeMillis() - target.lastModified() > MAX_AGE_MS
        if (target.length() < MIN_SIZE || stale) {
            runCatching { download(URL_LISTS, target) }
                .onFailure { if (target.length() < MIN_SIZE) seed(context, target) }
        }
    }

    private fun seed(context: Context, target: File) {
        runCatching {
            context.assets.open("blocklist.txt").use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        }
    }

    private fun download(url: String, target: File) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 30_000
            instanceFollowRedirects = true
        }
        try {
            val tmp = File(target.parentFile, "blocklist.tmp")
            connection.inputStream.use { input ->
                tmp.outputStream().use { output -> input.copyTo(output) }
            }
            if (tmp.length() >= MIN_SIZE) {
                if (target.exists()) target.delete()
                tmp.renameTo(target)
            } else {
                tmp.delete()
            }
        } finally {
            connection.disconnect()
        }
    }
}
