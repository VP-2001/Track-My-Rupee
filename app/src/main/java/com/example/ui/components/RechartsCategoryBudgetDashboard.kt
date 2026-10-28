package com.example.ui.components

import android.graphics.Paint as NativePaint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.StackedBarChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.viewmodel.AvailableMonth
import com.example.data.model.CategoryBudgetComparisonItem
import com.example.data.model.CategoryMultiMonthTrendPoint
import com.example.data.model.ExpenseEntity
import com.example.data.model.MonthlyCategoryBudgetReport
import java.util.Locale

enum class CategoryChartMode(val label: String) {
    GROUPED_BARS("Grouped Bars"),
    TREND_LINES("6M Trends"),
    VARIANCE("Variance")
}

enum class CategoryHealthFilter(val label: String) {
    ALL("All"),
    OVER_BUDGET("Over Limit"),
    UNDER_BUDGET("Safe")
}

@Composable
fun RechartsCategoryBudgetDashboard(
    report: MonthlyCategoryBudgetReport,
    multiMonthTrends: List<CategoryMultiMonthTrendPoint>,
    availableMonths: List<AvailableMonth> = emptyList(),
    onSelectMonth: (year: Int, month: Int) -> Unit = { _, _ -> },
    onNavigateToBudgets: (() -> Unit)? = null,
    onFilterTransactionsByCategory: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var chartMode by remember { mutableStateOf(CategoryChartMode.GROUPED_BARS) }
    var healthFilter by remember { mutableStateOf(CategoryHealthFilter.ALL) }
    var isExpandedOpen by remember { mutableStateOf(false) }

    // Filter items according to health filter
    val filteredItems = remember(report.items, healthFilter) {
        when (healthFilter) {
            CategoryHealthFilter.ALL -> report.items
            CategoryHealthFilter.OVER_BUDGET -> report.items.filter { it.isOverBudget }
            CategoryHealthFilter.UNDER_BUDGET -> report.items.filter { !it.isOverBudget }
        }
    }

    var activeCategoryIndex by remember(filteredItems) { mutableIntStateOf(0) }
    val safeActiveIndex = activeCategoryIndex.coerceIn(0, (filteredItems.size - 1).coerceAtLeast(0))
    val activeItem = filteredItems.getOrNull(safeActiveIndex) ?: filteredItems.firstOrNull()

    // Chart Palette (Recharts vibrant signature design)
    val spendTeal = Color(0xFF0D9488)
    val budgetAmber = Color(0xFFF59E0B)
    val overBudgetRed = Color(0xFFEF4444)
    val underBudgetGreen = Color(0xFF10B981)
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("recharts_category_budget_dashboard_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 1. Header with Month Label, Horizon Switcher, and Fullscreen Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = spendTeal.copy(alpha = 0.15f),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.BarChart,
                                    contentDescription = "Category Budget Dashboard",
                                    tint = spendTeal,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Category Budget Comparison",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = onSurface
                        )
                    }
                    Text(
                        text = "${report.monthName} • Grouped by Category",
                        style = MaterialTheme.typography.bodySmall,
                        color = onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Fullscreen Button
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .size(32.dp)
                            .clickable { isExpandedOpen = true }
                            .testTag("expand_category_budget_dashboard")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "Expand Category Dashboard",
                                tint = spendTeal,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Available Month Picker Bar (Scrollable chips)
            if (availableMonths.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_dashboard_month_picker"),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(availableMonths) { m ->
                        val isCurrent = m.year == report.year && m.month == report.month
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isCurrent) spendTeal else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                if (isCurrent) spendTeal else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .clickable { onSelectMonth(m.year, m.month) }
                                .testTag("select_month_${m.year}_${m.month}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = if (isCurrent) Color.Black else onSurfaceVariant,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = m.shortLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isCurrent) Color.Black else onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 3. Visualization Mode Toggle & Health Filter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mode Toggle: Grouped Bars vs Multi Trends vs Variance
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Row(modifier = Modifier.padding(2.dp)) {
                        CategoryChartMode.values().forEach { mode ->
                            val isSelected = chartMode == mode
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) spendTeal else Color.Transparent)
                                    .clickable { chartMode = mode }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("cat_chart_mode_${mode.name.lowercase()}")
                            ) {
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.Black else onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Health Filter Pills: All vs Over Limit
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Row(modifier = Modifier.padding(2.dp)) {
                        CategoryHealthFilter.values().forEach { filter ->
                            val isSelected = healthFilter == filter
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) (if (filter == CategoryHealthFilter.OVER_BUDGET) overBudgetRed else spendTeal) else Color.Transparent)
                                    .clickable { healthFilter = filter }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                                    .testTag("cat_health_filter_${filter.name.lowercase()}")
                            ) {
                                Text(
                                    text = filter.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. KPI Performance Summary Cards: Adherence, Over-Budget Breaches & Total Spend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Adherence Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Adherence",
                            style = MaterialTheme.typography.labelSmall,
                            color = onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${report.safeBudgetCount}/${report.items.size} Safe",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (report.overBudgetCount == 0) underBudgetGreen else spendTeal
                        )
                        Text(
                            text = "${String.format(Locale.getDefault(), "%.0f", report.adherenceRatePercent)}% adherence",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = onSurfaceVariant
                        )
                    }
                }

                // Over-Budget Alert Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (report.overBudgetCount > 0) overBudgetRed.copy(alpha = 0.12f) else underBudgetGreen.copy(alpha = 0.1f),
                    border = BorderStroke(
                        1.dp,
                        if (report.overBudgetCount > 0) overBudgetRed.copy(alpha = 0.35f) else underBudgetGreen.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.weight(1.1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = if (report.overBudgetCount > 0) "Over Limit" else "Status",
                            style = MaterialTheme.typography.labelSmall,
                            color = onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (report.overBudgetCount > 0) "${report.overBudgetCount} Categories!" else "All Safe ✓",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (report.overBudgetCount > 0) overBudgetRed else underBudgetGreen
                        )
                        Text(
                            text = if (report.totalVariance >= 0) "+${ExpenseEntity.formatInr(report.totalVariance)} Saved" else "-${ExpenseEntity.formatInr(kotlin.math.abs(report.totalVariance))} Over",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = if (report.totalVariance >= 0) underBudgetGreen else overBudgetRed
                        )
                    }
                }

                // Total Spend vs Budget Limit
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.weight(1.1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Total Spent",
                            style = MaterialTheme.typography.labelSmall,
                            color = onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = ExpenseEntity.formatInr(report.totalSpent),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = onSurface
                        )
                        Text(
                            text = "Limit: ${ExpenseEntity.formatInr(report.totalBudget)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            color = budgetAmber,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Interactive Recharts Category Tooltip Card for Active Category
            if (activeItem != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (activeItem.isOverBudget) overBudgetRed.copy(alpha = 0.6f) else spendTeal.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_budget_interactive_tooltip")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
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
                                        .background(if (activeItem.isOverBudget) overBudgetRed else spendTeal)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = activeItem.category,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (activeItem.isOverBudget) overBudgetRed.copy(alpha = 0.15f) else underBudgetGreen.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (activeItem.isOverBudget) "OVER BUDGET" else "WITHIN BUDGET",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (activeItem.isOverBudget) overBudgetRed else underBudgetGreen,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = "${String.format(Locale.getDefault(), "%.0f", activeItem.percentageUsed)}% used",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (activeItem.isOverBudget) overBudgetRed else spendTeal
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Spent: ${ExpenseEntity.formatInr(activeItem.spentAmount)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = onSurface
                                )
                                Text(
                                    text = "Budget Limit: ${ExpenseEntity.formatInr(activeItem.budgetLimit)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = budgetAmber
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                val varAmt = kotlin.math.abs(activeItem.variance)
                                Text(
                                    text = if (activeItem.variance >= 0) "Headroom: ${ExpenseEntity.formatInr(varAmt)}" else "Over by: ${ExpenseEntity.formatInr(varAmt)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeItem.variance >= 0) underBudgetGreen else overBudgetRed
                                )
                                Text(
                                    text = "${activeItem.transactionCount} transactions",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = onSurfaceVariant
                                )
                            }
                        }

                        // Shortcuts row
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (onFilterTransactionsByCategory != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = spendTeal.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, spendTeal.copy(alpha = 0.35f)),
                                    modifier = Modifier
                                        .clickable { onFilterTransactionsByCategory(activeItem.category) }
                                        .testTag("tooltip_view_category_txs_button")
                                ) {
                                    Text(
                                        text = "View ${activeItem.category} Txs",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = spendTeal,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            if (onNavigateToBudgets != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = budgetAmber.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, budgetAmber.copy(alpha = 0.35f)),
                                    modifier = Modifier
                                        .clickable { onNavigateToBudgets() }
                                        .testTag("tooltip_adjust_budget_button")
                                ) {
                                    Text(
                                        text = "Adjust Budget",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = budgetAmber,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 6. Native Recharts Interactive Category Canvas
            when (chartMode) {
                CategoryChartMode.GROUPED_BARS -> {
                    RechartsCategoryBarCanvas(
                        items = filteredItems,
                        activeIndex = safeActiveIndex,
                        onIndexChanged = { activeCategoryIndex = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .testTag("recharts_category_grouped_bar_canvas")
                    )
                }
                CategoryChartMode.TREND_LINES -> {
                    RechartsCategoryMultiTrendCanvas(
                        multiMonthTrends = multiMonthTrends,
                        selectedCategory = activeItem?.category,
                        onSelectCategory = { catName ->
                            val idx = filteredItems.indexOfFirst { it.category.equals(catName, ignoreCase = true) }
                            if (idx >= 0) activeCategoryIndex = idx
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .testTag("recharts_category_multi_trend_canvas")
                    )
                }
                CategoryChartMode.VARIANCE -> {
                    RechartsCategoryVarianceCanvas(
                        items = filteredItems,
                        activeIndex = safeActiveIndex,
                        onIndexChanged = { activeCategoryIndex = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .testTag("recharts_category_variance_canvas")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 7. Category Quick-Select Pills Bar
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("category_dashboard_pills_row"),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(filteredItems) { idx, item ->
                    val isSelected = idx == safeActiveIndex
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            isSelected && item.isOverBudget -> overBudgetRed
                            isSelected -> spendTeal
                            item.isOverBudget -> overBudgetRed.copy(alpha = 0.15f)
                            else -> MaterialTheme.colorScheme.surface
                        },
                        border = BorderStroke(
                            1.dp,
                            when {
                                isSelected -> Color.Transparent
                                item.isOverBudget -> overBudgetRed.copy(alpha = 0.5f)
                                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            }
                        ),
                        modifier = Modifier
                            .clickable { activeCategoryIndex = idx }
                            .testTag("cat_pill_${item.category.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (item.isOverBudget) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color.White else overBudgetRed)
                                )
                            }
                            Text(
                                text = item.category,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) (if (item.isOverBudget) Color.White else Color.Black) else onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Fullscreen Detailed Deep-Dive Dialog
    if (isExpandedOpen) {
        Dialog(
            onDismissRequest = { isExpandedOpen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Category Budget & Spending Trends",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = onSurface
                            )
                            Text(
                                text = "${report.monthName} • Recharts Category Analysis",
                                style = MaterialTheme.typography.bodySmall,
                                color = onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { isExpandedOpen = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = onSurface)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    RechartsCategoryBarCanvas(
                        items = filteredItems,
                        activeIndex = safeActiveIndex,
                        onIndexChanged = { activeCategoryIndex = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "All Category Benchmarks",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredItems) { item ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = BorderStroke(
                                    1.dp,
                                    if (item.isOverBudget) overBudgetRed.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.width(170.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = item.category,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = ExpenseEntity.formatInr(item.spentAmount),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.isOverBudget) overBudgetRed else spendTeal
                                    )
                                    Text(
                                        text = "Limit: ${ExpenseEntity.formatInr(item.budgetLimit)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = budgetAmber
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${String.format(Locale.getDefault(), "%.0f", item.percentageUsed)}% used • ${if (item.isOverBudget) "OVER" else "SAFE"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.isOverBudget) overBudgetRed else underBudgetGreen
                                    )
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
 * Recharts Grouped Bar Canvas:
 * - X-Axis represents Categories.
 * - Each category has two bars side-by-side: Actual Spend (Teal/Red) vs Budget Limit (Amber Outline).
 * - Percentage label floating on top of each category bar.
 * - Cartesian grid lines and Y-axis tick currency values.
 */
@Composable
fun RechartsCategoryBarCanvas(
    items: List<CategoryBudgetComparisonItem>,
    activeIndex: Int,
    onIndexChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val progress = remember { Animatable(0f) }
    LaunchedEffect(items) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(700, easing = FastOutSlowInEasing))
    }

    val spendTeal = Color(0xFF0D9488)
    val budgetAmber = Color(0xFFF59E0B)
    val overBudgetRed = Color(0xFFEF4444)
    val onSurface = MaterialTheme.colorScheme.onSurface

    val maxVal = (items.maxOfOrNull { maxOf(it.spentAmount, it.budgetLimit) } ?: 20000.0).coerceAtLeast(1000.0)
    val effectiveMax = (maxVal * 1.15).coerceAtLeast(100.0)

    Canvas(
        modifier = modifier
            .pointerInput(items) {
                detectTapGestures { offset ->
                    val count = items.size
                    val stepX = size.width / count.coerceAtLeast(1)
                    val tappedIdx = (offset.x / stepX).toInt().coerceIn(0, count - 1)
                    onIndexChanged(tappedIdx)
                }
            }
            .pointerInput(items) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val count = items.size
                    val stepX = size.width / count.coerceAtLeast(1)
                    val draggedIdx = (change.position.x / stepX).toInt().coerceIn(0, count - 1)
                    onIndexChanged(draggedIdx)
                }
            }
    ) {
        val count = items.size
        if (count < 1) return@Canvas

        val chartPaddingTop = 24.dp.toPx()
        val chartPaddingBottom = 32.dp.toPx()
        val chartPaddingStart = 20.dp.toPx()
        val chartPaddingEnd = 20.dp.toPx()

        val plotWidth = size.width - chartPaddingStart - chartPaddingEnd
        val plotHeight = size.height - chartPaddingTop - chartPaddingBottom

        // 1. Cartesian Grid: Horizontal dashed lines & Currency Y-labels
        val gridLines = 4
        val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
        val gridPaint = NativePaint().apply {
            color = android.graphics.Color.GRAY
            textSize = 20f
            alpha = 140
            isAntiAlias = true
        }

        for (i in 0..gridLines) {
            val ratio = i.toFloat() / gridLines
            val gridY = chartPaddingTop + (plotHeight * ratio)
            val gridVal = effectiveMax * (1f - ratio)

            drawLine(
                color = onSurface.copy(alpha = 0.08f),
                start = Offset(chartPaddingStart, gridY),
                end = Offset(size.width - chartPaddingEnd, gridY),
                strokeWidth = 1.dp.toPx(),
                pathEffect = dashPathEffect
            )

            val formattedVal = if (gridVal >= 1000) "₹${(gridVal / 1000).toInt()}k" else "₹${gridVal.toInt()}"
            drawContext.canvas.nativeCanvas.drawText(
                formattedVal,
                chartPaddingStart,
                gridY - 4f,
                gridPaint
            )
        }

        // 2. Render Grouped Bars: Spent vs Budget per Category
        val groupWidth = plotWidth / count
        val barWidth = (groupWidth * 0.32f).coerceAtMost(20.dp.toPx())
        val barSpacing = 4.dp.toPx()

        val labelPaint = NativePaint().apply {
            color = android.graphics.Color.LTGRAY
            textSize = if (count > 6) 18f else 22f
            textAlign = NativePaint.Align.CENTER
            isAntiAlias = true
        }
        val activeLabelPaint = NativePaint().apply {
            color = android.graphics.Color.WHITE
            textSize = if (count > 6) 20f else 24f
            isFakeBoldText = true
            textAlign = NativePaint.Align.CENTER
            isAntiAlias = true
        }
        val percentPaint = NativePaint().apply {
            textSize = 17f
            textAlign = NativePaint.Align.CENTER
            isAntiAlias = true
        }

        items.forEachIndexed { idx, item ->
            val isSelected = idx == activeIndex
            val groupCenterX = chartPaddingStart + (idx * groupWidth) + (groupWidth / 2f)

            val spendHeight = ((item.spentAmount / effectiveMax).toFloat() * plotHeight * progress.value).coerceAtLeast(4f)
            val budgetHeight = ((item.budgetLimit / effectiveMax).toFloat() * plotHeight * progress.value).coerceAtLeast(4f)

            val spendLeft = groupCenterX - barWidth - (barSpacing / 2f)
            val spendTop = chartPaddingTop + plotHeight - spendHeight

            val budgetLeft = groupCenterX + (barSpacing / 2f)
            val budgetTop = chartPaddingTop + plotHeight - budgetHeight

            val barColor = if (item.isOverBudget) overBudgetRed else spendTeal

            // Active Background Halo
            if (isSelected) {
                drawRoundRect(
                    color = onSurface.copy(alpha = 0.08f),
                    topLeft = Offset(spendLeft - 4.dp.toPx(), chartPaddingTop),
                    size = Size((barWidth * 2) + barSpacing + 8.dp.toPx(), plotHeight),
                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                )
            }

            // Spent Bar
            drawRoundRect(
                color = if (isSelected) barColor else barColor.copy(alpha = 0.85f),
                topLeft = Offset(spendLeft, spendTop),
                size = Size(barWidth, spendHeight),
                cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
            )

            // Budget Limit Bar (Amber fill/outline)
            drawRoundRect(
                color = budgetAmber.copy(alpha = if (isSelected) 0.85f else 0.5f),
                topLeft = Offset(budgetLeft, budgetTop),
                size = Size(barWidth, budgetHeight),
                cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
            )

            // Percentage text on top of spend bar
            percentPaint.color = if (item.isOverBudget) android.graphics.Color.rgb(239, 68, 68) else android.graphics.Color.rgb(13, 148, 136)
            val pctStr = "${item.percentageUsed.toInt()}%"
            drawContext.canvas.nativeCanvas.drawText(
                pctStr,
                spendLeft + (barWidth / 2f),
                spendTop - 4f,
                percentPaint
            )

            // Category Label below axis
            val labelY = chartPaddingTop + plotHeight + 22.dp.toPx()
            val shortCat = if (item.category.length > 7) item.category.take(6) + ".." else item.category
            drawContext.canvas.nativeCanvas.drawText(
                shortCat,
                groupCenterX,
                labelY,
                if (isSelected) activeLabelPaint else labelPaint
            )
        }
    }
}

/**
 * Multi-Month Category Trend Stream:
 * Renders continuous lines for categories across months, highlighting category budget reference line.
 */
@Composable
fun RechartsCategoryMultiTrendCanvas(
    multiMonthTrends: List<CategoryMultiMonthTrendPoint>,
    selectedCategory: String?,
    onSelectCategory: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (multiMonthTrends.isEmpty()) return

    val progress = remember { Animatable(0f) }
    LaunchedEffect(multiMonthTrends, selectedCategory) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(700, easing = FastOutSlowInEasing))
    }

    val spendTeal = Color(0xFF0D9488)
    val budgetAmber = Color(0xFFF59E0B)
    val onSurface = MaterialTheme.colorScheme.onSurface

    val targetCat = selectedCategory ?: multiMonthTrends.first().categorySpends.keys.firstOrNull() ?: "Food"

    // Extract trend points for this category
    val catPoints = multiMonthTrends.map { pt ->
        val spend = pt.categorySpends[targetCat] ?: 0.0
        val budget = pt.categoryBudgets[targetCat] ?: 6000.0
        Triple(pt.shortMonth, spend, budget)
    }

    val maxVal = (catPoints.maxOfOrNull { maxOf(it.second, it.third) } ?: 10000.0).coerceAtLeast(1000.0)
    val effectiveMax = (maxVal * 1.15).coerceAtLeast(100.0)

    Canvas(modifier = modifier) {
        val count = catPoints.size
        if (count < 2) return@Canvas

        val chartPaddingTop = 24.dp.toPx()
        val chartPaddingBottom = 32.dp.toPx()
        val chartPaddingStart = 20.dp.toPx()
        val chartPaddingEnd = 20.dp.toPx()

        val plotWidth = size.width - chartPaddingStart - chartPaddingEnd
        val plotHeight = size.height - chartPaddingTop - chartPaddingBottom
        val stepX = plotWidth / (count - 1)

        // Cartesian Grid
        val gridLines = 4
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
        for (i in 0..gridLines) {
            val ratio = i.toFloat() / gridLines
            val gridY = chartPaddingTop + (plotHeight * ratio)
            drawLine(
                color = onSurface.copy(alpha = 0.08f),
                start = Offset(chartPaddingStart, gridY),
                end = Offset(size.width - chartPaddingEnd, gridY),
                strokeWidth = 1.dp.toPx(),
                pathEffect = dashEffect
            )
        }

        // Budget Reference Line for this category
        val budgetVal = catPoints.first().third
        val budgetY = chartPaddingTop + plotHeight - ((budgetVal / effectiveMax).toFloat() * plotHeight * progress.value)
        if (budgetY in chartPaddingTop..(chartPaddingTop + plotHeight)) {
            drawLine(
                color = budgetAmber.copy(alpha = 0.85f),
                start = Offset(chartPaddingStart, budgetY),
                end = Offset(size.width - chartPaddingEnd, budgetY),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f), 0f)
            )

            val budgetPaint = NativePaint().apply {
                color = android.graphics.Color.rgb(245, 158, 11)
                textSize = 20f
                isFakeBoldText = true
                isAntiAlias = true
            }
            drawContext.canvas.nativeCanvas.drawText(
                "$targetCat Limit",
                size.width - chartPaddingEnd - 130f,
                budgetY - 6f,
                budgetPaint
            )
        }

        // Spending Curve
        val points = catPoints.mapIndexed { idx, item ->
            val x = chartPaddingStart + (idx * stepX)
            val y = chartPaddingTop + plotHeight - ((item.second / effectiveMax).toFloat() * plotHeight * progress.value)
            Offset(x, y)
        }

        val path = Path()
        val fillPath = Path()
        path.moveTo(points.first().x, points.first().y)
        fillPath.moveTo(points.first().x, chartPaddingTop + plotHeight)
        fillPath.lineTo(points.first().x, points.first().y)

        for (i in 0 until points.size - 1) {
            val p0 = points[i]
            val p1 = points[i + 1]
            val cx = (p0.x + p1.x) / 2f
            path.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
            fillPath.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
        }

        fillPath.lineTo(points.last().x, chartPaddingTop + plotHeight)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(spendTeal.copy(alpha = 0.35f), Color.Transparent),
                startY = chartPaddingTop,
                endY = chartPaddingTop + plotHeight
            )
        )

        drawPath(
            path = path,
            color = spendTeal,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Dots & month labels
        val textPaint = NativePaint().apply {
            color = android.graphics.Color.LTGRAY
            textSize = 22f
            textAlign = NativePaint.Align.CENTER
            isAntiAlias = true
        }

        points.forEachIndexed { idx, pt ->
            val isOver = catPoints[idx].second > catPoints[idx].third
            val dotColor = if (isOver) Color(0xFFEF4444) else spendTeal

            drawCircle(color = dotColor, radius = 5.dp.toPx(), center = pt)
            drawCircle(color = Color.White, radius = 2.dp.toPx(), center = pt)

            val labelY = chartPaddingTop + plotHeight + 22.dp.toPx()
            drawContext.canvas.nativeCanvas.drawText(
                catPoints[idx].first,
                pt.x,
                labelY,
                textPaint
            )
        }
    }
}

/**
 * Recharts Category Variance Canvas (D3 Diverging Waterfall):
 * Positive variance (Savings) bars extend upward, negative variance (Overrun) extends downward.
 */
@Composable
fun RechartsCategoryVarianceCanvas(
    items: List<CategoryBudgetComparisonItem>,
    activeIndex: Int,
    onIndexChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val progress = remember { Animatable(0f) }
    LaunchedEffect(items) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(700, easing = FastOutSlowInEasing))
    }

    val overBudgetRed = Color(0xFFEF4444)
    val underBudgetGreen = Color(0xFF10B981)
    val onSurface = MaterialTheme.colorScheme.onSurface

    Canvas(modifier = modifier) {
        val count = items.size
        if (count < 1) return@Canvas

        val chartPaddingTop = 20.dp.toPx()
        val chartPaddingBottom = 30.dp.toPx()
        val chartPaddingStart = 20.dp.toPx()
        val chartPaddingEnd = 20.dp.toPx()

        val plotWidth = size.width - chartPaddingStart - chartPaddingEnd
        val plotHeight = size.height - chartPaddingTop - chartPaddingBottom
        val zeroY = chartPaddingTop + (plotHeight / 2f)

        // Center zero line
        drawLine(
            color = onSurface.copy(alpha = 0.25f),
            start = Offset(chartPaddingStart, zeroY),
            end = Offset(size.width - chartPaddingEnd, zeroY),
            strokeWidth = 1.5.dp.toPx()
        )

        val maxVar = (items.maxOfOrNull { kotlin.math.abs(it.variance) } ?: 5000.0).coerceAtLeast(100.0)
        val barGroupWidth = plotWidth / count
        val barWidth = (barGroupWidth * 0.45f).coerceAtMost(24.dp.toPx())

        val labelPaint = NativePaint().apply {
            color = android.graphics.Color.LTGRAY
            textSize = if (count > 6) 18f else 22f
            textAlign = NativePaint.Align.CENTER
            isAntiAlias = true
        }

        items.forEachIndexed { idx, item ->
            val isSelected = idx == activeIndex
            val centerX = chartPaddingStart + (idx * barGroupWidth) + (barGroupWidth / 2f)
            val barLeft = centerX - (barWidth / 2f)

            val frac = ((item.variance / maxVar).toFloat() * progress.value).coerceIn(-1f, 1f)
            val barHeight = kotlin.math.abs(frac) * (plotHeight / 2f)

            if (item.variance >= 0) {
                // Surplus green bar upward
                drawRoundRect(
                    color = underBudgetGreen.copy(alpha = if (isSelected) 1f else 0.8f),
                    topLeft = Offset(barLeft, zeroY - barHeight),
                    size = Size(barWidth, barHeight.coerceAtLeast(3f)),
                    cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                )
            } else {
                // Over budget red bar downward
                drawRoundRect(
                    color = overBudgetRed.copy(alpha = if (isSelected) 1f else 0.8f),
                    topLeft = Offset(barLeft, zeroY),
                    size = Size(barWidth, barHeight.coerceAtLeast(3f)),
                    cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                )
            }

            // Category label
            val labelY = chartPaddingTop + plotHeight + 22.dp.toPx()
            val shortCat = if (item.category.length > 7) item.category.take(6) + ".." else item.category
            drawContext.canvas.nativeCanvas.drawText(
                shortCat,
                centerX,
                labelY,
                labelPaint
            )
        }
    }
}
