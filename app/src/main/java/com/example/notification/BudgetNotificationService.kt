package com.example.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.BudgetEntity
import com.example.data.model.ExpenseEntity
import com.example.ui.viewmodel.BudgetProgressItem
import java.util.Calendar
import java.util.Locale

enum class BudgetAlertThreshold(val percentage: Int, val label: String) {
    WARNING_80(80, "80% Warning"),
    EXCEEDED_100(100, "100% Exceeded")
}

data class BudgetAlertResult(
    val category: String,
    val threshold: BudgetAlertThreshold,
    val spentAmount: Double,
    val monthlyLimit: Double,
    val percentage: Float,
    val notificationSent: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

object BudgetNotificationService {

    const val CHANNEL_ID = "budget_spending_alerts_channel"
    const val CHANNEL_NAME = "Budget & Spending Alerts"
    const val CHANNEL_DESCRIPTION = "Alerts when monthly spending reaches 80% or 100% of defined budget limits"

    private const val PREFS_NAME = "budget_alerts_prefs"
    private const val KEY_ALERTS_ENABLED = "alerts_enabled"
    private const val KEY_ALERT_80_ENABLED = "alert_80_enabled"
    private const val KEY_ALERT_100_ENABLED = "alert_100_enabled"

    fun initNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isAlertsEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ALERTS_ENABLED, true)
    }

    fun setAlertsEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ALERTS_ENABLED, enabled).apply()
    }

    fun is80AlertEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ALERT_80_ENABLED, true)
    }

    fun set80AlertEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ALERT_80_ENABLED, enabled).apply()
    }

    fun is100AlertEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ALERT_100_ENABLED, true)
    }

    fun set100AlertEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ALERT_100_ENABLED, enabled).apply()
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    private fun getAlertKey(year: Int, month: Int, category: String, threshold: BudgetAlertThreshold): String {
        val cleanCat = category.trim().lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "_")
        return "alert_${year}_${month}_${cleanCat}_${threshold.percentage}"
    }

    fun isAlertAlreadySent(context: Context, year: Int, month: Int, category: String, threshold: BudgetAlertThreshold): Boolean {
        return getPrefs(context).getBoolean(getAlertKey(year, month, category, threshold), false)
    }

    fun markAlertSent(context: Context, year: Int, month: Int, category: String, threshold: BudgetAlertThreshold) {
        getPrefs(context).edit().putBoolean(getAlertKey(year, month, category, threshold), true).apply()
    }

    fun clearAlertFlag(context: Context, year: Int, month: Int, category: String, threshold: BudgetAlertThreshold) {
        getPrefs(context).edit().remove(getAlertKey(year, month, category, threshold)).apply()
    }

    fun resetAllAlertsHistory(context: Context) {
        getPrefs(context).edit().clear().apply()
    }

    /**
     * Evaluates monthly spending for all active budget items against defined limits.
     * Fires a notification when monthly spending reaches 80% (warning) or 100% (exceeded).
     * Deduplicates so notifications fire only once per threshold per month.
     */
    fun evaluateBudgetsAndNotify(
        context: Context,
        budgetProgressList: List<BudgetProgressItem>
    ): List<BudgetAlertResult> {
        if (!isAlertsEnabled(context)) return emptyList()

        initNotificationChannel(context)

        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)
        val currentMonth = cal.get(Calendar.MONTH) + 1

        val results = mutableListOf<BudgetAlertResult>()

        for (item in budgetProgressList) {
            if (item.monthlyLimit <= 0) continue

            val pct = item.percentage

            // Check 100% limit exceeded
            if (pct >= 100f) {
                if (is100AlertEnabled(context)) {
                    val alreadySent100 = isAlertAlreadySent(context, currentYear, currentMonth, item.category, BudgetAlertThreshold.EXCEEDED_100)
                    if (!alreadySent100) {
                        val sent = postBudgetNotification(
                            context = context,
                            category = item.category,
                            threshold = BudgetAlertThreshold.EXCEEDED_100,
                            spent = item.spentAmount,
                            limit = item.monthlyLimit,
                            percentage = pct
                        )
                        markAlertSent(context, currentYear, currentMonth, item.category, BudgetAlertThreshold.EXCEEDED_100)
                        // Also mark 80% as covered
                        markAlertSent(context, currentYear, currentMonth, item.category, BudgetAlertThreshold.WARNING_80)

                        results.add(
                            BudgetAlertResult(
                                category = item.category,
                                threshold = BudgetAlertThreshold.EXCEEDED_100,
                                spentAmount = item.spentAmount,
                                monthlyLimit = item.monthlyLimit,
                                percentage = pct,
                                notificationSent = sent
                            )
                        )
                    }
                }
            } else if (pct >= 80f) {
                // If user reduced spending below 100%, allow 100% to trigger again in future if they cross it
                clearAlertFlag(context, currentYear, currentMonth, item.category, BudgetAlertThreshold.EXCEEDED_100)

                if (is80AlertEnabled(context)) {
                    val alreadySent80 = isAlertAlreadySent(context, currentYear, currentMonth, item.category, BudgetAlertThreshold.WARNING_80)
                    if (!alreadySent80) {
                        val sent = postBudgetNotification(
                            context = context,
                            category = item.category,
                            threshold = BudgetAlertThreshold.WARNING_80,
                            spent = item.spentAmount,
                            limit = item.monthlyLimit,
                            percentage = pct
                        )
                        markAlertSent(context, currentYear, currentMonth, item.category, BudgetAlertThreshold.WARNING_80)

                        results.add(
                            BudgetAlertResult(
                                category = item.category,
                                threshold = BudgetAlertThreshold.WARNING_80,
                                spentAmount = item.spentAmount,
                                monthlyLimit = item.monthlyLimit,
                                percentage = pct,
                                notificationSent = sent
                            )
                        )
                    }
                }
            } else {
                // Spending is under 80%: reset flags so if user spends again and hits 80%, alert can trigger
                clearAlertFlag(context, currentYear, currentMonth, item.category, BudgetAlertThreshold.WARNING_80)
                clearAlertFlag(context, currentYear, currentMonth, item.category, BudgetAlertThreshold.EXCEEDED_100)
            }
        }

        return results
    }

    /**
     * Posts a notification to the system notification tray.
     */
    fun postBudgetNotification(
        context: Context,
        category: String,
        threshold: BudgetAlertThreshold,
        spent: Double,
        limit: Double,
        percentage: Float
    ): Boolean {
        initNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "budgets")
            putExtra("alert_category", category)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            (category.hashCode() * 31 + threshold.percentage).and(0x7FFFFFFF),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val (title, content, bigText) = when (threshold) {
            BudgetAlertThreshold.EXCEEDED_100 -> {
                val over = (spent - limit).coerceAtLeast(0.0)
                val t = "🚨 Budget Exceeded: $category (${percentage.toInt()}%)"
                val c = "You have exceeded your ${ExpenseEntity.formatInr(limit)} limit! Spent: ${ExpenseEntity.formatInr(spent)}."
                val b = "🚨 Critical Budget Alert!\n\nYour monthly spending in '$category' has exceeded 100% of your defined budget.\n\n• Limit: ${ExpenseEntity.formatInr(limit)}\n• Total Spent: ${ExpenseEntity.formatInr(spent)}\n• Over Budget: ${ExpenseEntity.formatInr(over)}\n\nTap to review transactions and adjust spending."
                Triple(t, c, b)
            }
            BudgetAlertThreshold.WARNING_80 -> {
                val remaining = (limit - spent).coerceAtLeast(0.0)
                val t = "⚠️ 80% Budget Warning: $category (${percentage.toInt()}%)"
                val c = "You have used ${percentage.toInt()}% of your ${ExpenseEntity.formatInr(limit)} limit. ${ExpenseEntity.formatInr(remaining)} remaining."
                val b = "⚠️ Spending Caution!\n\nYour monthly spending in '$category' has reached ${percentage.toInt()}% of your budget limit.\n\n• Budget Limit: ${ExpenseEntity.formatInr(limit)}\n• Current Spend: ${ExpenseEntity.formatInr(spent)}\n• Remaining: ${ExpenseEntity.formatInr(remaining)}\n\nTap to monitor your spending and stay within your monthly goal."
                Triple(t, c, b)
            }
        }

        val notificationId = (category.hashCode() * 31 + threshold.percentage).and(0x7FFFFFFF)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_budget_alert)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(if (threshold == BudgetAlertThreshold.EXCEEDED_100) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(if (threshold == BudgetAlertThreshold.EXCEEDED_100) 0xFFE11D48.toInt() else 0xFFF59E0B.toInt())
            .addAction(
                R.drawable.ic_stat_budget_alert,
                "View Budget",
                pendingIntent
            )
            .build()

        return try {
            val manager = NotificationManagerCompat.from(context)
            if (hasNotificationPermission(context)) {
                manager.notify(notificationId, notification)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Sends a direct test notification for immediate UI verification.
     */
    fun sendTestNotification(
        context: Context,
        threshold: BudgetAlertThreshold,
        category: String = "Food & Dining"
    ): Boolean {
        val (spent, limit) = when (threshold) {
            BudgetAlertThreshold.EXCEEDED_100 -> Pair(5450.0, 5000.0)
            BudgetAlertThreshold.WARNING_80 -> Pair(4100.0, 5000.0)
        }
        val pct = ((spent / limit) * 100).toFloat()
        return postBudgetNotification(
            context = context,
            category = category,
            threshold = threshold,
            spent = spent,
            limit = limit,
            percentage = pct
        )
    }
}
