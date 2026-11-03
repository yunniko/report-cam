package cz.julienika.reportcam.location

import cz.julienika.reportcam.location.LocationMarker.Tone
import cz.julienika.reportcam.stamp.GeoFix
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationMarkerTest {
    private val now = 1_800_000_000_000L

    private fun status(
        access: LocationAccess,
        fix: GeoFix? = null,
        updating: Boolean = false,
    ) = LocationStatus(access, fix, updating)

    private fun fix(
        accuracy: Float? = 6f,
        ageMs: Long = 5_000,
        coarse: Boolean = false,
    ) = GeoFix(50.0, 14.0, accuracy, now - ageMs, coarse)

    @Test
    fun deniedIsGrayCrossedOutAndStill() {
        val m = LocationMarker.of(status(LocationAccess.DENIED, fix()), now)
        assertEquals(Tone.GRAY, m.tone)
        assertTrue(m.crossedOut)
        assertFalse(m.pulsing)
        assertEquals(LocationMarker.Labels().denied, m.text)
    }

    @Test
    fun servicesOffIsGrayCrossedOut() {
        val m = LocationMarker.of(status(LocationAccess.SERVICES_OFF), now)
        assertEquals(Tone.GRAY, m.tone)
        assertTrue(m.crossedOut)
    }

    @Test
    fun searchingWithoutFixPulsesAmber() {
        val m = LocationMarker.of(status(LocationAccess.PRECISE, updating = true), now)
        assertEquals(Tone.AMBER, m.tone)
        assertTrue(m.pulsing)
        assertEquals(LocationMarker.Labels().searching, m.text)
    }

    @Test
    fun preciseFreshFixIsGreenWithAccuracy() {
        val m = LocationMarker.of(status(LocationAccess.PRECISE, fix()), now)
        assertEquals(Tone.GREEN, m.tone)
        assertFalse(m.pulsing)
        assertEquals("±6 m", m.text)
    }

    @Test
    fun approximateFixIsAmberAndSaysSo() {
        val m = LocationMarker.of(status(LocationAccess.PRECISE, fix(accuracy = 900f), updating = true), now)
        assertEquals(Tone.AMBER, m.tone)
        assertTrue(m.text.startsWith("≈ "))
        assertTrue(m.text.contains("approximate"))
        assertTrue(m.text.endsWith("updating"))
    }

    @Test
    fun staleFixShowsItsAge() {
        val m = LocationMarker.of(status(LocationAccess.PRECISE, fix(ageMs = 5 * 60_000L)), now)
        assertEquals(Tone.AMBER, m.tone)
        assertTrue(m.text, m.text.contains("5 min old"))
    }

    @Test
    fun approximateOnlyPermissionInvitesUpgrade() {
        val m = LocationMarker.of(status(LocationAccess.APPROXIMATE_ONLY, fix(accuracy = 1500f, coarse = true)), now)
        assertTrue(m.text.contains(LocationMarker.Labels().approximateOnly))
        assertFalse(m.crossedOut)
    }
}
