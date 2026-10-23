package com.example.export

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.BudgetEntity
import com.example.data.model.ExpenseEntity
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object ReportExportManager {

    fun generatePdfReport(
        context: Context,
        expenses: List<ExpenseEntity>,
        budgets: List<BudgetEntity>,
        monthYearTitle: String = "Monthly Financial Report"
    ): File {
        val pdfDoc = PdfDocument()
        val pageWidth = 595 // A4 standard width in points
        val pageHeight = 842 // A4 standard height in points

        val totalSpent = expenses.sumOf { it.amount }
        val categoryGroups = expenses.groupBy { it.category }
        val topCategory = categoryGroups.maxByOrNull { it.value.sumOf { exp -> exp.amount } }?.key ?: "N/A"

        val titlePaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 18f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val subTitlePaint = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 10f
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.parseColor("#1E293B")
            textSize = 9f
            isAntiAlias = true
        }

        val boldTextPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 9f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val headerBgPaint = Paint().apply {
            color = Color.parseColor("#0D9488") // Emerald Teal
            style = Paint.Style.FILL
        }

        val cardBgPaint = Paint().apply {
            color = Color.parseColor("#F1F5F9")
            style = Paint.Style.FILL
        }

        val linePaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 0.8f
        }

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDoc.startPage(pageInfo)
        var canvas = page.canvas

        // Header Banner
        canvas.drawRoundRect(RectF(30f, 30f, 565f, 90f), 8f, 8f, headerBgPaint)
        val bannerTitlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 16f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val bannerSubPaint = Paint().apply {
            color = Color.parseColor("#E0F2FE")
            textSize = 9f
            isAntiAlias = true
        }
        canvas.drawText("EXPENSE TRACKER — $monthYearTitle", 45f, 58f, bannerTitlePaint)
        canvas.drawText("Generated on ${SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(Date())} • Currency: INR (₹)", 45f, 75f, bannerSubPaint)

        var y = 110f

        // 3 Summary Cards
        val cardWidth = 165f
        // Card 1: Total Spent
        canvas.drawRoundRect(RectF(30f, y, 30f + cardWidth, y + 55f), 6f, 6f, cardBgPaint)
        canvas.drawText("TOTAL SPENDING", 40f, y + 20f, subTitlePaint)
        canvas.drawText(ExpenseEntity.formatInr(totalSpent), 40f, y + 42f, titlePaint)

        // Card 2: Transactions
        val c2X = 30f + cardWidth + 15f
        canvas.drawRoundRect(RectF(c2X, y, c2X + cardWidth, y + 55f), 6f, 6f, cardBgPaint)
        canvas.drawText("TOTAL TRANSACTIONS", c2X + 10f, y + 20f, subTitlePaint)
        canvas.drawText("${expenses.size} entries", c2X + 10f, y + 42f, titlePaint)

        // Card 3: Top Category
        val c3X = c2X + cardWidth + 15f
        canvas.drawRoundRect(RectF(c3X, y, c3X + cardWidth, y + 55f), 6f, 6f, cardBgPaint)
        canvas.drawText("TOP SPENDING CATEGORY", c3X + 10f, y + 20f, subTitlePaint)
        canvas.drawText(topCategory, c3X + 10f, y + 42f, boldTextPaint)

        y += 75f

        // Category Breakdown Section
        canvas.drawText("Category Breakdown & Budgets", 30f, y, titlePaint)
        y += 15f

        // Table Header
        val colCat = 35f
        val colSpent = 220f
        val colBudget = 320f
        val colStatus = 430f

        canvas.drawRect(30f, y, 565f, y + 18f, cardBgPaint)
        canvas.drawText("Category", colCat, y + 12f, boldTextPaint)
        canvas.drawText("Spent (₹)", colSpent, y + 12f, boldTextPaint)
        canvas.drawText("Budget (₹)", colBudget, y + 12f, boldTextPaint)
        canvas.drawText("Adherence", colStatus, y + 12f, boldTextPaint)
        y += 24f

        for ((cat, catExpenses) in categoryGroups.entries.sortedByDescending { it.value.sumOf { e -> e.amount } }) {
            val spent = catExpenses.sumOf { it.amount }
            val budget = budgets.firstOrNull { it.category.equals(cat, ignoreCase = true) }?.monthlyLimit ?: 0.0
            val status = if (budget > 0) {
                val pct = (spent / budget) * 100
                if (pct > 100) "Over budget (${pct.toInt()}%)" else "${(100 - pct).toInt()}% remaining"
            } else "No budget limit"

            canvas.drawText(cat, colCat, y, textPaint)
            canvas.drawText(ExpenseEntity.formatInr(spent), colSpent, y, textPaint)
            canvas.drawText(if (budget > 0) ExpenseEntity.formatInr(budget) else "—", colBudget, y, textPaint)
            canvas.drawText(status, colStatus, y, textPaint)
            canvas.drawLine(30f, y + 4f, 565f, y + 4f, linePaint)
            y += 18f
        }

        y += 15f
        canvas.drawText("Detailed Transaction Ledger", 30f, y, titlePaint)
        y += 15f

        // Ledger Table Header
        val colDate = 35f
        val colTitle = 110f
        val colCategory = 260f
        val colMethod = 370f
        val colAmount = 480f

        canvas.drawRect(30f, y, 565f, y + 18f, cardBgPaint)
        canvas.drawText("Date", colDate, y + 12f, boldTextPaint)
        canvas.drawText("Description / Merchant", colTitle, y + 12f, boldTextPaint)
        canvas.drawText("Category", colCategory, y + 12f, boldTextPaint)
        canvas.drawText("Payment", colMethod, y + 12f, boldTextPaint)
        canvas.drawText("Amount (₹)", colAmount, y + 12f, boldTextPaint)
        y += 24f

        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        for ((index, item) in expenses.withIndex()) {
            if (y > pageHeight - 50f) {
                // draw footer
                canvas.drawText("Page $pageNumber", pageWidth / 2f - 15f, pageHeight - 20f, subTitlePaint)
                pdfDoc.finishPage(page)

                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDoc.startPage(pageInfo)
                canvas = page.canvas
                y = 40f

                // repeat header on subsequent pages
                canvas.drawRect(30f, y, 565f, y + 18f, cardBgPaint)
                canvas.drawText("Date", colDate, y + 12f, boldTextPaint)
                canvas.drawText("Description / Merchant", colTitle, y + 12f, boldTextPaint)
                canvas.drawText("Category", colCategory, y + 12f, boldTextPaint)
                canvas.drawText("Payment", colMethod, y + 12f, boldTextPaint)
                canvas.drawText("Amount (₹)", colAmount, y + 12f, boldTextPaint)
                y += 24f
            }

            // Alternating row styling
            if (index % 2 == 0) {
                canvas.drawRect(30f, y - 10f, 565f, y + 6f, cardBgPaint)
            }

            val methodStr = when {
                item.creditCardName.isNotBlank() -> item.creditCardName
                item.bankName.isNotBlank() -> "${item.paymentMethod} - ${item.bankName}"
                else -> item.paymentMethod
            }
            canvas.drawText(sdf.format(Date(item.date)), colDate, y, textPaint)
            canvas.drawText(item.title.take(24), colTitle, y, textPaint)
            canvas.drawText(item.category.take(18), colCategory, y, textPaint)
            canvas.drawText(methodStr.take(18), colMethod, y, textPaint)
            canvas.drawText(ExpenseEntity.formatInr(item.amount), colAmount, y, boldTextPaint)
            canvas.drawLine(30f, y + 6f, 565f, y + 6f, linePaint)
            y += 18f
        }

        canvas.drawText("Page $pageNumber", pageWidth / 2f - 15f, pageHeight - 20f, subTitlePaint)
        pdfDoc.finishPage(page)

        val outputFile = File(context.cacheDir, "Expense_Report_${System.currentTimeMillis()}.pdf")
        val out = FileOutputStream(outputFile)
        pdfDoc.writeTo(out)
        out.flush()
        out.close()
        pdfDoc.close()
        return outputFile
    }

    fun generateCsvReport(context: Context, expenses: List<ExpenseEntity>): File {
        val outputFile = File(context.cacheDir, "Expenses_${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}.csv")
        val writer = OutputStreamWriter(FileOutputStream(outputFile), Charsets.UTF_8)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // UTF-8 BOM (Byte Order Mark) prefix for Excel compatibility
        writer.write("\uFEFF")
        writer.write("ID,Date,Title,Amount (INR),Category,Payment Method,Bank Name,Credit Card,Tag,Notes\n")
        for (e in expenses) {
            val dateStr = sdf.format(Date(e.date))
            val safeTitle = "\"${e.title.replace("\"", "\"\"")}\""
            val safeCategory = "\"${e.category.replace("\"", "\"\"")}\""
            val safePaymentMethod = "\"${e.paymentMethod.replace("\"", "\"\"")}\""
            val safeBankName = "\"${e.bankName.replace("\"", "\"\"")}\""
            val safeCreditCard = "\"${e.creditCardName.replace("\"", "\"\"")}\""
            val safeTag = "\"${e.tag.replace("\"", "\"\"")}\""
            val safeNotes = "\"${e.notes.replace("\"", "\"\"")}\""
            writer.write("${e.id},$dateStr,$safeTitle,${e.amount},$safeCategory,$safePaymentMethod,$safeBankName,$safeCreditCard,$safeTag,$safeNotes\n")
        }
        writer.flush()
        writer.close()
        return outputFile
    }

    fun generateExcelSpreadsheet(context: Context, expenses: List<ExpenseEntity>, budgets: List<BudgetEntity>): File {
        val outputFile = File(context.cacheDir, "Expense_Ledger_${System.currentTimeMillis()}.xlsx")
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val total = expenses.sumOf { it.amount }

        // Sheet 1: Expenses
        val expRows = mutableListOf<OpenXmlXlsxGenerator.XlsxRow>()
        expRows.add(
            OpenXmlXlsxGenerator.XlsxRow(
                cells = listOf(
                    OpenXmlXlsxGenerator.XlsxCell.Text("ID", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Date", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Description", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Amount (INR)", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Category", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Payment Method", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Bank Name", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Credit Card", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Tag", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Notes", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL)
                ),
                height = 24.0
            )
        )

        for (e in expenses) {
            expRows.add(
                OpenXmlXlsxGenerator.XlsxRow(
                    cells = listOf(
                        OpenXmlXlsxGenerator.XlsxCell.Number(e.id.toDouble(), OpenXmlXlsxGenerator.XlsxStyle.TEXT_BORDER),
                        OpenXmlXlsxGenerator.XlsxCell.Text(sdf.format(Date(e.date)), OpenXmlXlsxGenerator.XlsxStyle.DATE),
                        OpenXmlXlsxGenerator.XlsxCell.Text(e.title, OpenXmlXlsxGenerator.XlsxStyle.TEXT_BORDER),
                        OpenXmlXlsxGenerator.XlsxCell.Number(e.amount, OpenXmlXlsxGenerator.XlsxStyle.CURRENCY),
                        OpenXmlXlsxGenerator.XlsxCell.Text(e.category, OpenXmlXlsxGenerator.XlsxStyle.TEXT_BORDER),
                        OpenXmlXlsxGenerator.XlsxCell.Text(e.paymentMethod, OpenXmlXlsxGenerator.XlsxStyle.TEXT_BORDER),
                        OpenXmlXlsxGenerator.XlsxCell.Text(e.bankName, OpenXmlXlsxGenerator.XlsxStyle.TEXT_BORDER),
                        OpenXmlXlsxGenerator.XlsxCell.Text(e.creditCardName, OpenXmlXlsxGenerator.XlsxStyle.TEXT_BORDER),
                        OpenXmlXlsxGenerator.XlsxCell.Text(e.tag, OpenXmlXlsxGenerator.XlsxStyle.TEXT_BORDER),
                        OpenXmlXlsxGenerator.XlsxCell.Text(e.notes, OpenXmlXlsxGenerator.XlsxStyle.TEXT_BORDER)
                    )
                )
            )
        }

        // Total Summary Row
        expRows.add(
            OpenXmlXlsxGenerator.XlsxRow(
                cells = listOf(
                    OpenXmlXlsxGenerator.XlsxCell.Text("TOTAL", OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("${expenses.size} Items", OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Empty(OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Number(total, OpenXmlXlsxGenerator.XlsxStyle.TOTAL_CURRENCY),
                    OpenXmlXlsxGenerator.XlsxCell.Empty(OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Empty(OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Empty(OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Empty(OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Empty(OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Empty(OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL)
                ),
                height = 22.0
            )
        )

        val expenseSheet = OpenXmlXlsxGenerator.XlsxSheet(
            name = "Expenses",
            colWidths = listOf(8.0, 14.0, 26.0, 16.0, 18.0, 18.0, 18.0, 20.0, 14.0, 26.0),
            rows = expRows
        )

        // Sheet 2: Budgets
        val budgetRows = mutableListOf<OpenXmlXlsxGenerator.XlsxRow>()
        budgetRows.add(
            OpenXmlXlsxGenerator.XlsxRow(
                cells = listOf(
                    OpenXmlXlsxGenerator.XlsxCell.Text("Category", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Monthly Limit (₹)", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL)
                ),
                height = 24.0
            )
        )
        for (b in budgets) {
            budgetRows.add(
                OpenXmlXlsxGenerator.XlsxRow(
                    cells = listOf(
                        OpenXmlXlsxGenerator.XlsxCell.Text(b.category, OpenXmlXlsxGenerator.XlsxStyle.TEXT_BORDER),
                        OpenXmlXlsxGenerator.XlsxCell.Number(b.monthlyLimit, OpenXmlXlsxGenerator.XlsxStyle.CURRENCY)
                    )
                )
            )
        }

        val budgetSheet = OpenXmlXlsxGenerator.XlsxSheet(
            name = "Budgets",
            colWidths = listOf(22.0, 18.0),
            rows = budgetRows
        )

        return OpenXmlXlsxGenerator.createWorkbook(outputFile, listOf(expenseSheet, budgetSheet))
    }

    fun saveToDownloads(context: Context, file: File, mimeType: String): Pair<Boolean, String> {
        return try {
            val fileName = file.name
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return Pair(false, "Failed to create download entry in MediaStore")
                resolver.openOutputStream(uri)?.use { out ->
                    file.inputStream().use { input ->
                        input.copyTo(out)
                    }
                }
                Pair(true, "Successfully saved to Downloads: $fileName")
            } else {
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                val destFile = File(downloadsDir, fileName)
                file.copyTo(destFile, overwrite = true)
                Pair(true, "Successfully saved to Downloads: ${destFile.name}")
            }
        } catch (e: Exception) {
            Pair(false, e.localizedMessage ?: "Failed to save to Downloads")
        }
    }

    fun openFile(context: Context, file: File, mimeType: String): Boolean {
        return try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun shareFile(context: Context, file: File, mimeType: String, title: String = "Share Financial Report") {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun generateFullPersonalDashboardExcel(context: Context, expenses: List<ExpenseEntity>, budgets: List<BudgetEntity>): File {
        val outputFile = File(context.cacheDir, "Personal_Finance_Dashboard_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.xlsx")

        val totalSpent = expenses.sumOf { it.amount }
        val totalTransactions = expenses.size
        val avgTransaction = if (totalTransactions > 0) totalSpent / totalTransactions else 0.0
        val totalBudget = budgets.sumOf { it.monthlyLimit }

        val categoryGroups = expenses.groupBy { it.category }
        val topCategory = categoryGroups.maxByOrNull { it.value.sumOf { e -> e.amount } }?.key ?: "None"
        val paymentGroups = expenses.groupBy { it.paymentMethod }
        val topPaymentMethod = paymentGroups.maxByOrNull { it.value.sumOf { e -> e.amount } }?.key ?: "None"
        val maxExpense = expenses.maxByOrNull { it.amount }

        val minDate = expenses.minOfOrNull { it.date } ?: System.currentTimeMillis()
        val maxDate = expenses.maxOfOrNull { it.date } ?: System.currentTimeMillis()

        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfYear = SimpleDateFormat("yyyy", Locale.getDefault())
        val sdfMonthNum = SimpleDateFormat("MM", Locale.getDefault())
        val sdfMonthName = SimpleDateFormat("MMMM", Locale.getDefault())
        val sdfDay = SimpleDateFormat("dd", Locale.getDefault())
        val sdfDayOfWeek = SimpleDateFormat("EEEE", Locale.getDefault())
        val sdfYearMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val sdfFullTimestamp = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

        // ----------------------------------------------------
        // SHEET 1: Dashboard KPIs
        // ----------------------------------------------------
        val kpiRows = mutableListOf<OpenXmlXlsxGenerator.XlsxRow>()
        // Title Row
        kpiRows.add(
            OpenXmlXlsxGenerator.XlsxRow(
                cells = listOf(
                    OpenXmlXlsxGenerator.XlsxCell.Text(" PERSONAL FINANCE DASHBOARD & KPI SUMMARY", OpenXmlXlsxGenerator.XlsxStyle.TITLE),
                    OpenXmlXlsxGenerator.XlsxCell.Empty(OpenXmlXlsxGenerator.XlsxStyle.TITLE),
                    OpenXmlXlsxGenerator.XlsxCell.Empty(OpenXmlXlsxGenerator.XlsxStyle.TITLE)
                ),
                height = 30.0
            )
        )
        // Metadata row
        kpiRows.add(
            OpenXmlXlsxGenerator.XlsxRow(
                cells = listOf(
                    OpenXmlXlsxGenerator.XlsxCell.Text("Generated On: ${sdfFullTimestamp.format(Date())}", OpenXmlXlsxGenerator.XlsxStyle.SUBHEADER),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Currency: Indian Rupee (INR - ₹)", OpenXmlXlsxGenerator.XlsxStyle.SUBHEADER),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Ready for Excel/Sheets Pivot Tables & Charts", OpenXmlXlsxGenerator.XlsxStyle.SUBHEADER)
                )
            )
        )
        // Empty spacer
        kpiRows.add(OpenXmlXlsxGenerator.XlsxRow(cells = emptyList()))
        // Table Header
        kpiRows.add(
            OpenXmlXlsxGenerator.XlsxRow(
                cells = listOf(
                    OpenXmlXlsxGenerator.XlsxCell.Text("Key Performance Indicator (KPI)", OpenXmlXlsxGenerator.XlsxStyle.HEADER_PURPLE),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Value", OpenXmlXlsxGenerator.XlsxStyle.HEADER_PURPLE),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Notes / Context", OpenXmlXlsxGenerator.XlsxStyle.HEADER_PURPLE)
                ),
                height = 24.0
            )
        )
        // KPI: Total Spent
        kpiRows.add(
            OpenXmlXlsxGenerator.XlsxRow(
                cells = listOf(
                    OpenXmlXlsxGenerator.XlsxCell.Text("Total All-Time Spend", OpenXmlXlsxGenerator.XlsxStyle.KPI_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Number(totalSpent, OpenXmlXlsxGenerator.XlsxStyle.KPI_CURRENCY),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Sum of all recorded transactions", OpenXmlXlsxGenerator.XlsxStyle.KPI_TEXT)
                )
            )
        )
        // KPI: Total Transactions
        kpiRows.add(
            OpenXmlXlsxGenerator.XlsxRow(
                cells = listOf(
                    OpenXmlXlsxGenerator.XlsxCell.Text("Total Number of Transactions", OpenXmlXlsxGenerator.XlsxStyle.KPI_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Number(totalTransactions.toDouble(), OpenXmlXlsxGenerator.XlsxStyle.KPI_NUMBER),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Distinct expense records", OpenXmlXlsxGenerator.XlsxStyle.KPI_TEXT)
                )
            )
        )
        // KPI: Average Ticket
        kpiRows.add(
            OpenXmlXlsxGenerator.XlsxRow(
                cells = listOf(
                    OpenXmlXlsxGenerator.XlsxCell.Text("Average Ticket / Expense", OpenXmlXlsxGenerator.XlsxStyle.KPI_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Number(avgTransaction, OpenXmlXlsxGenerator.XlsxStyle.KPI_CURRENCY),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Average amount per purchase", OpenXmlXlsxGenerator.XlsxStyle.KPI_TEXT)
                )
            )
        )
        // KPI: Total Budget
        kpiRows.add(
            OpenXmlXlsxGenerator.XlsxRow(
                cells = listOf(
                    OpenXmlXlsxGenerator.XlsxCell.Text("Total Monthly Budget Allocated", OpenXmlXlsxGenerator.XlsxStyle.KPI_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Number(totalBudget, OpenXmlXlsxGenerator.XlsxStyle.KPI_CURRENCY),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Combined budget limits across categories", OpenXmlXlsxGenerator.XlsxStyle.KPI_TEXT)
                )
            )
        )
        // KPI: Top Category
        kpiRows.add(
            OpenXmlXlsxGenerator.XlsxRow(
                cells = listOf(
                    OpenXmlXlsxGenerator.XlsxCell.Text("Top Spending Category", OpenXmlXlsxGenerator.XlsxStyle.KPI_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Text(topCategory, OpenXmlXlsxGenerator.XlsxStyle.KPI_TEXT),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Category with highest cumulative volume", OpenXmlXlsxGenerator.XlsxStyle.KPI_TEXT)
                )
            )
        )
        // KPI: Top Payment Method
        kpiRows.add(
            OpenXmlXlsxGenerator.XlsxRow(
                cells = listOf(
                    OpenXmlXlsxGenerator.XlsxCell.Text("Most Used Payment Method", OpenXmlXlsxGenerator.XlsxStyle.KPI_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Text(topPaymentMethod, OpenXmlXlsxGenerator.XlsxStyle.KPI_TEXT),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Preferred mode of settlement", OpenXmlXlsxGenerator.XlsxStyle.KPI_TEXT)
                )
            )
        )
        if (maxExpense != null) {
            kpiRows.add(
                OpenXmlXlsxGenerator.XlsxRow(
                    cells = listOf(
                        OpenXmlXlsxGenerator.XlsxCell.Text("Highest Single Expense", OpenXmlXlsxGenerator.XlsxStyle.KPI_LABEL),
                        OpenXmlXlsxGenerator.XlsxCell.Number(maxExpense.amount, OpenXmlXlsxGenerator.XlsxStyle.KPI_CURRENCY),
                        OpenXmlXlsxGenerator.XlsxCell.Text("${maxExpense.title} (${maxExpense.category}) on ${sdfDate.format(Date(maxExpense.date))}", OpenXmlXlsxGenerator.XlsxStyle.KPI_TEXT)
                    )
                )
            )
        }
        kpiRows.add(
            OpenXmlXlsxGenerator.XlsxRow(
                cells = listOf(
                    OpenXmlXlsxGenerator.XlsxCell.Text("Data Date Range", OpenXmlXlsxGenerator.XlsxStyle.KPI_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("${sdfDate.format(Date(minDate))} to ${sdfDate.format(Date(maxDate))}", OpenXmlXlsxGenerator.XlsxStyle.KPI_TEXT),
                    OpenXmlXlsxGenerator.XlsxCell.Text("All historical ledger entries", OpenXmlXlsxGenerator.XlsxStyle.KPI_TEXT)
                )
            )
        )
        kpiRows.add(OpenXmlXlsxGenerator.XlsxRow(cells = emptyList()))
        kpiRows.add(
            OpenXmlXlsxGenerator.XlsxRow(
                cells = listOf(
                    OpenXmlXlsxGenerator.XlsxCell.Text("TIP: Open Sheet 'All_Expenses_Raw' to build Pivot Tables & Charts (Excel/Sheets: Insert -> Pivot Table)", OpenXmlXlsxGenerator.XlsxStyle.KPI_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Empty(OpenXmlXlsxGenerator.XlsxStyle.KPI_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Empty(OpenXmlXlsxGenerator.XlsxStyle.KPI_LABEL)
                )
            )
        )

        val kpiSheet = OpenXmlXlsxGenerator.XlsxSheet(
            name = "Dashboard_KPIs",
            colWidths = listOf(30.0, 26.0, 36.0),
            rows = kpiRows,
            mergedRegions = listOf("A1:C1", "A14:C14")
        )

        // ----------------------------------------------------
        // SHEET 2: Master Raw Data
        // ----------------------------------------------------
        val rawRows = mutableListOf<OpenXmlXlsxGenerator.XlsxRow>()
        rawRows.add(
            OpenXmlXlsxGenerator.XlsxRow(
                cells = listOf(
                    OpenXmlXlsxGenerator.XlsxCell.Text("ID", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Date", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Year", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Month_No", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Month_Name", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Day", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Day_Of_Week", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Description_Merchant", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Amount_INR", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Category", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Payment_Method", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Bank_Name", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Credit_Card", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Tag", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Notes", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL)
                ),
                height = 24.0
            )
        )

        val sortedExpenses = expenses.sortedByDescending { it.date }
        for (e in sortedExpenses) {
            val d = Date(e.date)
            rawRows.add(
                OpenXmlXlsxGenerator.XlsxRow(
                    cells = listOf(
                        OpenXmlXlsxGenerator.XlsxCell.Number(e.id.toDouble(), OpenXmlXlsxGenerator.XlsxStyle.TEXT_BORDER),
                        OpenXmlXlsxGenerator.XlsxCell.Text(sdfDate.format(d), OpenXmlXlsxGenerator.XlsxStyle.DATE),
                        OpenXmlXlsxGenerator.XlsxCell.Number(sdfYear.format(d).toDoubleOrNull() ?: 2026.0, OpenXmlXlsxGenerator.XlsxStyle.NUMBER_INT),
                        OpenXmlXlsxGenerator.XlsxCell.Number(sdfMonthNum.format(d).toDoubleOrNull() ?: 1.0, OpenXmlXlsxGenerator.XlsxStyle.NUMBER_INT),
                        OpenXmlXlsxGenerator.XlsxCell.Text(sdfMonthName.format(d), OpenXmlXlsxGenerator.XlsxStyle.TEXT_BORDER),
                        OpenXmlXlsxGenerator.XlsxCell.Number(sdfDay.format(d).toDoubleOrNull() ?: 1.0, OpenXmlXlsxGenerator.XlsxStyle.NUMBER_INT),
                        OpenXmlXlsxGenerator.XlsxCell.Text(sdfDayOfWeek.format(d), OpenXmlXlsxGenerator.XlsxStyle.TEXT_BORDER),
                        OpenXmlXlsxGenerator.XlsxCell.Text(e.title, OpenXmlXlsxGenerator.XlsxStyle.TEXT_BORDER),
                        OpenXmlXlsxGenerator.XlsxCell.Number(e.amount, OpenXmlXlsxGenerator.XlsxStyle.CURRENCY),
                        OpenXmlXlsxGenerator.XlsxCell.Text(e.category, OpenXmlXlsxGenerator.XlsxStyle.TEXT_BORDER),
                        OpenXmlXlsxGenerator.XlsxCell.Text(e.paymentMethod, OpenXmlXlsxGenerator.XlsxStyle.TEXT_BORDER),
                        OpenXmlXlsxGenerator.XlsxCell.Text(e.bankName, OpenXmlXlsxGenerator.XlsxStyle.TEXT_BORDER),
                        OpenXmlXlsxGenerator.XlsxCell.Text(e.creditCardName, OpenXmlXlsxGenerator.XlsxStyle.TEXT_BORDER),
                        OpenXmlXlsxGenerator.XlsxCell.Text(e.tag, OpenXmlXlsxGenerator.XlsxStyle.TEXT_BORDER),
                        OpenXmlXlsxGenerator.XlsxCell.Text(e.notes, OpenXmlXlsxGenerator.XlsxStyle.TEXT_BORDER)
                    )
                )
            )
        }

        // Total Row
        rawRows.add(
            OpenXmlXlsxGenerator.XlsxRow(
                cells = listOf(
                    OpenXmlXlsxGenerator.XlsxCell.Text("TOTAL", OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("${expenses.size} Records", OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Empty(OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Empty(OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Empty(OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Empty(OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Empty(OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Empty(OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Number(totalSpent, OpenXmlXlsxGenerator.XlsxStyle.TOTAL_CURRENCY),
                    OpenXmlXlsxGenerator.XlsxCell.Empty(OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Empty(OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Empty(OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Empty(OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Empty(OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Empty(OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL)
                ),
                height = 22.0
            )
        )

        val rawSheet = OpenXmlXlsxGenerator.XlsxSheet(
            name = "All_Expenses_Raw",
            colWidths = listOf(8.0, 13.0, 9.0, 11.0, 14.0, 8.0, 14.0, 28.0, 16.0, 18.0, 16.0, 14.0, 28.0),
            rows = rawRows
        )

        // ----------------------------------------------------
        // SHEET 3: Category Summary
        // ----------------------------------------------------
        val catRows = mutableListOf<OpenXmlXlsxGenerator.XlsxRow>()
        catRows.add(
            OpenXmlXlsxGenerator.XlsxRow(
                cells = listOf(
                    OpenXmlXlsxGenerator.XlsxCell.Text("Category", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Total Spent (₹)", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Share (%)", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Tx Count", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Average Ticket (₹)", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Monthly Budget (₹)", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Budget Remaining (₹)", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Budget Utilized", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Adherence Status", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL)
                ),
                height = 24.0
            )
        )

        for ((cat, catExpenses) in categoryGroups.entries.sortedByDescending { it.value.sumOf { e -> e.amount } }) {
            val catSum = catExpenses.sumOf { it.amount }
            val share = if (totalSpent > 0) catSum / totalSpent else 0.0
            val catCount = catExpenses.size
            val catAvg = if (catCount > 0) catSum / catCount else 0.0
            val b = budgets.firstOrNull { it.category.equals(cat, ignoreCase = true) }?.monthlyLimit ?: 0.0
            val remaining = b - catSum
            val utilPct = if (b > 0) catSum / b else 0.0
            val status = if (b <= 0) "No Budget Limit" else if (catSum > b) "EXCEEDED BUDGET" else "Within Limit"

            catRows.add(
                OpenXmlXlsxGenerator.XlsxRow(
                    cells = listOf(
                        OpenXmlXlsxGenerator.XlsxCell.Text(cat, OpenXmlXlsxGenerator.XlsxStyle.TEXT_BORDER),
                        OpenXmlXlsxGenerator.XlsxCell.Number(catSum, OpenXmlXlsxGenerator.XlsxStyle.CURRENCY),
                        OpenXmlXlsxGenerator.XlsxCell.Number(share, OpenXmlXlsxGenerator.XlsxStyle.PERCENTAGE),
                        OpenXmlXlsxGenerator.XlsxCell.Number(catCount.toDouble(), OpenXmlXlsxGenerator.XlsxStyle.NUMBER_INT),
                        OpenXmlXlsxGenerator.XlsxCell.Number(catAvg, OpenXmlXlsxGenerator.XlsxStyle.CURRENCY),
                        OpenXmlXlsxGenerator.XlsxCell.Number(b, OpenXmlXlsxGenerator.XlsxStyle.CURRENCY),
                        OpenXmlXlsxGenerator.XlsxCell.Number(remaining, OpenXmlXlsxGenerator.XlsxStyle.CURRENCY),
                        OpenXmlXlsxGenerator.XlsxCell.Number(utilPct, OpenXmlXlsxGenerator.XlsxStyle.PERCENTAGE),
                        OpenXmlXlsxGenerator.XlsxCell.Text(status, OpenXmlXlsxGenerator.XlsxStyle.TEXT_BORDER)
                    )
                )
            )
        }

        // Total Row
        catRows.add(
            OpenXmlXlsxGenerator.XlsxRow(
                cells = listOf(
                    OpenXmlXlsxGenerator.XlsxCell.Text("TOTAL", OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Number(totalSpent, OpenXmlXlsxGenerator.XlsxStyle.TOTAL_CURRENCY),
                    OpenXmlXlsxGenerator.XlsxCell.Number(1.0, OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Number(totalTransactions.toDouble(), OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Number(avgTransaction, OpenXmlXlsxGenerator.XlsxStyle.TOTAL_CURRENCY),
                    OpenXmlXlsxGenerator.XlsxCell.Number(totalBudget, OpenXmlXlsxGenerator.XlsxStyle.TOTAL_CURRENCY),
                    OpenXmlXlsxGenerator.XlsxCell.Number(totalBudget - totalSpent, OpenXmlXlsxGenerator.XlsxStyle.TOTAL_CURRENCY),
                    OpenXmlXlsxGenerator.XlsxCell.Number(if (totalBudget > 0) totalSpent / totalBudget else 0.0, OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Empty(OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL)
                ),
                height = 22.0
            )
        )

        val categorySheet = OpenXmlXlsxGenerator.XlsxSheet(
            name = "Category_Summary",
            colWidths = listOf(22.0, 18.0, 12.0, 12.0, 18.0, 18.0, 18.0, 15.0, 18.0),
            rows = catRows
        )

        // ----------------------------------------------------
        // SHEET 4: Monthly Trends
        // ----------------------------------------------------
        val monthRows = mutableListOf<OpenXmlXlsxGenerator.XlsxRow>()
        monthRows.add(
            OpenXmlXlsxGenerator.XlsxRow(
                cells = listOf(
                    OpenXmlXlsxGenerator.XlsxCell.Text("Year-Month", OpenXmlXlsxGenerator.XlsxStyle.HEADER_PURPLE),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Period Name", OpenXmlXlsxGenerator.XlsxStyle.HEADER_PURPLE),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Total Spending (₹)", OpenXmlXlsxGenerator.XlsxStyle.HEADER_PURPLE),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Tx Count", OpenXmlXlsxGenerator.XlsxStyle.HEADER_PURPLE),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Average Tx (₹)", OpenXmlXlsxGenerator.XlsxStyle.HEADER_PURPLE),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Max Single Tx (₹)", OpenXmlXlsxGenerator.XlsxStyle.HEADER_PURPLE)
                ),
                height = 24.0
            )
        )

        val monthlyGroups = expenses.groupBy { sdfYearMonth.format(Date(it.date)) }
        for ((ym, mExpenses) in monthlyGroups.entries.sortedByDescending { it.key }) {
            val mSum = mExpenses.sumOf { it.amount }
            val mCount = mExpenses.size
            val mAvg = if (mCount > 0) mSum / mCount else 0.0
            val mMax = mExpenses.maxOfOrNull { it.amount } ?: 0.0
            val periodName = if (mExpenses.isNotEmpty()) SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(mExpenses[0].date)) else ym

            monthRows.add(
                OpenXmlXlsxGenerator.XlsxRow(
                    cells = listOf(
                        OpenXmlXlsxGenerator.XlsxCell.Text(ym, OpenXmlXlsxGenerator.XlsxStyle.DATE),
                        OpenXmlXlsxGenerator.XlsxCell.Text(periodName, OpenXmlXlsxGenerator.XlsxStyle.TEXT_BORDER),
                        OpenXmlXlsxGenerator.XlsxCell.Number(mSum, OpenXmlXlsxGenerator.XlsxStyle.CURRENCY),
                        OpenXmlXlsxGenerator.XlsxCell.Number(mCount.toDouble(), OpenXmlXlsxGenerator.XlsxStyle.NUMBER_INT),
                        OpenXmlXlsxGenerator.XlsxCell.Number(mAvg, OpenXmlXlsxGenerator.XlsxStyle.CURRENCY),
                        OpenXmlXlsxGenerator.XlsxCell.Number(mMax, OpenXmlXlsxGenerator.XlsxStyle.CURRENCY)
                    )
                )
            )
        }

        val monthlySheet = OpenXmlXlsxGenerator.XlsxSheet(
            name = "Monthly_Trends",
            colWidths = listOf(15.0, 18.0, 18.0, 12.0, 18.0, 18.0),
            rows = monthRows
        )

        // ----------------------------------------------------
        // SHEET 5: Payment Methods Share
        // ----------------------------------------------------
        val payRows = mutableListOf<OpenXmlXlsxGenerator.XlsxRow>()
        payRows.add(
            OpenXmlXlsxGenerator.XlsxRow(
                cells = listOf(
                    OpenXmlXlsxGenerator.XlsxCell.Text("Payment Method", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Total Amount (₹)", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Share (%)", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Transactions", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Avg Transaction (₹)", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL)
                ),
                height = 24.0
            )
        )

        for ((method, mList) in paymentGroups.entries.sortedByDescending { it.value.sumOf { e -> e.amount } }) {
            val pSum = mList.sumOf { it.amount }
            val pShare = if (totalSpent > 0) pSum / totalSpent else 0.0
            val pCount = mList.size
            val pAvg = if (pCount > 0) pSum / pCount else 0.0

            payRows.add(
                OpenXmlXlsxGenerator.XlsxRow(
                    cells = listOf(
                        OpenXmlXlsxGenerator.XlsxCell.Text(method, OpenXmlXlsxGenerator.XlsxStyle.TEXT_BORDER),
                        OpenXmlXlsxGenerator.XlsxCell.Number(pSum, OpenXmlXlsxGenerator.XlsxStyle.CURRENCY),
                        OpenXmlXlsxGenerator.XlsxCell.Number(pShare, OpenXmlXlsxGenerator.XlsxStyle.PERCENTAGE),
                        OpenXmlXlsxGenerator.XlsxCell.Number(pCount.toDouble(), OpenXmlXlsxGenerator.XlsxStyle.NUMBER_INT),
                        OpenXmlXlsxGenerator.XlsxCell.Number(pAvg, OpenXmlXlsxGenerator.XlsxStyle.CURRENCY)
                    )
                )
            )
        }

        val paySheet = OpenXmlXlsxGenerator.XlsxSheet(
            name = "Payment_Methods",
            colWidths = listOf(20.0, 18.0, 12.0, 14.0, 18.0),
            rows = payRows
        )

        // ----------------------------------------------------
        // SHEET 6: Tags Distribution
        // ----------------------------------------------------
        val tagRows = mutableListOf<OpenXmlXlsxGenerator.XlsxRow>()
        tagRows.add(
            OpenXmlXlsxGenerator.XlsxRow(
                cells = listOf(
                    OpenXmlXlsxGenerator.XlsxCell.Text("Tag / Purpose", OpenXmlXlsxGenerator.XlsxStyle.HEADER_PURPLE),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Total Amount (₹)", OpenXmlXlsxGenerator.XlsxStyle.HEADER_PURPLE),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Share (%)", OpenXmlXlsxGenerator.XlsxStyle.HEADER_PURPLE),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Tx Count", OpenXmlXlsxGenerator.XlsxStyle.HEADER_PURPLE)
                ),
                height = 24.0
            )
        )

        val tagGroups = expenses.groupBy { it.tag }
        for ((tag, tList) in tagGroups.entries.sortedByDescending { it.value.sumOf { e -> e.amount } }) {
            val tSum = tList.sumOf { it.amount }
            val tShare = if (totalSpent > 0) tSum / totalSpent else 0.0
            val tCount = tList.size

            tagRows.add(
                OpenXmlXlsxGenerator.XlsxRow(
                    cells = listOf(
                        OpenXmlXlsxGenerator.XlsxCell.Text(tag, OpenXmlXlsxGenerator.XlsxStyle.TEXT_BORDER),
                        OpenXmlXlsxGenerator.XlsxCell.Number(tSum, OpenXmlXlsxGenerator.XlsxStyle.CURRENCY),
                        OpenXmlXlsxGenerator.XlsxCell.Number(tShare, OpenXmlXlsxGenerator.XlsxStyle.PERCENTAGE),
                        OpenXmlXlsxGenerator.XlsxCell.Number(tCount.toDouble(), OpenXmlXlsxGenerator.XlsxStyle.NUMBER_INT)
                    )
                )
            )
        }

        val tagSheet = OpenXmlXlsxGenerator.XlsxSheet(
            name = "Tags_Distribution",
            colWidths = listOf(18.0, 18.0, 12.0, 12.0),
            rows = tagRows
        )

        // ----------------------------------------------------
        // SHEET 7: Budgets Configuration
        // ----------------------------------------------------
        val budgetRows = mutableListOf<OpenXmlXlsxGenerator.XlsxRow>()
        budgetRows.add(
            OpenXmlXlsxGenerator.XlsxRow(
                cells = listOf(
                    OpenXmlXlsxGenerator.XlsxCell.Text("Category", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Monthly Limit (₹)", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL)
                ),
                height = 24.0
            )
        )
        for (b in budgets) {
            budgetRows.add(
                OpenXmlXlsxGenerator.XlsxRow(
                    cells = listOf(
                        OpenXmlXlsxGenerator.XlsxCell.Text(b.category, OpenXmlXlsxGenerator.XlsxStyle.TEXT_BORDER),
                        OpenXmlXlsxGenerator.XlsxCell.Number(b.monthlyLimit, OpenXmlXlsxGenerator.XlsxStyle.CURRENCY)
                    )
                )
            )
        }

        val budgetSheet = OpenXmlXlsxGenerator.XlsxSheet(
            name = "Budgets_Config",
            colWidths = listOf(22.0, 18.0),
            rows = budgetRows
        )

        return OpenXmlXlsxGenerator.createWorkbook(
            outputFile = outputFile,
            sheets = listOf(kpiSheet, rawSheet, categorySheet, monthlySheet, paySheet, tagSheet, budgetSheet)
        )
    }

    /**
     * Comprehensive Multi-Sheet Excel (.xlsx) Report Generator for Monthly and Custom Date Ranges
     * with Full Transaction Details (all 18 columns), KPIs, Category Budgets, Daily Trends, and Payment Breakdowns.
     */
    fun generatePeriodDetailedExcelReport(
        context: Context,
        expenses: List<ExpenseEntity>,
        budgets: List<BudgetEntity>,
        reportTitle: String,
        periodDescription: String,
        fileName: String
    ): File {
        val outputFile = File(context.cacheDir, fileName)
        val sortedExpenses = expenses.sortedWith(compareByDescending<ExpenseEntity> { it.date }.thenByDescending { it.id })

        val totalSpent = sortedExpenses.sumOf { it.amount }
        val totalTransactions = sortedExpenses.size
        val avgTransaction = if (totalTransactions > 0) totalSpent / totalTransactions else 0.0

        val categoryGroups = sortedExpenses.groupBy { it.category }
        val topCategory = categoryGroups.maxByOrNull { it.value.sumOf { e -> e.amount } }?.key ?: "None"
        val topCategoryAmount = categoryGroups[topCategory]?.sumOf { it.amount } ?: 0.0

        val paymentGroups = sortedExpenses.groupBy { it.paymentMethod }
        val topPaymentMethod = paymentGroups.maxByOrNull { it.value.sumOf { e -> e.amount } }?.key ?: "None"
        val topPaymentAmount = paymentGroups[topPaymentMethod]?.sumOf { it.amount } ?: 0.0

        val maxExpense = sortedExpenses.maxByOrNull { it.amount }
        val totalBudget = budgets.sumOf { it.monthlyLimit }

        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfTime = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val sdfYear = SimpleDateFormat("yyyy", Locale.getDefault())
        val sdfMonthNum = SimpleDateFormat("MM", Locale.getDefault())
        val sdfMonthName = SimpleDateFormat("MMMM", Locale.getDefault())
        val sdfDay = SimpleDateFormat("dd", Locale.getDefault())
        val sdfDayOfWeek = SimpleDateFormat("EEEE", Locale.getDefault())
        val sdfFullTimestamp = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val sdfCreatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        // Calculate unique active spending days in the period
        val activeDaysCount = sortedExpenses.map { sdfDate.format(Date(it.date)) }.distinct().size
        val dailyAverage = if (activeDaysCount > 0) totalSpent / activeDaysCount else 0.0

        // ==========================================
        // SHEET 1: Report Overview & Financial KPIs
        // ==========================================
        val kpiRows = mutableListOf<OpenXmlXlsxGenerator.XlsxRow>()
        // Title Row
        kpiRows.add(
            OpenXmlXlsxGenerator.XlsxRow(
                cells = listOf(
                    OpenXmlXlsxGenerator.XlsxCell.Text(" $reportTitle", OpenXmlXlsxGenerator.XlsxStyle.TITLE),
                    OpenXmlXlsxGenerator.XlsxCell.Empty(OpenXmlXlsxGenerator.XlsxStyle.TITLE),
                    OpenXmlXlsxGenerator.XlsxCell.Empty(OpenXmlXlsxGenerator.XlsxStyle.TITLE)
                ),
                height = 30.0
            )
        )
        // Subtitle / Metadata
        kpiRows.add(
            OpenXmlXlsxGenerator.XlsxRow(
                cells = listOf(
                    OpenXmlXlsxGenerator.XlsxCell.Text("Period: $periodDescription", OpenXmlXlsxGenerator.XlsxStyle.SUBHEADER),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Generated: ${sdfFullTimestamp.format(Date())}", OpenXmlXlsxGenerator.XlsxStyle.SUBHEADER),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Currency: INR (₹)", OpenXmlXlsxGenerator.XlsxStyle.SUBHEADER)
                )
            )
        )
        kpiRows.add(OpenXmlXlsxGenerator.XlsxRow(cells = emptyList()))

        // KPI Table Header
        kpiRows.add(
            OpenXmlXlsxGenerator.XlsxRow(
                cells = listOf(
                    OpenXmlXlsxGenerator.XlsxCell.Text("Key Performance Indicator", OpenXmlXlsxGenerator.XlsxStyle.HEADER_PURPLE),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Value", OpenXmlXlsxGenerator.XlsxStyle.HEADER_PURPLE),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Context / Benchmark", OpenXmlXlsxGenerator.XlsxStyle.HEADER_PURPLE)
                ),
                height = 24.0
            )
        )

        // KPI Rows
        kpiRows.add(
            OpenXmlXlsxGenerator.XlsxRow(
                cells = listOf(
                    OpenXmlXlsxGenerator.XlsxCell.Text("Total Spending", OpenXmlXlsxGenerator.XlsxStyle.KPI_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Number(totalSpent, OpenXmlXlsxGenerator.XlsxStyle.KPI_CURRENCY),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Sum of transactions in selected period", OpenXmlXlsxGenerator.XlsxStyle.KPI_TEXT)
                )
            )
        )
        kpiRows.add(
            OpenXmlXlsxGenerator.XlsxRow(
                cells = listOf(
                    OpenXmlXlsxGenerator.XlsxCell.Text("Total Transactions", OpenXmlXlsxGenerator.XlsxStyle.KPI_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Number(totalTransactions.toDouble(), OpenXmlXlsxGenerator.XlsxStyle.KPI_NUMBER),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Number of recorded ledger entries", OpenXmlXlsxGenerator.XlsxStyle.KPI_TEXT)
                )
            )
        )
        kpiRows.add(
            OpenXmlXlsxGenerator.XlsxRow(
                cells = listOf(
                    OpenXmlXlsxGenerator.XlsxCell.Text("Daily Average Spend", OpenXmlXlsxGenerator.XlsxStyle.KPI_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Number(dailyAverage, OpenXmlXlsxGenerator.XlsxStyle.KPI_CURRENCY),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Across $activeDaysCount active spending days", OpenXmlXlsxGenerator.XlsxStyle.KPI_TEXT)
                )
            )
        )
        kpiRows.add(
            OpenXmlXlsxGenerator.XlsxRow(
                cells = listOf(
                    OpenXmlXlsxGenerator.XlsxCell.Text("Average Ticket Size", OpenXmlXlsxGenerator.XlsxStyle.KPI_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Number(avgTransaction, OpenXmlXlsxGenerator.XlsxStyle.KPI_CURRENCY),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Average expenditure per transaction", OpenXmlXlsxGenerator.XlsxStyle.KPI_TEXT)
                )
            )
        )
        kpiRows.add(
            OpenXmlXlsxGenerator.XlsxRow(
                cells = listOf(
                    OpenXmlXlsxGenerator.XlsxCell.Text("Top Spending Category", OpenXmlXlsxGenerator.XlsxStyle.KPI_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Text(topCategory, OpenXmlXlsxGenerator.XlsxStyle.KPI_TEXT),
                    OpenXmlXlsxGenerator.XlsxCell.Text(if (totalSpent > 0) "${ExpenseEntity.formatInr(topCategoryAmount)} (${String.format(Locale.US, "%.1f", (topCategoryAmount / totalSpent) * 100)}%)" else "N/A", OpenXmlXlsxGenerator.XlsxStyle.KPI_TEXT)
                )
            )
        )
        kpiRows.add(
            OpenXmlXlsxGenerator.XlsxRow(
                cells = listOf(
                    OpenXmlXlsxGenerator.XlsxCell.Text("Top Payment Method", OpenXmlXlsxGenerator.XlsxStyle.KPI_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Text(topPaymentMethod, OpenXmlXlsxGenerator.XlsxStyle.KPI_TEXT),
                    OpenXmlXlsxGenerator.XlsxCell.Text(if (totalSpent > 0) "${ExpenseEntity.formatInr(topPaymentAmount)} (${String.format(Locale.US, "%.1f", (topPaymentAmount / totalSpent) * 100)}%)" else "N/A", OpenXmlXlsxGenerator.XlsxStyle.KPI_TEXT)
                )
            )
        )
        if (maxExpense != null) {
            kpiRows.add(
                OpenXmlXlsxGenerator.XlsxRow(
                    cells = listOf(
                        OpenXmlXlsxGenerator.XlsxCell.Text("Largest Single Expense", OpenXmlXlsxGenerator.XlsxStyle.KPI_LABEL),
                        OpenXmlXlsxGenerator.XlsxCell.Number(maxExpense.amount, OpenXmlXlsxGenerator.XlsxStyle.KPI_CURRENCY),
                        OpenXmlXlsxGenerator.XlsxCell.Text("${maxExpense.title} (${maxExpense.category}) on ${sdfDate.format(Date(maxExpense.date))}", OpenXmlXlsxGenerator.XlsxStyle.KPI_TEXT)
                    )
                )
            )
        }
        if (totalBudget > 0) {
            kpiRows.add(
                OpenXmlXlsxGenerator.XlsxRow(
                    cells = listOf(
                        OpenXmlXlsxGenerator.XlsxCell.Text("Total Monthly Budget Limit", OpenXmlXlsxGenerator.XlsxStyle.KPI_LABEL),
                        OpenXmlXlsxGenerator.XlsxCell.Number(totalBudget, OpenXmlXlsxGenerator.XlsxStyle.KPI_CURRENCY),
                        OpenXmlXlsxGenerator.XlsxCell.Text("Allocated across all configured categories", OpenXmlXlsxGenerator.XlsxStyle.KPI_TEXT)
                    )
                )
            )
            val budgetVariance = totalBudget - totalSpent
            val budgetVarianceLabel = if (budgetVariance >= 0) "Budget Remaining (Surplus)" else "Budget Deficit (Exceeded)"
            kpiRows.add(
                OpenXmlXlsxGenerator.XlsxRow(
                    cells = listOf(
                        OpenXmlXlsxGenerator.XlsxCell.Text(budgetVarianceLabel, OpenXmlXlsxGenerator.XlsxStyle.KPI_LABEL),
                        OpenXmlXlsxGenerator.XlsxCell.Number(budgetVariance, OpenXmlXlsxGenerator.XlsxStyle.KPI_CURRENCY),
                        OpenXmlXlsxGenerator.XlsxCell.Text("Budget utilization: ${String.format(Locale.US, "%.1f", (totalSpent / totalBudget) * 100)}%", OpenXmlXlsxGenerator.XlsxStyle.KPI_TEXT)
                    )
                )
            )
        }
        kpiRows.add(OpenXmlXlsxGenerator.XlsxRow(cells = emptyList()))
        kpiRows.add(
            OpenXmlXlsxGenerator.XlsxRow(
                cells = listOf(
                    OpenXmlXlsxGenerator.XlsxCell.Text("See Sheet 'Full_Ledger_Details' for full transaction fields, and subsequent sheets for category & daily trends.", OpenXmlXlsxGenerator.XlsxStyle.KPI_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Empty(OpenXmlXlsxGenerator.XlsxStyle.KPI_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Empty(OpenXmlXlsxGenerator.XlsxStyle.KPI_LABEL)
                )
            )
        )

        val kpiSheet = OpenXmlXlsxGenerator.XlsxSheet(
            name = "Overview_KPIs",
            colWidths = listOf(30.0, 26.0, 42.0),
            rows = kpiRows,
            mergedRegions = listOf("A1:C1", "A${kpiRows.size}:C${kpiRows.size}")
        )

        // ==========================================
        // SHEET 2: Full Detailed Ledger (All 18 Fields)
        // ==========================================
        val ledgerRows = mutableListOf<OpenXmlXlsxGenerator.XlsxRow>()
        ledgerRows.add(
            OpenXmlXlsxGenerator.XlsxRow(
                cells = listOf(
                    OpenXmlXlsxGenerator.XlsxCell.Text("ID", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Date", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Time", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Year", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Month_No", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Month_Name", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Day", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Day_Of_Week", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Description_Merchant", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Amount_INR", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Category", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Payment_Method", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Bank_Name", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Credit_Card_Name", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Tag_Purpose", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Notes", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Receipt_Attached", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Created_Timestamp", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL)
                ),
                height = 24.0
            )
        )

        for (e in sortedExpenses) {
            val d = Date(e.date)
            ledgerRows.add(
                OpenXmlXlsxGenerator.XlsxRow(
                    cells = listOf(
                        OpenXmlXlsxGenerator.XlsxCell.Number(e.id.toDouble(), OpenXmlXlsxGenerator.XlsxStyle.TEXT_BORDER),
                        OpenXmlXlsxGenerator.XlsxCell.Text(sdfDate.format(d), OpenXmlXlsxGenerator.XlsxStyle.DATE),
                        OpenXmlXlsxGenerator.XlsxCell.Text(sdfTime.format(d), OpenXmlXlsxGenerator.XlsxStyle.TEXT_BORDER),
                        OpenXmlXlsxGenerator.XlsxCell.Number(sdfYear.format(d).toDoubleOrNull() ?: 2026.0, OpenXmlXlsxGenerator.XlsxStyle.NUMBER_INT),
                        OpenXmlXlsxGenerator.XlsxCell.Number(sdfMonthNum.format(d).toDoubleOrNull() ?: 1.0, OpenXmlXlsxGenerator.XlsxStyle.NUMBER_INT),
                        OpenXmlXlsxGenerator.XlsxCell.Text(sdfMonthName.format(d), OpenXmlXlsxGenerator.XlsxStyle.TEXT_BORDER),
                        OpenXmlXlsxGenerator.XlsxCell.Number(sdfDay.format(d).toDoubleOrNull() ?: 1.0, OpenXmlXlsxGenerator.XlsxStyle.NUMBER_INT),
                        OpenXmlXlsxGenerator.XlsxCell.Text(sdfDayOfWeek.format(d), OpenXmlXlsxGenerator.XlsxStyle.TEXT_BORDER),
                        OpenXmlXlsxGenerator.XlsxCell.Text(e.title, OpenXmlXlsxGenerator.XlsxStyle.TEXT_BORDER),
                        OpenXmlXlsxGenerator.XlsxCell.Number(e.amount, OpenXmlXlsxGenerator.XlsxStyle.CURRENCY),
                        OpenXmlXlsxGenerator.XlsxCell.Text(e.category, OpenXmlXlsxGenerator.XlsxStyle.TEXT_BORDER),
                        OpenXmlXlsxGenerator.XlsxCell.Text(e.paymentMethod, OpenXmlXlsxGenerator.XlsxStyle.TEXT_BORDER),
                        OpenXmlXlsxGenerator.XlsxCell.Text(e.bankName, OpenXmlXlsxGenerator.XlsxStyle.TEXT_BORDER),
                        OpenXmlXlsxGenerator.XlsxCell.Text(e.creditCardName, OpenXmlXlsxGenerator.XlsxStyle.TEXT_BORDER),
                        OpenXmlXlsxGenerator.XlsxCell.Text(e.tag, OpenXmlXlsxGenerator.XlsxStyle.TEXT_BORDER),
                        OpenXmlXlsxGenerator.XlsxCell.Text(e.notes, OpenXmlXlsxGenerator.XlsxStyle.TEXT_BORDER),
                        OpenXmlXlsxGenerator.XlsxCell.Text(if (!e.receiptUri.isNullOrBlank()) "Yes" else "No", OpenXmlXlsxGenerator.XlsxStyle.TEXT_BORDER),
                        OpenXmlXlsxGenerator.XlsxCell.Text(sdfCreatedAt.format(Date(e.createdAt)), OpenXmlXlsxGenerator.XlsxStyle.TEXT_BORDER)
                    )
                )
            )
        }

        // Ledger Total Row
        ledgerRows.add(
            OpenXmlXlsxGenerator.XlsxRow(
                cells = listOf(
                    OpenXmlXlsxGenerator.XlsxCell.Text("TOTAL", OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("${sortedExpenses.size} Records", OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Empty(OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Empty(OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Empty(OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Empty(OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Empty(OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Empty(OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Empty(OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Number(totalSpent, OpenXmlXlsxGenerator.XlsxStyle.TOTAL_CURRENCY),
                    OpenXmlXlsxGenerator.XlsxCell.Empty(OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Empty(OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Empty(OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Empty(OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Empty(OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Empty(OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Empty(OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Empty(OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL)
                ),
                height = 22.0
            )
        )

        val ledgerSheet = OpenXmlXlsxGenerator.XlsxSheet(
            name = "Full_Ledger_Details",
            colWidths = listOf(8.0, 13.0, 11.0, 9.0, 11.0, 14.0, 8.0, 14.0, 28.0, 16.0, 18.0, 18.0, 18.0, 20.0, 14.0, 28.0, 14.0, 18.0),
            rows = ledgerRows
        )

        // ==========================================
        // SHEET 3: Category Summary & Budgets
        // ==========================================
        val catRows = mutableListOf<OpenXmlXlsxGenerator.XlsxRow>()
        catRows.add(
            OpenXmlXlsxGenerator.XlsxRow(
                cells = listOf(
                    OpenXmlXlsxGenerator.XlsxCell.Text("Category", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Total Spent (₹)", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Share (%)", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Tx Count", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Average Ticket (₹)", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Monthly Budget (₹)", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Budget Remaining (₹)", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Budget Utilized", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Adherence Status", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL)
                ),
                height = 24.0
            )
        )

        for ((cat, catExpenses) in categoryGroups.entries.sortedByDescending { it.value.sumOf { e -> e.amount } }) {
            val catSum = catExpenses.sumOf { it.amount }
            val share = if (totalSpent > 0) catSum / totalSpent else 0.0
            val catCount = catExpenses.size
            val catAvg = if (catCount > 0) catSum / catCount else 0.0
            val b = budgets.firstOrNull { it.category.equals(cat, ignoreCase = true) }?.monthlyLimit ?: 0.0
            val remaining = b - catSum
            val utilPct = if (b > 0) catSum / b else 0.0
            val status = if (b <= 0) "No Budget Set" else if (catSum > b) "EXCEEDED BUDGET" else "Within Limit"

            catRows.add(
                OpenXmlXlsxGenerator.XlsxRow(
                    cells = listOf(
                        OpenXmlXlsxGenerator.XlsxCell.Text(cat, OpenXmlXlsxGenerator.XlsxStyle.TEXT_BORDER),
                        OpenXmlXlsxGenerator.XlsxCell.Number(catSum, OpenXmlXlsxGenerator.XlsxStyle.CURRENCY),
                        OpenXmlXlsxGenerator.XlsxCell.Number(share, OpenXmlXlsxGenerator.XlsxStyle.PERCENTAGE),
                        OpenXmlXlsxGenerator.XlsxCell.Number(catCount.toDouble(), OpenXmlXlsxGenerator.XlsxStyle.NUMBER_INT),
                        OpenXmlXlsxGenerator.XlsxCell.Number(catAvg, OpenXmlXlsxGenerator.XlsxStyle.CURRENCY),
                        OpenXmlXlsxGenerator.XlsxCell.Number(b, OpenXmlXlsxGenerator.XlsxStyle.CURRENCY),
                        OpenXmlXlsxGenerator.XlsxCell.Number(remaining, OpenXmlXlsxGenerator.XlsxStyle.CURRENCY),
                        OpenXmlXlsxGenerator.XlsxCell.Number(utilPct, OpenXmlXlsxGenerator.XlsxStyle.PERCENTAGE),
                        OpenXmlXlsxGenerator.XlsxCell.Text(status, OpenXmlXlsxGenerator.XlsxStyle.TEXT_BORDER)
                    )
                )
            )
        }

        // Category Total Row
        catRows.add(
            OpenXmlXlsxGenerator.XlsxRow(
                cells = listOf(
                    OpenXmlXlsxGenerator.XlsxCell.Text("TOTAL", OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Number(totalSpent, OpenXmlXlsxGenerator.XlsxStyle.TOTAL_CURRENCY),
                    OpenXmlXlsxGenerator.XlsxCell.Number(1.0, OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Number(totalTransactions.toDouble(), OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Number(avgTransaction, OpenXmlXlsxGenerator.XlsxStyle.TOTAL_CURRENCY),
                    OpenXmlXlsxGenerator.XlsxCell.Number(totalBudget, OpenXmlXlsxGenerator.XlsxStyle.TOTAL_CURRENCY),
                    OpenXmlXlsxGenerator.XlsxCell.Number(totalBudget - totalSpent, OpenXmlXlsxGenerator.XlsxStyle.TOTAL_CURRENCY),
                    OpenXmlXlsxGenerator.XlsxCell.Number(if (totalBudget > 0) totalSpent / totalBudget else 0.0, OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Empty(OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL)
                ),
                height = 22.0
            )
        )

        val categorySheet = OpenXmlXlsxGenerator.XlsxSheet(
            name = "Category_Summary",
            colWidths = listOf(22.0, 18.0, 12.0, 12.0, 18.0, 18.0, 18.0, 15.0, 18.0),
            rows = catRows
        )

        // ==========================================
        // SHEET 4: Daily Breakdown & Trend
        // ==========================================
        val dailyRows = mutableListOf<OpenXmlXlsxGenerator.XlsxRow>()
        dailyRows.add(
            OpenXmlXlsxGenerator.XlsxRow(
                cells = listOf(
                    OpenXmlXlsxGenerator.XlsxCell.Text("Date", OpenXmlXlsxGenerator.XlsxStyle.HEADER_PURPLE),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Day of Week", OpenXmlXlsxGenerator.XlsxStyle.HEADER_PURPLE),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Daily Spend (₹)", OpenXmlXlsxGenerator.XlsxStyle.HEADER_PURPLE),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Tx Count", OpenXmlXlsxGenerator.XlsxStyle.HEADER_PURPLE),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Top Expense Title", OpenXmlXlsxGenerator.XlsxStyle.HEADER_PURPLE),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Top Amount (₹)", OpenXmlXlsxGenerator.XlsxStyle.HEADER_PURPLE)
                ),
                height = 24.0
            )
        )

        val dailyGroups = sortedExpenses.groupBy { sdfDate.format(Date(it.date)) }
        for ((dateStr, dList) in dailyGroups.entries.sortedBy { it.key }) {
            val dSum = dList.sumOf { it.amount }
            val dCount = dList.size
            val dMax = dList.maxByOrNull { it.amount }
            val dayOfWeek = if (dList.isNotEmpty()) sdfDayOfWeek.format(Date(dList[0].date)) else ""

            dailyRows.add(
                OpenXmlXlsxGenerator.XlsxRow(
                    cells = listOf(
                        OpenXmlXlsxGenerator.XlsxCell.Text(dateStr, OpenXmlXlsxGenerator.XlsxStyle.DATE),
                        OpenXmlXlsxGenerator.XlsxCell.Text(dayOfWeek, OpenXmlXlsxGenerator.XlsxStyle.TEXT_BORDER),
                        OpenXmlXlsxGenerator.XlsxCell.Number(dSum, OpenXmlXlsxGenerator.XlsxStyle.CURRENCY),
                        OpenXmlXlsxGenerator.XlsxCell.Number(dCount.toDouble(), OpenXmlXlsxGenerator.XlsxStyle.NUMBER_INT),
                        OpenXmlXlsxGenerator.XlsxCell.Text(dMax?.title ?: "", OpenXmlXlsxGenerator.XlsxStyle.TEXT_BORDER),
                        OpenXmlXlsxGenerator.XlsxCell.Number(dMax?.amount ?: 0.0, OpenXmlXlsxGenerator.XlsxStyle.CURRENCY)
                    )
                )
            )
        }

        dailyRows.add(
            OpenXmlXlsxGenerator.XlsxRow(
                cells = listOf(
                    OpenXmlXlsxGenerator.XlsxCell.Text("TOTAL", OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("${dailyGroups.size} Days Active", OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Number(totalSpent, OpenXmlXlsxGenerator.XlsxStyle.TOTAL_CURRENCY),
                    OpenXmlXlsxGenerator.XlsxCell.Number(totalTransactions.toDouble(), OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Empty(OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Empty(OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL)
                ),
                height = 22.0
            )
        )

        val dailySheet = OpenXmlXlsxGenerator.XlsxSheet(
            name = "Daily_Breakdown",
            colWidths = listOf(14.0, 16.0, 18.0, 12.0, 28.0, 18.0),
            rows = dailyRows
        )

        // ==========================================
        // SHEET 5: Payment Methods Breakdown
        // ==========================================
        val payRows = mutableListOf<OpenXmlXlsxGenerator.XlsxRow>()
        payRows.add(
            OpenXmlXlsxGenerator.XlsxRow(
                cells = listOf(
                    OpenXmlXlsxGenerator.XlsxCell.Text("Payment Method", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Total Amount (₹)", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Share (%)", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Transactions", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Avg Transaction (₹)", OpenXmlXlsxGenerator.XlsxStyle.HEADER_TEAL)
                ),
                height = 24.0
            )
        )

        for ((method, mList) in paymentGroups.entries.sortedByDescending { it.value.sumOf { e -> e.amount } }) {
            val pSum = mList.sumOf { it.amount }
            val pShare = if (totalSpent > 0) pSum / totalSpent else 0.0
            val pCount = mList.size
            val pAvg = if (pCount > 0) pSum / pCount else 0.0

            payRows.add(
                OpenXmlXlsxGenerator.XlsxRow(
                    cells = listOf(
                        OpenXmlXlsxGenerator.XlsxCell.Text(method, OpenXmlXlsxGenerator.XlsxStyle.TEXT_BORDER),
                        OpenXmlXlsxGenerator.XlsxCell.Number(pSum, OpenXmlXlsxGenerator.XlsxStyle.CURRENCY),
                        OpenXmlXlsxGenerator.XlsxCell.Number(pShare, OpenXmlXlsxGenerator.XlsxStyle.PERCENTAGE),
                        OpenXmlXlsxGenerator.XlsxCell.Number(pCount.toDouble(), OpenXmlXlsxGenerator.XlsxStyle.NUMBER_INT),
                        OpenXmlXlsxGenerator.XlsxCell.Number(pAvg, OpenXmlXlsxGenerator.XlsxStyle.CURRENCY)
                    )
                )
            )
        }

        payRows.add(
            OpenXmlXlsxGenerator.XlsxRow(
                cells = listOf(
                    OpenXmlXlsxGenerator.XlsxCell.Text("TOTAL", OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Number(totalSpent, OpenXmlXlsxGenerator.XlsxStyle.TOTAL_CURRENCY),
                    OpenXmlXlsxGenerator.XlsxCell.Number(1.0, OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Number(totalTransactions.toDouble(), OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Number(avgTransaction, OpenXmlXlsxGenerator.XlsxStyle.TOTAL_CURRENCY)
                ),
                height = 22.0
            )
        )

        val paySheet = OpenXmlXlsxGenerator.XlsxSheet(
            name = "Payment_Methods",
            colWidths = listOf(22.0, 18.0, 12.0, 14.0, 18.0),
            rows = payRows
        )

        // ==========================================
        // SHEET 6: Tags & Purpose Breakdown
        // ==========================================
        val tagRows = mutableListOf<OpenXmlXlsxGenerator.XlsxRow>()
        tagRows.add(
            OpenXmlXlsxGenerator.XlsxRow(
                cells = listOf(
                    OpenXmlXlsxGenerator.XlsxCell.Text("Tag / Purpose", OpenXmlXlsxGenerator.XlsxStyle.HEADER_PURPLE),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Total Amount (₹)", OpenXmlXlsxGenerator.XlsxStyle.HEADER_PURPLE),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Share (%)", OpenXmlXlsxGenerator.XlsxStyle.HEADER_PURPLE),
                    OpenXmlXlsxGenerator.XlsxCell.Text("Tx Count", OpenXmlXlsxGenerator.XlsxStyle.HEADER_PURPLE)
                ),
                height = 24.0
            )
        )

        val tagGroups = sortedExpenses.groupBy { it.tag }
        for ((tag, tList) in tagGroups.entries.sortedByDescending { it.value.sumOf { e -> e.amount } }) {
            val tSum = tList.sumOf { it.amount }
            val tShare = if (totalSpent > 0) tSum / totalSpent else 0.0
            val tCount = tList.size

            tagRows.add(
                OpenXmlXlsxGenerator.XlsxRow(
                    cells = listOf(
                        OpenXmlXlsxGenerator.XlsxCell.Text(tag, OpenXmlXlsxGenerator.XlsxStyle.TEXT_BORDER),
                        OpenXmlXlsxGenerator.XlsxCell.Number(tSum, OpenXmlXlsxGenerator.XlsxStyle.CURRENCY),
                        OpenXmlXlsxGenerator.XlsxCell.Number(tShare, OpenXmlXlsxGenerator.XlsxStyle.PERCENTAGE),
                        OpenXmlXlsxGenerator.XlsxCell.Number(tCount.toDouble(), OpenXmlXlsxGenerator.XlsxStyle.NUMBER_INT)
                    )
                )
            )
        }

        tagRows.add(
            OpenXmlXlsxGenerator.XlsxRow(
                cells = listOf(
                    OpenXmlXlsxGenerator.XlsxCell.Text("TOTAL", OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Number(totalSpent, OpenXmlXlsxGenerator.XlsxStyle.TOTAL_CURRENCY),
                    OpenXmlXlsxGenerator.XlsxCell.Number(1.0, OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL),
                    OpenXmlXlsxGenerator.XlsxCell.Number(totalTransactions.toDouble(), OpenXmlXlsxGenerator.XlsxStyle.TOTAL_LABEL)
                ),
                height = 22.0
            )
        )

        val tagSheet = OpenXmlXlsxGenerator.XlsxSheet(
            name = "Tags_Breakdown",
            colWidths = listOf(20.0, 18.0, 12.0, 12.0),
            rows = tagRows
        )

        return OpenXmlXlsxGenerator.createWorkbook(
            outputFile = outputFile,
            sheets = listOf(kpiSheet, ledgerSheet, categorySheet, dailySheet, paySheet, tagSheet)
        )
    }

    /**
     * Generate Full Detailed Monthly Report in Excel (.xlsx) for a given Year and Month (1..12).
     */
    fun generateMonthlyDetailedExcelReport(
        context: Context,
        allExpenses: List<ExpenseEntity>,
        budgets: List<BudgetEntity>,
        year: Int,
        month: Int
    ): File {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startMs = cal.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endMs = cal.timeInMillis

        val monthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(Date(startMs))
        val filtered = allExpenses.filter { it.date in startMs..endMs }
        val reportTitle = "MONTHLY FINANCIAL REPORT — ${monthName.uppercase(Locale.getDefault())} $year"
        val periodDesc = "$monthName $year (1 to ${cal.getActualMaximum(Calendar.DAY_OF_MONTH)})"
        val fileName = "Monthly_Expense_Report_${year}_${String.format(Locale.US, "%02d", month)}_${monthName}.xlsx"

        return generatePeriodDetailedExcelReport(
            context = context,
            expenses = filtered,
            budgets = budgets,
            reportTitle = reportTitle,
            periodDescription = periodDesc,
            fileName = fileName
        )
    }

    /**
     * Generate Full Detailed Custom Date Range Report in Excel (.xlsx) between two timestamp boundaries.
     */
    fun generateCustomDateDetailedExcelReport(
        context: Context,
        allExpenses: List<ExpenseEntity>,
        budgets: List<BudgetEntity>,
        startDateMs: Long,
        endDateMs: Long
    ): File {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val sdfFile = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
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
        val startMs = calStart.timeInMillis
        val endMs = calEnd.timeInMillis

        val filtered = allExpenses.filter { it.date in startMs..endMs }
        val reportTitle = "CUSTOM DATE RANGE FINANCIAL REPORT"
        val periodDesc = "${sdf.format(Date(startMs))} to ${sdf.format(Date(endMs))}"
        val fileName = "Custom_Expense_Report_${sdfFile.format(Date(startMs))}_to_${sdfFile.format(Date(endMs))}.xlsx"

        return generatePeriodDetailedExcelReport(
            context = context,
            expenses = filtered,
            budgets = budgets,
            reportTitle = reportTitle,
            periodDescription = periodDesc,
            fileName = fileName
        )
    }
}
