package cz.julienika.reportcam.photo

import cz.julienika.reportcam.stamp.CoordinatesField
import cz.julienika.reportcam.stamp.Corner
import cz.julienika.reportcam.stamp.DateTimeField
import cz.julienika.reportcam.stamp.GeoFix
import cz.julienika.reportcam.stamp.LocationReading
import cz.julienika.reportcam.stamp.OverlayPlacement
import cz.julienika.reportcam.stamp.StampData
import cz.julienika.reportcam.stamp.StampKeys
import cz.julienika.reportcam.stamp.StampSpec
import cz.julienika.reportcam.stamp.StampStyle
import cz.julienika.reportcam.stamp.TextField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

class PhotoMetadataTest {
    private val at = ZonedDateTime.of(2026, 11, 3, 14, 5, 9, 0, ZoneId.of("Europe/Prague"))
    private val atMs = at.toInstant().toEpochMilli()
    private val spec =
        StampSpec(
            listOf(TextField(), DateTimeField("yyyy-MM-dd HH:mm", locale = Locale.ROOT), CoordinatesField(nowMillis = { atMs })),
            OverlayPlacement(Corner.BOTTOM_START),
            StampStyle(),
        )

    private fun data(
        text: String,
        reading: LocationReading?,
    ) = StampData.EMPTY
        .with(StampKeys.CAPTURE_TIME, at)
        .with(StampKeys.USER_TEXT, text)
        .with(StampKeys.LOCATION, reading)

    @Test
    fun stampTextIsTheStampLinesInOrder() {
        val m = PhotoMetadata.of(spec, data("Žižkov <dvůr> & sklad", null))
        assertEquals("Žižkov <dvůr> & sklad\n2026-11-03 14:05", m.stampText)
    }

    @Test
    fun asciiDescriptionStripsAccentsAndKeepsAscii() {
        val m = PhotoMetadata.of(spec, data("Žluťoučký kůň", null))
        assertEquals("Zlutoucky kun | 2026-11-03 14:05", m.asciiDescription)
        assertTrue(m.asciiDescription.all { it.code in 32..126 })
    }

    @Test
    fun xmpEscapesTextAndCarriesTheRawFix() {
        val fix = GeoFix(50.087654, 14.421329, 7.5f, atMs - 90_000, false)
        val xmp = PhotoMetadata.of(spec, data("A <b> & \"c\"", LocationReading.Fix(fix))).xmp()
        assertTrue(xmp.contains("A &lt;b&gt; &amp; &quot;c&quot;"))
        assertFalse(xmp.contains("<b>"))
        assertTrue(xmp.contains("<rc:latitude>50.087654</rc:latitude>"))
        assertTrue(xmp.contains("<rc:accuracyM>7.5</rc:accuracyM>"))
        assertTrue(xmp.contains("<rc:fixAgeS>90</rc:fixAgeS>"))
        assertTrue(xmp.contains("<rc:approximate>false</rc:approximate>"))
        assertTrue(xmp.contains("xmlns:rc=\"${PhotoMetadata.NS}\""))
    }

    @Test
    fun xmpIsAsciiAndKeepsEveryCharacterAsAReference() {
        val fix = GeoFix(50.0, 14.0, 1500f, atMs - 10_000, true)
        val xmp = PhotoMetadata.of(spec, data("Žižkov 📷", LocationReading.Fix(fix))).xmp()
        assertTrue(xmp.all { it.code in 0..126 })
        assertTrue(xmp, xmp.contains("&#x17D;i&#x17E;kov &#x1F4F7;"))
        assertTrue(xmp, xmp.contains("&#x2248;50.00000, 14.00000 &#xB1;1.5 km"))
    }

    @Test
    fun asciiDescriptionSpellsOutStampSigns() {
        val fix = GeoFix(50.0, 14.0, 1500f, atMs - 10_000, true)
        val d = PhotoMetadata.of(spec, data("", LocationReading.Fix(fix))).asciiDescription
        assertTrue(d, d.contains("~50.00000, 14.00000 +/-1.5 km"))
        assertEquals("A - B", PhotoMetadata.of(spec, data("A – B", null)).asciiDescription.substringBefore(" |"))
    }

    @Test
    fun deniedLocationLeavesNoLocationInMetadata() {
        val xmp = PhotoMetadata.of(spec, data("x", null)).xmp()
        assertFalse(xmp.contains("rc:coordinates"))
        assertFalse(xmp.contains("rc:latitude"))
        assertFalse(xmp.contains("rc:location"))
    }

    @Test
    fun noFixYetIsRecordedAsUnavailable() {
        val xmp = PhotoMetadata.of(spec, data("x", LocationReading.None)).xmp()
        assertTrue(xmp.contains("<rc:location>unavailable</rc:location>"))
    }
}
