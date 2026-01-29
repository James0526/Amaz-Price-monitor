package com.example.amazpricetracker.data.network

import java.util.Locale

object PriceParser {
    private val numericRegex = Regex("""([0-9]+(?:[.,][0-9]+)*)""")

    fun parsePriceValue(priceText: String?): Double? {
        if (priceText.isNullOrBlank()) return null
        val match = numericRegex.find(priceText) ?: return null
        val normalized = normalizeNumericString(match.value)
        return normalized.toDoubleOrNull()
    }

    private fun normalizeNumericString(value: String): String {
        val hasComma = value.contains(',')
        val hasPeriod = value.contains('.')

        return when {
            hasComma && hasPeriod -> {
                // Both present: determine which is the decimal separator
                val lastCommaIdx = value.lastIndexOf(',')
                val lastPeriodIdx = value.lastIndexOf('.')
                if (lastCommaIdx > lastPeriodIdx) {
                    // European format: 1.234,56 -> comma is decimal
                    value.replace(".", "").replace(",", ".")
                } else {
                    // US format: 1,234.56 -> period is decimal
                    value.replace(",", "")
                }
            }
            hasComma -> {
                // Only comma: check if it looks like thousands separator
                val parts = value.split(',')
                if (parts.size == 2 && parts[1].length == 3) {
                    // Likely thousands separator (e.g., 1,000)
                    value.replace(",", "")
                } else {
                    // Likely decimal separator (e.g., 1,50)
                    value.replace(",", ".")
                }
            }
            else -> value // Only period or no separator
        }
    }

    fun normalizeTitle(title: String?, fallback: String): String {
        return title?.trim()?.takeIf { it.isNotEmpty() } ?: fallback
    }

    fun fallbackTitleFromUrl(url: String): String {
        val cleaned = url.lowercase(Locale.US)
        return cleaned.substringAfterLast("/").takeIf { it.isNotBlank() } ?: "Amazon Item"
    }
}
