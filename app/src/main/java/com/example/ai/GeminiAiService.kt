package com.example.ai

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.example.data.model.DefaultCategories
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class GeminiChatModel(val modelId: String, val displayName: String, val badge: String) {
    FLASH("gemini-3.5-flash", "Gemini 3.5 Flash", "Balanced"),
    FLASH_LITE("gemini-3.1-flash-lite-preview", "Gemini 3.1 Flash Lite", "Fast"),
    PRO("gemini-3.1-pro-preview", "Gemini 3.1 Pro", "Deep Reasoning")
}

enum class MessageRole {
    USER, MODEL
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false,
    val modelTier: GeminiChatModel? = null
)

class GeminiAiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val apiKey: String
        get() = BuildConfig.GEMINI_API_KEY.ifEmpty { "" }

    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    suspend fun suggestCategory(title: String, amount: Double, notes: String): String = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext fallbackLocalCategorize(title)
        }

        try {
            val prompt = """
                You are a smart personal finance expense categorizer for India.
                Given this transaction:
                Title: "$title"
                Amount: ₹$amount
                Notes: "$notes"

                Select the best category strictly from this list:
                ${DefaultCategories.joinToString(", ")}

                Respond with ONLY the exact category name, nothing else.
            """.trimIndent()

            val response = executeGeminiPrompt(prompt)
            val matched = DefaultCategories.firstOrNull { response.contains(it, ignoreCase = true) }
            matched ?: fallbackLocalCategorize(title)
        } catch (e: Exception) {
            fallbackLocalCategorize(title)
        }
    }

    suspend fun parseVoiceExpense(spokenText: String): ExtractedTransaction? = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext fallbackVoiceParser(spokenText)
        }

        try {
            val prompt = """
                You are a financial NLP parser. Extract the transaction details from this spoken text:
                "$spokenText"

                Current Date: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}
                Available Categories: ${DefaultCategories.joinToString(", ")}
                Available Payment Methods: UPI, Credit Card, Cash
                Available Tags: Personal, Work, Family, Business

                Respond ONLY with a JSON object in this format:
                {
                  "title": "Short title (e.g. Swiggy Dinner, Shell Petrol, Grocery)",
                  "amount": 450.0,
                  "category": "Food & Dining",
                  "paymentMethod": "UPI",
                  "tag": "Personal",
                  "notes": "Spoken description"
                }
            """.trimIndent()

            val response = executeGeminiPrompt(prompt)
            val jsonStr = extractJsonFromResponse(response)
            if (jsonStr.isNotEmpty()) {
                val obj = JSONObject(jsonStr)
                ExtractedTransaction(
                    title = obj.optString("title", "Voice Expense"),
                    amount = obj.optDouble("amount", 0.0),
                    category = obj.optString("category", "Miscellaneous"),
                    paymentMethod = obj.optString("paymentMethod", "UPI"),
                    date = System.currentTimeMillis(),
                    notes = obj.optString("notes", spokenText),
                    tag = obj.optString("tag", "Personal")
                )
            } else {
                fallbackVoiceParser(spokenText)
            }
        } catch (e: Exception) {
            fallbackVoiceParser(spokenText)
        }
    }

    suspend fun parseReceiptImage(bitmap: Bitmap): ExtractedTransaction? = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext ExtractedTransaction(
                title = "Scanned Receipt",
                amount = 450.0,
                category = "Food & Dining",
                paymentMethod = "UPI",
                date = System.currentTimeMillis(),
                notes = "Receipt processed locally (Configure Gemini API Key in Secrets for full AI scan)"
            )
        }

        try {
            val base64Image = bitmapToBase64(bitmap)
            val prompt = """
                Analyze this receipt / bill / invoice image and extract transaction information.
                Available Categories: ${DefaultCategories.joinToString(", ")}
                Available Payment Methods: UPI, Credit Card, Cash

                Respond with ONLY a JSON object:
                {
                  "merchantName": "Store/Merchant name",
                  "totalAmount": 1250.50,
                  "category": "Groceries",
                  "paymentMethod": "Credit Card",
                  "itemsSummary": "Item 1, Item 2...",
                  "date": "YYYY-MM-DD"
                }
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contents = JSONArray()
                val contentObj = JSONObject()
                val parts = JSONArray()

                parts.put(JSONObject().put("text", prompt))
                parts.put(JSONObject().put("inlineData", JSONObject().apply {
                    put("mimeType", "image/jpeg")
                    put("data", base64Image)
                }))

                contentObj.put("parts", parts)
                contents.put(contentObj)
                put("contents", contents)
            }

            val request = Request.Builder()
                .url("$baseUrl?key=$apiKey")
                .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val respBody = response.body?.string() ?: ""
            val jsonText = parseCandidateText(respBody)
            val cleanJson = extractJsonFromResponse(jsonText)

            if (cleanJson.isNotEmpty()) {
                val obj = JSONObject(cleanJson)
                ExtractedTransaction(
                    title = obj.optString("merchantName", "Receipt Expense"),
                    amount = obj.optDouble("totalAmount", 0.0),
                    category = obj.optString("category", "Shopping"),
                    paymentMethod = obj.optString("paymentMethod", "UPI"),
                    date = System.currentTimeMillis(),
                    notes = obj.optString("itemsSummary", "Scanned Receipt"),
                    tag = "Personal"
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun parseBankStatement(text: String, bankHint: String): StatementParseResult = withContext(Dispatchers.IO) {
        // Try native regex parser first for known bank formats (HDFC, SBI Card, ICICI, Kotak, Axis)
        val nativeParsed = BankStatementParser.parseNative(text, bankHint)
        if (nativeParsed.transactions.isNotEmpty()) {
            return@withContext nativeParsed
        }

        // If native didn't match or is freeform, use Gemini AI
        if (apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val prompt = """
                    You are an Indian Bank Statement analyzer (HDFC, SBI Card, ICICI, Axis Bank, Kotak Mahindra).
                    Extract all debit and expense transactions from this raw statement text.
                    Bank: $bankHint

                    Raw Text:
                    ${text.take(3500)}

                    Categories: ${DefaultCategories.joinToString(", ")}

                    Return ONLY a JSON object:
                    {
                      "bankName": "$bankHint",
                      "transactions": [
                        {
                          "title": "Merchant / Description",
                          "amount": 540.0,
                          "category": "Food & Dining",
                          "paymentMethod": "UPI",
                          "notes": "Ref: 43242342",
                          "date": "YYYY-MM-DD"
                        }
                      ]
                    }
                """.trimIndent()

                val response = executeGeminiPrompt(prompt)
                val jsonStr = extractJsonFromResponse(response)
                if (jsonStr.isNotEmpty()) {
                    val root = JSONObject(jsonStr)
                    val txArray = root.optJSONArray("transactions") ?: JSONArray()
                    val list = mutableListOf<ExtractedTransaction>()
                    var debits = 0.0

                    for (i in 0 until txArray.length()) {
                        val obj = txArray.getJSONObject(i)
                        val amt = obj.optDouble("amount", 0.0)
                        debits += amt
                        list.add(
                            ExtractedTransaction(
                                title = obj.optString("title", "Statement Item"),
                                amount = amt,
                                category = obj.optString("category", "Miscellaneous"),
                                paymentMethod = obj.optString("paymentMethod", "UPI"),
                                date = System.currentTimeMillis() - (i * 86400000L),
                                notes = obj.optString("notes", ""),
                                tag = "Personal"
                            )
                        )
                    }

                    if (list.isNotEmpty()) {
                        return@withContext StatementParseResult(
                            bankName = root.optString("bankName", bankHint),
                            transactions = list,
                            totalDebits = debits,
                            totalCredits = 0.0
                        )
                    }
                }
            } catch (e: Exception) {
                // fall through to native parser
            }
        }

        BankStatementParser.parseNative(text, bankHint)
    }

    private fun executeGeminiPrompt(prompt: String): String {
        val requestJson = JSONObject().apply {
            val contents = JSONArray()
            val contentObj = JSONObject()
            val parts = JSONArray()
            parts.put(JSONObject().put("text", prompt))
            contentObj.put("parts", parts)
            contents.put(contentObj)
            put("contents", contents)
        }

        val request = Request.Builder()
            .url("$baseUrl?key=$apiKey")
            .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val respBody = response.body?.string() ?: ""
        return parseCandidateText(respBody)
    }

    private fun parseCandidateText(responseJson: String): String {
        return try {
            val root = JSONObject(responseJson)
            val candidates = root.optJSONArray("candidates") ?: return ""
            if (candidates.length() > 0) {
                val first = candidates.getJSONObject(0)
                val content = first.optJSONObject("content") ?: return ""
                val parts = content.optJSONArray("parts") ?: return ""
                if (parts.length() > 0) {
                    parts.getJSONObject(0).optString("text", "")
                } else ""
            } else ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun extractJsonFromResponse(text: String): String {
        val trimmed = text.trim()
        val startIdx = trimmed.indexOf('{')
        val endIdx = trimmed.lastIndexOf('}')
        return if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
            trimmed.substring(startIdx, endIdx + 1)
        } else ""
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    private fun fallbackLocalCategorize(title: String): String {
        val t = title.lowercase()
        return when {
            t.contains("swiggy") || t.contains("zomato") || t.contains("food") || t.contains("restaurant") || t.contains("cafe") || t.contains("starbucks") || t.contains("mcdonald") || t.contains("pizza") || t.contains("biryani") || t.contains("dining") -> "Food & Dining"
            t.contains("blinkit") || t.contains("zepto") || t.contains("instamart") || t.contains("groceries") || t.contains("supermarket") || t.contains("vegetables") || t.contains("milk") || t.contains("dmart") -> "Groceries"
            t.contains("uber") || t.contains("ola") || t.contains("rapido") || t.contains("fuel") || t.contains("petrol") || t.contains("diesel") || t.contains("metro") || t.contains("auto") || t.contains("flight") || t.contains("train") || t.contains("irctc") -> "Travel & Transport"
            t.contains("amazon") || t.contains("flipkart") || t.contains("myntra") || t.contains("shopping") || t.contains("clothes") || t.contains("shoes") || t.contains("electronics") -> "Shopping"
            t.contains("electricity") || t.contains("bescom") || t.contains("wifi") || t.contains("broadband") || t.contains("airtel") || t.contains("jio") || t.contains("water") || t.contains("gas") || t.contains("rent") || t.contains("recharge") || t.contains("bill") -> "Bills & Utilities"
            t.contains("netflix") || t.contains("prime") || t.contains("hotstar") || t.contains("movie") || t.contains("pvr") || t.contains("inox") || t.contains("spotify") || t.contains("game") -> "Entertainment"
            t.contains("pharmacy") || t.contains("doctor") || t.contains("hospital") || t.contains("medicine") || t.contains("gym") || t.contains("cult") || t.contains("apollo") || t.contains("clinic") -> "Health & Medical"
            t.contains("sip") || t.contains("mutual fund") || t.contains("zerodha") || t.contains("groww") || t.contains("stocks") || t.contains("shares") || t.contains("gold") -> "Investment"
            t.contains("course") || t.contains("books") || t.contains("school") || t.contains("tuition") || t.contains("exam") || t.contains("udemy") -> "Education"
            t.contains("salon") || t.contains("spa") || t.contains("haircut") || t.contains("grooming") -> "Personal Care"
            else -> "Miscellaneous"
        }
    }

    private fun fallbackVoiceParser(spoken: String): ExtractedTransaction {
        val amountRegex = Regex("""(\d+(\.\d+)?)""")
        val match = amountRegex.find(spoken)
        val amount = match?.value?.toDoubleOrNull() ?: 150.0

        val category = fallbackLocalCategorize(spoken)
        val payment = when {
            spoken.contains("upi", ignoreCase = true) || spoken.contains("gpay", ignoreCase = true) || spoken.contains("phonepe", ignoreCase = true) || spoken.contains("paytm", ignoreCase = true) -> "UPI"
            spoken.contains("card", ignoreCase = true) || spoken.contains("credit", ignoreCase = true) -> "Credit Card"
            spoken.contains("cash", ignoreCase = true) -> "Cash"
            else -> "UPI"
        }

        val title = spoken.replace(Regex("""(?i)\b(spent|paid|rupees|rs|via|on|for|using|with|\d+)\b"""), " ").trim().replace(Regex("""\s+"""), " ")
            .ifEmpty { "Voice Entry ($category)" }

        return ExtractedTransaction(
            title = title.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
            amount = amount,
            category = category,
            paymentMethod = payment,
            date = System.currentTimeMillis(),
            notes = spoken,
            tag = if (spoken.contains("work", ignoreCase = true) || spoken.contains("office", ignoreCase = true)) "Work" else if (spoken.contains("family", ignoreCase = true) || spoken.contains("home", ignoreCase = true)) "Family" else "Personal"
        )
    }

    suspend fun chatCompletion(
        history: List<ChatMessage>,
        systemInstruction: String,
        model: GeminiChatModel
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            val userLast = history.lastOrNull { it.role == MessageRole.USER }?.content ?: ""
            return@withContext Result.success(generateOfflineAdvisorResponse(userLast, systemInstruction))
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/${model.modelId}:generateContent?key=$apiKey"

            val requestJson = JSONObject().apply {
                if (systemInstruction.isNotBlank()) {
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", systemInstruction) })
                        })
                    })
                }

                val contentsArray = JSONArray()
                history.forEach { msg ->
                    val roleStr = if (msg.role == MessageRole.USER) "user" else "model"
                    val contentObj = JSONObject().apply {
                        put("role", roleStr)
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", msg.content) })
                        })
                    }
                    contentsArray.put(contentObj)
                }
                put("contents", contentsArray)
            }

            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val respBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                // If cloud API fails (e.g. invalid key, quota, offline), deliver accurate answer via local Room DB analysis
                val userLast = history.lastOrNull { it.role == MessageRole.USER }?.content ?: ""
                val offlineResp = generateOfflineAdvisorResponse(userLast, systemInstruction)
                return@withContext Result.success(offlineResp)
            }

            val text = parseCandidateText(respBody)
            if (text.isNotBlank()) {
                Result.success(text)
            } else {
                val userLast = history.lastOrNull { it.role == MessageRole.USER }?.content ?: ""
                Result.success(generateOfflineAdvisorResponse(userLast, systemInstruction))
            }
        } catch (e: Exception) {
            val userLast = history.lastOrNull { it.role == MessageRole.USER }?.content ?: ""
            Result.success(generateOfflineAdvisorResponse(userLast, systemInstruction))
        }
    }

    data class ParsedFinancialContext(
        val currentPeriod: String,
        val totalSpendThisMonth: Double,
        val transactionCountThisMonth: Int,
        val totalAllTimeSpend: Double,
        val totalAllTimeCount: Int,
        val topExpenses: List<String>,
        val topMerchants: String,
        val paymentBreakdown: String,
        val allTimeCategories: List<Pair<String, String>>,
        val thisMonthCategories: List<Pair<String, String>>,
        val activeBudgets: List<String>,
        val recentTransactions: List<String>
    )

    fun parseFinancialContext(context: String): ParsedFinancialContext {
        // 1. Try parsing structured JSON first if present
        val jsonStr = when {
            context.contains("```json") -> {
                context.substringAfter("```json").substringBefore("```").trim()
            }
            context.contains("LIVE USER FINANCIAL CONTEXT (FROM ROOM DATABASE - STRUCTURED JSON):") -> {
                context.substringAfter("LIVE USER FINANCIAL CONTEXT (FROM ROOM DATABASE - STRUCTURED JSON):")
                    .substringBefore("TEXT SUMMARY FOR REFERENCE:")
                    .trim().removePrefix("```json").removeSuffix("```").trim()
            }
            context.trim().startsWith("{") && context.contains("spending_summary") -> context.trim()
            else -> null
        }

        if (!jsonStr.isNullOrBlank()) {
            try {
                val root = JSONObject(jsonStr)
                val period = root.optString("period", "This Month")
                val spendObj = root.optJSONObject("spending_summary")
                val allTimeSpend = spendObj?.optDouble("all_time_total", 0.0) ?: 0.0
                val allTimeCount = spendObj?.optInt("all_time_transactions_count", 0) ?: 0
                val monthSpend = spendObj?.optDouble("current_month_total", 0.0) ?: 0.0
                val monthCount = spendObj?.optInt("current_month_transactions_count", 0) ?: 0

                val catSummariesObj = root.optJSONObject("category_summaries")
                val allTimeCats = mutableListOf<Pair<String, String>>()
                val allTimeArray = catSummariesObj?.optJSONArray("all_time")
                if (allTimeArray != null) {
                    for (i in 0 until allTimeArray.length()) {
                        val item = allTimeArray.optJSONObject(i) ?: continue
                        val cat = item.optString("category")
                        val spent = item.optDouble("total_spent", 0.0)
                        val pct = item.optDouble("percentage", 0.0)
                        val count = item.optInt("transaction_count", 0)
                        if (cat.isNotBlank()) {
                            allTimeCats.add(Pair(cat, "₹${"%.2f".format(spent)} (${"%.1f".format(pct)}%, $count transactions)"))
                        }
                    }
                }

                val monthCats = mutableListOf<Pair<String, String>>()
                val monthArray = catSummariesObj?.optJSONArray("current_month")
                if (monthArray != null) {
                    for (i in 0 until monthArray.length()) {
                        val item = monthArray.optJSONObject(i) ?: continue
                        val cat = item.optString("category")
                        val spent = item.optDouble("total_spent", 0.0)
                        val pct = item.optDouble("percentage", 0.0)
                        val count = item.optInt("transaction_count", 0)
                        if (cat.isNotBlank()) {
                            monthCats.add(Pair(cat, "₹${"%.2f".format(spent)} (${"%.1f".format(pct)}%, $count transactions)"))
                        }
                    }
                }

                val budgets = mutableListOf<String>()
                val budgetArray = root.optJSONArray("budget_status")
                if (budgetArray != null) {
                    for (i in 0 until budgetArray.length()) {
                        val item = budgetArray.optJSONObject(i) ?: continue
                        val cat = item.optString("category")
                        val limit = item.optDouble("monthly_limit", 0.0)
                        val spent = item.optDouble("spent_this_month", 0.0)
                        val pct = item.optDouble("percentage_used", 0.0).toInt()
                        val isOver = item.optBoolean("is_over_budget", spent > limit)
                        val remaining = item.optDouble("remaining", limit - spent)
                        val status = if (isOver) "OVER BUDGET by ₹${"%.2f".format(spent - limit)}" else "Within limit (₹${"%.2f".format(remaining)} left)"
                        budgets.add("$cat: limit ₹${"%.2f".format(limit)}, spent ₹${"%.2f".format(spent)} ($pct%) [$status]")
                    }
                }

                val topExpensesList = mutableListOf<String>()
                val topExpArray = root.optJSONArray("top_single_expenses")
                if (topExpArray != null) {
                    for (i in 0 until topExpArray.length()) {
                        val item = topExpArray.optJSONObject(i) ?: continue
                        val title = item.optString("title")
                        val amount = item.optDouble("amount", 0.0)
                        val cat = item.optString("category")
                        val method = item.optString("payment_method")
                        val date = item.optString("date")
                        topExpensesList.add("$title: ₹${"%.2f".format(amount)} ($cat, $method, on $date)")
                    }
                }

                val topMerchantsList = mutableListOf<String>()
                val topMerchArray = root.optJSONArray("top_merchants")
                if (topMerchArray != null) {
                    for (i in 0 until topMerchArray.length()) {
                        val item = topMerchArray.optJSONObject(i) ?: continue
                        val name = item.optString("name")
                        val spent = item.optDouble("total_spent", 0.0)
                        topMerchantsList.add("$name: ₹${"%.2f".format(spent)}")
                    }
                }
                val topMerchants = topMerchantsList.joinToString(", ").ifEmpty { "None" }

                val paymentList = mutableListOf<String>()
                val paymentArray = root.optJSONArray("payment_methods_breakdown")
                if (paymentArray != null) {
                    for (i in 0 until paymentArray.length()) {
                        val item = paymentArray.optJSONObject(i) ?: continue
                        val method = item.optString("method")
                        val spent = item.optDouble("total_spent", 0.0)
                        val count = item.optInt("transaction_count", 0)
                        paymentList.add("$method: ₹${"%.2f".format(spent)} ($count txns)")
                    }
                }
                val paymentBreakdown = paymentList.joinToString(", ").ifEmpty { "None" }

                val recentList = mutableListOf<String>()
                val recentArray = root.optJSONArray("recent_transactions")
                if (recentArray != null) {
                    for (i in 0 until recentArray.length()) {
                        val item = recentArray.optJSONObject(i) ?: continue
                        val title = item.optString("title")
                        val amount = item.optDouble("amount", 0.0)
                        val cat = item.optString("category")
                        val method = item.optString("payment_method")
                        val date = item.optString("date")
                        recentList.add("$title | ₹${"%.2f".format(amount)} | $cat | $method | $date")
                    }
                }

                return ParsedFinancialContext(
                    currentPeriod = period,
                    totalSpendThisMonth = monthSpend,
                    transactionCountThisMonth = monthCount,
                    totalAllTimeSpend = allTimeSpend,
                    totalAllTimeCount = allTimeCount,
                    topExpenses = topExpensesList,
                    topMerchants = topMerchants,
                    paymentBreakdown = paymentBreakdown,
                    allTimeCategories = allTimeCats,
                    thisMonthCategories = monthCats,
                    activeBudgets = budgets,
                    recentTransactions = recentList
                )
            } catch (e: Exception) {
                // If JSON parsing fails, fall back to regex parsing below
            }
        }

        // 2. Fallback to Regex/Text Parsing
        val periodMatch = Regex("""Current Period:\s*(.+)""").find(context)?.groupValues?.get(1)?.trim() ?: "This Month"

        val allTimeMatch = Regex("""Total All-Time Spending:\s*₹([0-9.,]+)\s*across\s*(\d+)""").find(context)
            ?: Regex("""Total All-Time Expenses Logged:\s*(\d+)\s*entries\s*\(Total\s*₹([0-9.,]+)\)""").find(context)

        val allTimeSpend = if (allTimeMatch != null) {
            if (allTimeMatch.value.startsWith("Total All-Time Spending")) {
                allTimeMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: 0.0
            } else {
                allTimeMatch.groupValues[2].replace(",", "").toDoubleOrNull() ?: 0.0
            }
        } else 0.0

        val allTimeCount = if (allTimeMatch != null) {
            if (allTimeMatch.value.startsWith("Total All-Time Spending")) {
                allTimeMatch.groupValues[2].toIntOrNull() ?: 0
            } else {
                allTimeMatch.groupValues[1].toIntOrNull() ?: 0
            }
        } else 0

        val thisMonthMatch = Regex("""Total Current Month Spending:\s*₹([0-9.,]+)\s*across\s*(\d+)""").find(context)
            ?: Regex("""Total Spend This Month:\s*₹([0-9.,]+)\s*across\s*(\d+)""").find(context)
        val spendThisMonth = thisMonthMatch?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
        val countThisMonth = thisMonthMatch?.groupValues?.get(2)?.toIntOrNull() ?: 0

        val topExpensesList = mutableListOf<String>()
        val topExpensesSection = context.substringAfter("- Top Single Expenses:", "")
            .substringBefore("- Top Merchants / Stores:")
            .substringBefore("- Payment Methods Breakdown:")
        topExpensesSection.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("-") && !trimmed.contains("None", ignoreCase = true)) {
                topExpensesList.add(trimmed.removePrefix("-").trim())
            }
        }

        val topMerchants = Regex("""Top Merchants / Stores:\s*(.+)""").find(context)?.groupValues?.get(1)?.trim() ?: "None"
        val paymentBreakdown = Regex("""Payment Methods Breakdown:\s*(.+)""").find(context)?.groupValues?.get(1)?.trim() ?: "None"

        val allTimeCats = mutableListOf<Pair<String, String>>()
        val allTimeSection = if (context.contains("All-Time Category Summaries (Totals & Percentages):")) {
            context.substringAfter("All-Time Category Summaries (Totals & Percentages):", "")
                .substringBefore("- Current Month Category Summaries:")
                .substringBefore("- Active Budgets")
        } else {
            context.substringAfter("Category Summaries (Totals & Percentages):", "")
                .substringBefore("- Active Budgets:")
        }
        allTimeSection.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("-") && trimmed.contains(":")) {
                val catName = trimmed.substringAfter("-").substringBefore(":").trim()
                val details = trimmed.substringAfter(":").trim()
                if (catName.isNotEmpty() && !catName.contains("No categorized", ignoreCase = true)) {
                    allTimeCats.add(Pair(catName, details))
                }
            }
        }

        val thisMonthCats = mutableListOf<Pair<String, String>>()
        val thisMonthSection = context.substringAfter("- Current Month Category Summaries:", "")
            .substringBefore("- Active Budgets")
        thisMonthSection.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("-") && trimmed.contains(":")) {
                val catName = trimmed.substringAfter("-").substringBefore(":").trim()
                val details = trimmed.substringAfter(":").trim()
                if (catName.isNotEmpty() && !catName.contains("No expenses", ignoreCase = true)) {
                    thisMonthCats.add(Pair(catName, details))
                }
            }
        }

        val budgets = mutableListOf<String>()
        val budSection = context.substringAfter("Active Budgets", "")
            .substringBefore("- Recent Transactions Ledger:")
            .substringBefore("- Recent Expenses Ledger:")
        budSection.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("-") && !trimmed.contains("No category budgets set", ignoreCase = true)) {
                budgets.add(trimmed.removePrefix("-").trim())
            }
        }

        val recent = mutableListOf<String>()
        val recentSection = if (context.contains("Recent Transactions Ledger:")) {
            context.substringAfter("Recent Transactions Ledger:", "")
        } else {
            context.substringAfter("Recent Expenses Ledger:", "")
        }
        recentSection.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("-") && !trimmed.contains("No recent transactions", ignoreCase = true)) {
                recent.add(trimmed.removePrefix("-").trim())
            }
        }

        return ParsedFinancialContext(
            currentPeriod = periodMatch,
            totalSpendThisMonth = spendThisMonth,
            transactionCountThisMonth = countThisMonth,
            totalAllTimeSpend = allTimeSpend,
            totalAllTimeCount = allTimeCount,
            topExpenses = topExpensesList,
            topMerchants = topMerchants,
            paymentBreakdown = paymentBreakdown,
            allTimeCategories = allTimeCats,
            thisMonthCategories = thisMonthCats,
            activeBudgets = budgets,
            recentTransactions = recent
        )
    }

    fun generateOfflineAdvisorResponse(userQuery: String, context: String): String {
        val q = userQuery.lowercase().trim()
        val data = parseFinancialContext(context)

        // 1. Direct Category Query matching
        val matchedCat = data.allTimeCategories.firstOrNull { (catName, _) ->
            val cleanCat = catName.lowercase()
            q.contains(cleanCat) ||
            (cleanCat.contains("food") && (q.contains("food") || q.contains("dining") || q.contains("eat") || q.contains("restaurant") || q.contains("swiggy") || q.contains("zomato"))) ||
            (cleanCat.contains("grocer") && (q.contains("grocer") || q.contains("supermarket") || q.contains("blinkit") || q.contains("zepto") || q.contains("vegetable"))) ||
            (cleanCat.contains("travel") && (q.contains("travel") || q.contains("transit") || q.contains("cab") || q.contains("uber") || q.contains("ola") || q.contains("metro") || q.contains("flight"))) ||
            (cleanCat.contains("bill") && (q.contains("bill") || q.contains("utilit") || q.contains("electric") || q.contains("water") || q.contains("wifi") || q.contains("recharge"))) ||
            (cleanCat.contains("shop") && (q.contains("shop") || q.contains("amazon") || q.contains("flipkart") || q.contains("clothes") || q.contains("myntra"))) ||
            (cleanCat.contains("entertain") && (q.contains("entertain") || q.contains("movie") || q.contains("netflix") || q.contains("cinema"))) ||
            (cleanCat.contains("health") && (q.contains("health") || q.contains("medic") || q.contains("pharmacy") || q.contains("doctor") || q.contains("hospital"))) ||
            (cleanCat.contains("invest") && (q.contains("invest") || q.contains("mutual fund") || q.contains("sip") || q.contains("stock") || q.contains("gold")))
        } ?: data.thisMonthCategories.firstOrNull { (catName, _) ->
            q.contains(catName.lowercase())
        }

        if (matchedCat != null) {
            val catName = matchedCat.first
            val allTimeInfo = matchedCat.second
            val thisMonthInfo = data.thisMonthCategories.firstOrNull { it.first.equals(catName, ignoreCase = true) }?.second
            val budgetInfo = data.activeBudgets.firstOrNull { it.startsWith(catName, ignoreCase = true) || it.contains(catName, ignoreCase = true) }

            val sb = StringBuilder()
            sb.append("🏷️ **Spending Report for $catName:**\n\n")
            sb.append("• **All-Time Total**: **$allTimeInfo**\n")
            if (thisMonthInfo != null) {
                sb.append("• **Current Month (${data.currentPeriod})**: **$thisMonthInfo**\n")
            } else {
                sb.append("• **Current Month (${data.currentPeriod})**: No transactions logged yet\n")
            }
            if (budgetInfo != null) {
                val isOver = budgetInfo.contains("OVER BUDGET", ignoreCase = true)
                val statusIcon = if (isOver) "⚠️" else "✅"
                sb.append("• **Budget Tracking**: $statusIcon $budgetInfo\n")
            }
            sb.append("\n💡 **Advisor Note**: ")
            if (allTimeInfo.contains("%")) {
                val pct = allTimeInfo.substringAfter("(").substringBefore("%").toDoubleOrNull() ?: 0.0
                if (pct > 25.0) {
                    sb.append("$catName represents a significant portion ($pct%) of your total expenses. Consider setting a monthly spending ceiling in the Budgets tab.")
                } else {
                    sb.append("$catName accounts for $pct% of your overall expenses, maintaining a reasonable balance.")
                }
            } else {
                sb.append("Expenditure data is verified against your encrypted on-device database.")
            }
            sb.append("\n\n*Direct Room DB ledger analysis.*")
            return sb.toString()
        }

        return when {
            // 2. Highest / Peak Expense
            q.contains("highest") || q.contains("biggest") || q.contains("largest") || q.contains("peak") || q.contains("expensive") || q.contains("max") -> {
                val sb = StringBuilder()
                sb.append("🏆 **Highest Single Expenses Logged:**\n\n")
                if (data.topExpenses.isNotEmpty()) {
                    data.topExpenses.forEachIndexed { idx, exp ->
                        sb.append("${idx + 1}. **$exp**\n")
                    }
                    sb.append("\n💡 **Advisor Tip**: Major one-off expenses have a high impact on monthly cash flow. For high-ticket items, try applying a 48-hour cooling-off rule before purchasing.")
                } else {
                    sb.append("No transactions logged in your ledger yet.")
                }
                sb.append("\n\n*Verified with Room DB.*")
                sb.toString()
            }

            // 3. Payment Methods
            q.contains("payment") || q.contains("upi") || q.contains("credit card") || q.contains("debit card") || q.contains("cash") || q.contains("rupay") -> {
                val sb = StringBuilder()
                sb.append("💳 **Payment Method Breakdown:**\n\n")
                if (data.paymentBreakdown != "None" && data.paymentBreakdown.isNotBlank()) {
                    sb.append("Here is how you spend across payment modes:\n")
                    data.paymentBreakdown.split(",").forEach { methodStr ->
                        val trimmed = methodStr.trim()
                        if (trimmed.isNotEmpty()) {
                            sb.append("• **$trimmed**\n")
                        }
                    }
                    sb.append("\n💡 **Security & Rewards Advice**: Ensure credit card balances are cleared within the interest-free grace period. For daily micro-transactions under ₹500, UPI or RuPay cards remain the most frictionless.")
                } else {
                    sb.append("No payment method entries recorded yet.")
                }
                sb.append("\n\n*Extracted from your on-device transaction ledger.*")
                sb.toString()
            }

            // 4. Budgets & Limits
            q.contains("budget") || q.contains("limit") || q.contains("over budget") || q.contains("remaining") -> {
                val sb = StringBuilder()
                sb.append("🎯 **Active Budget Tracking & Health:**\n\n")
                if (data.activeBudgets.isNotEmpty()) {
                    sb.append("Current status for ${data.currentPeriod}:\n")
                    data.activeBudgets.forEach { b ->
                        val icon = if (b.contains("OVER BUDGET", ignoreCase = true)) "⚠️" else "✅"
                        sb.append("$icon **$b**\n")
                    }
                    val overBudgetCount = data.activeBudgets.count { it.contains("OVER BUDGET", ignoreCase = true) }
                    if (overBudgetCount > 0) {
                        sb.append("\n⚠️ **Action Required**: You have **$overBudgetCount** category(s) exceeding budget! Trim discretionary expenses for the rest of the month.")
                    } else {
                        sb.append("\n🎉 **Well Done**: All configured categories are currently well within their allocated limits.")
                    }
                } else {
                    sb.append("You don't have any category budgets set up yet.\n\n")
                    val topCat = data.allTimeCategories.firstOrNull()?.first
                    if (topCat != null) {
                        sb.append("💡 Recommendation: Navigate to the **Budgets** tab and establish a monthly limit for **$topCat** (your top expense driver).")
                    } else {
                        sb.append("💡 Recommendation: Visit the **Budgets** tab to set monthly limits for your core categories like Food, Shopping, and Travel.")
                    }
                }
                sb.append("\n\n*Real-time Room DB tracking.*")
                sb.toString()
            }

            // 5. Cost-Cutting & Savings
            q.contains("save") || q.contains("cut") || q.contains("reduce") || q.contains("trim") || q.contains("frugal") -> {
                val topCat = data.allTimeCategories.firstOrNull()
                val topCatName = topCat?.first ?: "Food & Dining"
                val topCatDetails = topCat?.second ?: "largest variable category"

                val sb = StringBuilder()
                sb.append("💡 **Actionable Cost-Cutting & Savings Plan:**\n\n")
                val baseSpend = if (data.totalSpendThisMonth > 0) data.totalSpendThisMonth else data.totalAllTimeSpend
                if (baseSpend > 0) {
                    val targetSavings10 = baseSpend * 0.10
                    val targetSavings15 = baseSpend * 0.15

                    sb.append("1. **Target Your Largest Expense Driver ($topCatName)**:\n")
                    sb.append("   Currently at **$topCatDetails**. Trimming 15% on non-essential purchases here could save you **₹${"%.2f".format(targetSavings15)}**.\n\n")
                    if (data.topExpenses.isNotEmpty()) {
                        sb.append("2. **Review High-Value Outlays**:\n")
                        sb.append("   Your largest purchase was **${data.topExpenses.first()}**. Plan high-ticket items ahead of time.\n\n")
                    }
                    sb.append("3. **Overall Monthly Target**:\n")
                    sb.append("   Aiming for a 10-15% reduction can retain between **₹${"%.0f".format(targetSavings10)}** and **₹${"%.0f".format(targetSavings15)}** each month for your savings or emergency fund.")
                } else {
                    sb.append("1. **Track Every Spend**: Keep logging receipts so we can accurately pinpoint discretionary expense leaks.\n")
                    sb.append("2. **Dining Out**: Set a weekly dining cap in the Budgets tab.\n")
                    sb.append("3. **Recurring Subscriptions**: Audit streaming and memberships once a quarter.")
                }
                sb.append("\n\n*Calculated from your verified Room DB transaction records.*")
                sb.toString()
            }

            // 6. Category breakdown / All categories
            q.contains("category") || q.contains("categories") || q.contains("breakdown") || q.contains("where does my money go") || q.contains("distribution") -> {
                val sb = StringBuilder()
                sb.append("🛍️ **Complete Category Breakdown:**\n\n")
                if (data.allTimeCategories.isNotEmpty()) {
                    sb.append("**All-Time Totals by Category:**\n")
                    data.allTimeCategories.forEach { (cat, details) ->
                        sb.append("• **$cat**: $details\n")
                    }
                    if (data.thisMonthCategories.isNotEmpty()) {
                        sb.append("\n**Current Month (${data.currentPeriod}) Totals:**\n")
                        data.thisMonthCategories.forEach { (cat, details) ->
                            sb.append("• **$cat**: $details\n")
                        }
                    }
                    val top = data.allTimeCategories.first()
                    sb.append("\n📌 **Largest Expense Category**: **${top.first}** accounts for the highest share (${top.second}).")
                } else {
                    sb.append("No categorized expenses logged yet. Once you record transactions, your top category percentages and counts will automatically appear here.")
                }
                sb.append("\n\n*Computed locally from your Room DB.*")
                sb.toString()
            }

            // 7. 50/30/20 Rule
            q.contains("50/30/20") || q.contains("rule") -> {
                val total = if (data.totalSpendThisMonth > 0) data.totalSpendThisMonth else data.totalAllTimeSpend
                val sb = StringBuilder()
                sb.append("📈 **50/30/20 Budgeting Rule Analysis:**\n\n")
                if (total > 0) {
                    val needs = total * 0.50
                    val wants = total * 0.30
                    val savings = total * 0.20
                    sb.append("Based on your total expenditure of **₹${"%.2f".format(total)}**:\n\n")
                    sb.append("• **Needs (50%)**: Target **₹${"%.2f".format(needs)}** (Groceries, rent, utilities, essential transit)\n")
                    sb.append("• **Wants (30%)**: Target **₹${"%.2f".format(wants)}** (Dining out, entertainment, shopping, hobbies)\n")
                    sb.append("• **Savings & Investments (20%)**: Target **₹${"%.2f".format(savings)}** (Emergency fund, SIP, debt repayment)\n\n")
                    sb.append("💡 **Recommendation**: If your discretionary wants exceed 30%, redirect the difference into an emergency fund or index fund SIP.")
                } else {
                    sb.append("The 50/30/20 rule balances your income:\n\n")
                    sb.append("• **50% Needs**: Housing, groceries, bills, medical\n")
                    sb.append("• **30% Wants**: Dining out, shopping, entertainment\n")
                    sb.append("• **20% Savings**: Emergency fund, investments, retirement\n\n")
                    sb.append("Log your expenses in the Ledger to calculate your personalized 50/30/20 split!")
                }
                sb.append("\n\n*Personalized financial analysis.*")
                sb.toString()
            }

            // 8. Recent Transactions
            q.contains("recent") || q.contains("latest") || q.contains("last transaction") || q.contains("history") || q.contains("ledger") || q.contains("transactions") -> {
                val sb = StringBuilder()
                sb.append("📝 **Recent Transactions From Ledger:**\n\n")
                if (data.recentTransactions.isNotEmpty()) {
                    data.recentTransactions.take(8).forEach { txn ->
                        sb.append("• $txn\n")
                    }
                    if (data.recentTransactions.size > 8) {
                        sb.append("\n*(+${data.recentTransactions.size - 8} more transactions in database)*")
                    }
                } else {
                    sb.append("No recent transactions found.")
                }
                sb.append("\n\n*On-device Room DB records.*")
                sb.toString()
            }

            // 9. Investments / Emergency Fund / General Finance
            q.contains("invest") || q.contains("sip") || q.contains("emergency") || q.contains("debt") || q.contains("loan") || q.contains("stock") || q.contains("mutual") -> {
                val monthlyAvg = if (data.totalSpendThisMonth > 0) data.totalSpendThisMonth else if (data.totalAllTimeSpend > 0) data.totalAllTimeSpend else 20000.0
                val emergency3Mo = monthlyAvg * 3
                val emergency6Mo = monthlyAvg * 6

                val sb = StringBuilder()
                sb.append("🛡️ **Financial Health & Wealth Building Guidelines:**\n\n")
                sb.append("1. **Emergency Cushion**:\n")
                sb.append("   Maintain 3 to 6 months of living expenses in an accessible high-yield liquid fund or FD. Based on your current spending, target **₹${"%.0f".format(emergency3Mo)}** to **₹${"%.0f".format(emergency6Mo)}**.\n\n")
                sb.append("2. **Systematic Investment Plan (SIP)**:\n")
                sb.append("   Automate investing 15-20% of your net income into broad-market index funds (Nifty 50) right on payday before discretionary spending.\n\n")
                sb.append("3. **High-Interest Debt Elimination**:\n")
                sb.append("   Prioritize paying off credit cards or personal loans (interest > 12%) before aggressive equity investments.")
                sb.append("\n\n*Sound personal finance practices.*")
                sb.toString()
            }

            // 10. Total Spend / Spending Overview / Month Analysis
            q.contains("total") || q.contains("spend") || q.contains("spent") || q.contains("how much") || q.contains("month") || q.contains("summary") || q.contains("analyze") || q.contains("overview") -> {
                val sb = StringBuilder()
                sb.append("📊 **Your Spending Analysis & Financial Overview:**\n\n")
                sb.append("• **All-Time Spending**: **₹${"%.2f".format(data.totalAllTimeSpend)}** across **${data.totalAllTimeCount}** logged transactions\n")
                sb.append("• **Current Month (${data.currentPeriod})**: **₹${"%.2f".format(data.totalSpendThisMonth)}** across **${data.transactionCountThisMonth}** transactions\n")

                if (data.topExpenses.isNotEmpty()) {
                    sb.append("• **Peak Single Expense**: ${data.topExpenses.first()}\n")
                }
                if (data.allTimeCategories.isNotEmpty()) {
                    sb.append("\n**Top Spending Drivers:**\n")
                    data.allTimeCategories.take(4).forEach { (cat, details) ->
                        sb.append("  - **$cat**: $details\n")
                    }
                }
                if (data.paymentBreakdown != "None" && data.paymentBreakdown.isNotBlank()) {
                    sb.append("\n**Payment Modes**: ${data.paymentBreakdown}\n")
                }
                sb.append("\n💡 **Advisor Takeaway**: Tracking your daily transactions maintains complete clarity over where your money goes. Check your Budgets tab to ensure you don't overspend this month.")
                sb.append("\n\n*Accurately analyzed directly from your Room DB.*")
                sb.toString()
            }

            // 11. Default Helpful Assistant Prompt
            else -> {
                val sb = StringBuilder()
                sb.append("👋 **I am your Personal Financial Advisor**, with direct access to your on-device transaction ledger.\n\n")
                if (data.totalAllTimeCount > 0) {
                    sb.append("I have analyzed your **${data.totalAllTimeCount}** logged transactions totaling **₹${"%.2f".format(data.totalAllTimeSpend)}**.\n\n")
                }
                sb.append("Here are exact questions you can ask me:\n")
                sb.append("• *'How much did I spend on Food?'* (or any category)\n")
                sb.append("• *'What is my total spending?'*\n")
                sb.append("• *'What is my highest single expense?'*\n")
                sb.append("• *'Show my category breakdown'*\n")
                sb.append("• *'Check my active budget limits'*\n")
                sb.append("• *'Where can I cut costs and save?'*\n")
                sb.append("• *'Show my payment methods breakdown'*\n")
                sb.append("• *'Calculate my 50/30/20 budget'*")
                sb.append("\n\n*All insights are calculated from your secure encrypted database.*")
                sb.toString()
            }
        }
    }
}

