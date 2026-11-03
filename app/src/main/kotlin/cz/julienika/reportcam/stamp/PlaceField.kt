package cz.julienika.reportcam.stamp

/**
 * A reverse-geocoded place, in the parts the formats need. Any part may be missing: geocoders leave out
 * what they do not know, and many villages have house numbers but no street names.
 */
data class Place(
    val street: String? = null,
    val houseNumber: String? = null,
    val city: String? = null,
    /** The geocoder's own one-line address, used as-is by [PlaceFormat.FULL]. */
    val fullAddress: String? = null,
)

/** How the place prints, by stored id (Owner, 2026-11-03). */
enum class PlaceFormat(
    val id: String,
) {
    FULL("full"),
    STREET_NUMBER("street_number"),
    STREET_CITY("street_city"),
    CITY("city"),
    ;

    /** The place in this format, falling back to coarser parts when a part is missing; null if nothing fits. */
    fun format(p: Place): String? {
        val streetLine =
            when {
                p.street != null && p.houseNumber != null -> "${p.street} ${p.houseNumber}"
                p.street != null -> p.street
                // Villages without street names are addressed "Village 12".
                p.city != null && p.houseNumber != null -> "${p.city} ${p.houseNumber}"
                else -> null
            }
        val full = p.fullAddress ?: listOfNotNull(streetLine, p.city.takeIf { p.street != null }).joinToString(", ")
        return when (this) {
            FULL -> full
            STREET_NUMBER -> streetLine ?: p.city
            STREET_CITY ->
                when {
                    p.street != null -> listOfNotNull(streetLine, p.city).joinToString(", ")
                    else -> streetLine ?: p.city
                }
            CITY -> p.city
        }?.trim()?.takeIf { it.isNotEmpty() }
    }

    companion object {
        /** Null for "off" or an unknown id. */
        fun of(id: String): PlaceFormat? = entries.firstOrNull { it.id == id }
    }
}

/**
 * The place name of the stamped fix, in the chosen format. An approximate fix is too coarse for a street, so
 * it prints the city only, marked like approximate coordinates (D011). No place collected prints nothing:
 * the coordinates line below still says where the photo was taken.
 */
class PlaceField(
    private val format: PlaceFormat,
    private val preciseWithinM: Float,
) : StampField {
    override val id = "place"

    override fun lines(data: StampData): List<String> {
        val place = data[StampKeys.PLACE] ?: return emptyList()
        val fix = (data[StampKeys.LOCATION] as? LocationReading.Fix)?.fix ?: return emptyList()
        val at = data[StampKeys.CAPTURE_TIME]?.toInstant()?.toEpochMilli() ?: fix.timeMillis
        val approximate = FixQuality.of(fix, at, preciseWithinM).approximate
        val text = (if (approximate) PlaceFormat.CITY else format).format(place) ?: return emptyList()
        return listOf(if (approximate) "${CoordinatesField.APPROX_SIGN}$text" else text)
    }
}
