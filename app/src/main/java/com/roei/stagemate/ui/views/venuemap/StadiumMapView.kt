package com.roei.stagemate.ui.views.venuemap

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
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
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// Draws a football stadium map with semicircular north/south ends.
// Sections wrap around the pitch on all four sides plus corners.
class StadiumMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseVenueMapView(context, attrs, defStyleAttr) {

    // --- Density helpers ---

    private val density: Float = resources.displayMetrics.density

    private fun dp(dp: Float): Float = dp * density
    private fun sp(sp: Float): Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics)

    // --- Pre-allocated paints ---

    private val pitchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.venue_pitch_green)
    }

    private val pitchLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeWidth = dp(2f)
    }

    private val pitchTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.DEFAULT_BOLD
        textSize = sp(12f)
        textAlign = Paint.Align.CENTER
        alpha = 120
    }

    private val stadiumBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.venue_stadium_bg)
    }

    private val stageBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.venue_pitch_green)
    }

    private val floorBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#1A1A2E")
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    // --- Cached geometry ---

    private val outerRect = RectF()
    private val pitchRect = RectF()
    private val stadiumPath = Path()

    private var capRadius = 0f

    private var sectionGap = 0f

    private var isStageMode = false

    private companion object {
        const val STAGE_HEIGHT_RATIO = 0.25f
    }

    // Set to true to show a stage instead of a football pitch.
    var showStage: Boolean
        get() = isStageMode
        set(value) {
            isStageMode = value
            invalidate()
        }

    // --- Pitch drawing ---

    override fun onDraw(canvas: Canvas) {
        if (sectionPaths.isNotEmpty()) {
            canvas.save()
            canvas.concat(transformMatrix)
            drawStadiumBackground(canvas)
            canvas.restore()
        }
        super.onDraw(canvas)
    }

    private fun drawStadiumBackground(canvas: Canvas) {
        canvas.drawPath(stadiumPath, stadiumBgPaint)

        if (isStageMode) {
            drawStageArea(canvas)
        } else {
            drawFootballPitch(canvas)
        }
    }

    private fun drawStageArea(canvas: Canvas) {
        val stageRect = getStageRect()

        canvas.drawRoundRect(stageRect, dp(8f), dp(8f), stageBgPaint)
        canvas.drawRoundRect(stageRect, dp(8f), dp(8f), pitchLinePaint)

        val inset = dp(4f)
        val innerRect = RectF(
            stageRect.left + inset, stageRect.top + inset,
            stageRect.right - inset, stageRect.bottom - inset
        )
        canvas.drawRoundRect(innerRect, dp(4f), dp(4f), pitchLinePaint)

        val cx = stageRect.centerX()
        val cy = stageRect.centerY()
        canvas.drawText("STAGE", cx, cy + sp(5f), pitchTextPaint)

        val floorRect = RectF(pitchRect.left, stageRect.bottom + dp(2f), pitchRect.right, pitchRect.bottom)
        if (floorRect.height() > 0f) {
            canvas.drawRect(floorRect, floorBgPaint)
        }
    }

    private fun getStageRect(): RectF {
        val stageHeight = pitchRect.height() * STAGE_HEIGHT_RATIO
        return RectF(pitchRect.left, pitchRect.top, pitchRect.right, pitchRect.top + stageHeight)
    }

    private fun drawFootballPitch(canvas: Canvas) {
        canvas.drawRect(pitchRect, pitchPaint)
        canvas.drawRect(pitchRect, pitchLinePaint)

        val pw = pitchRect.width()
        val ph = pitchRect.height()
        val cx = pitchRect.centerX()
        val cy = pitchRect.centerY()

        // Center line (horizontal, splitting north/south halves)
        canvas.drawLine(pitchRect.left, cy, pitchRect.right, cy, pitchLinePaint)

        // Center circle
        val centerCircleRadius = min(pw, ph) * 0.12f
        canvas.drawCircle(cx, cy, centerCircleRadius, pitchLinePaint)

        // Center dot
        canvas.drawCircle(cx, cy, dp(3f), dotPaint)

        // Penalty areas (top = north, bottom = south)
        val penaltyWidth = pw * 0.6f
        val penaltyHeight = ph * 0.18f
        val penaltyLeft = cx - penaltyWidth / 2f
        val penaltyRight = cx + penaltyWidth / 2f

        // North penalty area
        canvas.drawRect(penaltyLeft, pitchRect.top, penaltyRight, pitchRect.top + penaltyHeight, pitchLinePaint)
        // South penalty area
        canvas.drawRect(penaltyLeft, pitchRect.bottom - penaltyHeight, penaltyRight, pitchRect.bottom, pitchLinePaint)

        // Goal areas (smaller boxes inside penalty areas)
        val goalWidth = pw * 0.3f
        val goalHeight = ph * 0.08f
        val goalLeft = cx - goalWidth / 2f
        val goalRight = cx + goalWidth / 2f

        // North goal area
        canvas.drawRect(goalLeft, pitchRect.top, goalRight, pitchRect.top + goalHeight, pitchLinePaint)
        // South goal area
        canvas.drawRect(goalLeft, pitchRect.bottom - goalHeight, goalRight, pitchRect.bottom, pitchLinePaint)

        // Penalty dots
        val penaltyDotOffset = ph * 0.14f
        canvas.drawCircle(cx, pitchRect.top + penaltyDotOffset, dp(2f), dotPaint)
        canvas.drawCircle(cx, pitchRect.bottom - penaltyDotOffset, dp(2f), dotPaint)

        // Penalty arcs (the curved part outside the penalty box)
        val penaltyArcRadius = penaltyHeight * 0.6f
        val arcRect = RectF()

        // North penalty arc
        arcRect.set(
            cx - penaltyArcRadius, pitchRect.top + penaltyDotOffset - penaltyArcRadius,
            cx + penaltyArcRadius, pitchRect.top + penaltyDotOffset + penaltyArcRadius
        )
        canvas.drawArc(arcRect, 0f, 180f, false, pitchLinePaint)

        // South penalty arc
        arcRect.set(
            cx - penaltyArcRadius, pitchRect.bottom - penaltyDotOffset - penaltyArcRadius,
            cx + penaltyArcRadius, pitchRect.bottom - penaltyDotOffset + penaltyArcRadius
        )
        canvas.drawArc(arcRect, 180f, 180f, false, pitchLinePaint)

        // Corner arcs (quarter circles at the four pitch corners)
        val cornerArcRadius = dp(8f)

        // Top-left corner
        arcRect.set(
            pitchRect.left - cornerArcRadius, pitchRect.top - cornerArcRadius,
            pitchRect.left + cornerArcRadius, pitchRect.top + cornerArcRadius
        )
        canvas.drawArc(arcRect, 0f, 90f, false, pitchLinePaint)

        // Top-right corner
        arcRect.set(
            pitchRect.right - cornerArcRadius, pitchRect.top - cornerArcRadius,
            pitchRect.right + cornerArcRadius, pitchRect.top + cornerArcRadius
        )
        canvas.drawArc(arcRect, 90f, 90f, false, pitchLinePaint)

        // Bottom-right corner
        arcRect.set(
            pitchRect.right - cornerArcRadius, pitchRect.bottom - cornerArcRadius,
            pitchRect.right + cornerArcRadius, pitchRect.bottom + cornerArcRadius
        )
        canvas.drawArc(arcRect, 180f, 90f, false, pitchLinePaint)

        // Bottom-left corner
        arcRect.set(
            pitchRect.left - cornerArcRadius, pitchRect.bottom - cornerArcRadius,
            pitchRect.left + cornerArcRadius, pitchRect.bottom + cornerArcRadius
        )
        canvas.drawArc(arcRect, 270f, 90f, false, pitchLinePaint)
    }

    // --- Section path construction ---

    override fun buildSectionPaths() {
        val padding = dp(16f)
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        outerRect.set(padding, padding, viewWidth - padding, viewHeight - padding)

        // The stadium shape: long sides east/west are straight, short sides north/south are semicircular arcs.
        // Cap radius = half the width of the stadium.
        capRadius = outerRect.width() / 2f

        // If cap radius would be taller than half the height, clamp it
        val maxCapRadius = outerRect.height() * 0.2f
        capRadius = min(capRadius, maxCapRadius)

        // Pitch: centered rectangle, proportional to stadium
        val pitchWidthRatio = 0.50f
        val pitchHeightRatio = 0.60f
        val pitchW = outerRect.width() * pitchWidthRatio
        val pitchH = outerRect.height() * pitchHeightRatio
        val pitchCx = outerRect.centerX()
        val pitchCy = outerRect.centerY()
        pitchRect.set(
            pitchCx - pitchW / 2f,
            pitchCy - pitchH / 2f,
            pitchCx + pitchW / 2f,
            pitchCy + pitchH / 2f
        )

        sectionGap = dp(4f)

        buildStadiumOutlinePath()

        val resolvedSections = resolveSectionPositions(sections)

        for ((section, position) in resolvedSections) {
            val path = buildPathForSection(position)
            if (path != null) {
                sectionPaths[section] = path
                computeSectionCentroid(section, position)
            }
        }

        if (isStageMode) {
            buildFloorZonePaths()
        }
    }

    // Builds rectangular floor zone paths below the stage (concert mode only).
    private fun buildFloorZonePaths() {
        val floorSections = sections.filter { it.position is SectionPosition.FloorPosition }
        if (floorSections.isEmpty()) return

        val stageBottom = pitchRect.top + pitchRect.height() * STAGE_HEIGHT_RATIO + dp(2f)
        val floorBottom = pitchRect.bottom
        val floorHeight = floorBottom - stageBottom
        if (floorHeight <= 0f) return

        val gap = dp(2f)
        val inset = dp(4f)

        for (section in floorSections) {
            val pos = section.position as SectionPosition.FloorPosition
            val zoneHeight = (floorHeight - gap * (pos.totalZones - 1)) / pos.totalZones
            val top = stageBottom + pos.zoneIndex * (zoneHeight + gap)
            val bottom = top + zoneHeight

            val path = Path().apply {
                addRoundRect(
                    RectF(pitchRect.left + inset, top, pitchRect.right - inset, bottom),
                    dp(4f), dp(4f), Path.Direction.CW
                )
            }
            sectionPaths[section] = path
        }
    }

    // Builds the stadium outline: straight east/west sides, semicircular north/south caps.
    private fun buildStadiumOutlinePath() {
        stadiumPath.reset()

        val left = outerRect.left
        val right = outerRect.right
        val top = outerRect.top + capRadius
        val bottom = outerRect.bottom - capRadius

        stadiumPath.moveTo(left, top)

        // North cap semicircle (left to right)
        val northCapRect = RectF(left, outerRect.top, right, outerRect.top + capRadius * 2f)
        stadiumPath.arcTo(northCapRect, 180f, 180f, false)

        stadiumPath.lineTo(right, bottom)

        // South cap semicircle (right to left)
        val southCapRect = RectF(left, outerRect.bottom - capRadius * 2f, right, outerRect.bottom)
        stadiumPath.arcTo(southCapRect, 0f, 180f, false)

        stadiumPath.lineTo(left, top)

        stadiumPath.close()
    }

    // Uses explicit StadiumPosition if available, otherwise auto-distributes
    // sections around four sides (first section goes to VIP on WEST).
    private fun resolveSectionPositions(
        sectionList: List<SeatSection>
    ): List<Pair<SeatSection, SectionPosition.StadiumPosition>> {
        // Check if any sections already have StadiumPosition
        val hasPositions = sectionList.any { it.position is SectionPosition.StadiumPosition }

        if (hasPositions) {
            return sectionList.mapNotNull { section ->
                val pos = section.position as? SectionPosition.StadiumPosition
                if (pos != null) section to pos else null
            }
        }

        // Auto-distribute fallback
        if (sectionList.isEmpty()) return emptyList()

        val result = mutableListOf<Pair<SeatSection, SectionPosition.StadiumPosition>>()
        val sides = listOf(
            SectionPosition.StadiumPosition.Side.WEST,
            SectionPosition.StadiumPosition.Side.EAST,
            SectionPosition.StadiumPosition.Side.NORTH,
            SectionPosition.StadiumPosition.Side.SOUTH
        )

        if (sectionList.size == 1) {
            result.add(
                sectionList[0] to SectionPosition.StadiumPosition(
                    side = SectionPosition.StadiumPosition.Side.WEST,
                    indexOnSide = 0,
                    totalOnSide = 1
                )
            )
            return result
        }

        // First section = VIP on WEST
        result.add(
            sectionList[0] to SectionPosition.StadiumPosition(
                side = SectionPosition.StadiumPosition.Side.WEST,
                indexOnSide = 0,
                totalOnSide = 1
            )
        )

        // Distribute remaining sections across all four sides
        val remaining = sectionList.drop(1)
        val perSide = mutableMapOf<SectionPosition.StadiumPosition.Side, MutableList<SeatSection>>()
        for (side in sides) {
            perSide[side] = mutableListOf()
        }

        remaining.forEachIndexed { index, section ->
            val sideIndex = (index + 1) % sides.size  // +1 to skip WEST which already has VIP
            perSide[sides[sideIndex]]?.add(section)
        }

        for ((side, sectionsOnSide) in perSide) {
            if (side == SectionPosition.StadiumPosition.Side.WEST && sectionsOnSide.isEmpty()) continue
            val total = if (side == SectionPosition.StadiumPosition.Side.WEST) {
                sectionsOnSide.size + 1  // +1 for VIP already placed
            } else {
                sectionsOnSide.size
            }
            if (total == 0) continue

            val startIndex = if (side == SectionPosition.StadiumPosition.Side.WEST) 1 else 0
            sectionsOnSide.forEachIndexed { i, section ->
                result.add(
                    section to SectionPosition.StadiumPosition(
                        side = side,
                        indexOnSide = startIndex + i,
                        totalOnSide = total
                    )
                )
            }
            // Update VIP totalOnSide if more WEST sections were added
            if (side == SectionPosition.StadiumPosition.Side.WEST && sectionsOnSide.isNotEmpty()) {
                val vipIndex = result.indexOfFirst {
                    it.second.side == SectionPosition.StadiumPosition.Side.WEST && it.second.indexOnSide == 0
                }
                if (vipIndex >= 0) {
                    result[vipIndex] = result[vipIndex].first to SectionPosition.StadiumPosition(
                        side = SectionPosition.StadiumPosition.Side.WEST,
                        indexOnSide = 0,
                        totalOnSide = total
                    )
                }
            }
        }

        return result
    }

    private fun buildPathForSection(position: SectionPosition.StadiumPosition): Path? {
        return when (position.side) {
            SectionPosition.StadiumPosition.Side.EAST ->
                buildStraightSidePath(position, isEast = true)
            SectionPosition.StadiumPosition.Side.WEST ->
                buildStraightSidePath(position, isEast = false)
            SectionPosition.StadiumPosition.Side.NORTH ->
                buildCurvedSidePath(position, isNorth = true)
            SectionPosition.StadiumPosition.Side.SOUTH ->
                buildCurvedSidePath(position, isNorth = false)
            SectionPosition.StadiumPosition.Side.CORNER_NE ->
                buildCornerPath(isNorth = true, isEast = true)
            SectionPosition.StadiumPosition.Side.CORNER_NW ->
                buildCornerPath(isNorth = true, isEast = false)
            SectionPosition.StadiumPosition.Side.CORNER_SE ->
                buildCornerPath(isNorth = false, isEast = true)
            SectionPosition.StadiumPosition.Side.CORNER_SW ->
                buildCornerPath(isNorth = false, isEast = false)
        }
    }

    // Caches the centroid for a section. Curved sides use arc midpoint math;
    // straight sides use simple rectangle center.
    private fun computeSectionCentroid(
        section: SeatSection,
        position: SectionPosition.StadiumPosition
    ) {
        val side = position.side
        val index = position.indexOnSide
        val total = position.totalOnSide.coerceAtLeast(1)

        when (side) {
            SectionPosition.StadiumPosition.Side.NORTH,
            SectionPosition.StadiumPosition.Side.SOUTH -> {
                val isNorth = side == SectionPosition.StadiumPosition.Side.NORTH
                val cx = outerRect.centerX()
                val arcCenterY = if (isNorth) outerRect.top + capRadius
                                 else outerRect.bottom - capRadius
                val outerRx = outerRect.width() / 2f
                val outerRy = capRadius
                val innerRx = pitchRect.width() / 2f + sectionGap
                val innerRy = (if (isNorth) arcCenterY - pitchRect.top + sectionGap
                               else pitchRect.bottom - arcCenterY + sectionGap)
                    .coerceAtMost(capRadius * 0.6f)
                val sectionSweep = 180f / total
                val startAngle = (if (isNorth) 180f else 0f) + index * sectionSweep
                val midAngle = Math.toRadians((startAngle + sectionSweep / 2f).toDouble())
                val midRx = (innerRx + outerRx) / 2f
                val midRy = (innerRy + outerRy) / 2f
                sectionCentroids[section] = android.graphics.PointF(
                    cx + (midRx * cos(midAngle)).toFloat(),
                    arcCenterY + (midRy * sin(midAngle)).toFloat()
                )
            }
            SectionPosition.StadiumPosition.Side.EAST,
            SectionPosition.StadiumPosition.Side.WEST -> {
                // Explicit center math is more reliable than grid-sampling
                // for tall narrow sections near tier separators.
                val sideTop = outerRect.top + capRadius
                val sideBottom = outerRect.bottom - capRadius
                val sideLength = sideBottom - sideTop
                val sectionHeight = sideLength / total
                val centerY = sideTop + index * sectionHeight + sectionHeight / 2f

                val isEast = side == SectionPosition.StadiumPosition.Side.EAST
                val innerX = if (isEast) pitchRect.right + sectionGap else outerRect.left
                val outerX = if (isEast) outerRect.right else pitchRect.left - sectionGap
                val centerX = (innerX + outerX) / 2f

                sectionCentroids[section] = android.graphics.PointF(centerX, centerY)
            }
            else -> { /* corners — grid-based fallback is fine */ }
        }
    }

    private fun buildStraightSidePath(
        position: SectionPosition.StadiumPosition,
        isEast: Boolean
    ): Path {
        val path = Path()
        val total = position.totalOnSide.coerceAtLeast(1)
        val index = position.indexOnSide

        val sideTop = outerRect.top + capRadius
        val sideBottom = outerRect.bottom - capRadius
        val sideLength = sideBottom - sideTop
        val sectionHeight = sideLength / total

        val top = sideTop + index * sectionHeight
        val bottom = top + sectionHeight

        if (isEast) {
            val innerX = pitchRect.right + sectionGap
            val outerX = outerRect.right
            path.moveTo(innerX, top)
            path.lineTo(outerX, top)
            path.lineTo(outerX, bottom)
            path.lineTo(innerX, bottom)
        } else {
            val innerX = pitchRect.left - sectionGap
            val outerX = outerRect.left
            path.moveTo(outerX, top)
            path.lineTo(innerX, top)
            path.lineTo(innerX, bottom)
            path.lineTo(outerX, bottom)
        }
        path.close()
        return path
    }

    // Creates a wedge-shaped path for a section on the curved north/south end.
    private fun buildCurvedSidePath(
        position: SectionPosition.StadiumPosition,
        isNorth: Boolean
    ): Path {
        val path = Path()
        val total = position.totalOnSide.coerceAtLeast(1)
        val index = position.indexOnSide

        val cx = outerRect.centerX()

        val outerArcRect: RectF
        val innerArcRect: RectF

        val innerRadiusX: Float
        val innerRadiusY: Float

        if (isNorth) {
            val arcCenterY = outerRect.top + capRadius
            outerArcRect = RectF(outerRect.left, outerRect.top, outerRect.right, outerRect.top + capRadius * 2f)

            innerRadiusX = pitchRect.width() / 2f + sectionGap
            innerRadiusY = (arcCenterY - pitchRect.top + sectionGap).coerceAtMost(capRadius * 0.6f)
            innerArcRect = RectF(
                cx - innerRadiusX, arcCenterY - innerRadiusY,
                cx + innerRadiusX, arcCenterY + innerRadiusY
            )
        } else {
            val arcCenterY = outerRect.bottom - capRadius
            outerArcRect = RectF(outerRect.left, outerRect.bottom - capRadius * 2f, outerRect.right, outerRect.bottom)

            innerRadiusX = pitchRect.width() / 2f + sectionGap
            innerRadiusY = (pitchRect.bottom - arcCenterY + sectionGap).coerceAtMost(capRadius * 0.6f)
            innerArcRect = RectF(
                cx - innerRadiusX, arcCenterY - innerRadiusY,
                cx + innerRadiusX, arcCenterY + innerRadiusY
            )
        }

        // Sweep angle for each section
        val totalSweep = 180f
        val sectionSweep = totalSweep / total

        if (isNorth) {
            // North arc sweeps from 180 (left) to 360/0 (right)
            val startAngle = 180f + index * sectionSweep

            // Outer arc (clockwise)
            path.arcTo(outerArcRect, startAngle, sectionSweep, true)

            // Inner arc (counter-clockwise, i.e. reverse direction)
            val innerStartAngle = startAngle + sectionSweep
            path.arcTo(innerArcRect, innerStartAngle, -sectionSweep)
        } else {
            // South arc sweeps from 0 (right) to 180 (left)
            val startAngle = 0f + index * sectionSweep

            path.arcTo(outerArcRect, startAngle, sectionSweep, true)

            val innerStartAngle = startAngle + sectionSweep
            path.arcTo(innerArcRect, innerStartAngle, -sectionSweep)
        }

        path.close()
        return path
    }

    private fun buildCornerPath(isNorth: Boolean, isEast: Boolean): Path {
        val path = Path()

        val cornerSize = dp(24f)
        val sideTop = outerRect.top + capRadius
        val sideBottom = outerRect.bottom - capRadius

        // Corner wedge sits at the junction of the arc and the straight side
        val y = if (isNorth) sideTop else sideBottom
        val x = if (isEast) outerRect.right else outerRect.left

        val innerX = if (isEast) pitchRect.right + sectionGap else pitchRect.left - sectionGap

        if (isNorth && isEast) {
            path.moveTo(x, y)
            path.lineTo(x, y - cornerSize)
            path.lineTo(innerX + (x - innerX) * 0.5f, y)
            path.close()
        } else if (isNorth && !isEast) {
            path.moveTo(x, y)
            path.lineTo(x, y - cornerSize)
            path.lineTo(innerX - (innerX - x) * 0.5f, y)
            path.close()
        } else if (!isNorth && isEast) {
            path.moveTo(x, y)
            path.lineTo(x, y + cornerSize)
            path.lineTo(innerX + (x - innerX) * 0.5f, y)
            path.close()
        } else {
            path.moveTo(x, y)
            path.lineTo(x, y + cornerSize)
            path.lineTo(innerX - (innerX - x) * 0.5f, y)
            path.close()
        }

        return path
    }

}
