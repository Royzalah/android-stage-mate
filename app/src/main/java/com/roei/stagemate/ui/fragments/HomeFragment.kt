package com.roei.stagemate.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.roei.stagemate.R
import com.google.firebase.firestore.ListenerRegistration
import com.roei.stagemate.ui.adapters.EventAdapter
import com.roei.stagemate.data.interfaces.EventCallback
import com.roei.stagemate.databinding.FragmentHomeBinding
import com.roei.stagemate.ui.dialogs.TrendingEventDialog
import com.roei.stagemate.data.models.Event
import com.roei.stagemate.MyApp
import com.roei.stagemate.utilities.Constants
import com.roei.stagemate.utilities.DateFormatter
import com.roei.stagemate.utilities.SkeletonLoader
import com.roei.stagemate.data.repository.DataRepository
import com.roei.stagemate.utilities.showInfoSnackbar
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope

// Main home screen fragment. Displays hot/recommended events in a list.
// Used by MainActivity as Tab 1. Connects to FirebaseManager for event data.
class HomeFragment : Fragment() {

    // Fragment callback interface per course standard (fragments-callbacks.md).
    // NEVER cast activities directly — use this interface instead.
    interface Callback {
        fun onNotificationsClicked()
    }

    var callback: Callback? = null

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var eventAdapter: EventAdapter

    private var allEvents: List<Event> = emptyList()
    private var hasShownTrendingDialog = false
    private var notificationsListener: ListenerRegistration? = null
    private var lastAppliedLocationMode: String = "all"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lastAppliedLocationMode = MyApp.sharedPrefsManager.getString(
            Constants.SharedPrefs.KEY_LOCATION_FILTER_MODE, "all"
        )
        initViews()
        setupEventsRecyclerView()
        observeLiveEvents()
        DataRepository.seedEventsIfEmpty {
            DataRepository.refreshExpiredEvents {
                if (_binding != null && isAdded) loadEvents()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (_binding == null) return
        val currentMode = MyApp.sharedPrefsManager.getString(
            Constants.SharedPrefs.KEY_LOCATION_FILTER_MODE, "all"
        )
        if (currentMode != lastAppliedLocationMode && allEvents.isNotEmpty()) {
            lastAppliedLocationMode = currentMode
            loadEvents()
        }
    }

    override fun onPause() {
        super.onPause()
        // Stop live events listener when paused to reduce Firestore quota usage
        DataRepository.stopLiveEventsListener()
    }

    private fun initViews() {
        setupGreeting()

        binding.homeIMGNotification.setOnClickListener {
            callback?.onNotificationsClicked()
        }

        binding.homeSRLRefresh.setColorSchemeResources(R.color.accent_primary)
        binding.homeSRLRefresh.setOnRefreshListener {
            loadEvents()
        }

        setupNotificationBadge()
    }

    private fun setupNotificationBadge() {
        notificationsListener?.remove()
        notificationsListener = DataRepository.listenToNotifications { notifications ->
            val b = _binding ?: return@listenToNotifications
            val unreadCount = notifications.count { !it.isRead }
            if (unreadCount > 0) {
                b.homeLBLNotificationBadge.text = if (unreadCount > 9) "9+" else unreadCount.toString()
                b.homeLBLNotificationBadge.visibility = View.VISIBLE
            } else {
                b.homeLBLNotificationBadge.visibility = View.GONE
            }
        }
    }

    private fun getDisplayName(): String {
        val currentUser = DataRepository.getCurrentUser()
        val authName = currentUser?.displayName
        if (!authName.isNullOrBlank()) return authName
        val prefsName = MyApp.sharedPrefsManager.getUserName()
        if (!prefsName.isNullOrBlank()) return prefsName
        val emailPrefix = currentUser?.email?.substringBefore("@")
        if (!emailPrefix.isNullOrBlank()) return emailPrefix
        return getString(R.string.default_user_name)
    }

    private fun setupGreeting() {
        val currentUser = DataRepository.getCurrentUser()
        if (currentUser != null) {
            val displayName = getDisplayName()
            if (MyApp.sharedPrefsManager.isFirstTimeLogin()) {
                MyApp.sharedPrefsManager.setFirstTimeLogin(false)
                binding.homeLBLGreeting.text = getString(R.string.greeting_user, displayName)
            } else {
                val timeStr = DateFormatter.getCurrentTime()
                val hour = timeStr.substringBefore(":").toIntOrNull() ?: 12
                val greetingRes = when {
                    hour < 12 -> R.string.good_morning
                    hour < 18 -> R.string.good_afternoon
                    else -> R.string.good_evening
                }
                binding.homeLBLGreeting.text = getString(greetingRes, displayName)
            }
        } else {
            binding.homeLBLGreeting.text = getString(R.string.greeting_guest)
        }
        binding.homeLBLWelcome.visibility = View.GONE
    }

    private fun setupEventsRecyclerView() {
        eventAdapter = EventAdapter()
        eventAdapter.eventCallback = createEventCallback(eventAdapter)

        binding.homeRVEvents.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = eventAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun createEventCallback(adapter: EventAdapter): EventCallback =
        EventAdapter.createStandardCallback(this, adapter, binding.root, viewLifecycleOwner.lifecycleScope)

    private fun loadEvents() {
        if (_binding == null) return
        showSkeletonLoaders()
        DataRepository.loadHotEvents { hotEvents ->
            val b = _binding ?: return@loadHotEvents
            hideSkeletonLoaders()
            b.homeSRLRefresh.isRefreshing = false

            val preferredCategories = MyApp.sharedPrefsManager.getPreferredCategories()

            var categoryFiltered = if (preferredCategories.isNotEmpty()) {
                val filtered = hotEvents.filter { event ->
                    preferredCategories.any { cat ->
                        event.category.equals(cat, ignoreCase = true) ||
                                event.category.contains(cat, ignoreCase = true)
                    }
                }
                if (filtered.isEmpty()) hotEvents else filtered
            } else {
                hotEvents
            }
            categoryFiltered = categoryFiltered.distinctBy { it.title }
                .filter { DateFormatter.isEventDateTodayOrFuture(it.date) }

            applyLocationFilter(categoryFiltered) { locationFiltered ->
                if (_binding == null) return@applyLocationFilter
                allEvents = locationFiltered
                eventAdapter.submitList(allEvents)
                updateEmptyState(allEvents.isEmpty())
                showTrendingEventDialogIfNeeded()
            }
        }
    }

    private fun applyLocationFilter(events: List<Event>, callback: (List<Event>) -> Unit) {
        val prefs = MyApp.sharedPrefsManager
        val mode = prefs.getString(Constants.SharedPrefs.KEY_LOCATION_FILTER_MODE, "all")

        if (mode == "all" || events.isEmpty()) {
            callback(events)
            return
        }

        val ctx = context ?: run { callback(events); return }
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            callback(events)
            return
        }

        MyApp.locationManager.getLastLocation(
            callback = { lat, lon ->
                val filtered = when (mode) {
                    "city" -> {
                        val nearestCity = MyApp.locationManager.findNearestCity(lat, lon)
                        val cityFiltered = events.filter { event ->
                            event.location.contains(nearestCity, ignoreCase = true)
                        }
                        if (cityFiltered.isEmpty()) events else cityFiltered
                    }
                    "distance" -> {
                        val radiusStr = prefs.getString(Constants.SharedPrefs.KEY_LOCATION_RADIUS, "30")
                        val radius = radiusStr.toIntOrNull() ?: 30
                        val distanceFiltered = events.filter { event ->
                            if (event.latitude == 0.0 && event.longitude == 0.0) true
                            else MyApp.locationManager.calculateDistance(lat, lon, event.latitude, event.longitude) <= radius
                        }
                        if (distanceFiltered.isEmpty()) events else distanceFiltered
                    }
                    else -> events
                }
                callback(filtered)
            },
            onFailure = {
                if (isAdded) showInfoSnackbar(getString(R.string.location_unavailable))
                callback(events)
            }
        )
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        val b = _binding ?: return
        b.homeRVEvents.visibility = if (isEmpty) View.GONE else View.VISIBLE
        b.homeLAYOUTEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        b.homeLBLHotEvents.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    // Always check the cloud value first — local SharedPrefs default is `true`,
    // which causes the dialog to pop up even when the user previously disabled it.
    private fun showTrendingEventDialogIfNeeded() {
        if (hasShownTrendingDialog) return
        val preferredCategories = MyApp.sharedPrefsManager.getPreferredCategories()
        if (preferredCategories.isEmpty() || allEvents.isEmpty()) return

        DataRepository.loadUserSettings { settings ->
            _binding ?: return@loadUserSettings
            val cloudEnabled = settings["trendingNotifications"] as? Boolean

            // If cloud explicitly says false → respect it, sync local, never show
            if (cloudEnabled == false) {
                MyApp.sharedPrefsManager.setTrendingNotificationsEnabled(false)
                hasShownTrendingDialog = true
                return@loadUserSettings
            }
            // Sync local from cloud if cloud has a value
            if (cloudEnabled == true) {
                MyApp.sharedPrefsManager.setTrendingNotificationsEnabled(true)
            }
            // Final check against local pref (handles cloud-null case)
            if (!MyApp.sharedPrefsManager.isTrendingNotificationsEnabled()) {
                hasShownTrendingDialog = true
                return@loadUserSettings
            }

            val trendingEvent = allEvents.filter { !it.isPast() }.shuffled().firstOrNull() ?: return@loadUserSettings
            context?.let { ctx ->
                TrendingEventDialog(ctx, trendingEvent) {}.showDialog()
            }
            hasShownTrendingDialog = true
        }
    }

    private fun showSkeletonLoaders() {
        val b = _binding ?: return
        SkeletonLoader.show(b.homeLAYOUTSkeleton)
        b.homeRVEvents.visibility = View.GONE
        b.homeLAYOUTEmpty.visibility = View.GONE
    }

    private fun hideSkeletonLoaders() {
        val b = _binding ?: return
        SkeletonLoader.hide(b.homeLAYOUTSkeleton, b.homeRVEvents)
    }

    // Callback-based live events listener (replaces forbidden LiveData)
    private fun observeLiveEvents() {
        DataRepository.startLiveEventsListener { liveEvents ->
            _binding ?: return@startLiveEventsListener
            if (liveEvents.isEmpty()) return@startLiveEventsListener

            val preferredCategories = MyApp.sharedPrefsManager.getPreferredCategories()
            val categoryFiltered = if (preferredCategories.isNotEmpty()) {
                val result = liveEvents.filter { event ->
                    event.isHot && DateFormatter.isEventDateTodayOrFuture(event.date) && preferredCategories.any { cat ->
                        event.category.equals(cat, ignoreCase = true)
                    }
                }
                if (result.isEmpty()) liveEvents.filter { it.isHot && DateFormatter.isEventDateTodayOrFuture(it.date) } else result
            } else {
                liveEvents.filter { it.isHot && DateFormatter.isEventDateTodayOrFuture(it.date) }
            }
            if (categoryFiltered.isEmpty()) return@startLiveEventsListener

            applyLocationFilter(categoryFiltered.distinctBy { it.title }) { locationFiltered ->
                if (_binding == null) return@applyLocationFilter
                allEvents = locationFiltered
                eventAdapter.submitList(allEvents)
                updateEmptyState(false)
            }
        }
    }

    override fun onDestroyView() {
        DataRepository.stopLiveEventsListener()
        notificationsListener?.remove()
        notificationsListener = null
        _binding?.homeRVEvents?.adapter = null
        super.onDestroyView()
        _binding = null
    }
}