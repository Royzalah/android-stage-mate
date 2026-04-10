package com.roei.stagemate.ui.views.venuemap

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import androidx.core.content.ContextCompat
import com.roei.stagemate.R
import com.roei.stagemate.data.models.SeatSection
import com.roei.stagemate.data.models.SectionPosition
import com.roei.stagemate.data.models.SectionPosition.TheaterPosition
import com.roei.stagemate.utilities.Constants
import kotlin.math.cos
import kotlin.math.sin

// Draws the theater-style venue map with curved seating rows.
// Stage sits at the bottom; seat sections fan out upward as concentric arcs.
class TheaterMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseVenueMapView(context, attrs, defStyleAttr) {

    // --- Density helpers ---

    private val density: Float = resources.displayMetrics.density

    private fun dp(value: Float): Float = value * density
    private fun sp(value: Float): Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)

    // --- Stage paints ---

    private val stageFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.venue_stage_bg)
    }

    private val stageStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeWidth = dp(2f)
    }

    private val stageTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.DEFAULT_BOLD
        textSize = sp(14f)
        textAlign = Paint.Align.CENTER
    }

    private val balconyDashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeWidth = dp(1.5f)
        pathEffect = DashPathEffect(floatArrayOf(dp(6f), dp(4f)), 0f)
    }

    // --- Cached stage rect ---

    private val stageRect = RectF()
    private val stageCornerRadius = dp(8f)

    // --- Reusable RectF for arc math ---

    private val outerOval = RectF()
    private val innerOval = RectF()

    // --- Balcony separator ---
    private val balconySeparatorPath = Path()

    // --- Drawing ---

    override fun onDraw(canvas: Canvas) {
        if (width > 0 && height > 0 && sections.isNotEmpty()) {
            canvas.save()
            canvas.concat(transformMatrix)
            drawStage(canvas)
            if (!balconySeparatorPath.isEmpty) {
                canvas.drawPath(balconySeparatorPath, balconyDashPaint)
            }
            canvas.restore()
        }
        super.onDraw(canvas)
    }

    private fun drawStage(canvas: Canvas) {
        canvas.drawRoundRect(stageRect, stageCornerRadius, stageCornerRadius, stageFillPaint)
        canvas.drawRoundRect(stageRect, stageCornerRadius, stageCornerRadius, stageStrokePaint)
        val textY = stageRect.centerY() - (stageTextPaint.descent() + stageTextPaint.ascent()) / 2f
        canvas.drawText("STAGE", stageRect.centerX(), textY, stageTextPaint)
    }

    // --- Section path construction ---

    override fun buildSectionPaths() {
        val w = width.toFloat()
        val h = height.toFloat()
        val padding = dp(12f)

        // Stage dimensions and position (bottom-center)
        val stageWidth = w * 0.4f
        val stageHeight = dp(40f)
        val stageLeft = (w - stageWidth) / 2f
        val stageTop = h - padding - stageHeight
        stageRect.set(stageLeft, stageTop, stageLeft + stageWidth, stageTop + stageHeight)

        // Fan center point: center of stage's top edge
        val cx = stageRect.centerX()
        val cy = stageRect.top

        // Determine fan angle based on section characteristics
        val fanAngle = determineFanAngle()

        // Start angle so fan is centered pointing upward
        // In Android Canvas: 0° = 3 o'clock, -90° = 12 o'clock
        val startAngle = -(90f + fanAngle / 2f)

        // Resolve tier/column assignments for each section
        val assignments = resolveAssignments()

        // Determine unique tiers and count
        val tiers = assignments.values
            .map { it.tier }
            .distinct()
            .sortedBy { it.ordinal }

        val numTiers = tiers.size.coerceAtLeast(1)

        // Radii
        val minRadius = dp(20f) // gap above stage
        val maxRadius = cy - padding // extend to near top of view
        val totalRadiusRange = (maxRadius - minRadius).coerceAtLeast(dp(60f))

        // Calculate per-tier band height
        val tierGap = dp(3f)
        val balconyGap = dp(10f)
        val totalGaps = tiers.foldIndexed(0f) { index, acc, tier ->
            if (index == 0) acc
            else acc + if (tier == TheaterPosition.Tier.BALCONY) balconyGap else tierGap
        }
        val usableRadius = totalRadiusRange - totalGaps
        val tierBandHeight = usableRadius / numTiers

        // Map each tier to its inner/outer radius
        val tierRadii = mutableMapOf<TheaterPosition.Tier, Pair<Float, Float>>()
        var currentInner = minRadius
        for ((index, tier) in tiers.withIndex()) {
            if (index > 0) {
                currentInner += if (tier == TheaterPosition.Tier.BALCONY) balconyGap else tierGap
            }
            val outerR = currentInner + tierBandHeight
            tierRadii[tier] = Pair(currentInner, outerR)
            currentInner = outerR
        }

        // Build balcony separator arc if BALCONY tier exists
        balconySeparatorPath.reset()
        val balconyRadii = tierRadii[TheaterPosition.Tier.BALCONY]
        if (balconyRadii != null) {
            val separatorR = balconyRadii.first - balconyGap / 2f
            val separatorOval = RectF(cx - separatorR, cy - separatorR, cx + separatorR, cy + separatorR)
            balconySeparatorPath.arcTo(separatorOval, startAngle, fanAngle)
        }

        // Determine columns present per tier
        val columnsPerTier = mutableMapOf<TheaterPosition.Tier, Set<TheaterPosition.Column>>()
        for ((_, pos) in assignments) {
            val existing = columnsPerTier.getOrDefault(pos.tier, emptySet())
            columnsPerTier[pos.tier] = existing + pos.column
        }

        val columnGapAngle = dp(2f) / (maxRadius * Math.PI.toFloat() / 180f) // approx degrees for 2dp arc

        // Build a path for each section
        for ((section, pos) in assignments) {
            val (innerR, outerR) = tierRadii[pos.tier] ?: continue
            val columns = columnsPerTier[pos.tier] ?: setOf(TheaterPosition.Column.CENTER)

            // Compute angle range for this column
            val (colStartAngle, colSweepAngle) = computeColumnAngles(
                pos.column, columns, startAngle, fanAngle, columnGapAngle
            )

            val path = Path()

            outerOval.set(cx - outerR, cy - outerR, cx + outerR, cy + outerR)
            innerOval.set(cx - innerR, cy - innerR, cx + innerR, cy + innerR)

            // Outer arc (clockwise)
            path.arcTo(outerOval, colStartAngle, colSweepAngle)
            // Inner arc (counter-clockwise)
            path.arcTo(innerOval, colStartAngle + colSweepAngle, -colSweepAngle)
            path.close()

            sectionPaths[section] = path

            // Compute centroid at angular midpoint, radial midpoint of the fan arc
            val midAngleRad = Math.toRadians((colStartAngle + colSweepAngle / 2.0).toDouble())
            val midR = (innerR + outerR) / 2f
            sectionCentroids[section] = android.graphics.PointF(
                cx + (midR * cos(midAngleRad)).toFloat(),
                cy + (midR * sin(midAngleRad)).toFloat()
            )
        }
    }

    // --- Fan angle determination ---

    private fun determineFanAngle(): Float {
        // Wide fan for outdoor / grass venues
        val hasGrass = sections.any { Constants.VenueUtils.isStandingSection(it.name) }
        if (hasGrass) return 150f

        // Narrow fan if all sections are center-only
        val allCenter = sections.all { section ->
            val pos = section.position
            pos is TheaterPosition && pos.column == TheaterPosition.Column.CENTER
        }
        if (allCenter) return 90f

        return 120f
    }

    // --- Assignment resolution ---

    // Maps each section to a TheaterPosition. Uses explicit positions when available,
    // otherwise auto-distributes sections across tiers and columns.
    private fun resolveAssignments(): Map<SeatSection, TheaterPosition> {
        val result = mutableMapOf<SeatSection, TheaterPosition>()

        // Separate sections with and without explicit positions
        val explicitSections = mutableListOf<Pair<SeatSection, TheaterPosition>>()
        val unpositioned = mutableListOf<SeatSection>()

        for (section in sections) {
            val pos = section.position
            if (pos is TheaterPosition) {
                explicitSections.add(section to pos)
            } else {
                unpositioned.add(section)
            }
        }

        // Add explicitly positioned sections
        for ((section, pos) in explicitSections) {
            result[section] = pos
        }

        // Auto-distribute unpositioned sections
        if (unpositioned.isNotEmpty()) {
            val autoPositions = autoDistribute(unpositioned)
            result.putAll(autoPositions)
        }

        return result
    }

    // Auto-distributes sections without explicit positions:
    // 1-3 sections go in CENTER column across tiers, 4+ spread across LEFT/CENTER/RIGHT.
    // Accessible sections are placed at BACK tier edges.
    private fun autoDistribute(unpositioned: List<SeatSection>): Map<SeatSection, TheaterPosition> {
        val result = mutableMapOf<SeatSection, TheaterPosition>()

        // Separate accessible sections
        val accessible = unpositioned.filter { it.name.lowercase().contains("accessible") }
        val regular = unpositioned.filterNot { it.name.lowercase().contains("accessible") }

        val tierOrder = listOf(
            TheaterPosition.Tier.FRONT,
            TheaterPosition.Tier.MIDDLE,
            TheaterPosition.Tier.BACK,
            TheaterPosition.Tier.BALCONY
        )

        if (regular.size <= 3) {
            // Simple: each section in CENTER column, successive tiers
            for ((index, section) in regular.withIndex()) {
                val tier = tierOrder[index.coerceAtMost(tierOrder.lastIndex)]
                result[section] = TheaterPosition(TheaterPosition.Column.CENTER, tier)
            }
        } else {
            // Distribute across columns: fill tiers row by row (LEFT, CENTER, RIGHT)
            val columnOrder = listOf(
                TheaterPosition.Column.LEFT,
                TheaterPosition.Column.CENTER,
                TheaterPosition.Column.RIGHT
            )
            for ((index, section) in regular.withIndex()) {
                val tierIndex = (index / 3).coerceAtMost(tierOrder.lastIndex)
                val colIndex = index % 3
                result[section] = TheaterPosition(columnOrder[colIndex], tierOrder[tierIndex])
            }
        }

        // Accessible sections at bottom edges of the fan (BACK tier, side columns)
        for ((index, section) in accessible.withIndex()) {
            val col = if (index % 2 == 0) TheaterPosition.Column.LEFT else TheaterPosition.Column.RIGHT
            result[section] = TheaterPosition(col, TheaterPosition.Tier.BACK)
        }

        return result
    }

    // --- Column angle computation ---

    // Returns (startAngle, sweepAngle) in degrees for a column within a tier,
    // splitting the fan angle evenly and adding gaps between adjacent columns.
    private fun computeColumnAngles(
        column: TheaterPosition.Column,
        columnsInTier: Set<TheaterPosition.Column>,
        fanStartAngle: Float,
        fanAngle: Float,
        gapAngle: Float
    ): Pair<Float, Float> {
        // If only one column in this tier, use the full fan
        if (columnsInTier.size <= 1) {
            return Pair(fanStartAngle, fanAngle)
        }

        val sortedColumns = columnsInTier.sortedBy { it.ordinal }
        val numCols = sortedColumns.size
        val totalGaps = (numCols - 1) * gapAngle
        val usableSweep = fanAngle - totalGaps
        val colSweep = usableSweep / numCols

        val colIndex = sortedColumns.indexOf(column)
        if (colIndex < 0) {
            // Fallback: shouldn't happen
            return Pair(fanStartAngle, fanAngle)
        }

        val colStart = fanStartAngle + colIndex * (colSweep + gapAngle)
        return Pair(colStart, colSweep)
    }
}
