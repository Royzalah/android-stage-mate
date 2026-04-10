package com.roei.stagemate.utilities

import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import com.roei.stagemate.R

// Skeleton loading placeholder with shimmer animation, fades in real content when ready.
// Used by HomeFragment and SearchFragment during data loading.
object SkeletonLoader {

    fun show(skeletonView: View) {
        skeletonView.visibility = View.VISIBLE
        startShimmerAnimation(skeletonView)
    }
    
    fun hide(skeletonView: View, contentView: View) {
        stopShimmerAnimation(skeletonView)
        skeletonView.visibility = View.GONE
        contentView.visibility = View.VISIBLE
        val fadeIn = AnimationUtils.loadAnimation(
            contentView.context,
            R.anim.skeleton_fade_in
        )
        contentView.startAnimation(fadeIn)
    }
    
    private fun startShimmerAnimation(view: View) {
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                startShimmerAnimation(view.getChildAt(i))
            }
        } else {
            val shimmer = AnimationUtils.loadAnimation(
                view.context,
                R.anim.skeleton_shimmer
            )
            view.startAnimation(shimmer)
        }
    }
    
    private fun stopShimmerAnimation(view: View) {
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                stopShimmerAnimation(view.getChildAt(i))
            }
        } else {
            view.clearAnimation()
        }
    }
}
