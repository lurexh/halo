package dev.lurexh.halo.vpn

import android.content.Context
import dev.lurexh.halo.core.Native
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class ProfileStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("halo", Context.MODE_PRIVATE)

    fun list(): List<Profile> {
        migrate()
        val raw = prefs.getString(KEY_PROFILES, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { Profile.fromJson(arr.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }

    fun active(): Profile? {
        val all = list()
        val id = prefs.getString(KEY_ACTIVE, null)
        return all.firstOrNull { it.id == id } ?: all.firstOrNull()
    }

    fun upsert(profile: Profile) {
        val all = list().filterNot { it.id == profile.id } + profile
        saveAll(all)
        if (prefs.getString(KEY_ACTIVE, null) == null) setActive(profile.id)
    }

    fun remove(id: String) {
        saveAll(list().filterNot { it.id == id })
        if (prefs.getString(KEY_ACTIVE, null) == id) prefs.edit().remove(KEY_ACTIVE).apply()
    }

    fun setActive(id: String) {
        prefs.edit().putString(KEY_ACTIVE, id).apply()
    }

    fun adblockEnabled(): Boolean = prefs.getBoolean(KEY_ADBLOCK, true)

    fun setAdblock(enabled: Boolean) = prefs.edit().putBoolean(KEY_ADBLOCK, enabled).apply()

    fun autoconnect(): Boolean = prefs.getBoolean(KEY_AUTOCONNECT, false)

    fun setAutoconnect(enabled: Boolean) = prefs.edit().putBoolean(KEY_AUTOCONNECT, enabled).apply()

    fun engine(): String = prefs.getString(KEY_ENGINE, "smart") ?: "smart"

    fun setEngine(value: String) = prefs.edit().putString(KEY_ENGINE, value).apply()

    fun customRules(): String {
        val file = File(appContext.filesDir, "custom_rules.txt")
        return if (file.exists()) file.readText() else ""
    }

    fun setCustomRules(text: String) {
        File(appContext.filesDir, "custom_rules.txt").writeText(text)
    }

    fun importText(text: String, name: String): Result<Profile> = runCatching {
        val parsed = Native.parseConfig(text, name) ?: error("native parser unavailable")
        val obj = JSONObject(parsed)
        val error = obj.optString("error").takeIf { it.isNotBlank() }
        if (error != null) error(error)
        val profile = Profile.fromJson(obj).copy(
            id = UUID.randomUUID().toString(),
            blocklistFile = blocklistPath(appContext),
            allowlistFile = allowlistPath(appContext),
        )
        if (profile.addresses.isEmpty()) error("config has no Address")
        if (profile.privateKey.isBlank()) error("config has no PrivateKey")
        if (profile.peerPublicKey.isBlank()) error("config has no peer PublicKey")
        upsert(profile)
        profile
    }

    fun warp(): Result<Profile> = runCatching {
        val obj = JSONObject(Native.registerWarp())
        val error = obj.optString("error").takeIf { it.isNotBlank() }
        if (error != null) error(error)
        val profile = Profile.fromJson(obj).copy(
            id = UUID.randomUUID().toString(),
            source = "warp",
            blocklistFile = blocklistPath(appContext),
            allowlistFile = allowlistPath(appContext),
        )
        if (profile.addresses.isEmpty()) error("WARP returned no address")
        upsert(profile)
        profile
    }

    private fun saveAll(profiles: List<Profile>) {
        val arr = JSONArray()
        profiles.forEach { arr.put(JSONObject(it.toJson())) }
        prefs.edit().putString(KEY_PROFILES, arr.toString()).apply()
    }

    private fun migrate() {
        val legacy = prefs.getString("active_profile", null) ?: return
        if (prefs.getString(KEY_PROFILES, null) == null) {
            runCatching {
                val p = Profile.fromJson(JSONObject(legacy))
                saveAll(listOf(p))
                setActive(p.id)
            }
        }
        prefs.edit().remove("active_profile").apply()
    }

    companion object {
        private const val KEY_PROFILES = "profiles"
        private const val KEY_ACTIVE = "active_id"
        private const val KEY_ADBLOCK = "adblock"
        private const val KEY_AUTOCONNECT = "autoconnect"
        private const val KEY_ENGINE = "engine"

        fun blocklistPath(context: Context): String =
            File(context.applicationContext.filesDir, "blocklist.txt").absolutePath

        fun allowlistPath(context: Context): String =
            File(context.applicationContext.filesDir, "allowlist.txt").absolutePath
    }
}
