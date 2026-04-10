package com.roei.stagemate.data.models

// Single seat with status, reservation tracking, and Firebase serialization.
// Used by SeatMap, SeatSection, SeatAdapter, SeatSelectionActivity, and VenueTemplates.
data class Seat(
    val row: String = "",
    val number: Int = 0,
    var status: SeatStatus = SeatStatus.AVAILABLE,
    val section: String = "General",
    val price: Double = 0.0,
    var reservedBy: String? = null,
    var reservedAt: Long = 0L,
    var reservationExpiry: Long = 0L,
    val isAccessible: Boolean = false
) {
    enum class SeatStatus {
        AVAILABLE,
        SELECTED,
        OCCUPIED,
        RESERVED
    }

    fun getSeatId(): String = "$row$number"

    fun isAvailable() = status == SeatStatus.AVAILABLE

    fun isSelected() = status == SeatStatus.SELECTED

    fun isReserved() = status == SeatStatus.RESERVED

    fun isReservationExpired(): Boolean {
        return status == SeatStatus.RESERVED &&
               reservationExpiry > 0 &&
               System.currentTimeMillis() > reservationExpiry
    }

    fun isReservedBy(userId: String): Boolean {
        return status == SeatStatus.RESERVED && reservedBy == userId
    }

    fun reserve(userId: String, durationMinutes: Int = 5) {
        if (status == SeatStatus.AVAILABLE) {
            status = SeatStatus.RESERVED
            reservedBy = userId
            reservedAt = System.currentTimeMillis()
            reservationExpiry = reservedAt + (durationMinutes * 60 * 1000)
        }
    }

    fun releaseReservation() {
        if (status == SeatStatus.RESERVED) {
            status = SeatStatus.AVAILABLE
            reservedBy = null
            reservedAt = 0L
            reservationExpiry = 0L
        }
    }

    fun getRemainingReservationTime(): Long {
        if (status != SeatStatus.RESERVED || reservationExpiry == 0L) {
            return 0L
        }
        val remaining = reservationExpiry - System.currentTimeMillis()
        return if (remaining > 0) remaining else 0L
    }

    fun select() {
        if (status == SeatStatus.AVAILABLE) {
            status = SeatStatus.SELECTED
        }
    }

    fun deselect() {
        if (status == SeatStatus.SELECTED) {
            status = SeatStatus.AVAILABLE
        }
    }

    fun toggle() {
        when (status) {
            SeatStatus.AVAILABLE -> status = SeatStatus.SELECTED
            SeatStatus.SELECTED -> status = SeatStatus.AVAILABLE
            SeatStatus.OCCUPIED, SeatStatus.RESERVED -> {}
        }
    }

    fun toFirebaseMap(): Map<String, Any> {
        return mapOf(
            "row" to row,
            "number" to number,
            "status" to status.name,
            "section" to section,
            "price" to price,
            "reservedBy" to (reservedBy ?: ""),
            "reservedAt" to reservedAt,
            "reservationExpiry" to reservationExpiry,
            "isAccessible" to isAccessible
        )
    }

    companion object {
        fun fromFirebaseMap(map: Map<String, Any>): Seat {
            return Seat(
                row = map["row"] as? String ?: "",
                number = (map["number"] as? Long)?.toInt() ?: 0,
                status = try {
                    SeatStatus.valueOf(map["status"] as? String ?: "AVAILABLE")
                } catch (e: Exception) {
                    SeatStatus.AVAILABLE
                },
                section = map["section"] as? String ?: "General",
                price = (map["price"] as? Number)?.toDouble() ?: 0.0,
                reservedBy = (map["reservedBy"] as? String)?.takeIf { it.isNotEmpty() },
                reservedAt = (map["reservedAt"] as? Long) ?: 0L,
                reservationExpiry = (map["reservationExpiry"] as? Long) ?: 0L,
                isAccessible = (map["isAccessible"] as? Boolean) ?: false
            )
        }
    }
}
