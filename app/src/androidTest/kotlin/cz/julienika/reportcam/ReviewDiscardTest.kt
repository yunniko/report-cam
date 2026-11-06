package cz.julienika.reportcam

import android.Manifest
import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import cz.julienika.reportcam.helpers.assertGalleryCopies
import cz.julienika.reportcam.helpers.canSend
import cz.julienika.reportcam.helpers.deleteGalleryCopies
import cz.julienika.reportcam.helpers.deleteGalleryCopiesTakenAt
import cz.julienika.reportcam.helpers.galleryCopiesTakenAt
import cz.julienika.reportcam.helpers.keptState
import cz.julienika.reportcam.helpers.pendingDir
import cz.julienika.reportcam.helpers.shootAndStamp
import cz.julienika.reportcam.helpers.view
import cz.julienika.reportcam.helpers.waitFor
import cz.julienika.reportcam.settings.Keys
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.ZonedDateTime

/**
 * "Discard unsent photo after" (G-007): a photo still in review that long after it was taken is dropped
 * when the app next opens or comes back, with a gallery copy first when "Save discarded photo" is on.
 */
@RunWith(AndroidJUnit4::class)
class ReviewDiscardTest {
    @get:Rule
    val permissions: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION)

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    private val pendingDir = context.pendingDir()
    private val savedNames = mutableSetOf<String>()
    private val savedTimes = mutableSetOf<Long>()

    @Before
    fun setUp() {
        pendingDir.deleteRecursively()
        prefs
            .edit()
            .clear()
            .putString(Keys.STAMP_TEXT, "Discard test")
            .putBoolean(Keys.SAVE_GALLERY, false)
            .commit()
        Intents.init()
        intending(hasAction(Intent.ACTION_CHOOSER)).respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))
    }

    @After
    fun tearDown() {
        runCatching { Intents.release() }
        prefs.edit().clear().commit()
        pendingDir.deleteRecursively()
        context.deleteGalleryCopies(savedNames)
        for (t in savedTimes) context.deleteGalleryCopiesTakenAt(t)
    }

    private fun discardAfter(
        minutes: String,
        save: Boolean,
    ) {
        prefs.edit().putString(Keys.DISCARD_AFTER, minutes).putBoolean(Keys.SAVE_DISCARDED, save).commit()
    }

    /** Shoots, stamps and leaves; returns the kept file's name. */
    private fun shootAndLeave(): String {
        ActivityScenario.launch(CameraActivity::class.java).use { it.shootAndStamp(pendingDir) }
        return keptState(pendingDir).getProperty("file")!!.also { savedNames += it }
    }

    /** Rewrites the kept photo's capture time [hours] into the past, as if the phone had sat that long. */
    private fun ageKept(hours: Long) {
        val f = File(pendingDir, "ready.properties")
        val p = keptState(pendingDir)
        p.setProperty("capture_time", ZonedDateTime.parse(p.getProperty("capture_time")).minusHours(hours).toString())
        f.outputStream().use { p.store(it, null) }
    }

    private fun ActivityScenario<CameraActivity>.waitForCamera() = waitFor("live preview, nothing in review") { it.view(R.id.shutter).isEnabled && !it.canSend() }

    @Test
    fun aStalePhotoIsDroppedAndSavedOnTheNextStart() {
        val name = shootAndLeave()
        ageKept(2)
        discardAfter("60", save = true)
        ActivityScenario.launch(CameraActivity::class.java).use { s ->
            s.waitForCamera()
            assertFalse("nothing kept", File(pendingDir, "ready.properties").exists())
            context.assertGalleryCopies(1, name)
        }
    }

    @Test
    fun aStalePhotoIsDroppedWithoutACopyWhenNotAsked() {
        val name = shootAndLeave()
        ageKept(2)
        discardAfter("60", save = false)
        ActivityScenario.launch(CameraActivity::class.java).use { s ->
            s.waitForCamera()
            assertFalse(File(pendingDir, "ready.properties").exists())
            context.assertGalleryCopies(0, name)
        }
    }

    @Test
    fun aPhotoWithinTheLimitComesBackAsBefore() {
        val name = shootAndLeave()
        ageKept(2)
        discardAfter("720", save = true)
        ActivityScenario.launch(CameraActivity::class.java).use { s ->
            s.waitFor("the stamped photo back in review") { it.canSend() }
            context.assertGalleryCopies(0, name)
        }
    }

    @Test
    fun aStaleWaitingPhotoIsStampedWithoutLocationThenSaved() {
        // A photo that waited for a precise fix (D010), as the store keeps it, taken two hours ago.
        val taken = ZonedDateTime.now().minusHours(2).withNano(0)
        savedTimes += taken.toInstant().toEpochMilli()
        val bitmap = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(90, 140, 200)) }
        val jpeg = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }.toByteArray()
        pendingDir.mkdirs()
        File(pendingDir, "photo.jpg").writeBytes(jpeg)
        File(pendingDir, "meta.properties").writeText(
            "capture_time=${taken}\nuser_text=Discard test\nrotation=0\nlong_edge=1280\naspect=full\n",
        )
        discardAfter("60", save = true)
        ActivityScenario.launch(CameraActivity::class.java).use { s ->
            s.waitForCamera()
            assertFalse("the waiting entry is gone", File(pendingDir, "meta.properties").exists())
            val end = System.currentTimeMillis() + 5_000
            while (context.galleryCopiesTakenAt(savedTimes.single()) < 1 && System.currentTimeMillis() < end) Thread.sleep(100)
            assertEquals("one stamped copy, dated when it was taken", 1, context.galleryCopiesTakenAt(savedTimes.single()))
        }
    }

    /** The limit is checked on coming back too, not only on a cold start; this one really waits a minute. */
    @Test
    fun comingBackAfterTheLimitDropsThePhoto() {
        discardAfter("1", save = false)
        ActivityScenario.launch(CameraActivity::class.java).use { s ->
            s.shootAndStamp(pendingDir)
            savedNames += keptState(pendingDir).getProperty("file")!!
            s.moveToState(Lifecycle.State.CREATED)
            Thread.sleep(61_000)
            s.moveToState(Lifecycle.State.RESUMED)
            s.waitForCamera()
            assertFalse(File(pendingDir, "ready.properties").exists())
        }
    }
}
