package com.example.data.repository

import com.example.data.db.BudgetDao
import com.example.data.db.ExpenseDao
import com.example.data.db.RecurringExpenseDao
import com.example.data.db.SyncLogDao
import com.example.data.model.BudgetEntity
import com.example.data.model.CategorySummary
import com.example.data.model.DailySpending
import com.example.data.model.ExpenseEntity
import com.example.data.model.PaymentMethodSummary
import com.example.data.model.RecurringDateUtils
import com.example.data.model.RecurringExpenseEntity
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
    private val syncLogDao: SyncLogDao,
    private val recurringExpenseDao: RecurringExpenseDao
) {
    val allExpenses: Flow<List<ExpenseEntity>> = expenseDao.getAllExpensesFlow()
    val allBudgets: Flow<List<BudgetEntity>> = budgetDao.getAllBudgetsFlow()
    val syncLogs: Flow<List<SyncLogEntity>> = syncLogDao.getRecentSyncLogsFlow()
    val allRecurringExpenses: Flow<List<RecurringExpenseEntity>> = recurringExpenseDao.getAllRecurringExpensesFlow()
    val activeRecurringExpenses: Flow<List<RecurringExpenseEntity>> = recurringExpenseDao.getActiveRecurringExpensesFlow()

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

    suspend fun insertRecurringExpense(item: RecurringExpenseEntity): Long = withContext(Dispatchers.IO) {
        recurringExpenseDao.insertRecurringExpense(item)
    }

    suspend fun updateRecurringExpense(item: RecurringExpenseEntity) = withContext(Dispatchers.IO) {
        recurringExpenseDao.updateRecurringExpense(item)
    }

    suspend fun deleteRecurringExpense(item: RecurringExpenseEntity) = withContext(Dispatchers.IO) {
        recurringExpenseDao.deleteRecurringExpense(item)
    }

    suspend fun deleteRecurringExpenseById(id: Long) = withContext(Dispatchers.IO) {
        recurringExpenseDao.deleteRecurringExpenseById(id)
    }

    suspend fun toggleRecurringExpenseActive(id: Long, isActive: Boolean) = withContext(Dispatchers.IO) {
        val existing = recurringExpenseDao.getRecurringExpenseById(id) ?: return@withContext
        recurringExpenseDao.updateRecurringExpense(existing.copy(isActive = isActive))
    }

    suspend fun executeRecurringExpenseNow(id: Long, executionTime: Long = System.currentTimeMillis()): ExpenseEntity? = withContext(Dispatchers.IO) {
        val item = recurringExpenseDao.getRecurringExpenseById(id) ?: return@withContext null
        val newExpense = ExpenseEntity(
            title = item.title,
            amount = item.amount,
            date = executionTime,
            category = item.category,
            paymentMethod = item.paymentMethod,
            notes = if (item.notes.isNotBlank()) "Recurring: ${item.notes}" else "Recurring Fixed Bill",
            tag = item.tag,
            bankName = item.bankName,
            creditCardName = item.creditCardName
        )
        val newId = expenseDao.insertExpense(newExpense)
        val nextDue = RecurringDateUtils.calculateNextDueDate(
            item.frequency,
            item.nextDueDate,
            item.dayOfMonth,
            item.dayOfWeek
        )
        val hasEnded = item.endDate != null && nextDue > item.endDate
        recurringExpenseDao.updateRecurringExpense(
            item.copy(
                lastExecutedDate = executionTime,
                nextDueDate = nextDue,
                isActive = !hasEnded
            )
        )
        addSyncLog(
            action = "MANUAL_RECURRING_PAY",
            count = 1,
            status = "SUCCESS",
            email = "Room DB Scheduler",
            details = "Recorded recurring bill: ${item.title} (₹${item.amount})"
        )
        newExpense.copy(id = newId)
    }

    suspend fun processDueRecurringExpenses(currentTime: Long = System.currentTimeMillis()): List<ExpenseEntity> = withContext(Dispatchers.IO) {
        val dueItems = recurringExpenseDao.getDueRecurringExpenses(currentTime)
        val createdExpenses = mutableListOf<ExpenseEntity>()

        for (item in dueItems) {
            if (!item.isActive) continue
            // If manual recording is chosen, do not auto-advance or skip without user confirmation
            if (!item.autoCreateExpense) continue

            var nextDue = item.nextDueDate
            var iterations = 0
            while (nextDue <= currentTime && iterations < 12) {
                if (item.autoCreateExpense) {
                    val newExpense = ExpenseEntity(
                        title = item.title,
                        amount = item.amount,
                        date = nextDue,
                        category = item.category,
                        paymentMethod = item.paymentMethod,
                        notes = if (item.notes.isNotBlank()) "Scheduled: ${item.notes}" else "Scheduled Recurring Bill",
                        tag = item.tag,
                        bankName = item.bankName,
                        creditCardName = item.creditCardName
                    )
                    val newId = expenseDao.insertExpense(newExpense)
                    createdExpenses.add(newExpense.copy(id = newId))
                }

                nextDue = RecurringDateUtils.calculateNextDueDate(
                    item.frequency,
                    nextDue,
                    item.dayOfMonth,
                    item.dayOfWeek
                )
                iterations++
            }

            val hasEnded = item.endDate != null && nextDue > item.endDate
            val updated = item.copy(
                lastExecutedDate = currentTime,
                nextDueDate = nextDue,
                isActive = !hasEnded
            )
            recurringExpenseDao.updateRecurringExpense(updated)
        }

        if (createdExpenses.isNotEmpty()) {
            addSyncLog(
                action = "AUTO_RECURRING",
                count = createdExpenses.size,
                status = "SUCCESS",
                email = "Room DB Scheduler",
                details = "Auto-created ${createdExpenses.size} recurring bill entries (${createdExpenses.map { it.title }.take(3).joinToString()})"
            )
        }

        createdExpenses
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

        val recurringList = recurringExpenseDao.getAllRecurringExpensesList()
        val recurringArray = JSONArray()
        for (r in recurringList) {
            val obj = JSONObject().apply {
                put("title", r.title)
                put("amount", r.amount)
                put("category", r.category)
                put("paymentMethod", r.paymentMethod)
                put("frequency", r.frequency)
                put("dayOfMonth", r.dayOfMonth)
                put("dayOfWeek", r.dayOfWeek)
                put("startDate", r.startDate)
                if (r.endDate != null) put("endDate", r.endDate)
                if (r.lastExecutedDate != null) put("lastExecutedDate", r.lastExecutedDate)
                put("nextDueDate", r.nextDueDate)
                put("isActive", r.isActive)
                put("bankName", r.bankName)
                put("creditCardName", r.creditCardName)
                put("notes", r.notes)
                put("tag", r.tag)
                put("autoCreateExpense", r.autoCreateExpense)
            }
            recurringArray.put(obj)
        }
        root.put("recurring_expenses", recurringArray)

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

        val recurringArray = root.optJSONArray("recurring_expenses")
        if (recurringArray != null) {
            val importedRecurring = mutableListOf<RecurringExpenseEntity>()
            for (i in 0 until recurringArray.length()) {
                val obj = recurringArray.getJSONObject(i)
                importedRecurring.add(
                    RecurringExpenseEntity(
                        title = obj.getString("title"),
                        amount = obj.getDouble("amount"),
                        category = obj.optString("category", "Bills & Utilities"),
                        paymentMethod = obj.optString("paymentMethod", "UPI"),
                        frequency = obj.optString("frequency", "MONTHLY"),
                        dayOfMonth = obj.optInt("dayOfMonth", 1),
                        dayOfWeek = obj.optInt("dayOfWeek", 2),
                        startDate = obj.optLong("startDate", System.currentTimeMillis()),
                        endDate = if (obj.has("endDate")) obj.getLong("endDate") else null,
                        lastExecutedDate = if (obj.has("lastExecutedDate")) obj.getLong("lastExecutedDate") else null,
                        nextDueDate = obj.optLong("nextDueDate", System.currentTimeMillis()),
                        isActive = obj.optBoolean("isActive", true),
                        bankName = obj.optString("bankName", ""),
                        creditCardName = obj.optString("creditCardName", ""),
                        notes = obj.optString("notes", ""),
                        tag = obj.optString("tag", "Subscription"),
                        autoCreateExpense = obj.optBoolean("autoCreateExpense", true)
                    )
                )
            }
            if (importedRecurring.isNotEmpty()) {
                recurringExpenseDao.insertRecurringExpenses(importedRecurring)
            }
        }

        importedExpenses.size
    }

    /**
     * Executes Room Full-Text Search (FTS4) query with category and date range constraints.
     * Returns a reactive Flow directly from Room SQLite database.
     */
    fun searchExpensesFts(
        query: String,
        scope: String = "ALL",
        category: String? = null,
        startDate: Long? = null,
        endDate: Long? = null
    ): Flow<List<ExpenseEntity>> {
        val ftsQuery = RoomFtsQueryBuilder.buildFtsQuery(query, scope)
        return if (ftsQuery.isNotBlank()) {
            expenseDao.searchExpensesFtsWithFiltersFlow(
                ftsQuery = ftsQuery,
                category = if (category.isNullOrBlank() || category.equals("All", ignoreCase = true)) null else category,
                startDate = startDate,
                endDate = endDate
            )
        } else {
            expenseDao.getExpensesWithFiltersFlow(
                category = if (category.isNullOrBlank() || category.equals("All", ignoreCase = true)) null else category,
                startDate = startDate,
                endDate = endDate
            )
        }
    }

    suspend fun searchExpensesFtsList(
        query: String,
        scope: String = "ALL",
        category: String? = null,
        startDate: Long? = null,
        endDate: Long? = null
    ): List<ExpenseEntity> = withContext(Dispatchers.IO) {
        val ftsQuery = RoomFtsQueryBuilder.buildFtsQuery(query, scope)
        if (ftsQuery.isNotBlank()) {
            expenseDao.searchExpensesFtsWithFiltersList(
                ftsQuery = ftsQuery,
                category = if (category.isNullOrBlank() || category.equals("All", ignoreCase = true)) null else category,
                startDate = startDate,
                endDate = endDate
            )
        } else {
            expenseDao.getExpensesWithFiltersList(
                category = if (category.isNullOrBlank() || category.equals("All", ignoreCase = true)) null else category,
                startDate = startDate,
                endDate = endDate
            )
        }
    }
}

object RoomFtsQueryBuilder {
    /**
     * Sanitizes and builds an SQLite FTS4 query string.
     * Strips non-alphanumeric punctuation and formats words with trailing '*' wildcard.
     * Maps merchant/title, category, or notes scopes into column-specific FTS match expressions.
     */
    fun buildFtsQuery(rawQuery: String, scope: String = "ALL"): String {
        val clean = rawQuery.replace(Regex("[^a-zA-Z0-9\\s]"), " ").trim()
        val tokens = clean.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return ""

        return when (scope.uppercase()) {
            "MERCHANT", "TITLE" -> tokens.joinToString(" ") { "title:$it*" }
            "CATEGORY" -> tokens.joinToString(" ") { "category:$it*" }
            "NOTES" -> tokens.joinToString(" ") { "notes:$it*" }
            else -> tokens.joinToString(" ") { "$it*" }
        }
    }
}
