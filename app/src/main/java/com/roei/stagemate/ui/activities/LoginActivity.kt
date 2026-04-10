package com.roei.stagemate.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import com.roei.stagemate.utilities.showErrorSnackbar
import com.roei.stagemate.utilities.showSuccessSnackbar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.roei.stagemate.R
import com.roei.stagemate.databinding.ActivityLoginBinding
import com.roei.stagemate.data.models.User
import com.roei.stagemate.data.repository.DataRepository
import com.roei.stagemate.MyApp
import kotlinx.coroutines.launch

// Login screen with email/password and Google sign-in.
// On success goes to PreferredCategories or MainActivity.
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    private val googleSignInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val firebaseUser = FirebaseAuth.getInstance().currentUser ?: return@registerForActivityResult
            val user = User(
                id = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                name = firebaseUser.displayName ?: "User",
                phone = firebaseUser.phoneNumber ?: "",
                profileImageUrl = firebaseUser.photoUrl?.toString() ?: "",
                isGoogleAuth = true
            )
            DataRepository.saveUserProfile(user) { success, _ ->
                if (success) {
                    MyApp.sharedPrefsManager.apply {
                        saveUserId(firebaseUser.uid)
                        saveUserEmail(user.email)
                        saveUserName(user.name)
                        setRememberMe(true)
                        setLoggedIn(true)
                    }
                    DataRepository.updateUser(
                        mapOf("rememberMe" to true)
                    ) { _, _ -> }
                    MyApp.signalManager.toast(getString(R.string.welcome_login_toast))
                    loadCategoriesAndNavigate()
                }
            }
        } else {
            showErrorSnackbar(getString(R.string.google_signin_failed))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
    }

    private fun initViews() {
        val prefs = MyApp.sharedPrefsManager
        // Dead code wired: use SharedPrefsManager.isLoggedIn() as offline-first pre-fill
        if (prefs.isLoggedIn() || prefs.isRememberMe()) {
            binding.loginEDTUsername.setText(prefs.getUserEmail() ?: "")
            binding.loginCHKRemember.isChecked = true
        }

        binding.loginBTNLogin.setOnClickListener { handleLogin() }
        binding.loginBTNGoogle.setOnClickListener { signInWithGoogle() }
        binding.loginLBLSignupLink.setOnClickListener { startActivity(Intent(this, SignUpActivity::class.java)) }
        binding.loginLBLForgotPassword.setOnClickListener { showForgotPasswordDialog() }
    }

    private fun showForgotPasswordDialog() {
        val inputLayout = TextInputLayout(this).apply {
            hint = getString(R.string.email)
            setPadding(48, 16, 48, 0)
        }
        val input = TextInputEditText(this)
        input.setText(binding.loginEDTUsername.text?.toString() ?: "")
        inputLayout.addView(input)

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.forgot_password_dialog_title))
            .setMessage(getString(R.string.forgot_password_dialog_message))
            .setView(inputLayout)
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                val email = input.text?.toString()?.trim() ?: ""
                if (email.isNotEmpty()) {
                    DataRepository.resetPassword(email) { success, error ->
                        lifecycleScope.launch {
                            if (success) {
                                showSuccessSnackbar(getString(R.string.reset_email_sent))
                            } else {
                                showErrorSnackbar(error ?: getString(R.string.reset_email_failed))
                            }
                        }
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun signInWithGoogle() {
        val providers = listOf(AuthUI.IdpConfig.GoogleBuilder().build())
        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .build()
        googleSignInLauncher.launch(signInIntent)
    }

    private fun handleLogin() {
        val email = binding.loginEDTUsername.text.toString().trim()
        val password = binding.loginEDTPassword.text.toString().trim()
        if (email.isEmpty() || password.isEmpty()) return
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showErrorSnackbar(getString(R.string.invalid_email_format))
            return
        }
        performLogin(email, password)
    }

    // Authenticates with Firebase, enforces email verification, then navigates forward
    private fun performLogin(email: String, password: String) {
        DataRepository.signIn(email, password) { success, error ->
            lifecycleScope.launch {
                if (isFinishing || isDestroyed) return@launch
                if (success) {
                    val user = DataRepository.getCurrentUser()

                    if (user != null && !user.isEmailVerified) {
                        showErrorSnackbar(getString(R.string.please_verify_email))
                        DataRepository.signOut()
                        return@launch
                    }

                    val rememberMe = binding.loginCHKRemember.isChecked
                    MyApp.sharedPrefsManager.apply {
                        saveUserId(user?.uid ?: "")
                        saveUserEmail(email)
                        saveUserName(user?.displayName ?: "")
                        setRememberMe(rememberMe)
                        setLoggedIn(true)
                    }

                    DataRepository.updateUser(
                        mapOf("rememberMe" to rememberMe)
                    ) { _, _ -> }

                    MyApp.signalManager.toast(getString(R.string.welcome_login_toast))
                    loadCategoriesAndNavigate()
                } else {
                    showErrorSnackbar(error ?: getString(R.string.login_failed))
                }
            }
        }
    }

    private fun loadCategoriesAndNavigate() {
        DataRepository.loadPreferredCategories { categories ->
            lifecycleScope.launch {
                if (isFinishing || isDestroyed) return@launch
                if (categories.isNotEmpty()) {
                    MyApp.sharedPrefsManager.savePreferredCategories(categories)
                }
                val hasPreferred = MyApp.sharedPrefsManager.hasPreferredCategories()
                val intent = if (!hasPreferred) {
                    Intent(this@LoginActivity, PreferredCategoriesActivity::class.java)
                } else {
                    Intent(this@LoginActivity, MainActivity::class.java)
                }
                startActivity(intent)
                finish()
            }
        }
    }
}
