package com.example.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.ExtractedTransaction
import com.example.data.model.DefaultCategories
import com.example.data.model.ExpenseEntity
import com.example.data.model.ExpenseTag
import com.example.data.model.PaymentMethod
import com.example.ui.viewmodel.ExpenseViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditExpenseBottomSheet(
    expenseToEdit: ExpenseEntity? = null,
    initialPreFill: ExtractedTransaction? = null,
    viewModel: ExpenseViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember {
        mutableStateOf(
            expenseToEdit?.title ?: initialPreFill?.title ?: ""
        )
    }
    var amountText by remember {
        mutableStateOf(
            expenseToEdit?.amount?.let { if (it % 1 == 0.0) it.toInt().toString() else it.toString() }
                ?: initialPreFill?.amount?.let { if (it % 1 == 0.0) it.toInt().toString() else it.toString() }
                ?: ""
        )
    }
    var selectedCategory by remember {
        mutableStateOf(
            expenseToEdit?.category ?: initialPreFill?.category ?: DefaultCategories.first()
        )
    }
    var selectedPaymentMethod by remember {
        mutableStateOf(
            expenseToEdit?.paymentMethod ?: initialPreFill?.paymentMethod ?: PaymentMethod.UPI.displayName
        )
    }
    var selectedTag by remember {
        mutableStateOf(
            expenseToEdit?.tag ?: initialPreFill?.tag ?: ExpenseTag.PERSONAL.displayName
        )
    }
    var bankName by remember {
        mutableStateOf(
            expenseToEdit?.bankName ?: initialPreFill?.bankName ?: ""
        )
    }
    var creditCardName by remember {
        mutableStateOf(
            expenseToEdit?.creditCardName ?: initialPreFill?.creditCardName ?: ""
        )
    }
    var notes by remember {
        mutableStateOf(
            expenseToEdit?.notes ?: initialPreFill?.notes ?: ""
        )
    }
    var transactionDate by remember {
        mutableLongStateOf(
            expenseToEdit?.date ?: initialPreFill?.date ?: System.currentTimeMillis()
        )
    }

    var isRecurring by remember { mutableStateOf(false) }
    var recurringFrequency by remember { mutableStateOf("MONTHLY") }
    var recurringDayOfMonth by remember {
        val cal = Calendar.getInstance()
        mutableIntStateOf(cal.get(Calendar.DAY_OF_MONTH))
    }
    var recurringDayOfWeek by remember {
        val cal = Calendar.getInstance()
        mutableIntStateOf(cal.get(Calendar.DAY_OF_WEEK))
    }
    var recurringTag by remember { mutableStateOf("Subscription") }
    var autoRecordRecurring by remember { mutableStateOf(true) }

    var isAiCategorizing by remember { mutableStateOf(false) }
    val cardSuggestions by viewModel.cardSuggestions.collectAsStateWithLifecycle()
    var showEditCardSuggestionsDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = com.example.ui.theme.SophisticatedDarkSurfaceDim,
        contentColor = com.example.ui.theme.SophisticatedDarkOnSurface,
        modifier = Modifier.testTag("add_expense_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (expenseToEdit != null) "Edit Expense" else "Add New Expense",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = com.example.ui.theme.SophisticatedDarkOnSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = com.example.ui.theme.SophisticatedDarkOnSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Amount Input Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = com.example.ui.theme.SophisticatedDarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.SophisticatedPrimary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "AMOUNT (INR ₹)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = com.example.ui.theme.SophisticatedPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        placeholder = { Text("0.00", style = MaterialTheme.typography.headlineMedium) },
                        leadingIcon = {
                            Icon(Icons.Default.CurrencyRupee, contentDescription = "INR", tint = com.example.ui.theme.SophisticatedPrimary)
                        },
                        textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("expense_amount_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = com.example.ui.theme.SophisticatedPrimary,
                            unfocusedBorderColor = com.example.ui.theme.SophisticatedDarkSurfaceDim
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick Increment Chips (+100, +500, +1000, +2000)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(100, 500, 1000, 2000).forEach { inc ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = com.example.ui.theme.SophisticatedDarkSurfaceDim,
                                border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.SophisticatedDarkSurface),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        val cur = amountText.toDoubleOrNull() ?: 0.0
                                        amountText = (cur + inc).toInt().toString()
                                    }
                            ) {
                                Text(
                                    text = "+₹$inc",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = com.example.ui.theme.SophisticatedPrimary,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title / Merchant Input + AI Category Predictor
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Description / Merchant") },
                    placeholder = { Text("e.g. Swiggy, Blinkit, Petrol, Amazon") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("expense_title_input")
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (title.isNotBlank()) {
                            isAiCategorizing = true
                            coroutineScope.launch {
                                val amt = amountText.toDoubleOrNull() ?: 0.0
                                val suggested = viewModel.geminiService.suggestCategory(title, amt, notes)
                                selectedCategory = suggested
                                isAiCategorizing = false
                            }
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    if (isAiCategorizing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI Auto Category", tint = MaterialTheme.colorScheme.secondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Date Picker Shortcuts (Today, Yesterday, Custom)
            Text("Date", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isToday = isSameDay(transactionDate, System.currentTimeMillis())
                val isYesterday = isSameDay(transactionDate, System.currentTimeMillis() - 86400000L)

                FilterChip(
                    selected = isToday,
                    onClick = { transactionDate = System.currentTimeMillis() },
                    label = { Text("Today") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = isYesterday,
                    onClick = { transactionDate = System.currentTimeMillis() - 86400000L },
                    label = { Text("Yesterday") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = !isToday && !isYesterday,
                    onClick = {
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = transactionDate
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val newCal = Calendar.getInstance()
                                newCal.set(year, month, dayOfMonth)
                                transactionDate = newCal.timeInMillis
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = "Pick Date", modifier = Modifier.size(16.dp)) },
                    label = {
                        Text(SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(transactionDate)))
                    },
                    modifier = Modifier.weight(1.2f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category Selector Chips
            Text("Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DefaultCategories.forEach { cat ->
                    val isSelected = selectedCategory.equals(cat, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, style = MaterialTheme.typography.bodySmall) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Payment Method Selector
            Text("Payment Method", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("UPI", "Credit Card", "RuPay Credit Card", "Debit Card", "Net Banking", "Cash").forEach { method ->
                    val isSelected = selectedPaymentMethod.equals(method, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedPaymentMethod = method },
                        leadingIcon = {
                            if (method == "Credit Card" || method == "RuPay Credit Card" || method == "Debit Card") {
                                Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(16.dp))
                            } else if (method == "UPI" || method == "Net Banking") {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        },
                        label = { Text(method, style = MaterialTheme.typography.bodySmall) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val isCardPayment = selectedPaymentMethod.equals("Credit Card", ignoreCase = true) ||
                    selectedPaymentMethod.equals("RuPay Credit Card", ignoreCase = true)

            // Credit Card & Bank Name Ledger Details Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = com.example.ui.theme.SophisticatedDarkSurface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isCardPayment) com.example.ui.theme.TagShoppingAmber.copy(alpha = 0.5f)
                    else com.example.ui.theme.SophisticatedDarkSurfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            if (isCardPayment) Icons.Default.CreditCard else Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = if (isCardPayment) com.example.ui.theme.TagShoppingAmberText else com.example.ui.theme.SophisticatedPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (selectedPaymentMethod.equals("RuPay Credit Card", ignoreCase = true)) "RuPay Credit Card & Bank Ledger Details"
                            else if (selectedPaymentMethod.equals("Credit Card", ignoreCase = true)) "Credit Card & Bank Details"
                            else "Bank & Card Ledger Options",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = com.example.ui.theme.SophisticatedDarkOnSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Credit Card Name input
                    OutlinedTextField(
                        value = creditCardName,
                        onValueChange = { creditCardName = it },
                        label = { Text("Credit Card Name") },
                        placeholder = { Text("e.g. HDFC Regalia, ICICI Amazon Pay, SBI BPCL RuPay") },
                        leadingIcon = {
                            Icon(Icons.Default.CreditCard, contentDescription = "Credit Card", tint = com.example.ui.theme.TagShoppingAmberText)
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("expense_credit_card_input")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Quick Credit Card suggestions header with Edit action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Quick Card Suggestions",
                            style = MaterialTheme.typography.labelSmall,
                            color = com.example.ui.theme.SophisticatedDarkOnSurfaceVariant
                        )

                        TextButton(
                            onClick = { showEditCardSuggestionsDialog = true },
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = com.example.ui.theme.SophisticatedPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Edit Suggestions",
                                style = MaterialTheme.typography.labelSmall,
                                color = com.example.ui.theme.SophisticatedPrimary
                            )
                        }
                    }

                    // Quick Credit Card chips
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        cardSuggestions.forEach { card ->
                            val isChipSelected = creditCardName.equals(card.name, ignoreCase = true)
                            FilterChip(
                                selected = isChipSelected,
                                onClick = {
                                    creditCardName = card.name
                                    if (bankName.isBlank() && card.issuerBank.isNotBlank()) {
                                        bankName = card.issuerBank
                                    }
                                    if (card.isRuPay) {
                                        selectedPaymentMethod = "RuPay Credit Card"
                                    } else if (!isCardPayment) {
                                        selectedPaymentMethod = "Credit Card"
                                    }
                                },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(card.name, style = MaterialTheme.typography.labelSmall)
                                        if (card.isRuPay) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Surface(
                                                shape = RoundedCornerShape(3.dp),
                                                color = com.example.ui.theme.TagTransportBlue.copy(alpha = 0.3f)
                                            ) {
                                                Text(
                                                    text = "RuPay",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = com.example.ui.theme.TagTransportBlueText,
                                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bank Name input
                    OutlinedTextField(
                        value = bankName,
                        onValueChange = { bankName = it },
                        label = {
                            Text(
                                if (selectedPaymentMethod.equals("UPI", ignoreCase = true)) "Bank Name (UPI Account)"
                                else if (selectedPaymentMethod.equals("Credit Card", ignoreCase = true)) "Bank Name (Card Issuer)"
                                else "Bank Name"
                            )
                        },
                        placeholder = { Text("e.g. HDFC Bank, SBI, ICICI Bank, Axis Bank") },
                        leadingIcon = {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Bank", tint = com.example.ui.theme.SophisticatedPrimary)
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("expense_bank_name_input")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Quick Bank shortcuts
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("HDFC Bank", "SBI", "ICICI Bank", "Axis Bank", "Kotak Bank", "PNB", "BoB").forEach { bank ->
                            val isBankSelected = bankName.equals(bank, ignoreCase = true)
                            FilterChip(
                                selected = isBankSelected,
                                onClick = { bankName = bank },
                                label = { Text(bank, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tag Selector
            Text("Tag", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Personal", "Work", "Family", "Business").forEach { tag ->
                    val isSelected = selectedTag.equals(tag, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTag = tag },
                        label = { Text(tag) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (Optional)") },
                placeholder = { Text("Add receipt note or remarks...") },
                modifier = Modifier.fillMaxWidth()
            )

            // Recurring Schedule Card (Repeat this transaction)
            if (expenseToEdit == null) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = com.example.ui.theme.SophisticatedDarkSurfaceVariant,
                    border = BorderStroke(1.dp, if (isRecurring) com.example.ui.theme.SophisticatedPrimary.copy(alpha = 0.5f) else com.example.ui.theme.SophisticatedDarkBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recurring_toggle_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
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
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isRecurring) com.example.ui.theme.SophisticatedPrimary.copy(alpha = 0.2f) else com.example.ui.theme.SophisticatedDarkSurfaceDim),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EventRepeat,
                                        contentDescription = "Recurring",
                                        tint = if (isRecurring) com.example.ui.theme.SophisticatedPrimary else com.example.ui.theme.SophisticatedDarkOnSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Repeat this transaction (Recurring)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = com.example.ui.theme.SophisticatedDarkOnSurface
                                    )
                                    Text(
                                        text = "Auto-add to Room DB on scheduled intervals",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = com.example.ui.theme.SophisticatedDarkOnSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = isRecurring,
                                onCheckedChange = { isRecurring = it },
                                modifier = Modifier.testTag("repeat_transaction_switch")
                            )
                        }

                        if (isRecurring) {
                            Spacer(modifier = Modifier.height(12.dp))

                            // Frequency Selector
                            Text(
                                text = "Scheduled Interval / Frequency",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = com.example.ui.theme.SophisticatedPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("MONTHLY" to "Monthly", "WEEKLY" to "Weekly", "YEARLY" to "Yearly", "DAILY" to "Daily").forEach { (f, label) ->
                                    FilterChip(
                                        selected = recurringFrequency == f,
                                        onClick = { recurringFrequency = f },
                                        label = { Text(label, fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Day of Month / Day of Week
                            if (recurringFrequency == "MONTHLY" || recurringFrequency == "YEARLY") {
                                Text(
                                    text = "Repeat on Day of Month",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = com.example.ui.theme.SophisticatedDarkOnSurfaceVariant
                                )
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
                                            color = if (recurringDayOfMonth == day) com.example.ui.theme.SophisticatedPrimary else com.example.ui.theme.SophisticatedDarkSurfaceDim,
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clickable { recurringDayOfMonth = day }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = day.toString(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (recurringDayOfMonth == day) Color.Black else com.example.ui.theme.SophisticatedDarkOnSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            } else if (recurringFrequency == "WEEKLY") {
                                Text(
                                    text = "Repeat on Day of Week",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = com.example.ui.theme.SophisticatedDarkOnSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val days = listOf("Sun" to 1, "Mon" to 2, "Tue" to 3, "Wed" to 4, "Thu" to 5, "Fri" to 6, "Sat" to 7)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    days.forEach { (name, num) ->
                                        FilterChip(
                                            selected = recurringDayOfWeek == num,
                                            onClick = { recurringDayOfWeek = num },
                                            label = { Text(name, fontSize = 11.sp) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Recurring Tag
                            Text(
                                text = "Recurring Type / Tag",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = com.example.ui.theme.SophisticatedDarkOnSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("Subscription", "Rent", "Fixed Bill", "EMI", "Insurance", "Utility").forEach { t ->
                                    FilterChip(
                                        selected = recurringTag == t,
                                        onClick = { recurringTag = t },
                                        label = { Text(t, fontSize = 11.sp) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Auto-Record Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Auto-add to ledger when due",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = com.example.ui.theme.SophisticatedDarkOnSurface
                                    )
                                    Text(
                                        text = "Automatically writes to Room DB expenses table",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = com.example.ui.theme.SophisticatedDarkOnSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = autoRecordRecurring,
                                    onCheckedChange = { autoRecordRecurring = it },
                                    modifier = Modifier.testTag("auto_record_recurring_switch")
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save & Delete Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (expenseToEdit != null) {
                    OutlinedButton(
                        onClick = {
                            viewModel.deleteExpense(expenseToEdit)
                            onDismiss()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete")
                    }
                }

                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        if (title.isNotBlank() && amt > 0) {
                            if (expenseToEdit != null) {
                                viewModel.updateExpense(
                                    expenseToEdit.copy(
                                        title = title.trim(),
                                        amount = amt,
                                        category = selectedCategory,
                                        paymentMethod = selectedPaymentMethod,
                                        tag = selectedTag,
                                        date = transactionDate,
                                        notes = notes.trim(),
                                        bankName = bankName.trim(),
                                        creditCardName = creditCardName.trim()
                                    )
                                )
                            } else {
                                viewModel.addExpense(
                                    title = title.trim(),
                                    amount = amt,
                                    category = selectedCategory,
                                    paymentMethod = selectedPaymentMethod,
                                    date = transactionDate,
                                    notes = notes.trim(),
                                    tag = selectedTag,
                                    bankName = bankName.trim(),
                                    creditCardName = creditCardName.trim()
                                )

                                // If user defined this as a recurring transaction, insert recurring schedule into Room DB
                                if (isRecurring) {
                                    viewModel.addRecurringExpense(
                                        title = title.trim(),
                                        amount = amt,
                                        category = selectedCategory,
                                        frequency = recurringFrequency,
                                        dayOfMonth = recurringDayOfMonth,
                                        dayOfWeek = recurringDayOfWeek,
                                        paymentMethod = selectedPaymentMethod,
                                        bankName = bankName.trim(),
                                        creditCardName = creditCardName.trim(),
                                        tag = recurringTag,
                                        notes = notes.trim(),
                                        autoCreateExpense = autoRecordRecurring
                                    )
                                }
                            }
                            onDismiss()
                        }
                    },
                    enabled = title.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0,
                    modifier = Modifier
                        .weight(if (expenseToEdit != null) 1.5f else 1f)
                        .height(48.dp)
                        .testTag("save_expense_button")
                ) {
                    Text(
                        text = if (expenseToEdit != null) "Update Expense" else if (isRecurring) "Save & Schedule" else "Save Expense",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showEditCardSuggestionsDialog) {
        EditCardSuggestionsDialog(
            viewModel = viewModel,
            onDismiss = { showEditCardSuggestionsDialog = false }
        )
    }
}

private fun isSameDay(time1: Long, time2: Long): Boolean {
    val fmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    return fmt.format(Date(time1)) == fmt.format(Date(time2))
}
