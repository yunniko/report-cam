package cz.julienika.reportcam.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import cz.julienika.reportcam.stamp.GeoFix
import cz.julienika.reportcam.stamp.LocationReading

enum class LocationAccess { DENIED, SERVICES_OFF, APPROXIMATE_ONLY, PRECISE }

data class LocationStatus(
    val access: LocationAccess,
    val fix: GeoFix?,
    /** True while the receiver is on and looking for a better fix. */
    val updating: Boolean,
)

/** What a stamp made from this status carries: null with no permission (no line), [LocationReading.None] with no fix. */
fun LocationStatus.reading(): LocationReading? =
    if (access == LocationAccess.DENIED) null else fix?.let { LocationReading.Fix(it) } ?: LocationReading.None

/**
 * Foreground-only location. [start] in onStart begins a search; once a precise fix arrives the receiver
 * goes off, and a new search starts after a rest (Owner: tracked in reasonable cycles while the app runs,
 * refreshed on start and wake, never triggered by a capture). [stop] in onStop ends everything. See D005.
 */
class LocationTracker(
    context: Context,
) {
    private val app = context.applicationContext
    private val lm = app.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private var searching = false
    private var searchStartedAt = 0L
    private var best: GeoFix? = null
    private val registered = mutableListOf<String>()

    var onStatus: ((LocationStatus) -> Unit)? = null

    var status: LocationStatus = LocationStatus(LocationAccess.DENIED, null, false)
        private set

    private val listener = LocationListenerCompat { onLocation(it) }
    private val restOver = Runnable { if (running) beginSearch() }
    private val searchTimeout = Runnable { if (running && searching) endSearch() }

    fun access(): LocationAccess {
        val fine = granted(Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = granted(Manifest.permission.ACCESS_COARSE_LOCATION)
        return when {
            !fine && !coarse -> LocationAccess.DENIED
            !LocationManagerCompat.isLocationEnabled(lm) -> LocationAccess.SERVICES_OFF
            fine -> LocationAccess.PRECISE
            else -> LocationAccess.APPROXIMATE_ONLY
        }
    }

    /** What a capture right now puts on the stamp: null with no permission (no line at all). */
    fun reading(): LocationReading? = LocationStatus(access(), best, false).reading()

    fun start() {
        running = true
        seedFromLastKnown()
        beginSearch()
    }

    /** Starts a search now if resting; used while the shutter waits for a precise fix. */
    fun ensureSearching() {
        if (running && !searching) {
            handler.removeCallbacks(restOver)
            beginSearch()
        }
    }

    fun stop() {
        running = false
        handler.removeCallbacks(restOver)
        handler.removeCallbacks(searchTimeout)
        unregister()
        searching = false
        publish()
    }

    private fun beginSearch() {
        unregister()
        val access = access()
        if (access == LocationAccess.DENIED || access == LocationAccess.SERVICES_OFF) {
            searching = false
            publish()
            return
        }
        searching = true
        searchStartedAt = SystemClock.elapsedRealtime()
        val request =
            LocationRequestCompat
                .Builder(FixPolicy.SEARCH_INTERVAL_MS)
                .setQuality(LocationRequestCompat.QUALITY_HIGH_ACCURACY)
                .build()
        for (provider in providers(access)) {
            try {
                LocationManagerCompat.requestLocationUpdates(lm, provider, request, listener, Looper.getMainLooper())
                registered += provider
            } catch (e: SecurityException) {
                // Permission revoked between the check and the call: treated as denied on the next status.
            } catch (e: IllegalArgumentException) {
                // Provider missing on this device.
            }
        }
        handler.postDelayed(searchTimeout, FixPolicy.SEARCH_TIMEOUT_MS)
        publish()
    }

    private fun endSearch() {
        unregister()
        searching = false
        handler.removeCallbacks(searchTimeout)
        handler.postDelayed(restOver, FixPolicy.REST_MS)
        publish()
    }

    private fun onLocation(location: Location) {
        val fix = location.toFix(coarseOnly = access() == LocationAccess.APPROXIMATE_ONLY)
        if (FixPolicy.better(best, fix)) best = fix
        if (searching && FixPolicy.satisfied(best, System.currentTimeMillis())) endSearch() else publish()
    }

    @SuppressLint("MissingPermission") // checked by access() before every call path
    private fun seedFromLastKnown() {
        val access = access()
        if (access == LocationAccess.DENIED) return
        for (p in providers(access)) {
            val last =
                try {
                    lm.getLastKnownLocation(p)
                } catch (e: SecurityException) {
                    null
                } catch (e: IllegalArgumentException) {
                    null
                } ?: continue
            val fix = last.toFix(access == LocationAccess.APPROXIMATE_ONLY)
            if (FixPolicy.better(best, fix)) best = fix
        }
    }

    private fun providers(access: LocationAccess): List<String> {
        val enabled = lm.getProviders(true)
        val wanted =
            if (access == LocationAccess.PRECISE) {
                listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            } else {
                listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            }
        return wanted.filter { it in enabled }
    }

    private fun unregister() {
        if (registered.isNotEmpty()) LocationManagerCompat.removeUpdates(lm, listener)
        registered.clear()
    }

    private fun publish() {
        status = LocationStatus(access(), best, searching && running)
        onStatus?.invoke(status)
    }

    private fun granted(p: String) = ContextCompat.checkSelfPermission(app, p) == PackageManager.PERMISSION_GRANTED

    private fun Location.toFix(coarseOnly: Boolean) =
        GeoFix(
            latitude = latitude,
            longitude = longitude,
            accuracyM = if (hasAccuracy()) accuracy else null,
            timeMillis = time,
            coarseOnly = coarseOnly,
        )
}
