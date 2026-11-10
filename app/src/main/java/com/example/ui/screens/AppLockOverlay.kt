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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.security.SecurityAndSyncManager
import com.example.ui.theme.extendedColors
import com.example.util.findFragmentActivity
import kotlinx.coroutines.delay

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
    var lockoutSeconds by remember { mutableIntStateOf(securityManager.getLockoutRemainingSeconds()) }
    val bioAvailable = remember { securityManager.canUseBiometrics() }
    val bioStatusDescription = remember { securityManager.getBiometricStatusDescription() }
    val hasPin = remember { securityManager.hasPinSet() }

    // Intercept back button to prevent unauthorized bypass of financial data
    BackHandler(enabled = true) {
        fragmentActivity?.moveTaskToBack(true)
    }

    // Lockout countdown timer
    LaunchedEffect(lockoutSeconds) {
        if (lockoutSeconds > 0) {
            delay(1000L)
            lockoutSeconds = securityManager.getLockoutRemainingSeconds()
        }
    }

    // Automatically prompt BiometricPrompt upon app foreground if biometric authentication is available
    LaunchedEffect(fragmentActivity, bioAvailable) {
        if (fragmentActivity != null && bioAvailable && lockoutSeconds == 0) {
            delay(250)
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

    // If no PIN has been configured at all and lock was somehow active, unlock directly
    LaunchedEffect(hasPin) {
        if (!hasPin && !bioAvailable) {
            securityManager.unlockApp()
            onUnlocked()
        }
    }

    val isLockedOut = lockoutSeconds > 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceDim,
                        MaterialTheme.colorScheme.background
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
            // Biometric & Shield Emblem
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (bioAvailable) Icons.Default.Fingerprint else Icons.Default.Lock,
                    contentDescription = "Lock",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Track My Rupee",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Copy change per Section 7: "Track My Rupee is locked"
            Text(
                text = "Track My Rupee is locked",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Biometric Status Pill
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier.testTag("biometric_status_chip")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (bioAvailable) Icons.Default.Fingerprint else Icons.Default.Security,
                        contentDescription = null,
                        tint = if (bioAvailable) MaterialTheme.extendedColors.income else MaterialTheme.extendedColors.warning,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (bioAvailable) "Biometric unlock ready" else bioStatusDescription,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Error or Biometric Feedback Message
            AnimatedVisibility(
                visible = biometricFeedback != null && !isLockedOut,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                if (biometricFeedback != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.extendedColors.warningContainer.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, MaterialTheme.extendedColors.warning.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = biometricFeedback!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.extendedColors.warning,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Lockout Countdown Banner
            if (isLockedOut) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.extendedColors.dangerContainer,
                    border = BorderStroke(1.dp, MaterialTheme.extendedColors.danger)
                ) {
                    Text(
                        text = "Too many failed attempts. Try again in ${lockoutSeconds}s",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
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
                            isError -> MaterialTheme.extendedColors.danger
                            isFilled -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.surface
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
                                    if (isFilled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                ),
                                CircleShape
                            )
                            .testTag("pin_dot_$i")
                    )
                }
            }

            if (isError && !isLockedOut) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Incorrect PIN. Try again.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.extendedColors.danger,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("incorrect_pin_error")
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Biometric Unlock Button
            if (bioAvailable && !isLockedOut) {
                OutlinedButton(
                    onClick = {
                        if (fragmentActivity != null) {
                            securityManager.authenticateWithBiometrics(
                                activity = fragmentActivity,
                                onSuccess = { onUnlocked() },
                                onError = { err ->
                                    biometricFeedback = err
                                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(50.dp)
                        .testTag("biometric_unlock_button")
                ) {
                    Icon(
                        Icons.Default.Fingerprint,
                        contentDescription = "Biometric Unlock",
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Unlock with Fingerprint or Face",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

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
                                        if (fragmentActivity != null && bioAvailable && !isLockedOut) {
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
                                    enabled = bioAvailable && !isLockedOut,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .testTag("keypad_bio_button")
                                ) {
                                    Icon(
                                        Icons.Default.Fingerprint,
                                        contentDescription = "Biometric",
                                        tint = if (bioAvailable && !isLockedOut) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            "DEL" -> {
                                IconButton(
                                    onClick = {
                                        if (pinInput.isNotEmpty() && !isLockedOut) {
                                            pinInput = pinInput.dropLast(1)
                                            isError = false
                                        }
                                    },
                                    enabled = !isLockedOut,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .testTag("keypad_del_button")
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Backspace,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            else -> {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceDim,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .size(64.dp)
                                        .testTag("keypad_key_$key")
                                        .clickable(enabled = !isLockedOut) {
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
                                                        lockoutSeconds = securityManager.getLockoutRemainingSeconds()
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
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}
