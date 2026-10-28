package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import com.example.notification.BudgetAlertThreshold
import com.example.notification.BudgetAlertResult
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DefaultCategories
import com.example.data.model.ExpenseEntity
import com.example.data.model.RecurringExpenseEntity
import com.example.data.model.RecurringFrequency
import com.example.ui.theme.AmberGold
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedContainer
import com.example.ui.theme.DangerRedOnContainer
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
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TagFoodCoral
import com.example.ui.theme.TagFoodCoralText
import com.example.ui.theme.TagIncomeMint
import com.example.ui.theme.TagIncomeMintText
import com.example.ui.theme.TagShoppingAmber
import com.example.ui.theme.TagShoppingAmberText
import com.example.ui.theme.TagTransportBlue
import com.example.ui.theme.TagTransportBlueText
import com.example.ui.viewmodel.BudgetProgressItem
import com.example.ui.viewmodel.ExpenseViewModel
import java.util.Locale

internal data class QuickRecurringPreset(
    val title: String,
    val amount: Double,
    val category: String,
    val frequency: String,
    val tag: String,
    val dayOfMonth: Int = 1
)

private val QUICK_RECURRING_PRESETS = listOf(
    QuickRecurringPreset("House Rent", 15000.0, "Bills & Utilities", "MONTHLY", "Rent", 1),
    QuickRecurringPreset("Netflix Subscription", 649.0, "Entertainment", "MONTHLY", "Subscription", 5),
    QuickRecurringPreset("Wi-Fi Broadband", 999.0, "Bills & Utilities", "MONTHLY", "Fixed Bill", 10),
    QuickRecurringPreset("Gym Membership", 1500.0, "Health & Medical", "MONTHLY", "Subscription", 1),
    QuickRecurringPreset("Mobile Postpaid", 499.0, "Bills & Utilities", "MONTHLY", "Fixed Bill", 15),
    QuickRecurringPreset("Electricity Bill", 2500.0, "Bills & Utilities", "MONTHLY", "Fixed Bill", 8),
    QuickRecurringPreset("Amazon Prime", 1499.0, "Entertainment", "YEARLY", "Subscription", 1)
)

private val RECURRING_TAGS = listOf("Subscription", "Rent", "Fixed Bill", "EMI", "Insurance", "Utility")
private val PAYMENT_OPTIONS = listOf("UPI", "Credit Card", "Debit Card", "Net Banking", "Cash")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BudgetsScreen(
    viewModel: ExpenseViewModel
) {
    val budgetProgress by viewModel.budgetProgressList.collectAsStateWithLifecycle()
    val monthExpenses by viewModel.monthExpenses.collectAsStateWithLifecycle()
    val allRecurringExpenses by viewModel.allRecurringExpenses.collectAsStateWithLifecycle()
    val recurringActionMessage by viewModel.recurringActionMessage.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Category Budgets, 1: Recurring & Subscriptions

    // Category Budget Dialog State
    var showBudgetDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<String?>(null) }
    var editingLimit by remember { mutableDoubleStateOf(5000.0) }

    // Recurring Bill Dialog State
    var showRecurringDialog by remember { mutableStateOf(false) }
    var editingRecurringItem by remember { mutableStateOf<RecurringExpenseEntity?>(null) }
    var prefillPreset by remember { mutableStateOf<QuickRecurringPreset?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val budgetAlertsEnabled by viewModel.budgetAlertsEnabledState.collectAsStateWithLifecycle()
    val budget80AlertEnabled by viewModel.budget80AlertEnabledState.collectAsStateWithLifecycle()
    val budget100AlertEnabled by viewModel.budget100AlertEnabledState.collectAsStateWithLifecycle()
    val lastBudgetAlerts by viewModel.lastBudgetAlertResults.collectAsStateWithLifecycle()

    var hasNotificationPermission by remember {
        mutableStateOf(viewModel.hasBudgetNotificationPermission())
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Notifications enabled! You will receive 80% and 100% budget alerts.")
            }
            viewModel.evaluateBudgetsNow()
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Notification permission was denied. Alerts will not appear in status bar.")
            }
        }
    }

    LaunchedEffect(recurringActionMessage) {
        recurringActionMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearRecurringActionMessage()
        }
    }

    val totalBudget = budgetProgress.sumOf { it.monthlyLimit }
    val totalSpent = monthExpenses.sumOf { it.amount }
    val totalRemaining = (totalBudget - totalSpent).coerceAtLeast(0.0)
    val totalPct = if (totalBudget > 0) ((totalSpent / totalBudget) * 100).toFloat() else 0f

    // Recurring commitment calculations
    val activeRecurringExpenses = allRecurringExpenses.filter { it.isActive }
    val totalMonthlyRecurringCommitment = activeRecurringExpenses.sumOf { item ->
        when (item.frequency.uppercase(Locale.ROOT)) {
            "DAILY" -> item.amount * 30.0
            "WEEKLY" -> item.amount * 4.33
            "MONTHLY" -> item.amount
            "YEARLY" -> item.amount / 12.0
            else -> item.amount
        }
    }
    val dueRecurringCount = activeRecurringExpenses.count { it.isDueNow }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) {
                        editingCategory = null
                        editingLimit = 5000.0
                        showBudgetDialog = true
                    } else {
                        editingRecurringItem = null
                        prefillPreset = null
                        showRecurringDialog = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag(if (selectedTab == 0) "add_budget_fab" else "add_recurring_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (selectedTab == 0) "Add Budget" else "Add Recurring Bill"
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .testTag("budgets_screen"),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Budgets & Fixed Bills",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Tabs: Category Budgets vs Recurring Bills
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("budgets_tab_row")
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.testTag("tab_category_budgets"),
                        text = {
                            Text(
                                text = "Category Budgets",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.testTag("tab_recurring_bills"),
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Recurring Bills",
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (dueRecurringCount > 0) {
                                    Surface(
                                        shape = CircleShape,
                                        color = DangerRed,
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = dueRecurringCount.toString(),
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // TAB 0: CATEGORY BUDGETS
            if (selectedTab == 0) {
                // Summary Card
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = SophisticatedDarkSurfaceDim,
                        border = BorderStroke(1.dp, SophisticatedDarkSurface)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "OVERALL MONTHLY BUDGET HEALTH",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SophisticatedPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = ExpenseEntity.formatInr(totalSpent),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (totalSpent > totalBudget && totalBudget > 0) DangerRed else SophisticatedDarkOnSurface
                                    )
                                    Text(
                                        text = "Spent of ${ExpenseEntity.formatInr(totalBudget)} total budget",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SophisticatedDarkOnSurfaceVariant
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (totalPct >= 100f) DangerRedContainer else SophisticatedPrimaryContainer
                                ) {
                                    Text(
                                        text = "${totalPct.toInt()}% Used",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (totalPct >= 100f) DangerRedOnContainer else SophisticatedOnPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            val prog = (totalPct / 100f).coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { prog },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp)),
                                color = if (totalPct >= 100f) DangerRed else SophisticatedPrimary,
                                trackColor = SophisticatedDarkSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Remaining: ${ExpenseEntity.formatInr(totalRemaining)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (totalPct >= 100f) DangerRed else SuccessGreen
                                )
                                Text(
                                    text = "${budgetProgress.size} Categories Tracked",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SophisticatedDarkOnSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Budget Notification Monitor Card
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .testTag("budget_alert_monitor_card"),
                        shape = RoundedCornerShape(22.dp),
                        color = SophisticatedDarkSurfaceDim,
                        border = BorderStroke(1.dp, SophisticatedDarkBorder)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
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
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (budgetAlertsEnabled) SophisticatedPrimary.copy(alpha = 0.2f) else SophisticatedDarkSurface),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (budgetAlertsEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                            contentDescription = "Alerts",
                                            tint = if (budgetAlertsEnabled) SophisticatedPrimary else SophisticatedDarkOnSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "BUDGET NOTIFICATION MONITOR",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = SophisticatedPrimary
                                        )
                                        Text(
                                            text = if (budgetAlertsEnabled) "Active • Tracking 80% & 100% Limits" else "Spending alerts disabled",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SophisticatedDarkOnSurfaceVariant
                                        )
                                    }
                                }

                                Switch(
                                    checked = budgetAlertsEnabled,
                                    onCheckedChange = { isChecked ->
                                        viewModel.setBudgetAlertsEnabled(isChecked)
                                        if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    },
                                    modifier = Modifier.testTag("budget_alerts_master_switch")
                                )
                            }

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = AmberGold.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth()
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
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = AmberGold,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "Notification permission needed to alert you on lock screen & tray",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = AmberGold
                                            )
                                        }
                                        Button(
                                            onClick = {
                                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = Color.Black),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.testTag("grant_notification_permission_button")
                                        ) {
                                            Text("Grant", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            if (budgetAlertsEnabled) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "80% Warning Alert",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SophisticatedDarkOnSurface
                                        )
                                        Text(
                                            text = "Cautionary alert when spending hits 80% of limit",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SophisticatedDarkOnSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = budget80AlertEnabled,
                                        onCheckedChange = { viewModel.setBudget80AlertEnabled(it) },
                                        modifier = Modifier.testTag("budget_80_alert_switch")
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "100% Exceeded Alert",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SophisticatedDarkOnSurface
                                        )
                                        Text(
                                            text = "Critical high-priority alert when budget is breached",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SophisticatedDarkOnSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = budget100AlertEnabled,
                                        onCheckedChange = { viewModel.setBudget100AlertEnabled(it) },
                                        modifier = Modifier.testTag("budget_100_alert_switch")
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "TEST NOTIFICATION SERVICE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SophisticatedDarkOnSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            val ok = viewModel.sendTestBudgetNotification(BudgetAlertThreshold.WARNING_80, "Food & Dining")
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(if (ok) "Sent 80% Warning notification to tray!" else "Notifications permission required or disabled")
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("test_budget_notification_button"),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                                    ) {
                                        Text("Test 80% Alert", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val ok = viewModel.sendTestBudgetNotification(BudgetAlertThreshold.EXCEEDED_100, "Shopping")
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(if (ok) "Sent 100% Exceeded notification to tray!" else "Notifications permission required or disabled")
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("test_budget_notification_100_button"),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                                    ) {
                                        Text("Test 100% Alert", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            val alerts = viewModel.evaluateBudgetsNow()
                                            coroutineScope.launch {
                                                if (alerts.isNotEmpty()) {
                                                    snackbarHostState.showSnackbar("Triggered ${alerts.size} budget alert(s)!")
                                                } else {
                                                    snackbarHostState.showSnackbar("All budgets evaluated. No new threshold triggers.")
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("evaluate_budgets_now_button"),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Evaluate Now", fontSize = 11.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            viewModel.resetBudgetAlertsHistory()
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Budget alerts history reset. Alerts can trigger again.")
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("reset_budget_alerts_history_button"),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                                    ) {
                                        Text("Reset Alert Flags", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Category Budgets Header
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Category Limits & Adherence",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (budgetProgress.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No category budgets set yet. Tap + to set one!")
                        }
                    }
                } else {
                    items(budgetProgress, key = { it.category }) { item ->
                        BudgetCategoryCard(
                            item = item,
                            onEdit = {
                                editingCategory = item.category
                                editingLimit = item.monthlyLimit
                                showBudgetDialog = true
                            },
                            onDelete = {
                                viewModel.deleteBudget(item.category)
                            }
                        )
                    }
                }
            } else {
                // TAB 1: RECURRING BILLS & SUBSCRIPTIONS
                item {
                    // Recurring Commitment Overview Card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = SophisticatedDarkSurfaceDim,
                        border = BorderStroke(1.dp, SophisticatedDarkSurface)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "MONTHLY RECURRING COMMITMENT",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SophisticatedPrimary
                                )
                                if (dueRecurringCount > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = DangerRedContainer
                                    ) {
                                        Text(
                                            text = "$dueRecurringCount Due Now",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = DangerRedOnContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = ExpenseEntity.formatInr(totalMonthlyRecurringCommitment),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = SophisticatedDarkOnSurface
                            )
                            Text(
                                text = "$activeRecurringExpenses active subscription${if (activeRecurringExpenses.size != 1) "s" else ""} & bills (${allRecurringExpenses.size} total)",
                                style = MaterialTheme.typography.bodySmall,
                                color = SophisticatedDarkOnSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Action button: Process Due Recurring Bills Now
                            Button(
                                onClick = {
                                    viewModel.processDueRecurringExpenses()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("process_due_bills_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (dueRecurringCount > 0) DangerRed else SophisticatedPrimary,
                                    contentColor = SophisticatedOnPrimary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FlashOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (dueRecurringCount > 0) "Process $dueRecurringCount Due Bills Now" else "Check & Process Due Bills",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Quick Add Presets Row
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Quick Setup Presets",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QUICK_RECURRING_PRESETS.forEach { preset ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = SophisticatedDarkSurface,
                                border = BorderStroke(1.dp, SophisticatedDarkBorder),
                                modifier = Modifier
                                    .clickable {
                                        editingRecurringItem = null
                                        prefillPreset = preset
                                        showRecurringDialog = true
                                    }
                                    .testTag("preset_${preset.title.lowercase().replace(" ", "_")}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = SophisticatedPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column {
                                        Text(
                                            text = preset.title,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = SophisticatedDarkOnSurface
                                        )
                                        Text(
                                            text = "${ExpenseEntity.formatInr(preset.amount)} / ${preset.frequency.lowercase().replaceFirstChar { it.uppercase() }}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SophisticatedDarkOnSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Recurring Bills List Header
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Scheduled Bills & Subscriptions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${allRecurringExpenses.size} items",
                            style = MaterialTheme.typography.bodySmall,
                            color = SophisticatedDarkOnSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (allRecurringExpenses.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            shape = RoundedCornerShape(18.dp),
                            color = SophisticatedDarkSurfaceDim,
                            border = BorderStroke(1.dp, SophisticatedDarkSurface)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EventRepeat,
                                    contentDescription = null,
                                    tint = SophisticatedPrimary,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = "No Recurring Expenses Yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SophisticatedDarkOnSurface
                                )
                                Text(
                                    text = "Automate your fixed bills (like Rent, Netflix, Wi-Fi, or Gym). The app checks due dates and records them to your expenses automatically.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SophisticatedDarkOnSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = {
                                        editingRecurringItem = null
                                        prefillPreset = null
                                        showRecurringDialog = true
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Add Your First Recurring Bill")
                                }
                            }
                        }
                    }
                } else {
                    items(allRecurringExpenses, key = { it.id }) { recurring ->
                        RecurringExpenseItemCard(
                            item = recurring,
                            onToggleActive = { isActive ->
                                viewModel.toggleRecurringExpenseActive(recurring.id, isActive)
                            },
                            onPayNow = {
                                viewModel.executeRecurringExpenseNow(recurring.id)
                            },
                            onEdit = {
                                editingRecurringItem = recurring
                                prefillPreset = null
                                showRecurringDialog = true
                            },
                            onDelete = {
                                viewModel.deleteRecurringExpense(recurring.id)
                            }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Budget BottomSheet
    if (showBudgetDialog) {
        ModalBottomSheet(
            onDismissRequest = { showBudgetDialog = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            var selectedCategory by remember { mutableStateOf(editingCategory ?: DefaultCategories.first()) }
            var limitText by remember {
                mutableStateOf(if (editingLimit % 1 == 0.0) editingLimit.toInt().toString() else editingLimit.toString())
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (editingCategory != null) "Edit Budget" else "Set Category Budget",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { showBudgetDialog = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (editingCategory == null) {
                    Text("Select Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DefaultCategories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                } else {
                    Text("Category: $editingCategory", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Text("Monthly Limit (INR ₹)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it },
                    leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = "INR") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Presets (+2000, +5000, +10000, +20000)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(2000, 5000, 10000, 20000).forEach { amt ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { limitText = amt.toString() }
                        ) {
                            Text(
                                text = "₹$amt",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val limit = limitText.toDoubleOrNull() ?: 0.0
                        if (limit > 0) {
                            viewModel.saveBudget(selectedCategory, limit)
                            showBudgetDialog = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Save Budget Limit", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Add / Edit Recurring Expense BottomSheet
    if (showRecurringDialog) {
        AddEditRecurringExpenseBottomSheet(
            editingItem = editingRecurringItem,
            preset = prefillPreset,
            onDismiss = { showRecurringDialog = false },
            onSave = { entity ->
                if (editingRecurringItem != null) {
                    viewModel.updateRecurringExpense(entity)
                } else {
                    viewModel.addRecurringExpense(
                        title = entity.title,
                        amount = entity.amount,
                        category = entity.category,
                        frequency = entity.frequency,
                        dayOfMonth = entity.dayOfMonth,
                        dayOfWeek = entity.dayOfWeek,
                        paymentMethod = entity.paymentMethod,
                        bankName = entity.bankName,
                        creditCardName = entity.creditCardName,
                        notes = entity.notes,
                        tag = entity.tag,
                        autoCreateExpense = entity.autoCreateExpense,
                        startDate = entity.startDate,
                        endDate = entity.endDate
                    )
                }
                showRecurringDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddEditRecurringExpenseBottomSheet(
    editingItem: RecurringExpenseEntity?,
    preset: QuickRecurringPreset?,
    onDismiss: () -> Unit,
    onSave: (RecurringExpenseEntity) -> Unit
) {
    var title by remember {
        mutableStateOf(editingItem?.title ?: preset?.title ?: "")
    }
    var amountText by remember {
        val initialAmt = editingItem?.amount ?: preset?.amount
        mutableStateOf(if (initialAmt != null) {
            if (initialAmt % 1.0 == 0.0) initialAmt.toInt().toString() else initialAmt.toString()
        } else "")
    }
    var category by remember {
        mutableStateOf(editingItem?.category ?: preset?.category ?: "Bills & Utilities")
    }
    var frequency by remember {
        mutableStateOf(editingItem?.frequency ?: preset?.frequency ?: "MONTHLY")
    }
    var dayOfMonth by remember {
        mutableIntStateOf(editingItem?.dayOfMonth ?: preset?.dayOfMonth ?: 1)
    }
    var dayOfWeek by remember {
        mutableIntStateOf(editingItem?.dayOfWeek ?: 2) // Monday
    }
    var paymentMethod by remember {
        mutableStateOf(editingItem?.paymentMethod ?: "UPI")
    }
    var tag by remember {
        mutableStateOf(editingItem?.tag ?: preset?.tag ?: "Subscription")
    }
    var autoCreateExpense by remember {
        mutableStateOf(editingItem?.autoCreateExpense ?: true)
    }
    var notes by remember {
        mutableStateOf(editingItem?.notes ?: "")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (editingItem != null) "Edit Recurring Bill" else "Add Recurring Bill",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }

            // Title
            item {
                Text("Bill / Subscription Title", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("e.g. Netflix, Rent, Wi-Fi") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recurring_title_input")
                )
            }

            // Amount
            item {
                Text("Amount (INR ₹)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = "INR") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text("0.00") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recurring_amount_input")
                )
            }

            // Frequency
            item {
                Text("Frequency", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RecurringFrequency.entries.forEach { freq ->
                        FilterChip(
                            selected = frequency.equals(freq.name, ignoreCase = true),
                            onClick = { frequency = freq.name },
                            label = { Text(freq.label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Schedule Day Detail
            item {
                if (frequency.equals("MONTHLY", ignoreCase = true) || frequency.equals("YEARLY", ignoreCase = true)) {
                    Text("Due Day of Month", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        (1..31).forEach { day ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (dayOfMonth == day) SophisticatedPrimary else SophisticatedDarkSurfaceVariant,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable { dayOfMonth = day }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = day.toString(),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (dayOfMonth == day) SophisticatedOnPrimary else SophisticatedDarkOnSurface
                                    )
                                }
                            }
                        }
                    }
                } else if (frequency.equals("WEEKLY", ignoreCase = true)) {
                    Text("Due Day of Week", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    val days = listOf("Sun" to 1, "Mon" to 2, "Tue" to 3, "Wed" to 4, "Thu" to 5, "Fri" to 6, "Sat" to 7)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        days.forEach { (name, num) ->
                            FilterChip(
                                selected = dayOfWeek == num,
                                onClick = { dayOfWeek = num },
                                label = { Text(name) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Category
            item {
                Text("Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DefaultCategories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }
            }

            // Tag / Commitment Type
            item {
                Text("Commitment Type", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RECURRING_TAGS.forEach { t ->
                        FilterChip(
                            selected = tag == t,
                            onClick = { tag = t },
                            label = { Text(t) }
                        )
                    }
                }
            }

            // Payment Method
            item {
                Text("Payment Method", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PAYMENT_OPTIONS.forEach { method ->
                        FilterChip(
                            selected = paymentMethod == method,
                            onClick = { paymentMethod = method },
                            label = { Text(method) }
                        )
                    }
                }
            }

            // Auto-create switch
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SophisticatedDarkSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-record expense when due",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = SophisticatedDarkOnSurface
                            )
                            Text(
                                text = "Automatically adds to expenses table and advances schedule",
                                style = MaterialTheme.typography.labelSmall,
                                color = SophisticatedDarkOnSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoCreateExpense,
                            onCheckedChange = { autoCreateExpense = it },
                            modifier = Modifier.testTag("auto_create_switch")
                        )
                    }
                }
            }

            // Notes
            item {
                Text("Notes (Optional)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = { Text("Account number, subscriber ID, etc.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Save Button
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
                        if (title.isNotBlank() && parsedAmount > 0.0) {
                            val entity = (editingItem ?: RecurringExpenseEntity(
                                title = title.trim(),
                                amount = parsedAmount,
                                category = category,
                                frequency = frequency,
                                dayOfMonth = dayOfMonth,
                                dayOfWeek = dayOfWeek,
                                paymentMethod = paymentMethod,
                                tag = tag,
                                autoCreateExpense = autoCreateExpense,
                                notes = notes.trim()
                            )).copy(
                                title = title.trim(),
                                amount = parsedAmount,
                                category = category,
                                frequency = frequency,
                                dayOfMonth = dayOfMonth,
                                dayOfWeek = dayOfWeek,
                                paymentMethod = paymentMethod,
                                tag = tag,
                                autoCreateExpense = autoCreateExpense,
                                notes = notes.trim()
                            )
                            onSave(entity)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_recurring_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SophisticatedPrimary,
                        contentColor = SophisticatedOnPrimary
                    )
                ) {
                    Text(
                        text = if (editingItem != null) "Update Recurring Bill" else "Schedule Recurring Bill",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun RecurringExpenseItemCard(
    item: RecurringExpenseEntity,
    onToggleActive: (Boolean) -> Unit,
    onPayNow: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isDue = item.isDueNow
    val daysUntil = item.daysUntilDue

    val frequencyText = when (item.frequency.uppercase(Locale.ROOT)) {
        "DAILY" -> "Every day"
        "WEEKLY" -> "Every week on ${getDayOfWeekName(item.dayOfWeek)}"
        "MONTHLY" -> "Every month on ${getOrdinal(item.dayOfMonth)}"
        "YEARLY" -> "Every year"
        else -> item.frequency
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("recurring_card_${item.id}"),
        shape = RoundedCornerShape(18.dp),
        color = if (item.isActive) SophisticatedDarkSurfaceDim else SophisticatedDarkSurfaceDim.copy(alpha = 0.5f),
        border = BorderStroke(
            1.dp,
            if (isDue && item.isActive) DangerRed.copy(alpha = 0.6f) else SophisticatedDarkSurface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Title, Tag, and Active Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SophisticatedPrimary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = item.tag,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SophisticatedPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SophisticatedDarkOnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = item.isActive,
                        onCheckedChange = onToggleActive,
                        modifier = Modifier.testTag("recurring_switch_${item.id}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Amount and Schedule
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = item.formattedAmount,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = SophisticatedDarkOnSurface
                    )
                    Text(
                        text = "$frequencyText • ${item.category}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SophisticatedDarkOnSurfaceVariant
                    )
                }

                // Next Due Status Badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = when {
                        !item.isActive -> SophisticatedDarkSurfaceVariant
                        isDue -> DangerRedContainer
                        daysUntil <= 3 -> AmberGold.copy(alpha = 0.2f)
                        else -> SophisticatedPrimaryContainer
                    }
                ) {
                    Text(
                        text = when {
                            !item.isActive -> "Paused"
                            isDue -> "⚡ Due Now"
                            daysUntil == 0L -> "Due Today"
                            daysUntil == 1L -> "Due Tomorrow"
                            else -> "In $daysUntil days (${item.formattedNextDue})"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            !item.isActive -> SophisticatedDarkOnSurfaceVariant
                            isDue -> DangerRedOnContainer
                            daysUntil <= 3 -> AmberGold
                            else -> SophisticatedOnPrimaryContainer
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (item.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = item.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = SophisticatedDarkOnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom action row: Pay/Record Now + Edit + Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onPayNow,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("pay_now_button_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Record / Pay Now",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("edit_recurring_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = SophisticatedPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_recurring_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = DangerRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun getOrdinal(n: Int): String {
    return when {
        n in 11..13 -> "${n}th"
        n % 10 == 1 -> "${n}st"
        n % 10 == 2 -> "${n}nd"
        n % 10 == 3 -> "${n}rd"
        else -> "${n}th"
    }
}

private fun getDayOfWeekName(day: Int): String {
    return when (day) {
        1 -> "Sunday"
        2 -> "Monday"
        3 -> "Tuesday"
        4 -> "Wednesday"
        5 -> "Thursday"
        6 -> "Friday"
        7 -> "Saturday"
        else -> "Monday"
    }
}

@Composable
fun BudgetCategoryCard(
    item: BudgetProgressItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val progressVal = (item.percentage / 100f).coerceIn(0f, 1f)
    val progressColor = when {
        item.percentage >= 100f -> DangerRed
        item.percentage >= 75f -> AmberGold
        else -> SuccessGreen
    }

    val (badgeBg, badgeText) = when {
        item.category.contains("Food", true) -> Pair(TagFoodCoral, TagFoodCoralText)
        item.category.contains("Travel", true) || item.category.contains("Transport", true) -> Pair(TagTransportBlue, TagTransportBlueText)
        item.category.contains("Income", true) || item.category.contains("Salary", true) -> Pair(TagIncomeMint, TagIncomeMintText)
        else -> Pair(TagShoppingAmber, TagShoppingAmberText)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(18.dp),
        color = SophisticatedDarkSurfaceDim,
        border = BorderStroke(1.dp, SophisticatedDarkSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(badgeBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(item.category),
                            contentDescription = item.category,
                            tint = badgeText,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = item.category,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = SophisticatedDarkOnSurface
                        )
                        Text(
                            text = "${ExpenseEntity.formatInr(item.spentAmount)} spent of ${ExpenseEntity.formatInr(item.monthlyLimit)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SophisticatedDarkOnSurfaceVariant
                        )
                    }
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp), tint = SophisticatedPrimary)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp), tint = DangerRed)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Alert Threshold Badge
            val (alertBadgeText, alertBadgeBg, alertBadgeColor) = when {
                item.percentage >= 100f -> Triple("🚨 100% LIMIT EXCEEDED (Alert Fired)", DangerRedContainer, DangerRedOnContainer)
                item.percentage >= 80f -> Triple("⚠️ 80% WARNING REACHED (Alert Fired)", AmberGold.copy(alpha = 0.2f), AmberGold)
                else -> Triple("✓ On Track (<80%)", SuccessGreen.copy(alpha = 0.15f), SuccessGreen)
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = alertBadgeBg,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Text(
                    text = alertBadgeText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = alertBadgeColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            LinearProgressIndicator(
                progress = { progressVal },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = progressColor,
                trackColor = SophisticatedDarkSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (item.isOverBudget) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = "Over Budget", tint = DangerRed, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Over Budget by ${ExpenseEntity.formatInr(item.spentAmount - item.monthlyLimit)} (${item.percentage.toInt()}%)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = DangerRed
                        )
                    }
                } else {
                    Text(
                        text = "Remaining: ${ExpenseEntity.formatInr(item.remainingAmount)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = SuccessGreen
                    )
                    Text(
                        text = "${(100 - item.percentage).toInt()}% Left",
                        style = MaterialTheme.typography.labelSmall,
                        color = SophisticatedDarkOnSurfaceVariant
                    )
                }
            }
        }
    }
}
