package cz.julienika.reportcam.location

import android.location.Geocoder
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cz.julienika.reportcam.stamp.GeoFix
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/**
 * The real geocoder through the device's service. Needs the network: without it the lookups are skipped,
 * not failed, because "offline gives no place" is the designed behaviour (D011).
 */
@RunWith(AndroidJUnit4::class)
class GeocoderPlaceLookupTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val oldTownSquare = GeoFix(50.08765, 14.42133, 5f, System.currentTimeMillis())

    @Test
    fun aPragueFixGetsAStreetAndACity() {
        assumeTrue("no geocoder on this device", Geocoder.isPresent())
        @Suppress("DEPRECATION")
        val raw = Geocoder(context, Locale("cs", "CZ")).getFromLocation(oldTownSquare.latitude, oldTownSquare.longitude, 1)?.firstOrNull()
        assumeTrue("geocoder unreachable (offline?)", raw != null)
        Log.i(TAG, "raw: $raw")
        val place = runBlocking { GeocoderPlaceLookup(context).lookup(oldTownSquare) }
        assertNotNull(place)
        assertNotNull("street", place!!.street)
        assertTrue("city ${place.city}", place.city?.contains("Praha") == true)
    }

    @Test
    fun theTimeoutGivesNoPlaceInsteadOfWaiting() {
        val started = System.currentTimeMillis()
        val place = runBlocking { GeocoderPlaceLookup(context, timeoutMs = 1).lookup(oldTownSquare) }
        assertNull(place)
        assertTrue("returned in time", System.currentTimeMillis() - started < 1_000)
    }

    private companion object {
        const val TAG = "GeocoderTest"
    }
}
