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
}
