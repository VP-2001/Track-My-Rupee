package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.security.SecurityAndSyncManager
import com.example.ui.theme.AmberGold
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SophisticatedDarkBackground
import com.example.ui.theme.SophisticatedDarkBorder
import com.example.ui.theme.SophisticatedDarkOnSurface
import com.example.ui.theme.SophisticatedDarkOnSurfaceVariant
import com.example.ui.theme.SophisticatedDarkSurface
import com.example.ui.theme.SophisticatedDarkSurfaceDim
import com.example.ui.theme.SophisticatedDarkSurfaceVariant
import com.example.ui.theme.SophisticatedPrimary
import com.example.ui.theme.SophisticatedPrimaryContainer
import com.example.ui.theme.SuccessGreen
import com.example.util.findFragmentActivity

@Composable
fun AppLockOverlay(
    securityManager: SecurityAndSyncManager,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    val fragmentActivity = remember(context) {
        context.findFragmentActivity() ?: (context as? FragmentActivity)
    }
    var pinInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var biometricFeedback by remember { mutableStateOf<String?>(null) }
    val bioAvailable = remember { securityManager.canUseBiometrics() }
    val bioStatusDescription = remember { securityManager.getBiometricStatusDescription() }

    // Intercept back button to prevent unauthorized bypass of financial data
    BackHandler(enabled = true) {
        fragmentActivity?.moveTaskToBack(true)
    }

    // Automatically prompt BiometricPrompt upon app launch if biometric authentication is available
    LaunchedEffect(fragmentActivity, bioAvailable) {
        if (fragmentActivity != null && bioAvailable) {
            // Short delay to ensure activity window is fully foregrounded and ready for BiometricPrompt
            kotlinx.coroutines.delay(200)
            if (!fragmentActivity.isFinishing && !fragmentActivity.isDestroyed) {
                securityManager.authenticateWithBiometrics(
                    activity = fragmentActivity,
                    onSuccess = { onUnlocked() },
                    onError = { err ->
                        biometricFeedback = err
                    }
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SophisticatedDarkBackground,
                        Color(0xFF0D0F14),
                        SophisticatedDarkBackground
                    )
                )
            )
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .testTag("app_lock_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Glowing Biometric & Shield Emblem
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(SophisticatedDarkSurface)
                    .border(BorderStroke(2.dp, SophisticatedPrimary.copy(alpha = 0.6f)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (bioAvailable) Icons.Default.Fingerprint else Icons.Default.Lock,
                    contentDescription = "Lock",
                    tint = SophisticatedPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Track My Rupee",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = SophisticatedDarkOnSurface
            )

            Text(
                text = "Financial Ledger Locked",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = SophisticatedPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Biometric Status Pill
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SophisticatedDarkSurfaceVariant,
                border = BorderStroke(1.dp, SophisticatedDarkBorder),
                modifier = Modifier.testTag("biometric_status_chip")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (bioAvailable) Icons.Default.Fingerprint else Icons.Default.Security,
                        contentDescription = null,
                        tint = if (bioAvailable) SuccessGreen else AmberGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (bioAvailable) "BiometricPrompt API Ready" else bioStatusDescription,
                        style = MaterialTheme.typography.labelSmall,
                        color = SophisticatedDarkOnSurfaceVariant
                    )
                }
            }

            // Error or Biometric Feedback Message
            AnimatedVisibility(
                visible = biometricFeedback != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                if (biometricFeedback != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AmberGold.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = biometricFeedback!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberGold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4 PIN Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier.testTag("pin_dots_container")
            ) {
                for (i in 0 until 4) {
                    val isFilled = i < pinInput.length
                    val dotColor by animateColorAsState(
                        targetValue = when {
                            isError -> DangerRed
                            isFilled -> SophisticatedPrimary
                            else -> SophisticatedDarkSurface
                        },
                        label = "dot_color"
                    )
                    val dotSize by animateDpAsState(
                        targetValue = if (isFilled) 18.dp else 14.dp,
                        label = "dot_size"
                    )

                    Box(
                        modifier = Modifier
                            .size(dotSize)
                            .clip(CircleShape)
                            .background(dotColor)
                            .border(
                                BorderStroke(
                                    1.dp,
                                    if (isFilled) SophisticatedPrimary else SophisticatedDarkBorder
                                ),
                                CircleShape
                            )
                            .testTag("pin_dot_$i")
                    )
                }
            }

            if (isError) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Incorrect PIN. Default PIN is 1234.",
                    style = MaterialTheme.typography.labelSmall,
                    color = DangerRed,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("incorrect_pin_error")
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Quick Biometric Trigger Button (Fingerprint or Face)
            OutlinedButton(
                onClick = {
                    if (fragmentActivity != null) {
                        if (securityManager.canUseBiometrics()) {
                            securityManager.authenticateWithBiometrics(
                                activity = fragmentActivity,
                                onSuccess = { onUnlocked() },
                                onError = { err ->
                                    biometricFeedback = err
                                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                }
                            )
                        } else {
                            Toast.makeText(
                                context,
                                securityManager.getBiometricStatusDescription(),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        Toast.makeText(context, "Activity context unavailable. Enter PIN: 1234", Toast.LENGTH_SHORT).show()
                    }
                },
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, SophisticatedPrimary),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = SophisticatedPrimaryContainer.copy(alpha = 0.2f),
                    contentColor = SophisticatedPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(50.dp)
                    .testTag("biometric_unlock_button")
            ) {
                Icon(
                    Icons.Default.Fingerprint,
                    contentDescription = "Biometric Fingerprint / Face Unlock",
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Unlock with Fingerprint / Face",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Numeric Keypad Grid
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("BIO", "0", "DEL")
            )

            keys.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(0.85f),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { key ->
                        when (key) {
                            "BIO" -> {
                                IconButton(
                                    onClick = {
                                        if (fragmentActivity != null) {
                                            securityManager.authenticateWithBiometrics(
                                                activity = fragmentActivity,
                                                onSuccess = { onUnlocked() },
                                                onError = {
                                                    biometricFeedback = it
                                                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .size(64.dp)
                                        .testTag("keypad_bio_button")
                                ) {
                                    Icon(
                                        Icons.Default.Fingerprint,
                                        contentDescription = "Biometric",
                                        tint = SophisticatedPrimary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            "DEL" -> {
                                IconButton(
                                    onClick = {
                                        if (pinInput.isNotEmpty()) {
                                            pinInput = pinInput.dropLast(1)
                                            isError = false
                                        }
                                    },
                                    modifier = Modifier
                                        .size(64.dp)
                                        .testTag("keypad_del_button")
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Backspace,
                                        contentDescription = "Delete",
                                        tint = SophisticatedDarkOnSurfaceVariant
                                    )
                                }
                            }
                            else -> {
                                Surface(
                                    shape = CircleShape,
                                    color = SophisticatedDarkSurfaceDim,
                                    border = BorderStroke(1.dp, SophisticatedDarkBorder),
                                    modifier = Modifier
                                        .size(64.dp)
                                        .testTag("keypad_key_$key")
                                        .clickable {
                                            if (pinInput.length < 4) {
                                                isError = false
                                                val updated = pinInput + key
                                                pinInput = updated
                                                if (updated.length == 4) {
                                                    val success = securityManager.verifyPin(updated)
                                                    if (success) {
                                                        onUnlocked()
                                                    } else {
                                                        isError = true
                                                        pinInput = ""
                                                    }
                                                }
                                            }
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = key,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = SophisticatedDarkOnSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Default PIN: 1234 • Change anytime in Vault Settings",
                style = MaterialTheme.typography.labelSmall,
                color = SophisticatedDarkOnSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}
