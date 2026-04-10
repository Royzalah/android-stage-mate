package com.roei.stagemate.data.models

import com.roei.stagemate.utilities.Constants
import com.roei.stagemate.data.models.SectionPosition.ArenaPosition
import com.roei.stagemate.data.models.SectionPosition.StadiumPosition
import com.roei.stagemate.data.models.SectionPosition.TheaterPosition

// Predefined seat layouts for Israeli venues (arenas, theaters, stadiums, clubs).
// Each template includes SectionPosition data for visual venue map rendering.
// Used by SeatSelectionActivity to create SeatMap for a given event/venue.
object VenueTemplates {

    // --- ARENA LAYOUTS ---

    fun createMenoraSportsLayout(eventId: String, basePrice: Double = 150.0): SeatMap {
        val sections = mutableListOf<SeatSection>()

        // Lower Bowl (tierIndex=0)
        sections.add(SeatSection.create("Lower 1", 8, 30, basePrice, 1.3, Constants.SeatColors.GREEN, 'A', false,
            position = ArenaPosition(240f, 60f, tierIndex = 0), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Lower 2", 8, 30, basePrice, 1.0, Constants.SeatColors.GREEN, 'A', false,
            position = ArenaPosition(300f, 60f, tierIndex = 0), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Lower 3", 8, 30, basePrice, 1.0, Constants.SeatColors.GREEN, 'A', false,
            position = ArenaPosition(0f, 60f, tierIndex = 0), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Lower 4", 8, 30, basePrice, 1.3, Constants.SeatColors.GREEN, 'A', false,
            position = ArenaPosition(60f, 60f, tierIndex = 0), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Lower 5", 8, 30, basePrice, 1.0, Constants.SeatColors.GREEN, 'A', false,
            position = ArenaPosition(120f, 60f, tierIndex = 0), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Lower 6", 8, 30, basePrice, 1.0, Constants.SeatColors.GREEN, 'A', false,
            position = ArenaPosition(180f, 60f, tierIndex = 0), hasAccessibleSeating = true))

        // VIP Ring (tierIndex=1)
        sections.add(SeatSection.create("VIP 1", 3, 20, basePrice, 3.0, Constants.SeatColors.GOLD, 'A', false,
            position = ArenaPosition(240f, 60f, tierIndex = 1), hasAccessibleSeating = false))
        sections.add(SeatSection.create("VIP 2", 3, 20, basePrice, 2.5, Constants.SeatColors.GOLD, 'A', false,
            position = ArenaPosition(300f, 60f, tierIndex = 1), hasAccessibleSeating = false))
        sections.add(SeatSection.create("VIP 3", 3, 20, basePrice, 2.5, Constants.SeatColors.GOLD, 'A', false,
            position = ArenaPosition(0f, 60f, tierIndex = 1), hasAccessibleSeating = false))
        sections.add(SeatSection.create("VIP 4", 3, 20, basePrice, 3.0, Constants.SeatColors.GOLD, 'A', false,
            position = ArenaPosition(60f, 60f, tierIndex = 1), hasAccessibleSeating = false))
        sections.add(SeatSection.create("VIP 5", 3, 20, basePrice, 2.5, Constants.SeatColors.GOLD, 'A', false,
            position = ArenaPosition(120f, 60f, tierIndex = 1), hasAccessibleSeating = false))
        sections.add(SeatSection.create("VIP 6", 3, 20, basePrice, 2.5, Constants.SeatColors.GOLD, 'A', false,
            position = ArenaPosition(180f, 60f, tierIndex = 1), hasAccessibleSeating = false))

        // Upper Bowl (tierIndex=2)
        sections.add(SeatSection.create("Upper 1", 12, 40, basePrice, 0.85, Constants.SeatColors.BLUE, 'A', false,
            position = ArenaPosition(240f, 60f, tierIndex = 2), hasAccessibleSeating = false))
        sections.add(SeatSection.create("Upper 2", 12, 40, basePrice, 0.7, Constants.SeatColors.BLUE, 'A', false,
            position = ArenaPosition(300f, 60f, tierIndex = 2), hasAccessibleSeating = false))
        sections.add(SeatSection.create("Upper 3", 12, 40, basePrice, 0.7, Constants.SeatColors.BLUE, 'A', false,
            position = ArenaPosition(0f, 60f, tierIndex = 2), hasAccessibleSeating = false))
        sections.add(SeatSection.create("Upper 4", 12, 40, basePrice, 0.85, Constants.SeatColors.BLUE, 'A', false,
            position = ArenaPosition(60f, 60f, tierIndex = 2), hasAccessibleSeating = false))
        sections.add(SeatSection.create("Upper 5", 12, 40, basePrice, 0.7, Constants.SeatColors.BLUE, 'A', false,
            position = ArenaPosition(120f, 60f, tierIndex = 2), hasAccessibleSeating = false))
        sections.add(SeatSection.create("Upper 6", 12, 40, basePrice, 0.7, Constants.SeatColors.BLUE, 'A', false,
            position = ArenaPosition(180f, 60f, tierIndex = 2), hasAccessibleSeating = false))

        return SeatMap(id = eventId, eventId = eventId, sections = sections, venueLayoutType = SeatMap.VenueLayoutType.ARENA)
    }

    fun createMenoraConcertLayout(eventId: String, basePrice: Double = 200.0): SeatMap {
        // Same sections as sports layout — only the stage/court visual differs
        return createMenoraSportsLayout(eventId, basePrice)
    }

    fun createIceSpaceLayout(eventId: String, basePrice: Double = 120.0): SeatMap {
        val sections = mutableListOf<SeatSection>()

        // Lower Stand (tierIndex=0)
        sections.add(SeatSection.create("Lower Center", 5, 25, basePrice, 1.3, Constants.SeatColors.RED, 'A', false,
            position = ArenaPosition(240f, 90f, tierIndex = 0), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Lower Right", 5, 25, basePrice, 1.0, Constants.SeatColors.ORANGE, 'A', false,
            position = ArenaPosition(330f, 90f, tierIndex = 0), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Lower Left", 5, 25, basePrice, 1.0, Constants.SeatColors.DEEP_ORANGE, 'A', false,
            position = ArenaPosition(150f, 90f, tierIndex = 0), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Lower Back", 5, 25, basePrice, 0.8, Constants.SeatColors.GREEN, 'A', false,
            position = ArenaPosition(60f, 90f, tierIndex = 0), hasAccessibleSeating = true))

        // Upper Stand (tierIndex=1)
        sections.add(SeatSection.create("Upper Center", 5, 20, basePrice, 0.7, Constants.SeatColors.BLUE, 'A', false,
            position = ArenaPosition(240f, 90f, tierIndex = 1), hasAccessibleSeating = false))
        sections.add(SeatSection.create("Upper Right", 5, 20, basePrice, 0.6, Constants.SeatColors.LIGHT_BLUE, 'A', false,
            position = ArenaPosition(330f, 90f, tierIndex = 1), hasAccessibleSeating = false))
        sections.add(SeatSection.create("Upper Left", 5, 20, basePrice, 0.6, Constants.SeatColors.MEDIUM_BLUE, 'A', false,
            position = ArenaPosition(150f, 90f, tierIndex = 1), hasAccessibleSeating = false))
        sections.add(SeatSection.create("Upper Back", 5, 20, basePrice, 0.5, Constants.SeatColors.DARK_BLUE, 'A', false,
            position = ArenaPosition(60f, 90f, tierIndex = 1), hasAccessibleSeating = false))

        return SeatMap(id = eventId, eventId = eventId, sections = sections, venueLayoutType = SeatMap.VenueLayoutType.ARENA)
    }

    fun createTennisCenterLayout(eventId: String, basePrice: Double = 180.0): SeatMap {
        val sections = mutableListOf<SeatSection>()

        // Lower Stand (tierIndex=0)
        sections.add(SeatSection.create("Lower North", 8, 30, basePrice, 1.4, Constants.SeatColors.RED, 'A', false,
            position = ArenaPosition(270f, 90f, tierIndex = 0), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Lower South", 8, 30, basePrice, 1.4, Constants.SeatColors.RED, 'A', false,
            position = ArenaPosition(90f, 90f, tierIndex = 0), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Lower East", 8, 25, basePrice, 1.0, Constants.SeatColors.ORANGE, 'A', false,
            position = ArenaPosition(0f, 90f, tierIndex = 0), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Lower West", 8, 25, basePrice, 1.0, Constants.SeatColors.DEEP_ORANGE, 'A', false,
            position = ArenaPosition(180f, 90f, tierIndex = 0), hasAccessibleSeating = true))

        // Upper Stand (tierIndex=1)
        sections.add(SeatSection.create("Upper North", 10, 35, basePrice, 0.7, Constants.SeatColors.BLUE, 'A', false,
            position = ArenaPosition(270f, 90f, tierIndex = 1), hasAccessibleSeating = false))
        sections.add(SeatSection.create("Upper South", 10, 35, basePrice, 0.7, Constants.SeatColors.BLUE, 'A', false,
            position = ArenaPosition(90f, 90f, tierIndex = 1), hasAccessibleSeating = false))
        sections.add(SeatSection.create("Upper East", 10, 30, basePrice, 0.5, Constants.SeatColors.LIGHT_BLUE, 'A', false,
            position = ArenaPosition(0f, 90f, tierIndex = 1), hasAccessibleSeating = false))
        sections.add(SeatSection.create("Upper West", 10, 30, basePrice, 0.5, Constants.SeatColors.MEDIUM_BLUE, 'A', false,
            position = ArenaPosition(180f, 90f, tierIndex = 1), hasAccessibleSeating = false))

        return SeatMap(id = eventId, eventId = eventId, sections = sections, venueLayoutType = SeatMap.VenueLayoutType.ARENA)
    }

    // --- STADIUM LAYOUTS ---

    fun createBloomfieldSportsLayout(eventId: String, basePrice: Double = 100.0): SeatMap {
        val sections = mutableListOf<SeatSection>()

        sections.add(SeatSection.create("Gate 11", 18, 55, basePrice, 1.2, Constants.SeatColors.RED, 'A', false,
            position = StadiumPosition(StadiumPosition.Side.NORTH, 0, 1), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Gate 13", 18, 55, basePrice, 1.0, Constants.SeatColors.GREEN, 'A', false,
            position = StadiumPosition(StadiumPosition.Side.EAST, 0, 2), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Gate 5", 18, 55, basePrice, 1.2, Constants.SeatColors.ORANGE, 'A', false,
            position = StadiumPosition(StadiumPosition.Side.SOUTH, 0, 1), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Gate 8", 18, 55, basePrice, 1.0, Constants.SeatColors.GREEN, 'A', false,
            position = StadiumPosition(StadiumPosition.Side.WEST, 0, 2), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Gate 2", 22, 65, basePrice, 0.6, Constants.SeatColors.BLUE, 'A', false,
            position = StadiumPosition(StadiumPosition.Side.EAST, 1, 2), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Gate 7", 22, 65, basePrice, 0.6, Constants.SeatColors.BLUE, 'A', false,
            position = StadiumPosition(StadiumPosition.Side.WEST, 1, 2), hasAccessibleSeating = true))

        return SeatMap(id = eventId, eventId = eventId, sections = sections, venueLayoutType = SeatMap.VenueLayoutType.STADIUM)
    }

    fun createBloomfieldConcertLayout(eventId: String, basePrice: Double = 200.0): SeatMap {
        val sections = mutableListOf<SeatSection>()

        sections.add(SeatSection.create("VIP", 8, 35, basePrice, 2.5, Constants.SeatColors.GOLD, 'A', false,
            position = StadiumPosition(StadiumPosition.Side.SOUTH, 0, 1), hasAccessibleSeating = true))
        sections.add(SeatSection.create("East Lower", 18, 55, basePrice, 1.0, Constants.SeatColors.ORANGE, 'A', false,
            position = StadiumPosition(StadiumPosition.Side.EAST, 0, 2), hasAccessibleSeating = true))
        sections.add(SeatSection.create("West Lower", 18, 55, basePrice, 1.0, Constants.SeatColors.DEEP_ORANGE, 'A', false,
            position = StadiumPosition(StadiumPosition.Side.WEST, 0, 2), hasAccessibleSeating = true))
        sections.add(SeatSection.create("East Upper", 22, 65, basePrice, 0.7, Constants.SeatColors.DARK_BLUE, 'A', false,
            position = StadiumPosition(StadiumPosition.Side.EAST, 1, 2), hasAccessibleSeating = true))
        sections.add(SeatSection.create("West Upper", 22, 65, basePrice, 0.7, Constants.SeatColors.MEDIUM_BLUE, 'A', false,
            position = StadiumPosition(StadiumPosition.Side.WEST, 1, 2), hasAccessibleSeating = true))

        // Behind-stage section (blocked)
        sections.add(SeatSection.create("Back", 15, 70, basePrice, 0.4, Constants.SeatColors.GRAY, 'A', false,
            position = StadiumPosition(StadiumPosition.Side.NORTH, 0, 1), hasAccessibleSeating = false,
            isBlocked = true))

        // Floor zones (standing/GA)
        sections.add(SeatSection.create("Golden Ring", 1, 200, basePrice, 3.0, Constants.SeatColors.GOLD, 'A', false,
            position = SectionPosition.FloorPosition(zoneIndex = 0, totalZones = 2)))
        sections.add(SeatSection.create("Floor", 1, 300, basePrice, 1.8, Constants.SeatColors.AMBER, 'A', false,
            position = SectionPosition.FloorPosition(zoneIndex = 1, totalZones = 2)))

        return SeatMap(id = eventId, eventId = eventId, sections = sections, venueLayoutType = SeatMap.VenueLayoutType.STADIUM)
    }

    fun createRamatGanLayout(eventId: String, basePrice: Double = 120.0): SeatMap {
        val sections = mutableListOf<SeatSection>()

        sections.add(SeatSection.create("VIP West", 5, 30, basePrice, 2.5, Constants.SeatColors.GOLD, 'A', false,
            position = StadiumPosition(StadiumPosition.Side.WEST, 0, 2), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Lower West Tribune", 15, 50, basePrice, 1.2, Constants.SeatColors.RED, 'A', false,
            position = StadiumPosition(StadiumPosition.Side.WEST, 1, 2), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Lower East Tribune", 15, 50, basePrice, 1.0, Constants.SeatColors.GREEN, 'A', false,
            position = StadiumPosition(StadiumPosition.Side.EAST, 0, 1), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Lower North Tribune", 12, 40, basePrice, 1.0, Constants.SeatColors.AMBER, 'A', false,
            position = StadiumPosition(StadiumPosition.Side.NORTH, 0, 1), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Lower South Tribune", 12, 40, basePrice, 1.0, Constants.SeatColors.ORANGE, 'A', false,
            position = StadiumPosition(StadiumPosition.Side.SOUTH, 0, 1), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Upper West", 20, 55, basePrice, 0.6, Constants.SeatColors.BLUE, 'A', false,
            position = StadiumPosition(StadiumPosition.Side.WEST, 1, 2), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Upper East", 20, 55, basePrice, 0.6, Constants.SeatColors.LIGHT_BLUE, 'A', false,
            position = StadiumPosition(StadiumPosition.Side.EAST, 1, 2), hasAccessibleSeating = true))

        return SeatMap(id = eventId, eventId = eventId, sections = sections, venueLayoutType = SeatMap.VenueLayoutType.STADIUM)
    }

    // --- LARGE AMPHITHEATER / OUTDOOR LAYOUTS ---

    fun createCaesareaLayout(eventId: String, basePrice: Double = 250.0): SeatMap {
        val sections = mutableListOf<SeatSection>()

        sections.add(SeatSection.create("VIP", 3, 25, basePrice, 2.0, Constants.SeatColors.GOLD, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.FRONT), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Left", 6, 15, basePrice, 1.2, Constants.SeatColors.RED, 'D', false,
            position = TheaterPosition(TheaterPosition.Column.LEFT, TheaterPosition.Tier.FRONT), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Center", 8, 20, basePrice, 1.2, Constants.SeatColors.ORANGE, 'D', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.MIDDLE), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Right", 6, 15, basePrice, 1.2, Constants.SeatColors.DEEP_ORANGE, 'D', false,
            position = TheaterPosition(TheaterPosition.Column.RIGHT, TheaterPosition.Tier.FRONT), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Upper Left", 8, 18, basePrice, 0.8, Constants.SeatColors.GREEN, 'L', false,
            position = TheaterPosition(TheaterPosition.Column.LEFT, TheaterPosition.Tier.BACK), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Upper Stand", 10, 25, basePrice, 0.8, Constants.SeatColors.LIGHT_GREEN, 'L', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.BACK), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Upper Right", 8, 18, basePrice, 0.8, Constants.SeatColors.DARK_GREEN, 'L', false,
            position = TheaterPosition(TheaterPosition.Column.RIGHT, TheaterPosition.Tier.BACK), hasAccessibleSeating = true))

        return SeatMap(id = eventId, eventId = eventId, sections = sections, venueLayoutType = SeatMap.VenueLayoutType.THEATER)
    }

    fun createSultansPoolLayout(eventId: String, basePrice: Double = 200.0): SeatMap {
        val sections = mutableListOf<SeatSection>()

        sections.add(SeatSection.create("VIP", 5, 30, basePrice, 2.0, Constants.SeatColors.GOLD, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.FRONT), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Left", 8, 18, basePrice, 1.2, Constants.SeatColors.RED, 'F', false,
            position = TheaterPosition(TheaterPosition.Column.LEFT, TheaterPosition.Tier.MIDDLE), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Center", 10, 25, basePrice, 1.2, Constants.SeatColors.ORANGE, 'F', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.MIDDLE), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Right", 8, 18, basePrice, 1.2, Constants.SeatColors.DEEP_ORANGE, 'F', false,
            position = TheaterPosition(TheaterPosition.Column.RIGHT, TheaterPosition.Tier.MIDDLE), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Upper Stand", 20, 40, basePrice, 0.8, Constants.SeatColors.GREEN, 'P', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.BACK), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Back Stand", 15, 45, basePrice, 0.5, Constants.SeatColors.BLUE, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.BALCONY), hasAccessibleSeating = true))

        return SeatMap(id = eventId, eventId = eventId, sections = sections, venueLayoutType = SeatMap.VenueLayoutType.THEATER)
    }

    fun createParkHaYarkonLayout(eventId: String, basePrice: Double = 250.0): SeatMap {
        val sections = mutableListOf<SeatSection>()

        sections.add(SeatSection.create("VIP", 5, 40, basePrice, 2.5, Constants.SeatColors.GOLD, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.FRONT), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Center", 15, 60, basePrice, 1.2, Constants.SeatColors.RED, 'F', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.MIDDLE), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Left", 15, 50, basePrice, 1.0, Constants.SeatColors.GREEN, 'F', false,
            position = TheaterPosition(TheaterPosition.Column.LEFT, TheaterPosition.Tier.MIDDLE), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Right", 15, 50, basePrice, 1.0, Constants.SeatColors.LIGHT_GREEN, 'F', false,
            position = TheaterPosition(TheaterPosition.Column.RIGHT, TheaterPosition.Tier.MIDDLE), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Back Stand", 20, 80, basePrice, 0.5, Constants.SeatColors.LIME_GREEN, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.BACK), hasAccessibleSeating = true))

        return SeatMap(id = eventId, eventId = eventId, sections = sections, venueLayoutType = SeatMap.VenueLayoutType.CONCERT_STAGE)
    }

    fun createLiveParkLayout(eventId: String, basePrice: Double = 200.0): SeatMap {
        val sections = mutableListOf<SeatSection>()

        sections.add(SeatSection.create("VIP", 4, 30, basePrice, 2.5, Constants.SeatColors.GOLD, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.FRONT), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Lower Stand", 10, 40, basePrice, 1.3, Constants.SeatColors.RED, 'E', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.MIDDLE), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Left", 10, 30, basePrice, 1.0, Constants.SeatColors.GREEN, 'E', false,
            position = TheaterPosition(TheaterPosition.Column.LEFT, TheaterPosition.Tier.MIDDLE), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Right", 10, 30, basePrice, 1.0, Constants.SeatColors.LIGHT_GREEN, 'E', false,
            position = TheaterPosition(TheaterPosition.Column.RIGHT, TheaterPosition.Tier.MIDDLE), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Back Stand", 15, 50, basePrice, 0.6, Constants.SeatColors.BLUE, 'O', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.BACK), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Upper Stand", 10, 60, basePrice, 0.4, Constants.SeatColors.LIME_GREEN, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.BALCONY), hasAccessibleSeating = true))

        return SeatMap(id = eventId, eventId = eventId, sections = sections, venueLayoutType = SeatMap.VenueLayoutType.CONCERT_STAGE)
    }

    // --- LARGE THEATER / CONCERT HALL LAYOUTS ---

    fun createHabimaLayout(eventId: String, basePrice: Double = 200.0): SeatMap {
        val sections = mutableListOf<SeatSection>()

        sections.add(SeatSection.create("Lower Stand", 5, 25, basePrice, 1.5, Constants.SeatColors.RED, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.FRONT), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Left", 5, 12, basePrice, 1.3, Constants.SeatColors.ORANGE, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.LEFT, TheaterPosition.Tier.FRONT), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Right", 5, 12, basePrice, 1.3, Constants.SeatColors.DEEP_ORANGE, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.RIGHT, TheaterPosition.Tier.FRONT), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Center", 9, 28, basePrice, 1.0, Constants.SeatColors.GREEN, 'F', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.MIDDLE), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Upper Left", 3, 10, basePrice, 0.7, Constants.SeatColors.BLUE, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.LEFT, TheaterPosition.Tier.BALCONY), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Upper Stand", 6, 22, basePrice, 0.7, Constants.SeatColors.LIGHT_BLUE, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.BALCONY), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Upper Right", 3, 10, basePrice, 0.7, Constants.SeatColors.MEDIUM_BLUE, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.RIGHT, TheaterPosition.Tier.BALCONY), hasAccessibleSeating = true))

        return SeatMap(id = eventId, eventId = eventId, sections = sections, venueLayoutType = SeatMap.VenueLayoutType.THEATER)
    }

    fun createCameriLayout(eventId: String, basePrice: Double = 180.0): SeatMap {
        val sections = mutableListOf<SeatSection>()

        sections.add(SeatSection.create("Left", 4, 10, basePrice, 1.4, Constants.SeatColors.RED, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.LEFT, TheaterPosition.Tier.FRONT), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Center", 4, 12, basePrice, 1.5, Constants.SeatColors.ORANGE, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.FRONT), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Right", 4, 10, basePrice, 1.4, Constants.SeatColors.DEEP_ORANGE, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.RIGHT, TheaterPosition.Tier.FRONT), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Back Stand", 8, 25, basePrice, 1.0, Constants.SeatColors.GREEN, 'E', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.MIDDLE), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Upper Stand", 5, 20, basePrice, 0.7, Constants.SeatColors.BLUE, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.BALCONY), hasAccessibleSeating = true))

        return SeatMap(id = eventId, eventId = eventId, sections = sections, venueLayoutType = SeatMap.VenueLayoutType.THEATER)
    }

    fun createBronfmanLayout(eventId: String, basePrice: Double = 250.0): SeatMap {
        val sections = mutableListOf<SeatSection>()

        sections.add(SeatSection.create("Lower Left", 4, 10, basePrice, 1.5, Constants.SeatColors.RED, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.LEFT, TheaterPosition.Tier.FRONT), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Lower Stand", 8, 20, basePrice, 1.6, Constants.SeatColors.DARK_RED, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.FRONT), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Lower Right", 4, 10, basePrice, 1.5, Constants.SeatColors.DEEP_ORANGE, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.RIGHT, TheaterPosition.Tier.FRONT), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Left", 6, 12, basePrice, 1.0, Constants.SeatColors.GREEN, 'I', false,
            position = TheaterPosition(TheaterPosition.Column.LEFT, TheaterPosition.Tier.MIDDLE), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Center", 12, 22, basePrice, 1.1, Constants.SeatColors.MEDIUM_GREEN, 'I', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.MIDDLE), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Right", 6, 12, basePrice, 1.0, Constants.SeatColors.LIGHT_GREEN, 'I', false,
            position = TheaterPosition(TheaterPosition.Column.RIGHT, TheaterPosition.Tier.MIDDLE), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Upper Left", 5, 10, basePrice, 0.7, Constants.SeatColors.BLUE, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.LEFT, TheaterPosition.Tier.BALCONY), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Upper Stand", 10, 20, basePrice, 0.7, Constants.SeatColors.LIGHT_BLUE, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.BALCONY), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Upper Right", 5, 10, basePrice, 0.7, Constants.SeatColors.MEDIUM_BLUE, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.RIGHT, TheaterPosition.Tier.BALCONY), hasAccessibleSeating = true))

        return SeatMap(id = eventId, eventId = eventId, sections = sections, venueLayoutType = SeatMap.VenueLayoutType.THEATER)
    }

    fun createBinyaneiHaumaLayout(eventId: String, basePrice: Double = 200.0): SeatMap {
        val sections = mutableListOf<SeatSection>()

        sections.add(SeatSection.create("Lower Left", 4, 12, basePrice, 1.5, Constants.SeatColors.RED, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.LEFT, TheaterPosition.Tier.FRONT), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Lower Stand", 8, 20, basePrice, 1.6, Constants.SeatColors.DARK_RED, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.FRONT), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Lower Right", 4, 12, basePrice, 1.5, Constants.SeatColors.DEEP_ORANGE, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.RIGHT, TheaterPosition.Tier.FRONT), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Center", 12, 35, basePrice, 1.0, Constants.SeatColors.GREEN, 'I', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.MIDDLE), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Upper Left", 5, 12, basePrice, 0.7, Constants.SeatColors.BLUE, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.LEFT, TheaterPosition.Tier.BALCONY), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Upper Stand", 10, 20, basePrice, 0.7, Constants.SeatColors.LIGHT_BLUE, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.BALCONY), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Upper Right", 5, 12, basePrice, 0.7, Constants.SeatColors.MEDIUM_BLUE, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.RIGHT, TheaterPosition.Tier.BALCONY), hasAccessibleSeating = true))

        return SeatMap(id = eventId, eventId = eventId, sections = sections, venueLayoutType = SeatMap.VenueLayoutType.THEATER)
    }

    fun createJerusalemTheatreLayout(eventId: String, basePrice: Double = 190.0): SeatMap {
        val sections = mutableListOf<SeatSection>()

        sections.add(SeatSection.create("Lower Stand", 5, 22, basePrice, 1.5, Constants.SeatColors.RED, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.FRONT), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Left", 6, 12, basePrice, 1.0, Constants.SeatColors.GREEN, 'F', false,
            position = TheaterPosition(TheaterPosition.Column.LEFT, TheaterPosition.Tier.MIDDLE), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Right", 6, 12, basePrice, 1.0, Constants.SeatColors.LIGHT_GREEN, 'F', false,
            position = TheaterPosition(TheaterPosition.Column.RIGHT, TheaterPosition.Tier.MIDDLE), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Upper Stand", 6, 22, basePrice, 0.7, Constants.SeatColors.BLUE, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.BALCONY), hasAccessibleSeating = true))

        return SeatMap(id = eventId, eventId = eventId, sections = sections, venueLayoutType = SeatMap.VenueLayoutType.THEATER)
    }

    fun createPerformingArtsBeerShevaLayout(eventId: String, basePrice: Double = 180.0): SeatMap {
        val sections = mutableListOf<SeatSection>()

        sections.add(SeatSection.create("Lower Stand", 6, 25, basePrice, 1.5, Constants.SeatColors.RED, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.FRONT), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Left", 5, 12, basePrice, 1.0, Constants.SeatColors.GREEN, 'G', false,
            position = TheaterPosition(TheaterPosition.Column.LEFT, TheaterPosition.Tier.MIDDLE), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Center", 8, 20, basePrice, 1.0, Constants.SeatColors.MEDIUM_GREEN, 'G', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.MIDDLE), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Right", 5, 12, basePrice, 1.0, Constants.SeatColors.LIGHT_GREEN, 'G', false,
            position = TheaterPosition(TheaterPosition.Column.RIGHT, TheaterPosition.Tier.MIDDLE), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Upper Stand", 8, 25, basePrice, 0.7, Constants.SeatColors.BLUE, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.BALCONY), hasAccessibleSeating = true))

        return SeatMap(id = eventId, eventId = eventId, sections = sections, venueLayoutType = SeatMap.VenueLayoutType.THEATER)
    }

    fun createHaifaAuditoriumLayout(eventId: String, basePrice: Double = 180.0): SeatMap {
        val sections = mutableListOf<SeatSection>()

        sections.add(SeatSection.create("Lower Stand", 6, 25, basePrice, 1.5, Constants.SeatColors.RED, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.FRONT), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Left", 6, 15, basePrice, 1.0, Constants.SeatColors.GREEN, 'G', false,
            position = TheaterPosition(TheaterPosition.Column.LEFT, TheaterPosition.Tier.MIDDLE), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Right", 6, 15, basePrice, 1.0, Constants.SeatColors.LIGHT_GREEN, 'G', false,
            position = TheaterPosition(TheaterPosition.Column.RIGHT, TheaterPosition.Tier.MIDDLE), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Upper Left", 5, 12, basePrice, 0.7, Constants.SeatColors.BLUE, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.LEFT, TheaterPosition.Tier.BALCONY), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Upper Right", 5, 12, basePrice, 0.7, Constants.SeatColors.LIGHT_BLUE, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.RIGHT, TheaterPosition.Tier.BALCONY), hasAccessibleSeating = true))

        return SeatMap(id = eventId, eventId = eventId, sections = sections, venueLayoutType = SeatMap.VenueLayoutType.THEATER)
    }

    fun createReading3Layout(eventId: String, basePrice: Double = 180.0): SeatMap {
        val sections = mutableListOf<SeatSection>()

        sections.add(SeatSection.create("VIP", 4, 25, basePrice, 2.0, Constants.SeatColors.GOLD, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.FRONT), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Center", 8, 30, basePrice, 1.2, Constants.SeatColors.RED, 'E', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.MIDDLE), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Left", 8, 15, basePrice, 1.0, Constants.SeatColors.GREEN, 'E', false,
            position = TheaterPosition(TheaterPosition.Column.LEFT, TheaterPosition.Tier.MIDDLE), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Right", 8, 15, basePrice, 1.0, Constants.SeatColors.LIGHT_GREEN, 'E', false,
            position = TheaterPosition(TheaterPosition.Column.RIGHT, TheaterPosition.Tier.MIDDLE), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Back Stand", 5, 40, basePrice, 0.5, Constants.SeatColors.BLUE_GRAY, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.BACK), hasAccessibleSeating = true))

        return SeatMap(id = eventId, eventId = eventId, sections = sections, venueLayoutType = SeatMap.VenueLayoutType.CONCERT_STAGE)
    }

    fun createFirstStationLayout(eventId: String, basePrice: Double = 170.0): SeatMap {
        val sections = mutableListOf<SeatSection>()

        sections.add(SeatSection.create("VIP Front", 4, 25, basePrice, 2.0, Constants.SeatColors.GOLD, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.FRONT), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Center", 10, 30, basePrice, 1.2, Constants.SeatColors.RED, 'E', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.MIDDLE), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Left", 8, 15, basePrice, 1.0, Constants.SeatColors.GREEN, 'E', false,
            position = TheaterPosition(TheaterPosition.Column.LEFT, TheaterPosition.Tier.MIDDLE), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Right", 8, 15, basePrice, 1.0, Constants.SeatColors.LIGHT_GREEN, 'E', false,
            position = TheaterPosition(TheaterPosition.Column.RIGHT, TheaterPosition.Tier.MIDDLE), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Back Stand", 10, 35, basePrice, 0.6, Constants.SeatColors.BLUE, 'M', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.BACK), hasAccessibleSeating = true))

        return SeatMap(id = eventId, eventId = eventId, sections = sections, venueLayoutType = SeatMap.VenueLayoutType.CONCERT_STAGE)
    }

    // --- HEICHAL HATARBUT VENUES ---

    // Adapts section count based on venue capacity
    fun createHeichalHaTarbutLayout(eventId: String, basePrice: Double = 180.0, capacity: Int = 1500): SeatMap {
        val sections = mutableListOf<SeatSection>()

        if (capacity >= 1500) {
            sections.add(SeatSection.create("Lower Stand", 6, 25, basePrice, 1.5, Constants.SeatColors.RED, 'A', false,
                position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.FRONT), hasAccessibleSeating = true))
            sections.add(SeatSection.create("Left", 6, 12, basePrice, 1.0, Constants.SeatColors.GREEN, 'G', false,
                position = TheaterPosition(TheaterPosition.Column.LEFT, TheaterPosition.Tier.MIDDLE), hasAccessibleSeating = true))
            sections.add(SeatSection.create("Center", 8, 20, basePrice, 1.0, Constants.SeatColors.MEDIUM_GREEN, 'G', false,
                position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.MIDDLE), hasAccessibleSeating = true))
            sections.add(SeatSection.create("Right", 6, 12, basePrice, 1.0, Constants.SeatColors.LIGHT_GREEN, 'G', false,
                position = TheaterPosition(TheaterPosition.Column.RIGHT, TheaterPosition.Tier.MIDDLE), hasAccessibleSeating = true))
            sections.add(SeatSection.create("Upper Left", 4, 10, basePrice, 0.7, Constants.SeatColors.BLUE, 'A', false,
                position = TheaterPosition(TheaterPosition.Column.LEFT, TheaterPosition.Tier.BALCONY), hasAccessibleSeating = true))
            sections.add(SeatSection.create("Upper Stand", 6, 20, basePrice, 0.7, Constants.SeatColors.LIGHT_BLUE, 'A', false,
                position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.BALCONY), hasAccessibleSeating = true))
            sections.add(SeatSection.create("Upper Right", 4, 10, basePrice, 0.7, Constants.SeatColors.MEDIUM_BLUE, 'A', false,
                position = TheaterPosition(TheaterPosition.Column.RIGHT, TheaterPosition.Tier.BALCONY), hasAccessibleSeating = true))
        } else if (capacity >= 1000) {
            sections.add(SeatSection.create("Lower Stand", 6, 22, basePrice, 1.5, Constants.SeatColors.RED, 'A', false,
                position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.FRONT), hasAccessibleSeating = true))
            sections.add(SeatSection.create("Center", 10, 25, basePrice, 1.0, Constants.SeatColors.GREEN, 'G', false,
                position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.MIDDLE), hasAccessibleSeating = true))
            sections.add(SeatSection.create("Upper Stand", 6, 22, basePrice, 0.7, Constants.SeatColors.BLUE, 'A', false,
                position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.BALCONY), hasAccessibleSeating = true))
        } else {
            sections.add(SeatSection.create("Lower Stand", 5, 20, basePrice, 1.4, Constants.SeatColors.RED, 'A', false,
                position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.FRONT), hasAccessibleSeating = true))
            sections.add(SeatSection.create("Center", 8, 22, basePrice, 1.0, Constants.SeatColors.GREEN, 'F', false,
                position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.MIDDLE), hasAccessibleSeating = true))
            sections.add(SeatSection.create("Upper Stand", 5, 18, basePrice, 0.7, Constants.SeatColors.BLUE, 'A', false,
                position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.BALCONY), hasAccessibleSeating = true))
        }


        return SeatMap(id = eventId, eventId = eventId, sections = sections, venueLayoutType = SeatMap.VenueLayoutType.THEATER)
    }

    // Heichal HaTarbut Rishon LeZion (1,500 seats)
    fun createHeichalRishonLayout(eventId: String, basePrice: Double = 180.0): SeatMap {
        return createHeichalHaTarbutLayout(eventId, basePrice, 1500)
    }

    // Heichal HaTarbut Petah Tikva (1,200 seats)
    fun createHeichalPetahTikvaLayout(eventId: String, basePrice: Double = 170.0): SeatMap {
        return createHeichalHaTarbutLayout(eventId, basePrice, 1200)
    }

    // Heichal HaTarbut Netanya (1,200 seats)
    fun createHeichalNetanyaLayout(eventId: String, basePrice: Double = 170.0): SeatMap {
        return createHeichalHaTarbutLayout(eventId, basePrice, 1200)
    }

    // Heichal HaTarbut Ra'anana (1,000 seats)
    fun createHeichalRaananaLayout(eventId: String, basePrice: Double = 160.0): SeatMap {
        return createHeichalHaTarbutLayout(eventId, basePrice, 1000)
    }

    // Heichal HaTarbut Rehovot (1,000 seats)
    fun createHeichalRehovotLayout(eventId: String, basePrice: Double = 160.0): SeatMap {
        return createHeichalHaTarbutLayout(eventId, basePrice, 1000)
    }

    fun createMonArtLayout(eventId: String, basePrice: Double = 170.0): SeatMap {
        val sections = mutableListOf<SeatSection>()

        sections.add(SeatSection.create("Lower Stand", 5, 22, basePrice, 1.5, Constants.SeatColors.RED, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.FRONT), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Center", 8, 25, basePrice, 1.0, Constants.SeatColors.GREEN, 'F', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.MIDDLE), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Upper Stand", 6, 22, basePrice, 0.7, Constants.SeatColors.BLUE, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.BALCONY), hasAccessibleSeating = true))

        return SeatMap(id = eventId, eventId = eventId, sections = sections, venueLayoutType = SeatMap.VenueLayoutType.THEATER)
    }

    // --- MEDIUM THEATER LAYOUTS ---

    fun createGesherLayout(eventId: String, basePrice: Double = 170.0): SeatMap {
        val sections = mutableListOf<SeatSection>()

        sections.add(SeatSection.create("Left", 4, 8, basePrice, 1.3, Constants.SeatColors.RED, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.LEFT, TheaterPosition.Tier.FRONT), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Lower Stand", 4, 10, basePrice, 1.4, Constants.SeatColors.DARK_RED, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.FRONT), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Right", 4, 8, basePrice, 1.3, Constants.SeatColors.DEEP_ORANGE, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.RIGHT, TheaterPosition.Tier.FRONT), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Back Stand", 6, 20, basePrice, 0.8, Constants.SeatColors.GREEN, 'E', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.MIDDLE), hasAccessibleSeating = true))

        return SeatMap(id = eventId, eventId = eventId, sections = sections, venueLayoutType = SeatMap.VenueLayoutType.THEATER)
    }

    fun createSuzanneDellalLayout(eventId: String, basePrice: Double = 180.0): SeatMap {
        val sections = mutableListOf<SeatSection>()

        sections.add(SeatSection.create("Left", 4, 10, basePrice, 1.3, Constants.SeatColors.RED, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.LEFT, TheaterPosition.Tier.FRONT), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Right", 4, 10, basePrice, 1.3, Constants.SeatColors.DEEP_ORANGE, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.RIGHT, TheaterPosition.Tier.FRONT), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Center", 6, 18, basePrice, 1.0, Constants.SeatColors.GREEN, 'E', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.MIDDLE), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Back Stand", 5, 20, basePrice, 0.7, Constants.SeatColors.BLUE, 'K', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.BACK), hasAccessibleSeating = true))

        return SeatMap(id = eventId, eventId = eventId, sections = sections, venueLayoutType = SeatMap.VenueLayoutType.THEATER)
    }

    fun createHaifaTheatreLayout(eventId: String, basePrice: Double = 170.0): SeatMap {
        val sections = mutableListOf<SeatSection>()

        sections.add(SeatSection.create("Lower Stand", 5, 18, basePrice, 1.3, Constants.SeatColors.RED, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.FRONT), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Left", 5, 10, basePrice, 1.0, Constants.SeatColors.GREEN, 'F', false,
            position = TheaterPosition(TheaterPosition.Column.LEFT, TheaterPosition.Tier.MIDDLE), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Right", 5, 10, basePrice, 1.0, Constants.SeatColors.LIGHT_GREEN, 'F', false,
            position = TheaterPosition(TheaterPosition.Column.RIGHT, TheaterPosition.Tier.MIDDLE), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Back Stand", 5, 20, basePrice, 0.7, Constants.SeatColors.BLUE, 'K', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.BACK), hasAccessibleSeating = true))

        return SeatMap(id = eventId, eventId = eventId, sections = sections, venueLayoutType = SeatMap.VenueLayoutType.THEATER)
    }

    fun createHerzliyaPACLayout(eventId: String, basePrice: Double = 180.0): SeatMap {
        val sections = mutableListOf<SeatSection>()

        sections.add(SeatSection.create("Lower Stand", 5, 20, basePrice, 1.4, Constants.SeatColors.RED, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.FRONT), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Center", 8, 22, basePrice, 1.0, Constants.SeatColors.GREEN, 'F', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.MIDDLE), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Upper Stand", 5, 18, basePrice, 0.7, Constants.SeatColors.BLUE, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.BALCONY), hasAccessibleSeating = true))

        return SeatMap(id = eventId, eventId = eventId, sections = sections, venueLayoutType = SeatMap.VenueLayoutType.THEATER)
    }

    fun createBeerShevaLayout(eventId: String, basePrice: Double = 160.0): SeatMap {
        val sections = mutableListOf<SeatSection>()

        sections.add(SeatSection.create("Left", 4, 10, basePrice, 1.3, Constants.SeatColors.RED, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.LEFT, TheaterPosition.Tier.FRONT), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Right", 4, 10, basePrice, 1.3, Constants.SeatColors.DEEP_ORANGE, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.RIGHT, TheaterPosition.Tier.FRONT), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Center", 7, 22, basePrice, 1.0, Constants.SeatColors.GREEN, 'E', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.MIDDLE), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Back Stand", 5, 22, basePrice, 0.7, Constants.SeatColors.BLUE, 'L', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.BACK), hasAccessibleSeating = true))

        return SeatMap(id = eventId, eventId = eventId, sections = sections, venueLayoutType = SeatMap.VenueLayoutType.THEATER)
    }

    fun createHolonTheatreLayout(eventId: String, basePrice: Double = 160.0): SeatMap {
        val sections = mutableListOf<SeatSection>()

        sections.add(SeatSection.create("Lower Stand", 5, 18, basePrice, 1.3, Constants.SeatColors.RED, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.FRONT), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Center", 6, 20, basePrice, 1.0, Constants.SeatColors.GREEN, 'F', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.MIDDLE), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Back Stand", 5, 20, basePrice, 0.7, Constants.SeatColors.BLUE, 'L', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.BACK), hasAccessibleSeating = true))

        return SeatMap(id = eventId, eventId = eventId, sections = sections, venueLayoutType = SeatMap.VenueLayoutType.THEATER)
    }


    // --- SMALL / CLUB LAYOUTS ---

    fun createZappaLayout(eventId: String, basePrice: Double = 150.0): SeatMap {
        val sections = mutableListOf<SeatSection>()

        sections.add(SeatSection.create("Lower Stand", 3, 12, basePrice, 1.5, Constants.SeatColors.RED, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.FRONT), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Center", 4, 15, basePrice, 1.0, Constants.SeatColors.GREEN, 'D', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.MIDDLE), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Back Stand", 3, 15, basePrice, 0.7, Constants.SeatColors.BLUE, 'H', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.BACK), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Bar Seats", 2, 10, basePrice, 0.5, Constants.SeatColors.BLUE_GRAY, 'K', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.BALCONY), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Back Stand", 3, 20, basePrice, 0.4, Constants.SeatColors.GRAY, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.LEFT, TheaterPosition.Tier.BACK), hasAccessibleSeating = true))

        return SeatMap(id = eventId, eventId = eventId, sections = sections, venueLayoutType = SeatMap.VenueLayoutType.CLUB)
    }

    fun createZappaHaifaLayout(eventId: String, basePrice: Double = 140.0): SeatMap {
        val sections = mutableListOf<SeatSection>()

        sections.add(SeatSection.create("Lower Stand", 3, 12, basePrice, 1.5, Constants.SeatColors.RED, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.FRONT), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Center", 4, 14, basePrice, 1.0, Constants.SeatColors.GREEN, 'D', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.MIDDLE), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Back Stand", 3, 14, basePrice, 0.7, Constants.SeatColors.BLUE, 'H', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.BACK), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Bar Seats", 2, 10, basePrice, 0.5, Constants.SeatColors.BLUE_GRAY, 'K', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.BALCONY), hasAccessibleSeating = true))

        return SeatMap(id = eventId, eventId = eventId, sections = sections, venueLayoutType = SeatMap.VenueLayoutType.CLUB)
    }

    fun createZappaRishonLayout(eventId: String, basePrice: Double = 140.0): SeatMap {
        val sections = mutableListOf<SeatSection>()

        sections.add(SeatSection.create("Lower Stand", 3, 12, basePrice, 1.5, Constants.SeatColors.RED, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.FRONT), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Center", 4, 14, basePrice, 1.0, Constants.SeatColors.GREEN, 'D', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.MIDDLE), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Back Stand", 3, 14, basePrice, 0.7, Constants.SeatColors.BLUE, 'H', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.BACK), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Bar Seats", 2, 10, basePrice, 0.5, Constants.SeatColors.BLUE_GRAY, 'K', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.BALCONY), hasAccessibleSeating = true))

        return SeatMap(id = eventId, eventId = eventId, sections = sections, venueLayoutType = SeatMap.VenueLayoutType.CLUB)
    }

    fun createBarbyLayout(eventId: String, basePrice: Double = 130.0): SeatMap {
        val sections = mutableListOf<SeatSection>()

        sections.add(SeatSection.create("Lower Stand", 3, 20, basePrice, 1.5, Constants.SeatColors.RED, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.FRONT), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Center", 4, 15, basePrice, 1.2, Constants.SeatColors.ORANGE, 'D', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.MIDDLE), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Upper Left", 3, 8, basePrice, 0.8, Constants.SeatColors.GREEN, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.LEFT, TheaterPosition.Tier.BALCONY), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Upper Right", 3, 8, basePrice, 0.8, Constants.SeatColors.LIGHT_GREEN, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.RIGHT, TheaterPosition.Tier.BALCONY), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Bar Seats", 2, 10, basePrice, 0.5, Constants.SeatColors.BLUE_GRAY, 'H', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.BACK), hasAccessibleSeating = true))

        return SeatMap(id = eventId, eventId = eventId, sections = sections, venueLayoutType = SeatMap.VenueLayoutType.CLUB)
    }

    fun createComedyBarLayout(eventId: String, basePrice: Double = 100.0): SeatMap {
        val sections = mutableListOf<SeatSection>()

        sections.add(SeatSection.create("Lower Stand", 3, 10, basePrice, 1.5, Constants.SeatColors.RED, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.FRONT), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Center", 3, 12, basePrice, 1.0, Constants.SeatColors.GREEN, 'D', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.MIDDLE), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Back Stand", 3, 10, basePrice, 0.7, Constants.SeatColors.BLUE, 'G', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.BACK), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Bar Seats", 2, 6, basePrice, 0.5, Constants.SeatColors.DARK_BLUE, 'J', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.BALCONY), hasAccessibleSeating = true))

        return SeatMap(id = eventId, eventId = eventId, sections = sections, venueLayoutType = SeatMap.VenueLayoutType.CLUB)
    }

    fun createTmunaLayout(eventId: String, basePrice: Double = 120.0): SeatMap {
        val sections = mutableListOf<SeatSection>()

        sections.add(SeatSection.create("Lower Stand", 4, 10, basePrice, 1.3, Constants.SeatColors.RED, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.FRONT), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Center", 4, 12, basePrice, 1.0, Constants.SeatColors.GREEN, 'E', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.MIDDLE), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Back Stand", 4, 12, basePrice, 0.7, Constants.SeatColors.BLUE, 'I', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.BACK), hasAccessibleSeating = true))

        return SeatMap(id = eventId, eventId = eventId, sections = sections, venueLayoutType = SeatMap.VenueLayoutType.THEATER)
    }

    fun createKhanTheatreLayout(eventId: String, basePrice: Double = 150.0): SeatMap {
        val sections = mutableListOf<SeatSection>()

        sections.add(SeatSection.create("Left", 3, 8, basePrice, 1.3, Constants.SeatColors.RED, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.LEFT, TheaterPosition.Tier.FRONT), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Right", 3, 8, basePrice, 1.3, Constants.SeatColors.DEEP_ORANGE, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.RIGHT, TheaterPosition.Tier.FRONT), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Center", 5, 14, basePrice, 0.8, Constants.SeatColors.GREEN, 'D', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.MIDDLE), hasAccessibleSeating = true))

        return SeatMap(id = eventId, eventId = eventId, sections = sections, venueLayoutType = SeatMap.VenueLayoutType.THEATER)
    }

    fun createHolonCinemathequeLayout(eventId: String, basePrice: Double = 80.0): SeatMap {
        val sections = mutableListOf<SeatSection>()

        sections.add(SeatSection.create("Lower Stand", 3, 12, basePrice, 1.3, Constants.SeatColors.RED, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.FRONT), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Center", 4, 14, basePrice, 1.0, Constants.SeatColors.GREEN, 'D', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.MIDDLE), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Back Stand", 3, 14, basePrice, 0.7, Constants.SeatColors.BLUE, 'H', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.BACK), hasAccessibleSeating = true))

        return SeatMap(id = eventId, eventId = eventId, sections = sections, venueLayoutType = SeatMap.VenueLayoutType.CLUB)
    }

    fun createConfederationHouseLayout(eventId: String, basePrice: Double = 150.0): SeatMap {
        val sections = mutableListOf<SeatSection>()

        sections.add(SeatSection.create("Lower Stand", 4, 14, basePrice, 1.3, Constants.SeatColors.RED, 'A', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.FRONT), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Center", 5, 16, basePrice, 1.0, Constants.SeatColors.GREEN, 'E', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.MIDDLE), hasAccessibleSeating = true))
        sections.add(SeatSection.create("Back Stand", 4, 16, basePrice, 0.7, Constants.SeatColors.BLUE, 'J', false,
            position = TheaterPosition(TheaterPosition.Column.CENTER, TheaterPosition.Tier.BACK), hasAccessibleSeating = true))

        return SeatMap(id = eventId, eventId = eventId, sections = sections, venueLayoutType = SeatMap.VenueLayoutType.THEATER)
    }

    // --- GENERIC FALLBACK LAYOUTS ---

    fun createTheaterLayout(eventId: String, basePrice: Double = 200.0): SeatMap {
        val sections = listOf(
            SeatSection.createVIP(3, 20, basePrice),
            SeatSection.createGeneral(8, 24, basePrice, startRow = 'D'),
            SeatSection.createBalcony(4, 20, basePrice),
            SeatSection.createAccessible(1, 10, basePrice)
        )
        return SeatMap(id = eventId, eventId = eventId, sections = sections, venueLayoutType = SeatMap.VenueLayoutType.THEATER)
    }

    fun createConcertHallLayout(eventId: String, basePrice: Double = 300.0): SeatMap {
        val sections = listOf(
            SeatSection.createVIP(4, 22, basePrice),
            SeatSection.createGeneral(10, 28, basePrice, startRow = 'E'),
            SeatSection.createBalcony(5, 24, basePrice),
            SeatSection.createAccessible(1, 12, basePrice)
        )
        return SeatMap(id = eventId, eventId = eventId, sections = sections, venueLayoutType = SeatMap.VenueLayoutType.CONCERT_STAGE)
    }

    fun createStadiumLayout(eventId: String, basePrice: Double = 150.0): SeatMap {
        val sections = listOf(
            SeatSection.createVIP(3, 30, basePrice),
            SeatSection.createGeneral(12, 40, basePrice, startRow = 'D'),
            SeatSection.createBalcony(8, 35, basePrice),
            SeatSection.createAccessible(1, 15, basePrice)
        )
        return SeatMap(id = eventId, eventId = eventId, sections = sections, venueLayoutType = SeatMap.VenueLayoutType.STADIUM)
    }

}