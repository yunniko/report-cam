package cz.julienika.reportcam.location

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment

/**
 * One place that turns "location is not usable" into the right next step: ask for permission while
 * Android still shows the dialog, otherwise open the app's settings; for services off, open the phone's
 * location settings. Must be constructed before the host reaches STARTED (it registers a result launcher).
 */
class LocationPermissions private constructor(
    caller: ActivityResultCaller,
    private val host: () -> Host,
    private val onResult: () -> Unit,
) {
    constructor(activity: androidx.activity.ComponentActivity, onResult: () -> Unit) :
        this(activity, { Host.of(activity) }, onResult)

    constructor(fragment: Fragment, onResult: () -> Unit) :
        this(fragment, { Host.of(fragment.requireActivity()) }, onResult)

    private var askedAt = 0L

    private val launcher =
        caller.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val granted = result.values.any { it }
            // Android stops showing the dialog after repeated denials and answers at once. An instant
            // denial therefore means "no dialog was shown": the only way left is the app's settings page.
            if (!granted && SystemClock.elapsedRealtime() - askedAt < INSTANT_MS) openAppSettings()
            onResult()
        }

    fun resolve(access: LocationAccess) {
        when (access) {
            LocationAccess.SERVICES_OFF -> host().start(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            LocationAccess.DENIED, LocationAccess.APPROXIMATE_ONLY -> request()
            LocationAccess.PRECISE -> Unit
        }
    }

    fun request() {
        askedAt = SystemClock.elapsedRealtime()
        launcher.launch(PERMISSIONS)
    }

    private fun openAppSettings() {
        val h = host()
        h.start(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", h.packageName, null)))
    }

    /** The two things needed from whichever Activity hosts this. */
    class Host(
        val packageName: String,
        val start: (Intent) -> Unit,
    ) {
        companion object {
            fun of(a: android.app.Activity) = Host(a.packageName) { a.startActivity(it) }
        }
    }

    companion object {
        val PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        private const val INSTANT_MS = 400L
    }
}
