package com.roei.stagemate.ui.activities

import android.os.Bundle
import android.util.Patterns
import android.view.View
import com.roei.stagemate.utilities.showErrorSnackbar
import com.roei.stagemate.utilities.showSuccessSnackbar
import com.roei.stagemate.utilities.showWarningSnackbar
import androidx.appcompat.app.AppCompatActivity
import com.roei.stagemate.R
import com.roei.stagemate.databinding.ActivityPersonalInfoBinding
import com.roei.stagemate.data.repository.DataRepository
import com.roei.stagemate.data.models.User
import com.roei.stagemate.utilities.Constants
import com.roei.stagemate.MyApp

// Profile editing screen for name, email, phone, and city.
// Loads from Firestore (falls back to SharedPrefs), saves to both.
class PersonalInfoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPersonalInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPersonalInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
        loadUserData()
    }

    private fun initViews() {
        binding.personalInfoBTNSave.setOnClickListener { saveUserData() }
    }

    private fun loadUserData() {
        showLoading(true)

        DataRepository.getUserProfile { user, error ->
            if (isFinishing || isDestroyed) return@getUserProfile
            showLoading(false)

            if (user != null) {
                displayUserData(user)
                // If user has no city, try loading from cloud
                if (user.city.isBlank()) {
                    DataRepository.loadSelectedCity { cloudCity ->
                        if (!cloudCity.isNullOrBlank()) {
                            binding.personalInfoEDTCity.setText(cloudCity)
                        }
                    }
                }
            } else {
                val prefs = MyApp.sharedPrefsManager
                binding.personalInfoEDTName.setText(prefs.getUserName())
                binding.personalInfoEDTEmail.setText(prefs.getUserEmail())
                binding.personalInfoEDTPhone.setText(prefs.getString(Constants.SharedPrefs.KEY_USER_PHONE, ""))
                binding.personalInfoEDTCity.setText(prefs.getString(Constants.SharedPrefs.KEY_USER_CITY, ""))

                // Try cloud fallback for city
                DataRepository.loadSelectedCity { cloudCity ->
                    if (!cloudCity.isNullOrBlank() && binding.personalInfoEDTCity.text.isNullOrBlank()) {
                        binding.personalInfoEDTCity.setText(cloudCity)
                    }
                }

                if (error != null) {
                    showWarningSnackbar(getString(R.string.loading_from_local))
                }
            }
        }
    }

    private fun displayUserData(user: User) {
        binding.personalInfoEDTName.setText(user.name)
        binding.personalInfoEDTEmail.setText(user.email)
        binding.personalInfoEDTPhone.setText(user.phone)
        binding.personalInfoEDTCity.setText(user.city)
    }

    private fun saveUserData() {
        val name = binding.personalInfoEDTName.text.toString().trim()
        val email = binding.personalInfoEDTEmail.text.toString().trim()
        val phone = binding.personalInfoEDTPhone.text.toString().trim()
        val city = binding.personalInfoEDTCity.text.toString().trim()

        if (name.isEmpty()) {
            showErrorSnackbar(getString(R.string.enter_name))
            binding.personalInfoEDTName.requestFocus()
            return
        }

        if (email.isEmpty()) {
            showErrorSnackbar(getString(R.string.enter_email))
            binding.personalInfoEDTEmail.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showErrorSnackbar(getString(R.string.enter_valid_email))
            binding.personalInfoEDTEmail.requestFocus()
            return
        }

        if (phone.isNotEmpty() && !isValidPhone(phone)) {
            showErrorSnackbar(getString(R.string.enter_valid_phone))
            binding.personalInfoEDTPhone.requestFocus()
            return
        }

        val userId = DataRepository.getCurrentUserId() ?: "local_user"

        val user = User(
            id = userId,
            email = email,
            name = name,
            phone = phone,
            city = city
        )

        showLoading(true)

        DataRepository.saveUserProfile(user) { success, error ->
            if (isFinishing || isDestroyed) return@saveUserProfile
            showLoading(false)

            if (success) {
                val prefs = MyApp.sharedPrefsManager
                prefs.saveUserName(name)
                prefs.saveUserEmail(email)
                prefs.saveString(Constants.SharedPrefs.KEY_USER_PHONE, phone)
                prefs.saveSelectedCity(city)
                DataRepository.saveSelectedCity(city) { _, _ -> }

                showSuccessSnackbar(getString(R.string.profile_updated))
                finish()
            } else {
                showErrorSnackbar(getString(R.string.failed_to_update_profile, error ?: ""))
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.personalInfoPBLoading.visibility = if (show) View.VISIBLE else View.GONE
        binding.personalInfoBTNSave.isEnabled = !show
    }

    // Validates Israeli and international phone formats
    private fun isValidPhone(phone: String): Boolean {
        val cleaned = phone.replace(Regex("[\\s()-]"), "")
        return when {
            cleaned.startsWith("+972") -> cleaned.length in 12..14
            cleaned.startsWith("972") -> cleaned.length in 11..13
            cleaned.startsWith("0") -> cleaned.length in 9..10
            else -> cleaned.length in 7..15
        }
    }
}
