package com.roei.stagemate.ui.activities

import android.content.Intent
import android.os.Bundle
import com.roei.stagemate.utilities.showErrorSnackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.roei.stagemate.R
import com.roei.stagemate.ui.adapters.CategorySelectionAdapter
import com.roei.stagemate.databinding.ActivityPreferredCategoriesBinding
import com.roei.stagemate.data.repository.DataRepository
import com.roei.stagemate.utilities.Constants
import com.roei.stagemate.MyApp

// Category picker shown after first login — user must pick at least 3.
// Saves to SharedPrefs and Firebase, then goes to MainActivity.
class PreferredCategoriesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPreferredCategoriesBinding
    private lateinit var adapter: CategorySelectionAdapter
    private val MIN_SELECTION = 3
    private var fromProfile = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreferredCategoriesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fromProfile = intent.getBooleanExtra("from_profile", false)

        initViews()
    }

    private fun initViews() {
        val selectableCategories = DataRepository.getCategories()

        adapter = CategorySelectionAdapter(selectableCategories) { category ->
            handleCategoryClick(category.id)
        }

        binding.preferredRVCategories.apply {
            layoutManager = GridLayoutManager(this@PreferredCategoriesActivity, 2)
            adapter = this@PreferredCategoriesActivity.adapter
        }

        val savedCategories = MyApp.sharedPrefsManager.getPreferredCategories()
        if (savedCategories.isNotEmpty()) {
            adapter.setSelectedCategories(savedCategories.toList())
            updateSelectedCount()
        }

        binding.preferredBTNContinue.setOnClickListener { savePreferences() }

        if (fromProfile) {
            binding.preferredCVTrending.visibility = android.view.View.GONE
            binding.preferredBTNContinue.text = getString(R.string.save_btn)
        }
    }

    private fun isAllSelected(): Boolean {
        return adapter.getSelectedCategories().any { it.equals(Constants.Categories.ALL, ignoreCase = true) }
    }

    private fun handleCategoryClick(categoryId: String) {
        adapter.toggleCategory(categoryId)
        updateSelectedCount()
    }

    private fun updateSelectedCount() {
        val count = adapter.getSelectedCategories().size
        binding.preferredLBLSelectedCount.text = getString(R.string.categories_selected_count_dynamic, count, MIN_SELECTION)
        binding.preferredBTNContinue.isEnabled = isAllSelected() || count >= MIN_SELECTION
    }

    private fun savePreferences() {
        val selectedCategories = adapter.getSelectedCategories()

        if (!isAllSelected() && selectedCategories.size < MIN_SELECTION) {
            showErrorSnackbar(getString(R.string.select_categories_hint))
            return
        }

        MyApp.sharedPrefsManager.savePreferredCategories(selectedCategories.toSet())
        DataRepository.savePreferredCategories(selectedCategories.toSet()) { _, _ -> }

        val trendingEnabled = binding.preferredSWITCHTrending.isChecked
        MyApp.sharedPrefsManager.setTrendingNotificationsEnabled(trendingEnabled)

        if (fromProfile) {
            finish()
        } else {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
