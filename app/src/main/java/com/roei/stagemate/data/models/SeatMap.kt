package com.roei.stagemate.data.models

import com.roei.stagemate.utilities.Constants
import kotlin.math.abs

// Seat layout for a venue/event, containing sections of seats with occupancy tracking.
// Used by SeatSelectionActivity, VenueTemplates, and VenueSectionAdapter.
data class SeatMap(
    var id: String = "",
    val eventId: String = "",
    val venueId: String = "",
    val venueName: String = "",
    val layout: List<Seat> = emptyList(),
    val sections: List<SeatSection> = emptyList(),
    val stagePosition: StagePosition = StagePosition.NORTH,
    val venueLayoutType: VenueLayoutType = VenueLayoutType.THEATER,
    val totalSeats: Int = 0,
    var availableSeats: Int = 0,
    var occupiedSeats: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
) {

    enum class StagePosition {
        NORTH,
        SOUTH,
        EAST,
        WEST,
        CENTER
    }

    enum class VenueLayoutType {
        THEATER,
        STADIUM,
        CONCERT_STAGE,
        CLUB,
        ARENA
    }

    data class SeatZone(
        val id: String = "",
        val name: String = "",
        val color: String = Constants.SeatColors.BLACK,
        val seatCount: Int = 0,
        val basePrice: Double = 0.0,
        val description: String = ""
    )

    @Transient
    private var _allSeatsCache: List<Seat>? = null

    fun allSeats(): List<Seat> {
        return _allSeatsCache
            ?: (if (layout.isNotEmpty()) layout else sections.flatMap { it.seats })
                .also { _allSeatsCache = it }
    }

    fun findSeat(seatId: String): Seat? {
        return allSeats().find { it.getSeatId() == seatId }
    }

    fun getAvailableSeats(): List<Seat> {
        return allSeats().filter { it.status == Seat.SeatStatus.AVAILABLE }
    }

    fun getSeatsBySection(sectionName: String): List<Seat> {
        return sections.find { it.name == sectionName }?.seats ?: emptyList()
    }

    fun hasAvailableSeats(): Boolean {
        return allSeats().any { it.status == Seat.SeatStatus.AVAILABLE }
    }

    fun getAvailableDisabledSeats(): List<Seat> {
        return allSeats().filter { it.isAccessible && it.status == Seat.SeatStatus.AVAILABLE }
    }

    fun updateOccupancyStats() {
        val allSeatsList = allSeats()
        occupiedSeats = allSeatsList.count { it.status == Seat.SeatStatus.OCCUPIED || it.status == Seat.SeatStatus.RESERVED }
        availableSeats = allSeatsList.count { it.status == Seat.SeatStatus.AVAILABLE }
    }

}

// Spatial position metadata for drawing sections on visual venue maps.
sealed class SectionPosition {
    // tierIndex: 0 = Lower Bowl (closest to court), 1 = VIP Ring, 2 = Upper Bowl, etc.
    data class ArenaPosition(
        val startAngleDeg: Float,
        val sweepAngleDeg: Float,
        val tierIndex: Int = 0,
        val standGapDeg: Float = 0f
    ) : SectionPosition()

    data class StadiumPosition(
        val side: Side,
        val indexOnSide: Int,
        val totalOnSide: Int
    ) : SectionPosition() {
        enum class Side { NORTH, SOUTH, EAST, WEST, CORNER_NE, CORNER_NW, CORNER_SE, CORNER_SW }
    }

    data class TheaterPosition(
        val column: Column,
        val tier: Tier
    ) : SectionPosition() {
        enum class Column { LEFT, CENTER, RIGHT }
        enum class Tier { FRONT, MIDDLE, BACK, BALCONY }
    }

    // Standing/floor zones inside the pitch/court area during concerts.
    // zoneIndex 0 = closest to stage, higher indices are further away.
    data class FloorPosition(
        val zoneIndex: Int,
        val totalZones: Int
    ) : SectionPosition()
}

// Seat section (VIP, General, Balcony) with dynamic pricing based on row/position.
// Used by SeatMap, VenueTemplates, and VenueSectionAdapter.
data class SeatSection(
    val name: String = "",
    val rows: Int = 0,
    val seatsPerRow: Int = 0,
    val seats: List<Seat> = emptyList(),
    val basePrice: Double = 100.0,
    val priceMultiplier: Double = 1.0,
    val color: String = Constants.SeatColors.GREEN,
    val position: SectionPosition? = null,
    val hasAccessibleSeating: Boolean = false,
    val accessiblePriceMultiplier: Double = 0.8,
    val isBlocked: Boolean = false
) {
    // Price varies by row proximity and distance from center
    fun getSeatPrice(row: String, number: Int): Double {
        val rowIndex = row[0] - 'A'
        val centerSeat = seatsPerRow / 2.0
        val distanceFromCenter = abs(number - centerSeat)

        val rowMultiplier = 1.0 + (0.1 * (rows - rowIndex - 1) / rows)
        val centerMultiplier = 1.0 + (0.05 * (1 - distanceFromCenter / centerSeat))

        return Math.round(basePrice * priceMultiplier * rowMultiplier * centerMultiplier).toDouble()
    }

    companion object {
        fun createVIP(rows: Int, seatsPerRow: Int, basePrice: Double = 300.0): SeatSection {
            return create("VIP", rows, seatsPerRow, basePrice, 2.0, Constants.SeatColors.GOLD, 'A', false)
        }

        fun createGeneral(rows: Int, seatsPerRow: Int, basePrice: Double = 150.0, startRow: Char = 'A'): SeatSection {
            return create("General", rows, seatsPerRow, basePrice, 1.0, Constants.SeatColors.GREEN, startRow, false)
        }

        fun createBalcony(rows: Int, seatsPerRow: Int, basePrice: Double = 100.0): SeatSection {
            return create("Balcony", rows, seatsPerRow, basePrice, 0.7, Constants.SeatColors.BLUE, 'A', false)
        }

        fun createAccessible(rows: Int, seatsPerRow: Int, basePrice: Double = 120.0): SeatSection {
            return create("Accessible", rows, seatsPerRow, basePrice, 0.8, Constants.SeatColors.BLUE, 'A', true)
        }

        // All seats start AVAILABLE; occupancy simulated later in SeatSelectionActivity.
        // When hasAccessibleSeating is true, the last 2 rows are marked as accessible
        // with a discounted price (accessiblePriceMult), integrating accessible seating
        // into every section rather than isolating it.
        fun create(
            name: String,
            rows: Int,
            seatsPerRow: Int,
            basePrice: Double = 100.0,
            priceMultiplier: Double = 1.0,
            color: String = Constants.SeatColors.GREEN,
            startRow: Char = 'A',
            isAccessible: Boolean = false,
            position: SectionPosition? = null,
            hasAccessibleSeating: Boolean = false,
            accessiblePriceMult: Double = 0.8,
            isBlocked: Boolean = false
        ): SeatSection {
            val seats = mutableListOf<Seat>()

            for (rowIndex in 0 until rows) {
                val rowLetter = (startRow + rowIndex).toString()
                val isAccessibleRow = hasAccessibleSeating && (rowIndex == 0 || rowIndex == rows - 1)

                for (seatNumber in 1..seatsPerRow) {
                    val rowMult = 1.0 + (0.1 * (rows - rowIndex - 1) / rows)
                    val centerSeat = seatsPerRow / 2.0
                    val distanceFromCenter = abs(seatNumber - centerSeat)
                    val centerMult = 1.0 + (0.05 * (1 - distanceFromCenter / centerSeat))
                    val priceMult = if (isAccessibleRow) accessiblePriceMult else priceMultiplier
                    val seatPrice = Math.round(basePrice * priceMult * rowMult * centerMult).toDouble()

                    seats.add(Seat(
                        row = rowLetter,
                        number = seatNumber,
                        status = Seat.SeatStatus.AVAILABLE,
                        section = name,
                        price = seatPrice,
                        isAccessible = isAccessible || isAccessibleRow
                    ))
                }
            }

            return SeatSection(name, rows, seatsPerRow, seats, basePrice, priceMultiplier, color, position,
                hasAccessibleSeating, accessiblePriceMult, isBlocked)
        }
    }
}