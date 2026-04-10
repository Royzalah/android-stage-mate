package com.roei.stagemate.ui.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.roei.stagemate.R
import com.roei.stagemate.databinding.FragmentPaymentDetailsBinding

// Credit card input form with live card preview and field validation.
// Used by PaymentActivity via Navigation. Notifies PaymentActivity via PaymentCallback on confirm.
class PaymentDetailsFragment : Fragment() {

    interface PaymentCallback {
        fun onPaymentConfirmed()
    }

    companion object {
        private const val CARD_NUMBER_LENGTH = 16
        private const val ID_NUMBER_LENGTH = 9
        private const val CVV_LENGTH = 3
    }

    private var _binding: FragmentPaymentDetailsBinding? = null
    private val binding get() = _binding!!
    var paymentCallback: PaymentCallback? = null
    private var expandRunnable: Runnable? = null
    private var reExpandRunnable: Runnable? = null
    private var cardNumberPreviewWatcher: TextWatcher? = null
    private var cardholderPreviewWatcher: TextWatcher? = null
    private var expiryPreviewWatcher: TextWatcher? = null
    private var cardNumberFormatterWatcher: TextWatcher? = null
    private var expiryFormatterWatcher: TextWatcher? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPaymentDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCardNumberFormatter()
        setupExpiryFormatter()
        setupLiveCardPreview()
        binding.paymentBTNPay.onExpandedClickListener = {
            if (validateForm()) {
                binding.paymentBTNPay.startLoop()
                paymentCallback?.onPaymentConfirmed()
            } else {
                binding.paymentBTNPay.collapse()
                reExpandRunnable = Runnable { _binding?.paymentBTNPay?.expand() }
                view.postDelayed(reExpandRunnable!!, 1500)
            }
        }

        // Auto-expand payment button after short delay so label is visible.
        // Stored as field and removed in onDestroyView to prevent memory leak.
        expandRunnable = Runnable { _binding?.paymentBTNPay?.expand() }
        view.postDelayed(expandRunnable!!, 500)
    }

    private fun setupLiveCardPreview() {
        cardNumberPreviewWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s.toString().trim()
                if (text.isEmpty()) {
                    _binding?.paymentLBLCardNumberDisplay?.text = getString(R.string.card_number_placeholder)
                } else {
                    val digits = text.replace(" ", "")
                    val padded = digits.padEnd(CARD_NUMBER_LENGTH, '•')
                    val formatted = padded.chunked(4).joinToString(" ")
                    _binding?.paymentLBLCardNumberDisplay?.text = formatted
                }
            }
        }
        binding.paymentEDTCardNumber.addTextChangedListener(cardNumberPreviewWatcher)

        cardholderPreviewWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val name = s.toString().trim()
                if (name.isEmpty()) {
                    _binding?.paymentLBLCardHolderName?.text = getString(R.string.card_holder_placeholder)
                } else {
                    _binding?.paymentLBLCardHolderName?.text = name.uppercase()
                }
            }
        }
        binding.paymentEDTCardholderName.addTextChangedListener(cardholderPreviewWatcher)

        expiryPreviewWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val expiry = s.toString().trim()
                if (expiry.isEmpty()) {
                    _binding?.paymentLBLCardExpiry?.text = getString(R.string.card_expiry_placeholder)
                } else {
                    _binding?.paymentLBLCardExpiry?.text = expiry
                }
            }
        }
        binding.paymentEDTExpiryDate.addTextChangedListener(expiryPreviewWatcher)
    }

    private fun validateForm(): Boolean {
        var isValid = true

        val cardDigits = binding.paymentEDTCardNumber.text.toString().replace(" ", "")
        if (cardDigits.length != CARD_NUMBER_LENGTH) {
            binding.paymentTILCardNumber.error = getString(R.string.invalid_card_number)
            isValid = false
        } else {
            binding.paymentTILCardNumber.error = null
        }

        val name = binding.paymentEDTCardholderName.text.toString().trim()
        if (name.isEmpty()) {
            binding.paymentTILCardholderName.error = getString(R.string.field_required)
            isValid = false
        } else {
            binding.paymentTILCardholderName.error = null
        }

        val idNumber = binding.paymentEDTIdNumber.text.toString().trim()
        if (idNumber.length != ID_NUMBER_LENGTH) {
            binding.paymentTILIdNumber.error = getString(R.string.invalid_id_number)
            isValid = false
        } else {
            binding.paymentTILIdNumber.error = null
        }

        val expiry = binding.paymentEDTExpiryDate.text.toString().trim()
        if (!expiry.matches(Regex("^(0[1-9]|1[0-2])/\\d{2}$"))) {
            binding.paymentTILExpiryDate.error = getString(R.string.invalid_expiry)
            isValid = false
        } else {
            // Validate that the card is not expired
            val parts = expiry.split("/")
            // Runtime safety fix: use toIntOrNull for defensive parsing
            val expMonth = parts[0].toIntOrNull() ?: 0
            val expYear = 2000 + (parts[1].toIntOrNull() ?: 0)
            val now = java.util.Calendar.getInstance()
            val currentYear = now.get(java.util.Calendar.YEAR)
            val currentMonth = now.get(java.util.Calendar.MONTH) + 1
            if (expYear < currentYear || (expYear == currentYear && expMonth < currentMonth)) {
                binding.paymentTILExpiryDate.error = getString(R.string.invalid_expiry)
                isValid = false
            } else {
                binding.paymentTILExpiryDate.error = null
            }
        }

        val cvv = binding.paymentEDTCvv.text.toString().trim()
        if (cvv.length != CVV_LENGTH) {
            binding.paymentTILCvv.error = getString(R.string.invalid_cvv)
            isValid = false
        } else {
            binding.paymentTILCvv.error = null
        }

        return isValid
    }

    private fun setupCardNumberFormatter() {
        cardNumberFormatterWatcher = object : TextWatcher {
            private var isFormatting = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isFormatting) return
                isFormatting = true
                val digits = s.toString().replace(" ", "")
                if (digits.length <= CARD_NUMBER_LENGTH) {
                    val formatted = digits.chunked(4).joinToString(" ")
                    s?.replace(0, s.length, formatted)
                }
                isFormatting = false
            }
        }
        binding.paymentEDTCardNumber.addTextChangedListener(cardNumberFormatterWatcher)
    }

    private fun setupExpiryFormatter() {
        expiryFormatterWatcher = object : TextWatcher {
            private var isFormatting = false
            private var previousLength = 0

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                previousLength = s?.length ?: 0
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isFormatting || s == null) return
                isFormatting = true

                val isDeleting = s.length < previousLength
                val digits = s.toString().replace("/", "")

                val trimmed = if (digits.length > 4) digits.substring(0, 4) else digits

                val formatted = when {
                    trimmed.isEmpty() -> ""
                    isDeleting && trimmed.length <= 2 -> trimmed
                    trimmed.length == 1 -> {
                        val d = trimmed[0]
                        if (d in '2'..'9') "0$d/"
                        else trimmed
                    }
                    trimmed.length >= 2 -> {
                        var month = trimmed.substring(0, 2).toIntOrNull() ?: 0
                        if (month < 1) month = 1
                        if (month > 12) month = 12
                        val monthStr = "%02d".format(month)
                        if (trimmed.length > 2) {
                            "$monthStr/${trimmed.substring(2)}"
                        } else {
                            "$monthStr/"
                        }
                    }
                    else -> trimmed
                }

                if (s.toString() != formatted) {
                    s.replace(0, s.length, formatted)
                }

                _binding?.paymentEDTExpiryDate?.setSelection(
                    _binding?.paymentEDTExpiryDate?.text?.length ?: 0
                )

                isFormatting = false
            }
        }
        binding.paymentEDTExpiryDate.addTextChangedListener(expiryFormatterWatcher)
    }

    fun onPaymentComplete() {
        _binding?.paymentBTNPay?.stopLoop()
        _binding?.paymentBTNPay?.showSuccess()
    }

    override fun onDestroyView() {
        view?.removeCallbacks(expandRunnable)
        view?.removeCallbacks(reExpandRunnable)
        _binding?.paymentEDTCardNumber?.removeTextChangedListener(cardNumberPreviewWatcher)
        _binding?.paymentEDTCardNumber?.removeTextChangedListener(cardNumberFormatterWatcher)
        _binding?.paymentEDTCardholderName?.removeTextChangedListener(cardholderPreviewWatcher)
        _binding?.paymentEDTExpiryDate?.removeTextChangedListener(expiryPreviewWatcher)
        _binding?.paymentEDTExpiryDate?.removeTextChangedListener(expiryFormatterWatcher)
        expandRunnable = null
        reExpandRunnable = null
        cardNumberPreviewWatcher = null
        cardholderPreviewWatcher = null
        expiryPreviewWatcher = null
        cardNumberFormatterWatcher = null
        expiryFormatterWatcher = null
        super.onDestroyView()
        _binding = null
    }
}