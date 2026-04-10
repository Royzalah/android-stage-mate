package com.roei.stagemate.ui.activities

import android.os.Bundle
import android.util.Patterns
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.roei.stagemate.utilities.showErrorSnackbar
import com.roei.stagemate.utilities.showInfoSnackbar
import com.roei.stagemate.R
import com.roei.stagemate.databinding.ActivitySignupBinding
import com.roei.stagemate.MyApp
import com.roei.stagemate.data.models.User
import com.roei.stagemate.data.repository.DataRepository

// Sign-up screen with email/password registration.
// Sends a verification email on success, then returns to LoginActivity.
class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
    }

    private fun initViews() {
        binding.signupBTNSignup.setOnClickListener { handleSignUp() }
        binding.signupLBLLoginLink.setOnClickListener { finish() }
    }

    private fun handleSignUp() {
        binding.signupTILFirstName.error = null
        binding.signupTILLastName.error = null
        binding.signupTILEmail.error = null
        binding.signupTILPassword.error = null
        binding.signupTILConfirmPassword.error = null

        val firstName = binding.signupEDTFirstName.text.toString().trim()
        val lastName = binding.signupEDTLastName.text.toString().trim()
        val email = binding.signupEDTEmail.text.toString().trim()
        val password = binding.signupEDTPassword.text.toString().trim()
        val confirmPassword = binding.signupEDTConfirmPassword.text.toString().trim()

        when {
            firstName.isEmpty() -> {
                binding.signupTILFirstName.error = getString(R.string.enter_first_name)
                return
            }
            lastName.isEmpty() -> {
                binding.signupTILLastName.error = getString(R.string.enter_last_name)
                return
            }
            email.isEmpty() -> {
                binding.signupTILEmail.error = getString(R.string.enter_email)
                return
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.signupTILEmail.error = getString(R.string.enter_valid_email)
                return
            }
            password.length < 6 -> {
                binding.signupTILPassword.error = getString(R.string.password_min_length)
                return
            }
            password != confirmPassword -> {
                binding.signupTILConfirmPassword.error = getString(R.string.passwords_dont_match)
                return
            }
        }

        performSignUp(firstName, lastName, email, password)
    }

    private fun performSignUp(
        firstName: String,
        lastName: String,
        email: String,
        password: String
    ) {
        DataRepository.signUp(email, password) { success, error ->
            lifecycleScope.launch {
                if (isFinishing || isDestroyed) return@launch
                if (success) {
                    val firebaseUser = DataRepository.getCurrentUser() ?: return@launch

                    firebaseUser.sendEmailVerification().addOnCompleteListener { verifyTask ->
                        if (verifyTask.isSuccessful) {
                            showInfoSnackbar(getString(R.string.verification_email_sent))

                            val user = User(
                                id = firebaseUser.uid,
                                email = firebaseUser.email ?: "",
                                name = "$firstName $lastName",
                                firstName = firstName,
                                lastName = lastName
                            )

                            DataRepository.saveUserProfile(user) { saveSuccess, _ ->
                                if (saveSuccess) {
                                    MyApp.sharedPrefsManager.setFirstTimeLogin(true)
                                    DataRepository.signOut()
                                    finish()
                                }
                            }
                        }
                    }
                } else {
                    showErrorSnackbar(error ?: getString(R.string.signup_error))
                }
            }
        }
    }
}
