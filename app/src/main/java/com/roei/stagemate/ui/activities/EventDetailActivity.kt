package com.roei.stagemate.ui.activities

import android.content.Intent
import android.os.Bundle
import com.roei.stagemate.data.models.AppNotification
import com.roei.stagemate.utilities.showErrorSnackbar
import com.roei.stagemate.utilities.showSuccessSnackbar
import com.roei.stagemate.utilities.showWarningSnackbar
import com.roei.stagemate.utilities.showInfoSnackbar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.ListenerRegistration
import com.roei.stagemate.R
import com.roei.stagemate.databinding.ActivityEventDetailBinding
import com.roei.stagemate.data.models.Event
import com.roei.stagemate.data.models.IsraeliEventsGenerator
import com.roei.stagemate.data.models.TicketType
import com.roei.stagemate.data.models.Venue
import com.roei.stagemate.utilities.Constants
import com.roei.stagemate.utilities.DateFormatter
import com.roei.stagemate.data.models.IsraeliLocations
import com.roei.stagemate.data.repository.DataRepository
import com.roei.stagemate.MyApp
import com.roei.stagemate.utilities.GoogleCalendarManager
import androidx.activity.result.contract.ActivityResultContracts
import java.text.SimpleDateFormat
import java.util.Locale

// Full event details screen with image, description, map, favorites, sharing, and calendar.
// Listens to real-time Firestore updates for the event.
class EventDetailActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityEventDetailBinding
    private var analytics: FirebaseAnalytics? = null
    private var event: Event? = null
    private var isEventDataLoading = false
    private var eventListener: ListenerRegistration? = null
    private var googleMap: GoogleMap? = null

    companion object {
        fun newIntent(context: android.content.Context, eventId: String, event: Event? = null): Intent {
            return Intent(context, EventDetailActivity::class.java).apply {
                putExtra(Constants.IntentKeys.EVENT_ID, eventId)
                if (event != null) putExtra("EVENT_OBJECT", event)
            }
        }
    }

    private val calendarSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        GoogleCalendarManager.handleSignInResult(this) { success, _ ->
            if (success) showSuccessSnackbar(getString(R.string.event_added_to_calendar))
            else showInfoSnackbar(getString(R.string.calendar_fallback_used))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try { analytics = FirebaseAnalytics.getInstance(this) } catch (e: Exception) { android.util.Log.w("EventDetail", "Analytics init failed", e) }
        try {
            FirebaseCrashlytics.getInstance().apply {
                setCustomKey("last_activity", "EventDetailActivity")
                val eventId = intent.getStringExtra(Constants.IntentKeys.EVENT_ID)
                if (eventId != null) setCustomKey("viewing_event_id", eventId)
            }
        } catch (e: Exception) { android.util.Log.w("EventDetail", "Crashlytics init failed", e) }

        loadEventData()
        initViews()
        initializeMap()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        loadEventData()
    }

    // Loads event from Firestore listener, falls back to the Event object passed via intent
    private fun loadEventData() {
        eventListener?.remove()
        isEventDataLoading = true
        var eventId = intent.getStringExtra(Constants.IntentKeys.EVENT_ID)
        if (eventId == null) {
            val data = intent.data
            if (data != null && data.pathSegments.getOrNull(0) == "event") {
                eventId = data.pathSegments.getOrNull(1)
            }
        }
        if (eventId == null) {
            showErrorSnackbar(getString(R.string.event_id_not_found))
            finish()
            return
        }
        val passedEvent = intent.getSerializableExtraCompat<Event>("EVENT_OBJECT")

        eventListener = DataRepository.listenToEvent(eventId) { loadedEvent ->
            isEventDataLoading = false
            if (loadedEvent != null) {
                event = loadedEvent
                lifecycleScope.launch { displayEventData() }
            } else if (passedEvent != null && event == null) {
                event = passedEvent
                lifecycleScope.launch { displayEventData() }
            } else if (event == null) {
                showErrorSnackbar(getString(R.string.error_loading_event))
            }
        }

        // Show passed event immediately while Firebase loads
        if (passedEvent != null) {
            event = passedEvent
            displayEventData()
        }
        DataRepository.incrementViewCount(eventId)
        MyApp.sharedPrefsManager.saveRecentlyViewed(eventId)
        DataRepository.saveRecentlyViewed(MyApp.sharedPrefsManager.getRecentlyViewedIds()) { _, _ -> }
    }

    private fun initViews() {
        binding.detailIMGFavorite.setOnClickListener { toggleFavorite() }
        binding.detailBTNBookNow.setOnClickListener { buyNow() }
        binding.detailBTNReadMore.setOnClickListener { expandDescription() }
        binding.detailBTNAddToCalendar.setOnClickListener { addToGoogleCalendar() }
        binding.detailBTNShare.setOnClickListener { shareEvent() }
    }

    private fun displayEventData() {
        val currentEvent = event ?: return

        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.CONTENT_TYPE, "event")
            putString(FirebaseAnalytics.Param.ITEM_ID, currentEvent.id)
            putString(FirebaseAnalytics.Param.ITEM_NAME, currentEvent.title)
            putString(FirebaseAnalytics.Param.ITEM_CATEGORY, currentEvent.category)
        }
        analytics?.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)

        MyApp.imageLoader.loadImage(currentEvent.imageUrl, binding.detailIMGEvent)

        binding.detailLBLTitle.text = currentEvent.title
        binding.detailLBLSubtitle.text = "+${currentEvent.participants} " + getString(R.string.participants)
        // Fall back to generator description if Firebase has a stale/generic one
        val desc = currentEvent.description
        binding.detailLBLDescription.text = if (desc.isBlank() || desc.matches(Regex(".+ event: .+", RegexOption.DOT_MATCHES_ALL))) {
            IsraeliEventsGenerator.getDescriptionForTitle(currentEvent.title, currentEvent.category)
        } else {
            desc
        }
        binding.detailLBLDate.text = DateFormatter.formatDate(currentEvent.date)
        binding.detailLBLTime.text = DateFormatter.formatTime(currentEvent.time)
        binding.detailLBLLocation.text = currentEvent.venue

        updateFavoriteIcon()
        displayAvailability(currentEvent)
        displayEventLocationOnMap(currentEvent)
    }

    private fun displayAvailability(currentEvent: Event) {
        when {
            currentEvent.isPast() -> {
                binding.detailBTNBookNow.isEnabled = false
                binding.detailBTNBookNow.text = getString(R.string.event_ended)
                binding.detailBTNBookNow.alpha = 0.5f
            }
            currentEvent.soldOut -> {
                binding.detailBTNBookNow.isEnabled = false
                binding.detailBTNBookNow.text = getString(R.string.sold_out)
            }
            currentEvent.almostSoldOut -> {
                val pct = currentEvent.calcAvailabilityPercentage()
                binding.detailBTNBookNow.text = getString(R.string.book_now)
                binding.detailBTNBookNow.isEnabled = true
                showWarningSnackbar(getString(R.string.only_n_percent_tickets_left, pct))
            }
            else -> {
                binding.detailBTNBookNow.isEnabled = true
                binding.detailBTNBookNow.text = getString(R.string.book_now)
            }
        }
    }

    private fun toggleFavorite() {
        val currentEvent = event ?: return
        currentEvent.toggleFavorite()
        val newFavoriteState = currentEvent.isFavorite
        updateFavoriteIcon()

        DataRepository.toggleFavorite(currentEvent.id, newFavoriteState) { success ->
            lifecycleScope.launch {
                if (success) {
                    val message = if (newFavoriteState) getString(R.string.added_to_favorites)
                    else getString(R.string.removed_from_favorites)
                    showSuccessSnackbar(message)

                    if (newFavoriteState) {
                        val notification = AppNotification.Builder()
                            .title(getString(R.string.added_to_favorites_title))
                            .message(getString(R.string.added_to_favorites_message, currentEvent.title))
                            .type(AppNotification.NotificationType.INFO)
                            .eventId(currentEvent.id)
                            .imageUrl(currentEvent.imageUrl)
                            .build()
                        DataRepository.saveNotification(notification) { _, _ -> }
                    }
                } else {
                    currentEvent.isFavorite = !newFavoriteState
                    updateFavoriteIcon()
                    showErrorSnackbar(getString(R.string.failed_to_update_favorite))
                }
            }
        }
    }

    private fun updateFavoriteIcon() {
        val iconRes = if (event?.isFavorite == true) R.drawable.ic_heart_filled
        else R.drawable.ic_heart_empty
        binding.detailIMGFavorite.setImageResource(iconRes)
    }

    // Opens seat selection (or payment directly for standing/festival events)
    private fun buyNow() {
        if (isEventDataLoading) {
            showWarningSnackbar(getString(R.string.loading))
            return
        }

        val currentEvent = event ?: return

        if (currentEvent.isPast()) {
            showWarningSnackbar(getString(R.string.event_already_passed))
            return
        }

        if (currentEvent.soldOut) {
            showSoldOutDialog()
            return
        }

        // Festivals: quantity picker, no seat selection
        if (currentEvent.category.equals(Constants.Categories.FESTIVAL, ignoreCase = true)) {
            showFestivalQuantityPicker(currentEvent)
            return
        }

        // Force seated categories to use the seat map even if Firestore has stale eventType
        val seatedCategories = listOf(
            Constants.Categories.MUSIC, Constants.Categories.STAND_UP,
            Constants.Categories.THEATER, Constants.Categories.CHILDREN,
            Constants.Categories.SPORT
        )
        if (currentEvent.eventType != Event.EventType.SEATED &&
            seatedCategories.any { currentEvent.category.equals(it, ignoreCase = true) }) {
            currentEvent.eventType = Event.EventType.SEATED
        }

        // Standing-only: skip seat selection, go straight to payment
        if (currentEvent.eventType == Event.EventType.STANDING_ONLY) {
            buyStandingTicket(currentEvent)
            return
        }

        val intent = Intent(this, SeatSelectionActivity::class.java).apply {
            putExtra(Constants.IntentKeys.EVENT_ID, currentEvent.id)
            putExtra(Constants.IntentKeys.EVENT_TITLE, currentEvent.title)
            putExtra(Constants.IntentKeys.EVENT_DATE, currentEvent.date)
            putExtra(Constants.IntentKeys.EVENT_TIME, currentEvent.time)
            putExtra(Constants.IntentKeys.EVENT_LOCATION, currentEvent.location)
            putExtra(Constants.IntentKeys.EVENT_IMAGE_URL, currentEvent.imageUrl)
            putExtra(Constants.IntentKeys.EVENT_PRICE, currentEvent.price)
            putExtra(Constants.IntentKeys.QUANTITY, 1)
            putExtra(Constants.IntentKeys.TICKET_TYPE, "REGULAR")
            putExtra(Constants.IntentKeys.TOTAL_AMOUNT, currentEvent.price)
            putExtra(Constants.IntentKeys.VENUE_TYPE, determineVenueType(currentEvent).name)
            putExtra(Constants.IntentKeys.VENUE_NAME, currentEvent.venue)
            putExtra(Constants.IntentKeys.EVENT_CATEGORY, currentEvent.category)
            putExtra(Constants.IntentKeys.EVENT_TYPE, currentEvent.eventType.name)
            putExtra("REQUIRES_SEAT_SELECTION", currentEvent.requiresSeatSelection())
        }
        startActivity(intent)
    }

    // Goes to payment directly for standing/festival events (no seat picking)
    private fun buyStandingTicket(event: Event, quantity: Int = 1) {
        val seatNumbers = Array(quantity) { i -> "General Admission #${i + 1}" }
        val seatSections = Array(quantity) { "General Admission" }
        val seatRows = Array(quantity) { "GA" }
        val seatPrices = DoubleArray(quantity) { event.price }
        val totalAmount = quantity * event.price

        val intent = Intent(this, PaymentActivity::class.java).apply {
            putExtra(Constants.IntentKeys.EVENT_ID, event.id)
            putExtra(Constants.IntentKeys.EVENT_TITLE, event.title)
            putExtra(Constants.IntentKeys.EVENT_DATE, event.date)
            putExtra(Constants.IntentKeys.EVENT_TIME, event.time)
            putExtra(Constants.IntentKeys.EVENT_LOCATION, event.location)
            putExtra(Constants.IntentKeys.EVENT_VENUE, event.venue)
            putExtra(Constants.IntentKeys.EVENT_IMAGE_URL, event.imageUrl)
            putExtra(Constants.IntentKeys.TOTAL_AMOUNT, totalAmount.toString())
            putExtra(Constants.IntentKeys.QUANTITY, quantity)
            putExtra(Constants.IntentKeys.TICKET_TYPE, TicketType.REGULAR.name)
            putExtra(Constants.IntentKeys.SEAT_NUMBERS, seatNumbers)
            putExtra(Constants.IntentKeys.SEAT_SECTIONS, seatSections)
            putExtra(Constants.IntentKeys.SEAT_ROWS, seatRows)
            putExtra(Constants.IntentKeys.SEAT_PRICES, seatPrices)
            putExtra(Constants.IntentKeys.IS_STANDING_EVENT, true)
            putExtra(Constants.IntentKeys.RESERVATION_DEADLINE,
                System.currentTimeMillis() + 7 * 60 * 1000L)
        }
        startActivity(intent)
    }

    // BottomSheet quantity picker for festival events
    private fun showFestivalQuantityPicker(event: Event) {
        val maxQty = minOf(10, event.availableTickets)
        if (maxQty <= 0) {
            showSoldOutDialog()
            return
        }

        var quantity = 1

        val sheet = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val ctx = this

        val content = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(dp(24), dp(24), dp(24), dp(24))
            setBackgroundColor(androidx.core.content.ContextCompat.getColor(ctx, R.color.surface))
        }

        // Title
        val titleView = com.google.android.material.textview.MaterialTextView(ctx).apply {
            text = event.title
            setTextColor(androidx.core.content.ContextCompat.getColor(ctx, R.color.white))
            textSize = 20f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        content.addView(titleView)

        // Spacer
        content.addView(android.view.View(ctx).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(16))
        })

        // Price per ticket
        val priceLabel = com.google.android.material.textview.MaterialTextView(ctx).apply {
            text = getString(R.string.price_format, event.price)
            setTextColor(androidx.core.content.ContextCompat.getColor(ctx, R.color.text_secondary))
            textSize = 16f
        }
        content.addView(priceLabel)

        // Remaining tickets
        val remainingLabel = com.google.android.material.textview.MaterialTextView(ctx).apply {
            text = "${event.availableTickets} ${getString(R.string.tickets_remaining)}"
            setTextColor(androidx.core.content.ContextCompat.getColor(ctx, R.color.text_secondary))
            textSize = 14f
        }
        content.addView(remainingLabel)

        // Spacer
        content.addView(android.view.View(ctx).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(20))
        })

        // Quantity selector row
        val qtyRow = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        val qtyText = com.google.android.material.textview.MaterialTextView(ctx).apply {
            text = quantity.toString()
            setTextColor(androidx.core.content.ContextCompat.getColor(ctx, R.color.white))
            textSize = 24f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = android.view.Gravity.CENTER
            layoutParams = android.widget.LinearLayout.LayoutParams(dp(60), android.widget.LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val totalLabel = com.google.android.material.textview.MaterialTextView(ctx).apply {
            text = getString(R.string.total_price_format, quantity * event.price)
            setTextColor(androidx.core.content.ContextCompat.getColor(ctx, R.color.accent_primary))
            textSize = 18f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        fun updateQtyUI() {
            qtyText.text = quantity.toString()
            totalLabel.text = getString(R.string.total_price_format, quantity * event.price)
        }

        val minusBtn = com.google.android.material.button.MaterialButton(ctx).apply {
            text = "−"
            textSize = 20f
            layoutParams = android.widget.LinearLayout.LayoutParams(dp(52), dp(52))
            setOnClickListener {
                if (quantity > 1) { quantity--; updateQtyUI() }
            }
        }

        val plusBtn = com.google.android.material.button.MaterialButton(ctx).apply {
            text = "+"
            textSize = 20f
            layoutParams = android.widget.LinearLayout.LayoutParams(dp(52), dp(52))
            setOnClickListener {
                if (quantity < maxQty) { quantity++; updateQtyUI() }
            }
        }

        qtyRow.addView(minusBtn)
        qtyRow.addView(qtyText)
        qtyRow.addView(plusBtn)
        content.addView(qtyRow)

        // Spacer
        content.addView(android.view.View(ctx).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(16))
        })

        // Total price
        content.addView(totalLabel)

        // Spacer
        content.addView(android.view.View(ctx).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(20))
        })

        // Buy button
        val buyBtn = com.google.android.material.button.MaterialButton(ctx).apply {
            text = getString(R.string.buy_tickets)
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(52))
            setBackgroundColor(androidx.core.content.ContextCompat.getColor(ctx, R.color.accent_primary))
            setOnClickListener {
                sheet.dismiss()
                buyStandingTicket(event, quantity)
            }
        }
        content.addView(buyBtn)

        sheet.setContentView(content)
        sheet.show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    // Maps category/location to a venue type for seat layout
    private fun determineVenueType(event: Event): Venue.VenueType {
        val category = event.category
        val location = event.location
        return when {
            category.contains("Theater", ignoreCase = true) -> Venue.VenueType.THEATER
            category.contains("Children", ignoreCase = true) || category.contains("Kids", ignoreCase = true) -> Venue.VenueType.CHILDREN_THEATER
            category.contains("Sport", ignoreCase = true) -> Venue.VenueType.STADIUM
            category.contains("Stand", ignoreCase = true) || category.contains("Comedy", ignoreCase = true) -> Venue.VenueType.COMEDY_CLUB
            location.contains("Park", ignoreCase = true) -> Venue.VenueType.OPEN_AIR
            else -> Venue.VenueType.CONCERT_HALL
        }
    }

    private fun addToGoogleCalendar() {
        val currentEvent = event ?: run { showErrorSnackbar(getString(R.string.error_adding_to_calendar)); return }
        try {
            val beginTime = parseEventDateTime(currentEvent.date, currentEvent.time)
            val endTime = beginTime + Constants.DEFAULT_EVENT_DURATION_MS

            val eventData = GoogleCalendarManager.CalendarEventData(
                title = currentEvent.title,
                location = currentEvent.location,
                description = currentEvent.description,
                startMillis = beginTime,
                endMillis = endTime
            )

            GoogleCalendarManager.addToGoogleCalendar(this, eventData, calendarSignInLauncher) { success, _ ->
                if (success) showSuccessSnackbar(getString(R.string.event_added_to_calendar))
            }
        } catch (e: Exception) { showErrorSnackbar(getString(R.string.error_adding_to_calendar)) }
    }

    private fun parseEventDateTime(date: String, time: String): Long {
        return try {
            val format = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.ENGLISH)
            format.parse("$date $time")?.time ?: System.currentTimeMillis()
        } catch (e: Exception) { System.currentTimeMillis() }
    }

    private fun shareEvent() {
        val currentEvent = event ?: return
        val shareText = buildString {
            append("${currentEvent.title}\n")
            append("${currentEvent.date} ${currentEvent.time}\n")
            append("${currentEvent.venue}\n")
            if (currentEvent.price > 0) append("${getString(R.string.price)}: ${Constants.CURRENCY_SYMBOL}${currentEvent.price.toInt()}\n")
            append("\n${getString(R.string.share_via)} StageMate")
            append("\nhttps://stagemate.app/event/${currentEvent.id}")
        }
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, currentEvent.title)
        }
        startActivity(Intent.createChooser(sendIntent, getString(R.string.share_via)))
    }

    private fun expandDescription() {
        binding.detailLBLDescription.apply { maxLines = if (maxLines == Int.MAX_VALUE) 4 else Int.MAX_VALUE }
        binding.detailBTNReadMore.text = if (binding.detailLBLDescription.maxLines == Int.MAX_VALUE) getString(R.string.read_less) else getString(R.string.read_more)
    }

    private fun showSoldOutDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.sold_out))
            .setMessage(getString(R.string.sold_out_message))
            .setPositiveButton(getString(R.string.yes)) { dialog, _ -> dialog.dismiss(); loadAlternativeDates() }
            .setNegativeButton(getString(R.string.close), null)
            .show()
    }

    private fun loadAlternativeDates() {
        val currentEvent = event ?: return
        DataRepository.loadAlternativeDates(currentEvent.id) onResult@{ alternativeEvents, error ->
            if (error != null || alternativeEvents.isEmpty()) { showInfoSnackbar(getString(R.string.no_alternative_dates)); return@onResult }
            showAlternativeDatesDialog(alternativeEvents)
        }
    }

    private fun showAlternativeDatesDialog(events: List<Event>) {
        val dates = events.map { "${it.date} - ${it.time}" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.alternative_dates_title))
            .setItems(dates) { _, which ->
                startActivity(newIntent(this, events[which].id))
                finish()
            }
            .setNegativeButton(getString(R.string.close), null)
            .show()
    }

    private fun initializeMap() {
        try { val mapFragment = supportFragmentManager.findFragmentById(R.id.detail_map_fragment) as? SupportMapFragment; mapFragment?.getMapAsync(this) }
        catch (_: Exception) { }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.apply { isZoomControlsEnabled = false; isMapToolbarEnabled = true; isMyLocationButtonEnabled = false; isZoomGesturesEnabled = true; isScrollGesturesEnabled = true }
        event?.let { displayEventLocationOnMap(it) }
    }

    private fun displayEventLocationOnMap(event: Event) {
        val eventLocation = if (event.latitude != 0.0 || event.longitude != 0.0) {
            LatLng(event.latitude, event.longitude)
        } else {
            // Fallback: resolve coordinates from city name
            val city = IsraeliLocations.getCityByName(event.location.split(",").first().trim())
            if (city != null) LatLng(city.latitude, city.longitude) else return
        }
        googleMap?.addMarker(MarkerOptions().position(eventLocation).title(event.title).snippet(event.location).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)))
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(eventLocation, 15f))
    }

    @Suppress("DEPRECATION")
    private inline fun <reified T : java.io.Serializable> Intent.getSerializableExtraCompat(key: String): T? {
        return if (android.os.Build.VERSION.SDK_INT >= 33) {
            getSerializableExtra(key, T::class.java)
        } else {
            getSerializableExtra(key) as? T
        }
    }

    override fun onDestroy() {
        GoogleCalendarManager.clearPendingData()
        googleMap = null
        eventListener?.remove()
        eventListener = null
        super.onDestroy()
    }
}
