package cz.julienika.reportcam

import android.Manifest
import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.view.View
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import cz.julienika.reportcam.settings.Keys
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The whole hot path on the real camera stack: launch, shutter, stamped review, share with caption. */
@RunWith(AndroidJUnit4::class)
class CaptureFlowTest {
    @get:Rule
    val permissions: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION)

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .edit()
            .clear()
            .putString(Keys.STAMP_TEXT, "Flow test")
            .putBoolean(Keys.SAVE_GALLERY, false)
            .commit()
        Intents.init()
        // Answer the chooser at once, so the test never leaves the app.
        intending(hasAction(Intent.ACTION_CHOOSER)).respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))
    }

    @After
    fun tearDown() {
        Intents.release()
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
    }

    private fun ActivityScenario<CameraActivity>.waitFor(
        what: String,
        timeoutMs: Long = 15_000,
        condition: (CameraActivity) -> Boolean,
    ) {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            var ok = false
            onActivity { ok = condition(it) }
            if (ok) return
            Thread.sleep(100)
        }
        throw AssertionError("timed out waiting for $what")
    }

    private fun CameraActivity.view(id: Int): View = findViewById(id)

    @Test
    fun shootReviewAndShareWithCaption() {
        ActivityScenario.launch(CameraActivity::class.java).use { s ->
            s.waitFor("live preview") { it.view(R.id.shutter).isEnabled }
            s.onActivity { it.view(R.id.shutter).performClick() }
            s.waitFor("review") { it.view(R.id.review_controls).visibility == View.VISIBLE }
            s.onActivity {
                assertEquals(View.GONE, it.view(R.id.camera_controls).visibility)
                it.view(R.id.share_other).performClick()
            }
            intended(hasAction(Intent.ACTION_CHOOSER))
            val chooser = Intents.getIntents().last { it.action == Intent.ACTION_CHOOSER }
            @Suppress("DEPRECATION")
            val send = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
            assertEquals("image/jpeg", send.type)
            val caption = send.getStringExtra(Intent.EXTRA_TEXT)!!
            assertTrue(caption, caption.startsWith("Flow test\n"))
        }
    }

    @Test
    fun captionSwitchedOffSendsPhotoOnly() {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(Keys.CAPTION, false).commit()
        ActivityScenario.launch(CameraActivity::class.java).use { s ->
            s.waitFor("live preview") { it.view(R.id.shutter).isEnabled }
            s.onActivity { it.view(R.id.shutter).performClick() }
            s.waitFor("review") { it.view(R.id.review_controls).visibility == View.VISIBLE }
            s.onActivity { it.view(R.id.share_other).performClick() }
            val chooser = Intents.getIntents().last { it.action == Intent.ACTION_CHOOSER }
            @Suppress("DEPRECATION")
            val send = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
            assertTrue(!send.hasExtra(Intent.EXTRA_TEXT))
        }
    }
}
