package com.example.data.repository

import com.example.data.db.BudgetDao
import com.example.data.db.ExpenseDao
import com.example.data.db.SyncLogDao
import com.example.data.model.BudgetEntity
import com.example.data.model.CategorySummary
import com.example.data.model.DailySpending
import com.example.data.model.ExpenseEntity
import com.example.data.model.PaymentMethodSummary
import com.example.data.model.SyncLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val budgetDao: BudgetDao,
    private val syncLogDao: SyncLogDao
) {
    val allExpenses: Flow<List<ExpenseEntity>> = expenseDao.getAllExpensesFlow()
    val allBudgets: Flow<List<BudgetEntity>> = budgetDao.getAllBudgetsFlow()
    val syncLogs: Flow<List<SyncLogEntity>> = syncLogDao.getRecentSyncLogsFlow()

    suspend fun insertExpense(expense: ExpenseEntity): Long = withContext(Dispatchers.IO) {
        expenseDao.insertExpense(expense)
    }

    suspend fun insertExpenses(expenses: List<ExpenseEntity>): List<Long> = withContext(Dispatchers.IO) {
        expenseDao.insertExpenses(expenses)
    }

    suspend fun updateExpense(expense: ExpenseEntity) = withContext(Dispatchers.IO) {
        expenseDao.updateExpense(expense)
    }

    suspend fun deleteExpense(expense: ExpenseEntity) = withContext(Dispatchers.IO) {
        expenseDao.deleteExpense(expense)
    }

    suspend fun deleteExpensesByIds(ids: List<Long>) = withContext(Dispatchers.IO) {
        expenseDao.deleteExpensesByIds(ids)
    }

    suspend fun getExpensesList(): List<ExpenseEntity> = withContext(Dispatchers.IO) {
        expenseDao.getAllExpensesList()
    }

    suspend fun insertOrUpdateBudget(budget: BudgetEntity) = withContext(Dispatchers.IO) {
        budgetDao.insertOrUpdateBudget(budget)
    }

    suspend fun deleteBudget(category: String) = withContext(Dispatchers.IO) {
        budgetDao.deleteBudget(category)
    }

    suspend fun addSyncLog(action: String, count: Int, status: String, email: String, details: String) = withContext(Dispatchers.IO) {
        syncLogDao.insertLog(
            SyncLogEntity(
                timestamp = System.currentTimeMillis(),
                action = action,
                itemCount = count,
                status = status,
                accountEmail = email,
                details = details
            )
        )
    }

    fun getCategorySummaries(startDate: Long, endDate: Long): Flow<List<CategorySummary>> {
        return expenseDao.getExpensesByDateRangeFlow(startDate, endDate).map { list ->
            val totalExpense = list.sumOf { it.amount }
            list.groupBy { it.category }
                .map { (cat, expenses) ->
                    val sum = expenses.sumOf { it.amount }
                    val percentage = if (totalExpense > 0) ((sum / totalExpense) * 100).toFloat() else 0f
                    CategorySummary(
                        category = cat,
                        totalAmount = sum,
                        transactionCount = expenses.size,
                        percentage = percentage
                    )
                }
                .sortedByDescending { it.totalAmount }
        }
    }

    fun getPaymentMethodSummaries(startDate: Long, endDate: Long): Flow<List<PaymentMethodSummary>> {
        return expenseDao.getExpensesByDateRangeFlow(startDate, endDate).map { list ->
            list.groupBy { it.paymentMethod }
                .map { (method, expenses) ->
                    PaymentMethodSummary(
                        paymentMethod = method,
                        totalAmount = expenses.sumOf { it.amount },
                        transactionCount = expenses.size
                    )
                }
                .sortedByDescending { it.totalAmount }
        }
    }

    fun getDailySpendingTrend(startDate: Long, endDate: Long): Flow<List<DailySpending>> {
        return expenseDao.getExpensesByDateRangeFlow(startDate, endDate).map { list ->
            val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
            val cal = Calendar.getInstance()
            cal.timeInMillis = startDate
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)

            val daysMap = mutableMapOf<String, Pair<Long, Double>>()
            while (cal.timeInMillis <= endDate) {
                val key = sdf.format(Date(cal.timeInMillis))
                daysMap[key] = Pair(cal.timeInMillis, 0.0)
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }

            for (expense in list) {
                val key = sdf.format(Date(expense.date))
                val existing = daysMap[key]
                if (existing != null) {
                    daysMap[key] = Pair(existing.first, existing.second + expense.amount)
                }
            }

            daysMap.map { (key, pair) ->
                DailySpending(date = key, timestamp = pair.first, amount = pair.second)
            }
        }
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        expenseDao.deleteAllExpenses()
        budgetDao.deleteAllBudgets()
        syncLogDao.clearLogs()
    }

    suspend fun purgeSampleSeedData() = withContext(Dispatchers.IO) {
        val sampleTitles = setOf(
            "Swiggy Gourmet Dinner",
            "Blinkit Quick Groceries",
            "Shell Petrol Fuel",
            "Electricity Bill (BESCOM)",
            "Amazon Office Ergonomic Chair",
            "Starbucks Cappuccino & Muffin",
            "Apollo Pharmacy Medicines",
            "Airtel Fiber Broadband",
            "Cult.fit Gym Subscription",
            "PVR IMAX Movie Tickets",
            "Auto & Metro Commute",
            "Mutual Fund SIP Investment"
        )
        val all = expenseDao.getAllExpensesList()
        val sampleIds = all.filter { it.title in sampleTitles }.map { it.id }
        if (sampleIds.isNotEmpty()) {
            expenseDao.deleteExpensesByIds(sampleIds)
        }
        syncLogDao.clearLogs()
    }

    suspend fun exportEncryptedJson(): String = withContext(Dispatchers.IO) {
        val expenses = expenseDao.getAllExpensesList()
        val budgets = budgetDao.getAllBudgetsList()

        val root = JSONObject()
        root.put("version", "1.0")
        root.put("exportedAt", System.currentTimeMillis())
        root.put("app", "ExpenseTracker")

        val expensesArray = JSONArray()
        for (e in expenses) {
            val obj = JSONObject().apply {
                put("id", e.id)
                put("title", e.title)
                put("amount", e.amount)
                put("date", e.date)
                put("category", e.category)
                put("paymentMethod", e.paymentMethod)
                put("notes", e.notes)
                put("tag", e.tag)
                put("createdAt", e.createdAt)
                put("bankName", e.bankName)
                put("creditCardName", e.creditCardName)
            }
            expensesArray.put(obj)
        }
        root.put("expenses", expensesArray)

        val budgetsArray = JSONArray()
        for (b in budgets) {
            val obj = JSONObject().apply {
                put("category", b.category)
                put("monthlyLimit", b.monthlyLimit)
                put("monthYear", b.monthYear)
            }
            budgetsArray.put(obj)
        }
        root.put("budgets", budgetsArray)

        root.toString(2)
    }

    suspend fun importEncryptedJson(jsonString: String): Int = withContext(Dispatchers.IO) {
        val root = JSONObject(jsonString)
        val expensesArray = root.optJSONArray("expenses") ?: JSONArray()
        val importedExpenses = mutableListOf<ExpenseEntity>()

        for (i in 0 until expensesArray.length()) {
            val obj = expensesArray.getJSONObject(i)
            importedExpenses.add(
                ExpenseEntity(
                    title = obj.getString("title"),
                    amount = obj.getDouble("amount"),
                    date = obj.getLong("date"),
                    category = obj.optString("category", "Miscellaneous"),
                    paymentMethod = obj.optString("paymentMethod", "UPI"),
                    notes = obj.optString("notes", ""),
                    tag = obj.optString("tag", "Personal"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    syncStatus = "SYNCED",
                    bankName = obj.optString("bankName", ""),
                    creditCardName = obj.optString("creditCardName", "")
                )
            )
        }

        if (importedExpenses.isNotEmpty()) {
            expenseDao.insertExpenses(importedExpenses)
        }

        val budgetsArray = root.optJSONArray("budgets")
        if (budgetsArray != null) {
            val importedBudgets = mutableListOf<BudgetEntity>()
            for (i in 0 until budgetsArray.length()) {
                val obj = budgetsArray.getJSONObject(i)
                importedBudgets.add(
                    BudgetEntity(
                        category = obj.getString("category"),
                        monthlyLimit = obj.getDouble("monthlyLimit"),
                        monthYear = obj.optString("monthYear", SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()))
                    )
                )
            }
            if (importedBudgets.isNotEmpty()) {
                budgetDao.insertOrUpdateBudgets(importedBudgets)
            }
        }

        importedExpenses.size
    }
}
