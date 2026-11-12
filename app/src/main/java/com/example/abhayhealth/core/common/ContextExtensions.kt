package com.example.abhayhealth.core.common

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Traverses the Context wrapper hierarchy to find the enclosing Activity.
 * This is critical in Jetpack Compose where LocalContext.current is often wrapped
 * inside a ContextThemeWrapper or ViewCompositionStrategy wrapper.
 */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
