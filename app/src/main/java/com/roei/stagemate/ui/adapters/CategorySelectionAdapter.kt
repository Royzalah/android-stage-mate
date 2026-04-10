package com.roei.stagemate.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.roei.stagemate.R
import com.roei.stagemate.databinding.ItemCategorySelectionBinding
import com.roei.stagemate.data.models.Category

// Grid adapter for multi-select category cards during onboarding/preferences.
// Used by PreferredCategoriesActivity.
class CategorySelectionAdapter(
    private val categories: List<Category>,
    private val onCategoryClick: (Category) -> Unit
) : RecyclerView.Adapter<CategorySelectionAdapter.ViewHolder>() {

    private val selectedCategories = mutableSetOf<String>()

    inner class ViewHolder(private val binding: ItemCategorySelectionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.itemCategoryCard.setOnClickListener {
                val pos = absoluteAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
                onCategoryClick(categories[pos])
            }
        }

        fun bind(category: Category) {
            binding.itemCategoryLBLName.text = category.name
            if (category.iconResId != 0) {
                binding.itemCategoryIMG.setImageResource(category.iconResId)
            } else {
                binding.itemCategoryIMG.setImageDrawable(null)
            }

            val isSelected = selectedCategories.contains(category.id)
            binding.itemCategoryIMGSelected.isVisible = isSelected

            binding.itemCategoryCard.strokeColor = if (isSelected) {
                binding.root.context.getColor(R.color.accent_primary)
            } else {
                Color.TRANSPARENT
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategorySelectionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount() = categories.size

    fun toggleCategory(categoryId: String) {
        if (selectedCategories.contains(categoryId)) {
            selectedCategories.remove(categoryId)
        } else {
            selectedCategories.add(categoryId)
        }
        val index = categories.indexOfFirst { it.id == categoryId }
        if (index != -1) {
            categories[index].toggleSelection()
            notifyItemChanged(index)
        }
    }

    fun getSelectedCategories(): List<String> = selectedCategories.toList()

    fun setSelectedCategories(categories: List<String>) {
        selectedCategories.clear()
        selectedCategories.addAll(categories)
        notifyItemRangeChanged(0, this.categories.size)
    }
}