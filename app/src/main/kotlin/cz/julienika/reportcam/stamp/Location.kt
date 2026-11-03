package cz.julienika.reportcam.stamp

/** One position fix as Android reported it. [coarseOnly] means the user granted only approximate location. */
data class GeoFix(
    val latitude: Double,
    val longitude: Double,
    /** Radius of 68% confidence in metres, or null if the provider gave none. */
    val accuracyM: Float?,
    /** Wall-clock time of the fix, epoch milliseconds. */
    val timeMillis: Long,
    val coarseOnly: Boolean = false,
)

sealed interface LocationReading {
    data class Fix(
        val fix: GeoFix,
    ) : LocationReading

    /** Permission granted, but no fix (location services off, or none arrived yet). */
    data object None : LocationReading
}

/** How trustworthy a fix is at a given moment. Shared by the stamp and the live marker so both agree. */
data class FixQuality(
    val approximate: Boolean,
    val ageMillis: Long,
    val stale: Boolean,
) {
    val precise: Boolean get() = !approximate && !stale

    companion object {
        /**
         * A fix counts as precise within this radius unless the user picks another in Settings (Owner, G-003:
         * 5/10/20/30/50 m). A phone GPS outdoors is typically 3–15 m.
         */
        const val DEFAULT_PRECISE_M = 20f
        val PRECISE_CHOICES_M = listOf(5f, 10f, 20f, 30f, 50f)

        /** Older than this, a fix is marked with its age. */
        const val STALE_AFTER_MS = 2 * 60_000L

        fun of(
            fix: GeoFix,
            nowMillis: Long,
            preciseWithinM: Float = DEFAULT_PRECISE_M,
        ): FixQuality {
            val age = (nowMillis - fix.timeMillis).coerceAtLeast(0)
            val approximate = fix.coarseOnly || fix.accuracyM == null || fix.accuracyM > preciseWithinM
            return FixQuality(approximate = approximate, ageMillis = age, stale = age > STALE_AFTER_MS)
        }
    }
}
