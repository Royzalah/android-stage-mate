package com.roei.stagemate.ui.adapters

import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.Color
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import com.roei.stagemate.R
import com.roei.stagemate.data.models.Seat
import com.roei.stagemate.data.models.SeatSection
import com.roei.stagemate.databinding.ItemVenueSectionBinding
import com.roei.stagemate.utilities.Constants

// Adapter for venue section overview cards showing price range, availability, and occupancy.
// Used by SeatSelectionActivity to display sections before seat-level selection.
class VenueSectionAdapter(
    private val sections: List<SeatSection>,
    private val selectedSeats: List<Seat>,
    private val onSectionClick: (SeatSection) -> Unit
) : RecyclerView.Adapter<VenueSectionAdapter.SectionViewHolder>() {

    inner class SectionViewHolder(private val binding: ItemVenueSectionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.sectionCVCard.setOnClickListener {
                val pos = absoluteAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
                val section = sections[pos]
                val availableCount = section.seats.count { it.isAvailable() || it.isSelected() }
                if (availableCount > 0) {
                    onSectionClick(section)
                }
            }
        }

        fun bind(section: SeatSection) {
            val context = binding.root.context
            binding.sectionLBLName.text = section.name

            val isStanding = Constants.VenueUtils.isStandingSection(section.name)
            binding.sectionLBLTypeBadge.visibility = View.VISIBLE
            if (isStanding) {
                binding.sectionLBLTypeBadge.text = context.getString(R.string.standing_label)
                binding.sectionLBLTypeBadge.setTextColor(ContextCompat.getColor(context, R.color.success_s300))
            } else {
                binding.sectionLBLTypeBadge.text = context.getString(R.string.seated_label)
                binding.sectionLBLTypeBadge.setTextColor(ContextCompat.getColor(context, R.color.accent_primary))
            }

            val sectionColor = try {
                Color.parseColor(section.color)
            } catch (e: Exception) {
                ContextCompat.getColor(context, R.color.success_s400)
            }

            binding.sectionVIEWColorDot.background.setTint(sectionColor)
            binding.sectionCVCard.setCardBackgroundColor(ColorUtils.setAlphaComponent(sectionColor, 30))

            val seats = section.seats
            if (seats.isNotEmpty()) {
                val prices = seats.map { it.price }.filter { it > 0 }
                if (prices.isNotEmpty()) {
                    val minPrice = prices.minOrNull() ?: 0.0
                    val maxPrice = prices.maxOrNull() ?: 0.0
                    binding.sectionLBLPrice.text = context.getString(
                        R.string.section_price_range_format, minPrice, maxPrice
                    )
                } else {
                    binding.sectionLBLPrice.text = ""
                }
            } else {
                binding.sectionLBLPrice.text = ""
            }

            val availableCount = seats.count { it.isAvailable() || it.isSelected() }
            val totalCount = seats.size
            binding.sectionLBLAvailable.text = context.getString(
                R.string.section_available_format, availableCount, totalCount
            )

            val occupancyPercent = if (totalCount > 0) {
                ((totalCount - availableCount) * 100) / totalCount
            } else 0

            binding.sectionPBOccupancy.max = 100
            binding.sectionPBOccupancy.progress = occupancyPercent

            val barColor = when {
                occupancyPercent >= Constants.Occupancy.CRITICAL_PERCENT -> ContextCompat.getColor(context, R.color.danger_d300)
                occupancyPercent >= Constants.Occupancy.HIGH_PERCENT -> ContextCompat.getColor(context, R.color.warning_w400)
                occupancyPercent >= Constants.Occupancy.MODERATE_PERCENT -> ContextCompat.getColor(context, R.color.occupancy_caution)
                else -> ContextCompat.getColor(context, R.color.success_s400)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                binding.sectionPBOccupancy.progressDrawable?.colorFilter = BlendModeColorFilter(barColor, BlendMode.SRC_IN)
            } else {
                @Suppress("DEPRECATION")
                binding.sectionPBOccupancy.progressDrawable?.setColorFilter(barColor, android.graphics.PorterDuff.Mode.SRC_IN)
            }

            if (occupancyPercent >= Constants.Occupancy.ALMOST_FULL_PERCENT) {
                binding.sectionLBLOccupancy.text = context.getString(R.string.section_almost_full)
                binding.sectionLBLOccupancy.setTextColor(ContextCompat.getColor(context, R.color.danger_d300))
            } else {
                val availablePercent = 100 - occupancyPercent
                binding.sectionLBLOccupancy.text = context.getString(
                    R.string.section_availability_format, availablePercent
                )
                binding.sectionLBLOccupancy.setTextColor(barColor)
            }

            val selectedCount = selectedSeats.count { it.section == section.name }
            if (selectedCount > 0) {
                binding.sectionLBLSelectedBadge.visibility = View.VISIBLE
                binding.sectionLBLSelectedBadge.text = context.getString(
                    R.string.seats_selected_badge, selectedCount
                )
            } else {
                binding.sectionLBLSelectedBadge.visibility = View.GONE
            }

            if (availableCount == 0) {
                binding.sectionLBLSoldOut.visibility = View.VISIBLE
                binding.sectionPBOccupancy.visibility = View.GONE
                binding.sectionLBLOccupancy.visibility = View.GONE
                binding.sectionCVCard.alpha = 0.5f
                binding.sectionCVCard.isClickable = false
            } else {
                binding.sectionLBLSoldOut.visibility = View.GONE
                binding.sectionPBOccupancy.visibility = View.VISIBLE
                binding.sectionLBLOccupancy.visibility = View.VISIBLE
                binding.sectionCVCard.alpha = 1.0f
                binding.sectionCVCard.isClickable = true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionViewHolder {
        val binding = ItemVenueSectionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SectionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
        holder.bind(sections[position])
    }

    override fun getItemCount() = sections.size
}
