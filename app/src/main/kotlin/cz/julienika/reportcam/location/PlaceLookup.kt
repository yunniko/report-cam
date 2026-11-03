package cz.julienika.reportcam.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log
import cz.julienika.reportcam.stamp.GeoFix
import cz.julienika.reportcam.stamp.Place
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

/** Turns the one fix being stamped into a place name. Null means "none": the stamp then shows coordinates only. */
fun interface PlaceLookup {
    suspend fun lookup(fix: GeoFix): Place?
}

/**
 * Android's [Geocoder], which on a phone with Google services asks Google over the network (Owner,
 * 2026-11-03). Called only for the fix being stamped, never in the background (D011). Offline, with no
 * geocoder, or past [timeoutMs], it gives null and the photo is stamped without a place.
 */
class GeocoderPlaceLookup(
    private val context: Context,
    private val timeoutMs: Long = TIMEOUT_MS,
) : PlaceLookup {
    override suspend fun lookup(fix: GeoFix): Place? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale.getDefault())
        val address =
            withTimeoutOrNull(timeoutMs) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        suspendCancellableCoroutine<Address?> { cont ->
                            geocoder.getFromLocation(
                                fix.latitude,
                                fix.longitude,
                                1,
                                object : Geocoder.GeocodeListener {
                                    override fun onGeocode(addresses: MutableList<Address>) {
                                        if (cont.isActive) cont.resume(addresses.firstOrNull())
                                    }

                                    override fun onError(errorMessage: String?) {
                                        Log.w(TAG, "geocoder: $errorMessage")
                                        if (cont.isActive) cont.resume(null)
                                    }
                                },
                            )
                        }
                    } else {
                        // The old call blocks and cannot be cancelled; run it detached so the timeout still
                        // returns on time, and drop its answer if it comes late.
                        suspendCancellableCoroutine<Address?> { cont ->
                            Dispatchers.IO.asExecutor().execute {
                                val found =
                                    try {
                                        @Suppress("DEPRECATION")
                                        geocoder.getFromLocation(fix.latitude, fix.longitude, 1)?.firstOrNull()
                                    } catch (e: Exception) {
                                        Log.w(TAG, "geocoder failed: ${e.message}")
                                        null
                                    }
                                if (cont.isActive) cont.resume(found)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // IOException when offline or the service is unreachable; IllegalArgumentException on bad input.
                    Log.w(TAG, "geocoder failed: ${e.message}")
                    null
                }
            }
        return address?.let(::toPlace)
    }

    companion object {
        private const val TAG = "PlaceLookup"

        /** The stamp waits at most this long for a name; a capture that pays more would feel stuck. */
        const val TIMEOUT_MS = 4_000L

        fun toPlace(a: Address): Place? =
            placeOf(
                street = a.thoroughfare,
                houseNumber = a.subThoroughfare,
                featureName = a.featureName,
                city = a.locality ?: a.subAdminArea,
                addressLine = if (a.maxAddressLineIndex >= 0) a.getAddressLine(0) else null,
            )

        /**
         * Plain-Kotlin mapping, unit-tested. Czech results often carry the house number ("846/1") only as the
         * feature name, so a number-like feature name stands in for a missing house number.
         */
        fun placeOf(
            street: String?,
            houseNumber: String?,
            featureName: String?,
            city: String?,
            addressLine: String?,
        ): Place? {
            val number = houseNumber.clean() ?: featureName.clean()?.takeIf { it.matches(HOUSE_NUMBER) }
            val p = Place(street.clean(), number, city.clean(), addressLine.clean())
            return p.takeIf { it.street != null || it.city != null || it.fullAddress != null }
        }

        private val HOUSE_NUMBER = Regex("""\d+[a-zA-Z]?(/\d+[a-zA-Z]?)?""")

        private fun String?.clean() = this?.trim()?.takeIf { it.isNotEmpty() }
    }
}
