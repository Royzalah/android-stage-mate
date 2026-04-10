package com.roei.stagemate.ui.activities

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import java.util.concurrent.atomic.AtomicBoolean
import com.roei.stagemate.utilities.showErrorSnackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.firebase.analytics.FirebaseAnalytics
import com.roei.stagemate.R
import com.roei.stagemate.databinding.ActivityPaymentBinding
import com.roei.stagemate.data.repository.DataRepository
import com.roei.stagemate.data.models.AppNotification
import com.roei.stagemate.data.models.Ticket
import com.roei.stagemate.data.models.TicketType
import com.roei.stagemate.services.EventReminderReceiver
import com.roei.stagemate.MyApp
import com.roei.stagemate.utilities.Constants
import com.roei.stagemate.utilities.DateFormatter
import com.roei.stagemate.ui.fragments.PaymentDetailsFragment
import com.roei.stagemate.utilities.M3LoadingDialog
import com.roei.stagemate.utilities.QRCodeManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// Payment flow: hosts OrderSummary -> PaymentDetails fragments via NavController.
// Creates tickets atomically and schedules a 24h event reminder after purchase.
class PaymentActivity : AppCompatActivity(), PaymentDetailsFragment.PaymentCallback {

    private lateinit var binding: ActivityPaymentBinding
    private lateinit var navController: NavController
    private var analytics: FirebaseAnalytics? = null

    private var totalAmount: String = "0"
    private var quantity: Int = 1
    private var ticketType: TicketType = TicketType.REGULAR

    private var eventId: String? = null
    private var eventTitle: String? = null
    private var eventDate: String? = null
    private var eventTime: String? = null
    private var eventLocation: String? = null
    private var eventVenue: String? = null
    private var eventImageUrl: String? = null

    private var seatNumbers: Array<String>? = null
    private var seatSections: Array<String>? = null
    private var seatRows: Array<String>? = null
    private var seatPrices: DoubleArray? = null

    private val isPaymentProcessing = AtomicBoolean(false)
    private lateinit var loadingDialog: M3LoadingDialog
    private val paymentHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var reservationTimer: CountDownTimer? = null
    private var reservationDeadline: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        analytics = FirebaseAnalytics.getInstance(this)
        loadingDialog = M3LoadingDialog(this)

        loadPaymentDetails()
        setupNavigation()
        startReservationCountdown()
    }

    // --- Reservation Timer (continues from SeatSelection / starts for standing events) ---

    private fun startReservationCountdown() {
        reservationDeadline = intent.getLongExtra(Constants.IntentKeys.RESERVATION_DEADLINE, 0L)
        if (reservationDeadline == 0L) return

        val remaining = reservationDeadline - System.currentTimeMillis()
        if (remaining <= 0) {
            showTimerExpiredDialog()
            return
        }

        binding.paymentLBLTimer.visibility = android.view.View.VISIBLE

        reservationTimer = object : CountDownTimer(remaining, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = millisUntilFinished / 60000
                val seconds = (millisUntilFinished % 60000) / 1000
                binding.paymentLBLTimer.text = getString(R.string.seat_timer_format, minutes, seconds)
                if (millisUntilFinished < 30000) {
                    binding.paymentLBLTimer.setTextColor(getColor(R.color.error))
                } else {
                    binding.paymentLBLTimer.setTextColor(getColor(R.color.accent_secondary))
                }
            }

            override fun onFinish() {
                binding.paymentLBLTimer.visibility = android.view.View.GONE
                showTimerExpiredDialog()
            }
        }.start()
    }

    private fun showTimerExpiredDialog() {
        if (isFinishing) return
        val isStanding = intent.getBooleanExtra(Constants.IntentKeys.IS_STANDING_EVENT, false)

        com.google.android.material.dialog.MaterialAlertDialogBuilder(
            this, R.style.ThemeOverlay_StageMate_Dialog
        )
            .setTitle(getString(R.string.seat_timer_title_expired))
            .setMessage(getString(R.string.seat_timer_message_expired))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.seat_timer_restart)) { dialog, _ ->
                dialog.dismiss()
                if (isStanding) {
                    reservationDeadline = System.currentTimeMillis() + 7 * 60 * 1000L
                    binding.paymentLBLTimer.setTextColor(getColor(R.color.accent_secondary))
                    startReservationCountdown()
                } else {
                    finish()
                }
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
                if (!isStanding) {
                    val eventId = intent.getStringExtra(Constants.IntentKeys.EVENT_ID)
                    val seatNumbers = intent.getStringArrayExtra(Constants.IntentKeys.SEAT_NUMBERS)
                    if (eventId != null && !seatNumbers.isNullOrEmpty()) {
                        DataRepository.releaseSeats(eventId, seatNumbers.toList()) { _, _ -> }
                    }
                }
                val homeIntent = Intent(this, MainActivity::class.java)
                homeIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(homeIntent)
                finish()
            }
            .show()
    }

    // --- Intent Parsing ---

    private fun loadPaymentDetails() {
        totalAmount = intent.getStringExtra(Constants.IntentKeys.TOTAL_AMOUNT) ?: "0"
        quantity = intent.getIntExtra(Constants.IntentKeys.QUANTITY, 1)

        val ticketTypeName = intent.getStringExtra(Constants.IntentKeys.TICKET_TYPE)
        ticketType = try {
            TicketType.valueOf(ticketTypeName ?: "REGULAR")
        } catch (e: Exception) {
            TicketType.REGULAR
        }

        eventId       = intent.getStringExtra(Constants.IntentKeys.EVENT_ID)
        eventTitle    = intent.getStringExtra(Constants.IntentKeys.EVENT_TITLE)
        eventDate     = intent.getStringExtra(Constants.IntentKeys.EVENT_DATE)
        eventTime     = intent.getStringExtra(Constants.IntentKeys.EVENT_TIME)
        eventLocation = intent.getStringExtra(Constants.IntentKeys.EVENT_LOCATION)
        eventVenue = intent.getStringExtra(Constants.IntentKeys.EVENT_VENUE)
        eventImageUrl = intent.getStringExtra(Constants.IntentKeys.EVENT_IMAGE_URL)
        seatNumbers   = intent.getStringArrayExtra(Constants.IntentKeys.SEAT_NUMBERS)
        seatSections  = intent.getStringArrayExtra(Constants.IntentKeys.SEAT_SECTIONS)
        seatRows      = intent.getStringArrayExtra(Constants.IntentKeys.SEAT_ROWS)
        seatPrices    = intent.getDoubleArrayExtra(Constants.IntentKeys.SEAT_PRICES)

        if (eventId.isNullOrBlank() || eventTitle.isNullOrBlank()) {
            MyApp.signalManager.toast(getString(R.string.error_missing_data))
            finish()
            return
        }
    }

    // --- Navigation ---

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.payment_LAYOUT_fragment_container) as? NavHostFragment
        if (navHostFragment == null) {
            showErrorSnackbar(getString(R.string.booking_failed))
            finish()
            return
        }
        navController = navHostFragment.navController

        val args = Bundle().apply {
            putInt(Constants.IntentKeys.QUANTITY, quantity)
            putString(Constants.IntentKeys.TICKET_TYPE, ticketType.name)
            putString(Constants.IntentKeys.EVENT_ID, eventId)
            putString(Constants.IntentKeys.EVENT_DATE, eventDate)
            putString(Constants.IntentKeys.EVENT_TIME, eventTime)
            putString(Constants.IntentKeys.EVENT_LOCATION, eventLocation)
            putString(Constants.IntentKeys.EVENT_IMAGE_URL, eventImageUrl)
            putStringArray(Constants.IntentKeys.SEAT_NUMBERS, seatNumbers)
            putStringArray(Constants.IntentKeys.SEAT_SECTIONS, seatSections)
            putStringArray(Constants.IntentKeys.SEAT_ROWS, seatRows)
            putDoubleArray(Constants.IntentKeys.SEAT_PRICES, seatPrices)
            putString(Constants.IntentKeys.TOTAL_AMOUNT, totalAmount)
            putString(Constants.IntentKeys.EVENT_TITLE, eventTitle)
        }
        navHostFragment.childFragmentManager.addFragmentOnAttachListener { _, fragment ->
            if (fragment is PaymentDetailsFragment) {
                fragment.paymentCallback = this
            }
        }

        val graph = navController.navInflater.inflate(R.navigation.nav_checkout)
        navController.setGraph(graph, args)

        setupNavListener()
    }

    private fun setupNavListener() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.orderSummaryFragment -> {
                    binding.paymentTOOLBARMain.title = getString(R.string.order_summary_title)
                    binding.paymentPBProgress.setProgressCompat(50, true)
                }
                R.id.paymentDetailsFragment -> {
                    binding.paymentTOOLBARMain.title = getString(R.string.payment_details)
                    binding.paymentPBProgress.setProgressCompat(100, true)
                }
            }
        }
    }

    // --- Payment Callback ---

    override fun onPaymentConfirmed() {
        reservationTimer?.cancel()
        binding.paymentLBLTimer.visibility = android.view.View.GONE
        if (!isPaymentProcessing.compareAndSet(false, true)) return
        loadingDialog.show(getString(R.string.processing))
        paymentHandler.postDelayed({
            loadingDialog.updateMessage(getString(R.string.creating_ticket))
        }, 500)
        createSingleTicket()
    }

    // --- Payment Processing ---

    // Uses purchaseTicketAtomic for inventory-safe ticket creation
    private fun createSingleTicket() {
        val user = DataRepository.getCurrentUser()
        val userName = user?.displayName ?: "Guest User"
        val userEmail = user?.email ?: "guest@example.com"

        val currentDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())
        val now = System.currentTimeMillis()
        val bookingReference = "SM${now}_${(1000..9999).random()}"
        val ticketId = java.util.UUID.randomUUID().toString()
        val qrCode = QRCodeManager.generateTicketQRData(
            ticketId = ticketId,
            eventId = eventId ?: "",
            userId = user?.uid ?: "",
            purchaseDate = currentDate,
            bookingReference = bookingReference
        )

        val ticket = buildTicket(ticketId, userName, userEmail, currentDate, bookingReference, qrCode)

        val safeEventId = eventId
        if (safeEventId != null) {
            DataRepository.purchaseTicketAtomic(safeEventId, ticket, quantity) { success, error ->
                lifecycleScope.launch {
                    // Runtime safety fix: guard against activity finishing during async callback
                    if (isFinishing || isDestroyed) return@launch
                    loadingDialog.dismiss()
                    isPaymentProcessing.set(false)
                    notifyPaymentDetailsComplete()
                    if (success) {
                        MyApp.signalManager.vibrate(200)
                        MyApp.signalManager.toast(getString(R.string.purchase_successful_title))
                        createPurchaseNotification(ticket)
                        sendPurchaseConfirmationEmail(ticket)
                        navigateToReceipt(ticket)
                    } else if (error == "Event not found") {
                        // Event from local generator — save ticket without inventory check
                        DataRepository.saveTicket(ticket) { saved, saveErr ->
                            lifecycleScope.launch saveTicketLaunch@{
                                if (isFinishing || isDestroyed) return@saveTicketLaunch
                                if (saved) {
                                    MyApp.signalManager.vibrate(200)
                                    MyApp.signalManager.toast(getString(R.string.purchase_successful_title))
                                    createPurchaseNotification(ticket)
                                    sendPurchaseConfirmationEmail(ticket)
                                    navigateToReceipt(ticket)
                                } else {
                                    showErrorSnackbar(getString(R.string.payment_failed_error, saveErr ?: ""))
                                }
                            }
                        }
                    } else {
                        val msg = when (error) {
                            "Sold Out" -> getString(R.string.not_enough_tickets_available)
                            else -> getString(R.string.payment_failed_error, error ?: "")
                        }
                        showErrorSnackbar(msg)
                    }
                }
            }
        } else {
            DataRepository.saveTicket(ticket) { success, error ->
                lifecycleScope.launch {
                    // Runtime safety fix: guard against activity finishing during async callback
                    if (isFinishing || isDestroyed) return@launch
                    loadingDialog.dismiss()
                    isPaymentProcessing.set(false)
                    notifyPaymentDetailsComplete()
                    if (success) {
                        MyApp.signalManager.vibrate(200)
                        MyApp.signalManager.toast(getString(R.string.purchase_successful_title))
                        createPurchaseNotification(ticket)
                        sendPurchaseConfirmationEmail(ticket)
                        navigateToReceipt(ticket)
                    } else {
                        showErrorSnackbar(getString(R.string.payment_failed_error, error ?: ""))
                    }
                }
            }
        }
    }

    private fun buildTicket(
        ticketId: String,
        userName: String,
        userEmail: String,
        currentDate: String,
        bookingReference: String,
        qrCode: String
    ): Ticket {
        return Ticket(
            id = ticketId,
            eventId = eventId ?: "",
            eventTitle = eventTitle ?: "",
            eventDate = eventDate ?: "",
            eventTime = eventTime ?: "",
            eventLocation = eventLocation ?: "",
            eventImageUrl = eventImageUrl ?: "",
            ticketType = ticketType,
            ticketStatus = Ticket.TicketStatus.UPCOMING,
            price = totalAmount.toDoubleOrNull() ?: 0.0,
            quantity = quantity,
            seatNumbers = seatNumbers?.toList() ?: Ticket.generateSeatNumbers(quantity),
            qrCode = qrCode,
            purchaseDate = currentDate,
            bookingReference = bookingReference,
            userName = userName,
            userEmail = userEmail
        )
    }

    private fun createPurchaseNotification(ticket: Ticket) {
        val purchaseDate = DateFormatter.getCurrentDate()
        val notification = AppNotification.Builder()
            .title(getString(R.string.purchase_successful_title))
            .message(getString(R.string.purchase_successful_message, eventTitle ?: ticket.eventTitle, eventDate ?: ticket.eventDate) + "\nPurchased: $purchaseDate | Ref: ${ticket.bookingReference}")
            .type(AppNotification.NotificationType.SUCCESS)
            .eventId(eventId ?: ticket.eventId)
            .imageUrl(eventImageUrl ?: "")
            .ticketId(ticket.id)
            .build()
        DataRepository.saveNotification(notification) { _, _ -> }

        scheduleEventReminder(ticket)
    }

    private fun notifyPaymentDetailsComplete() {
        val navHost = supportFragmentManager.findFragmentById(R.id.payment_LAYOUT_fragment_container) as? NavHostFragment
        val currentFragment = navHost?.childFragmentManager?.primaryNavigationFragment
        (currentFragment as? PaymentDetailsFragment)?.onPaymentComplete()
    }

    // Fires reminder broadcasts 24h and 3h before the event via AlarmManager
    private fun scheduleEventReminder(ticket: Ticket) {
        val dateStr = eventDate ?: ticket.eventDate
        if (dateStr.isNullOrBlank()) return

        try {
            val formats = listOf(
                SimpleDateFormat("dd MMM yyyy", Locale("en")),
                SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH),
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            )
            var eventDateMillis: Long? = null
            for (fmt in formats) {
                try { eventDateMillis = fmt.parse(dateStr)?.time; if (eventDateMillis != null) break } catch (_: Exception) { /* Expected: format didn't match, try next */ }
            }
            val parsedMillis = eventDateMillis ?: return

            var resolvedMillis = parsedMillis

            // Parse event start time from "HH:mm - HH:mm" format
            val eventTimeStr = eventTime
            val startTime = eventTimeStr?.split("-", "–")?.firstOrNull()?.trim()
            if (!startTime.isNullOrBlank()) {
                try {
                    val timeParts = startTime.split(":")
                    if (timeParts.size == 2) {
                        val cal = java.util.Calendar.getInstance()
                        cal.timeInMillis = resolvedMillis
                        cal.set(java.util.Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                        cal.set(java.util.Calendar.MINUTE, timeParts[1].toInt())
                        resolvedMillis = cal.timeInMillis
                    }
                } catch (_: Exception) { /* Time parsing failed, use date-only */ }
            }

            // 24-hour reminder
            scheduleAlarm(ticket, resolvedMillis - TimeUnit.HOURS.toMillis(24),
                EventReminderReceiver.REMINDER_24H, 0)

            // 3-hour reminder
            scheduleAlarm(ticket, resolvedMillis - TimeUnit.HOURS.toMillis(3),
                EventReminderReceiver.REMINDER_3H, 1)

        } catch (e: Exception) {
            android.util.Log.w("Payment", "Failed to schedule reminders", e)
        }
    }

    private fun scheduleAlarm(
        ticket: Ticket, triggerAtMillis: Long,
        reminderType: String, requestCodeSuffix: Int
    ) {
        if (triggerAtMillis <= System.currentTimeMillis()) return

        val reminderIntent = Intent(this, EventReminderReceiver::class.java).apply {
            putExtra(EventReminderReceiver.KEY_EVENT_ID, eventId ?: ticket.eventId)
            putExtra(EventReminderReceiver.KEY_EVENT_TITLE, eventTitle ?: ticket.eventTitle)
            putExtra(EventReminderReceiver.KEY_EVENT_LOCATION, eventLocation ?: ticket.eventLocation)
            putExtra(EventReminderReceiver.KEY_REMINDER_TYPE, reminderType)
        }

        val requestCode = (eventId ?: ticket.eventId).hashCode() + requestCodeSuffix
        val pendingIntent = PendingIntent.getBroadcast(
            this, requestCode, reminderIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    }

    // --- Email Confirmation (silent, never blocks user flow) ---

    private fun sendPurchaseConfirmationEmail(ticket: Ticket) {
        val recipient = DataRepository.getCurrentUserEmail()
            ?: ticket.userEmail.takeIf { it.isNotBlank() }
            ?: return

        val subject = getString(R.string.email_subject_purchase_confirmation, ticket.eventTitle)
        val htmlBody = buildPurchaseConfirmationHtml(ticket)
        val plainBody = buildPurchaseConfirmationPlainText(ticket)

        DataRepository.sendEmail(
            toEmail = recipient,
            subject = subject,
            htmlBody = htmlBody,
            plainTextBody = plainBody
        ) { success, error ->
            if (!success) {
                android.util.Log.w("PaymentActivity", "Email confirmation failed: $error")
            }
        }
    }

    private fun buildDetailedSeatInfo(): String {
        val isStanding = intent.getBooleanExtra(Constants.IntentKeys.IS_STANDING_EVENT, false)
        if (isStanding || seatNumbers.isNullOrEmpty()) return "General Admission"

        val sections = seatSections
        val rows = seatRows
        val seats = seatNumbers ?: return "General Admission"

        val grouped = mutableMapOf<String, MutableList<String>>()
        for (i in seats.indices) {
            val section = sections?.getOrNull(i) ?: "General"
            val row = rows?.getOrNull(i) ?: ""
            val seat = seats[i]
            val detail = if (row.isNotBlank() && row != "GA") "Row $row, Seat $seat" else seat
            grouped.getOrPut(section) { mutableListOf() }.add(detail)
        }
        return buildString {
            for ((section, seatList) in grouped) {
                append("$section: ${seatList.joinToString(", ")}\n")
            }
        }.trimEnd()
    }

    private fun buildPurchaseConfirmationHtml(ticket: Ticket): String {
        val currency = Constants.CURRENCY_SYMBOL
        val isStanding = intent.getBooleanExtra(Constants.IntentKeys.IS_STANDING_EVENT, false)

        val seatDetailsHtml = if (isStanding || seatSections.isNullOrEmpty()) {
            """<tr><td style="padding:8px 0;color:#b0b0b0;">Section</td><td style="padding:8px 0;color:#fff;text-align:right;">General Admission</td></tr>"""
        } else {
            val grouped = mutableMapOf<String, MutableList<String>>()
            for (i in 0 until (seatNumbers?.size ?: 0)) {
                val section = seatSections?.getOrNull(i) ?: ""
                val row = seatRows?.getOrNull(i) ?: ""
                val seat = seatNumbers?.getOrNull(i) ?: ""
                val detail = if (row.isNotBlank() && row != "GA") "Row $row, Seat $seat" else seat
                grouped.getOrPut(section) { mutableListOf() }.add(detail)
            }
            buildString {
                for ((section, seatList) in grouped) {
                    append("""<tr><td style="padding:8px 0;color:#b0b0b0;">Section</td><td style="padding:8px 0;color:#fff;text-align:right;">$section</td></tr>""")
                    append("""<tr><td style="padding:8px 0;color:#b0b0b0;">Seats</td><td style="padding:8px 0;color:#fff;text-align:right;">${seatList.joinToString(", ")}</td></tr>""")
                }
            }
        }

        return """
        <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;background:#1a1a2e;color:#e0e0e0;border-radius:12px;overflow:hidden;">
            <div style="background:linear-gradient(135deg,#6c63ff,#3f3d9e);padding:24px;text-align:center;">
                <h1 style="margin:0;color:#fff;font-size:22px;">Booking Confirmed</h1>
            </div>
            <div style="padding:24px;">
                <h2 style="color:#fff;margin:0 0 4px;">${ticket.eventTitle}</h2>
                <p style="color:#b0b0b0;margin:0 0 16px;">Ref: ${ticket.bookingReference}</p>
                <table style="width:100%;border-collapse:collapse;">
                    <tr><td style="padding:8px 0;color:#b0b0b0;">Date</td><td style="padding:8px 0;color:#fff;text-align:right;">${ticket.eventDate}</td></tr>
                    <tr><td style="padding:8px 0;color:#b0b0b0;">Venue</td><td style="padding:8px 0;color:#fff;text-align:right;">${ticket.eventLocation}</td></tr>
                    <tr><td style="padding:8px 0;color:#b0b0b0;">Ticket Type</td><td style="padding:8px 0;color:#fff;text-align:right;">${ticketType.displayName}</td></tr>
                    $seatDetailsHtml
                    <tr><td style="padding:8px 0;color:#b0b0b0;">Quantity</td><td style="padding:8px 0;color:#fff;text-align:right;">${ticket.quantity}</td></tr>
                    <tr style="border-top:1px solid #333;"><td style="padding:12px 0;color:#b0b0b0;font-weight:bold;">Total</td><td style="padding:12px 0;color:#6c63ff;text-align:right;font-size:18px;font-weight:bold;">$currency${String.format("%.2f", ticket.price)}</td></tr>
                </table>
            </div>
            <div style="padding:16px 24px;background:#151525;text-align:center;color:#888;font-size:12px;">
                StageMate &mdash; Enjoy the show!
            </div>
        </div>
        """.trimIndent()
    }

    private fun buildPurchaseConfirmationPlainText(ticket: Ticket): String {
        val currency = Constants.CURRENCY_SYMBOL
        val seatInfo = buildDetailedSeatInfo()
        return """
            Booking Confirmed

            ${ticket.eventTitle}
            Booking Reference: ${ticket.bookingReference}

            Date: ${ticket.eventDate}
            Venue: ${ticket.eventLocation}
            Ticket Type: ${ticketType.displayName}
            Location: $seatInfo
            Quantity: ${ticket.quantity}
            Total: $currency${String.format("%.2f", ticket.price)}

            Enjoy the show!
            StageMate
        """.trimIndent()
    }

    private fun navigateToReceipt(ticket: Ticket) {
        val analyticsBundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.TRANSACTION_ID, ticket.bookingReference)
            putDouble(FirebaseAnalytics.Param.VALUE, totalAmount.toDoubleOrNull() ?: 0.0)
            putString(FirebaseAnalytics.Param.CURRENCY, "ILS")
            putString(FirebaseAnalytics.Param.ITEM_ID, ticket.eventId)
            putString(FirebaseAnalytics.Param.ITEM_NAME, ticket.eventTitle)
            putLong(FirebaseAnalytics.Param.QUANTITY, ticket.quantity.toLong())
        }
        analytics?.logEvent(FirebaseAnalytics.Event.PURCHASE, analyticsBundle)

        val intent = Intent(this, ReceiptActivity::class.java)
        intent.putExtra(Constants.IntentKeys.TICKET_ID, ticket.id)
        intent.putExtra(Constants.IntentKeys.BOOKING_REFERENCE, ticket.bookingReference)
        intent.putExtra(Constants.IntentKeys.TOTAL_AMOUNT, totalAmount)
        intent.putExtra(Constants.IntentKeys.QR_CODE, ticket.qrCode)
        intent.putExtra(Constants.IntentKeys.PURCHASE_DATE, ticket.purchaseDate)
        intent.putExtra(Constants.IntentKeys.QUANTITY, ticket.quantity)
        intent.putExtra(Constants.IntentKeys.EVENT_TITLE, eventTitle)
        intent.putExtra(Constants.IntentKeys.EVENT_DATE, eventDate)
        intent.putExtra(Constants.IntentKeys.EVENT_TIME, eventTime)
        intent.putExtra(Constants.IntentKeys.EVENT_VENUE, eventVenue ?: eventLocation)
        intent.putExtra(Constants.IntentKeys.EVENT_LOCATION, eventLocation)
        intent.putExtra(Constants.IntentKeys.EVENT_IMAGE_URL, eventImageUrl)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        reservationTimer?.cancel()
        paymentHandler.removeCallbacksAndMessages(null)
        // Dead code wired: guard dismiss with isShowing check
        if (loadingDialog.isShowing()) loadingDialog.dismiss()
        super.onDestroy()
    }
}
