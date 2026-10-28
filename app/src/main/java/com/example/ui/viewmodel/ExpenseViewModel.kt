package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.BankStatementParser
import com.example.ai.ChatMessage
import com.example.ai.ExtractedTransaction
import com.example.ai.GeminiAiService
import com.example.ai.GeminiChatModel
import com.example.ai.MessageRole
import com.example.ai.StatementParseResult
import com.example.data.db.ExpenseDatabase
import com.example.data.model.BudgetEntity
import com.example.data.model.CategoryCrossMonthItem
import com.example.data.model.CategoryMonthlySpend
import com.example.data.model.CategorySummary
import com.example.data.model.ComparisonViewMode
import com.example.data.model.CreditCardSuggestion
import com.example.data.model.DailySpending
import com.example.data.model.DefaultCategories
import com.example.data.model.ExpenseEntity
import com.example.data.model.MerchantCrossMonthItem
import com.example.data.model.MerchantMonthlySpend
import com.example.data.model.MonthlyBreakdownItem
import com.example.data.model.CategoryBudgetComparisonItem
import com.example.data.model.CategoryMultiMonthTrendPoint
import com.example.data.model.MonthlyBudgetHabitStatus
import com.example.data.model.MonthlyCategoryBudgetReport
import com.example.data.model.MonthlyBudgetTrendCalculator
import com.example.data.model.MonthlyBudgetTrendPoint
import com.example.data.model.MonthlyBudgetTrendSummary
import com.example.data.model.MonthlyComparisonReport
import com.example.data.model.MonthlyAiSpendingReport
import com.example.data.model.MonthlyExpenseReportDetail
import com.example.data.model.MonthlySpendingHabitsSummary
import com.example.data.model.MonthlyTrendData
import com.example.data.model.OneYearGrowthSummary
import com.example.data.model.PaymentMethodSummary
import com.example.data.model.RecurringDateUtils
import com.example.data.model.RecurringExpenseEntity
import com.example.data.model.RecurringFrequency
import com.example.data.model.SixMonthGrowthSummary
import com.example.data.model.SyncLogEntity
import com.example.data.repository.CreditCardSuggestionsManager
import com.example.auth.AuthManager
import com.example.data.firestore.FirestoreSyncRepository
import com.example.data.firestore.SyncSummary
import com.example.data.repository.ExpenseRepository
import com.example.export.ReportExportManager
import com.example.notification.BudgetNotificationService
import com.google.firebase.auth.FirebaseUser
import com.example.notification.BudgetAlertThreshold
import com.example.notification.BudgetAlertResult
import com.example.security.CryptoBackupManager
import com.example.security.SecurityAndSyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

enum class DateFilterType(val label: String) {
    THIS_MONTH("This Month"),
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    LAST_7_DAYS("Last 7 Days"),
    ALL_TIME("All Time"),
    CUSTOM("Custom")
}

enum class SortType(val label: String) {
    DATE_NEWEST("Date (Newest)"),
    DATE_OLDEST("Date (Oldest)"),
    AMOUNT_HIGHEST("Amount (High to Low)"),
    AMOUNT_LOWEST("Amount (Low to High)")
}

enum class SearchFilterScope(val label: String, val hint: String) {
    ALL("All", "Search merchant, category, notes, or date..."),
    MERCHANT("Merchant", "Filter by merchant / title (e.g. Amazon, Swiggy)..."),
    CATEGORY("Category", "Filter by category (e.g. Food, Travel, Utilities)..."),
    NOTES("Notes", "Filter by note content (e.g. Dinner, Gift, Refund)..."),
    DATE("Date", "Filter by date (e.g. Oct, 16 Oct, 2026, Today)...")
}

data class BudgetProgressItem(
    val category: String,
    val monthlyLimit: Double,
    val spentAmount: Double,
    val remainingAmount: Double,
    val percentage: Float,
    val isOverBudget: Boolean
)

data class AvailableMonth(
    val year: Int,
    val month: Int, // 1..12
    val label: String, // e.g. "September 2026"
    val shortLabel: String, // e.g. "Sep 2026"
    val count: Int,
    val totalSpent: Double
)

sealed interface MonthlyAiReportUiState {
    data object Idle : MonthlyAiReportUiState
    data class Loading(val monthName: String) : MonthlyAiReportUiState
    data class Success(val report: MonthlyAiSpendingReport) : MonthlyAiReportUiState
    data class Error(val message: String) : MonthlyAiReportUiState
}

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {
    private val db = ExpenseDatabase.getDatabase(application)
    val repository = ExpenseRepository(db.expenseDao(), db.budgetDao(), db.syncLogDao(), db.recurringExpenseDao())
    val geminiService = GeminiAiService()
    val securityManager = SecurityAndSyncManager(application)
    val cardSuggestionsManager = CreditCardSuggestionsManager(application)
    val cardSuggestions: StateFlow<List<CreditCardSuggestion>> = cardSuggestionsManager.suggestions

    val authManager = AuthManager(application)
    val currentUser: StateFlow<FirebaseUser?> = authManager.currentUser
    val isAuthenticating: StateFlow<Boolean> = authManager.isAuthenticating
    val authErrorMessage: StateFlow<String?> = authManager.authErrorMessage

    val firestoreSyncRepository = FirestoreSyncRepository(application)
    private val _isCloudSyncing = MutableStateFlow(false)
    val isCloudSyncing: StateFlow<Boolean> = _isCloudSyncing.asStateFlow()
    private val _syncSummary = MutableStateFlow<SyncSummary?>(null)
    val syncSummary: StateFlow<SyncSummary?> = _syncSummary.asStateFlow()

    // Gemini Monthly AI Spending Habits Report
    private val _monthlyAiReportState = MutableStateFlow<MonthlyAiReportUiState>(MonthlyAiReportUiState.Idle)
    val monthlyAiReportState: StateFlow<MonthlyAiReportUiState> = _monthlyAiReportState.asStateFlow()

    fun generateMonthlyAiSpendingReport(
        year: Int? = null,
        month: Int? = null, // 1..12
        forceRefresh: Boolean = false
    ) {
        val targetCal = Calendar.getInstance()
        if (year != null && month != null) {
            targetCal.set(Calendar.YEAR, year)
            targetCal.set(Calendar.MONTH, month - 1)
        }
        val targetMonthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(targetCal.time)
        val targetYear = targetCal.get(Calendar.YEAR)

        val currentState = _monthlyAiReportState.value
        if (!forceRefresh && currentState is MonthlyAiReportUiState.Success &&
            currentState.report.monthName.equals(targetMonthName, ignoreCase = true) &&
            currentState.report.year == targetYear
        ) {
            return
        }

        viewModelScope.launch {
            _monthlyAiReportState.value = MonthlyAiReportUiState.Loading(targetMonthName)
            try {
                val startCal = (targetCal.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val endCal = (targetCal.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }

                val prevMonthCal = (targetCal.clone() as Calendar).apply {
                    add(Calendar.MONTH, -1)
                }
                val prevStartCal = (prevMonthCal.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val prevEndCal = (prevMonthCal.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }

                val allExp = repository.allExpenses.first()
                val allBudgets = repository.allBudgets.first()
                val allRecurring = repository.allRecurringExpenses.first()

                val monthExpenses = allExp.filter { it.date in startCal.timeInMillis..endCal.timeInMillis }
                val prevMonthExpenses = allExp.filter { it.date in prevStartCal.timeInMillis..prevEndCal.timeInMillis }

                val report = geminiService.generateMonthlySpendingHabitsReport(
                    monthName = targetMonthName,
                    year = targetYear,
                    monthExpenses = monthExpenses,
                    budgets = allBudgets,
                    recurringExpenses = allRecurring,
                    previousMonthExpenses = prevMonthExpenses
                )

                _monthlyAiReportState.value = MonthlyAiReportUiState.Success(report)
            } catch (e: Exception) {
                _monthlyAiReportState.value = MonthlyAiReportUiState.Error(e.message ?: "Failed to generate report")
            }
        }
    }

    fun signInWithGoogle(activityContext: android.content.Context, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val result = authManager.signInWithGoogle(activityContext)
            if (result.isSuccess) {
                val user = result.getOrNull()
                val name = user?.displayName ?: "User"
                val email = user?.email ?: ""
                _syncMessage.value = "Signed in as $name. Syncing with Firestore..."
                securityManager.signInGoogle(email)
                repository.addSyncLog("SIGN_IN", 0, "SUCCESS", email, "Connected Google Account: $name ($email)")
                // Automatically synchronize local Room DB records with Firebase Firestore
                syncFirestoreTransactions()
                onComplete?.invoke(true)
            } else {
                onComplete?.invoke(false)
            }
        }
    }

    fun syncFirestoreTransactions(onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            if (!firestoreSyncRepository.isUserSignedIn()) {
                _syncMessage.value = "Please sign in with Google to sync transactions to Firebase Firestore."
                onComplete?.invoke(false)
                return@launch
            }
            _isCloudSyncing.value = true
            _syncMessage.value = "Synchronizing ledger with Firebase Firestore..."
            try {
                val result = firestoreSyncRepository.syncWithRoom(repository)
                if (result.isSuccess) {
                    val summary = result.getOrNull()
                    _syncSummary.value = summary
                    securityManager.recordSyncSuccess()
                    _syncMessage.value = summary?.message ?: "Transactions successfully synchronized with Firestore!"
                    onComplete?.invoke(true)
                } else {
                    val err = result.exceptionOrNull()?.localizedMessage ?: "Sync failed"
                    _syncMessage.value = "Firestore sync error: $err"
                    onComplete?.invoke(false)
                }
            } catch (e: Exception) {
                _syncMessage.value = "Sync exception: ${e.message}"
                onComplete?.invoke(false)
            } finally {
                _isCloudSyncing.value = false
            }
        }
    }

    fun signOut(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            authManager.signOut()
            securityManager.signOutGoogle()
            repository.addSyncLog("SIGN_OUT", 0, "SUCCESS", "Local Vault", "Signed out of Google Account")
            _syncMessage.value = "Signed out. Local ledger remains intact."
            onComplete?.invoke()
        }
    }

    fun clearAuthError() {
        authManager.clearError()
    }

    fun addCardSuggestion(name: String, issuerBank: String, isRuPay: Boolean = false) {
        cardSuggestionsManager.addSuggestion(name, issuerBank, isRuPay)
    }

    fun updateCardSuggestion(id: String, newName: String, newBank: String, isRuPay: Boolean) {
        cardSuggestionsManager.updateSuggestion(id, newName, newBank, isRuPay)
    }

    fun deleteCardSuggestion(id: String) {
        cardSuggestionsManager.deleteSuggestion(id)
    }

    fun resetCardSuggestions() {
        cardSuggestionsManager.resetToDefaults()
    }

    // Recurring Expenses
    val allRecurringExpenses: StateFlow<List<RecurringExpenseEntity>> = repository.allRecurringExpenses.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val activeRecurringExpenses: StateFlow<List<RecurringExpenseEntity>> = repository.activeRecurringExpenses.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val upcomingRecurringBills: StateFlow<List<RecurringExpenseEntity>> = repository.activeRecurringExpenses.map { list ->
        list.sortedBy { it.nextDueDate }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _recurringActionMessage = MutableStateFlow<String?>(null)
    val recurringActionMessage: StateFlow<String?> = _recurringActionMessage.asStateFlow()

    fun clearRecurringActionMessage() {
        _recurringActionMessage.value = null
    }

    fun addRecurringExpense(
        title: String,
        amount: Double,
        category: String,
        frequency: String,
        dayOfMonth: Int = 1,
        dayOfWeek: Int = 2,
        paymentMethod: String = "UPI",
        bankName: String = "",
        creditCardName: String = "",
        notes: String = "",
        tag: String = "Subscription",
        autoCreateExpense: Boolean = true,
        startDate: Long = System.currentTimeMillis(),
        endDate: Long? = null
    ) {
        viewModelScope.launch {
            val initialDue = RecurringDateUtils.computeInitialDueDate(
                frequency = frequency,
                startDate = startDate,
                dayOfMonth = dayOfMonth,
                dayOfWeek = dayOfWeek
            )
            val entity = RecurringExpenseEntity(
                title = title.trim(),
                amount = amount,
                category = category,
                paymentMethod = paymentMethod,
                frequency = frequency,
                dayOfMonth = dayOfMonth,
                dayOfWeek = dayOfWeek,
                startDate = startDate,
                endDate = endDate,
                nextDueDate = initialDue,
                isActive = true,
                bankName = bankName,
                creditCardName = creditCardName,
                notes = notes.trim(),
                tag = tag,
                autoCreateExpense = autoCreateExpense
            )
            repository.insertRecurringExpense(entity)
            _recurringActionMessage.value = "Scheduled recurring: ${entity.title}"
            processDueRecurringExpenses()
        }
    }

    fun updateRecurringExpense(item: RecurringExpenseEntity) {
        viewModelScope.launch {
            repository.updateRecurringExpense(item)
            _recurringActionMessage.value = "Updated: ${item.title}"
            processDueRecurringExpenses()
        }
    }

    fun toggleRecurringExpenseActive(id: Long, isActive: Boolean) {
        viewModelScope.launch {
            repository.toggleRecurringExpenseActive(id, isActive)
            _recurringActionMessage.value = if (isActive) "Resumed recurring schedule" else "Paused recurring schedule"
        }
    }

    fun deleteRecurringExpense(id: Long) {
        viewModelScope.launch {
            repository.deleteRecurringExpenseById(id)
            _recurringActionMessage.value = "Deleted recurring schedule"
        }
    }

    fun executeRecurringExpenseNow(id: Long) {
        viewModelScope.launch {
            val created = repository.executeRecurringExpenseNow(id)
            if (created != null) {
                _recurringActionMessage.value = "Recorded bill: ${created.title} (${created.formattedAmount})"
            }
        }
    }

    fun processDueRecurringExpenses() {
        viewModelScope.launch {
            val created = repository.processDueRecurringExpenses()
            if (created.isNotEmpty()) {
                _recurringActionMessage.value = "Auto-created ${created.size} recurring bill(s): ${created.take(2).joinToString { it.title }}"
            }
        }
    }


    // Filter & Search States
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchScope = MutableStateFlow(SearchFilterScope.ALL)
    val searchScope: StateFlow<SearchFilterScope> = _searchScope.asStateFlow()

    private val _searchState = combine(_searchQuery, _searchScope) { q, s -> Pair(q, s) }

    private val _selectedCategoryFilter = MutableStateFlow<String?>("All")
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter.asStateFlow()

    private val _selectedPaymentFilter = MutableStateFlow<String?>("All")
    val selectedPaymentFilter: StateFlow<String?> = _selectedPaymentFilter.asStateFlow()

    private val _selectedTagFilter = MutableStateFlow<String?>("All")
    val selectedTagFilter: StateFlow<String?> = _selectedTagFilter.asStateFlow()

    private val _dateFilter = MutableStateFlow(DateFilterType.THIS_MONTH)
    val dateFilter: StateFlow<DateFilterType> = _dateFilter.asStateFlow()

    private val _customDateRange = MutableStateFlow<Pair<Long, Long>?>(null)
    val customDateRange: StateFlow<Pair<Long, Long>?> = _customDateRange.asStateFlow()

    private val _sortType = MutableStateFlow(SortType.DATE_NEWEST)
    val sortType: StateFlow<SortType> = _sortType.asStateFlow()

    // Multi-select & Undo state
    private val _selectedComparisonView = MutableStateFlow(ComparisonViewMode.BY_MONTH)
    val selectedComparisonView: StateFlow<ComparisonViewMode> = _selectedComparisonView.asStateFlow()

    fun setComparisonView(mode: ComparisonViewMode) {
        _selectedComparisonView.value = mode
    }

    private val _selectedExpenseIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedExpenseIds: StateFlow<Set<Long>> = _selectedExpenseIds.asStateFlow()

    private val _lastDeletedExpenses = MutableStateFlow<List<ExpenseEntity>>(emptyList())
    val lastDeletedExpenses: StateFlow<List<ExpenseEntity>> = _lastDeletedExpenses.asStateFlow()

    // AI & Tool States
    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _aiExtractedTransaction = MutableStateFlow<ExtractedTransaction?>(null)
    val aiExtractedTransaction: StateFlow<ExtractedTransaction?> = _aiExtractedTransaction.asStateFlow()

    private val _statementParseResult = MutableStateFlow<StatementParseResult?>(null)
    val statementParseResult: StateFlow<StatementParseResult?> = _statementParseResult.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    val allExpenses: StateFlow<List<ExpenseEntity>> = repository.allExpenses.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    val allBudgets: StateFlow<List<BudgetEntity>> = repository.allBudgets.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    val syncLogs = repository.syncLogs

    // AI Financial Advisor Chat State
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                role = MessageRole.MODEL,
                content = "Hello! I am your Gemini Financial Advisor. I have analyzed your spending patterns, categories, and budgets. Ask me anything—such as where you can save, how your budget is tracking, or a summary of this month's expenses!",
                modelTier = GeminiChatModel.FLASH
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatGenerating = MutableStateFlow(false)
    val isChatGenerating: StateFlow<Boolean> = _isChatGenerating.asStateFlow()

    private val _selectedChatModel = MutableStateFlow(GeminiChatModel.FLASH)
    val selectedChatModel: StateFlow<GeminiChatModel> = _selectedChatModel.asStateFlow()

    fun setChatModel(model: GeminiChatModel) {
        _selectedChatModel.value = model
    }

    fun clearChatHistory() {
        _chatMessages.value = listOf(
            ChatMessage(
                role = MessageRole.MODEL,
                content = "Chat history cleared. How can I help you manage your finances today?",
                modelTier = _selectedChatModel.value
            )
        )
    }

    fun sendChatMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty() || _isChatGenerating.value) return

        val userMessage = ChatMessage(
            role = MessageRole.USER,
            content = trimmed
        )
        val updatedHistory = _chatMessages.value + userMessage
        _chatMessages.value = updatedHistory
        _isChatGenerating.value = true

        viewModelScope.launch {
            val systemPrompt = buildFinancialContextSnapshot()
            val currentModel = _selectedChatModel.value
            val result = geminiService.chatCompletion(
                history = updatedHistory,
                systemInstruction = systemPrompt,
                model = currentModel
            )

            result.onSuccess { replyText ->
                _chatMessages.value = _chatMessages.value + ChatMessage(
                    role = MessageRole.MODEL,
                    content = replyText,
                    modelTier = currentModel
                )
            }.onFailure { err ->
                _chatMessages.value = _chatMessages.value + ChatMessage(
                    role = MessageRole.MODEL,
                    content = "I encountered an issue connecting with Gemini: ${err.localizedMessage ?: "Unknown error"}. If you haven't set your API key yet, please add GEMINI_API_KEY in the AI Studio Secrets panel.",
                    isError = true,
                    modelTier = currentModel
                )
            }
            _isChatGenerating.value = false
        }
    }

    fun buildFinancialContextSnapshot(): String {
        val allExp = allExpenses.value
        val allBud = allBudgets.value
        val now = Calendar.getInstance()
        val currentMonth = now.get(Calendar.MONTH)
        val currentYear = now.get(Calendar.YEAR)
        val periodStr = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(now.time)

        val cal = Calendar.getInstance()
        val thisMonthExpenses = allExp.filter {
            cal.timeInMillis = it.date
            cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
        }

        val totalSpendAllTime = allExp.sumOf { it.amount }
        val totalSpendThisMonth = thisMonthExpenses.sumOf { it.amount }
        val allTimeCount = allExp.size
        val thisMonthCount = thisMonthExpenses.size

        // All-Time Category Summaries (Computed directly from allExp, completely unfiltered)
        val allTimeCategories = allExp.groupBy { it.category }
            .mapValues { entry ->
                val sum = entry.value.sumOf { it.amount }
                val pct = if (totalSpendAllTime > 0) (sum / totalSpendAllTime * 100) else 0.0
                Triple(sum, pct, entry.value.size)
            }
            .toList()
            .sortedByDescending { it.second.first }

        val allTimeCategoryStr = if (allTimeCategories.isNotEmpty()) {
            allTimeCategories.joinToString("\n") { (cat, info) ->
                "- $cat: ₹${"%.2f".format(info.first)} (${"%.1f".format(info.second)}%, ${info.third} transactions)"
            }
        } else {
            "No categorized expenses logged yet."
        }

        // Current Month Category Summaries
        val thisMonthCategories = thisMonthExpenses.groupBy { it.category }
            .mapValues { entry ->
                val sum = entry.value.sumOf { it.amount }
                val pct = if (totalSpendThisMonth > 0) (sum / totalSpendThisMonth * 100) else 0.0
                Triple(sum, pct, entry.value.size)
            }
            .toList()
            .sortedByDescending { it.second.first }

        val thisMonthCategoryStr = if (thisMonthCategories.isNotEmpty()) {
            thisMonthCategories.joinToString("\n") { (cat, info) ->
                "- $cat: ₹${"%.2f".format(info.first)} (${"%.1f".format(info.second)}%, ${info.third} transactions)"
            }
        } else {
            "No expenses in current calendar month."
        }

        // Payment Methods Breakdown (All-Time)
        val allTimePaymentBreakdown = allExp.groupBy { it.paymentMethod }
            .map { (method, list) -> "$method: ₹${"%.2f".format(list.sumOf { it.amount })} (${list.size} txns)" }
            .joinToString(", ")

        // Top 5 Highest Expenses
        val topExpenses = allExp.sortedByDescending { it.amount }.take(5).map {
            "- ${it.title}: ₹${"%.2f".format(it.amount)} (${it.category}, ${it.paymentMethod}, on ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it.date))})"
        }

        // Top 5 Merchants / Store Titles
        val topMerchants = allExp.groupBy { it.title.trim() }
            .map { (title, list) -> Pair(title, list.sumOf { it.amount }) }
            .sortedByDescending { it.second }
            .take(5)
            .joinToString(", ") { "${it.first}: ₹${"%.2f".format(it.second)}" }

        // Active Budgets
        val budgetSummaries = allBud.map { b ->
            val spent = thisMonthExpenses.filter { it.category.equals(b.category, ignoreCase = true) }.sumOf { it.amount }
            val pct = if (b.monthlyLimit > 0) (spent / b.monthlyLimit * 100).toInt() else 0
            val remaining = b.monthlyLimit - spent
            val status = if (spent > b.monthlyLimit) "OVER BUDGET by ₹${"%.2f".format(spent - b.monthlyLimit)}" else "Within limit (₹${"%.2f".format(remaining)} left)"
            "- ${b.category}: limit ₹${"%.2f".format(b.monthlyLimit)}, spent ₹${"%.2f".format(spent)} ($pct%) [$status]"
        }

        // Recent 35 Transactions itemized
        val recentTransactions = allExp.take(35).map {
            val bankInfo = if (it.bankName.isNotBlank() || it.creditCardName.isNotBlank()) " [${listOf(it.bankName, it.creditCardName).filter { s -> s.isNotBlank() }.joinToString(" - ")}]" else ""
            val noteInfo = if (it.notes.isNotBlank()) " (Notes: ${it.notes})" else ""
            "- ${it.title} | ₹${"%.2f".format(it.amount)} | ${it.category} | ${it.paymentMethod}$bankInfo | ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it.date))}$noteInfo"
        }

        // Build Structured JSON for Room DB summaries
        val structuredJson = JSONObject().apply {
            put("period", periodStr)
            put("currency", "INR")
            put("spending_summary", JSONObject().apply {
                put("all_time_total", totalSpendAllTime)
                put("all_time_transactions_count", allTimeCount)
                put("current_month_total", totalSpendThisMonth)
                put("current_month_transactions_count", thisMonthCount)
            })

            put("category_summaries", JSONObject().apply {
                val allTimeArr = JSONArray()
                allTimeCategories.forEach { (cat, info) ->
                    allTimeArr.put(JSONObject().apply {
                        put("category", cat)
                        put("total_spent", info.first)
                        put("percentage", info.second)
                        put("transaction_count", info.third)
                    })
                }
                put("all_time", allTimeArr)

                val monthArr = JSONArray()
                thisMonthCategories.forEach { (cat, info) ->
                    monthArr.put(JSONObject().apply {
                        put("category", cat)
                        put("total_spent", info.first)
                        put("percentage", info.second)
                        put("transaction_count", info.third)
                    })
                }
                put("current_month", monthArr)
            })

            val budgetArr = JSONArray()
            allBud.forEach { b ->
                val spent = thisMonthExpenses.filter { it.category.equals(b.category, ignoreCase = true) }.sumOf { it.amount }
                val pct = if (b.monthlyLimit > 0) (spent / b.monthlyLimit * 100) else 0.0
                val remaining = b.monthlyLimit - spent
                val isOver = spent > b.monthlyLimit
                budgetArr.put(JSONObject().apply {
                    put("category", b.category)
                    put("monthly_limit", b.monthlyLimit)
                    put("spent_this_month", spent)
                    put("percentage_used", pct)
                    put("remaining", remaining)
                    put("is_over_budget", isOver)
                    put("status_label", if (isOver) "OVER BUDGET by ₹${"%.2f".format(spent - b.monthlyLimit)}" else "Within limit (₹${"%.2f".format(remaining)} left)")
                })
            }
            put("budget_status", budgetArr)

            val topExpArr = JSONArray()
            allExp.sortedByDescending { it.amount }.take(5).forEach {
                topExpArr.put(JSONObject().apply {
                    put("title", it.title)
                    put("amount", it.amount)
                    put("category", it.category)
                    put("payment_method", it.paymentMethod)
                    put("date", SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it.date)))
                })
            }
            put("top_single_expenses", topExpArr)

            val paymentArr = JSONArray()
            allExp.groupBy { it.paymentMethod }.forEach { (method, list) ->
                paymentArr.put(JSONObject().apply {
                    put("method", method)
                    put("total_spent", list.sumOf { it.amount })
                    put("transaction_count", list.size)
                })
            }
            put("payment_methods_breakdown", paymentArr)

            val topMerchArr = JSONArray()
            allExp.groupBy { it.title.trim() }
                .map { (title, list) -> Pair(title, list.sumOf { it.amount }) }
                .sortedByDescending { it.second }
                .take(5)
                .forEach {
                    topMerchArr.put(JSONObject().apply {
                        put("name", it.first)
                        put("total_spent", it.second)
                    })
                }
            put("top_merchants", topMerchArr)

            val recentTxArr = JSONArray()
            allExp.take(25).forEach {
                recentTxArr.put(JSONObject().apply {
                    put("title", it.title)
                    put("amount", it.amount)
                    put("category", it.category)
                    put("payment_method", it.paymentMethod)
                    put("date", SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it.date)))
                })
            }
            put("recent_transactions", recentTxArr)

            val recurringList = allRecurringExpenses.value.filter { it.isActive }
            val totalMonthlyRecurringCommitment = recurringList.sumOf {
                when (it.frequency.uppercase(Locale.ROOT)) {
                    "DAILY" -> it.amount * 30
                    "WEEKLY" -> it.amount * 4.33
                    "YEARLY" -> it.amount / 12
                    else -> it.amount
                }
            }
            val recurringArr = JSONArray()
            recurringList.forEach { r ->
                recurringArr.put(JSONObject().apply {
                    put("title", r.title)
                    put("amount", r.amount)
                    put("frequency", r.frequency)
                    put("category", r.category)
                    put("next_due", r.formattedNextDue)
                    put("days_until_due", r.daysUntilDue)
                    put("auto_create", r.autoCreateExpense)
                })
            }
            put("recurring_commitments", JSONObject().apply {
                put("monthly_estimated_total", totalMonthlyRecurringCommitment)
                put("active_count", recurringList.size)
                put("items", recurringArr)
            })
        }

        val recurringList = allRecurringExpenses.value.filter { it.isActive }
        val totalMonthlyRecurringCommitment = recurringList.sumOf {
            when (it.frequency.uppercase(Locale.ROOT)) {
                "DAILY" -> it.amount * 30
                "WEEKLY" -> it.amount * 4.33
                "YEARLY" -> it.amount / 12
                else -> it.amount
            }
        }

        return """
            You are a knowledgeable, friendly, and precise Personal Finance Advisor in the Expense Tracker Android app.
            
            ROLE & ACCURACY RULES:
            1. ACCURACY FIRST: Use the EXACT figures and data from the LIVE USER FINANCIAL CONTEXT (STRUCTURED JSON) provided below. Never make up numbers, hallucinate transactions, or contradict the user's data.
            2. TOTAL SPENDING: If the user asks about their total spending or expenditure, quote:
               - All-Time Total: ₹${"%.2f".format(totalSpendAllTime)} (spending_summary.all_time_total) across $allTimeCount logged transactions
               - Current Month Total: ₹${"%.2f".format(totalSpendThisMonth)} (spending_summary.current_month_total) across $thisMonthCount transactions for $periodStr.
            3. CATEGORY INQUIRIES: If the user asks how much they spent on a specific category (e.g. Food, Groceries, Shopping, Travel, Bills, etc.):
               - Look up the category in `category_summaries.all_time` and `category_summaries.current_month`.
               - Quote the exact total_spent, percentage, and transaction_count.
            4. BUDGET STATUS & OVER-LIMIT CHECKS:
               - Examine `budget_status` array.
               - For each budgeted category, note monthly_limit, spent_this_month, percentage_used, remaining, and is_over_budget.
               - If is_over_budget is true, explicitly alert the user about the overage.
            5. PAYMENT METHODS: Quote from `payment_methods_breakdown`.
            6. HIGHEST EXPENSES: If the user asks for their biggest/highest single expense, quote from `top_single_expenses`.
            7. RECURRING BILLS & SUBSCRIPTIONS: Reference `recurring_commitments` (~₹${"%.2f".format(totalMonthlyRecurringCommitment)}/month committed).
            8. ADVICE & COST-CUTTING: Offer realistic, actionable financial strategies tailored specifically to their top spending categories and peak transactions.
            9. GENERAL FINANCE: If asked general financial questions (investing, 50/30/20 rule, emergency funds, inflation, debt payoff), provide clear, educationally sound advice.
            10. FORMATTING: Use localized Indian Rupee formatting (₹) for all currency amounts. Use clean bullet points, bold key figures, and concise paragraphs for mobile readability.
            
            LIVE USER FINANCIAL CONTEXT (FROM ROOM DATABASE - STRUCTURED JSON):
            ```json
            ${structuredJson.toString(2)}
            ```

            TEXT SUMMARY FOR REFERENCE:
            - Current Period: $periodStr
            - Total All-Time Spending: ₹${"%.2f".format(totalSpendAllTime)} across $allTimeCount logged transactions
            - Total Current Month Spending: ₹${"%.2f".format(totalSpendThisMonth)} across $thisMonthCount transactions
            - Top Single Expenses:
            ${topExpenses.joinToString("\n").ifEmpty { "- None" }}
            - Top Merchants / Stores: ${topMerchants.ifEmpty { "None" }}
            - Payment Methods Breakdown: ${allTimePaymentBreakdown.ifEmpty { "None" }}
            - All-Time Category Summaries (Totals & Percentages):
            $allTimeCategoryStr
            - Current Month Category Summaries:
            $thisMonthCategoryStr
            - Active Budgets & Adherence:
            ${budgetSummaries.joinToString("\n").ifEmpty { "No category budgets set" }}
            - Active Recurring Bills & Subscriptions (~₹${"%.2f".format(totalMonthlyRecurringCommitment)}/month):
            ${recurringList.joinToString("\n") { "- ${it.title}: ₹${"%.2f".format(it.amount)} (${it.frequency}, next due ${it.formattedNextDue})" }.ifEmpty { "No recurring bills set" }}
            - Recent Transactions Ledger:
            ${recentTransactions.joinToString("\n").ifEmpty { "No recent transactions" }}
        """.trimIndent()
    }


    init {
        viewModelScope.launch {
            // Purge all false starter/sample seeded data immediately
            repository.purgeSampleSeedData()
            // Check and automatically create entries for any due recurring bills
            repository.processDueRecurringExpenses()
            // Silent auto sign-in if account is already authorized on this device
            authManager.attemptAutoSignIn()
        }
    }

    fun purgeSampleData() {
        viewModelScope.launch {
            repository.purgeSampleSeedData()
            _syncMessage.value = "All sample & false data removed successfully!"
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            _syncMessage.value = "All ledger data cleared successfully."
        }
    }

    // Filtered & Sorted Expenses Stream
    val filteredExpenses: StateFlow<List<ExpenseEntity>> = combine(
        repository.allExpenses,
        _searchState,
        _selectedCategoryFilter,
        _selectedPaymentFilter,
        _selectedTagFilter,
        _dateFilter,
        _customDateRange,
        _sortType
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val rawList = args[0] as List<ExpenseEntity>
        @Suppress("UNCHECKED_CAST")
        val searchState = args[1] as Pair<String, SearchFilterScope>
        val query = searchState.first
        val scope = searchState.second
        val catFilter = (args[2] as? String) ?: "All"
        val payFilter = (args[3] as? String) ?: "All"
        val tagFilter = (args[4] as? String) ?: "All"
        val dateF = args[5] as DateFilterType
        @Suppress("UNCHECKED_CAST")
        val customRange = args[6] as Pair<Long, Long>?
        val sortT = args[7] as SortType

        var list = rawList

        // 1. Search Query: Filter entries by Category, Date, or Merchant Name
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            val todayCal = Calendar.getInstance()
            val yestCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

            list = list.filter { expense ->
                // Merchant Name Matching (title / bank / credit card)
                val matchesMerchant = expense.title.lowercase().contains(q) ||
                        expense.bankName.lowercase().contains(q) ||
                        expense.creditCardName.lowercase().contains(q)

                // Category Matching (category / tag)
                val matchesCategory = expense.category.lowercase().contains(q) ||
                        expense.tag.lowercase().contains(q)

                // Notes Matching
                val matchesNotes = expense.notes.lowercase().contains(q)

                // Date Matching (formatted date strings, month names, year, day, today/yesterday)
                val dateObj = Date(expense.date)
                val sdf1 = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(dateObj).lowercase()
                val sdf2 = SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH).format(dateObj).lowercase()
                val sdf3 = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(dateObj).lowercase()
                val sdf4 = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).format(dateObj).lowercase()
                val sdf5 = SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH).format(dateObj).lowercase()
                val sdf6 = SimpleDateFormat("EEEE EEE", Locale.ENGLISH).format(dateObj).lowercase()
                val sdf7 = SimpleDateFormat("MMMM", Locale.ENGLISH).format(dateObj).lowercase()
                val sdf8 = SimpleDateFormat("MMM yyyy", Locale.ENGLISH).format(dateObj).lowercase()
                val sdf9 = SimpleDateFormat("d MMM", Locale.ENGLISH).format(dateObj).lowercase()
                val sdf10 = SimpleDateFormat("d MMMM", Locale.ENGLISH).format(dateObj).lowercase()
                val sdf11 = SimpleDateFormat("yyyy", Locale.ENGLISH).format(dateObj).lowercase()

                val expCal = Calendar.getInstance().apply { timeInMillis = expense.date }
                val isToday = expCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                        expCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)
                val isYesterday = expCal.get(Calendar.YEAR) == yestCal.get(Calendar.YEAR) &&
                        expCal.get(Calendar.DAY_OF_YEAR) == yestCal.get(Calendar.DAY_OF_YEAR)

                val matchesDate = sdf1.contains(q) ||
                        sdf2.contains(q) ||
                        sdf3.contains(q) ||
                        sdf4.contains(q) ||
                        sdf5.contains(q) ||
                        sdf6.contains(q) ||
                        sdf7.contains(q) ||
                        sdf8.contains(q) ||
                        sdf9.contains(q) ||
                        sdf10.contains(q) ||
                        (q.length >= 4 && sdf11.contains(q)) ||
                        (isToday && ("today".contains(q) || q.contains("today"))) ||
                        (isYesterday && ("yesterday".contains(q) || q.contains("yesterday")))

                when (scope) {
                    SearchFilterScope.MERCHANT -> matchesMerchant
                    SearchFilterScope.CATEGORY -> matchesCategory
                    SearchFilterScope.NOTES -> matchesNotes
                    SearchFilterScope.DATE -> matchesDate
                    SearchFilterScope.ALL -> matchesMerchant || matchesCategory || matchesNotes || matchesDate ||
                            expense.paymentMethod.lowercase().contains(q) ||
                            expense.amount.toString().contains(q)
                }
            }
        }

        // 2. Category Filter
        if (catFilter.isNotBlank() && catFilter != "All") {
            list = list.filter { it.category.equals(catFilter, ignoreCase = true) }
        }

        // 3. Payment Filter
        if (payFilter.isNotBlank() && payFilter != "All") {
            list = list.filter { it.paymentMethod.equals(payFilter, ignoreCase = true) }
        }

        // 4. Tag Filter
        if (tagFilter.isNotBlank() && tagFilter != "All") {
            list = list.filter { it.tag.equals(tagFilter, ignoreCase = true) }
        }

        // 5. Date Filter (Skip if explicitly searching specifically by Date across historical data)
        val skipDateFilter = scope == SearchFilterScope.DATE && query.isNotBlank()
        if (!skipDateFilter) {
            val (start, end) = getDateRangeForFilter(dateF, customRange)
            list = list.filter { it.date in start..end }
        }

        // 6. Sorting
        when (sortT) {
            SortType.DATE_NEWEST -> list.sortedByDescending { it.date }
            SortType.DATE_OLDEST -> list.sortedBy { it.date }
            SortType.AMOUNT_HIGHEST -> list.sortedByDescending { it.amount }
            SortType.AMOUNT_LOWEST -> list.sortedBy { it.amount }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Current Month Summary Metrics
    val monthExpenses: StateFlow<List<ExpenseEntity>> = repository.allExpenses.combine(_dateFilter) { list, dFilter ->
        val (start, end) = getDateRangeForFilter(DateFilterType.THIS_MONTH, null)
        list.filter { it.date in start..end }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Dynamic Category Summaries for Charts
    val categorySummaries: StateFlow<List<CategorySummary>> = combine(filteredExpenses, repository.allExpenses) { filtered, _ ->
        val total = filtered.sumOf { it.amount }
        filtered.groupBy { it.category }
            .map { (cat, expenses) ->
                val sum = expenses.sumOf { it.amount }
                val pct = if (total > 0) ((sum / total) * 100).toFloat() else 0f
                CategorySummary(
                    category = cat,
                    totalAmount = sum,
                    transactionCount = expenses.size,
                    percentage = pct
                )
            }
            .sortedByDescending { it.totalAmount }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Payment Method Breakdown
    val paymentSummaries: StateFlow<List<PaymentMethodSummary>> = filteredExpenses.combine(_searchQuery) { list, _ ->
        list.groupBy { it.paymentMethod }
            .map { (method, expenses) ->
                PaymentMethodSummary(
                    paymentMethod = method,
                    totalAmount = expenses.sumOf { it.amount },
                    transactionCount = expenses.size
                )
            }
            .sortedByDescending { it.totalAmount }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Daily Spending Trend
    val dailyTrend: StateFlow<List<DailySpending>> = combine(repository.allExpenses, _dateFilter, _customDateRange) { list, dFilter, customRange ->
        val (start, end) = getDateRangeForFilter(dFilter, customRange)
        val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
        val inRange = list.filter { it.date in start..end }
        
        // Ensure effective start does not start at 1970 for ALL_TIME or unbounded ranges
        val effectiveStart = if (start <= 0L) {
            val minTxDate = inRange.minOfOrNull { it.date }
            if (minTxDate != null) {
                val c = Calendar.getInstance().apply {
                    timeInMillis = minTxDate
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                c.timeInMillis
            } else {
                val c = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -30)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                c.timeInMillis
            }
        } else {
            start
        }

        val cal = Calendar.getInstance()
        cal.timeInMillis = effectiveStart
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val daysMap = mutableMapOf<String, Pair<Long, Double>>()
        var iterations = 0
        while (cal.timeInMillis <= end && iterations < 365) {
            val key = sdf.format(Date(cal.timeInMillis))
            daysMap[key] = Pair(cal.timeInMillis, 0.0)
            cal.add(Calendar.DAY_OF_YEAR, 1)
            iterations++
        }

        for (expense in inRange) {
            val key = sdf.format(Date(expense.date))
            val existing = daysMap[key]
            if (existing != null) {
                daysMap[key] = Pair(existing.first, existing.second + expense.amount)
            } else {
                daysMap[key] = Pair(expense.date, (daysMap[key]?.second ?: 0.0) + expense.amount)
            }
        }

        daysMap.values.sortedBy { it.first }.map { pair ->
            DailySpending(date = sdf.format(Date(pair.first)), timestamp = pair.first, amount = pair.second)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Budget Progress Tracker
    val budgetProgressList: StateFlow<List<BudgetProgressItem>> = combine(repository.allBudgets, monthExpenses) { budgets, monthTxs ->
        val spentByCategory = monthTxs.groupBy { it.category }
            .mapValues { it.value.sumOf { exp -> exp.amount } }

        budgets.map { budget ->
            val spent = spentByCategory[budget.category] ?: 0.0
            val remaining = (budget.monthlyLimit - spent).coerceAtLeast(0.0)
            val pct = if (budget.monthlyLimit > 0) ((spent / budget.monthlyLimit) * 100).toFloat() else 0f
            BudgetProgressItem(
                category = budget.category,
                monthlyLimit = budget.monthlyLimit,
                spentAmount = spent,
                remainingAmount = remaining,
                percentage = pct,
                isOverBudget = spent > budget.monthlyLimit
            )
        }.sortedByDescending { it.percentage }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Budget Alerts Monitoring State & Controls
    private val _lastBudgetAlertResults = MutableStateFlow<List<BudgetAlertResult>>(emptyList())
    val lastBudgetAlertResults: StateFlow<List<BudgetAlertResult>> = _lastBudgetAlertResults.asStateFlow()

    private val _budgetAlertsEnabledState = MutableStateFlow(BudgetNotificationService.isAlertsEnabled(application))
    val budgetAlertsEnabledState: StateFlow<Boolean> = _budgetAlertsEnabledState.asStateFlow()

    private val _budget80AlertEnabledState = MutableStateFlow(BudgetNotificationService.is80AlertEnabled(application))
    val budget80AlertEnabledState: StateFlow<Boolean> = _budget80AlertEnabledState.asStateFlow()

    private val _budget100AlertEnabledState = MutableStateFlow(BudgetNotificationService.is100AlertEnabled(application))
    val budget100AlertEnabledState: StateFlow<Boolean> = _budget100AlertEnabledState.asStateFlow()

    init {
        BudgetNotificationService.initNotificationChannel(application)
        viewModelScope.launch {
            budgetProgressList.collect { progressItems ->
                if (progressItems.isNotEmpty()) {
                    val alerts = BudgetNotificationService.evaluateBudgetsAndNotify(getApplication(), progressItems)
                    if (alerts.isNotEmpty()) {
                        _lastBudgetAlertResults.value = alerts
                    }
                }
            }
        }
    }

    fun evaluateBudgetsNow(): List<BudgetAlertResult> {
        val results = BudgetNotificationService.evaluateBudgetsAndNotify(getApplication(), budgetProgressList.value)
        if (results.isNotEmpty()) {
            _lastBudgetAlertResults.value = results
        }
        return results
    }

    fun isBudgetAlertsEnabled(): Boolean = BudgetNotificationService.isAlertsEnabled(getApplication())

    fun setBudgetAlertsEnabled(enabled: Boolean) {
        BudgetNotificationService.setAlertsEnabled(getApplication(), enabled)
        _budgetAlertsEnabledState.value = enabled
        if (enabled) {
            evaluateBudgetsNow()
        }
    }

    fun isBudget80AlertEnabled(): Boolean = BudgetNotificationService.is80AlertEnabled(getApplication())

    fun setBudget80AlertEnabled(enabled: Boolean) {
        BudgetNotificationService.set80AlertEnabled(getApplication(), enabled)
        _budget80AlertEnabledState.value = enabled
        if (enabled) {
            evaluateBudgetsNow()
        }
    }

    fun isBudget100AlertEnabled(): Boolean = BudgetNotificationService.is100AlertEnabled(getApplication())

    fun setBudget100AlertEnabled(enabled: Boolean) {
        BudgetNotificationService.set100AlertEnabled(getApplication(), enabled)
        _budget100AlertEnabledState.value = enabled
        if (enabled) {
            evaluateBudgetsNow()
        }
    }

    fun hasBudgetNotificationPermission(): Boolean = BudgetNotificationService.hasNotificationPermission(getApplication())

    fun sendTestBudgetNotification(threshold: BudgetAlertThreshold, category: String = "Food & Dining"): Boolean {
        return BudgetNotificationService.sendTestNotification(getApplication(), threshold, category)
    }

    fun resetBudgetAlertsHistory() {
        BudgetNotificationService.resetAllAlertsHistory(getApplication())
        _lastBudgetAlertResults.value = emptyList()
        evaluateBudgetsNow()
    }

    // Monthly Spending Habits Summary (Current vs Previous Month & Top Category)
    val monthlySpendingHabits: StateFlow<MonthlySpendingHabitsSummary> = combine(
        repository.allExpenses,
        repository.allBudgets
    ) { allExp, budgets ->
        val now = Calendar.getInstance()
        val currentDay = now.get(Calendar.DAY_OF_MONTH)
        val totalDaysInCurrentMonth = now.getActualMaximum(Calendar.DAY_OF_MONTH)

        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val currentMonthName = monthFormat.format(now.time)

        val curMonthCal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val curMonthStart = curMonthCal.timeInMillis

        val curMonthEndCal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val curMonthEnd = curMonthEndCal.timeInMillis

        val prevMonthCal = Calendar.getInstance().apply {
            add(Calendar.MONTH, -1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val prevMonthStart = prevMonthCal.timeInMillis
        val prevMonthName = monthFormat.format(prevMonthCal.time)

        val prevMonthEndCal = (prevMonthCal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val prevMonthEnd = prevMonthEndCal.timeInMillis

        val currentMonthExpenses = allExp.filter { it.date in curMonthStart..curMonthEnd }
        val previousMonthExpenses = allExp.filter { it.date in prevMonthStart..prevMonthEnd }

        val curTotal = currentMonthExpenses.sumOf { it.amount }
        val prevTotal = previousMonthExpenses.sumOf { it.amount }
        val curTxCount = currentMonthExpenses.size
        val prevTxCount = previousMonthExpenses.size

        val diffAmount = curTotal - prevTotal
        val isSpendingIncreased = diffAmount > 0
        val isPrevAvailable = previousMonthExpenses.isNotEmpty() || prevTotal > 0

        val pctChange = if (prevTotal > 0) {
            ((curTotal - prevTotal) / prevTotal) * 100.0
        } else if (curTotal > 0) {
            100.0
        } else {
            0.0
        }

        val dailyAvg = if (currentDay > 0) curTotal / currentDay else 0.0
        val projectedTotal = dailyAvg * totalDaysInCurrentMonth

        val activeDays = currentMonthExpenses.map {
            val c = Calendar.getInstance().apply { timeInMillis = it.date }
            c.get(Calendar.DAY_OF_YEAR)
        }.distinct().size

        val topCategories = currentMonthExpenses.groupBy { it.category }
            .map { (cat, exps) ->
                val sum = exps.sumOf { it.amount }
                val pct = if (curTotal > 0) ((sum / curTotal) * 100).toFloat() else 0f
                CategorySummary(
                    category = cat,
                    totalAmount = sum,
                    transactionCount = exps.size,
                    percentage = pct
                )
            }
            .sortedByDescending { it.totalAmount }

        val topCategory = topCategories.firstOrNull()

        val paymentBreakdown = currentMonthExpenses.groupBy { it.paymentMethod }
            .map { (method, exps) ->
                val sum = exps.sumOf { it.amount }
                val pct = if (curTotal > 0) ((sum / curTotal) * 100).toFloat() else 0f
                PaymentMethodSummary(
                    paymentMethod = method,
                    totalAmount = sum,
                    transactionCount = exps.size,
                    percentage = pct
                )
            }
            .sortedByDescending { it.totalAmount }

        val topPayment = paymentBreakdown.firstOrNull()
        val highestExpense = currentMonthExpenses.maxByOrNull { it.amount }

        val totalBudget = budgets.sumOf { it.monthlyLimit }
        val budgetStatus = if (totalBudget > 0) {
            val remaining = (totalBudget - curTotal).coerceAtLeast(0.0)
            val pctUsed = ((curTotal / totalBudget) * 100).toFloat()
            MonthlyBudgetHabitStatus(
                totalBudget = totalBudget,
                spentAmount = curTotal,
                remainingBudget = remaining,
                percentageUsed = pctUsed,
                isOverBudget = curTotal > totalBudget
            )
        } else null

        val habitInsight = when {
            curTotal == 0.0 -> "No expenses logged yet for $currentMonthName. Add expenses or scan receipts to generate your spending habit insights."
            topCategory != null && isPrevAvailable && diffAmount < 0 -> {
                val absDiff = kotlin.math.abs(diffAmount)
                "Great savings momentum! You've spent ${ExpenseEntity.formatInr(absDiff)} (${String.format(Locale.getDefault(), "%.1f", kotlin.math.abs(pctChange))}%) less than $prevMonthName so far. Top expenditure is ${topCategory.category} (${String.format(Locale.getDefault(), "%.1f", topCategory.percentage)}%)."
            }
            topCategory != null && isPrevAvailable && diffAmount > 0 -> {
                "Spending is trending ${String.format(Locale.getDefault(), "%.1f", pctChange)}% higher than $prevMonthName. ${topCategory.category} is your highest category at ${ExpenseEntity.formatInr(topCategory.totalAmount)} (${String.format(Locale.getDefault(), "%.1f", topCategory.percentage)}% of total)."
            }
            topCategory != null -> {
                "Your primary spending driver this month is ${topCategory.category} at ${ExpenseEntity.formatInr(topCategory.totalAmount)}, accounting for ${String.format(Locale.getDefault(), "%.1f", topCategory.percentage)}% of your monthly expenditure."
            }
            else -> "Tracking $curTxCount transactions across $activeDays active spending days this month."
        }

        MonthlySpendingHabitsSummary(
            currentMonthName = currentMonthName,
            currentMonthTotal = curTotal,
            currentMonthTxCount = curTxCount,
            currentMonthDailyAverage = dailyAvg,
            currentMonthDaysElapsed = currentDay,
            currentMonthDaysTotal = totalDaysInCurrentMonth,
            previousMonthName = prevMonthName,
            previousMonthTotal = prevTotal,
            previousMonthTxCount = prevTxCount,
            differenceAmount = diffAmount,
            percentageChange = pctChange,
            isSpendingIncreased = isSpendingIncreased,
            isPreviousMonthAvailable = isPrevAvailable,
            topCategory = topCategory,
            topCategories = topCategories,
            topPaymentMethod = topPayment,
            paymentBreakdown = paymentBreakdown,
            highestSingleExpense = highestExpense,
            activeSpendingDaysCount = activeDays,
            projectedMonthEndTotal = projectedTotal,
            budgetStatus = budgetStatus,
            habitInsightMessage = habitInsight
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MonthlySpendingHabitsSummary(
            currentMonthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date()),
            currentMonthTotal = 0.0,
            currentMonthTxCount = 0,
            currentMonthDailyAverage = 0.0,
            currentMonthDaysElapsed = 1,
            currentMonthDaysTotal = 30,
            previousMonthName = "Previous Month",
            previousMonthTotal = 0.0,
            previousMonthTxCount = 0,
            differenceAmount = 0.0,
            percentageChange = 0.0,
            isSpendingIncreased = false,
            isPreviousMonthAvailable = false,
            topCategory = null,
            topCategories = emptyList(),
            topPaymentMethod = null,
            paymentBreakdown = emptyList(),
            highestSingleExpense = null,
            activeSpendingDaysCount = 0,
            projectedMonthEndTotal = 0.0,
            budgetStatus = null,
            habitInsightMessage = "Loading monthly spending habits..."
        )
    )

    // 6-Month Spending Trends & Financial Growth Stream
    val sixMonthGrowthSummary: StateFlow<SixMonthGrowthSummary> = repository.allExpenses.map { allTxs ->
        calculateLast6MonthsTrend(allTxs)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = calculateLast6MonthsTrend(emptyList())
    )

    internal fun calculateLast6MonthsTrend(allTxs: List<ExpenseEntity>): SixMonthGrowthSummary {
        val monthKeys = mutableListOf<Triple<Int, Int, Calendar>>()
        for (i in 5 downTo 0) {
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

        var prevMonthSpent: Double? = null
        val trendPoints = monthKeys.map { (y, m, c) ->
            val txsInMonth = txMap[Pair(y, m)] ?: emptyList()
            val totalSpent = txsInMonth.sumOf { it.amount }
            val count = txsInMonth.size
            val momChange = if (prevMonthSpent != null && prevMonthSpent!! > 0) {
                ((totalSpent - prevMonthSpent!!) / prevMonthSpent!!) * 100.0
            } else {
                0.0
            }
            prevMonthSpent = totalSpent

            MonthlyTrendData(
                monthKey = String.format(Locale.ENGLISH, "%04d-%02d", y, m),
                shortMonth = shortFormat.format(c.time),
                fullMonthName = fullFormat.format(c.time),
                year = y,
                month = m,
                totalSpent = totalSpent,
                transactionCount = count,
                momChangePercent = momChange,
                isIncreased = momChange > 0
            )
        }

        val total6Months = trendPoints.sumOf { it.totalSpent }
        val avg6Months = if (trendPoints.isNotEmpty()) total6Months / trendPoints.size else 0.0
        val nonZeroMonths = trendPoints.filter { it.totalSpent > 0 }
        val firstActiveMonth = nonZeroMonths.firstOrNull()?.totalSpent ?: (trendPoints.firstOrNull()?.totalSpent ?: 0.0)
        val latestMonthSpent = trendPoints.lastOrNull()?.totalSpent ?: 0.0
        val overallChange = if (firstActiveMonth > 0) {
            ((latestMonthSpent - firstActiveMonth) / firstActiveMonth) * 100.0
        } else {
            0.0
        }

        val highest = trendPoints.maxByOrNull { it.totalSpent }?.shortMonth ?: "N/A"
        val lowest = trendPoints.minByOrNull { it.totalSpent }?.shortMonth ?: "N/A"

        return SixMonthGrowthSummary(
            totalSpent6Months = total6Months,
            averageMonthlySpend = avg6Months,
            latestMonthSpend = latestMonthSpent,
            firstMonthSpend = firstActiveMonth,
            overallGrowthPercent = overallChange,
            isSpendingDecreased = latestMonthSpent <= firstActiveMonth,
            highestSpendMonth = highest,
            lowestSpendMonth = lowest,
            trendPoints = trendPoints
        )
    }

    // 1-Year Spending Trends & Financial Growth Stream
    val oneYearGrowthSummary: StateFlow<OneYearGrowthSummary> = repository.allExpenses.map { allTxs ->
        calculateLast12MonthsTrend(allTxs)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = calculateLast12MonthsTrend(emptyList())
    )

    internal fun calculateLast12MonthsTrend(allTxs: List<ExpenseEntity>): OneYearGrowthSummary {
        val monthKeys = mutableListOf<Triple<Int, Int, Calendar>>()
        for (i in 11 downTo 0) {
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

        var prevMonthSpent: Double? = null
        val trendPoints = monthKeys.map { (y, m, c) ->
            val txsInMonth = txMap[Pair(y, m)] ?: emptyList()
            val totalSpent = txsInMonth.sumOf { it.amount }
            val count = txsInMonth.size
            val momChange = if (prevMonthSpent != null && prevMonthSpent!! > 0) {
                ((totalSpent - prevMonthSpent!!) / prevMonthSpent!!) * 100.0
            } else {
                0.0
            }
            prevMonthSpent = totalSpent

            MonthlyTrendData(
                monthKey = String.format(Locale.ENGLISH, "%04d-%02d", y, m),
                shortMonth = shortFormat.format(c.time),
                fullMonthName = fullFormat.format(c.time),
                year = y,
                month = m,
                totalSpent = totalSpent,
                transactionCount = count,
                momChangePercent = momChange,
                isIncreased = momChange > 0
            )
        }

        val total1Year = trendPoints.sumOf { it.totalSpent }
        val avg1Year = if (trendPoints.isNotEmpty()) total1Year / trendPoints.size else 0.0
        val nonZeroMonths = trendPoints.filter { it.totalSpent > 0 }
        val firstActiveMonth = nonZeroMonths.firstOrNull()?.totalSpent ?: (trendPoints.firstOrNull()?.totalSpent ?: 0.0)
        val latestMonthSpent = trendPoints.lastOrNull()?.totalSpent ?: 0.0
        val overallChange = if (firstActiveMonth > 0) {
            ((latestMonthSpent - firstActiveMonth) / firstActiveMonth) * 100.0
        } else {
            0.0
        }

        val highest = trendPoints.maxByOrNull { it.totalSpent }?.shortMonth ?: "N/A"
        val lowest = trendPoints.minByOrNull { it.totalSpent }?.shortMonth ?: "N/A"

        return OneYearGrowthSummary(
            totalSpent1Year = total1Year,
            averageMonthlySpend = avg1Year,
            latestMonthSpend = latestMonthSpent,
            firstMonthSpend = firstActiveMonth,
            overallGrowthPercent = overallChange,
            isSpendingDecreased = latestMonthSpent <= firstActiveMonth,
            highestSpendMonth = highest,
            lowestSpendMonth = lowest,
            trendPoints = trendPoints
        )
    }

    // Selected Category Filter for Budget vs Trend Analysis (null for All Categories)
    private val _selectedTrendCategory = MutableStateFlow<String?>(null)
    val selectedTrendCategory: StateFlow<String?> = _selectedTrendCategory.asStateFlow()

    fun setSelectedTrendCategory(category: String?) {
        _selectedTrendCategory.value = category
    }

    // 6-Month Spending Trends vs Set Budget Limits Stream
    val monthlyBudgetTrend6M: StateFlow<MonthlyBudgetTrendSummary> = combine(
        repository.allExpenses,
        repository.allBudgets,
        _selectedTrendCategory
    ) { allTxs, budgets, selectedCat ->
        calculateMonthlyBudgetTrends(allTxs, budgets, 6, selectedCat)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = calculateMonthlyBudgetTrends(emptyList(), emptyList(), 6, null)
    )

    // 1-Year (12-Month) Spending Trends vs Set Budget Limits Stream
    val monthlyBudgetTrend1Y: StateFlow<MonthlyBudgetTrendSummary> = combine(
        repository.allExpenses,
        repository.allBudgets,
        _selectedTrendCategory
    ) { allTxs, budgets, selectedCat ->
        calculateMonthlyBudgetTrends(allTxs, budgets, 12, selectedCat)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = calculateMonthlyBudgetTrends(emptyList(), emptyList(), 12, null)
    )

    internal fun calculateMonthlyBudgetTrends(
        allTxs: List<ExpenseEntity>,
        budgets: List<BudgetEntity>,
        monthsCount: Int,
        categoryFilter: String? = null
    ): MonthlyBudgetTrendSummary = MonthlyBudgetTrendCalculator.calculateMonthlyBudgetTrends(allTxs, budgets, monthsCount, categoryFilter)

    // Selected Month for Category-Grouped Budget Analysis (null = Current Month)
    private val _selectedCategoryReportMonth = MutableStateFlow<Pair<Int, Int>?>(null)
    val selectedCategoryReportMonth: StateFlow<Pair<Int, Int>?> = _selectedCategoryReportMonth.asStateFlow()

    fun setSelectedCategoryReportMonth(year: Int, month: Int) {
        _selectedCategoryReportMonth.value = Pair(year, month)
    }

    // Category-Grouped Monthly Spending vs Set Budget Limits Report
    val categoryBudgetReport: StateFlow<MonthlyCategoryBudgetReport> = combine(
        repository.allExpenses,
        repository.allBudgets,
        _selectedCategoryReportMonth
    ) { allTxs, budgets, selectedMonthPair ->
        val now = Calendar.getInstance()
        val y = selectedMonthPair?.first ?: now.get(Calendar.YEAR)
        val m = selectedMonthPair?.second ?: (now.get(Calendar.MONTH) + 1)
        MonthlyBudgetTrendCalculator.calculateCategoryBudgetReport(allTxs, budgets, y, m)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MonthlyBudgetTrendCalculator.calculateCategoryBudgetReport(
            emptyList(),
            emptyList(),
            Calendar.getInstance().get(Calendar.YEAR),
            Calendar.getInstance().get(Calendar.MONTH) + 1
        )
    )

    // Category-Grouped Multi-Month Trend Stream (6 Months)
    val categoryMultiMonthTrends: StateFlow<List<CategoryMultiMonthTrendPoint>> = combine(
        repository.allExpenses,
        repository.allBudgets
    ) { allTxs, budgets ->
        MonthlyBudgetTrendCalculator.calculateCategoryMultiMonthTrends(allTxs, budgets, 6)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MonthlyBudgetTrendCalculator.calculateCategoryMultiMonthTrends(emptyList(), emptyList(), 6)
    )

    fun getMonthlyExpenseReportDetail(year: Int, month: Int): MonthlyExpenseReportDetail {
        val all = allExpenses.value
        val cal = Calendar.getInstance()
        val monthTxs = all.filter {
            cal.timeInMillis = it.date
            cal.get(Calendar.YEAR) == year && (cal.get(Calendar.MONTH) + 1) == month
        }.sortedByDescending { it.date }

        val totalSpent = monthTxs.sumOf { it.amount }
        val daysInMonth = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
        }.getActualMaximum(Calendar.DAY_OF_MONTH)
        val dailyAvg = if (daysInMonth > 0) totalSpent / daysInMonth else 0.0

        val catBreakdown = monthTxs.groupBy { it.category }
            .map { (cat, list) ->
                val sum = list.sumOf { it.amount }
                val pct = if (totalSpent > 0) ((sum / totalSpent) * 100).toFloat() else 0f
                CategoryMonthlySpend(cat, sum, list.size, pct)
            }.sortedByDescending { it.amount }

        val merchBreakdown = monthTxs.groupBy { it.title.trim().ifBlank { "Unspecified Merchant" } }
            .map { (merch, list) ->
                val sum = list.sumOf { it.amount }
                val pct = if (totalSpent > 0) ((sum / totalSpent) * 100).toFloat() else 0f
                MerchantMonthlySpend(merch, sum, list.size, pct)
            }.sortedByDescending { it.amount }

        val payBreakdown = monthTxs.groupBy { it.paymentMethod }
            .mapValues { it.value.sumOf { exp -> exp.amount } }

        val calHeader = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }

        return MonthlyExpenseReportDetail(
            year = year,
            month = month,
            monthName = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(calHeader.time),
            shortName = SimpleDateFormat("MMM yyyy", Locale.ENGLISH).format(calHeader.time),
            totalSpent = totalSpent,
            transactionCount = monthTxs.size,
            dailyAverage = dailyAvg,
            highestExpense = monthTxs.maxByOrNull { it.amount },
            categoryBreakdown = catBreakdown,
            merchantBreakdown = merchBreakdown,
            paymentBreakdown = payBreakdown,
            expenses = monthTxs
        )
    }

    fun getAllMonthlyReports(): List<MonthlyExpenseReportDetail> {
        val months = getAvailableMonths()
        return months.map { getMonthlyExpenseReportDetail(it.year, it.month) }
    }

    // Multi-Month Comparative Analysis Stream
    val monthlyComparisonReport: StateFlow<MonthlyComparisonReport> = repository.allExpenses.map { allTxs ->
        calculateMonthlyComparison(allTxs)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = calculateMonthlyComparison(emptyList())
    )

    internal fun calculateMonthlyComparison(allTxs: List<ExpenseEntity>): MonthlyComparisonReport {
        if (allTxs.isEmpty()) {
            return MonthlyComparisonReport(
                months = emptyList(),
                categoryCrossMonth = emptyList(),
                merchantCrossMonth = emptyList(),
                highestSpendMonth = null,
                lowestSpendMonth = null,
                averageMonthlySpend = 0.0,
                totalAllTimeSpend = 0.0,
                overallTopCategory = null,
                overallTopMerchant = null
            )
        }

        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH)
        val shortMonthFormat = SimpleDateFormat("MMM yyyy", Locale.ENGLISH)

        // Group transactions by year and month
        val groupedByMonth = allTxs.groupBy { exp ->
            val cal = Calendar.getInstance().apply { timeInMillis = exp.date }
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH) + 1
            Pair(y, m)
        }

        val monthItems = groupedByMonth.map { (ym, txs) ->
            val (year, month) = ym
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            val monthKey = String.format(Locale.ENGLISH, "%04d-%02d", year, month)
            val monthName = monthFormat.format(cal.time)
            val shortName = shortMonthFormat.format(cal.time)
            val totalMonthSpend = txs.sumOf { it.amount }

            // Category breakdown for this month
            val catSpends = txs.groupBy { it.category }
                .map { (cat, cList) ->
                    val sum = cList.sumOf { it.amount }
                    val pct = if (totalMonthSpend > 0) ((sum / totalMonthSpend) * 100).toFloat() else 0f
                    CategoryMonthlySpend(cat, sum, cList.size, pct)
                }
                .sortedByDescending { it.amount }

            // Merchant breakdown for this month
            val merchSpends = txs.groupBy { it.title.trim().ifBlank { "Unspecified Merchant" } }
                .map { (merch, mList) ->
                    val sum = mList.sumOf { it.amount }
                    val pct = if (totalMonthSpend > 0) ((sum / totalMonthSpend) * 100).toFloat() else 0f
                    MerchantMonthlySpend(merch, sum, mList.size, pct)
                }
                .sortedByDescending { it.amount }

            MonthlyBreakdownItem(
                monthKey = monthKey,
                monthName = monthName,
                shortName = shortName,
                year = year,
                month = month,
                totalAmount = totalMonthSpend,
                transactionCount = txs.size,
                categorySpends = catSpends,
                merchantSpends = merchSpends,
                topCategory = catSpends.firstOrNull(),
                topMerchant = merchSpends.firstOrNull()
            )
        }.sortedWith(compareByDescending<MonthlyBreakdownItem> { it.year }.thenByDescending { it.month })

        val totalAllTime = monthItems.sumOf { it.totalAmount }
        val avgMonthly = if (monthItems.isNotEmpty()) totalAllTime / monthItems.size else 0.0
        val highestMonth = monthItems.maxByOrNull { it.totalAmount }
        val lowestMonth = monthItems.minByOrNull { it.totalAmount }

        // Category Cross Month
        val allCategories = allTxs.map { it.category }.distinct()
        val categoryCrossMonth = allCategories.map { cat ->
            val catTxs = allTxs.filter { it.category == cat }
            val monthlyMap = mutableMapOf<String, Double>()
            for (m in monthItems) {
                val catSumInMonth = catTxs.filter {
                    val c = Calendar.getInstance().apply { timeInMillis = it.date }
                    c.get(Calendar.YEAR) == m.year && (c.get(Calendar.MONTH) + 1) == m.month
                }.sumOf { it.amount }
                if (catSumInMonth > 0) {
                    monthlyMap[m.monthKey] = catSumInMonth
                }
            }
            val peakEntry = monthlyMap.maxByOrNull { it.value }
            val peakKey = peakEntry?.key ?: ""
            val peakName = monthItems.find { it.monthKey == peakKey }?.shortName ?: peakKey
            val peakAmount = peakEntry?.value ?: 0.0

            CategoryCrossMonthItem(
                category = cat,
                totalAllMonths = catTxs.sumOf { it.amount },
                monthlySpends = monthlyMap,
                peakMonthKey = peakKey,
                peakMonthName = peakName,
                peakMonthAmount = peakAmount
            )
        }.sortedByDescending { it.totalAllMonths }

        // Merchant Cross Month (Top 30 merchants by total spend)
        val allMerchants = allTxs.map { it.title.trim().ifBlank { "Unspecified Merchant" } }.distinct()
        val merchantCrossMonth = allMerchants.map { merch ->
            val merchTxs = allTxs.filter { (it.title.trim().ifBlank { "Unspecified Merchant" }).equals(merch, ignoreCase = true) }
            val monthlyMap = mutableMapOf<String, Double>()
            for (m in monthItems) {
                val mSum = merchTxs.filter {
                    val c = Calendar.getInstance().apply { timeInMillis = it.date }
                    c.get(Calendar.YEAR) == m.year && (c.get(Calendar.MONTH) + 1) == m.month
                }.sumOf { it.amount }
                if (mSum > 0) {
                    monthlyMap[m.monthKey] = mSum
                }
            }
            val peakEntry = monthlyMap.maxByOrNull { it.value }
            val peakKey = peakEntry?.key ?: ""
            val peakName = monthItems.find { it.monthKey == peakKey }?.shortName ?: peakKey
            val peakAmount = peakEntry?.value ?: 0.0

            MerchantCrossMonthItem(
                merchant = merch,
                totalAllMonths = merchTxs.sumOf { it.amount },
                monthlySpends = monthlyMap,
                peakMonthKey = peakKey,
                peakMonthName = peakName,
                peakMonthAmount = peakAmount
            )
        }.sortedByDescending { it.totalAllMonths }.take(30)

        return MonthlyComparisonReport(
            months = monthItems,
            categoryCrossMonth = categoryCrossMonth,
            merchantCrossMonth = merchantCrossMonth,
            highestSpendMonth = highestMonth,
            lowestSpendMonth = lowestMonth,
            averageMonthlySpend = avgMonthly,
            totalAllTimeSpend = totalAllTime,
            overallTopCategory = categoryCrossMonth.firstOrNull()?.category,
            overallTopMerchant = merchantCrossMonth.firstOrNull()?.merchant
        )
    }

    // Search and filter modifiers
    fun setSearchQuery(q: String) { _searchQuery.value = q }
    fun setSearchScope(scope: SearchFilterScope) { _searchScope.value = scope }
    fun filterByCategory(category: String) {
        _searchScope.value = SearchFilterScope.CATEGORY
        _searchQuery.value = category
    }
    fun filterByMerchant(merchant: String) {
        _searchScope.value = SearchFilterScope.MERCHANT
        _searchQuery.value = merchant
    }
    fun filterByDate(dateQuery: String) {
        _searchScope.value = SearchFilterScope.DATE
        _searchQuery.value = dateQuery
    }
    fun clearSearch() {
        _searchQuery.value = ""
        _searchScope.value = SearchFilterScope.ALL
    }
    fun clearAllFilters() {
        _searchQuery.value = ""
        _searchScope.value = SearchFilterScope.ALL
        _selectedCategoryFilter.value = "All"
        _selectedPaymentFilter.value = "All"
        _selectedTagFilter.value = "All"
        _dateFilter.value = DateFilterType.THIS_MONTH
        _customDateRange.value = null
    }
    fun setCategoryFilter(c: String?) { _selectedCategoryFilter.value = c }
    fun setPaymentFilter(p: String?) { _selectedPaymentFilter.value = p }
    fun setTagFilter(t: String?) { _selectedTagFilter.value = t }
    fun setDateFilter(df: DateFilterType) { _dateFilter.value = df }
    fun setCustomDateRange(range: Pair<Long, Long>?) {
        _customDateRange.value = range
        _dateFilter.value = DateFilterType.CUSTOM
    }
    fun setSortType(s: SortType) { _sortType.value = s }

    fun toggleExpenseSelection(id: Long) {
        val current = _selectedExpenseIds.value.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        _selectedExpenseIds.value = current
    }

    fun selectAllExpenses() {
        val allIds = filteredExpenses.value.map { it.id }.toSet()
        _selectedExpenseIds.value = allIds
    }

    fun clearExpenseSelection() {
        _selectedExpenseIds.value = emptySet()
    }

    fun deleteSelectedExpenses() {
        val ids = _selectedExpenseIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val toDelete = filteredExpenses.value.filter { ids.contains(it.id) }
            _lastDeletedExpenses.value = toDelete
            repository.deleteExpensesByIds(ids)
            clearExpenseSelection()
        }
    }

    fun undoDelete() {
        viewModelScope.launch {
            val restoreList = _lastDeletedExpenses.value
            if (restoreList.isNotEmpty()) {
                repository.insertExpenses(restoreList)
                _lastDeletedExpenses.value = emptyList()
            }
        }
    }

    // CRUD Actions
    fun addExpense(
        title: String,
        amount: Double,
        category: String,
        paymentMethod: String,
        date: Long,
        notes: String = "",
        tag: String = "Personal",
        receiptUri: String? = null,
        bankName: String = "",
        creditCardName: String = ""
    ) {
        viewModelScope.launch {
            repository.insertExpense(
                ExpenseEntity(
                    title = title.trim(),
                    amount = amount,
                    category = category,
                    paymentMethod = paymentMethod,
                    date = date,
                    notes = notes.trim(),
                    tag = tag,
                    receiptUri = receiptUri,
                    bankName = bankName.trim(),
                    creditCardName = creditCardName.trim(),
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun updateExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.updateExpense(expense)
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            _lastDeletedExpenses.value = listOf(expense)
            repository.deleteExpense(expense)
        }
    }

    fun saveBudget(category: String, monthlyLimit: Double) {
        viewModelScope.launch {
            repository.insertOrUpdateBudget(
                BudgetEntity(
                    category = category,
                    monthlyLimit = monthlyLimit
                )
            )
        }
    }

    fun deleteBudget(category: String) {
        viewModelScope.launch {
            repository.deleteBudget(category)
        }
    }

    // AI & Scanner Operations
    fun scanReceiptImage(bitmap: Bitmap, onResult: (ExtractedTransaction?) -> Unit) {
        viewModelScope.launch {
            _isAiLoading.value = true
            val res = geminiService.parseReceiptImage(bitmap)
            _aiExtractedTransaction.value = res
            _isAiLoading.value = false
            onResult(res)
        }
    }

    fun processVoiceExpense(spokenText: String, onResult: (ExtractedTransaction?) -> Unit) {
        viewModelScope.launch {
            _isAiLoading.value = true
            val res = geminiService.parseVoiceExpense(spokenText)
            _aiExtractedTransaction.value = res
            _isAiLoading.value = false
            onResult(res)
        }
    }

    fun parseStatementText(text: String, bankHint: String) {
        viewModelScope.launch {
            _isAiLoading.value = true
            val result = geminiService.parseBankStatement(text, bankHint)
            _statementParseResult.value = result
            _isAiLoading.value = false
        }
    }

    fun importTransactions(transactions: List<ExtractedTransaction>) {
        viewModelScope.launch {
            val entities = transactions.map {
                ExpenseEntity(
                    title = it.title,
                    amount = it.amount,
                    date = it.date,
                    category = it.category,
                    paymentMethod = it.paymentMethod,
                    notes = it.notes,
                    tag = it.tag,
                    bankName = it.bankName,
                    creditCardName = it.creditCardName,
                    syncStatus = "LOCAL"
                )
            }
            repository.insertExpenses(entities)
            _statementParseResult.value = null
            _syncMessage.value = "Successfully imported ${transactions.size} transactions to your ledger!"
        }
    }

    // Cloud Backup & Sync with Firebase Firestore
    fun performCloudBackup() {
        viewModelScope.launch {
            if (firestoreSyncRepository.isUserSignedIn()) {
                syncFirestoreTransactions()
            } else {
                _syncMessage.value = "Sign in with Google to synchronize transactions to Firebase Firestore across devices."
            }
        }
    }

    fun performCloudRestore() {
        viewModelScope.launch {
            if (!firestoreSyncRepository.isUserSignedIn()) {
                _syncMessage.value = "Sign in with Google to restore transactions from Firebase Firestore."
                return@launch
            }
            _isAiLoading.value = true
            try {
                val fetchResult = firestoreSyncRepository.fetchCloudExpenses()
                if (fetchResult.isSuccess) {
                    val cloudList = fetchResult.getOrNull().orEmpty()
                    val localList = repository.getExpensesList()
                    val localLookup = localList.associateBy {
                        "${it.title.trim().lowercase()}_${it.amount}_${it.date}"
                    }
                    val newToInsert = mutableListOf<ExpenseEntity>()
                    for (cloud in cloudList) {
                        val key = "${cloud.title.trim().lowercase()}_${cloud.amount}_${cloud.date}"
                        if (!localLookup.containsKey(key)) {
                            newToInsert.add(cloud.toExpenseEntity())
                        }
                    }
                    if (newToInsert.isNotEmpty()) {
                        repository.insertExpenses(newToInsert)
                    }
                    securityManager.recordSyncSuccess()
                    val count = cloudList.size
                    repository.addSyncLog(
                        action = "CLOUD_RESTORE",
                        count = newToInsert.size,
                        status = "SUCCESS",
                        email = securityManager.googleUserEmail.value ?: "Google Account",
                        details = "Restored ${newToInsert.size} new transactions from Firebase Firestore (Total cloud: $count)"
                    )
                    _syncMessage.value = "Restored ${newToInsert.size} new transactions from Firestore. Total accessible: $count"
                } else {
                    _syncMessage.value = "Failed to fetch cloud expenses: ${fetchResult.exceptionOrNull()?.localizedMessage}"
                }
            } catch (e: Exception) {
                _syncMessage.value = "Restore error: ${e.message}"
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    // Manual Encrypted Local Storage Backup
    fun backupToLocalStorage(
        customPassword: String? = null,
        onComplete: (File, Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                val json = repository.exportEncryptedJson()
                val encFile = CryptoBackupManager.createEncryptedBackupFile(
                    context = getApplication(),
                    jsonPayload = json,
                    userPassword = customPassword
                )
                val (saved, message) = ReportExportManager.saveToDownloads(
                    context = getApplication(),
                    file = encFile,
                    mimeType = "application/octet-stream"
                )
                val count = allExpenses.value.size
                repository.addSyncLog(
                    action = "LOCAL_BACKUP",
                    count = count,
                    status = if (saved) "SUCCESS" else "WARNING",
                    email = securityManager.googleUserEmail.value ?: "Local Device",
                    details = "Encrypted database state saved to Downloads (${encFile.name})"
                )
                securityManager.recordSyncSuccess()
                _syncMessage.value = "Local Backup complete! Saved encrypted file: ${encFile.name}"
                onComplete(encFile, saved, message)
            } catch (e: Exception) {
                _syncMessage.value = "Local Backup failed: ${e.message}"
                onComplete(File(""), false, e.localizedMessage ?: "Failed to generate encrypted backup")
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    // Manual Local Encrypted Restore
    fun restoreFromEncryptedBackupFile(
        fileBytes: ByteArray,
        customPassword: String? = null,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                val decryptedJson = CryptoBackupManager.decryptBackupFile(fileBytes, customPassword)
                val count = repository.importEncryptedJson(decryptedJson)
                repository.addSyncLog(
                    action = "LOCAL_RESTORE",
                    count = count,
                    status = "SUCCESS",
                    email = securityManager.googleUserEmail.value ?: "Local Device",
                    details = "Restored $count records from encrypted backup"
                )
                securityManager.recordSyncSuccess()
                _syncMessage.value = "Successfully restored $count records from backup!"
                onResult(true, "Successfully restored $count records into your database!")
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: "Failed to decrypt or restore backup file")
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    // Export Helpers
    fun exportPdfReport(title: String = "Transactions Ledger", expensesList: List<ExpenseEntity>? = null): File {
        return ReportExportManager.generatePdfReport(
            context = getApplication(),
            expenses = expensesList ?: filteredExpenses.value,
            budgets = allBudgets.value,
            monthYearTitle = title,
            subTitleInfo = "Transaction History • ${SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(Date())} • Currency: INR (₹)"
        )
    }

    fun exportCsvReport(prefix: String = "Transactions", expensesList: List<ExpenseEntity>? = null): File {
        return ReportExportManager.generateCsvReport(
            context = getApplication(),
            expenses = expensesList ?: filteredExpenses.value,
            fileNamePrefix = prefix
        )
    }

    fun exportMonthlyPdfReport(year: Int, month: Int): File {
        val detail = getMonthlyExpenseReportDetail(year, month)
        return ReportExportManager.generateMonthlyPdfReport(
            context = getApplication(),
            expenses = detail.expenses,
            budgets = allBudgets.value,
            year = year,
            month = month
        )
    }

    fun exportMonthlyCsvReport(year: Int, month: Int): File {
        val detail = getMonthlyExpenseReportDetail(year, month)
        return ReportExportManager.generateMonthlyCsvReport(
            context = getApplication(),
            expenses = detail.expenses,
            year = year,
            month = month
        )
    }

    fun exportCustomDatePdfReport(startDateMs: Long, endDateMs: Long): File {
        val start = minOf(startDateMs, endDateMs)
        val end = maxOf(startDateMs, endDateMs)
        val rangeExpenses = allExpenses.value.filter { it.date in start..end }
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val title = "Statement (${sdf.format(Date(start))} to ${sdf.format(Date(end))})"
        return ReportExportManager.generatePdfReport(
            context = getApplication(),
            expenses = rangeExpenses,
            budgets = allBudgets.value,
            monthYearTitle = title,
            subTitleInfo = "Custom Period: ${sdf.format(Date(start))} - ${sdf.format(Date(end))} • Currency: INR (₹)"
        )
    }

    fun exportCustomDateCsvReport(startDateMs: Long, endDateMs: Long): File {
        val start = minOf(startDateMs, endDateMs)
        val end = maxOf(startDateMs, endDateMs)
        val rangeExpenses = allExpenses.value.filter { it.date in start..end }
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        return ReportExportManager.generateCsvReport(
            context = getApplication(),
            expenses = rangeExpenses,
            fileNamePrefix = "Period_Expenses_${sdf.format(Date(start))}_to_${sdf.format(Date(end))}"
        )
    }

    fun exportExcelSpreadsheet(): File {
        return ReportExportManager.generateExcelSpreadsheet(
            getApplication(),
            filteredExpenses.value,
            allBudgets.value
        )
    }

    fun exportFullExcelDashboard(): File {
        return ReportExportManager.generateFullPersonalDashboardExcel(
            getApplication(),
            allExpenses.value,
            allBudgets.value
        )
    }

    fun exportMonthlyExcelReport(year: Int, month: Int): File {
        return ReportExportManager.generateMonthlyDetailedExcelReport(
            context = getApplication(),
            allExpenses = allExpenses.value,
            budgets = allBudgets.value,
            year = year,
            month = month
        )
    }

    fun exportCustomDateExcelReport(startDateMs: Long, endDateMs: Long): File {
        return ReportExportManager.generateCustomDateDetailedExcelReport(
            context = getApplication(),
            allExpenses = allExpenses.value,
            budgets = allBudgets.value,
            startDateMs = startDateMs,
            endDateMs = endDateMs
        )
    }

    /**
     * Exports the Gemini AI Monthly Spending Habits Report into a beautifully formatted PDF document.
     */
    fun exportAiReportPdf(report: MonthlyAiSpendingReport): File {
        val monthNum = getMonthNumber(report.monthName)
        val monthExpenses = allExpenses.value.filter { exp ->
            val cal = Calendar.getInstance().apply { timeInMillis = exp.date }
            cal.get(Calendar.YEAR) == report.year && (cal.get(Calendar.MONTH) + 1) == monthNum
        }
        return ReportExportManager.generateAiMonthlySpendingReportPdf(
            context = getApplication(),
            report = report,
            expenses = monthExpenses,
            budgets = allBudgets.value
        )
    }

    /**
     * Retrieves the active AI spending report or creates an offline synthesized report on demand.
     */
    fun getOrGenerateCurrentAiReport(year: Int? = null, month: Int? = null): MonthlyAiSpendingReport {
        val currentState = monthlyAiReportState.value
        if (currentState is MonthlyAiReportUiState.Success) {
            return currentState.report
        }
        val cal = Calendar.getInstance()
        val targetYear = year ?: cal.get(Calendar.YEAR)
        val targetMonth = month ?: (cal.get(Calendar.MONTH) + 1)
        val monthExpenses = getMonthlyExpenseReportDetail(targetYear, targetMonth).expenses
        val activeRecurring = allRecurringExpenses.value.filter { it.isActive }
        val categoryGroups = monthExpenses.groupBy { it.category }
            .map { Pair(it.key, it.value.sumOf { e -> e.amount }) }
            .sortedByDescending { it.second }
        val totalSpent = monthExpenses.sumOf { it.amount }
        val txCount = monthExpenses.size
        val dailyAvg = if (monthExpenses.isNotEmpty()) totalSpent / 30.0 else 0.0
        val recurringMonthlyEstimated = activeRecurring.sumOf { it.amount }

        return geminiService.generateOfflineMonthlyHabitsReport(
            monthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(cal.time),
            year = targetYear,
            totalSpent = totalSpent,
            txCount = txCount,
            dailyAvg = dailyAvg,
            prevComparisonText = "Baseline month evaluation",
            categoryGroup = categoryGroups,
            budgets = allBudgets.value,
            activeRecurring = activeRecurring,
            recurringMonthlyEstimated = recurringMonthlyEstimated,
            monthExpenses = monthExpenses,
            pctChange = 0.0
        )
    }

    private fun getMonthNumber(monthName: String): Int {
        val monthNames = listOf(
            "january", "february", "march", "april", "may", "june",
            "july", "august", "september", "october", "november", "december"
        )
        val clean = monthName.trim().lowercase(Locale.getDefault())
        val index = monthNames.indexOfFirst { clean.startsWith(it.take(3)) }
        return if (index != -1) index + 1 else Calendar.getInstance().get(Calendar.MONTH) + 1
    }

    fun getAvailableMonths(): List<AvailableMonth> {
        val expenses = allExpenses.value
        val sdfLabel = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val sdfShort = SimpleDateFormat("MMM yyyy", Locale.getDefault())

        val monthsMap = mutableMapOf<Pair<Int, Int>, Pair<Int, Double>>()

        // Always ensure current month and previous month exist in list
        val nowCal = Calendar.getInstance()
        val curYear = nowCal.get(Calendar.YEAR)
        val curMonth = nowCal.get(Calendar.MONTH) + 1
        monthsMap[Pair(curYear, curMonth)] = Pair(0, 0.0)

        nowCal.add(Calendar.MONTH, -1)
        val prevYear = nowCal.get(Calendar.YEAR)
        val prevMonth = nowCal.get(Calendar.MONTH) + 1
        monthsMap[Pair(prevYear, prevMonth)] = Pair(0, 0.0)

        // Tally all actual expenses
        val cal = Calendar.getInstance()
        for (e in expenses) {
            cal.timeInMillis = e.date
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH) + 1
            val existing = monthsMap[Pair(y, m)] ?: Pair(0, 0.0)
            monthsMap[Pair(y, m)] = Pair(existing.first + 1, existing.second + e.amount)
        }

        return monthsMap.entries
            .map { (ym, data) ->
                val (y, m) = ym
                val c = Calendar.getInstance().apply {
                    set(y, m - 1, 1)
                }
                AvailableMonth(
                    year = y,
                    month = m,
                    label = sdfLabel.format(c.time),
                    shortLabel = sdfShort.format(c.time),
                    count = data.first,
                    totalSpent = data.second
                )
            }
            .sortedWith(compareByDescending<AvailableMonth> { it.year }.thenByDescending { it.month })
    }

    val availableMonths: StateFlow<List<AvailableMonth>> = repository.allExpenses.map {
        getAvailableMonths()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun getCustomDateRangeStats(startDateMs: Long, endDateMs: Long): Pair<Int, Double> {
        val calStart = Calendar.getInstance().apply {
            timeInMillis = minOf(startDateMs, endDateMs)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val calEnd = Calendar.getInstance().apply {
            timeInMillis = maxOf(startDateMs, endDateMs)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val filtered = allExpenses.value.filter { it.date in calStart.timeInMillis..calEnd.timeInMillis }
        return Pair(filtered.size, filtered.sumOf { it.amount })
    }

    private fun getDateRangeForFilter(type: DateFilterType, custom: Pair<Long, Long>?): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis

        return when (type) {
            DateFilterType.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                Pair(cal.timeInMillis, end)
            }
            DateFilterType.TODAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                Pair(cal.timeInMillis, end)
            }
            DateFilterType.YESTERDAY -> {
                cal.add(Calendar.DAY_OF_YEAR, -1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val yStart = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                val yEnd = cal.timeInMillis
                Pair(yStart, yEnd)
            }
            DateFilterType.LAST_7_DAYS -> {
                cal.add(Calendar.DAY_OF_YEAR, -7)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                Pair(cal.timeInMillis, end)
            }
            DateFilterType.ALL_TIME -> {
                Pair(0L, end + (365L * 86400000L))
            }
            DateFilterType.CUSTOM -> {
                custom ?: Pair(0L, end)
            }
        }
    }
}
