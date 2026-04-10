package com.roei.stagemate.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.roei.stagemate.R
import com.roei.stagemate.databinding.FragmentOrderSummaryBinding
import com.roei.stagemate.utilities.Constants
import com.roei.stagemate.MyApp
import com.roei.stagemate.data.repository.DataRepository
import com.google.android.material.dialog.MaterialAlertDialogBuilder

// Shows order summary (event, seats, total) before proceeding to payment.
// Used by PaymentActivity via Navigation. Navigates to PaymentDetailsFragment on confirm.
class OrderSummaryFragment : Fragment() {

    private var _binding: FragmentOrderSummaryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        populateUI()
        binding.orderBTNNext.setOnClickListener {
            findNavController().navigate(
                R.id.action_orderSummaryFragment_to_paymentDetailsFragment,
                arguments ?: Bundle()   // null-safe: won't throw if no args
            )
        }
        binding.orderBTNCancel.setOnClickListener {
            showCancelConfirmation()
        }
    }

    private fun showCancelConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.cancel_selection_confirm_title))
            .setMessage(getString(R.string.cancel_selection_confirm_message))
            .setPositiveButton(getString(R.string.confirm)) { _, _ ->
                val eventId = arguments?.getString(Constants.IntentKeys.EVENT_ID)
                val seatNumbers = arguments?.getStringArray(Constants.IntentKeys.SEAT_NUMBERS)
                if (!eventId.isNullOrBlank() && !seatNumbers.isNullOrEmpty()) {
                    DataRepository.releaseSeats(eventId, seatNumbers.toList()) { _, _ -> }
                }
                requireActivity().finish()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun populateUI() {
        val args = arguments ?: Bundle()

        val imageUrl = args.getString(Constants.IntentKeys.EVENT_IMAGE_URL)
        if (!imageUrl.isNullOrBlank()) {
            MyApp.imageLoader.loadImage(imageUrl, binding.orderIMGEvent)
        }

        binding.orderLBLEventTitle.text = args.getString(Constants.IntentKeys.EVENT_TITLE) ?: ""

        val date = args.getString(Constants.IntentKeys.EVENT_DATE) ?: ""
        val time = args.getString(Constants.IntentKeys.EVENT_TIME)
        binding.orderLBLEventDate.text = if (!time.isNullOrBlank()) "$date · $time" else date

        binding.orderLBLEventLocation.text = args.getString(Constants.IntentKeys.EVENT_LOCATION) ?: ""

        binding.orderLBLTicketType.text     = args.getString(Constants.IntentKeys.TICKET_TYPE) ?: ""
        binding.orderLBLTicketQuantity.text = args.getInt(Constants.IntentKeys.QUANTITY, 1).toString()

        val seats = args.getStringArray(Constants.IntentKeys.SEAT_NUMBERS)
        val seatSections = args.getStringArray(Constants.IntentKeys.SEAT_SECTIONS)
        val seatRows = args.getStringArray(Constants.IntentKeys.SEAT_ROWS)
        val seatPrices = args.getDoubleArray(Constants.IntentKeys.SEAT_PRICES)

        if (!seats.isNullOrEmpty()) {
            val seatDetails = buildSeatDetailsText(seats, seatSections, seatRows, seatPrices)
            binding.orderLBLTicketSeats.text = seatDetails
            binding.orderLBLTicketSeats.visibility = View.VISIBLE
        } else {
            binding.orderLBLTicketSeats.visibility = View.GONE
        }

        binding.orderLBLTotalAmount.text = "${Constants.CURRENCY_SYMBOL}${args.getString(Constants.IntentKeys.TOTAL_AMOUNT) ?: "0"}"
    }

    private fun buildSeatDetailsText(
        seats: Array<String>,
        seatSections: Array<String>?,
        seatRows: Array<String>?,
        seatPrices: DoubleArray?
    ): String {
        if (seatSections.isNullOrEmpty()) {
            return seats.joinToString(", ")
        }

        val grouped = seats.indices.groupBy { seatSections.getOrNull(it) ?: "" }
        return buildString {
            for ((section, indices) in grouped) {
                if (section.isNotEmpty()) {
                    append("$section:\n")
                }
                for (i in indices) {
                    val row = seatRows?.getOrNull(i) ?: ""
                    val seatId = seats[i]
                    val price = seatPrices?.getOrNull(i)
                    append("  ${getString(R.string.row_label)} $row, ${getString(R.string.seat_label)} $seatId")
                    if (price != null) {
                        append("  —  ${Constants.CURRENCY_SYMBOL}${price.toInt()}")
                    }
                    append("\n")
                }
            }
        }.trimEnd()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
