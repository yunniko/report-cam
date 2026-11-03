package cz.julienika.reportcam.review

import cz.julienika.reportcam.location.FixPolicy
import cz.julienika.reportcam.location.LocationStatus
import cz.julienika.reportcam.stamp.CoordinateFormat
import cz.julienika.reportcam.stamp.CoordinatesField

/**
 * Pure rules for a photo that waits in review for a precise fix (D008): when the wait is over, and what
 * the live readout says meanwhile. No Android types, so it is unit-tested on the JVM.
 */
object PendingLocation {
    data class Labels(
        val noFixYet: String,
        val secondsAgo: (Int) -> String,
        val minutesAgo: (Int) -> String,
    )

    /** [current] is the fix the stamp would get now, or [Labels.noFixYet]; [hasFix] picks the "use" button's label. */
    data class Readout(
        val current: String,
        val hasFix: Boolean,
    )

    /** The wait ends when there is nothing to wait for any more: a precise fresh fix, or location gone. */
    fun settled(
        status: LocationStatus,
        nowMillis: Long,
    ): Boolean = !FixPolicy.shouldWait(status, nowMillis)

    fun readout(
        status: LocationStatus,
        nowMillis: Long,
        format: CoordinateFormat,
        labels: Labels,
    ): Readout {
        val fix = status.fix ?: return Readout(labels.noFixYet, hasFix = false)
        val sb = StringBuilder(CoordinatesField.position(fix.latitude, fix.longitude, format))
        fix.accuracyM?.let { sb.append(" ").append(CoordinatesField.PLUS_MINUS).append(CoordinatesField.formatDistance(it)) }
        val ageS = ((nowMillis - fix.timeMillis).coerceAtLeast(0) / 1000).toInt()
        sb.append(", ").append(if (ageS < 60) labels.secondsAgo(ageS) else labels.minutesAgo(ageS / 60))
        return Readout(sb.toString(), hasFix = true)
    }
}
