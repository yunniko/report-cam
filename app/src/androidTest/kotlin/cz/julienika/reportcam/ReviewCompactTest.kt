package cz.julienika.reportcam

import android.Manifest
import android.app.Activity
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import cz.julienika.reportcam.helpers.canSend
import cz.julienika.reportcam.helpers.keptState
import cz.julienika.reportcam.helpers.pendingDir
import cz.julienika.reportcam.helpers.shootAndStamp
import cz.julienika.reportcam.helpers.view
import cz.julienika.reportcam.helpers.waitFor
import cz.julienika.reportcam.settings.Keys
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * "Compact text fields" (Owner, G-004 M2): the review's note and message read as plain text until tapped, are
 * edited after the tap, and read as plain text again once the edit ends. Off by default.
 */
@RunWith(AndroidJUnit4::class)
class ReviewCompactTest {
    @get:Rule
    val permissions: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION)

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val pendingDir = context.pendingDir()

    private fun prefs(compact: Boolean?) {
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .edit()
            .clear()
            .putString(Keys.STAMP_TEXT, "Site note")
            .putBoolean(Keys.SAVE_GALLERY, false)
            .apply { if (compact != null) putBoolean(Keys.COMPACT_TEXTS, compact) }
            .commit()
    }

    @Before
    fun setUp() {
        pendingDir.deleteRecursively()
    }

    @After
    fun tearDown() {
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
        pendingDir.deleteRecursively()
    }

    private fun Activity.field(id: Int) = view(id) as EditText

    private fun EditText.isPlain() = !isFocusable && background == null

    @Test
    fun compactFieldsReadAsTextUntilTappedAndAgainOnceTheEditEnds() {
        prefs(compact = true)
        ActivityScenario.launch(CameraActivity::class.java).use { s ->
            s.shootAndStamp(pendingDir)
            s.onActivity {
                val note = it.field(R.id.note_text)
                assertTrue("plain text before a tap", note.isPlain())
                assertTrue(it.field(R.id.message_text).isPlain())
                note.performClick()
                assertTrue("a tap starts the edit", note.hasFocus())
                assertNotNull("with its underline", note.background)
                note.setText("Compact edit")
                note.onEditorAction(EditorInfo.IME_ACTION_DONE)
                assertTrue("plain again after Done", note.isPlain())
            }
            s.waitFor("stamped with the edit") { keptState(pendingDir).getProperty("data.user_text") == "Compact edit" && it.canSend() }
            s.onActivity {
                val message = it.field(R.id.message_text)
                message.performClick()
                assertTrue(message.hasFocus())
                it.view(R.id.review_controls).requestFocus() // what a tap beside the fields does
                assertTrue("plain again when the focus leaves", message.isPlain())
                assertTrue("the message followed the note", message.text.startsWith("Compact edit\n"))
            }
        }
    }

    @Test
    fun offByDefaultTheFieldsAreEditableAtOnce() {
        prefs(compact = null)
        ActivityScenario.launch(CameraActivity::class.java).use { s ->
            s.shootAndStamp(pendingDir)
            s.onActivity {
                val note = it.field(R.id.note_text)
                assertTrue(note.isFocusable)
                assertNotNull(note.background)
                assertTrue(note.requestFocus())
            }
        }
    }

    @Test
    fun turningItOnAppliesWhenThePhotoComesBackInReview() {
        prefs(compact = false)
        ActivityScenario.launch(CameraActivity::class.java).use { s ->
            s.shootAndStamp(pendingDir)
            s.onActivity { assertFalse(it.field(R.id.note_text).isPlain()) }
            prefs(compact = true)
            s.recreate()
            s.waitFor("back in review") { it.canSend() }
            s.onActivity {
                assertTrue(it.field(R.id.note_text).isPlain())
                assertEquals("Site note", it.field(R.id.note_text).text.toString())
                assertNull(it.field(R.id.message_text).background)
            }
        }
    }
}
