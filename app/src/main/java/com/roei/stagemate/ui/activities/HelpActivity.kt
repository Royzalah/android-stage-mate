package com.roei.stagemate.ui.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView
import com.roei.stagemate.MyApp
import com.roei.stagemate.R
import com.roei.stagemate.databinding.ActivityHelpBinding

// FAQ/Help screen with expandable question-answer cards, opened from ProfileFragment.
class HelpActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHelpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHelpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
        setupFAQCards()
    }

    private fun initViews() {
        binding.helpBTNContactSupport.setOnClickListener { openContactSupport() }
        binding.helpBTNRefundContact.setOnClickListener { openContactSupport() }
    }

    // Sets up click listeners on FAQ cards to toggle answer visibility
    private fun setupFAQCards() {
        val faqContainer = binding.helpLLFaqContainer
        var cardIndex = 0

        for (i in 0 until faqContainer.childCount) {
            val child = faqContainer.getChildAt(i)
            if (child is MaterialCardView) {
                val innerLayout = child.getChildAt(0) as? LinearLayout ?: continue
                if (innerLayout.childCount < 2) continue

                // Collect all views after the question title (index 0) — answer text, buttons, etc.
                val collapsibleViews = mutableListOf<View>()
                for (j in 1 until innerLayout.childCount) {
                    collapsibleViews.add(innerLayout.getChildAt(j))
                }

                // 3rd FAQ card (contact support) — always visible
                val initialVisibility = if (cardIndex == 2) View.VISIBLE else View.GONE
                collapsibleViews.forEach { it.visibility = initialVisibility }

                child.setOnClickListener {
                    val isVisible = collapsibleViews.firstOrNull()?.visibility == View.VISIBLE
                    val newVisibility = if (isVisible) View.GONE else View.VISIBLE
                    collapsibleViews.forEach { it.visibility = newVisibility }
                }
                cardIndex++
            }
        }
    }

    // Opens email client for support — uses MyApp.signalManager per course standards
    private fun openContactSupport() {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:roeiza34@gmail.com")
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.contact_support))
            }
            startActivity(intent)
        } catch (_: Exception) {
            MyApp.signalManager.toast(getString(R.string.no_email_app))
        }
    }
}
