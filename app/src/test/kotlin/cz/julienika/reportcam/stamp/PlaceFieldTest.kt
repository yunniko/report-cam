package cz.julienika.reportcam.stamp

import cz.julienika.reportcam.photo.PhotoMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class PlaceFieldTest {
    private val prague =
        Place(
            street = "Václavské náměstí",
            houseNumber = "846/1",
            city = "Praha",
            fullAddress = "Václavské náměstí 846/1, 110 00 Nové Město, Česko",
        )
    private val village = Place(houseNumber = "12", city = "Lhota")
    private val at = 1_000_000_000_000L
    private val time = ZonedDateTime.ofInstant(Instant.ofEpochMilli(at), ZoneId.of("Europe/Prague"))

    private fun data(
        place: Place?,
        accuracyM: Float = 5f,
    ) = StampData.EMPTY
        .with(StampKeys.CAPTURE_TIME, time)
        .with(StampKeys.LOCATION, LocationReading.Fix(GeoFix(50.08, 14.42, accuracyM, at)))
        .with(StampKeys.PLACE, place)

    @Test
    fun eachFormatPrintsItsParts() {
        assertEquals("Václavské náměstí 846/1, 110 00 Nové Město, Česko", PlaceFormat.FULL.format(prague))
        assertEquals("Václavské náměstí 846/1", PlaceFormat.STREET_NUMBER.format(prague))
        assertEquals("Václavské náměstí 846/1, Praha", PlaceFormat.STREET_CITY.format(prague))
        assertEquals("Praha", PlaceFormat.CITY.format(prague))
    }

    @Test
    fun aVillageWithoutStreetsIsAddressedByVillageAndNumber() {
        assertEquals("Lhota 12", PlaceFormat.STREET_NUMBER.format(village))
        assertEquals("Lhota 12", PlaceFormat.STREET_CITY.format(village))
        assertEquals("Lhota 12", PlaceFormat.FULL.format(village))
        assertEquals("Lhota", PlaceFormat.CITY.format(village))
    }

    @Test
    fun missingPartsFallBackToWhatIsKnown() {
        assertEquals("Praha", PlaceFormat.STREET_NUMBER.format(Place(city = "Praha")))
        assertEquals("Dlouhá", PlaceFormat.STREET_CITY.format(Place(street = "Dlouhá")))
        assertEquals("Dlouhá, Praha", PlaceFormat.FULL.format(Place(street = "Dlouhá", city = "Praha")))
        assertNull(PlaceFormat.CITY.format(Place(street = "Dlouhá")))
        assertNull(PlaceFormat.FULL.format(Place()))
    }

    @Test
    fun storedIdsMapToFormatsAndOffIsNull() {
        PlaceFormat.entries.forEach { assertEquals(it, PlaceFormat.of(it.id)) }
        assertNull(PlaceFormat.of("off"))
        assertNull(PlaceFormat.of("garbage"))
    }

    @Test
    fun theFieldPrintsThePlaceOfAPreciseFix() {
        assertEquals(listOf("Václavské náměstí 846/1, Praha"), PlaceField(PlaceFormat.STREET_CITY, preciseWithinM = FixQuality.DEFAULT_PRECISE_M).lines(data(prague)))
    }

    @Test
    fun anApproximateFixGetsTheCityOnlyMarkedApproximate() {
        assertEquals(listOf("≈Praha"), PlaceField(PlaceFormat.FULL, preciseWithinM = FixQuality.DEFAULT_PRECISE_M).lines(data(prague, accuracyM = 2_000f)))
    }

    @Test
    fun noPlaceOrNoFixPrintsNothing() {
        assertTrue(PlaceField(PlaceFormat.FULL, preciseWithinM = FixQuality.DEFAULT_PRECISE_M).lines(data(null)).isEmpty())
        assertTrue(PlaceField(PlaceFormat.FULL, preciseWithinM = FixQuality.DEFAULT_PRECISE_M).lines(StampData.EMPTY.with(StampKeys.PLACE, prague)).isEmpty())
        assertTrue(PlaceField(PlaceFormat.CITY, preciseWithinM = FixQuality.DEFAULT_PRECISE_M).lines(data(Place(street = "Dlouhá"))).isEmpty())
    }

    @Test
    fun thePlaceReachesTheCaptionAndXmp() {
        val spec =
            StampSpec(
                listOf(PlaceField(PlaceFormat.CITY, preciseWithinM = FixQuality.DEFAULT_PRECISE_M), CoordinatesField(preciseWithinM = FixQuality.DEFAULT_PRECISE_M)),
                OverlayPlacement(Corner.BOTTOM_START),
                StampStyle(),
            )
        val m = PhotoMetadata.of(spec, data(prague), preciseWithinM = FixQuality.DEFAULT_PRECISE_M)
        assertTrue(m.stampText, m.stampText.startsWith("Praha\n50.08000, 14.42000"))
        assertTrue(m.xmp(), m.xmp().contains("<rc:place>Praha</rc:place>"))
    }
}
