package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.security.SecurityAndSyncManager
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

    LaunchedEffect(fragmentActivity, bioAvailable) {
        if (fragmentActivity != null && bioAvailable) {
            // Wait briefly for activity window to gain focus before presenting biometric dialog
            kotlinx.coroutines.delay(250)
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
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .testTag("app_lock_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(com.example.ui.theme.SophisticatedDarkSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Lock",
                    tint = com.example.ui.theme.SophisticatedPrimary,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Expense Tracker Locked",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = com.example.ui.theme.SophisticatedDarkOnSurface
            )

            Text(
                text = "Use Fingerprint/Face or Enter PIN (Default: 1234)",
                style = MaterialTheme.typography.bodySmall,
                color = com.example.ui.theme.SophisticatedDarkOnSurfaceVariant
            )

            if (biometricFeedback != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = biometricFeedback!!,
                    style = MaterialTheme.typography.labelSmall,
                    color = com.example.ui.theme.AmberGold,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4 PIN Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.testTag("pin_dots_container")
            ) {
                for (i in 0 until 4) {
                    val isFilled = i < pinInput.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (isError) com.example.ui.theme.DangerRed
                                else if (isFilled) com.example.ui.theme.SophisticatedPrimary
                                else com.example.ui.theme.SophisticatedDarkSurface
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
                    color = com.example.ui.theme.DangerRed,
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
                        Toast.makeText(context, "Activity context not available. Enter PIN: 1234", Toast.LENGTH_SHORT).show()
                    }
                },
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, com.example.ui.theme.SophisticatedPrimary),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = com.example.ui.theme.SophisticatedPrimary),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(48.dp)
                    .testTag("biometric_unlock_button")
            ) {
                Icon(
                    Icons.Default.Fingerprint,
                    contentDescription = "Biometric Fingerprint / Face Unlock",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Unlock with Fingerprint / Face",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

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
                                        tint = com.example.ui.theme.SophisticatedPrimary,
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
                                        tint = com.example.ui.theme.SophisticatedDarkOnSurfaceVariant
                                    )
                                }
                            }
                            else -> {
                                Surface(
                                    shape = CircleShape,
                                    color = com.example.ui.theme.SophisticatedDarkSurfaceDim,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.SophisticatedDarkSurface),
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
                                            fontWeight = FontWeight.SemiBold,
                                            color = com.example.ui.theme.SophisticatedDarkOnSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
