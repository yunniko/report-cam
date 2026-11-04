package cz.julienika.reportcam

import android.Manifest
import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
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
import cz.julienika.reportcam.camera.CropGuideView
import cz.julienika.reportcam.helpers.canSend
import cz.julienika.reportcam.helpers.lastSend
import cz.julienika.reportcam.helpers.pendingDir
import cz.julienika.reportcam.helpers.view
import cz.julienika.reportcam.helpers.waitFor
import cz.julienika.reportcam.photo.AspectRatio
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
        // A stamped photo kept by another test would come back instead of the camera.
        context.pendingDir().deleteRecursively()
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

    @Test
    fun shootReviewAndShareWithCaption() {
        ActivityScenario.launch(CameraActivity::class.java).use { s ->
            s.waitFor("live preview") { it.view(R.id.shutter).isEnabled }
            s.onActivity { it.view(R.id.shutter).performClick() }
            s.waitFor("stamped review") { it.canSend() }
            s.onActivity {
                assertEquals(View.GONE, it.view(R.id.camera_controls).visibility)
                it.view(R.id.share_other).performClick()
            }
            intended(hasAction(Intent.ACTION_CHOOSER))
            val send = lastSend()
            assertEquals("image/jpeg", send.type)
            val caption = send.getStringExtra(Intent.EXTRA_TEXT)!!
            assertTrue(caption, caption.startsWith("Flow test\n"))
        }
    }

    @Test
    fun captionSwitchedOffSendsPhotoOnly() {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(Keys.MESSAGE, false).commit()
        ActivityScenario.launch(CameraActivity::class.java).use { s ->
            s.waitFor("live preview") { it.view(R.id.shutter).isEnabled }
            s.onActivity { it.view(R.id.shutter).performClick() }
            s.waitFor("stamped review") { it.canSend() }
            s.onActivity { it.view(R.id.share_other).performClick() }
            val send = lastSend()
            assertTrue(!send.hasExtra(Intent.EXTRA_TEXT))
        }
    }

    @Test
    fun squareShapeCropsThePhotoItself() {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(Keys.ASPECT, "1:1").commit()
        ActivityScenario.launch(CameraActivity::class.java).use { s ->
            s.waitFor("live preview") { it.view(R.id.shutter).isEnabled }
            s.onActivity {
                assertEquals(AspectRatio.SQUARE, (it.view(R.id.crop_guide) as CropGuideView).ratio)
                it.view(R.id.shutter).performClick()
            }
            s.waitFor("stamped review") { it.canSend() }
            s.onActivity { it.view(R.id.share_other).performClick() }
            val send = lastSend()
            @Suppress("DEPRECATION")
            val uri = send.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)!!
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, bounds) }
            assertTrue("decoded ${bounds.outWidth}x${bounds.outHeight}", bounds.outWidth > 0)
            assertEquals(bounds.outWidth, bounds.outHeight)
        }
    }
}
