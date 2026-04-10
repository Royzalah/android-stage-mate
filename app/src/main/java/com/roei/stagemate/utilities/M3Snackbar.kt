package com.roei.stagemate.utilities

import android.view.View
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.roei.stagemate.R

// Styled M3 Snackbar helper with success/error/info/warning variants.
// Used by SnackbarExtensions, FavoriteHelper, and all Activities.
object M3Snackbar {

    fun success(view: View, message: String, duration: Int = Snackbar.LENGTH_SHORT) {
        show(view, message, duration, R.color.success_s500)
    }

    fun error(view: View, message: String, duration: Int = Snackbar.LENGTH_LONG) {
        show(view, message, duration, R.color.danger_d500)
    }

    fun info(view: View, message: String, duration: Int = Snackbar.LENGTH_SHORT) {
        show(view, message, duration, R.color.secondary_s500)
    }

    fun warning(view: View, message: String, duration: Int = Snackbar.LENGTH_LONG) {
        show(view, message, duration, R.color.warning_w500)
    }

    fun action(
        view: View,
        message: String,
        actionText: String,
        duration: Int = Snackbar.LENGTH_LONG,
        action: () -> Unit
    ) {
        val snackbar = Snackbar.make(view, message, duration)
            .setAction(actionText) { action() }
            .setActionTextColor(view.context.getColor(R.color.primaryColor))

        applyM3Style(snackbar, R.color.neutral_n500)
        snackbar.show()
    }

    private fun show(view: View, message: String, duration: Int, backgroundColorRes: Int) {
        val snackbar = Snackbar.make(view, message, duration)
            .setTextColor(view.context.getColor(R.color.white))

        applyM3Style(snackbar, backgroundColorRes)
        snackbar.show()
    }

    private fun applyM3Style(snackbar: Snackbar, backgroundColorRes: Int) {
        val snackbarView = snackbar.view
        val context = snackbarView.context

        // Apply rounded shape background first, then tint with the status color
        snackbarView.background = ContextCompat.getDrawable(context, R.drawable.snackbar_background)
        snackbarView.backgroundTintList = ContextCompat.getColorStateList(context, backgroundColorRes)
        snackbarView.elevation = 6f

        snackbarView.alpha = 0f
        snackbarView.animate()
            .alpha(1f)
            .setDuration(300)
            .start()
    }
}
