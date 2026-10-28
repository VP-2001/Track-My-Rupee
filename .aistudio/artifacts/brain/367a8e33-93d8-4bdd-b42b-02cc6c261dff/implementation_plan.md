# Firebase Authentication & Google Sign-In Integration

Add Google Sign-In via Firebase Auth and Jetpack Credential Manager to Track My Rupee, providing an optional account profile in the top app bar, an account details bottom sheet showing user avatar and display name, one-tap sign-out, and automatic local data preservation and cloud linking.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following user preferences were confirmed during the interactive interview:
> - **Access Flow**: Optional sign-in from the Profile avatar with full offline / guest ledger access allowed by default. Users can freely log expenses without being blocked by a mandatory login wall.
> - **Profile & Avatar Placement**: Prominently displayed in the top app bar across screens with a clickable user avatar (or guest avatar) that opens a dedicated Material 3 Account Bottom Sheet.
> - **Existing Data Handling**: Existing offline records (expenses, categories, budgets, recurring schedules) are automatically linked and preserved upon Google Sign-In rather than wiped or started afresh.
> - **Backend Provisioning**: Firebase will be provisioned via the interactive `set_up_firebase` tool (prompting user confirmation) to generate `google-services.json` and configure Google Sign-In web client credentials.

---

## 1. Overview & Core Concept

- **What It Delivers**: Secure, seamless Google Account authentication for the personal finance tracker. Users can sign in using their existing Google Account using Android's native Jetpack `CredentialManager` (`GetSignInWithGoogleOption`).
- **Profile Experience**:
  - **Signed In**: Displays the user's Google profile picture (loaded via Coil), full display name, email address, account status badge ("Google Account Linked"), cloud sync status, and an accessible "Sign Out" button.
  - **Guest Mode**: Displays a subtle guest avatar icon in the top app bar. Tapping opens the bottom sheet with a branded "Sign in with Google" button and an explanation of account benefits (cloud sync, multi-device backup, profile personalization).
- **Zero Interruption**: Existing expense tracking, charts, voice input, receipt scanning, and budget features remain 100% accessible in guest mode.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Viewing Profile / Sign-In Status**:
   - In the top bar of the application (e.g. Dashboard or app header), a 36dp rounded avatar displays the user's Google photo (or a clean silhouette avatar when guest).
   - Tapping the avatar slides up the **Material 3 Account Bottom Sheet**.
2. **Initiating Google Sign-In**:
   - In the bottom sheet (or from the Reports/Backup screen), user taps **"Sign in with Google"**.
   - Android's native Credential Manager bottom sheet appears with the user's Google accounts.
   - User selects their Google Account; Firebase Auth completes credential authentication in the background.
   - The bottom sheet updates dynamically to display the user's profile photo, display name, email, and sign-out option.
3. **Signing Out**:
   - User taps **"Sign Out"** in the Account Bottom Sheet.
   - An instant confirmation dialog or snackbar confirms sign-out.
   - Auth state returns to guest mode without deleting on-device ledger data.

### Visual Identity & Theme (Material Design 3)

- **Top Bar Profile Action**:
  - `Box` with smooth ripple, 36dp circular avatar with a subtle 1dp border (`SophisticatedDarkBorder`).
  - Online/sync indicator badge (small green dot) when authenticated with Google.
- **Account Bottom Sheet**:
  - Rounded top corners (28dp), `SophisticatedDarkSurfaceDim` container background.
  - **Avatar & Header**: Large 72dp circular avatar (Coil `AsyncImage`) with display name in `titleLarge` (bold) and email in `bodyMedium` (`onSurfaceVariant`).
  - **Account Status Card**: Outlined card highlighting "Google Account Linked" with security shield icon and account UID summary.
  - **Sign-In Action**: Standard branded Google Sign-In button with Google "G" logo and high-contrast typography.
  - **Sign-Out Action**: Outlined danger button with `Icons.Default.Logout`, styled with `DangerRed` border and text.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Jetpack Credential Manager (`GetSignInWithGoogleOption`) vs Legacy GoogleSignInClient**
  - *Chosen Approach*: Modern Android standard Jetpack Credential Manager API (`androidx.credentials:credentials`, `androidx.credentials:credentials-play-services-auth`, `com.google.android.libraries.identity.googleid:googleid`).
  - *Why*: Google deprecated legacy `GoogleSignInClient` in Android. Credential Manager is future-proof, edge-to-edge native, and handles passkeys and Google accounts with modern system UX.
- **Decision 2: Optional Sign-In with Guest Mode vs Mandatory Gate**
  - *Chosen Approach*: User confirmed optional sign-in with full guest support.
  - *Why*: Reduces onboarding friction to zero; users can immediately track expenses and optionally sign in whenever they want cloud sync or profile customization.
- **Decision 3: Seamless Local Data Linking**
  - *Chosen Approach*: Local Room DB entities are retained and associated with the signed-in user's identity.
  - *Why*: Prevents any data loss when transitioning from guest to authenticated user.

---

## 4. Technical Architecture & Data Strategy

### System Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                       Top App Bar / Header                  │
│  [App Title: Track My Rupee]        [Profile Avatar (Coil)] │
└──────────────────────────────▲──────────────────────────────┘
                               │ Tap
┌──────────────────────────────┴──────────────────────────────┐
│                    Account Bottom Sheet                     │
│  - User Profile Picture (AsyncImage)                        │
│  - Display Name & Google Email Address                      │
│  - Account Status Chip ("Connected" / "Guest")              │
│  - "Sign in with Google" Button (if Guest)                  │
│  - "Sign Out" Action Button (if Authenticated)              │
└──────────────────────────────▲──────────────────────────────┘
                               │
┌──────────────────────────────┴──────────────────────────────┐
│                   AuthManager / ViewModel                   │
│  - currentUser: StateFlow<FirebaseUser?>                    │
│  - signInWithGoogle(activityContext)                        │
│  - signOut()                                                │
│  - isGuestMode: Boolean                                     │
└──────────────────────────────▲──────────────────────────────┘
                               │
            ┌──────────────────┴──────────────────┐
            ▼                                     ▼
┌──────────────────────────────┐     ┌────────────────────────┐
│  Jetpack Credential Manager  │     │   Firebase Auth SDK    │
│  GetSignInWithGoogleOption   │     │  GoogleAuthProvider   │
└──────────────────────────────┘     └────────────────────────┘
```

### Component & State Plan

1. **Gradle Dependencies**:
   - Uncomment in `app/build.gradle.kts`:
     - `implementation(libs.firebase.auth)`
     - `implementation(libs.androidx.credentials)`
     - `implementation(libs.androidx.credentials.play.services)`
     - `implementation(libs.googleid)`
2. **Backend Provisioning**:
   - Execute `set_up_firebase` tool to obtain `google-services.json` and `default_web_client_id` for Google Sign-In.
3. **Auth Management (`AuthManager.kt`)**:
   - Observes `FirebaseAuth.getInstance().addAuthStateListener`.
   - Exposes `currentUser: StateFlow<FirebaseUser?>`.
   - Coordinates `CredentialManager.getCredential` with `GetSignInWithGoogleOption`.
   - Passes Google ID token to `FirebaseAuth.getInstance().signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))`.
4. **UI Components**:
   - `ProfileAvatarButton`: Top bar action displaying profile picture or fallback avatar.
   - `AccountBottomSheet`: Displays photo, display name, email, account status, sign-in button, and sign-out button.
   - Integration into `MainActivity.kt` and `ExpenseViewModel.kt`.

---

## 5. Step-by-Step Execution Plan

- [ ] **Step 1: Firebase Provisioning Handshake**: Call `set_up_firebase` with `userConfirmedTermsAcceptedInUI: false` and await confirmation.
- [ ] **Step 2: Dependency Configuration**: Uncomment Firebase Auth, Credential Manager, and GoogleID in `app/build.gradle.kts`.
- [ ] **Step 3: Implement `AuthManager`**: Create authentication coordinator with Google Sign-In and sign-out operations.
- [ ] **Step 4: Expose Auth State in ViewModel**: Expose `currentUser`, `isUserSignedIn`, and sign-in/sign-out actions in `ExpenseViewModel`.
- [ ] **Step 5: Build Profile & Account UI**: Create `AccountBottomSheet` and `ProfileAvatarButton` with Material 3 styling.
- [ ] **Step 6: Integrate with Top Bar & Screens**: Add profile avatar to top app bar and connect to `AccountBottomSheet`.
- [ ] **Step 7: Verification**: Verify compilation via `compile_applet` and run Robolectric unit tests.
