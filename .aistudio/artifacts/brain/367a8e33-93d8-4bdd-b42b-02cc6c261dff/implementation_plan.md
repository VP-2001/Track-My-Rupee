# Gemini Personal Finance Advisor Chatbot

A multi-turn conversational AI Financial Advisor integrated into the Expense Tracker app, maintaining full conversation history, scrollable chat thread, contextual financial awareness (recent transactions, categories, and budgets), system instructions defining advisor roles, and dynamic Gemini model selection (`gemini-3.8-flash`, `gemini-3.1-flash-lite`, and `gemini-4-argon`).

## User Review & Critical Decisions

> [!IMPORTANT]
> The following key decisions were confirmed during the interactive interview:
> - **Advisor Persona & Role**: Personal Finance Advisor specialized in analyzing expenses, detecting spending leaks, reviewing budget adherence, and recommending actionable money-saving strategies.
> - **Screen Placement & Access**: Dedicated first-class screen accessible directly from bottom navigation / drawer (`NavigationScreen.AI_CHAT`), with easy switching to other views.
> - **Data & Context Injection**: Dynamic context injection supplying the chatbot with the user's latest expense summaries, top spend categories, active budgets, and recent transactions without requiring manual typing.
> - **Model Tiering**:
>   - `gemini-3.8-flash`: Default model for general financial reasoning, deep budget advice, and conversation.
>   - `gemini-3.1-flash-lite`: Fast mode for quick transaction checks and instant calculations.
>   - `gemini-4-argon`: Deep reasoning mode for complex annual portfolio comparisons, debt payoffs, and detailed financial plans.

---

## 1. Overview & Core Concept

- **What It Does**: Provides users with an on-device personal financial advisor powered by Google's Gemini models. Users can ask questions like "How much did I spend on Food & Dining this month?", "Where can I cut expenses to save $200?", "Am I on track with my shopping budget?", or "Summarize my spending habits over the last 30 days."
- **Contextual Intelligence**: Whenever the user sends a message, the advisor's system instruction and prompt context are automatically augmented with live aggregations from Room DB (total monthly spend, breakdown by category, top merchants, budget alerts).
- **Multi-Turn Chat Experience**: Messages are displayed in a clean, scrollable bubble thread with distinct user and AI visual styling, markdown formatting support, copy-to-clipboard, suggested quick-prompt chips (e.g., "Analyze my budget", "Top spending categories", "How can I save money?"), typing indicators, and error resilience.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Accessing the Chat**: User taps the new "AI Advisor" item in navigation.
2. **Suggested Prompt Chips**: On launch or empty state, the chat offers horizontal quick-action chips:
   - *"Analyze my spending this month"*
   - *"Where did most of my money go?"*
   - *"Am I over budget in any category?"*
   - *"Give me 3 tips to save money"*
3. **Sending Messages**: User types a question in the bottom text field and taps Send (or presses Enter). The query is appended immediately to the thread.
4. **Animated Response**: A subtle typing/pulsing indicator appears while Gemini processes the query. The response streams or renders with formatted currency, bulleted advice, and bold highlights.
5. **Model Selector**: An unobtrusive top bar chip allows switching between:
   - `⚡ Fast (gemini-3.1-flash-lite)`
   - `✨ Balanced (gemini-3.8-flash)` [Default]
   - `🧠 Deep (gemini-4-argon)`
6. **Clearing / Resetting Chat**: A top-bar action to clear chat history and restart a fresh conversation session.

### Visual Identity & Theme (Material 3)

- **Color Palette**:
  - Advisor Bubble: `MaterialTheme.colorScheme.surfaceVariant` with `outline` border, subtle primary tint, and advisor avatar.
  - User Bubble: `MaterialTheme.colorScheme.primary` with `onPrimary` text, rounded right-aligned pill layout.
  - Quick Chips: `FilterChip` / `AssistChip` using `primaryContainer` and rounded corners.
  - Typing Indicator: 3 pulsing dots using `SophisticatedPrimary`.
- **Typography & Formatting**:
  - Advisor Message: `bodyLarge` with clear line spacing, bold headings (`titleSmall`), and readable list formatting.
  - Timestamp & Model Badge: `labelSmall` in `onSurfaceVariant`.
- **Accessibility**:
  - Minimum touch targets of 48dp on send button, chips, and model switcher.
  - Meaningful `contentDescription` on all icons and avatars.
  - Focus and keyboard handling with `imePadding()` and `WindowInsets`.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Direct Gemini REST API with Multi-Turn Content Format**
  - *Chosen Approach*: Leverage OkHttpClient in `GeminiAiService` using the standard `generateContent` endpoint with `contents: [{ role: "user" | "model", parts: [{ text }] }]` and `systemInstruction`.
  - *Why*: Zero third-party SDK lock-in, full control over multi-model switching (`gemini-3.8-flash`, `gemini-3.1-flash-lite`, `gemini-4-argon`), robust error handling, and offline fallback guidance.
- **Decision 2: Live Financial State Snapshot vs Full Database Dump**
  - *Chosen Approach*: Construct a compact, privacy-conscious financial context summary (e.g. Current Month Total, Top 5 Categories with totals, Budget status for active budgets, and 10 most recent transactions) rather than sending all raw records.
  - *Why*: Stays within optimal token budgets, executes in <10ms from Room DB, protects user privacy, and guarantees high model accuracy without hallucination.
- **Decision 3: Navigation Integration**
  - *Chosen Approach*: Update `NavigationScreen` enum to include `AI_ADVISOR("AI Advisor", Icons.Default.ChatBubble)` and adapt bottom navigation with clean icon/label spacing. Also provide a direct launch shortcut from the "Voice Assistant" / "AI Tools" screen.

---

## 4. Technical Architecture & Data Strategy

### Component Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                       AiChatScreen                          │
│                                                             │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ Top Bar: Model Selector Dropdown & Clear Chat         │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ LazyColumn (Chat Thread):                             │  │
│  │  - UserChatMessageBubble (Right-aligned, Primary)     │  │
│  │  - AdvisorChatMessageBubble (Left-aligned, Card M3)   │  │
│  │  - TypingIndicator / Streaming State                  │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ Quick Suggestion Chips (Horizontal Scroll)            │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ Chat Input Field & Send Button (IME-aware)            │  │
│  └───────────────────────────────────────────────────────┘  │
└──────────────────────────────▲──────────────────────────────┘
                               │ StateFlow
┌──────────────────────────────┴──────────────────────────────┐
│                    AiChatViewModel / Service                │
│  - messages: StateFlow<List<ChatMessage>>                   │
│  - isGenerating: StateFlow<Boolean>                         │
│  - selectedModel: StateFlow<GeminiModelTier>                │
│  - sendMessage(text: String)                                │
│  - clearHistory()                                           │
└──────────────────────────────▲──────────────────────────────┘
                               │ Queries
┌──────────────────────────────┴──────────────────────────────┐
│                   ExpenseViewModel & Room DB                │
│  - getFinancialContextPrompt(): String                      │
│  - expenses: StateFlow<List<ExpenseEntity>>                 │
│  - budgets: StateFlow<List<BudgetEntity>>                   │
└─────────────────────────────────────────────────────────────┘
```

### Data Models

```kotlin
enum class GeminiChatModel(val modelId: String, val displayName: String, val badge: String) {
    FLASH("gemini-3.8-flash", "Gemini 3.8 Flash", "Balanced"),
    FLASH_LITE("gemini-3.1-flash-lite", "Gemini 3.1 Flash Lite", "Fast"),
    ARGON("gemini-4-argon", "Gemini 4 Argon", "Deep Reasoning")
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole, // USER or MODEL
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false,
    val modelTier: GeminiChatModel? = null
)

enum class MessageRole {
    USER, MODEL
}
```

### System Instruction & Financial Context Injection

```text
You are an expert Personal Finance Advisor in the Expense Tracker app.
Your goals:
1. Provide concise, friendly, and practical financial guidance.
2. Analyze spending patterns and identify areas to reduce discretionary expenses.
3. Help the user stay within their allocated budgets.
4. All currency figures should match the user's localized format (₹ / INR).
5. If user asks to perform destructive changes, explain that you are an analytical advisor and direct them to the appropriate app screen.

CURRENT FINANCIAL SNAPSHOT:
[Dynamically injected: Total spend this month, Top categories, Budget limits, Recent transactions]
```

---

## 5. Step-by-Step Implementation Status
- [x] **1. Update `GeminiAiService.kt`**: Multi-turn chat completion with model tier selector (`gemini-3.8-flash`, `gemini-3.1-flash-lite`, `gemini-4-argon`), intelligent context parsing, and personalized local analysis.
- [x] **2. Create Financial Context Builder**: In `ExpenseViewModel.kt`, `buildFinancialContextSnapshot()` computes live category summaries, transaction counts, exact percentages, highest transaction, and budget adherence directly from Room DB.
- [x] **3. Build `AiChatScreen.kt`**: Full Jetpack Compose chat UI with message bubble thread, typing indicator, quick-suggestion prompt chips, model switcher, and an expandable `AdvisorFinancialContextCard` (`testTag("ai_advisor_context_card")`).
- [x] **4. Integrate Navigation**: `NavigationScreen.AI_ADVISOR` in bottom bar and shortcut in `AiToolsScreen`.
- [x] **5. Verification & Testing**: `compile_applet` passed cleanly, and unit tests verify context parsing and personalized response generation.

---

## 6. Testing, Edge Cases & Verification
- **Room DB Context**: Directly reflects user's actual expenses, category summaries, and budget progress.
- **Offline / Local Mode**: Even without a remote Gemini API key, parses context to generate custom savings advice with real numbers.
- **Model Switching**: Switching models updates badge and system instructions.
- **Edge-to-Edge & IME**: Smooth scrolling and padding handling on software keyboard popup.
