package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.NumberFormat
import java.text.SimpleDateFormat
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


