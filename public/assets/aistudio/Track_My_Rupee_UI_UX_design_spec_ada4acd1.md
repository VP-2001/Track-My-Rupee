# Track My Rupee: UI/UX design spec

Version 1.0 · Target: Google Play launch · Based on a code review of the current build (not a running-app review)

## 1. Design goals

1. Answer one question the moment the app opens: how much can I still spend this month?
2. Add an expense in under 5 seconds (manual, voice or scan).
3. Feel trustworthy: plain language, visible privacy promises, no developer jargon.
4. Pass Play review: accessibility, policy flows and store assets are part of the design, not an afterthought.

### Principles

- One primary action per screen.
- Money first, decoration second. Numbers get the largest type and tabular figures.
- Utilities (backup, export, sync, security) live in Settings, never on Home.
- Color carries meaning: green is income, amber is nearing a limit, red is over a limit. Ordinary expenses are neutral.
- Every screen has designed loading, empty and error states.

## 2. Brand and color

The launcher icon is navy, teal and gold. The UI should match it, replacing the current lavender palette.

### Dark theme

| Role | Hex | Use |
| --- | --- | --- |
| background | #0B1626 | Screen background |
| surface | #12233A | Cards |
| surfaceVariant | #1A2F4B | Chips, inputs |
| outline | #2E4A6B | Borders, dividers |
| onSurface | #E8EEF6 | Primary text |
| onSurfaceVariant | #9FB1C7 | Secondary text |
| primary | #2DD4BF | Main actions, selected tab |
| onPrimary | #04342C | Text on primary |
| accent (gold) | #F5C451 | Highlights, AI sparkle (sparingly) |
| income | #6FCF97 | Income amounts |
| warning | #FBBF24 | 80–99% of budget |
| danger | #F2B8B5 | Over budget (container #8C1D18) |

### Light theme (new)

| Role | Hex |
| --- | --- |
| background | #F6F8FB |
| surface | #FFFFFF |
| surfaceVariant | #E8EEF6 |
| outline | #C5D0DD |
| onSurface | #0B1626 |
| onSurfaceVariant | #475569 |
| primary | #0F766E |
| onPrimary | #FFFFFF |
| income | #2E7D32 |
| warning | #B45309 |
| danger | #B3261E |

The app must follow the system setting (light, dark, system) with a manual override in Settings. Verify every text and background pair for 4.5:1 contrast before release; the hexes above are starting points.

## 3. Typography

| Style | Size / line | Weight | Use |
| --- | --- | --- | --- |
| amountHero | 32 / 40 sp | SemiBold | Left to spend |
| headline | 22 / 28 sp | Medium | Screen titles |
| title | 16 / 24 sp | Medium | Card titles, list primary |
| body | 14 / 20 sp | Regular | Default text |
| label | 12 / 16 sp | Medium | Chips, captions, tab labels |

- Minimum size is 12sp. Remove all 9, 10 and 11sp text.
- Use tabular figures (`tnum`) for every money value so columns align.
- Format with the Indian grouping, for example ₹1,25,000.
- Test at 200% font scale. Nothing may clip or overlap.

## 4. Information architecture

### Current to proposed

| Current tab | Proposed home |
| --- | --- |
| Home | Home (simplified) |
| Ledger | Ledger |
| Charts | Insights |
| Budgets | Budgets |
| Advisor + Tools | Ask AI (tabs inside: Chat, Tools) |
| Backup (Reports) | Insights > Reports; backup and export move to Settings |
| Scan | Add sheet > Scan receipt |

Bottom bar: Home, Ledger, Insights, Budgets, Ask AI. Five items, labels always visible, 12sp.

Settings opens from the avatar at the top of Home. It holds: Account and sync, Security (PIN, biometrics), Backup and restore, Export, Appearance (theme), Notifications, Privacy and data, Delete account, About.

Tablet (600dp and up): keep the navigation rail, same five destinations, Add button at the top of the rail.

## 5. Screen specs

### 5.1 Onboarding (first launch only)

1. Welcome: "Know where your money goes." One illustration, one button: Get started.
2. Privacy: "Your ledger stays on this device. Sign in only if you want sync." Explain AI features send the receipt or statement you choose to a Google AI service. Button: Continue.
3. Secure it: choose a PIN (required to continue; no default PIN) and optionally enable biometrics. Skip is allowed but shows what you give up.
4. Set a monthly budget: one number field, with Skip.

Permissions are never requested here. Camera is requested when the user taps Scan, microphone when the user taps Voice, each with a one-line explanation first.

### 5.2 Home

Top to bottom:

1. Header: month label, "Hi, {name}" (no name when a guest), avatar button to Settings.
2. Hero card: "Left to spend this month", amount in amountHero, progress bar, "₹X of ₹Y spent" and "N days left". Tapping opens Budgets. If no budget is set, the card becomes "Set a monthly budget" with a button.
3. Quick actions (3 equal buttons, 56dp tall): Add, Voice, Scan.
4. One alert row at most: the most urgent budget state (nearest to or over a limit). Tap opens that category.
5. Recent: last 3 transactions and See all.

Removed from Home: export cards, Drive backup, Excel export, bank-statement promo, recurring-transaction promo, the two-chart dashboard. These move to Settings, Insights or the Add sheet.

No FAB on Home, because the quick actions replace it. Elsewhere, one FAB (Add) on Ledger only.

### 5.3 Add sheet

A bottom sheet with four rows: Type it in, Say it, Scan a receipt, Import a bank statement.

The manual form is amount-first:

1. Large amount field with the numeric keypad open.
2. Expense / Income toggle.
3. Category chips (last-used first), with More.
4. Optional: merchant, date (defaults to today), payment method, note.
5. Save button always visible above the keyboard.

After saving, show a snackbar: "Added ₹420 to Food" with Undo. Do not use a confirmation dialog.

Import a bank statement shows what is sent for AI parsing before the user picks a file.

### 5.4 Ledger

- Title, compact search bar (keep the current design), filter chips below.
- Group by day with sticky headers and a daily total.
- Row: category icon, merchant, category and payment method, amount right-aligned. Expenses neutral, income green.
- Swipe to delete with Undo; tap opens the detail sheet (keep the current sheet).
- Empty state: "No transactions yet" with the button Add your first expense.

### 5.5 Insights

- Month selector at top.
- Three summary tiles: Spent, Income, Saved.
- Category breakdown (donut) and daily trend (line). Each chart has a text summary for TalkBack.
- Compare with last month (the existing comparison view).
- Reports section: monthly report, date-range report.

### 5.6 Budgets

- Overall budget card first, then category cards sorted by percent used.
- State colors: under 80% teal, 80–99% amber, 100% and over red. Always pair color with a text label, for example "92% used" and an icon, so color is never the only signal.
- Edit budget opens a sheet with the amount and the rollover option.

### 5.7 Ask AI

- Chat is the default view; Tools (receipt scan, statement parsing) is a second segment.
- Show a persistent line under the input: "AI can make mistakes. This isn't financial advice."
- Suggested prompts when empty: "Where did I overspend this month?", "Can I afford a ₹5,000 purchase?".
- Show what data is shared with the AI service on first use and in Settings > Privacy and data.

### 5.8 Settings

Grouped list. Account and sync shows plain status ("Synced just now", "Sync paused"), never the UID or backend names. Delete account is in this screen and removes cloud data and signs out.

### 5.9 Lock screen

- Remove the "Default PIN: 1234" text and the 1234 fallback.
- Biometric prompt opens automatically; PIN pad is the fallback.
- After 5 failed PIN attempts, add an increasing delay.

## 6. Components

- Card: 16dp radius, 16dp padding, 1dp outline, no shadow in dark theme.
- Buttons: filled (one per screen), tonal (secondary), text (tertiary). Height 48dp minimum.
- Chips: 32dp visual height, 48dp touch target.
- Inputs: outlined, label above, error text below in danger color with an icon.
- Progress bar: 8dp, rounded, with a text equivalent.
- Sheets: drag handle, 24dp top corners, dismissible with back.

### States every screen needs

| State | Treatment |
| --- | --- |
| Loading | Skeleton shapes for cards and rows (no spinners over empty screens) |
| Empty | Headline, one line of help, one button |
| Error | What happened, what to do, Retry. Never raw exception text |
| Offline | Quiet banner: "Offline. Changes will sync later." |

## 7. Copy changes

| Now | Change to |
| --- | --- |
| Export Room DB to CSV | Export to CSV |
| Syncing with Firestore... | Syncing… |
| Force Manual Sync | Sync now |
| UID: abc123… | (remove) |
| Guest Session / Local device storage only | Using without an account |
| Financial Ledger Locked | Track My Rupee is locked |
| Default PIN: 1234 • Change anytime in Vault Settings | (remove) |
| Budget Limit Exceeded | Over budget in 2 categories |
| Backup tab | Settings > Backup and restore |

Use sentence case, active verbs and no exclamation marks on system messages.

## 8. Accessibility checklist

- Touch targets 48dp minimum.
- Meaningful icons have a `contentDescription`; decorative icons use `null` on purpose. Audit the current 174 null descriptions.
- Charts expose a text summary.
- Support light, dark and system theme.
- Test with TalkBack, 200% font scale and a screen reader on the Add flow.
- Don't rely on color alone for budget states.
- Respect reduced motion.

## 9. Localization

Move every string into `strings.xml` (currently none are). Start with English and add Hindi and Malayalam first if those are your target users. Use `NumberFormat` with the `en-IN` locale for rupee formatting.

## 10. Google Play readiness

### Policy and product

- Privacy policy URL, published on the web and linked in Settings.
- Data safety form: declare account info, financial info, photos (receipts), audio (voice) and AI processing.
- Account deletion: in-app path plus a web URL, as Play requires for apps with account creation.
- Financial features declaration in Play Console.
- AI disclosure and "not financial advice" text in Ask AI.
- Runtime permission rationale for camera and microphone.
- Keep AI API keys out of the APK; call AI through Firebase AI Logic or your own server.

### Build

- Enable `isMinifyEnabled` and resource shrinking, then retest.
- Choose the final application ID now; it cannot change after publishing.
- Increment `versionCode` for every upload; upload an AAB, not an APK.
- Confirm `allowBackup` and the empty `backup_rules.xml` do what you intend for a finance app.

### Store listing assets

- App icon 512 × 512 PNG, plus an adaptive icon with a separate foreground layer (the current icon has a rounded square baked in).
- Feature graphic 1024 × 500.
- At least 4 phone screenshots (recommended 8): Home, Add, Insights, Budgets, Ask AI, Lock, light theme, tablet.
- Short description (80 characters) and full description.
- Content rating questionnaire.

### Testing track

New personal developer accounts have been required to run a closed test with a minimum number of testers for 14 days before applying for production. Confirm the current numbers in Play Console.

## 11. Roadmap

### Phase 1: before you submit

1. Remove the visible default PIN and the 1234 fallback; add PIN setup.
2. Replace developer jargon (section 7).
3. Add privacy policy, account deletion, AI disclosure.
4. Move the AI key off the client.
5. Fix the icon, set the final application ID, enable minify.

### Phase 2: core redesign

1. New theme tokens, light theme, type scale.
2. Five-tab navigation and Settings screen.
3. Home rebuild and Add sheet.
4. Onboarding.

### Phase 3: polish

1. String extraction and localization.
2. Skeleton, empty and error states.
3. Accessibility pass and screenshots.

## 12. Risks and open questions

- Rewiring navigation touches several 1,500–2,500-line screen files; budget real time for regression testing.
- This spec comes from code review. Run it past 5 real users on a device before locking the design.
- Moving backup and export off Home may reduce discoverability. Add a one-time tip pointing to Settings.
