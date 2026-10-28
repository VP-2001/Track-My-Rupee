package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.filled.Share
import com.example.data.model.ExpenseEntity
import com.example.export.ReportExportManager
import com.example.ui.components.CategoryBudgetGaugeCard
import com.example.ui.components.InteractiveAreaLineChart
import com.example.ui.components.InteractivePieDonutChart
import com.example.ui.components.LocalBackupRestoreCard
import com.example.ui.components.MonthlyExpenseReportDialog
import com.example.ui.components.MonthlySpendingHabitsBottomSheet
import com.example.ui.components.RechartsBudgetTrendDashboard
import com.example.ui.components.RechartsCategoryBudgetDashboard
import com.example.ui.components.RechartsDailyTrendAndCategoryDashboard
import com.example.ui.components.RechartsMonthlyTrendLineChart
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SophisticatedDarkBackground
import com.example.ui.theme.SophisticatedDarkBorder
import com.example.ui.theme.SophisticatedDarkOnSurface
import com.example.ui.theme.SophisticatedDarkOnSurfaceVariant
import com.example.ui.theme.SophisticatedDarkSurface
import com.example.ui.theme.SophisticatedDarkSurfaceDim
import com.example.ui.theme.SophisticatedDarkSurfaceVariant
import com.example.ui.theme.SophisticatedOnPrimary
import com.example.ui.theme.SophisticatedOnPrimaryContainer
import com.example.ui.theme.SophisticatedPrimary
import com.example.ui.theme.SophisticatedPrimaryContainer
import com.example.ui.theme.SophisticatedTertiary
import com.example.ui.theme.TagFoodCoral
import com.example.ui.theme.TagFoodCoralText
import com.example.ui.theme.TagIncomeMint
import com.example.ui.theme.TagIncomeMintText
import com.example.ui.theme.TagShoppingAmber
import com.example.ui.theme.TagShoppingAmberText
import com.example.ui.theme.TagTransportBlue
import com.example.ui.theme.TagTransportBlueText
import com.example.ui.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: ExpenseViewModel,
    onNavigateToTransactions: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToAiTools: (String) -> Unit, // "scan", "statement", "voice"
    onNavigateToReports: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onEditExpenseClick: (ExpenseEntity) -> Unit,
    onOpenAccountSheet: () -> Unit = {}
) {
    val context = LocalContext.current
    val monthExpenses by viewModel.monthExpenses.collectAsStateWithLifecycle()
    val allExpenses by viewModel.allExpenses.collectAsStateWithLifecycle(emptyList())
    val categorySummaries by viewModel.categorySummaries.collectAsStateWithLifecycle()
    val budgetProgress by viewModel.budgetProgressList.collectAsStateWithLifecycle()
    val dailyTrend by viewModel.dailyTrend.collectAsStateWithLifecycle()
    val monthlySpendingHabits by viewModel.monthlySpendingHabits.collectAsStateWithLifecycle()
    val sixMonthGrowthSummary by viewModel.sixMonthGrowthSummary.collectAsStateWithLifecycle()
    val oneYearGrowthSummary by viewModel.oneYearGrowthSummary.collectAsStateWithLifecycle()
    val monthlyBudgetTrend6M by viewModel.monthlyBudgetTrend6M.collectAsStateWithLifecycle()
    val monthlyBudgetTrend1Y by viewModel.monthlyBudgetTrend1Y.collectAsStateWithLifecycle()
    val selectedTrendCategory by viewModel.selectedTrendCategory.collectAsStateWithLifecycle()
    val categoryBudgetReport by viewModel.categoryBudgetReport.collectAsStateWithLifecycle()
    val categoryMultiMonthTrends by viewModel.categoryMultiMonthTrends.collectAsStateWithLifecycle()
    val upcomingRecurringBills by viewModel.upcomingRecurringBills.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    val availableMonths = remember(monthExpenses, allExpenses) { viewModel.getAvailableMonths() }
    var selectedBudgetViewMode by remember { mutableStateOf("CATEGORY") } // "CATEGORY" or "OVERALL"
    var showHabitsBottomSheet by remember { mutableStateOf(false) }
    var isDonutChartMode by remember { mutableStateOf(false) }
    var expenseToDelete by remember { mutableStateOf<ExpenseEntity?>(null) }
    var selectedReportMonth by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    val totalMonthSpent = monthExpenses.sumOf { it.amount }
    val totalBudget = if (budgetProgress.isNotEmpty()) budgetProgress.sumOf { it.monthlyLimit } else 40000.0
    val budgetUsedPercent = if (totalBudget > 0) ((totalMonthSpent / totalBudget) * 100).toInt().coerceIn(0, 100) else 0
    val remainingBudget = (totalBudget - totalMonthSpent).coerceAtLeast(0.0)

    val currentMonthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
    val daysInMonth = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH)
    val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    val dailyAverage = if (currentDay > 0) totalMonthSpent / currentDay else 0.0

    val overBudgetCount = budgetProgress.count { it.isOverBudget }

    val userDisplayName = currentUser?.displayName ?: "Personal Ledger"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // 1. Sophisticated Dark Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.clickable { onOpenAccountSheet() }
                ) {
                    com.example.ui.components.ProfileAvatarButton(
                        viewModel = viewModel,
                        onClick = onOpenAccountSheet
                    )
                    Column {
                        Text(
                            text = if (currentUser != null) "Welcome back," else "Guest Session,",
                            style = MaterialTheme.typography.labelSmall,
                            color = SophisticatedDarkOnSurfaceVariant
                        )
                        Text(
                            text = userDisplayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = SophisticatedDarkOnSurface
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SophisticatedDarkSurface)
                            .clickable { onNavigateToTransactions() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = SophisticatedDarkOnSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SophisticatedDarkSurface)
                            .clickable { onNavigateToReports() }
                            .testTag("dashboard_header_backup_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = "Backup, Restore & Security",
                            tint = SophisticatedPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 2. Hero Monthly Spending Card (32dp rounded, #2D2F36, #3D3F47 border)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, SophisticatedDarkBorder)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = "Monthly Spending",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = SophisticatedPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = ExpenseEntity.formatInr(totalMonthSpent),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = SophisticatedDarkOnSurface,
                                letterSpacing = (-0.5).sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SophisticatedPrimary.copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SophisticatedPrimary.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .clickable { showHabitsBottomSheet = true }
                                    .testTag("habits_summary_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Insights,
                                        contentDescription = "Habits",
                                        tint = SophisticatedPrimary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "Habits",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = SophisticatedPrimary
                                    )
                                }
                            }

                            Surface(
                                shape = CircleShape,
                                color = if (overBudgetCount > 0) DangerRed.copy(alpha = 0.2f) else SophisticatedPrimaryContainer
                            ) {
                                Text(
                                    text = if (overBudgetCount > 0) "Alert" else "Safe",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (overBudgetCount > 0) DangerRed else SophisticatedOnPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Spending Habit Quick Glance Bar
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SophisticatedDarkSurfaceDim,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SophisticatedDarkBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showHabitsBottomSheet = true }
                            .testTag("dashboard_spending_habits_glance")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(SophisticatedPrimary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Insights,
                                        contentDescription = null,
                                        tint = SophisticatedPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                Text(
                                    text = if (monthlySpendingHabits.topCategory != null) {
                                        "Top: ${monthlySpendingHabits.topCategory?.category} (${String.format(Locale.getDefault(), "%.0f", monthlySpendingHabits.topCategory?.percentage)}%) • ${if (monthlySpendingHabits.isPreviousMonthAvailable) "${String.format(Locale.getDefault(), "%.1f", kotlin.math.abs(monthlySpendingHabits.percentageChange))}% ${if (monthlySpendingHabits.isSpendingIncreased) "higher" else "lower"}" else "View habits summary"}"
                                    } else {
                                        "Tap to view monthly spending habits summary"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SophisticatedDarkOnSurface,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "View habits",
                                tint = SophisticatedPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Progress bar & used stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Budget: ${ExpenseEntity.formatInr(totalBudget)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SophisticatedDarkOnSurfaceVariant
                        )
                        Text(
                            text = "$budgetUsedPercent% used",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = SophisticatedPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Progress Track
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(SophisticatedDarkSurfaceDim)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = (budgetUsedPercent / 100f).coerceIn(0.02f, 1f))
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (budgetUsedPercent > 90) DangerRed else SophisticatedPrimary
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Buttons: + Add Expense & Voice AI
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onAddExpenseClick,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SophisticatedPrimary,
                                contentColor = SophisticatedOnPrimary
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Add Expense",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(SophisticatedDarkSurfaceVariant)
                                .border(1.dp, SophisticatedDarkBorder, RoundedCornerShape(16.dp))
                                .clickable { onNavigateToAiTools("voice") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = "Voice Input",
                                tint = SophisticatedDarkOnSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(SophisticatedDarkSurfaceVariant)
                                .border(1.dp, SophisticatedDarkBorder, RoundedCornerShape(16.dp))
                                .clickable { onNavigateToAiTools("scan") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.QrCodeScanner,
                                contentDescription = "Scan Receipt",
                                tint = SophisticatedDarkOnSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. Over-budget Alert if active
        if (overBudgetCount > 0) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { onNavigateToBudgets() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Alert",
                            tint = DangerRed,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Budget Limit Exceeded",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = DangerRed
                            )
                            Text(
                                text = "$overBudgetCount categories have exceeded their monthly thresholds.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SophisticatedDarkOnSurface
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Review",
                            tint = SophisticatedDarkOnSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Upcoming Recurring Bills Glance & Automation Card
        if (upcomingRecurringBills.isNotEmpty()) {
            val dueBills = upcomingRecurringBills.filter { it.isDueNow }
            val nextBill = upcomingRecurringBills.first()
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { onNavigateToBudgets() }
                        .testTag("dashboard_recurring_bills_card"),
                    colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurface),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(
                        1.dp,
                        if (dueBills.isNotEmpty()) DangerRed.copy(alpha = 0.5f) else SophisticatedDarkBorder
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (dueBills.isNotEmpty()) DangerRed.copy(alpha = 0.15f) else SophisticatedPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.EventRepeat,
                                contentDescription = "Recurring Bills",
                                tint = if (dueBills.isNotEmpty()) DangerRed else SophisticatedPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (dueBills.isNotEmpty()) {
                                    "${dueBills.size} Recurring Bill${if (dueBills.size > 1) "s" else ""} Due!"
                                } else {
                                    "Next Bill: ${nextBill.title}"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (dueBills.isNotEmpty()) DangerRed else SophisticatedDarkOnSurface
                            )
                            Text(
                                text = if (dueBills.isNotEmpty()) {
                                    "${dueBills.joinToString(", ") { it.title }} (${ExpenseEntity.formatInr(dueBills.sumOf { it.amount })})"
                                } else {
                                    "${ExpenseEntity.formatInr(nextBill.amount)} • ${if (nextBill.daysUntilDue == 0L) "Due Today" else "Due in ${nextBill.daysUntilDue} days"}"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = SophisticatedDarkOnSurfaceVariant
                            )
                        }

                        if (dueBills.isNotEmpty()) {
                            Button(
                                onClick = { viewModel.processDueRecurringExpenses() },
                                colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = Color.White),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("dashboard_pay_due_bills_btn")
                            ) {
                                Text("Add Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "View Bills",
                                tint = SophisticatedPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        } else {
            // Empty state promo to define recurring transactions in Room DB
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { onNavigateToBudgets() }
                        .testTag("dashboard_recurring_empty_card"),
                    colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurfaceDim),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, SophisticatedDarkBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SophisticatedPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.EventRepeat,
                                contentDescription = "Recurring Transactions",
                                tint = SophisticatedPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Automate Recurring Transactions",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = SophisticatedDarkOnSurface
                            )
                            Text(
                                text = "Define monthly rent, subscriptions & EMI to auto-record on schedule",
                                style = MaterialTheme.typography.bodySmall,
                                color = SophisticatedDarkOnSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SophisticatedPrimaryContainer
                        ) {
                            Text(
                                text = "+ Set Up",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SophisticatedOnPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3b. Category Budget Progress Gauges (Consumed % vs. Remaining Headroom)
        item {
            CategoryBudgetGaugeCard(
                budgetProgress = budgetProgress,
                onNavigateToBudgets = onNavigateToBudgets,
                onCategoryClick = { cat ->
                    viewModel.filterByCategory(cat)
                    onNavigateToTransactions()
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        // 4. Recharts Interactive Daily Spending & Category Breakdown Dashboard
        item {
            RechartsDailyTrendAndCategoryDashboard(
                monthExpenses = monthExpenses,
                categorySummaries = categorySummaries,
                onNavigateToAnalytics = onNavigateToAnalytics,
                onNavigateToTransactions = onNavigateToTransactions,
                onCategoryClick = { cat ->
                    viewModel.filterByCategory(cat)
                    onNavigateToTransactions()
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        // 4b. Recharts Monthly Spending Trends vs Budget Dashboard (Category Grouped & Overall)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                // View Selector Header: By Category vs Overall Trends
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Budget Visualization",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SophisticatedDarkOnSurface
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SophisticatedDarkSurfaceVariant,
                        border = BorderStroke(1.dp, SophisticatedDarkBorder)
                    ) {
                        Row(modifier = Modifier.padding(2.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selectedBudgetViewMode == "CATEGORY") SophisticatedPrimary else Color.Transparent)
                                    .clickable { selectedBudgetViewMode = "CATEGORY" }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                    .testTag("budget_view_mode_category")
                            ) {
                                Text(
                                    text = "By Category",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedBudgetViewMode == "CATEGORY") Color.Black else SophisticatedDarkOnSurfaceVariant
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selectedBudgetViewMode == "OVERALL") SophisticatedPrimary else Color.Transparent)
                                    .clickable { selectedBudgetViewMode = "OVERALL" }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                    .testTag("budget_view_mode_overall")
                            ) {
                                Text(
                                    text = "Overall Trend",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedBudgetViewMode == "OVERALL") Color.Black else SophisticatedDarkOnSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (selectedBudgetViewMode == "CATEGORY") {
                    RechartsCategoryBudgetDashboard(
                        report = categoryBudgetReport,
                        multiMonthTrends = categoryMultiMonthTrends,
                        availableMonths = availableMonths,
                        onSelectMonth = { y, m -> viewModel.setSelectedCategoryReportMonth(y, m) },
                        onNavigateToBudgets = onNavigateToBudgets,
                        onFilterTransactionsByCategory = { cat ->
                            viewModel.filterByCategory(cat)
                            onNavigateToTransactions()
                        },
                        modifier = Modifier.testTag("dashboard_recharts_category_budget_dashboard")
                    )
                } else {
                    RechartsBudgetTrendDashboard(
                        summary6M = monthlyBudgetTrend6M,
                        summary1Y = monthlyBudgetTrend1Y,
                        selectedCategory = selectedTrendCategory,
                        onCategorySelected = { cat -> viewModel.setSelectedTrendCategory(cat) },
                        onViewMonthlyReport = { year, month ->
                            selectedReportMonth = Pair(year, month)
                        },
                        onNavigateToBudgets = onNavigateToBudgets,
                        modifier = Modifier.testTag("dashboard_recharts_budget_trend_dashboard")
                    )
                }
            }
        }

        // 5. Recent Expenses / Transactions Section (Scrollable list below Pie Chart)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurfaceVariant),
                border = BorderStroke(1.dp, SophisticatedDarkBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("recent_expenses_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = null,
                                tint = SophisticatedPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Recent Expenses",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SophisticatedDarkOnSurface
                            )
                            if (allExpenses.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = SophisticatedPrimaryContainer
                                ) {
                                    Text(
                                        text = "${allExpenses.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = SophisticatedOnPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "View All",
                            style = MaterialTheme.typography.bodySmall,
                            color = SophisticatedPrimary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clickable { onNavigateToTransactions() }
                                .padding(4.dp)
                                .testTag("view_all_transactions_btn")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (allExpenses.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No transactions yet. Tap '+ Add Expense' or Voice FAB to get started!",
                                color = SophisticatedDarkOnSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        // Scrollable list below the dashboard pie chart displaying recent expense entries
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 340.dp)
                                .testTag("recent_expenses_list"),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(allExpenses, key = { it.id }) { expense ->
                                DashboardTransactionItem(
                                    expense = expense,
                                    onEdit = { onEditExpenseClick(expense) },
                                    onDelete = { expenseToDelete = expense }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. AI Statement Analysis Banner (Matching Design HTML)
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SophisticatedDarkSurfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, SophisticatedDarkBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clickable { onNavigateToAiTools("statement") }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(SophisticatedPrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.TableChart,
                            contentDescription = "AI Converter",
                            tint = SophisticatedOnPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI Bank Statement Analysis",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = SophisticatedDarkOnSurface
                        )
                        Text(
                            text = "Convert HDFC/SBI PDFs to Sheets in seconds",
                            style = MaterialTheme.typography.labelSmall,
                            color = SophisticatedDarkOnSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { onNavigateToAiTools("statement") },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SophisticatedPrimary,
                            contentColor = SophisticatedOnPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Try Now",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 7. Local Encrypted Backup & Restore Card
        item {
            LocalBackupRestoreCard(
                viewModel = viewModel,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                onNavigateToFullSettings = onNavigateToReports
            )
        }

        // 7b. Room DB CSV Ledger Backup Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SophisticatedDarkSurfaceDim,
                border = BorderStroke(1.dp, SophisticatedPrimary.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("dashboard_csv_export_card")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(SophisticatedPrimary.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.TableChart,
                            contentDescription = "CSV Export",
                            tint = SophisticatedPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Export Room DB to CSV",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = SophisticatedDarkOnSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = SophisticatedPrimaryContainer
                            ) {
                                Text(
                                    text = "External Backup",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SophisticatedOnPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "${allExpenses.size} records ready for external spreadsheet backup",
                            style = MaterialTheme.typography.labelSmall,
                            color = SophisticatedDarkOnSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                val csv = viewModel.exportCsvReport("Room_Ledger_Backup", allExpenses)
                                val (success, msg) = ReportExportManager.saveToDownloads(
                                    context,
                                    csv,
                                    "text/csv"
                                )
                                android.widget.Toast.makeText(
                                    context,
                                    if (success) "Exported ${allExpenses.size} records to Downloads: ${csv.name}" else msg,
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SophisticatedPrimary,
                                contentColor = Color.Black
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("dashboard_export_csv_btn")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Download CSV", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "CSV",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = {
                                val csv = viewModel.exportCsvReport("Room_Ledger_Backup", allExpenses)
                                ReportExportManager.shareFile(
                                    context,
                                    csv,
                                    "text/csv",
                                    "Room DB Expense Ledger (CSV Backup)"
                                )
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("dashboard_share_csv_btn")
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Share CSV",
                                tint = SophisticatedPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // 8. Master Dashboard Excel Export Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SophisticatedDarkSurfaceDim,
                border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.SuccessGreen.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(com.example.ui.theme.SuccessGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.TableChart,
                            contentDescription = "Excel Export",
                            tint = com.example.ui.theme.SuccessGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Export Master Excel (.xlsx)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = SophisticatedDarkOnSurface
                        )
                        Text(
                            text = "7-sheet package ready for your custom dashboard",
                            style = MaterialTheme.typography.labelSmall,
                            color = SophisticatedDarkOnSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = {
                                val excel = viewModel.exportFullExcelDashboard()
                                val (success, msg) = ReportExportManager.saveToDownloads(
                                    context,
                                    excel,
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                )
                                android.widget.Toast.makeText(
                                    context,
                                    if (success) "Downloaded to Downloads folder: ${excel.name}" else msg,
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = com.example.ui.theme.SuccessGreen,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Download", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Download",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    if (showHabitsBottomSheet) {
        MonthlySpendingHabitsBottomSheet(
            summary = monthlySpendingHabits,
            onDismiss = { showHabitsBottomSheet = false },
            onNavigateToAnalytics = onNavigateToAnalytics,
            onNavigateToTransactions = onNavigateToTransactions,
            viewModel = viewModel
        )
    }

    if (expenseToDelete != null) {
        val expense = expenseToDelete!!
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = {
                Text(
                    text = "Delete Expense?",
                    fontWeight = FontWeight.Bold,
                    color = SophisticatedDarkOnSurface
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${expense.title}\" (${expense.formattedAmount})?",
                    color = SophisticatedDarkOnSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = expense
                        expenseToDelete = null
                        viewModel.deleteExpense(target)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    modifier = Modifier.testTag("confirm_delete_expense_btn")
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { expenseToDelete = null },
                    modifier = Modifier.testTag("cancel_delete_expense_btn")
                ) {
                    Text("Cancel")
                }
            },
            containerColor = SophisticatedDarkSurfaceVariant,
            modifier = Modifier.testTag("delete_expense_dialog")
        )
    }

    if (selectedReportMonth != null) {
        MonthlyExpenseReportDialog(
            initialYear = selectedReportMonth!!.first,
            initialMonth = selectedReportMonth!!.second,
            viewModel = viewModel,
            onDismissRequest = { selectedReportMonth = null }
        )
    }
}

@Composable
fun DashboardTransactionItem(
    expense: ExpenseEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (badgeBg, badgeText) = when {
        expense.category.contains("Food", true) -> Pair(TagFoodCoral, TagFoodCoralText)
        expense.category.contains("Travel", true) || expense.category.contains("Transport", true) -> Pair(TagTransportBlue, TagTransportBlueText)
        expense.category.contains("Income", true) || expense.category.contains("Salary", true) -> Pair(TagIncomeMint, TagIncomeMintText)
        else -> Pair(TagShoppingAmber, TagShoppingAmberText)
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = SophisticatedDarkSurfaceDim,
        border = androidx.compose.foundation.BorderStroke(1.dp, SophisticatedDarkSurface),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 3.dp)
            .clickable { onEdit() }
            .testTag("dashboard_transaction_${expense.id}")
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(badgeBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getCategoryIcon(expense.category),
                        contentDescription = expense.category,
                        tint = badgeText,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
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
                        style = MaterialTheme.typography.labelSmall,
                        color = SophisticatedDarkOnSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "- ${expense.formattedAmount}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = SophisticatedDarkOnSurface
                    )
                    if (expense.tag.isNotBlank()) {
                        Text(
                            text = expense.tag.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.Bold,
                            color = if (expense.tag.contains("AI", true)) SophisticatedTertiary else SophisticatedDarkOnSurfaceVariant
                        )
                    }
                }

                // Edit Action Button
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("edit_expense_btn_${expense.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit ${expense.title}",
                        tint = SophisticatedPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Delete Action Button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("delete_expense_btn_${expense.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete ${expense.title}",
                        tint = DangerRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

fun getCategoryIcon(category: String): ImageVector {
    return when {
        category.contains("Food", true) -> Icons.AutoMirrored.Filled.ReceiptLong
        category.contains("Groceries", true) -> Icons.Default.AccountBalanceWallet
        category.contains("Shopping", true) -> Icons.Default.CreditCard
        category.contains("Travel", true) || category.contains("Transport", true) -> Icons.AutoMirrored.Filled.TrendingUp
        category.contains("Bills", true) -> Icons.Default.Description
        category.contains("Health", true) -> Icons.Default.Security
        category.contains("Investment", true) || category.contains("Income", true) -> Icons.Default.CurrencyRupee
        else -> Icons.AutoMirrored.Filled.ReceiptLong
    }
}

