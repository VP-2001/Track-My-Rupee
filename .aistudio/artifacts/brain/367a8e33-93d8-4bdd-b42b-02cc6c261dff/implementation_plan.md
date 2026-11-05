# Monthly Financial PDF Export, Ledger Month Dropdown & Filter Rearrangement

This plan delivers a shareable PDF report combining monthly KPIs, visual charts (rendered pie/donut and horizontal category bars), and itemized category summaries; introduces a combined month switcher with previous/next stepper controls in the transaction ledger to inspect any month; and reorganizes date filter chips into the requested order (Today, Yesterday, This Week, This Month, Previous Month, All, Custom).

## User Review & Critical Decisions

> [!IMPORTANT]
> The following design choices were confirmed by user selections during Phase 1:

- **Confirmed Decision 1 (PDF Report Scope)**: Full summary with visual charts (pie and bar charts rendered directly on the PDF Canvas), high-level financial KPIs, and category breakdown with budget adherence.
- **Confirmed Decision 2 (Ledger Month Selector UX)**: Combined month dropdown with previous (`<`) and next (`>`) steppers, accessible directly in the ledger toolbar, enabling instant viewing of every recorded month.
- **Confirmed Decision 3 (Date Filter Chip Ordering)**: Rearranged to: `Today` → `Yesterday` → `This Week` → `This Month` → `Previous Month` → `All` → `Custom`.

---

## 1. Overview & Core Concept

- **What It Does**:
  1. **Visual Monthly PDF Export**: Generates an A4 PDF document containing financial KPI summary cards, a vector-rendered category donut/pie chart, a horizontal category ranking bar chart, category budget adherence tables, and a share intent (`Intent.ACTION_SEND`).
  2. **Every-Month Dropdown in Ledger**: A month navigation control in `TransactionsScreen.kt` featuring a formatted month anchor (e.g., "November 2026"), `< Previous` and `Next >` steppers, and a dropdown listing all months with recorded transactions.
  3. **Streamlined Date Filter Chips**: Updated `DateFilterType` chip order to prioritize immediate day/week filters followed by monthly and historical filters.
- **Target Audience / Persona**: Users managing household budgets, business expenses, or tax bookkeeping who need to share monthly visual reports with accountants or family members and review past months in their ledger.
- **Key Value**: Eliminates manual screenshotting by producing printable vector PDFs with visual charts, and allows effortless ledger navigation across any past or present month.

---

## 2. User Experience & Visual Design

### Key User Flows

```
┌────────────────────────────────────────────────────────────────────────┐
│ USER FLOW 1: Exporting Monthly Visual PDF                              │
│                                                                        │
│ [Analytics / Reports / Insights Screen]                                │
│       │                                                                │
│       ▼                                                                │
│ Tap "Export Monthly PDF"                                               │
│       │                                                                │
│       ▼                                                                │
│ Select Month Dialog (Defaults to currently viewed month)               │
│       │                                                                │
│       ▼                                                                │
│ System generates PDF with Canvas-drawn Pie Chart & Bar Chart           │
│       │                                                                │
│       ▼                                                                │
│ Android Share Sheet opens (WhatsApp, Drive, Gmail, Print, Files)       │
└────────────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────────────┐
│ USER FLOW 2: Navigating Every Month in Transactions Ledger             │
│                                                                        │
│ [Transactions Ledger Screen]                                           │
│       │                                                                │
│       ├── Tap [<] / [>] Stepper ──► Steps back or forward by 1 month   │
│       │                                                                │
│       └── Tap Month Label Dropdown ──► Shows all recorded months       │
│                                        (e.g., "Nov 2026", "Oct 2026")  │
│                                        Selects month & updates ledger  │
└────────────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────────────┐
│ USER FLOW 3: Quick Filter Chips in Ledger                              │
│                                                                        │
│ [Today]  [Yesterday]  [This Week]  [This Month]  [Prev Month]  [All]   │
└────────────────────────────────────────────────────────────────────────┘
```

### Visual Identity & Theme
- **Theme Consistency**: Sophisticated Dark surface styling (`#1E1035`, `#0D9488`, `#6366F1`, `#F59E0B`, `#10B981`) in Compose UI matching the established design system.
- **PDF Document Design**:
  - Crisp vector graphics rendered via Android `android.graphics.pdf.PdfDocument` and `android.graphics.Canvas`.
  - Deep Slate (`#0F172A`) typography, Emerald Teal (`#0D9488`) accent banner, and Soft Gray (`#F1F5F9`) card containers.
  - Multi-colored category slices and bars using the app's standard palette (`ChartColors`).

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Native Canvas Drawing vs. View Snapshot for PDF Charts**:
  - *Chosen Approach*: Draw pie slices (`canvas.drawArc`) and horizontal bars (`canvas.drawRoundRect`) directly onto the `PdfDocument` Canvas using vector geometry.
  - *Why*: Direct vector drawing yields infinite resolution, tiny file sizes (~50KB), zero bitmap pixelation when printed or zoomed, and works reliably without depending on off-screen Compose view capture.
  - *Alternatives Considered*: Capturing a bitmap of the Compose screen, which can suffer from device density scaling discrepancies and blurry fonts in printed PDFs.

- **Decision 2: Month Filter Integration with Room Database**:
  - *Chosen Approach*: When a month is picked from the dropdown or steppers, calculate `startOfMonth` (00:00:00.000) and `endOfMonth` (23:59:59.999) timestamps and feed them to `viewModel.setCustomDateRange(Pair(start, end))` while setting the filter label.
  - *Why*: Seamlessly integrates with the existing Room FTS search queries and filter pipelines without requiring database schema changes.

- **Decision 3: Date Filter Enum Alignment**:
  - *Chosen Approach*: Update `DateFilterType` enum declaration order to: `TODAY`, `YESTERDAY`, `THIS_WEEK`, `THIS_MONTH`, `PREVIOUS_MONTH`, `ALL_TIME`, `CUSTOM`.
  - *Why*: Directly preserves the requested ordering across all chips throughout the app (`DateFilterType.entries`).

---

## 4. Technical Architecture & Data Strategy

### System Component Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                         PRESENTATION LAYER                             │
│                                                                        │
│  ┌───────────────────────────┐      ┌───────────────────────────────┐  │
│  │   TransactionsScreen.kt   │      │  RechartsSpendingInsights.kt  │  │
│  │  - Rearranged Filter Chips│      │  - Insights Dashboard         │  │
│  │  - Combined Month Stepper │      │  - "Export Monthly PDF" Btn   │  │
│  │  - Month Dropdown List    │      └───────────────┬───────────────┘  │
│  └─────────────┬─────────────┘                      │                  │
└────────────────┼────────────────────────────────────┼──────────────────┘
                 │                                    │
                 ▼                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                          VIEWMODEL LAYER                               │
│                                                                        │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ ExpenseViewModel.kt                                              │  │
│  │  - exportMonthlyVisualPdfReport(year, month): File               │  │
│  │  - dateFilter: StateFlow<DateFilterType>                         │  │
│  │  - availableMonths: StateFlow<List<AvailableMonth>>              │  │
│  │  - selectMonthForLedger(year, month)                             │  │
│  └──────────────────────────────┬───────────────────────────────────┘  │
└─────────────────────────────────┼──────────────────────────────────────┘
                                  │
                                  ▼
┌────────────────────────────────────────────────────────────────────────┐
│                         DATA & EXPORT LAYER                            │
│                                                                        │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ ReportExportManager.kt                                           │  │
│  │  - generateMonthlyFinancialSummaryWithChartsPdf(...)             │  │
│  │      * Page 1: Header + 4 KPI Cards                              │  │
│  │      * Page 1: Vector Donut/Pie Chart + Legend                   │  │
│  │      * Page 1: Vector Ranked Category Bar Chart                  │  │
│  │      * Page 2: Budget Adherence Table + Itemized Breakdown       │  │
│  │  - shareFile(context, pdfFile, "application/pdf")                │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

### Data Model & Method Additions

1. **`ReportExportManager.kt`**:
   - `generateMonthlyFinancialSummaryWithChartsPdf(context, expenses, budgets, year, month): File`
   - Draws vector pie chart with arcs, center hole, and legend.
   - Draws ranked horizontal bars with category names, amounts in INR, and percentages.
   - Outputs a shareable PDF cached in `context.cacheDir`.

2. **`ExpenseViewModel.kt`**:
   - Update `DateFilterType` enum order.
   - Add `exportMonthlyVisualPdfReport(year: Int, month: Int): File`.
   - Add `selectMonthForLedger(year: Int, month: Int)` to update filter bounds.

3. **`TransactionsScreen.kt`**:
   - Move month switcher bar outside conditional blocks so it is always accessible or shown when selecting historical months.
   - Wire dropdown items to populate all available recorded months.
   - Display chips in order: `Today`, `Yesterday`, `This Week`, `This Month`, `Previous Month`, `All`, `Custom`.

4. **`RechartsSpendingInsightsDashboard.kt` & `AnalyticsScreen.kt`**:
   - Add an "Export Visual PDF" button in the Monthly Spending Insights header to directly export the currently viewed month with charts.

---

## 5. Verification Plan

1. **Build Verification**:
   - Execute `compile_applet` to confirm zero compilation errors across Compose components and export managers.
2. **Automated Testing**:
   - Run `gradle :app:testDebugUnitTest` via `run_command` to verify all 45+ existing tests pass.
   - Add new test cases to `ExampleRobolectricTest.kt` verifying `generateMonthlyFinancialSummaryWithChartsPdf` generates a valid multi-page PDF with vector elements and that the new `DateFilterType` order functions accurately.
