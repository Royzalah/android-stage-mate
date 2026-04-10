package com.roei.stagemate.utilities

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

// Generates QR codes and barcodes for tickets, and encodes/parses ticket data strings.
// Used by ReceiptActivity and TicketAdapter.
object QRCodeManager {

    fun generateQRCode(
        text: String,
        size: Int = 512,
        darkColor: Int = Color.BLACK,
        lightColor: Int = Color.WHITE
    ): Bitmap? {
        return try {
            val hints = hashMapOf<EncodeHintType, Any>().apply {
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
                put(EncodeHintType.MARGIN, 1)
            }

            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size, hints)

            bitMatrixToBitmap(bitMatrix, size, size, darkColor, lightColor)
        } catch (_: Exception) {
            null
        }
    }


    private fun bitMatrixToBitmap(
        bitMatrix: com.google.zxing.common.BitMatrix,
        width: Int,
        height: Int,
        darkColor: Int = Color.BLACK,
        lightColor: Int = Color.WHITE
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                pixels[y * width + x] = if (bitMatrix[x, y]) darkColor else lightColor
            }
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    // Format: TICKET_ID|EVENT_ID|USER_ID|PURCHASE_DATE|BOOKING_REF
    fun generateTicketQRData(
        ticketId: String,
        eventId: String,
        userId: String,
        purchaseDate: String,
        bookingReference: String
    ): String {
        return "$ticketId|$eventId|$userId|$purchaseDate|$bookingReference"
    }
    
    fun isValidTicketQRData(qrData: String): Boolean {
        return try {
            val parts = qrData.split("|")
            parts.size == 5 && parts.all { it.isNotBlank() }
        } catch (e: Exception) {
            false
        }
    }
}
