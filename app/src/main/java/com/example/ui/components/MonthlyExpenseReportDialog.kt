package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ExpenseEntity
import com.example.export.ReportExportManager
import com.example.ui.theme.AmberGold
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SophisticatedDarkBorder
import com.example.ui.theme.SophisticatedDarkOnSurface
import com.example.ui.theme.SophisticatedDarkOnSurfaceVariant
import com.example.ui.theme.SophisticatedDarkSurface
import com.example.ui.theme.SophisticatedDarkSurfaceDim
import com.example.ui.theme.SophisticatedDarkSurfaceVariant
import com.example.ui.theme.SophisticatedPrimary
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MonthlyExpenseReportDialog(
    initialYear: Int,
    initialMonth: Int, // 1..12
    viewModel: ExpenseViewModel,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val allExpenses by viewModel.allExpenses.collectAsStateWithLifecycle()

    var selectedYear by remember { mutableIntStateOf(initialYear) }
    var selectedMonth by remember { mutableIntStateOf(initialMonth) }

    val availableMonths = remember(allExpenses) { viewModel.getAvailableMonths() }
    val report = remember(selectedYear, selectedMonth, allExpenses) {
        viewModel.getMonthlyExpenseReportDetail(selectedYear, selectedMonth)
    }

    var showTransactionsList by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SophisticatedDarkSurfaceDim,
            border = BorderStroke(1.dp, SophisticatedDarkBorder),
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 12.dp)
                .testTag("monthly_expense_report_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header with Title & Close Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SophisticatedPrimary.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = null,
                                tint = SophisticatedPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Monthly Expense Report",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SophisticatedDarkOnSurface
                            )
                            Text(
                                text = report.monthName,
                                style = MaterialTheme.typography.bodySmall,
                                color = SophisticatedPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.testTag("monthly_report_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = SophisticatedDarkOnSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 1. Month Selector Bar (Option to view every monthly report)
                Text(
                    text = "SELECT MONTHLY REPORT:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = SophisticatedDarkOnSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableMonths) { m ->
                        val isSelected = m.year == selectedYear && m.month == selectedMonth
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedYear = m.year
                                selectedMonth = m.month
                            },
                            label = {
                                Column(modifier = Modifier.padding(vertical = 2.dp)) {
                                    Text(
                                        text = m.shortLabel,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Text(
                                        text = if (m.count > 0) "${m.count} txs • ${ExpenseEntity.formatInr(m.totalSpent)}" else "No entries",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = if (isSelected) Color.Black else SophisticatedDarkOnSurfaceVariant
                                    )
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SophisticatedPrimary,
                                selectedLabelColor = Color.Black,
                                containerColor = SophisticatedDarkSurfaceVariant.copy(alpha = 0.6f),
                                labelColor = SophisticatedDarkOnSurface
                            ),
                            border = BorderStroke(1.dp, if (isSelected) SophisticatedPrimary else SophisticatedDarkBorder),
                            modifier = Modifier.testTag("monthly_report_month_chip_${m.year}_${m.month}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 2. High-Level KPI Cards
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Total Spend Card
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = SophisticatedDarkSurfaceVariant,
                                border = BorderStroke(1.dp, SophisticatedDarkBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Total Spending",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SophisticatedDarkOnSurfaceVariant
                                    )
                                    Text(
                                        text = ExpenseEntity.formatInr(report.totalSpent),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = SophisticatedPrimary
                                    )
                                    Text(
                                        text = "${report.transactionCount} transactions",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SophisticatedDarkOnSurfaceVariant
                                    )
                                }
                            }

                            // Daily Average Card
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = SophisticatedDarkSurfaceVariant,
                                border = BorderStroke(1.dp, SophisticatedDarkBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Daily Average",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SophisticatedDarkOnSurfaceVariant
                                    )
                                    Text(
                                        text = ExpenseEntity.formatInr(report.dailyAverage),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = AmberGold
                                    )
                                    Text(
                                        text = "Per active calendar day",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SophisticatedDarkOnSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Peak Expense Card
                    if (report.highestExpense != null) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = SophisticatedDarkSurface,
                                border = BorderStroke(1.dp, SophisticatedDarkBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "Largest Single Transaction",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SophisticatedDarkOnSurfaceVariant
                                        )
                                        Text(
                                            text = report.highestExpense.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = SophisticatedDarkOnSurface
                                        )
                                        Text(
                                            text = "${report.highestExpense.category} • ${report.highestExpense.paymentMethod}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SophisticatedDarkOnSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = ExpenseEntity.formatInr(report.highestExpense.amount),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = SophisticatedPrimary
                                    )
                                }
                            }
                        }
                    }

                    // 3. Category Breakdown
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurfaceVariant),
                            border = BorderStroke(1.dp, SophisticatedDarkBorder)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Category Distribution",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SophisticatedDarkOnSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                if (report.categoryBreakdown.isEmpty()) {
                                    Text(
                                        text = "No expenses recorded for this month.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SophisticatedDarkOnSurfaceVariant
                                    )
                                } else {
                                    report.categoryBreakdown.forEach { cat ->
                                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = cat.category,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium,
                                                    color = SophisticatedDarkOnSurface
                                                )
                                                Text(
                                                    text = "${ExpenseEntity.formatInr(cat.amount)} (${String.format(Locale.getDefault(), "%.1f", cat.percentage)}%)",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = SophisticatedPrimary
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(3.dp))
                                            LinearProgressIndicator(
                                                progress = { (cat.percentage / 100f).coerceIn(0f, 1f) },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(6.dp)
                                                    .clip(RoundedCornerShape(3.dp)),
                                                color = SophisticatedPrimary,
                                                trackColor = SophisticatedDarkSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4. Top Merchants Breakdown
                    if (report.merchantBreakdown.isNotEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurfaceVariant),
                                border = BorderStroke(1.dp, SophisticatedDarkBorder)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Top Merchants & Payees",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = SophisticatedDarkOnSurface
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    report.merchantBreakdown.take(5).forEach { merch ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = merch.merchant,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium,
                                                    color = SophisticatedDarkOnSurface
                                                )
                                                Text(
                                                    text = "${merch.count} transactions",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = SophisticatedDarkOnSurfaceVariant
                                                )
                                            }
                                            Text(
                                                text = ExpenseEntity.formatInr(merch.amount),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = AmberGold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 5. Expandable Full Monthly Transactions Ledger
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurfaceVariant),
                            border = BorderStroke(1.dp, SophisticatedDarkBorder)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showTransactionsList = !showTransactionsList },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Itemized Expense Ledger",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = SophisticatedDarkOnSurface
                                        )
                                        Text(
                                            text = "${report.expenses.size} entries recorded in ${report.shortName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SophisticatedDarkOnSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        imageVector = if (showTransactionsList) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = if (showTransactionsList) "Hide" else "Show",
                                        tint = SophisticatedPrimary
                                    )
                                }

                                AnimatedVisibility(visible = showTransactionsList) {
                                    Column(modifier = Modifier.padding(top = 10.dp)) {
                                        HorizontalDivider(color = SophisticatedDarkBorder)
                                        Spacer(modifier = Modifier.height(8.dp))

                                        if (report.expenses.isEmpty()) {
                                            Text(
                                                text = "No expenses to display.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = SophisticatedDarkOnSurfaceVariant
                                            )
                                        } else {
                                            val sdfDate = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
                                            report.expenses.forEach { exp ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = exp.title,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = SophisticatedDarkOnSurface
                                                        )
                                                        Text(
                                                            text = "${exp.category} • ${exp.paymentMethod} • ${sdfDate.format(Date(exp.date))}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = SophisticatedDarkOnSurfaceVariant
                                                        )
                                                        if (exp.notes.isNotBlank()) {
                                                            Text(
                                                                text = "Notes: ${exp.notes}",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = SophisticatedDarkOnSurfaceVariant.copy(alpha = 0.8f)
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        text = ExpenseEntity.formatInr(exp.amount),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = SophisticatedPrimary
                                                    )
                                                }
                                                HorizontalDivider(color = SophisticatedDarkBorder.copy(alpha = 0.4f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Export Actions Row
                Text(
                    text = "EXPORT REPORT (${report.monthName.uppercase(Locale.getDefault())}):",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = SophisticatedDarkOnSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    var showPdfMenu by remember { mutableStateOf(false) }
                    var showCsvMenu by remember { mutableStateOf(false) }
                    var showExcelMenu by remember { mutableStateOf(false) }

                    // 1. Export PDF
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { showPdfMenu = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("monthly_report_export_pdf_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, DangerRed)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = DangerRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "PDF",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = DangerRed
                            )
                        }

                        DropdownMenu(
                            expanded = showPdfMenu,
                            onDismissRequest = { showPdfMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("Save PDF to Downloads", fontWeight = FontWeight.Bold)
                                        Text("Save for personal tax & bookkeeping records", style = MaterialTheme.typography.bodySmall, color = SophisticatedDarkOnSurfaceVariant)
                                    }
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Download, contentDescription = null, tint = DangerRed)
                                },
                                onClick = {
                                    showPdfMenu = false
                                    val pdfFile = viewModel.exportMonthlyPdfReport(selectedYear, selectedMonth)
                                    val (saved, msg) = ReportExportManager.saveToDownloads(
                                        context,
                                        pdfFile,
                                        "application/pdf"
                                    )
                                    android.widget.Toast.makeText(
                                        context,
                                        if (saved) "Saved PDF to Downloads: ${pdfFile.name}" else msg,
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("Share PDF with Accountant", fontWeight = FontWeight.Bold)
                                        Text("Send formatted statement via Email, WhatsApp, Drive", style = MaterialTheme.typography.bodySmall, color = SophisticatedDarkOnSurfaceVariant)
                                    }
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = DangerRed)
                                },
                                onClick = {
                                    showPdfMenu = false
                                    val pdfFile = viewModel.exportMonthlyPdfReport(selectedYear, selectedMonth)
                                    ReportExportManager.shareFile(
                                        context,
                                        pdfFile,
                                        "application/pdf",
                                        "Monthly Expense Report PDF (${report.monthName})"
                                    )
                                }
                            )
                        }
                    }

                    // 2. Export CSV
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { showCsvMenu = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("monthly_report_export_csv_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, SophisticatedPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = SophisticatedPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "CSV",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = SophisticatedPrimary
                            )
                        }

                        DropdownMenu(
                            expanded = showCsvMenu,
                            onDismissRequest = { showCsvMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("Save CSV to Downloads", fontWeight = FontWeight.Bold)
                                        Text("UTF-8 comma-separated file for spreadsheets", style = MaterialTheme.typography.bodySmall, color = SophisticatedDarkOnSurfaceVariant)
                                    }
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Download, contentDescription = null, tint = SophisticatedPrimary)
                                },
                                onClick = {
                                    showCsvMenu = false
                                    val csvFile = viewModel.exportMonthlyCsvReport(selectedYear, selectedMonth)
                                    val (saved, msg) = ReportExportManager.saveToDownloads(
                                        context,
                                        csvFile,
                                        "text/csv"
                                    )
                                    android.widget.Toast.makeText(
                                        context,
                                        if (saved) "Saved CSV to Downloads: ${csvFile.name}" else msg,
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("Share CSV with Accountant", fontWeight = FontWeight.Bold)
                                        Text("Send data file for accounting software", style = MaterialTheme.typography.bodySmall, color = SophisticatedDarkOnSurfaceVariant)
                                    }
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = SophisticatedPrimary)
                                },
                                onClick = {
                                    showCsvMenu = false
                                    val csvFile = viewModel.exportMonthlyCsvReport(selectedYear, selectedMonth)
                                    ReportExportManager.shareFile(
                                        context,
                                        csvFile,
                                        "text/csv",
                                        "Monthly Expense Report CSV (${report.monthName})"
                                    )
                                }
                            )
                        }
                    }

                    // 3. Export Excel
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { showExcelMenu = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("monthly_report_export_excel_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, SuccessGreen)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TableChart,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Excel",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                        }

                        DropdownMenu(
                            expanded = showExcelMenu,
                            onDismissRequest = { showExcelMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("Save Excel to Downloads", fontWeight = FontWeight.Bold)
                                        Text("18-col ledger workbook with formatting", style = MaterialTheme.typography.bodySmall, color = SophisticatedDarkOnSurfaceVariant)
                                    }
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Download, contentDescription = null, tint = SuccessGreen)
                                },
                                onClick = {
                                    showExcelMenu = false
                                    val excelFile = viewModel.exportMonthlyExcelReport(selectedYear, selectedMonth)
                                    val (saved, msg) = ReportExportManager.saveToDownloads(
                                        context,
                                        excelFile,
                                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                    )
                                    android.widget.Toast.makeText(
                                        context,
                                        if (saved) "Saved Excel to Downloads: ${excelFile.name}" else msg,
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("Share Excel with Accountant", fontWeight = FontWeight.Bold)
                                        Text("Send formatted workbook via Email, WhatsApp", style = MaterialTheme.typography.bodySmall, color = SophisticatedDarkOnSurfaceVariant)
                                    }
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = SuccessGreen)
                                },
                                onClick = {
                                    showExcelMenu = false
                                    val excelFile = viewModel.exportMonthlyExcelReport(selectedYear, selectedMonth)
                                    ReportExportManager.shareFile(
                                        context,
                                        excelFile,
                                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                        "Monthly Expense Report (${report.monthName})"
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
