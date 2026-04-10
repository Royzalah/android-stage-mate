package com.roei.stagemate.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.roei.stagemate.MyApp
import com.roei.stagemate.R
import com.roei.stagemate.databinding.ItemTicketBinding
import com.roei.stagemate.data.interfaces.TicketCallback
import com.roei.stagemate.data.models.Ticket
import androidx.core.content.ContextCompat
import com.roei.stagemate.data.models.TicketType
import com.roei.stagemate.utilities.Constants
import android.view.View

// RecyclerView adapter for purchased ticket cards with status badge and download button.
// Used by TicketsFragment.
class TicketAdapter(tickets: List<Ticket> = emptyList()) :
    RecyclerView.Adapter<TicketAdapter.TicketViewHolder>() {

    var tickets: List<Ticket> = tickets
        private set

    var ticketCallback: TicketCallback? = null

    fun submitList(newList: List<Ticket>) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = tickets.size
            override fun getNewListSize() = newList.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int) = tickets[oldPos].id == newList[newPos].id
            override fun areContentsTheSame(oldPos: Int, newPos: Int) = tickets[oldPos] == newList[newPos]
        })
        tickets = newList
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TicketViewHolder {
        val binding = ItemTicketBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TicketViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TicketViewHolder, position: Int) =
        holder.bind(tickets[position])

    override fun getItemCount(): Int = tickets.size

    fun getItem(position: Int): Ticket = tickets[position]

    inner class TicketViewHolder(val binding: ItemTicketBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val pos = absoluteAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
                ticketCallback?.onTicketClicked(getItem(pos), pos)
            }

            binding.ticketBTNDownload.setOnClickListener {
                val pos = absoluteAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
                ticketCallback?.onDownloadClicked(getItem(pos), pos)
            }

            binding.ticketBTNRate.setOnClickListener {
                val pos = absoluteAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
                ticketCallback?.onRateClicked(getItem(pos), pos)
            }
        }

        fun bind(item: Ticket) {
            val context = binding.root.context
            with(binding) {
                ticketLBLTitle.text = item.eventTitle
                ticketLBLDate.text = item.getFormattedDate()
                ticketLBLLocation.text = item.eventLocation
                ticketLBLPrice.text = "${Constants.CURRENCY_SYMBOL}${item.price}"
                ticketLBLQuantity.text = "x${item.quantity}"
                ticketLBLReference.text = item.bookingReference

                ticketIMGPoster.setImageResource(R.drawable.placeholder_event)
                MyApp.imageLoader.loadImage(item.eventImageUrl, ticketIMGPoster)

                // Compute live status from date, not from stored enum
                val displayStatus = if (item.isPast()) Ticket.TicketStatus.PAST else item.ticketStatus
                val (statusText, statusBg) = when (displayStatus) {
                    Ticket.TicketStatus.UPCOMING -> context.getString(R.string.status_upcoming) to R.drawable.bg_ticket_status_upcoming
                    Ticket.TicketStatus.PAST      -> context.getString(R.string.status_past)     to R.drawable.bg_ticket_status_past
                    Ticket.TicketStatus.CANCELLED -> context.getString(R.string.status_cancelled) to R.drawable.bg_ticket_status_past
                    Ticket.TicketStatus.LIVE_NOW  -> context.getString(R.string.status_live_now)  to R.drawable.bg_ticket_status_live
                }
                ticketLBLStatus.text = statusText
                ticketLBLStatus.setBackgroundResource(statusBg)
                val statusColor = when (displayStatus) {
                    Ticket.TicketStatus.PAST -> R.color.danger_d500
                    else -> item.getStatusBadgeColor()
                }
                ticketLBLStatus.setTextColor(ContextCompat.getColor(context, statusColor))

                ticketLBLType.text = item.ticketType.englishName()

                val isPast = item.isPast()
                when {
                    item.isRated -> {
                        ticketBTNDownload.visibility = View.GONE
                        ticketBTNRate.visibility = View.GONE
                        ticketRATINGBARRating.visibility = View.VISIBLE
                        ticketRATINGBARRating.rating = item.rating
                    }
                    isPast -> {
                        // Past event, not yet rated → show Rate only
                        ticketBTNDownload.visibility = View.GONE
                        ticketBTNRate.visibility = View.VISIBLE
                        ticketRATINGBARRating.visibility = View.GONE
                    }
                    else -> {
                        // Upcoming event → show Download only (rating not allowed before event)
                        ticketBTNDownload.visibility = View.VISIBLE
                        ticketBTNRate.visibility = View.GONE
                        ticketRATINGBARRating.visibility = View.GONE
                    }
                }
            }
        }
    }

}
