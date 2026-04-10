package com.roei.stagemate.ui.views.venuemap

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF

import android.graphics.Region
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.roei.stagemate.R
import com.roei.stagemate.data.models.Seat
import com.roei.stagemate.data.models.SeatSection
import com.roei.stagemate.data.models.SectionPosition

// Base class for all venue map views (theater, arena, stadium).
// Draws sections as colored shapes and handles tap-to-select with pinch-zoom and pan.
abstract class BaseVenueMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // --- Public data ---

    var sections: List<SeatSection> = emptyList()
        set(value) {
            field = value
            rebuildPaths()
        }

    var selectedSeats: List<Seat> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    var onSectionClicked: ((SeatSection) -> Unit)? = null

    var selectedSection: SeatSection? = null
        set(value) {
            field = value
            invalidate()
        }

    // --- Path cache ---

    protected val sectionPaths = mutableMapOf<SeatSection, Path>()
    protected val sectionCentroids = mutableMapOf<SeatSection, PointF>()
    protected val sectionTextRotation = mutableMapOf<SeatSection, Float>()

    protected abstract fun buildSectionPaths()

    // --- Density helpers ---

    private val density: Float = resources.displayMetrics.density

    protected fun dpToPx(dp: Float): Float = dp * density
    protected fun spToPx(sp: Float): Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics)

    // --- Pre-allocated Paint objects ---

    protected val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    protected val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeWidth = dpToPx(2f)
    }

    protected val selectedStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeWidth = dpToPx(5f)
    }

    protected val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.DEFAULT_BOLD
        textSize = spToPx(11f)
        textAlign = Paint.Align.CENTER
        setShadowLayer(3f, 1f, 1f, Color.BLACK)
    }


    protected val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.RED
    }

    private val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.DEFAULT_BOLD
        textSize = spToPx(8f)
        textAlign = Paint.Align.CENTER
    }

    private val blockedStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.GRAY
        strokeWidth = dpToPx(1f)
    }


    // --- Zoom / Pan state ---

    protected val transformMatrix = Matrix()
    private val inverseMatrix = Matrix()
    private var scaleFactor = 1f
    private var translateX = 0f
    private var translateY = 0f

    private val scaleGestureDetector: ScaleGestureDetector
    private val gestureDetector: GestureDetector

    // --- Temporary objects (avoid allocations in onDraw / touch) ---

    // Memory leak fix: cache parsed colors to avoid string parsing per frame
    private val colorCache = HashMap<String, Int>()

    private val tmpBounds = RectF()
    private val tmpTextBounds = Rect()
    private val tmpPoint = FloatArray(2)
    private val tmpRegion = Region()
    private val tmpClipRegion = Region()

    // --- Init ---

    init {
        scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
        gestureDetector = GestureDetector(context, GestureListener())
    }

    // --- Measurement ---

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val defaultSize = dpToPx(400f).toInt()

        val width = when (MeasureSpec.getMode(widthMeasureSpec)) {
            MeasureSpec.EXACTLY -> MeasureSpec.getSize(widthMeasureSpec)
            MeasureSpec.AT_MOST -> minOf(defaultSize, MeasureSpec.getSize(widthMeasureSpec))
            else -> defaultSize
        }

        val height = when (MeasureSpec.getMode(heightMeasureSpec)) {
            MeasureSpec.EXACTLY -> MeasureSpec.getSize(heightMeasureSpec)
            MeasureSpec.AT_MOST -> minOf(defaultSize, MeasureSpec.getSize(heightMeasureSpec))
            else -> defaultSize
        }

        setMeasuredDimension(width, height)
    }

    // --- Layout ---

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rebuildPaths()
    }

    // --- Drawing ---

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (sectionPaths.isEmpty()) return

        canvas.save()
        canvas.concat(transformMatrix)

        for ((section, path) in sectionPaths) {
            try {
                // Blocked sections: gray out, no labels, non-interactive
                if (section.isBlocked) {
                    fillPaint.color = Color.DKGRAY
                    fillPaint.alpha = 80
                    canvas.drawPath(path, fillPaint)
                    canvas.drawPath(path, blockedStrokePaint)
                    continue
                }

                // Fill with section color (cached to avoid per-frame string parsing)
                fillPaint.color = colorCache.getOrPut(section.color) {
                    try { Color.parseColor(section.color) } catch (_: Exception) { Color.GRAY }
                }
                fillPaint.alpha = 200
                canvas.drawPath(path, fillPaint)

                // Border
                canvas.drawPath(path, strokePaint)

                // Selected highlight (thick white stroke, no shadow for performance)
                if (section == selectedSection) {
                    canvas.drawPath(path, selectedStrokePaint)
                }

                // Section label — auto-fit text to section bounds
                path.computeBounds(tmpBounds, true)
                val hasExactCentroid = section in sectionCentroids
                val sectionWidth = tmpBounds.width() * if (hasExactCentroid) 0.65f else 0.80f
                val sectionHeight = tmpBounds.height() * if (hasExactCentroid) 0.55f else 0.70f
                if (sectionWidth <= 0f || sectionHeight <= 0f) continue

                val centroid = sectionCentroids[section] ?: getPathCentroid(path)

                // Clip canvas to section path — prevents text/badge overflow
                canvas.save()
                canvas.clipPath(path)

                // Skip text for VIP tier (tierIndex == 1) — too narrow for labels
                val isVipTier = (section.position as? SectionPosition.ArenaPosition)?.tierIndex == 1
                val minDrawableHeight = dpToPx(12f)
                if (!isVipTier && sectionHeight > minDrawableHeight) {
                    val isArena = section.position is SectionPosition.ArenaPosition

                    // Build display name: strip generic prefixes
                    var displayName = if (isArena) {
                        section.name
                            .replace(Regex("^(Upper|Lower|VIP|Sec|Section|Tier)\\s+", RegexOption.IGNORE_CASE), "")
                            .trim()
                    } else {
                        section.name
                            .replace(Regex("^(Sec|Section|Tier)\\s+", RegexOption.IGNORE_CASE), "")
                            .trim()
                    }

                    // Estimate how many characters fit in the available width
                    val charWidthEstimate = spToPx(6f)
                    val maxChars = (sectionWidth / charWidthEstimate).toInt().coerceAtLeast(2)

                    if (displayName.length > maxChars) {
                        val words = displayName.split("\\s+".toRegex())
                        displayName = when {
                            // Two lines worth of space: let drawSectionLabel scale it down
                            displayName.length <= maxChars * 2 -> displayName
                            // Multi-word: use abbreviated form
                            words.size > 1 -> words.joinToString(" ") {
                                if (it.length <= 3) it else it.take(1).uppercase() + "."
                            }
                            // Single long word: truncate
                            else -> displayName.take(maxChars)
                        }
                    }

                    // Scale text proportionally to section size
                    val originalTextSize = textPaint.textSize
                    val proportionalSize = if (isArena) {
                        (sectionHeight * 0.35f).coerceIn(spToPx(6f), spToPx(12f))
                    } else {
                        (sectionHeight * 0.28f).coerceIn(spToPx(7f), spToPx(14f))
                    }
                    textPaint.textSize = proportionalSize

                    val rotation = sectionTextRotation[section] ?: 0f
                    if (rotation != 0f) {
                        canvas.save()
                        canvas.rotate(rotation, centroid.x, centroid.y)
                        // Swap width/height constraints when rotated 90°
                        drawSectionLabel(canvas, displayName, centroid.x, centroid.y, sectionHeight, sectionWidth * 0.45f)
                        canvas.restore()
                    } else {
                        drawSectionLabel(canvas, displayName, centroid.x, centroid.y, sectionWidth, sectionHeight * 0.45f)
                    }
                    textPaint.textSize = originalTextSize
                }



                // Badge for selected seats (constrained within section bounds)
                val count = getSelectedCountForSection(section)
                if (count > 0) {
                    val badgeX = minOf(centroid.x + dpToPx(20f), tmpBounds.right - dpToPx(10f))
                    val badgeY = maxOf(centroid.y - dpToPx(16f), tmpBounds.top + dpToPx(10f))
                    drawBadge(canvas, badgeX, badgeY, count)
                }

                canvas.restore()
            } catch (_: Exception) { }
        }

        canvas.restore()
    }

    // --- Touch handling ---

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        return true
    }

    // --- Zoom / Pan gesture listeners ---

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor = (scaleFactor * detector.scaleFactor).coerceIn(0.5f, 3f)
            updateTransformMatrix()
            invalidate()
            return true
        }
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            translateX -= distanceX / scaleFactor
            translateY -= distanceY / scaleFactor
            updateTransformMatrix()
            invalidate()
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val hitSection = hitTest(e.x, e.y)
            if (hitSection != null) {
                selectedSection = hitSection
                onSectionClicked?.invoke(hitSection)
                invalidate()
            }
            return true
        }
    }

    // --- Transform matrix ---

    private fun updateTransformMatrix() {
        transformMatrix.reset()
        transformMatrix.postTranslate(translateX, translateY)
        transformMatrix.postScale(scaleFactor, scaleFactor, width / 2f, height / 2f)
    }

    // --- Hit testing ---

    private fun hitTest(touchX: Float, touchY: Float): SeatSection? {
        transformMatrix.invert(inverseMatrix)
        tmpPoint[0] = touchX
        tmpPoint[1] = touchY
        inverseMatrix.mapPoints(tmpPoint)
        val x = tmpPoint[0].toInt()
        val y = tmpPoint[1].toInt()

        tmpClipRegion.set(0, 0, width * 3, height * 3)

        for ((section, path) in sectionPaths) {
            if (section.isBlocked) continue
            tmpRegion.setEmpty()
            tmpRegion.setPath(path, tmpClipRegion)
            if (tmpRegion.contains(x, y)) {
                return section
            }
        }
        return null
    }

    // --- Helper methods ---

    // Finds the visual center of a path by averaging grid points inside it.
    // More accurate than bounding-box center for arcs, wedges, and irregular shapes.
    protected fun getPathCentroid(path: Path): PointF {
        path.computeBounds(tmpBounds, true)
        if (tmpBounds.isEmpty) return PointF(tmpBounds.centerX(), tmpBounds.centerY())

        // Grid-based visual center: sample points inside the path
        val region = Region()
        val clip = Region(
            tmpBounds.left.toInt(), tmpBounds.top.toInt(),
            tmpBounds.right.toInt(), tmpBounds.bottom.toInt()
        )
        region.setPath(path, clip)

        val stepX = tmpBounds.width() / 10f
        val stepY = tmpBounds.height() / 10f
        if (stepX <= 0f || stepY <= 0f) return PointF(tmpBounds.centerX(), tmpBounds.centerY())

        var sumX = 0f
        var sumY = 0f
        var count = 0

        var y = tmpBounds.top + stepY / 2f
        while (y < tmpBounds.bottom) {
            var x = tmpBounds.left + stepX / 2f
            while (x < tmpBounds.right) {
                if (region.contains(x.toInt(), y.toInt())) {
                    sumX += x
                    sumY += y
                    count++
                }
                x += stepX
            }
            y += stepY
        }

        return if (count > 0) PointF(sumX / count, sumY / count)
        else PointF(tmpBounds.centerX(), tmpBounds.centerY())
    }

    // Draws centered text that auto-scales down to fit within maxWidth/maxHeight.
    protected fun drawSectionLabel(canvas: Canvas, text: String, cx: Float, cy: Float, maxWidth: Float, maxHeight: Float = Float.MAX_VALUE) {
        if (maxWidth <= 0f || text.isEmpty()) return
        val originalSize = textPaint.textSize

        // Auto-scale if text overflows width
        val textWidth = textPaint.measureText(text)
        if (textWidth > maxWidth && textWidth > 0f) {
            textPaint.textSize = originalSize * (maxWidth / textWidth).coerceIn(0.5f, 1f)
        }

        // Auto-scale if text overflows height
        textPaint.getTextBounds(text, 0, text.length, tmpTextBounds)
        val measuredHeight = tmpTextBounds.height().toFloat()
        if (measuredHeight > maxHeight && measuredHeight > 0f) {
            textPaint.textSize = textPaint.textSize * (maxHeight / measuredHeight).coerceIn(0.5f, 1f)
        }

        // Precise vertical centering using getTextBounds
        textPaint.getTextBounds(text, 0, text.length, tmpTextBounds)
        val textHeight = tmpTextBounds.height()

        // After auto-shrink, if the text still does not fit horizontally and contains a space,
        // wrap to one word per line. Otherwise fall through to the existing single-line draw.
        val finalWidth = textPaint.measureText(text)
        if (finalWidth > maxWidth && text.contains(' ')) {
            val words = text.split(' ').filter { it.isNotBlank() }
            val lineHeight = textPaint.textSize * 1.1f
            val totalHeight = lineHeight * words.size
            val firstBaseline = cy - totalHeight / 2f + textPaint.textSize
            for ((i, word) in words.withIndex()) {
                canvas.drawText(word, cx, firstBaseline + i * lineHeight, textPaint)
            }
        } else {
            val baselineY = cy + textHeight / 2f
            canvas.drawText(text, cx, baselineY, textPaint)
        }
        textPaint.textSize = originalSize
    }


    protected fun getSelectedCountForSection(section: SeatSection): Int {
        return selectedSeats.count { it.section == section.name && it.isSelected() }
    }

    fun updateSections(newSections: List<SeatSection>) {
        sections = newSections
    }

    // --- Private helpers ---

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (sectionPaths.isEmpty() && sections.isNotEmpty()) {
            post { rebuildPaths() }
        }
    }

    private fun rebuildPaths() {
        sectionPaths.clear()
        sectionCentroids.clear()
        sectionTextRotation.clear()
        if (width > 0 && height > 0 && sections.isNotEmpty()) {
            buildSectionPaths()
        }
        invalidate()
    }


    private fun drawBadge(canvas: Canvas, cx: Float, cy: Float, count: Int) {
        val radius = dpToPx(8f)
        canvas.drawCircle(cx, cy, radius, badgePaint)
        canvas.drawText(count.toString(), cx, cy + spToPx(3f), badgeTextPaint)
    }

}
