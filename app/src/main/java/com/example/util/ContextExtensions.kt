package com.example.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Traverses ContextWrapper hierarchy to safely resolve the underlying Activity.
 * In Jetpack Compose (especially inside bottom sheets, dialogs, and themes),
 * LocalContext.current is often wrapped and direct (context as? Activity) cast fails.
 */
fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) {
            return current
        }
        current = current.baseContext
    }
    return null
}

fun Context.findFragmentActivity(): androidx.fragment.app.FragmentActivity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is androidx.fragment.app.FragmentActivity) {
            return current
        }
        current = current.baseContext
    }
    return null
}
