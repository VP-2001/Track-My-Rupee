package com.example.ui.components

import android.app.Activity
import com.example.util.findActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.AmberGold
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedContainer
import com.example.ui.theme.DangerRedOnContainer
import com.example.ui.theme.SophisticatedDarkBorder
import com.example.ui.theme.SophisticatedDarkOnSurface
import com.example.ui.theme.SophisticatedDarkOnSurfaceVariant
import com.example.ui.theme.SophisticatedDarkSurface
import com.example.ui.theme.SophisticatedDarkSurfaceDim
import com.example.ui.theme.SophisticatedDarkSurfaceVariant
import com.example.ui.theme.SophisticatedPrimary
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.ExpenseViewModel
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

@Composable
fun ProfileAvatarButton(
    viewModel: ExpenseViewModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isUserSignedIn = currentUser != null

    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(SophisticatedDarkSurface)
            .border(
                1.5.dp,
                if (isUserSignedIn) SophisticatedPrimary else SophisticatedDarkBorder,
                CircleShape
            )
            .clickable(onClick = onClick)
            .testTag("profile_avatar_button"),
        contentAlignment = Alignment.Center
    ) {
        if (currentUser?.photoUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(currentUser?.photoUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Profile Picture",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )
        } else if (isUserSignedIn) {
            val initial = currentUser?.displayName?.firstOrNull()?.uppercaseChar()
                ?: currentUser?.email?.firstOrNull()?.uppercaseChar()
                ?: 'U'
            Text(
                text = initial.toString(),
                fontWeight = FontWeight.Bold,
                color = SophisticatedPrimary,
                fontSize = 16.sp
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Guest Profile",
                tint = SophisticatedDarkOnSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }

        // Online indicator dot
        if (isUserSignedIn) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(SuccessGreen)
                    .border(1.5.dp, SophisticatedDarkSurfaceDim, CircleShape)
                    .align(Alignment.BottomEnd)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountBottomSheet(
    viewModel: ExpenseViewModel,
    isOpen: Boolean,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isAuthenticating by viewModel.isAuthenticating.collectAsStateWithLifecycle()
    val authError by viewModel.authErrorMessage.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showSignOutConfirmation by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SophisticatedDarkSurfaceDim,
        dragHandle = null,
        modifier = Modifier.testTag("account_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Account & Profile",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SophisticatedDarkOnSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = SophisticatedDarkOnSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Error Notice
            authError?.let { err ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DangerRedContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = DangerRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            color = DangerRedOnContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (currentUser != null) {
                // LOGGED IN STATE
                val user = currentUser!!
                val isCloudSyncing by viewModel.isCloudSyncing.collectAsStateWithLifecycle()
                LoggedInProfileView(
                    user = user,
                    isAuthenticating = isAuthenticating,
                    isSyncing = isCloudSyncing,
                    onSyncWithFirestore = { viewModel.syncFirestoreTransactions() },
                    showSignOutConfirmation = showSignOutConfirmation,
                    onShowSignOutConfirm = { showSignOutConfirmation = true },
                    onDismissSignOutConfirm = { showSignOutConfirmation = false },
                    onSignOutConfirmed = {
                        viewModel.signOut {
                            showSignOutConfirmation = false
                            onDismiss()
                        }
                    }
                )
            } else {
                // GUEST / SIGN-IN STATE
                GuestSignInView(
                    isAuthenticating = isAuthenticating,
                    onSignInWithGoogle = {
                        val activity = context.findActivity() ?: (context as? Activity) ?: context
                        viewModel.signInWithGoogle(activity) { success ->
                            if (success) {
                                onDismiss()
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun LoggedInProfileView(
    user: FirebaseUser,
    isAuthenticating: Boolean,
    isSyncing: Boolean = false,
    onSyncWithFirestore: () -> Unit = {},
    showSignOutConfirmation: Boolean,
    onShowSignOutConfirm: () -> Unit,
    onDismissSignOutConfirm: () -> Unit,
    onSignOutConfirmed: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // User Profile Picture
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(SophisticatedDarkSurface)
                .border(2.dp, SophisticatedPrimary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (user.photoUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(user.photoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Google Profile Picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                )
            } else {
                val initial = user.displayName?.firstOrNull()?.uppercaseChar()
                    ?: user.email?.firstOrNull()?.uppercaseChar()
                    ?: 'G'
                Text(
                    text = initial.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = SophisticatedPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Display Name & Email
        Text(
            text = user.displayName ?: "Google User",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = SophisticatedDarkOnSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = user.email ?: "",
            style = MaterialTheme.typography.bodyMedium,
            color = SophisticatedDarkOnSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Connected Badge
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SuccessGreen.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Google Account Linked",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = SuccessGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Account Details Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = SophisticatedDarkSurface,
            border = BorderStroke(1.dp, SophisticatedDarkBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ACCOUNT & SYNC STATUS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = SophisticatedPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = SophisticatedPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Local Ledger Linked",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = SophisticatedDarkOnSurface
                        )
                        Text(
                            text = "All expenses, categories, and budgets are linked to this Google profile.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SophisticatedDarkOnSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Authenticated with Credential Manager",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = SophisticatedDarkOnSurface
                        )
                        Text(
                            text = "UID: ${user.uid.take(12)}...",
                            style = MaterialTheme.typography.bodySmall,
                            color = SophisticatedDarkOnSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Sync with Firestore Button
        Button(
            onClick = onSyncWithFirestore,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("sheet_sync_button"),
            enabled = !isSyncing
        ) {
            if (isSyncing) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Syncing with Firestore...")
            } else {
                Icon(Icons.Default.Sync, contentDescription = "Sync", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sync Ledger with Firestore", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Sign Out Controls
        if (showSignOutConfirmation) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = DangerRedContainer.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, DangerRed)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Sign out of your Google Account?",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = DangerRedOnContainer
                    )
                    Text(
                        text = "Your on-device ledger data will remain safe on this device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SophisticatedDarkOnSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismissSignOutConfirm,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = onSignOutConfirmed,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DangerRed,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("confirm_sign_out_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Sign Out")
                        }
                    }
                }
            }
        } else {
            OutlinedButton(
                onClick = onShowSignOutConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sign_out_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = DangerRed
                ),
                border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Sign Out",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sign Out",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun GuestSignInView(
    isAuthenticating: Boolean,
    onSignInWithGoogle: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Guest Silhouette
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(SophisticatedDarkSurface)
                .border(1.dp, SophisticatedDarkBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Guest",
                tint = SophisticatedDarkOnSurfaceVariant,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Guest Session",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = SophisticatedDarkOnSurface
        )
        Text(
            text = "Local device storage only",
            style = MaterialTheme.typography.bodySmall,
            color = SophisticatedDarkOnSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Value Proposition Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = SophisticatedDarkSurface,
            border = BorderStroke(1.dp, SophisticatedDarkBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "WHY SIGN IN WITH GOOGLE?",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = SophisticatedPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))

                BenefitItem(
                    icon = Icons.Default.Security,
                    title = "Automatic Data Linking",
                    description = "Existing local transactions and budgets will seamlessly attach to your account."
                )
                Spacer(modifier = Modifier.height(8.dp))
                BenefitItem(
                    icon = Icons.Default.Sync,
                    title = "Cloud Backup & Restore",
                    description = "Never lose your financial history when switching or reinstalling devices."
                )
                Spacer(modifier = Modifier.height(8.dp))
                BenefitItem(
                    icon = Icons.Default.Person,
                    title = "Personalized Profile",
                    description = "See your Google profile picture, display name, and synced account preferences."
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Google Sign-In Button
        Button(
            onClick = onSignInWithGoogle,
            enabled = !isAuthenticating,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("google_sign_in_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            )
        ) {
            if (isAuthenticating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.Black,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Connecting to Google...",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Google "G" icon
                    Text(
                        text = "G",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = Color(0xFF4285F4)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Sign in with Google",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F1F1F),
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun BenefitItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SophisticatedPrimary,
            modifier = Modifier
                .size(18.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = SophisticatedDarkOnSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = SophisticatedDarkOnSurfaceVariant
            )
        }
    }
}
