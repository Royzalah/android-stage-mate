package com.roei.stagemate.data.models

import androidx.annotation.ColorRes
import com.roei.stagemate.R
import com.roei.stagemate.utilities.DateFormatter

// Purchased ticket model with QR code, seat info, and booking reference.
// Used by TicketsFragment, TicketAdapter, PaymentActivity, ReceiptActivity, and FirebaseRepository.
data class Ticket(
    var id: String = "",
    val eventId: String = "",
    val eventTitle: String = "",
    val eventDate: String = "",
    val eventTime: String = "",
    val eventLocation: String = "",
    val eventImageUrl: String = "",
    val ticketType: TicketType = TicketType.REGULAR,
    val ticketStatus: TicketStatus = TicketStatus.UPCOMING,
    val price: Double = 0.0,
    val quantity: Int = 0,
    val seatNumbers: List<String> = emptyList(),
    val qrCode: String = "",
    val purchaseDate: String = "",
    val bookingReference: String = "",
    val userName: String = "",
    val userEmail: String = "",
    var rating: Float = 0f,
    var isRated: Boolean = false
) {
    enum class TicketStatus {
        UPCOMING,
        PAST,
        CANCELLED,
        LIVE_NOW
    }

    // Pretty event date for display ("dd MMM yyyy"); falls back to raw on parse errors.
    fun getFormattedDate(): String = DateFormatter.formatDate(eventDate)

    // True when the event date has already passed. Tries the same three patterns
    // the legacy adapter helpers used (server seed, US, EU).
    fun isPast(): Boolean {
        if (eventDate.isBlank()) return false
        for (pattern in DATE_PARSE_PATTERNS) {
            try {
                val fmt = java.text.SimpleDateFormat(pattern, java.util.Locale.ENGLISH)
                val parsed = fmt.parse(eventDate) ?: continue
                return parsed.before(java.util.Date())
            } catch (_: Exception) { }
        }
        return false
    }

    // Color resource for the status badge text. Returned as @ColorRes so the model
    // stays Context-free; callers resolve it via ContextCompat.getColor.
    @ColorRes
    fun getStatusBadgeColor(): Int = when (ticketStatus) {
        TicketStatus.UPCOMING  -> R.color.success_s400
        TicketStatus.PAST      -> R.color.status_gray
        TicketStatus.CANCELLED -> R.color.status_gray
        TicketStatus.LIVE_NOW  -> R.color.danger_d500
    }

    companion object {
        // SimpleDateFormat is not thread-safe; instances are created per-call inside isPast().
        private val DATE_PARSE_PATTERNS = listOf(
            "dd MMM yyyy",
            "MMM dd, yyyy",
            "dd/MM/yyyy"
        )

        fun generateSeatNumbers(count: Int): List<String> {
            val seats = mutableListOf<String>()
            val rows = listOf("A", "B", "C", "D", "E", "F")
            val startSeat = (1..20).random()
            for (i in 0 until count) {
                val row = rows[(startSeat + i) / 20]
                val seat = ((startSeat + i) % 20) + 1
                seats.add("$row$seat")
            }
            return seats
        }
    }
}
