package cz.julienika.reportcam.stamp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

class StampFieldTest {
    private val captured = ZonedDateTime.of(2026, 11, 3, 14, 5, 9, 0, ZoneId.of("Europe/Prague"))
    private val capturedMs = captured.toInstant().toEpochMilli()

    private fun dataWith(reading: LocationReading?) =
        StampData.EMPTY
            .with(StampKeys.CAPTURE_TIME, captured)
            .with(StampKeys.LOCATION, reading)

    private fun fix(
        accuracy: Float? = 8f,
        ageMs: Long = 10_000,
        coarse: Boolean = false,
    ) = LocationReading.Fix(GeoFix(50.087654, 14.421329, accuracy, capturedMs - ageMs, coarse))

    @Test
    fun dateTimeUsesPatternAndCaptureTime() {
        val f = DateTimeField("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
        assertEquals(listOf("2026-11-03 14:05:09"), f.lines(dataWith(null)))
    }

    @Test
    fun textSkipsBlankLines() {
        val d = StampData.EMPTY.with(StampKeys.USER_TEXT, "Site 4\n\n  \nCrew B  ")
        assertEquals(listOf("Site 4", "Crew B"), TextField().lines(d))
        assertEquals(emptyList<String>(), TextField().lines(StampData.EMPTY))
    }

    @Test
    fun noPermissionMeansNoLocationLine() {
        assertEquals(emptyList<String>(), CoordinatesField().lines(dataWith(null)))
    }

    @Test
    fun permissionButNoFixSaysUnavailable() {
        assertEquals(listOf("Location unavailable"), CoordinatesField().lines(dataWith(LocationReading.None)))
    }

    @Test
    fun preciseFreshFixHasNoMarks() {
        assertEquals(listOf("50.08765, 14.42133 ±8 m"), CoordinatesField().lines(dataWith(fix())))
    }

    @Test
    fun wideAccuracyIsMarkedApproximate() {
        val line = CoordinatesField().lines(dataWith(fix(accuracy = 1500f))).single()
        assertEquals("≈50.08765, 14.42133 ±1.5 km (approximate)", line)
    }

    @Test
    fun coarsePermissionIsApproximateEvenWhenAccuracyLooksGood() {
        val line = CoordinatesField().lines(dataWith(fix(accuracy = 10f, coarse = true))).single()
        assertTrue(line, line.endsWith("(approximate)"))
    }

    @Test
    fun oldFixShowsItsAgeAtCaptureTime() {
        val line = CoordinatesField().lines(dataWith(fix(ageMs = 7 * 60_000L))).single()
        assertEquals("50.08765, 14.42133 ±8 m (7 min old)", line)
        val hours = CoordinatesField().lines(dataWith(fix(ageMs = 5 * 3_600_000L))).single()
        assertTrue(hours, hours.endsWith("(5 h old)"))
    }

    @Test
    fun approximateAndOldTogether() {
        val line = CoordinatesField().lines(dataWith(fix(accuracy = 200f, ageMs = 3 * 60_000L))).single()
        assertTrue(line, line.endsWith("(approximate, 3 min old)"))
    }

    @Test
    fun dmsFormat() {
        assertEquals("50°05'15.6\"N", CoordinatesField.dms(50.087654, 'N', 'S'))
        assertEquals("14°25'16.8\"W", CoordinatesField.dms(-14.421329, 'E', 'W'))
        val line = CoordinatesField(CoordinateFormat.DMS).lines(dataWith(fix())).single()
        assertTrue(line, line.startsWith("50°05'15.6\"N 14°25'16.8\"E"))
    }

    @Test
    fun qualityThresholds() {
        val now = 1_000_000_000L
        val q = FixQuality.of(GeoFix(0.0, 0.0, FixQuality.PRECISE_ACCURACY_M, now), now)
        assertTrue(q.precise)
        assertFalse(FixQuality.of(GeoFix(0.0, 0.0, null, now), now).precise)
        assertTrue(FixQuality.of(GeoFix(0.0, 0.0, 5f, now - FixQuality.STALE_AFTER_MS - 1), now).stale)
    }
}
