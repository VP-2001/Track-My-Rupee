package com.example.ml

import com.example.data.model.ExpenseEntity
import com.example.data.model.DefaultCategories
import java.util.Locale
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Result of on-device ML category & tag classification.
 */
data class MlCategoryPrediction(
    val predictedCategory: String,
    val confidence: Float,
    val isConfident: Boolean,
    val topCandidates: List<CategoryCandidate>,
    val source: PredictionSource,
    val explanation: String,
    val predictedTag: String = "Personal",
    val tagConfidence: Float = 0.85f,
    val isTagConfident: Boolean = true,
    val topTagCandidates: List<TagCandidate> = listOf(TagCandidate("Personal", 0.85f))
)

data class CategoryCandidate(
    val category: String,
    val confidence: Float
)

data class TagCandidate(
    val tag: String,
    val confidence: Float
)

data class TagPredictionResult(
    val predictedTag: String,
    val confidence: Float,
    val isConfident: Boolean,
    val topCandidates: List<TagCandidate>,
    val source: PredictionSource
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
    val tagFrequencies: Map<String, Int> = emptyMap(),
    val lastTrainedTimestamp: Long = 0L
)

val DefaultTags = listOf("Personal", "Work", "Family", "Business", "Travel")

/**
 * High-performance on-device Machine Learning classifier for personal finance transactions.
 * Learns from historical user transactions and applies Naive Bayes + N-gram tokenization
 * + Gaussian amount likelihood + merchant affinity to predict both transaction categories
 * and tags entirely on-device, eliminating reliance on server-side APIs for known patterns.
 */
class OnDeviceMlCategoryClassifier(
    private val confidenceThreshold: Float = 0.50f
) {
    // Merchant -> (Category -> Count)
    private val merchantCategoryMap = mutableMapOf<String, MutableMap<String, Int>>()

    // Merchant -> (Tag -> Count)
    private val merchantTagMap = mutableMapOf<String, MutableMap<String, Int>>()

    // Token -> (Category -> Count)
    private val tokenCategoryMap = mutableMapOf<String, MutableMap<String, Int>>()

    // Token -> (Tag -> Count)
    private val tokenTagMap = mutableMapOf<String, MutableMap<String, Int>>()

    // Category -> Total transactions count
    private val categoryCounts = mutableMapOf<String, Int>()

    // Tag -> Total transactions count
    private val tagCounts = mutableMapOf<String, Int>()

    // Category -> Amount list for statistical Gaussian profiling
    private val categoryAmounts = mutableMapOf<String, MutableList<Double>>()

    // Category -> Mean amount
    private val categoryMeanAmount = mutableMapOf<String, Double>()

    // Category -> Std deviation amount
    private val categoryStdAmount = mutableMapOf<String, Double>()

    private var totalHistoricalTransactions: Int = 0
    private var lastTrainedTime: Long = 0L

    // Curated on-device semantic dictionary for cold-start and pre-trained category inference
    private val semanticRules = mapOf(
        "Food & Dining" to listOf(
            "swiggy", "zomato", "starbucks", "mcdonald", "kfc", "burger king", "pizza",
            "domino", "subway", "biryani", "chaayos", "chai point", "haldiram", "restaurant",
            "dhaba", "dining", "cafe", "coffee", "chai", "tea", "bakery", "pastry", "shawarma",
            "dosa", "idli", "thali", "barbeque", "bbq", "eats", "bar", "pub", "bistro",
            "lunch", "dinner", "breakfast", "snacks", "dessert", "ice cream", "sweet shop",
            "mithai", "saravana bhavan", "bikanervala", "mainland china", "faasos", "behrouz"
        ),
        "Groceries" to listOf(
            "blinkit", "zepto", "instamart", "dmart", "d-mart", "d mart", "bigbasket",
            "nature basket", "spar", "more retail", "supermarket", "grocery", "groceries",
            "vegetable", "vegetables", "fruit", "fruits", "milk", "dairy", "bread", "eggs",
            "curd", "paneer", "atta", "dal", "rice", "oil", "spices", "kirana", "ration",
            "provisions", "dry fruits", "mother dairy", "amul", "reliance fresh", "spencer"
        ),
        "Travel & Transport" to listOf(
            "uber", "ola", "rapido", "metro", "irctc", "train", "railway", "flight",
            "indigo", "air india", "vistara", "spicejet", "akasa", "petrol", "fuel",
            "diesel", "shell", "indian oil", "bpcl", "hpcl", "cng", "fastag", "toll",
            "parking", "auto", "cab", "taxi", "bus", "redbus", "abhibus", "chalo", "yulu",
            "bounce", "makemytrip", "goibibo", "easemytrip", "yatra", "cleartrip"
        ),
        "Bills & Utilities" to listOf(
            "electricity", "bescom", "tneb", "msedcl", "tata power", "torrent", "cesc",
            "jio", "airtel", "vodafone", "vi", "bsnl", "act fibernet", "hathway",
            "broadband", "wifi", "recharge", "gas", "lpg", "indane", "bharat gas",
            "hp gas", "piped gas", "water bill", "municipal", "society", "maintenance",
            "rent", "dth", "tata sky", "tata play", "dish tv", "airtel digital",
            "mygate", "nobroker", "adani electricity"
        ),
        "Shopping" to listOf(
            "amazon", "flipkart", "myntra", "ajio", "meesho", "nykaa", "zara", "h&m",
            "decathlon", "croma", "reliance digital", "vijay sales", "ikea", "westside",
            "pantaloons", "max fashion", "shoppers stop", "lifestyle", "clothes", "shoes",
            "apparel", "bag", "electronics", "gadget", "mobile", "laptop", "watch",
            "tata cliq", "uniqlo", "lenskart", "titan", "fastrack", "boat", "noise"
        ),
        "Entertainment" to listOf(
            "netflix", "prime video", "hotstar", "disney", "sony liv", "zee5", "spotify",
            "apple music", "youtube", "pvr", "inox", "cinepolis", "bookmyshow", "steam",
            "playstation", "xbox", "cinema", "movie", "gaming", "amusement", "concert",
            "gaana", "wynk", "imagica", "wonderla"
        ),
        "Health & Medical" to listOf(
            "apollo", "apollo pharmacy", "pharmacy", "medplus", "1mg", "pharmeasy",
            "netmeds", "wellness forever", "doctor", "clinic", "hospital", "dentist",
            "dental", "eye", "lenskart", "diagnostic", "lab", "blood test", "pathlab",
            "medicine", "medicines", "tablet", "syrup", "medical store", "druggist",
            "gym", "cult.fit", "cultfit", "fitness", "yoga", "protein"
        ),
        "Investment" to listOf(
            "zerodha", "groww", "upstox", "angel one", "coin", "mutual fund", "sip",
            "kite", "indmoney", "kuvera", "smallcase", "shares", "stocks", "equity",
            "gold", "sovereign gold", "ppf", "nps", "fixed deposit", "fd", "rd",
            "paytm money", "motilal oswal"
        ),
        "Education" to listOf(
            "udemy", "coursera", "edx", "unacademy", "physicswallah", "byju", "allen",
            "coaching", "school", "college", "university", "tuition", "books", "book",
            "stationery", "exam", "course", "degree", "certification", "textbook",
            "crossword", "sapna book"
        ),
        "Personal Care" to listOf(
            "urban company", "salon", "spa", "naturals", "enrich", "jawed habib",
            "haircut", "facial", "massage", "manicure", "pedicure", "cosmetics",
            "grooming", "skincare", "fragrance", "perfume", "parlour", "geetanjali"
        )
    )

    // Curated on-device semantic dictionary for tag inference
    private val semanticTagRules = mapOf(
        "Work" to listOf(
            "wework", "aws", "amazon web services", "google cloud", "gcp", "azure", "slack",
            "zoom", "github", "gitlab", "linkedin", "microsoft", "office", "workspace",
            "coworking", "trello", "jira", "adobe", "figma", "notion", "canva", "courier",
            "blue dart", "fedex", "dhl", "printer", "godaddy", "domain", "hosting",
            "digitalocean", "zoho", "freshworks", "consulting", "client", "business lunch",
            "meeting", "stationery", "paper"
        ),
        "Business" to listOf(
            "wholesale", "inventory", "vendor", "supplier", "freelance", "incorporation",
            "ca fees", "legal", "gst", "trademark", "accounting", "tally", "zoho books",
            "indiamart", "justdial", "b2b", "trade", "enterprise", "commercial", "raw material"
        ),
        "Family" to listOf(
            "dmart", "d-mart", "d mart", "reliance fresh", "spencer", "kirana", "supermarket",
            "groceries", "milk", "dairy", "mother dairy", "amul", "bigbasket", "bb daily",
            "firstcry", "baby", "pediatric", "school", "tuition", "kids", "toys", "furniture",
            "ikea", "pepperfry", "urban ladder", "kitchen", "ration", "society maintenance",
            "apartment maintenance", "parents", "home needs", "home centre", "grocery"
        ),
        "Travel" to listOf(
            "uber", "ola", "rapido", "irctc", "railway", "train", "flight", "airline",
            "indigo", "air india", "vistara", "spicejet", "akasa", "makemytrip", "goibibo",
            "easemytrip", "yatra", "cleartrip", "hotel", "resort", "airbnb", "booking.com",
            "agoda", "fastag", "toll", "parking", "petrol", "diesel", "fuel", "shell",
            "cng", "redbus", "abhibus", "taxi", "cab", "tour", "trip"
        ),
        "Personal" to listOf(
            "swiggy", "zomato", "starbucks", "cafe", "mcdonald", "kfc", "burger king",
            "subway", "domino", "pizza", "netflix", "spotify", "gym", "cult.fit", "cultfit",
            "salon", "spa", "haircut", "zara", "h&m", "myntra", "nykaa", "cinema", "pvr",
            "inox", "bookmyshow", "gaming", "steam", "playstation", "personal", "dessert"
        )
    )

    /**
     * Ingests and trains the on-device ML model on historical transactions.
     * Continuously learns merchant-category, merchant-tag, and token affinities.
     */
    @Synchronized
    fun trainFromHistoricalExpenses(expenses: List<ExpenseEntity>) {
        merchantCategoryMap.clear()
        merchantTagMap.clear()
        tokenCategoryMap.clear()
        tokenTagMap.clear()
        categoryCounts.clear()
        tagCounts.clear()
        categoryAmounts.clear()
        categoryMeanAmount.clear()
        categoryStdAmount.clear()

        totalHistoricalTransactions = expenses.size
        lastTrainedTime = System.currentTimeMillis()

        if (expenses.isEmpty()) return

        for (exp in expenses) {
            val cat = exp.category.trim()
            val tag = exp.tag.trim().ifEmpty { "Personal" }

            if (cat.isNotEmpty()) {
                categoryCounts[cat] = (categoryCounts[cat] ?: 0) + 1
                val amtList = categoryAmounts.getOrPut(cat) { mutableListOf() }
                amtList.add(exp.amount)
            }
            if (tag.isNotEmpty()) {
                tagCounts[tag] = (tagCounts[tag] ?: 0) + 1
            }

            // 3. Exact normalized merchant pattern
            val merchantKey = normalizeText(exp.title)
            if (merchantKey.isNotEmpty()) {
                if (cat.isNotEmpty()) {
                    val catMap = merchantCategoryMap.getOrPut(merchantKey) { mutableMapOf() }
                    catMap[cat] = (catMap[cat] ?: 0) + 1
                }
                if (tag.isNotEmpty()) {
                    val tagMap = merchantTagMap.getOrPut(merchantKey) { mutableMapOf() }
                    tagMap[tag] = (tagMap[tag] ?: 0) + 1
                }
            }

            // 4. Tokenization & N-grams (unigrams and bigrams from title + notes)
            val tokens = extractTokens(exp.title + " " + exp.notes)
            for (token in tokens) {
                if (cat.isNotEmpty()) {
                    val catMap = tokenCategoryMap.getOrPut(token) { mutableMapOf() }
                    catMap[cat] = (catMap[cat] ?: 0) + 1
                }
                if (tag.isNotEmpty()) {
                    val tagMap = tokenTagMap.getOrPut(token) { mutableMapOf() }
                    tagMap[tag] = (tagMap[tag] ?: 0) + 1
                }
            }
        }

        // Compute Gaussian distribution parameters (mean & std-dev) for each category
        for ((cat, amounts) in categoryAmounts) {
            if (amounts.isNotEmpty()) {
                val mean = amounts.average()
                val variance = amounts.map { (it - mean) * (it - mean) }.average()
                val std = sqrt(variance).coerceAtLeast(10.0)
                categoryMeanAmount[cat] = mean
                categoryStdAmount[cat] = std
            }
        }
    }

    /**
     * Predicts both category and tag for a transaction based on shop name, amount, and notes.
     */
    fun predictCategory(
        title: String,
        amount: Double = 0.0,
        notes: String = ""
    ): MlCategoryPrediction {
        val cleanTitle = normalizeText(title)
        val combinedText = "$cleanTitle ${normalizeText(notes)}".trim()

        if (combinedText.isEmpty() && amount <= 0.0) {
            val defaultTagRes = predictTag("", "Miscellaneous", amount, notes)
            return MlCategoryPrediction(
                predictedCategory = "Miscellaneous",
                confidence = 0.1f,
                isConfident = false,
                topCandidates = listOf(CategoryCandidate("Miscellaneous", 0.1f)),
                source = PredictionSource.ON_DEVICE_HEURISTIC,
                explanation = "Enter shop name to auto-predict category & tags.",
                predictedTag = defaultTagRes.predictedTag,
                tagConfidence = defaultTagRes.confidence,
                isTagConfident = defaultTagRes.isConfident,
                topTagCandidates = defaultTagRes.topCandidates
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

                    val tagRes = predictTag(title, topEntry.key, amount, notes)

                    return MlCategoryPrediction(
                        predictedCategory = topEntry.key,
                        confidence = confidence,
                        isConfident = confidence >= confidenceThreshold,
                        topCandidates = candidates,
                        source = PredictionSource.ON_DEVICE_HISTORICAL_ML,
                        explanation = "Learned from ${topEntry.value} of your past '$title' transactions",
                        predictedTag = tagRes.predictedTag,
                        tagConfidence = tagRes.confidence,
                        isTagConfident = tagRes.isConfident,
                        topTagCandidates = tagRes.topCandidates
                    )
                }
            }

            // Check if any known merchant is a substring/superstring of the input
            for ((knownMerchant, catMap) in merchantCategoryMap) {
                if (knownMerchant.length >= 3 && (cleanTitle.contains(knownMerchant) || knownMerchant.contains(cleanTitle))) {
                    val topEntry = catMap.maxByOrNull { it.value }
                    if (topEntry != null && topEntry.value >= 2) {
                        val confidence = 0.88f
                        val tagRes = predictTag(title, topEntry.key, amount, notes)
                        return MlCategoryPrediction(
                            predictedCategory = topEntry.key,
                            confidence = confidence,
                            isConfident = true,
                            topCandidates = listOf(CategoryCandidate(topEntry.key, confidence)),
                            source = PredictionSource.ON_DEVICE_HISTORICAL_ML,
                            explanation = "Matches your historical merchant '$knownMerchant' (${topEntry.value} entries)",
                            predictedTag = tagRes.predictedTag,
                            tagConfidence = tagRes.confidence,
                            isTagConfident = tagRes.isConfident,
                            topTagCandidates = tagRes.topCandidates
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
                val logPrior = ln((countInCat + 1.0) / (totalHistoricalTransactions + DefaultCategories.size))

                var logTokenLikelihood = 0.0
                val totalTokensInCat = tokenCategoryMap.values.sumOf { it[category] ?: 0 }

                for (token in tokens) {
                    val tokenCountInCat = tokenCategoryMap[token]?.get(category) ?: 0
                    val pToken = (tokenCountInCat + 0.1) / (totalTokensInCat + 0.1 * allVocabSize)
                    logTokenLikelihood += ln(pToken)
                }

                var logAmountLikelihood = 0.0
                if (amount > 0.0 && categoryMeanAmount.containsKey(category)) {
                    val mean = categoryMeanAmount[category] ?: 0.0
                    val std = categoryStdAmount[category] ?: 100.0
                    val z = (amount - mean) / std
                    logAmountLikelihood = (-0.5 * (z * z).coerceAtMost(10.0)) - ln(std)
                }

                categoryScores[category] = logPrior + logTokenLikelihood + (logAmountLikelihood * 0.15)
            }

            val maxScore = categoryScores.values.maxOrNull() ?: 0.0
            val expScores = categoryScores.mapValues { exp((it.value - maxScore).coerceIn(-50.0, 0.0)) }
            val sumExp = expScores.values.sum().coerceAtLeast(1e-9)
            val probabilities = expScores.mapValues { (it.value / sumExp).toFloat() }

            val topSorted = probabilities.entries.sortedByDescending { it.value }
            val bestCandidate = topSorted.firstOrNull()

            if (bestCandidate != null && bestCandidate.value >= 0.35f) {
                val confidence = bestCandidate.value.coerceIn(0.55f, 0.95f)
                val candidates = topSorted.take(3).map { CategoryCandidate(it.key, it.value) }
                val tagRes = predictTag(title, bestCandidate.key, amount, notes)

                return MlCategoryPrediction(
                    predictedCategory = bestCandidate.key,
                    confidence = confidence,
                    isConfident = confidence >= confidenceThreshold,
                    topCandidates = candidates,
                    source = PredictionSource.ON_DEVICE_HISTORICAL_ML,
                    explanation = "Inferred from $totalHistoricalTransactions spending records",
                    predictedTag = tagRes.predictedTag,
                    tagConfidence = tagRes.confidence,
                    isTagConfident = tagRes.isConfident,
                    topTagCandidates = tagRes.topCandidates
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
                    score += 4
                } else if (keywords.any { it.contains(token) || token.contains(it) }) {
                    score += 2
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
            val calibratedConfidence = (0.75f + (rawConfidence * 0.22f)).coerceIn(0.75f, 0.96f)

            val candidates = semanticScores.entries
                .sortedByDescending { it.value }
                .take(3)
                .map { CategoryCandidate(it.key, (it.value / totalScore) * calibratedConfidence) }

            val tagRes = predictTag(title, bestSemantic.key, amount, notes)

            return MlCategoryPrediction(
                predictedCategory = bestSemantic.key,
                confidence = calibratedConfidence,
                isConfident = true,
                topCandidates = candidates,
                source = PredictionSource.ON_DEVICE_SEMANTIC_MATCH,
                explanation = "Recognized shop pattern for '${bestSemantic.key}'",
                predictedTag = tagRes.predictedTag,
                tagConfidence = tagRes.confidence,
                isTagConfident = tagRes.isConfident,
                topTagCandidates = tagRes.topCandidates
            )
        }

        // =========================================================================
        // LEVEL 4: Intelligent Amount & Context Heuristics
        // =========================================================================
        val fallbackCategory = when {
            amount > 10000.0 -> "Investment"
            combinedText.contains("bill") || combinedText.contains("pay") -> "Bills & Utilities"
            combinedText.contains("card") || combinedText.contains("shop") || combinedText.contains("store") -> "Shopping"
            else -> "Miscellaneous"
        }

        val fallbackTagRes = predictTag(title, fallbackCategory, amount, notes)

        return MlCategoryPrediction(
            predictedCategory = fallbackCategory,
            confidence = 0.50f,
            isConfident = false,
            topCandidates = listOf(CategoryCandidate(fallbackCategory, 0.50f)),
            source = PredictionSource.ON_DEVICE_HEURISTIC,
            explanation = "Default estimate based on transaction attributes",
            predictedTag = fallbackTagRes.predictedTag,
            tagConfidence = fallbackTagRes.confidence,
            isTagConfident = fallbackTagRes.isConfident,
            topTagCandidates = fallbackTagRes.topCandidates
        )
    }

    /**
     * Predicts the most suitable tag (Personal, Work, Family, Business, Travel)
     * using merchant affinity, token probability, and semantic rules.
     */
    fun predictTag(
        title: String,
        predictedCategory: String,
        amount: Double = 0.0,
        notes: String = ""
    ): TagPredictionResult {
        val cleanTitle = normalizeText(title)
        val combinedText = "$cleanTitle ${normalizeText(notes)}".trim()
        val tokens = extractTokens(combinedText)

        // 1. Exact/Substring historical merchant affinity for tags
        if (cleanTitle.isNotEmpty()) {
            val merchantMatch = merchantTagMap[cleanTitle]
            if (merchantMatch != null && merchantMatch.isNotEmpty()) {
                val total = merchantMatch.values.sum().toFloat()
                val topEntry = merchantMatch.maxByOrNull { it.value }
                if (topEntry != null) {
                    val conf = (0.78f + 0.20f * (topEntry.value / total)).coerceIn(0.75f, 0.98f)
                    val candidates = merchantMatch.entries
                        .sortedByDescending { it.value }
                        .map { TagCandidate(it.key, (it.value / total) * conf) }
                    return TagPredictionResult(
                        predictedTag = topEntry.key,
                        confidence = conf,
                        isConfident = true,
                        topCandidates = candidates,
                        source = PredictionSource.ON_DEVICE_HISTORICAL_ML
                    )
                }
            }

            for ((knownMerchant, tMap) in merchantTagMap) {
                if (knownMerchant.length >= 3 && (cleanTitle.contains(knownMerchant) || knownMerchant.contains(cleanTitle))) {
                    val topEntry = tMap.maxByOrNull { it.value }
                    if (topEntry != null && topEntry.value >= 2) {
                        return TagPredictionResult(
                            predictedTag = topEntry.key,
                            confidence = 0.88f,
                            isConfident = true,
                            topCandidates = listOf(TagCandidate(topEntry.key, 0.88f)),
                            source = PredictionSource.ON_DEVICE_HISTORICAL_ML
                        )
                    }
                }
            }
        }

        // 2. Probabilistic Naive Bayes on user tokens for tags
        if (totalHistoricalTransactions >= 3 && tagCounts.isNotEmpty()) {
            val tagScores = mutableMapOf<String, Double>()
            val vocabSize = tokenTagMap.size.coerceAtLeast(10)
            for (tag in DefaultTags) {
                val countInTag = tagCounts[tag] ?: 0
                val logPrior = ln((countInTag + 1.0) / (totalHistoricalTransactions + DefaultTags.size))
                var logLikelihood = 0.0
                val totalTokensInTag = tokenTagMap.values.sumOf { it[tag] ?: 0 }
                for (token in tokens) {
                    val tokenCountInTag = tokenTagMap[token]?.get(tag) ?: 0
                    val p = (tokenCountInTag + 0.1) / (totalTokensInTag + 0.1 * vocabSize)
                    logLikelihood += ln(p)
                }
                tagScores[tag] = logPrior + logLikelihood
            }
            val maxScore = tagScores.values.maxOrNull() ?: 0.0
            val expScores = tagScores.mapValues { exp((it.value - maxScore).coerceIn(-50.0, 0.0)) }
            val sumExp = expScores.values.sum().coerceAtLeast(1e-9)
            val probs = expScores.mapValues { (it.value / sumExp).toFloat() }
            val topSorted = probs.entries.sortedByDescending { it.value }
            val best = topSorted.firstOrNull()
            if (best != null && best.value >= 0.45f) {
                val conf = best.value.coerceIn(0.60f, 0.92f)
                return TagPredictionResult(
                    predictedTag = best.key,
                    confidence = conf,
                    isConfident = true,
                    topCandidates = topSorted.map { TagCandidate(it.key, it.value) },
                    source = PredictionSource.ON_DEVICE_HISTORICAL_ML
                )
            }
        }

        // 3. Semantic Knowledge Base for Shop Tags
        val semanticTagScores = mutableMapOf<String, Int>()
        for ((tag, keywords) in semanticTagRules) {
            var score = 0
            for (token in tokens) {
                if (keywords.contains(token)) {
                    score += 4
                } else if (keywords.any { it.contains(token) || token.contains(it) }) {
                    score += 2
                }
            }
            if (score > 0) {
                semanticTagScores[tag] = score
            }
        }
        if (semanticTagScores.isNotEmpty()) {
            val totalScore = semanticTagScores.values.sum().toFloat()
            val best = semanticTagScores.maxByOrNull { it.value }!!
            val conf = (0.75f + (best.value / totalScore) * 0.20f).coerceIn(0.75f, 0.95f)
            val candidates = semanticTagScores.entries
                .sortedByDescending { it.value }
                .map { TagCandidate(it.key, (it.value / totalScore) * conf) }
            return TagPredictionResult(
                predictedTag = best.key,
                confidence = conf,
                isConfident = true,
                topCandidates = candidates,
                source = PredictionSource.ON_DEVICE_SEMANTIC_MATCH
            )
        }

        // 4. Category-to-Tag Correlation Prior
        val (fallbackTag, fallbackConf) = when (predictedCategory) {
            "Travel & Transport" -> "Travel" to 0.88f
            "Groceries" -> "Family" to 0.82f
            "Bills & Utilities" -> "Family" to 0.78f
            "Education" -> "Family" to 0.72f
            else -> "Personal" to 0.80f
        }

        return TagPredictionResult(
            predictedTag = fallbackTag,
            confidence = fallbackConf,
            isConfident = true,
            topCandidates = listOf(
                TagCandidate(fallbackTag, fallbackConf),
                TagCandidate("Personal", 0.60f),
                TagCandidate("Work", 0.40f)
            ),
            source = PredictionSource.ON_DEVICE_HEURISTIC
        )
    }

    /**
     * Alias for predictCategory that emphasizes both category and tag output.
     */
    fun predictTransaction(
        title: String,
        amount: Double = 0.0,
        notes: String = ""
    ): MlCategoryPrediction = predictCategory(title, amount, notes)

    /**
     * Returns metadata about the trained on-device machine learning model.
     */
    fun getModelMetadata(): MlModelMetadata {
        return MlModelMetadata(
            trainedTransactionsCount = totalHistoricalTransactions,
            uniqueMerchantsCount = merchantCategoryMap.size,
            categoryFrequencies = categoryCounts.toMap(),
            tagFrequencies = tagCounts.toMap(),
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
