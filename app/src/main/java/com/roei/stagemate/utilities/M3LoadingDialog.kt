package com.roei.stagemate.utilities

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.Window
import com.roei.stagemate.R
import com.roei.stagemate.databinding.DialogLoadingBinding
import java.lang.ref.WeakReference

// Modal loading dialog with a circular progress indicator and message.
// Used by PaymentActivity, SeatSelectionActivity, and other async operations.
class M3LoadingDialog(context: Context) {

    private val contextRef = WeakReference(context)
    private var dialog: Dialog? = null
    private var binding: DialogLoadingBinding? = null

    fun show(message: String = "Loading...") {
        val ctx = contextRef.get() ?: return
        if (dialog?.isShowing == true) return

        binding = DialogLoadingBinding.inflate(LayoutInflater.from(ctx))

        val currentBinding = binding ?: return
        dialog = Dialog(ctx).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(currentBinding.root)
            setCancelable(false)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
        
        binding?.loadingLBLMessage?.text = message
        dialog?.show()
    }
    
    fun updateMessage(message: String) {
        binding?.loadingLBLMessage?.text = message
    }
    
    fun dismiss() {
        dialog?.dismiss()
        dialog = null
        binding = null
    }
    
    fun isShowing(): Boolean {
        return dialog?.isShowing == true
    }
}
