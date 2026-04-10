package com.roei.stagemate.ui.adapters

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.roei.stagemate.R
import com.roei.stagemate.utilities.Constants

// Generic skeleton loader adapter per course standard (animations.md — ObjectAnimator).
// Shows [count] shimmer-style placeholder items while real data loads.
class SkeletonAdapter(
    private val layoutRes: Int = R.layout.item_skeleton_notification,
    private val count: Int = 5
) : RecyclerView.Adapter<SkeletonAdapter.SkeletonViewHolder>() {

    private val activeHolders = mutableSetOf<SkeletonViewHolder>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SkeletonViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        return SkeletonViewHolder(view)
    }

    override fun onBindViewHolder(holder: SkeletonViewHolder, position: Int) {
        activeHolders.add(holder)
        holder.bind()
    }

    override fun onViewRecycled(holder: SkeletonViewHolder) {
        super.onViewRecycled(holder)
        holder.stopAnimation()
        activeHolders.remove(holder)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        stopAnimation()
    }

    override fun getItemCount(): Int = count

    fun stopAnimation() {
        activeHolders.forEach { it.stopAnimation() }
        activeHolders.clear()
    }

    class SkeletonViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        private var pulseAnimator: ObjectAnimator? = null

        fun bind() {
            stopAnimation()
            pulseAnimator = ObjectAnimator.ofFloat(
                itemView, "alpha",
                1f, Constants.UI.SKELETON_ALPHA_MIN
            ).apply {
                duration = Constants.UI.SKELETON_PULSE_DURATION
                repeatMode = ObjectAnimator.REVERSE
                repeatCount = ObjectAnimator.INFINITE
                start()
            }
        }

        fun stopAnimation() {
            pulseAnimator?.cancel()
            pulseAnimator = null
            itemView.alpha = 1f
        }
    }
}
