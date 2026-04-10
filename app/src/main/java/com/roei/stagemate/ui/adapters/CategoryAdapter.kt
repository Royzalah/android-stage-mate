package com.roei.stagemate.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.roei.stagemate.R
import com.roei.stagemate.databinding.ItemCategoryBinding
import com.roei.stagemate.data.interfaces.CategoryCallback
import com.roei.stagemate.data.models.Category

// Horizontal category chip adapter with single-selection highlighting.
// Used by SearchFragment for city filter chips.
class CategoryAdapter(categories: List<Category>) :
    RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    var categories: List<Category> = categories
        set(value) {
            field = value
            selectedPosition = 0
            notifyDataSetChanged()
        }

    var categoryCallback: CategoryCallback? = null
    private var selectedPosition = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) =
        holder.bind(categories[position], position)

    override fun getItemCount(): Int = categories.size

    fun getItem(position: Int): Category = categories[position]

    inner class CategoryViewHolder(val binding: ItemCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val pos = absoluteAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
                val previousPosition = selectedPosition
                selectedPosition = pos

                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)

                categoryCallback?.onCategoryClicked(getItem(pos), pos)
            }
        }

        fun bind(item: Category, position: Int) {
            binding.categoryLBLName.text = item.name

            if (item.iconResId != 0) {
                binding.categoryIMGIcon.visibility = View.VISIBLE
                binding.categoryIMGIcon.setImageResource(item.iconResId)
            } else {
                binding.categoryIMGIcon.visibility = View.GONE
            }

            val isSelected = position == selectedPosition
            binding.categoryIMGIcon.clearColorFilter()
            if (isSelected) {
                binding.categoryCard.setCardBackgroundColor(
                    itemView.context.getColor(R.color.accent_primary)
                )
                binding.categoryLBLName.setTextColor(
                    itemView.context.getColor(R.color.white)
                )
            } else {
                binding.categoryCard.setCardBackgroundColor(
                    itemView.context.getColor(R.color.background_card)
                )
                binding.categoryLBLName.setTextColor(
                    itemView.context.getColor(R.color.text_primary)
                )
            }
        }
    }
}
