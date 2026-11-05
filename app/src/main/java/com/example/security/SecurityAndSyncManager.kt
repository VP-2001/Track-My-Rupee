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

    private val _isAutoSyncEnabled = MutableStateFlow(prefs.getBoolean("auto_cloud_sync_enabled", true))
    val isAutoSyncEnabled: StateFlow<Boolean> = _isAutoSyncEnabled.asStateFlow()

    fun isAutoSyncEnabled(): Boolean = prefs.getBoolean("auto_cloud_sync_enabled", true)

    fun setAutoSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("auto_cloud_sync_enabled", enabled).apply()
        _isAutoSyncEnabled.value = enabled
    }

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

    fun isAutoLockOnBackgroundEnabled(): Boolean = prefs.getBoolean("auto_lock_on_background", true)

    fun setAutoLockOnBackgroundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("auto_lock_on_background", enabled).apply()
    }

    fun getLockTimeoutSeconds(): Int = prefs.getInt("lock_timeout_seconds", 0)

    fun setLockTimeoutSeconds(seconds: Int) {
        prefs.edit().putInt("lock_timeout_seconds", seconds).apply()
    }

    fun onAppBackgrounded() {
        if (isLockEnabled() && isAutoLockOnBackgroundEnabled()) {
            prefs.edit().putLong("last_backgrounded_time", System.currentTimeMillis()).apply()
        }
    }

    fun onAppForegrounded() {
        if (!isLockEnabled()) return
        if (isAutoLockOnBackgroundEnabled()) {
            val bgTime = prefs.getLong("last_backgrounded_time", 0L)
            val timeoutMillis = getLockTimeoutSeconds() * 1000L
            val now = System.currentTimeMillis()
            if (bgTime > 0L && (now - bgTime >= timeoutMillis)) {
                _isAppLocked.value = true
            }
        }
    }

    fun canUseBiometrics(): Boolean {
        if (!isBiometricEnabled()) return false
        return try {
            val biometricManager = BiometricManager.from(context)
            val canStrong = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
            val canWeak = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
            val canDevice = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                biometricManager.canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
            } else {
                false
            }
            canStrong || canWeak || canDevice
        } catch (_: Throwable) {
            false
        }
    }

    fun isBiometricEnrolled(): Boolean {
        return try {
            val biometricManager = BiometricManager.from(context)
            biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
        } catch (_: Throwable) {
            false
        }
    }

    fun getBiometricStatusDescription(): String {
        return try {
            val biometricManager = BiometricManager.from(context)
            when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
                BiometricManager.BIOMETRIC_SUCCESS -> "Biometric (Fingerprint/Face) Enrolled & Active"
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "Biometric sensor ready, but no Fingerprint/Face enrolled in device Settings"
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "No biometric sensor detected on this device (4-digit PIN fallback active)"
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Biometric sensor is temporarily unavailable"
                BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> "Security update required for biometric hardware"
                else -> {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R &&
                        biometricManager.canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS) {
                        "Device Screen Lock (PIN/Pattern) Active"
                    } else {
                        "PIN Security Active (Default: 1234)"
                    }
                }
            }
        } catch (_: Throwable) {
            "PIN Security Active (Default: 1234)"
        }
    }

    fun authenticateWithBiometrics(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (activity.isFinishing || activity.isDestroyed) {
            return
        }

        try {
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
                        when (errorCode) {
                            BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                            BiometricPrompt.ERROR_USER_CANCELED -> {
                                onError("Biometric prompt dismissed. Enter 4-digit PIN.")
                            }
                            BiometricPrompt.ERROR_LOCKOUT,
                            BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                                onError("Too many biometric attempts. Please enter your 4-digit PIN.")
                            }
                            else -> {
                                onError(errString.toString())
                            }
                        }
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        onError("Biometric authentication not recognized. Please retry or enter PIN.")
                    }
                }
            )

            val biometricManager = BiometricManager.from(activity)
            val canStrong = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
            val canWeak = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
            val canDevice = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                biometricManager.canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
            } else false

            val promptInfo = when {
                canStrong || canWeak -> {
                    BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Unlock Track My Rupee")
                        .setSubtitle("Confirm fingerprint or face recognition")
                        .setDescription("Verify identity to access your financial transactions & balances")
                        .setNegativeButtonText("Use App PIN")
                        .setAllowedAuthenticators(
                            if (canWeak) {
                                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
                            } else {
                                BiometricManager.Authenticators.BIOMETRIC_STRONG
                            }
                        )
                        .build()
                }
                canDevice && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R -> {
                    BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Unlock Track My Rupee")
                        .setSubtitle("Confirm screen lock credentials")
                        .setDescription("Verify credentials to access your financial ledger")
                        .setAllowedAuthenticators(
                            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
                        )
                        .build()
                }
                else -> {
                    BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Unlock Track My Rupee")
                        .setSubtitle("Fingerprint or Face unlock")
                        .setDescription("Authenticate to view financial transactions")
                        .setNegativeButtonText("Use PIN")
                        .build()
                }
            }

            prompt.authenticate(promptInfo)
        } catch (t: Throwable) {
            onError("Biometric authentication unavailable on this device. Enter PIN.")
        }
    }

    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
