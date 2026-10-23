package com.example.ui.components

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ExpenseEntity
import com.example.export.ReportExportManager
import com.example.ui.theme.SophisticatedDarkBorder
import com.example.ui.theme.SophisticatedDarkOnSurface
import com.example.ui.theme.SophisticatedDarkOnSurfaceVariant
import com.example.ui.theme.SophisticatedDarkSurface
import com.example.ui.theme.SophisticatedDarkSurfaceDim
import com.example.ui.theme.SophisticatedDarkSurfaceVariant
import com.example.ui.theme.SophisticatedPrimary
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.AvailableMonth
import com.example.ui.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExportDateReportDialog(
    viewModel: ExpenseViewModel,
    initialTab: Int = 0, // 0 for Monthly, 1 for Custom Date Range
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(initialTab) }

    val availableMonths = remember(viewModel) { viewModel.getAvailableMonths() }
    val now = Calendar.getInstance()
    var selectedYear by remember { mutableIntStateOf(now.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(now.get(Calendar.MONTH) + 1) } // 1..12

    // Custom Date Range State: defaults to current month (1st of month to today)
    val calStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
    val calEnd = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
    }
    var customStartDate by remember { mutableLongStateOf(calStart.timeInMillis) }
    var customEndDate by remember { mutableLongStateOf(calEnd.timeInMillis) }

    val sdfDisplay = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val sdfMonthHeader = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SophisticatedDarkSurfaceDim,
            border = BorderStroke(1.dp, SophisticatedDarkBorder),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 20.dp)
                .testTag("export_date_report_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SuccessGreen.copy(alpha = 0.2f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.TableChart,
                                    contentDescription = "Excel Export",
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Detailed Excel Export (.xlsx)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SophisticatedDarkOnSurface
                            )
                            Text(
                                text = "All 18 transaction fields, KPIs & breakdown sheets",
                                style = MaterialTheme.typography.bodySmall,
                                color = SophisticatedDarkOnSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = SophisticatedDarkOnSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tab Switcher: Monthly vs Custom Date Range
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = SophisticatedDarkSurfaceVariant,
                    contentColor = SophisticatedPrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = SuccessGreen
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Monthly Report", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Custom Date Range", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ==========================================
                // TAB 0: MONTHLY REPORT
                // ==========================================
                if (selectedTab == 0) {
                    Text(
                        text = "1. Select Month to Export:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = SophisticatedDarkOnSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Available Months Chips
                    LazyRow(
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
                                        Text(m.shortLabel, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                        Text(
                                            text = if (m.count > 0) "${m.count} items" else "0 items",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) SuccessGreen else SophisticatedDarkOnSurfaceVariant
                                        )
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SuccessGreen.copy(alpha = 0.2f),
                                    selectedLabelColor = SuccessGreen
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Pick Other Month / Year Action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = {
                                val c = Calendar.getInstance().apply {
                                    set(selectedYear, selectedMonth - 1, 1)
                                }
                                DatePickerDialog(
                                    context,
                                    { _, year, month, _ ->
                                        selectedYear = year
                                        selectedMonth = month + 1
                                    },
                                    c.get(Calendar.YEAR),
                                    c.get(Calendar.MONTH),
                                    c.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(36.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pick Custom Month...", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Month Preview Summary Card
                    val curCal = Calendar.getInstance().apply { set(selectedYear, selectedMonth - 1, 1) }
                    val monthFormatted = sdfMonthHeader.format(curCal.time)
                    val monthData = availableMonths.firstOrNull { it.year == selectedYear && it.month == selectedMonth }
                    val txCount = monthData?.count ?: 0
                    val totalAmount = monthData?.totalSpent ?: 0.0

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SophisticatedDarkSurface,
                        border = BorderStroke(1.dp, SophisticatedDarkBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = monthFormatted.uppercase(Locale.getDefault()),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SuccessGreen
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SuccessGreen.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "$txCount Transactions",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SuccessGreen,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Total Spending: ${ExpenseEntity.formatInr(totalAmount)}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = SophisticatedDarkOnSurface
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Includes 6 comprehensive worksheets with complete details for Pivot Tables, Category Budgets, Daily Trends, Payment Modes & Tag Summaries.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SophisticatedDarkOnSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "1. Overview & KPIs",
                                    "2. Full 18-Col Ledger",
                                    "3. Category Budgets",
                                    "4. Daily Trends",
                                    "5. Payment Methods",
                                    "6. Tag Summaries"
                                ).forEach { sheet ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = SophisticatedDarkSurfaceVariant
                                    ) {
                                        Text(
                                            text = sheet,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SophisticatedDarkOnSurface,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Action Buttons: Download & Share
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val file = viewModel.exportMonthlyExcelReport(selectedYear, selectedMonth)
                                val (saved, msg) = ReportExportManager.saveToDownloads(
                                    context,
                                    file,
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                )
                                Toast.makeText(context, if (saved) "Saved to Downloads: ${file.name}" else msg, Toast.LENGTH_LONG).show()
                                onDismissRequest()
                            },
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp)
                                .testTag("download_monthly_excel_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SuccessGreen,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Download Excel", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Download (.xlsx)", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val file = viewModel.exportMonthlyExcelReport(selectedYear, selectedMonth)
                                ReportExportManager.shareFile(
                                    context,
                                    file,
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                    "Monthly Expense Report - $monthFormatted"
                                )
                                onDismissRequest()
                            },
                            modifier = Modifier
                                .weight(0.9f)
                                .height(48.dp)
                                .testTag("share_monthly_excel_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share Excel", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share")
                        }
                    }
                }

                // ==========================================
                // TAB 1: CUSTOM DATE RANGE
                // ==========================================
                if (selectedTab == 1) {
                    Text(
                        text = "1. Quick Presets:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = SophisticatedDarkOnSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = false,
                                onClick = {
                                    val c = Calendar.getInstance()
                                    customEndDate = c.timeInMillis
                                    c.add(Calendar.DAY_OF_YEAR, -7)
                                    customStartDate = c.timeInMillis
                                },
                                label = { Text("Last 7 Days") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = false,
                                onClick = {
                                    val c = Calendar.getInstance()
                                    customEndDate = c.timeInMillis
                                    c.add(Calendar.DAY_OF_YEAR, -30)
                                    customStartDate = c.timeInMillis
                                },
                                label = { Text("Last 30 Days") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = false,
                                onClick = {
                                    val c = Calendar.getInstance()
                                    customEndDate = c.timeInMillis
                                    c.set(Calendar.DAY_OF_MONTH, 1)
                                    customStartDate = c.timeInMillis
                                },
                                label = { Text("This Month") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = false,
                                onClick = {
                                    val c = Calendar.getInstance()
                                    c.add(Calendar.MONTH, -1)
                                    c.set(Calendar.DAY_OF_MONTH, 1)
                                    customStartDate = c.timeInMillis
                                    c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH))
                                    customEndDate = c.timeInMillis
                                },
                                label = { Text("Last Month") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = false,
                                onClick = {
                                    val c = Calendar.getInstance()
                                    customEndDate = c.timeInMillis
                                    c.set(Calendar.DAY_OF_YEAR, 1)
                                    customStartDate = c.timeInMillis
                                },
                                label = { Text("This Year") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "2. Select Specific Date Range:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = SophisticatedDarkOnSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // From Date Selector Card
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SophisticatedDarkSurface,
                            border = BorderStroke(1.dp, SophisticatedDarkBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val c = Calendar.getInstance().apply { timeInMillis = customStartDate }
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            val newC = Calendar.getInstance().apply {
                                                set(year, month, dayOfMonth, 0, 0, 0)
                                                set(Calendar.MILLISECOND, 0)
                                            }
                                            customStartDate = newC.timeInMillis
                                        },
                                        c.get(Calendar.YEAR),
                                        c.get(Calendar.MONTH),
                                        c.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                                .testTag("pick_custom_start_date")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "FROM DATE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SophisticatedDarkOnSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = SophisticatedPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = sdfDisplay.format(Date(customStartDate)),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SophisticatedDarkOnSurface
                                    )
                                }
                            }
                        }

                        // To Date Selector Card
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SophisticatedDarkSurface,
                            border = BorderStroke(1.dp, SophisticatedDarkBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val c = Calendar.getInstance().apply { timeInMillis = customEndDate }
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            val newC = Calendar.getInstance().apply {
                                                set(year, month, dayOfMonth, 23, 59, 59)
                                                set(Calendar.MILLISECOND, 999)
                                            }
                                            customEndDate = newC.timeInMillis
                                        },
                                        c.get(Calendar.YEAR),
                                        c.get(Calendar.MONTH),
                                        c.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                                .testTag("pick_custom_end_date")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "TO DATE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SophisticatedDarkOnSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = SophisticatedPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = sdfDisplay.format(Date(customEndDate)),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SophisticatedDarkOnSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Real-time Range Stats Preview Card
                    val (rangeCount, rangeTotal) = remember(customStartDate, customEndDate, viewModel) {
                        viewModel.getCustomDateRangeStats(customStartDate, customEndDate)
                    }
                    val daysDiff = ((maxOf(customStartDate, customEndDate) - minOf(customStartDate, customEndDate)) / 86400000L) + 1

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SophisticatedDarkSurface,
                        border = BorderStroke(1.dp, SophisticatedDarkBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${sdfDisplay.format(Date(minOf(customStartDate, customEndDate)))} — ${sdfDisplay.format(Date(maxOf(customStartDate, customEndDate)))}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SuccessGreen
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SuccessGreen.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "$daysDiff Days",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SuccessGreen,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Period Spend: ${ExpenseEntity.formatInr(rangeTotal)}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = SophisticatedDarkOnSurface
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "$rangeCount transactions found in selected window. Full details include Payment Modes, Bank Names, Credit Cards, Tags & Notes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SophisticatedDarkOnSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "1. Period KPIs",
                                    "2. Detailed Ledger",
                                    "3. Categories",
                                    "4. Daily Spend",
                                    "5. Payment Methods",
                                    "6. Purpose Tags"
                                ).forEach { sheet ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = SophisticatedDarkSurfaceVariant
                                    ) {
                                        Text(
                                            text = sheet,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SophisticatedDarkOnSurface,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Action Buttons: Download & Share
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val file = viewModel.exportCustomDateExcelReport(customStartDate, customEndDate)
                                val (saved, msg) = ReportExportManager.saveToDownloads(
                                    context,
                                    file,
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                )
                                Toast.makeText(context, if (saved) "Saved to Downloads: ${file.name}" else msg, Toast.LENGTH_LONG).show()
                                onDismissRequest()
                            },
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp)
                                .testTag("download_custom_excel_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SuccessGreen,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Download Excel", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Download (.xlsx)", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val file = viewModel.exportCustomDateExcelReport(customStartDate, customEndDate)
                                val startStr = sdfDisplay.format(Date(minOf(customStartDate, customEndDate)))
                                val endStr = sdfDisplay.format(Date(maxOf(customStartDate, customEndDate)))
                                ReportExportManager.shareFile(
                                    context,
                                    file,
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                    "Custom Expense Report ($startStr to $endStr)"
                                )
                                onDismissRequest()
                            },
                            modifier = Modifier
                                .weight(0.9f)
                                .height(48.dp)
                                .testTag("share_custom_excel_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share Excel", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share")
                        }
                    }
                }
            }
        }
    }
}
