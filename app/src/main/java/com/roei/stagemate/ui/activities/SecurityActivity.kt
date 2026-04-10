package com.roei.stagemate.ui.activities

import android.os.Bundle
import com.roei.stagemate.utilities.showErrorSnackbar
import com.roei.stagemate.utilities.showSuccessSnackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.roei.stagemate.R
import com.roei.stagemate.databinding.ActivitySecurityBinding
import com.roei.stagemate.data.repository.DataRepository

// Security settings screen for changing password, opened from ProfileFragment.
class SecurityActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySecurityBinding
    private val isProcessing = java.util.concurrent.atomic.AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySecurityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
    }

    private fun initViews() {
        binding.securityBTNChangePassword.setOnClickListener { changePassword() }
    }

    // Re-authenticates with current password before updating to new one
    private fun changePassword() {
        if (!isProcessing.compareAndSet(false, true)) return

        val currentPassword = binding.securityEDTCurrentPassword.text.toString()
        val newPassword = binding.securityEDTNewPassword.text.toString()
        val confirmPassword = binding.securityEDTConfirmPassword.text.toString()

        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            isProcessing.set(false)
            showErrorSnackbar(getString(R.string.please_fill_all_fields))
            return
        }

        if (newPassword != confirmPassword) {
            isProcessing.set(false)
            showErrorSnackbar(getString(R.string.passwords_dont_match))
            return
        }

        if (newPassword.length < 6) {
            isProcessing.set(false)
            showErrorSnackbar(getString(R.string.password_min_length))
            return
        }
        binding.securityBTNChangePassword.isEnabled = false
        binding.securityBTNChangePassword.text = getString(R.string.processing)

        DataRepository.changePassword(currentPassword, newPassword) { success, error ->
            lifecycleScope.launch {
                if (isFinishing || isDestroyed) return@launch
                isProcessing.set(false)
                binding.securityBTNChangePassword.isEnabled = true
                binding.securityBTNChangePassword.text = getString(R.string.change_password)

                if (success) {
                    binding.securityEDTCurrentPassword.text?.clear()
                    binding.securityEDTNewPassword.text?.clear()
                    binding.securityEDTConfirmPassword.text?.clear()

                    showSuccessSnackbar(getString(R.string.password_changed_successfully))
                    finish()
                } else {
                    showErrorSnackbar(error ?: getString(R.string.failed_to_change_password))
                }
            }
        }
    }

}
