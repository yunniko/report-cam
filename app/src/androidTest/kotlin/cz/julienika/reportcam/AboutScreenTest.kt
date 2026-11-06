package cz.julienika.reportcam

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import cz.julienika.reportcam.settings.SettingsActivity
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.regex.Pattern

/** The About card (G-006 M2): the version, the two text dialogs and the links, through the real screen. */
@RunWith(AndroidJUnit4::class)
class AboutScreenTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private lateinit var scenario: ActivityScenario<SettingsActivity>

    @Before
    fun setUp() {
        Intents.init()
        // No browser on the test device need open: every link is answered here and checked by its data.
        intending(hasAction(Intent.ACTION_VIEW)).respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))
        scenario = ActivityScenario.launch(SettingsActivity::class.java)
        device.wait(Until.hasObject(By.text("Stamp")), TIMEOUT)
    }

    @After
    fun tearDown() {
        scenario.close()
        runCatching { Intents.release() }
    }

    private fun tapRow(title: String) {
        val row = UiSelector().text(title)
        UiScrollable(UiSelector().scrollable(true)).scrollIntoView(row)
        device.findObject(row).click()
        device.waitForIdle()
    }

    @Test
    fun theVersionRowShowsTheBuild() {
        UiScrollable(UiSelector().scrollable(true)).scrollIntoView(UiSelector().text("Version"))
        assertTrue(device.findObject(UiSelector().text("${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")).exists())
    }

    @Test
    fun thePrivacyPolicyOpensInTheAppAndOnline() {
        tapRow("Privacy policy")
        assertTrue(device.wait(Until.hasObject(By.textContains("no internet access of its own")), TIMEOUT))
        device.findObject(CLOSE).click()
        assertTrue(device.wait(Until.gone(By.textContains("no internet access of its own")), TIMEOUT))
        tapRow("Privacy policy")
        device.wait(Until.findObject(By.text(Pattern.compile("(?i)open online"))), TIMEOUT).click()
        intended(allOf(hasAction(Intent.ACTION_VIEW), hasData(context.getString(R.string.privacy_policy_url))))
    }

    @Test
    fun theLicencesListTheIconSetsAndTheTexts() {
        tapRow("Open-source licences")
        assertTrue(device.wait(Until.hasObject(By.textContains("Boxicons")), TIMEOUT))
        val text = device.findObject(By.textContains("Boxicons")).text
        assertTrue(text.contains("OpenSearch UI"))
        assertTrue(text.contains("Apache License"))
        assertTrue(text.contains("Aniket Suvarna"))
        device.findObject(CLOSE).click()
    }

    @Test
    fun theSourceRowOpensTheRepository() {
        tapRow("Source code")
        intended(allOf(hasAction(Intent.ACTION_VIEW), hasData("https://github.com/yunniko/report-cam")))
    }

    private companion object {
        const val TIMEOUT = 10_000L

        /** Dialog buttons render in capitals in the AppCompat theme. */
        val CLOSE: BySelector = By.text(Pattern.compile("(?i)close"))
    }
}
