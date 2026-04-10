package com.roei.stagemate.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.view.Window
import com.roei.stagemate.R
import com.roei.stagemate.ui.activities.EventDetailActivity
import com.roei.stagemate.databinding.DialogTrendingEventBinding
import com.roei.stagemate.data.models.Event
import com.roei.stagemate.utilities.Constants
import com.roei.stagemate.MyApp
import com.roei.stagemate.data.repository.DataRepository

// Pop-up dialog promoting a trending event with view-details and dismiss actions.
// Shown once per session by HomeFragment when trending notifications are enabled.
class TrendingEventDialog(
    context: Context,
    private val event: Event,
    private var onDismiss: (() -> Unit)? = {}
) : Dialog(context) {

    private lateinit var binding: DialogTrendingEventBinding

    init {
        setupDialog()
    }

    override fun onStop() {
        super.onStop()
        onDismiss = null
    }

    private fun setupDialog() {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogTrendingEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        populateEventData()
        setupButtons()
    }

    private fun populateEventData() {
        binding.apply {
            trendingLBLTitle.text = event.title
            trendingLBLCategory.text = event.category.replaceFirstChar { it.uppercase() }
            trendingLBLDate.text = event.date
            trendingLBLLocation.text = event.location
            MyApp.imageLoader.loadImage(event.imageUrl, trendingIMGEvent)
        }
    }

    private fun setupButtons() {
        binding.trendingBTNNotInterested.setOnClickListener {
            // Permanently disable trending popups
            MyApp.sharedPrefsManager.setTrendingNotificationsEnabled(false)
            DataRepository.saveUserSettings(
                mapOf("trendingNotifications" to false)
            ) { success, _ ->
                if (!success) {
                    // Cloud save failed — revert local so next session re-syncs correctly
                    MyApp.sharedPrefsManager.setTrendingNotificationsEnabled(true)
                }
            }
            dismiss()
            onDismiss?.invoke()
        }

        binding.trendingBTNViewDetails.setOnClickListener {
            dismiss()
            val intent = Intent(getContext(), EventDetailActivity::class.java)
            intent.putExtra(Constants.IntentKeys.EVENT_ID, event.id)
            getContext().startActivity(intent)
            onDismiss?.invoke()
        }
    }

    fun showDialog() {
        if (!isShowing) {
            show()
        }
    }
}