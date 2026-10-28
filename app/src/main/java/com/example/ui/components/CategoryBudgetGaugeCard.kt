package com.example.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExpenseEntity
import com.example.ui.screens.getCategoryIcon
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SophisticatedDarkBorder
import com.example.ui.theme.SophisticatedDarkOnSurface
import com.example.ui.theme.SophisticatedDarkOnSurfaceVariant
import com.example.ui.theme.SophisticatedDarkSurface
import com.example.ui.theme.SophisticatedDarkSurfaceDim
import com.example.ui.theme.SophisticatedDarkSurfaceVariant
import com.example.ui.theme.SophisticatedOnPrimary
import com.example.ui.theme.SophisticatedPrimary
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TagIncomeMint
import com.example.ui.theme.TagShoppingAmber
import com.example.ui.viewmodel.BudgetProgressItem
import java.util.Locale

enum class BudgetGaugeDisplayMode {
    GAUGE, // Visual radial circular arc gauges
    BAR    // Sleek horizontal progress bars
}

/**
 * Visual Progress Bar & Gauge card for each budget category on the dashboard.
 * Displays:
 * 1. Percentage of budget consumed (% used)
 * 2. Remaining headroom (remaining amount vs over-budget amount)
 * 3. Spent amount vs set monthly limit
 * 4. Distinct visual state (Safe teal/green, Warning amber >= 80%, Over-budget danger red > 100%)
 */
@Composable
fun CategoryBudgetGaugeCard(
    budgetProgress: List<BudgetProgressItem>,
    onNavigateToBudgets: () -> Unit,
    onCategoryClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var displayMode by remember { mutableStateOf(BudgetGaugeDisplayMode.GAUGE) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("category_budget_gauge_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurface),
        border = BorderStroke(1.dp, SophisticatedDarkBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
                .animateContentSize()
        ) {
            // Header Row: Title, Subtitle, Mode Switcher & Manage Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = SophisticatedPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Category Budget Gauges",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SophisticatedDarkOnSurface
                        )
                    }
                    Text(
                        text = "Consumed % vs. remaining headroom",
                        style = MaterialTheme.typography.bodySmall,
                        color = SophisticatedDarkOnSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Gauge vs Bar View Mode Toggle
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SophisticatedDarkSurfaceVariant,
                        border = BorderStroke(1.dp, SophisticatedDarkBorder)
                    ) {
                        Row(modifier = Modifier.padding(2.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (displayMode == BudgetGaugeDisplayMode.GAUGE) SophisticatedPrimary else Color.Transparent)
                                    .clickable { displayMode = BudgetGaugeDisplayMode.GAUGE }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("budget_gauge_mode_radial")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Speed,
                                        contentDescription = "Gauge",
                                        tint = if (displayMode == BudgetGaugeDisplayMode.GAUGE) Color.Black else SophisticatedDarkOnSurfaceVariant,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "Gauge",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (displayMode == BudgetGaugeDisplayMode.GAUGE) Color.Black else SophisticatedDarkOnSurfaceVariant
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (displayMode == BudgetGaugeDisplayMode.BAR) SophisticatedPrimary else Color.Transparent)
                                    .clickable { displayMode = BudgetGaugeDisplayMode.BAR }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("budget_gauge_mode_linear")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ViewAgenda,
                                        contentDescription = "Bars",
                                        tint = if (displayMode == BudgetGaugeDisplayMode.BAR) Color.Black else SophisticatedDarkOnSurfaceVariant,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "Bars",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (displayMode == BudgetGaugeDisplayMode.BAR) Color.Black else SophisticatedDarkOnSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Manage Button
                    Surface(
                        shape = CircleShape,
                        color = SophisticatedDarkSurfaceVariant,
                        border = BorderStroke(1.dp, SophisticatedDarkBorder),
                        modifier = Modifier
                            .clickable { onNavigateToBudgets() }
                            .testTag("manage_category_budgets_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "Manage",
                                style = MaterialTheme.typography.labelSmall,
                                color = SophisticatedPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Manage Budgets",
                                tint = SophisticatedPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (budgetProgress.isEmpty()) {
                // Empty state when user hasn't set any budgets
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SophisticatedDarkSurfaceDim,
                    border = BorderStroke(1.dp, SophisticatedDarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.Speed,
                            contentDescription = null,
                            tint = SophisticatedPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "No Category Budgets Set",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SophisticatedDarkOnSurface
                        )
                        Text(
                            text = "Set monthly spending limits for Food, Shopping, Bills, etc. to visualize your consumed percentage and remaining headroom.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SophisticatedDarkOnSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(
                            onClick = onNavigateToBudgets,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SophisticatedPrimary,
                                contentColor = SophisticatedOnPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("empty_set_budgets_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Set Category Budgets", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                when (displayMode) {
                    BudgetGaugeDisplayMode.GAUGE -> {
                        // Horizontally scrollable row of visual circular radial gauges
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("budget_gauges_row")
                        ) {
                            items(budgetProgress, key = { it.category }) { item ->
                                CategoryRadialGaugeCard(
                                    item = item,
                                    onClick = { onCategoryClick?.invoke(item.category) }
                                )
                            }
                        }
                    }

                    BudgetGaugeDisplayMode.BAR -> {
                        // Detailed vertical column of category progress bars
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("budget_bars_column")
                        ) {
                            budgetProgress.forEach { item ->
                                CategoryLinearProgressBarItem(
                                    item = item,
                                    onClick = { onCategoryClick?.invoke(item.category) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual Radial Arc Gauge card for a category.
 * Displays circular arc gauge with percentage consumed and remaining amount.
 */
@Composable
fun CategoryRadialGaugeCard(
    item: BudgetProgressItem,
    onClick: () -> Unit
) {
    val tag = item.category.lowercase().replace(" ", "_")
    val isOver = item.isOverBudget
    val pct = item.percentage.toInt()

    // Determine status color: Safe (Teal), Warning (Amber >= 80%), Exceeded (Danger Red > 100%)
    val statusColor = when {
        isOver -> DangerRed
        pct >= 80 -> TagShoppingAmber
        else -> SophisticatedPrimary
    }

    val statusBg = statusColor.copy(alpha = 0.14f)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = SophisticatedDarkSurfaceDim,
        border = BorderStroke(
            1.dp,
            if (isOver) DangerRed.copy(alpha = 0.45f) else SophisticatedDarkBorder
        ),
        modifier = Modifier
            .width(168.dp)
            .clickable { onClick() }
            .testTag("budget_category_gauge_$tag")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Category Header with Icon & Status Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = statusBg,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = getCategoryIcon(item.category),
                            contentDescription = item.category,
                            tint = statusColor,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusBg
                ) {
                    Text(
                        text = if (isOver) "Exceeded" else if (pct >= 80) "Warning" else "Safe",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Category Name
            Text(
                text = item.category,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = SophisticatedDarkOnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Radial Arc Gauge Canvas
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                RadialGaugeCanvas(
                    percentage = item.percentage,
                    gaugeColor = statusColor,
                    modifier = Modifier.fillMaxSize()
                )

                // Inside Arc Gauge: Consumed %
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "$pct%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor,
                        modifier = Modifier.testTag("budget_category_percent_$tag")
                    )
                    Text(
                        text = "used",
                        style = MaterialTheme.typography.labelSmall,
                        color = SophisticatedDarkOnSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }

            // Spent vs Limit
            Text(
                text = "${ExpenseEntity.formatInr(item.spentAmount)} / ${ExpenseEntity.formatInr(item.monthlyLimit)}",
                style = MaterialTheme.typography.labelSmall,
                color = SophisticatedDarkOnSurfaceVariant,
                fontSize = 11.sp,
                maxLines = 1
            )

            // Remaining Headroom Pill
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isOver) DangerRed.copy(alpha = 0.15f) else SuccessGreen.copy(alpha = 0.15f),
                border = BorderStroke(
                    0.8.dp,
                    if (isOver) DangerRed.copy(alpha = 0.3f) else SuccessGreen.copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("budget_category_remaining_$tag")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isOver) Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (isOver) DangerRed else SuccessGreen,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isOver) {
                            "+${ExpenseEntity.formatInr(item.spentAmount - item.monthlyLimit)} over"
                        } else {
                            "${ExpenseEntity.formatInr(item.remainingAmount)} left"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isOver) DangerRed else SuccessGreen,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Custom Canvas drawing a 240-degree radial arc gauge.
 */
@Composable
private fun RadialGaugeCanvas(
    percentage: Float,
    gaugeColor: Color,
    modifier: Modifier = Modifier
) {
    val trackColor = SophisticatedDarkSurfaceVariant

    Canvas(modifier = modifier) {
        val strokeWidth = 8.dp.toPx()
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
        val arcSize = Size(diameter, diameter)

        // Arc starts at 150° and sweeps 240° (ends at 390° = 30°)
        val startAngle = 150f
        val totalSweep = 240f

        // Draw background track
        drawArc(
            color = trackColor,
            startAngle = startAngle,
            sweepAngle = totalSweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Draw consumed progress arc (clamped from 2% to 100% of total sweep)
        val progressFraction = (percentage / 100f).coerceIn(0.02f, 1f)
        val activeSweep = totalSweep * progressFraction

        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(gaugeColor.copy(alpha = 0.8f), gaugeColor),
                center = Offset(size.width / 2, size.height / 2)
            ),
            startAngle = startAngle,
            sweepAngle = activeSweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

/**
 * Linear Progress Bar item for each category showing % consumed vs remaining amount.
 */
@Composable
fun CategoryLinearProgressBarItem(
    item: BudgetProgressItem,
    onClick: () -> Unit
) {
    val tag = item.category.lowercase().replace(" ", "_")
    val isOver = item.isOverBudget
    val pct = item.percentage.toInt()

    val statusColor = when {
        isOver -> DangerRed
        pct >= 80 -> TagShoppingAmber
        else -> SophisticatedPrimary
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SophisticatedDarkSurfaceDim,
        border = BorderStroke(
            1.dp,
            if (isOver) DangerRed.copy(alpha = 0.45f) else SophisticatedDarkBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("budget_category_gauge_$tag")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Category Title + Spent / Limit + % Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = statusColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = getCategoryIcon(item.category),
                                contentDescription = item.category,
                                tint = statusColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = item.category,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = SophisticatedDarkOnSurface
                        )
                        Text(
                            text = "Spent ${ExpenseEntity.formatInr(item.spentAmount)} of ${ExpenseEntity.formatInr(item.monthlyLimit)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SophisticatedDarkOnSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "$pct% consumed",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("budget_category_percent_$tag")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar Track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(SophisticatedDarkSurfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = (item.percentage / 100f).coerceIn(0.02f, 1f))
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(statusColor.copy(alpha = 0.75f), statusColor)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Info Row: Remaining Headroom vs Over-Budget Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isOver) Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (isOver) DangerRed else SuccessGreen,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = if (isOver) "Limit Exceeded" else "Within Budget",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isOver) DangerRed else SuccessGreen
                    )
                }

                Text(
                    text = if (isOver) {
                        "Over by ${ExpenseEntity.formatInr(item.spentAmount - item.monthlyLimit)}"
                    } else {
                        "Remaining: ${ExpenseEntity.formatInr(item.remainingAmount)}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isOver) DangerRed else SophisticatedPrimary,
                    modifier = Modifier.testTag("budget_category_remaining_$tag")
                )
            }
        }
    }
}
