package com.roei.stagemate.utilities

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// Date/time formatting and relative timestamp display (e.g. "5 minutes ago").
// Used by NotificationAdapter, TicketAdapter, and EventDetailActivity.
object DateFormatter {

    private val displayFormat = ThreadLocal.withInitial { SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH) }
    private val timeFormat = ThreadLocal.withInitial { SimpleDateFormat("HH:mm", Locale.ENGLISH) }
    private val displayTimeFormat = ThreadLocal.withInitial { SimpleDateFormat("h:mm a", Locale.ENGLISH) }
    private val fullFormat = ThreadLocal.withInitial { SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.ENGLISH) }

    fun formatDate(dateString: String): String {
        return try {
            val date = fullFormat.get()?.parse(dateString) ?: return dateString
            displayFormat.get()?.format(date) ?: dateString
        } catch (e: Exception) {
            dateString
        }
    }

    fun formatTime(timeString: String): String {
        if (timeString.isBlank()) return ""
        return try {
            if (timeString.contains(" - ")) {
                val parts = timeString.split(" - ")
                "${formatSingleTime(parts[0].trim())} - ${formatSingleTime(parts[1].trim())}"
            } else {
                formatSingleTime(timeString.trim())
            }
        } catch (_: Exception) {
            timeString
        }
    }

    private fun formatSingleTime(timeString: String): String {
        return try {
            val date = timeFormat.get()?.parse(timeString) ?: return timeString
            displayTimeFormat.get()?.format(date) ?: timeString
        } catch (_: Exception) {
            timeString
        }
    }

    fun formatTimestamp(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
            diff < TimeUnit.HOURS.toMillis(1) -> {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                "$minutes ${if (minutes == 1L) "minute" else "minutes"} ago"
            }
            diff < TimeUnit.DAYS.toMillis(1) -> {
                val hours = TimeUnit.MILLISECONDS.toHours(diff)
                "$hours ${if (hours == 1L) "hour" else "hours"} ago"
            }
            diff < TimeUnit.DAYS.toMillis(7) -> {
                val days = TimeUnit.MILLISECONDS.toDays(diff)
                "$days ${if (days == 1L) "day" else "days"} ago"
            }
            else -> {
                val date = Date(timestamp)
                displayFormat.get()?.format(date) ?: ""
            }
        }
    }

    fun getCurrentDate(): String {
        return displayFormat.get()?.format(Date()) ?: ""
    }

    fun getCurrentTime(): String {
        return timeFormat.get()?.format(Date()) ?: ""
    }

    private val eventDateFormat = ThreadLocal.withInitial { SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH) }

    fun isEventDateTodayOrFuture(dateString: String): Boolean {
        return try {
            val eventDate = eventDateFormat.get()?.parse(dateString) ?: return true
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            !eventDate.before(today)
        } catch (e: Exception) {
            true
        }
    }
}
