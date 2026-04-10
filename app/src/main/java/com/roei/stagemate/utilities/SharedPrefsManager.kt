package com.roei.stagemate.utilities

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.roei.stagemate.data.models.FilterOptions

// Local SharedPreferences storage for user session, filters, search history, and settings.
// Used by LoginActivity, ProfileFragment, SearchFragment, and FilterActivity.
class SharedPrefsManager(context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        Constants.SharedPrefs.PREFS_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_HAS_SEEN_ONBOARDING = "has_seen_onboarding"
        private const val KEY_FIRST_TIME_LOGIN = "first_time_login"
        private const val KEY_TRENDING_NOTIFICATIONS_ENABLED = "trending_notifications_enabled"
        private const val KEY_SEARCH_HISTORY = "search_history"
        private const val KEY_RECENTLY_VIEWED_IDS = "recently_viewed_ids"
        private const val KEY_FILTERS = "filters"
    }

    fun saveUserId(userId: String) {
        sharedPreferences.edit().putString(Constants.SharedPrefs.KEY_USER_ID, userId).apply()
    }

    fun getUserId(): String? {
        return sharedPreferences.getString(Constants.SharedPrefs.KEY_USER_ID, null)
    }

    fun saveUserEmail(email: String) {
        sharedPreferences.edit().putString(Constants.SharedPrefs.KEY_USER_EMAIL, email).apply()
    }

    fun getUserEmail(): String? {
        return sharedPreferences.getString(Constants.SharedPrefs.KEY_USER_EMAIL, null)
    }

    fun saveUserName(name: String) {
        sharedPreferences.edit().putString(Constants.SharedPrefs.KEY_USER_NAME, name).apply()
    }

    fun getUserName(): String? {
        return sharedPreferences.getString(Constants.SharedPrefs.KEY_USER_NAME, null)
    }

    fun saveSelectedCity(city: String) {
        sharedPreferences.edit().putString(Constants.SharedPrefs.KEY_SELECTED_CITY, city).apply()
    }

    fun getSelectedCity(): String? {
        return sharedPreferences.getString(Constants.SharedPrefs.KEY_SELECTED_CITY, null)
    }

    fun setLoggedIn(isLoggedIn: Boolean) {
        sharedPreferences.edit().putBoolean(Constants.SharedPrefs.KEY_IS_LOGGED_IN, isLoggedIn).apply()
    }

    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(Constants.SharedPrefs.KEY_IS_LOGGED_IN, false)
    }

    fun setRememberMe(rememberMe: Boolean) {
        sharedPreferences.edit().putBoolean(Constants.SharedPrefs.KEY_REMEMBER_ME, rememberMe).apply()
    }

    fun isRememberMe(): Boolean {
        return sharedPreferences.getBoolean(Constants.SharedPrefs.KEY_REMEMBER_ME, false)
    }

    fun hasSeenOnboarding(): Boolean {
        return sharedPreferences.getBoolean(KEY_HAS_SEEN_ONBOARDING, false)
    }

    fun setHasSeenOnboarding(seen: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_HAS_SEEN_ONBOARDING, seen).apply()
    }

    fun setTrendingNotificationsEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_TRENDING_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    fun isTrendingNotificationsEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_TRENDING_NOTIFICATIONS_ENABLED, true)
    }

    fun isFirstTimeLogin(): Boolean {
        return sharedPreferences.getBoolean(KEY_FIRST_TIME_LOGIN, true)
    }

    fun setFirstTimeLogin(isFirstTime: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_FIRST_TIME_LOGIN, isFirstTime).apply()
    }

    fun savePreferredCategories(categories: Set<String>) {
        sharedPreferences.edit().putStringSet(Constants.SharedPrefs.PREFERRED_CATEGORIES, categories).apply()
    }

    fun getPreferredCategories(): Set<String> {
        return sharedPreferences.getStringSet(Constants.SharedPrefs.PREFERRED_CATEGORIES, emptySet()) ?: emptySet()
    }

    fun hasPreferredCategories(): Boolean {
        return getPreferredCategories().isNotEmpty()
    }

    fun clearAll() {
        sharedPreferences.edit().clear().apply()
    }

    // Clears data but preserves email if rememberMe is set, and ALWAYS preserves
    // the trending notifications preference so the user's choice survives logout.
    fun logout() {
        val rememberMe = isRememberMe()
        val email = if (rememberMe) getUserEmail() else null
        val trendingPref = isTrendingNotificationsEnabled()
        clearAll()
        if (rememberMe && email != null) {
            saveUserEmail(email)
            setRememberMe(true)
        }
        setTrendingNotificationsEnabled(trendingPref)
    }

    fun saveFilters(filterOptions: FilterOptions) {
        val json = Gson().toJson(filterOptions)
        sharedPreferences.edit().putString(KEY_FILTERS, json).apply()
    }

    fun getFilters(): FilterOptions? {
        val json = sharedPreferences.getString(KEY_FILTERS, null) ?: return null
        return try {
            Gson().fromJson(json, FilterOptions::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun clearFilters() {
        sharedPreferences.edit().remove(KEY_FILTERS).apply()
    }

    fun saveSearchHistory(history: List<String>) {
        sharedPreferences.edit()
            .putStringSet(KEY_SEARCH_HISTORY, history.toSet())
            .apply()
    }

    fun getSearchHistory(): List<String> {
        return sharedPreferences.getStringSet(KEY_SEARCH_HISTORY, emptySet())?.toList() ?: emptyList()
    }

    fun clearSearchHistory() {
        sharedPreferences.edit()
            .remove(KEY_SEARCH_HISTORY)
            .apply()
    }

    fun saveRecentlyViewed(eventId: String) {
        val current = getRecentlyViewedIds().toMutableList()
        current.remove(eventId)
        current.add(0, eventId)
        sharedPreferences.edit()
            .putString(KEY_RECENTLY_VIEWED_IDS, current.take(5).joinToString(","))
            .apply()
    }

    fun getRecentlyViewedIds(): List<String> {
        val stored = sharedPreferences.getString(KEY_RECENTLY_VIEWED_IDS, "") ?: ""
        return if (stored.isEmpty()) emptyList() else stored.split(",")
    }

    fun saveString(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    fun getString(key: String, defaultValue: String = ""): String {
        return sharedPreferences.getString(key, defaultValue) ?: defaultValue
    }

    fun putBoolean(key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }
}