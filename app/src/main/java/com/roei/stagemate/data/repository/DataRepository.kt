package com.roei.stagemate.data.repository

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ListenerRegistration
import com.roei.stagemate.R
import com.roei.stagemate.data.models.AppNotification
import com.roei.stagemate.data.models.Category
import com.roei.stagemate.data.models.Event
import com.roei.stagemate.data.models.IsraeliEventsGenerator
import com.roei.stagemate.data.models.Seat
import com.roei.stagemate.data.models.SeatSection
import com.roei.stagemate.data.models.Ticket
import com.roei.stagemate.data.models.User
import com.roei.stagemate.utilities.Constants
import com.roei.stagemate.MyApp
import com.roei.stagemate.utilities.FirebaseManager

// Wraps FirebaseManager with local event fallback.
// Used by all screens to load events, tickets, users, and notifications.
object DataRepository {

    fun getEvents(callback: (List<Event>?, String?) -> Unit) {
        FirebaseManager.getEvents { events, _ ->
            if (events != null && events.isNotEmpty()) {
                callback(events, null)
            } else {
                callback(IsraeliEventsGenerator.getCachedEvents(), null)
            }
        }
    }

    fun getEventsByIds(eventIds: List<String>, callback: (List<Event>) -> Unit) {
        FirebaseManager.getEventsByIds(eventIds) { events ->
            if (events.isNotEmpty()) {
                callback(events)
            } else {
                // Fallback: filter local events by ID
                val localEvents = IsraeliEventsGenerator.getCachedEvents()
                callback(localEvents.filter { it.id in eventIds })
            }
        }
    }

    fun getRecommendedEvents(categories: Set<String>, callback: (List<Event>) -> Unit) {
        FirebaseManager.getRecommendedEvents(categories, callback)
    }

    fun getUserProfile(callback: (User?, String?) -> Unit) {
        FirebaseManager.getUser(callback)
    }

    fun saveUserProfile(user: User, callback: (Boolean, String?) -> Unit) {
        FirebaseManager.saveUser(user, callback)
    }

    fun listenToMyTickets(callback: (List<Ticket>) -> Unit): ListenerRegistration? {
        return FirebaseManager.listenToMyTickets(callback)
    }

    fun saveTicket(ticket: Ticket, callback: (Boolean, String?) -> Unit) {
        FirebaseManager.saveTicket(ticket, callback)
    }

    fun listenToFavoriteEvents(callback: (List<Event>) -> Unit): ListenerRegistration? {
        return FirebaseManager.listenToFavoriteEvents(callback)
    }

    fun listenToNotifications(callback: (List<AppNotification>) -> Unit): ListenerRegistration? {
        return FirebaseManager.listenToMyNotifications(callback)
    }

    fun markNotificationAsRead(notificationId: String, callback: (Boolean, String?) -> Unit) {
        FirebaseManager.markNotificationAsRead(notificationId, callback)
    }

    fun deleteNotification(notificationId: String, callback: (Boolean, String?) -> Unit) {
        FirebaseManager.deleteNotification(notificationId, callback)
    }

    fun deleteAllNotifications(callback: (Boolean, String?) -> Unit) {
        FirebaseManager.deleteAllNotifications(callback)
    }

    fun saveNotification(notification: AppNotification, callback: (Boolean, String?) -> Unit) {
        FirebaseManager.saveNotification(notification, callback)
    }

    fun saveTicketRating(ticketId: String, rating: Float, callback: (Boolean, String?) -> Unit) {
        FirebaseManager.updateTicketRating(ticketId, rating, callback)
    }

    fun updateEventRating(eventId: String, rating: Float, callback: (Boolean, String?) -> Unit) {
        FirebaseManager.updateEventAverageRating(eventId, rating, callback)
    }

    // --- Auth ---
    fun getCurrentUser(): FirebaseUser? = FirebaseManager.getCurrentUser()

    // Falls back to SharedPrefs cached ID if Firebase auth is unavailable (e.g., offline cold start)
    fun getCurrentUserId(): String? =
        FirebaseManager.getCurrentUser()?.uid ?: MyApp.sharedPrefsManager.getUserId()
    fun getCurrentUserEmail(): String? = FirebaseManager.getCurrentUser()?.email
    fun getCurrentUserDisplayName(): String? = FirebaseManager.getCurrentUser()?.displayName
    fun isLoggedIn(): Boolean = FirebaseManager.getCurrentUser() != null
    fun signOut() = FirebaseManager.signOut()
    fun signIn(email: String, password: String, callback: (Boolean, String?) -> Unit) =
        FirebaseManager.signIn(email, password, callback)
    fun signUp(email: String, password: String, callback: (Boolean, String?) -> Unit) =
        FirebaseManager.signUp(email, password, callback)
    fun resetPassword(email: String, callback: (Boolean, String?) -> Unit) =
        FirebaseManager.resetPassword(email, callback)
    fun loadPreferredCategories(callback: (Set<String>) -> Unit) =
        FirebaseManager.loadPreferredCategories(callback)
    fun savePreferredCategories(categories: Set<String>, callback: (Boolean, String?) -> Unit) =
        FirebaseManager.savePreferredCategories(categories, callback)

    // --- Events ---
    fun seedEventsIfEmpty(onComplete: () -> Unit = {}) =
        FirebaseManager.seedEventsIfEmpty(onComplete)

    fun refreshExpiredEvents(onComplete: () -> Unit = {}) =
        FirebaseManager.refreshExpiredEvents(onComplete)

    fun loadHotEvents(onResult: (List<Event>) -> Unit) =
        FirebaseManager.loadHotEvents(onResult)

    fun startLiveEventsListener(callback: (List<Event>) -> Unit) =
        FirebaseManager.startLiveEventsListener(callback)

    fun stopLiveEventsListener() = FirebaseManager.stopLiveEventsListener()

    // --- Favorites cache lifecycle (called from MainActivity) ---
    fun startFavoritesCacheListener() = FirebaseManager.startFavoritesCacheListener()
    fun stopFavoritesCacheListener() = FirebaseManager.stopFavoritesCacheListener()

    // --- User settings ---
    fun loadUserSettings(callback: (Map<String, Any>) -> Unit) =
        FirebaseManager.loadUserSettings(callback)

    fun saveUserSettings(settings: Map<String, Any>, callback: (Boolean, String?) -> Unit) =
        FirebaseManager.saveUserSettings(settings, callback)

    fun listenToUser(callback: (User?) -> Unit): ListenerRegistration? =
        FirebaseManager.listenToUser(callback)

    fun updateUser(updates: Map<String, Any>, callback: (Boolean, String?) -> Unit) =
        FirebaseManager.updateUser(updates, callback)

    // --- Recently viewed / FCM ---
    fun loadRecentlyViewed(callback: (List<String>) -> Unit) =
        FirebaseManager.loadRecentlyViewed(callback)

    fun saveUserFCMToken(token: String, callback: (Boolean, String?) -> Unit) =
        FirebaseManager.saveUserFCMToken(token, callback)

    fun sendEmail(
        toEmail: String,
        subject: String,
        htmlBody: String,
        plainTextBody: String,
        callback: (Boolean, String?) -> Unit
    ) {
        FirebaseManager.sendEmail(toEmail, subject, htmlBody, plainTextBody, callback)
    }

    // --- Event details ---
    fun listenToEvent(eventId: String, callback: (Event?) -> Unit): ListenerRegistration =
        FirebaseManager.listenToEvent(eventId, callback)

    fun incrementViewCount(eventId: String) =
        FirebaseManager.incrementViewCount(eventId)

    fun saveRecentlyViewed(eventIds: List<String>, callback: (Boolean, String?) -> Unit) =
        FirebaseManager.saveRecentlyViewed(eventIds, callback)

    fun toggleFavorite(eventId: String, isFavorite: Boolean, callback: (Boolean) -> Unit) =
        FirebaseManager.toggleFavorite(eventId, isFavorite, callback)

    fun loadAlternativeDates(eventId: String, callback: (List<Event>, String?) -> Unit) =
        FirebaseManager.loadAlternativeDates(eventId, callback)

    // --- City / Password ---
    fun loadSelectedCity(callback: (String?) -> Unit) =
        FirebaseManager.loadSelectedCity(callback)

    fun saveSelectedCity(city: String, callback: (Boolean, String?) -> Unit) =
        FirebaseManager.saveSelectedCity(city, callback)

    fun changePassword(currentPassword: String, newPassword: String, callback: (Boolean, String?) -> Unit) =
        FirebaseManager.changePassword(currentPassword, newPassword, callback)

    // --- Seats ---
    fun listenToSeatUpdates(eventId: String, callback: (List<Seat>) -> Unit): ListenerRegistration =
        FirebaseManager.listenToSeatUpdates(eventId, callback)

    fun initializeEventSeats(eventId: String, sections: List<SeatSection>, callback: (Boolean, String?) -> Unit) =
        FirebaseManager.initializeEventSeats(eventId, sections, callback)

    fun reserveSeats(eventId: String, seats: List<Seat>, userId: String, callback: (Boolean, String?) -> Unit) =
        FirebaseManager.reserveSeats(eventId, seats, userId, callback = callback)

    fun releaseSeats(eventId: String, seatIds: List<String>, callback: (Boolean, String?) -> Unit) =
        FirebaseManager.releaseSeats(eventId, seatIds, callback)

    // --- Purchase ---
    fun purchaseTicketAtomic(eventId: String, ticket: Ticket, quantity: Int, onResult: (Boolean, String?) -> Unit) =
        FirebaseManager.purchaseTicketAtomic(eventId, ticket, quantity, onResult)

    fun getCategories(): List<Category> {
        return listOf(
            Category("All",      Constants.Categories.ALL,      R.drawable.category_all,      isSelected = true),
            Category("Music",    Constants.Categories.MUSIC,    R.drawable.category_music),
            Category("Stand-Up", Constants.Categories.STAND_UP, R.drawable.category_comedy),
            Category("Sport",    Constants.Categories.SPORT,    R.drawable.category_sport),
            Category("Theater",  Constants.Categories.THEATER,  R.drawable.category_theater),
            Category("Children", Constants.Categories.CHILDREN, R.drawable.category_children),
            Category("Festival", Constants.Categories.FESTIVAL, R.drawable.category_music)
        )
    }

}