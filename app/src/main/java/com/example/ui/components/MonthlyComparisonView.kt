package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryCrossMonthItem
import com.example.data.model.CategoryMonthlySpend
import com.example.data.model.ComparisonViewMode
import com.example.data.model.ExpenseEntity
import com.example.data.model.MerchantCrossMonthItem
import com.example.data.model.MerchantMonthlySpend
import com.example.data.model.MonthlyBreakdownItem
import com.example.data.model.MonthlyComparisonReport
import com.example.ui.screens.getCategoryIcon
import com.example.ui.theme.AmberGold
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SophisticatedDarkBorder
import com.example.ui.theme.SophisticatedDarkOnSurface
import com.example.ui.theme.SophisticatedDarkOnSurfaceVariant
import com.example.ui.theme.SophisticatedDarkSurface
import com.example.ui.theme.SophisticatedDarkSurfaceVariant
import com.example.ui.theme.SophisticatedPrimary
import com.example.ui.theme.SuccessGreen

@Composable
fun MonthlyComparisonView(
    report: MonthlyComparisonReport,
    selectedViewMode: ComparisonViewMode,
    onSelectViewMode: (ComparisonViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    // Maintain set of expanded card IDs (monthKeys, category names, or merchant names)
    val expandedStates = remember { mutableStateMapOf<String, Boolean>() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top KPI & Spending Drivers Card
        ComparisonKpiSummaryCard(report = report)

        // View Mode Perspective Selector (By Month, By Category, By Merchant)
        ComparisonPerspectiveSelector(
            selectedViewMode = selectedViewMode,
            onSelectViewMode = onSelectViewMode
        )

        // Empty state when no data exists
        if (report.months.isEmpty()) {
            EmptyComparisonState()
            return
        }

        // Single month notice
        if (report.months.size == 1) {
            SingleMonthNoticeCard(monthName = report.months.first().monthName)
        }

        // Search within comparison view (for categories or merchants)
        if (selectedViewMode != ComparisonViewMode.BY_MONTH) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("comparison_search_input"),
                placeholder = {
                    Text(
                        if (selectedViewMode == ComparisonViewMode.BY_CATEGORY) {
                            "Filter categories in comparison..."
                        } else {
                            "Filter merchants in comparison..."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = SophisticatedDarkOnSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = SophisticatedPrimary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = SophisticatedDarkOnSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SophisticatedPrimary,
                    unfocusedBorderColor = SophisticatedDarkBorder,
                    focusedContainerColor = SophisticatedDarkSurfaceVariant,
                    unfocusedContainerColor = SophisticatedDarkSurfaceVariant
                )
            )
        }

        // Content according to selected perspective
        when (selectedViewMode) {
            ComparisonViewMode.BY_MONTH -> {
                MonthsComparativeList(
                    months = report.months,
                    highestMonthKey = report.highestSpendMonth?.monthKey,
                    lowestMonthKey = if (report.months.size > 1) report.lowestSpendMonth?.monthKey else null,
                    averageMonthlySpend = report.averageMonthlySpend,
                    expandedStates = expandedStates
                )
            }
            ComparisonViewMode.BY_CATEGORY -> {
                val filteredCategories = if (searchQuery.isBlank()) {
                    report.categoryCrossMonth
                } else {
                    report.categoryCrossMonth.filter { it.category.contains(searchQuery, ignoreCase = true) }
                }
                CategoryComparativeList(
                    categories = filteredCategories,
                    months = report.months,
                    expandedStates = expandedStates
                )
            }
            ComparisonViewMode.BY_MERCHANT -> {
                val filteredMerchants = if (searchQuery.isBlank()) {
                    report.merchantCrossMonth
                } else {
                    report.merchantCrossMonth.filter { it.merchant.contains(searchQuery, ignoreCase = true) }
                }
                MerchantComparativeList(
                    merchants = filteredMerchants,
                    months = report.months,
                    expandedStates = expandedStates
                )
            }
        }
    }
}

@Composable
private fun ComparisonKpiSummaryCard(report: MonthlyComparisonReport) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("monthly_comparison_kpi_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurface),
        border = BorderStroke(1.dp, SophisticatedDarkBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SophisticatedPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Insights,
                            contentDescription = null,
                            tint = SophisticatedPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Monthly Spending Comparison",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SophisticatedDarkOnSurface
                        )
                        Text(
                            text = if (report.months.isNotEmpty()) {
                                "Comparing ${report.months.size} recorded month${if (report.months.size > 1) "s" else ""}"
                            } else {
                                "No transactions recorded"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = SophisticatedDarkOnSurfaceVariant
                        )
                    }
                }

                if (report.months.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SophisticatedPrimary.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, SophisticatedPrimary.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = ExpenseEntity.formatInr(report.totalAllTimeSpend),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = SophisticatedPrimary
                        )
                    }
                }
            }

            if (report.months.isNotEmpty()) {
                // Key KPI Metrics Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Highest Spend Month Card
                    report.highestSpendMonth?.let { peak ->
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurfaceVariant),
                            border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.35f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                        contentDescription = null,
                                        tint = DangerRed,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Peak Month",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = DangerRed
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = peak.shortName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SophisticatedDarkOnSurface
                                )
                                Text(
                                    text = ExpenseEntity.formatInr(peak.totalAmount),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SophisticatedPrimary
                                )
                            }
                        }
                    }

                    // Monthly Average Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurfaceVariant),
                        border = BorderStroke(1.dp, SophisticatedDarkBorder)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = SophisticatedDarkOnSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Monthly Avg",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = SophisticatedDarkOnSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = ExpenseEntity.formatInr(report.averageMonthlySpend),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = SophisticatedDarkOnSurface
                            )
                            Text(
                                text = "${report.months.sumOf { it.transactionCount }} total txns",
                                style = MaterialTheme.typography.bodySmall,
                                color = SophisticatedDarkOnSurfaceVariant
                            )
                        }
                    }
                }

                // Spending Drivers (Top Category & Top Merchant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    report.overallTopCategory?.let { topCat ->
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = SophisticatedDarkSurfaceVariant,
                            border = BorderStroke(1.dp, SophisticatedDarkBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Category,
                                    contentDescription = null,
                                    tint = AmberGold,
                                    modifier = Modifier.size(14.dp)
                                )
                                Column {
                                    Text(
                                        text = "Top Category",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SophisticatedDarkOnSurfaceVariant
                                    )
                                    Text(
                                        text = topCat,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = SophisticatedDarkOnSurface
                                    )
                                }
                            }
                        }
                    }

                    report.overallTopMerchant?.let { topMerchant ->
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = SophisticatedDarkSurfaceVariant,
                            border = BorderStroke(1.dp, SophisticatedDarkBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = SophisticatedPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Column {
                                    Text(
                                        text = "Top Merchant",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SophisticatedDarkOnSurfaceVariant
                                    )
                                    Text(
                                        text = topMerchant,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = SophisticatedDarkOnSurface
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

@Composable
private fun ComparisonPerspectiveSelector(
    selectedViewMode: ComparisonViewMode,
    onSelectViewMode: (ComparisonViewMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("comparison_view_mode_selector"),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ComparisonViewMode.entries.forEach { mode ->
            val isSelected = selectedViewMode == mode
            val icon = when (mode) {
                ComparisonViewMode.BY_MONTH -> Icons.Default.CalendarMonth
                ComparisonViewMode.BY_CATEGORY -> Icons.Default.Category
                ComparisonViewMode.BY_MERCHANT -> Icons.Default.Business
            }

            FilterChip(
                selected = isSelected,
                onClick = { onSelectViewMode(mode) },
                label = {
                    Text(
                        text = mode.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("comparison_tab_${mode.name.lowercase()}"),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SophisticatedPrimary,
                    selectedLabelColor = Color(0xFF1E1035),
                    selectedLeadingIconColor = Color(0xFF1E1035),
                    containerColor = SophisticatedDarkSurfaceVariant,
                    labelColor = SophisticatedDarkOnSurfaceVariant,
                    iconColor = SophisticatedDarkOnSurfaceVariant
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) SophisticatedPrimary else SophisticatedDarkBorder
                ),
                shape = RoundedCornerShape(10.dp)
            )
        }
    }
}

@Composable
private fun MonthsComparativeList(
    months: List<MonthlyBreakdownItem>,
    highestMonthKey: String?,
    lowestMonthKey: String?,
    averageMonthlySpend: Double,
    expandedStates: MutableMap<String, Boolean>
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        months.forEach { month ->
            val isExpanded = expandedStates[month.monthKey] == true
            val isPeak = month.monthKey == highestMonthKey
            val isLowest = month.monthKey == lowestMonthKey

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("comparison_month_card_${month.monthKey}"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurface),
                border = BorderStroke(
                    1.dp,
                    if (isPeak) DangerRed.copy(alpha = 0.5f) else SophisticatedDarkBorder
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Month Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = month.monthName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SophisticatedDarkOnSurface
                                )

                                if (isPeak) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = DangerRed.copy(alpha = 0.18f),
                                        border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.35f))
                                    ) {
                                        Text(
                                            text = "Peak Spend",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = DangerRed
                                        )
                                    }
                                } else if (isLowest) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = SuccessGreen.copy(alpha = 0.18f),
                                        border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.35f))
                                    ) {
                                        Text(
                                            text = "Lowest Spend",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = SuccessGreen
                                        )
                                    }
                                }
                            }

                            val diffFromAvg = if (averageMonthlySpend > 0) {
                                ((month.totalAmount - averageMonthlySpend) / averageMonthlySpend) * 100
                            } else 0.0

                            Text(
                                text = if (diffFromAvg > 0) {
                                    "+${String.format(java.util.Locale.US, "%.1f", diffFromAvg)}% vs avg • ${month.transactionCount} txns"
                                } else {
                                    "${String.format(java.util.Locale.US, "%.1f", diffFromAvg)}% vs avg • ${month.transactionCount} txns"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (diffFromAvg > 10) AmberGold else SophisticatedDarkOnSurfaceVariant
                            )
                        }

                        Text(
                            text = ExpenseEntity.formatInr(month.totalAmount),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = SophisticatedPrimary
                        )
                    }

                    // Top drivers highlights for this month
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        month.topCategory?.let { tc ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SophisticatedDarkSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = getCategoryIcon(tc.category),
                                        contentDescription = null,
                                        tint = SophisticatedPrimary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "${tc.category}: ${ExpenseEntity.formatInr(tc.amount)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SophisticatedDarkOnSurface,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        month.topMerchant?.let { tm ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SophisticatedDarkSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Storefront,
                                        contentDescription = null,
                                        tint = AmberGold,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "${tm.merchant}: ${ExpenseEntity.formatInr(tm.amount)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SophisticatedDarkOnSurface,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // Expander Accordion Button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SophisticatedDarkSurfaceVariant.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, SophisticatedDarkBorder.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedStates[month.monthKey] = !isExpanded
                            }
                            .testTag("expand_month_${month.monthKey}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isExpanded) "Hide Category & Merchant Breakdown" else "View Categories & Merchants Breakdown",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = SophisticatedPrimary
                            )
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = SophisticatedPrimary
                            )
                        }
                    }

                    // Expandable Details Section
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Category Breakdown in this month
                            Text(
                                text = "Categories in ${month.shortName}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = SophisticatedDarkOnSurface
                            )

                            month.categorySpends.forEach { cat ->
                                CategoryBreakdownItemRow(categorySpend = cat)
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Merchant Breakdown in this month
                            Text(
                                text = "Merchants & Payees in ${month.shortName}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = SophisticatedDarkOnSurface
                            )

                            month.merchantSpends.forEach { merch ->
                                MerchantBreakdownItemRow(merchantSpend = merch)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnnualSpendingTrendLineChart(
    title: String,
    monthlySpends: Map<String, Double>,
    allMonths: List<MonthlyBreakdownItem>,
    peakMonthKey: String,
    modifier: Modifier = Modifier
) {
    val chronologicalMonths = remember(allMonths) {
        allMonths.sortedWith(compareBy<MonthlyBreakdownItem> { it.year }.thenBy { it.month })
    }

    if (chronologicalMonths.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(130.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No historical monthly data to plot trend line",
                color = SophisticatedDarkOnSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
        return
    }

    val trendPoints = remember(chronologicalMonths, monthlySpends) {
        chronologicalMonths.map { m ->
            Pair(m, monthlySpends[m.monthKey] ?: 0.0)
        }
    }

    var activeIndex by remember { mutableIntStateOf(-1) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(trendPoints) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(650))
    }

    val maxAmount = remember(trendPoints) {
        (trendPoints.maxOfOrNull { it.second } ?: 1.0).coerceAtLeast(10.0)
    }

    val peakPoint = remember(trendPoints, peakMonthKey) {
        trendPoints.firstOrNull { it.first.monthKey == peakMonthKey }
            ?: trendPoints.maxByOrNull { it.second }
    }

    val totalAnnualSpend = remember(trendPoints) {
        trendPoints.sumOf { it.second }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Annual spending trend for $title. Total annual spend ${ExpenseEntity.formatInr(totalAnnualSpend)}. Peak in ${peakPoint?.first?.monthName ?: "N/A"} of ${ExpenseEntity.formatInr(peakPoint?.second ?: 0.0)}"
            }
            .testTag("annual_trend_line_chart"),
        shape = RoundedCornerShape(12.dp),
        color = SophisticatedDarkSurfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, SophisticatedDarkBorder.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header with interactive tooltip or trend title + Peak badge
            val activeItem = trendPoints.getOrNull(activeIndex)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = SophisticatedPrimary
                    )
                    Text(
                        text = "Annual Spending Trend",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SophisticatedDarkOnSurface
                    )
                }

                // Peak badge
                if (peakPoint != null && peakPoint.second > 0) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = AmberGold.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "Peak: ${peakPoint.first.shortName} • ${ExpenseEntity.formatInr(peakPoint.second)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = AmberGold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Interactive inspection tooltip bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = if (activeItem != null) SophisticatedPrimary.copy(alpha = 0.15f) else Color.Transparent
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (activeItem != null) {
                        Text(
                            text = "${activeItem.first.monthName} ${activeItem.first.year}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = SophisticatedDarkOnSurface
                        )
                        Text(
                            text = ExpenseEntity.formatInr(activeItem.second),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = SophisticatedPrimary
                        )
                    } else {
                        Text(
                            text = "Tap or drag along curve to inspect monthly spend",
                            style = MaterialTheme.typography.labelSmall,
                            color = SophisticatedDarkOnSurfaceVariant
                        )
                    }
                }
            }

            // Canvas Chart with Smooth Bézier curves
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .pointerInput(trendPoints) {
                        detectTapGestures { offset ->
                            val count = trendPoints.size
                            if (count > 0) {
                                val step = size.width / count
                                val idx = (offset.x / step).toInt().coerceIn(0, trendPoints.lastIndex)
                                activeIndex = idx
                            }
                        }
                    }
                    .pointerInput(trendPoints) {
                        detectDragGestures { change, _ ->
                            val count = trendPoints.size
                            if (count > 0) {
                                val step = size.width / count
                                val idx = (change.position.x / step).toInt().coerceIn(0, trendPoints.lastIndex)
                                activeIndex = idx
                            }
                        }
                    }
            ) {
                val width = size.width
                val chartHeight = size.height - 18.dp.toPx()
                val count = trendPoints.size
                val stepX = if (count > 1) width / (count - 1) else width

                // Horizontal Guide Lines
                for (i in 0..2) {
                    val y = chartHeight * (i / 2f)
                    drawLine(
                        color = Color.White.copy(alpha = 0.08f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                val points = trendPoints.mapIndexed { index, pair ->
                    val x = if (count > 1) index * stepX else width / 2f
                    val normalized = (pair.second / maxAmount).toFloat() * progress.value
                    val y = chartHeight - (normalized * chartHeight)
                    Offset(x, y)
                }

                if (points.isNotEmpty()) {
                    // Smooth Bézier curve path
                    val curvePath = Path().apply {
                        moveTo(points[0].x, points[0].y)
                        for (i in 0 until points.size - 1) {
                            val p0 = points[i]
                            val p1 = points[i + 1]
                            val controlPoint1 = Offset(p0.x + (p1.x - p0.x) / 2f, p0.y)
                            val controlPoint2 = Offset(p0.x + (p1.x - p0.x) / 2f, p1.y)
                            cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p1.x, p1.y)
                        }
                    }

                    // Gradient Area under curve
                    val areaPath = Path().apply {
                        addPath(curvePath)
                        lineTo(points.last().x, chartHeight)
                        lineTo(points.first().x, chartHeight)
                        close()
                    }

                    drawPath(
                        path = areaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                SophisticatedPrimary.copy(alpha = 0.35f),
                                SophisticatedPrimary.copy(alpha = 0.02f)
                            ),
                            startY = 0f,
                            endY = chartHeight
                        )
                    )

                    // Line Stroke
                    drawPath(
                        path = curvePath,
                        color = SophisticatedPrimary,
                        style = Stroke(
                            width = 2.8.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // Draw Point Nodes
                    points.forEachIndexed { index, pt ->
                        val pair = trendPoints[index]
                        val isPeak = pair.first.monthKey == peakMonthKey && pair.second > 0
                        val isSelected = index == activeIndex

                        val pointColor = when {
                            isSelected -> AmberGold
                            isPeak -> AmberGold
                            else -> SophisticatedPrimary
                        }

                        val radius = when {
                            isSelected -> 7.dp.toPx()
                            isPeak -> 5.5.dp.toPx()
                            else -> 3.5.dp.toPx()
                        }

                        drawCircle(color = pointColor, radius = radius, center = pt)

                        if (isPeak || isSelected) {
                            drawCircle(color = Color.White, radius = 2.dp.toPx(), center = pt)
                        }
                    }
                }
            }

            // X-Axis Month Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                trendPoints.forEachIndexed { index, pair ->
                    val isPeak = pair.first.monthKey == peakMonthKey && pair.second > 0
                    val isSelected = index == activeIndex
                    Text(
                        text = pair.first.shortName.take(3),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = if (isPeak || isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                        color = when {
                            isSelected -> AmberGold
                            isPeak -> AmberGold
                            else -> SophisticatedDarkOnSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryComparativeList(
    categories: List<CategoryCrossMonthItem>,
    months: List<MonthlyBreakdownItem>,
    expandedStates: MutableMap<String, Boolean>
) {
    if (categories.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurface)
        ) {
            Text(
                text = "No categories matched the filter.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = SophisticatedDarkOnSurfaceVariant
            )
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        categories.forEach { item ->
            val isExpanded = expandedStates["cat_${item.category}"] == true

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("comparison_category_card_${item.category}"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurface),
                border = BorderStroke(1.dp, SophisticatedDarkBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(SophisticatedPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getCategoryIcon(item.category),
                                    contentDescription = null,
                                    tint = SophisticatedPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = item.category,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SophisticatedDarkOnSurface
                                )
                                Text(
                                    text = "Peak: ${item.peakMonthName} (${ExpenseEntity.formatInr(item.peakMonthAmount)})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AmberGold
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = ExpenseEntity.formatInr(item.totalAllMonths),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SophisticatedPrimary
                            )
                            Text(
                                text = "across ${item.monthlySpends.size} mo",
                                style = MaterialTheme.typography.bodySmall,
                                color = SophisticatedDarkOnSurfaceVariant
                            )
                        }
                    }

                    // Expander Accordion Button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SophisticatedDarkSurfaceVariant.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, SophisticatedDarkBorder.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedStates["cat_${item.category}"] = !isExpanded
                            }
                            .testTag("expand_cat_${item.category}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isExpanded) "Hide Month-by-Month Matrix" else "Show Month-by-Month Breakdown",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = SophisticatedPrimary
                            )
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = SophisticatedPrimary
                            )
                        }
                    }

                    // Expandable Month-by-Month Matrix
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Annual Spending Trend Line Chart inside expanded category
                            AnnualSpendingTrendLineChart(
                                title = item.category,
                                monthlySpends = item.monthlySpends,
                                allMonths = months,
                                peakMonthKey = item.peakMonthKey
                            )

                            Text(
                                text = "Monthly Spending Table",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = SophisticatedDarkOnSurfaceVariant
                            )

                            months.forEach { m ->
                                val spend = item.monthlySpends[m.monthKey] ?: 0.0
                                val isPeakMonth = m.monthKey == item.peakMonthKey && spend > 0
                                val relativeProgress = if (item.peakMonthAmount > 0) {
                                    (spend / item.peakMonthAmount).toFloat().coerceIn(0f, 1f)
                                } else 0f

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isPeakMonth) SophisticatedPrimary.copy(alpha = 0.1f) else SophisticatedDarkSurfaceVariant,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isPeakMonth) SophisticatedPrimary.copy(alpha = 0.3f) else SophisticatedDarkBorder.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = m.monthName,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (isPeakMonth) FontWeight.Bold else FontWeight.Normal,
                                                    color = SophisticatedDarkOnSurface
                                                )
                                                if (isPeakMonth) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = AmberGold.copy(alpha = 0.2f)
                                                    ) {
                                                        Text(
                                                            text = "Highest",
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = AmberGold
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))
                                            LinearProgressIndicator(
                                                progress = { relativeProgress },
                                                modifier = Modifier
                                                    .fillMaxWidth(0.85f)
                                                    .height(4.dp)
                                                    .clip(RoundedCornerShape(2.dp)),
                                                color = if (isPeakMonth) AmberGold else SophisticatedPrimary,
                                                trackColor = SophisticatedDarkBorder
                                            )
                                        }

                                        Text(
                                            text = ExpenseEntity.formatInr(spend),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (spend > 0) SophisticatedDarkOnSurface else SophisticatedDarkOnSurfaceVariant
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
}

@Composable
private fun MerchantComparativeList(
    merchants: List<MerchantCrossMonthItem>,
    months: List<MonthlyBreakdownItem>,
    expandedStates: MutableMap<String, Boolean>
) {
    if (merchants.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurface)
        ) {
            Text(
                text = "No merchants matched the filter.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = SophisticatedDarkOnSurfaceVariant
            )
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        merchants.forEach { item ->
            val isExpanded = expandedStates["merch_${item.merchant}"] == true

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("comparison_merchant_card_${item.merchant}"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurface),
                border = BorderStroke(1.dp, SophisticatedDarkBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(AmberGold.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = AmberGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = item.merchant,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SophisticatedDarkOnSurface
                                )
                                Text(
                                    text = "Peak: ${item.peakMonthName} (${ExpenseEntity.formatInr(item.peakMonthAmount)})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AmberGold
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = ExpenseEntity.formatInr(item.totalAllMonths),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SophisticatedPrimary
                            )
                            Text(
                                text = "across ${item.monthlySpends.size} mo",
                                style = MaterialTheme.typography.bodySmall,
                                color = SophisticatedDarkOnSurfaceVariant
                            )
                        }
                    }

                    // Expander Accordion Button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SophisticatedDarkSurfaceVariant.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, SophisticatedDarkBorder.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedStates["merch_${item.merchant}"] = !isExpanded
                            }
                            .testTag("expand_merch_${item.merchant}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isExpanded) "Hide Month-by-Month Breakdown" else "Show Month-by-Month Breakdown",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = SophisticatedPrimary
                            )
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = SophisticatedPrimary
                            )
                        }
                    }

                    // Expandable Month-by-Month Matrix
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Annual Spending Trend Line Chart inside expanded merchant card
                            AnnualSpendingTrendLineChart(
                                title = item.merchant,
                                monthlySpends = item.monthlySpends,
                                allMonths = months,
                                peakMonthKey = item.peakMonthKey
                            )

                            Text(
                                text = "Monthly Spending Table",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = SophisticatedDarkOnSurfaceVariant
                            )

                            months.forEach { m ->
                                val spend = item.monthlySpends[m.monthKey] ?: 0.0
                                val isPeakMonth = m.monthKey == item.peakMonthKey && spend > 0
                                val relativeProgress = if (item.peakMonthAmount > 0) {
                                    (spend / item.peakMonthAmount).toFloat().coerceIn(0f, 1f)
                                } else 0f

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isPeakMonth) AmberGold.copy(alpha = 0.1f) else SophisticatedDarkSurfaceVariant,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isPeakMonth) AmberGold.copy(alpha = 0.3f) else SophisticatedDarkBorder.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = m.monthName,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (isPeakMonth) FontWeight.Bold else FontWeight.Normal,
                                                    color = SophisticatedDarkOnSurface
                                                )
                                                if (isPeakMonth) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = AmberGold.copy(alpha = 0.2f)
                                                    ) {
                                                        Text(
                                                            text = "Highest",
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = AmberGold
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))
                                            LinearProgressIndicator(
                                                progress = { relativeProgress },
                                                modifier = Modifier
                                                    .fillMaxWidth(0.85f)
                                                    .height(4.dp)
                                                    .clip(RoundedCornerShape(2.dp)),
                                                color = if (isPeakMonth) AmberGold else SophisticatedPrimary,
                                                trackColor = SophisticatedDarkBorder
                                            )
                                        }

                                        Text(
                                            text = ExpenseEntity.formatInr(spend),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (spend > 0) SophisticatedDarkOnSurface else SophisticatedDarkOnSurfaceVariant
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
}

@Composable
private fun CategoryBreakdownItemRow(categorySpend: CategoryMonthlySpend) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = SophisticatedDarkSurfaceVariant,
        border = BorderStroke(1.dp, SophisticatedDarkBorder.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = getCategoryIcon(categorySpend.category),
                        contentDescription = null,
                        tint = SophisticatedPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = categorySpend.category,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = SophisticatedDarkOnSurface
                    )
                    Text(
                        text = "(${categorySpend.count} txns)",
                        style = MaterialTheme.typography.bodySmall,
                        color = SophisticatedDarkOnSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${String.format(java.util.Locale.US, "%.1f", categorySpend.percentage)}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = SophisticatedDarkOnSurfaceVariant
                    )
                    Text(
                        text = ExpenseEntity.formatInr(categorySpend.amount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = SophisticatedDarkOnSurface
                    )
                }
            }

            LinearProgressIndicator(
                progress = { (categorySpend.percentage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = SophisticatedPrimary,
                trackColor = SophisticatedDarkBorder
            )
        }
    }
}

@Composable
private fun MerchantBreakdownItemRow(merchantSpend: MerchantMonthlySpend) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = SophisticatedDarkSurfaceVariant,
        border = BorderStroke(1.dp, SophisticatedDarkBorder.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = null,
                        tint = AmberGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = merchantSpend.merchant,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = SophisticatedDarkOnSurface
                    )
                    Text(
                        text = "(${merchantSpend.count} txns)",
                        style = MaterialTheme.typography.bodySmall,
                        color = SophisticatedDarkOnSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${String.format(java.util.Locale.US, "%.1f", merchantSpend.percentage)}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = SophisticatedDarkOnSurfaceVariant
                    )
                    Text(
                        text = ExpenseEntity.formatInr(merchantSpend.amount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = SophisticatedDarkOnSurface
                    )
                }
            }

            LinearProgressIndicator(
                progress = { (merchantSpend.percentage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = AmberGold,
                trackColor = SophisticatedDarkBorder
            )
        }
    }
}

@Composable
private fun SingleMonthNoticeCard(monthName: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurfaceVariant),
        border = BorderStroke(1.dp, SophisticatedDarkBorder)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Insights,
                contentDescription = null,
                tint = SophisticatedPrimary,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = "Showing initial data for $monthName",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = SophisticatedDarkOnSurface
                )
                Text(
                    text = "As you log transactions across upcoming or past months, cross-month comparative graphs and peak shift badges will automatically expand here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SophisticatedDarkOnSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyComparisonState() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("empty_comparison_state"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurface),
        border = BorderStroke(1.dp, SophisticatedDarkBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(SophisticatedPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = SophisticatedPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Text(
                text = "No Expenses to Compare",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SophisticatedDarkOnSurface
            )
            Text(
                text = "Add transactions in different months to view month-by-month spending comparisons, category shifts, and top merchant drivers.",
                style = MaterialTheme.typography.bodyMedium,
                color = SophisticatedDarkOnSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
