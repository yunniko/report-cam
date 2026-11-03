package cz.julienika.reportcam.location

import cz.julienika.reportcam.stamp.GeoFix
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import cz.julienika.reportcam.stamp.FixQuality
import org.junit.Test

class FixPolicyTest {
    private val now = 1_800_000_000_000L

    private fun fix(
        accuracy: Float?,
        ageMs: Long,
        coarse: Boolean = false,
    ) = GeoFix(50.0, 14.0, accuracy, now - ageMs, coarse)

    @Test
    fun anyFixBeatsNone() {
        assertTrue(FixPolicy.better(null, fix(500f, 0)))
    }

    @Test
    fun olderFixNeverReplacesNewer() {
        assertFalse(FixPolicy.better(fix(50f, 1_000), fix(5f, 5_000)))
    }

    @Test
    fun newerAndAtLeastAsAccurateWins() {
        assertTrue(FixPolicy.better(fix(10f, 5_000), fix(10f, 1_000)))
    }

    @Test
    fun muchWorseRecentFixDoesNotDisplaceAGoodOne() {
        // A network fix arriving just after a GPS fix must not throw the GPS fix away.
        assertFalse(FixPolicy.better(fix(5f, 6_000), fix(800f, 1_000)))
    }

    @Test
    fun worseFixWinsOnceTheGoodOneHasAgedOut() {
        assertTrue(FixPolicy.better(fix(5f, 60_000), fix(800f, 0)))
    }

    @Test
    fun shutterWaitsOnlyWhenABetterFixCanCome() {
        assertFalse(FixPolicy.shouldWait(LocationStatus(LocationAccess.DENIED, null, false), now, preciseWithinM = FixQuality.DEFAULT_PRECISE_M))
        assertFalse(FixPolicy.shouldWait(LocationStatus(LocationAccess.SERVICES_OFF, null, false), now, preciseWithinM = FixQuality.DEFAULT_PRECISE_M))
        assertTrue(FixPolicy.shouldWait(LocationStatus(LocationAccess.PRECISE, null, true), now, preciseWithinM = FixQuality.DEFAULT_PRECISE_M))
        assertTrue(FixPolicy.shouldWait(LocationStatus(LocationAccess.PRECISE, fix(200f, 1_000), true), now, preciseWithinM = FixQuality.DEFAULT_PRECISE_M))
        assertTrue(FixPolicy.shouldWait(LocationStatus(LocationAccess.PRECISE, fix(5f, 10 * 60_000L), false), now, preciseWithinM = FixQuality.DEFAULT_PRECISE_M))
        assertFalse(FixPolicy.shouldWait(LocationStatus(LocationAccess.PRECISE, fix(5f, 1_000), false), now, preciseWithinM = FixQuality.DEFAULT_PRECISE_M))
    }

    @Test
    fun searchStopsOnlyOnPreciseFreshFix() {
        assertTrue(FixPolicy.satisfied(fix(8f, 2_000), now, preciseWithinM = FixQuality.DEFAULT_PRECISE_M))
        assertFalse(FixPolicy.satisfied(null, now, preciseWithinM = FixQuality.DEFAULT_PRECISE_M))
        assertFalse(FixPolicy.satisfied(fix(120f, 2_000), now, preciseWithinM = FixQuality.DEFAULT_PRECISE_M))
        assertFalse(FixPolicy.satisfied(fix(8f, 2_000, coarse = true), now, preciseWithinM = FixQuality.DEFAULT_PRECISE_M))
        assertFalse(FixPolicy.satisfied(fix(8f, FixPolicy.REST_MS + 1), now, preciseWithinM = FixQuality.DEFAULT_PRECISE_M))
    }

    @Test
    fun theUsersThresholdDecidesWhatIsPrecise() {
        val f = fix(25f, 1_000)
        assertFalse("25 m is not within the default 20 m", FixPolicy.satisfied(f, now, preciseWithinM = FixQuality.DEFAULT_PRECISE_M))
        assertTrue(FixPolicy.satisfied(f, now, preciseWithinM = 30f))
        assertFalse(FixPolicy.satisfied(fix(8f, 1_000), now, preciseWithinM = 5f))
        assertTrue(FixQuality.of(fix(20f, 0), now).precise)
    }
}
