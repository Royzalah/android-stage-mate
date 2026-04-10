package com.roei.stagemate.data.models

// Ticket type enum with pricing tiers (Regular, VIP, Family packages, etc.).
// Used by Ticket, PricingTier, SeatSelectionActivity, and PaymentActivity.
enum class TicketType(val displayName: String, val requiresVerification: Boolean, val includesCompanion: Boolean) {
    REGULAR("Regular", false, false),
    CHILD("Child", false, false),
    DISABLED("Disabled + Companion", true, true),

    // Family packages
    PARENT_CHILD("Parent + Child", false, false),
    PARENTS_CHILD("2 Parents + Child", false, false),
    PARENTS_TWO_CHILDREN("2 Parents + 2 Children", false, false),

    // VIP
    VIP("VIP", false, false),
    EARLY_BIRD("Early Bird", false, false),

    // Additional types
    FAMILY("Family", false, false),
    LIVE_STREAM("Live Stream", false, false);

    companion object {
        fun fromString(type: String): TicketType {
            return entries.find { it.name == type } ?: REGULAR
        }

        fun getChildTickets(): List<TicketType> {
            return listOf(CHILD, PARENT_CHILD, PARENTS_CHILD, PARENTS_TWO_CHILDREN)
        }

        fun getSpecialNeeds(): List<TicketType> {
            return listOf(DISABLED)
        }
    }

    fun englishName(): String = when (this) {
        REGULAR -> "Regular"
        VIP -> "VIP"
        LIVE_STREAM -> "Live Stream"
        EARLY_BIRD -> "Early Bird"
        CHILD -> "Child"
        DISABLED -> "Disabled"
        PARENT_CHILD -> "Parent + Child"
        PARENTS_CHILD -> "2 Parents + Child"
        PARENTS_TWO_CHILDREN -> "2 Parents + 2 Children"
        FAMILY -> "Family"
    }
}
