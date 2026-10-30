package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DefaultCategories
import com.example.data.model.ExpenseEntity
import com.example.export.ReportExportManager
import com.example.ui.components.EditCardSuggestionsDialog
import com.example.ui.components.ExportDateReportDialog
import com.example.ui.components.ExportTransactionsDialog
import com.example.ui.components.MonthlyExpenseReportDialog
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import android.app.DatePickerDialog
import com.example.ui.viewmodel.DateFilterType
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.ui.viewmodel.SearchFilterScope
import com.example.ui.viewmodel.SortType
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransactionsScreen(
    viewModel: ExpenseViewModel,
    onEditExpenseClick: (ExpenseEntity) -> Unit
) {
    val context = LocalContext.current
    val expenses by viewModel.filteredExpenses.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchScope by viewModel.searchScope.collectAsStateWithLifecycle()
    val allExpenses by viewModel.allExpenses.collectAsStateWithLifecycle(emptyList())
    val categoryFilter by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val paymentFilter by viewModel.selectedPaymentFilter.collectAsStateWithLifecycle()
    val tagFilter by viewModel.selectedTagFilter.collectAsStateWithLifecycle()
    val dateFilter by viewModel.dateFilter.collectAsStateWithLifecycle()
    val sortType by viewModel.sortType.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedExpenseIds.collectAsStateWithLifecycle()

    var showSortMenu by remember { mutableStateOf(false) }
    var isMultiSelectMode by remember { mutableStateOf(false) }
    var showCardSuggestionsDialog by remember { mutableStateOf(false) }
    var showExportTransactionsDialog by remember { mutableStateOf(false) }
    var showMonthlyReportDialog by remember { mutableStateOf(false) }
    var showExportDateReportDialog by remember { mutableStateOf(false) }
    var exportDateReportInitialTab by remember { mutableIntStateOf(0) }
    val customRange by viewModel.customDateRange.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    var showDateRangeFilterDialog by remember { mutableStateOf(false) }

    val topMerchants = remember(allExpenses) {
        allExpenses.map { it.title.trim() }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key }
            .take(6)
    }

    if (showDatePicker) {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selCal = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                val formatted = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(selCal.time)
                viewModel.setSearchScope(SearchFilterScope.DATE)
                viewModel.setSearchQuery(formatted)
                showDatePicker = false
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setOnDismissListener { showDatePicker = false }
            show()
        }
    }

    if (showDateRangeFilterDialog) {
        var tempStartMillis by remember {
            mutableStateOf(customRange?.first ?: (System.currentTimeMillis() - 7L * 24 * 3600 * 1000))
        }
        var tempEndMillis by remember {
            mutableStateOf(customRange?.second ?: System.currentTimeMillis())
        }
        val rangeSdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH) }

        AlertDialog(
            onDismissRequest = { showDateRangeFilterDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Filter by Date Range",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Filter Room FTS results within a specific date range:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Quick Presets:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "Today" to 0,
                            "Last 7 Days" to 7,
                            "Last 30 Days" to 30,
                            "Last 90 Days" to 90
                        ).forEach { (presetLabel, days) ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    val now = System.currentTimeMillis()
                                    if (days == 0) {
                                        val c = Calendar.getInstance().apply {
                                            set(Calendar.HOUR_OF_DAY, 0)
                                            set(Calendar.MINUTE, 0)
                                            set(Calendar.SECOND, 0)
                                            set(Calendar.MILLISECOND, 0)
                                        }
                                        tempStartMillis = c.timeInMillis
                                        tempEndMillis = now
                                    } else {
                                        tempStartMillis = now - days.toLong() * 24 * 3600 * 1000
                                        tempEndMillis = now
                                    }
                                },
                                label = { Text(presetLabel, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val c = Calendar.getInstance().apply { timeInMillis = tempStartMillis }
                                DatePickerDialog(context, { _, y, m, d ->
                                    val newCal = Calendar.getInstance().apply {
                                        set(y, m, d, 0, 0, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    tempStartMillis = newCal.timeInMillis
                                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                            },
                            modifier = Modifier.weight(1f).testTag("pick_range_start_date_button")
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("From", style = MaterialTheme.typography.labelSmall)
                                Text(rangeSdf.format(Date(tempStartMillis)), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                val c = Calendar.getInstance().apply { timeInMillis = tempEndMillis }
                                DatePickerDialog(context, { _, y, m, d ->
                                    val newCal = Calendar.getInstance().apply {
                                        set(y, m, d, 23, 59, 59)
                                        set(Calendar.MILLISECOND, 999)
                                    }
                                    tempEndMillis = newCal.timeInMillis
                                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                            },
                            modifier = Modifier.weight(1f).testTag("pick_range_end_date_button")
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("To", style = MaterialTheme.typography.labelSmall)
                                Text(rangeSdf.format(Date(tempEndMillis)), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val s = minOf(tempStartMillis, tempEndMillis)
                        val e = maxOf(tempStartMillis, tempEndMillis)
                        viewModel.setCustomDateRange(Pair(s, e))
                        showDateRangeFilterDialog = false
                    },
                    modifier = Modifier.testTag("apply_date_range_filter_button")
                ) {
                    Text("Apply Range")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDateRangeFilterDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val isSelectionActive = isMultiSelectMode || selectedIds.isNotEmpty()
    val totalFilteredAmount = expenses.sumOf { it.amount }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .testTag("transactions_screen")
        ) {
            // Top Bar / Multi-select Action Bar
            if (isSelectionActive) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                isMultiSelectMode = false
                                viewModel.clearExpenseSelection()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel selection")
                            }
                            Text(
                                text = "${selectedIds.size} selected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Row {
                            IconButton(onClick = { viewModel.selectAllExpenses() }) {
                                Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                            }
                            IconButton(
                                onClick = {
                                    val count = selectedIds.size
                                    viewModel.deleteSelectedExpenses()
                                    isMultiSelectMode = false
                                    coroutineScope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Deleted $count transactions",
                                            actionLabel = "Undo",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.undoDelete()
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Selected", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            } else {
                // Header with Total and Sort
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Transactions Ledger",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${expenses.size} items • Total: ${ExpenseEntity.formatInr(totalFilteredAmount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showExportTransactionsDialog = true },
                            modifier = Modifier.testTag("export_transactions_button")
                        ) {
                            Icon(
                                Icons.Default.FileDownload,
                                contentDescription = "Export Transactions",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort", tint = MaterialTheme.colorScheme.primary)
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                SortType.entries.forEach { sort ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = sort.label,
                                                fontWeight = if (sortType == sort) FontWeight.Bold else FontWeight.Normal,
                                                color = if (sortType == sort) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        onClick = {
                                            viewModel.setSortType(sort)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { showCardSuggestionsDialog = true },
                            modifier = Modifier.testTag("manage_card_suggestions_button")
                        ) {
                            Icon(
                                Icons.Default.CreditCard,
                                contentDescription = "Manage Credit Card Suggestions",
                                tint = com.example.ui.theme.TagShoppingAmberText
                            )
                        }
                    }
                }
            }

            // ====================================================
            // Search Bar at Top of Transaction List
            // Filters entries by Category, Date, or Merchant Name
            // ====================================================
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    // Header with Title & Room FTS indicator
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Search Transactions",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Room FTS",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (customRange != null) {
                        val rangeFormatter = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.DateRange,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Date: ${rangeFormatter.format(Date(customRange!!.first))} – ${rangeFormatter.format(Date(customRange!!.second))}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        viewModel.setCustomDateRange(null)
                                        viewModel.setDateFilter(DateFilterType.THIS_MONTH)
                                    },
                                    modifier = Modifier
                                        .size(20.dp)
                                        .testTag("clear_date_range_chip_button")
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear range", modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = {
                            Text(
                                text = searchScope.hint,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = if (searchQuery.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { viewModel.setSearchQuery("") },
                                        modifier = Modifier.testTag("clear_search_button")
                                    ) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "Clear search",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { showDateRangeFilterDialog = true },
                                    modifier = Modifier.testTag("pick_search_date_range_button")
                                ) {
                                    Icon(
                                        Icons.Default.DateRange,
                                        contentDescription = "Pick Date Range",
                                        tint = if (customRange != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { showDatePicker = true },
                                    modifier = Modifier.testTag("pick_search_date_button")
                                ) {
                                    Icon(
                                        Icons.Default.CalendarMonth,
                                        contentDescription = "Pick single date",
                                        tint = if (searchScope == SearchFilterScope.DATE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("transaction_search_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick Search Scope Chips: Merchant, Category, Notes, Date, or All
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SearchFilterScope.entries.forEach { scope ->
                            val isSelected = searchScope == scope
                            val scopeIcon = when (scope) {
                                SearchFilterScope.ALL -> Icons.Default.FilterList
                                SearchFilterScope.MERCHANT -> Icons.Default.Storefront
                                SearchFilterScope.CATEGORY -> Icons.Default.Category
                                SearchFilterScope.NOTES -> Icons.Default.Description
                                SearchFilterScope.DATE -> Icons.Default.DateRange
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    viewModel.setSearchScope(scope)
                                    if (scope == SearchFilterScope.DATE && searchQuery.isBlank()) {
                                        showDatePicker = true
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = scopeIcon,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = scope.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.testTag("search_scope_${scope.name.lowercase()}")
                            )
                        }
                    }

                    // Contextual Quick Filters for Category Scope
                    AnimatedVisibility(visible = searchScope == SearchFilterScope.CATEGORY) {
                        Column {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Quick Category Filter:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(DefaultCategories) { cat ->
                                    val isCatActive = searchQuery.equals(cat, ignoreCase = true)
                                    FilterChip(
                                        selected = isCatActive,
                                        onClick = {
                                            if (isCatActive) viewModel.setSearchQuery("")
                                            else viewModel.setSearchQuery(cat)
                                        },
                                        label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        }
                    }

                    // Contextual Quick Filters for Merchant Scope
                    AnimatedVisibility(visible = searchScope == SearchFilterScope.MERCHANT && topMerchants.isNotEmpty()) {
                        Column {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Popular Merchants & Stores:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(topMerchants) { merchant ->
                                    val isMerchantActive = searchQuery.equals(merchant, ignoreCase = true)
                                    FilterChip(
                                        selected = isMerchantActive,
                                        onClick = {
                                            if (isMerchantActive) viewModel.setSearchQuery("")
                                            else viewModel.setSearchQuery(merchant)
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(12.dp))
                                        },
                                        label = { Text(merchant, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        }
                    }

                    // Contextual Quick Filters for Notes Scope
                    AnimatedVisibility(visible = searchScope == SearchFilterScope.NOTES) {
                        Column {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Common Note Keywords:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val commonNotePresets = listOf("Refund", "Bill", "Dinner", "Trip", "Gift", "Shared", "Office", "Rent", "Subscription")
                                items(commonNotePresets) { notePreset ->
                                    val isNoteActive = searchQuery.equals(notePreset, ignoreCase = true)
                                    FilterChip(
                                        selected = isNoteActive,
                                        onClick = {
                                            if (isNoteActive) viewModel.setSearchQuery("")
                                            else viewModel.setSearchQuery(notePreset)
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(12.dp))
                                        },
                                        label = { Text(notePreset, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        }
                    }

                    // Contextual Quick Filters for Date Scope
                    AnimatedVisibility(visible = searchScope == SearchFilterScope.DATE) {
                        Column {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Quick Date Filter:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val currentMonthLabel = SimpleDateFormat("MMM yyyy", Locale.ENGLISH).format(Date())
                                listOf("Today", "Yesterday", currentMonthLabel).forEach { datePreset ->
                                    item {
                                        FilterChip(
                                            selected = searchQuery.equals(datePreset, ignoreCase = true),
                                            onClick = {
                                                if (searchQuery.equals(datePreset, ignoreCase = true)) viewModel.setSearchQuery("")
                                                else viewModel.setSearchQuery(datePreset)
                                            },
                                            label = { Text(datePreset, style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }
                                item {
                                    FilterChip(
                                        selected = customRange != null,
                                        onClick = { showDateRangeFilterDialog = true },
                                        leadingIcon = {
                                            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(12.dp))
                                        },
                                        label = { Text("Pick Date Range...", style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier.testTag("pick_date_range_chip")
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = false,
                                        onClick = { showDatePicker = true },
                                        leadingIcon = {
                                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(12.dp))
                                        },
                                        label = { Text("Pick Single Date...", style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Active Search/Filter Summary Banner
            if (searchQuery.isNotBlank() || categoryFilter != "All" || paymentFilter != "All" || tagFilter != "All" || customRange != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = buildString {
                                    append("${expenses.size} result${if (expenses.size == 1) "" else "s"}")
                                    if (searchQuery.isNotBlank()) {
                                        append(" for \"$searchQuery\"")
                                        if (searchScope != SearchFilterScope.ALL) {
                                            append(" in ${searchScope.label}")
                                        }
                                    }
                                    if (customRange != null) {
                                        append(" (Custom Date Range)")
                                    }
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "Reset All",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable {
                                    viewModel.clearAllFilters()
                                }
                                .padding(4.dp)
                                .testTag("reset_filters_button")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Date Range Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(DateFilterType.entries) { df ->
                    FilterChip(
                        selected = dateFilter == df,
                        onClick = {
                            if (df == DateFilterType.CUSTOM) {
                                showDateRangeFilterDialog = true
                            } else {
                                viewModel.setDateFilter(df)
                            }
                        },
                        label = { Text(df.label, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Payment Method Filter Chips (UPI, Credit Card, etc.)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    FilterChip(
                        selected = paymentFilter == "All" || paymentFilter.isNullOrBlank(),
                        onClick = { viewModel.setPaymentFilter("All") },
                        label = { Text("All Payments") }
                    )
                }
                listOf("UPI", "Credit Card", "RuPay Credit Card", "Debit Card", "Net Banking", "Cash").forEach { method ->
                    item {
                        FilterChip(
                            selected = paymentFilter.equals(method, ignoreCase = true),
                            onClick = {
                                if (paymentFilter.equals(method, ignoreCase = true)) viewModel.setPaymentFilter("All")
                                else viewModel.setPaymentFilter(method)
                            },
                            leadingIcon = {
                                if (method == "Credit Card" || method == "RuPay Credit Card" || method == "Debit Card") {
                                    Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(14.dp))
                                } else if (method == "UPI" || method == "Net Banking") {
                                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            },
                            label = { Text(method) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Category & Tag Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    FilterChip(
                        selected = categoryFilter == "All",
                        onClick = { viewModel.setCategoryFilter("All") },
                        label = { Text("All Categories") }
                    )
                }
                items(DefaultCategories) { cat ->
                    FilterChip(
                        selected = categoryFilter.equals(cat, ignoreCase = true),
                        onClick = {
                            if (categoryFilter.equals(cat, ignoreCase = true)) viewModel.setCategoryFilter("All")
                            else viewModel.setCategoryFilter(cat)
                        },
                        label = { Text(cat) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.width(8.dp))
                }
                listOf("Personal", "Work", "Family", "Business").forEach { tag ->
                    item {
                        FilterChip(
                            selected = tagFilter.equals(tag, ignoreCase = true),
                            onClick = {
                                if (tagFilter.equals(tag, ignoreCase = true)) viewModel.setTagFilter("All")
                                else viewModel.setTagFilter(tag)
                            },
                            label = { Text("Tag: $tag") }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Transactions List
            if (expenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "Empty",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No transactions matching \"$searchQuery\"" else "No matching transactions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "Try searching by another merchant name, category, or date format" else "Try adjusting your search query or filter chips",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        if (searchQuery.isNotBlank() || categoryFilter != "All" || paymentFilter != "All" || tagFilter != "All") {
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedButton(
                                onClick = { viewModel.clearAllFilters() },
                                modifier = Modifier.testTag("empty_state_clear_filters_button")
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Clear Search & Filters")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(expenses, key = { it.id }) { item ->
                        val isSelected = selectedIds.contains(item.id)

                        TransactionLedgerItem(
                            expense = item,
                            isSelectMode = isSelectionActive,
                            isSelected = isSelected,
                            onItemClick = {
                                if (isSelectionActive) {
                                    viewModel.toggleExpenseSelection(item.id)
                                } else {
                                    onEditExpenseClick(item)
                                }
                            },
                            onLongClick = {
                                isMultiSelectMode = true
                                viewModel.toggleExpenseSelection(item.id)
                            },
                            onDelete = {
                                viewModel.deleteExpense(item)
                                coroutineScope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Deleted ${item.title}",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.undoDelete()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )

        if (showCardSuggestionsDialog) {
            EditCardSuggestionsDialog(
                viewModel = viewModel,
                onDismiss = { showCardSuggestionsDialog = false }
            )
        }

        if (showExportTransactionsDialog) {
            ExportTransactionsDialog(
                viewModel = viewModel,
                filteredExpenses = expenses,
                allExpenses = allExpenses,
                onOpenMonthlyReports = {
                    showMonthlyReportDialog = true
                },
                onOpenCustomDateReport = {
                    exportDateReportInitialTab = 1
                    showExportDateReportDialog = true
                },
                onDismissRequest = { showExportTransactionsDialog = false }
            )
        }

        if (showMonthlyReportDialog) {
            val nowCal = Calendar.getInstance()
            MonthlyExpenseReportDialog(
                initialYear = nowCal.get(Calendar.YEAR),
                initialMonth = nowCal.get(Calendar.MONTH) + 1,
                viewModel = viewModel,
                onDismissRequest = { showMonthlyReportDialog = false }
            )
        }

        if (showExportDateReportDialog) {
            ExportDateReportDialog(
                viewModel = viewModel,
                initialTab = exportDateReportInitialTab,
                onDismissRequest = { showExportDateReportDialog = false }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransactionLedgerItem(
    expense: ExpenseEntity,
    isSelectMode: Boolean,
    isSelected: Boolean,
    onItemClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit
) {
    val (badgeBg, badgeText) = when {
        expense.category.contains("Food", true) -> Pair(com.example.ui.theme.TagFoodCoral, com.example.ui.theme.TagFoodCoralText)
        expense.category.contains("Travel", true) || expense.category.contains("Transport", true) -> Pair(com.example.ui.theme.TagTransportBlue, com.example.ui.theme.TagTransportBlueText)
        expense.category.contains("Income", true) || expense.category.contains("Salary", true) -> Pair(com.example.ui.theme.TagIncomeMint, com.example.ui.theme.TagIncomeMintText)
        else -> Pair(com.example.ui.theme.TagShoppingAmber, com.example.ui.theme.TagShoppingAmberText)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() },
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) com.example.ui.theme.SophisticatedPrimaryContainer.copy(alpha = 0.35f) else com.example.ui.theme.SophisticatedDarkSurfaceDim,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) com.example.ui.theme.SophisticatedPrimary else com.example.ui.theme.SophisticatedDarkSurface
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onItemClick() }
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else {
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
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = com.example.ui.theme.SophisticatedDarkOnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Category Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = com.example.ui.theme.SophisticatedDarkSurfaceVariant
                    ) {
                        Text(
                            text = expense.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = com.example.ui.theme.SophisticatedDarkOnSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Payment Method Badge
                    val isRuPay = expense.paymentMethod.contains("RuPay", ignoreCase = true)
                    val isCredit = expense.paymentMethod.equals("Credit Card", ignoreCase = true) || isRuPay
                    val isUpi = expense.paymentMethod.equals("UPI", ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isRuPay) com.example.ui.theme.TagTransportBlue.copy(alpha = 0.25f)
                        else if (isCredit) com.example.ui.theme.TagShoppingAmber.copy(alpha = 0.25f)
                        else if (isUpi) com.example.ui.theme.SophisticatedSecondaryContainer.copy(alpha = 0.6f)
                        else com.example.ui.theme.SophisticatedSecondaryContainer.copy(alpha = 0.4f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            if (isRuPay) {
                                Icon(
                                    Icons.Default.CreditCard,
                                    contentDescription = null,
                                    tint = com.example.ui.theme.TagTransportBlueText,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                            } else if (isCredit) {
                                Icon(
                                    Icons.Default.CreditCard,
                                    contentDescription = null,
                                    tint = com.example.ui.theme.TagShoppingAmberText,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                            } else if (isUpi) {
                                Icon(
                                    Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = com.example.ui.theme.SophisticatedSecondary,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                            }
                            Text(
                                text = expense.paymentMethod,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isRuPay) com.example.ui.theme.TagTransportBlueText
                                else if (isCredit) com.example.ui.theme.TagShoppingAmberText
                                else com.example.ui.theme.SophisticatedSecondary
                            )
                        }
                    }

                    // Credit Card Badge
                    if (expense.creditCardName.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = com.example.ui.theme.TagShoppingAmber.copy(alpha = 0.18f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    Icons.Default.CreditCard,
                                    contentDescription = null,
                                    tint = com.example.ui.theme.TagShoppingAmberText,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = expense.creditCardName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = com.example.ui.theme.TagShoppingAmberText
                                )
                            }
                        }
                    }

                    // Bank Name Badge
                    if (expense.bankName.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = com.example.ui.theme.SophisticatedPrimaryContainer.copy(alpha = 0.35f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = com.example.ui.theme.SophisticatedPrimary,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = expense.bankName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = com.example.ui.theme.SophisticatedPrimary
                                )
                            }
                        }
                    }

                    // Tag Badge
                    if (expense.tag.isNotBlank() && expense.tag != "Personal") {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = com.example.ui.theme.SophisticatedTertiaryContainer.copy(alpha = 0.4f)
                        ) {
                            Text(
                                text = expense.tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = com.example.ui.theme.SophisticatedTertiary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (expense.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = expense.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = com.example.ui.theme.SophisticatedDarkOnSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "- ${expense.formattedAmount}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = com.example.ui.theme.SophisticatedDarkOnSurface
                )
                Text(
                    text = "${expense.formattedDate} • ${expense.formattedTime}",
                    style = MaterialTheme.typography.labelSmall,
                    color = com.example.ui.theme.SophisticatedDarkOnSurfaceVariant
                )
            }
        }
    }
}
