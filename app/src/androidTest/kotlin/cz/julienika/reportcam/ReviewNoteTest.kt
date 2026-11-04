package cz.julienika.reportcam

import android.Manifest
import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Switch
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Properties

/**
 * The review's Note row (Owner, G-004 M1): finishing an edit or switching it redraws the photo, the message
 * follows the note until it is edited by hand, sending waits for the note being typed, and the row comes
 * back after a restart. Stamped text is read from the kept state (`data.user_text`), which drives the redraw.
 */
@RunWith(AndroidJUnit4::class)
class ReviewNoteTest {
    @get:Rule
    val permissions: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION)

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val pendingDir = context.pendingDir()

    @Before
    fun setUp() {
        pendingDir.deleteRecursively()
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .edit()
            .clear()
            .putString(Keys.STAMP_TEXT, "Site note")
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
    }

    private fun kept(): Properties = keptState(pendingDir)

    private fun Activity.note() = view(R.id.note_text) as EditText

    private fun Activity.message() = (view(R.id.message_text) as EditText).text.toString()

    /** Waits for the version stamped with [note] (null: none) to be the one in review; returns its file name. */
    private fun ActivityScenario<CameraActivity>.awaitStamped(note: String?): String {
        waitFor("stamped with $note") { kept().getProperty("data.user_text") == note && it.canSend() }
        return kept().getProperty("file")
    }

    @Test
    fun finishingANoteEditRedrawsThePhotoAndTheMessageFollows() {
        ActivityScenario.launch(CameraActivity::class.java).use { s ->
            s.shootAndStamp(pendingDir)
            val first = kept().getProperty("file")
            s.onActivity {
                assertEquals("Site note", it.note().text.toString())
                assertTrue(it.message().startsWith("Site note\n"))
                it.note().requestFocus()
                it.note().setText("Roof, north side")
                it.note().onEditorAction(EditorInfo.IME_ACTION_DONE)
            }
            val edited = s.awaitStamped("Roof, north side")
            assertNotEquals("a new version", first, edited)
            s.onActivity {
                assertFalse("Done ends the edit", it.note().hasFocus())
                assertTrue(it.message(), it.message().startsWith("Roof, north side\n"))
            }
        }
    }

    @Test
    fun switchingTheNoteOffHidesItAndDrawsThePhotoWithoutIt() {
        ActivityScenario.launch(CameraActivity::class.java).use { s ->
            s.shootAndStamp(pendingDir)
            val withNote = kept().getProperty("file")
            s.onActivity { (it.view(R.id.note_switch) as Switch).isChecked = false }
            s.awaitStamped(null)
            s.onActivity {
                assertFalse("only the name and switch while off", it.note().isShown)
                assertTrue("the switch stays", it.view(R.id.note_switch).isShown)
                assertFalse(it.message(), it.message().contains("Site note"))
                (it.view(R.id.note_switch) as Switch).isChecked = true
            }
            assertEquals("on again finds the first version", withNote, s.awaitStamped("Site note"))
        }
    }

    @Test
    fun aMessageEditedByHandNoLongerFollowsTheNote() {
        ActivityScenario.launch(CameraActivity::class.java).use { s ->
            s.shootAndStamp(pendingDir)
            s.onActivity {
                (it.view(R.id.message_text) as EditText).setText("My own words")
                it.note().setText("Changed")
                it.note().onEditorAction(EditorInfo.IME_ACTION_DONE)
            }
            s.awaitStamped("Changed")
            s.onActivity { assertEquals("My own words", it.message()) }
        }
    }

    @Test
    fun sendingWhileTheNoteIsTypedSendsThePhotoWithThatNote() {
        ActivityScenario.launch(CameraActivity::class.java).use { s ->
            s.shootAndStamp(pendingDir)
            val first = kept().getProperty("file")
            val before = Intents.getIntents().size
            s.onActivity {
                it.note().requestFocus()
                it.note().setText("Typed, not finished")
                it.view(R.id.share_other).performClick()
            }
            // Intents are read off the main thread, so this waits here instead of in waitFor.
            val end = System.currentTimeMillis() + 15_000
            while (Intents.getIntents().size == before && System.currentTimeMillis() < end) Thread.sleep(100)
            val send = lastSend()
            assertTrue(send.getStringExtra(Intent.EXTRA_TEXT)!!.startsWith("Typed, not finished\n"))
            @Suppress("DEPRECATION")
            val uri = send.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM)!!
            s.onActivity { assertFalse("the keyboard is done", it.note().hasFocus()) }
            assertNull("sent: nothing kept", runCatching { kept() }.getOrNull())
            assertFalse("the version with the note, not the first one", uri.toString().endsWith(first))
        }
    }

    @Test
    fun theNoteRowComesBackAfterARestart() {
        ActivityScenario.launch(CameraActivity::class.java).use { s ->
            s.shootAndStamp(pendingDir)
            s.onActivity {
                it.note().setText("Kept note")
                (it.view(R.id.note_switch) as Switch).isChecked = false
            }
            s.awaitStamped(null)
        }
        assertEquals("Kept note", kept().getProperty("note"))
        assertEquals("false", kept().getProperty("note_on"))
        ActivityScenario.launch(CameraActivity::class.java).use { s ->
            s.waitFor("back in review") { it.canSend() }
            s.onActivity {
                assertFalse((it.view(R.id.note_switch) as Switch).isChecked)
                assertFalse(it.note().isShown)
                (it.view(R.id.note_switch) as Switch).isChecked = true
            }
            s.awaitStamped("Kept note")
            s.onActivity { assertEquals("Kept note", it.note().text.toString()) }
        }
    }
}
