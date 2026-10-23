package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CreditCardSuggestion
import com.example.ui.viewmodel.ExpenseViewModel

@Composable
fun EditCardSuggestionsDialog(
    viewModel: ExpenseViewModel,
    onDismiss: () -> Unit
) {
    val suggestions by viewModel.cardSuggestions.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var cardToEdit by remember { mutableStateOf<CreditCardSuggestion?>(null) }
    var cardToDelete by remember { mutableStateOf<CreditCardSuggestion?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CreditCard,
                        contentDescription = "Credit Card Suggestions",
                        tint = com.example.ui.theme.TagShoppingAmberText,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Edit Card Suggestions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Customize quick credit card suggestions shown in the transaction ledger and add-expense sheet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = com.example.ui.theme.SophisticatedDarkOnSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.SophisticatedPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("add_custom_card_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add New Card", style = MaterialTheme.typography.labelMedium)
                    }

                    TextButton(
                        onClick = { showResetConfirm = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = com.example.ui.theme.SophisticatedSecondary)
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset Defaults", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = com.example.ui.theme.SophisticatedDarkSurface)
                Spacer(modifier = Modifier.height(8.dp))

                if (suggestions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No suggestions saved. Tap 'Add New Card' or 'Reset Defaults'.",
                            style = MaterialTheme.typography.bodySmall,
                            color = com.example.ui.theme.SophisticatedDarkOnSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(suggestions, key = { it.id }) { item ->
                            CardSuggestionItem(
                                item = item,
                                onEdit = { cardToEdit = item },
                                onDelete = { cardToDelete = item }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.SophisticatedPrimary)
            ) {
                Text("Done")
            }
        }
    )

    // Add / Edit Dialog
    if (showAddDialog || cardToEdit != null) {
        val isEditing = cardToEdit != null
        var nameInput by remember { mutableStateOf(cardToEdit?.name ?: "") }
        var bankInput by remember { mutableStateOf(cardToEdit?.issuerBank ?: "") }
        var isRuPayInput by remember { mutableStateOf(cardToEdit?.isRuPay ?: false) }

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                cardToEdit = null
            },
            title = {
                Text(
                    text = if (isEditing) "Edit Card Suggestion" else "Add Card Suggestion",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Card Name") },
                        placeholder = { Text("e.g. HDFC Tata Neu Infinity, Axis Neo") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("card_suggestion_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = com.example.ui.theme.SophisticatedPrimary,
                            unfocusedBorderColor = com.example.ui.theme.SophisticatedDarkSurface
                        )
                    )

                    OutlinedTextField(
                        value = bankInput,
                        onValueChange = { bankInput = it },
                        label = { Text("Issuer Bank (Optional)") },
                        placeholder = { Text("e.g. HDFC Bank, ICICI Bank, SBI") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("card_suggestion_bank_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = com.example.ui.theme.SophisticatedPrimary,
                            unfocusedBorderColor = com.example.ui.theme.SophisticatedDarkSurface
                        )
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Checkbox(
                            checked = isRuPayInput,
                            onCheckedChange = { isRuPayInput = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "RuPay Credit Card",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Enables RuPay UPI badge and quick payment linking",
                                style = MaterialTheme.typography.bodySmall,
                                color = com.example.ui.theme.SophisticatedDarkOnSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nameInput.isNotBlank()) {
                            if (isEditing && cardToEdit != null) {
                                viewModel.updateCardSuggestion(
                                    id = cardToEdit!!.id,
                                    newName = nameInput,
                                    newBank = bankInput,
                                    isRuPay = isRuPayInput
                                )
                            } else {
                                viewModel.addCardSuggestion(
                                    name = nameInput,
                                    issuerBank = bankInput,
                                    isRuPay = isRuPayInput
                                )
                            }
                            showAddDialog = false
                            cardToEdit = null
                        }
                    },
                    enabled = nameInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.SophisticatedPrimary),
                    modifier = Modifier.testTag("save_card_suggestion_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    cardToEdit = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation
    if (cardToDelete != null) {
        AlertDialog(
            onDismissRequest = { cardToDelete = null },
            title = { Text("Delete Suggestion?") },
            text = { Text("Remove \"${cardToDelete?.name}\" from credit card suggestions?") },
            confirmButton = {
                Button(
                    onClick = {
                        cardToDelete?.let { viewModel.deleteCardSuggestion(it.id) }
                        cardToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.DangerRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { cardToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Reset Confirmation
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset to Defaults?") },
            text = { Text("This will reset all credit card suggestions back to the standard curated bank list (including RuPay cards).") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetCardSuggestions()
                        showResetConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.SophisticatedSecondary)
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CardSuggestionItem(
    item: CreditCardSuggestion,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = com.example.ui.theme.SophisticatedDarkSurfaceDim,
        border = BorderStroke(1.dp, com.example.ui.theme.SophisticatedDarkSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.CreditCard,
                    contentDescription = null,
                    tint = if (item.isRuPay) com.example.ui.theme.TagTransportBlueText else com.example.ui.theme.TagShoppingAmberText,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = com.example.ui.theme.SophisticatedDarkOnSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (item.isRuPay) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = com.example.ui.theme.TagTransportBlue.copy(alpha = 0.3f)
                            ) {
                                Text(
                                    text = "RuPay",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = com.example.ui.theme.TagTransportBlueText,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    if (item.issuerBank.isNotBlank()) {
                        Text(
                            text = item.issuerBank,
                            style = MaterialTheme.typography.labelSmall,
                            color = com.example.ui.theme.SophisticatedDarkOnSurfaceVariant
                        )
                    }
                }
            }

            Row {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Card",
                        tint = com.example.ui.theme.SophisticatedDarkOnSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Card",
                        tint = com.example.ui.theme.DangerRed.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
