package com.roei.stagemate.ui.activities

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.roei.stagemate.utilities.showWarningSnackbar
import com.roei.stagemate.R
import com.roei.stagemate.databinding.ActivitySplashBinding
import com.roei.stagemate.utilities.Constants
import com.roei.stagemate.data.repository.DataRepository
import com.roei.stagemate.MyApp

// Splash screen — checks auth and rememberMe, then goes to Onboarding, Login, or Main.
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val animators = mutableListOf<ObjectAnimator>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAnimations()
    }

    // Syncs rememberMe flag from Firestore before deciding navigation target
    // Dead code wired: uses DataRepository.isLoggedIn() instead of direct FirebaseAuth check
    private fun syncRememberMeAndNavigate() {
        val prefs = MyApp.sharedPrefsManager

        if (!DataRepository.isLoggedIn()) {
            navigateToNextScreen(prefs.isRememberMe())
            return
        }

        DataRepository.getUserProfile { firestoreUser, _ ->
            lifecycleScope.launch {
                val rememberMe = if (firestoreUser != null) {
                    prefs.setRememberMe(firestoreUser.rememberMe)
                    firestoreUser.rememberMe
                } else {
                    prefs.isRememberMe()
                }
                navigateToNextScreen(rememberMe)
            }
        }
    }

    // Determines next screen based on onboarding, auth, and preferences state
    private fun navigateToNextScreen(rememberMe: Boolean) {
        val prefs = MyApp.sharedPrefsManager
        val user = DataRepository.getCurrentUser()
        val hasSeenOnboarding = prefs.hasSeenOnboarding()
        val hasPreferredCategories = prefs.hasPreferredCategories()

        val intent = when {
            !hasSeenOnboarding -> Intent(this, OnboardingActivity::class.java)
            user == null || !rememberMe -> {
                if (!rememberMe) DataRepository.signOut()
                Intent(this, LoginActivity::class.java)
            }
            !user.isEmailVerified -> {
                showWarningSnackbar(getString(R.string.please_verify_email))
                Intent(this, LoginActivity::class.java)
            }
            !hasPreferredCategories -> Intent(this, PreferredCategoriesActivity::class.java)
            else -> Intent(this, MainActivity::class.java)
        }
        startActivity(intent)
        finish()
    }

    private fun setupAnimations() {
        binding.splashIMGLogo.scaleX = 0.7f
        binding.splashIMGLogo.scaleY = 0.7f
        binding.splashLBLAppName.alpha = 0f
        binding.splashLBLTagline.alpha = 0f

        ObjectAnimator.ofFloat(binding.splashIMGLogo, "alpha", 0f, 1f).apply {
            duration = 1000L
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    syncRememberMeAndNavigate()
                }
            })
            start()
        }.also { animators += it }

        ObjectAnimator.ofFloat(binding.splashIMGLogo, "scaleX", 0.7f, 1f).apply {
            duration = 700L
            startDelay = 200L
            interpolator = android.view.animation.DecelerateInterpolator()
            start()
        }.also { animators += it }
        ObjectAnimator.ofFloat(binding.splashIMGLogo, "scaleY", 0.7f, 1f).apply {
            duration = 700L
            startDelay = 200L
            interpolator = android.view.animation.DecelerateInterpolator()
            start()
        }.also { animators += it }

        ObjectAnimator.ofFloat(binding.splashLBLAppName, "alpha", 0f, 1f).apply {
            duration = Constants.UI.SPLASH_TEXT_DURATION
            startDelay = 500L
            start()
        }.also { animators += it }
        ObjectAnimator.ofFloat(binding.splashLBLTagline, "alpha", 0f, 1f).apply {
            duration = Constants.UI.SPLASH_TEXT_DURATION
            startDelay = 700L
            start()
        }.also { animators += it }
        ObjectAnimator.ofFloat(binding.splashPBProgress, "alpha", 0f, 1f).apply {
            duration = Constants.UI.SPLASH_PROGRESS_DURATION
            startDelay = 1000L
            start()
        }.also { animators += it }
    }

    override fun onDestroy() {
        animators.forEach {
            it.removeAllListeners()
            it.cancel()
        }
        super.onDestroy()
    }
}
