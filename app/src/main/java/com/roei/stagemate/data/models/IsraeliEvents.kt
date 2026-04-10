package com.roei.stagemate.data.models

// Static database of Israeli event titles and price ranges by category.
// Used by IsraeliEventsGenerator to build realistic event listings.
object IsraeliEvents {

    val standUpShows = listOf(
        "Gad Elmaleh - New Show 2025",
        "Shaanan Street - Stand-Up Comedy",
        "Lior Schleien - Solo Show",
        "Avihu Medina Comedy Night",
        "Adir Miller - New Stand-Up",
        "Guy Hochman Comedy Hour",
        "Tom Aharon - Stand-Up",
        "Shachar Hasson - Comedy Night"
    )

    val musicArtists = listOf(
        // Pop/Mizrahi
        "Omer Adam - Live Concert",
        "Eden Ben Zaken - Solo Show",
        "Noa Kirel - Park HaYarkon Concert",
        "Noa Kirel - Menora Mivtachim Show",
        "Static & Ben El - Tour",
        "Eliad Nachum - Live",
        "Moshe Peretz - Concert",
        "Eyal Golan - Special Show",
        "Sarit Hadad - Live Performance",
        "Lior Narkis - Concert",
        "Harel Skaat - Solo Concert",

        // Rock/Alternative
        "Mashina - Reunion Tour",
        "HaYehudim - Concert",
        "Ethnix - Concert",

        // Hip Hop/Rap
        "Subliminal - Live Show",
        "Nechi Nech - Concert",
        "Tuna - Hip Hop Show",

        // Classical/Traditional
        "Idan Raichel - Concert",
        "Shlomo Artzi - Tour 2025",
        "Yehoram Gaon - Special Show"
    )

    val theaterShows = listOf(
        // Habima Theatre
        "Romeo and Juliet",
        "The Cherry Orchard",
        "Death of a Salesman",
        "A Midsummer Night's Dream",

        // Beer Sheva Theatre
        "Waiting for Godot",

        // Israeli Original
        "Kazablan"
    )

    val childrenShows = listOf(
        "Disney On Ice - Israel Tour",
        "Frozen Live",
        "The Lion King - Kids Version",
        "SpongeBob Musical",
        "Michal HaKtana Live Show",
        "Baby Shark Live",
        "Mani HaMatara Live Show",
        "Kofiko Live Show"
    )

    val sportsEvents = listOf(
        // Football
        "Maccabi Tel Aviv vs Hapoel Jerusalem",
        "Hapoel Beer Sheva vs Hapoel Tel Aviv - Football",
        // Basketball
        "Basketball Euroleague Derby",
        "Maccabi Tel Aviv vs Real Madrid - Euroleague",
        "State Cup Final - Maccabi TA vs Hapoel Jerusalem Game 1",
        // Volleyball
        "Maccabi Yedim Tel Aviv vs Hapoel Galil Mate Asher",
        // Tennis
        "Israel Tennis Championship",
        "ATP Challenger Tel Aviv"
    )

    val festivals = listOf(
        "Red Sea Jazz Festival",
        "Israel Festival",
        "InDNegev Festival",
        "Tamar Festival",
        "Hutzot HaYotzer Festival"
    )

    // Returns a rounded random price in ILS based on event type.
    fun getPrice(eventType: String): Double {
        val raw = when (eventType.lowercase()) {
            "stand-up", "comedy" -> (80.0..180.0).random()
            "music_small" -> (120.0..250.0).random()
            "music_large" -> (200.0..450.0).random()
            "theater" -> (150.0..350.0).random()
            "children" -> (80.0..180.0).random()
            "sports" -> (50.0..300.0).random()
            "festival" -> (150.0..500.0).random()
            else -> (100.0..250.0).random()
        }
        return Math.round(raw / 10.0) * 10.0
    }

    private fun ClosedRange<Double>.random(): Double {
        return start + kotlin.random.Random.nextDouble() * (endInclusive - start)
    }
}
