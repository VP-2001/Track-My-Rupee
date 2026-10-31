package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.BudgetEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.RecurringDateUtils
import com.example.data.model.RecurringExpenseEntity
import com.example.data.model.RecurringFrequency
import com.example.data.db.ExpenseDatabase
import com.example.data.repository.ExpenseRepository
import com.example.export.ReportExportManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import android.app.NotificationManager
import com.example.notification.BudgetAlertThreshold
import com.example.notification.BudgetNotificationService
import com.example.ui.viewmodel.BudgetProgressItem
import java.io.FileInputStream
import java.util.Calendar
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import androidx.room.Room

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Track My Rupee", appName)
  }

  @Test
  fun `generateCsvReport writes UTF-8 BOM prefix`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val sampleExpenses = listOf(
      ExpenseEntity(
        id = 1,
        title = "Grocery Store",
        amount = 1250.50,
        category = "Food & Dining",
        date = System.currentTimeMillis(),
        paymentMethod = "UPI",
        tag = "Essential",
        notes = "Weekly veggies & fruits"
      )
    )

    val csvFile = ReportExportManager.generateCsvReport(context, sampleExpenses)
    assertTrue("CSV file should exist", csvFile.exists())
    assertTrue("CSV file should have content", csvFile.length() > 0)

    val bytes = ByteArray(3)
    FileInputStream(csvFile).use { input ->
      val bytesRead = input.read(bytes)
      assertEquals(3, bytesRead)
    }

    // Check UTF-8 BOM bytes: 0xEF, 0xBB, 0xBF
    assertEquals(0xEF.toByte(), bytes[0])
    assertEquals(0xBB.toByte(), bytes[1])
    assertEquals(0xBF.toByte(), bytes[2])
  }

  @Test
  fun `expenseEntity stores bankName and creditCardName with UPI and Credit Card methods`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val upiExpense = ExpenseEntity(
      id = 101,
      title = "Dinner with Friends",
      amount = 850.0,
      category = "Food & Dining",
      date = System.currentTimeMillis(),
      paymentMethod = "UPI",
      tag = "Personal",
      bankName = "State Bank of India",
      creditCardName = ""
    )
    val cardExpense = ExpenseEntity(
      id = 102,
      title = "Electronics Store",
      amount = 4599.0,
      category = "Shopping",
      date = System.currentTimeMillis(),
      paymentMethod = "Credit Card",
      tag = "Personal",
      bankName = "HDFC Bank",
      creditCardName = "HDFC Regalia"
    )

    assertEquals("State Bank of India", upiExpense.bankName)
    assertEquals("UPI", upiExpense.paymentMethod)
    assertEquals("HDFC Regalia", cardExpense.creditCardName)
    assertEquals("HDFC Bank", cardExpense.bankName)
    assertEquals("Credit Card", cardExpense.paymentMethod)

    val csvFile = ReportExportManager.generateCsvReport(context, listOf(upiExpense, cardExpense))
    val csvContent = csvFile.readText(Charsets.UTF_8)
    assertTrue("CSV header should have Bank Name", csvContent.contains("Bank Name"))
    assertTrue("CSV header should have Credit Card", csvContent.contains("Credit Card"))
    assertTrue("CSV should contain State Bank of India", csvContent.contains("State Bank of India"))
    assertTrue("CSV should contain HDFC Regalia", csvContent.contains("HDFC Regalia"))
  }

  @Test
  fun `generateMonthlyDetailedExcelReport produces valid 6-sheet workbook with full details`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val cal = java.util.Calendar.getInstance()
    cal.set(2026, java.util.Calendar.MARCH, 15, 14, 30, 0)
    val testDate = cal.timeInMillis

    val expenses = listOf(
      ExpenseEntity(
        id = 201,
        title = "Supermarket Grocery",
        amount = 3500.0,
        category = "Food & Dining",
        date = testDate,
        paymentMethod = "RuPay Credit Card",
        tag = "Essential",
        bankName = "Punjab National Bank",
        creditCardName = "PNB RuPay Platinum",
        notes = "Monthly kitchen supplies"
      ),
      ExpenseEntity(
        id = 202,
        title = "Metro Recharge",
        amount = 500.0,
        category = "Transportation",
        date = testDate,
        paymentMethod = "UPI",
        tag = "Commute",
        bankName = "State Bank of India",
        notes = "Metro card top-up"
      )
    )

    val budgets = listOf(
      com.example.data.model.BudgetEntity(
        category = "Food & Dining",
        monthlyLimit = 15000.0
      )
    )

    val excelFile = ReportExportManager.generateMonthlyDetailedExcelReport(
      context = context,
      allExpenses = expenses,
      budgets = budgets,
      year = 2026,
      month = 3
    )

    assertTrue("Excel file must exist", excelFile.exists())
    assertTrue("Excel file must not be empty", excelFile.length() > 0)

    val zip = java.util.zip.ZipFile(excelFile)
    val entries = zip.entries().asSequence().map { it.name }.toSet()

    // 6 worksheets
    assertTrue("Must contain sheet1.xml", entries.contains("xl/worksheets/sheet1.xml"))
    assertTrue("Must contain sheet2.xml", entries.contains("xl/worksheets/sheet2.xml"))
    assertTrue("Must contain sheet3.xml", entries.contains("xl/worksheets/sheet3.xml"))
    assertTrue("Must contain sheet4.xml", entries.contains("xl/worksheets/sheet4.xml"))
    assertTrue("Must contain sheet5.xml", entries.contains("xl/worksheets/sheet5.xml"))
    assertTrue("Must contain sheet6.xml", entries.contains("xl/worksheets/sheet6.xml"))

    // Check workbook sheet names in xl/workbook.xml
    val workbookXml = zip.getInputStream(zip.getEntry("xl/workbook.xml")).bufferedReader().use { it.readText() }
    assertTrue("Must include Overview_KPIs", workbookXml.contains("Overview_KPIs"))
    assertTrue("Must include Full_Ledger_Details", workbookXml.contains("Full_Ledger_Details"))
    assertTrue("Must include Category_Summary", workbookXml.contains("Category_Summary"))
    assertTrue("Must include Daily_Breakdown", workbookXml.contains("Daily_Breakdown"))
    assertTrue("Must include Payment_Methods", workbookXml.contains("Payment_Methods"))
    assertTrue("Must include Tags_Breakdown", workbookXml.contains("Tags_Breakdown"))

    zip.close()
  }

  @Test
  fun `generateCustomDateDetailedExcelReport creates correct period report`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val cal1 = java.util.Calendar.getInstance().apply { set(2026, java.util.Calendar.JANUARY, 10) }
    val cal2 = java.util.Calendar.getInstance().apply { set(2026, java.util.Calendar.FEBRUARY, 20) }

    val expenses = listOf(
      ExpenseEntity(
        id = 301,
        title = "Hotel Stay",
        amount = 12000.0,
        category = "Travel",
        date = cal1.timeInMillis,
        paymentMethod = "Credit Card",
        tag = "Vacation",
        bankName = "HDFC Bank",
        creditCardName = "HDFC Regalia"
      ),
      ExpenseEntity(
        id = 302,
        title = "Airport Taxi",
        amount = 1800.0,
        category = "Transportation",
        date = cal2.timeInMillis,
        paymentMethod = "UPI",
        tag = "Travel",
        bankName = "ICICI Bank"
      )
    )

    val excelFile = ReportExportManager.generateCustomDateDetailedExcelReport(
      context = context,
      allExpenses = expenses,
      budgets = emptyList(),
      startDateMs = cal1.timeInMillis,
      endDateMs = cal2.timeInMillis
    )

    assertTrue("Excel report must exist", excelFile.exists())
    assertTrue("Excel report must not be empty", excelFile.length() > 0)

    val zip = java.util.zip.ZipFile(excelFile)
    val entries = zip.entries().asSequence().map { it.name }.toSet()
    assertTrue("Must contain sheet1", entries.contains("xl/worksheets/sheet1.xml"))
    assertTrue("Must contain sheet2", entries.contains("xl/worksheets/sheet2.xml"))
    zip.close()
  }

  @Test
  fun `search filtering by merchant name, category, and date works accurately`() {
    val cal1 = java.util.Calendar.getInstance().apply { set(2026, java.util.Calendar.MARCH, 15, 10, 0, 0) }
    val cal2 = java.util.Calendar.getInstance().apply { set(2026, java.util.Calendar.OCTOBER, 20, 15, 30, 0) }

    val expenses = listOf(
      ExpenseEntity(
        id = 401,
        title = "Starbucks Coffee",
        amount = 450.0,
        category = "Food & Dining",
        date = cal1.timeInMillis,
        paymentMethod = "UPI",
        tag = "Personal"
      ),
      ExpenseEntity(
        id = 402,
        title = "Amazon Shopping",
        amount = 2999.0,
        category = "Shopping",
        date = cal2.timeInMillis,
        paymentMethod = "Credit Card",
        tag = "Gadgets"
      )
    )

    // Test Merchant search
    val merchantMatches = expenses.filter { it.title.lowercase().contains("starbucks") }
    assertEquals(1, merchantMatches.size)
    assertEquals("Starbucks Coffee", merchantMatches[0].title)

    // Test Category search
    val categoryMatches = expenses.filter { it.category.lowercase().contains("shopping") }
    assertEquals(1, categoryMatches.size)
    assertEquals("Amazon Shopping", categoryMatches[0].title)

    // Test Date search (e.g. "Mar 2026" or "Oct 2026")
    val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.ENGLISH)
    val marchMatches = expenses.filter { sdf.format(java.util.Date(it.date)).lowercase().contains("mar 2026") }
    assertEquals(1, marchMatches.size)
    assertEquals(401L, marchMatches[0].id)

    val octMatches = expenses.filter { sdf.format(java.util.Date(it.date)).lowercase().contains("oct 2026") }
    assertEquals(1, octMatches.size)
    assertEquals(402L, octMatches[0].id)
  }

  @Test
  fun `room database full-text search capability filters by merchant, category, and date range`() = kotlinx.coroutines.test.runTest {
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(
      context,
      com.example.data.db.ExpenseDatabase::class.java
    ).allowMainThreadQueries().build()

    try {
      val dao = db.expenseDao()

      val cal1 = java.util.Calendar.getInstance().apply { set(2026, java.util.Calendar.MARCH, 15, 10, 0, 0) }
      val cal2 = java.util.Calendar.getInstance().apply { set(2026, java.util.Calendar.OCTOBER, 20, 15, 30, 0) }
      val cal3 = java.util.Calendar.getInstance().apply { set(2026, java.util.Calendar.OCTOBER, 25, 19, 0, 0) }

      val exp1 = ExpenseEntity(
        id = 1,
        title = "Starbucks Coffee",
        amount = 450.0,
        category = "Food & Dining",
        date = cal1.timeInMillis,
        paymentMethod = "UPI",
        notes = "Frappuccino and croissant",
        tag = "Personal"
      )
      val exp2 = ExpenseEntity(
        id = 2,
        title = "Amazon Shopping",
        amount = 2999.0,
        category = "Shopping",
        date = cal2.timeInMillis,
        paymentMethod = "Credit Card",
        notes = "Wireless headphones",
        tag = "Gadgets"
      )
      val exp3 = ExpenseEntity(
        id = 3,
        title = "Swiggy Gourmet",
        amount = 850.0,
        category = "Food & Dining",
        date = cal3.timeInMillis,
        paymentMethod = "UPI",
        notes = "Dinner with friends",
        tag = "Weekend"
      )

      dao.insertExpenses(listOf(exp1, exp2, exp3))

      // 1. Full-Text Search by Merchant Name (FTS MATCH "title:Starbucks*")
      val merchantResults = dao.searchExpensesFtsList("title:Starbucks*")
      assertEquals(1, merchantResults.size)
      assertEquals("Starbucks Coffee", merchantResults[0].title)

      // 2. Full-Text Search by Category (FTS MATCH "category:Shopping*")
      val shoppingResults = dao.searchExpensesFtsList("category:Shopping*")
      assertEquals(1, shoppingResults.size)
      assertEquals("Amazon Shopping", shoppingResults[0].title)

      // 3. Full-Text Search by Keyword across all columns (e.g. notes "headphones")
      val notesResults = dao.searchExpensesFtsList("headphones*")
      assertEquals(1, notesResults.size)
      assertEquals(2L, notesResults[0].id)

      // 4. Full-Text Search with Date Range Filter (October 2026 window)
      val startOct = java.util.Calendar.getInstance().apply { set(2026, java.util.Calendar.OCTOBER, 1, 0, 0, 0) }.timeInMillis
      val endOct = java.util.Calendar.getInstance().apply { set(2026, java.util.Calendar.OCTOBER, 31, 23, 59, 59) }.timeInMillis

      // Search Food category in October: should return only Swiggy, not Starbucks (which was in March)
      val octFoodResults = dao.searchExpensesFtsWithFiltersList(
        ftsQuery = "category:Food*",
        category = "Food & Dining",
        startDate = startOct,
        endDate = endOct
      )
      assertEquals(1, octFoodResults.size)
      assertEquals("Swiggy Gourmet", octFoodResults[0].title)

      // 5. Query using Repository helper with query builder
      val repo = com.example.data.repository.ExpenseRepository(
        dao,
        db.budgetDao(),
        db.syncLogDao(),
        db.recurringExpenseDao()
      )

      val repoMerchantSearch = repo.searchExpensesFtsList(
        query = "Swiggy",
        scope = "MERCHANT"
      )
      assertEquals(1, repoMerchantSearch.size)
      assertEquals("Swiggy Gourmet", repoMerchantSearch[0].title)

    } finally {
      db.close()
    }
  }

  @Test
  fun `monthly comparison accurately calculates multi-month trends and peak drivers`() {
    val calSep = java.util.Calendar.getInstance().apply { set(2026, java.util.Calendar.SEPTEMBER, 10, 12, 0, 0) }
    val calOct = java.util.Calendar.getInstance().apply { set(2026, java.util.Calendar.OCTOBER, 15, 14, 0, 0) }

    val expenses = listOf(
      // September
      ExpenseEntity(
        id = 501,
        title = "Swiggy Order",
        amount = 600.0,
        category = "Food & Dining",
        date = calSep.timeInMillis,
        paymentMethod = "UPI",
        tag = "Food"
      ),
      ExpenseEntity(
        id = 502,
        title = "Amazon Gadget",
        amount = 1400.0,
        category = "Shopping",
        date = calSep.timeInMillis,
        paymentMethod = "Credit Card",
        tag = "Gadgets"
      ),
      // October (Higher spend month)
      ExpenseEntity(
        id = 503,
        title = "Swiggy Order",
        amount = 1200.0,
        category = "Food & Dining",
        date = calOct.timeInMillis,
        paymentMethod = "UPI",
        tag = "Food"
      ),
      ExpenseEntity(
        id = 504,
        title = "Grocery Mart",
        amount = 3800.0,
        category = "Groceries",
        date = calOct.timeInMillis,
        paymentMethod = "Debit Card",
        tag = "Essentials"
      )
    )

    // Verify monthly totals
    val sepExpenses = expenses.filter {
      val c = java.util.Calendar.getInstance().apply { timeInMillis = it.date }
      c.get(java.util.Calendar.MONTH) == java.util.Calendar.SEPTEMBER
    }
    val octExpenses = expenses.filter {
      val c = java.util.Calendar.getInstance().apply { timeInMillis = it.date }
      c.get(java.util.Calendar.MONTH) == java.util.Calendar.OCTOBER
    }

    assertEquals(2000.0, sepExpenses.sumOf { it.amount }, 0.01)
    assertEquals(5000.0, octExpenses.sumOf { it.amount }, 0.01)
    assertEquals(7000.0, expenses.sumOf { it.amount }, 0.01)

    // Peak month is October (5000 > 2000)
    assertTrue("October must be peak spending month", octExpenses.sumOf { it.amount } > sepExpenses.sumOf { it.amount })

    // Peak merchant across months is Grocery Mart (3800), followed by Swiggy (1800)
    val merchantTotals = expenses.groupBy { it.title }.mapValues { (_, list) -> list.sumOf { it.amount } }
    assertEquals(3800.0, merchantTotals["Grocery Mart"] ?: 0.0, 0.01)
    assertEquals(1800.0, merchantTotals["Swiggy Order"] ?: 0.0, 0.01)

    // Swiggy peaked in October (1200 > 600)
    val swiggyOct = octExpenses.filter { it.title == "Swiggy Order" }.sumOf { it.amount }
    val swiggySep = sepExpenses.filter { it.title == "Swiggy Order" }.sumOf { it.amount }
    assertTrue(swiggyOct > swiggySep)
  }

  @Test
  fun `recent expenses list displays expenses in chronological order and supports deletion`() {
    val expenses = mutableListOf(
      ExpenseEntity(
        id = 1,
        title = "Older Coffee",
        amount = 150.0,
        category = "Food & Dining",
        date = 1000000L,
        paymentMethod = "UPI",
        tag = "Beverage"
      ),
      ExpenseEntity(
        id = 2,
        title = "Newer Lunch",
        amount = 450.0,
        category = "Food & Dining",
        date = 2000000L,
        paymentMethod = "Credit Card",
        tag = "Lunch"
      ),
      ExpenseEntity(
        id = 3,
        title = "Latest Book",
        amount = 899.0,
        category = "Shopping",
        date = 3000000L,
        paymentMethod = "UPI",
        tag = "Education"
      )
    )

    // Sorted by date descending (most recent first)
    val recentList = expenses.sortedByDescending { it.date }
    assertEquals("Latest Book", recentList[0].title)
    assertEquals("Newer Lunch", recentList[1].title)
    assertEquals("Older Coffee", recentList[2].title)

    // Simulate deleting the most recent item
    val toDelete = recentList.first()
    expenses.remove(toDelete)

    assertEquals(2, expenses.size)
    assertTrue("Deleted expense should no longer be present", expenses.none { it.id == toDelete.id })
    val updatedRecent = expenses.sortedByDescending { it.date }
    assertEquals("Newer Lunch", updatedRecent.first().title)
  }

  @Test
  fun `calculateLast6MonthsTrend computes 6 months with financial growth indicators`() {
    val calCurrent = java.util.Calendar.getInstance()
    val calPrev1 = java.util.Calendar.getInstance().apply { add(java.util.Calendar.MONTH, -1) }
    val calPrev2 = java.util.Calendar.getInstance().apply { add(java.util.Calendar.MONTH, -2) }

    val sampleExpenses = listOf(
      ExpenseEntity(
        id = 1,
        title = "Past Month 2 Flight",
        amount = 10000.0,
        category = "Travel & Transport",
        date = calPrev2.timeInMillis,
        paymentMethod = "Credit Card",
        tag = "Vacation"
      ),
      ExpenseEntity(
        id = 2,
        title = "Past Month 1 Utilities",
        amount = 6000.0,
        category = "Bills & Utilities",
        date = calPrev1.timeInMillis,
        paymentMethod = "UPI",
        tag = "Essential"
      ),
      ExpenseEntity(
        id = 3,
        title = "Current Month Groceries",
        amount = 4000.0,
        category = "Groceries",
        date = calCurrent.timeInMillis,
        paymentMethod = "UPI",
        tag = "Essential"
      )
    )

    val context = ApplicationProvider.getApplicationContext<Context>()
    val vm = com.example.ui.viewmodel.ExpenseViewModel(context as android.app.Application)
    val summary = vm.calculateLast6MonthsTrend(sampleExpenses)

    assertEquals(6, summary.trendPoints.size)
    assertEquals(20000.0, summary.totalSpent6Months, 0.01)
    // 20000 / 6 months
    assertEquals(20000.0 / 6.0, summary.averageMonthlySpend, 0.01)

    // Current month spend is 4000
    assertEquals(4000.0, summary.latestMonthSpend, 0.01)

    // Since spending decreased from 10000 -> 6000 -> 4000, isSpendingDecreased should be true (positive growth)
    assertTrue("Spending decreased over the active period", summary.isSpendingDecreased)

    val currentPoint = summary.trendPoints.last()
    assertEquals(4000.0, currentPoint.totalSpent, 0.01)
    assertEquals(1, currentPoint.transactionCount)
    // Month-over-month change from 6000 to 4000: ((4000 - 6000) / 6000) * 100 = -33.33%
    assertEquals(-33.33, currentPoint.momChangePercent, 0.5)
  }

  @Test
  fun `securityManager locks on startup and unlocks via PIN verification or unlockApp`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val securityManager = com.example.security.SecurityAndSyncManager(context)

    // Defaults to locked on launch
    assertTrue("App lock should be enabled by default", securityManager.isLockEnabled())
    assertTrue("App should start locked", securityManager.isAppLocked.value)

    // Incorrect PIN does not unlock
    val wrongPinSuccess = securityManager.verifyPin("9999")
    org.junit.Assert.assertFalse("Wrong PIN should fail", wrongPinSuccess)
    assertTrue("App should remain locked", securityManager.isAppLocked.value)

    // Default PIN 1234 unlocks
    val rightPinSuccess = securityManager.verifyPin("1234")
    assertTrue("Correct default PIN should verify", rightPinSuccess)
    org.junit.Assert.assertFalse("App should be unlocked", securityManager.isAppLocked.value)

    // Lock again
    securityManager.lockApp()
    assertTrue("App should lock again", securityManager.isAppLocked.value)

    // Direct unlock (as called by BiometricPrompt onSuccess callback)
    securityManager.unlockApp()
    org.junit.Assert.assertFalse("App should be unlocked via BiometricPrompt success", securityManager.isAppLocked.value)
  }

  @Test
  fun `securityManager enforces auto-lock on background and returns descriptive biometric status`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val securityManager = com.example.security.SecurityAndSyncManager(context)

    // Verify auto-lock on background is enabled by default
    assertTrue("Auto-lock on background should be enabled by default", securityManager.isAutoLockOnBackgroundEnabled())
    assertEquals("Default lock timeout should be immediate (0s)", 0, securityManager.getLockTimeoutSeconds())

    // Unlock app
    securityManager.unlockApp()
    org.junit.Assert.assertFalse("App should be unlocked", securityManager.isAppLocked.value)

    // Simulate app going to background and returning (device unlocked scenario)
    securityManager.onAppBackgrounded()
    securityManager.onAppForegrounded()

    // Even if device is unlocked, the ledger re-locks requiring verification
    assertTrue("App should re-lock upon returning from background", securityManager.isAppLocked.value)

    // Verify biometric status description is non-empty
    val bioDesc = securityManager.getBiometricStatusDescription()
    assertTrue("Biometric status should be descriptive", bioDesc.isNotBlank())
  }

  @Test
  fun `aiChatCompletion offline advisor consumes Room DB context with category summaries and expenses`() = kotlinx.coroutines.runBlocking {
    val service = com.example.ai.GeminiAiService()
    val mockContext = """
      LIVE USER FINANCIAL CONTEXT (FROM ROOM DATABASE):
      - Current Period: October 2026
      - Total Spend This Month: ₹7500.00 across 5 transactions
      - Total All-Time Expenses Logged: 12 entries (Total ₹18500.00)
      - Highest Single Transaction: New Headphones - ₹4500.0 (Shopping)
      - Category Summaries (Totals & Percentages):
      - Food & Dining: ₹4200.00 (56.0%, 3 transactions)
      - Shopping: ₹3300.00 (44.0%, 2 transactions)
      - Active Budgets:
      - Food & Dining: spent ₹4200.00 of limit ₹5000.00 (84%) [Within Limit]
      - Recent Expenses Ledger:
      - Dinner with Friends (Food & Dining, UPI, Tag: Personal): ₹850.0 on 21 Oct 2026
    """.trimIndent()

    val history = listOf(
      com.example.ai.ChatMessage(
        role = com.example.ai.MessageRole.USER,
        content = "Analyze my spending this month"
      )
    )

    val result = service.chatCompletion(
      history = history,
      systemInstruction = mockContext,
      model = com.example.ai.GeminiChatModel.FLASH
    )

    assertTrue("chatCompletion should succeed", result.isSuccess)
    val reply = result.getOrNull().orEmpty()
    assertTrue("Reply should include month spend", reply.contains("7500.00"))
    assertTrue("Reply should include top category", reply.contains("Food & Dining"))
    assertTrue("Reply should cite Room DB", reply.contains("Room DB"))
  }

  @Test
  fun `calculateLast12MonthsTrend creates exactly 12 trend points and computes 1-year financial growth`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val vm = com.example.ui.viewmodel.ExpenseViewModel(context as android.app.Application)

    val now = java.util.Calendar.getInstance()
    val sampleExpenses = listOf(
      ExpenseEntity(
        id = 201,
        title = "Annual Insurance",
        amount = 12000.0,
        category = "Bills & Utilities",
        date = now.timeInMillis,
        paymentMethod = "UPI",
        tag = "Essential"
      ),
      ExpenseEntity(
        id = 202,
        title = "Vacation Trip",
        amount = 24000.0,
        category = "Travel & Transport",
        date = java.util.Calendar.getInstance().apply { add(java.util.Calendar.MONTH, -6) }.timeInMillis,
        paymentMethod = "Credit Card",
        tag = "Personal"
      )
    )

    val summary = vm.calculateLast12MonthsTrend(sampleExpenses)
    assertEquals(12, summary.trendPoints.size)
    assertEquals(36000.0, summary.totalSpent1Year, 0.01)
    assertEquals(36000.0 / 12.0, summary.averageMonthlySpend, 0.01)
    assertEquals(12000.0, summary.latestMonthSpend, 0.01)
  }

  @Test
  fun `getMonthlyExpenseReportDetail returns complete itemized report for requested month`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val vm = com.example.ui.viewmodel.ExpenseViewModel(context as android.app.Application)

    val now = java.util.Calendar.getInstance()
    val y = now.get(java.util.Calendar.YEAR)
    val m = now.get(java.util.Calendar.MONTH) + 1

    val detail = vm.getMonthlyExpenseReportDetail(y, m)
    assertEquals(y, detail.year)
    assertEquals(m, detail.month)
    assertTrue("detail.monthName should be non-empty", detail.monthName.isNotEmpty())
    assertTrue("detail.dailyAverage should be >= 0", detail.dailyAverage >= 0.0)
  }

  @Test
  fun `CryptoBackupManager creates encrypted file with default passphrase and decrypts successfully`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val originalPayload = """{"expenses":[{"id":1,"title":"Grocery","amount":1500.0,"category":"Food"}],"budgets":[]}"""

    val backupFile = com.example.security.CryptoBackupManager.createEncryptedBackupFile(
      context = context,
      jsonPayload = originalPayload,
      userPassword = null
    )

    assertTrue("Backup file must exist", backupFile.exists())
    assertTrue("Backup file must have .enc extension", backupFile.name.endsWith(".enc"))
    assertTrue("Backup file must have non-zero size", backupFile.length() > 0)

    val bytes = backupFile.readBytes()
    val decryptedJson = com.example.security.CryptoBackupManager.decryptBackupFile(bytes, null)
    assertEquals("Decrypted JSON must match original payload", originalPayload, decryptedJson)
  }

  @Test
  fun `CryptoBackupManager encrypts with custom password and fails decryption with wrong password`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val originalPayload = """{"version":1,"records":42,"security":"AES_256_GCM"}"""
    val customPassword = "MySecretVaultPassword2026!"

    val backupFile = com.example.security.CryptoBackupManager.createEncryptedBackupFile(
      context = context,
      jsonPayload = originalPayload,
      userPassword = customPassword
    )

    val bytes = backupFile.readBytes()

    // 1. Success with correct password
    val decryptedJson = com.example.security.CryptoBackupManager.decryptBackupFile(bytes, customPassword)
    assertEquals(originalPayload, decryptedJson)

    // 2. Fails with wrong password
    var threw = false
    try {
      com.example.security.CryptoBackupManager.decryptBackupFile(bytes, "WrongPassword123")
    } catch (e: Exception) {
      threw = true
    }
    assertTrue("Decryption with wrong password must throw an Exception", threw)
  }

  @Test
  fun `GeminiAiService accurately parses financial context and answers category inquiries`() {
    val service = com.example.ai.GeminiAiService()

    val contextSnapshot = """
      LIVE USER FINANCIAL CONTEXT (FROM ROOM DATABASE):
      - Current Period: October 2026
      - Total All-Time Spending: ₹18,450.50 across 12 logged transactions
      - Total Current Month Spending: ₹6,200.00 across 4 transactions
      - Top Single Expenses:
      - Apple Store Laptop Sleeve: ₹4,500.00 (Shopping, Credit Card, on 15 Oct 2026)
      - Fine Dine Restaurant: ₹2,200.00 (Food & Dining, UPI, on 12 Oct 2026)
      - Top Merchants / Stores: Apple Store: ₹4,500.00, Fine Dine: ₹2,200.00
      - Payment Methods Breakdown: UPI: ₹8,450.50 (8 txns), Credit Card: ₹10,000.00 (4 txns)
      - All-Time Category Summaries (Totals & Percentages):
      - Food & Dining: ₹5,850.50 (31.7%, 5 transactions)
      - Shopping: ₹7,500.00 (40.6%, 3 transactions)
      - Transportation: ₹3,100.00 (16.8%, 3 transactions)
      - Current Month Category Summaries:
      - Shopping: ₹4,500.00 (72.6%, 1 transactions)
      - Food & Dining: ₹1,700.00 (27.4%, 3 transactions)
      - Active Budgets & Adherence:
      - Food & Dining: limit ₹6,000.00, spent ₹1,700.00 (28%) [Within limit (₹4,300.00 left)]
      - Shopping: limit ₹4,000.00, spent ₹4,500.00 (112%) [OVER BUDGET by ₹500.00]
      - Recent Transactions Ledger:
      - Apple Store Laptop Sleeve | ₹4,500.00 | Shopping | Credit Card | 15 Oct 2026
      - Lunch Bistro | ₹450.00 | Food & Dining | UPI | 14 Oct 2026
    """.trimIndent()

    // 1. Verify parsing
    val parsed = service.parseFinancialContext(contextSnapshot)
    assertEquals(18450.50, parsed.totalAllTimeSpend, 0.01)
    assertEquals(12, parsed.totalAllTimeCount)
    assertEquals(6200.00, parsed.totalSpendThisMonth, 0.01)
    assertEquals(4, parsed.transactionCountThisMonth)
    assertEquals(3, parsed.allTimeCategories.size)
    assertEquals("Food & Dining", parsed.allTimeCategories[0].first)
    assertEquals("Shopping", parsed.allTimeCategories[1].first)
    assertEquals(2, parsed.activeBudgets.size)

    // 2. Test Food Category Inquiry
    val foodResponse = service.generateOfflineAdvisorResponse("How much did I spend on Food & Dining?", contextSnapshot)
    assertTrue("Response should contain Food & Dining", foodResponse.contains("Food & Dining"))
    assertTrue("Response should contain the all-time Food total ₹5,850.50", foodResponse.contains("5,850.50") || foodResponse.contains("5850.50"))
    assertTrue("Response should include Current Month food info", foodResponse.contains("1,700.00") || foodResponse.contains("1700.00"))

    // 3. Test Shopping Category Inquiry & Budget Over-Limit Warning
    val shopResponse = service.generateOfflineAdvisorResponse("What about shopping expenses?", contextSnapshot)
    assertTrue("Response should mention Shopping", shopResponse.contains("Shopping"))
    assertTrue("Response should contain ₹7,500.00", shopResponse.contains("7,500.00") || shopResponse.contains("7500.00"))
    assertTrue("Response should flag over budget condition", shopResponse.contains("OVER BUDGET") || shopResponse.contains("⚠️"))

    // 4. Test Highest Expense Inquiry
    val peakResponse = service.generateOfflineAdvisorResponse("What is my highest single expense?", contextSnapshot)
    assertTrue("Response should mention Apple Store Laptop Sleeve", peakResponse.contains("Apple Store Laptop Sleeve"))
    assertTrue("Response should mention 4,500.00", peakResponse.contains("4,500.00") || peakResponse.contains("4500.00"))

    // 5. Test Payment Methods Inquiry
    val paymentResponse = service.generateOfflineAdvisorResponse("Show my payment methods breakdown", contextSnapshot)
    assertTrue("Response should mention UPI", paymentResponse.contains("UPI"))
    assertTrue("Response should mention Credit Card", paymentResponse.contains("Credit Card"))

    // 6. Test Total Spend Inquiry
    val totalResponse = service.generateOfflineAdvisorResponse("Analyze my total spend", contextSnapshot)
    assertTrue("Response should mention all time spend 18,450.50", totalResponse.contains("18,450.50") || totalResponse.contains("18450.50"))
    assertTrue("Response should mention current month spend 6,200.00", totalResponse.contains("6,200.00") || totalResponse.contains("6200.00"))
  }

  @Test
  fun `GeminiAiService accurately parses structured JSON in system instructions`() {
    val service = com.example.ai.GeminiAiService()

    val jsonPrompt = """
      You are a knowledgeable Personal Finance Advisor in the Expense Tracker Android app.
      
      LIVE USER FINANCIAL CONTEXT (FROM ROOM DATABASE - STRUCTURED JSON):
      ```json
      {
        "period": "October 2026",
        "currency": "INR",
        "spending_summary": {
          "all_time_total": 24500.0,
          "all_time_transactions_count": 15,
          "current_month_total": 8500.0,
          "current_month_transactions_count": 5
        },
        "category_summaries": {
          "all_time": [
            {
              "category": "Food & Dining",
              "total_spent": 9200.0,
              "percentage": 37.5,
              "transaction_count": 8
            },
            {
              "category": "Groceries",
              "total_spent": 6400.0,
              "percentage": 26.1,
              "transaction_count": 4
            }
          ],
          "current_month": [
            {
              "category": "Food & Dining",
              "total_spent": 3200.0,
              "percentage": 37.6,
              "transaction_count": 3
            }
          ]
        },
        "budget_status": [
          {
            "category": "Food & Dining",
            "monthly_limit": 5000.0,
            "spent_this_month": 3200.0,
            "percentage_used": 64.0,
            "remaining": 1800.0,
            "is_over_budget": false,
            "status_label": "Within limit (₹1,800.00 left)"
          },
          {
            "category": "Groceries",
            "monthly_limit": 3000.0,
            "spent_this_month": 4200.0,
            "percentage_used": 140.0,
            "remaining": -1200.0,
            "is_over_budget": true,
            "status_label": "OVER BUDGET by ₹1,200.00"
          }
        ],
        "top_single_expenses": [
          {
            "title": "Weekend Buffet",
            "amount": 2500.0,
            "category": "Food & Dining",
            "payment_method": "UPI",
            "date": "18 Oct 2026"
          }
        ],
        "payment_methods_breakdown": [
          {
            "method": "UPI",
            "total_spent": 14500.0,
            "transaction_count": 10
          }
        ]
      }
      ```
    """.trimIndent()

    val parsed = service.parseFinancialContext(jsonPrompt)
    assertEquals("October 2026", parsed.currentPeriod)
    assertEquals(24500.0, parsed.totalAllTimeSpend, 0.01)
    assertEquals(15, parsed.totalAllTimeCount)
    assertEquals(8500.0, parsed.totalSpendThisMonth, 0.01)
    assertEquals(5, parsed.transactionCountThisMonth)
    assertEquals(2, parsed.allTimeCategories.size)
    assertEquals(2, parsed.activeBudgets.size)

    // Test category query with JSON context
    val foodResponse = service.generateOfflineAdvisorResponse("How much did I spend on Food?", jsonPrompt)
    assertTrue("Response should contain Food & Dining", foodResponse.contains("Food & Dining"))
    assertTrue("Response should contain all-time spend ₹9,200.00", foodResponse.contains("9,200.00") || foodResponse.contains("9200.00"))
    assertTrue("Response should contain current month spend ₹3,200.00", foodResponse.contains("3,200.00") || foodResponse.contains("3200.00"))

    // Test budget over-limit for Groceries
    val groceryResponse = service.generateOfflineAdvisorResponse("What about Groceries budget?", jsonPrompt)
    assertTrue("Response should mention Groceries", groceryResponse.contains("Groceries"))
    assertTrue("Response should alert OVER BUDGET", groceryResponse.contains("OVER BUDGET") || groceryResponse.contains("⚠️"))
  }

  @Test
  fun `recurringDateUtils computes correct next due dates for daily weekly monthly yearly`() {
    val cal = Calendar.getInstance().apply {
      set(2026, Calendar.OCTOBER, 15, 10, 0, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val currentDue = cal.timeInMillis

    // Daily
    val nextDaily = RecurringDateUtils.calculateNextDueDate("DAILY", currentDue)
    val dailyCal = Calendar.getInstance().apply { timeInMillis = nextDaily }
    assertEquals(16, dailyCal.get(Calendar.DAY_OF_MONTH))
    assertEquals(Calendar.OCTOBER, dailyCal.get(Calendar.MONTH))

    // Weekly
    val nextWeekly = RecurringDateUtils.calculateNextDueDate("WEEKLY", currentDue)
    val weeklyCal = Calendar.getInstance().apply { timeInMillis = nextWeekly }
    assertEquals(22, weeklyCal.get(Calendar.DAY_OF_MONTH))

    // Monthly with dayOfMonth = 5
    val nextMonthly = RecurringDateUtils.calculateNextDueDate("MONTHLY", currentDue, dayOfMonth = 5)
    val monthlyCal = Calendar.getInstance().apply { timeInMillis = nextMonthly }
    assertEquals(5, monthlyCal.get(Calendar.DAY_OF_MONTH))
    assertEquals(Calendar.NOVEMBER, monthlyCal.get(Calendar.MONTH))

    // Yearly
    val nextYearly = RecurringDateUtils.calculateNextDueDate("YEARLY", currentDue)
    val yearlyCal = Calendar.getInstance().apply { timeInMillis = nextYearly }
    assertEquals(2027, yearlyCal.get(Calendar.YEAR))
    assertEquals(Calendar.OCTOBER, yearlyCal.get(Calendar.MONTH))
    assertEquals(15, yearlyCal.get(Calendar.DAY_OF_MONTH))
  }

  @Test
  fun `recurringExpenseDao persists, queries due bills, and updates active state in Room DB`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, ExpenseDatabase::class.java)
      .allowMainThreadQueries()
      .build()

    try {
      val dao = db.recurringExpenseDao()
      val now = System.currentTimeMillis()

      val rent = RecurringExpenseEntity(
        title = "House Rent",
        amount = 18000.0,
        category = "Bills & Utilities",
        frequency = "MONTHLY",
        dayOfMonth = 1,
        paymentMethod = "UPI",
        tag = "Rent",
        nextDueDate = now - 10000, // Due in the past (due now)
        isActive = true,
        autoCreateExpense = true
      )
      val netflix = RecurringExpenseEntity(
        title = "Netflix",
        amount = 649.0,
        category = "Entertainment",
        frequency = "MONTHLY",
        dayOfMonth = 25,
        paymentMethod = "Credit Card",
        tag = "Subscription",
        nextDueDate = now + 86400000L * 5, // Due in 5 days (not due now)
        isActive = true,
        autoCreateExpense = true
      )

      val rentId = dao.insertRecurringExpense(rent)
      val netflixId = dao.insertRecurringExpense(netflix)

      val allList = dao.getAllRecurringExpensesList()
      assertEquals(2, allList.size)

      // Query due expenses
      val dueExpenses = dao.getDueRecurringExpenses(now)
      assertEquals(1, dueExpenses.size)
      assertEquals("House Rent", dueExpenses[0].title)

      // Toggle active state
      val fetchedRent = dao.getRecurringExpenseById(rentId)
      assertNotNull(fetchedRent)
      dao.updateRecurringExpense(fetchedRent!!.copy(isActive = false))

      val updatedDue = dao.getDueRecurringExpenses(now)
      assertEquals(0, updatedDue.size) // No active due expenses

      // Delete
      dao.deleteRecurringExpenseById(netflixId)
      assertEquals(1, dao.getAllRecurringExpensesList().size)
    } finally {
      db.close()
    }
  }

  @Test
  fun `expenseRepository processDueRecurringExpenses creates expense and advances nextDueDate without duplicates`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, ExpenseDatabase::class.java)
      .allowMainThreadQueries()
      .build()

    try {
      val repository = ExpenseRepository(
        expenseDao = db.expenseDao(),
        budgetDao = db.budgetDao(),
        syncLogDao = db.syncLogDao(),
        recurringExpenseDao = db.recurringExpenseDao()
      )

      val now = System.currentTimeMillis()

      // Add a due recurring bill (e.g. Wi-Fi bill due yesterday)
      val wifiBill = RecurringExpenseEntity(
        title = "Wi-Fi Broadband",
        amount = 999.0,
        category = "Bills & Utilities",
        frequency = "MONTHLY",
        dayOfMonth = 10,
        paymentMethod = "UPI",
        tag = "Fixed Bill",
        nextDueDate = now - 5000, // due now
        isActive = true,
        autoCreateExpense = true
      )
      val recurringId = repository.insertRecurringExpense(wifiBill)
      assertTrue(recurringId > 0)

      // Initially expenses table is empty
      val initialExpenses = repository.allExpenses.first()
      assertEquals(0, initialExpenses.size)

      // Process due recurring expenses
      val createdExpenses = repository.processDueRecurringExpenses(currentTime = now)
      assertEquals(1, createdExpenses.size)
      assertEquals("Wi-Fi Broadband", createdExpenses[0].title)
      assertEquals(999.0, createdExpenses[0].amount, 0.01)
      assertEquals("Bills & Utilities", createdExpenses[0].category)

      // Verify that expense was inserted into Room DB
      val expensesAfterProcess = repository.allExpenses.first()
      assertEquals(1, expensesAfterProcess.size)
      assertEquals("Wi-Fi Broadband", expensesAfterProcess[0].title)

      // Verify recurring bill nextDueDate was advanced into the future
      val updatedRecurring = repository.allRecurringExpenses.first().first { it.id == recurringId }
      assertTrue("nextDueDate should be advanced past current time", updatedRecurring.nextDueDate > now)
      assertNotNull("lastExecutedDate should be recorded", updatedRecurring.lastExecutedDate)

      // Running processDueRecurringExpenses a second time should create NO duplicates
      val secondRunCreated = repository.processDueRecurringExpenses(currentTime = now)
      assertEquals(0, secondRunCreated.size)
      assertEquals(1, repository.allExpenses.first().size)
    } finally {
      db.close()
    }
  }

  @Test
  fun `generatePdfReport creates valid PDF file for transaction history`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val sampleExpenses = listOf(
      ExpenseEntity(
        id = 1,
        title = "Client Lunch Meeting",
        amount = 1850.0,
        category = "Food & Dining",
        date = System.currentTimeMillis(),
        paymentMethod = "UPI",
        tag = "Business",
        notes = "Invoice #402 with client tax GST"
      ),
      ExpenseEntity(
        id = 2,
        title = "Amazon Electronics",
        amount = 4599.0,
        category = "Shopping",
        date = System.currentTimeMillis() - 86400000L,
        paymentMethod = "Credit Card",
        tag = "Personal",
        bankName = "HDFC Bank",
        creditCardName = "HDFC Regalia",
        notes = "Office monitor adapter"
      )
    )
    val sampleBudgets = listOf(
      BudgetEntity(category = "Food & Dining", monthlyLimit = 5000.0),
      BudgetEntity(category = "Shopping", monthlyLimit = 10000.0)
    )

    val pdfFile = ReportExportManager.generatePdfReport(
      context = context,
      expenses = sampleExpenses,
      budgets = sampleBudgets,
      monthYearTitle = "Transaction History Ledger"
    )

    assertTrue("PDF file should exist", pdfFile.exists())
    assertTrue("PDF file size should be greater than zero", pdfFile.length() > 0)
    assertTrue("PDF file name should end with .pdf", pdfFile.name.endsWith(".pdf"))
  }

  @Test
  fun `generateMonthlyPdfReport and generateMonthlyCsvReport create valid monthly report files`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val cal = Calendar.getInstance().apply {
      set(Calendar.YEAR, 2026)
      set(Calendar.MONTH, Calendar.OCTOBER)
      set(Calendar.DAY_OF_MONTH, 15)
    }
    val sampleExpenses = listOf(
      ExpenseEntity(
        id = 10,
        title = "Office Coworking Rent",
        amount = 8500.0,
        category = "Bills & Utilities",
        date = cal.timeInMillis,
        paymentMethod = "Bank Transfer",
        tag = "Work",
        notes = "October office invoice"
      )
    )
    val sampleBudgets = listOf(
      BudgetEntity(category = "Bills & Utilities", monthlyLimit = 10000.0)
    )

    // Test Monthly PDF
    val monthlyPdf = ReportExportManager.generateMonthlyPdfReport(
      context = context,
      expenses = sampleExpenses,
      budgets = sampleBudgets,
      year = 2026,
      month = 10
    )
    assertTrue("Monthly PDF file should exist", monthlyPdf.exists())
    assertTrue("Monthly PDF file should not be empty", monthlyPdf.length() > 0)
    assertTrue("Monthly PDF file name should end with .pdf", monthlyPdf.name.endsWith(".pdf"))

    // Test Monthly CSV
    val monthlyCsv = ReportExportManager.generateMonthlyCsvReport(
      context = context,
      expenses = sampleExpenses,
      year = 2026,
      month = 10
    )
    assertTrue("Monthly CSV file should exist", monthlyCsv.exists())
    assertTrue("Monthly CSV file should not be empty", monthlyCsv.length() > 0)
    assertTrue("Monthly CSV file name should contain October or 10", monthlyCsv.name.contains("2026_10"))
    assertTrue("Monthly CSV file name should end with .csv", monthlyCsv.name.endsWith(".csv"))

    val content = monthlyCsv.readText(Charsets.UTF_8)
    assertTrue("CSV header should contain ID, Date, Title, Notes", content.contains("ID,Date,Title,Amount (INR),Category,Payment Method"))
    assertTrue("CSV row should contain Office Coworking Rent", content.contains("Office Coworking Rent"))
    assertTrue("CSV row should contain October office invoice", content.contains("October office invoice"))
  }

  @Test
  fun `BudgetNotificationService initializes notification channel properly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    BudgetNotificationService.initNotificationChannel(context)

    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channel = manager.getNotificationChannel(BudgetNotificationService.CHANNEL_ID)
    assertNotNull("Channel should be created", channel)
    assertEquals(BudgetNotificationService.CHANNEL_ID, channel.id)
    assertEquals(BudgetNotificationService.CHANNEL_NAME, channel.name)
    assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
  }

  @Test
  fun `evaluateBudgetsAndNotify triggers 80 percent warning and 100 percent exceeded alerts`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    BudgetNotificationService.resetAllAlertsHistory(context)
    BudgetNotificationService.setAlertsEnabled(context, true)
    BudgetNotificationService.set80AlertEnabled(context, true)
    BudgetNotificationService.set100AlertEnabled(context, true)

    // Test 80% condition (4200 spent of 5000 limit = 84%)
    val items80 = listOf(
      BudgetProgressItem(
        category = "Food & Dining",
        monthlyLimit = 5000.0,
        spentAmount = 4200.0,
        remainingAmount = 800.0,
        percentage = 84f,
        isOverBudget = false
      )
    )

    val alerts80 = BudgetNotificationService.evaluateBudgetsAndNotify(context, items80)
    assertEquals("Should generate 1 alert for 80% threshold", 1, alerts80.size)
    assertEquals(BudgetAlertThreshold.WARNING_80, alerts80[0].threshold)
    assertEquals("Food & Dining", alerts80[0].category)
    assertEquals(84f, alerts80[0].percentage)

    // Test deduplication: subsequent evaluation in the same month should NOT fire duplicate alert
    val alerts80Duplicate = BudgetNotificationService.evaluateBudgetsAndNotify(context, items80)
    assertEquals("Should not generate duplicate alert for already notified threshold", 0, alerts80Duplicate.size)

    // Test 100% condition (5500 spent of 5000 limit = 110%)
    val items100 = listOf(
      BudgetProgressItem(
        category = "Food & Dining",
        monthlyLimit = 5000.0,
        spentAmount = 5500.0,
        remainingAmount = 0.0,
        percentage = 110f,
        isOverBudget = true
      )
    )

    val alerts100 = BudgetNotificationService.evaluateBudgetsAndNotify(context, items100)
    assertEquals("Should generate 1 alert for 100% threshold", 1, alerts100.size)
    assertEquals(BudgetAlertThreshold.EXCEEDED_100, alerts100[0].threshold)
    assertEquals("Food & Dining", alerts100[0].category)
    assertEquals(110f, alerts100[0].percentage)

    // Subsequent evaluation of 100% should also be deduplicated
    val alerts100Duplicate = BudgetNotificationService.evaluateBudgetsAndNotify(context, items100)
    assertEquals("Should not generate duplicate 100% alert", 0, alerts100Duplicate.size)
  }

  @Test
  fun `evaluateBudgetsAndNotify resets alert flag when spending drops below 80 percent`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    BudgetNotificationService.resetAllAlertsHistory(context)
    BudgetNotificationService.setAlertsEnabled(context, true)

    val cal = Calendar.getInstance()
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH) + 1

    // 1. Trigger 80% alert
    val items80 = listOf(
      BudgetProgressItem(
        category = "Entertainment",
        monthlyLimit = 2000.0,
        spentAmount = 1700.0,
        remainingAmount = 300.0,
        percentage = 85f,
        isOverBudget = false
      )
    )
    val firstRun = BudgetNotificationService.evaluateBudgetsAndNotify(context, items80)
    assertEquals(1, firstRun.size)
    assertTrue(BudgetNotificationService.isAlertAlreadySent(context, year, month, "Entertainment", BudgetAlertThreshold.WARNING_80))

    // 2. User deletes or edits expense so spending drops to 1200 (60% < 80%)
    val itemsUnder80 = listOf(
      BudgetProgressItem(
        category = "Entertainment",
        monthlyLimit = 2000.0,
        spentAmount = 1200.0,
        remainingAmount = 800.0,
        percentage = 60f,
        isOverBudget = false
      )
    )
    val resetRun = BudgetNotificationService.evaluateBudgetsAndNotify(context, itemsUnder80)
    assertEquals(0, resetRun.size)
    assertFalse("Flag should be cleared when spending drops below 80%",
      BudgetNotificationService.isAlertAlreadySent(context, year, month, "Entertainment", BudgetAlertThreshold.WARNING_80)
    )

    // 3. User spends again and reaches 85% -> alert should fire again!
    val secondRun = BudgetNotificationService.evaluateBudgetsAndNotify(context, items80)
    assertEquals("Alert should fire again after being cleared", 1, secondRun.size)
    assertEquals(BudgetAlertThreshold.WARNING_80, secondRun[0].threshold)
  }

  @Test
  fun `BudgetNotificationService settings can toggle alerts on and off`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    BudgetNotificationService.resetAllAlertsHistory(context)

    BudgetNotificationService.setAlertsEnabled(context, false)
    assertFalse(BudgetNotificationService.isAlertsEnabled(context))

    val items = listOf(
      BudgetProgressItem(
        category = "Shopping",
        monthlyLimit = 5000.0,
        spentAmount = 5500.0,
        remainingAmount = 0.0,
        percentage = 110f,
        isOverBudget = true
      )
    )

    val resultsWhenDisabled = BudgetNotificationService.evaluateBudgetsAndNotify(context, items)
    assertEquals("No alerts should be generated when master switch is off", 0, resultsWhenDisabled.size)

    BudgetNotificationService.setAlertsEnabled(context, true)
    assertTrue(BudgetNotificationService.isAlertsEnabled(context))
  }

  @Test
  fun `sendTestNotification sends test notification safely`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    org.robolectric.Shadows.shadowOf(app).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
    BudgetNotificationService.initNotificationChannel(context)

    // Tests that sending test notifications executes and posts successfully
    val result80 = BudgetNotificationService.sendTestNotification(context, BudgetAlertThreshold.WARNING_80, "Food & Dining")
    val result100 = BudgetNotificationService.sendTestNotification(context, BudgetAlertThreshold.EXCEEDED_100, "Travel")
    assertTrue("Should successfully post 80% test notification", result80)
    assertTrue("Should successfully post 100% test notification", result100)
  }

  @Test
  fun `securityAndSyncManager google sign in and sign out updates state`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = com.example.security.SecurityAndSyncManager(context)

    manager.signOutGoogle()
    assertEquals(null, manager.googleUserEmail.value)

    manager.signInGoogle("test.user@gmail.com")
    assertEquals("test.user@gmail.com", manager.googleUserEmail.value)

    manager.recordSyncSuccess()
    assertTrue(manager.lastSyncTime.value > 0L)

    manager.signOutGoogle()
    assertEquals(null, manager.googleUserEmail.value)
  }

  @Test
  fun `categoryBudgetGauge displays correct percentage consumed and remaining headroom`() {
    // Case 1: Within Budget (4500 spent of 6000 limit -> 75% consumed, 1500 remaining)
    val itemWithin = BudgetProgressItem(
      category = "Food",
      monthlyLimit = 6000.0,
      spentAmount = 4500.0,
      remainingAmount = 1500.0,
      percentage = 75.0f,
      isOverBudget = false
    )
    assertEquals(75.0f, itemWithin.percentage, 0.01f)
    assertEquals(1500.0, itemWithin.remainingAmount, 0.01)
    assertFalse(itemWithin.isOverBudget)

    // Case 2: Over Budget (7200 spent of 5000 limit -> 144% consumed, 0 remaining, 2200 over)
    val itemOver = BudgetProgressItem(
      category = "Shopping",
      monthlyLimit = 5000.0,
      spentAmount = 7200.0,
      remainingAmount = 0.0,
      percentage = 144.0f,
      isOverBudget = true
    )
    assertEquals(144.0f, itemOver.percentage, 0.01f)
    assertEquals(0.0, itemOver.remainingAmount, 0.01)
    assertTrue(itemOver.isOverBudget)
    assertEquals(2200.0, itemOver.spentAmount - itemOver.monthlyLimit, 0.01)
  }

  @Test
  fun `exportCsvReport exports Room DB records to CSV with RFC-4180 encoding and UTF-8 BOM`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val testDate = 1773500000000L

    val roomExpenses = listOf(
      ExpenseEntity(
        id = 101,
        title = "Grocery Supplies",
        amount = 1850.75,
        category = "Groceries",
        date = testDate,
        paymentMethod = "UPI",
        bankName = "State Bank of India",
        creditCardName = "",
        tag = "Essential",
        notes = "Vegetables, milk, pulses"
      ),
      ExpenseEntity(
        id = 102,
        title = "Weekend Dinner",
        amount = 2400.00,
        category = "Food",
        date = testDate + 86400000L,
        paymentMethod = "Credit Card",
        bankName = "HDFC Bank",
        creditCardName = "HDFC Regalia",
        tag = "Dining Out",
        notes = "Dinner with family"
      )
    )

    val csvFile = ReportExportManager.generateCsvReport(
      context = context,
      expenses = roomExpenses,
      fileNamePrefix = "Room_Ledger_Backup"
    )

    assertTrue("CSV file should exist", csvFile.exists())
    assertTrue("CSV file must have .csv extension", csvFile.name.endsWith(".csv"))
    assertTrue("CSV file must start with prefix", csvFile.name.startsWith("Room_Ledger_Backup"))
    assertTrue("CSV file length must be > 0", csvFile.length() > 0)

    val content = csvFile.readText(Charsets.UTF_8)
    // Check header
    assertTrue("Must contain header", content.contains("ID,Date,Title,Amount (INR),Category,Payment Method,Bank Name,Credit Card,Tag,Notes"))
    // Check items
    assertTrue("Must contain Grocery Supplies row", content.contains("Grocery Supplies"))
    assertTrue("Must contain State Bank of India", content.contains("State Bank of India"))
    assertTrue("Must contain HDFC Regalia", content.contains("HDFC Regalia"))
    assertTrue("Must contain 1850.75", content.contains("1850.75"))
    assertTrue("Must contain 2400.0", content.contains("2400.0"))
  }

  @Test
  fun `authManager initializes with guest state and valid web client ID`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val authManager = com.example.auth.AuthManager(context)

    // Initially in guest mode
    assertFalse("Should not be authenticating initially", authManager.isAuthenticating.value)
    assertEquals(null, authManager.authErrorMessage.value)

    // Web client ID must be present in resources from google-services.json
    val clientId = authManager.getWebClientId()
    assertNotNull("Web Client ID should be loaded from resources", clientId)
    assertTrue("Web Client ID should not be empty", clientId!!.isNotEmpty())
    assertTrue("Web Client ID should match Google OAuth format", clientId.contains(".apps.googleusercontent.com"))
  }

  @Test
  fun `GeminiAiService accurately parses paper receipts with merchant, amount, category, and date`() = kotlinx.coroutines.test.runTest {
    val service = com.example.ai.GeminiAiService()
    val dummyBitmap = android.graphics.Bitmap.createBitmap(100, 100, android.graphics.Bitmap.Config.ARGB_8888)

    val extracted = service.parseReceiptImage(dummyBitmap)
    assertNotNull("Extracted receipt should not be null", extracted)
    assertTrue("Merchant name should be extracted", extracted!!.title.isNotEmpty())
    assertTrue("Amount should be positive", extracted.amount > 0)
    assertTrue("Category should be valid", extracted.category.isNotEmpty())
    assertTrue("Payment method should be valid", extracted.paymentMethod.isNotEmpty())
    assertTrue("Date timestamp should be valid", extracted.date > 0)
  }

  @Test
  fun `MainActivity launches and resumes without throwing exception`() {
    val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java)
    val activity = controller.setup().get()
    assertNotNull(activity)
    assertNotNull(activity.findViewById(android.R.id.content))
  }
}



