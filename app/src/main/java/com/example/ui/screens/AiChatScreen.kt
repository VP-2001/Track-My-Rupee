package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SmartToy
import com.example.data.model.CategorySummary
import java.util.Calendar
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.ChatMessage
import com.example.ai.GeminiChatModel
import com.example.ai.MessageRole
import com.example.ui.theme.AmberGold
import com.example.ui.theme.SophisticatedDarkBorder
import com.example.ui.theme.SophisticatedDarkOnSurface
import com.example.ui.theme.SophisticatedDarkOnSurfaceVariant
import com.example.ui.theme.SophisticatedDarkSurface
import com.example.ui.theme.SophisticatedDarkSurfaceVariant
import com.example.ui.theme.SophisticatedPrimary
import com.example.ui.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isChatGenerating.collectAsStateWithLifecycle()
    val selectedModel by viewModel.selectedChatModel.collectAsStateWithLifecycle()
    val allExpenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    val categorySummaries by viewModel.categorySummaries.collectAsStateWithLifecycle()

    val totalSpentThisMonth = remember(allExpenses) {
        val now = Calendar.getInstance()
        val curMonth = now.get(Calendar.MONTH)
        val curYear = now.get(Calendar.YEAR)
        val cal = Calendar.getInstance()
        allExpenses.filter {
            cal.timeInMillis = it.date
            cal.get(Calendar.MONTH) == curMonth && cal.get(Calendar.YEAR) == curYear
        }.sumOf { it.amount }
    }

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current

    var modelMenuExpanded by remember { mutableStateOf(false) }

    // Auto-scroll to bottom on new message
    LaunchedEffect(messages.size, isGenerating) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("ai_chat_screen"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(SophisticatedPrimary, AmberGold)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Financial Advisor",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SophisticatedDarkOnSurface
                            )
                            Text(
                                text = "Personalized Insights with Gemini",
                                style = MaterialTheme.typography.labelSmall,
                                color = SophisticatedDarkOnSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Model Tier Selector Chip
                    Box {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SophisticatedDarkSurfaceVariant,
                            border = BorderStroke(1.dp, SophisticatedDarkBorder),
                            modifier = Modifier
                                .clickable { modelMenuExpanded = true }
                                .testTag("model_selector_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = when (selectedModel) {
                                        GeminiChatModel.FLASH_LITE -> Icons.Default.Bolt
                                        GeminiChatModel.FLASH -> Icons.Default.SmartToy
                                        GeminiChatModel.PRO -> Icons.Default.Psychology
                                    },
                                    contentDescription = null,
                                    tint = when (selectedModel) {
                                        GeminiChatModel.FLASH_LITE -> AmberGold
                                        GeminiChatModel.FLASH -> SophisticatedPrimary
                                        GeminiChatModel.PRO -> Color(0xFFCE93D8)
                                    },
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = selectedModel.badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SophisticatedDarkOnSurface
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Select Model",
                                    tint = SophisticatedDarkOnSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = modelMenuExpanded,
                            onDismissRequest = { modelMenuExpanded = false },
                            modifier = Modifier.background(SophisticatedDarkSurface)
                        ) {
                            GeminiChatModel.values().forEach { model ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = model.displayName,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (model == selectedModel) FontWeight.Bold else FontWeight.Normal,
                                                    color = SophisticatedDarkOnSurface
                                                )
                                                if (model == selectedModel) {
                                                    Icon(
                                                        Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = SophisticatedPrimary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = when (model) {
                                                    GeminiChatModel.FLASH_LITE -> "Fast calculations & quick answers"
                                                    GeminiChatModel.FLASH -> "Balanced general financial advisor"
                                                    GeminiChatModel.PRO -> "Deep multi-year planning & reasoning"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SophisticatedDarkOnSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.setChatModel(model)
                                        modelMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Reset / Clear Chat Button
                    IconButton(
                        onClick = { viewModel.clearChatHistory() },
                        modifier = Modifier.testTag("clear_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear Chat History",
                            tint = SophisticatedDarkOnSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            // Chat Message Thread
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                item(key = "financial_context_card") {
                    AdvisorFinancialContextCard(
                        totalSpentThisMonth = totalSpentThisMonth,
                        allExpensesCount = allExpenses.size,
                        categorySummaries = categorySummaries,
                        onQuickAction = { prompt ->
                            if (!isGenerating) {
                                viewModel.sendChatMessage(prompt)
                            }
                        }
                    )
                }

                items(messages, key = { it.id }) { message ->
                    ChatMessageBubble(message = message)
                }

                if (isGenerating) {
                    item(key = "generating_indicator") {
                        AdvisorTypingBubble()
                    }
                }
            }

            // Quick Prompt Suggestion Chips
            val quickPrompts = listOf(
                "📊 Analyze my spending this month",
                "💡 Where can I cut costs?",
                "🎯 Check my budget limits",
                "🛍️ Top spending categories",
                "📈 50/30/20 budget breakdown"
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickPrompts) { prompt ->
                    AssistChip(
                        onClick = {
                            if (!isGenerating) {
                                viewModel.sendChatMessage(prompt.replace(Regex("^[\\p{So}\\p{Sk}]\\s*"), ""))
                            }
                        },
                        label = {
                            Text(
                                text = prompt,
                                style = MaterialTheme.typography.labelSmall,
                                color = SophisticatedDarkOnSurface
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = SophisticatedDarkSurfaceVariant.copy(alpha = 0.8f)
                        ),
                        border = BorderStroke(1.dp, SophisticatedDarkBorder)
                    )
                }
            }

            // Input Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SophisticatedDarkSurface,
                border = BorderStroke(1.dp, SophisticatedDarkBorder.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_text_field"),
                        placeholder = {
                            Text(
                                text = "Ask your financial advisor...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SophisticatedDarkOnSurfaceVariant
                            )
                        },
                        maxLines = 4,
                        shape = RoundedCornerShape(22.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (inputText.isNotBlank() && !isGenerating) {
                                    val textToSend = inputText
                                    inputText = ""
                                    keyboardController?.hide()
                                    viewModel.sendChatMessage(textToSend)
                                }
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SophisticatedPrimary,
                            unfocusedBorderColor = SophisticatedDarkBorder,
                            focusedContainerColor = SophisticatedDarkSurfaceVariant,
                            unfocusedContainerColor = SophisticatedDarkSurfaceVariant
                        )
                    )

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank() && !isGenerating) {
                                val textToSend = inputText
                                inputText = ""
                                keyboardController?.hide()
                                viewModel.sendChatMessage(textToSend)
                            }
                        },
                        enabled = inputText.isNotBlank() && !isGenerating,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (inputText.isNotBlank() && !isGenerating) SophisticatedPrimary else SophisticatedDarkSurfaceVariant
                            )
                            .testTag("chat_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Message",
                            tint = if (inputText.isNotBlank() && !isGenerating) Color.Black else SophisticatedDarkOnSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageBubble(message: ChatMessage) {
    val isUser = message.role == MessageRole.USER
    val timeFormatted = remember(message.timestamp) {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.timestamp))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            val avatarBrush = if (message.isError) {
                Brush.linearGradient(listOf(MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.errorContainer))
            } else {
                Brush.linearGradient(listOf(SophisticatedPrimary, AmberGold))
            }
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(avatarBrush),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Advisor Avatar",
                    tint = if (message.isError) MaterialTheme.colorScheme.error else Color.Black,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 310.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                color = when {
                    isUser -> SophisticatedPrimary
                    message.isError -> MaterialTheme.colorScheme.errorContainer
                    else -> SophisticatedDarkSurfaceVariant
                },
                border = if (!isUser && !message.isError) BorderStroke(1.dp, SophisticatedDarkBorder) else null,
                modifier = Modifier.testTag(if (isUser) "user_chat_bubble" else "advisor_chat_bubble")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = when {
                            isUser -> Color.Black
                            message.isError -> MaterialTheme.colorScheme.onErrorContainer
                            else -> SophisticatedDarkOnSurface
                        },
                        lineHeight = 21.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (!isUser && message.modelTier != null) {
                    Text(
                        text = message.modelTier.badge,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = SophisticatedPrimary
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = SophisticatedDarkOnSurfaceVariant
                    )
                }
                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = SophisticatedDarkOnSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AdvisorTypingBubble() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(SophisticatedPrimary, AmberGold))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SophisticatedDarkSurfaceVariant,
            border = BorderStroke(1.dp, SophisticatedDarkBorder),
            modifier = Modifier.testTag("typing_indicator")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Advisor is thinking",
                    style = MaterialTheme.typography.labelSmall,
                    color = SophisticatedDarkOnSurfaceVariant
                )
                BouncingDot(delayMs = 0)
                BouncingDot(delayMs = 150)
                BouncingDot(delayMs = 300)
            }
        }
    }
}

@Composable
fun BouncingDot(delayMs: Int) {
    val offset = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMs.toLong())
        offset.animateTo(
            targetValue = -5f,
            animationSpec = infiniteRepeatable(
                animation = tween(400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(0, offset.value.dp.roundToPx()) }
            .size(5.dp)
            .clip(CircleShape)
            .background(SophisticatedPrimary)
    )
}

@Composable
fun AdvisorFinancialContextCard(
    totalSpentThisMonth: Double,
    allExpensesCount: Int,
    categorySummaries: List<CategorySummary>,
    onQuickAction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ai_advisor_context_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurfaceVariant),
        border = BorderStroke(1.dp, SophisticatedDarkBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
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
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50))
                    )
                    Text(
                        text = "Live Room DB Context",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SophisticatedDarkOnSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SophisticatedPrimary.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, SophisticatedPrimary.copy(alpha = 0.3f)),
                    modifier = Modifier.clickable { expanded = !expanded }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (expanded) "Hide Details" else "View Details",
                            style = MaterialTheme.typography.labelSmall,
                            color = SophisticatedPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                            tint = SophisticatedPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Key metrics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Metric 1: Spent this month
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = SophisticatedDarkSurface,
                    border = BorderStroke(1.dp, SophisticatedDarkBorder.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Month Total",
                            style = MaterialTheme.typography.labelSmall,
                            color = SophisticatedDarkOnSurfaceVariant
                        )
                        Text(
                            text = "₹${"%.0f".format(totalSpentThisMonth)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SophisticatedPrimary
                        )
                    }
                }

                // Metric 2: Total logged transactions
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = SophisticatedDarkSurface,
                    border = BorderStroke(1.dp, SophisticatedDarkBorder.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Logged Entries",
                            style = MaterialTheme.typography.labelSmall,
                            color = SophisticatedDarkOnSurfaceVariant
                        )
                        Text(
                            text = "$allExpensesCount txns",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AmberGold
                        )
                    }
                }

                // Metric 3: Top Category
                val topCat = categorySummaries.firstOrNull()
                Surface(
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(12.dp),
                    color = SophisticatedDarkSurface,
                    border = BorderStroke(1.dp, SophisticatedDarkBorder.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Top Category",
                            style = MaterialTheme.typography.labelSmall,
                            color = SophisticatedDarkOnSurfaceVariant
                        )
                        Text(
                            text = topCat?.category ?: "None",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = SophisticatedDarkOnSurface,
                            maxLines = 1
                        )
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Text(
                        text = "Category Summaries (Room DB):",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = SophisticatedDarkOnSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (categorySummaries.isEmpty()) {
                        Text(
                            text = "No expenses logged yet. Add your first expense to see instant category analysis.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SophisticatedDarkOnSurfaceVariant
                        )
                    } else {
                        categorySummaries.take(5).forEach { cat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = cat.category,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SophisticatedDarkOnSurface,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = SophisticatedDarkSurface
                                    ) {
                                        Text(
                                            text = "${cat.transactionCount} txns",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            color = SophisticatedDarkOnSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "₹${"%.2f".format(cat.totalAmount)} (${"%.1f".format(cat.percentage)}%)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SophisticatedPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AssistChip(
                            onClick = { onQuickAction("Analyze my top category spending") },
                            label = { Text("Analyze Top Category", style = MaterialTheme.typography.labelSmall) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = SophisticatedDarkSurface
                            ),
                            border = BorderStroke(1.dp, SophisticatedDarkBorder)
                        )
                        AssistChip(
                            onClick = { onQuickAction("Where can I cut costs this month?") },
                            label = { Text("Cost-Cutting Ideas", style = MaterialTheme.typography.labelSmall) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = SophisticatedDarkSurface
                            ),
                            border = BorderStroke(1.dp, SophisticatedDarkBorder)
                        )
                    }
                }
            }
        }
    }
}

