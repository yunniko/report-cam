package cz.julienika.reportcam.helpers

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.intent.Intents
import cz.julienika.reportcam.CameraActivity
import cz.julienika.reportcam.R
import java.io.File
import java.util.Properties

/** Shoots with the live camera and waits until the stamped photo can be sent and is kept on disk. */
fun ActivityScenario<CameraActivity>.shootAndStamp(pendingDir: File) {
    waitFor("live preview") { it.view(R.id.shutter).isEnabled }
    onActivity { it.view(R.id.shutter).performClick() }
    waitFor("stamped review") { it.canSend() }
    waitFor("stamped copy kept on disk") { File(pendingDir, "ready.properties").isFile }
}

/** The state kept for the stamped photo under review (PendingStore's ready.properties). */
fun keptState(pendingDir: File): Properties = File(pendingDir, "ready.properties").inputStream().use { Properties().apply { load(it) } }

/** The ACTION_SEND inside the last chooser the app started; needs [Intents.init]. */
fun lastSend(): Intent {
    val chooser = Intents.getIntents().last { it.action == Intent.ACTION_CHOOSER }
    @Suppress("DEPRECATION")
    return chooser.getParcelableExtra(Intent.EXTRA_INTENT)!!
}
