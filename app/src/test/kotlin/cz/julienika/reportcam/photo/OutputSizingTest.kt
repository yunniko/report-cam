package cz.julienika.reportcam.photo

import org.junit.Assert.assertEquals
import org.junit.Test

class OutputSizingTest {
    @Test
    fun sampleSizeKeepsLongEdgeAtOrAboveTarget() {
        assertEquals(1, OutputSizing.sampleSize(1600, 1600))
        assertEquals(1, OutputSizing.sampleSize(3199, 1600))
        assertEquals(2, OutputSizing.sampleSize(3200, 1600))
        assertEquals(2, OutputSizing.sampleSize(4000, 1600))
        assertEquals(4, OutputSizing.sampleSize(6400, 1600))
    }

    @Test
    fun zeroTargetMeansOriginal() {
        assertEquals(1, OutputSizing.sampleSize(4000, 0))
        assertEquals(1f, OutputSizing.scale(4000, 0))
    }

    @Test
    fun scaleNeverEnlarges() {
        assertEquals(1f, OutputSizing.scale(1280, 1600))
        assertEquals(0.5f, OutputSizing.scale(3200, 1600))
    }
}
