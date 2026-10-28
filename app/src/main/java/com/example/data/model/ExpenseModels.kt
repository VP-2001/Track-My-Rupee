package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val category: String = "Miscellaneous",
    val paymentMethod: String = "UPI",
    val notes: String = "",
    val tag: String = "Personal",
    val receiptUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "LOCAL", // LOCAL, SYNCED, PENDING
    val bankName: String = "",
    val creditCardName: String = ""
) {
    val formattedAmount: String
        get() = formatInr(amount)

    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            return sdf.format(Date(date))
        }

    val formattedTime: String
        get() {
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            return sdf.format(Date(date))
        }

    companion object {
        fun formatInr(amount: Double): String {
            val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
            return formatter.format(amount).replace("INR", "₹").trim()
        }
    }
}

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey
    val category: String,
    val monthlyLimit: Double,
    val monthYear: String = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
)

@Entity(tableName = "sync_logs")
data class SyncLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val action: String, // "CLOUD_BACKUP", "CLOUD_RESTORE", "AUTO_SYNC"
    val itemCount: Int,
    val status: String, // "SUCCESS", "FAILED"
    val accountEmail: String = "local_account",
    val details: String = ""
)

data class CategorySummary(
    val category: String,
    val totalAmount: Double,
    val transactionCount: Int,
    val percentage: Float = 0f
)

data class PaymentMethodSummary(
    val paymentMethod: String,
    val totalAmount: Double,
    val transactionCount: Int,
    val percentage: Float = 0f
)

data class MonthlySpendingHabitsSummary(
    val currentMonthName: String,
    val currentMonthTotal: Double,
    val currentMonthTxCount: Int,
    val currentMonthDailyAverage: Double,
    val currentMonthDaysElapsed: Int,
    val currentMonthDaysTotal: Int,
    val previousMonthName: String,
    val previousMonthTotal: Double,
    val previousMonthTxCount: Int,
    val differenceAmount: Double, // currentMonthTotal - previousMonthTotal
    val percentageChange: Double, // ((current - previous) / previous) * 100
    val isSpendingIncreased: Boolean,
    val isPreviousMonthAvailable: Boolean,
    val topCategory: CategorySummary?,
    val topCategories: List<CategorySummary>,
    val topPaymentMethod: PaymentMethodSummary?,
    val paymentBreakdown: List<PaymentMethodSummary>,
    val highestSingleExpense: ExpenseEntity?,
    val activeSpendingDaysCount: Int,
    val projectedMonthEndTotal: Double,
    val budgetStatus: MonthlyBudgetHabitStatus?,
    val habitInsightMessage: String
)

data class MonthlyBudgetHabitStatus(
    val totalBudget: Double,
    val spentAmount: Double,
    val remainingBudget: Double,
    val percentageUsed: Float,
    val isOverBudget: Boolean
)

data class DailySpending(
    val date: String,
    val timestamp: Long,
    val amount: Double
)

enum class PaymentMethod(val displayName: String, val iconName: String) {
    UPI("UPI", "ic_upi"),
    CREDIT_CARD("Credit Card", "ic_card"),
    RUPAY_CREDIT_CARD("RuPay Credit Card", "ic_rupay"),
    DEBIT_CARD("Debit Card", "ic_debit"),
    NET_BANKING("Net Banking", "ic_bank"),
    CASH("Cash", "ic_cash")
}

data class CreditCardSuggestion(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val issuerBank: String,
    val isRuPay: Boolean = false
)

enum class ExpenseTag(val displayName: String) {
    PERSONAL("Personal"),
    WORK("Work"),
    FAMILY("Family"),
    TRAVEL("Travel"),
    BUSINESS("Business")
}

val DefaultCategories = listOf(
    "Food & Dining",
    "Groceries",
    "Shopping",
    "Bills & Utilities",
    "Travel & Transport",
    "Entertainment",
    "Health & Medical",
    "Investment",
    "Education",
    "Personal Care",
    "Miscellaneous"
)

enum class ComparisonViewMode(val label: String) {
    BY_MONTH("By Month"),
    BY_CATEGORY("By Category"),
    BY_MERCHANT("By Merchant")
}

data class CategoryMonthlySpend(
    val category: String,
    val amount: Double,
    val count: Int,
    val percentage: Float
)

data class MerchantMonthlySpend(
    val merchant: String,
    val amount: Double,
    val count: Int,
    val percentage: Float
)

data class MonthlyBreakdownItem(
    val monthKey: String,
    val monthName: String,
    val shortName: String,
    val year: Int,
    val month: Int,
    val totalAmount: Double,
    val transactionCount: Int,
    val categorySpends: List<CategoryMonthlySpend>,
    val merchantSpends: List<MerchantMonthlySpend>,
    val topCategory: CategoryMonthlySpend?,
    val topMerchant: MerchantMonthlySpend?
)

data class CategoryCrossMonthItem(
    val category: String,
    val totalAllMonths: Double,
    val monthlySpends: Map<String, Double>,
    val peakMonthKey: String,
    val peakMonthName: String,
    val peakMonthAmount: Double
)

data class MerchantCrossMonthItem(
    val merchant: String,
    val totalAllMonths: Double,
    val monthlySpends: Map<String, Double>,
    val peakMonthKey: String,
    val peakMonthName: String,
    val peakMonthAmount: Double
)

data class MonthlyComparisonReport(
    val months: List<MonthlyBreakdownItem>,
    val categoryCrossMonth: List<CategoryCrossMonthItem>,
    val merchantCrossMonth: List<MerchantCrossMonthItem>,
    val highestSpendMonth: MonthlyBreakdownItem?,
    val lowestSpendMonth: MonthlyBreakdownItem?,
    val averageMonthlySpend: Double,
    val totalAllTimeSpend: Double,
    val overallTopCategory: String?,
    val overallTopMerchant: String?
)

data class MonthlyTrendData(
    val monthKey: String,
    val shortMonth: String,
    val fullMonthName: String,
    val year: Int,
    val month: Int,
    val totalSpent: Double,
    val transactionCount: Int,
    val momChangePercent: Double,
    val isIncreased: Boolean
)

data class SixMonthGrowthSummary(
    val totalSpent6Months: Double,
    val averageMonthlySpend: Double,
    val latestMonthSpend: Double,
    val firstMonthSpend: Double,
    val overallGrowthPercent: Double,
    val isSpendingDecreased: Boolean,
    val highestSpendMonth: String,
    val lowestSpendMonth: String,
    val trendPoints: List<MonthlyTrendData>
)

data class OneYearGrowthSummary(
    val totalSpent1Year: Double,
    val averageMonthlySpend: Double,
    val latestMonthSpend: Double,
    val firstMonthSpend: Double,
    val overallGrowthPercent: Double,
    val isSpendingDecreased: Boolean,
    val highestSpendMonth: String,
    val lowestSpendMonth: String,
    val trendPoints: List<MonthlyTrendData>
)

data class MonthlyBudgetTrendPoint(
    val monthKey: String,
    val shortMonth: String,
    val fullMonthName: String,
    val year: Int,
    val month: Int,
    val totalSpent: Double,
    val budgetLimit: Double,
    val isOverBudget: Boolean,
    val variance: Double, // budgetLimit - totalSpent (> 0: savings, < 0: overrun)
    val budgetUsedPercent: Float,
    val transactionCount: Int,
    val momChangePercent: Double,
    val isIncreased: Boolean,
    val topCategory: String? = null,
    val categorySpends: Map<String, Double> = emptyMap()
)

data class MonthlyBudgetTrendSummary(
    val trendPoints: List<MonthlyBudgetTrendPoint>,
    val averageMonthlySpend: Double,
    val averageMonthlyBudget: Double,
    val totalSpent: Double,
    val totalBudget: Double,
    val netSavings: Double,
    val monthsUnderBudgetCount: Int,
    val monthsOverBudgetCount: Int,
    val adherenceRatePercent: Float,
    val overallGrowthPercent: Double,
    val isSpendingDecreased: Boolean,
    val selectedCategory: String? = null
)

object MonthlyBudgetTrendCalculator {
    fun calculateMonthlyBudgetTrends(
        allTxs: List<ExpenseEntity>,
        budgets: List<BudgetEntity>,
        monthsCount: Int,
        categoryFilter: String? = null
    ): MonthlyBudgetTrendSummary {
        val monthKeys = mutableListOf<Triple<Int, Int, Calendar>>()
        for (i in (monthsCount - 1) downTo 0) {
            val c = Calendar.getInstance().apply {
                add(Calendar.MONTH, -i)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val y = c.get(Calendar.YEAR)
            val m = c.get(Calendar.MONTH) + 1
            monthKeys.add(Triple(y, m, c))
        }

        val shortFormat = SimpleDateFormat("MMM", Locale.ENGLISH)
        val fullFormat = SimpleDateFormat("MMM yyyy", Locale.ENGLISH)

        val txMap = allTxs.groupBy { exp ->
            val c = Calendar.getInstance().apply { timeInMillis = exp.date }
            Pair(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1)
        }

        // Default baseline monthly budget if user hasn't set any budgets in Room yet
        val defaultTotalBudget = 40000.0
        val defaultCategoryBudget = 6000.0

        var prevMonthSpent: Double? = null
        val points = monthKeys.map { (y, m, c) ->
            val allTxsInMonth = txMap[Pair(y, m)] ?: emptyList()
            val filteredTxs = if (categoryFilter.isNullOrBlank() || categoryFilter.equals("All", ignoreCase = true)) {
                allTxsInMonth
            } else {
                allTxsInMonth.filter { it.category.equals(categoryFilter, ignoreCase = true) }
            }

            val spent = filteredTxs.sumOf { it.amount }
            val count = filteredTxs.size

            val budgetLimit = if (categoryFilter.isNullOrBlank() || categoryFilter.equals("All", ignoreCase = true)) {
                if (budgets.isNotEmpty()) budgets.sumOf { it.monthlyLimit } else defaultTotalBudget
            } else {
                budgets.firstOrNull { it.category.equals(categoryFilter, ignoreCase = true) }?.monthlyLimit ?: defaultCategoryBudget
            }

            val isOver = spent > budgetLimit
            val variance = budgetLimit - spent
            val usedPct = if (budgetLimit > 0) ((spent / budgetLimit) * 100).toFloat() else 0f

            val momChange = if (prevMonthSpent != null && prevMonthSpent!! > 0) {
                ((spent - prevMonthSpent!!) / prevMonthSpent!!) * 100.0
            } else {
                0.0
            }
            prevMonthSpent = spent

            val catSpends = allTxsInMonth.groupBy { it.category }.mapValues { it.value.sumOf { exp -> exp.amount } }
            val topCat = catSpends.maxByOrNull { it.value }?.key

            MonthlyBudgetTrendPoint(
                monthKey = String.format(Locale.ENGLISH, "%04d-%02d", y, m),
                shortMonth = shortFormat.format(c.time),
                fullMonthName = fullFormat.format(c.time),
                year = y,
                month = m,
                totalSpent = spent,
                budgetLimit = budgetLimit,
                isOverBudget = isOver,
                variance = variance,
                budgetUsedPercent = usedPct,
                transactionCount = count,
                momChangePercent = momChange,
                isIncreased = momChange > 0,
                topCategory = topCat,
                categorySpends = catSpends
            )
        }

        val totalSpent = points.sumOf { it.totalSpent }
        val totalBudget = points.sumOf { it.budgetLimit }
        val avgSpend = if (points.isNotEmpty()) totalSpent / points.size else 0.0
        val avgBudget = if (points.isNotEmpty()) totalBudget / points.size else 0.0
        val netSavings = totalBudget - totalSpent
        val underCount = points.count { !it.isOverBudget }
        val overCount = points.count { it.isOverBudget }
        val adherencePct = if (points.isNotEmpty()) ((underCount.toFloat() / points.size) * 100f) else 100f

        val nonZeroMonths = points.filter { it.totalSpent > 0 }
        val firstActive = nonZeroMonths.firstOrNull()?.totalSpent ?: (points.firstOrNull()?.totalSpent ?: 0.0)
        val latestSpent = points.lastOrNull()?.totalSpent ?: 0.0
        val overallGrowth = if (firstActive > 0) {
            ((latestSpent - firstActive) / firstActive) * 100.0
        } else {
            0.0
        }

        return MonthlyBudgetTrendSummary(
            trendPoints = points,
            averageMonthlySpend = avgSpend,
            averageMonthlyBudget = avgBudget,
            totalSpent = totalSpent,
            totalBudget = totalBudget,
            netSavings = netSavings,
            monthsUnderBudgetCount = underCount,
            monthsOverBudgetCount = overCount,
            adherenceRatePercent = adherencePct,
            overallGrowthPercent = overallGrowth,
            isSpendingDecreased = latestSpent <= firstActive,
            selectedCategory = categoryFilter
        )
    }

    fun calculateCategoryBudgetReport(
        allTxs: List<ExpenseEntity>,
        budgets: List<BudgetEntity>,
        year: Int,
        month: Int // 1..12
    ): MonthlyCategoryBudgetReport {
        val calTarget = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val monthNameFormat = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH)
        val shortNameFormat = SimpleDateFormat("MMM yyyy", Locale.ENGLISH)
        val monthLabel = monthNameFormat.format(calTarget.time)
        val shortLabel = shortNameFormat.format(calTarget.time)

        val cal = Calendar.getInstance()
        val monthTxs = allTxs.filter {
            cal.timeInMillis = it.date
            cal.get(Calendar.YEAR) == year && (cal.get(Calendar.MONTH) + 1) == month
        }

        // Previous month for MoM change
        val calPrev = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            add(Calendar.MONTH, -1)
        }
        val prevYear = calPrev.get(Calendar.YEAR)
        val prevMonth = calPrev.get(Calendar.MONTH) + 1
        val prevMonthTxs = allTxs.filter {
            cal.timeInMillis = it.date
            cal.get(Calendar.YEAR) == prevYear && (cal.get(Calendar.MONTH) + 1) == prevMonth
        }

        // Last 6 months history keys for sparklines
        val historyMonthKeys = mutableListOf<Triple<Int, Int, String>>()
        val shortMonthFormat = SimpleDateFormat("MMM", Locale.ENGLISH)
        for (i in 5 downTo 0) {
            val c = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
                add(Calendar.MONTH, -i)
            }
            historyMonthKeys.add(Triple(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, shortMonthFormat.format(c.time)))
        }

        val allCategoriesSet = linkedSetOf<String>()
        // First add categories with active budgets
        budgets.forEach { allCategoriesSet.add(it.category) }
        // Next add categories with transactions in this month
        monthTxs.forEach { allCategoriesSet.add(it.category) }
        // Next add default categories if set is small
        if (allCategoriesSet.size < 5) {
            DefaultCategories.forEach { allCategoriesSet.add(it) }
        }

        val defaultCatBudget = 6000.0

        val items = allCategoriesSet.map { cat ->
            val spent = monthTxs.filter { it.category.equals(cat, ignoreCase = true) }.sumOf { it.amount }
            val count = monthTxs.count { it.category.equals(cat, ignoreCase = true) }
            val budgetLimit = budgets.firstOrNull { it.category.equals(cat, ignoreCase = true) }?.monthlyLimit ?: defaultCatBudget
            val isOver = spent > budgetLimit
            val variance = budgetLimit - spent
            val pctUsed = if (budgetLimit > 0) ((spent / budgetLimit) * 100).toFloat() else 0f

            val prevSpent = prevMonthTxs.filter { it.category.equals(cat, ignoreCase = true) }.sumOf { it.amount }
            val momChange = if (prevSpent > 0) ((spent - prevSpent) / prevSpent) * 100.0 else 0.0

            val history = historyMonthKeys.map { (hY, hM, hLabel) ->
                val hSpent = allTxs.filter { exp ->
                    cal.timeInMillis = exp.date
                    cal.get(Calendar.YEAR) == hY && (cal.get(Calendar.MONTH) + 1) == hM && exp.category.equals(cat, ignoreCase = true)
                }.sumOf { it.amount }
                Pair(hLabel, hSpent)
            }

            CategoryBudgetComparisonItem(
                category = cat,
                spentAmount = spent,
                budgetLimit = budgetLimit,
                isOverBudget = isOver,
                variance = variance,
                percentageUsed = pctUsed,
                transactionCount = count,
                previousMonthSpent = prevSpent,
                momChangePercent = momChange,
                monthlyHistory = history
            )
        }.sortedWith(compareByDescending<CategoryBudgetComparisonItem> { it.percentageUsed }.thenByDescending { it.spentAmount })

        val totalBudget = items.sumOf { it.budgetLimit }
        val totalSpent = items.sumOf { it.spentAmount }
        val totalVariance = totalBudget - totalSpent
        val overCount = items.count { it.isOverBudget }
        val safeCount = items.count { !it.isOverBudget }
        val adherencePct = if (items.isNotEmpty()) (safeCount.toFloat() / items.size * 100f) else 100f

        return MonthlyCategoryBudgetReport(
            year = year,
            month = month,
            monthName = monthLabel,
            shortName = shortLabel,
            items = items,
            totalBudget = totalBudget,
            totalSpent = totalSpent,
            totalVariance = totalVariance,
            overBudgetCount = overCount,
            safeBudgetCount = safeCount,
            adherenceRatePercent = adherencePct
        )
    }

    fun calculateCategoryMultiMonthTrends(
        allTxs: List<ExpenseEntity>,
        budgets: List<BudgetEntity>,
        monthsCount: Int = 6
    ): List<CategoryMultiMonthTrendPoint> {
        val monthKeys = mutableListOf<Triple<Int, Int, String>>()
        val shortFormat = SimpleDateFormat("MMM", Locale.ENGLISH)
        for (i in (monthsCount - 1) downTo 0) {
            val c = Calendar.getInstance().apply {
                add(Calendar.MONTH, -i)
            }
            monthKeys.add(Triple(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, shortFormat.format(c.time)))
        }

        val cal = Calendar.getInstance()
        val defaultCatBudget = 6000.0

        val distinctCats = (budgets.map { it.category } + allTxs.map { it.category } + DefaultCategories).distinct()

        val budgetMap = distinctCats.associateWith { cat ->
            budgets.firstOrNull { it.category.equals(cat, ignoreCase = true) }?.monthlyLimit ?: defaultCatBudget
        }

        return monthKeys.map { (y, m, label) ->
            val mKey = String.format(Locale.ENGLISH, "%04d-%02d", y, m)
            val monthTxs = allTxs.filter {
                cal.timeInMillis = it.date
                cal.get(Calendar.YEAR) == y && (cal.get(Calendar.MONTH) + 1) == m
            }
            val catSpends = distinctCats.associateWith { cat ->
                monthTxs.filter { it.category.equals(cat, ignoreCase = true) }.sumOf { it.amount }
            }

            CategoryMultiMonthTrendPoint(
                monthKey = mKey,
                shortMonth = label,
                categorySpends = catSpends,
                categoryBudgets = budgetMap
            )
        }
    }
}

data class CategoryBudgetComparisonItem(
    val category: String,
    val spentAmount: Double,
    val budgetLimit: Double,
    val isOverBudget: Boolean,
    val variance: Double, // budgetLimit - spentAmount (> 0 savings, < 0 over)
    val percentageUsed: Float,
    val transactionCount: Int,
    val previousMonthSpent: Double = 0.0,
    val momChangePercent: Double = 0.0,
    val monthlyHistory: List<Pair<String, Double>> = emptyList() // (ShortMonth, Amount)
)

data class MonthlyCategoryBudgetReport(
    val year: Int,
    val month: Int,
    val monthName: String,
    val shortName: String,
    val items: List<CategoryBudgetComparisonItem>,
    val totalBudget: Double,
    val totalSpent: Double,
    val totalVariance: Double,
    val overBudgetCount: Int,
    val safeBudgetCount: Int,
    val adherenceRatePercent: Float
)

data class CategoryMultiMonthTrendPoint(
    val monthKey: String,
    val shortMonth: String,
    val categorySpends: Map<String, Double>,
    val categoryBudgets: Map<String, Double>
)

data class MonthlyExpenseReportDetail(
    val year: Int,
    val month: Int,
    val monthName: String,
    val shortName: String,
    val totalSpent: Double,
    val transactionCount: Int,
    val dailyAverage: Double,
    val highestExpense: ExpenseEntity?,
    val categoryBreakdown: List<CategoryMonthlySpend>,
    val merchantBreakdown: List<MerchantMonthlySpend>,
    val paymentBreakdown: Map<String, Double>,
    val expenses: List<ExpenseEntity>
)

enum class RecurringFrequency(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly")
}

@Entity(tableName = "recurring_expenses")
data class RecurringExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val category: String = "Bills & Utilities",
    val paymentMethod: String = "UPI",
    val frequency: String = "MONTHLY", // DAILY, WEEKLY, MONTHLY, YEARLY
    val dayOfMonth: Int = 1, // 1..31
    val dayOfWeek: Int = 2, // 1=Sunday, 2=Monday, ...
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long? = null,
    val lastExecutedDate: Long? = null,
    val nextDueDate: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val bankName: String = "",
    val creditCardName: String = "",
    val notes: String = "",
    val tag: String = "Subscription", // "Rent", "Subscription", "Fixed Bill", "EMI", "Insurance"
    val autoCreateExpense: Boolean = true
) {
    val formattedAmount: String
        get() = ExpenseEntity.formatInr(amount)

    val formattedNextDue: String
        get() {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            return sdf.format(Date(nextDueDate))
        }

    val daysUntilDue: Long
        get() {
            val now = System.currentTimeMillis()
            val diffMs = nextDueDate - now
            return if (diffMs <= 0) 0 else (diffMs / (1000 * 60 * 60 * 24))
        }

    val isDueNow: Boolean
        get() = System.currentTimeMillis() >= nextDueDate
}

object RecurringDateUtils {
    fun calculateNextDueDate(
        frequency: String,
        currentDueDate: Long,
        dayOfMonth: Int = 1,
        dayOfWeek: Int = 2
    ): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = currentDueDate
        }
        when (frequency.uppercase(Locale.ROOT)) {
            "DAILY" -> {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            "WEEKLY" -> {
                cal.add(Calendar.DAY_OF_YEAR, 7)
            }
            "YEARLY" -> {
                cal.add(Calendar.YEAR, 1)
            }
            else -> { // "MONTHLY"
                cal.add(Calendar.MONTH, 1)
                val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth.coerceIn(1, maxDay))
            }
        }
        return cal.timeInMillis
    }

    fun computeInitialDueDate(
        frequency: String,
        startDate: Long,
        dayOfMonth: Int = 1,
        dayOfWeek: Int = 2
    ): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = startDate
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = System.currentTimeMillis()

        when (frequency.uppercase(Locale.ROOT)) {
            "DAILY" -> {
                if (cal.timeInMillis < now) {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            "WEEKLY" -> {
                cal.set(Calendar.DAY_OF_WEEK, dayOfWeek)
                if (cal.timeInMillis < now) {
                    cal.add(Calendar.WEEK_OF_YEAR, 1)
                }
            }
            "YEARLY" -> {
                val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth.coerceIn(1, maxDay))
                if (cal.timeInMillis < now) {
                    cal.add(Calendar.YEAR, 1)
                }
            }
            else -> { // "MONTHLY"
                val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth.coerceIn(1, maxDay))
                if (cal.timeInMillis < now) {
                    cal.add(Calendar.MONTH, 1)
                    val nextMaxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    cal.set(Calendar.DAY_OF_MONTH, dayOfMonth.coerceIn(1, nextMaxDay))
                }
            }
        }
        return cal.timeInMillis
    }
}

data class MonthlyAiSpendingReport(
    val monthName: String,
    val year: Int,
    val totalSpent: Double,
    val transactionCount: Int,
    val dailyAverage: Double,
    val previousMonthComparisonText: String,
    val executiveSummary: String,
    val spendingHabitRating: Int, // 0 to 100
    val spendingHabitRatingLabel: String, // e.g. "Disciplined Saver", "Balanced & Mindful", "Discretionary Caution"
    val categoryHabitsInsight: String,
    val recurringImpactInsight: String,
    val keyBehavioralObservations: List<String>,
    val actionableSavingsTips: List<String>,
    val encouragementQuote: String,
    val generatedAtMillis: Long = System.currentTimeMillis(),
    val isAiGenerated: Boolean = true
)


