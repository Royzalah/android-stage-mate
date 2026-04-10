package com.roei.stagemate.data.models

// User profile model with auth info, favorites, purchase history, and preferences.
// Used by ProfileFragment, LoginActivity, SignUpActivity, FirebaseManager, and SharedPrefsManager.
data class User(
    var id: String = "",
    var email: String = "",
    var name: String = "",
    var firstName: String = "",
    var lastName: String = "",
    var phone: String = "",
    var city: String = "",
    var profileImageUrl: String = "",
    var isGoogleAuth: Boolean = false,
    var userType: UserType = UserType.REGULAR,
    var favoriteEvents: MutableList<String> = mutableListOf(),
    var purchasedTickets: MutableList<String> = mutableListOf(),
    var preferredCategories: MutableList<String> = mutableListOf(),
    var selectedCity: String = "",
    var recentlyViewed: MutableList<String> = mutableListOf(),
    var searchHistory: MutableList<String> = mutableListOf(),
    var trendingNotificationsEnabled: Boolean = true,
    var rememberMe: Boolean = false
) {

    enum class UserType {
        REGULAR
    }

}