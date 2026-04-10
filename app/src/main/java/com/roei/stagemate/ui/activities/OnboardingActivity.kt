package com.roei.stagemate.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.roei.stagemate.R
import com.roei.stagemate.databinding.ActivityOnboardingBinding
import com.roei.stagemate.ui.adapters.OnboardingAdapter
import com.roei.stagemate.ui.adapters.OnboardingPage
import com.roei.stagemate.MyApp

// First-time onboarding with 3 swipeable intro pages.
// Shown once before LoginActivity, then marked as seen in SharedPrefs.
class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null

    private val dots: List<View> by lazy {
        listOf(binding.onboardingDot1, binding.onboardingDot2, binding.onboardingDot3)
    }

    private val pages = listOf(
        OnboardingPage(
            imageRes = R.drawable.category_music,
            titleRes = R.string.onboarding_title_1,
            descriptionRes = R.string.onboarding_desc_1
        ),
        OnboardingPage(
            imageRes = R.drawable.category_theater,
            titleRes = R.string.onboarding_title_2,
            descriptionRes = R.string.onboarding_desc_2
        ),
        OnboardingPage(
            imageRes = R.drawable.category_sport,
            titleRes = R.string.onboarding_title_3,
            descriptionRes = R.string.onboarding_desc_3
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewPager()
        setupButtons()

        savedInstanceState?.getInt(KEY_PAGER_POSITION)?.let {
            binding.onboardingViewPager.currentItem = it
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_PAGER_POSITION, binding.onboardingViewPager.currentItem)
    }

    companion object {
        private const val KEY_PAGER_POSITION = "pager_position"
    }

    private fun setupViewPager() {
        val adapter = OnboardingAdapter(pages)
        binding.onboardingViewPager.adapter = adapter

        val callback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateDots(position)
                updateNextButton(position)
            }
        }
        pageChangeCallback = callback
        binding.onboardingViewPager.registerOnPageChangeCallback(callback)
    }

    private fun setupButtons() {
        binding.onboardingBTNSkip.setOnClickListener {
            finishOnboarding()
        }

        binding.onboardingBTNNext.setOnClickListener {
            val currentPage = binding.onboardingViewPager.currentItem
            if (currentPage < pages.size - 1) {
                binding.onboardingViewPager.currentItem = currentPage + 1
            } else {
                finishOnboarding()
            }
        }
    }

    private fun updateDots(activePosition: Int) {
        val activeSize = resources.getDimensionPixelSize(R.dimen.dot_active_size)
        val inactiveSize = resources.getDimensionPixelSize(R.dimen.dot_inactive_size)

        for (i in dots.indices) {
            if (i == activePosition) {
                dots[i].setBackgroundResource(R.drawable.dot_active)
                dots[i].layoutParams.width = activeSize
                dots[i].layoutParams.height = activeSize
            } else {
                dots[i].setBackgroundResource(R.drawable.dot_inactive)
                dots[i].layoutParams.width = inactiveSize
                dots[i].layoutParams.height = inactiveSize
            }
            dots[i].requestLayout()
        }
    }

    private fun updateNextButton(position: Int) {
        if (position == pages.size - 1) {
            binding.onboardingBTNNext.text = getString(R.string.onboarding_get_started)
            binding.onboardingBTNNext.setIconResource(0)
            binding.onboardingBTNSkip.visibility = View.GONE
        } else {
            binding.onboardingBTNNext.text = getString(R.string.onboarding_next)
            binding.onboardingBTNNext.setIconResource(R.drawable.ic_arrow_forward)
            binding.onboardingBTNSkip.visibility = View.VISIBLE
        }
    }

    override fun onDestroy() {
        pageChangeCallback?.let { binding.onboardingViewPager.unregisterOnPageChangeCallback(it) }
        pageChangeCallback = null
        super.onDestroy()
    }

    private fun finishOnboarding() {
        MyApp.sharedPrefsManager.setHasSeenOnboarding(true)
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

}
