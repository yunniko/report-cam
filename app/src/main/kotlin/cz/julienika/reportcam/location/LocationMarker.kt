package cz.julienika.reportcam.location

import cz.julienika.reportcam.stamp.CoordinatesField
import cz.julienika.reportcam.stamp.FixQuality

/** What the on-screen location marker shows. Pure, so every state is unit-tested. */
data class LocationMarker(
    val tone: Tone,
    val crossedOut: Boolean,
    val text: String,
    /** Pulses while the receiver is looking, so the user can tell whether to wait. */
    val pulsing: Boolean,
) {
    enum class Tone { GRAY, AMBER, GREEN }

    data class Labels(
        val denied: String = "Location not allowed. Tap to allow.",
        val servicesOff: String = "Location is off. Tap to turn it on.",
        val searching: String = "Locating…",
        val updating: String = "updating",
        val approximate: String = "approximate",
        val approximateOnly: String = "approximate only, tap for precise",
        val minutesOld: (Int) -> String = { "$it min old" },
    )

    companion object {
        fun of(
            status: LocationStatus,
            nowMillis: Long,
            labels: Labels = Labels(),
            preciseWithinM: Float,
        ): LocationMarker {
            when (status.access) {
                LocationAccess.DENIED -> return LocationMarker(Tone.GRAY, true, labels.denied, false)
                LocationAccess.SERVICES_OFF -> return LocationMarker(Tone.GRAY, true, labels.servicesOff, false)
                else -> Unit
            }
            val fix = status.fix ?: return LocationMarker(Tone.AMBER, false, labels.searching, status.updating)
            val q = FixQuality.of(fix, nowMillis, preciseWithinM)
            val parts = mutableListOf<String>()
            parts +=
                (if (q.approximate) "${CoordinatesField.APPROX_SIGN} " else "") +
                (fix.accuracyM?.let { "${CoordinatesField.PLUS_MINUS}${CoordinatesField.formatDistance(it)}" } ?: "?")
            if (status.access == LocationAccess.APPROXIMATE_ONLY) {
                parts += labels.approximateOnly
            } else if (q.approximate) {
                parts += labels.approximate
            }
            if (q.stale) parts += labels.minutesOld((q.ageMillis / 60_000).toInt())
            if (status.updating) parts += labels.updating
            return LocationMarker(if (q.precise) Tone.GREEN else Tone.AMBER, false, parts.joinToString(", "), status.updating)
        }
    }
}
