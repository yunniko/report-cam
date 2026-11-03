package cz.julienika.reportcam.camera

import org.junit.Assert.assertEquals
import org.junit.Test

class ExposureDefaultTest {
    @Test
    fun evMapsToTheNearestStep() {
        assertEquals(0, ExposureDefault.index(0f, 1f / 3, -6, 6))
        assertEquals(-3, ExposureDefault.index(-1f, 1f / 3, -6, 6))
        assertEquals(2, ExposureDefault.index(0.5f, 1f / 3, -6, 6)) // 1.5 steps rounds up
        assertEquals(3, ExposureDefault.index(0.5f, 1f / 6, -12, 12))
    }

    @Test
    fun aCameraThatCannotReachTheDefaultGetsItsNearestEnd() {
        assertEquals(4, ExposureDefault.index(2f, 1f / 3, -4, 4))
        assertEquals(-4, ExposureDefault.index(-2f, 1f / 3, -4, 4))
    }

    @Test
    fun noCompensationMeansZero() {
        assertEquals(0, ExposureDefault.index(1f, 0f, 0, 0))
    }
}
