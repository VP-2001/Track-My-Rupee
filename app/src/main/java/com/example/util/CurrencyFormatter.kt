package com.example.util

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {
    private val indianLocale = Locale.forLanguageTag("en-IN")

    /**
     * Formats an amount with Indian numbering format (e.g. ₹1,25,000).
     */
    fun formatRupees(amount: Double): String {
        return try {
            val format = NumberFormat.getCurrencyInstance(indianLocale)
            format.maximumFractionDigits = if (amount % 1.0 == 0.0) 0 else 2
            format.minimumFractionDigits = 0
            val formatted = format.format(amount)
            // Ensure ₹ symbol is present
            if (!formatted.startsWith("₹")) {
                "₹" + formatted.replace(Regex("[^0-9,.]"), "").trim()
            } else {
                formatted
            }
        } catch (_: Exception) {
            "₹%,.0f".format(amount)
        }
    }

    /**
     * Formats an amount rounded to the nearest rupee with Indian numbering.
     */
    fun formatRupeesRounded(amount: Double): String {
        return try {
            val format = NumberFormat.getCurrencyInstance(indianLocale)
            format.maximumFractionDigits = 0
            format.minimumFractionDigits = 0
            val formatted = format.format(amount)
            if (!formatted.startsWith("₹")) {
                "₹" + formatted.replace(Regex("[^0-9,.]"), "").trim()
            } else {
                formatted
            }
        } catch (_: Exception) {
            "₹%,.0f".format(amount)
        }
    }
}
