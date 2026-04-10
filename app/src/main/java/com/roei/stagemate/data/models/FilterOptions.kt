package com.roei.stagemate.data.models

// Filter state for event search (city, category, price range, distance).
// Used by FilterActivity, HomeFragment, and SearchFragment.
data class FilterOptions(
    var selectedCity: String = "",
    var selectedCategories: MutableList<String> = mutableListOf(),
    var selectedTicketTypes: MutableList<String> = mutableListOf(),
    var minPrice: Double = 0.0,
    var maxPrice: Double = 1000.0,
    var maxDistance: Int = 50
) {
    fun clear() {
        selectedCity = ""
        selectedCategories.clear()
        selectedTicketTypes.clear()
        minPrice = 0.0
        maxPrice = 1000.0
        maxDistance = 50
    }

    fun hasFilters(): Boolean {
        return selectedCity.isNotEmpty() ||
                selectedCategories.isNotEmpty() ||
                selectedTicketTypes.isNotEmpty() ||
                minPrice > 0.0 ||
                maxPrice < 1000.0 ||
                maxDistance < 50
    }

    fun getActiveFilterCount(): Int {
        var count = 0
        if (selectedCity.isNotEmpty()) count++
        if (selectedCategories.isNotEmpty()) count++
        if (selectedTicketTypes.isNotEmpty()) count++
        if (minPrice > 0.0 || maxPrice < 1000.0) count++
        if (maxDistance < 50) count++
        return count
    }
}
