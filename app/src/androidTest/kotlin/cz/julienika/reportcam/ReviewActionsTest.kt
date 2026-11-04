package cz.julienika.reportcam

import android.Manifest
import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.graphics.BitmapFactory
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import cz.julienika.reportcam.helpers.canSend
import cz.julienika.reportcam.helpers.keptState
import cz.julienika.reportcam.helpers.lastSend
import cz.julienika.reportcam.helpers.pendingDir
import cz.julienika.reportcam.helpers.shootAndStamp
import cz.julienika.reportcam.helpers.view
import cz.julienika.reportcam.helpers.waitFor
import cz.julienika.reportcam.settings.Keys
import cz.julienika.reportcam.share.Sharing
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Properties

/**
 * The stamped-photo half of the review screen (G-003 M1): the editable message and its switch, the
 * one-gallery-copy rule, and the unsent photo kept across a restart; the dark/light redraw (G-003 M2), with
 * a version found again when its look comes back (G-004, D025). The note row is in [ReviewNoteTest].
 */
@RunWith(AndroidJUnit4::class)
class ReviewActionsTest {
    @get:Rule
    val permissions: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION)

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val pendingDir = context.pendingDir()
    private val savedNames = mutableSetOf<String>()

    @Before
    fun setUp() {
        pendingDir.deleteRecursively()
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .edit()
            .clear()
            .putString(Keys.STAMP_TEXT, "Review test")
            .putBoolean(Keys.SAVE_GALLERY, false)
            .commit()
        Intents.init()
        intending(hasAction(Intent.ACTION_CHOOSER)).respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))
    }

    @After
    fun tearDown() {
        runCatching { Intents.release() }
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
        pendingDir.deleteRecursively()
        // The app owns its gallery rows, so it may remove the ones these tests made.
        for (name in savedNames) {
            context.contentResolver.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "${MediaStore.Images.Media.DISPLAY_NAME}=?", arrayOf(name))
        }
    }

    private fun ActivityScenario<CameraActivity>.shootAndStamp() = shootAndStamp(pendingDir)

    private fun keptState(): Properties = keptState(pendingDir)

    private fun keptFileName(): String = keptState().getProperty("file")!!.also { savedNames += it }

    private fun galleryCopies(name: String): Int =
        context.contentResolver
            .query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Images.Media._ID),
                "${MediaStore.Images.Media.DISPLAY_NAME}=?",
                arrayOf(name),
                null,
            )!!
            .use { it.count }

    /** Waits for the gallery writes in flight, then checks no more arrive. */
    private fun assertCopies(
        expected: Int,
        name: String,
    ) {
        val end = System.currentTimeMillis() + 5_000
        while (galleryCopies(name) < expected && System.currentTimeMillis() < end) Thread.sleep(100)
        Thread.sleep(1_000)
        assertEquals("gallery copies of $name", expected, galleryCopies(name))
    }

    /** Needs the stand-in messengers (`stub-messenger/`) for WhatsApp and Telegram. */
    @Test
    fun changedQuickMessengersShowOnComingBackFromSettings() {
        assumeTrue(Sharing.isInstalled(context, "com.whatsapp") && Sharing.isInstalled(context, "org.telegram.messenger"))
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().putStringSet(Keys.QUICK_MESSENGERS, setOf("whatsapp", "telegram")).commit()
        ActivityScenario.launch(CameraActivity::class.java).use { s ->
            s.shootAndStamp()
            s.onActivity { assertTrue("two get their own row", it.view(R.id.quick_row).isShown) }
            // As if Settings were opened and closed while the photo is in review.
            s.moveToState(Lifecycle.State.CREATED)
            prefs.edit().putStringSet(Keys.QUICK_MESSENGERS, setOf("whatsapp")).commit()
            s.moveToState(Lifecycle.State.RESUMED)
            s.onActivity {
                assertFalse(it.view(R.id.quick_row).isShown)
                val single = it.view(R.id.quick_start)
                assertTrue("one sits beside Share", single.isShown)
                assertEquals("icon only, named for TalkBack", "WhatsApp", single.contentDescription)
            }
        }
    }

    @Test
    fun theEditedMessageIsWhatGetsSent() {
        ActivityScenario.launch(CameraActivity::class.java).use { s ->
            s.shootAndStamp()
            s.onActivity {
                val text = it.view(R.id.message_text) as EditText
                assertTrue("starts as the stamp text", text.text.startsWith("Review test\n"))
                assertTrue((it.view(R.id.message_switch) as Switch).isChecked)
                text.setText("Edited on site")
                it.view(R.id.share_other).performClick()
            }
            assertEquals("Edited on site", lastSend().getStringExtra(Intent.EXTRA_TEXT))
        }
    }

    @Test
    fun theSwitchStartsAtTheSettingAndOffSendsThePhotoOnly() {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(Keys.MESSAGE, false).commit()
        ActivityScenario.launch(CameraActivity::class.java).use { s ->
            s.shootAndStamp()
            s.onActivity {
                val switch = it.view(R.id.message_switch) as Switch
                assertFalse(switch.isChecked)
                assertFalse("only the name and switch while off", it.view(R.id.message_text).isShown)
                it.view(R.id.share_other).performClick()
            }
            assertFalse(lastSend().hasExtra(Intent.EXTRA_TEXT))
            s.onActivity {
                (it.view(R.id.message_switch) as Switch).isChecked = true
                it.view(R.id.share_other).performClick()
            }
            assertTrue(lastSend().getStringExtra(Intent.EXTRA_TEXT)!!.startsWith("Review test\n"))
        }
    }

    @Test
    fun saveThenSendMakesOneCopy() {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(Keys.SAVE_GALLERY, true).commit()
        ActivityScenario.launch(CameraActivity::class.java).use { s ->
            s.shootAndStamp()
            val name = keptFileName()
            assertEquals("nothing saved on stamping any more", 0, galleryCopies(name))
            s.onActivity { it.view(R.id.save_gallery).performClick() }
            s.waitFor("saved") { !it.view(R.id.save_gallery).isEnabled }
            s.onActivity {
                assertEquals(it.getString(R.string.saved_gallery), (it.view(R.id.save_gallery) as Button).text.toString())
                it.view(R.id.save_gallery).performClick() // disabled: no effect
                it.view(R.id.share_other).performClick()
            }
            assertCopies(1, name)
        }
    }

    @Test
    fun sendingTwiceWithTheSettingOnMakesOneCopy() {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(Keys.SAVE_GALLERY, true).commit()
        ActivityScenario.launch(CameraActivity::class.java).use { s ->
            s.shootAndStamp()
            val name = keptFileName()
            s.onActivity { it.view(R.id.share_other).performClick() }
            s.onActivity { it.view(R.id.share_other).performClick() }
            assertCopies(1, name)
        }
    }

    @Test
    fun sendingWithTheSettingOffMakesNoCopy() {
        ActivityScenario.launch(CameraActivity::class.java).use { s ->
            s.shootAndStamp()
            val name = keptFileName()
            s.onActivity { it.view(R.id.share_other).performClick() }
            assertCopies(0, name)
        }
    }

    @Test
    fun retakeWithoutSendingMakesNoCopyAndKeepsNothing() {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(Keys.SAVE_GALLERY, true).commit()
        ActivityScenario.launch(CameraActivity::class.java).use { s ->
            s.shootAndStamp()
            val name = keptFileName()
            s.onActivity { it.view(R.id.retake).performClick() }
            s.waitFor("live preview again") { it.view(R.id.shutter).isEnabled }
            assertCopies(0, name)
            assertFalse(File(pendingDir, "ready.properties").exists())
        }
    }

    @Test
    fun anUnsentPhotoComesBackWithItsTextsSwitchesAndSavedFlag() {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(Keys.SAVE_GALLERY, true).commit()
        var name = ""
        ActivityScenario.launch(CameraActivity::class.java).use { s ->
            s.shootAndStamp()
            name = keptFileName()
            s.onActivity {
                (it.view(R.id.message_text) as EditText).setText("Kept across a restart")
                (it.view(R.id.message_switch) as Switch).isChecked = false
                it.view(R.id.save_gallery).performClick()
            }
            s.waitFor("saved") { !it.view(R.id.save_gallery).isEnabled }
            assertCopies(1, name)
        }
        // The activity is gone with everything it held; only the disk copy remains (D015).
        assertEquals("Kept across a restart", keptState().getProperty("caption"))
        ActivityScenario.launch(CameraActivity::class.java).use { s ->
            s.waitFor("the stamped photo back in review") { it.canSend() }
            s.onActivity {
                assertEquals("the camera stays closed", View.GONE, it.view(R.id.camera_controls).visibility)
                assertEquals("Kept across a restart", (it.view(R.id.message_text) as EditText).text.toString())
                assertFalse((it.view(R.id.message_switch) as Switch).isChecked)
                assertFalse("still saved", it.view(R.id.save_gallery).isEnabled)
                (it.view(R.id.message_switch) as Switch).isChecked = true
                it.view(R.id.share_other).performClick()
            }
            val send = lastSend()
            assertEquals("Kept across a restart", send.getStringExtra(Intent.EXTRA_TEXT))
            @Suppress("DEPRECATION")
            val uri = send.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM)!!
            assertTrue("the same file is sent", uri.toString().endsWith(name))
            assertTrue(context.contentResolver.openInputStream(uri)!!.use { it.readBytes().isNotEmpty() })
            assertCopies(1, name)
            assertFalse("sent: nothing to bring back", File(pendingDir, "ready.properties").exists())
        }
    }

    private fun stampedBytes() = File(pendingDir, "stamped.jpg").readBytes()

    /** Mean luminance of a downsampled decode: the same photo with a lighter stamp box reads brighter. */
    private fun brightness(jpeg: ByteArray): Double {
        val b = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size, BitmapFactory.Options().apply { inSampleSize = 8 })
        var sum = 0.0
        for (y in 0 until b.height) {
            for (x in 0 until b.width) {
                val c = b.getPixel(x, y)
                sum += 0.299 * (c shr 16 and 0xFF) + 0.587 * (c shr 8 and 0xFF) + 0.114 * (c and 0xFF)
            }
        }
        return sum / (b.width * b.height)
    }

    /** Taps a scheme button and waits for that scheme's version; returns its file name. */
    private fun ActivityScenario<CameraActivity>.restampTo(
        button: Int,
        scheme: String,
    ): String {
        val before = keptState().getProperty("file")
        onActivity { it.view(button).performClick() }
        waitFor("redrawn $scheme") {
            keptState().let { k -> k.getProperty("scheme") == scheme && k.getProperty("file") != before } && it.canSend()
        }
        return keptState().getProperty("file")
    }

    @Test
    fun aRestampIsANewUnsavedVersionAndSaveAddsOnlyThatOne() {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(Keys.SAVE_GALLERY, true).commit()
        ActivityScenario.launch(CameraActivity::class.java).use { s ->
            s.shootAndStamp()
            val dark = keptFileName()
            assertEquals("dark", keptState().getProperty("scheme"))
            val darkBytes = stampedBytes()
            s.onActivity { it.view(R.id.save_gallery).performClick() }
            s.waitFor("saved") { !it.view(R.id.save_gallery).isEnabled }
            assertCopies(1, dark)

            val light = s.restampTo(R.id.scheme_light, "light").also { savedNames += it }
            assertEquals("light", keptState().getProperty("scheme"))
            assertFalse("not among the saved versions", keptState().getProperty("saved_files").contains(light))
            assertTrue("a lighter box", brightness(stampedBytes()) > brightness(darkBytes) + 1.0)
            s.onActivity {
                assertTrue("the new version is not saved yet", it.view(R.id.save_gallery).isEnabled)
                it.view(R.id.save_gallery).performClick()
            }
            s.waitFor("saved again") { !it.view(R.id.save_gallery).isEnabled }
            assertCopies(1, light)
            assertCopies(1, dark)
            s.onActivity { it.view(R.id.share_other).performClick() }
            @Suppress("DEPRECATION")
            val uri = lastSend().getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM)!!
            assertTrue("the restamped file is sent", uri.toString().endsWith(light))
        }
    }

    @Test
    fun aRestoredPhotoKeepsItsSchemeAndCanBeRestamped() {
        var firstDark = ""
        var firstLight = ""
        ActivityScenario.launch(CameraActivity::class.java).use { s ->
            s.shootAndStamp()
            firstDark = keptState().getProperty("file")
            firstLight = s.restampTo(R.id.scheme_light, "light")
        }
        ActivityScenario.launch(CameraActivity::class.java).use { s ->
            s.waitFor("the stamped photo back in review") { it.canSend() }
            assertEquals("light", keptState().getProperty("scheme"))
            val lightBytes = stampedBytes()
            assertEquals("back to dark finds the first version", firstDark, s.restampTo(R.id.scheme_dark, "dark"))
            assertTrue("a darker box", brightness(stampedBytes()) < brightness(lightBytes) - 1.0)
            assertEquals("and light again the second", firstLight, s.restampTo(R.id.scheme_light, "light"))
        }
    }
}
