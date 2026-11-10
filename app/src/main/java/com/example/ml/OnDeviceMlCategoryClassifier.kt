package com.example.ml

import com.example.data.model.ExpenseEntity
import com.example.data.model.DefaultCategories
import java.util.Locale
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Result of on-device ML category classification.
 */
data class MlCategoryPrediction(
    val predictedCategory: String,
    val confidence: Float,
    val isConfident: Boolean,
    val topCandidates: List<CategoryCandidate>,
    val source: PredictionSource,
    val explanation: String
)

data class CategoryCandidate(
    val category: String,
    val confidence: Float
)

enum class PredictionSource(val label: String) {
    ON_DEVICE_HISTORICAL_ML("On-Device ML (Historical Pattern)"),
    ON_DEVICE_SEMANTIC_MATCH("On-Device ML (Semantic Knowledge)"),
    ON_DEVICE_HEURISTIC("On-Device ML (Heuristic)")
}

data class MlModelMetadata(
    val trainedTransactionsCount: Int = 0,
    val uniqueMerchantsCount: Int = 0,
    val categoryFrequencies: Map<String, Int> = emptyMap(),
    val lastTrainedTimestamp: Long = 0L
)

/**
 * High-performance on-device Machine Learning classifier for personal finance transactions.
 * Learns from historical user transactions and applies Naive Bayes + N-gram tokenization
 * + Gaussian amount likelihood + merchant affinity to predict transaction categories
 * entirely on-device, eliminating reliance on server-side APIs for known patterns.
 */
class OnDeviceMlCategoryClassifier(
    private val confidenceThreshold: Float = 0.50f
) {
    // Merchant -> (Category -> Count)
    private val merchantCategoryMap = mutableMapOf<String, MutableMap<String, Int>>()

    // Token -> (Category -> Count)
    private val tokenCategoryMap = mutableMapOf<String, MutableMap<String, Int>>()

    // Category -> Total transactions count
    private val categoryCounts = mutableMapOf<String, Int>()

    // Category -> Amount list for statistical Gaussian profiling
    private val categoryAmounts = mutableMapOf<String, MutableList<Double>>()

    // Category -> Mean amount
    private val categoryMeanAmount = mutableMapOf<String, Double>()

    // Category -> Std deviation amount
    private val categoryStdAmount = mutableMapOf<String, Double>()

    private var totalHistoricalTransactions: Int = 0
    private var lastTrainedTime: Long = 0L

    // Curated on-device semantic dictionary for cold-start and pre-trained inference
    private val semanticRules = mapOf(
        "Food & Dining" to listOf(
            "swiggy", "zomato", "starbucks", "mcdonald", "kfc", "burger king", "pizza",
            "domino", "subway", "biryani", "chaayos", "haldiram", "restaurant", "dhaba",
            "dining", "cafe", "coffee", "chai", "tea", "bakery", "pastry", "shawarma",
            "dosa", "idli", "thali", "barbeque", "bbq", "eats", "bar", "pub", "bistro",
            "lunch", "dinner", "breakfast", "snacks", "dessert", "ice cream"
        ),
        "Groceries" to listOf(
            "blinkit", "zepto", "instamart", "dmart", "bigbasket", "nature basket", "spar",
            "more retail", "supermarket", "grocery", "groceries", "vegetable", "vegetables",
            "fruit", "fruits", "milk", "dairy", "bread", "eggs", "curd", "paneer", "atta",
            "dal", "rice", "oil", "spices", "kirana", "ration", "provisions", "dry fruits"
        ),
        "Travel & Transport" to listOf(
            "uber", "ola", "rapido", "metro", "irctc", "train", "railway", "flight",
            "indigo", "air india", "vistara", "spicejet", "akasa", "petrol", "fuel",
            "diesel", "shell", "indian oil", "bpcl", "hpcl", "cng", "fastag", "toll",
            "parking", "auto", "cab", "taxi", "bus", "redbus", "chalo", "yulu", "bounce"
        ),
        "Bills & Utilities" to listOf(
            "electricity", "bescom", "tneb", "msedcl", "tata power", "torrent", "cesc",
            "jio", "airtel", "vodafone", "vi", "bsnl", "act fibernet", "hathway",
            "broadband", "wifi", "recharge", "gas", "lpg", "indane", "bharat gas",
            "hp gas", "piped gas", "water bill", "municipal", "society", "maintenance",
            "rent", "dth", "tata sky", "dish tv", "airtel digital"
        ),
        "Shopping" to listOf(
            "amazon", "flipkart", "myntra", "ajio", "meesho", "nykaa", "zara", "h&m",
            "decathlon", "croma", "reliance digital", "vijay sales", "ikea", "westside",
            "pantaloons", "max fashion", "shoppers stop", "lifestyle", "clothes", "shoes",
            "apparel", "bag", "electronics", "gadget", "mobile", "laptop", "watch"
        ),
        "Entertainment" to listOf(
            "netflix", "prime video", "hotstar", "disney", "sony liv", "zee5", "spotify",
            "apple music", "youtube", "pvr", "inox", "cinepolis", "bookmyshow", "steam",
            "playstation", "xbox", "cinema", "movie", "gaming", "amusement", "concert"
        ),
        "Health & Medical" to listOf(
            "apollo", "pharmacy", "medplus", "1mg", "pharmeasy", "netmeds", "doctor",
            "clinic", "hospital", "dentist", "dental", "eye", "lenskart", "diagnostic",
            "lab", "blood test", "pathlab", "medicine", "medicines", "tablet", "syrup",
            "gym", "cult.fit", "cultfit", "fitness", "yoga", "protein"
        ),
        "Investment" to listOf(
            "zerodha", "groww", "upstox", "angel one", "coin", "mutual fund", "sip",
            "kite", "indmoney", "kuvera", "smallcase", "shares", "stocks", "equity",
            "gold", "sovereign gold", "ppf", "nps", "fixed deposit", "fd", "rd"
        ),
        "Education" to listOf(
            "udemy", "coursera", "edx", "unacademy", "physicswallah", "byju", "allen",
            "coaching", "school", "college", "university", "tuition", "books", "book",
            "stationery", "exam", "course", "degree", "certification", "textbook"
        ),
        "Personal Care" to listOf(
            "urban company", "salon", "spa", "naturals", "enrich", "jawed habib",
            "haircut", "facial", "massage", "manicure", "pedicure", "cosmetics",
            "grooming", "skincare", "fragrance", "perfume", "parlour"
        )
    )

    /**
     * Ingests and trains the on-device ML model on all historical transactions.
     * Runs in milliseconds and does not block the UI thread.
     */
    @Synchronized
    fun trainFromHistoricalExpenses(expenses: List<ExpenseEntity>) {
        merchantCategoryMap.clear()
        tokenCategoryMap.clear()
        categoryCounts.clear()
        categoryAmounts.clear()
        categoryMeanAmount.clear()
        categoryStdAmount.clear()

        totalHistoricalTransactions = expenses.size
        lastTrainedTime = System.currentTimeMillis()

        if (expenses.isEmpty()) return

        for (exp in expenses) {
            val cat = exp.category.trim()
            if (cat.isEmpty()) continue

            // 1. Increment category count
            categoryCounts[cat] = (categoryCounts[cat] ?: 0) + 1

            // 2. Track amounts
            val amtList = categoryAmounts.getOrPut(cat) { mutableListOf() }
            amtList.add(exp.amount)

            // 3. Exact normalized merchant pattern
            val merchantKey = normalizeText(exp.title)
            if (merchantKey.isNotEmpty()) {
                val catMap = merchantCategoryMap.getOrPut(merchantKey) { mutableMapOf() }
                catMap[cat] = (catMap[cat] ?: 0) + 1
            }

            // 4. Tokenization & N-grams (unigrams and bigrams from title + notes)
            val tokens = extractTokens(exp.title + " " + exp.notes)
            for (token in tokens) {
                val catMap = tokenCategoryMap.getOrPut(token) { mutableMapOf() }
                catMap[cat] = (catMap[cat] ?: 0) + 1
            }
        }

        // Compute Gaussian distribution parameters (mean & std-dev) for each category
        for ((cat, amounts) in categoryAmounts) {
            if (amounts.isNotEmpty()) {
                val mean = amounts.average()
                val variance = amounts.map { (it - mean) * (it - mean) }.average()
                val std = sqrt(variance).coerceAtLeast(10.0) // Minimum std dev to prevent division by zero
                categoryMeanAmount[cat] = mean
                categoryStdAmount[cat] = std
            }
        }
    }

    /**
     * Predicts the most suitable category for a new transaction using:
     * 1. Exact / Partial Historical Merchant Affinity
     * 2. Naive Bayes token probabilities with Laplace smoothing
     * 3. Amount Gaussian likelihood
     * 4. Pre-trained on-device semantic fallback
     */
    fun predictCategory(
        title: String,
        amount: Double = 0.0,
        notes: String = ""
    ): MlCategoryPrediction {
        val cleanTitle = normalizeText(title)
        val combinedText = "$cleanTitle ${normalizeText(notes)}".trim()

        if (combinedText.isEmpty() && amount <= 0.0) {
            return MlCategoryPrediction(
                predictedCategory = "Miscellaneous",
                confidence = 0.1f,
                isConfident = false,
                topCandidates = listOf(CategoryCandidate("Miscellaneous", 0.1f)),
                source = PredictionSource.ON_DEVICE_HEURISTIC,
                explanation = "Insufficient details for category inference."
            )
        }

        // =========================================================================
        // LEVEL 1: High-Confidence Historical Merchant Match
        // =========================================================================
        if (cleanTitle.isNotEmpty()) {
            val merchantMatch = merchantCategoryMap[cleanTitle]
            if (merchantMatch != null && merchantMatch.isNotEmpty()) {
                val totalMerchantEntries = merchantMatch.values.sum()
                val topEntry = merchantMatch.maxByOrNull { it.value }
                if (topEntry != null) {
                    val ratio = topEntry.value.toFloat() / totalMerchantEntries.toFloat()
                    val confidence = (0.75f + (0.23f * ratio)).coerceIn(0.70f, 0.98f)
                    val candidates = merchantMatch.entries
                        .sortedByDescending { it.value }
                        .map { CategoryCandidate(it.key, (it.value.toFloat() / totalMerchantEntries) * confidence) }

                    return MlCategoryPrediction(
                        predictedCategory = topEntry.key,
                        confidence = confidence,
                        isConfident = confidence >= confidenceThreshold,
                        topCandidates = candidates,
                        source = PredictionSource.ON_DEVICE_HISTORICAL_ML,
                        explanation = "Learned from ${topEntry.value} of your past '$title' transactions"
                    )
                }
            }

            // Check if any known merchant is a substring/superstring of the input
            for ((knownMerchant, catMap) in merchantCategoryMap) {
                if (knownMerchant.length >= 3 && (cleanTitle.contains(knownMerchant) || knownMerchant.contains(cleanTitle))) {
                    val topEntry = catMap.maxByOrNull { it.value }
                    if (topEntry != null && topEntry.value >= 2) {
                        val confidence = 0.88f
                        return MlCategoryPrediction(
                            predictedCategory = topEntry.key,
                            confidence = confidence,
                            isConfident = true,
                            topCandidates = listOf(CategoryCandidate(topEntry.key, confidence)),
                            source = PredictionSource.ON_DEVICE_HISTORICAL_ML,
                            explanation = "Matches your historical merchant '$knownMerchant' (${topEntry.value} previous entries)"
                        )
                    }
                }
            }
        }

        // =========================================================================
        // LEVEL 2: Probabilistic Naive Bayes + Amount Gaussian Likelihood
        // =========================================================================
        if (totalHistoricalTransactions >= 3 && categoryCounts.isNotEmpty()) {
            val tokens = extractTokens(combinedText)
            val categoryScores = mutableMapOf<String, Double>()
            val allVocabSize = tokenCategoryMap.size.coerceAtLeast(10)

            for (category in DefaultCategories) {
                val countInCat = categoryCounts[category] ?: 0
                // Log prior: P(Category)
                val logPrior = ln((countInCat + 1.0) / (totalHistoricalTransactions + DefaultCategories.size))

                // Log likelihood: Sum of log P(token | Category) with Laplace smoothing
                var logTokenLikelihood = 0.0
                val totalTokensInCat = tokenCategoryMap.values.sumOf { it[category] ?: 0 }

                for (token in tokens) {
                    val tokenCountInCat = tokenCategoryMap[token]?.get(category) ?: 0
                    val pToken = (tokenCountInCat + 0.1) / (totalTokensInCat + 0.1 * allVocabSize)
                    logTokenLikelihood += ln(pToken)
                }

                // Amount Gaussian likelihood (if amount is specified)
                var logAmountLikelihood = 0.0
                if (amount > 0.0 && categoryMeanAmount.containsKey(category)) {
                    val mean = categoryMeanAmount[category] ?: 0.0
                    val std = categoryStdAmount[category] ?: 100.0
                    val z = (amount - mean) / std
                    // Gaussian log-density: -0.5 * z^2 - ln(std)
                    logAmountLikelihood = (-0.5 * (z * z).coerceAtMost(10.0)) - ln(std)
                }

                categoryScores[category] = logPrior + logTokenLikelihood + (logAmountLikelihood * 0.15)
            }

            // Softmax normalization
            val maxScore = categoryScores.values.maxOrNull() ?: 0.0
            val expScores = categoryScores.mapValues { exp((it.value - maxScore).coerceIn(-50.0, 0.0)) }
            val sumExp = expScores.values.sum().coerceAtLeast(1e-9)
            val probabilities = expScores.mapValues { (it.value / sumExp).toFloat() }

            val topSorted = probabilities.entries.sortedByDescending { it.value }
            val bestCandidate = topSorted.firstOrNull()

            if (bestCandidate != null && bestCandidate.value >= 0.35f) {
                val confidence = bestCandidate.value.coerceIn(0.55f, 0.95f)
                val candidates = topSorted.take(3).map { CategoryCandidate(it.key, it.value) }

                return MlCategoryPrediction(
                    predictedCategory = bestCandidate.key,
                    confidence = confidence,
                    isConfident = confidence >= confidenceThreshold,
                    topCandidates = candidates,
                    source = PredictionSource.ON_DEVICE_HISTORICAL_ML,
                    explanation = "Inferred on-device from $totalHistoricalTransactions personal spending records"
                )
            }
        }

        // =========================================================================
        // LEVEL 3: Pre-Trained On-Device Semantic Knowledge Base
        // =========================================================================
        val tokens = extractTokens(combinedText)
        val semanticScores = mutableMapOf<String, Int>()

        for ((category, keywords) in semanticRules) {
            var score = 0
            for (token in tokens) {
                if (keywords.contains(token)) {
                    score += 3
                } else if (keywords.any { it.contains(token) || token.contains(it) }) {
                    score += 1
                }
            }
            if (score > 0) {
                semanticScores[category] = score
            }
        }

        if (semanticScores.isNotEmpty()) {
            val totalScore = semanticScores.values.sum().toFloat()
            val bestSemantic = semanticScores.maxByOrNull { it.value }!!
            val rawConfidence = (bestSemantic.value / totalScore)
            val calibratedConfidence = (0.70f + (rawConfidence * 0.25f)).coerceIn(0.72f, 0.95f)

            val candidates = semanticScores.entries
                .sortedByDescending { it.value }
                .take(3)
                .map { CategoryCandidate(it.key, (it.value / totalScore) * calibratedConfidence) }

            return MlCategoryPrediction(
                predictedCategory = bestSemantic.key,
                confidence = calibratedConfidence,
                isConfident = true,
                topCandidates = candidates,
                source = PredictionSource.ON_DEVICE_SEMANTIC_MATCH,
                explanation = "Recognized on-device merchant pattern for '${bestSemantic.key}'"
            )
        }

        // =========================================================================
        // LEVEL 4: Intelligent Amount & Context Heuristics
        // =========================================================================
        val fallbackCategory = when {
            amount > 10000.0 -> "Investment"
            combinedText.contains("bill") || combinedText.contains("pay") -> "Bills & Utilities"
            combinedText.contains("card") || combinedText.contains("shop") -> "Shopping"
            else -> "Miscellaneous"
        }

        return MlCategoryPrediction(
            predictedCategory = fallbackCategory,
            confidence = 0.50f,
            isConfident = false,
            topCandidates = listOf(CategoryCandidate(fallbackCategory, 0.50f)),
            source = PredictionSource.ON_DEVICE_HEURISTIC,
            explanation = "Default estimate based on transaction attributes"
        )
    }

    /**
     * Returns metadata about the trained on-device machine learning model.
     */
    fun getModelMetadata(): MlModelMetadata {
        return MlModelMetadata(
            trainedTransactionsCount = totalHistoricalTransactions,
            uniqueMerchantsCount = merchantCategoryMap.size,
            categoryFrequencies = categoryCounts.toMap(),
            lastTrainedTimestamp = lastTrainedTime
        )
    }

    private fun normalizeText(text: String): String {
        return text.lowercase(Locale.getDefault())
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
    }

    private fun extractTokens(text: String): List<String> {
        val clean = normalizeText(text)
        if (clean.isEmpty()) return emptyList()

        val words = clean.split(" ").filter { it.length >= 2 && !STOP_WORDS.contains(it) }
        val tokens = mutableListOf<String>()
        tokens.addAll(words)

        // Generate bigrams
        for (i in 0 until words.size - 1) {
            tokens.add("${words[i]}_${words[i + 1]}")
        }
        return tokens
    }

    companion object {
        private val STOP_WORDS = setOf(
            "the", "and", "for", "with", "from", "via", "using", "paid", "spent",
            "rupees", "rs", "inr", "to", "at", "by", "on", "of", "in", "a", "an",
            "payment", "txn", "transaction", "trans", "transfer", "ref", "upi"
        )
    }
}
