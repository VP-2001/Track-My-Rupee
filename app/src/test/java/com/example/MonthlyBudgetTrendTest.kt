package com.example

import com.example.data.model.BudgetEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.MonthlyBudgetTrendCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class MonthlyBudgetTrendTest {

    @Test
    fun calculateMonthlyBudgetTrends_emptyTransactions_returnsDefaultPoints() {
        val summary = MonthlyBudgetTrendCalculator.calculateMonthlyBudgetTrends(
            allTxs = emptyList(),
            budgets = emptyList(),
            monthsCount = 6,
            categoryFilter = null
        )

        assertEquals(6, summary.trendPoints.size)
        assertEquals(0.0, summary.totalSpent, 0.01)
        // 6 months * default 40000.0 budget = 240000.0
        assertEquals(240000.0, summary.totalBudget, 0.01)
        assertEquals(100f, summary.adherenceRatePercent, 0.01f)
        assertEquals(6, summary.monthsUnderBudgetCount)
        assertEquals(0, summary.monthsOverBudgetCount)
    }

    @Test
    fun calculateMonthlyBudgetTrends_withSpendingAndBudgets_correctlyIdentifiesOverBudgetMonths() {
        val cal = Calendar.getInstance()

        // Current month: 50,000 spend
        val thisMonthTime = cal.timeInMillis

        // 1 month ago: 25,000 spend
        val cal1 = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
        val lastMonthTime = cal1.timeInMillis

        val txs = listOf(
            ExpenseEntity(title = "Swiggy", amount = 30000.0, category = "Food", paymentMethod = "UPI", date = thisMonthTime),
            ExpenseEntity(title = "Rent", amount = 20000.0, category = "Bills", paymentMethod = "Net Banking", date = thisMonthTime),
            ExpenseEntity(title = "Groceries", amount = 25000.0, category = "Food", paymentMethod = "Credit Card", date = lastMonthTime)
        )

        val budgets = listOf(
            BudgetEntity(category = "Food", monthlyLimit = 20000.0),
            BudgetEntity(category = "Bills", monthlyLimit = 15000.0)
        ) // Total budget = 35,000.0

        val summary = MonthlyBudgetTrendCalculator.calculateMonthlyBudgetTrends(
            allTxs = txs,
            budgets = budgets,
            monthsCount = 6,
            categoryFilter = null
        )

        assertEquals(6, summary.trendPoints.size)

        val currentMonthPoint = summary.trendPoints.last()
        assertEquals(50000.0, currentMonthPoint.totalSpent, 0.01)
        assertEquals(35000.0, currentMonthPoint.budgetLimit, 0.01)
        assertTrue("Current month should be marked over budget", currentMonthPoint.isOverBudget)
        assertEquals(-15000.0, currentMonthPoint.variance, 0.01)

        val prevMonthPoint = summary.trendPoints[summary.trendPoints.size - 2]
        assertEquals(25000.0, prevMonthPoint.totalSpent, 0.01)
        assertEquals(35000.0, prevMonthPoint.budgetLimit, 0.01)
        assertFalse("Previous month should be within budget", prevMonthPoint.isOverBudget)
        assertEquals(10000.0, prevMonthPoint.variance, 0.01)
    }

    @Test
    fun calculateMonthlyBudgetTrends_categoryFilter_filtersCategorySpendingAndBudget() {
        val cal = Calendar.getInstance()
        val thisMonthTime = cal.timeInMillis

        val txs = listOf(
            ExpenseEntity(title = "Dinner", amount = 12000.0, category = "Food", paymentMethod = "UPI", date = thisMonthTime),
            ExpenseEntity(title = "Flight", amount = 25000.0, category = "Travel", paymentMethod = "Credit Card", date = thisMonthTime)
        )

        val budgets = listOf(
            BudgetEntity(category = "Food", monthlyLimit = 10000.0),
            BudgetEntity(category = "Travel", monthlyLimit = 30000.0)
        )

        val foodSummary = MonthlyBudgetTrendCalculator.calculateMonthlyBudgetTrends(
            allTxs = txs,
            budgets = budgets,
            monthsCount = 6,
            categoryFilter = "Food"
        )

        val foodMonthPoint = foodSummary.trendPoints.last()
        assertEquals(12000.0, foodMonthPoint.totalSpent, 0.01)
        assertEquals(10000.0, foodMonthPoint.budgetLimit, 0.01)
        assertTrue("Food should be over budget by 2000", foodMonthPoint.isOverBudget)
        assertEquals(-2000.0, foodMonthPoint.variance, 0.01)
    }

    @Test
    fun calculateCategoryBudgetReport_groupsExpensesByCategoryAndIdentifiesBreaches() {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val date = cal.timeInMillis

        val txs = listOf(
            ExpenseEntity(title = "Cafe", amount = 15000.0, category = "Food", paymentMethod = "UPI", date = date),
            ExpenseEntity(title = "Clothes", amount = 8000.0, category = "Shopping", paymentMethod = "Card", date = date),
            ExpenseEntity(title = "Uber", amount = 3000.0, category = "Transport", paymentMethod = "UPI", date = date)
        )

        val budgets = listOf(
            BudgetEntity(category = "Food", monthlyLimit = 12000.0),
            BudgetEntity(category = "Shopping", monthlyLimit = 10000.0),
            BudgetEntity(category = "Transport", monthlyLimit = 5000.0)
        )

        val report = MonthlyBudgetTrendCalculator.calculateCategoryBudgetReport(
            allTxs = txs,
            budgets = budgets,
            year = year,
            month = month
        )

        assertEquals(26000.0, report.totalSpent, 0.01)
        val foodItem = report.items.find { it.category == "Food" }
        val shoppingItem = report.items.find { it.category == "Shopping" }
        val transportItem = report.items.find { it.category == "Transport" }

        assertTrue("Food item must be present", foodItem != null)
        assertTrue("Food must be marked over budget", foodItem!!.isOverBudget)
        assertEquals(15000.0, foodItem.spentAmount, 0.01)
        assertEquals(12000.0, foodItem.budgetLimit, 0.01)
        assertEquals(-3000.0, foodItem.variance, 0.01)
        assertEquals(125f, foodItem.percentageUsed, 0.1f)

        assertTrue("Shopping must be safe", shoppingItem != null && !shoppingItem.isOverBudget)
        assertEquals(2000.0, shoppingItem!!.variance, 0.01)

        assertTrue("Transport must be safe", transportItem != null && !transportItem.isOverBudget)
        assertEquals(2000.0, transportItem!!.variance, 0.01)

        assertEquals(1, report.overBudgetCount)
    }

    @Test
    fun calculateCategoryMultiMonthTrends_generatesMultiMonthDataPointsForEachCategory() {
        val cal = Calendar.getInstance()
        val txs = listOf(
            ExpenseEntity(title = "Groceries", amount = 4000.0, category = "Food", paymentMethod = "UPI", date = cal.timeInMillis)
        )
        val budgets = listOf(BudgetEntity(category = "Food", monthlyLimit = 8000.0))

        val trends = MonthlyBudgetTrendCalculator.calculateCategoryMultiMonthTrends(
            allTxs = txs,
            budgets = budgets,
            monthsCount = 6
        )

        assertEquals(6, trends.size)
        val latest = trends.last()
        assertEquals(4000.0, latest.categorySpends["Food"] ?: 0.0, 0.01)
        assertEquals(8000.0, latest.categoryBudgets["Food"] ?: 0.0, 0.01)
    }
}
