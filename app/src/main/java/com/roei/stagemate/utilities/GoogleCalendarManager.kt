@file:Suppress("DEPRECATION") // GoogleSignIn APIs — functional, migration to Credential Manager is non-trivial
package com.roei.stagemate.utilities

import android.app.Activity
import android.content.Intent
import android.provider.CalendarContract
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import java.util.TimeZone

// Manages Google Calendar API integration for adding StageMate events to user's calendar.
// Uses OAuth2 via Google Sign-In for Calendar scope, with CalendarContract Intent fallback.
object GoogleCalendarManager {

    private val CALENDAR_SCOPE = Scope(CalendarScopes.CALENDAR_EVENTS)

    // Pending event data stored while waiting for sign-in consent.
    // Must be cleared via clearPendingData() if the sign-in flow is abandoned.
    private var pendingEventData: CalendarEventData? = null

    // Call when the calendar flow is abandoned (e.g., Activity destroyed during sign-in).
    fun clearPendingData() {
        pendingEventData = null
    }

    data class CalendarEventData(
        val title: String,
        val location: String,
        val description: String,
        val startMillis: Long,
        val endMillis: Long
    )

    // Adds an event to Google Calendar via the REST API.
    // If the user hasn't granted the Calendar scope, launches the sign-in consent flow
    // and stores the event data for retry after consent is granted.
    // Falls back to CalendarContract Intent if API insertion fails.
    fun addToGoogleCalendar(
        activity: Activity,
        eventData: CalendarEventData,
        signInLauncher: ActivityResultLauncher<Intent>,
        callback: (Boolean, String?) -> Unit
    ) {
        val account = GoogleSignIn.getLastSignedInAccount(activity)

        if (account == null || !GoogleSignIn.hasPermissions(account, CALENDAR_SCOPE)) {
            pendingEventData = eventData
            requestCalendarPermission(activity, signInLauncher)
            callback(false, "Requesting calendar permission")
            return
        }

        insertEventViaApi(activity, account, eventData, callback)
    }

    // Called from the ActivityResultLauncher after the user completes sign-in consent.
    fun handleSignInResult(
        activity: Activity,
        callback: (Boolean, String?) -> Unit
    ) {
        val account = GoogleSignIn.getLastSignedInAccount(activity)
        val eventData = pendingEventData

        if (account != null && eventData != null) {
            pendingEventData = null
            insertEventViaApi(activity, account, eventData, callback)
        } else {
            pendingEventData = null
            callback(false, "Sign-in failed or no pending event")
        }
    }

    private fun requestCalendarPermission(
        activity: Activity,
        signInLauncher: ActivityResultLauncher<Intent>
    ) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(CALENDAR_SCOPE)
            .build()
        val client = GoogleSignIn.getClient(activity, gso)
        signInLauncher.launch(client.signInIntent)
    }

    private fun insertEventViaApi(
        activity: Activity,
        account: GoogleSignInAccount,
        eventData: CalendarEventData,
        callback: (Boolean, String?) -> Unit
    ) {
        val appContext = activity.applicationContext
        val weakActivity = WeakReference(activity)
        val selectedAccount = account.account

        (activity as? AppCompatActivity)?.lifecycleScope?.launch(Dispatchers.IO) {
            try {
                val credential = GoogleAccountCredential.usingOAuth2(
                    appContext, listOf(CalendarScopes.CALENDAR_EVENTS)
                )
                credential.selectedAccount = selectedAccount

                val calendarService = Calendar.Builder(
                    NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential
                )
                    .setApplicationName("StageMate")
                    .build()

                val tz = TimeZone.getDefault().id

                val calendarEvent = Event().apply {
                    summary = eventData.title
                    location = eventData.location
                    description = eventData.description
                    start = EventDateTime()
                        .setDateTime(com.google.api.client.util.DateTime(eventData.startMillis))
                        .setTimeZone(tz)
                    end = EventDateTime()
                        .setDateTime(com.google.api.client.util.DateTime(eventData.endMillis))
                        .setTimeZone(tz)
                }

                calendarService.events().insert("primary", calendarEvent).execute()

                withContext(Dispatchers.Main) {
                    callback(true, null)
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    val act = weakActivity.get()
                    if (act != null && !act.isDestroyed) {
                        fallbackToCalendarIntent(act, eventData)
                    }
                    callback(false, "Used calendar intent fallback")
                }
            }
        }
    }

    // Fallback: opens the device calendar app with pre-filled event details.
    private fun fallbackToCalendarIntent(activity: Activity, eventData: CalendarEventData) {
        try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, eventData.title)
                putExtra(CalendarContract.Events.EVENT_LOCATION, eventData.location)
                putExtra(CalendarContract.Events.DESCRIPTION, eventData.description)
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, eventData.startMillis)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, eventData.endMillis)
                putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
            }
            activity.startActivity(intent)
        } catch (_: Exception) { }
    }
}
