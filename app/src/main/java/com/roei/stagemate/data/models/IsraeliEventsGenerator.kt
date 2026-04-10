package com.roei.stagemate.data.models

import com.roei.stagemate.utilities.Constants
import java.text.SimpleDateFormat
import java.util.*
import java.util.UUID
import kotlin.random.Random

// Generates 80+ realistic Israeli events with real venues, artists, and ILS prices.
// Used by DataRepository and FirebaseRepository to seed initial event data.
object IsraeliEventsGenerator {

    private val dateFormat: ThreadLocal<SimpleDateFormat> =
        ThreadLocal.withInitial { SimpleDateFormat("dd MMM yyyy", Locale("en")) }
    private val timeFormat: ThreadLocal<SimpleDateFormat> =
        ThreadLocal.withInitial { SimpleDateFormat("HH:mm", Locale.ENGLISH) }

    private var cachedEvents: List<Event>? = null

    fun getCachedEvents(): List<Event> {
        return cachedEvents ?: generateRealisticIsraeliEvents().also { cachedEvents = it }
    }

    private fun generateUpcomingDates(count: Int): List<Date> {
        val dates = mutableListOf<Date>()
        val calendar = Calendar.getInstance()

        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val maxDays = 180

        repeat(count) {
            calendar.add(Calendar.DAY_OF_MONTH, Random.nextInt(3, 15))

            if (calendar.get(Calendar.DAY_OF_MONTH) <= maxDays) {
                dates.add(calendar.time)
            }
        }

        return dates.sorted()
    }

    fun generateRealisticIsraeliEvents(): List<Event> {
        val events = mutableListOf<Event>()

        events.addAll(generateStandUpShows())
        events.addAll(generateMusicConcerts())
        events.addAll(generateTheaterShows())
        events.addAll(generateChildrenShows())
        events.addAll(generateSportsEvents())
        events.addAll(generateFestivals())

        return events.shuffled()
    }

    private fun generateStandUpShows(): List<Event> {
        val shows = mutableListOf<Event>()
        val dates = generateUpcomingDates(20)

        val comedians = listOf(
            "Gad Elmaleh",
            "Adir Miller",
            "Shachar Hasson",
            "Ben Ben Baruch",
            "Lior Schleien",
            "Guy Hochman",
            "Asi Cohen"
        )

        val venues = listOf(
            IsraeliLocations.venues.find { it.name.contains("Comedy Bar") },
            IsraeliLocations.venues.find { it.name.contains("Zappa") },
            IsraeliLocations.venues.find { it.name.contains("Cameri") },
            IsraeliLocations.venues.find { it.name.contains("Habima") },
            IsraeliLocations.venues.find { it.name.contains("Haifa Auditorium") },
            IsraeliLocations.venues.find { it.name.contains("Beer Sheva") },
            IsraeliLocations.venues.find { it.name.contains("Jerusalem Theatre") }
        ).filterNotNull()

        comedians.take(dates.size).forEachIndexed { index, title ->
            shows.add(buildEvent(
                title = "$title - Stand-Up Show",
                category = Constants.Categories.STAND_UP,
                subcategory = Constants.SubCategories.STANDUP,
                eventType = Event.EventType.SEATED,
                date = dates[index],
                timeRange = (20 to 22) to (22 to 24),
                venue = venues[index % venues.size],
                priceKey = "stand-up",
                ratingBase = 4.3, ratingRange = 0.6,
                description = getStandUpDescription(title),
                participantsRange = 200..800
            ))
        }

        shows.filter { it.title.contains("Gad Elmaleh") || it.title.contains("Shachar Hasson") }
            .forEach { it.isHot = true }
        return shows
    }

    // Maps artist to venue based on popularity and capacity
    private fun getMusicVenue(title: String): IsraeliLocations.Venue {
        val defaultVenue = IsraeliLocations.venues.first()
        val parkHayarkon = IsraeliLocations.venues.find { it.name.contains("Park HaYarkon") } ?: defaultVenue
        val menora = IsraeliLocations.venues.find { it.name.contains("Menora") } ?: defaultVenue
        val caesarea = IsraeliLocations.venues.find { it.name.contains("Caesarea") } ?: defaultVenue
        val zappa = IsraeliLocations.venues.find { it.name.contains("Zappa") } ?: defaultVenue
        val bronfman = IsraeliLocations.venues.find { it.name.contains("Bronfman") } ?: defaultVenue
        val bloomfield = IsraeliLocations.venues.find { it.name.contains("Bloomfield") } ?: defaultVenue
        val sultansPool = IsraeliLocations.venues.find { it.name.contains("Sultan's Pool") } ?: defaultVenue
        val haifaAuditorium = IsraeliLocations.venues.find { it.name.contains("Haifa Auditorium") } ?: defaultVenue
        val binyaneiHaUma = IsraeliLocations.venues.find { it.name.contains("International Convention Center") || it.name.contains("Binyanei") } ?: defaultVenue
        val performingArtsBeerSheva = IsraeliLocations.venues.find { it.name.contains("Beer Sheva") } ?: defaultVenue
        val reading3 = IsraeliLocations.venues.find { it.name.contains("Reading") } ?: defaultVenue
        val firstStation = IsraeliLocations.venues.find { it.name.contains("First Station") } ?: defaultVenue

        return when {
            title.contains("Eyal Golan") -> bloomfield
            title.contains("Omer Adam") -> parkHayarkon
            title.contains("Noa Kirel") && title.contains("Park HaYarkon") -> parkHayarkon
            title.contains("Noa Kirel") && title.contains("Menora") -> menora
            title.contains("Static") -> sultansPool
            title.contains("Eden Ben Zaken") -> haifaAuditorium
            title.contains("Moshe Peretz") -> performingArtsBeerSheva
            title.contains("Sarit Hadad") -> binyaneiHaUma
            title.contains("Eliad Nachum") -> menora
            title.contains("Lior Narkis") -> menora
            title.contains("Shlomo Artzi") -> caesarea
            title.contains("Idan Raichel") -> sultansPool
            title.contains("Harel Skaat") -> bronfman
            title.contains("Yehoram Gaon") -> firstStation
            title.contains("Subliminal") -> zappa
            title.contains("Nechi Nech") -> reading3
            title.contains("Tuna") -> zappa
            title.contains("Mashina") -> caesarea
            title.contains("HaYehudim") -> reading3
            title.contains("Ethnix") -> haifaAuditorium
            else -> listOf(menora, caesarea, parkHayarkon, bronfman, haifaAuditorium, binyaneiHaUma)
                .let { it[(title.hashCode() and Int.MAX_VALUE) % it.size] }
        }
    }

    private fun generateMusicConcerts(): List<Event> {
        val concerts = mutableListOf<Event>()
        val dates = generateUpcomingDates(30)

        val artists = IsraeliEvents.musicArtists

        artists.take(dates.size).forEachIndexed { index, title ->
            val venue = getMusicVenue(title)
            val isLarge = venue.capacity > 5000
            concerts.add(buildEvent(
                title = title,
                category = Constants.Categories.MUSIC,
                subcategory = Constants.SubCategories.POP_MIZRAHI,
                eventType = if (isLarge) Event.EventType.MIXED else Event.EventType.SEATED,
                date = dates[index],
                timeRange = (19 to 21) to (22 to 24),
                venue = venue,
                priceKey = if (isLarge) "music_large" else "music_small",
                ratingBase = 4.2, ratingRange = 0.7,
                description = getMusicDescription(title),
                participantsRange = venue.capacity / 2..venue.capacity,
                distanceRange = 0.5..25.0
            ))
        }

        concerts.filter { it.title.contains("Noa Kirel") || it.title.contains("Omer Adam") || it.title.contains("Eyal Golan") }
            .forEach { it.isHot = true }
        return concerts
    }

    private fun generateTheaterShows(): List<Event> {
        val shows = mutableListOf<Event>()
        val dates = generateUpcomingDates(20)

        val plays = IsraeliEvents.theaterShows

        val venues = listOf(
            IsraeliLocations.venues.find { it.name.contains("Habima") },
            IsraeliLocations.venues.find { it.name.contains("Cameri") },
            IsraeliLocations.venues.find { it.name.contains("Gesher") },
            IsraeliLocations.venues.find { it.name.contains("Beer Sheva") }
        ).filterNotNull()

        plays.take(dates.size).forEachIndexed { index, title ->
            val venue = venues[index % venues.size]
            shows.add(buildEvent(
                title = title,
                category = Constants.Categories.THEATER,
                subcategory = Constants.SubCategories.DRAMA,
                eventType = Event.EventType.SEATED,
                date = dates[index],
                timeRange = (19 to 20) to (21 to 23),
                venue = venue,
                priceKey = "theater",
                ratingBase = 4.4, ratingRange = 0.5,
                description = getTheaterDescription(title),
                participantsRange = 300..venue.capacity,
                distanceRange = 0.5..30.0
            ))
        }

        shows.filter { it.title.contains("Romeo") || it.title.contains("Godot") }
            .forEach { it.isHot = true }
        return shows
    }

    private fun generateChildrenShows(): List<Event> {
        val shows = mutableListOf<Event>()
        val dates = generateUpcomingDates(15)

        val childrenEvents = IsraeliEvents.childrenShows

        val venues = listOf(
            IsraeliLocations.venues.find { it.name.contains("Menora") },
            IsraeliLocations.venues.find { it.name.contains("Bronfman") },
            IsraeliLocations.venues.find { it.name.contains("Habima") },
            IsraeliLocations.venues.find { it.name.contains("Haifa Auditorium") },
            IsraeliLocations.venues.find { it.name.contains("Beer Sheva") },
            IsraeliLocations.venues.find { it.name.contains("Jerusalem Theatre") },
            IsraeliLocations.venues.find { it.name.contains("Cameri") }
        ).filterNotNull()

        childrenEvents.take(dates.size).forEachIndexed { index, title ->
            shows.add(buildEvent(
                title = title,
                category = Constants.Categories.CHILDREN,
                subcategory = Constants.SubCategories.DISNEY,
                eventType = Event.EventType.SEATED,
                date = dates[index],
                timeRange = (10 to 14) to (15 to 17),
                venue = venues[index % venues.size],
                priceKey = "children",
                ratingBase = 4.5, ratingRange = 0.4,
                description = getChildrenDescription(title),
                participantsRange = 200..1000
            ))
        }

        shows.filter { it.title.contains("Disney On Ice") || it.title.contains("Frozen") || it.title.contains("Lion King") }
            .forEach { it.isHot = true }
        return shows
    }

    private fun getSportsVenue(title: String): IsraeliLocations.Venue {
        val defaultVenue = IsraeliLocations.venues.first()
        val menora = IsraeliLocations.venues.find { it.name.contains("Menora") } ?: defaultVenue
        val bloomfield = IsraeliLocations.venues.find { it.name.contains("Bloomfield") } ?: defaultVenue

        return when {
            // Tennis goes to the Israel Tennis Center in Ramat HaSharon
            title.contains("Tennis") || title.contains("ATP") -> {
                IsraeliLocations.venues.find { it.name.contains("Israel Tennis Center") } ?: defaultVenue
            }
            // Indoor volleyball at Hadar Yosef Sports Center in Tel Aviv
            title.contains("Volleyball") || title.contains("Yedim") -> {
                IsraeliLocations.venues.find { it.name.contains("Hadar Yosef") } ?: defaultVenue
            }
            // Football matches at Bloomfield Stadium
            title.contains("Hapoel Beer Sheva vs Hapoel Tel Aviv") || title.contains("Football") -> bloomfield
            // Everything else (basketball, derbies, Real Madrid) plays at Menora Mivtachim Arena
            else -> menora
        }
    }

    private fun generateSportsEvents(): List<Event> {
        val events = mutableListOf<Event>()
        val dates = generateUpcomingDates(15)

        val sportsGames = IsraeliEvents.sportsEvents

        sportsGames.take(dates.size).forEachIndexed { index, title ->
            val venue = getSportsVenue(title)
            events.add(buildEvent(
                title = title,
                category = Constants.Categories.SPORT,
                subcategory = Constants.SubCategories.FOOTBALL,
                eventType = Event.EventType.SEATED,
                date = dates[index],
                timeRange = (19 to 21) to (21 to 23),
                venue = venue,
                priceKey = "sports",
                ratingBase = 4.0, ratingRange = 0.8,
                description = getSportsDescription(title),
                participantsRange = venue.capacity / 2..venue.capacity,
                distanceRange = 0.5..15.0
            ))
        }

        events.filter { it.title.contains("Maccabi Tel Aviv") || it.title.contains("Euroleague") }
            .take(3).forEach { it.isHot = true }
        return events
    }

    private fun getFestivalVenue(title: String): IsraeliLocations.Venue {
        val defaultVenue = IsraeliLocations.venues.first()
        val parkHayarkon = IsraeliLocations.venues.find { it.name.contains("Park HaYarkon") } ?: defaultVenue
        val sultansPool = IsraeliLocations.venues.find { it.name.contains("Sultan's Pool") } ?: defaultVenue
        val caesarea = IsraeliLocations.venues.find { it.name.contains("Caesarea") } ?: defaultVenue
        val eilat = IsraeliLocations.venues.find { it.name.contains("Ice Space") } ?: defaultVenue
        val beerSheva = IsraeliLocations.venues.find { it.name.contains("Performing Arts") && it.city == "Beer Sheva" } ?: defaultVenue

        return when {
            title.contains("Red Sea Jazz") -> eilat
            title.contains("InDNegev") -> beerSheva
            title.contains("Israel Festival") -> sultansPool
            title.contains("Tamar") -> caesarea
            title.contains("Hutzot HaYotzer") -> sultansPool
            else -> parkHayarkon
        }
    }

    private fun generateFestivals(): List<Event> {
        val festivalEvents = mutableListOf<Event>()
        val dates = generateUpcomingDates(10)

        val festivals = IsraeliEvents.festivals

        festivals.take(dates.size).forEachIndexed { index, title ->
            val venue = getFestivalVenue(title)
            festivalEvents.add(buildEvent(
                title = title,
                category = Constants.Categories.FESTIVAL,
                eventType = Event.EventType.STANDING_ONLY,
                date = dates[index],
                timeRange = (16 to 18) to (23 to 2),
                venue = venue,
                priceKey = "festival",
                ratingBase = 4.3, ratingRange = 0.6,
                description = getFestivalDescription(title),
                participantsRange = 1000..venue.capacity,
                distanceRange = 0.5..40.0
            ))
        }

        festivalEvents.filter { it.title.contains("InDNegev") || it.title.contains("Red Sea Jazz") || it.title.contains("Jerusalem Film") }
            .forEach { it.isHot = true }
        return festivalEvents
    }

    private fun buildEvent(
        title: String,
        category: String,
        subcategory: String? = null,
        eventType: Event.EventType = Event.EventType.SEATED,
        date: Date,
        timeRange: Pair<Pair<Int, Int>, Pair<Int, Int>>,
        venue: IsraeliLocations.Venue,
        priceKey: String,
        ratingBase: Double,
        ratingRange: Double,
        description: String,
        participantsRange: IntRange,
        distanceRange: ClosedFloatingPointRange<Double> = 0.5..20.0
    ): Event {
        return Event(
            id = UUID.randomUUID().toString(),
            title = title,
            category = category,
            eventType = eventType,
            date = dateFormat.get()!!.format(date),
            time = "${generateTime(timeRange.first.first, timeRange.first.second)} - ${generateTime(timeRange.second.first, timeRange.second.second)}",
            location = "${venue.name}, ${venue.city}",
            venue = venue.name,
            imageUrl = getEventImage(title),
            price = IsraeliEvents.getPrice(priceKey),
            rating = (ratingBase + Random.nextDouble() * ratingRange).toFloat(),
            description = description,
            participants = Random.nextInt(participantsRange.first, participantsRange.last),
            distance = Random.nextDouble(distanceRange.start, distanceRange.endInclusive),
            latitude = venue.latitude,
            longitude = venue.longitude,
            availableTickets = venue.capacity,
            totalTickets = venue.capacity,
            subcategory = subcategory ?: ""
        )
    }

    // Handles midnight wrap-around (e.g. 23:00 to 02:00)
    private fun generateTime(startHour: Int, endHour: Int): String {
        val calendar = Calendar.getInstance()
        val safeStart = startHour.coerceIn(0, 23)
        val safeEnd = endHour.coerceIn(0, 23)
        val range = if (safeEnd > safeStart) {
            safeEnd - safeStart
        } else {
            24 - safeStart + safeEnd
        }
        val safeRange = if (range <= 0) 1 else range
        calendar.set(Calendar.HOUR_OF_DAY, (safeStart + Random.nextInt(safeRange)) % 24)
        calendar.set(Calendar.MINUTE, if (Random.nextBoolean()) 0 else 30)
        return timeFormat.get()!!.format(calendar.time)
    }

    // Maps event title to local drawable resource, returns empty string as fallback
    private fun getEventImage(title: String): String {
        val pkg = "android.resource://com.stagemate.official/drawable"
        return when {
            // --- Stand-Up ---
            title.contains("Gad Elmaleh", ignoreCase = true)       -> "$pkg/event_gad_elmaleh"
            title.contains("Adir Miller", ignoreCase = true)       -> "$pkg/event_adir_miller"
            title.contains("Shachar Hasson", ignoreCase = true)    -> "$pkg/event_shachar_hasson"
            title.contains("Ben Ben Baruch", ignoreCase = true) ||
                title.contains("Ben Baruch", ignoreCase = true)    -> "$pkg/event_ben_baruch"
            title.contains("Lior Schleien", ignoreCase = true)     -> "$pkg/event_lior_schleien"
            title.contains("Guy Hochman", ignoreCase = true)       -> "$pkg/event_guy_hochman"
            title.contains("Asi Cohen", ignoreCase = true)         -> "$pkg/event_asi_cohen"
            title.contains("Tom Aharon", ignoreCase = true)        -> "$pkg/event_tom_aharon"

            // --- Music ---
            title.contains("Omer Adam", ignoreCase = true)         -> "$pkg/event_omer_adam"
            title.contains("Eden Ben Zaken", ignoreCase = true)    -> "$pkg/event_eden_ben_zaken"
            title.contains("Noa Kirel", ignoreCase = true)         -> "$pkg/event_noa_kirel"
            title.contains("Static", ignoreCase = true)            -> "$pkg/event_static"
            title.contains("Eliad Nachum", ignoreCase = true)      -> "$pkg/event_eliad_nachum"
            title.contains("Moshe Peretz", ignoreCase = true)      -> "$pkg/event_moshe_peretz"
            title.contains("Eyal Golan", ignoreCase = true)        -> "$pkg/event_eyal_golan"
            title.contains("Sarit Hadad", ignoreCase = true)       -> "$pkg/event_sarit_hadad"
            title.contains("Lior Narkis", ignoreCase = true)       -> "$pkg/event_lior_narkis"
            title.contains("Harel Skaat", ignoreCase = true)       -> "$pkg/event_harel_skaat"
            title.contains("Mashina", ignoreCase = true)           -> "$pkg/event_mashina"
            title.contains("HaYehudim", ignoreCase = true)         -> "$pkg/event_hayehudim"
            title.contains("Ethnix", ignoreCase = true)            -> "$pkg/event_ethnix"
            title.contains("Subliminal", ignoreCase = true)        -> "$pkg/event_subliminal"
            title.contains("Nechi Nech", ignoreCase = true)        -> "$pkg/event_nechi_nech"
            title.contains("Tuna", ignoreCase = true)              -> "$pkg/event_tuna"
            title.contains("Idan Raichel", ignoreCase = true)      -> "$pkg/event_idan_raichel"
            title.contains("Shlomo Artzi", ignoreCase = true)      -> "$pkg/event_shlomo_artzi"
            title.contains("Yehoram Gaon", ignoreCase = true)      -> "$pkg/event_yehoram_gaon"

            // --- Theater ---
            title.contains("Romeo", ignoreCase = true)             -> "$pkg/event_romeo_juliet"
            title.contains("Cherry Orchard", ignoreCase = true)    -> "$pkg/event_cherry_orchard"
            title.contains("Salesman", ignoreCase = true)          -> "$pkg/event_death_of_salesman"
            title.contains("Midsummer", ignoreCase = true)         -> "$pkg/event_midsummer"
            title.contains("Godot", ignoreCase = true)             -> "$pkg/event_waiting_for_godot"
            title.contains("Kazablan", ignoreCase = true)          -> "$pkg/event_kazablan"

            // --- Children ---
            title.contains("Disney On Ice", ignoreCase = true)     -> "$pkg/event_disney_on_ice"
            title.contains("Frozen", ignoreCase = true)            -> "$pkg/event_frozen"
            title.contains("Lion King", ignoreCase = true)         -> "$pkg/event_lion_king"
            title.contains("SpongeBob", ignoreCase = true)         -> "$pkg/event_spongebob"
            title.contains("Michal HaKtana", ignoreCase = true)    -> "$pkg/event_michal_haktana"
            title.contains("Baby Shark", ignoreCase = true)        -> "$pkg/event_baby_shark"
            title.contains("Mani HaMatara", ignoreCase = true)     -> "$pkg/event_mani_hamatara"
            title.contains("Kofiko", ignoreCase = true)            -> "$pkg/event_kofiko"

            // --- Sports ---
            title.contains("State Cup Final", ignoreCase = true)   -> "$pkg/event_state_cup_final"
            title.contains("Real Madrid", ignoreCase = true)       -> "$pkg/event_euroleague_real_madrid"
            title.contains("Euroleague Derby", ignoreCase = true)  -> "$pkg/event_euroleague_derby"
            title.contains("Hapoel Beer Sheva vs Hapoel Tel Aviv", ignoreCase = true) -> "$pkg/event_hapoel_beer_sheva"
            title.contains("Basketball", ignoreCase = true) ||
                title.contains("Euroleague", ignoreCase = true)    -> "$pkg/event_euroleague"
            title.contains("Yedim", ignoreCase = true) ||
                title.contains("Volleyball", ignoreCase = true)    -> "$pkg/event_volleyball_league"
            title.contains("ATP", ignoreCase = true)               -> "$pkg/event_atp"
            title.contains("Tennis", ignoreCase = true)            -> "$pkg/event_tennis"
            title.contains("Maccabi Tel Aviv", ignoreCase = true)  -> "$pkg/event_maccabi_tel_aviv_basketball"

            // --- Festivals ---
            title.contains("Red Sea Jazz", ignoreCase = true)      -> "$pkg/event_red_sea_jazz"
            title.contains("Israel Festival", ignoreCase = true)   -> "$pkg/event_israel_festival"
            title.contains("InDNegev", ignoreCase = true)          -> "$pkg/event_indnegev"
            title.contains("Tamar Festival", ignoreCase = true)    -> "$pkg/event_israel_festival"
            title.contains("Hutzot HaYotzer", ignoreCase = true)   -> "$pkg/event_israel_festival"

            else -> ""
        }
    }

    fun getDescriptionForTitle(title: String, category: String): String {
        return when (category.lowercase()) {
            "comedy", "stand-up", "standup" -> getStandUpDescription(title)
            "music", "concert" -> getMusicDescription(title)
            "theater", "theatre" -> getTheaterDescription(title)
            "children", "kids", "family" -> getChildrenDescription(title)
            "sports", "sport" -> getSportsDescription(title)
            "festival", "festivals" -> getFestivalDescription(title)
            else -> {
                // Try all description functions based on title keywords
                val desc = getMusicDescription(title)
                if (!desc.startsWith("A spectacular live concert")) return desc
                val standUp = getStandUpDescription(title)
                if (!standUp.startsWith("An evening of top-tier")) return standUp
                val sports = getSportsDescription(title)
                if (!sports.startsWith("An exciting live sporting")) return sports
                val theater = getTheaterDescription(title)
                if (!theater.startsWith("A captivating theatrical")) return theater
                getFestivalDescription(title)
            }
        }
    }

    private fun getStandUpDescription(name: String): String {
        return when {
            name.contains("Gad Elmaleh") -> "French-Israeli comedy superstar Gad Elmaleh brings his internationally acclaimed humor to the Israeli stage. Expect a hilarious blend of observational comedy, physical humor, and cross-cultural jokes that have made him one of the world's top comedians."
            name.contains("Adir Miller") -> "One of Israel's most beloved comedians, Adir Miller delivers his signature sharp wit and satirical take on Israeli life. Known for his iconic TV characters, this stand-up show promises an evening of relentless laughter and clever social commentary."
            name.contains("Shachar Hasson") -> "Sharp, energetic, and unfiltered, Shachar Hasson takes the stage with his rapid-fire delivery and bold humor. From everyday absurdities to biting political observations, this show will leave you breathless with laughter."
            name.contains("Lior Schleien") -> "Israel's master of deadpan satire, Lior Schleien brings his intellectual and razor-sharp humor to the stage. Expect thought-provoking comedy that tackles politics, media, and the absurdities of modern Israeli society."
            name.contains("Guy Hochman") -> "Rising star Guy Hochman brings fresh energy and relatable humor to the stand-up scene. His unique perspective on relationships, technology, and daily life resonates with audiences of all ages in this must-see comedy hour."
            name.contains("Asi Cohen") -> "Asi Cohen, known for his unforgettable characters and impeccable timing, delivers a stand-up show packed with hilarious impressions and original sketches. A true entertainer who brings the house down every time."
            name.contains("Ben Baruch") -> "Rising comedian Ben Ben Baruch brings his fresh perspective and youthful energy to the stage. With sharp observations about millennial life and Israeli culture, this show is a breath of comedic fresh air."
            else -> "An evening of top-tier Israeli stand-up comedy featuring sharp wit, brilliant timing, and unforgettable punchlines. Get ready for non-stop laughs from one of the country's finest comedians."
        }
    }

    private fun getMusicDescription(title: String): String {
        return when {
            title.contains("Omer Adam") -> "Israel's pop king Omer Adam takes the stage for an electrifying live concert. Expect his biggest hits, dazzling stage production, and the infectious energy that has made him the country's most popular performer."
            title.contains("Eden Ben Zaken") -> "Eden Ben Zaken delivers a powerful solo performance featuring her soulful voice and chart-topping Mizrahi pop hits. An intimate evening of music, emotion, and the unmistakable sound of one of Israel's brightest stars."
            title.contains("Noa Kirel") && title.contains("Park HaYarkon") -> "Global sensation Noa Kirel brings her high-energy pop spectacle to Park HaYarkon's massive outdoor stage. Fresh from international success, expect stunning choreography, dazzling visuals, and all her biggest hits under the open sky."
            title.contains("Noa Kirel") && title.contains("Menora") -> "Global sensation Noa Kirel delivers an intimate yet electrifying indoor show at Menora Mivtachim Arena. A perfect blend of her chart-topping hits, cutting-edge production, and the close-up energy only an arena show can deliver."
            title.contains("Static") -> "The dynamic duo Static & Ben El bring their unmatched chemistry and infectious beats to the stage. From Mizrahi pop anthems to dance floor hits, this tour promises a non-stop party atmosphere."
            title.contains("Eyal Golan") -> "Israeli music icon Eyal Golan delivers a special concert featuring his most beloved songs spanning decades. His powerful voice and emotional performances create an unforgettable evening of Mizrahi music at its finest."
            title.contains("Moshe Peretz") -> "Moshe Peretz brings his signature romantic ballads and upbeat Mizrahi pop to the stage. Known for his warm stage presence and powerful vocals, this concert promises an emotional and joyful musical experience."
            title.contains("Sarit Hadad") -> "The queen of Israeli pop Sarit Hadad takes the stage for an unforgettable live performance. Her powerhouse vocals and decades of beloved hits guarantee an evening of pure musical magic."
            title.contains("Shlomo Artzi") -> "Israeli rock legend Shlomo Artzi performs classic and new material in this highly anticipated tour. Four decades of iconic songwriting come alive in an intimate, emotionally charged concert experience."
            title.contains("Idan Raichel") -> "Idan Raichel and his ensemble bring their signature world-fusion sound to the amphitheater. A mesmerizing blend of Ethiopian, Middle Eastern, and electronic influences creates a transcendent musical journey."
            title.contains("Mashina") -> "Israeli rock legends Mashina reunite for a special tour performing their iconic catalog. From 80s new wave hits to timeless rock anthems, this reunion concert is a celebration of Israeli rock history."
            title.contains("Subliminal") -> "Israeli hip-hop pioneer Subliminal brings raw energy and powerful lyrics to the stage. Expect classic tracks, new material, and the uncompromising spirit that defined Israeli rap music."
            title.contains("Harel Skaat") -> "Acclaimed vocalist Harel Skaat performs an intimate solo concert showcasing his extraordinary vocal range. From pop ballads to classical crossover, this show highlights one of Israel's most gifted singers."
            title.contains("Yehoram Gaon") -> "Legendary performer Yehoram Gaon takes the stage for a special evening of timeless Israeli classics. His iconic voice and charismatic stage presence make this a must-attend celebration of Israeli musical heritage."
            title.contains("Eliad Nachum") -> "Eliad Nachum brings his smooth vocals and romantic Mizrahi pop to the stage. Known for heartfelt ballads and upbeat dance tracks, this concert offers a perfect blend of emotion and energy."
            title.contains("Lior Narkis") -> "Lior Narkis delivers a high-energy concert packed with Mizrahi pop anthems and crowd favorites. His charismatic stage presence and powerful voice make every show an unforgettable celebration."
            title.contains("HaYehudim") -> "Israeli alternative rock band HaYehudim bring their signature sound to the stage. A night of powerful rock, poetic lyrics, and the raw energy that has defined Israeli alternative music for years."
            title.contains("Ethnix") -> "Ethnix bring their unique blend of rock, world music, and Israeli pop to the stage. Known for their energetic live performances, this concert promises a night of dancing and timeless hits."
            title.contains("Nechi Nech") -> "Nechi Nech brings his fresh hip-hop sound and clever wordplay to the stage. One of the new generation of Israeli rap artists, his energetic live shows are a must-see for hip-hop fans."
            title.contains("Tuna") -> "Tuna brings his bold hip-hop sound and streetwise lyrics to an electrifying live show. Raw energy, powerful beats, and authentic Israeli rap at its finest."
            else -> "A spectacular live concert featuring one of Israel's top artists performing their greatest hits and new material. An evening of world-class music, stunning production, and unforgettable performances."
        }
    }

    private fun getTheaterDescription(title: String): String {
        return when {
            title.contains("Romeo") -> "Shakespeare's timeless tale of star-crossed lovers comes alive on the Israeli stage. This acclaimed production combines stunning visuals, powerful performances, and a fresh interpretation of the world's greatest love story."
            title.contains("Cherry Orchard") -> "Chekhov's masterpiece about a declining aristocratic family resonates with new meaning in this compelling production. A poignant exploration of change, memory, and the passage of time performed by Israel's finest actors."
            title.contains("Salesman") -> "Arthur Miller's searing American classic receives a powerful Israeli production. The tragic story of Willy Loman's pursuit of the American dream is brought to life with devastating emotional authenticity."
            title.contains("Midsummer") -> "Shakespeare's enchanted comedy of love and magic delights audiences with whimsical staging and brilliant performances. A night of mischief, romance, and fairy dust that will captivate theater lovers of all ages."
            title.contains("Godot") -> "Beckett's absurdist masterpiece about two men waiting for someone who never arrives is both hilarious and deeply moving. This production brings fresh insight to one of the 20th century's most important plays."
            title.contains("Kazablan") -> "The iconic Israeli musical about a Mizrahi neighborhood in Jaffa bursts with color, humor, and unforgettable songs. A celebration of Israeli culture, love, and the triumph of the human spirit."
            else -> "A captivating theatrical production from one of Israel's leading theater companies. Expert direction, powerful acting, and stunning stage design combine for an unforgettable evening at the theater."
        }
    }

    private fun getChildrenDescription(title: String): String {
        return when {
            title.contains("Disney On Ice") -> "The magic of Disney comes to life on ice in this spectacular touring show. Kids and families will be dazzled by beloved characters, stunning ice skating performances, and enchanting stories brought to life in a winter wonderland."
            title.contains("Frozen") -> "Join Elsa, Anna, and all your favorite Frozen characters in this magical live stage adaptation. Featuring spectacular costumes, snow effects, and all the beloved songs that kids will sing along to with joy."
            title.contains("Lion King") -> "The Pride Lands come alive in this spectacular kids' production of The Lion King. Young audiences will be thrilled by colorful costumes, exciting music, and Simba's inspiring journey from cub to king."
            title.contains("SpongeBob") -> "SpongeBob SquarePants and his Bikini Bottom friends leap from the screen to the stage in this hilarious musical adventure. Packed with catchy songs, silly humor, and positive messages kids will love."
            title.contains("Michal HaKtana") -> "Beloved Israeli character Michal HaKtana comes to life in this delightful interactive show for young children. Sing along, dance, and join Michal on a fun-filled adventure that will have little ones smiling from start to finish."
            title.contains("Baby Shark") -> "The viral sensation Baby Shark swims onto the stage in a colorful, interactive live show. Toddlers and young children will love singing, dancing, and splashing along with the whole shark family."
            title.contains("Mani HaMatara") -> "Mani HaMatara brings educational fun and catchy songs to the stage in this interactive live show. Kids will learn, laugh, and dance as their favorite character leads them through an exciting adventure."
            title.contains("Kofiko") -> "Israel's beloved monkey Kofiko swings onto the stage in a charming live show full of adventures and mischief. A classic Israeli children's character brought to life with humor, music, and plenty of surprises."
            else -> "A magical live show designed especially for young audiences. Featuring colorful characters, interactive moments, and age-appropriate fun that will create lasting memories for the whole family."
        }
    }

    private fun getSportsDescription(title: String): String {
        return when {
            title.contains("Maccabi Tel Aviv vs Hapoel Jerusalem") -> "Two of Israel's biggest clubs meet at Menora Mivtachim Arena for an intense match between rival cities. Maccabi Tel Aviv hosts Hapoel Jerusalem in front of a packed crowd."
            title.contains("Real Madrid") -> "Maccabi Tel Aviv hosts Real Madrid in a top Euroleague clash at Menora Mivtachim Arena. Two of Europe's biggest basketball clubs go head to head in front of a sold-out crowd."
            title.contains("Yedim") || title.contains("Galil Mate Asher") -> "Maccabi Yedim Tel Aviv hosts Hapoel Galil Mate Asher in an Israeli Volleyball Premier League match at Hadar Yosef Sports Center. Fast-paced action and competitive spikes from two of the league's top teams."
            title.contains("Maccabi Tel Aviv vs Maccabi Haifa") && title.contains("Football") -> "Two Maccabi powerhouses meet in a top-of-the-table football clash. This all-Maccabi showdown features some of the league's best talent in a match that could define the championship race."
            title.contains("Hapoel Beer Sheva") -> "Southern pride meets city tradition as Hapoel Beer Sheva hosts Hapoel Tel Aviv. A passionate crowd and competitive football make this a highlight of the Israeli Premier League season."
            title.contains("Euroleague") || title.contains("Basketball") -> "Top-tier European basketball comes to Israel in this exciting Euroleague matchup. Watch elite athletes compete at the highest level in one of European basketball's most prestigious competitions."
            title.contains("State Cup") -> "The pinnacle of Israeli basketball: Maccabi Tel Aviv faces Hapoel Jerusalem in Game 1 of the State Cup Final. Championship-level basketball with everything on the line in front of a sold-out crowd."
title.contains("Tennis") || title.contains("ATP") -> "Top Israeli and international tennis players compete in this prestigious tournament. Experience world-class tennis with powerful serves, brilliant rallies, and the thrill of live championship competition."
            title.contains("Volleyball") -> "Israel's top volleyball teams compete in this exciting league match. Fast-paced action, powerful spikes, and team athleticism make for a thrilling spectator experience."
            else -> "An exciting live sporting event featuring top Israeli athletes and teams. Experience the thrill of competition, the energy of the crowd, and unforgettable moments of athletic excellence."
        }
    }

    private fun getFestivalDescription(title: String): String {
        return when {
            title.contains("Red Sea Jazz") -> "Israel's premier jazz festival brings world-class musicians to the shores of Eilat. Multiple stages feature international and Israeli jazz artists performing across genres from classic jazz to fusion, all against the stunning Red Sea backdrop."
            title.contains("Israel Festival") -> "Israel's flagship performing arts festival showcases the best in theater, dance, music, and visual arts. A multi-day celebration featuring international and local artists in Jerusalem's most iconic venues."
            title.contains("InDNegev") -> "The Negev desert comes alive with Israel's beloved indie music festival. Multiple stages, emerging artists, desert camping, and the unique atmosphere of music under the southern stars make InDNegev an unforgettable experience."
            title.contains("Tamar") -> "Set against the breathtaking backdrop of the Dead Sea, the Tamar Festival brings top Israeli artists to one of the world's most unique natural stages. Music, culture, and stunning desert landscapes combine for a magical multi-day event."
            title.contains("Hutzot HaYotzer") -> "Jerusalem's premier arts and crafts festival combines live music performances with artisan markets in the historic setting near the Old City walls. An enchanting blend of culture, creativity, and entertainment."
            else -> "A spectacular multi-day festival featuring diverse performances, multiple stages, and a vibrant atmosphere. From headlining acts to emerging artists, this festival offers an unforgettable cultural experience."
        }
    }
}
