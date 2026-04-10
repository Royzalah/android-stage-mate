package com.roei.stagemate.data.models

// Pricing tier for an event with discount logic and combo packages.
// Used by SeatSelectionActivity, PaymentActivity, and EventDetailActivity.
data class PricingTier(
    var id: String = "",
    val eventId: String = "",
    val category: String = "",
    val tierName: String = "",
    val basePrice: Double = 0.0,
    val discountPercentage: Int = 0,
    val description: String = "",
    val minAge: Int? = null,
    val maxAge: Int? = null,
    val requiresVerification: Boolean = false,
    val isCombo: Boolean = false,
    val comboDescription: String = "",
    val comboQuantity: Map<String, Int> = emptyMap()
) {

    fun getFinalPrice(): Double {
        return basePrice * (1 - discountPercentage / 100.0)
    }

    fun getFinalPrice(quantity: Int): Double {
        return getFinalPrice() * quantity
    }

    fun getFullDescription(): String {
        return buildString {
            append(tierName)
            if (discountPercentage > 0) {
                append(" (${discountPercentage}% Discount)")
            }
            if (comboDescription.isNotEmpty()) {
                append(" - $comboDescription")
            }
            if (description.isNotEmpty()) {
                append(": $description")
            }
        }
    }

    companion object
}

// Factory that builds pricing tier lists for each event category.
object PricingTierFactory {
        fun createConcertPricing(eventId: String, basePrice: Double): List<PricingTier> {
            return listOf(
                PricingTier(
                    id = "${eventId}_VIP",
                    eventId = eventId,
                    category = "Concert",
                    tierName = "VIP",
                    basePrice = basePrice * 2.0,
                    description = "Special access, front row seats"
                ),
                PricingTier(
                    id = "${eventId}_STANDING",
                    eventId = eventId,
                    category = "Concert",
                    tierName = "Standing",
                    basePrice = basePrice * 1.5,
                    description = "Standing area near the stage"
                ),
                PricingTier(
                    id = "${eventId}_REGULAR",
                    eventId = eventId,
                    category = "Concert",
                    tierName = "Regular",
                    basePrice = basePrice,
                    description = "Standard seat"
                )
            )
        }

        fun createChildrenPricing(eventId: String, basePrice: Double): List<PricingTier> {
            return listOf(
                PricingTier(
                    id = "${eventId}_ADULT",
                    eventId = eventId,
                    category = "Kids",
                    tierName = "Adult",
                    basePrice = basePrice,
                    description = "Adult ticket",
                    minAge = 18
                ),
                PricingTier(
                    id = "${eventId}_CHILD",
                    eventId = eventId,
                    category = "Kids",
                    tierName = "Child",
                    basePrice = basePrice * 0.6,
                    discountPercentage = 40,
                    description = "Child ticket",
                    minAge = 0,
                    maxAge = 12
                ),
                PricingTier(
                    id = "${eventId}_FAMILY_SMALL",
                    eventId = eventId,
                    category = "Kids",
                    tierName = "Small Family",
                    basePrice = basePrice * 2.2,
                    discountPercentage = 10,
                    isCombo = true,
                    comboDescription = "1 Parent + 1 Child",
                    comboQuantity = mapOf("adult" to 1, "child" to 1)
                ),
                PricingTier(
                    id = "${eventId}_FAMILY_MEDIUM",
                    eventId = eventId,
                    category = "Kids",
                    tierName = "Medium Family",
                    basePrice = basePrice * 3.5,
                    discountPercentage = 15,
                    isCombo = true,
                    comboDescription = "2 Parents + 1 Child",
                    comboQuantity = mapOf("adult" to 2, "child" to 1)
                ),
                PricingTier(
                    id = "${eventId}_FAMILY_LARGE",
                    eventId = eventId,
                    category = "Kids",
                    tierName = "Large Family",
                    basePrice = basePrice * 4.5,
                    discountPercentage = 20,
                    isCombo = true,
                    comboDescription = "2 Parents + 2 Children",
                    comboQuantity = mapOf("adult" to 2, "child" to 2)
                )
            )
        }

        fun createSportsPricing(eventId: String, basePrice: Double): List<PricingTier> {
            return listOf(
                PricingTier(
                    id = "${eventId}_SKYBOX",
                    eventId = eventId,
                    category = "Sports",
                    tierName = "VIP Box",
                    basePrice = basePrice * 5.0,
                    description = "Private box with service"
                ),
                PricingTier(
                    id = "${eventId}_PREMIUM",
                    eventId = eventId,
                    category = "Sports",
                    tierName = "Premium",
                    basePrice = basePrice * 2.0,
                    description = "Center seats, perfect view"
                ),
                PricingTier(
                    id = "${eventId}_REGULAR",
                    eventId = eventId,
                    category = "Sports",
                    tierName = "Regular",
                    basePrice = basePrice,
                    description = "Standard seat"
                ),
                PricingTier(
                    id = "${eventId}_STANDING",
                    eventId = eventId,
                    category = "Sports",
                    tierName = "Standing",
                    basePrice = basePrice * 0.7,
                    discountPercentage = 30,
                    description = "Standing area"
                )
            )
        }

        fun createTheaterPricing(eventId: String, basePrice: Double): List<PricingTier> {
            return listOf(
                PricingTier(
                    id = "${eventId}_ROW_A_C",
                    eventId = eventId,
                    category = "Theater",
                    tierName = "Rows A-C",
                    basePrice = basePrice * 1.8,
                    description = "Front rows, excellent view"
                ),
                PricingTier(
                    id = "${eventId}_ROW_D_F",
                    eventId = eventId,
                    category = "Theater",
                    tierName = "Rows D-F",
                    basePrice = basePrice * 1.4,
                    description = "Middle rows"
                ),
                PricingTier(
                    id = "${eventId}_ROW_G_PLUS",
                    eventId = eventId,
                    category = "Theater",
                    tierName = "Rows G+",
                    basePrice = basePrice,
                    description = "Back rows"
                ),
                PricingTier(
                    id = "${eventId}_BALCONY",
                    eventId = eventId,
                    category = "Theater",
                    tierName = "Balcony",
                    basePrice = basePrice * 0.8,
                    discountPercentage = 20,
                    description = "Balcony seats"
                )
            )
        }

        fun createStandingPricing(eventId: String, basePrice: Double): List<PricingTier> {
            return listOf(
                PricingTier(
                    id = "${eventId}_VIP_STANDING",
                    eventId = eventId,
                    category = "Standing",
                    tierName = "VIP Standing",
                    basePrice = basePrice * 2.0,
                    description = "VIP area near the stage"
                ),
                PricingTier(
                    id = "${eventId}_EARLY_ENTRY",
                    eventId = eventId,
                    category = "Standing",
                    tierName = "Early Entry",
                    basePrice = basePrice * 1.3,
                    description = "Early entry, first come first served"
                ),
                PricingTier(
                    id = "${eventId}_GENERAL_STANDING",
                    eventId = eventId,
                    category = "Standing",
                    tierName = "General Standing",
                    basePrice = basePrice,
                    description = "General admission standing area"
                )
            )
        }

        fun createSpecialPricing(eventId: String, basePrice: Double): List<PricingTier> {
            return listOf(
                PricingTier(
                    id = "${eventId}_DISABLED",
                    eventId = eventId,
                    category = "Special",
                    tierName = "Disabled",
                    basePrice = basePrice,
                    description = "Accessible seat + free companion"
                )
            )
        }
}
