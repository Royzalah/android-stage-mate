package com.roei.stagemate.data.models

import java.io.Serializable

// Core event model representing a single event listing.
// Used by HomeFragment, SearchFragment, TicketsFragment, EventDetailActivity,
// FirebaseRepository, DataRepository, and IsraeliEventsGenerator.
data class Event(
    var id: String = "",
    var title: String = "",
    var category: String = "",
    var date: String = "",
    var time: String = "",
    var location: String = "",
    var venue: String = "",
    var imageUrl: String = "",
    var price: Double = 0.0,
    var rating: Float = 0.0f,
    var description: String = "",
    var participants: Int = 0,
    var distance: Double = 0.0,
    var isFavorite: Boolean = false,
    var isPurchased: Boolean = false,
    var availableTickets: Int = 0,
    var totalTickets: Int = 0,
    var favoriteCount: Int = 0,
    var viewCount: Int = 0,
    var isHot: Boolean = false,
    var lastUpdated: Long = System.currentTimeMillis(),
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var eventType: EventType = EventType.SEATED,
    val subcategory: String = "",
    var availabilityPercentage: Int = 0,
    var ratingCount: Int = 0
) : Serializable {

    val almostSoldOut: Boolean get() = availableTickets > 0 && availableTickets <= (totalTickets * 0.1)
    val soldOut: Boolean get() = availableTickets == 0

    enum class EventType {
        SEATED,
        STANDING_ONLY,
        MIXED
    }

    fun calcAvailabilityPercentage() = if (totalTickets > 0) (availableTickets * 100 / totalTickets) else 0

    fun toggleFavorite() = apply { isFavorite = !isFavorite }

    fun requiresSeatSelection(): Boolean {
        return eventType == EventType.SEATED || eventType == EventType.MIXED
    }

    fun supportsStanding(): Boolean {
        return eventType == EventType.STANDING_ONLY || eventType == EventType.MIXED
    }

    fun isPast(): Boolean {
        if (date.isBlank()) return false
        val patterns = listOf("dd MMM yyyy", "MMM dd, yyyy", "dd/MM/yyyy")
        for (pattern in patterns) {
            try {
                val fmt = java.text.SimpleDateFormat(pattern, java.util.Locale.ENGLISH)
                val parsed = fmt.parse(date) ?: continue
                return parsed.before(java.util.Date())
            } catch (_: Exception) { }
        }
        return false
    }

}
