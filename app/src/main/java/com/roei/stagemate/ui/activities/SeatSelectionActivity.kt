package com.roei.stagemate.ui.activities

import android.content.Intent
import android.graphics.Color
import androidx.core.content.ContextCompat
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import com.roei.stagemate.utilities.showErrorSnackbar
import com.roei.stagemate.utilities.showSuccessSnackbar
import com.roei.stagemate.utilities.showWarningSnackbar
import com.roei.stagemate.utilities.showInfoSnackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.firestore.ListenerRegistration
import com.roei.stagemate.R
import com.roei.stagemate.ui.adapters.SeatRowAdapter
import com.roei.stagemate.ui.views.venuemap.ArenaMapView
import com.roei.stagemate.ui.views.venuemap.BaseVenueMapView
import com.roei.stagemate.ui.views.venuemap.StadiumMapView
import com.roei.stagemate.ui.views.venuemap.TheaterMapView
import com.roei.stagemate.databinding.ActivitySeatSelectionBinding
import com.roei.stagemate.data.models.Event
import com.roei.stagemate.data.models.IsraeliLocations
import com.roei.stagemate.data.models.PricingTier
import com.roei.stagemate.data.models.PricingTierFactory
import com.roei.stagemate.data.models.Seat
import com.roei.stagemate.data.models.SeatMap
import com.roei.stagemate.data.models.SeatSection
import com.roei.stagemate.data.models.SectionPosition
import com.roei.stagemate.data.models.TicketType
import com.roei.stagemate.data.models.Venue
import com.roei.stagemate.data.models.VenueTemplates
import com.roei.stagemate.utilities.Constants
import com.roei.stagemate.data.repository.DataRepository
import com.roei.stagemate.MyApp
import kotlin.random.Random

// Two-step seat selection: first pick a section on the venue map, then pick seats.
// Reserves seats in Firebase with a 7-minute countdown timer.
class SeatSelectionActivity : AppCompatActivity() {

    companion object {
        private const val MIN_QUANTITY = 1
        private const val MAX_QUANTITY = 10
        private const val RESERVATION_DURATION_MS = 7 * 60 * 1000L
    }

    private lateinit var binding: ActivitySeatSelectionBinding
    private lateinit var seatMap: SeatMap
    private var selectedSeats = mutableListOf<Seat>()

    private var seatsListener: ListenerRegistration? = null
    private var reservationTimer: CountDownTimer? = null
    private var reservationDeadline: Long = 0L

    private var eventId: String = ""
    private var eventTitle: String = ""
    private var eventDate: String = ""
    private var eventTime: String = ""
    private var eventLocation: String = ""
    private var eventImageUrl: String = ""
    private var ticketQuantity: Int = 1
    private var ticketType: TicketType = TicketType.REGULAR
    private var venueType: Venue.VenueType = Venue.VenueType.GENERAL
    private var totalAmount: Double = 0.0
    private var userId: String = ""
    private var venueName: String = ""
    private var eventCategory: String = ""
    private var eventBasePrice: Double = 0.0
    private var eventType: Event.EventType = Event.EventType.SEATED
    private var pricingTiers: List<PricingTier> = emptyList()
    private var selectedTier: PricingTier? = null

    private var currentSection: SeatSection? = null
    private var isShowingSectionMap = true
    private var isStandingPurchase = false
    private var venueMapView: BaseVenueMapView? = null
    private var occupancySimulated = false

    private fun isStandingSection(section: SeatSection): Boolean =
        section.position is SectionPosition.FloorPosition ||
        (Event(eventType = eventType).supportsStanding() && Constants.VenueUtils.isStandingSection(section.name))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySeatSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = DataRepository.getCurrentUserId() ?: ""
        if (userId.isEmpty()) {
            showErrorSnackbar(getString(R.string.login_required_for_seats))
            finish()
            return
        }

        loadIntentData()
        initViews()
        loadSeatMap()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!isShowingSectionMap) {
                    showSectionMap()
                } else {
                    releaseReservationsAndFinish()
                }
            }
        })
    }

    private fun loadIntentData() {
        eventId = intent.getStringExtra(Constants.IntentKeys.EVENT_ID) ?: ""
        eventTitle = intent.getStringExtra(Constants.IntentKeys.EVENT_TITLE) ?: ""
        eventDate = intent.getStringExtra(Constants.IntentKeys.EVENT_DATE) ?: ""
        eventTime = intent.getStringExtra(Constants.IntentKeys.EVENT_TIME) ?: ""
        eventLocation = intent.getStringExtra(Constants.IntentKeys.EVENT_LOCATION) ?: ""
        eventImageUrl = intent.getStringExtra(Constants.IntentKeys.EVENT_IMAGE_URL) ?: ""
        ticketQuantity = intent.getIntExtra(Constants.IntentKeys.QUANTITY, 1)
        totalAmount = intent.getDoubleExtra(Constants.IntentKeys.TOTAL_AMOUNT, 0.0)
        eventBasePrice = intent.getDoubleExtra(Constants.IntentKeys.EVENT_PRICE, 0.0)

        ticketType = TicketType.fromString(intent.getStringExtra(Constants.IntentKeys.TICKET_TYPE) ?: "REGULAR")
        venueName = intent.getStringExtra(Constants.IntentKeys.VENUE_NAME) ?: ""
        eventCategory = intent.getStringExtra(Constants.IntentKeys.EVENT_CATEGORY) ?: ""

        val venueTypeStr = intent.getStringExtra(Constants.IntentKeys.VENUE_TYPE) ?: Venue.VenueType.GENERAL.name
        venueType = try {
            Venue.VenueType.valueOf(venueTypeStr)
        } catch (e: Exception) {
            Venue.VenueType.GENERAL
        }

        val eventTypeStr = intent.getStringExtra(Constants.IntentKeys.EVENT_TYPE) ?: Event.EventType.SEATED.name
        eventType = try {
            Event.EventType.valueOf(eventTypeStr)
        } catch (e: Exception) {
            Event.EventType.SEATED
        }
    }

    private fun initViews() {
        binding.seatLBLEventTitle.text = eventTitle
        binding.seatLBLEventDate.text = getString(R.string.date_time_separator, eventDate, eventTime)
        binding.seatLBLEventLocation.text = eventLocation

        MyApp.imageLoader.loadImage(eventImageUrl, binding.seatIMGEvent)

        binding.seatBTNContinue.isEnabled = false
        binding.seatBTNContinue.setOnClickListener { proceedToPayment() }

        ticketType = TicketType.REGULAR
        updateSelectedSeatsLabel()
        setupTicketTypeSpinner()
    }

    // --- Ticket Type Spinner ---

    private fun setupTicketTypeSpinner() {
        val bp = if (eventBasePrice > 0) eventBasePrice else 150.0
        pricingTiers = when {
            eventCategory.contains("Concert", ignoreCase = true) ||
                    eventCategory.contains("Music", ignoreCase = true) ->
                PricingTierFactory.createConcertPricing(eventId, bp) + PricingTierFactory.createSpecialPricing(eventId, bp)
            eventCategory.contains("Sport", ignoreCase = true) ->
                PricingTierFactory.createSportsPricing(eventId, bp) + PricingTierFactory.createSpecialPricing(eventId, bp)
            eventCategory.contains("Theater", ignoreCase = true) ->
                PricingTierFactory.createTheaterPricing(eventId, bp) + PricingTierFactory.createSpecialPricing(eventId, bp)
            eventCategory.contains("Children", ignoreCase = true) ||
                    eventCategory.contains("Kids", ignoreCase = true) ->
                PricingTierFactory.createChildrenPricing(eventId, bp) + PricingTierFactory.createSpecialPricing(eventId, bp)
            eventCategory.contains("Stand-Up", ignoreCase = true) ->
                PricingTierFactory.createTheaterPricing(eventId, bp) + PricingTierFactory.createSpecialPricing(eventId, bp)
            eventCategory.contains("Festival", ignoreCase = true) ->
                PricingTierFactory.createStandingPricing(eventId, bp) + PricingTierFactory.createSpecialPricing(eventId, bp)
            else -> listOf(
                PricingTier(
                    id = "${eventId}_REGULAR",
                    eventId = eventId,
                    category = eventCategory,
                    tierName = "Regular",
                    basePrice = bp,
                    description = "Standard ticket"
                )
            ) + PricingTierFactory.createSpecialPricing(eventId, bp)
        }

        val tierNames = pricingTiers.map { tier ->
            "${tier.getFullDescription()} — ${Constants.CURRENCY_SYMBOL}${"%.0f".format(tier.getFinalPrice())}"
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, tierNames)
        binding.seatACTTicketType.setAdapter(adapter)

        val regularIndex = pricingTiers.indexOfFirst {
            it.tierName.equals("Regular", ignoreCase = true)
        }
        if (regularIndex >= 0) {
            binding.seatACTTicketType.setText(tierNames[regularIndex], false)
            selectedTier = pricingTiers[regularIndex]
            ticketType = TicketType.fromString(pricingTiers[regularIndex].id.substringAfterLast("_"))
        }

        val childTicketNames = TicketType.getChildTickets().map { it.displayName.lowercase() }
        val specialNeedsNames = TicketType.getSpecialNeeds().map { it.displayName.lowercase() }

        binding.seatACTTicketType.setOnItemClickListener { _, _, position, _ ->
            selectedTier = pricingTiers[position]
            val tier = pricingTiers[position]
            val tierNameLower = tier.tierName.lowercase()

            ticketType = when {
                tier.tierName.contains("VIP", ignoreCase = true) -> TicketType.VIP
                childTicketNames.any { tierNameLower.contains(it) } -> TicketType.CHILD
                specialNeedsNames.any { tierNameLower.contains(it) } -> TicketType.DISABLED
                else -> TicketType.REGULAR
            }

            updateSelectedSeatsLabel()
        }
    }

    // --- Firebase Seat Sync ---

    private fun startListeningToSeatUpdates() {
        seatsListener = DataRepository.listenToSeatUpdates(eventId) { updatedSeats ->
            updateSeatsFromFirebase(updatedSeats)
        }
    }

    // Merges Firebase seat states into local map without overwriting user's current selections
    private fun updateSeatsFromFirebase(firebaseSeats: List<Seat>) {
        firebaseSeats.forEach { fbSeat ->
            seatMap.findSeat(fbSeat.getSeatId())?.let { localSeat ->
                if (localSeat.isReservationExpired()) {
                    localSeat.releaseReservation()
                } else if (localSeat.status != Seat.SeatStatus.SELECTED) {
                    localSeat.status = fbSeat.status
                    localSeat.reservedBy = fbSeat.reservedBy
                    localSeat.reservedAt = fbSeat.reservedAt
                    localSeat.reservationExpiry = fbSeat.reservationExpiry
                }
            }
        }
        seatMap.updateOccupancyStats()

        if (isShowingSectionMap) {
            // Dead code wired: update venue map sections with latest occupancy data
            venueMapView?.updateSections(seatMap.sections)
            venueMapView?.invalidate()
        } else {
            binding.seatRVSeats.adapter?.notifyDataSetChanged()
        }
    }

    private fun startReservationTimer() {
        reservationTimer?.cancel()
        reservationDeadline = System.currentTimeMillis() + RESERVATION_DURATION_MS

        reservationTimer = object : CountDownTimer(RESERVATION_DURATION_MS, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = millisUntilFinished / 60000
                val seconds = (millisUntilFinished % 60000) / 1000
                binding.seatLBLTimer.text = getString(R.string.seat_timer_format, minutes, seconds)
                if (millisUntilFinished < 30000) {
                    binding.seatLBLTimer.setTextColor(getColor(R.color.error))
                }
            }
            override fun onFinish() {
                binding.seatTimerCard.visibility = View.GONE
                binding.seatLBLTimer.visibility = View.GONE
                showTimerExpiredDialog()
            }
        }.start()
        binding.seatLBLTimer.visibility = View.VISIBLE
        binding.seatTimerCard.visibility = View.VISIBLE
        binding.seatLBLTimer.setTextColor(getColor(R.color.warning))
    }

    private fun showTimerExpiredDialog() {
        if (isFinishing) return
        com.google.android.material.dialog.MaterialAlertDialogBuilder(
            this, R.style.ThemeOverlay_StageMate_Dialog
        )
            .setTitle(getString(R.string.seat_timer_title_expired))
            .setMessage(getString(R.string.seat_timer_message_expired))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.seat_timer_restart)) { dialog, _ ->
                dialog.dismiss()
                resetSeatSelectionAndRestart()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
                releaseReservationsAndFinish()
            }
            .show()
    }

    private fun resetSeatSelectionAndRestart() {
        if (selectedSeats.isNotEmpty()) {
            val seatIds = selectedSeats.map { it.getSeatId() }
            DataRepository.releaseSeats(eventId, seatIds) { _, _ -> }
        }
        selectedSeats.forEach { seat ->
            seat.deselect()
            seat.releaseReservation()
        }
        selectedSeats.clear()
        binding.seatLBLTimer.setTextColor(getColor(R.color.warning))
        updateSelectedSeatsLabel()
        venueMapView?.invalidate()
    }

    private fun releaseReservationsAndFinish() {
        if (isFinishing) return
        if (selectedSeats.isNotEmpty()) {
            val seatIds = selectedSeats.map { it.getSeatId() }
            DataRepository.releaseSeats(eventId, seatIds) { _, _ ->
                finish()
            }
        } else {
            finish()
        }
    }

    // --- Seat Map Loading ---

    // Picks venue layout by name first, then falls back to venueType
    private fun loadSeatMap() {
        val basePrice = if (eventBasePrice > 0) eventBasePrice else if (ticketQuantity > 0) totalAmount / ticketQuantity else totalAmount

        seatMap = resolveVenueTemplate(basePrice)

        // Simulate occupancy before Firebase write so occupied state gets persisted
        simulateSeatOccupancy()
        seatMap.updateOccupancyStats()

        val totalSeats = seatMap.allSeats().size
        val availableCount = seatMap.getAvailableSeats().size
        binding.seatLBLAvailableSeats.text = getString(R.string.section_available_format, availableCount, totalSeats)
        binding.seatLBLAvailableSeats.contentDescription = getString(R.string.total_seats_format, totalSeats)

        // Show map from local template data right away (no Firebase needed)
        if (!seatMap.hasAvailableSeats()) {
            showWarningSnackbar(getString(R.string.no_available_seats))
            finish()
            return
        }

        if (ticketType == TicketType.DISABLED) {
            val disabledSeats = seatMap.getAvailableDisabledSeats()
            if (disabledSeats.isEmpty()) {
                showWarningSnackbar(getString(R.string.no_accessible_seats_available))
            }
        }

        populatePricingLegend()
        showSectionMap()

        // Persist seat data to Firebase in background; map works even if this fails
        DataRepository.initializeEventSeats(eventId, seatMap.sections) { success, _ ->
            if (success) {
                startListeningToSeatUpdates()
            } else {
                showErrorSnackbar(getString(R.string.error_loading_seat_map))
            }
        }
    }

    // Matches venue name to a specific template, falls back to venueType generic layout
    private fun resolveVenueTemplate(basePrice: Double): SeatMap {
        val isSports = eventCategory.contains("Sport", ignoreCase = true)
        val name = venueName

        return when {
            // Arena venues
            name.contains("Menora", ignoreCase = true) ->
                if (isSports) VenueTemplates.createMenoraSportsLayout(eventId, basePrice)
                else VenueTemplates.createMenoraConcertLayout(eventId, basePrice)

            name.contains("Ice Space", ignoreCase = true) ->
                VenueTemplates.createIceSpaceLayout(eventId, basePrice)

            name.contains("Tennis Center", ignoreCase = true) ||
                name.contains("Tennis & Education", ignoreCase = true) ->
                VenueTemplates.createTennisCenterLayout(eventId, basePrice)

            // Stadium venues
            name.contains("Bloomfield", ignoreCase = true) ->
                if (isSports) VenueTemplates.createBloomfieldSportsLayout(eventId, basePrice)
                else VenueTemplates.createBloomfieldConcertLayout(eventId, basePrice)

            name.contains("Ramat Gan", ignoreCase = true) ->
                VenueTemplates.createRamatGanLayout(eventId, basePrice)

            // Large amphitheater / outdoor venues
            name.contains("Caesarea", ignoreCase = true) -> VenueTemplates.createCaesareaLayout(eventId, basePrice)
            name.contains("Sultan", ignoreCase = true) -> VenueTemplates.createSultansPoolLayout(eventId, basePrice)
            name.contains("Park HaYarkon", ignoreCase = true) -> VenueTemplates.createParkHaYarkonLayout(eventId, basePrice)
            name.contains("Live Park", ignoreCase = true) -> VenueTemplates.createLiveParkLayout(eventId, basePrice)

            // Large theater / concert hall venues
            name.contains("Habima", ignoreCase = true) -> VenueTemplates.createHabimaLayout(eventId, basePrice)
            name.contains("Cameri", ignoreCase = true) -> VenueTemplates.createCameriLayout(eventId, basePrice)
            name.contains("Bronfman", ignoreCase = true) -> VenueTemplates.createBronfmanLayout(eventId, basePrice)
            name.contains("Convention", ignoreCase = true) || name.contains("Binyanei", ignoreCase = true) ->
                VenueTemplates.createBinyaneiHaumaLayout(eventId, basePrice)
            name.contains("Jerusalem Theatre", ignoreCase = true) -> VenueTemplates.createJerusalemTheatreLayout(eventId, basePrice)
            name.contains("Performing Arts", ignoreCase = true) -> VenueTemplates.createPerformingArtsBeerShevaLayout(eventId, basePrice)
            name.contains("Haifa Auditorium", ignoreCase = true) -> VenueTemplates.createHaifaAuditoriumLayout(eventId, basePrice)
            name.contains("Reading", ignoreCase = true) -> VenueTemplates.createReading3Layout(eventId, basePrice)
            name.contains("First Station", ignoreCase = true) -> VenueTemplates.createFirstStationLayout(eventId, basePrice)
            name.contains("MonArt", ignoreCase = true) -> VenueTemplates.createMonArtLayout(eventId, basePrice)

            // Heichal HaTarbut venues (by city)
            name.contains("Heichal", ignoreCase = true) -> when {
                name.contains("Rishon", ignoreCase = true) -> VenueTemplates.createHeichalRishonLayout(eventId, basePrice)
                name.contains("Petah", ignoreCase = true) -> VenueTemplates.createHeichalPetahTikvaLayout(eventId, basePrice)
                name.contains("Netanya", ignoreCase = true) -> VenueTemplates.createHeichalNetanyaLayout(eventId, basePrice)
                name.contains("Raanana", ignoreCase = true) -> VenueTemplates.createHeichalRaananaLayout(eventId, basePrice)
                name.contains("Rehovot", ignoreCase = true) -> VenueTemplates.createHeichalRehovotLayout(eventId, basePrice)
                else -> VenueTemplates.createHeichalHaTarbutLayout(eventId, basePrice)
            }

            // Medium theater venues
            name.contains("Gesher", ignoreCase = true) -> VenueTemplates.createGesherLayout(eventId, basePrice)
            name.contains("Suzanne", ignoreCase = true) || name.contains("Dellal", ignoreCase = true) ->
                VenueTemplates.createSuzanneDellalLayout(eventId, basePrice)
            name.contains("Haifa Theatre", ignoreCase = true) -> VenueTemplates.createHaifaTheatreLayout(eventId, basePrice)
            name.contains("Herzliya", ignoreCase = true) -> VenueTemplates.createHerzliyaPACLayout(eventId, basePrice)
            name.contains("Beer Sheva", ignoreCase = true) -> VenueTemplates.createBeerShevaLayout(eventId, basePrice)
            name.contains("Holon Theatre", ignoreCase = true) -> VenueTemplates.createHolonTheatreLayout(eventId, basePrice)
            name.contains("Confederation", ignoreCase = true) -> VenueTemplates.createConfederationHouseLayout(eventId, basePrice)

            // Small / club venues
            name.contains("Zappa") && name.contains("Haifa", ignoreCase = true) -> VenueTemplates.createZappaHaifaLayout(eventId, basePrice)
            name.contains("Zappa") && name.contains("Rishon", ignoreCase = true) -> VenueTemplates.createZappaRishonLayout(eventId, basePrice)
            name.contains("Zappa", ignoreCase = true) -> VenueTemplates.createZappaLayout(eventId, basePrice)
            name.contains("Barby", ignoreCase = true) -> VenueTemplates.createBarbyLayout(eventId, basePrice)
            name.contains("Comedy Bar", ignoreCase = true) -> VenueTemplates.createComedyBarLayout(eventId, basePrice)
            name.contains("Tmuna", ignoreCase = true) -> VenueTemplates.createTmunaLayout(eventId, basePrice)
            name.contains("Khan", ignoreCase = true) -> VenueTemplates.createKhanTheatreLayout(eventId, basePrice)
            name.contains("Cinematheque", ignoreCase = true) -> VenueTemplates.createHolonCinemathequeLayout(eventId, basePrice)

            // Fallback by venue type
            else -> resolveByVenueType(basePrice)
        }
    }

    private fun resolveByVenueType(basePrice: Double): SeatMap {
        // Try to resolve a venue name if none was provided
        if (venueName.isBlank()) {
            val matchingVenues = IsraeliLocations.getVenueByType(venueType.name)
            if (matchingVenues.isNotEmpty()) {
                venueName = matchingVenues.firstOrNull()?.name ?: ""
                binding.seatLBLEventLocation.text = venueName
            }
        }
        return when (venueType) {
        Venue.VenueType.THEATER, Venue.VenueType.CHILDREN_THEATER ->
            VenueTemplates.createTheaterLayout(eventId, basePrice)
        Venue.VenueType.CONCERT_HALL -> VenueTemplates.createConcertHallLayout(eventId, basePrice)
        Venue.VenueType.STADIUM, Venue.VenueType.ARENA -> VenueTemplates.createStadiumLayout(eventId, basePrice)
        Venue.VenueType.OPEN_AIR -> VenueTemplates.createCaesareaLayout(eventId, basePrice)
        Venue.VenueType.COMEDY_CLUB -> VenueTemplates.createComedyBarLayout(eventId, basePrice)
        Venue.VenueType.GENERAL -> VenueTemplates.createTheaterLayout(eventId, basePrice)
        }
    }

    // Marks seats as occupied using eventId hash for demo (deterministic per event)
    private fun simulateSeatOccupancy() {
        if (occupancySimulated) return
        occupancySimulated = true

        val occupancyLevel = (eventId.hashCode() and 0x7FFFFFFF) % 100
        val occupancyPercent = when {
            occupancyLevel < 5 -> 0.95
            occupancyLevel < 20 -> 0.75
            occupancyLevel < 45 -> 0.50
            occupancyLevel < 70 -> 0.25
            occupancyLevel < 85 -> 0.10
            else -> 0.0
        }

        if (occupancyPercent > 0) {
            val random = Random(eventId.hashCode().toLong())
            seatMap.sections.forEach { section ->
                val sectionSeats = seatMap.getSeatsBySection(section.name)
                val seatsToOccupy = (sectionSeats.size * occupancyPercent).toInt()
                val availableSeats = sectionSeats.filter { it.status == Seat.SeatStatus.AVAILABLE }
                availableSeats.shuffled(random).take(seatsToOccupy).forEach { seat ->
                    seat.status = Seat.SeatStatus.OCCUPIED
                }
            }
            seatMap.updateOccupancyStats()
        }
    }

    private fun populatePricingLegend() {
        val container = binding.seatPricingLegendContainer
        container.removeAllViews()

        when (seatMap.venueLayoutType) {
            SeatMap.VenueLayoutType.ARENA -> populateArenaPricing(container)
            SeatMap.VenueLayoutType.THEATER,
            SeatMap.VenueLayoutType.CONCERT_STAGE,
            SeatMap.VenueLayoutType.CLUB -> populateTheaterPricing(container)
            SeatMap.VenueLayoutType.STADIUM -> populateStadiumPricing(container)
        }
    }

    private fun populateArenaPricing(container: LinearLayout) {
        val tiers = seatMap.sections.filter { !it.isBlocked }.groupBy { section ->
            (section.position as? SectionPosition.ArenaPosition)?.tierIndex ?: -1
        }.toSortedMap()

        for ((tierIndex, tierSections) in tiers) {
            val tierLabel = when (tierIndex) {
                0 -> "Lower Tier"
                1 -> "VIP Tier"
                2 -> "Upper Tier"
                else -> "Tier $tierIndex"
            }
            val tierColor = tierSections.firstOrNull()?.color ?: Constants.SeatColors.FALLBACK_GRAY

            val sideline = tierSections.filter { s ->
                val num = s.name.replace(Regex("^\\D+"), "").toIntOrNull() ?: 0
                num == 1 || num == 4
            }
            val corners = tierSections.filter { s ->
                val num = s.name.replace(Regex("^\\D+"), "").toIntOrNull() ?: 0
                num in listOf(2, 3, 5, 6)
            }

            addTierHeader(container, tierLabel)

            if (sideline.isNotEmpty()) {
                val prices = sideline.flatMap { s -> s.seats.map { s.getSeatPrice(it.row, it.number) } }
                addPricingRow(container, tierColor, "Sideline (1, 4)", prices)
            }
            if (corners.isNotEmpty()) {
                val prices = corners.flatMap { s -> s.seats.map { s.getSeatPrice(it.row, it.number) } }
                addPricingRow(container, tierColor, "Corners (2, 3, 5, 6)", prices)
            }
        }
    }

    private fun populateTheaterPricing(container: LinearLayout) {
        val tierGroups = seatMap.sections.filter { !it.isBlocked }.groupBy { section ->
            (section.position as? SectionPosition.TheaterPosition)?.tier
                ?: inferTheaterTier(section.name)
        }

        val tierOrder = listOf(
            SectionPosition.TheaterPosition.Tier.FRONT,
            SectionPosition.TheaterPosition.Tier.MIDDLE,
            SectionPosition.TheaterPosition.Tier.BACK,
            SectionPosition.TheaterPosition.Tier.BALCONY
        )

        for (tier in tierOrder) {
            val tierSections = tierGroups[tier] ?: continue
            val tierLabel = when (tier) {
                SectionPosition.TheaterPosition.Tier.FRONT -> "Front Section"
                SectionPosition.TheaterPosition.Tier.MIDDLE -> "Middle Section"
                SectionPosition.TheaterPosition.Tier.BACK -> "Back Section"
                SectionPosition.TheaterPosition.Tier.BALCONY -> "Balcony"
            }

            addTierHeader(container, tierLabel)
            for (section in tierSections) {
                val prices = section.seats.map { section.getSeatPrice(it.row, it.number) }
                addPricingRow(container, section.color, section.name, prices)
            }
        }
    }

    private fun populateStadiumPricing(container: LinearLayout) {
        val sideGroups = seatMap.sections.filter { !it.isBlocked }.groupBy { section ->
            (section.position as? SectionPosition.StadiumPosition)?.side
                ?: inferStadiumSide(section.name)
        }

        val sideOrder = listOf(
            SectionPosition.StadiumPosition.Side.WEST,
            SectionPosition.StadiumPosition.Side.EAST,
            SectionPosition.StadiumPosition.Side.NORTH,
            SectionPosition.StadiumPosition.Side.SOUTH
        )

        for (side in sideOrder) {
            val sideSections = sideGroups[side] ?: continue
            val sideLabel = when (side) {
                SectionPosition.StadiumPosition.Side.WEST -> "West Stand"
                SectionPosition.StadiumPosition.Side.EAST -> "East Stand"
                SectionPosition.StadiumPosition.Side.NORTH -> "North Stand"
                SectionPosition.StadiumPosition.Side.SOUTH -> "South Stand"
                else -> side.name
            }

            addTierHeader(container, sideLabel)
            for (section in sideSections) {
                val prices = section.seats.map { section.getSeatPrice(it.row, it.number) }
                addPricingRow(container, section.color, section.name, prices)
            }
        }

        // Corner sections
        val cornerSides = listOf(
            SectionPosition.StadiumPosition.Side.CORNER_NE,
            SectionPosition.StadiumPosition.Side.CORNER_NW,
            SectionPosition.StadiumPosition.Side.CORNER_SE,
            SectionPosition.StadiumPosition.Side.CORNER_SW
        )
        val cornerSections = cornerSides.flatMap { sideGroups[it] ?: emptyList() }
        if (cornerSections.isNotEmpty()) {
            addTierHeader(container, "Corners")
            for (section in cornerSections) {
                val prices = section.seats.map { section.getSeatPrice(it.row, it.number) }
                addPricingRow(container, section.color, section.name, prices)
            }
        }
    }

    private fun inferTheaterTier(name: String): SectionPosition.TheaterPosition.Tier {
        val lower = name.lowercase()
        return when {
            lower.contains("front") || lower.contains("orchestra") || lower.contains("vip") ->
                SectionPosition.TheaterPosition.Tier.FRONT
            lower.contains("middle") || lower.contains("center") ->
                SectionPosition.TheaterPosition.Tier.MIDDLE
            lower.contains("balcony") || lower.contains("gallery") ->
                SectionPosition.TheaterPosition.Tier.BALCONY
            else -> SectionPosition.TheaterPosition.Tier.BACK
        }
    }

    private fun inferStadiumSide(name: String): SectionPosition.StadiumPosition.Side {
        val lower = name.lowercase()
        return when {
            lower.contains("west") || lower.contains("vip") ->
                SectionPosition.StadiumPosition.Side.WEST
            lower.contains("east") -> SectionPosition.StadiumPosition.Side.EAST
            lower.contains("north") -> SectionPosition.StadiumPosition.Side.NORTH
            lower.contains("south") -> SectionPosition.StadiumPosition.Side.SOUTH
            else -> SectionPosition.StadiumPosition.Side.WEST
        }
    }

    private fun addTierHeader(container: LinearLayout, label: String) {
        val header = TextView(this).apply {
            text = label
            setTextColor(getColor(R.color.text_primary))
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, (12 * resources.displayMetrics.density).toInt(), 0, 4)
        }
        container.addView(header)
    }

    private fun addPricingRow(container: LinearLayout, colorHex: String, groupLabel: String, prices: List<Double>) {
        val minPrice = prices.minOrNull()?.toInt() ?: 0
        val maxPrice = prices.maxOrNull()?.toInt() ?: 0

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 6, 0, 6)
        }

        val dot = View(this).apply {
            val size = (12 * resources.displayMetrics.density).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = (8 * resources.displayMetrics.density).toInt()
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(try { Color.parseColor(colorHex) } catch (_: Exception) { Color.GREEN })
            }
        }

        val priceText = if (minPrice == maxPrice) {
            "${Constants.CURRENCY_SYMBOL}$minPrice"
        } else {
            "${Constants.CURRENCY_SYMBOL}$minPrice – ${Constants.CURRENCY_SYMBOL}$maxPrice"
        }

        val label = TextView(this).apply {
            text = "$groupLabel  $priceText"
            setTextColor(getColor(R.color.text_secondary))
            textSize = 13f
        }

        row.addView(dot)
        row.addView(label)
        container.addView(row)
    }

    // --- Section Map (ViewFlipper Child 0) ---

    private fun showSectionMap() {
        isShowingSectionMap = true
        currentSection = null


        binding.venueMapInclude.venueMapSectionInfo.visibility = View.GONE

        // Create venue map view based on layout type
        val container = binding.venueMapInclude.venueMapContainer
        container.removeAllViews()

        venueMapView = when (seatMap.venueLayoutType) {
            SeatMap.VenueLayoutType.ARENA -> ArenaMapView(this)
            SeatMap.VenueLayoutType.STADIUM -> StadiumMapView(this)
            SeatMap.VenueLayoutType.THEATER,
            SeatMap.VenueLayoutType.CONCERT_STAGE,
            SeatMap.VenueLayoutType.CLUB -> TheaterMapView(this)
        }

        venueMapView?.apply {
            val isConcert = !eventCategory.contains("Sport", ignoreCase = true)
            if (this is ArenaMapView) {
                showStage = isConcert
            }
            if (this is StadiumMapView) {
                showStage = isConcert
            }
            sections = seatMap.sections
            selectedSeats = this@SeatSelectionActivity.selectedSeats
            onSectionClicked = { section -> onMapSectionTapped(section) }
        }

        container.addView(
            venueMapView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )



        binding.venueMapInclude.venueMapSkeleton.visibility = View.GONE

        if (binding.seatViewFlipper.displayedChild != 0) {
            binding.seatViewFlipper.displayedChild = 0
        }
    }

    private fun onMapSectionTapped(section: SeatSection) {
        venueMapView?.selectedSection = section

        val mapBinding = binding.venueMapInclude

        mapBinding.sectionInfoName.text = section.name

        mapBinding.sectionInfoColorDot.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(try { Color.parseColor(section.color) } catch (e: Exception) { Color.GREEN })
        }

        val prices = section.seats.map { section.getSeatPrice(it.row, it.number) }
        val minPrice = prices.minOrNull() ?: 0.0
        val maxPrice = prices.maxOrNull() ?: 0.0
        mapBinding.sectionInfoPrice.text = getString(R.string.section_price_range_format, minPrice, maxPrice)

        val available = section.seats.count { it.status == Seat.SeatStatus.AVAILABLE }
        mapBinding.sectionInfoAvailability.text = getString(R.string.section_available_format, available, section.seats.size)

        val occupancyPercent = if (section.seats.isNotEmpty()) {
            ((section.seats.size - available) * 100) / section.seats.size
        } else 0
        mapBinding.sectionInfoOccupancyBar.progress = occupancyPercent

        if (isStandingSection(section)) {
            mapBinding.sectionInfoSelectBtn.text = getString(R.string.venue_map_standing_section)
        } else {
            mapBinding.sectionInfoSelectBtn.text = getString(R.string.venue_map_select_seats)
        }

        if (available == 0) {
            mapBinding.sectionInfoSelectBtn.isEnabled = false
            mapBinding.sectionInfoSelectBtn.text = getString(R.string.section_sold_out)
        } else {
            mapBinding.sectionInfoSelectBtn.isEnabled = true
            mapBinding.sectionInfoSelectBtn.setOnClickListener { showSectionSeats(section) }
        }

        mapBinding.venueMapSectionInfo.visibility = View.VISIBLE
    }

    // --- Section Seats (ViewFlipper Child 1) ---

    private fun showSectionSeats(section: SeatSection) {
        isShowingSectionMap = false
        currentSection = section

        val sectionName = section.name.lowercase()
        ticketType = when {
            sectionName.contains("vip") || sectionName.contains("golden") || sectionName.contains("courtside") -> TicketType.VIP
            sectionName.contains("accessible") || sectionName.contains("disabled") -> TicketType.DISABLED
            else -> ticketType
        }

        // Standing/GA: auto-assign one ticket per click
        if (isStandingSection(section)) {
            isStandingPurchase = true

            if (selectedSeats.size >= MAX_QUANTITY) {
                showWarningSnackbar(getString(R.string.seat_max_reached))
                return
            }

            val availableSeats = section.seats.filter { it.status == Seat.SeatStatus.AVAILABLE }
            if (availableSeats.isEmpty()) {
                showWarningSnackbar(getString(R.string.not_enough_tickets_available))
                isShowingSectionMap = true
                return
            }

            val seat = availableSeats.first()
            seat.select()
            seat.reserve(userId)
            selectedSeats.add(seat)
            if (selectedSeats.size == 1) startReservationTimer()

            DataRepository.reserveSeats(eventId, listOf(seat), userId) { success, _ ->
                if (isFinishing || isDestroyed) return@reserveSeats
                if (!success) {
                    showErrorSnackbar(getString(R.string.error_saving_seat))
                    seat.deselect()
                    seat.releaseReservation()
                    selectedSeats.remove(seat)
                    if (selectedSeats.isEmpty()) reservationTimer?.cancel()
                }
            }

            val sectionPrice = section.getSeatPrice("A", 1)
            showSuccessSnackbar("${section.name} — ${Constants.CURRENCY_SYMBOL}${"%.0f".format(sectionPrice)}")
            updateSelectedSeatsLabel()
            return
        }

        // Seated section: show seat grid
        isStandingPurchase = false

        binding.seatDetailLBLSectionName.text = section.name
        val sectionColor = try { Color.parseColor(section.color) } catch (e: Exception) { ContextCompat.getColor(this, R.color.success_s400) }
        binding.seatDetailColorDot.background.setTint(sectionColor)

        val isConcertMode = !eventCategory.contains("Sport", ignoreCase = true)
        binding.seatLBLStage.text = when {
            isConcertMode -> getString(R.string.stage_label)
            seatMap.venueLayoutType == SeatMap.VenueLayoutType.STADIUM -> getString(R.string.field_label)
            seatMap.venueLayoutType == SeatMap.VenueLayoutType.ARENA -> getString(R.string.court_label)
            else -> getString(R.string.stage_label)
        }

        val availableInSection = section.seats.count {
            it.status == Seat.SeatStatus.AVAILABLE || it.status == Seat.SeatStatus.SELECTED
        }
        binding.seatLBLAvailableSeats.text = getString(
            R.string.section_available_format, availableInSection, section.seats.size
        )

        val sectionRows = mutableMapOf<String, List<Seat>>()
        section.seats.groupBy { it.row }.forEach { (row, seats) ->
            sectionRows[row] = seats
        }

        binding.seatRVSeats.apply {
            layoutManager = LinearLayoutManager(this@SeatSelectionActivity)
            adapter = SeatRowAdapter(sectionRows) { seat -> handleSeatClick(seat) }
        }

        if (binding.seatViewFlipper.displayedChild != 1) {
            binding.seatViewFlipper.displayedChild = 1
        }
    }

    // --- Seat Click Handling ---

    private fun handleSeatClick(seat: Seat) {
        if (seat.isReservationExpired()) {
            seat.releaseReservation()
            binding.seatRVSeats.adapter?.notifyDataSetChanged()
        }

        when {
            seat.isSelected() -> {
                seat.deselect()
                selectedSeats.removeIf { it.getSeatId() == seat.getSeatId() }
                MyApp.signalManager.vibrate(30)
                DataRepository.releaseSeats(eventId, listOf(seat.getSeatId())) { _, _ -> }
                binding.seatRVSeats.adapter?.notifyDataSetChanged()
            }
            seat.isAvailable() -> {
                if (selectedSeats.size < MAX_QUANTITY) {
                    seat.select()
                    seat.reserve(userId)
                    selectedSeats.add(seat)
                    if (selectedSeats.size == 1) startReservationTimer()
                    MyApp.signalManager.vibrate(50)

                    val section = seatMap.sections.find { it.name == seat.section }
                    val dynamicPrice = section?.getSeatPrice(seat.row, seat.number) ?: seat.price
                    showSuccessSnackbar(getString(R.string.seat_selected_price, seat.getSeatId(), dynamicPrice))

                    DataRepository.reserveSeats(eventId, listOf(seat), userId) { success, _ ->
                        if (isFinishing || isDestroyed) return@reserveSeats
                        if (!success) {
                            showErrorSnackbar(getString(R.string.error_saving_seat))
                            seat.deselect()
                            seat.releaseReservation()
                            selectedSeats.remove(seat)
                            if (selectedSeats.isEmpty()) reservationTimer?.cancel()
                            binding.seatRVSeats.adapter?.notifyDataSetChanged()
                        }
                    }
                } else {
                    showWarningSnackbar(getString(R.string.seat_max_reached))
                }
                binding.seatRVSeats.adapter?.notifyDataSetChanged()
            }
            seat.isReserved() -> {
                if (seat.isReservedBy(userId)) {
                    showInfoSnackbar(getString(R.string.seat_reserved_for_you))
                } else {
                    val remaining = seat.getRemainingReservationTime() / 1000
                    showInfoSnackbar(getString(R.string.seat_reserved_by_other_seconds, remaining))
                }
            }
        }

        updateSelectedSeatsLabel()
    }

    // --- Bottom Bar Updates ---

    private fun updateSelectedSeatsLabel() {
        val count = selectedSeats.size
        binding.seatLBLSelectedSeats.text = getString(R.string.seats_selected_count_format, count)
        binding.seatLBLSelectedSeats.contentDescription = getString(R.string.seats_selected_format, count, MAX_QUANTITY)

        val totalPrice = selectedSeats.sumOf { seat ->
            val section = seatMap.sections.find { it.name == seat.section }
            section?.getSeatPrice(seat.row, seat.number) ?: seat.price
        }
        binding.seatLBLTotalPrice.text = getString(R.string.total_price_format, totalPrice)
        binding.seatLBLTotalPrice.contentDescription = getString(R.string.available_seats_format, count)
        binding.seatLBLTotalPrice.visibility = if (count > 0) View.VISIBLE else View.GONE

        binding.seatBTNContinue.isEnabled = (count >= MIN_QUANTITY)
    }

    // --- Proceed to Payment ---

    // Bundles selected seats and prices, then launches PaymentActivity
    private fun proceedToPayment() {
        if (selectedSeats.isEmpty()) {
            showErrorSnackbar(getString(R.string.please_select_at_least_one_seat))
            return
        }

        ticketQuantity = selectedSeats.size

        reservationTimer?.cancel()
        seatsListener?.remove()

        val seatNumbers: Array<String>
        val seatSections: Array<String>
        val seatRows: Array<String>
        val seatPrices: DoubleArray
        val totalPrice: Double

        val standingSection = currentSection
        if (isStandingPurchase && standingSection != null) {
            val sectionPrice = standingSection.getSeatPrice("A", 1)
            seatNumbers = Array(ticketQuantity) { "${standingSection.name} #${it + 1}" }
            seatSections = Array(ticketQuantity) { standingSection.name }
            seatRows = Array(ticketQuantity) { "GA" }
            seatPrices = DoubleArray(ticketQuantity) { sectionPrice }
            totalPrice = sectionPrice * ticketQuantity
        } else {
            seatNumbers = selectedSeats.map { it.getSeatId() }.toTypedArray()
            seatSections = selectedSeats.map { it.section }.toTypedArray()
            seatRows = selectedSeats.map { it.row }.toTypedArray()
            seatPrices = selectedSeats.map { seat ->
                val section = seatMap.sections.find { it.name == seat.section }
                section?.getSeatPrice(seat.row, seat.number) ?: seat.price
            }.toDoubleArray()
            totalPrice = seatPrices.sum()
        }

        val intent = Intent(this, PaymentActivity::class.java).apply {
            putExtra(Constants.IntentKeys.EVENT_ID, eventId)
            putExtra(Constants.IntentKeys.EVENT_TITLE, eventTitle)
            putExtra(Constants.IntentKeys.EVENT_DATE, eventDate)
            putExtra(Constants.IntentKeys.EVENT_TIME, eventTime)
            putExtra(Constants.IntentKeys.EVENT_LOCATION, eventLocation)
            putExtra(Constants.IntentKeys.EVENT_VENUE, venueName)
            putExtra(Constants.IntentKeys.EVENT_IMAGE_URL, eventImageUrl)
            putExtra(Constants.IntentKeys.TOTAL_AMOUNT, totalPrice.toString())
            putExtra(Constants.IntentKeys.QUANTITY, ticketQuantity)
            putExtra(Constants.IntentKeys.TICKET_TYPE, ticketType.name)
            putExtra(Constants.IntentKeys.TICKET_TYPE_ENUM, ticketType.name)
            putExtra(Constants.IntentKeys.SEAT_NUMBERS, seatNumbers)
            putExtra(Constants.IntentKeys.SEAT_SECTIONS, seatSections)
            putExtra(Constants.IntentKeys.SEAT_ROWS, seatRows)
            putExtra(Constants.IntentKeys.SEAT_PRICES, seatPrices)
            putExtra(Constants.IntentKeys.IS_STANDING_EVENT, isStandingPurchase)
            putExtra(Constants.IntentKeys.RESERVATION_DEADLINE, reservationDeadline)
        }

        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        reservationTimer?.cancel()
        binding.seatTimerCard.visibility = View.GONE
        seatsListener?.remove()
        venueMapView = null
        super.onDestroy()
    }
}
