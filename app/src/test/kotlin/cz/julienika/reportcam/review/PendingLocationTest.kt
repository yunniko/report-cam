package cz.julienika.reportcam.review

import cz.julienika.reportcam.stamp.FixQuality
import cz.julienika.reportcam.location.LocationAccess
import cz.julienika.reportcam.location.LocationStatus
import cz.julienika.reportcam.location.reading
import cz.julienika.reportcam.stamp.CoordinateFormat
import cz.julienika.reportcam.stamp.GeoFix
import cz.julienika.reportcam.stamp.LocationReading
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingLocationTest {
    private val now = 1_800_000_000_000L
    private val labels = PendingLocation.Labels("no position yet", "location is off", "not allowed", { "$it s ago" }, { "$it min ago" })

    private fun fix(
        accuracy: Float?,
        ageMs: Long,
        coarse: Boolean = false,
    ) = GeoFix(50.087654, 14.421329, accuracy, now - ageMs, coarse)

    private fun status(
        fix: GeoFix?,
        access: LocationAccess = LocationAccess.PRECISE,
    ) = LocationStatus(access, fix, updating = true)

    @Test
    fun waitsWithNoFixOrAnApproximateOne() {
        assertFalse(PendingLocation.settled(status(null), now, preciseWithinM = FixQuality.DEFAULT_PRECISE_M))
        assertFalse(PendingLocation.settled(status(fix(500f, 1_000)), now, preciseWithinM = FixQuality.DEFAULT_PRECISE_M))
        assertFalse(PendingLocation.settled(status(fix(8f, 1_000, coarse = true)), now, preciseWithinM = FixQuality.DEFAULT_PRECISE_M))
    }

    @Test
    fun aPreciseFreshFixEndsTheWait() {
        assertTrue(PendingLocation.settled(status(fix(8f, 1_000)), now, preciseWithinM = FixQuality.DEFAULT_PRECISE_M))
    }

    @Test
    fun aPreciseButStaleFixDoesNotEndIt() {
        assertFalse(PendingLocation.settled(status(fix(8f, 5 * 60_000)), now, preciseWithinM = FixQuality.DEFAULT_PRECISE_M))
    }

    @Test
    fun losingLocationKeepsThePhotoWaitingForTheUser() {
        // Owner, 2026-11-03: switching location off or denying it must not stamp the photo by itself (D010).
        assertFalse(PendingLocation.settled(status(fix(5f, 1_000), LocationAccess.DENIED), now, preciseWithinM = FixQuality.DEFAULT_PRECISE_M))
        assertFalse(PendingLocation.settled(status(fix(5f, 1_000), LocationAccess.SERVICES_OFF), now, preciseWithinM = FixQuality.DEFAULT_PRECISE_M))
        assertFalse(PendingLocation.settled(status(null, LocationAccess.SERVICES_OFF), now, preciseWithinM = FixQuality.DEFAULT_PRECISE_M))
    }

    @Test
    fun withLocationLostUseCurrentStampsNoFixEvenIfAnOldOneIsHeld() {
        assertNull(PendingLocation.current(status(fix(5f, 1_000), LocationAccess.DENIED)))
        assertEquals(LocationReading.None, PendingLocation.current(status(fix(5f, 1_000), LocationAccess.SERVICES_OFF)))
        assertEquals(LocationReading.Fix(fix(500f, 0)), PendingLocation.current(status(fix(500f, 0))))
    }

    @Test
    fun readoutSaysWhenLocationIsOffOrDenied() {
        val off = PendingLocation.readout(status(fix(5f, 0), LocationAccess.SERVICES_OFF), now, CoordinateFormat.DECIMAL, labels)
        assertEquals(PendingLocation.Readout("location is off", hasFix = false), off)
        val denied = PendingLocation.readout(status(null, LocationAccess.DENIED), now, CoordinateFormat.DECIMAL, labels)
        assertEquals(PendingLocation.Readout("not allowed", hasFix = false), denied)
    }

    @Test
    fun readoutShowsPositionAccuracyAndAge() {
        val r = PendingLocation.readout(status(fix(35f, 12_000)), now, CoordinateFormat.DECIMAL, labels)
        assertTrue(r.hasFix)
        assertEquals("50.08765, 14.42133 ±35 m, 12 s ago", r.current)
    }

    @Test
    fun readoutAgeSwitchesToMinutes() {
        val r = PendingLocation.readout(status(fix(2_000f, 150_000)), now, CoordinateFormat.DMS, labels)
        assertEquals("50°05'15.6\"N 14°25'16.8\"E ±2.0 km, 2 min ago", r.current)
    }

    @Test
    fun readoutWithoutAFixSaysSo() {
        val r = PendingLocation.readout(status(null), now, CoordinateFormat.DECIMAL, labels)
        assertFalse(r.hasFix)
        assertEquals("no position yet", r.current)
    }

    @Test
    fun aFixNewerThanTheClockReadsAsZeroSecondsOld() {
        val r = PendingLocation.readout(status(fix(5f, -2_000)), now, CoordinateFormat.DECIMAL, labels)
        assertTrue(r.current, r.current.endsWith(", 0 s ago"))
    }

    @Test
    fun statusReadingMatchesTheStampRules() {
        assertNull(status(fix(5f, 0), LocationAccess.DENIED).reading())
        assertEquals(LocationReading.None, status(null).reading())
        assertEquals(LocationReading.Fix(fix(5f, 0)), status(fix(5f, 0)).reading())
    }
}
