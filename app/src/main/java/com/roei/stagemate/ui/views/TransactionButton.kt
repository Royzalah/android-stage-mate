package com.roei.stagemate.ui.views

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.roei.stagemate.R
import com.roei.stagemate.databinding.ViewTransactionButtonBinding

// Animated expand/collapse payment button with card icon slide and text fade.
// Used by PaymentDetailsFragment as the "Pay" action button.
class TransactionButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding: ViewTransactionButtonBinding =
        ViewTransactionButtonBinding.inflate(LayoutInflater.from(context), this, true)

    private var isExpanded = false
    private var autoLoop = false

    private var widthAnimator: ValueAnimator? = null
    private var textAnimator: ObjectAnimator? = null
    private var cardAnimator: ObjectAnimator? = null

    private var loopHandler: Handler? = null
    private var loopRunnable: Runnable? = null

    var onExpandedClickListener: (() -> Unit)? = null

    private val collapsedWidthPx = 63f.dpToPx()
    private val expandedWidthPx = 220f.dpToPx()
    private val collapsedCornerPx = 6f.dpToPx()
    private val expandedCornerPx = 38f.dpToPx()
    private val cardSlidePx = 6f.dpToPx()

    companion object {
        private const val WIDTH_ANIM_DURATION = 600L
        private const val TEXT_ANIM_DURATION = 400L
        private const val TEXT_ANIM_DELAY = 200L
        private const val TEXT_COLLAPSE_DURATION = 200L
        private const val CARD_ANIM_DURATION = 600L
        private const val LOOP_INTERVAL = 3000L
    }

    init {
        attrs?.let {
            val ta = context.obtainStyledAttributes(it, R.styleable.TransactionButton)
            autoLoop = ta.getBoolean(R.styleable.TransactionButton_autoLoop, false)
            ta.recycle()
        }

        binding.transLAYOUTRoot.background = binding.transLAYOUTRoot.background.mutate()

        binding.transLBLText.alpha = 0f
        binding.transIMGCard.translationY = 0f

        setOnClickListener {
            if (isExpanded) {
                onExpandedClickListener?.invoke()
            } else {
                expand()
            }
        }

        if (autoLoop) {
            post { startLoop() }
        }
    }

    fun expand() {
        if (isExpanded) return
        isExpanded = true
        cancelAllAnimators()

        binding.transLBLText.alpha = 0f
        binding.transIMGCard.translationY = 0f

        widthAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = WIDTH_ANIM_DURATION
            interpolator = FastOutSlowInInterpolator()
            addUpdateListener { animator ->
                val fraction = animator.animatedValue as Float

                val width = lerp(collapsedWidthPx, expandedWidthPx, fraction).toInt()
                binding.transLAYOUTRoot.layoutParams.width = width
                binding.transLAYOUTRoot.requestLayout()

                val cornerRadius = lerp(collapsedCornerPx, expandedCornerPx, fraction)
                (binding.transLAYOUTRoot.background as? GradientDrawable)?.cornerRadius = cornerRadius
            }
            start()
        }

        textAnimator = ObjectAnimator.ofFloat(binding.transLBLText, View.ALPHA, 0f, 1f).apply {
            duration = TEXT_ANIM_DURATION
            startDelay = TEXT_ANIM_DELAY
            start()
        }

        cardAnimator = ObjectAnimator.ofFloat(binding.transIMGCard, View.TRANSLATION_Y, 0f, cardSlidePx).apply {
            duration = CARD_ANIM_DURATION
            interpolator = FastOutSlowInInterpolator()
            start()
        }
    }

    fun collapse() {
        if (!isExpanded) return
        isExpanded = false
        cancelAllAnimators()

        textAnimator = ObjectAnimator.ofFloat(binding.transLBLText, View.ALPHA, 1f, 0f).apply {
            duration = TEXT_COLLAPSE_DURATION
            start()
        }

        widthAnimator = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = WIDTH_ANIM_DURATION
            interpolator = FastOutSlowInInterpolator()
            addUpdateListener { animator ->
                val fraction = animator.animatedValue as Float

                val width = lerp(collapsedWidthPx, expandedWidthPx, fraction).toInt()
                binding.transLAYOUTRoot.layoutParams.width = width
                binding.transLAYOUTRoot.requestLayout()

                val cornerRadius = lerp(collapsedCornerPx, expandedCornerPx, fraction)
                (binding.transLAYOUTRoot.background as? GradientDrawable)?.cornerRadius = cornerRadius
            }
            start()
        }

        cardAnimator = ObjectAnimator.ofFloat(binding.transIMGCard, View.TRANSLATION_Y, cardSlidePx, 0f).apply {
            duration = CARD_ANIM_DURATION
            interpolator = FastOutSlowInInterpolator()
            start()
        }
    }

    fun toggle() {
        if (isExpanded) collapse() else expand()
    }

    fun showSuccess() {
        stopLoop()
        cancelAllAnimators()
        isExpanded = true

        binding.transLAYOUTRoot.layoutParams.width = expandedWidthPx.toInt()
        (binding.transLAYOUTRoot.background as? GradientDrawable)?.cornerRadius = expandedCornerPx
        binding.transLAYOUTRoot.requestLayout()

        (binding.transLAYOUTRoot.background as? GradientDrawable)?.setColor(
            context.getColor(R.color.success_s400)
        )

        binding.transLBLText.alpha = 1f
        binding.transLBLText.text = context.getString(R.string.payment_successful)

        binding.transIMGCard.visibility = View.GONE
        binding.transIMGPos.setImageResource(R.drawable.ic_check)
    }

    fun startLoop() {
        stopLoop()
        loopHandler = Handler(Looper.getMainLooper())
        loopRunnable = object : Runnable {
            override fun run() {
                toggle()
                loopHandler?.postDelayed(this, LOOP_INTERVAL)
            }
        }
        loopRunnable?.let { loopHandler?.postDelayed(it, LOOP_INTERVAL) }
    }

    fun stopLoop() {
        loopRunnable?.let { loopHandler?.removeCallbacks(it) }
        loopHandler = null
        loopRunnable = null
    }

    override fun onDetachedFromWindow() {
        stopLoop()
        cancelAllAnimators()
        super.onDetachedFromWindow()
    }

    private fun cancelAllAnimators() {
        widthAnimator?.cancel()
        widthAnimator = null
        textAnimator?.cancel()
        textAnimator = null
        cardAnimator?.cancel()
        cardAnimator = null
    }

    private fun lerp(start: Float, end: Float, fraction: Float): Float {
        return start + (end - start) * fraction
    }

    private fun Float.dpToPx(): Float = this * resources.displayMetrics.density
}
