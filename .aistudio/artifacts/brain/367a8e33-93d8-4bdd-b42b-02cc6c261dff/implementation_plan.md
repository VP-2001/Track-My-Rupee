# On-Device Machine Learning Category Classifier

An on-device machine learning engine that learns from historical Room database spending records to predict transaction categories in real-time, eliminating network latency and reducing server-side API reliance by up to 90%.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following design decisions were confirmed during the interactive interview and govern this implementation:

- **Confirmed Decision 1 (High-Confidence Auto-Selection)**: When typing a merchant or transaction description, the on-device ML model will automatically select the predicted category if confidence is high (>= 75%), while displaying a subtle "Auto-selected via On-Device ML (X% match)" badge.
- **Confirmed Decision 2 (Continuous & Manual Training)**: The on-device ML model automatically updates its internal statistical weights in the background whenever a new transaction is recorded or modified in Room DB, and provides a manual "Retrain Model from Ledger" trigger in AI Tools.
- **Confirmed Decision 3 (Server AI Fallback Strategy)**: When on-device ML confidence is low (< 50%) or for novel merchants, the system displays an intuitive one-tap "Ask Gemini AI" button instead of making unsolicited background network calls, keeping the app fast, predictable, and privacy-first.

---

## 1. Overview & Core Concept

- **What It Does**: Uses local statistical machine learning (Naive Bayes with Laplace smoothing, N-gram tokenization, Gaussian amount profiling, and merchant affinity weighting) directly on the device. When the user enters a merchant name or note, the model computes category probabilities from historical records in < 2ms without requiring internet connectivity.
- **Target Audience / Persona**: Users managing active personal ledgers who want instant, frictionless expense logging without wait times, data consumption, or server latency.
- **Key Value**: Instant category resolution, full offline functionality, personalized accuracy that improves with every transaction, and massive reductions in server-side API consumption.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Adding an Expense (`AddEditExpenseBottomSheet`)**:
   - User types a merchant name (e.g., "Swiggy", "Shell Petrol", "Airtel Broadband").
   - In < 2ms, the on-device model evaluates the text tokens and amount.
   - If confidence >= 75%: The matching category chip is automatically selected, marked with an accent bolt indicator (`⚡`), and a dismissible ML match chip displays: *"Auto-selected via On-Device ML (94% match)"*.
   - If confidence is moderate (50%–74%): An on-device suggestion card appears above the category chips with a 1-tap "Apply" button without auto-switching.
   - If confidence is low (< 50%): The auto-categorize button displays a Gemini spark icon with a prompt: *"Tap to categorize with Gemini AI"*.

2. **Scanned Receipts & Voice Input**:
   - Extracted merchant text is routed through the on-device ML classifier to suggest or pre-fill categories before presenting the confirmation sheet.

3. **AI Smart Finance Tools (`AiToolsScreen`)**:
   - A dedicated **"On-Device ML"** dashboard tab displaying model health, number of historical records learned, distinct merchants cataloged, and a live interactive testing sandbox.

### Visual Identity & Theme Tokens
- **Theme**: Luxury Dark / Emerald FinTech (`#0D0F11` dark surface, `#D4AF37` brass primary, `#00E676` emerald accents).
- **ML Badging**: Subtle primary container pill (`#262316` surface, `#E5C365` text) with a compact lightning bolt icon (`Icons.Default.Bolt`) indicating zero-latency local computation.
- **Touch Targets**: All action buttons and chips maintain a minimum touch target of 48.dp.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Local Statistical ML vs. Heavy Neural Network Model**
  - *Chosen Approach*: On-device Naive Bayes + Bigram Tokenizer + Gaussian Amount Profiling implemented directly in Kotlin.
  - *Why*: Instant inference (< 2ms), zero additional binary size overhead, 100% offline compatibility, and zero native library crash risks across diverse Android architectures.
  - *Alternatives Considered*: Bundling a heavy TensorFlow Lite / PyTorch Mobile model (adds 15-30MB APK bloat, slow cold starts, complex quantization).

- **Decision 2: Dual Training Lifecycle (Automatic Reactive + Manual)**
  - *Chosen Approach*: Observe `repository.allExpenses` via Kotlin Coroutines `StateFlow` to update priors reactively, combined with an explicit "Retrain Model" button.
  - *Why*: Ensures the model is never stale while giving users full transparency and control over their local data.

- **Decision 3: Transparent User Overrides**
  - *Chosen Approach*: Users can tap any category chip at any time to override an auto-selected category. The user's manual selection is immediately saved to Room and fed back into the training corpus.

---

## 4. Technical Architecture & Data Strategy

```
┌─────────────────────────────────────────────────────────────┐
│                       Room Database                         │
│   (ExpenseEntity: title, amount, category, notes, date)     │
└──────────────────────────────┬──────────────────────────────┘
                               │ Flow<List<ExpenseEntity>>
                               ▼
┌─────────────────────────────────────────────────────────────┐
│             ExpenseViewModel (Repository Layer)             │
│   - Reactively listens to database mutations                │
│   - Exposes onDeviceMlStats & predictCategoryOnDevice()    │
└──────────────┬──────────────────────────────┬───────────────┘
               │                              │
               ▼                              ▼
┌──────────────────────────────┐ ┌────────────────────────────┐
│ OnDeviceMlCategoryClassifier │ │      GeminiAiService       │
│  - Merchant Affinity Map     │ │  (Server-Side API Fallback)│
│  - Unigram & Bigram Priors   │ │                            │
│  - Amount Gaussian Profiler  │ │  Only called when on-device│
│  - Cold-Start Semantic KB    │ │  ML confidence < 50%       │
└──────────────┬───────────────┘ └────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────────────────────────┐
│                 User Interface Surfaces                     │
│  1. AddEditExpenseBottomSheet: Auto-select & ML Suggestion  │
│  2. ReceiptParsedReviewSheet: Local pattern resolution      │
│  3. AiToolsScreen: Interactive ML Sandbox & Retrain Control │
└─────────────────────────────────────────────────────────────┘
```

### Component & State Mapping
- **`OnDeviceMlCategoryClassifier`**:
  - `trainFromHistoricalExpenses(expenses: List<ExpenseEntity>)`: Computes token priors, merchant mapping, and category amount statistics.
  - `predictCategory(title, amount, notes)`: Returns `MlCategoryPrediction` containing `predictedCategory`, `confidence`, `isConfident`, `topCandidates`, and `explanation`.
  - `getModelMetadata()`: Exposes trained transaction count, unique merchant count, and timestamp.
- **`AddEditExpenseBottomSheet`**:
  - `onDevicePrediction`: `derivedStateOf(title, amountText, notes)` triggers local inference without recomposition lag.
  - Auto-selects category when `prediction.confidence >= 0.75f` and user hasn't locked a manual selection.
- **`AiToolsScreen (OnDeviceMlTab)`**:
  - Interactive sandbox text fields for testing arbitrary merchants with live probability distributions.
