package com.roei.stagemate.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.roei.stagemate.R
import com.roei.stagemate.databinding.ItemNotificationBinding
import com.roei.stagemate.data.interfaces.NotificationCallback
import com.roei.stagemate.data.models.AppNotification

// RecyclerView adapter for notification items with type-based icons and unread indicator.
// Used by NotificationsFragment.
class NotificationAdapter(notifications: List<AppNotification> = emptyList()) :
    RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    var notifications: List<AppNotification> = notifications
        private set

    var notificationCallback: NotificationCallback? = null

    fun submitList(newList: List<AppNotification>) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = notifications.size
            override fun getNewListSize() = newList.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int) = notifications[oldPos].id == newList[newPos].id
            override fun areContentsTheSame(oldPos: Int, newPos: Int) = notifications[oldPos] == newList[newPos]
        })
        notifications = newList
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) =
        holder.bind(notifications[position])

    override fun getItemCount(): Int = notifications.size

    fun getItem(position: Int): AppNotification = notifications[position]

    inner class NotificationViewHolder(val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val pos = absoluteAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
                notificationCallback?.onNotificationClicked(getItem(pos), pos)
            }

            binding.notificationBTNDelete.setOnClickListener {
                val pos = absoluteAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
                notificationCallback?.onDeleteClicked(notifications[pos], pos)
            }
        }

        fun bind(item: AppNotification) {
            binding.notificationLBLTitle.text = item.title
            binding.notificationLBLMessage.text = item.message
            binding.notificationLBLTimestamp.text = item.getFormattedTime()
            binding.notificationBTNDelete.contentDescription = "Delete notification"

            val (iconRes, bgRes) = getTypeResources(item.type)
            binding.notificationIMGIcon.setImageResource(iconRes)
            binding.notificationIMGIcon.setBackgroundResource(bgRes)

            binding.notificationVIEWIndicator.visibility = if (item.isRead) View.GONE else View.VISIBLE
        }

        private fun getTypeResources(type: AppNotification.NotificationType): Pair<Int, Int> =
            when (type) {
                AppNotification.NotificationType.SUCCESS      -> R.drawable.ic_check to R.drawable.circle_background_success
                AppNotification.NotificationType.CANCELLED    -> R.drawable.ic_close to R.drawable.circle_background_danger
                AppNotification.NotificationType.INFO         -> R.drawable.ic_info to R.drawable.circle_background_info
                AppNotification.NotificationType.REMINDER     -> R.drawable.ic_bell to R.drawable.circle_background_warning
                AppNotification.NotificationType.EVENT_UPDATE -> R.drawable.ic_info to R.drawable.circle_background_info
            }
    }
}
