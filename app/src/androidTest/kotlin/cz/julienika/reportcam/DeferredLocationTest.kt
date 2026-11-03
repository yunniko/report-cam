package cz.julienika.reportcam

import android.Manifest
import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import cz.julienika.reportcam.helpers.canSend
import cz.julienika.reportcam.helpers.pendingDir
import cz.julienika.reportcam.helpers.view
import cz.julienika.reportcam.helpers.waitFor
import cz.julienika.reportcam.settings.Keys
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * "Wait for precise location" through the real tracker, fed by mock providers (D008): the shutter fires
 * at once, the photo waits unstamped with a live readout, and nothing can be sent until it settles.
 */
@RunWith(AndroidJUnit4::class)
class DeferredLocationTest {
    @get:Rule
    val permissions: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION)

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
    private val pendingDir = context.pendingDir()

    @Before
    fun setUp() {
        shell("appops set ${context.packageName} android:mock_location allow")
        for (p in providers) {
            runCatching { lm.removeTestProvider(p) }
            @Suppress("DEPRECATION")
            lm.addTestProvider(p, false, false, false, false, true, true, true, 1, 1)
            lm.setTestProviderEnabled(p, true)
        }
        push(APPROX_M)
        pendingDir.deleteRecursively()
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .edit()
            .clear()
            .putString(Keys.STAMP_TEXT, "Deferred")
            .putBoolean(Keys.WAIT_PRECISE, true)
            .putBoolean(Keys.SAVE_GALLERY, false)
            .commit()
        Intents.init()
        intending(hasAction(Intent.ACTION_CHOOSER)).respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))
    }

    @After
    fun tearDown() {
        runCatching { Intents.release() } // not initialised when setUp itself failed
        for (p in providers) runCatching { lm.removeTestProvider(p) }
        shell("appops set ${context.packageName} android:mock_location deny")
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
        pendingDir.deleteRecursively()
    }

    /** Runs [cmd] and waits for it to finish: the command runs asynchronously until its output is read. */
    private fun shell(cmd: String) {
        val out = instrumentation.uiAutomation.executeShellCommand(cmd)
        ParcelFileDescriptor.AutoCloseInputStream(out).use { it.readBytes() }
    }

    /** One fix on every mock provider, timestamped now. */
    private fun push(accuracyM: Float) {
        for (p in providers) {
            val loc =
                Location(p).apply {
                    latitude = LAT
                    longitude = LON
                    accuracy = accuracyM
                    time = System.currentTimeMillis()
                    elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                }
            lm.setTestProviderLocation(p, loc)
        }
    }

    private fun ActivityScenario<CameraActivity>.shootIntoWaiting() {
        waitFor("live preview") { it.view(R.id.shutter).isEnabled }
        onActivity { it.view(R.id.shutter).performClick() }
        waitFor("photo waiting for a fix") { it.view(R.id.pending_row).visibility == View.VISIBLE }
        onActivity {
            assertFalse("no sending while waiting", it.canSend())
            assertEquals(View.VISIBLE, it.view(R.id.use_current).visibility)
            assertEquals(View.GONE, it.view(R.id.camera_controls).visibility)
        }
        waitFor("readout of the approximate fix") {
            (it.view(R.id.pending_status) as TextView).text.contains("±500 m")
        }
    }

    private fun lastCaption(): String {
        val chooser = Intents.getIntents().last { it.action == Intent.ACTION_CHOOSER }
        @Suppress("DEPRECATION")
        val send = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
        return send.getStringExtra(Intent.EXTRA_TEXT)!!
    }

    @Test
    fun aPreciseFixStampsTheWaitingPhotoByItself() {
        ActivityScenario.launch(CameraActivity::class.java).use { s ->
            s.shootIntoWaiting()
            push(PRECISE_M)
            s.waitFor("stamped after the precise fix") { it.canSend() }
            s.onActivity {
                assertEquals(View.GONE, it.view(R.id.pending_row).visibility)
                it.view(R.id.share_other).performClick()
            }
            val caption = lastCaption()
            assertTrue(caption, caption.contains("±5 m"))
            assertTrue(caption, !caption.contains("≈"))
        }
    }

    @Test
    fun useCurrentStampsTheApproximateFix() {
        ActivityScenario.launch(CameraActivity::class.java).use { s ->
            s.shootIntoWaiting()
            s.onActivity {
                assertEquals(it.getString(R.string.use_current), (it.view(R.id.use_current) as Button).text.toString())
                it.view(R.id.use_current).performClick()
            }
            s.waitFor("stamped with the current fix") { it.canSend() }
            s.onActivity { it.view(R.id.share_other).performClick() }
            val caption = lastCaption()
            assertTrue(caption, caption.contains("≈") && caption.contains("±500 m"))
        }
    }

    @Test
    fun retakeWhileWaitingReturnsToTheLiveCamera() {
        ActivityScenario.launch(CameraActivity::class.java).use { s ->
            s.shootIntoWaiting()
            s.onActivity { it.view(R.id.retake).performClick() }
            s.waitFor("live preview again") { it.view(R.id.shutter).isEnabled }
            // A precise fix arriving now must not stamp anything: the waiting photo is gone.
            push(PRECISE_M)
            Thread.sleep(1_500)
            s.onActivity {
                assertEquals(View.GONE, it.view(R.id.review_controls).visibility)
                assertEquals(View.VISIBLE, it.view(R.id.camera_controls).visibility)
            }
        }
    }

    @Test
    fun aWaitingPhotoSurvivesTheAppBeingClosedAndReopened() {
        ActivityScenario.launch(CameraActivity::class.java).use { it.shootIntoWaiting() }
        // The first activity is destroyed with everything it held in memory; only the disk copy remains (D010).
        assertTrue("waiting photo kept on disk", File(pendingDir, "meta.properties").isFile)
        ActivityScenario.launch(CameraActivity::class.java).use { s ->
            s.waitFor("the same photo waiting again") { it.view(R.id.pending_row).visibility == View.VISIBLE }
            s.onActivity {
                assertEquals("the camera stays closed", View.GONE, it.view(R.id.camera_controls).visibility)
                assertFalse(it.canSend())
            }
            push(PRECISE_M)
            s.waitFor("stamped after the precise fix") { it.canSend() }
            s.onActivity { it.view(R.id.share_other).performClick() }
            val caption = lastCaption()
            assertTrue(caption, caption.startsWith("Deferred\n") && caption.contains("±5 m"))
            assertFalse("disk copy dropped once stamped", File(pendingDir, "meta.properties").exists())
        }
    }

    @Test
    fun retakeDropsTheDiskCopyToo() {
        ActivityScenario.launch(CameraActivity::class.java).use { s ->
            s.shootIntoWaiting()
            s.onActivity { it.view(R.id.retake).performClick() }
            s.waitFor("live preview again") { it.view(R.id.shutter).isEnabled }
        }
        assertFalse(File(pendingDir, "meta.properties").exists())
        ActivityScenario.launch(CameraActivity::class.java).use { s ->
            s.waitFor("live preview, nothing restored") { it.view(R.id.shutter).isEnabled }
        }
    }

    private companion object {
        const val LAT = 50.087654
        const val LON = 14.421329
        const val APPROX_M = 500f
        const val PRECISE_M = 5f
    }
}
