package com.roei.stagemate.ui.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SweepGradient
import android.util.AttributeSet
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.roei.stagemate.R

// Custom FrameLayout with a rotating gradient border effect.
// The padding defines the visible border; the child card masks the center.
class AnimatedGradientBorderLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val CORNER_RADIUS_DP = 17f
        private const val ROTATION_DURATION_MS = 2500L
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderRect = RectF()
    private val shaderMatrix = Matrix()
    private var rotationAngle = 0f

    // Memory leak fix: cache shader to avoid allocation per frame (was 60 allocs/sec)
    private var cachedShader: SweepGradient? = null
    private var lastWidth = 0
    private var lastHeight = 0

    private val cornerRadiusPx = CORNER_RADIUS_DP * resources.displayMetrics.density

    private val gradientColors = intArrayOf(
        ContextCompat.getColor(context, R.color.accent_primary),
        ContextCompat.getColor(context, R.color.gradient_cyan),
        ContextCompat.getColor(context, R.color.accent_primary),
        ContextCompat.getColor(context, R.color.gradient_cyan),
        ContextCompat.getColor(context, R.color.accent_primary)
    )

    private val animator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = ROTATION_DURATION_MS
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            rotationAngle = it.animatedValue as Float
            invalidate()
        }
    }

    init {
        setWillNotDraw(false)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator.start()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        borderRect.set(0f, 0f, width.toFloat(), height.toFloat())

        // Memory leak fix: only recreate shader when dimensions change
        if (cachedShader == null || width != lastWidth || height != lastHeight) {
            cachedShader = SweepGradient(
                width / 2f, height / 2f,
                gradientColors, null
            )
            lastWidth = width
            lastHeight = height
        }

        shaderMatrix.reset()
        shaderMatrix.postRotate(rotationAngle, width / 2f, height / 2f)
        cachedShader!!.setLocalMatrix(shaderMatrix)

        borderPaint.shader = cachedShader
        canvas.drawRoundRect(borderRect, cornerRadiusPx, cornerRadiusPx, borderPaint)
    }
}
