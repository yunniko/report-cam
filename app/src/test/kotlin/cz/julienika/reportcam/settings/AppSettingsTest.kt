package cz.julienika.reportcam.settings

import cz.julienika.reportcam.photo.AspectRatio
import cz.julienika.reportcam.share.Messenger
import cz.julienika.reportcam.stamp.Corner
import cz.julienika.reportcam.stamp.FramePlacement
import cz.julienika.reportcam.stamp.GeoFix
import cz.julienika.reportcam.stamp.LocationReading
import cz.julienika.reportcam.stamp.OverlayPlacement
import cz.julienika.reportcam.stamp.Place
import cz.julienika.reportcam.stamp.PlaceFormat
import cz.julienika.reportcam.stamp.SchemeId
import cz.julienika.reportcam.stamp.StampData
import cz.julienika.reportcam.stamp.StampKeys
import cz.julienika.reportcam.stamp.StampScheme
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
    fun defaultsAreSmallPhotoGalleryOnMessageOnLocationOnFieldsFull() {
        val s = AppSettings.from(FakeSource())
        assertEquals(1280, s.outputLongEdge)
        assertTrue(s.saveToGallery)
        assertTrue(s.message)
        assertFalse("compact text fields are off by default (Owner, G-004)", s.compactTexts)
        assertTrue(s.showLocation)
        assertFalse(s.waitForPrecise)
        assertEquals(listOf("text", "datetime", "coordinates"), s.stampSpec.fields.map { it.id })
        assertEquals(Keys.DEFAULT_PLACEMENT, s.stampSpec.placement.id)
    }

    @Test
    fun messageCanBeSwitchedOff() {
        assertFalse(AppSettings.from(FakeSource(Keys.MESSAGE to false)).message)
    }

    @Test
    fun compactTextFieldsCanBeTurnedOn() {
        assertTrue(AppSettings.from(FakeSource(Keys.COMPACT_TEXTS to true)).compactTexts)
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

    @Test
    fun theDarkSchemeIsTheDefaultWithTheOldColours() {
        val a = AppSettings.from(FakeSource())
        assertEquals(SchemeId.DARK, a.scheme)
        assertEquals(0xFFFFFFFF.toInt(), a.stampSpec.style.textColor)
        assertEquals(0x99000000.toInt(), a.stampSpec.style.backgroundColor)
    }

    @Test
    fun storedSchemeColoursAndOpacityAreUsedAndBrokenOnesFallBack() {
        val a =
            AppSettings.from(
                FakeSource(
                    Keys.STAMP_SCHEME to "light",
                    Keys.schemeBackground(SchemeId.LIGHT) to "#EBE6DC",
                    Keys.schemeText(SchemeId.LIGHT) to "#101B30",
                    Keys.schemeOpacity(SchemeId.LIGHT) to "100",
                    Keys.schemeBackground(SchemeId.DARK) to "not a colour",
                    Keys.schemeOpacity(SchemeId.DARK) to "5",
                ),
            )
        assertEquals(SchemeId.LIGHT, a.scheme)
        assertEquals(0xFF101B30.toInt(), a.stampSpec.style.textColor)
        assertEquals(0xFFEBE6DC.toInt(), a.stampSpec.style.backgroundColor)
        val dark = a.schemes.getValue(SchemeId.DARK)
        assertEquals(StampScheme.DEFAULT_DARK.background, dark.background)
        assertEquals("clamped to the minimum", StampScheme.MIN_OPACITY, dark.overlayOpacity)
    }

    @Test
    fun aColourOutsideItsSchemesPaletteFallsBackToTheDefault() {
        val a =
            AppSettings.from(
                FakeSource(
                    // Light ones in the dark scheme: valid colours, but from the old shared palette.
                    Keys.schemeBackground(SchemeId.DARK) to "#FFD600",
                    Keys.schemeText(SchemeId.DARK) to "#000000",
                ),
            )
        val dark = a.schemes.getValue(SchemeId.DARK)
        assertEquals(StampScheme.DEFAULT_DARK.background, dark.background)
        assertEquals(StampScheme.DEFAULT_DARK.text, dark.text)
    }

    @Test
    fun withSchemeSwitchesTheStyleAndKeepsTheTextSize() {
        val a = AppSettings.from(FakeSource(Keys.TEXT_SIZE to "large"))
        val light = a.withScheme(SchemeId.LIGHT)
        assertEquals(SchemeId.LIGHT, light.scheme)
        assertEquals(0xFF000000.toInt(), light.stampSpec.style.textColor)
        assertEquals(StampStyle.TEXT_LARGE, light.stampSpec.style.textSizeFraction, 0f)
        assertEquals(a.stampSpec.fields, light.stampSpec.fields)
    }

    @Test
    fun withPlacementMovesTheStampAndAFrameKeepsTheBorderFromSettings() {
        val a = AppSettings.from(FakeSource(Keys.PLACEMENT to "overlay_top_end", Keys.FRAME_BORDER to "thick"))
        assertEquals("overlay_top_end", a.placementId)
        val framed = a.withPlacement("frame_bottom")
        assertEquals(FramePlacement(top = false, borderFraction = 0.1f), framed.stampSpec.placement)
        assertEquals(a.stampSpec.style, framed.stampSpec.style)
        assertEquals(a.stampSpec.fields, framed.stampSpec.fields)
        assertEquals(OverlayPlacement(Corner.BOTTOM_START), framed.withPlacement("overlay_bottom_start").stampSpec.placement)
        // An id no build knows falls back to the default, as a stored one does.
        assertEquals(Keys.DEFAULT_PLACEMENT, a.withPlacement("nowhere").placementId)
    }

    @Test
    fun theOldColourChoiceCarriesOverToTheSchemes() {
        assertEquals(SchemeId.LIGHT, AppSettings.from(FakeSource(Keys.LEGACY_COLORS to "dark_on_light")).scheme)
        val yellow = AppSettings.from(FakeSource(Keys.LEGACY_COLORS to "yellow_on_dark"))
        assertEquals(SchemeId.DARK, yellow.scheme)
        assertEquals(0xFFFFD600.toInt(), yellow.stampSpec.style.textColor)
        assertEquals(0xB3000000.toInt(), yellow.stampSpec.style.backgroundColor)
        // A scheme chosen since wins over the old key.
        assertEquals(SchemeId.DARK, AppSettings.from(FakeSource(Keys.LEGACY_COLORS to "dark_on_light", Keys.STAMP_SCHEME to "dark")).scheme)
    }

    @Test
    fun theLegacySeedWritesTheCarriedOverChoiceOnce() {
        assertEquals(
            mapOf(Keys.STAMP_SCHEME to "dark", Keys.schemeText(SchemeId.DARK) to "#FFD600", Keys.schemeOpacity(SchemeId.DARK) to "70"),
            AppSettings.legacySeed(FakeSource(Keys.LEGACY_COLORS to "yellow_on_dark")),
        )
        assertEquals(mapOf(Keys.STAMP_SCHEME to "dark"), AppSettings.legacySeed(FakeSource()))
        assertEquals(emptyMap<String, String>(), AppSettings.legacySeed(FakeSource(Keys.STAMP_SCHEME to "light")))
    }

    @Test
    fun theWeekdayIsOffByDefaultAndPrintsBeforeTheDateWhenOn() {
        val data = StampData.EMPTY.with(StampKeys.CAPTURE_TIME, java.time.ZonedDateTime.parse("2026-11-03T13:38:47+01:00[Europe/Prague]"))
        fun dateLine(vararg v: Pair<String, Any>) =
            AppSettings
                .from(FakeSource(*v))
                .stampSpec.fields
                .single { it.id == "datetime" }
                .lines(data)
                .single()
        assertEquals("2026-11-03 13:38:47", dateLine())
        assertTrue(dateLine(Keys.SHOW_WEEKDAY to true).endsWith(" 2026-11-03 13:38:47"))
        assertEquals(dateLine(Keys.SHOW_WEEKDAY to true), dateLine(Keys.SHOW_WEEKDAY to true, Keys.DATETIME_PATTERN to Keys.DEFAULT_PATTERN))
    }

    @Test
    fun theAccuracyThresholdIsOneOfTheChoicesAndDefaultsTo20m() {
        assertEquals(20f, AppSettings.from(FakeSource()).preciseWithinM)
        assertEquals(5f, AppSettings.from(FakeSource(Keys.PRECISE_ACCURACY to "5")).preciseWithinM)
        assertEquals(50f, AppSettings.from(FakeSource(Keys.PRECISE_ACCURACY to "50")).preciseWithinM)
        for (bad in listOf("7", "", "abc", "-20")) {
            assertEquals("'$bad' falls back", 20f, AppSettings.from(FakeSource(Keys.PRECISE_ACCURACY to bad)).preciseWithinM)
        }
    }

    @Test
    fun theAccuracyThresholdReachesTheStampedLocation() {
        val at15 = StampData.EMPTY.with(StampKeys.LOCATION, LocationReading.Fix(GeoFix(50.08765, 14.42133, 15f, System.currentTimeMillis())))
        fun lines(threshold: String) =
            AppSettings.from(FakeSource(Keys.PRECISE_ACCURACY to threshold)).stampSpec.fields.flatMap { it.lines(at15) }.joinToString("|")
        assertFalse(lines("20").contains("approximate"))
        assertTrue(lines("10").contains("approximate"))
    }

    @Test
    fun anUnknownThemeFollowsTheSystem() {
        assertEquals(AppTheme.SYSTEM, AppTheme.of(null))
        assertEquals(AppTheme.SYSTEM, AppTheme.of("sepia"))
        assertEquals(AppTheme.DARK, AppTheme.of("dark"))
        assertEquals(AppTheme.LIGHT, AppTheme.of("light"))
    }

    @Test
    fun theThemeReachesTheReviewControls() {
        assertEquals(AppTheme.SYSTEM, AppSettings.from(FakeSource()).appTheme)
        assertEquals(AppTheme.LIGHT, AppSettings.from(FakeSource(Keys.APP_THEME to "light")).appTheme)
        assertTrue(AppTheme.SYSTEM.isDark(systemDark = true))
        assertFalse(AppTheme.SYSTEM.isDark(systemDark = false))
        assertFalse(AppTheme.LIGHT.isDark(systemDark = true))
        assertTrue(AppTheme.DARK.isDark(systemDark = false))
    }
}
