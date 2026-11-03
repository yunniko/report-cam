package cz.julienika.reportcam.location

import cz.julienika.reportcam.stamp.FixQuality
import cz.julienika.reportcam.stamp.GeoFix

/** Pure rules for foreground location tracking, kept apart from LocationManager so they are unit-tested. See D005. */
object FixPolicy {
    /** Updates requested while searching. */
    const val SEARCH_INTERVAL_MS = 1_000L

    /** A search that finds no precise fix in this time rests anyway, keeping what it has. */
    const val SEARCH_TIMEOUT_MS = 45_000L

    /** Pause between searches once a precise fix is in hand; the receiver is off meanwhile. */
    const val REST_MS = 30_000L

    /** A newer fix replaces the current one unless it is much worse while the current one is still recent. */
    fun better(
        current: GeoFix?,
        candidate: GeoFix,
    ): Boolean {
        if (current == null) return true
        val newer = candidate.timeMillis - current.timeMillis
        if (newer <= 0) return false
        val curAcc = current.accuracyM ?: Float.MAX_VALUE
        val candAcc = candidate.accuracyM ?: Float.MAX_VALUE
        if (candAcc <= curAcc) return true
        // A worse fix wins only once the better one has aged out, or if it is not much worse.
        return newer > RECENT_MS || candAcc <= curAcc * 2
    }

    /** Whether a search can stop: a precise fix that is also fresh. */
    fun satisfied(
        fix: GeoFix?,
        nowMillis: Long,
        preciseWithinM: Float,
    ): Boolean = fix != null && FixQuality.of(fix, nowMillis, preciseWithinM).precise && nowMillis - fix.timeMillis < REST_MS

    /**
     * Whether a shutter press with "wait for precise location" on should hold the photo. Never when there is
     * nothing to wait for (no permission, location off); otherwise until the fix is precise and fresh.
     */
    fun shouldWait(
        status: LocationStatus,
        nowMillis: Long,
        preciseWithinM: Float,
    ): Boolean {
        if (status.access == LocationAccess.DENIED || status.access == LocationAccess.SERVICES_OFF) return false
        val fix = status.fix ?: return true
        return !FixQuality.of(fix, nowMillis, preciseWithinM).precise
    }

    private const val RECENT_MS = 20_000L
}
