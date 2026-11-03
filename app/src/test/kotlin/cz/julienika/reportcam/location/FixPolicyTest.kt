package cz.julienika.reportcam.location

import cz.julienika.reportcam.stamp.GeoFix
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
        assertFalse(FixPolicy.shouldWait(LocationStatus(LocationAccess.DENIED, null, false), now))
        assertFalse(FixPolicy.shouldWait(LocationStatus(LocationAccess.SERVICES_OFF, null, false), now))
        assertTrue(FixPolicy.shouldWait(LocationStatus(LocationAccess.PRECISE, null, true), now))
        assertTrue(FixPolicy.shouldWait(LocationStatus(LocationAccess.PRECISE, fix(200f, 1_000), true), now))
        assertTrue(FixPolicy.shouldWait(LocationStatus(LocationAccess.PRECISE, fix(5f, 10 * 60_000L), false), now))
        assertFalse(FixPolicy.shouldWait(LocationStatus(LocationAccess.PRECISE, fix(5f, 1_000), false), now))
    }

    @Test
    fun searchStopsOnlyOnPreciseFreshFix() {
        assertTrue(FixPolicy.satisfied(fix(8f, 2_000), now))
        assertFalse(FixPolicy.satisfied(null, now))
        assertFalse(FixPolicy.satisfied(fix(120f, 2_000), now))
        assertFalse(FixPolicy.satisfied(fix(8f, 2_000, coarse = true), now))
        assertFalse(FixPolicy.satisfied(fix(8f, FixPolicy.REST_MS + 1), now))
    }
}
