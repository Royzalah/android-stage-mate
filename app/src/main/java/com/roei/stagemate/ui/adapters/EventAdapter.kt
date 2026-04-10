package com.roei.stagemate.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.roei.stagemate.MyApp
import com.roei.stagemate.R
import com.roei.stagemate.databinding.ItemEventBinding
import com.roei.stagemate.data.interfaces.EventCallback
import com.roei.stagemate.data.models.Event
import com.roei.stagemate.ui.activities.EventDetailActivity
import com.roei.stagemate.utilities.Constants
import com.roei.stagemate.utilities.DateFormatter
import com.roei.stagemate.utilities.FavoriteHelper
import androidx.lifecycle.LifecycleCoroutineScope
import android.view.View as AndroidView
import androidx.fragment.app.Fragment
import java.lang.ref.WeakReference

// RecyclerView adapter for event cards with image, title, price, rating, and favorite toggle.
// Used by HomeFragment, SearchFragment, TicketsFragment with inline EventCallback implementations.
class EventAdapter(events: List<Event> = emptyList()) :
    RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    var events: List<Event> = events
        private set

    var eventCallback: EventCallback? = null

    companion object {
        // Standard callback: click/bookNow opens EventDetail, favorite calls FavoriteHelper.toggle
        fun createStandardCallback(
            fragment: Fragment,
            adapter: EventAdapter,
            rootView: AndroidView,
            scope: LifecycleCoroutineScope
        ): EventCallback {
            val weakFragment = WeakReference(fragment)
            val weakView = WeakReference(rootView)
            return object : EventCallback {
                override fun onEventClicked(event: Event, position: Int) {
                    val f = weakFragment.get() ?: return
                    f.startActivity(EventDetailActivity.newIntent(f.requireContext(), event.id, event))
                }
                override fun onFavoriteClicked(event: Event, position: Int) {
                    val view = weakView.get() ?: return
                    FavoriteHelper.toggle(event, adapter, position, view, scope)
                }
                override fun onBookNowClicked(event: Event, position: Int) {
                    val f = weakFragment.get() ?: return
                    f.startActivity(EventDetailActivity.newIntent(f.requireContext(), event.id, event))
                }
            }
        }
    }

    fun submitList(newList: List<Event>) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = events.size
            override fun getNewListSize() = newList.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int) = events[oldPos].id == newList[newPos].id
            override fun areContentsTheSame(oldPos: Int, newPos: Int) = events[oldPos] == newList[newPos]
        })
        events = newList
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) =
        holder.bind(events[position])

    override fun getItemCount(): Int = events.size

    fun getItem(position: Int): Event = events[position]

    inner class EventViewHolder(val binding: ItemEventBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val pos = absoluteAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
                eventCallback?.onEventClicked(getItem(pos), pos)
            }
            val favoriteClick = View.OnClickListener {
                val pos = absoluteAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@OnClickListener
                eventCallback?.onFavoriteClicked(getItem(pos), pos)
            }
            binding.eventIMGFavorite.setOnClickListener(favoriteClick)
            binding.eventCVFavorite.setOnClickListener(favoriteClick)
            binding.eventBTNBookNow.setOnClickListener {
                val pos = absoluteAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
                eventCallback?.onBookNowClicked(getItem(pos), pos)
            }
        }

        fun bind(item: Event) {
            with(binding) {
                eventLBLTitle.text = item.title
                eventLBLCategory.text = item.category
                eventLBLLocation.text = item.location
                eventLBLDate.text = DateFormatter.formatDate(item.date)
                eventLBLRating.text = String.format("%.1f", item.rating)
                eventIMGPoster.setImageResource(R.drawable.placeholder_event)
                MyApp.imageLoader.loadImage(item.imageUrl, eventIMGPoster)

                if (item.isFavorite) {
                    eventIMGFavorite.setImageResource(R.drawable.ic_heart_filled)
                    eventIMGFavorite.contentDescription = root.context.getString(R.string.remove_from_favorites)
                } else {
                    eventIMGFavorite.setImageResource(R.drawable.ic_heart_empty)
                    eventIMGFavorite.contentDescription = root.context.getString(R.string.add_to_favorites)
                }
                eventIMGFavorite.clearColorFilter()

                if (item.isHot) {
                    eventCHIPHot.visibility = View.VISIBLE
                } else {
                    eventCHIPHot.visibility = View.GONE
                }

                if (item.soldOut) {
                    eventLBLPrice.text = root.context.getString(R.string.sold_out)
                    eventLBLPrice.setTextColor(ContextCompat.getColor(root.context, R.color.error))
                    eventLBLParticipants.text = "${item.participants}+ Going"
                } else if (item.almostSoldOut) {
                    eventLBLPrice.text = "${Constants.CURRENCY_SYMBOL}${item.price.toInt()}"
                    eventLBLPrice.setTextColor(ContextCompat.getColor(root.context, R.color.white))
                    eventLBLParticipants.text = root.context.getString(R.string.tickets_left_pct, item.calcAvailabilityPercentage())
                } else {
                    eventLBLPrice.text = "${Constants.CURRENCY_SYMBOL}${item.price.toInt()}"
                    eventLBLPrice.setTextColor(ContextCompat.getColor(root.context, R.color.white))
                    eventLBLParticipants.text = "${item.participants}+ Going"
                }
            }
        }
    }
}
