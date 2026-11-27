package dev.lurexh.halo.vpn

import java.util.Locale

data class Country(
    val code: String,
    val name: String,
)

object Countries {
    fun all(): List<Country> = Locale.getISOCountries()
        .asSequence()
        .map { code -> Country(code, name(code)) }
        .distinctBy { it.code }
        .sortedBy { it.name }
        .toList()

    fun name(code: String): String {
        if (code.isBlank()) return "Unknown"
        return runCatching {
            Locale.Builder().setRegion(code.uppercase(Locale.US)).build()
                .getDisplayCountry(Locale.ENGLISH)
                .ifBlank { code.uppercase(Locale.US) }
        }.getOrDefault(code.uppercase(Locale.US))
    }

    fun flag(code: String): String {
        val value = code.trim().uppercase(Locale.US)
        if (value.length != 2 || value.any { it !in 'A'..'Z' }) return "\uD83C\uDF10"
        val first = Character.toChars(0x1F1E6 + (value[0] - 'A'))
        val second = Character.toChars(0x1F1E6 + (value[1] - 'A'))
        return String(first) + String(second)
    }
}
