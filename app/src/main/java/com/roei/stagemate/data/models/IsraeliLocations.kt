package com.roei.stagemate.data.models

// Database of Israeli cities and venues with coordinates and capacity.
// Used by IsraeliEventsGenerator, MapFragment, and CityAdapter.
object IsraeliLocations {

    data class City(
        val name: String,
        val nameHebrew: String,
        val latitude: Double,
        val longitude: Double,
        val region: String // North, Center, South
    )

    data class Venue(
        val name: String,
        val nameHebrew: String,
        val city: String,
        val address: String,
        val latitude: Double,
        val longitude: Double,
        val capacity: Int,
        val venueType: String,
        val imageUrl: String = ""
    )

    val cities = listOf(
        // Center
        City("Tel Aviv", "", 32.0853, 34.7818, "Center"),
        City("Ramat Gan", "", 32.0853, 34.8120, "Center"),
        City("Givatayim", "", 32.0697, 34.8112, "Center"),
        City("Holon", "", 32.0117, 34.7750, "Center"),
        City("Bat Yam", "", 32.0167, 34.7500, "Center"),
        City("Rishon LeZion", "", 31.9730, 34.7925, "Center"),
        City("Petah Tikva", "", 32.0853, 34.8883, "Center"),
        City("Raanana", "", 32.1847, 34.8708, "Center"),
        City("Kfar Saba", "", 32.1764, 34.9070, "Center"),
        City("Hod Hasharon", "", 32.1510, 34.8890, "Center"),
        City("Herzliya", "", 32.1624, 34.8443, "Center"),
        City("Ramat HaSharon", "", 32.1428, 34.8413, "Center"),
        City("Netanya", "", 32.3341, 34.8594, "Center"),
        City("Rehovot", "", 31.8944, 34.8090, "Center"),
        City("Ness Ziona", "", 31.9303, 34.7991, "Center"),
        City("Yavne", "", 31.8780, 34.7377, "Center"),
        City("Ashdod", "", 31.8044, 34.6553, "Center"),
        City("Ashkelon", "", 31.6688, 34.5742, "Center"),

        // Jerusalem Area
        City("Jerusalem", "", 31.7683, 35.2137, "Jerusalem"),
        City("Beit Shemesh", "", 31.7522, 34.9888, "Jerusalem"),
        City("Modiin", "", 31.8969, 35.0095, "Jerusalem"),

        // North
        City("Haifa", "", 32.8191, 34.9983, "North"),
        City("Nahariya", "", 33.0083, 35.0950, "North"),
        City("Acre", "", 32.9283, 35.0833, "North"),
        City("Karmiel", "", 32.9186, 35.2958, "North"),
        City("Tiberias", "", 32.7950, 35.5308, "North"),
        City("Afula", "", 32.6078, 35.2897, "North"),
        City("Kiryat Shmona", "", 33.2069, 35.5706, "North"),

        // South
        City("Beer Sheva", "", 31.2518, 34.7913, "South"),
        City("Eilat", "", 29.5577, 34.9519, "South"),
        City("Dimona", "", 31.0686, 35.0330, "South"),
        City("Arad", "", 31.2590, 35.2130, "South")
    )

    val venues = listOf(
        // Tel Aviv
        Venue("Park HaYarkon", "", "Tel Aviv", "HaYarkon St 90", 32.1060, 34.8021, 50000, "Outdoor Arena",
            "https://images.unsplash.com/photo-1502781252888-9143ba7f074e?w=800&q=80"),
        Venue("Menora Mivtachim Arena", "", "Tel Aviv", "Yigal Alon 51", 32.0568, 34.7889, 11500, "Indoor Arena",
            "https://images.unsplash.com/photo-1546519638-68e109498ffc?w=800&q=80"),
        Venue("Habima Theatre", "", "Tel Aviv", "Habima Square 2", 32.0729, 34.7744, 1000, "Theater",
            "https://images.unsplash.com/photo-1503095396549-807759245b35?w=800&q=80"),
        Venue("Cameri Theatre", "", "Tel Aviv", "Shaul HaMelech 19", 32.0779, 34.7791, 500, "Theater",
            "https://images.unsplash.com/photo-1507924538820-ede94a04019d?w=800&q=80"),
        Venue("Charles Bronfman Auditorium", "", "Tel Aviv", "Huberman 1", 32.0770, 34.7820, 3000, "Concert Hall",
            "https://images.unsplash.com/photo-1518837695005-2083093ee35b?w=800&q=80"),
        Venue("Reading 3", "", "Tel Aviv", "Atarim Square", 32.0827, 34.7719, 2000, "Concert Venue",
            "https://images.unsplash.com/photo-1514320291840-2e0a9bf2a9ae?w=800&q=80"),
        Venue("Barby Club", "", "Tel Aviv", "Kibbutz Galuyot 52", 32.0551, 34.7559, 500, "Live Music Club",
            "https://images.unsplash.com/photo-1501612780327-45045538702b?w=800&q=80"),
        Venue("The Comedy Bar", "", "Tel Aviv", "Nahalat Binyamin 34", 32.0635, 34.7694, 200, "Comedy Club",
            "https://images.unsplash.com/photo-1585699324551-f6c309eedeca?w=800&q=80"),
        Venue("Zappa Tel Aviv", "", "Tel Aviv", "Raoul Wallenberg 30", 32.0954, 34.7974, 800, "Live Music Club",
            "https://images.unsplash.com/photo-1501281668745-f7f57925c3b4?w=800&q=80"),
        Venue("Tmuna Theatre", "", "Tel Aviv", "Shonzino 8", 32.0697, 34.7675, 250, "Theater",
            "https://images.unsplash.com/photo-1478720568477-152d9b164e26?w=800&q=80"),
        Venue("Bloomfield Stadium", "", "Tel Aviv", "HaTikva Quarter", 32.0625, 34.7700, 26000, "Sports Stadium",
            "https://images.unsplash.com/photo-1574629810360-7efbbe195018?w=800&q=80"),
        Venue("Gesher Theatre", "", "Tel Aviv", "Yehudit Gardens", 32.0520, 34.7565, 400, "Theater",
            "https://images.unsplash.com/photo-1516715094483-75da06c15fd1?w=800&q=80"),
        Venue("Suzanne Dellal Centre", "", "Tel Aviv", "Yehieli 5", 32.0598, 34.7645, 500, "Dance & Theater",
            "https://images.unsplash.com/photo-1518611012118-696072aa579a?w=800&q=80"),

        // Jerusalem
        Venue("International Convention Center", "", "Jerusalem", "Zalman Shazar 1", 31.7801, 35.2035, 3000, "Convention Center",
            "https://images.unsplash.com/photo-1519167758481-83f29da8a77b?w=800&q=80"),
        Venue("Jerusalem Theatre", "", "Jerusalem", "David Marcus 20", 31.7721, 35.2188, 950, "Theater",
            "https://images.unsplash.com/photo-1503095396549-807759245b35?w=800&q=80"),
        Venue("Khan Theatre", "", "Jerusalem", "David Remez 2", 31.7683, 35.2255, 250, "Theater",
            "https://images.unsplash.com/photo-1478720568477-152d9b164e26?w=800&q=80"),
        Venue("Confederation House", "", "Jerusalem", "Emile Botta 12", 31.7733, 35.2177, 400, "Concert Hall",
            "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=800&q=80"),
        Venue("First Station", "", "Jerusalem", "David Remez 4", 31.7648, 35.2269, 2000, "Entertainment Complex",
            "https://images.unsplash.com/photo-1533137354742-b8066e04aeff?w=800&q=80"),
        Venue("Sultan's Pool", "", "Jerusalem", "Sultan's Pool", 31.7722, 35.2274, 5000, "Outdoor Amphitheater",
            "https://images.unsplash.com/photo-1533137354742-b8066e04aeff?w=800&q=80"),

        // Haifa
        Venue("Haifa Auditorium", "", "Haifa", "Haifa University", 32.7740, 35.0235, 1500, "Concert Hall",
            "https://images.unsplash.com/photo-1518837695005-2083093ee35b?w=800&q=80"),
        Venue("Haifa Theatre", "", "Haifa", "Pica 50", 32.8150, 34.9894, 600, "Theater",
            "https://images.unsplash.com/photo-1507924538820-ede94a04019d?w=800&q=80"),
        Venue("Zappa Haifa", "", "Haifa", "Horev Center", 32.7985, 34.9894, 600, "Live Music Club",
            "https://images.unsplash.com/photo-1470229722913-7c0e2dbbafd3?w=800&q=80"),

        // Beer Sheva
        Venue("Performing Arts Center", "", "Beer Sheva", "Henrietta Szold 14", 31.2443, 34.8101, 1500, "Theater & Concert Hall",
            "https://images.unsplash.com/photo-1518837695005-2083093ee35b?w=800&q=80"),
        Venue("Beer Sheva Theatre", "", "Beer Sheva", "Henrietta Szold 14", 31.2443, 34.8101, 450, "Theater",
            "https://images.unsplash.com/photo-1583795128727-6ec3642408f8?w=800&q=80"),

        // Caesarea
        Venue("Caesarea Amphitheater", "", "Caesarea", "Caesarea National Park", 32.5010, 34.8933, 4000, "Ancient Amphitheater",
            "https://images.unsplash.com/photo-1459749411175-04bf5292ceea?w=800&q=80"),

        // Rishon LeZion
        Venue("Heichal HaTarbut", "", "Rishon LeZion", "Shoham 3", 31.9651, 34.7987, 1500, "Concert Hall",
            "https://images.unsplash.com/photo-1518837695005-2083093ee35b?w=800&q=80"),
        Venue("Zappa Rishon", "", "Rishon LeZion", "Moshe Levi 1", 31.9690, 34.7954, 600, "Live Music Club",
            "https://images.unsplash.com/photo-1501281668745-f7f57925c3b4?w=800&q=80"),
        Venue("Live Park Rishon LeZion", "", "Rishon LeZion", "Moshe Levi 1", 31.9693, 34.7956, 10000, "Outdoor Arena",
            "https://images.unsplash.com/photo-1470229722913-7c0e2dbbafd3?w=800&q=80"),

        // Petah Tikva
        Venue("Heichal HaTarbut", "", "Petah Tikva", "Em HaMoshavot 4", 32.0861, 34.8867, 1200, "Concert Hall",
            "https://images.unsplash.com/photo-1518837695005-2083093ee35b?w=800&q=80"),

        // Herzliya
        Venue("Herzliya Performing Arts Center", "", "Herzliya", "Sokolov 4", 32.1655, 34.8397, 800, "Theater & Concert Hall",
            "https://images.unsplash.com/photo-1516715094483-75da06c15fd1?w=800&q=80"),

        // Ra'anana
        Venue("Heichal HaTarbut", "", "Raanana", "Ahuza 23", 32.1850, 34.8715, 1000, "Concert Hall",
            "https://images.unsplash.com/photo-1518837695005-2083093ee35b?w=800&q=80"),

        // Netanya
        Venue("Heichal HaTarbut", "", "Netanya", "David Hamelech 24", 32.3325, 34.8531, 1200, "Concert Hall",
            "https://images.unsplash.com/photo-1518837695005-2083093ee35b?w=800&q=80"),

        // Eilat
        Venue("Ice Space", "", "Eilat", "Kampen Center", 29.5497, 34.9545, 1500, "Entertainment Complex",
            "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?w=800&q=80"),

        // Rehovot
        Venue("Heichal HaTarbut", "", "Rehovot", "Herzl 58", 31.8951, 34.8042, 1000, "Concert Hall",
            "https://images.unsplash.com/photo-1518837695005-2083093ee35b?w=800&q=80"),

        // Holon
        Venue("Holon Theatre", "", "Holon", "Golomb 82", 32.0159, 34.7810, 600, "Theater",
            "https://images.unsplash.com/photo-1507924538820-ede94a04019d?w=800&q=80"),
        Venue("Holon Cinematheque", "", "Holon", "Golda Meir 17", 32.0142, 34.7742, 300, "Cinema & Theater",
            "https://images.unsplash.com/photo-1577083552431-6e5fd01988ec?w=800&q=80"),

        // Ramat Gan
        Venue("Ramat Gan Stadium", "", "Ramat Gan", "Abba Hillel Silver 11", 32.0948, 34.8189, 41583, "Sports Stadium",
            "https://images.unsplash.com/photo-1574629810360-7efbbe195018?w=800&q=80"),

        // Hadar Yosef (Tel Aviv, north — volleyball/handball center)
        Venue("Hadar Yosef Sports Center", "", "Tel Aviv", "Levi Eshkol Blvd 70", 32.1097, 34.8186, 2500, "Sports Arena",
            "https://images.unsplash.com/photo-1612872087720-bb876e2e67d1?w=800&q=80"),

        // Ramat HaSharon (Israel Tennis Center — Canada Stadium)
        Venue("Israel Tennis Center Ramat HaSharon", "", "Ramat HaSharon", "HaShoftim 1", 32.1428, 34.8413, 4500, "Tennis Stadium",
            "https://images.unsplash.com/photo-1554068865-24cecd4e34b8?w=800&q=80"),

        // Ashdod
        Venue("MonArt Center", "", "Ashdod", "Ashdod Cultural Center", 31.8044, 34.6553, 1200, "Art Center",
            "https://images.unsplash.com/photo-1577083552431-6e5fd01988ec?w=800&q=80"),

        // Comedy Clubs
        Venue("Comedy Bar Tel Aviv", "", "Tel Aviv", "Lilienblum 48", 32.0598, 34.7682, 180, "Comedy Club",
            "https://images.unsplash.com/photo-1516450137517-162bfbeb8dba?w=800&q=80"),
    )

    fun getCityByName(name: String): City? {
        return cities.find { it.name.equals(name, ignoreCase = true) }
    }

    fun getVenuesByCity(cityName: String): List<Venue> {
        return venues.filter { it.city.equals(cityName, ignoreCase = true) }
    }

    fun getVenueByType(type: String): List<Venue> {
        return venues.filter { it.venueType.equals(type, ignoreCase = true) }
    }
}
