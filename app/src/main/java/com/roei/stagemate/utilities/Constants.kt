package com.roei.stagemate.utilities

// App-wide constants: categories, intent keys, SharedPrefs keys, UI timing, pricing.
// Used by virtually every Activity, Fragment, and utility class.
object Constants {

    object Categories {
        const val ALL = "All"
        const val MUSIC = "Music"
        const val SPORT = "Sport"
        const val THEATER = "Theater"
        const val CHILDREN = "Children"
        const val STAND_UP = "Stand-Up"
        const val FESTIVAL = "Festival"
    }

    object SubCategories {
        const val POP_MIZRAHI = "Pop/Mizrahi"
        const val ROCK = "Rock"
        const val HIP_HOP = "Hip-Hop"
        const val CLASSICAL = "Classical"
        const val FESTIVAL = "Festival"
        const val FOOTBALL = "Football"
        const val BASKETBALL = "Basketball"
        const val TENNIS = "Tennis"
        const val RUNNING = "Running"
        const val HANDBALL = "Handball"
        const val SWIMMING = "Swimming"
        const val STANDUP = "Stand-Up"
        const val DRAMA = "Drama"
        const val MUSICAL = "Musical"
        const val DISNEY = "Disney"
        const val ANIMATION = "Animation"
        const val PUPPETS = "Puppets"
        fun getSubCategories(category: String): List<String> {
            return when (category) {
                Categories.MUSIC -> listOf(POP_MIZRAHI, ROCK, HIP_HOP, CLASSICAL, FESTIVAL)
                Categories.SPORT -> listOf(FOOTBALL, BASKETBALL, TENNIS, RUNNING, HANDBALL, SWIMMING)
                Categories.STAND_UP -> listOf(STANDUP)
                Categories.THEATER -> listOf(DRAMA, MUSICAL)
                Categories.CHILDREN -> listOf(DISNEY, ANIMATION, PUPPETS)
                else -> emptyList()
            }
        }
    }

    object SharedPrefs {
        const val PREFS_NAME = "StageMatePrefs"
        const val KEY_USER_ID = "userId"
        const val KEY_USER_EMAIL = "userEmail"
        const val KEY_USER_NAME = "userName"
        const val KEY_SELECTED_CITY = "selectedCity"
        const val KEY_IS_LOGGED_IN = "isLoggedIn"
        const val KEY_REMEMBER_ME = "rememberMe"
        const val PREFERRED_CATEGORIES = "preferredCategories"
        const val KEY_USER_PHONE = "user_phone"
        const val KEY_USER_CITY = "user_city"
        const val KEY_TWO_FACTOR_ENABLED = "two_factor_enabled"
        const val KEY_LOCATION_FILTER_MODE = "location_filter_mode"
        const val KEY_LOCATION_RADIUS = "location_radius"
        const val KEY_SEARCH_HISTORY = "search_history"
    }

    object IntentKeys {
        const val EVENT_ID = "EVENT_ID"
        const val EVENT_TITLE = "EVENT_TITLE"
        const val EVENT_DATE = "EVENT_DATE"
        const val EVENT_TIME = "EVENT_TIME"
        const val EVENT_LOCATION = "EVENT_LOCATION"
        const val EVENT_IMAGE_URL = "EVENT_IMAGE_URL"
        const val EVENT_VENUE = "EVENT_VENUE"
        const val EVENT_DESCRIPTION = "EVENT_DESCRIPTION"
        const val EVENT_CATEGORY = "EVENT_CATEGORY"

        const val EVENT_PRICE = "EVENT_PRICE"
        const val EVENT_TYPE = "EVENT_TYPE"
        const val TOTAL_AMOUNT = "TOTAL_AMOUNT"
        const val TOTAL_PRICE = "TOTAL_PRICE"
        const val QUANTITY = "QUANTITY"
        const val TICKET_TYPE = "TICKET_TYPE"
        const val TICKET_TYPE_ENUM = "TICKET_TYPE_ENUM"
        const val TICKET_ID = "TICKET_ID"
        const val FROM_CART = "FROM_CART"
        const val ITEM_COUNT = "ITEM_COUNT"
        const val IS_STANDING_EVENT = "IS_STANDING_EVENT"
        const val SEAT_NUMBERS = "SEAT_NUMBERS"
        const val SEAT_SECTIONS = "SEAT_SECTIONS"
        const val SEAT_ROWS = "SEAT_ROWS"
        const val SEAT_PRICES = "SEAT_PRICES"
        const val SELECTED_SEATS = "SELECTED_SEATS"
        const val BOOKING_REFERENCE = "BOOKING_REFERENCE"
        const val QR_CODE = "QR_CODE"
        const val PURCHASE_DATE = "PURCHASE_DATE"
        const val OPEN_TAB = "OPEN_TAB"
        const val TICKET_FILTER = "TICKET_FILTER"
        const val VENUE_TYPE = "VENUE_TYPE"
        const val VENUE_NAME = "VENUE_NAME"
        const val NOTIFICATION_TYPE = "NOTIFICATION_TYPE"
        const val RESERVATION_DEADLINE = "RESERVATION_DEADLINE"
    }

    object Cities {
        val ISRAEL_CITIES = listOf(
            "Tel Aviv", "Jerusalem", "Haifa", "Rishon LeZion",
            "Petah Tikva", "Ashdod", "Netanya", "Beer Sheva",
            "Holon", "Bnei Brak", "Ramat Gan", "Rehovot",
            "Bat Yam", "Ashkelon", "Herzliya", "Kfar Saba"
        )
    }

    object Filters {
        const val MIN_PRICE = 0.0
        const val MAX_PRICE = 1000.0
        const val MIN_DISTANCE = 0
        const val MAX_DISTANCE = 100
    }

    object UI {
        const val ANIMATION_DURATION = 300L
        const val SKELETON_PULSE_DURATION = 900L
        const val SKELETON_ALPHA_MIN = 0.4f
        const val SPLASH_STAGGER_DELAY = 150L
        const val SPLASH_STAGGER_OFFSET = 200L
        const val SPLASH_TEXT_DURATION = 800L
        const val SPLASH_PROGRESS_DURATION = 600L
        const val SPLASH_TEXT_OFFSET_Y = 30f
    }

    object VenueUtils {
        private val STANDING_KEYWORDS = listOf(
            "grass", "standing", "general admission", "bar area", "lawn", "field"
        )

        fun isStandingSection(sectionName: String): Boolean {
            val lower = sectionName.lowercase()
            return STANDING_KEYWORDS.any { lower.contains(it) }
        }
    }

    object Occupancy {
        const val CRITICAL_PERCENT = 90
        const val ALMOST_FULL_PERCENT = 85
        const val HIGH_PERCENT = 70
        const val MODERATE_PERCENT = 40
    }

    const val CURRENCY_SYMBOL = "₪"
    const val VAT_RATE = 0.18
    const val DEFAULT_EVENT_DURATION_MS = 2 * 60 * 60 * 1000L

    object PriceMultipliers {
        const val VIP = 2.0
        const val EARLY_BIRD = 0.8
        const val REGULAR = 1.0
    }

    object SeatColors {
        const val BLACK = "#000000"
        const val GREEN = "#4CAF50"
        const val GOLD = "#FFD700"
        const val BLUE = "#2196F3"
        const val DARK_BLUE = "#1976D2"
        const val MEDIUM_BLUE = "#1E88E5"
        const val LIGHT_BLUE = "#42A5F5"
        const val DARK_GREEN = "#2E7D32"
        const val MEDIUM_GREEN = "#388E3C"
        const val LIGHT_GREEN = "#66BB6A"
        const val LIME_GREEN = "#8BC34A"
        const val PURPLE = "#9C27B0"
        const val GRAY = "#9E9E9E"
        const val DARK_RED = "#D32F2F"
        const val RED = "#E53935"
        const val DEEP_ORANGE = "#FF5722"
        const val ORANGE = "#FF7043"
        const val AMBER = "#FF9800"
        const val FALLBACK_GRAY = "#888888"
        const val BLUE_GRAY = "#78909C"
    }
}