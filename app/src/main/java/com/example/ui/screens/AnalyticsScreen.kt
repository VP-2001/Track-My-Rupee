package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ExpenseEntity
import com.example.export.ReportExportManager
import com.example.ui.components.ChartColors
import com.example.ui.components.InteractiveAreaLineChart
import com.example.ui.components.InteractiveBarChart
import com.example.ui.components.InteractivePieDonutChart
import com.example.ui.components.InteractiveRadarChart
import com.example.ui.components.MonthlyComparisonView
import com.example.ui.components.MonthlySpendingHabitsBottomSheet
import com.example.ui.components.RechartsCategoryBudgetDashboard
import com.example.ui.viewmodel.DateFilterType
import com.example.ui.viewmodel.ExpenseViewModel

enum class ChartViewType(val label: String, val icon: ImageVector) {
    DONUT("Donut", Icons.Default.DonutLarge),
    PIE("Pie", Icons.Default.PieChart),
    BAR("Daily Bar", Icons.Default.BarChart),
    AREA("Trend Line", Icons.AutoMirrored.Filled.ShowChart),
    RADAR("Radar", Icons.Default.Radar)
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: ExpenseViewModel
) {
    val context = LocalContext.current
    val categorySummaries by viewModel.categorySummaries.collectAsStateWithLifecycle()
    val paymentSummaries by viewModel.paymentSummaries.collectAsStateWithLifecycle()
    val dailyTrend by viewModel.dailyTrend.collectAsStateWithLifecycle()
    val dateFilter by viewModel.dateFilter.collectAsStateWithLifecycle()
    val filteredExpenses by viewModel.filteredExpenses.collectAsStateWithLifecycle()
    val monthlySpendingHabits by viewModel.monthlySpendingHabits.collectAsStateWithLifecycle()
    val monthlyComparisonReport by viewModel.monthlyComparisonReport.collectAsStateWithLifecycle()
    val selectedComparisonView by viewModel.selectedComparisonView.collectAsStateWithLifecycle()
    val categoryBudgetReport by viewModel.categoryBudgetReport.collectAsStateWithLifecycle()
    val categoryMultiMonthTrends by viewModel.categoryMultiMonthTrends.collectAsStateWithLifecycle()
    val availableMonths = remember(filteredExpenses) { viewModel.getAvailableMonths() }

    var activeAnalyticsTab by remember { mutableIntStateOf(0) }
    var selectedChartView by remember { mutableStateOf(ChartViewType.DONUT) }
    var showHabitsSheet by remember { mutableStateOf(false) }
    val totalPeriodSpent = filteredExpenses.sumOf { it.amount }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("analytics_screen"),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // Header & Share
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Analytics & Reports",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Total: ${ExpenseEntity.formatInr(totalPeriodSpent)} (${filteredExpenses.size} transactions)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = com.example.ui.theme.SophisticatedPrimary.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.SophisticatedPrimary.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .clickable { showHabitsSheet = true }
                            .testTag("analytics_habits_summary_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Insights,
                                contentDescription = "Habits",
                                tint = com.example.ui.theme.SophisticatedPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                "Habits",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = com.example.ui.theme.SophisticatedPrimary
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val pdf = viewModel.exportPdfReport()
                            ReportExportManager.shareFile(context, pdf, "application/pdf", "Expense Report PDF")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PDF", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // Primary Analytics Tab Row: Overview & Charts vs Monthly Comparison
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = com.example.ui.theme.SophisticatedDarkSurfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.SophisticatedDarkBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        Triple("Overview", Icons.Default.PieChart, "tab_overview_charts"),
                        Triple("Category Budgets", Icons.Default.BarChart, "tab_category_budgets"),
                        Triple("Comparison", Icons.Default.CalendarMonth, "tab_monthly_comparison")
                    ).forEachIndexed { index, (title, icon, testTag) ->
                        val isSelected = activeAnalyticsTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) com.example.ui.theme.SophisticatedPrimary else Color.Transparent)
                                .clickable { activeAnalyticsTab = index }
                                .padding(vertical = 10.dp)
                                .testTag(testTag),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color(0xFF1E1035) else com.example.ui.theme.SophisticatedDarkOnSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color(0xFF1E1035) else com.example.ui.theme.SophisticatedDarkOnSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        if (activeAnalyticsTab == 1) {
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    RechartsCategoryBudgetDashboard(
                        report = categoryBudgetReport,
                        multiMonthTrends = categoryMultiMonthTrends,
                        availableMonths = availableMonths,
                        onSelectMonth = { y, m -> viewModel.setSelectedCategoryReportMonth(y, m) },
                        onNavigateToBudgets = null,
                        onFilterTransactionsByCategory = { cat -> viewModel.filterByCategory(cat) }
                    )
                }
            }
        } else if (activeAnalyticsTab == 2) {
            item {
                MonthlyComparisonView(
                    report = monthlyComparisonReport,
                    selectedViewMode = selectedComparisonView,
                    onSelectViewMode = { viewModel.setComparisonView(it) }
                )
            }
        } else {
            // Date Range Selector
            item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(DateFilterType.entries) { df ->
                    FilterChip(
                        selected = dateFilter == df,
                        onClick = { viewModel.setDateFilter(df) },
                        label = { Text(df.label) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Chart Type Selector Tabs
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ChartViewType.entries) { chartType ->
                    FilterChip(
                        selected = selectedChartView == chartType,
                        onClick = { selectedChartView = chartType },
                        leadingIcon = {
                            Icon(chartType.icon, contentDescription = chartType.label, modifier = Modifier.size(16.dp))
                        },
                        label = { Text(chartType.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Main Chart Canvas Container Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                color = com.example.ui.theme.SophisticatedDarkSurfaceDim,
                border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.SophisticatedDarkSurface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    when (selectedChartView) {
                        ChartViewType.DONUT -> {
                            Text(
                                text = "Category Distribution (Donut)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = com.example.ui.theme.SophisticatedDarkOnSurface
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            InteractivePieDonutChart(data = categorySummaries, isDonut = true)
                        }
                        ChartViewType.PIE -> {
                            Text(
                                text = "Category Proportion (Pie)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = com.example.ui.theme.SophisticatedDarkOnSurface
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            InteractivePieDonutChart(data = categorySummaries, isDonut = false)
                        }
                        ChartViewType.BAR -> {
                            Text(
                                text = "Daily Spending Activity",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = com.example.ui.theme.SophisticatedDarkOnSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            InteractiveBarChart(data = dailyTrend, isStackedByPayment = false)
                        }
                        ChartViewType.AREA -> {
                            Text(
                                text = "Cumulative Spending Trend",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = com.example.ui.theme.SophisticatedDarkOnSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            InteractiveAreaLineChart(data = dailyTrend)
                        }
                        ChartViewType.RADAR -> {
                            Text(
                                text = "Multi-Category Radar Balance",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = com.example.ui.theme.SophisticatedDarkOnSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            InteractiveRadarChart(categories = categorySummaries)
                        }
                    }
                }
            }
        }

        // Payment Method Breakdown Card
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                color = com.example.ui.theme.SophisticatedDarkSurfaceDim,
                border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.SophisticatedDarkSurface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Payment Method Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = com.example.ui.theme.SophisticatedDarkOnSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    paymentSummaries.forEach { item ->
                        val pct = if (totalPeriodSpent > 0) (item.totalAmount / totalPeriodSpent).toFloat() else 0f
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = item.paymentMethod,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = com.example.ui.theme.SophisticatedDarkOnSurface
                                )
                                Text(
                                    text = "${ExpenseEntity.formatInr(item.totalAmount)} (${(pct * 100).toInt()}%)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = com.example.ui.theme.SophisticatedPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { pct },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = com.example.ui.theme.SophisticatedPrimary,
                                trackColor = com.example.ui.theme.SophisticatedDarkSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Category Ranking List
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "Category Ranking",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = com.example.ui.theme.SophisticatedDarkOnSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        items(categorySummaries.withIndex().toList()) { (idx, item) ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                color = com.example.ui.theme.SophisticatedDarkSurfaceDim,
                border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.SophisticatedDarkSurface)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ChartColors[idx % ChartColors.size].copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "#${idx + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = ChartColors[idx % ChartColors.size]
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.category,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = com.example.ui.theme.SophisticatedDarkOnSurface
                        )
                        Text(
                            text = "${item.transactionCount} transactions",
                            style = MaterialTheme.typography.bodySmall,
                            color = com.example.ui.theme.SophisticatedDarkOnSurfaceVariant
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = ExpenseEntity.formatInr(item.totalAmount),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = com.example.ui.theme.SophisticatedDarkOnSurface
                        )
                        Text(
                            text = "${String.format(java.util.Locale.getDefault(), "%.1f", item.percentage)}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = com.example.ui.theme.SophisticatedPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

    if (showHabitsSheet) {
        MonthlySpendingHabitsBottomSheet(
            summary = monthlySpendingHabits,
            onDismiss = { showHabitsSheet = false },
            viewModel = viewModel
        )
    }
}
