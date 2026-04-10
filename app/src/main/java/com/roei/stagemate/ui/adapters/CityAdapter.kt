package com.roei.stagemate.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.roei.stagemate.R
import com.roei.stagemate.databinding.ItemCityBinding

// Simple list adapter for city name items.
// Used by SearchFragment city filter dropdown.
class CityAdapter(
    private var cities: List<String>,
    private val onCityClick: (String) -> Unit
) : RecyclerView.Adapter<CityAdapter.CityViewHolder>() {

    private var selectedCity: String = "all"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CityViewHolder {
        val binding = ItemCityBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CityViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CityViewHolder, position: Int) {
        holder.bind(cities[position])
    }

    override fun getItemCount(): Int = cities.size

    fun setSelectedCity(city: String) {
        val oldIndex = cities.indexOf(selectedCity)
        val newIndex = cities.indexOf(city)
        selectedCity = city
        if (oldIndex >= 0) notifyItemChanged(oldIndex)
        if (newIndex >= 0 && newIndex != oldIndex) notifyItemChanged(newIndex)
    }

    fun updateCities(newCities: List<String>) {
        val oldList = cities
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = oldList.size
            override fun getNewListSize() = newCities.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int) = oldList[oldPos] == newCities[newPos]
            override fun areContentsTheSame(oldPos: Int, newPos: Int) = oldList[oldPos] == newCities[newPos]
        })
        cities = newCities
        diffResult.dispatchUpdatesTo(this)
    }

    inner class CityViewHolder(private val binding: ItemCityBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val pos = absoluteAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
                onCityClick(cities[pos])
            }
        }

        fun bind(city: String) {
            binding.cityLBLName.text = city

            val context = binding.root.context
            if (city == selectedCity) {
                binding.root.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.accent_primary)
                )
                binding.cityLBLName.setTextColor(
                    ContextCompat.getColor(context, R.color.white)
                )
            } else {
                binding.root.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.background_card)
                )
                binding.cityLBLName.setTextColor(
                    ContextCompat.getColor(context, R.color.text_primary)
                )
            }
        }
    }
}
