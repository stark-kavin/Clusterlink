package com.clusterlink.app.core

/**
 * One colour edition of a bike: its photo and the app theme it brings (Settings → Theme → Match my bike).
 * [primary] is the accent (buttons, "connected", highlights) and also tints the background, cards and
 * sheets; [secondary] is the second background glow. Text colour on the accent is worked out automatically.
 */
data class BikeColor(
    /** Stored in VehicleStore; never change an existing id. */
    val id: String,
    val name: String,
    /**
     * Photo file name in res/drawable-nodpi (without .webp). The photos are Hero's official renders, so they
     * are not in the public repo; when a file is missing the app shows a bike symbol instead (see README).
     */
    val image: String,
    val primary: Long,
    val secondary: Long,
)

/**
 * Bike models the rider can pick when adding a vehicle. The chosen model + colour sets the bike photo on
 * the home screen and the app's theme colours.
 * To add a bike: add an entry here and, optionally, its photos in res/drawable-nodpi as bike_<model>_<colour>.webp.
 */
enum class BikeModel(
    /** Stored in VehicleStore; never change an existing id. */
    val id: String,
    val displayName: String,
    val kind: String,
    val colors: List<BikeColor>,
) {
    KARIZMA_XMR_210(
        "karizma_xmr_210", "Karizma XMR 210", "Sport tourer",
        listOf(
            BikeColor("yellow", "Yellow", "bike_karizma_210_yellow", primary = 0xFFFFC814, secondary = 0xFFC0C7D0),
            BikeColor("red", "Red", "bike_karizma_210_red", primary = 0xFFFF3B4A, secondary = 0xFFC0C7D0),
            BikeColor("black", "Black", "bike_karizma_210_black", primary = 0xFFE3E8EF, secondary = 0xFF6B7684),
            BikeColor("combat", "Combat Edition", "bike_karizma_210_combat_edition", primary = 0xFFE4F23C, secondary = 0xFF9AA5B1),
        ),
    ),
    XPULSE_210(
        "xpulse_210", "Xpulse 210", "Adventure",
        listOf(
            BikeColor("blue", "Blue", "bike_xpulse_210_blue", primary = 0xFF4C82FF, secondary = 0xFFFF3B4A),
            BikeColor("silver", "Silver", "bike_xpulse_210_silver", primary = 0xFFCFD6DF, secondary = 0xFFFF3B4A),
        ),
    ),
    XTREME_250R(
        "xtreme_250r", "Xtreme 250R", "Streetfighter",
        listOf(
            BikeColor("red", "Red", "bike_xtreme_250_red", primary = 0xFFFF3B4A, secondary = 0xFFF2F4F7),
            BikeColor("neon", "Neon", "bike_xtreme_250_neon", primary = 0xFFE1FF3C, secondary = 0xFF9AA5B1),
            BikeColor("black", "Black", "bike_xtreme_250_black", primary = 0xFFDCE1E8, secondary = 0xFFFF3B4A),
            BikeColor("carbon_black", "Carbon Black", "bike_xtreme_250_carbon_black", primary = 0xFFE8B44C, secondary = 0xFF6B7684),
            BikeColor("carbon_red", "Carbon Red", "bike_xtreme_250_carbon_red", primary = 0xFFFF3B4A, secondary = 0xFFE8B44C),
            BikeColor("carbon_white", "Carbon White", "bike_xtreme_250_carbon_white", primary = 0xFFF2F4F7, secondary = 0xFFFF3B4A),
        ),
    );

    val defaultColor: BikeColor get() = colors.first()

    fun color(id: String?): BikeColor = colors.firstOrNull { it.id == id } ?: defaultColor

    companion object {
        fun of(id: String?): BikeModel? = entries.firstOrNull { it.id == id }
    }
}

/** A model in a specific colour — what a vehicle stores and what the theme follows. */
data class BikeChoice(val model: BikeModel, val color: BikeColor)
