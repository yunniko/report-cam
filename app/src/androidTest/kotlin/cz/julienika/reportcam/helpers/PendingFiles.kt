package cz.julienika.reportcam.helpers

import android.content.Context
import java.io.File

/** Where the app keeps the photo under review (CameraActivity's PendingStore); cleared between tests. */
fun Context.pendingDir(): File = File(filesDir, "pending")
