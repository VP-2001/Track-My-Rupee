package com.example

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.ExtractedTransaction
import com.example.data.model.ExpenseEntity
import com.example.ui.components.AccountBottomSheet
import com.example.ui.components.AddEditExpenseBottomSheet
import com.example.ui.components.ProfileAvatarButton
import com.example.ui.components.VoiceExpenseDialog
import com.example.ui.screens.AiChatScreen
import com.example.ui.theme.AmberGold
import com.example.ui.screens.AiToolsScreen
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.AppLockOverlay
import com.example.ui.screens.BudgetsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ExpenseViewModel

enum class NavigationScreen(val label: String, val icon: ImageVector) {
    DASHBOARD("Home", Icons.Default.Dashboard),
    TRANSACTIONS("Ledger", Icons.AutoMirrored.Filled.ReceiptLong),
    ANALYTICS("Charts", Icons.Default.PieChart),
    BUDGETS("Budgets", Icons.Default.AccountBalanceWallet),
    AI_ADVISOR("Advisor", Icons.AutoMirrored.Filled.Chat),
    AI_TOOLS("Tools", Icons.Default.AutoAwesome),
    REPORTS("Backup", Icons.Default.Security)
}


class MainActivity : FragmentActivity() {
    private val viewModel: ExpenseViewModel by viewModels()
    private val targetScreenState = androidx.compose.runtime.mutableStateOf(NavigationScreen.DASHBOARD)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            MyApplicationTheme {
                ExpenseTrackerApp(
                    viewModel = viewModel,
                    initialScreen = targetScreenState.value
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Automatically check and add any due recurring transactions at scheduled intervals
        viewModel.processDueRecurringExpenses()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent?) {
        if (intent?.getStringExtra("navigate_to") == "budgets") {
            targetScreenState.value = NavigationScreen.BUDGETS
        }
    }
}

@Composable
fun ExpenseTrackerApp(
    viewModel: ExpenseViewModel,
    initialScreen: NavigationScreen = NavigationScreen.DASHBOARD
) {
    val isLocked by viewModel.securityManager.isAppLocked.collectAsStateWithLifecycle()

    var currentScreen by remember { mutableStateOf(initialScreen) }

    androidx.compose.runtime.LaunchedEffect(initialScreen) {
        if (initialScreen != NavigationScreen.DASHBOARD) {
            currentScreen = initialScreen
        }
    }

    var aiToolsTab by remember { mutableIntStateOf(0) }

    var isAccountSheetOpen by remember { mutableStateOf(false) }
    var isAddExpenseOpen by remember { mutableStateOf(false) }
    var isVoiceExpenseDialogOpen by remember { mutableStateOf(false) }
    var expenseToEdit by remember { mutableStateOf<ExpenseEntity?>(null) }
    var preFillTransaction by remember { mutableStateOf<ExtractedTransaction?>(null) }

    if (isLocked) {
        AppLockOverlay(
            securityManager = viewModel.securityManager,
            onUnlocked = { viewModel.securityManager.unlockApp() }
        )
    } else {
        BackHandler(enabled = currentScreen != NavigationScreen.DASHBOARD) {
            currentScreen = NavigationScreen.DASHBOARD
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Track My Rupee",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        ProfileAvatarButton(
                            viewModel = viewModel,
                            onClick = { isAccountSheetOpen = true }
                        )
                    }
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceDim,
                    tonalElevation = 0.dp,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    val navItems = listOf(
                        NavigationScreen.DASHBOARD,
                        NavigationScreen.TRANSACTIONS,
                        NavigationScreen.ANALYTICS,
                        NavigationScreen.BUDGETS,
                        NavigationScreen.AI_ADVISOR,
                        NavigationScreen.AI_TOOLS,
                        NavigationScreen.REPORTS
                    )

                    navItems.forEach { screen ->
                        val isSelected = currentScreen == screen
                        NavigationBarItem(
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
            },
            floatingActionButton = {
                if (currentScreen == NavigationScreen.DASHBOARD) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Voice-to-Text Input FAB to quickly add new expenses
                        FloatingActionButton(
                            onClick = { isVoiceExpenseDialogOpen = true },
                            containerColor = AmberGold,
                            contentColor = Color.Black,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.testTag("voice_expense_fab")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice Input to Add Expense",
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = "Voice Add",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Standard manual Add FAB
                        FloatingActionButton(
                            onClick = {
                                expenseToEdit = null
                                preFillTransaction = null
                                isAddExpenseOpen = true
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.testTag("add_expense_fab")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Expense")
                        }
                    }
                } else if (currentScreen == NavigationScreen.TRANSACTIONS) {
                    FloatingActionButton(
                        onClick = {
                            expenseToEdit = null
                            preFillTransaction = null
                            isAddExpenseOpen = true
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.testTag("add_expense_fab")
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
                when (currentScreen) {
                    NavigationScreen.DASHBOARD -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToTransactions = { currentScreen = NavigationScreen.TRANSACTIONS },
                        onNavigateToAnalytics = { currentScreen = NavigationScreen.ANALYTICS },
                        onNavigateToBudgets = { currentScreen = NavigationScreen.BUDGETS },
                        onNavigateToAiTools = { tool ->
                            aiToolsTab = when (tool) {
                                "scan" -> 0
                                "statement" -> 1
                                "voice" -> 2
                                else -> 0
                            }
                            currentScreen = NavigationScreen.AI_TOOLS
                        },
                        onNavigateToReports = { currentScreen = NavigationScreen.REPORTS },
                        onAddExpenseClick = {
                            expenseToEdit = null
                            preFillTransaction = null
                            isAddExpenseOpen = true
                        },
                        onEditExpenseClick = { exp ->
                            expenseToEdit = exp
                            preFillTransaction = null
                            isAddExpenseOpen = true
                        },
                        onOpenAccountSheet = { isAccountSheetOpen = true }
                    )

                    NavigationScreen.TRANSACTIONS -> TransactionsScreen(
                        viewModel = viewModel,
                        onEditExpenseClick = { exp ->
                            expenseToEdit = exp
                            preFillTransaction = null
                            isAddExpenseOpen = true
                        }
                    )

                    NavigationScreen.ANALYTICS -> AnalyticsScreen(
                        viewModel = viewModel
                    )

                    NavigationScreen.BUDGETS -> BudgetsScreen(
                        viewModel = viewModel
                    )

                    NavigationScreen.AI_ADVISOR -> AiChatScreen(
                        viewModel = viewModel
                    )

                    NavigationScreen.AI_TOOLS -> AiToolsScreen(
                        viewModel = viewModel,
                        initialTab = aiToolsTab,
                        onOpenAddExpenseWithPreFill = { extracted ->
                            expenseToEdit = null
                            preFillTransaction = extracted
                            isAddExpenseOpen = true
                        }
                    )

                    NavigationScreen.REPORTS -> ReportsScreen(
                        viewModel = viewModel
                    )
                }

                // Add / Edit Expense BottomSheet
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

                // Voice-to-Text Expense Dialog
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

                // Account Profile & Google Sign-In Bottom Sheet
                AccountBottomSheet(
                    viewModel = viewModel,
                    isOpen = isAccountSheetOpen,
                    onDismiss = { isAccountSheetOpen = false }
                )
            }
        }
    }
}
