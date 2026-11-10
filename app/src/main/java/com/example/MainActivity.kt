package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.ExtractedTransaction
import com.example.data.model.ExpenseEntity
import com.example.ui.components.AddEditExpenseBottomSheet
import com.example.ui.components.ProfileAvatarButton
import com.example.ui.components.VoiceExpenseDialog
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.AppLockOverlay
import com.example.ui.screens.AskAiScreen
import com.example.ui.screens.BudgetsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ReceiptScanScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ExpenseViewModel

// Section 4 Information Architecture:
// 5 core tabs: Home, Ledger, Insights, Budgets, Ask AI
enum class NavigationScreen(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Dashboard),
    LEDGER("Ledger", Icons.AutoMirrored.Filled.ReceiptLong),
    INSIGHTS("Insights", Icons.Default.PieChart),
    BUDGETS("Budgets", Icons.Default.AccountBalanceWallet),
    ASK_AI("Ask AI", Icons.Default.AutoAwesome),
    SETTINGS("Settings", Icons.Default.Settings),
    RECEIPT_SCAN("Scan", Icons.Default.QrCodeScanner)
}

class MainActivity : FragmentActivity() {
    private val viewModel: ExpenseViewModel by viewModels()
    private val targetScreenState = androidx.compose.runtime.mutableStateOf(NavigationScreen.HOME)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            val themeMode by viewModel.securityManager.themeMode.collectAsStateWithLifecycle()
            MyApplicationTheme(themeMode = themeMode) {
                ExpenseTrackerApp(
                    viewModel = viewModel,
                    initialScreen = targetScreenState.value
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            // Enforce biometric/PIN verification if app was backgrounded
            viewModel.securityManager.onAppForegrounded()
            // Automatically check and add any due recurring transactions at scheduled intervals
            viewModel.processDueRecurringExpenses()
            // Automatically synchronize Firestore cloud transactions on return to app
            if (viewModel.firestoreSyncRepository.isUserSignedIn() && viewModel.securityManager.isAutoSyncEnabled()) {
                viewModel.syncFirestoreTransactions(silent = true)
                viewModel.startAutomaticCloudSync()
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error in onResume: ${e.message}", e)
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            viewModel.securityManager.onAppBackgrounded()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error in onStop: ${e.message}", e)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getStringExtra("navigate_to") == "budgets") {
            targetScreenState.value = NavigationScreen.BUDGETS
        }
    }
}

@Composable
fun ExpenseTrackerApp(
    viewModel: ExpenseViewModel,
    initialScreen: NavigationScreen = NavigationScreen.HOME
) {
    val securityManager = viewModel.securityManager
    val isLocked by securityManager.isAppLocked.collectAsStateWithLifecycle()
    val hasCompletedOnboarding by securityManager.hasCompletedOnboarding.collectAsStateWithLifecycle()

    var currentScreen by remember { mutableStateOf(initialScreen) }

    LaunchedEffect(initialScreen) {
        if (initialScreen != NavigationScreen.HOME) {
            currentScreen = initialScreen
        }
    }

    var isAddExpenseOpen by remember { mutableStateOf(false) }
    var isVoiceExpenseDialogOpen by remember { mutableStateOf(false) }
    var expenseToEdit by remember { mutableStateOf<ExpenseEntity?>(null) }
    var preFillTransaction by remember { mutableStateOf<ExtractedTransaction?>(null) }

    // 1. Onboarding check (first launch only)
    if (!hasCompletedOnboarding) {
        OnboardingScreen(
            viewModel = viewModel,
            securityManager = securityManager,
            onComplete = {
                currentScreen = NavigationScreen.HOME
            }
        )
        return
    }

    // 2. Lock screen check
    if (isLocked) {
        AppLockOverlay(
            securityManager = securityManager,
            onUnlocked = { securityManager.unlockApp() }
        )
        return
    }

    // 3. Main Navigation
    BackHandler(enabled = currentScreen != NavigationScreen.HOME) {
        currentScreen = NavigationScreen.HOME
    }

    // Five core tabs per Section 4
    val coreNavItems = listOf(
        NavigationScreen.HOME,
        NavigationScreen.LEDGER,
        NavigationScreen.INSIGHTS,
        NavigationScreen.BUDGETS,
        NavigationScreen.ASK_AI
    )

    val isFullScreenOverlay = currentScreen == NavigationScreen.SETTINGS || currentScreen == NavigationScreen.RECEIPT_SCAN

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 600.dp

        if (isTablet) {
            // Tablet Canonical Layout: Left NavigationRail + Right Content Workspace
            Row(modifier = Modifier.fillMaxSize()) {
                if (!isFullScreenOverlay) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surfaceDim,
                        header = {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "₹",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                // Top Add button per Section 4 tablet spec
                                FloatingActionButton(
                                    onClick = {
                                        expenseToEdit = null
                                        preFillTransaction = null
                                        isAddExpenseOpen = true
                                    },
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .size(44.dp)
                                        .testTag("tablet_add_expense_fab")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Expense")
                                }
                            }
                        },
                        modifier = Modifier.testTag("tablet_navigation_rail")
                    ) {
                        Column(
                            modifier = Modifier.fillMaxHeight(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                coreNavItems.forEach { screen ->
                                    val isSelected = currentScreen == screen
                                    NavigationRailItem(
                                        selected = isSelected,
                                        onClick = { currentScreen = screen },
                                        icon = {
                                            Icon(
                                                imageVector = screen.icon,
                                                contentDescription = screen.label,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = screen.label,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        },
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                        )
                                    )
                                }
                            }

                            // Bottom actions on the tablet rail
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                if (securityManager.isLockEnabled()) {
                                    androidx.compose.material3.IconButton(
                                        onClick = { securityManager.lockApp() },
                                        modifier = Modifier.testTag("quick_lock_button")
                                    ) {
                                        Icon(
                                            Icons.Default.Lock,
                                            contentDescription = "Lock App",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                ProfileAvatarButton(
                                    viewModel = viewModel,
                                    onClick = { currentScreen = NavigationScreen.SETTINGS }
                                )
                            }
                        }
                    }
                }

                // Tablet Content Workspace
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    RenderScreen(
                        currentScreen = currentScreen,
                        viewModel = viewModel,
                        onNavigate = { currentScreen = it },
                        onAddExpense = {
                            expenseToEdit = null
                            preFillTransaction = null
                            isAddExpenseOpen = true
                        },
                        onVoiceExpense = { isVoiceExpenseDialogOpen = true },
                        onEditExpense = { exp ->
                            expenseToEdit = exp
                            preFillTransaction = null
                            isAddExpenseOpen = true
                        },
                        onOpenSettings = { currentScreen = NavigationScreen.SETTINGS }
                    )
                }
            }
        } else {
            // Smartphone Layout: Top Bar (only when on secondary screens) + Content + 5-Tab Bottom Bar
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    if (!isFullScreenOverlay) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surfaceDim,
                            tonalElevation = 0.dp,
                            modifier = Modifier.testTag("bottom_nav_bar")
                        ) {
                            coreNavItems.forEach { screen ->
                                val isSelected = currentScreen == screen
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { currentScreen = screen },
                                    alwaysShowLabel = true, // Labels always visible per Section 4
                                    icon = {
                                        Icon(
                                            imageVector = screen.icon,
                                            contentDescription = screen.label,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = screen.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 12.sp, // Minimum 12sp per Section 3 & 4
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                    )
                                )
                            }
                        }
                    }
                },
                // Section 5.2: "No FAB on Home, because the quick actions replace it. Elsewhere, one FAB (Add) on Ledger only."
                floatingActionButton = {
                    if (!isFullScreenOverlay && currentScreen == NavigationScreen.LEDGER) {
                        FloatingActionButton(
                            onClick = {
                                expenseToEdit = null
                                preFillTransaction = null
                                isAddExpenseOpen = true
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.testTag("ledger_add_expense_fab")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Expense")
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    RenderScreen(
                        currentScreen = currentScreen,
                        viewModel = viewModel,
                        onNavigate = { currentScreen = it },
                        onAddExpense = {
                            expenseToEdit = null
                            preFillTransaction = null
                            isAddExpenseOpen = true
                        },
                        onVoiceExpense = { isVoiceExpenseDialogOpen = true },
                        onEditExpense = { exp ->
                            expenseToEdit = exp
                            preFillTransaction = null
                            isAddExpenseOpen = true
                        },
                        onOpenSettings = { currentScreen = NavigationScreen.SETTINGS }
                    )
                }
            }
        }
    }

    // Modal Add / Edit Expense Sheet
    if (isAddExpenseOpen) {
        AddEditExpenseBottomSheet(
            expenseToEdit = expenseToEdit,
            initialPreFill = preFillTransaction,
            viewModel = viewModel,
            onDismiss = {
                isAddExpenseOpen = false
                expenseToEdit = null
                preFillTransaction = null
            }
        )
    }

    // Voice Expense Dialog
    if (isVoiceExpenseDialogOpen) {
        VoiceExpenseDialog(
            viewModel = viewModel,
            onDismiss = { isVoiceExpenseDialogOpen = false },
            onExpenseExtracted = { extracted ->
                expenseToEdit = null
                preFillTransaction = extracted
                isAddExpenseOpen = true
            }
        )
    }
}

@Composable
private fun RenderScreen(
    currentScreen: NavigationScreen,
    viewModel: ExpenseViewModel,
    onNavigate: (NavigationScreen) -> Unit,
    onAddExpense: () -> Unit,
    onVoiceExpense: () -> Unit,
    onEditExpense: (ExpenseEntity) -> Unit,
    onOpenSettings: () -> Unit
) {
    when (currentScreen) {
        NavigationScreen.HOME -> DashboardScreen(
            viewModel = viewModel,
            onNavigateToTransactions = { onNavigate(NavigationScreen.LEDGER) },
            onNavigateToBudgets = { onNavigate(NavigationScreen.BUDGETS) },
            onNavigateToReceiptScan = { onNavigate(NavigationScreen.RECEIPT_SCAN) },
            onAddExpenseClick = onAddExpense,
            onVoiceExpenseClick = onVoiceExpense,
            onEditExpenseClick = onEditExpense,
            onOpenSettingsClick = onOpenSettings
        )

        NavigationScreen.LEDGER -> TransactionsScreen(
            viewModel = viewModel,
            onEditExpenseClick = onEditExpense
        )

        NavigationScreen.INSIGHTS -> AnalyticsScreen(
            viewModel = viewModel,
            onNavigateToTransactions = { onNavigate(NavigationScreen.LEDGER) }
        )

        NavigationScreen.BUDGETS -> BudgetsScreen(
            viewModel = viewModel
        )

        NavigationScreen.ASK_AI -> AskAiScreen(
            viewModel = viewModel,
            initialTab = 0,
            onOpenAddExpenseWithPreFill = { extracted ->
                // Open add expense with AI extracted transaction
                onAddExpense()
            },
            onOpenReceiptScannerScreen = {
                onNavigate(NavigationScreen.RECEIPT_SCAN)
            }
        )

        NavigationScreen.SETTINGS -> SettingsScreen(
            viewModel = viewModel,
            onNavigateBack = { onNavigate(NavigationScreen.HOME) }
        )

        NavigationScreen.RECEIPT_SCAN -> ReceiptScanScreen(
            viewModel = viewModel,
            onNavigateBack = { onNavigate(NavigationScreen.HOME) },
            onNavigateToTransactions = { onNavigate(NavigationScreen.LEDGER) },
            onOpenFullAddExpense = { extracted ->
                onAddExpense()
            }
        )
    }
}
