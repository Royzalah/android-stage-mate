package com.roei.stagemate.ui.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.roei.stagemate.R
import com.roei.stagemate.ui.adapters.CityAdapter
import com.roei.stagemate.ui.adapters.EventAdapter
import com.roei.stagemate.databinding.FragmentSearchBinding
import com.roei.stagemate.data.models.Event
import com.roei.stagemate.data.models.FilterOptions
import com.roei.stagemate.data.models.IsraeliLocations
import com.roei.stagemate.data.repository.DataRepository
import com.roei.stagemate.data.interfaces.EventCallback
import com.roei.stagemate.utilities.Constants
import com.roei.stagemate.utilities.DateFormatter
import com.roei.stagemate.MyApp
import android.content.Intent
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

// Search/explore screen with category chips, city filters, text search, and "Near Me" location filter.
// Used by MainActivity as Tab 2. Connects to FirebaseManager for events, DataRepository for recommendations.
class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var eventAdapter: EventAdapter
    private lateinit var trendingAdapter: EventAdapter
    private lateinit var recommendationsAdapter: EventAdapter
    private lateinit var cityAdapter: CityAdapter
    private var allEvents: List<Event> = emptyList()

    private var selectedCategory: String = "all"
    private var selectedSubcategory: String? = null
    private var selectedCity: String = "all"

    private val searchDebounceHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private var searchWatcher: TextWatcher? = null

    companion object {
        private const val ALL_CITIES = "all"
    }

    private val chipCategoryMap by lazy {
        mapOf(
            R.id.search_CHIP_all to "all",
            R.id.search_CHIP_music to "Music",
            R.id.search_CHIP_comedy to "Comedy",
            R.id.search_CHIP_sport to "Sport",
            R.id.search_CHIP_theater to "Theater",
            R.id.search_CHIP_children to "Children",
            R.id.search_CHIP_festival to "Festival"
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCategoryChips()
        setupCityChips()
        setupRecyclerViews()
        setupSearchInput()
        setupSearchHistory()
        restoreSavedFilters()
        binding.searchSRLRefresh.setColorSchemeResources(R.color.accent_primary)
        binding.searchSRLRefresh.setOnRefreshListener {
            // Dead code wired: refresh city list on pull-to-refresh
            val cityNames = listOf(ALL_CITIES) + IsraeliLocations.cities.map { it.name }
            cityAdapter.updateCities(cityNames)
            loadEvents()
            loadRecommendations()
        }
        viewLifecycleOwner.lifecycleScope.launch {
            loadEvents()
            loadRecommendations()
        }
    }

    // --- Category Chips ---

    private fun setupCategoryChips() {
        binding.searchCHIPGCategory.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: R.id.search_CHIP_all
            selectedCategory = chipCategoryMap[checkedId] ?: "all"
            selectedSubcategory = null
            updateSubcategoryChips()
            applyFilters()
        }
    }

    // --- Subcategory Chips ---

    private fun updateSubcategoryChips() {
        val chipGroup = binding.searchCHIPGSubcategory
        chipGroup.removeAllViews()

        if (selectedCategory == "all") {
            binding.searchHSVSubcategories.visibility = View.GONE
            return
        }

        val subcategories = Constants.SubCategories.getSubCategories(selectedCategory)
        if (subcategories.isEmpty() || subcategories.size <= 1) {
            binding.searchHSVSubcategories.visibility = View.GONE
            return
        }

        val allChip = Chip(requireContext()).apply {
            text = getString(R.string.all)
            isCheckable = true
            isChecked = true
            isCheckedIconVisible = false
            setChipBackgroundColorResource(R.color.background_card)
            setChipStrokeColorResource(R.color.accent_secondary)
            chipStrokeWidth = 1f * resources.displayMetrics.density
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
        }
        allChip.setOnClickListener {
            selectedSubcategory = null
            for (i in 0 until chipGroup.childCount) {
                val child = chipGroup.getChildAt(i)
                if (child is Chip) child.isChecked = child == allChip
            }
            applyFilters()
        }
        chipGroup.addView(allChip)

        subcategories.forEach { subcat ->
            val chip = Chip(requireContext()).apply {
                text = subcat
                isCheckable = true
                isCheckedIconVisible = false
                setChipBackgroundColorResource(R.color.background_card)
                setChipStrokeColorResource(R.color.accent_secondary)
                chipStrokeWidth = 1f * resources.displayMetrics.density
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            }
            chip.setOnClickListener {
                selectedSubcategory = subcat
                allChip.isChecked = false
                for (i in 1 until chipGroup.childCount) {
                    val child = chipGroup.getChildAt(i)
                    if (child is Chip && child != chip) child.isChecked = false
                }
                chip.isChecked = true
                applyFilters()
            }
            chipGroup.addView(chip)
        }

        binding.searchHSVSubcategories.visibility = View.VISIBLE
    }

    // --- City Chips ---

    private fun setupCityChips() {
        val cityNames = listOf(ALL_CITIES) + IsraeliLocations.cities.map { it.name }
        cityAdapter = CityAdapter(cityNames) { city ->
            selectedCity = city
            cityAdapter.setSelectedCity(city)
            applyFilters()
        }

        binding.searchRVCities.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = cityAdapter
        }
    }

    // --- RecyclerViews ---

    private fun createEventCallback(adapter: EventAdapter): EventCallback =
        EventAdapter.createStandardCallback(this, adapter, binding.root, viewLifecycleOwner.lifecycleScope)

    private fun setupRecyclerViews() {
        eventAdapter = EventAdapter()
        eventAdapter.eventCallback = createEventCallback(eventAdapter)
        binding.searchRVResults.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = eventAdapter
        }

        trendingAdapter = EventAdapter()
        trendingAdapter.eventCallback = createEventCallback(trendingAdapter)
        binding.searchRVTrending.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = trendingAdapter
            setHasFixedSize(false)
        }

        recommendationsAdapter = EventAdapter()
        recommendationsAdapter.eventCallback = createEventCallback(recommendationsAdapter)
        binding.searchRVRecommendations.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = recommendationsAdapter
            setHasFixedSize(false)
        }
    }

    // --- Search Input ---

    private fun setupSearchInput() {
        searchWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchRunnable?.let { searchDebounceHandler.removeCallbacks(it) }
                val runnable = Runnable { applyFilters() }
                searchRunnable = runnable
                searchDebounceHandler.postDelayed(runnable, 300)
            }
        }
        binding.searchEDTQuery.addTextChangedListener(searchWatcher)
    }

    // --- Data Loading ---

    private fun loadEvents() {
        DataRepository.getEvents { events, _ ->
            viewLifecycleOwner.lifecycleScope.launch {
                if (_binding == null) return@launch
                allEvents = events ?: emptyList()
                updateTrendingSection()
                applyFilters()
                _binding?.searchSRLRefresh?.isRefreshing = false
            }
        }
    }

    private fun updateTrendingSection() {
        val b = _binding ?: return
        val hotEvents = allEvents.filter { it.isHot && DateFormatter.isEventDateTodayOrFuture(it.date) }
        trendingAdapter.submitList(hotEvents)

        b.searchLAYOUTTrending.visibility =
            if (hotEvents.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun loadRecommendations() {
        val preferredCategories = MyApp.sharedPrefsManager.getPreferredCategories()
        if (preferredCategories.isEmpty()) {
            _binding?.searchLAYOUTRecommendations?.visibility = View.GONE
            return
        }

        DataRepository.getRecommendedEvents(preferredCategories) { events ->
            viewLifecycleOwner.lifecycleScope.launch {
                val b = _binding ?: return@launch
                recommendationsAdapter.submitList(events)
                b.searchLBLNoRecommendations.visibility =
                    if (events.isEmpty()) View.VISIBLE else View.GONE
                b.searchRVRecommendations.visibility =
                    if (events.isEmpty()) View.GONE else View.VISIBLE
                b.searchLAYOUTRecommendations.visibility =
                    if (events.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }

    // --- Apply All Filters ---

    private fun applyFilters() {
        val b = _binding ?: return
        val query = b.searchEDTQuery.text?.toString()?.trim() ?: ""

        val finalFiltered = allEvents.filter { event ->
            // Date filter — never show past events
            DateFormatter.isEventDateTodayOrFuture(event.date) &&
            // Text search
            (query.isEmpty() ||
                    event.title.contains(query, ignoreCase = true) ||
                    event.category.contains(query, ignoreCase = true) ||
                    event.subcategory.contains(query, ignoreCase = true) ||
                    event.location.contains(query, ignoreCase = true) ||
                    event.venue.contains(query, ignoreCase = true)) &&
            // Category filter
            (selectedCategory == "all" ||
                    event.category.equals(selectedCategory, ignoreCase = true) ||
                    event.category.contains(selectedCategory, ignoreCase = true)) &&
            // Subcategory filter
            (selectedSubcategory == null ||
                    event.subcategory.equals(selectedSubcategory, ignoreCase = true)) &&
            // City filter — match by location name or venue names in that city
            (selectedCity == ALL_CITIES ||
                    event.location.contains(selectedCity, ignoreCase = true) ||
                    IsraeliLocations.getVenuesByCity(selectedCity).any { venue ->
                        event.venue.contains(venue.name, ignoreCase = true)
                    })
        }

        eventAdapter.submitList(finalFiltered)

        // Save query to search history if it produced results
        if (query.isNotEmpty() && finalFiltered.isNotEmpty()) {
            saveQueryToHistory(query)
        }

        // Persist current filter state
        saveCurrentFilters()

        val filterOptions = FilterOptions(
            selectedCity = if (selectedCity == ALL_CITIES) "" else selectedCity,
            selectedCategories = if (selectedCategory == "all") mutableListOf() else mutableListOf(selectedCategory)
        )
        val activeFilterCount = filterOptions.getActiveFilterCount()

        val hasActiveFilter = query.isNotEmpty() || selectedCategory != "all" ||
                selectedCity != ALL_CITIES || selectedSubcategory != null

        // Hide search history when there's an active query
        b.searchLAYOUTHistory.visibility = if (query.isEmpty() && !hasActiveFilter) {
            if (MyApp.sharedPrefsManager.getSearchHistory().isNotEmpty()) View.VISIBLE else View.GONE
        } else View.GONE

        if (!hasActiveFilter) {
            b.searchLAYOUTEmpty.visibility = View.GONE
            b.searchLAYOUTTrending.visibility =
                if (allEvents.any { it.isHot && DateFormatter.isEventDateTodayOrFuture(it.date) }) View.VISIBLE else View.GONE
            b.searchLAYOUTRecommendations.visibility = View.VISIBLE
            b.searchRVResults.visibility = View.VISIBLE
            b.searchLBLResultsCount.visibility = View.VISIBLE
            b.searchLBLResultsCount.text = getString(R.string.search_results_count, allEvents.size)
        } else if (finalFiltered.isEmpty()) {
            b.searchLBLResultsCount.visibility = View.GONE
            b.searchRVResults.visibility = View.GONE
            b.searchLAYOUTTrending.visibility = View.GONE
            b.searchLAYOUTRecommendations.visibility = View.GONE
            b.searchLAYOUTEmpty.visibility = View.VISIBLE
            b.searchLBLEmptyMessage.text =
                getString(R.string.no_results_for_query, if (query.isNotEmpty()) query else selectedCategory)
        } else {
            b.searchLAYOUTEmpty.visibility = View.GONE
            b.searchLAYOUTTrending.visibility = View.GONE
            b.searchLAYOUTRecommendations.visibility = View.GONE
            b.searchRVResults.visibility = View.VISIBLE
            b.searchLBLResultsCount.visibility = View.VISIBLE
            val countText = getString(R.string.search_results_count, finalFiltered.size)
            b.searchLBLResultsCount.text = if (activeFilterCount > 0) {
                "$countText (${getString(R.string.apply_filters_count, activeFilterCount)})"
            } else countText
            b.searchLBLResultsCount.contentDescription = getString(R.string.apply_filters)
        }
    }

    // --- Search History ---

    private fun setupSearchHistory() {
        loadSearchHistory()
        // Merge cloud-synced User.searchHistory into the local prefs list so users see
        // their history across devices. The local list is the source of truth for display.
        DataRepository.getUserProfile { user, _ ->
            if (_binding == null || user == null) return@getUserProfile
            val cloud = user.searchHistory
            if (cloud.isEmpty()) return@getUserProfile
            val prefs = MyApp.sharedPrefsManager
            val merged = (prefs.getSearchHistory() + cloud).distinct().take(10)
            prefs.saveSearchHistory(merged)
            viewLifecycleOwner.lifecycleScope.launch { loadSearchHistory() }
        }
        binding.searchBTNClearHistory.setOnClickListener {
            MyApp.sharedPrefsManager.clearSearchHistory()
            binding.searchCHIPGHistory.removeAllViews()
            binding.searchLAYOUTHistory.visibility = View.GONE
            // Also clear the cloud copy so the next sign-in starts empty.
            DataRepository.updateUser(mapOf("searchHistory" to emptyList<String>())) { _, _ -> }
        }
    }

    private fun loadSearchHistory() {
        val history = MyApp.sharedPrefsManager.getSearchHistory()
        val b = _binding ?: return
        b.searchCHIPGHistory.removeAllViews()

        if (history.isEmpty()) {
            b.searchLAYOUTHistory.visibility = View.GONE
            return
        }

        b.searchLAYOUTHistory.visibility = View.VISIBLE
        history.take(8).forEach { query ->
            val chip = Chip(requireContext()).apply {
                text = query
                isCloseIconVisible = false
                isCheckable = false
                setChipBackgroundColorResource(R.color.background_card)
                setChipStrokeColorResource(R.color.stroke_subtle)
                chipStrokeWidth = 1f * resources.displayMetrics.density
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            }
            chip.setOnClickListener {
                b.searchEDTQuery.setText(query)
                b.searchEDTQuery.setSelection(query.length)
                applyFilters()
            }
            b.searchCHIPGHistory.addView(chip)
        }
    }

    private fun saveQueryToHistory(query: String) {
        if (query.length < 2) return
        val prefs = MyApp.sharedPrefsManager
        val history = prefs.getSearchHistory().toMutableList()
        history.remove(query)
        history.add(0, query)
        val trimmed = history.take(10)
        prefs.saveSearchHistory(trimmed)
        // Persist the same list to User.searchHistory so it follows the user across devices.
        DataRepository.updateUser(mapOf("searchHistory" to trimmed)) { _, _ -> }
    }

    // --- Filter Persistence ---

    private fun restoreSavedFilters() {
        val saved = MyApp.sharedPrefsManager.getFilters() ?: return
        if (!saved.hasFilters()) return

        if (saved.selectedCity.isNotEmpty()) {
            selectedCity = saved.selectedCity
        }
        if (saved.selectedCategories.isNotEmpty()) {
            selectedCategory = saved.selectedCategories.firstOrNull() ?: "all"
        }
    }

    private fun saveCurrentFilters() {
        val filters = FilterOptions(
            selectedCity = if (selectedCity == ALL_CITIES) "" else selectedCity,
            selectedCategories = if (selectedCategory == "all") mutableListOf() else mutableListOf(selectedCategory)
        )
        MyApp.sharedPrefsManager.saveFilters(filters)
    }

    override fun onDestroyView() {
        _binding?.searchEDTQuery?.removeTextChangedListener(searchWatcher)
        searchRunnable?.let { searchDebounceHandler.removeCallbacks(it) }
        searchDebounceHandler.removeCallbacksAndMessages(null)
        searchRunnable = null
        searchWatcher = null
        _binding?.searchRVResults?.adapter = null
        _binding?.searchRVTrending?.adapter = null
        _binding?.searchRVRecommendations?.adapter = null
        _binding?.searchRVCities?.adapter = null
        super.onDestroyView()
        _binding = null
    }
}