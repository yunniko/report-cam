package cz.julienika.reportcam.review

import cz.julienika.reportcam.location.FixPolicy
import cz.julienika.reportcam.location.LocationAccess
import cz.julienika.reportcam.location.LocationStatus
import cz.julienika.reportcam.location.reading
import cz.julienika.reportcam.stamp.CoordinateFormat
import cz.julienika.reportcam.stamp.CoordinatesField
import cz.julienika.reportcam.stamp.LocationReading

/**
 * Pure rules for a photo that waits in review for a precise fix (D008): when the wait is over, and what
 * the live readout says meanwhile. No Android types, so it is unit-tested on the JVM.
 */
object PendingLocation {
    data class Labels(
        val noFixYet: String,
        val servicesOff: String,
        val denied: String,
        val secondsAgo: (Int) -> String,
        val minutesAgo: (Int) -> String,
    )

    /** [current] is the fix the stamp would get now, or [Labels.noFixYet]; [hasFix] picks the "use" button's label. */
    data class Readout(
        val current: String,
        val hasFix: Boolean,
    )

    /**
     * The wait ends by itself only with a precise fresh fix. Location switched off or denied meanwhile keeps
     * the photo waiting: the user may turn it back on, or choose Retake or "No location" (D010).
     */
    fun settled(
        status: LocationStatus,
        nowMillis: Long,
    ): Boolean = !lost(status) && !FixPolicy.shouldWait(status, nowMillis)

    /** What "use current" stamps: nothing when location is off or denied, even if an old fix is still held. */
    fun current(status: LocationStatus): LocationReading? =
        when (status.access) {
            LocationAccess.DENIED -> null
            LocationAccess.SERVICES_OFF -> LocationReading.None
            else -> status.reading()
        }

    private fun lost(status: LocationStatus) =
        status.access == LocationAccess.DENIED || status.access == LocationAccess.SERVICES_OFF

    fun readout(
        status: LocationStatus,
        nowMillis: Long,
        format: CoordinateFormat,
        labels: Labels,
    ): Readout {
        if (status.access == LocationAccess.DENIED) return Readout(labels.denied, hasFix = false)
        if (status.access == LocationAccess.SERVICES_OFF) return Readout(labels.servicesOff, hasFix = false)
        val fix = status.fix ?: return Readout(labels.noFixYet, hasFix = false)
        val sb = StringBuilder(CoordinatesField.position(fix.latitude, fix.longitude, format))
        fix.accuracyM?.let { sb.append(" ").append(CoordinatesField.PLUS_MINUS).append(CoordinatesField.formatDistance(it)) }
        val ageS = ((nowMillis - fix.timeMillis).coerceAtLeast(0) / 1000).toInt()
        sb.append(", ").append(if (ageS < 60) labels.secondsAgo(ageS) else labels.minutesAgo(ageS / 60))
        return Readout(sb.toString(), hasFix = true)
    }
}
