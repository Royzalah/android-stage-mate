package com.roei.stagemate.ui.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import com.roei.stagemate.utilities.showWarningSnackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging
import com.roei.stagemate.R
import com.roei.stagemate.databinding.ActivityMainBinding
import com.roei.stagemate.ui.fragments.HomeFragment
import com.roei.stagemate.ui.fragments.NotificationsFragment
import com.roei.stagemate.ui.fragments.SearchFragment
import com.roei.stagemate.ui.fragments.ProfileFragment
import com.roei.stagemate.ui.fragments.TicketsFragment
import com.roei.stagemate.BuildConfig
import com.roei.stagemate.utilities.Constants
import com.roei.stagemate.data.repository.DataRepository

// Main screen with bottom navigation: Home, Search, Tickets, Profile.
// Also handles deep links and FCM token registration.
class MainActivity : AppCompatActivity(), HomeFragment.Callback, ProfileFragment.Callback, NotificationsFragment.Callback {

    private lateinit var binding: ActivityMainBinding

    private val homeFragment by lazy { HomeFragment().also { it.callback = this } }
    private val searchFragment by lazy { SearchFragment() }
    private val ticketsFragment by lazy { TicketsFragment() }
    private val profileFragment by lazy { ProfileFragment().also { it.callback = this } }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            getFCMToken()
        } else {
            showWarningSnackbar(getString(R.string.notifications_disabled_warning))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupCrashlytics()
        setupBottomNavigation()
        setupBackNavigation()
        requestNotificationPermission()
        handleDeepLink(intent)
        DataRepository.startFavoritesCacheListener()

        if (savedInstanceState == null) {
            val openTab = intent.getStringExtra(Constants.IntentKeys.OPEN_TAB)
            if (openTab == "tickets") {
                // Dead code wired: use navigateToTab for programmatic tab switching
                navigateToTab(R.id.navigation_tickets)
            } else {
                loadFragment(homeFragment)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    // Parses deep link URIs like /event/{eventId} and opens EventDetailActivity
    private fun handleDeepLink(intent: Intent?) {
        val data = intent?.data ?: return

        when {
            data.pathSegments?.getOrNull(0) == "event" -> {
                val eventId = data.pathSegments?.getOrNull(1)
                if (eventId != null) {
                    startActivity(EventDetailActivity.newIntent(this, eventId))
                }
            }
        }
    }

    private fun setupBottomNavigation() {
        binding.mainBNVNavigation.isItemActiveIndicatorEnabled = true
        binding.mainBNVNavigation.itemActiveIndicatorColor =
            android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.bottom_nav_indicator))

        binding.mainBNVNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> { loadFragment(homeFragment); true }
                R.id.navigation_search -> { loadFragment(searchFragment); true }
                R.id.navigation_tickets -> { loadFragment(ticketsFragment); true }
                R.id.navigation_profile -> { loadFragment(profileFragment); true }
                else -> false
            }
        }
    }

    // Programmatically switch to a tab, used by fragments
    fun navigateToTab(tabId: Int) {
        binding.mainBNVNavigation.selectedItemId = tabId
    }

    // Navigate to Tickets tab and scroll to a specific ticket by eventId
    fun scrollToTicket(eventId: String) {
        if (eventId.isBlank()) return
        binding.mainBNVNavigation.selectedItemId = R.id.navigation_tickets
        ticketsFragment.scrollToTicket(eventId)
    }

    // ProfileFragment.Callback — navigate to Tickets tab with filter
    override fun onNavigateToTickets(filter: String) {
        binding.mainBNVNavigation.selectedItemId = R.id.navigation_tickets
        ticketsFragment.applyExternalFilter(filter)
    }

    // Opens NotificationsFragment above the current tab, used by HomeFragment
    override fun onNavigateToTicketWithScroll(eventId: String) {
        scrollToTicket(eventId)
    }

    override fun onOpenTicketDetail(ticketId: String, eventId: String) {
        binding.mainBNVNavigation.selectedItemId = R.id.navigation_tickets
        ticketsFragment.openTicketById(ticketId)
    }

    fun openNotifications() {
        val fragment = NotificationsFragment().also { it.callback = this }
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_LAYOUT_fragment_container, fragment)
            .addToBackStack("notifications")
            .commit()
        binding.mainBNVNavigation.menu.setGroupCheckable(0, true, false)
        for (i in 0 until binding.mainBNVNavigation.menu.size()) {
            binding.mainBNVNavigation.menu.getItem(i).isChecked = false
        }
        binding.mainBNVNavigation.menu.setGroupCheckable(0, true, true)
    }

    override fun onNotificationsClicked() {
        openNotifications()
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_LAYOUT_fragment_container, fragment)
            .commit()
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                    binding.mainBNVNavigation.selectedItemId = R.id.navigation_home
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun setupCrashlytics() {
        try {
            val crashlytics = FirebaseCrashlytics.getInstance()
            val currentUser = DataRepository.getCurrentUser()

            if (currentUser != null) {
                crashlytics.setUserId(currentUser.uid)
                crashlytics.setCustomKey("user_email", currentUser.email ?: "unknown")
                crashlytics.setCustomKey("user_name", currentUser.displayName ?: "unknown")
            }

            crashlytics.setCustomKey("last_activity", "MainActivity")
            crashlytics.setCustomKey("app_version", BuildConfig.VERSION_NAME)
        } catch (_: Exception) { }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    getFCMToken()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    showWarningSnackbar(getString(R.string.notification_permission_event_updates))
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            getFCMToken()
        }
    }

    private fun getFCMToken() {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                DataRepository.saveUserFCMToken(token) { _, _ -> }
            }
            .addOnFailureListener { }
    }

    override fun onDestroy() {
        DataRepository.stopFavoritesCacheListener()
        super.onDestroy()
    }

}
