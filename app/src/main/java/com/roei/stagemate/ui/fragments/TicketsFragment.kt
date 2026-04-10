package com.roei.stagemate.ui.fragments

import android.app.AlertDialog
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.ListenerRegistration
import com.roei.stagemate.R
import com.roei.stagemate.ui.adapters.EventAdapter
import com.roei.stagemate.ui.adapters.SkeletonAdapter
import com.roei.stagemate.ui.adapters.TicketAdapter
import com.roei.stagemate.databinding.FragmentTicketsBinding
import com.roei.stagemate.data.interfaces.TicketCallback
import com.roei.stagemate.data.interfaces.EventCallback
import com.roei.stagemate.data.repository.DataRepository
import com.roei.stagemate.ui.activities.ReceiptActivity
import com.roei.stagemate.ui.activities.TicketDetailActivity
import com.roei.stagemate.utilities.Constants
import com.roei.stagemate.data.models.Event
import com.roei.stagemate.data.models.Ticket
import com.roei.stagemate.utilities.QRCodeManager
import com.roei.stagemate.MyApp
import com.roei.stagemate.utilities.showSuccessSnackbar
import com.roei.stagemate.utilities.showInfoSnackbar
import com.roei.stagemate.utilities.showErrorSnackbar
import android.content.Intent
import androidx.lifecycle.lifecycleScope
import com.roei.stagemate.ui.dialogs.RatingDialog
import java.io.File
import java.io.FileOutputStream

// Displays user's favorites, recently viewed events, and purchased tickets.
// Used by MainActivity as Tab 3. Connects to DataRepository for favorites/tickets listeners.
class TicketsFragment : Fragment() {

    private var _binding: FragmentTicketsBinding? = null
    private val binding get() = _binding!!

    private var allTickets: List<Ticket> = emptyList()
    private lateinit var ticketAdapter: TicketAdapter
    private var currentFilter: Ticket.TicketStatus? = Ticket.TicketStatus.UPCOMING
    private var ticketsListener: ListenerRegistration? = null
    private var pendingFilter: String? = null

    private var favoritesListener: ListenerRegistration? = null

    private lateinit var favoritesAdapter: EventAdapter
    private lateinit var recentlyViewedAdapter: EventAdapter

    private var allFavoriteEvents: List<Event> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTicketsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupFavoritesRecyclerView()
        setupRecentlyViewedRecyclerView()
        setupTicketsRecyclerView()
        setupTicketFilterButtons()

        binding.ticketsRVFavorites.adapter = SkeletonAdapter(R.layout.item_skeleton_event, 3)
        binding.ticketsRVRecentlyViewed.adapter = SkeletonAdapter(R.layout.item_skeleton_event, 3)
        binding.ticketsRVUpcoming.adapter = SkeletonAdapter(R.layout.item_skeleton_event, 2)

        loadFavorites()
        loadRecentlyViewed()
        loadTickets()

        // Apply any filter requested before the view was created (e.g. from ProfileFragment)
        pendingFilter?.let { filter ->
            pendingFilter = null
            applyExternalFilter(filter)
        }
    }

    // --- Favorites ---

    private fun createEventCallback(adapter: EventAdapter): EventCallback =
        EventAdapter.createStandardCallback(this, adapter, binding.root, viewLifecycleOwner.lifecycleScope)

    private fun setupFavoritesRecyclerView() {
        favoritesAdapter = EventAdapter()
        favoritesAdapter.eventCallback = createEventCallback(favoritesAdapter)
        binding.ticketsRVFavorites.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = favoritesAdapter
        }
    }

    private fun loadFavorites() {
        favoritesListener?.remove()
        favoritesListener = DataRepository.listenToFavoriteEvents { events ->
            val b = _binding ?: return@listenToFavoriteEvents
            (b.ticketsRVFavorites.adapter as? SkeletonAdapter)?.stopAnimation()
            b.ticketsRVFavorites.adapter = favoritesAdapter
            allFavoriteEvents = events
            favoritesAdapter.submitList(events)
            b.ticketsLBLNoFavorites.visibility = if (events.isEmpty()) View.VISIBLE else View.GONE
            b.ticketsRVFavorites.visibility = if (events.isEmpty()) View.GONE else View.VISIBLE
        }
        if (favoritesListener == null) {
            _binding?.ticketsLBLNoFavorites?.visibility = View.VISIBLE
        }
    }

    // --- Recently Viewed ---

    private fun setupRecentlyViewedRecyclerView() {
        recentlyViewedAdapter = EventAdapter()
        recentlyViewedAdapter.eventCallback = createEventCallback(recentlyViewedAdapter)
        binding.ticketsRVRecentlyViewed.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recentlyViewedAdapter
        }
    }

    private fun loadRecentlyViewed() {
        // Try Firebase first for cross-device sync, fallback to local
        DataRepository.loadRecentlyViewed callback@{ cloudIds ->
            val b = _binding ?: return@callback
            val recentIds = (if (!cloudIds.isNullOrEmpty()) cloudIds
                else MyApp.sharedPrefsManager.getRecentlyViewedIds())
                .distinct()
                .take(10)

            if (recentIds.isEmpty()) {
                (b.ticketsRVRecentlyViewed.adapter as? SkeletonAdapter)?.stopAnimation()
                b.ticketsRVRecentlyViewed.adapter = recentlyViewedAdapter
                b.ticketsLBLNoRecentlyViewed.visibility = View.VISIBLE
                b.ticketsRVRecentlyViewed.visibility = View.GONE
                return@callback
            }

            DataRepository.getEventsByIds(recentIds) { events ->
                val b2 = _binding ?: return@getEventsByIds
                (b2.ticketsRVRecentlyViewed.adapter as? SkeletonAdapter)?.stopAnimation()
                b2.ticketsRVRecentlyViewed.adapter = recentlyViewedAdapter
                recentlyViewedAdapter.submitList(events)
                b2.ticketsLBLNoRecentlyViewed.visibility = if (events.isEmpty()) View.VISIBLE else View.GONE
                b2.ticketsRVRecentlyViewed.visibility = if (events.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }

    // --- Tickets ---

    private fun setupTicketsRecyclerView() {
        ticketAdapter = TicketAdapter()
        ticketAdapter.ticketCallback = object : TicketCallback {
            override fun onTicketClicked(ticket: Ticket, position: Int) {
                startActivity(TicketDetailActivity.newIntent(requireContext(), ticket))
            }

            override fun onDownloadClicked(ticket: Ticket, position: Int) {
                downloadTicket(ticket)
            }

            override fun onRateClicked(ticket: Ticket, position: Int) {
                showRatingDialog(ticket)
            }
        }

        binding.ticketsRVUpcoming.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ticketAdapter
        }
    }

    private fun setupTicketFilterButtons() {
        binding.ticketsBTNUpcoming.setOnClickListener {
            currentFilter = Ticket.TicketStatus.UPCOMING
            highlightButton(binding.ticketsBTNUpcoming)
            applyCurrentFilter()
        }
        binding.ticketsBTNPast.setOnClickListener {
            currentFilter = Ticket.TicketStatus.PAST
            highlightButton(binding.ticketsBTNPast)
            applyCurrentFilter()
        }
        highlightButton(binding.ticketsBTNUpcoming)
    }

    fun scrollToTicket(eventId: String) {
        if (_binding == null || eventId.isBlank()) return
        // Ensure we're showing upcoming tickets (newly purchased tickets are upcoming)
        currentFilter = Ticket.TicketStatus.UPCOMING
        highlightButton(binding.ticketsBTNUpcoming)
        applyCurrentFilter()
        // Find the ticket position and scroll to it
        val position = ticketAdapter.tickets.indexOfFirst { it.eventId == eventId }
        if (position >= 0) {
            binding.ticketsRVUpcoming.scrollToPosition(position)
        }
    }

    fun openTicketById(ticketId: String) {
        if (_binding == null || ticketId.isBlank()) return
        val ticket = allTickets.firstOrNull { it.id == ticketId }
        if (ticket != null) {
            val intent = Intent(requireContext(), ReceiptActivity::class.java).apply {
                putExtra(Constants.IntentKeys.TICKET_ID, ticket.id)
                putExtra(Constants.IntentKeys.BOOKING_REFERENCE, ticket.bookingReference)
                putExtra(Constants.IntentKeys.TOTAL_AMOUNT, String.format("%.2f", ticket.price))
                putExtra(Constants.IntentKeys.QR_CODE, ticket.qrCode)
                putExtra(Constants.IntentKeys.PURCHASE_DATE, ticket.purchaseDate)
                putExtra(Constants.IntentKeys.QUANTITY, ticket.quantity)
                putExtra(Constants.IntentKeys.EVENT_TITLE, ticket.eventTitle)
                putExtra(Constants.IntentKeys.EVENT_DATE, ticket.eventDate)
                putExtra(Constants.IntentKeys.EVENT_VENUE, ticket.eventLocation)
                putExtra(Constants.IntentKeys.EVENT_LOCATION, ticket.eventLocation)
                putExtra(Constants.IntentKeys.EVENT_IMAGE_URL, ticket.eventImageUrl)
            }
            startActivity(intent)
        } else {
            scrollToTicket(ticketId)
        }
    }

    fun applyExternalFilter(filter: String) {
        if (_binding == null) {
            pendingFilter = filter
            return
        }
        currentFilter = if (filter == "past") Ticket.TicketStatus.PAST else Ticket.TicketStatus.UPCOMING
        val b = _binding ?: return
        highlightButton(if (filter == "past") b.ticketsBTNPast else b.ticketsBTNUpcoming)
        applyCurrentFilter()
    }

    private fun loadTickets() {
        ticketsListener?.remove()
        ticketsListener = DataRepository.listenToMyTickets { tickets ->
            val b = _binding ?: return@listenToMyTickets
            (b.ticketsRVUpcoming.adapter as? SkeletonAdapter)?.stopAnimation()
            b.ticketsRVUpcoming.adapter = ticketAdapter
            allTickets = tickets
            applyCurrentFilter()
        }
    }

    private fun applyCurrentFilter() {
        val b = _binding ?: return
        val filtered = allTickets.filter { ticket ->
            val isPast = ticket.isPast()
            when (currentFilter) {
                Ticket.TicketStatus.UPCOMING -> !isPast
                Ticket.TicketStatus.PAST -> isPast
                else -> true
            }
        }

        ticketAdapter.submitList(filtered)

        val isEmpty = filtered.isEmpty()
        b.ticketsLBLNoTickets.visibility = if (isEmpty) View.VISIBLE else View.GONE
        b.ticketsRVUpcoming.visibility = if (isEmpty) View.GONE else View.VISIBLE
        if (isEmpty) {
            b.ticketsLBLNoTickets.text = if (currentFilter == Ticket.TicketStatus.PAST)
                getString(R.string.no_past_events)
            else
                getString(R.string.no_tickets_yet)
        }
    }

    private fun highlightButton(selectedButton: View) {
        listOf(
            binding.ticketsBTNUpcoming,
            binding.ticketsBTNPast
        ).forEach { it.isSelected = (it == selectedButton) }
    }

    // --- Rating ---

    // Dead code wired: uses existing RatingDialog class instead of inline MaterialAlertDialogBuilder
    private fun showRatingDialog(ticket: Ticket) {
        val dialog = RatingDialog(requireContext(), ticket) { _ ->
            _binding ?: return@RatingDialog
            showSuccessSnackbar(getString(R.string.thank_you_for_rating))
            loadTickets()
        }
        dialog.showDialog()
    }

    // --- Helpers ---

    private fun showTicketQRCode(ticket: Ticket) {
        if (!QRCodeManager.isValidTicketQRData(ticket.qrCode)) {
            showErrorSnackbar(getString(R.string.invalid_qr_data))
            return
        }
        try {
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_qr_code, null)
            val qrImageView = dialogView.findViewById<ImageView>(R.id.qr_IMG_code)
            val qrBitmap = QRCodeManager.generateQRCode(ticket.qrCode, 512, 512)
            qrImageView.setImageBitmap(qrBitmap)

            AlertDialog.Builder(requireContext())
                .setTitle(ticket.eventTitle)
                .setMessage("${getString(R.string.booking_reference)}: ${ticket.bookingReference}\n${ticket.seatNumbers.joinToString(", ")}")
                .setView(dialogView)
                .setPositiveButton(getString(R.string.close), null)
                .setNeutralButton(getString(R.string.download)) { _, _ -> downloadTicket(ticket) }
                .show()

        } catch (e: Exception) {
            val qrBitmap = QRCodeManager.generateQRCode(ticket.qrCode, 512, 512)
            val imageView = ImageView(requireContext()).apply {
                setImageBitmap(qrBitmap)
                setPadding(40, 40, 40, 40)
            }

            AlertDialog.Builder(requireContext())
                .setTitle(ticket.eventTitle)
                .setMessage("${getString(R.string.booking_reference)}: ${ticket.bookingReference}")
                .setView(imageView)
                .setPositiveButton(getString(R.string.close), null)
                .setNeutralButton(getString(R.string.download)) { _, _ -> downloadTicket(ticket) }
                .show()
        }
    }

    private fun downloadTicket(ticket: Ticket) {
        if (!QRCodeManager.isValidTicketQRData(ticket.qrCode)) {
            showErrorSnackbar(getString(R.string.invalid_qr_data))
            return
        }
        val qrBitmap = QRCodeManager.generateQRCode(ticket.qrCode, 1024, 1024) ?: run {
            showErrorSnackbar(getString(R.string.failed_to_download_ticket))
            return
        }

        try {
            val filename = "StageMate_Ticket_${ticket.bookingReference}.png"
            val file = File(requireContext().getExternalFilesDir(null), filename)

            FileOutputStream(file).use { out ->
                qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            MyApp.signalManager.vibrate(100)
            MyApp.signalManager.toast(getString(R.string.ticket_downloaded_toast))
            showSuccessSnackbar(getString(R.string.ticket_downloaded, file.absolutePath))

        } catch (e: Exception) {
            showErrorSnackbar(getString(R.string.failed_to_download_ticket))
        }
    }

    override fun onDestroyView() {
        _binding?.ticketsRVFavorites?.adapter = null
        _binding?.ticketsRVRecentlyViewed?.adapter = null
        _binding?.ticketsRVUpcoming?.adapter = null
        ticketsListener?.remove()
        favoritesListener?.remove()
        super.onDestroyView()
        _binding = null
    }
}