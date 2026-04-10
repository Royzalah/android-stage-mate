package com.roei.stagemate.data.models

import com.google.firebase.firestore.GeoPoint

// Venue model with location, capacity, and type info.
// Used by IsraeliLocations, VenueTemplates, and EventDetailActivity.
data class Venue(
    var id: String = "",
    val name: String = "",
    val type: VenueType = VenueType.GENERAL,
    val address: String = "",
    val city: String = "",
    val capacity: Int = 0,
    val description: String = "",
    val imageUrl: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val geoPoint: GeoPoint? = null,
    val amenities: List<String> = emptyList(),
    val hasDisabledSeating: Boolean = true
) {
    enum class VenueType {
        GENERAL,
        CONCERT_HALL,
        STADIUM,
        THEATER,
        CHILDREN_THEATER,
        COMEDY_CLUB,
        ARENA,
        OPEN_AIR
    }
}
