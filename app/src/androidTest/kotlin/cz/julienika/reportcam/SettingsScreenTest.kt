package cz.julienika.reportcam

import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import cz.julienika.reportcam.settings.Keys
import cz.julienika.reportcam.settings.SettingsActivity
import cz.julienika.reportcam.stamp.SchemeId
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.regex.Pattern

/** The redesigned Settings controls (G-003 M3) store what they show, through the real screen. */
@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private lateinit var scenario: ActivityScenario<SettingsActivity>

    @Before
    fun setUp() {
        prefs.edit().clear().commit()
        scenario = ActivityScenario.launch(SettingsActivity::class.java)
        device.wait(Until.hasObject(By.text("Stamp")), TIMEOUT)
    }

    @After
    fun tearDown() {
        scenario.close()
        prefs.edit().clear().commit()
    }

    /** Scrolls the list until [selector] is on screen, then taps it. */
    private fun tap(selector: UiSelector) {
        UiScrollable(UiSelector().scrollable(true)).scrollIntoView(selector)
        device.findObject(selector).click()
        device.waitForIdle()
    }

    private fun waitUntil(
        what: String,
        condition: () -> Boolean,
    ) {
        val end = System.currentTimeMillis() + TIMEOUT
        while (System.currentTimeMillis() < end) {
            if (condition()) return
            Thread.sleep(100)
        }
        throw AssertionError("timed out waiting for $what")
    }

    @Test
    fun aSliderAndASegmentStoreTheirValues() {
        val slider = UiSelector().description("Precise means within")
        UiScrollable(UiSelector().scrollable(true)).scrollIntoView(slider)
        val bar = device.findObject(slider).bounds
        // Dragged to the far end: the last choice, 50 m.
        device.drag(bar.left + 10, bar.centerY(), bar.right + 40, bar.centerY(), 20)
        waitUntil("threshold stored") { prefs.getString(Keys.PRECISE_ACCURACY, null) == "50" }
        tap(UiSelector().text("Frame above"))
        waitUntil("placement stored") { prefs.getString(Keys.PLACEMENT, null) == "frame_top" }
    }

    @Test
    fun aFourthQuickMessengerIsRefused() {
        tap(UiSelector().description("Discord"))
        waitUntil("third stored") { prefs.getStringSet(Keys.QUICK_MESSENGERS, null)?.size == 3 }
        tap(UiSelector().description("Slack"))
        Thread.sleep(500)
        assertEquals(setOf("whatsapp", "telegram", "discord"), prefs.getStringSet(Keys.QUICK_MESSENGERS, null))
    }

    @Test
    fun theColourDialogStoresOnlyOnSave() {
        tap(UiSelector().text("Edit colours").instance(0))
        // #80D8FF is in the text palette only.
        device.wait(Until.findObject(By.desc("#80D8FF")), TIMEOUT).click()
        device.findObject(By.text(Pattern.compile("(?i)cancel"))).click()
        device.waitForIdle()
        assertNull(prefs.getString(Keys.schemeText(SchemeId.DARK), null))

        tap(UiSelector().text("Edit colours").instance(0))
        device.wait(Until.findObject(By.desc("#80D8FF")), TIMEOUT).click()
        device.findObject(By.text(Pattern.compile("(?i)save"))).click()
        waitUntil("colour stored") { prefs.getString(Keys.schemeText(SchemeId.DARK), null) == "#80D8FF" }
        assertNotNull(prefs.getString(Keys.schemeBackground(SchemeId.DARK), null))
    }

    private companion object {
        const val TIMEOUT = 10_000L
    }
}
