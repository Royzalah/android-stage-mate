package com.roei.stagemate.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.view.Window
import com.roei.stagemate.MyApp
import com.roei.stagemate.R
import com.roei.stagemate.databinding.DialogRatingBinding
import com.roei.stagemate.data.models.Ticket
import com.roei.stagemate.data.repository.DataRepository

// Dialog that asks the user to rate an event after purchase.
// Auto-dismisses after 8 seconds if the Firestore save doesn't return.
class RatingDialog(
    context: Context,
    private val ticket: Ticket,
    private val onRatingSubmitted: (Float) -> Unit = {}
) : Dialog(context) {

    private lateinit var binding: DialogRatingBinding
    private val handler = Handler(Looper.getMainLooper())
    private var hasResponded = false

    private val timeoutRunnable = Runnable {
        if (!hasResponded) {
            hasResponded = true
            MyApp.signalManager.toast(context.getString(R.string.rating_failed))
            binding.ratingBTNSubmit.isEnabled = true
        }
    }

    init {
        setupDialog()
    }

    private fun setupDialog() {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogRatingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setCancelable(true)
        setCanceledOnTouchOutside(true)

        setOnDismissListener {
            handler.removeCallbacks(timeoutRunnable)
        }

        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        binding.ratingLBLTitle.text = ticket.eventTitle

        // Force interactive mode in case the XML style overrides it
        binding.ratingRATINGBARRating.setIsIndicator(false)

        binding.ratingBTNSubmit.setOnClickListener {
            val rating = binding.ratingRATINGBARRating.rating
            if (rating > 0f) {
                binding.ratingBTNSubmit.isEnabled = false
                binding.ratingBTNSubmit.text = context.getString(R.string.submitting)
                handler.postDelayed(timeoutRunnable, 3000)

                try {
                    DataRepository.saveTicketRating(ticket.id, rating) { success, errorMsg ->
                        handler.post {
                            if (hasResponded) return@post
                            hasResponded = true
                            handler.removeCallbacks(timeoutRunnable)
                            if (success) {
                                DataRepository.updateEventRating(ticket.eventId, rating) { _, _ -> }
                                MyApp.signalManager.toast(context.getString(R.string.thank_you_for_rating))
                                onRatingSubmitted(rating)
                                safeDismiss()
                            } else {
                                MyApp.signalManager.toast(errorMsg ?: context.getString(R.string.rating_failed))
                                binding.ratingBTNSubmit.text = context.getString(R.string.submit_rating)
                                binding.ratingBTNSubmit.isEnabled = true
                            }
                        }
                    }
                } catch (e: Exception) {
                    hasResponded = true
                    handler.removeCallbacks(timeoutRunnable)
                    MyApp.signalManager.toast(e.message ?: context.getString(R.string.rating_error))
                    binding.ratingBTNSubmit.text = context.getString(R.string.submit_rating)
                    binding.ratingBTNSubmit.isEnabled = true
                }
            } else {
                MyApp.signalManager.toast(context.getString(R.string.please_select_rating))
            }
        }
    }

    private fun safeDismiss() {
        try {
            if (isShowing) dismiss()
        } catch (_: Exception) { }
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacksAndMessages(null)
    }

    fun showDialog() {
        if (!isShowing) show()
    }
}
