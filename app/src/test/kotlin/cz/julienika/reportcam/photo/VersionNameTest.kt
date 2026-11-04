package cz.julienika.reportcam.photo

import cz.julienika.reportcam.stamp.Corner
import cz.julienika.reportcam.stamp.DateTimeField
import cz.julienika.reportcam.stamp.FramePlacement
import cz.julienika.reportcam.stamp.OverlayPlacement
import cz.julienika.reportcam.stamp.StampData
import cz.julienika.reportcam.stamp.StampKeys
import cz.julienika.reportcam.stamp.StampSpec
import cz.julienika.reportcam.stamp.StampStyle
import cz.julienika.reportcam.stamp.TextField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

/** A version's file name is a hash of what it shows (Owner, G-004; D025). */
class VersionNameTest {
    private val at = ZonedDateTime.of(2026, 11, 4, 9, 30, 15, 123_000_000, ZoneId.of("Europe/Prague"))
    private val dark = StampStyle(textColor = 0xFFFFFFFF.toInt(), backgroundColor = 0x99000000.toInt())
    private val light = StampStyle(textColor = 0xFF000000.toInt(), backgroundColor = 0xB3FFFFFF.toInt())

    private fun spec(style: StampStyle) =
        StampSpec(listOf(TextField(), DateTimeField("yyyy-MM-dd HH:mm", locale = Locale.ROOT)), OverlayPlacement(Corner.BOTTOM_START), style)

    private fun data(note: String?) = StampData.EMPTY.with(StampKeys.CAPTURE_TIME, at).with(StampKeys.USER_TEXT, note)

    private fun name(
        style: StampStyle = dark,
        note: String? = "Stavba 4",
        precise: Float = 20f,
    ) = PhotoPipeline.fileName(spec(style), data(note), precise)

    @Test
    fun theNameIsTheCaptureTimeAndAShortKey() {
        assertTrue(name(), Regex("report_20261104_093015_123_[0-9a-f]{8}[.]jpg").matches(name()))
    }

    @Test
    fun theSameLookGetsTheSameNameEveryTime() {
        // Dark, then light, then dark again: the third is the first version, found again.
        assertEquals(name(dark), name(dark))
        assertNotEquals(name(dark), name(light))
        // Note off, then on again with the same text.
        assertEquals(name(note = "Stavba 4"), name(note = "Stavba 4"))
    }

    @Test
    fun anythingThePhotoShowsChangesTheName() {
        val first = name()
        assertNotEquals(first, name(note = null))
        assertNotEquals(first, name(note = "Stavba 5"))
        assertNotEquals(first, PhotoPipeline.fileName(StampSpec(spec(dark).fields, FramePlacement(top = false), dark), data("Stavba 4"), 20f))
        // The EXIF accuracy verdict follows the threshold, so a version kept under another one is not reused.
        assertNotEquals(first, name(precise = 50f))
    }
}
