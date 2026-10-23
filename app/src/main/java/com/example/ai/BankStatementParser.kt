package com.example.ai

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BankStatementParser {

    fun parseNative(rawText: String, bankHint: String): StatementParseResult {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val extracted = mutableListOf<ExtractedTransaction>()
        var totalDebits = 0.0

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dateAltFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

        val dateRegex = Regex("""(\d{2}[/-]\d{2}[/-]\d{4}|\d{2}\s+[A-Za-z]{3}\s+\d{4})""")
        val amountRegex = Regex("""(?i)(?:rs\.?|inr)?\s*([0-9,]+(?:\.\d{2})?)""")

        for (line in lines) {
            // Ignore header lines or summary lines
            if (line.contains("Opening Balance", true) || line.contains("Closing Balance", true) || line.contains("Statement Period", true)) continue

            val dateMatch = dateRegex.find(line)
            if (dateMatch != null) {
                val dateStr = dateMatch.value
                val afterDate = line.substring(dateMatch.range.last + 1).trim()

                // Look for debit amounts or DR indicators
                val isDebit = line.contains("DR", true) || line.contains("Debit", true) || line.contains("Dr.", true) || !line.contains("CR", true)
                if (isDebit) {
                    val amtMatches = amountRegex.findAll(afterDate).toList()
                    if (amtMatches.isNotEmpty()) {
                        val rawAmtStr = amtMatches.last().groupValues[1].replace(",", "")
                        val amount = rawAmtStr.toDoubleOrNull()
                        if (amount != null && amount > 0 && amount < 5000000) {
                            val description = afterDate
                                .replace(amtMatches.last().value, "")
                                .replace(Regex("""(?i)\b(DR|CR|Debit|Credit|UPI|NEFT|IMPS|POS)\b"""), "")
                                .replace(Regex("""\s+"""), " ")
                                .trim()
                                .ifEmpty { "Bank Transaction" }

                            val category = inferCategory(description)
                            val paymentMethod = if (line.contains("UPI", true)) "UPI" else if (line.contains("POS", true) || bankHint.contains("Card", true)) "Credit Card" else "UPI"

                            var txTime = System.currentTimeMillis()
                            try {
                                val parsedDate = if (dateStr.contains("-")) dateAltFormat.parse(dateStr) else dateFormat.parse(dateStr)
                                if (parsedDate != null) txTime = parsedDate.time
                            } catch (_: Exception) {}

                            val isCreditCard = paymentMethod == "Credit Card" || bankHint.contains("Card", true)
                            extracted.add(
                                ExtractedTransaction(
                                    title = cleanMerchantTitle(description),
                                    amount = amount,
                                    category = category,
                                    paymentMethod = paymentMethod,
                                    date = txTime,
                                    notes = "Raw: $description",
                                    tag = "Personal",
                                    bankName = if (isCreditCard) "" else bankHint,
                                    creditCardName = if (isCreditCard) bankHint else ""
                                )
                            )
                            totalDebits += amount
                        }
                    }
                }
            }
        }

        return StatementParseResult(
            bankName = bankHint,
            transactions = extracted,
            totalDebits = totalDebits,
            totalCredits = 0.0
        )
    }

    private fun inferCategory(text: String): String {
        val t = text.lowercase()
        return when {
            t.contains("swiggy") || t.contains("zomato") || t.contains("eats") || t.contains("cafe") || t.contains("restaurant") -> "Food & Dining"
            t.contains("blinkit") || t.contains("zepto") || t.contains("instamart") || t.contains("dmart") || t.contains("supermarket") -> "Groceries"
            t.contains("amazon") || t.contains("flipkart") || t.contains("myntra") || t.contains("pos") || t.contains("store") -> "Shopping"
            t.contains("uber") || t.contains("ola") || t.contains("fuel") || t.contains("hpcl") || t.contains("bpcl") || t.contains("ioc") || t.contains("petrol") -> "Travel & Transport"
            t.contains("bescom") || t.contains("airtel") || t.contains("jio") || t.contains("electricity") || t.contains("bill") || t.contains("bbps") -> "Bills & Utilities"
            t.contains("netflix") || t.contains("spotify") || t.contains("bookmyshow") || t.contains("pvr") -> "Entertainment"
            t.contains("apollo") || t.contains("pharma") || t.contains("med") || t.contains("hospital") || t.contains("cult") -> "Health & Medical"
            t.contains("zerodha") || t.contains("groww") || t.contains("mf") || t.contains("sip") -> "Investment"
            else -> "Miscellaneous"
        }
    }

    private fun cleanMerchantTitle(raw: String): String {
        return raw.replace(Regex("""UPI/[0-9]+/|UPI-[0-9]+-|POS\s+[0-9]+|NEFT-[0-9]+-"""), "")
            .replace(Regex("""(?i)\b(pvt|ltd|india|bangalore|mumbai|delhi)\b"""), "")
            .trim()
            .take(35)
            .ifEmpty { "Card / Bank Expense" }
    }

    fun generateSampleStatement(bankName: String): StatementParseResult {
        return StatementParseResult(
            bankName = bankName,
            transactions = emptyList(),
            totalDebits = 0.0,
            totalCredits = 0.0
        )
    }
}
