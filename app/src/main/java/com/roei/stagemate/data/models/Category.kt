package com.roei.stagemate.data.models

// Event category for filtering and display (e.g. Music, Sports, Theater).
// Used by CategoryAdapter, CategorySelectionAdapter, HomeFragment, and FilterActivity.
data class Category(
    val id: String = "",
    val name: String = "",
    val iconResId: Int = 0,
    var isSelected: Boolean = false
) {
    fun toggleSelection() = apply { isSelected = !isSelected }
}
