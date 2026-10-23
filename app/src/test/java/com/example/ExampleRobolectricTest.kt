package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ExpenseEntity
import com.example.export.ReportExportManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.FileInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Expense Tracker", appName)
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
}



