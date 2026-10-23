package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategorySummary
import com.example.data.model.ExpenseEntity
import com.example.data.model.MonthlySpendingHabitsSummary
import com.example.ui.theme.AmberGold
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SophisticatedDarkBorder
import com.example.ui.theme.SophisticatedDarkOnSurface
import com.example.ui.theme.SophisticatedDarkOnSurfaceVariant
import com.example.ui.theme.SophisticatedDarkSurface
import com.example.ui.theme.SophisticatedDarkSurfaceDim
import com.example.ui.theme.SophisticatedDarkSurfaceVariant
import com.example.ui.theme.SophisticatedOnPrimary
import com.example.ui.theme.SophisticatedPrimary
import com.example.ui.theme.SophisticatedPrimaryContainer
import com.example.ui.theme.SophisticatedSecondary
import com.example.ui.theme.SophisticatedTertiary
import com.example.ui.theme.SuccessGreen
import java.util.Locale
import kotlin.math.abs

/**
 * Bottom sheet displaying a comprehensive summary of the user's spending habits
 * for the current month, including total expenses, top category spotlight,
 * and comparison to the previous month.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlySpendingHabitsBottomSheet(
    summary: MonthlySpendingHabitsSummary,
    onDismiss: () -> Unit,
    onNavigateToAnalytics: (() -> Unit)? = null,
    onNavigateToTransactions: (() -> Unit)? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SophisticatedDarkSurfaceDim,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = SophisticatedDarkBorder,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        },
        modifier = Modifier.testTag("monthly_spending_habits_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row: Title, Month Badge & Close Button
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
                            .size(38.dp)
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
                            text = "Monthly Spending Habits",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SophisticatedDarkOnSurface
                        )
                        Text(
                            text = summary.currentMonthName,
                            style = MaterialTheme.typography.bodySmall,
                            color = SophisticatedPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = SophisticatedDarkOnSurfaceVariant
                    )
                }
            }

            // 1. Current Month Total Hero Card
            CurrentMonthTotalHeroCard(summary = summary)

            // 2. Previous Month Comparison Card
            MonthOverMonthComparisonCard(summary = summary)

            // 3. Top Category Spotlight Card (Prominently Highlighted)
            if (summary.topCategory != null) {
                TopCategorySpotlightCard(
                    topCategory = summary.topCategory,
                    monthTotal = summary.currentMonthTotal
                )
            }

            // 4. Key Habit Velocity & Daily Burn Metrics
            SpendingVelocityMetricsRow(summary = summary)

            // 5. Category Habits Breakdown List (if more than 1 category)
            if (summary.topCategories.isNotEmpty()) {
                CategoryHabitsBreakdownCard(
                    categories = summary.topCategories,
                    monthTotal = summary.currentMonthTotal
                )
            }

            // 6. Payment Habits & Largest Single Expense Card
            PaymentAndPeakExpenseCard(summary = summary)

            // 7. Smart AI Habit Guidance Tip
            SmartHabitInsightCard(insightMessage = summary.habitInsightMessage)

            // 8. Navigation Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (onNavigateToAnalytics != null) {
                    Button(
                        onClick = {
                            onDismiss()
                            onNavigateToAnalytics()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SophisticatedPrimary,
                            contentColor = SophisticatedOnPrimary
                        )
                    ) {
                        Icon(
                            Icons.Default.PieChart,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("View Analytics", fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }

                if (onNavigateToTransactions != null) {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onNavigateToTransactions()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, SophisticatedDarkBorder)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = SophisticatedDarkOnSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Transactions",
                            color = SophisticatedDarkOnSurface,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * Hero Card showing current month's total spend, transaction count, and budget status.
 */
@Composable
private fun CurrentMonthTotalHeroCard(summary: MonthlySpendingHabitsSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurface),
        border = BorderStroke(1.dp, SophisticatedDarkBorder)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TOTAL SPENT THIS MONTH",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    fontWeight = FontWeight.Bold,
                    color = SophisticatedPrimary
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SophisticatedDarkSurfaceVariant,
                    border = BorderStroke(1.dp, SophisticatedDarkBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = SophisticatedDarkOnSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Day ${summary.currentMonthDaysElapsed} of ${summary.currentMonthDaysTotal}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SophisticatedDarkOnSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Big Amount Display
            Text(
                text = ExpenseEntity.formatInr(summary.currentMonthTotal),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = SophisticatedDarkOnSurface,
                letterSpacing = (-0.5).sp
            )

            // Sub-metrics row: Tx count & Daily Average
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = SophisticatedDarkOnSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${summary.currentMonthTxCount} recorded expenses",
                        style = MaterialTheme.typography.bodySmall,
                        color = SophisticatedDarkOnSurfaceVariant
                    )
                }

                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodySmall,
                    color = SophisticatedDarkBorder
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Speed,
                        contentDescription = null,
                        tint = SophisticatedDarkOnSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${ExpenseEntity.formatInr(summary.currentMonthDailyAverage)}/day avg",
                        style = MaterialTheme.typography.bodySmall,
                        color = SophisticatedDarkOnSurfaceVariant
                    )
                }
            }

            // Optional Budget status bar if configured
            summary.budgetStatus?.let { budget ->
                Spacer(modifier = Modifier.height(2.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Monthly Budget: ${ExpenseEntity.formatInr(budget.totalBudget)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SophisticatedDarkOnSurfaceVariant
                        )
                        Text(
                            text = if (budget.isOverBudget) "Over Limit" else "${ExpenseEntity.formatInr(budget.remainingBudget)} left",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (budget.isOverBudget) DangerRed else SuccessGreen
                        )
                    }

                    val progress = (budget.percentageUsed / 100f).coerceIn(0f, 1f)
                    val animatedProgress by animateFloatAsState(
                        targetValue = progress,
                        animationSpec = tween(600),
                        label = "budget_progress"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(SophisticatedDarkSurfaceDim)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = animatedProgress.coerceAtLeast(0.02f))
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (budget.isOverBudget) DangerRed else SophisticatedPrimary
                                )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Visual Month-over-Month Comparison Card comparing current month vs previous month.
 */
@Composable
private fun MonthOverMonthComparisonCard(summary: MonthlySpendingHabitsSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurface),
        border = BorderStroke(1.dp, SophisticatedDarkBorder)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                    Icon(
                        imageVector = if (summary.isSpendingIncreased) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null,
                        tint = if (summary.isSpendingIncreased) AmberGold else SuccessGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Month-over-Month Trend",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SophisticatedDarkOnSurface
                    )
                }

                // Trend badge indicator
                if (summary.isPreviousMonthAvailable) {
                    val isIncreased = summary.isSpendingIncreased
                    val badgeBg = if (isIncreased) DangerRed.copy(alpha = 0.15f) else SuccessGreen.copy(alpha = 0.15f)
                    val badgeColor = if (isIncreased) AmberGold else SuccessGreen
                    val icon = if (isIncreased) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = badgeBg
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = badgeColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "${String.format(Locale.getDefault(), "%.1f", abs(summary.percentageChange))}% ${if (isIncreased) "More" else "Less"}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SophisticatedPrimary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "First Month",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SophisticatedPrimary
                        )
                    }
                }
            }

            // Comparison Side-by-Side Values
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // This Month Column
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = summary.currentMonthName,
                        style = MaterialTheme.typography.labelSmall,
                        color = SophisticatedPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = ExpenseEntity.formatInr(summary.currentMonthTotal),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SophisticatedDarkOnSurface
                    )
                    Text(
                        text = "${summary.currentMonthTxCount} txs",
                        style = MaterialTheme.typography.labelSmall,
                        color = SophisticatedDarkOnSurfaceVariant
                    )
                }

                // VS Divider
                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(SophisticatedDarkSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "VS",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        color = SophisticatedDarkOnSurfaceVariant
                    )
                }

                // Last Month Column
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = summary.previousMonthName,
                        style = MaterialTheme.typography.labelSmall,
                        color = SophisticatedDarkOnSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = ExpenseEntity.formatInr(summary.previousMonthTotal),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SophisticatedDarkOnSurfaceVariant
                    )
                    Text(
                        text = "${summary.previousMonthTxCount} txs",
                        style = MaterialTheme.typography.labelSmall,
                        color = SophisticatedDarkOnSurfaceVariant
                    )
                }
            }

            // Visual comparative bars
            val maxAmount = maxOf(summary.currentMonthTotal, summary.previousMonthTotal, 1.0)
            val curFraction = (summary.currentMonthTotal / maxAmount).toFloat().coerceIn(0.02f, 1f)
            val prevFraction = (summary.previousMonthTotal / maxAmount).toFloat().coerceIn(0.02f, 1f)

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Current Month Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Current",
                        style = MaterialTheme.typography.labelSmall,
                        color = SophisticatedDarkOnSurfaceVariant,
                        modifier = Modifier.width(52.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(SophisticatedDarkSurfaceDim)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = curFraction)
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(SophisticatedPrimary)
                        )
                    }
                }

                // Previous Month Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Previous",
                        style = MaterialTheme.typography.labelSmall,
                        color = SophisticatedDarkOnSurfaceVariant,
                        modifier = Modifier.width(52.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(SophisticatedDarkSurfaceDim)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = prevFraction)
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(SophisticatedSecondary.copy(alpha = 0.5f))
                        )
                    }
                }
            }

            // Net Difference explanation
            if (summary.isPreviousMonthAvailable) {
                val diffAbs = abs(summary.differenceAmount)
                val diffFormatted = ExpenseEntity.formatInr(diffAbs)
                Text(
                    text = if (summary.isSpendingIncreased) {
                        "Spending is $diffFormatted higher than ${summary.previousMonthName}."
                    } else {
                        "You've spent $diffFormatted less than ${summary.previousMonthName} so far."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (summary.isSpendingIncreased) AmberGold else SuccessGreen,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Top Category Spotlight Card prominently highlighting the #1 expenditure category.
 */
@Composable
private fun TopCategorySpotlightCard(
    topCategory: CategorySummary,
    monthTotal: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurface),
        border = BorderStroke(1.dp, SophisticatedPrimary.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
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
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = AmberGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Top Spending Category",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SophisticatedDarkOnSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SophisticatedPrimary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "#1 Expense",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = SophisticatedPrimary
                    )
                }
            }

            // Category Details Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(SophisticatedPrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getHabitCategoryIcon(topCategory.category),
                            contentDescription = topCategory.category,
                            tint = SophisticatedPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = topCategory.category,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SophisticatedDarkOnSurface
                        )
                        Text(
                            text = "${topCategory.transactionCount} transactions this month",
                            style = MaterialTheme.typography.bodySmall,
                            color = SophisticatedDarkOnSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = ExpenseEntity.formatInr(topCategory.totalAmount),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = SophisticatedPrimary
                    )
                    Text(
                        text = "${String.format(Locale.getDefault(), "%.1f", topCategory.percentage)}% of total",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = SophisticatedDarkOnSurfaceVariant
                    )
                }
            }

            // Category Share Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(SophisticatedDarkSurfaceDim)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = (topCategory.percentage / 100f).coerceIn(0.05f, 1f))
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(SophisticatedPrimary)
                    )
                }

                Text(
                    text = "${topCategory.category} accounts for ${String.format(Locale.getDefault(), "%.1f", topCategory.percentage)}% of your expenses this month.",
                    style = MaterialTheme.typography.labelSmall,
                    color = SophisticatedDarkOnSurfaceVariant
                )
            }
        }
    }
}

/**
 * Grid of Spending Velocity, Daily Average Burn, and Projected Month End.
 */
@Composable
private fun SpendingVelocityMetricsRow(summary: MonthlySpendingHabitsSummary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Daily Average Card
        MetricTileCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Speed,
            iconTint = SophisticatedSecondary,
            title = "Daily Average",
            value = ExpenseEntity.formatInr(summary.currentMonthDailyAverage),
            subtitle = "per elapsed day"
        )

        // Active Spending Days Card
        MetricTileCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.DateRange,
            iconTint = SophisticatedTertiary,
            title = "Active Days",
            value = "${summary.activeSpendingDaysCount} days",
            subtitle = "of ${summary.currentMonthDaysElapsed} elapsed"
        )

        // Projected End Spend Card
        MetricTileCard(
            modifier = Modifier.weight(1f),
            icon = Icons.AutoMirrored.Filled.TrendingFlat,
            iconTint = AmberGold,
            title = "Projected",
            value = ExpenseEntity.formatInr(summary.projectedMonthEndTotal),
            subtitle = "month-end est."
        )
    }
}

@Composable
private fun MetricTileCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    title: String,
    value: String,
    subtitle: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = SophisticatedDarkSurface,
        border = BorderStroke(1.dp, SophisticatedDarkBorder)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = SophisticatedDarkOnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = SophisticatedDarkOnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = SophisticatedDarkOnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Breakdown of top categories with ranked progress indicators.
 */
@Composable
private fun CategoryHabitsBreakdownCard(
    categories: List<CategorySummary>,
    monthTotal: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurface),
        border = BorderStroke(1.dp, SophisticatedDarkBorder)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
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
                    Icon(
                        Icons.Default.Category,
                        contentDescription = null,
                        tint = SophisticatedSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Category Breakdown",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SophisticatedDarkOnSurface
                    )
                }

                Text(
                    text = "${categories.size} categories",
                    style = MaterialTheme.typography.labelSmall,
                    color = SophisticatedDarkOnSurfaceVariant
                )
            }

            // Show top 4 categories
            categories.take(4).forEachIndexed { index, cat ->
                val fraction = if (monthTotal > 0) (cat.totalAmount / monthTotal).toFloat().coerceIn(0.02f, 1f) else 0f
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "#${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SophisticatedDarkOnSurfaceVariant
                            )
                            Text(
                                text = cat.category,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = SophisticatedDarkOnSurface
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = ExpenseEntity.formatInr(cat.totalAmount),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = SophisticatedDarkOnSurface
                            )
                            Text(
                                text = "${String.format(Locale.getDefault(), "%.1f", cat.percentage)}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = SophisticatedPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(SophisticatedDarkSurfaceDim)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = fraction)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(
                                    when (index) {
                                        0 -> SophisticatedPrimary
                                        1 -> SophisticatedSecondary
                                        2 -> SophisticatedTertiary
                                        else -> AmberGold
                                    }
                                )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Payment method breakdown and largest single expense record.
 */
@Composable
private fun PaymentAndPeakExpenseCard(summary: MonthlySpendingHabitsSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurface),
        border = BorderStroke(1.dp, SophisticatedDarkBorder)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Payment Habit Row
            summary.topPaymentMethod?.let { payment ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SophisticatedDarkSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Payments,
                                contentDescription = null,
                                tint = SophisticatedSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Preferred Payment",
                                style = MaterialTheme.typography.labelSmall,
                                color = SophisticatedDarkOnSurfaceVariant
                            )
                            Text(
                                text = payment.paymentMethod,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = SophisticatedDarkOnSurface
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = ExpenseEntity.formatInr(payment.totalAmount),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = SophisticatedDarkOnSurface
                        )
                        Text(
                            text = "${payment.transactionCount} transactions",
                            style = MaterialTheme.typography.labelSmall,
                            color = SophisticatedDarkOnSurfaceVariant
                        )
                    }
                }
            }

            // Divider if both exist
            if (summary.topPaymentMethod != null && summary.highestSingleExpense != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(SophisticatedDarkBorder.copy(alpha = 0.6f))
                )
            }

            // Highest Single Expense Record
            summary.highestSingleExpense?.let { expense ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SophisticatedDarkSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = null,
                                tint = AmberGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(modifier = Modifier.width(180.dp)) {
                            Text(
                                text = "Largest Single Purchase",
                                style = MaterialTheme.typography.labelSmall,
                                color = SophisticatedDarkOnSurfaceVariant
                            )
                            Text(
                                text = expense.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = SophisticatedDarkOnSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${expense.formattedDate} • ${expense.category}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = SophisticatedDarkOnSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = expense.formattedAmount,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AmberGold
                    )
                }
            }
        }
    }
}

/**
 * Smart insight card with personalized tips and summary.
 */
@Composable
private fun SmartHabitInsightCard(insightMessage: String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = SophisticatedDarkSurfaceVariant,
        border = BorderStroke(1.dp, SophisticatedPrimary.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(SophisticatedPrimaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = SophisticatedPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Habit Insights",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = SophisticatedPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = insightMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = SophisticatedDarkOnSurface,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

/**
 * Helper to resolve appropriate Category icons for the bottom sheet.
 */
private fun getHabitCategoryIcon(category: String): ImageVector {
    return when {
        category.contains("Food", true) || category.contains("Dining", true) -> Icons.AutoMirrored.Filled.ReceiptLong
        category.contains("Groceries", true) -> Icons.Default.AccountBalanceWallet
        category.contains("Shopping", true) -> Icons.Default.CreditCard
        category.contains("Travel", true) || category.contains("Transport", true) -> Icons.AutoMirrored.Filled.TrendingUp
        category.contains("Bills", true) || category.contains("Utilities", true) -> Icons.Default.Description
        category.contains("Health", true) || category.contains("Medical", true) -> Icons.Default.Security
        category.contains("Investment", true) || category.contains("Income", true) -> Icons.Default.CurrencyRupee
        else -> Icons.AutoMirrored.Filled.ReceiptLong
    }
}
