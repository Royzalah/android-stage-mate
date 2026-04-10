package com.roei.stagemate.utilities

import android.content.ContentValues.TAG
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.roei.stagemate.data.models.SeatSection
import com.roei.stagemate.data.models.Event
import com.roei.stagemate.data.models.IsraeliEventsGenerator
import com.roei.stagemate.data.models.Ticket
import com.roei.stagemate.data.models.User
import com.roei.stagemate.data.models.AppNotification
import com.roei.stagemate.data.models.Seat

// All raw Firebase CRUD operations (Auth, Firestore, RTDB).
// Used by DataRepository, FavoriteHelper, and all Activities/Fragments.
object FirebaseManager {

    private const val SEED_VERSION = 32
    private const val COLLECTION_EVENTS = "events"
    private const val BATCH_CHUNK_SIZE = 450  // Firebase batch limit is 500; use 450 for safety

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val rtdb by lazy {
        FirebaseDatabase.getInstance("https://stagemate-89f9c-default-rtdb.europe-west1.firebasedatabase.app")
            .reference
    }

    private var eventsListenerRegistration: ListenerRegistration? = null

    // In-memory favorites cache, kept in sync via a single user-document listener.
    // Eliminates the N+1 query pattern where every event refresh fetched the user doc.
    private var cachedFavoriteIds: Set<String> = emptySet()
    private var favoritesCacheListener: ListenerRegistration? = null

    // Starts a listener on the user doc to keep cachedFavoriteIds in sync.
    // Call once after sign-in and again on user change. Safe to call multiple times.
    fun startFavoritesCacheListener() {
        favoritesCacheListener?.remove()
        favoritesCacheListener = null
        cachedFavoriteIds = emptySet()
        val userId = getCurrentUser()?.uid ?: return
        favoritesCacheListener = db.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                @Suppress("UNCHECKED_CAST")
                val ids = (snapshot.get("favoriteEvents") as? List<String>)
                    ?.filter { it.isNotBlank() }
                    ?.toHashSet()
                    ?: hashSetOf()
                cachedFavoriteIds = ids
            }
    }

    fun stopFavoritesCacheListener() {
        favoritesCacheListener?.remove()
        favoritesCacheListener = null
        cachedFavoriteIds = emptySet()
    }

    // --- Auth ---

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun signIn(email: String, password: String, callback: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) callback(true, null)
                else callback(false, task.exception?.message)
            }
    }

    fun signUp(email: String, password: String, callback: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) callback(true, null)
                else callback(false, task.exception?.message)
            }
    }

    fun signOut() {
        auth.signOut()
    }

    fun resetPassword(email: String, callback: (Boolean, String?) -> Unit) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) callback(true, null)
                else callback(false, task.exception?.message)
            }
    }

    fun changePassword(currentPassword: String, newPassword: String, callback: (Boolean, String?) -> Unit) {
        val user = getCurrentUser() ?: run { callback(false, "User not logged in"); return }
        val email = user.email ?: run { callback(false, "Email not found"); return }

        val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, currentPassword)
        user.reauthenticate(credential)
            .addOnSuccessListener {
                user.updatePassword(newPassword)
                    .addOnSuccessListener { callback(true, null) }
                    .addOnFailureListener { callback(false, it.message) }
            }
            .addOnFailureListener { e ->
                // Distinguish wrong-password (FirebaseAuthInvalidCredentialsException)
                // from network/server errors so the user gets an accurate message.
                val msg = when (e) {
                    is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException ->
                        "Current password is incorrect"
                    is com.google.firebase.FirebaseNetworkException ->
                        "Network error — please check your connection"
                    else -> e.message ?: "Re-authentication failed"
                }
                callback(false, msg)
            }
    }

    // --- User Management ---

    fun saveUser(user: User, callback: (Boolean, String?) -> Unit) {
        val userId = getCurrentUser()?.uid ?: run { callback(false, "User not logged in"); return }
        db.collection("users").document(userId).set(user)
            .addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { callback(false, it.message) }
        rtdb.child("users").child(userId).setValue(mapOf(
            "email" to user.email,
            "name" to user.name,
            "firstName" to user.firstName,
            "lastName" to user.lastName,
            "phone" to user.phone,
            "city" to user.city,
            "isGoogleAuth" to user.isGoogleAuth
        ))
    }

    fun getUser(callback: (User?, String?) -> Unit) {
        val userId = getCurrentUser()?.uid ?: run { callback(null, "User not logged in"); return }
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) callback(document.toObject(User::class.java), null)
                else callback(null, "User not found")
            }
            .addOnFailureListener { callback(null, it.message) }
    }

    fun updateUser(updates: Map<String, Any>, callback: (Boolean, String?) -> Unit) {
        val userId = getCurrentUser()?.uid ?: run { callback(false, "User not logged in"); return }
        db.collection("users").document(userId).update(updates)
            .addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { callback(false, it.message) }
    }

    fun listenToUser(callback: (User?) -> Unit): ListenerRegistration? {
        val userId = getCurrentUser()?.uid ?: return null
        return db.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { callback(null); return@addSnapshotListener }
                if (snapshot != null && snapshot.exists()) callback(snapshot.toObject(User::class.java))
                else callback(null)
            }
    }

    fun saveUserFCMToken(token: String, callback: (Boolean, String?) -> Unit) {
        val userId = getCurrentUser()?.uid ?: run { callback(false, "User not logged in"); return }
        db.collection("users").document(userId).update("fcmToken", token)
            .addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { callback(false, it.message) }
    }

    fun savePreferredCategories(categories: Set<String>, callback: (Boolean, String?) -> Unit) {
        val userId = getCurrentUser()?.uid ?: run { callback(false, "User not logged in"); return }
        val data = categories.toList()
        db.collection("users").document(userId).update("preferredCategories", data)
            .addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { callback(false, it.message) }
        rtdb.child("users").child(userId).child("preferredCategories").setValue(data)
    }

    fun loadPreferredCategories(callback: (Set<String>) -> Unit) {
        val userId = getCurrentUser()?.uid ?: run { callback(emptySet()); return }
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                @Suppress("UNCHECKED_CAST")
                val categories = (doc.get("preferredCategories") as? List<String>)?.toSet() ?: emptySet()
                callback(categories)
            }
            .addOnFailureListener { callback(emptySet()) }
    }

    fun saveSelectedCity(city: String, callback: (Boolean, String?) -> Unit) {
        val userId = getCurrentUser()?.uid ?: run { callback(false, "User not logged in"); return }
        db.collection("users").document(userId).update("selectedCity", city)
            .addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { callback(false, it.message) }
        rtdb.child("users").child(userId).child("selectedCity").setValue(city)
    }

    fun loadSelectedCity(callback: (String?) -> Unit) {
        val userId = getCurrentUser()?.uid ?: run { callback(null); return }
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc -> callback(doc.getString("selectedCity")) }
            .addOnFailureListener { callback(null) }
    }

    fun saveRecentlyViewed(eventIds: List<String>, callback: (Boolean, String?) -> Unit) {
        val userId = getCurrentUser()?.uid ?: run { callback(false, "User not logged in"); return }
        db.collection("users").document(userId).update("recentlyViewed", eventIds)
            .addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { callback(false, it.message) }
        rtdb.child("users").child(userId).child("recentlyViewed").setValue(eventIds)
    }

    fun loadRecentlyViewed(callback: (List<String>) -> Unit) {
        val userId = getCurrentUser()?.uid ?: run { callback(emptyList()); return }
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                @Suppress("UNCHECKED_CAST")
                val ids = (doc.get("recentlyViewed") as? List<String>) ?: emptyList()
                callback(ids)
            }
            .addOnFailureListener { callback(emptyList()) }
    }

    fun saveUserSettings(settings: Map<String, Any>, callback: (Boolean, String?) -> Unit) {
        val userId = getCurrentUser()?.uid ?: run { callback(false, "User not logged in"); return }
        // Use dot-notation so only the specified keys inside the "settings" map are
        // updated — the previous code replaced the ENTIRE map, wiping sibling keys.
        val dotUpdates = settings.mapKeys { (key, _) -> "settings.$key" }
        db.collection("users").document(userId).update(dotUpdates)
            .addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { e ->
                // If the user document doesn't exist yet, fall back to set-with-merge
                if (e.message?.contains("NOT_FOUND") == true || e.message?.contains("No document") == true) {
                    db.collection("users").document(userId)
                        .set(mapOf("settings" to settings), SetOptions.merge())
                        .addOnSuccessListener { callback(true, null) }
                        .addOnFailureListener { callback(false, it.message) }
                } else {
                    callback(false, e.message)
                }
            }
        rtdb.child("users").child(userId).child("settings").updateChildren(settings)
    }

    fun loadUserSettings(callback: (Map<String, Any>) -> Unit) {
        val userId = getCurrentUser()?.uid ?: run { callback(emptyMap()); return }
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                @Suppress("UNCHECKED_CAST")
                val settings = (doc.get("settings") as? Map<String, Any>) ?: emptyMap()
                callback(settings)
            }
            .addOnFailureListener { callback(emptyMap()) }
    }

    // --- Events ---

    fun getEvents(callback: (List<Event>?, String?) -> Unit) {
        db.collection(COLLECTION_EVENTS).get()
            .addOnSuccessListener { documents ->
                callback(documents.map { doc ->
                    doc.toObject(Event::class.java).also { it.id = doc.id }
                }, null)
            }
            .addOnFailureListener { callback(null, it.message) }
    }

    fun listenToEvent(eventId: String, callback: (Event?) -> Unit): ListenerRegistration {
        return db.collection(COLLECTION_EVENTS).document(eventId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { callback(null); return@addSnapshotListener }
                if (snapshot != null && snapshot.exists()) {
                    callback(snapshot.toObject(Event::class.java)?.also { it.id = snapshot.id })
                } else {
                    callback(null)
                }
            }
    }

    fun incrementViewCount(eventId: String) {
        db.collection("events").document(eventId).update("viewCount", FieldValue.increment(1))
    }

    fun getEventsByIds(eventIds: List<String>, callback: (List<Event>) -> Unit) {
        if (eventIds.isEmpty()) { callback(emptyList()); return }
        db.collection(COLLECTION_EVENTS).whereIn(FieldPath.documentId(), eventIds.take(10)).get()
            .addOnSuccessListener { docs ->
                val eventsMap = docs.associate { doc ->
                    doc.id to doc.toObject(Event::class.java).also { it.id = doc.id }
                }
                callback(eventIds.mapNotNull { eventsMap[it] })
            }
            .addOnFailureListener { callback(emptyList()) }
    }

    fun getRecommendedEvents(categories: Set<String>, callback: (List<Event>) -> Unit) {
        if (categories.isEmpty()) { callback(emptyList()); return }
        db.collection(COLLECTION_EVENTS).whereEqualTo("category", categories.first()).limit(5).get()
            .addOnSuccessListener { docs ->
                callback(docs.documents.mapNotNull { doc ->
                    doc.toObject(Event::class.java)?.also { it.id = doc.id }
                })
            }
            .addOnFailureListener { callback(emptyList()) }
    }

    fun loadAlternativeDates(eventId: String, callback: (List<Event>, String?) -> Unit) {
        db.collection(COLLECTION_EVENTS).document(eventId).get()
            .addOnSuccessListener { eventDoc ->
                if (!eventDoc.exists()) { callback(emptyList(), "Event not found"); return@addOnSuccessListener }
                val title = eventDoc.toObject(Event::class.java)?.also { it.id = eventDoc.id }?.title ?: ""
                db.collection(COLLECTION_EVENTS)
                    .whereEqualTo("title", title)
                    .whereGreaterThan("availableTickets", 0)
                    .limit(5).get()
                    .addOnSuccessListener { snapshot ->
                        val events = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(Event::class.java)?.also { it.id = doc.id }
                        }.filter { it.id != eventId }
                        callback(events, null)
                    }
                    .addOnFailureListener { callback(emptyList(), it.message) }
            }
            .addOnFailureListener { callback(emptyList(), it.message) }
    }

    // --- Favorites ---

    fun toggleFavorite(eventId: String, isFavorite: Boolean, callback: (Boolean) -> Unit) {
        val userId = getCurrentUser()?.uid ?: run { callback(false); return }
        val eventRef = db.collection("events").document(eventId)
        val userRef = db.collection("users").document(userId)

        db.runTransaction { transaction ->
            val increment = if (isFavorite) 1 else -1
            transaction.set(eventRef, mapOf("favoriteCount" to FieldValue.increment(increment.toLong())), SetOptions.merge())
            val favUpdate = if (isFavorite) mapOf("favoriteEvents" to FieldValue.arrayUnion(eventId))
                else mapOf("favoriteEvents" to FieldValue.arrayRemove(eventId))
            transaction.set(userRef, favUpdate, SetOptions.merge())
        }.addOnSuccessListener {
            val rtdbFavRef = rtdb.child("users").child(userId).child("favoriteEvents").child(eventId)
            if (isFavorite) rtdbFavRef.setValue(true) else rtdbFavRef.removeValue()
            callback(true)
        }.addOnFailureListener { callback(false) }
    }

    fun listenToFavoriteEvents(callback: (List<Event>) -> Unit): ListenerRegistration? {
        val userId = getCurrentUser()?.uid ?: return null
        return db.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) { callback(emptyList()); return@addSnapshotListener }
                @Suppress("UNCHECKED_CAST")
                val favoriteIds = (snapshot.get("favoriteEvents") as? List<String>)?.filter { it.isNotBlank() } ?: emptyList()
                if (favoriteIds.isEmpty()) { callback(emptyList()); return@addSnapshotListener }
                db.collection(COLLECTION_EVENTS).whereIn(FieldPath.documentId(), favoriteIds.take(10)).get()
                    .addOnSuccessListener { docs ->
                        callback(docs.documents.mapNotNull { doc ->
                            doc.toObject(Event::class.java)?.also {
                                it.id = doc.id
                                it.isFavorite = true
                            }
                        })
                    }
                    .addOnFailureListener { callback(emptyList()) }
            }
    }

    // --- Tickets ---

    fun saveTicket(ticket: Ticket, callback: (Boolean, String?) -> Unit) {
        val userId = getCurrentUser()?.uid ?: run { callback(false, "User not logged in"); return }
        val userRef = db.collection("users").document(userId)
        val ticketRef = userRef.collection("tickets").document(ticket.id)

        // Atomic write: ticket + purchasedTickets array, in a single transaction.
        // Prevents the previous race where the array update was fired-and-forgotten
        // after the success callback already returned true.
        db.runTransaction { transaction ->
            transaction.set(ticketRef, ticket)
            transaction.update(userRef, "purchasedTickets", FieldValue.arrayUnion(ticket.id))
        }
            .addOnSuccessListener {
                // RTDB shadow write — best-effort backup, logged on failure
                rtdb.child("users").child(userId).child("tickets").child(ticket.id).setValue(mapOf(
                    "eventId" to ticket.eventId,
                    "eventTitle" to ticket.eventTitle,
                    "eventDate" to ticket.eventDate,
                    "eventLocation" to ticket.eventLocation,
                    "ticketType" to ticket.ticketType.name,
                    "ticketStatus" to ticket.ticketStatus.name,
                    "price" to ticket.price,
                    "quantity" to ticket.quantity,
                    "seatNumbers" to ticket.seatNumbers,
                    "purchaseDate" to ticket.purchaseDate,
                    "bookingReference" to ticket.bookingReference
                )).addOnFailureListener { e ->
                    android.util.Log.w("FirebaseManager", "RTDB ticket shadow write failed", e)
                }
                callback(true, null)
            }
            .addOnFailureListener { e -> callback(false, e.message) }
    }

    fun listenToMyTickets(callback: (List<Ticket>) -> Unit): ListenerRegistration? {
        val userId = getCurrentUser()?.uid ?: return null
        return db.collection("users").document(userId).collection("tickets")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { callback(emptyList()); return@addSnapshotListener }
                callback(snapshot?.mapNotNull { doc ->
                    doc.toObject(Ticket::class.java).also { it.id = doc.id }
                } ?: emptyList())
            }
    }

    // --- Ratings ---

    fun updateTicketRating(ticketId: String, rating: Float, callback: (Boolean, String?) -> Unit) {
        val userId = getCurrentUser()?.uid ?: run { callback(false, "User not logged in"); return }
        db.collection("users").document(userId).collection("tickets").document(ticketId)
            .update(mapOf("rating" to rating, "isRated" to true))
            .addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { callback(false, it.message) }
    }

    fun updateEventAverageRating(eventId: String, newRating: Float, callback: (Boolean, String?) -> Unit) {
        val eventRef = db.collection(COLLECTION_EVENTS).document(eventId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(eventRef)
            val currentRating = snapshot.getDouble("rating")?.toFloat() ?: 0f
            val currentCount = snapshot.getLong("ratingCount")?.toInt() ?: 0
            val newCount = currentCount + 1
            val newAverage = ((currentRating * currentCount) + newRating) / newCount
            transaction.update(eventRef, mapOf("rating" to newAverage, "ratingCount" to newCount))
        }
            .addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { callback(false, it.message) }
    }

    // --- Notifications ---

    fun listenToMyNotifications(callback: (List<AppNotification>) -> Unit): ListenerRegistration? {
        val userId = getCurrentUser()?.uid ?: return null
        return db.collection("users").document(userId).collection("notifications")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { callback(emptyList()); return@addSnapshotListener }
                val notifications = snapshot?.mapNotNull { doc ->
                    try {
                        val type = try {
                            AppNotification.NotificationType.valueOf(doc.getString("type") ?: "INFO")
                        } catch (e: IllegalArgumentException) { AppNotification.NotificationType.INFO }

                        AppNotification.Builder()
                            .title(doc.getString("title") ?: "")
                            .message(doc.getString("message") ?: "")
                            .type(type)
                            .eventId(doc.getString("eventId") ?: "")
                            .imageUrl(doc.getString("imageUrl") ?: "")
                            .timestamp(doc.getTimestamp("timestamp")?.toDate()?.time ?: System.currentTimeMillis())
                            .build()
                            .also { it.id = doc.id }
                            .also { it.isRead = doc.getBoolean("isRead") ?: false }
                    } catch (e: Exception) { null }
                } ?: emptyList()
                callback(notifications)
            }
    }

    fun markNotificationAsRead(notificationId: String, callback: (Boolean, String?) -> Unit) {
        val userId = getCurrentUser()?.uid ?: run { callback(false, "User not logged in"); return }
        db.collection("users").document(userId).collection("notifications").document(notificationId)
            .update("isRead", true)
            .addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { callback(false, it.message) }
    }

    fun deleteNotification(notificationId: String, callback: (Boolean, String?) -> Unit) {
        val userId = getCurrentUser()?.uid ?: run { callback(false, "User not logged in"); return }
        db.collection("users").document(userId).collection("notifications").document(notificationId)
            .delete()
            .addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { callback(false, it.message) }
    }

    fun deleteAllNotifications(callback: (Boolean, String?) -> Unit) {
        val userId = getCurrentUser()?.uid ?: run { callback(false, "User not logged in"); return }
        db.collection("users").document(userId).collection("notifications").get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                snapshot.documents.forEach { doc -> batch.delete(doc.reference) }
                batch.commit()
                    .addOnSuccessListener { callback(true, null) }
                    .addOnFailureListener { callback(false, it.message) }
            }
            .addOnFailureListener { callback(false, it.message) }
    }

    fun saveNotification(notification: AppNotification, callback: (Boolean, String?) -> Unit) {
        val userId = getCurrentUser()?.uid ?: run { callback(false, "User not logged in"); return }

        val notificationId = java.util.UUID.randomUUID().toString()

        val notificationData = mapOf(
            "title" to notification.title,
            "message" to notification.message,
            "type" to notification.type.name,
            "timestamp" to FieldValue.serverTimestamp(),
            "isRead" to false,
            "eventId" to notification.eventId,
            "imageUrl" to notification.imageUrl
        )

        db.collection("users").document(userId).collection("notifications")
            .document(notificationId).set(notificationData)
            .addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { callback(false, it.message) }
    }

    // --- Seats ---

    fun reserveSeats(
        eventId: String, seats: List<Seat>, userId: String,
        durationMinutes: Int = 5, callback: (Boolean, String?) -> Unit
    ) {
        val batch = db.batch()
        val reservationExpiry = System.currentTimeMillis() + (durationMinutes * 60 * 1000)
        seats.forEach { seat ->
            val seatRef = db.collection("events").document(eventId).collection("seats").document(seat.getSeatId())
            batch.set(seatRef, mapOf(
                "row" to seat.row, "number" to seat.number, "section" to seat.section, "price" to seat.price,
                "status" to "RESERVED", "reservedBy" to userId,
                "reservedAt" to System.currentTimeMillis(), "reservationExpiry" to reservationExpiry
            ))
        }
        batch.commit()
            .addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { callback(false, it.message) }
    }

    fun releaseSeats(eventId: String, seatIds: List<String>, callback: (Boolean, String?) -> Unit) {
        val batch = db.batch()
        seatIds.forEach { seatId ->
            val seatRef = db.collection("events").document(eventId).collection("seats").document(seatId)
            batch.update(seatRef, mapOf("status" to "AVAILABLE", "reservedBy" to null, "reservedAt" to 0, "reservationExpiry" to 0))
        }
        batch.commit()
            .addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { callback(false, it.message) }
    }

    fun listenToSeatUpdates(eventId: String, callback: (List<Seat>) -> Unit): ListenerRegistration {
        return db.collection("events").document(eventId).collection("seats")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { callback(emptyList()); return@addSnapshotListener }
                val seats = snapshot?.documents?.mapNotNull { doc -> doc.data?.let { Seat.fromFirebaseMap(it) } } ?: emptyList()
                callback(seats)
            }
    }

    fun initializeEventSeats(eventId: String, sections: List<SeatSection>, callback: (Boolean, String?) -> Unit) {
        // Firebase batches are limited to 500 operations. Large venues (e.g., Menora
        // Mivtachim ~4,000+ seats) exceed that limit, so we chunk writes into batches
        // of 450 (with a safety margin) and track completion across all chunks.
        val allSeatPairs = sections.flatMap { section ->
            section.seats.map { seat -> seat.getSeatId() to seat.toFirebaseMap() }
        }

        val chunks = allSeatPairs.chunked(BATCH_CHUNK_SIZE)
        if (chunks.isEmpty()) {
            callback(true, null)
            return
        }

        var completedChunks = 0
        var failed = false

        for (chunk in chunks) {
            val batch = db.batch()
            chunk.forEach { (seatId, seatData) ->
                val seatRef = db.collection(COLLECTION_EVENTS).document(eventId)
                    .collection("seats").document(seatId)
                batch.set(seatRef, seatData)
            }
            batch.commit()
                .addOnSuccessListener {
                    completedChunks++
                    if (completedChunks == chunks.size && !failed) {
                        callback(true, null)
                    }
                }
                .addOnFailureListener { e ->
                    if (!failed) {
                        failed = true
                        callback(false, e.message)
                    }
                }
        }
    }

    // --- Event Seeding & Atomic Purchase ---

    fun seedEventsIfEmpty(onComplete: () -> Unit = {}) {
        db.collection("config").document("seed_info").get()
            .addOnSuccessListener { configDoc ->
                val currentVersion = configDoc.getLong("version")?.toInt() ?: 0
                Log.d(TAG, "Seed check: Firestore version=$currentVersion, required=$SEED_VERSION")
                if (currentVersion < SEED_VERSION) {
                    Log.d(TAG, "Seed: version outdated, reseeding...")
                    clearAndReseed(onComplete)
                } else {
                    db.collection(COLLECTION_EVENTS).limit(1).get()
                        .addOnSuccessListener { snapshot ->
                            if (snapshot.isEmpty) {
                                Log.d(TAG, "Seed: no events found, reseeding...")
                                clearAndReseed(onComplete)
                            } else {
                                Log.d(TAG, "Seed: events exist and version is current")
                                onComplete()
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Seed: failed to check events", e)
                            onComplete()
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Seed: failed to read config", e)
                clearAndReseed(onComplete)
            }
    }

    private fun clearAndReseed(onComplete: () -> Unit) {
        db.collection(COLLECTION_EVENTS).get()
            .addOnSuccessListener { snapshot ->
                Log.d(TAG, "Seed: deleting ${snapshot.size()} old events...")
                val deleteBatch = db.batch()
                snapshot.documents.forEach { deleteBatch.delete(it.reference) }
                deleteBatch.commit().addOnCompleteListener {
                    val events = IsraeliEventsGenerator.generateRealisticIsraeliEvents()
                    Log.d(TAG, "Seed: writing ${events.size} new events...")
                    val seedBatch = db.batch()
                    events.forEach { event ->
                        val docRef = db.collection(COLLECTION_EVENTS).document(event.id)
                        seedBatch.set(docRef, event)
                    }
                    seedBatch.set(
                        db.collection("config").document("seed_info"),
                        mapOf("version" to SEED_VERSION)
                    )
                    seedBatch.commit()
                        .addOnSuccessListener {
                            Log.d(TAG, "Seed: SUCCESS — ${events.size} events written, version=$SEED_VERSION")
                            onComplete()
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Seed: FAILED to write events", e)
                            onComplete()
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Seed: FAILED to read existing events", e)
                onComplete()
            }
    }

    /**
     * Scans all events in Firestore. Any event whose date is in the past
     * gets its date pushed forward by 2-5 months and optionally gets a new venue.
     * Runs on app startup after seeding.
     */
    fun refreshExpiredEvents(onComplete: () -> Unit = {}) {
        val dateFormat = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale("en"))
        val today = java.util.Calendar.getInstance().time

        db.collection(COLLECTION_EVENTS).get()
            .addOnSuccessListener { snapshot ->
                val expiredDocs = snapshot.documents.filter { doc ->
                    val dateStr = doc.getString("date") ?: return@filter false
                    try {
                        val eventDate = dateFormat.parse(dateStr)
                        eventDate != null && eventDate.before(today)
                    } catch (_: Exception) { false }
                }

                if (expiredDocs.isEmpty()) {
                    onComplete()
                    return@addOnSuccessListener
                }

                val random = kotlin.random.Random
                val venues = com.roei.stagemate.data.models.IsraeliLocations.venues
                val calendar = java.util.Calendar.getInstance()

                val chunks = expiredDocs.chunked(BATCH_CHUNK_SIZE)
                var completed = 0

                for (chunk in chunks) {
                    val batch = db.batch()
                    for (doc in chunk) {
                        calendar.time = today
                        calendar.add(java.util.Calendar.MONTH, random.nextInt(2, 6))
                        calendar.add(java.util.Calendar.DAY_OF_MONTH, random.nextInt(1, 28))
                        val newDate = dateFormat.format(calendar.time)

                        val updates = mutableMapOf<String, Any>(
                            "date" to newDate,
                            "lastUpdated" to System.currentTimeMillis()
                        )

                        // 30% chance to reassign to a different venue for variety
                        if (random.nextInt(100) < 30 && venues.isNotEmpty()) {
                            val newVenue = venues[random.nextInt(venues.size)]
                            updates["location"] = "${newVenue.name}, ${newVenue.city}"
                            updates["venue"] = newVenue.name
                            updates["latitude"] = newVenue.latitude
                            updates["longitude"] = newVenue.longitude
                            updates["availableTickets"] = newVenue.capacity
                            updates["totalTickets"] = newVenue.capacity
                        }

                        // Reset ticket availability
                        val currentTotal = doc.getLong("totalTickets")?.toInt() ?: 500
                        updates["availableTickets"] = currentTotal

                        batch.update(doc.reference, updates)
                    }
                    batch.commit()
                        .addOnCompleteListener {
                            completed++
                            if (completed == chunks.size) onComplete()
                        }
                }
            }
            .addOnFailureListener { onComplete() }
    }

    fun loadHotEvents(onResult: (List<Event>) -> Unit) {
        db.collection(COLLECTION_EVENTS)
            .whereEqualTo("hot", true)
            .get()
            .addOnSuccessListener { snapshot ->
                val hotEvents = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Event::class.java)?.also { it.id = doc.id }
                }.filter { DateFormatter.isEventDateTodayOrFuture(it.date) }
                if (hotEvents.isEmpty()) {
                    val localEvents = IsraeliEventsGenerator.getCachedEvents()
                        .filter { it.isHot }
                        .filter { DateFormatter.isEventDateTodayOrFuture(it.date) }
                    onResult(localEvents)
                } else {
                    syncFavorites(hotEvents.toMutableList()) { syncedList ->
                        onResult(syncedList)
                    }
                }
            }
            .addOnFailureListener {
                val localEvents = IsraeliEventsGenerator.getCachedEvents()
                    .filter { it.isHot }
                    .filter { DateFormatter.isEventDateTodayOrFuture(it.date) }
                onResult(localEvents)
            }
    }

    private fun syncFavorites(events: MutableList<Event>, onResult: (List<Event>) -> Unit) {
        // Use the cached set populated by [startFavoritesCacheListener] instead of
        // issuing a fresh Firestore read on every call. Eliminates N+1 reads on
        // live event updates.
        val favoriteIds = cachedFavoriteIds
        if (favoriteIds.isNotEmpty()) {
            events.forEach { event ->
                event.isFavorite = favoriteIds.contains(event.id)
            }
        }
        onResult(events.toList())
    }

    fun purchaseTicketAtomic(
        eventId: String,
        ticket: Ticket,
        quantity: Int,
        onResult: (Boolean, String?) -> Unit
    ) {
        val userId = getCurrentUser()?.uid
        if (userId == null) {
            onResult(false, "User not logged in")
            return
        }

        val eventRef = db.collection(COLLECTION_EVENTS).document(eventId)
        val ticketRef = db.collection("users").document(userId)
            .collection("tickets").document(ticket.id)

        db.runTransaction { transaction ->
            val eventSnapshot = transaction.get(eventRef)

            if (!eventSnapshot.exists()) {
                throw Exception("Event not found")
            }

            // If the field is missing (old seed), treat as available rather than sold-out
            val available = eventSnapshot.getLong("availableTickets")
            if (available != null && available < quantity) {
                throw Exception("Sold Out")
            }

            transaction.update(eventRef, "availableTickets", FieldValue.increment(-quantity.toLong()))
            transaction.set(ticketRef, ticket)
        }
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { e -> onResult(false, e.message) }
    }

    fun startLiveEventsListener(callback: (List<Event>) -> Unit) {
        stopLiveEventsListener()
        eventsListenerRegistration = db.collection(COLLECTION_EVENTS)
            .orderBy("date")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                val events = snapshots?.documents?.mapNotNull { doc ->
                    doc.toObject(Event::class.java)?.also { it.id = doc.id }
                }?.filter { DateFormatter.isEventDateTodayOrFuture(it.date) } ?: emptyList()

                syncFavorites(events.toMutableList()) { synced ->
                    callback(synced)
                }
            }
    }

    fun stopLiveEventsListener() {
        eventsListenerRegistration?.remove()
        eventsListenerRegistration = null
    }

    // --- Email ---

    fun sendEmail(
        toEmail: String,
        subject: String,
        htmlBody: String,
        plainTextBody: String,
        callback: (Boolean, String?) -> Unit
    ) {
        if (toEmail.isBlank()) {
            callback(false, "Recipient email is empty")
            return
        }
        val docId = java.util.UUID.randomUUID().toString()
        val mailDoc = hashMapOf(
            "to" to listOf(toEmail),
            "message" to hashMapOf(
                "subject" to subject,
                "html" to htmlBody,
                "text" to plainTextBody
            ),
            "createdAt" to com.google.firebase.Timestamp.now()
        )
        db.collection("mail")
            .document(docId)
            .set(mailDoc)
            .addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { e -> callback(false, e.message) }
    }
}