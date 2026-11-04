package cz.julienika.reportcam

import android.Manifest
import android.app.Activity
import android.widget.EditText
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import cz.julienika.reportcam.helpers.canSend
import cz.julienika.reportcam.helpers.pendingDir
import cz.julienika.reportcam.helpers.shootAndStamp
import cz.julienika.reportcam.helpers.view
import cz.julienika.reportcam.settings.Keys
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The Settings for the review's Note and Message rows (G-005 M3, D031): quick editing and the on/off switches,
 * each turned off separately, and both off.
 */
@RunWith(AndroidJUnit4::class)
class ReviewTextSettingsTest {
    @get:Rule
    val permissions: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION)

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val pendingDir = context.pendingDir()

    private fun prefs(
        editing: Boolean,
        switches: Boolean,
        messageOn: Boolean = true,
    ) {
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .edit()
            .clear()
            .putString(Keys.STAMP_TEXT, "Site note")
            .putBoolean(Keys.SAVE_GALLERY, false)
            .putBoolean(Keys.TEXT_EDITING, editing)
            .putBoolean(Keys.TEXT_SWITCHES, switches)
            .putBoolean(Keys.MESSAGE, messageOn)
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

    private fun review(check: (Activity) -> Unit) {
        ActivityScenario.launch(CameraActivity::class.java).use { s ->
            s.shootAndStamp(pendingDir)
            s.onActivity {
                assertTrue(it.canSend())
                check(it)
            }
        }
    }

    @Test
    fun withEditingOffTheTextsShowButATapDoesNotOpenThem() {
        prefs(editing = false, switches = true)
        review {
            val note = it.field(R.id.note_text)
            assertTrue(it.view(R.id.note_row).isShown)
            assertEquals("Site note", note.text.toString())
            assertFalse("not editable", note.isFocusable)
            note.performClick()
            assertFalse("a tap opens nothing", note.hasFocus())
            assertTrue("the switches stay", it.view(R.id.note_switch).isShown)
            assertTrue(it.view(R.id.message_switch).isShown)
        }
    }

    @Test
    fun withSwitchesOffTheRowsHaveNoneAndAnOffMessageIsHidden() {
        prefs(editing = true, switches = false, messageOn = false)
        review {
            assertTrue(it.view(R.id.note_row).isShown)
            assertFalse("no note switch", it.view(R.id.note_switch).isShown)
            assertTrue("the note still edits", it.field(R.id.note_text).isFocusable)
            assertFalse("the message started off and has no switch to turn it on", it.view(R.id.message_row).isShown)
        }
    }

    @Test
    fun withBothOffBothRowsAreHidden() {
        prefs(editing = false, switches = false)
        review {
            assertFalse(it.view(R.id.note_row).isShown)
            assertFalse(it.view(R.id.message_row).isShown)
            assertTrue("the rest of the review stays", it.view(R.id.share_other).isShown)
        }
    }
}
