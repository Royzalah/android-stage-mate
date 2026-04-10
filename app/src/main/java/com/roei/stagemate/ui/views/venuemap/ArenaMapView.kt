package com.roei.stagemate.ui.views.venuemap

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.roei.stagemate.R
import com.roei.stagemate.data.models.SeatSection
import com.roei.stagemate.data.models.SectionPosition
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// Draws a 360-degree oval arena map with concentric seating tiers around a court.
// Supports basketball court markings (FIBA proportions) or a stage in concert mode.
class ArenaMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseVenueMapView(context, attrs, defStyleAttr) {

    // --- Court drawing paints ---

    private val courtFloorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.venue_court_hardwood)
        style = Paint.Style.FILL
    }

    private val courtLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(1.5f)
    }

    private val courtKeyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.venue_court_key)
        style = Paint.Style.FILL
    }

    private val courtTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = spToPx(9f)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        alpha = 180
    }

    private val courtCenterCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.venue_court_center_circle)
        style = Paint.Style.FILL
    }

    private val tierSeparatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.venue_tier_separator)
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(2f)
    }

    private val courtDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        strokeWidth = dpToPx(1.5f)
    }

    private val arenaFloorBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#1A1A2E")
    }

    // --- Court geometry ---

    private val courtRect = RectF()
    private var courtCornerRadius = 0f
    private var isStageMode = false

    // Cached tier ovals for drawing separators
    private val cachedTierOvals = mutableListOf<RectF>()

    // Set to true to show a stage instead of a basketball court.
    var showStage: Boolean
        get() = isStageMode
        set(value) {
            isStageMode = value
            invalidate()
        }

    // Tier proportions for the 3-tier layout (Lower, VIP, Upper)
    // VIP ring is intentionally thinner than the Lower and Upper bowls.
    private val tierProportions = floatArrayOf(0.40f, 0.15f, 0.45f)

    private companion object {
        const val STAGE_HEIGHT_RATIO = 0.25f
    }

    // --- Section path construction ---

    override fun buildSectionPaths() {
        val cx = width / 2f
        val cy = height / 2f

        val tierCount = sections.mapNotNull {
            (it.position as? SectionPosition.ArenaPosition)?.tierIndex
        }.maxOrNull()?.plus(1) ?: 1

        // Outer ellipse boundary — clearly wider than tall for an elliptical shape
        val outerRx = width * 0.46f
        val outerRy = height * 0.37f

        // Court dimensions — FIBA basketball proportions (28m x 15m = 1.87:1)
        val courtPadding = dpToPx(27f)  // visible dark void between court and Lower Tier stands
        val courtHalfW = outerRx * 0.28f
        val courtHalfH = courtHalfW / 1.87f

        // Inner seating boundary — courtRect + padding
        val innerSeatRx = courtHalfW + courtPadding
        val innerSeatRy = courtHalfH + courtPadding

        // Court rectangle — centered, never touches seating
        courtRect.set(cx - courtHalfW, cy - courtHalfH, cx + courtHalfW, cy + courtHalfH)
        courtCornerRadius = dpToPx(4f)

        // Tier gap between concentric rings (visual separator)
        val tierGap = dpToPx(3f)

        // Build N+1 concentric elliptical boundaries using proportional distribution
        cachedTierOvals.clear()
        val totalRadialDepthX = outerRx - innerSeatRx
        val totalRadialDepthY = outerRy - innerSeatRy

        // tierOvals[0] = inner seat boundary, tierOvals[tierCount] = outer boundary
        val tierOvals = mutableListOf<RectF>()
        var accumulatedFractionX = 0f
        var accumulatedFractionY = 0f

        // First oval = inner seating boundary
        tierOvals.add(RectF(cx - innerSeatRx, cy - innerSeatRy, cx + innerSeatRx, cy + innerSeatRy))

        for (t in 0 until tierCount) {
            // Use explicit proportions if available, otherwise distribute evenly
            val proportion = if (t < tierProportions.size && tierCount <= tierProportions.size) {
                tierProportions[t]
            } else {
                1f / tierCount
            }

            accumulatedFractionX += proportion
            accumulatedFractionY += proportion

            val rx = innerSeatRx + totalRadialDepthX * accumulatedFractionX
            val ry = innerSeatRy + totalRadialDepthY * accumulatedFractionY

            tierOvals.add(RectF(cx - rx, cy - ry, cx + rx, cy + ry))
        }

        // Cache for drawing tier separators
        cachedTierOvals.addAll(tierOvals)

        // Apply tier gaps by shrinking outer edge of each tier slightly
        val adjustedTierOvals = tierOvals.toMutableList()
        for (t in 0 until tierCount) {
            val outer = adjustedTierOvals[t + 1]
            // Shrink the outer boundary inward by half the gap (gap is split between adjacent tiers)
            if (t < tierCount - 1) {
                adjustedTierOvals[t + 1] = RectF(
                    outer.left + tierGap / 2f,
                    outer.top + tierGap / 2f,
                    outer.right - tierGap / 2f,
                    outer.bottom - tierGap / 2f
                )
            }
        }

        val hasExplicitPositions = sections.any { it.position is SectionPosition.ArenaPosition }
        if (hasExplicitPositions) {
            buildExplicitPaths(adjustedTierOvals)
        } else {
            buildAutoDistributedPaths(adjustedTierOvals, tierCount)
        }

        if (isStageMode) {
            buildFloorZonePaths()
        }
    }

    // Builds rectangular floor zone paths below the stage (concert mode only).
    private fun buildFloorZonePaths() {
        val floorSections = sections.filter { it.position is SectionPosition.FloorPosition }
        if (floorSections.isEmpty()) return

        val stageBottom = courtRect.top + courtRect.height() * STAGE_HEIGHT_RATIO + dpToPx(2f)
        val floorBottom = courtRect.bottom
        val floorHeight = floorBottom - stageBottom
        if (floorHeight <= 0f) return

        val gap = dpToPx(2f)
        val inset = dpToPx(3f)

        for (section in floorSections) {
            val pos = section.position as SectionPosition.FloorPosition
            val zoneHeight = (floorHeight - gap * (pos.totalZones - 1)) / pos.totalZones
            val top = stageBottom + pos.zoneIndex * (zoneHeight + gap)
            val bottom = top + zoneHeight

            val path = Path().apply {
                addRoundRect(
                    RectF(courtRect.left + inset, top, courtRect.right - inset, bottom),
                    dpToPx(3f), dpToPx(3f), Path.Direction.CW
                )
            }
            sectionPaths[section] = path
        }
    }

    // Places each section in its tier ring at the angle given by ArenaPosition.
    private fun buildExplicitPaths(tierOvals: List<RectF>) {
        val maxTier = tierOvals.size - 2  // max valid tierIndex

        for (section in sections) {
            val arenaPos = section.position as? SectionPosition.ArenaPosition ?: continue
            val tierIdx = arenaPos.tierIndex.coerceIn(0, maxTier)
            val path = Path()

            val innerOval = tierOvals[tierIdx]
            val outerOval = tierOvals[tierIdx + 1]

            // No gap between sections — use full sweep angle
            val effectiveSweep = arenaPos.sweepAngleDeg

            buildPolygonWedge(path, outerOval, innerOval, arenaPos.startAngleDeg, effectiveSweep, segments = 24)
            sectionPaths[section] = path

            // Compute centroid at angular midpoint, radial midpoint of the wedge
            val cx = width / 2f
            val cy = height / 2f
            val midAngleRad = Math.toRadians((arenaPos.startAngleDeg + arenaPos.sweepAngleDeg / 2.0).toDouble())
            val midRx = (innerOval.width() / 2f + outerOval.width() / 2f) / 2f
            val midRy = (innerOval.height() / 2f + outerOval.height() / 2f) / 2f
            sectionCentroids[section] = android.graphics.PointF(
                cx + (midRx * cos(midAngleRad)).toFloat(),
                cy + (midRy * sin(midAngleRad)).toFloat()
            )
        }
    }

    // Splits sections evenly across tiers when no explicit positions exist.
    private fun buildAutoDistributedPaths(tierOvals: List<RectF>, tierCount: Int) {
        // Group sections into tiers — split evenly by order
        val sectionsPerTier = sections.size / tierCount.coerceAtLeast(1)
        val tierGroups = mutableMapOf<Int, MutableList<SeatSection>>()

        for ((index, section) in sections.withIndex()) {
            val tierIdx = (index / sectionsPerTier.coerceAtLeast(1)).coerceAtMost(tierCount - 1)
            tierGroups.getOrPut(tierIdx) { mutableListOf() }.add(section)
        }

        for ((tierIdx, tierSections) in tierGroups) {
            if (tierIdx >= tierOvals.size - 1) continue
            val innerOval = tierOvals[tierIdx]
            val outerOval = tierOvals[tierIdx + 1]

            val sweepPerSection = 360f / tierSections.size
            for ((sectionIdx, section) in tierSections.withIndex()) {
                val path = Path()
                val startAngle = sectionIdx * sweepPerSection
                buildPolygonWedge(path, outerOval, innerOval, startAngle, sweepPerSection, segments = 24)
                sectionPaths[section] = path

                // Compute centroid at angular midpoint, radial midpoint
                val cx = width / 2f
                val cy = height / 2f
                val midAngleRad = Math.toRadians((startAngle + sweepPerSection / 2.0).toDouble())
                val midRx = (innerOval.width() / 2f + outerOval.width() / 2f) / 2f
                val midRy = (innerOval.height() / 2f + outerOval.height() / 2f) / 2f
                sectionCentroids[section] = android.graphics.PointF(
                    cx + (midRx * cos(midAngleRad)).toFloat(),
                    cy + (midRy * sin(midAngleRad)).toFloat()
                )
            }
        }
    }

    // Builds a closed wedge between two concentric ellipses using arcTo for smooth curves.
    private fun buildPolygonWedge(
        path: Path,
        outerOval: RectF,
        innerOval: RectF,
        startAngle: Float,
        sweepAngle: Float,
        @Suppress("UNUSED_PARAMETER") segments: Int = 1  // kept for API compatibility, arcs are always smooth
    ) {
        if (sweepAngle <= 0f) return
        if (outerOval.width() <= 0f || outerOval.height() <= 0f ||
            innerOval.width() <= 0f || innerOval.height() <= 0f) return

        // Outer arc: clockwise from startAngle for sweepAngle degrees
        path.arcTo(outerOval, startAngle, sweepAngle, true)

        // Inner arc: counter-clockwise (negative sweep) back to start
        path.arcTo(innerOval, startAngle + sweepAngle, -sweepAngle)

        path.close()
    }

    // --- Drawing ---

    override fun onDraw(canvas: Canvas) {
        if (sectionPaths.isEmpty()) {
            super.onDraw(canvas)
            return
        }

        canvas.save()
        canvas.concat(transformMatrix)
        drawCourt(canvas)
        drawTierSeparators(canvas)
        canvas.restore()

        super.onDraw(canvas)
    }

    private fun drawTierSeparators(canvas: Canvas) {
        for (i in 1 until cachedTierOvals.size - 1) {
            val oval = cachedTierOvals[i]
            canvas.drawOval(oval, tierSeparatorPaint)
        }
    }

    private fun drawCourt(canvas: Canvas) {
        if (courtRect.isEmpty) return

        if (isStageMode) {
            drawStageMarkings(canvas)
        } else {
            drawBasketballCourt(canvas)
        }
    }

    // --- Basketball court (FIBA proportions) ---

    private fun drawBasketballCourt(canvas: Canvas) {
        val cw = courtRect.width()
        val ch = courtRect.height()
        if (cw <= 0f || ch <= 0f) return

        val cx = courtRect.centerX()
        val cy = courtRect.centerY()

        try {
            // 1. Court floor (hardwood brown)
            canvas.drawRoundRect(courtRect, courtCornerRadius, courtCornerRadius, courtFloorPaint)

            // 2. Court boundary
            canvas.drawRoundRect(courtRect, courtCornerRadius, courtCornerRadius, courtLinePaint)

            // 3. Half-court line (vertical center)
            canvas.drawLine(cx, courtRect.top, cx, courtRect.bottom, courtLinePaint)

            // 4. Center tip-off circle
            val centerR = min(cw, ch) * 0.22f
            canvas.drawCircle(cx, cy, centerR, courtCenterCirclePaint)
            canvas.drawCircle(cx, cy, centerR, courtLinePaint)
            canvas.drawCircle(cx, cy, dpToPx(2f), courtDotPaint)

            // 5. Free-throw lanes (the key / paint)
            // FIBA key: 5.8m wide x 4.9m from baseline, court is 28m x 15m
            val keyW = cw * 0.175f
            val keyH = ch * 0.387f

            val leftKeyRect = RectF(courtRect.left, cy - keyH / 2f, courtRect.left + keyW, cy + keyH / 2f)
            canvas.drawRect(leftKeyRect, courtKeyPaint)
            canvas.drawRect(leftKeyRect, courtLinePaint)

            val rightKeyRect = RectF(courtRect.right - keyW, cy - keyH / 2f, courtRect.right, cy + keyH / 2f)
            canvas.drawRect(rightKeyRect, courtKeyPaint)
            canvas.drawRect(rightKeyRect, courtLinePaint)

            // 6. Free-throw semicircles
            val ftRadius = keyH / 2f

            val leftFtOval = RectF(
                courtRect.left + keyW - ftRadius, cy - ftRadius,
                courtRect.left + keyW + ftRadius, cy + ftRadius
            )
            canvas.drawArc(leftFtOval, -90f, 180f, false, courtLinePaint)

            val rightFtOval = RectF(
                courtRect.right - keyW - ftRadius, cy - ftRadius,
                courtRect.right - keyW + ftRadius, cy + ftRadius
            )
            canvas.drawArc(rightFtOval, 90f, 180f, false, courtLinePaint)

            // 7. Three-point arcs
            val basketOffsetX = cw * 0.056f
            val threePointR = cw * 0.241f

            val leftBasketX = courtRect.left + basketOffsetX
            val left3Oval = RectF(
                leftBasketX - threePointR, cy - threePointR,
                leftBasketX + threePointR, cy + threePointR
            )
            canvas.drawArc(left3Oval, -68f, 136f, false, courtLinePaint)

            val left3SideY = cy - threePointR * sin(Math.toRadians(68.0)).toFloat()
            val left3SideYBot = cy + threePointR * sin(Math.toRadians(68.0)).toFloat()
            canvas.drawLine(courtRect.left, left3SideY, courtRect.left, cy - keyH * 0.75f, courtLinePaint)
            canvas.drawLine(courtRect.left, left3SideYBot, courtRect.left, cy + keyH * 0.75f, courtLinePaint)

            val rightBasketX = courtRect.right - basketOffsetX
            val right3Oval = RectF(
                rightBasketX - threePointR, cy - threePointR,
                rightBasketX + threePointR, cy + threePointR
            )
            canvas.drawArc(right3Oval, 112f, 136f, false, courtLinePaint)

            canvas.drawLine(courtRect.right, left3SideY, courtRect.right, cy - keyH * 0.75f, courtLinePaint)
            canvas.drawLine(courtRect.right, left3SideYBot, courtRect.right, cy + keyH * 0.75f, courtLinePaint)

            // 8. Restricted area arcs
            val restrictedR = ch * 0.085f
            val leftRaOval = RectF(
                leftBasketX - restrictedR, cy - restrictedR,
                leftBasketX + restrictedR, cy + restrictedR
            )
            canvas.drawArc(leftRaOval, -90f, 180f, false, courtLinePaint)

            val rightRaOval = RectF(
                rightBasketX - restrictedR, cy - restrictedR,
                rightBasketX + restrictedR, cy + restrictedR
            )
            canvas.drawArc(rightRaOval, 90f, 180f, false, courtLinePaint)

            // 9. "COURT" label
            canvas.drawText("COURT", cx, cy + spToPx(3f), courtTextPaint)

        } catch (_: Exception) { }
    }

    // --- Stage mode (concert/show) ---

    private fun drawStageMarkings(canvas: Canvas) {
        val stageHeight = courtRect.height() * STAGE_HEIGHT_RATIO
        val stageRect = RectF(courtRect.left, courtRect.top, courtRect.right, courtRect.top + stageHeight)

        canvas.drawRoundRect(stageRect, courtCornerRadius, courtCornerRadius, courtFloorPaint)
        canvas.drawRoundRect(stageRect, courtCornerRadius, courtCornerRadius, courtLinePaint)

        val inset = dpToPx(4f)
        val innerRect = RectF(
            stageRect.left + inset, stageRect.top + inset,
            stageRect.right - inset, stageRect.bottom - inset
        )
        canvas.drawRoundRect(innerRect, courtCornerRadius / 2f, courtCornerRadius / 2f, courtLinePaint)

        courtTextPaint.textSize = spToPx(12f)
        canvas.drawText("STAGE", stageRect.centerX(), stageRect.centerY() + spToPx(4f), courtTextPaint)
        courtTextPaint.textSize = spToPx(9f)

        val floorRect = RectF(courtRect.left, stageRect.bottom + dpToPx(2f), courtRect.right, courtRect.bottom)
        if (floorRect.height() > 0f) {
            canvas.drawRoundRect(floorRect, courtCornerRadius, courtCornerRadius, arenaFloorBgPaint)
        }
    }
}
