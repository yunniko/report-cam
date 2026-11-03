package cz.julienika.reportcam.settings

import cz.julienika.reportcam.stamp.FramePlacement
import cz.julienika.reportcam.stamp.OverlayPlacement
import cz.julienika.reportcam.stamp.StampStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {
    private class FakeSource(
        vararg values: Pair<String, Any>,
    ) : SettingsSource {
        private val map = values.toMap()

        override fun string(
            key: String,
            default: String,
        ) = map[key] as? String ?: default

        override fun bool(
            key: String,
            default: Boolean,
        ) = map[key] as? Boolean ?: default
    }

    @Test
    fun defaultsAreSmallPhotoGalleryOnCaptionOnLocationOn() {
        val s = AppSettings.from(FakeSource())
        assertEquals(1280, s.outputLongEdge)
        assertTrue(s.saveToGallery)
        assertTrue(s.caption)
        assertTrue(s.showLocation)
        assertFalse(s.waitForPrecise)
        assertEquals(listOf("text", "datetime", "coordinates"), s.stampSpec.fields.map { it.id })
        assertEquals(Keys.DEFAULT_PLACEMENT, s.stampSpec.placement.id)
    }

    @Test
    fun captionCanBeSwitchedOff() {
        assertFalse(AppSettings.from(FakeSource(Keys.CAPTION to false)).caption)
    }

    @Test
    fun locationOffRemovesTheFieldAndTheWait() {
        val s = AppSettings.from(FakeSource(Keys.SHOW_LOCATION to false, Keys.WAIT_PRECISE to true))
        assertFalse(s.showLocation)
        assertFalse(s.waitForPrecise)
        assertEquals(listOf("text", "datetime"), s.stampSpec.fields.map { it.id })
    }

    @Test
    fun everyOfferedPlacementMapsAndUnknownFallsBack() {
        assertTrue(AppSettings.from(FakeSource(Keys.PLACEMENT to "frame_top")).stampSpec.placement is FramePlacement)
        assertTrue(AppSettings.from(FakeSource(Keys.PLACEMENT to "overlay_top_end")).stampSpec.placement is OverlayPlacement)
        assertEquals(Keys.DEFAULT_PLACEMENT, AppSettings.from(FakeSource(Keys.PLACEMENT to "gone")).stampSpec.placement.id)
    }

    @Test
    fun brokenSizeAndPatternFallBackInsteadOfCrashing() {
        val s = AppSettings.from(FakeSource(Keys.OUTPUT_SIZE to "huge", Keys.DATETIME_PATTERN to "yyyy-MM-dd {"))
        assertEquals(1280, s.outputLongEdge)
        assertEquals(3, s.stampSpec.fields.size)
    }

    @Test
    fun fullSizeIsZeroAndTextIsTrimmed() {
        val s = AppSettings.from(FakeSource(Keys.OUTPUT_SIZE to "0", Keys.STAMP_TEXT to "  Site 4  "))
        assertEquals(0, s.outputLongEdge)
        assertEquals("Site 4", s.userText)
    }

    @Test
    fun textSizeMaps() {
        assertEquals(StampStyle.TEXT_LARGE, AppSettings.from(FakeSource(Keys.TEXT_SIZE to "large")).stampSpec.style.textSizeFraction, 0f)
    }
}
