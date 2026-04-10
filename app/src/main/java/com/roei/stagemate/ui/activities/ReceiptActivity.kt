package com.roei.stagemate.ui.activities

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import com.roei.stagemate.utilities.showErrorSnackbar
import com.roei.stagemate.utilities.showInfoSnackbar
import com.roei.stagemate.utilities.showSuccessSnackbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.appcompat.app.AppCompatActivity
import com.roei.stagemate.R
import com.roei.stagemate.databinding.ActivityReceiptBinding
import com.roei.stagemate.MyApp
import com.roei.stagemate.utilities.GoogleCalendarManager
import com.roei.stagemate.utilities.QRCodeManager
import java.io.File
import java.io.FileOutputStream
import com.roei.stagemate.utilities.Constants
import com.roei.stagemate.data.repository.DataRepository
import java.text.SimpleDateFormat
import java.util.Locale

// Receipt screen shown after successful ticket purchase.
// Shows ticket details, QR code, barcode, and options to share or add to calendar.
class ReceiptActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReceiptBinding
    private var qrCodeBitmap: Bitmap? = null

    private val calendarSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        GoogleCalendarManager.handleSignInResult(this) { success, _ ->
            if (success) showSuccessSnackbar(getString(R.string.event_added_to_calendar))
            else showInfoSnackbar(getString(R.string.calendar_fallback_used))
        }
    }

    private var ticketId: String = ""
    private var bookingReference: String = ""
    private var totalAmount: String = "0"
    private var qrCodeData: String = ""
    private var purchaseDate: String = ""
    private var quantity: Int = 1

    private var eventTitle: String = ""
    private var eventDate: String = ""
    private var eventTime: String = ""
    private var eventVenue: String = ""
    private var eventLocation: String = ""
    private var eventDescription: String = ""
    private var eventImageUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReceiptBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadReceiptDetails()
        initViews()
        generateQRCode()
        showAddToCalendarDialog()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateHome()
            }
        })
    }

    private fun loadReceiptDetails() {
        ticketId = intent.getStringExtra(Constants.IntentKeys.TICKET_ID) ?: ""
        bookingReference = intent.getStringExtra(Constants.IntentKeys.BOOKING_REFERENCE) ?: "SM000000"
        totalAmount = intent.getStringExtra(Constants.IntentKeys.TOTAL_AMOUNT) ?: "0"
        qrCodeData = intent.getStringExtra(Constants.IntentKeys.QR_CODE) ?: bookingReference
        purchaseDate = intent.getStringExtra(Constants.IntentKeys.PURCHASE_DATE) ?: ""
        quantity = intent.getIntExtra(Constants.IntentKeys.QUANTITY, 1)

        eventTitle = intent.getStringExtra(Constants.IntentKeys.EVENT_TITLE) ?: ""
        eventDate = intent.getStringExtra(Constants.IntentKeys.EVENT_DATE) ?: ""
        eventTime = intent.getStringExtra(Constants.IntentKeys.EVENT_TIME) ?: ""
        eventVenue = intent.getStringExtra(Constants.IntentKeys.EVENT_VENUE) ?: ""
        eventLocation = intent.getStringExtra(Constants.IntentKeys.EVENT_LOCATION) ?: ""
        eventDescription = intent.getStringExtra(Constants.IntentKeys.EVENT_DESCRIPTION) ?: ""
        eventImageUrl = intent.getStringExtra(Constants.IntentKeys.EVENT_IMAGE_URL) ?: ""
    }

    private fun initViews() {
        binding.receiptLBLAmount.text = "${Constants.CURRENCY_SYMBOL}$totalAmount"

        binding.receiptLBLEventTitle.text = eventTitle
        binding.receiptLBLEventDate.text = eventDate
        binding.receiptLBLEventTime.text = eventTime
        binding.receiptLBLEventVenue.text = eventLocation
        binding.receiptLBLQuantity.text = quantity.toString()

        if (eventImageUrl.isNotBlank()) {
            MyApp.imageLoader.loadImage(eventImageUrl, binding.receiptIMGEvent)
        }

        binding.receiptLBLReference.text = "${getString(R.string.ref_label)} $bookingReference"
        binding.receiptLBLDate.text = "${getString(R.string.purchase_date)}: $purchaseDate"

        binding.receiptBTNDownload.setOnClickListener { saveReceipt() }
        binding.receiptBTNViewTickets.setOnClickListener { navigateToTickets() }
        binding.receiptBTNDone.setOnClickListener { navigateHome() }
        binding.receiptBTNAddToCalendar.setOnClickListener { addToGoogleCalendar() }
    }



    private fun showAddToCalendarDialog() {
        if (eventTitle.isEmpty()) return

        MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_StageMate_Dialog)
            .setTitle(getString(R.string.add_to_calendar_title))
            .setMessage(getString(R.string.add_to_calendar_message, eventTitle))
            .setIcon(R.drawable.ic_calendar)
            .setPositiveButton(getString(R.string.yes)) { _, _ -> addToGoogleCalendar() }
            .setNegativeButton(getString(R.string.no), null)
            .show()
    }

    private fun addToGoogleCalendar() {
        if (eventTitle.isEmpty()) {
            showErrorSnackbar(getString(R.string.error_adding_to_calendar))
            return
        }

        try {
            val beginTime = parseEventDateTime(eventDate, eventTime)
            val endTime = beginTime + Constants.DEFAULT_EVENT_DURATION_MS

            val calendarDescription = buildString {
                append("$eventDescription\n\n")
                append("${getString(R.string.booking_reference)}: $bookingReference\n")
                append("${getString(R.string.number_of_tickets_label)}: $quantity\n")
                append("${getString(R.string.booked_via)} StageMate")
            }

            val eventData = GoogleCalendarManager.CalendarEventData(
                title = eventTitle,
                location = eventLocation,
                description = calendarDescription,
                startMillis = beginTime,
                endMillis = endTime
            )

            GoogleCalendarManager.addToGoogleCalendar(this, eventData, calendarSignInLauncher) { success, _ ->
                if (success) showSuccessSnackbar(getString(R.string.event_added_to_calendar))
            }
        } catch (e: Exception) {
            showErrorSnackbar(getString(R.string.error_adding_to_calendar))
        }
    }

    private fun parseEventDateTime(date: String, time: String): Long {
        return try {
            val format = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.ENGLISH)
            format.parse("$date $time")?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    private fun generateQRCode() {
        try {
            qrCodeBitmap = QRCodeManager.generateQRCode(qrCodeData, 512)
            binding.receiptIMGQRCode.setImageBitmap(qrCodeBitmap)
        } catch (e: Exception) {
            showErrorSnackbar(getString(R.string.failed_to_generate_qr))
        }
    }

    private fun saveReceipt() {
        val bitmap = qrCodeBitmap ?: run {
            showErrorSnackbar(getString(R.string.qr_not_available))
            return
        }

        try {
            val filename = "StageMate_Receipt_$bookingReference.png"
            val dir = getExternalFilesDir(null) ?: run {
                showErrorSnackbar(getString(R.string.receipt_download_failed))
                return
            }
            val file = File(dir, filename)

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            showSuccessSnackbar(getString(R.string.receipt_saved))

        } catch (e: Exception) {
            showErrorSnackbar(getString(R.string.receipt_download_failed))
        }
    }

    private fun navigateHome() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun navigateToTickets() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        intent.putExtra(Constants.IntentKeys.OPEN_TAB, "tickets")
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        qrCodeBitmap?.recycle()
        qrCodeBitmap = null
        super.onDestroy()
    }
}
