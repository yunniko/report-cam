package cz.julienika.reportcam.settings

import cz.julienika.reportcam.photo.AspectRatio
import cz.julienika.reportcam.share.Messenger
import cz.julienika.reportcam.stamp.FramePlacement
import cz.julienika.reportcam.stamp.GeoFix
import cz.julienika.reportcam.stamp.LocationReading
import cz.julienika.reportcam.stamp.OverlayPlacement
import cz.julienika.reportcam.stamp.Place
import cz.julienika.reportcam.stamp.PlaceFormat
import cz.julienika.reportcam.stamp.StampData
import cz.julienika.reportcam.stamp.StampKeys
import cz.julienika.reportcam.stamp.StampStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

        @Suppress("UNCHECKED_CAST")
        override fun stringSet(
            key: String,
            default: Set<String>,
        ) = map[key] as? Set<String> ?: default
    }

    @Test
    fun placeNamesAreOffByDefaultAndSitAboveTheCoordinatesWhenOn() {
        assertNull(AppSettings.from(FakeSource()).placeFormat)
        val on = AppSettings.from(FakeSource(Keys.PLACE_FORMAT to "street_city"))
        assertEquals(PlaceFormat.STREET_CITY, on.placeFormat)
        assertEquals(listOf("text", "datetime", "place", "coordinates"), on.stampSpec.fields.map { it.id })
        val noLocation = AppSettings.from(FakeSource(Keys.PLACE_FORMAT to "city", Keys.SHOW_LOCATION to false))
        assertNull("no location, no place", noLocation.placeFormat)
        assertEquals(listOf("text", "datetime"), noLocation.stampSpec.fields.map { it.id })
    }

    private val fixData =
        StampData.EMPTY
            .with(StampKeys.LOCATION, LocationReading.Fix(GeoFix(50.08765, 14.42133, 5f, System.currentTimeMillis())))
    private val withPlace = fixData.with(StampKeys.PLACE, Place(street = "Dlouhá", houseNumber = "5", city = "Praha"))

    private fun stampLines(
        s: AppSettings,
        data: StampData,
    ) = s.stampSpec.fields.flatMap { it.lines(data) }

    @Test
    fun coordinatesOffLeavesOnlyThePlaceNameButReturnWhenNoneIsFound() {
        val s = AppSettings.from(FakeSource(Keys.PLACE_FORMAT to "street_city", Keys.SHOW_COORDINATES to false))
        assertEquals(listOf("Dlouhá 5, Praha"), stampLines(s, withPlace))
        assertEquals(listOf("50.08765, 14.42133 ±5 m"), stampLines(s, fixData))
        // A place that prints nothing in this format counts as none found.
        val cityOnly = AppSettings.from(FakeSource(Keys.PLACE_FORMAT to "city", Keys.SHOW_COORDINATES to false))
        assertEquals(listOf("50.08765, 14.42133 ±5 m"), stampLines(cityOnly, fixData.with(StampKeys.PLACE, Place(street = "Dlouhá"))))
    }

    @Test
    fun coordinatesOffIsIgnoredWhilePlaceNamesAreOff() {
        val s = AppSettings.from(FakeSource(Keys.SHOW_COORDINATES to false))
        assertEquals(listOf("50.08765, 14.42133 ±5 m"), stampLines(s, withPlace))
    }

    @Test
    fun coordinatesStayOnByDefaultNextToThePlace() {
        val s = AppSettings.from(FakeSource(Keys.PLACE_FORMAT to "street_city"))
        assertEquals(listOf("Dlouhá 5, Praha", "50.08765, 14.42133 ±5 m"), stampLines(s, withPlace))
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

    @Test
    fun shapeAndExposureDefaultToTheWholeSensorAtZero() {
        val s = AppSettings.from(FakeSource())
        assertEquals(AspectRatio.FULL, s.aspect)
        assertEquals(0f, s.defaultEv, 0f)
    }

    @Test
    fun shapeAndExposureAreRead() {
        val s = AppSettings.from(FakeSource(Keys.ASPECT to "4:5", Keys.DEFAULT_EV to "-1.5"))
        assertEquals(AspectRatio.R4_5, s.aspect)
        assertEquals(-1.5f, s.defaultEv, 0f)
        assertEquals(0f, AppSettings.from(FakeSource(Keys.DEFAULT_EV to "bad")).defaultEv, 0f)
    }

    @Test
    fun frameBorderAppliesToFramePlacementsOnly() {
        val framed = AppSettings.from(FakeSource(Keys.PLACEMENT to "frame_bottom", Keys.FRAME_BORDER to "thick"))
        assertEquals(0.1f, (framed.stampSpec.placement as FramePlacement).borderFraction, 0f)
        val byDefault = AppSettings.from(FakeSource(Keys.PLACEMENT to "frame_top"))
        assertEquals(0.05f, (byDefault.stampSpec.placement as FramePlacement).borderFraction, 0f)
        val none = AppSettings.from(FakeSource(Keys.PLACEMENT to "frame_top", Keys.FRAME_BORDER to "none"))
        assertEquals(0f, (none.stampSpec.placement as FramePlacement).borderFraction, 0f)
        val overlay = AppSettings.from(FakeSource(Keys.FRAME_BORDER to "thick"))
        assertEquals(Keys.DEFAULT_PLACEMENT, overlay.stampSpec.placement.id)
    }

    @Test
    fun quickMessengersDefaultToWhatsappAndTelegramBeforeShare() {
        val s = AppSettings.from(FakeSource())
        assertEquals(listOf(Messenger.WHATSAPP, Messenger.TELEGRAM), s.quickMessengers)
        assertTrue(s.quickOnStart)
    }

    @Test
    fun quickMessengersKeepTheirOwnOrderAtMostThreeAndIgnoreUnknownIds() {
        val all = Messenger.entries.map { it.id }.toSet() + "icq"
        val s = AppSettings.from(FakeSource(Keys.QUICK_MESSENGERS to all.reversed().toSet(), Keys.QUICK_SIDE to "end"))
        assertEquals(listOf(Messenger.WHATSAPP, Messenger.TELEGRAM, Messenger.MESSENGER), s.quickMessengers)
        assertFalse(s.quickOnStart)
        assertEquals(emptyList<Messenger>(), AppSettings.from(FakeSource(Keys.QUICK_MESSENGERS to emptySet<String>())).quickMessengers)
    }
}
