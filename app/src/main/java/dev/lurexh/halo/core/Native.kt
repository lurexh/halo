package dev.lurexh.halo.core

object Native {
    val available: Boolean = runCatching { System.loadLibrary("halo_core") }.isSuccess

    private external fun nativeStart(fd: Int, configJson: String): Int
    private external fun nativeStop()
    private external fun nativeStats(): String
    private external fun nativeVersion(): String
    private external fun nativeParseConfig(text: String, name: String): String
    private external fun nativeProbe(endpoint: String): Int
    private external fun nativeLogs(): String
    private external fun nativeRegisterWarp(): String

    fun startTunnel(fd: Int, configJson: String): Int =
        if (available) runCatching { nativeStart(fd, configJson) }.getOrDefault(-3) else -3

    fun stopTunnel() {
        if (available) runCatching { nativeStop() }
    }

    fun statsJson(): String =
        if (available) runCatching { nativeStats() }.getOrDefault("{}") else "{}"

    fun version(): String =
        if (available) runCatching { nativeVersion() }.getOrDefault("halo-core") else "halo-core (not loaded)"

    fun parseConfig(text: String, name: String): String? =
        if (available) runCatching { nativeParseConfig(text, name) }.getOrNull() else null

    fun probe(endpoint: String): Int =
        if (available) runCatching { nativeProbe(endpoint) }.getOrDefault(-1) else -1

    fun logs(): String =
        if (available) runCatching { nativeLogs() }.getOrDefault("") else ""

    fun registerWarp(): String =
        if (available) runCatching { nativeRegisterWarp() }.getOrDefault("{\"error\":\"native unavailable\"}")
        else "{\"error\":\"native unavailable\"}"
}
