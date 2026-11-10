package com.example.ui.components

import android.graphics.Paint as NativePaint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.StackedBarChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategorySummary
import com.example.data.model.ExpenseEntity
import com.example.export.ReportExportManager
import com.example.ui.theme.SophisticatedDarkBorder
import com.example.ui.theme.SophisticatedDarkOnSurface
import com.example.ui.theme.SophisticatedDarkOnSurfaceVariant
import com.example.ui.theme.SophisticatedDarkSurface
import com.example.ui.theme.SophisticatedDarkSurfaceDim
import com.example.ui.theme.SophisticatedDarkSurfaceVariant
import com.example.ui.theme.SophisticatedPrimary
import com.example.ui.theme.SophisticatedPrimaryContainer
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.AvailableMonth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Display modes for the D3/Recharts Spending Insights Dashboard
 */
enum class InsightsChartMode(val label: String, val description: String) {
    DONUT_PIE("Pie Chart", "D3 interactive radial distribution"),
    BAR_CHART("Bar Chart", "Recharts ranked horizontal & vertical bars"),
    DUAL_VIEW("Combined", "Simultaneous pie & bar breakdown")
}

/**
 * D3 / Recharts inspired Interactive Spending Insights Dashboard
 * Displays monthly expense breakdowns by category using interactive pie and bar charts.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RechartsSpendingInsightsDashboard(
    allExpenses: List<ExpenseEntity>,
    availableMonths: List<AvailableMonth> = emptyList(),
    modifier: Modifier = Modifier,
    onNavigateToTransactions: (() -> Unit)? = null,
    onFilterCategoryInLedger: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    // Current selected calendar for monthly breakdown
    val initialCal = remember { Calendar.getInstance() }
    var selectedYear by remember { mutableIntStateOf(initialCal.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(initialCal.get(Calendar.MONTH)) } // 0-based

    var selectedChartMode by remember { mutableStateOf(InsightsChartMode.DONUT_PIE) }
    var selectedCategoryIndex by remember { mutableIntStateOf(-1) }
    var isDonutVariant by remember { mutableStateOf(true) }
    var showMonthPickerDropdown by remember { mutableStateOf(false) }

    // Format current selected month label
    val selectedMonthLabel = remember(selectedYear, selectedMonth) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
    }

    // Filter expenses belonging strictly to the selected year and month
    val monthExpenses = remember(allExpenses, selectedYear, selectedMonth) {
        val startCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = (startCal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val startMillis = startCal.timeInMillis
        val endMillis = endCal.timeInMillis
        allExpenses.filter { it.date in startMillis..endMillis }
    }

    // Aggregate category summaries for the selected month
    val totalMonthSpent = remember(monthExpenses) { monthExpenses.sumOf { it.amount } }

    val categorySummaries = remember(monthExpenses, totalMonthSpent) {
        if (monthExpenses.isEmpty() || totalMonthSpent <= 0.0) {
            emptyList()
        } else {
            monthExpenses.groupBy { it.category }
                .map { (cat, list) ->
                    val total = list.sumOf { it.amount }
                    val pct = ((total / totalMonthSpent) * 100).toFloat()
                    CategorySummary(
                        category = cat,
                        totalAmount = total,
                        transactionCount = list.size,
                        percentage = pct
                    )
                }
                .sortedByDescending { it.totalAmount }
        }
    }

    // Keep selectedCategory in bounds when month changes
    val activeCategory = remember(categorySummaries, selectedCategoryIndex) {
        if (selectedCategoryIndex in categorySummaries.indices) {
            categorySummaries[selectedCategoryIndex]
        } else null
    }

    // Reset slice selection if out of bounds
    LaunchedEffect(categorySummaries) {
        if (selectedCategoryIndex >= categorySummaries.size) {
            selectedCategoryIndex = -1
        }
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = SophisticatedDarkSurfaceDim,
        border = BorderStroke(1.5.dp, SophisticatedDarkBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("spending_insights_dashboard")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ==========================================
            // 1. Dashboard Header & D3/Recharts Badge
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SophisticatedPrimary.copy(alpha = 0.15f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Insights,
                                contentDescription = null,
                                tint = SophisticatedPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Spending Insights",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SophisticatedDarkOnSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = SophisticatedPrimaryContainer
                            ) {
                                Text(
                                    text = "D3 • RECHARTS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "Interactive category breakdown & visual analytics",
                            style = MaterialTheme.typography.bodySmall,
                            color = SophisticatedDarkOnSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SophisticatedPrimary.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, SophisticatedPrimary.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .clickable {
                            val pdf = ReportExportManager.generateMonthlyFinancialSummaryWithChartsPdf(
                                context = context,
                                expenses = monthExpenses,
                                budgets = emptyList(),
                                year = selectedYear,
                                month = selectedMonth + 1
                            )
                            ReportExportManager.shareFile(
                                context = context,
                                file = pdf,
                                mimeType = "application/pdf",
                                title = "Monthly Financial Statement & Visual Charts"
                            )
                        }
                        .testTag("export_monthly_visual_pdf_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export Monthly PDF",
                            tint = SophisticatedPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "PDF",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SophisticatedPrimary
                        )
                    }
                }
            }

            // ==========================================
            // 2. Month Selector & Navigation Bar
            // ==========================================
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SophisticatedDarkSurface,
                border = BorderStroke(1.dp, SophisticatedDarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Month Button
                    IconButton(
                        onClick = {
                            val c = Calendar.getInstance().apply {
                                set(Calendar.YEAR, selectedYear)
                                set(Calendar.MONTH, selectedMonth)
                                add(Calendar.MONTH, -1)
                            }
                            selectedYear = c.get(Calendar.YEAR)
                            selectedMonth = c.get(Calendar.MONTH)
                            selectedCategoryIndex = -1
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("insights_month_prev_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Month",
                            tint = SophisticatedPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Month Picker Dropdown Anchor
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showMonthPickerDropdown = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("insights_month_picker_dropdown_btn"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = SophisticatedPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = selectedMonthLabel,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = SophisticatedDarkOnSurface
                            )
                        }

                        DropdownMenu(
                            expanded = showMonthPickerDropdown,
                            onDismissRequest = { showMonthPickerDropdown = false }
                        ) {
                            // Current Month Quick Option
                            val currentCal = Calendar.getInstance()
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Current Month (${SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(currentCal.time)})",
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                onClick = {
                                    selectedYear = currentCal.get(Calendar.YEAR)
                                    selectedMonth = currentCal.get(Calendar.MONTH)
                                    selectedCategoryIndex = -1
                                    showMonthPickerDropdown = false
                                }
                            )

                            // Previous Month Quick Option
                            val prevCal = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
                            DropdownMenuItem(
                                text = {
                                    Text("Previous Month (${SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(prevCal.time)})")
                                },
                                onClick = {
                                    selectedYear = prevCal.get(Calendar.YEAR)
                                    selectedMonth = prevCal.get(Calendar.MONTH)
                                    selectedCategoryIndex = -1
                                    showMonthPickerDropdown = false
                                }
                            )

                            // Historical available months from recorded transactions
                            if (availableMonths.isNotEmpty()) {
                                androidx.compose.material3.HorizontalDivider()
                                availableMonths.take(12).forEach { am ->
                                    val isSelected = am.year == selectedYear && am.month == (selectedMonth + 1)
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = am.label,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) SophisticatedPrimary else MaterialTheme.colorScheme.onSurface
                                                )
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = null,
                                                        tint = SophisticatedPrimary,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            selectedYear = am.year
                                            selectedMonth = am.month - 1
                                            selectedCategoryIndex = -1
                                            showMonthPickerDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Next Month Button
                    IconButton(
                        onClick = {
                            val c = Calendar.getInstance().apply {
                                set(Calendar.YEAR, selectedYear)
                                set(Calendar.MONTH, selectedMonth)
                                add(Calendar.MONTH, 1)
                            }
                            selectedYear = c.get(Calendar.YEAR)
                            selectedMonth = c.get(Calendar.MONTH)
                            selectedCategoryIndex = -1
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("insights_month_next_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Month",
                            tint = SophisticatedPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // ==========================================
            // 3. Quick Month Summary KPI Bar
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Monthly Total
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SophisticatedDarkSurfaceVariant,
                    modifier = Modifier.weight(1.2f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "MONTH TOTAL",
                            style = MaterialTheme.typography.labelSmall,
                            color = SophisticatedDarkOnSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = ExpenseEntity.formatInr(totalMonthSpent),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = SophisticatedPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Active Categories
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SophisticatedDarkSurfaceVariant,
                    modifier = Modifier.weight(0.9f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "CATEGORIES",
                            style = MaterialTheme.typography.labelSmall,
                            color = SophisticatedDarkOnSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "${categorySummaries.size} Active",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SophisticatedDarkOnSurface
                        )
                    }
                }

                // Top Category
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SophisticatedDarkSurfaceVariant,
                    modifier = Modifier.weight(1.1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "TOP SPEND",
                            style = MaterialTheme.typography.labelSmall,
                            color = SophisticatedDarkOnSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        val topCat = categorySummaries.firstOrNull()
                        Text(
                            text = if (topCat != null) "${topCat.category} (${topCat.percentage.toInt()}%)" else "None",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // ==========================================
            // 4. Chart Mode Selection Tabs (Pie / Bar / Dual)
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                InsightsChartMode.entries.forEach { mode ->
                    val isSelected = selectedChartMode == mode
                    val icon = when (mode) {
                        InsightsChartMode.DONUT_PIE -> Icons.Default.PieChart
                        InsightsChartMode.BAR_CHART -> Icons.Default.BarChart
                        InsightsChartMode.DUAL_VIEW -> Icons.Default.StackedBarChart
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedChartMode = mode },
                        leadingIcon = {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp))
                        },
                        label = {
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SophisticatedPrimary,
                            selectedLabelColor = Color.Black,
                            selectedLeadingIconColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("insights_tab_${mode.name.lowercase()}")
                    )
                }

                if (selectedChartMode == InsightsChartMode.DONUT_PIE || selectedChartMode == InsightsChartMode.DUAL_VIEW) {
                    Spacer(modifier = Modifier.weight(1f))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isDonutVariant = !isDonutVariant }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isDonutVariant) Icons.Default.DonutLarge else Icons.Default.PieChart,
                            contentDescription = "Toggle Donut / Pie",
                            tint = SophisticatedPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isDonutVariant) "Donut Hole" else "Solid Pie",
                            style = MaterialTheme.typography.labelSmall,
                            color = SophisticatedPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Empty state check
            if (categorySummaries.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SophisticatedDarkSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = SophisticatedDarkOnSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No expenses recorded for $selectedMonthLabel",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = SophisticatedDarkOnSurface
                        )
                        Text(
                            text = "Switch months using the navigation arrows above or add new expenses to visualize spending.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SophisticatedDarkOnSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // ==========================================
                // 5. Chart Render Canvas (Pie / Bar / Dual)
                // ==========================================
                when (selectedChartMode) {
                    InsightsChartMode.DONUT_PIE -> {
                        D3RechartsInteractivePie(
                            categories = categorySummaries,
                            totalAmount = totalMonthSpent,
                            selectedIndex = selectedCategoryIndex,
                            isDonut = isDonutVariant,
                            onSelectIndex = { selectedCategoryIndex = it }
                        )
                    }
                    InsightsChartMode.BAR_CHART -> {
                        RechartsInteractiveBarChart(
                            categories = categorySummaries,
                            selectedIndex = selectedCategoryIndex,
                            onSelectIndex = { selectedCategoryIndex = it }
                        )
                    }
                    InsightsChartMode.DUAL_VIEW -> {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            D3RechartsInteractivePie(
                                categories = categorySummaries,
                                totalAmount = totalMonthSpent,
                                selectedIndex = selectedCategoryIndex,
                                isDonut = isDonutVariant,
                                onSelectIndex = { selectedCategoryIndex = it }
                            )
                            RechartsInteractiveBarChart(
                                categories = categorySummaries,
                                selectedIndex = selectedCategoryIndex,
                                onSelectIndex = { selectedCategoryIndex = it }
                            )
                        }
                    }
                }

                // ==========================================
                // 6. Interactive Category Legend Chips
                // ==========================================
                Text(
                    text = "Tap category to inspect details:",
                    style = MaterialTheme.typography.labelSmall,
                    color = SophisticatedDarkOnSurfaceVariant
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categorySummaries.forEachIndexed { index, item ->
                        val isSelected = selectedCategoryIndex == index
                        val color = ChartColors[index % ChartColors.size]

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) color.copy(alpha = 0.25f) else SophisticatedDarkSurfaceVariant,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) color else SophisticatedDarkBorder
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedCategoryIndex = if (isSelected) -1 else index
                                }
                                .testTag("insights_legend_chip_${item.category.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) color else SophisticatedDarkOnSurface
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${item.percentage.toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = color
                                )
                            }
                        }
                    }
                }

                // ==========================================
                // 7. Selected Category Deep-Dive Focus Card
                // ==========================================
                if (activeCategory != null) {
                    val color = ChartColors[selectedCategoryIndex % ChartColors.size]
                    val catExpenses = remember(monthExpenses, activeCategory) {
                        monthExpenses.filter { it.category.equals(activeCategory.category, ignoreCase = true) }
                            .sortedByDescending { it.amount }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SophisticatedDarkSurface,
                        border = BorderStroke(1.5.dp, color.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("insights_category_detail_card")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = activeCategory.category,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = SophisticatedDarkOnSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = color.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "${activeCategory.percentage.toInt()}% OF MONTH",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = color,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                TextButton(
                                    onClick = { selectedCategoryIndex = -1 },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Clear", style = MaterialTheme.typography.labelSmall, color = SophisticatedDarkOnSurfaceVariant)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Total Spent", style = MaterialTheme.typography.labelSmall, color = SophisticatedDarkOnSurfaceVariant)
                                    Text(
                                        text = ExpenseEntity.formatInr(activeCategory.totalAmount),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = color
                                    )
                                }
                                Column {
                                    Text("Transactions", style = MaterialTheme.typography.labelSmall, color = SophisticatedDarkOnSurfaceVariant)
                                    Text(
                                        text = "${activeCategory.transactionCount} items",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = SophisticatedDarkOnSurface
                                    )
                                }
                                Column {
                                    Text("Avg / Item", style = MaterialTheme.typography.labelSmall, color = SophisticatedDarkOnSurfaceVariant)
                                    val avg = if (activeCategory.transactionCount > 0) activeCategory.totalAmount / activeCategory.transactionCount else 0.0
                                    Text(
                                        text = ExpenseEntity.formatInr(avg),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = SophisticatedDarkOnSurface
                                    )
                                }
                            }

                            // Top 3 Transactions list
                            if (catExpenses.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Top ${activeCategory.category} transactions in $selectedMonthLabel:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SophisticatedDarkOnSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                catExpenses.take(3).forEach { exp ->
                                    val dateStr = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(exp.date))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = exp.title,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                color = SophisticatedDarkOnSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = dateStr,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SophisticatedDarkOnSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = ExpenseEntity.formatInr(exp.amount),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = SophisticatedDarkOnSurface
                                        )
                                    }
                                }
                            }

                            if (onFilterCategoryInLedger != null || onNavigateToTransactions != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        onFilterCategoryInLedger?.invoke(activeCategory.category)
                                        onNavigateToTransactions?.invoke()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(38.dp)
                                        .testTag("insights_filter_ledger_btn"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = color.copy(alpha = 0.25f),
                                        contentColor = color
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Filter ${activeCategory.category} in Ledger", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Interactive D3-style Pie/Donut Chart with touch slice detection and tooltips
 */
@Composable
fun D3RechartsInteractivePie(
    categories: List<CategorySummary>,
    totalAmount: Double,
    selectedIndex: Int,
    isDonut: Boolean,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(categories) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    val selectedCategory = if (selectedIndex in categories.indices) categories[selectedIndex] else null

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .testTag("d3_recharts_pie_chart"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(categories) {
                    detectTapGestures { offset ->
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val dx = offset.x - centerX
                        val dy = offset.y - centerY
                        val touchRadius = kotlin.math.sqrt(dx * dx + dy * dy)
                        val maxRadius = kotlin.math.min(centerX, centerY) * 0.88f

                        if (touchRadius <= maxRadius) {
                            var touchAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            if (touchAngle < 0f) touchAngle += 360f

                            // Slices start at -90 degrees (top: 270)
                            val normalizedAngle = (touchAngle + 90f) % 360f

                            var currentAngle = 0f
                            var foundIndex = -1
                            for ((idx, item) in categories.withIndex()) {
                                val sweep = (item.percentage / 100f) * 360f
                                if (normalizedAngle >= currentAngle && normalizedAngle < (currentAngle + sweep)) {
                                    foundIndex = idx
                                    break
                                }
                                currentAngle += sweep
                            }

                            if (foundIndex != -1) {
                                onSelectIndex(if (selectedIndex == foundIndex) -1 else foundIndex)
                            } else {
                                onSelectIndex(-1)
                            }
                        } else {
                            onSelectIndex(-1)
                        }
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val centerX = width / 2f
            val centerY = height / 2f
            val baseRadius = kotlin.math.min(centerX, centerY) * 0.78f
            val donutHoleRadius = if (isDonut) baseRadius * 0.58f else 0f

            var startAngle = -90f

            categories.forEachIndexed { index, item ->
                val sweepAngle = (item.percentage / 100f) * 360f * animationProgress.value
                val isSelected = selectedIndex == index
                val color = ChartColors[index % ChartColors.size]

                // Radial pop-out displacement for selected slice
                val popOutDistance = if (isSelected) 10.dp.toPx() else 0f
                val midAngle = Math.toRadians((startAngle + sweepAngle / 2f).toDouble())
                val offsetX = (popOutDistance * cos(midAngle)).toFloat()
                val offsetY = (popOutDistance * sin(midAngle)).toFloat()

                val sliceRadius = if (isSelected) baseRadius + 6.dp.toPx() else baseRadius

                if (isDonut) {
                    val strokeWidth = sliceRadius - donutHoleRadius
                    val arcCenterRadius = donutHoleRadius + strokeWidth / 2f

                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle - if (categories.size > 1) 1.5f else 0f,
                        useCenter = false,
                        topLeft = Offset(centerX - arcCenterRadius + offsetX, centerY - arcCenterRadius + offsetY),
                        size = Size(arcCenterRadius * 2f, arcCenterRadius * 2f),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    if (isSelected) {
                        // High-contrast glow border for selected slice
                        drawArc(
                            color = Color.White,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle - if (categories.size > 1) 1.5f else 0f,
                            useCenter = false,
                            topLeft = Offset(centerX - arcCenterRadius + offsetX, centerY - arcCenterRadius + offsetY),
                            size = Size(arcCenterRadius * 2f, arcCenterRadius * 2f),
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                } else {
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle - if (categories.size > 1) 1f else 0f,
                        useCenter = true,
                        topLeft = Offset(centerX - sliceRadius + offsetX, centerY - sliceRadius + offsetY),
                        size = Size(sliceRadius * 2f, sliceRadius * 2f)
                    )

                    if (isSelected) {
                        drawArc(
                            color = Color.White,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle - if (categories.size > 1) 1f else 0f,
                            useCenter = true,
                            topLeft = Offset(centerX - sliceRadius + offsetX, centerY - sliceRadius + offsetY),
                            size = Size(sliceRadius * 2f, sliceRadius * 2f),
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                }

                startAngle += sweepAngle
            }
        }

        // Center Info Callout in Donut Hole
        if (isDonut) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                if (selectedCategory != null) {
                    val catColor = ChartColors[selectedIndex % ChartColors.size]
                    Text(
                        text = selectedCategory.category,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = catColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = ExpenseEntity.formatInr(selectedCategory.totalAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = SophisticatedDarkOnSurface
                    )
                    Text(
                        text = "${selectedCategory.percentage.toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = catColor
                    )
                } else {
                    Text(
                        text = "TOTAL SPENT",
                        style = MaterialTheme.typography.labelSmall,
                        color = SophisticatedDarkOnSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = ExpenseEntity.formatInr(totalAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = SophisticatedPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${categories.size} categories",
                        style = MaterialTheme.typography.labelSmall,
                        color = SophisticatedDarkOnSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Recharts-style Interactive Category Bar Chart with horizontal rank bars and value tooltips
 */
@Composable
fun RechartsInteractiveBarChart(
    categories: List<CategorySummary>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(categories) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing)
        )
    }

    val maxAmount = remember(categories) { categories.maxOfOrNull { it.totalAmount } ?: 1.0 }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("recharts_category_bar_chart"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEachIndexed { index, item ->
            val isSelected = selectedIndex == index
            val color = ChartColors[index % ChartColors.size]
            val barFraction = if (maxAmount > 0.0) {
                ((item.totalAmount / maxAmount).toFloat() * animationProgress.value).coerceIn(0.04f, 1f)
            } else 0.04f

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) color.copy(alpha = 0.15f) else SophisticatedDarkSurfaceVariant.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, if (isSelected) color else Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        onSelectIndex(if (isSelected) -1 else index)
                    }
                    .testTag("insights_bar_row_${item.category.lowercase()}")
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    // Category Name & Amount Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item.category,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isSelected) color else SophisticatedDarkOnSurface
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = ExpenseEntity.formatInr(item.totalAmount),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = color
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(${item.percentage.toInt()}%)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SophisticatedDarkOnSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Recharts Gradient Rounded Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(SophisticatedDarkSurfaceDim)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = barFraction)
                                .height(10.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(color.copy(alpha = 0.7f), color)
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}
