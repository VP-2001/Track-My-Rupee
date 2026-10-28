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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.StackedBarChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
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
import com.example.data.model.DefaultCategories
import com.example.data.model.ExpenseEntity
import com.example.data.model.MonthlyBudgetTrendPoint
import com.example.data.model.MonthlyBudgetTrendSummary
import java.util.Locale

/**
 * Recharts & D3 inspired Visualization Modes for Monthly Spending Trends vs Budget Limits
 */
enum class BudgetTrendChartMode(val label: String, val iconDescription: String) {
    AREA_LINE("Area & Line", "Continuous curve vs budget reference"),
    COMPARATIVE_BARS("Bars", "Side-by-side spend vs budget"),
    VARIANCE("Variance", "D3 savings waterfall")
}

enum class BudgetTrendHorizon(val label: String, val monthsCount: Int) {
    SIX_MONTHS("6M", 6),
    ONE_YEAR("1Y", 12)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RechartsBudgetTrendDashboard(
    summary6M: MonthlyBudgetTrendSummary,
    summary1Y: MonthlyBudgetTrendSummary,
    selectedCategory: String? = null,
    onCategorySelected: (String?) -> Unit = {},
    onViewMonthlyReport: ((year: Int, month: Int) -> Unit)? = null,
    onNavigateToBudgets: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedHorizon by remember { mutableStateOf(BudgetTrendHorizon.SIX_MONTHS) }
    var chartMode by remember { mutableStateOf(BudgetTrendChartMode.AREA_LINE) }
    var isExpandedModalOpen by remember { mutableStateOf(false) }

    val activeSummary = when (selectedHorizon) {
        BudgetTrendHorizon.SIX_MONTHS -> summary6M
        BudgetTrendHorizon.ONE_YEAR -> summary1Y
    }

    val data = activeSummary.trendPoints

    var activeIndex by remember(data) { mutableIntStateOf((data.size - 1).coerceAtLeast(0)) }
    val safeActiveIndex = activeIndex.coerceIn(0, (data.size - 1).coerceAtLeast(0))
    val activeItem = data.getOrNull(safeActiveIndex) ?: data.lastOrNull()

    // Chart Palette (Recharts vibrant signature colors)
    val spendTeal = Color(0xFF0D9488) // Primary Spent Line / Area
    val budgetAmber = Color(0xFFF59E0B) // Budget Reference Line
    val overBudgetRed = Color(0xFFEF4444) // Over-budget breach
    val underBudgetGreen = Color(0xFF10B981) // Safe / Under-budget
    val gridGray = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceColor = MaterialTheme.colorScheme.surface

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("recharts_budget_trend_dashboard_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 1. Recharts Header: Title, Horizon Toggle (6M/1Y), Mode Toggle & Fullscreen
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
                                    imageVector = Icons.AutoMirrored.Filled.ShowChart,
                                    contentDescription = "Trend vs Budget",
                                    tint = spendTeal,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Spending Trends vs Budget",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = onSurface
                        )
                    }
                    Text(
                        text = if (selectedCategory == null) "All budgets vs monthly spending" else "$selectedCategory budget limit tracking",
                        style = MaterialTheme.typography.bodySmall,
                        color = onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Horizon Selector: 6M vs 1Y
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    ) {
                        Row(modifier = Modifier.padding(2.dp)) {
                            BudgetTrendHorizon.values().forEach { horizon ->
                                val isSelected = selectedHorizon == horizon
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) spendTeal else Color.Transparent)
                                        .clickable { selectedHorizon = horizon }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .testTag("trend_horizon_${horizon.label.lowercase()}")
                                ) {
                                    Text(
                                        text = horizon.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.Black else onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Fullscreen / Expanded Dialog Trigger
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .size(32.dp)
                            .clickable { isExpandedModalOpen = true }
                            .testTag("expand_budget_trend_dashboard")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "Expand Fullscreen Dashboard",
                                tint = spendTeal,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Chart Mode Switcher: Area & Line vs Grouped Bars vs Variance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Row(modifier = Modifier.padding(3.dp)) {
                        BudgetTrendChartMode.values().forEach { mode ->
                            val isSelected = chartMode == mode
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(if (isSelected) spendTeal else Color.Transparent)
                                    .clickable { chartMode = mode }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                    .testTag("chart_mode_${mode.name.lowercase()}")
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

                // Legend Pills
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(spendTeal)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Spent", style = MaterialTheme.typography.labelSmall, color = onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .height(3.dp)
                                .background(budgetAmber)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Budget", style = MaterialTheme.typography.labelSmall, color = onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Category Filter Chips (All + Categories)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("trend_category_filter_row"),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                item {
                    val isAllSelected = selectedCategory == null || selectedCategory.equals("All", ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isAllSelected) spendTeal.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (isAllSelected) spendTeal else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .clickable { onCategorySelected(null) }
                            .testTag("trend_filter_category_all")
                    ) {
                        Text(
                            text = "All Categories",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isAllSelected) spendTeal else onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                items(DefaultCategories) { cat ->
                    val isCatSelected = selectedCategory.equals(cat, ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isCatSelected) spendTeal.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (isCatSelected) spendTeal else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .clickable { onCategorySelected(cat) }
                            .testTag("trend_filter_category_${cat.lowercase()}")
                    ) {
                        Text(
                            text = cat,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCatSelected) spendTeal else onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Performance Stat Cards: Adherence Rate, Net Savings, and Total Period Spend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Adherence Rate Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (activeSummary.monthsOverBudgetCount == 0) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (activeSummary.monthsOverBudgetCount == 0) underBudgetGreen else budgetAmber,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Adherence",
                                style = MaterialTheme.typography.labelSmall,
                                color = onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${activeSummary.monthsUnderBudgetCount}/${data.size} Safe",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (activeSummary.monthsOverBudgetCount == 0) underBudgetGreen else onSurface
                        )
                        Text(
                            text = "${String.format(Locale.getDefault(), "%.0f", activeSummary.adherenceRatePercent)}% in budget",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = onSurfaceVariant
                        )
                    }
                }

                // Net Savings / Variance Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (activeSummary.netSavings >= 0) underBudgetGreen.copy(alpha = 0.1f) else overBudgetRed.copy(alpha = 0.1f),
                    border = BorderStroke(
                        1.dp,
                        if (activeSummary.netSavings >= 0) underBudgetGreen.copy(alpha = 0.35f) else overBudgetRed.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.weight(1.1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = if (activeSummary.netSavings >= 0) "Net Savings" else "Net Overrun",
                            style = MaterialTheme.typography.labelSmall,
                            color = onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = ExpenseEntity.formatInr(kotlin.math.abs(activeSummary.netSavings)),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (activeSummary.netSavings >= 0) underBudgetGreen else overBudgetRed
                        )
                        Text(
                            text = if (activeSummary.netSavings >= 0) "Under overall budget" else "Exceeded budget",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = if (activeSummary.netSavings >= 0) underBudgetGreen else overBudgetRed
                        )
                    }
                }

                // Avg Monthly Spend vs Budget
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Avg Budget",
                            style = MaterialTheme.typography.labelSmall,
                            color = onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = ExpenseEntity.formatInr(activeSummary.averageMonthlyBudget),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = budgetAmber
                        )
                        Text(
                            text = "Avg spend: ${ExpenseEntity.formatInr(activeSummary.averageMonthlySpend)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Interactive Floating Recharts Tooltip for Active Month
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
                        .testTag("budget_trend_interactive_tooltip")
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
                                    text = activeItem.fullMonthName,
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
                                        text = if (activeItem.isOverBudget) "OVER BUDGET" else "SAFE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (activeItem.isOverBudget) overBudgetRed else underBudgetGreen,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = "${String.format(Locale.getDefault(), "%.0f", activeItem.budgetUsedPercent)}% used",
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
                                    text = "Spent: ${ExpenseEntity.formatInr(activeItem.totalSpent)}",
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
                                val varAmount = kotlin.math.abs(activeItem.variance)
                                Text(
                                    text = if (activeItem.variance >= 0) "Saved: ${ExpenseEntity.formatInr(varAmount)}" else "Over by: ${ExpenseEntity.formatInr(varAmount)}",
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

                        // Action button inside tooltip: View Month Report
                        if (onViewMonthlyReport != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = spendTeal.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, spendTeal.copy(alpha = 0.35f)),
                                    modifier = Modifier
                                        .clickable { onViewMonthlyReport(activeItem.year, activeItem.month) }
                                        .testTag("tooltip_view_report_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Assessment,
                                            contentDescription = null,
                                            tint = spendTeal,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "View ${activeItem.shortMonth} Report",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = spendTeal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 6. Native Recharts Interactive Canvas
            RechartsBudgetTrendCanvas(
                data = data,
                activeIndex = safeActiveIndex,
                onIndexChanged = { activeIndex = it },
                chartMode = chartMode,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .testTag("budget_trend_interactive_canvas")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 7. Month Selectors Bar (Quick Scrubbing Pills)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("budget_trend_months_lazy_row"),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(data) { idx, item ->
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
                            .clickable { activeIndex = idx }
                            .testTag("trend_month_selector_${item.shortMonth.lowercase()}")
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
                                text = item.shortMonth,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.Black else onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Fullscreen Detailed Deep-Dive Modal
    if (isExpandedModalOpen) {
        Dialog(
            onDismissRequest = { isExpandedModalOpen = false },
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
                                text = "Monthly Spending Trends vs Budget",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = onSurface
                            )
                            Text(
                                text = "Full financial adherence analysis & statistical breakdown",
                                style = MaterialTheme.typography.bodySmall,
                                color = onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { isExpandedModalOpen = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = onSurface)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Recharts Canvas in expanded view
                    RechartsBudgetTrendCanvas(
                        data = data,
                        activeIndex = safeActiveIndex,
                        onIndexChanged = { activeIndex = it },
                        chartMode = chartMode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Month breakdown list
                    Text(
                        text = "Monthly Performance Ledger",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(data) { item ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = BorderStroke(
                                    1.dp,
                                    if (item.isOverBudget) overBudgetRed.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.width(160.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = item.shortMonth,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = onSurface
                                        )
                                        Text(
                                            text = if (item.isOverBudget) "Exceeded" else "Within",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (item.isOverBudget) overBudgetRed else underBudgetGreen
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = ExpenseEntity.formatInr(item.totalSpent),
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
                                        text = "${String.format(Locale.getDefault(), "%.0f", item.budgetUsedPercent)}% used",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = onSurfaceVariant
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
 * Native Canvas Chart rendering:
 * - Recharts CartesianGrid: Subtle horizontal dashed lines & Y-axis labels
 * - Recharts ReferenceLine: Set budget limit threshold line with text badge
 * - Mode 1: Monotone cubic Bezier curve & gradient area fill with over-budget glow dots
 * - Mode 2: Comparative grouped bars (Actual Spend vs Budget Limit) with % tag
 * - Mode 3: D3 Waterfall / Variance bar chart (Surplus Green vs Deficit Red)
 */
@Composable
fun RechartsBudgetTrendCanvas(
    data: List<MonthlyBudgetTrendPoint>,
    activeIndex: Int,
    onIndexChanged: (Int) -> Unit,
    chartMode: BudgetTrendChartMode,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val progress = remember { Animatable(0f) }
    LaunchedEffect(data, chartMode) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(700, easing = FastOutSlowInEasing))
    }

    val spendTeal = Color(0xFF0D9488)
    val budgetAmber = Color(0xFFF59E0B)
    val overBudgetRed = Color(0xFFEF4444)
    val underBudgetGreen = Color(0xFF10B981)
    val onSurface = MaterialTheme.colorScheme.onSurface
    val surfaceColor = MaterialTheme.colorScheme.surface

    val maxVal = (data.maxOfOrNull { maxOf(it.totalSpent, it.budgetLimit) } ?: 40000.0).coerceAtLeast(1000.0)
    val effectiveMax = (maxVal * 1.15).coerceAtLeast(100.0)

    Canvas(
        modifier = modifier
            .pointerInput(data) {
                detectTapGestures { offset ->
                    val count = data.size
                    val stepX = size.width / count.coerceAtLeast(1)
                    val tappedIdx = (offset.x / stepX).toInt().coerceIn(0, count - 1)
                    onIndexChanged(tappedIdx)
                }
            }
            .pointerInput(data) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val count = data.size
                    val stepX = size.width / count.coerceAtLeast(1)
                    val draggedIdx = (change.position.x / stepX).toInt().coerceIn(0, count - 1)
                    onIndexChanged(draggedIdx)
                }
            }
    ) {
        val count = data.size
        if (count < 1) return@Canvas

        val chartPaddingTop = 24.dp.toPx()
        val chartPaddingBottom = 32.dp.toPx()
        val chartPaddingStart = 20.dp.toPx()
        val chartPaddingEnd = 20.dp.toPx()

        val plotWidth = size.width - chartPaddingStart - chartPaddingEnd
        val plotHeight = size.height - chartPaddingTop - chartPaddingBottom

        // 1. Recharts CartesianGrid: Horizontal dashed lines & Currency Y-labels
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

            // Y-Axis tick label
            val formattedVal = if (gridVal >= 1000) "₹${(gridVal / 1000).toInt()}k" else "₹${gridVal.toInt()}"
            drawContext.canvas.nativeCanvas.drawText(
                formattedVal,
                chartPaddingStart,
                gridY - 4f,
                gridPaint
            )
        }

        when (chartMode) {
            BudgetTrendChartMode.AREA_LINE -> {
                // Step X distance
                val stepX = if (count > 1) plotWidth / (count - 1) else plotWidth

                // A. Recharts Reference Line for Set Budget Limit across months
                // Draw set budget threshold line
                val avgBudget = data.map { it.budgetLimit }.average()
                val budgetY = chartPaddingTop + plotHeight - ((avgBudget / effectiveMax).toFloat() * plotHeight * progress.value)

                if (budgetY in chartPaddingTop..(chartPaddingTop + plotHeight)) {
                    drawLine(
                        color = budgetAmber.copy(alpha = 0.85f),
                        start = Offset(chartPaddingStart, budgetY),
                        end = Offset(size.width - chartPaddingEnd, budgetY),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f), 0f)
                    )

                    // Reference label on canvas
                    val budgetTagPaint = NativePaint().apply {
                        color = android.graphics.Color.rgb(245, 158, 11)
                        textSize = 20f
                        isFakeBoldText = true
                        isAntiAlias = true
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        "BUDGET LIMIT",
                        size.width - chartPaddingEnd - 120f,
                        budgetY - 6f,
                        budgetTagPaint
                    )
                }

                // B. Plot Points
                val points = data.mapIndexed { idx, item ->
                    val x = chartPaddingStart + (idx * stepX)
                    val y = chartPaddingTop + plotHeight - ((item.totalSpent / effectiveMax).toFloat() * plotHeight * progress.value)
                    Offset(x, y)
                }

                if (points.size >= 2) {
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

                    // Gradient area fill under curve
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                spendTeal.copy(alpha = 0.35f),
                                spendTeal.copy(alpha = 0.12f),
                                Color.Transparent
                            ),
                            startY = chartPaddingTop,
                            endY = chartPaddingTop + plotHeight
                        )
                    )

                    // Monotone line stroke
                    drawPath(
                        path = path,
                        brush = Brush.horizontalGradient(
                            colors = listOf(spendTeal, Color(0xFF6366F1), spendTeal)
                        ),
                        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // C. Active Cursor Line
                val activePoint = points.getOrNull(activeIndex)
                if (activePoint != null) {
                    drawLine(
                        color = spendTeal.copy(alpha = 0.5f),
                        start = Offset(activePoint.x, chartPaddingTop),
                        end = Offset(activePoint.x, chartPaddingTop + plotHeight),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                    )
                }

                // D. Dots and X-Axis Labels
                val labelPaint = NativePaint().apply {
                    color = android.graphics.Color.LTGRAY
                    textSize = 22f
                    textAlign = NativePaint.Align.CENTER
                    isAntiAlias = true
                }
                val activeLabelPaint = NativePaint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 24f
                    isFakeBoldText = true
                    textAlign = NativePaint.Align.CENTER
                    isAntiAlias = true
                }

                points.forEachIndexed { idx, pt ->
                    val isSelected = idx == activeIndex
                    val item = data[idx]
                    val isOver = item.isOverBudget
                    val dotColor = if (isOver) overBudgetRed else spendTeal

                    if (isSelected) {
                        drawCircle(
                            color = dotColor.copy(alpha = 0.3f),
                            radius = 12.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = dotColor,
                            radius = 6.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 3.dp.toPx(),
                            center = pt
                        )
                    } else {
                        drawCircle(
                            color = surfaceColor,
                            radius = 5.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = dotColor,
                            radius = 4.dp.toPx(),
                            center = pt
                        )
                    }

                    // Month label below axis
                    val labelY = chartPaddingTop + plotHeight + 22.dp.toPx()
                    drawContext.canvas.nativeCanvas.drawText(
                        item.shortMonth,
                        pt.x,
                        labelY,
                        if (isSelected) activeLabelPaint else labelPaint
                    )
                }
            }

            BudgetTrendChartMode.COMPARATIVE_BARS -> {
                // Grouped Bars for each month: Actual Spend vs Budget Limit
                val barGroupWidth = plotWidth / count
                val barWidth = (barGroupWidth * 0.35f).coerceAtMost(22.dp.toPx())
                val barSpacing = 4.dp.toPx()

                val labelPaint = NativePaint().apply {
                    color = android.graphics.Color.LTGRAY
                    textSize = 22f
                    textAlign = NativePaint.Align.CENTER
                    isAntiAlias = true
                }
                val activeLabelPaint = NativePaint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 24f
                    isFakeBoldText = true
                    textAlign = NativePaint.Align.CENTER
                    isAntiAlias = true
                }
                val percentPaint = NativePaint().apply {
                    textSize = 18f
                    textAlign = NativePaint.Align.CENTER
                    isAntiAlias = true
                }

                data.forEachIndexed { idx, item ->
                    val isSelected = idx == activeIndex
                    val groupCenterX = chartPaddingStart + (idx * barGroupWidth) + (barGroupWidth / 2f)

                    val spendHeight = ((item.totalSpent / effectiveMax).toFloat() * plotHeight * progress.value).coerceAtLeast(4f)
                    val budgetHeight = ((item.budgetLimit / effectiveMax).toFloat() * plotHeight * progress.value).coerceAtLeast(4f)

                    val spendLeft = groupCenterX - barWidth - (barSpacing / 2f)
                    val spendTop = chartPaddingTop + plotHeight - spendHeight

                    val budgetLeft = groupCenterX + (barSpacing / 2f)
                    val budgetTop = chartPaddingTop + plotHeight - budgetHeight

                    val barColor = if (item.isOverBudget) overBudgetRed else spendTeal

                    // 1. Spend Bar
                    drawRoundRect(
                        color = if (isSelected) barColor else barColor.copy(alpha = 0.85f),
                        topLeft = Offset(spendLeft, spendTop),
                        size = Size(barWidth, spendHeight),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )

                    // 2. Budget Limit Bar (dashed outline/surface pattern)
                    drawRoundRect(
                        color = budgetAmber.copy(alpha = if (isSelected) 0.85f else 0.5f),
                        topLeft = Offset(budgetLeft, budgetTop),
                        size = Size(barWidth, budgetHeight),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )

                    // Percentage on top of spend bar
                    percentPaint.color = if (item.isOverBudget) android.graphics.Color.rgb(239, 68, 68) else android.graphics.Color.rgb(13, 148, 136)
                    val pctText = "${item.budgetUsedPercent.toInt()}%"
                    drawContext.canvas.nativeCanvas.drawText(
                        pctText,
                        spendLeft + (barWidth / 2f),
                        spendTop - 6f,
                        percentPaint
                    )

                    // Active highlight halo
                    if (isSelected) {
                        drawRoundRect(
                            color = onSurface.copy(alpha = 0.1f),
                            topLeft = Offset(spendLeft - 4.dp.toPx(), chartPaddingTop),
                            size = Size((barWidth * 2) + barSpacing + 8.dp.toPx(), plotHeight),
                            cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                        )
                    }

                    // Month label
                    val labelY = chartPaddingTop + plotHeight + 22.dp.toPx()
                    drawContext.canvas.nativeCanvas.drawText(
                        item.shortMonth,
                        groupCenterX,
                        labelY,
                        if (isSelected) activeLabelPaint else labelPaint
                    )
                }
            }

            BudgetTrendChartMode.VARIANCE -> {
                // D3 Variance / Waterfall style: Center zero line with surplus green bars going up and over-budget red bars going down
                val zeroY = chartPaddingTop + (plotHeight / 2f)
                val barGroupWidth = plotWidth / count
                val barWidth = (barGroupWidth * 0.45f).coerceAtMost(28.dp.toPx())

                // Zero Line
                drawLine(
                    color = onSurface.copy(alpha = 0.3f),
                    start = Offset(chartPaddingStart, zeroY),
                    end = Offset(size.width - chartPaddingEnd, zeroY),
                    strokeWidth = 1.5.dp.toPx()
                )

                val labelPaint = NativePaint().apply {
                    color = android.graphics.Color.LTGRAY
                    textSize = 22f
                    textAlign = NativePaint.Align.CENTER
                    isAntiAlias = true
                }
                val activeLabelPaint = NativePaint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 24f
                    isFakeBoldText = true
                    textAlign = NativePaint.Align.CENTER
                    isAntiAlias = true
                }

                val maxVariance = (data.maxOfOrNull { kotlin.math.abs(it.variance) } ?: 5000.0).coerceAtLeast(100.0)

                data.forEachIndexed { idx, item ->
                    val isSelected = idx == activeIndex
                    val groupCenterX = chartPaddingStart + (idx * barGroupWidth) + (barGroupWidth / 2f)
                    val barLeft = groupCenterX - (barWidth / 2f)

                    val varianceFraction = ((item.variance / maxVariance).toFloat() * progress.value).coerceIn(-1f, 1f)
                    val barHeight = kotlin.math.abs(varianceFraction) * (plotHeight / 2f)

                    if (item.variance >= 0) {
                        // Surplus: Bar goes upward from zero line
                        drawRoundRect(
                            color = underBudgetGreen.copy(alpha = if (isSelected) 1f else 0.8f),
                            topLeft = Offset(barLeft, zeroY - barHeight),
                            size = Size(barWidth, barHeight.coerceAtLeast(3f)),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    } else {
                        // Over Budget: Bar goes downward from zero line
                        drawRoundRect(
                            color = overBudgetRed.copy(alpha = if (isSelected) 1f else 0.8f),
                            topLeft = Offset(barLeft, zeroY),
                            size = Size(barWidth, barHeight.coerceAtLeast(3f)),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }

                    // Month label
                    val labelY = chartPaddingTop + plotHeight + 22.dp.toPx()
                    drawContext.canvas.nativeCanvas.drawText(
                        item.shortMonth,
                        groupCenterX,
                        labelY,
                        if (isSelected) activeLabelPaint else labelPaint
                    )
                }
            }
        }
    }
}
