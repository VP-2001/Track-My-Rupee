package com.example.security

import android.content.Context
import android.content.SharedPreferences
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.ui.theme.AppThemeMode
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

    private val _themeMode = MutableStateFlow(getStoredThemeMode())
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _hasCompletedOnboarding = MutableStateFlow(prefs.getBoolean("has_completed_onboarding", false))
    val hasCompletedOnboarding: StateFlow<Boolean> = _hasCompletedOnboarding.asStateFlow()

    // Lockout tracking for failed PIN attempts
    private var failedPinAttempts: Int = prefs.getInt("failed_pin_attempts", 0)
    private var lockoutUntilTimestamp: Long = prefs.getLong("lockout_until_timestamp", 0L)

    fun hasCompletedOnboarding(): Boolean = prefs.getBoolean("has_completed_onboarding", false)

    fun setOnboardingCompleted(completed: Boolean = true) {
        prefs.edit().putBoolean("has_completed_onboarding", completed).apply()
        _hasCompletedOnboarding.value = completed
    }

    private fun getStoredThemeMode(): AppThemeMode {
        val name = prefs.getString("app_theme_mode", AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name
        return try {
            AppThemeMode.valueOf(name)
        } catch (_: Exception) {
            AppThemeMode.SYSTEM
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString("app_theme_mode", mode.name).apply()
        _themeMode.value = mode
    }

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

    fun hasPinSet(): Boolean = getStoredPinHash() != null

    fun getStoredPinHash(): String? = prefs.getString("user_pin_hash", null)

    fun setPin(pin: String) {
        val hash = hashString(pin)
        prefs.edit().putString("user_pin_hash", hash).apply()
        // If a PIN is explicitly set, enable lock by default
        setLockEnabled(true)
    }

    fun clearPin() {
        prefs.edit().remove("user_pin_hash").apply()
        setLockEnabled(false)
    }

    fun isLockedOut(): Boolean {
        val now = System.currentTimeMillis()
        return now < lockoutUntilTimestamp
    }

    fun getLockoutRemainingSeconds(): Int {
        val now = System.currentTimeMillis()
        val remaining = (lockoutUntilTimestamp - now) / 1000L
        return if (remaining > 0) remaining.toInt() else 0
    }

    fun verifyPin(pin: String): Boolean {
        if (isLockedOut()) return false

        val stored = getStoredPinHash()
        // Strictly no default PIN per design spec Section 5.9
        val matches = stored != null && stored == hashString(pin)

        if (matches) {
            failedPinAttempts = 0
            lockoutUntilTimestamp = 0L
            prefs.edit()
                .putInt("failed_pin_attempts", 0)
                .putLong("lockout_until_timestamp", 0L)
                .apply()
            _isAppLocked.value = false
            return true
        } else {
            failedPinAttempts++
            prefs.edit().putInt("failed_pin_attempts", failedPinAttempts).apply()

            // After 5 failed attempts, impose delay: 30s * (failed - 4)
            if (failedPinAttempts >= 5) {
                val delaySeconds = 30L * (failedPinAttempts - 4).coerceAtMost(10)
                lockoutUntilTimestamp = System.currentTimeMillis() + (delaySeconds * 1000L)
                prefs.edit().putLong("lockout_until_timestamp", lockoutUntilTimestamp).apply()
            }
            return false
        }
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
                BiometricManager.BIOMETRIC_SUCCESS -> "Fingerprint or Face enrolled"
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "No fingerprint or face enrolled in device settings"
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "No biometric hardware detected"
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Biometric sensor temporarily unavailable"
                BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> "Security update required for biometric hardware"
                else -> {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R &&
                        biometricManager.canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS) {
                        "Device screen lock ready"
                    } else {
                        "PIN security ready"
                    }
                }
            }
        } catch (_: Throwable) {
            "PIN security ready"
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
                                onError("Enter PIN to unlock.")
                            }
                            BiometricPrompt.ERROR_LOCKOUT,
                            BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                                onError("Too many biometric attempts. Enter PIN.")
                            }
                            else -> {
                                onError(errString.toString())
                            }
                        }
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        onError("Biometric not recognized. Retry or enter PIN.")
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
                        .setNegativeButtonText("Use PIN")
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
                        .setAllowedAuthenticators(
                            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
                        )
                        .build()
                }
                else -> {
                    BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Unlock Track My Rupee")
                        .setSubtitle("Fingerprint or Face unlock")
                        .setNegativeButtonText("Use PIN")
                        .build()
                }
            }

            prompt.authenticate(promptInfo)
        } catch (_: Throwable) {
            onError("Biometric unlock unavailable. Enter PIN.")
        }
    }

    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
