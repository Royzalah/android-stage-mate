package com.roei.stagemate.ui.fragments

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.ListenerRegistration
import com.roei.stagemate.R
import com.roei.stagemate.data.interfaces.NotificationCallback
import com.roei.stagemate.data.models.AppNotification
import com.roei.stagemate.data.repository.DataRepository
import com.roei.stagemate.databinding.FragmentNotificationsBinding
import com.roei.stagemate.ui.activities.EventDetailActivity
import com.roei.stagemate.ui.activities.TicketDetailActivity
import com.roei.stagemate.ui.adapters.NotificationAdapter
import com.roei.stagemate.ui.adapters.SkeletonAdapter
import com.roei.stagemate.utilities.Constants
import android.content.Intent
import androidx.lifecycle.lifecycleScope
import com.roei.stagemate.utilities.showErrorSnackbar
import kotlinx.coroutines.launch
import com.roei.stagemate.utilities.M3Snackbar
import com.roei.stagemate.utilities.showSuccessSnackbar
import com.roei.stagemate.utilities.showInfoSnackbar

// Displays user notifications with swipe-to-delete and mark-as-read support.
// Opened from HomeFragment notification bell. Connects to DataRepository for notification CRUD.
class NotificationsFragment : Fragment() {

    // Interface callback per course standard (fragments-callbacks.md).
    // NEVER cast activity directly — always use this callback.
    interface Callback {
        fun onNavigateToTicketWithScroll(eventId: String)
        fun onOpenTicketDetail(ticketId: String, eventId: String)
    }
    var callback: Callback? = null

    companion object {
        private const val SWIPE_ICON_TEXT = "🗑"
        private const val SWIPE_TEXT_SIZE = 40f
        private const val SWIPE_PADDING_START = 40f
        private const val SWIPE_PADDING_END = 80f
    }

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private lateinit var notificationAdapter: NotificationAdapter
    private val skeletonAdapter = SkeletonAdapter(R.layout.item_skeleton_notification, 6)
    private var notifications: MutableList<AppNotification> = mutableListOf()
    private var notificationsListener: ListenerRegistration? = null
    private var isLoading = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        setupRecyclerView()
        viewLifecycleOwner.lifecycleScope.launch {
            loadNotifications()
        }
    }

    override fun onDestroyView() {
        notificationsListener?.remove()
        _binding?.notificationsRVList?.adapter = null
        super.onDestroyView()
        _binding = null
    }

    private fun initViews() {
        binding.notificationsBTNClearAll.setOnClickListener { clearAllNotifications() }
    }

    private fun setupRecyclerView() {
        notificationAdapter = NotificationAdapter()

        notificationAdapter.notificationCallback = object : NotificationCallback {
            override fun onNotificationClicked(notification: AppNotification, position: Int) {
                markAsRead(notification, position)
                if (notification.type == AppNotification.NotificationType.SUCCESS) {
                    parentFragmentManager.popBackStackImmediate()
                    if (notification.ticketId.isNotEmpty()) {
                        DataRepository.listenToMyTickets { tickets ->
                            if (_binding == null) return@listenToMyTickets
                            val ticket = tickets.firstOrNull { it.eventId == notification.eventId }
                            if (ticket != null) {
                                startActivity(TicketDetailActivity.newIntent(requireContext(), ticket))
                            } else {
                                callback?.onNavigateToTicketWithScroll(notification.eventId)
                            }
                        }?.remove()
                    } else {
                        callback?.onNavigateToTicketWithScroll(notification.eventId)
                    }
                } else if (notification.eventId.isNotEmpty()) {
                    val intent = Intent(requireContext(), EventDetailActivity::class.java)
                    intent.putExtra(Constants.IntentKeys.EVENT_ID, notification.eventId)
                    startActivity(intent)
                }
            }

            override fun onDeleteClicked(notification: AppNotification, position: Int) {
                deleteNotification(notification)
            }
        }

        binding.notificationsRVList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = notificationAdapter
        }

        setupSwipeToDelete()
    }

    private fun setupSwipeToDelete() {
        val swipeBackground = ColorDrawable(
            androidx.core.content.ContextCompat.getColor(requireContext(), R.color.danger_d400)
        )
        val paint = Paint().apply {
            color = Color.WHITE
            textSize = SWIPE_TEXT_SIZE
            isAntiAlias = true
        }

        val swipeCallback = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.absoluteAdapterPosition
                if (position != RecyclerView.NO_POSITION && position < notifications.size) {
                    deleteNotification(notifications[position])
                } else if (position != RecyclerView.NO_POSITION && position < notificationAdapter.itemCount) {
                    notificationAdapter.notifyItemChanged(position)
                }
            }

            override fun onChildDraw(
                canvas: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView

                swipeBackground.setBounds(
                    if (dX > 0) itemView.left else (itemView.right + dX.toInt()),
                    itemView.top,
                    if (dX > 0) dX.toInt() else itemView.right,
                    itemView.bottom
                )
                swipeBackground.draw(canvas)

                val iconText = SWIPE_ICON_TEXT
                val textX = if (dX > 0) itemView.left + SWIPE_PADDING_START else itemView.right - SWIPE_PADDING_END
                val textY = itemView.top + (itemView.height / 2f) + (paint.textSize / 2f)
                canvas.drawText(iconText, textX, textY, paint)

                super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }

        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.notificationsRVList)
    }

    private fun loadNotifications() {
        if (isLoading) return
        isLoading = true
        showLoading(true)

        notificationsListener = DataRepository.listenToNotifications { loaded ->
            viewLifecycleOwner.lifecycleScope.launch {
                if (_binding == null) return@launch
                isLoading = false
                showLoading(false)

                // Hide notifications older than 30 days (AppNotification.isExpired()).
                notifications = loaded.filterNot { it.isExpired() }.toMutableList()
                notificationAdapter.submitList(notifications)

                updateEmptyState(notifications.isEmpty())
            }
        }

        if (notificationsListener == null) {
            isLoading = false
            if (_binding != null) showLoading(false)
            showErrorSnackbar(getString(R.string.login_to_see_notifications))
        }
    }

    private fun markAsRead(notification: AppNotification, position: Int) {
        if (notification.isRead) return

        notification.markAsRead()
        notificationAdapter.notifyItemChanged(position)

        DataRepository.markNotificationAsRead(notification.id) { success, _ ->
            if (!success) {
                viewLifecycleOwner.lifecycleScope.launch {
                    notification.isRead = false
                    notificationAdapter.notifyItemChanged(position)
                    showErrorSnackbar(getString(R.string.notification_update_failed))
                }
            }
        }
    }

    private fun deleteNotification(notification: AppNotification) {
        DataRepository.deleteNotification(notification.id) { success, _ ->
            viewLifecycleOwner.lifecycleScope.launch {
                if (_binding == null) return@launch
                if (success) {
                    showSuccessSnackbar(getString(R.string.notification_deleted))
                } else {
                    showErrorSnackbar(getString(R.string.notification_delete_failed))
                }
            }
        }
    }

    private fun clearAllNotifications() {
        if (notifications.isEmpty()) {
            showInfoSnackbar(getString(R.string.no_notifications_to_clear))
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.clear_all_notifications_title))
            .setMessage(getString(R.string.clear_all_notifications_message))
            .setPositiveButton(getString(R.string.yes)) { dialog, _ ->
                dialog.dismiss()
                showLoading(true)
                DataRepository.deleteAllNotifications { success, _ ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        if (_binding == null) return@launch
                        showLoading(false)
                        if (success) {
                            val root = _binding?.root ?: return@launch
                            M3Snackbar.action(
                                root,
                                getString(R.string.notifications_cleared),
                                getString(R.string.undo)
                            ) { /* Server-side deletion is final — acknowledge only */ }
                        } else {
                            showErrorSnackbar(getString(R.string.notifications_clear_failed))
                        }
                    }
                }
            }
            .setNegativeButton(getString(R.string.no), null)
            .show()
    }

    private fun showLoading(show: Boolean) {
        val b = _binding ?: return
        if (show) {
            b.notificationsRVList.adapter = skeletonAdapter
        } else {
            (b.notificationsRVList.adapter as? SkeletonAdapter)?.stopAnimation()
            b.notificationsRVList.adapter = notificationAdapter
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        val b = _binding ?: return
        b.notificationsLAYOUTEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        b.notificationsRVList.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
}