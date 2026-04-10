package com.roei.stagemate.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.roei.stagemate.databinding.ItemOnboardingPageBinding

// ViewPager2 adapter for onboarding intro pages (image + title + description).
// Used by OnboardingActivity.

data class OnboardingPage(
    val imageRes: Int,
    val titleRes: Int,
    val descriptionRes: Int
)

class OnboardingAdapter(
    private val pages: List<OnboardingPage>
) : RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

    inner class OnboardingViewHolder(
        private val binding: ItemOnboardingPageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(page: OnboardingPage) {
            binding.onboardingIMGIllustration.setImageResource(page.imageRes)
            binding.onboardingLBLTitle.setText(page.titleRes)
            binding.onboardingLBLDescription.setText(page.descriptionRes)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val binding = ItemOnboardingPageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return OnboardingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        holder.bind(pages[position])
    }

    override fun getItemCount(): Int = pages.size
}
