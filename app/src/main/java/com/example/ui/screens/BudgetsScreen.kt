package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DefaultCategories
import com.example.data.model.ExpenseEntity
import com.example.ui.viewmodel.BudgetProgressItem
import com.example.ui.viewmodel.ExpenseViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BudgetsScreen(
    viewModel: ExpenseViewModel
) {
    val budgetProgress by viewModel.budgetProgressList.collectAsStateWithLifecycle()
    val monthExpenses by viewModel.monthExpenses.collectAsStateWithLifecycle()

    var showBudgetDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<String?>(null) }
    var editingLimit by remember { mutableDoubleStateOf(5000.0) }

    val totalBudget = budgetProgress.sumOf { it.monthlyLimit }
    val totalSpent = monthExpenses.sumOf { it.amount }
    val totalRemaining = (totalBudget - totalSpent).coerceAtLeast(0.0)
    val totalPct = if (totalBudget > 0) ((totalSpent / totalBudget) * 100).toFloat() else 0f

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingCategory = null
                    editingLimit = 5000.0
                    showBudgetDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_budget_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Budget")
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
                        text = "Monthly Budgets",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Summary Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = com.example.ui.theme.SophisticatedDarkSurfaceDim,
                    border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.SophisticatedDarkSurface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "OVERALL MONTHLY BUDGET HEALTH",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = com.example.ui.theme.SophisticatedPrimary
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
                                    color = if (totalSpent > totalBudget && totalBudget > 0) com.example.ui.theme.DangerRed else com.example.ui.theme.SophisticatedDarkOnSurface
                                )
                                Text(
                                    text = "Spent of ${ExpenseEntity.formatInr(totalBudget)} total budget",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = com.example.ui.theme.SophisticatedDarkOnSurfaceVariant
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (totalPct >= 100f) com.example.ui.theme.DangerRedContainer else com.example.ui.theme.SophisticatedPrimaryContainer
                            ) {
                                Text(
                                    text = "${totalPct.toInt()}% Used",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (totalPct >= 100f) com.example.ui.theme.DangerRedOnContainer else com.example.ui.theme.SophisticatedOnPrimaryContainer,
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
                            color = if (totalPct >= 100f) com.example.ui.theme.DangerRed else com.example.ui.theme.SophisticatedPrimary,
                            trackColor = com.example.ui.theme.SophisticatedDarkSurfaceVariant
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
                                color = if (totalPct >= 100f) com.example.ui.theme.DangerRed else com.example.ui.theme.SuccessGreen
                            )
                            Text(
                                text = "${budgetProgress.size} Categories Tracked",
                                style = MaterialTheme.typography.bodySmall,
                                color = com.example.ui.theme.SophisticatedDarkOnSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Category Budgets List
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

                // Presets (+1000, +2000, +5000, +10000)
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
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
}

@Composable
fun BudgetCategoryCard(
    item: BudgetProgressItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val progressVal = (item.percentage / 100f).coerceIn(0f, 1f)
    val progressColor = when {
        item.percentage >= 100f -> com.example.ui.theme.DangerRed
        item.percentage >= 75f -> com.example.ui.theme.AmberGold
        else -> com.example.ui.theme.SuccessGreen
    }

    val (badgeBg, badgeText) = when {
        item.category.contains("Food", true) -> Pair(com.example.ui.theme.TagFoodCoral, com.example.ui.theme.TagFoodCoralText)
        item.category.contains("Travel", true) || item.category.contains("Transport", true) -> Pair(com.example.ui.theme.TagTransportBlue, com.example.ui.theme.TagTransportBlueText)
        item.category.contains("Income", true) || item.category.contains("Salary", true) -> Pair(com.example.ui.theme.TagIncomeMint, com.example.ui.theme.TagIncomeMintText)
        else -> Pair(com.example.ui.theme.TagShoppingAmber, com.example.ui.theme.TagShoppingAmberText)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(18.dp),
        color = com.example.ui.theme.SophisticatedDarkSurfaceDim,
        border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.SophisticatedDarkSurface)
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
                            color = com.example.ui.theme.SophisticatedDarkOnSurface
                        )
                        Text(
                            text = "${ExpenseEntity.formatInr(item.spentAmount)} spent of ${ExpenseEntity.formatInr(item.monthlyLimit)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = com.example.ui.theme.SophisticatedDarkOnSurfaceVariant
                        )
                    }
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp), tint = com.example.ui.theme.SophisticatedPrimary)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp), tint = com.example.ui.theme.DangerRed)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progressVal },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = progressColor,
                trackColor = com.example.ui.theme.SophisticatedDarkSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (item.isOverBudget) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = "Over Budget", tint = com.example.ui.theme.DangerRed, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Over Budget by ${ExpenseEntity.formatInr(item.spentAmount - item.monthlyLimit)} (${item.percentage.toInt()}%)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = com.example.ui.theme.DangerRed
                        )
                    }
                } else {
                    Text(
                        text = "Remaining: ${ExpenseEntity.formatInr(item.remainingAmount)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = com.example.ui.theme.SuccessGreen
                    )
                    Text(
                        text = "${(100 - item.percentage).toInt()}% Left",
                        style = MaterialTheme.typography.labelSmall,
                        color = com.example.ui.theme.SophisticatedDarkOnSurfaceVariant
                    )
                }
            }
        }
    }
}
