package com.roei.stagemate.ui.activities

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.roei.stagemate.R
import com.roei.stagemate.MyApp
import com.roei.stagemate.databinding.ActivityTicketDetailBinding
import com.roei.stagemate.data.models.Ticket
import com.roei.stagemate.data.models.TicketType
import com.roei.stagemate.utilities.Constants
import com.roei.stagemate.utilities.QRCodeManager
import java.io.File
import java.io.FileOutputStream

class TicketDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTicketDetailBinding

    private var ticketId = ""
    private var eventId = ""
    private var eventTitle = ""
    private var eventDate = ""
    private var eventTime = ""
    private var eventLocation = ""
    private var eventImageUrl = ""
    private var ticketType = TicketType.REGULAR
    private var price = 0.0
    private var quantity = 1
    private var seatNumbers = ""
    private var bookingReference = ""
    private var qrCode = ""
    private var purchaseDate = ""
    private var isPast = false

    companion object {
        private const val EXTRA_TICKET_ID = "TICKET_ID"
        private const val EXTRA_EVENT_ID = "EVENT_ID"
        private const val EXTRA_EVENT_TITLE = "EVENT_TITLE"
        private const val EXTRA_EVENT_DATE = "EVENT_DATE"
        private const val EXTRA_EVENT_TIME = "EVENT_TIME"
        private const val EXTRA_EVENT_LOCATION = "EVENT_LOCATION"
        private const val EXTRA_EVENT_IMAGE_URL = "EVENT_IMAGE_URL"
        private const val EXTRA_TICKET_TYPE = "TICKET_TYPE"
        private const val EXTRA_PRICE = "PRICE"
        private const val EXTRA_QUANTITY = "QUANTITY"
        private const val EXTRA_SEAT_NUMBERS = "SEAT_NUMBERS"
        private const val EXTRA_BOOKING_REFERENCE = "BOOKING_REFERENCE"
        private const val EXTRA_QR_CODE = "QR_CODE"
        private const val EXTRA_PURCHASE_DATE = "PURCHASE_DATE"
        private const val EXTRA_IS_PAST = "IS_PAST"

        fun newIntent(context: Context, ticket: Ticket): Intent {
            return Intent(context, TicketDetailActivity::class.java).apply {
                putExtra(EXTRA_TICKET_ID, ticket.id)
                putExtra(EXTRA_EVENT_ID, ticket.eventId)
                putExtra(EXTRA_EVENT_TITLE, ticket.eventTitle)
                putExtra(EXTRA_EVENT_DATE, ticket.eventDate)
                putExtra(EXTRA_EVENT_TIME, ticket.eventTime)
                putExtra(EXTRA_EVENT_LOCATION, ticket.eventLocation)
                putExtra(EXTRA_EVENT_IMAGE_URL, ticket.eventImageUrl)
                putExtra(EXTRA_TICKET_TYPE, ticket.ticketType.name)
                putExtra(EXTRA_PRICE, ticket.price)
                putExtra(EXTRA_QUANTITY, ticket.quantity)
                putExtra(EXTRA_SEAT_NUMBERS, ticket.seatNumbers.joinToString(", "))
                putExtra(EXTRA_BOOKING_REFERENCE, ticket.bookingReference)
                putExtra(EXTRA_QR_CODE, ticket.qrCode)
                putExtra(EXTRA_PURCHASE_DATE, ticket.purchaseDate)
                putExtra(EXTRA_IS_PAST, ticket.isPast())
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTicketDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        extractIntentData()
        populateUI()
        setupListeners()
    }

    private fun extractIntentData() {
        ticketId = intent.getStringExtra(EXTRA_TICKET_ID) ?: ""
        eventId = intent.getStringExtra(EXTRA_EVENT_ID) ?: ""
        eventTitle = intent.getStringExtra(EXTRA_EVENT_TITLE) ?: ""
        eventDate = intent.getStringExtra(EXTRA_EVENT_DATE) ?: ""
        eventTime = intent.getStringExtra(EXTRA_EVENT_TIME) ?: ""
        eventLocation = intent.getStringExtra(EXTRA_EVENT_LOCATION) ?: ""
        eventImageUrl = intent.getStringExtra(EXTRA_EVENT_IMAGE_URL) ?: ""
        ticketType = try {
            TicketType.valueOf(intent.getStringExtra(EXTRA_TICKET_TYPE) ?: "REGULAR")
        } catch (_: Exception) { TicketType.REGULAR }
        price = intent.getDoubleExtra(EXTRA_PRICE, 0.0)
        quantity = intent.getIntExtra(EXTRA_QUANTITY, 1)
        seatNumbers = intent.getStringExtra(EXTRA_SEAT_NUMBERS) ?: ""
        bookingReference = intent.getStringExtra(EXTRA_BOOKING_REFERENCE) ?: ""
        qrCode = intent.getStringExtra(EXTRA_QR_CODE) ?: ""
        purchaseDate = intent.getStringExtra(EXTRA_PURCHASE_DATE) ?: ""
        isPast = intent.getBooleanExtra(EXTRA_IS_PAST, false)
    }

    private fun populateUI() {
        binding.ticketDetailLBLTitle.text = eventTitle
        binding.ticketDetailLBLDate.text = eventDate
        binding.ticketDetailLBLTime.text = if (eventTime.isNotBlank()) eventTime else getString(R.string.time_tba)
        binding.ticketDetailLBLLocation.text = eventLocation
        binding.ticketDetailLBLPrice.text = "${Constants.CURRENCY_SYMBOL}${"%.0f".format(price)}"
        binding.ticketDetailLBLQuantity.text = "x$quantity"
        binding.ticketDetailLBLReference.text = bookingReference
        binding.ticketDetailLBLType.text = ticketType.englishName()

        // Seat details grid (Entry, Section, Row, Seat)
        populateSeatDetails()

        if (purchaseDate.isNotBlank()) {
            binding.ticketDetailLBLPurchaseDate.text = getString(R.string.purchased_on, purchaseDate)
        } else {
            binding.ticketDetailLBLPurchaseDate.visibility = View.GONE
        }

        // Status badge
        if (isPast) {
            binding.ticketDetailLBLStatus.text = getString(R.string.status_past)
            binding.ticketDetailLBLStatus.setBackgroundResource(R.drawable.bg_ticket_status_past)
            binding.ticketDetailLBLStatus.setTextColor(ContextCompat.getColor(this, R.color.danger_d500))
        } else {
            binding.ticketDetailLBLStatus.text = getString(R.string.status_upcoming)
            binding.ticketDetailLBLStatus.setBackgroundResource(R.drawable.bg_ticket_status_upcoming)
            binding.ticketDetailLBLStatus.setTextColor(ContextCompat.getColor(this, R.color.success_s400))
        }

        // Event image
        binding.ticketDetailIMGPoster.setImageResource(R.drawable.img_placeholder)
        MyApp.imageLoader.loadImage(eventImageUrl, binding.ticketDetailIMGPoster)

        // QR Code
        if (qrCode.isNotBlank()) {
            val qrBitmap = QRCodeManager.generateQRCode(qrCode, 512)
            binding.ticketDetailIMGQrCode.setImageBitmap(qrBitmap)
        }

        // Hide download for past events
        if (isPast) {
            binding.ticketDetailBTNDownload.visibility = View.GONE
        }
    }

    private fun populateSeatDetails() {
        if (seatNumbers.isBlank()) {
            binding.ticketDetailLBLEntry.text = "-"
            binding.ticketDetailLBLSection.text = "-"
            binding.ticketDetailLBLRow.text = "-"
            binding.ticketDetailLBLSeat.text = getString(R.string.general_admission)
            return
        }

        // Parse seat IDs like "D4", "A12" → row = letter, seat = number
        val seats = seatNumbers.split(",").map { it.trim() }.filter { it.isNotBlank() }
        if (seats.isEmpty()) {
            binding.ticketDetailLBLEntry.text = "-"
            binding.ticketDetailLBLSection.text = "-"
            binding.ticketDetailLBLRow.text = "-"
            binding.ticketDetailLBLSeat.text = "-"
            return
        }

        val firstSeat = seats.first()
        val rowPart = firstSeat.takeWhile { it.isLetter() }
        val seatPart = firstSeat.dropWhile { it.isLetter() }

        binding.ticketDetailLBLEntry.text = "1"
        binding.ticketDetailLBLSection.text = if (rowPart.isNotBlank()) rowPart else "-"
        binding.ticketDetailLBLRow.text = if (rowPart.isNotBlank()) rowPart else "-"

        if (seats.size == 1) {
            binding.ticketDetailLBLSeat.text = if (seatPart.isNotBlank()) seatPart else "-"
        } else {
            val seatNums = seats.mapNotNull { s -> s.dropWhile { it.isLetter() }.toIntOrNull() }
            binding.ticketDetailLBLSeat.text = if (seatNums.isNotEmpty()) seatNums.joinToString(", ") else seats.joinToString(", ")
        }
    }

    private fun setupListeners() {
        binding.ticketDetailBTNDownload.setOnClickListener { downloadTicket() }

        binding.ticketDetailBTNViewEvent.setOnClickListener {
            if (eventId.isNotBlank()) {
                val intent = Intent(this, EventDetailActivity::class.java)
                intent.putExtra(Constants.IntentKeys.EVENT_ID, eventId)
                startActivity(intent)
            }
        }
    }

    private fun downloadTicket() {
        if (!QRCodeManager.isValidTicketQRData(qrCode)) {
            MyApp.signalManager.toast(getString(R.string.invalid_qr_data))
            return
        }
        val qrBitmap = QRCodeManager.generateQRCode(qrCode, 1024) ?: run {
            MyApp.signalManager.toast(getString(R.string.failed_to_download_ticket))
            return
        }
        try {
            val filename = "StageMate_Ticket_${bookingReference}.png"
            val file = File(getExternalFilesDir(null), filename)
            FileOutputStream(file).use { out ->
                qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            MyApp.signalManager.vibrate(100)
            MyApp.signalManager.toast(getString(R.string.ticket_downloaded_toast))
        } catch (_: Exception) {
            MyApp.signalManager.toast(getString(R.string.failed_to_download_ticket))
        }
    }
}
