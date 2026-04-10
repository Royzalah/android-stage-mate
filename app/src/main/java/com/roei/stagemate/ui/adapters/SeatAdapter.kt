package com.roei.stagemate.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.roei.stagemate.R
import com.roei.stagemate.databinding.ItemSeatBinding
import com.roei.stagemate.data.models.Seat
import com.roei.stagemate.databinding.ItemSeatRowBinding

// Adapter for individual seat icons within a row, used by SeatRowAdapter.
class SeatAdapter(
    private var seats: List<Seat>,
    private val onSeatClick: (Seat) -> Unit
) : RecyclerView.Adapter<SeatAdapter.SeatViewHolder>() {

    fun submitList(newSeats: List<Seat>) {
        seats = newSeats
        notifyDataSetChanged()
    }

    inner class SeatViewHolder(private val binding: ItemSeatBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.seatItemContainer.setOnClickListener {
                val pos = absoluteAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
                val seat = seats[pos]
                if (seat.isAvailable() || seat.isSelected()) {
                    onSeatClick(seat)
                }
            }
        }

        fun bind(seat: Seat) {
            val iconRes = when {
                seat.isAccessible -> when (seat.status) {
                    Seat.SeatStatus.AVAILABLE -> R.drawable.seat_accessible
                    Seat.SeatStatus.SELECTED -> R.drawable.seat_selected
                    Seat.SeatStatus.OCCUPIED, Seat.SeatStatus.RESERVED -> R.drawable.seat_occupied
                }
                else -> when (seat.status) {
                    Seat.SeatStatus.AVAILABLE -> R.drawable.seat_available
                    Seat.SeatStatus.SELECTED -> R.drawable.seat_selected
                    Seat.SeatStatus.OCCUPIED -> R.drawable.seat_occupied
                    Seat.SeatStatus.RESERVED -> R.drawable.seat_reserved
                }
            }
            binding.seatItemIMG.setImageResource(iconRes)

            val ctx = binding.root.context
            binding.seatItemContainer.contentDescription = when {
                seat.isAccessible -> ctx.getString(R.string.seat_accessible_label)
                seat.isSelected() -> ctx.getString(R.string.seat_selected_label)
                seat.isAvailable() -> ctx.getString(R.string.seat_available_label)
                else -> ctx.getString(R.string.seat_occupied_label)
            }
            binding.seatItemContainer.isClickable = seat.isAvailable() || seat.isSelected()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeatViewHolder {
        val binding = ItemSeatBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SeatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SeatViewHolder, position: Int) {
        holder.bind(seats[position])
    }

    override fun getItemCount() = seats.size
}

// Adapter for seat rows, each containing a horizontal SeatAdapter.
// Used by SeatSelectionActivity.
class SeatRowAdapter(
    private val rows: Map<String, List<Seat>>,
    private val onSeatClick: (Seat) -> Unit
) : RecyclerView.Adapter<SeatRowAdapter.RowViewHolder>() {

    private val rowKeys = rows.keys.toList()

    inner class RowViewHolder(private val binding: ItemSeatRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var seatAdapter: SeatAdapter? = null

        init {
            binding.seatRowRVSeats.apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                setHasFixedSize(true)
            }
        }

        fun bind(rowEntry: Pair<String, List<Seat>>) {
            binding.seatRowLBLRow.text = rowEntry.first
            val existing = seatAdapter
            if (existing != null) {
                existing.submitList(rowEntry.second)
            } else {
                val adapter = SeatAdapter(rowEntry.second, onSeatClick)
                seatAdapter = adapter
                binding.seatRowRVSeats.adapter = adapter
            }
        }

        fun clearAdapter() {
            binding.seatRowRVSeats.adapter = null
            seatAdapter = null
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowViewHolder {
        val binding = ItemSeatRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RowViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RowViewHolder, position: Int) {
        val rowKey = rowKeys[position]
        holder.bind(Pair(rowKey, rows[rowKey] ?: emptyList()))
    }

    override fun onViewRecycled(holder: RowViewHolder) {
        super.onViewRecycled(holder)
        holder.clearAdapter()
    }

    override fun getItemCount() = rows.size
}
