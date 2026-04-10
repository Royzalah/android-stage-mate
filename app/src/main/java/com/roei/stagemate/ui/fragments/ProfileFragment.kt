package com.roei.stagemate.ui.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.ListenerRegistration
import com.roei.stagemate.R
import com.roei.stagemate.ui.activities.LoginActivity
import com.roei.stagemate.ui.activities.PersonalInfoActivity
import com.roei.stagemate.ui.activities.SecurityActivity
import com.roei.stagemate.ui.activities.PrivacyPolicyActivity
import com.roei.stagemate.ui.activities.HelpActivity
import com.roei.stagemate.ui.activities.PreferredCategoriesActivity
import com.roei.stagemate.databinding.FragmentProfileBinding
import com.roei.stagemate.data.models.User
import com.roei.stagemate.utilities.Constants
import com.roei.stagemate.data.repository.DataRepository
import com.roei.stagemate.MyApp
import com.roei.stagemate.utilities.showErrorSnackbar
import com.roei.stagemate.utilities.showWarningSnackbar

// User profile screen with account info, settings, and sign-out.
// Used by MainActivity as Tab 4. Connects to FirebaseManager for user data.
class ProfileFragment : Fragment() {

    // Interface callback per course standard (fragments-callbacks.md).
    // NEVER cast activity directly — always use this callback.
    interface Callback {
        fun onNavigateToTickets(filter: String)
    }
    var callback: Callback? = null

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private var currentUser: User? = null
    private var userProfileListener: ListenerRegistration? = null
    private var pendingLocationMode: String? = null

    // Stored as a field so onResume can re-attach it after detaching for programmatic updates
    private val trendingCheckedListener =
        android.widget.CompoundButton.OnCheckedChangeListener { _, isChecked ->
            MyApp.sharedPrefsManager.setTrendingNotificationsEnabled(isChecked)
            DataRepository.saveUserSettings(
                mapOf("trendingNotifications" to isChecked)
            ) { success, _ ->
                if (!success) revertTrendingToggle(!isChecked)
            }
        }

    private fun revertTrendingToggle(revertTo: Boolean) {
        MyApp.sharedPrefsManager.setTrendingNotificationsEnabled(revertTo)
        val sw = _binding?.profileSWITCHTrending ?: return
        sw.setOnCheckedChangeListener(null)
        sw.isChecked = revertTo
        sw.setOnCheckedChangeListener(trendingCheckedListener)
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingLocationMode?.let { mode ->
                applyLocationMode(mode)
            }
        } else {
            showWarningSnackbar(getString(R.string.location_permission_denied))
            applyLocationMode("all")
            _binding?.profileBTGLocation?.check(R.id.profile_BTN_location_all)
        }
        pendingLocationMode = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        loadUserProfile()
    }

    override fun onResume() {
        super.onResume()
        // Re-sync the trending toggle in case TrendingEventDialog ("Not Interested")
        // changed the value while ProfileFragment was paused.
        val sw = _binding?.profileSWITCHTrending ?: return
        val current = MyApp.sharedPrefsManager.isTrendingNotificationsEnabled()
        if (sw.isChecked != current) {
            sw.setOnCheckedChangeListener(null)
            sw.isChecked = current
            sw.setOnCheckedChangeListener(trendingCheckedListener)
        }
    }

    private fun initViews() {
        binding.profileBTNEdit.setOnClickListener {
            startActivity(Intent(requireContext(), PersonalInfoActivity::class.java))
        }

        binding.profileBTNLogout.setOnClickListener {
            signOut()
        }

        binding.profileBTNPastEvents.setOnClickListener {
            callback?.onNavigateToTickets("past")
        }

        binding.profileBTNUpcomingEvents.setOnClickListener {
            callback?.onNavigateToTickets("upcoming")
        }

        binding.profileBTNSecurity.setOnClickListener {
            startActivity(Intent(requireContext(), SecurityActivity::class.java))
        }

        binding.profileBTNPrivacy.setOnClickListener {
            startActivity(Intent(requireContext(), PrivacyPolicyActivity::class.java))
        }

        binding.profileBTNHelp.setOnClickListener {
            startActivity(Intent(requireContext(), HelpActivity::class.java))
        }

        binding.profileBTNPreferredCategories.setOnClickListener {
            val intent = Intent(requireContext(), PreferredCategoriesActivity::class.java)
            intent.putExtra("from_profile", true)
            startActivity(intent)
        }

        setupTrendingToggle()
        setupLocationFilter()
    }

    private fun setupTrendingToggle() {
        val b = _binding ?: return

        // Step 1: detach any existing listener before mutating state
        b.profileSWITCHTrending.setOnCheckedChangeListener(null)

        // Step 2: show local cached value immediately
        b.profileSWITCHTrending.isChecked = MyApp.sharedPrefsManager.isTrendingNotificationsEnabled()

        // Step 3: attach the listener (defined as a field for re-attachment in onResume)
        b.profileSWITCHTrending.setOnCheckedChangeListener(trendingCheckedListener)

        // Step 4: load cloud value — guard against view destruction in async callback
        DataRepository.loadUserSettings { settings ->
            val binding = _binding ?: return@loadUserSettings
            val cloudEnabled = settings["trendingNotifications"] as? Boolean ?: return@loadUserSettings
            val localEnabled = MyApp.sharedPrefsManager.isTrendingNotificationsEnabled()
            if (cloudEnabled != localEnabled) {
                MyApp.sharedPrefsManager.setTrendingNotificationsEnabled(cloudEnabled)
                binding.profileSWITCHTrending.setOnCheckedChangeListener(null)
                binding.profileSWITCHTrending.isChecked = cloudEnabled
                binding.profileSWITCHTrending.setOnCheckedChangeListener(trendingCheckedListener)
            }
        }
    }

    private fun setupLocationFilter() {
        val prefs = MyApp.sharedPrefsManager
        val currentMode = prefs.getString(Constants.SharedPrefs.KEY_LOCATION_FILTER_MODE, "all")

        val checkedId = when (currentMode) {
            "city" -> R.id.profile_BTN_location_city
            "distance" -> R.id.profile_BTN_location_distance
            else -> R.id.profile_BTN_location_all
        }
        binding.profileBTGLocation.check(checkedId)
        binding.profileBTGRadius.visibility = if (currentMode == "distance") View.VISIBLE else View.GONE

        val savedRadius15 = prefs.getBoolean(Constants.SharedPrefs.KEY_LOCATION_RADIUS + "_15", false)
        val savedRadius30 = prefs.getBoolean(Constants.SharedPrefs.KEY_LOCATION_RADIUS + "_30", false)
        val savedRadius50 = prefs.getBoolean(Constants.SharedPrefs.KEY_LOCATION_RADIUS + "_50", false)
        val savedRadius100 = prefs.getBoolean(Constants.SharedPrefs.KEY_LOCATION_RADIUS + "_100", false)
        val radiusCheckedId = when {
            savedRadius15 -> R.id.profile_BTN_radius_15
            savedRadius30 -> R.id.profile_BTN_radius_30
            savedRadius50 -> R.id.profile_BTN_radius_50
            savedRadius100 -> R.id.profile_BTN_radius_100
            else -> R.id.profile_BTN_radius_30
        }
        binding.profileBTGRadius.check(radiusCheckedId)

        binding.profileBTGLocation.addOnButtonCheckedListener { _, checkedButtonId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val mode = when (checkedButtonId) {
                R.id.profile_BTN_location_city -> "city"
                R.id.profile_BTN_location_distance -> "distance"
                else -> "all"
            }

            if (mode == "city" || mode == "distance") {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    pendingLocationMode = mode
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    return@addOnButtonCheckedListener
                }
            }
            applyLocationMode(mode)
        }

        binding.profileBTGRadius.addOnButtonCheckedListener { _, checkedButtonId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val radius = when (checkedButtonId) {
                R.id.profile_BTN_radius_15 -> 15
                R.id.profile_BTN_radius_50 -> 50
                R.id.profile_BTN_radius_100 -> 100
                else -> 30
            }
            prefs.saveString(Constants.SharedPrefs.KEY_LOCATION_RADIUS, radius.toString())
            prefs.putBoolean(Constants.SharedPrefs.KEY_LOCATION_RADIUS + "_15", radius == 15)
            prefs.putBoolean(Constants.SharedPrefs.KEY_LOCATION_RADIUS + "_30", radius == 30)
            prefs.putBoolean(Constants.SharedPrefs.KEY_LOCATION_RADIUS + "_50", radius == 50)
            prefs.putBoolean(Constants.SharedPrefs.KEY_LOCATION_RADIUS + "_100", radius == 100)
            DataRepository.saveUserSettings(
                mapOf("locationRadius" to radius)
            ) { _, _ -> }
        }
    }

    private fun applyLocationMode(mode: String) {
        val b = _binding ?: return
        MyApp.sharedPrefsManager.saveString(Constants.SharedPrefs.KEY_LOCATION_FILTER_MODE, mode)
        b.profileBTGRadius.visibility = if (mode == "distance") View.VISIBLE else View.GONE
        DataRepository.saveUserSettings(
            mapOf("locationFilterMode" to mode)
        ) { _, _ -> }
    }

    private fun loadUserProfile() {
        val userId = DataRepository.getCurrentUserId()
        if (userId == null) {
            showErrorSnackbar(getString(R.string.please_log_in))
            return
        }
        userProfileListener = DataRepository.listenToUser { user ->
            // Guard against callback returning after onDestroyView
            _binding ?: return@listenToUser
            if (user != null) {
                currentUser = user
                displayUserProfile(user)
            } else {
                val firebaseUser = DataRepository.getCurrentUser()
                if (firebaseUser != null) {
                    val localUser = User(
                        id = firebaseUser.uid,
                        email = firebaseUser.email ?: "",
                        // Dead code wired: use DataRepository.getCurrentUserDisplayName() as fallback
                        name = DataRepository.getCurrentUserDisplayName() ?: getString(R.string.guest),
                        city = MyApp.sharedPrefsManager.getSelectedCity() ?: getString(R.string.default_city),
                        profileImageUrl = ""
                    )
                    currentUser = localUser
                    displayUserProfile(localUser)
                } else {
                    showErrorSnackbar(getString(R.string.not_logged_in))
                }
            }
        }
    }

    private fun displayUserProfile(user: User) {
        val b = _binding ?: return
        b.profileLBLName.text = user.name
        b.profileLBLEmail.text = user.email
    }

    private fun signOut() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.sign_out))
            .setMessage(getString(R.string.sign_out_confirm))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                DataRepository.updateUser(mapOf("rememberMe" to false)) { _, _ -> }
                DataRepository.signOut()
                // Dead code wired: clear saved filters on sign-out
                MyApp.sharedPrefsManager.clearFilters()
                MyApp.sharedPrefsManager.logout()
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                activity?.finish()
            }
            .setNegativeButton(getString(R.string.no), null)
            .show()
    }

    override fun onDestroyView() {
        userProfileListener?.remove()
        userProfileListener = null
        super.onDestroyView()
        _binding = null
    }
}