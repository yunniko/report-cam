package cz.julienika.reportcam.location

import cz.julienika.reportcam.stamp.Place
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaceLookupTest {
    private fun place(
        street: String? = null,
        houseNumber: String? = null,
        featureName: String? = null,
        city: String? = null,
        line: String? = null,
    ) = GeocoderPlaceLookup.placeOf(street, houseNumber, featureName, city, line)

    @Test
    fun aNumberOnlyGivenAsTheFeatureNameIsTheHouseNumber() {
        assertEquals("846/1", place(street = "Václavské náměstí", featureName = "846/1", city = "Praha")?.houseNumber)
        assertEquals("12a", place(city = "Lhota", featureName = "12a")?.houseNumber)
    }

    @Test
    fun aFeatureNameThatIsNotANumberIsIgnored() {
        assertNull(place(street = "Dlouhá", featureName = "Dlouhá", city = "Praha")?.houseNumber)
        assertNull(place(city = "Praha", featureName = "Obecní dům")?.houseNumber)
    }

    @Test
    fun aRealHouseNumberWinsOverTheFeatureName() {
        assertEquals("5", place(street = "Dlouhá", houseNumber = " 5 ", featureName = "7", city = "Praha")?.houseNumber)
    }

    @Test
    fun blankPartsAreMissingAndNothingUsableIsNull() {
        assertEquals(Place(city = "Praha"), place(street = " ", city = "Praha", line = ""))
        assertNull(place(houseNumber = "5"))
        assertNull(place())
    }

    private val oldTownLine = "Staroměstské nám. 1, 110 00 Praha 1-Staré Město, Česko"

    @Test
    fun withNoLocalityTheCityComesFromTheAddressLineAfterThePostcode() {
        // The fields Google returned for Old Town Square on the emulator (G-002, 2026-11-03).
        val p =
            GeocoderPlaceLookup.placeOf(
                street = "Staroměstské náměstí",
                houseNumber = null,
                featureName = "1",
                locality = null,
                addressLine = oldTownLine,
                region = "Hlavní město Praha",
                postalCode = "110 00",
            )
        assertEquals(Place("Staroměstské náměstí", "1", "Praha 1-Staré Město", oldTownLine), p)
    }

    @Test
    fun aLocalityWinsAndTheRegionIsTheLastFallback() {
        val withLocality = GeocoderPlaceLookup.placeOf(null, null, null, "Brno", oldTownLine, "Jihomoravský kraj", "110 00")
        assertEquals("Brno", withLocality?.city)
        val noPostcode = GeocoderPlaceLookup.placeOf(null, null, null, null, oldTownLine, "Hlavní město Praha", null)
        assertEquals("Hlavní město Praha", noPostcode?.city)
        val postcodeNotInLine = GeocoderPlaceLookup.placeOf(null, null, null, null, "Dlouhá 5, Česko", "Hlavní město Praha", "110 00")
        assertEquals("Hlavní město Praha", postcodeNotInLine?.city)
    }

    @Test
    fun aPostcodeStandingAloneInTheLineGivesNoCity() {
        assertNull(GeocoderPlaceLookup.cityInLine("Dlouhá 5, 110 00, Česko", "110 00"))
    }
}
