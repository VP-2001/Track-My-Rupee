package com.example.security

import android.content.Context
import android.content.SharedPreferences
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest

class SecurityAndSyncManager(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("expense_security_prefs", Context.MODE_PRIVATE)

    private val _isAppLocked = MutableStateFlow(isLockEnabled())
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    private val _googleUserEmail = MutableStateFlow(getStoredEmail())
    val googleUserEmail: StateFlow<String?> = _googleUserEmail.asStateFlow()

    private val _lastSyncTime = MutableStateFlow(prefs.getLong("last_sync_time", 0L))
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    fun isLockEnabled(): Boolean = prefs.getBoolean("app_lock_enabled", true)

    fun setLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("app_lock_enabled", enabled).apply()
        _isAppLocked.value = enabled
    }

    fun isBiometricEnabled(): Boolean = prefs.getBoolean("biometric_enabled", true)

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("biometric_enabled", enabled).apply()
    }

    fun getStoredPinHash(): String? = prefs.getString("user_pin_hash", null)

    fun setPin(pin: String) {
        val hash = hashString(pin)
        prefs.edit().putString("user_pin_hash", hash).apply()
    }

    fun verifyPin(pin: String): Boolean {
        val stored = getStoredPinHash()
        val matches = if (stored == null) {
            // Default PIN is 1234 if no custom PIN has been set
            pin == "1234"
        } else {
            stored == hashString(pin)
        }
        if (matches) {
            _isAppLocked.value = false
        }
        return matches
    }

    fun unlockApp() {
        _isAppLocked.value = false
    }

    fun lockApp() {
        if (isLockEnabled()) {
            _isAppLocked.value = true
        }
    }

    fun signInGoogle(email: String = "user@example.com") {
        prefs.edit().putString("google_user_email", email).apply()
        _googleUserEmail.value = email
    }

    fun signOutGoogle() {
        prefs.edit().remove("google_user_email").apply()
        _googleUserEmail.value = null
    }

    private fun getStoredEmail(): String? = prefs.getString("google_user_email", null)

    fun recordSyncSuccess() {
        val now = System.currentTimeMillis()
        prefs.edit().putLong("last_sync_time", now).apply()
        _lastSyncTime.value = now
    }

    fun canUseBiometrics(): Boolean {
        val biometricManager = BiometricManager.from(context)
        val canStrong = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
        val canWeak = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
        val canStrongOrDevice = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == BiometricManager.BIOMETRIC_SUCCESS
        return canStrong || canWeak || canStrongOrDevice
    }

    fun authenticateWithBiometrics(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    _isAppLocked.value = false
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("Biometric authentication not recognized. Please retry or enter PIN.")
                }
            }
        )

        val biometricManager = BiometricManager.from(activity)
        val canStrongOrDevice = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == BiometricManager.BIOMETRIC_SUCCESS

        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Expense Tracker")
            .setSubtitle("Fingerprint or Face unlock required on launch")
            .setDescription("Authenticate to securely access your personal financial data and ledger")

        if (canStrongOrDevice) {
            promptInfoBuilder.setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
        } else {
            promptInfoBuilder
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
                )
                .setNegativeButtonText("Use App PIN")
        }

        try {
            prompt.authenticate(promptInfoBuilder.build())
        } catch (e: Exception) {
            try {
                val fallbackPrompt = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Unlock Expense Tracker")
                    .setSubtitle("Fingerprint or Face unlock")
                    .setDescription("Authenticate to access financial data")
                    .setNegativeButtonText("Use PIN")
                    .build()
                prompt.authenticate(fallbackPrompt)
            } catch (fallbackEx: Exception) {
                onError(fallbackEx.message ?: e.message ?: "Biometric prompt error")
            }
        }
    }

    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
