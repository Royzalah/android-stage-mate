package com.roei.stagemate.data.interfaces

import com.roei.stagemate.data.models.Category

// Click handler for category selection, used by CategoryAdapter.
interface CategoryCallback {
    fun onCategoryClicked(category: Category, position: Int)
}
