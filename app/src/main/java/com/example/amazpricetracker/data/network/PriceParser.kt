package com.example.amazpricetracker.data.network

import java.util.Locale

object PriceParser {
    private val numericRegex = Regex("""([0-9]+(?:[.,][0-9]+)?)""")

    fun parsePriceValue(priceText: String?): Double? {
        if (priceText.isNullOrBlank()) return null
        val match = numericRegex.find(priceText) ?: return null
        val normalized = match.value.replace(",", ".")
        return normalized.toDoubleOrNull()
    }

    fun normalizeTitle(title: String?, fallback: String): String {
        return title?.trim()?.takeIf { it.isNotEmpty() } ?: fallback
    }

    fun fallbackTitleFromUrl(url: String): String {
        val cleaned = url.lowercase(Locale.US)
        return cleaned.substringAfterLast("/").takeIf { it.isNotBlank() } ?: "Amazon Item"
    }
}
