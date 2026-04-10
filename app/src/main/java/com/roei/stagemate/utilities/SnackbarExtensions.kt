package com.roei.stagemate.utilities

import android.app.Activity
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

// Lifecycle-aware Snackbar extensions for Activity and Fragment.
// Wraps M3Snackbar with coroutine-based main-thread dispatch and lifecycle checks.

private inline fun Activity.safeFindRoot(crossinline block: (View) -> Unit) {
    (this as? AppCompatActivity)?.lifecycleScope?.launch {
        if (isFinishing || isDestroyed) return@launch
        val root = findViewById<View>(android.R.id.content) ?: return@launch
        block(root)
    }
}

private inline fun Fragment.safeFindRoot(crossinline block: (View) -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch {
        if (!isAdded) return@launch
        val root = view ?: return@launch
        block(root)
    }
}

fun Activity.showSuccessSnackbar(message: String) =
    safeFindRoot { M3Snackbar.success(it, message) }

fun Activity.showErrorSnackbar(message: String) =
    safeFindRoot { M3Snackbar.error(it, message) }

fun Activity.showInfoSnackbar(message: String) =
    safeFindRoot { M3Snackbar.info(it, message) }

fun Activity.showWarningSnackbar(message: String) =
    safeFindRoot { M3Snackbar.warning(it, message) }

fun Fragment.showSuccessSnackbar(message: String) =
    safeFindRoot { M3Snackbar.success(it, message) }

fun Fragment.showErrorSnackbar(message: String) =
    safeFindRoot { M3Snackbar.error(it, message) }

fun Fragment.showInfoSnackbar(message: String) =
    safeFindRoot { M3Snackbar.info(it, message) }

fun Fragment.showWarningSnackbar(message: String) =
    safeFindRoot { M3Snackbar.warning(it, message) }
