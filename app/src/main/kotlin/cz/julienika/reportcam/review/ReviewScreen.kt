package cz.julienika.reportcam.review

import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import cz.julienika.reportcam.R
import cz.julienika.reportcam.databinding.ActivityCameraBinding
import cz.julienika.reportcam.location.LocationStatus
import cz.julienika.reportcam.location.LocationTracker
import cz.julienika.reportcam.location.reading
import cz.julienika.reportcam.photo.ExifWriter
import cz.julienika.reportcam.photo.GallerySaver
import cz.julienika.reportcam.photo.PhotoDecoder
import cz.julienika.reportcam.photo.PhotoMetadata
import cz.julienika.reportcam.photo.PhotoPipeline
import cz.julienika.reportcam.photo.ProcessedPhoto
import cz.julienika.reportcam.settings.AppSettings
import cz.julienika.reportcam.share.ShareTargets
import cz.julienika.reportcam.share.Sharing
import cz.julienika.reportcam.stamp.StampData
import cz.julienika.reportcam.stamp.StampKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The review half of the camera screen. A photo arrives either ready to stamp, or waiting for a precise
 * fix (D008): then it shows unstamped with a live readout, and is stamped when the fix settles or the
 * user picks "use current". Nothing can be sent until the photo is stamped. The camera stays closed
 * throughout (D003); [onLeave] hands back to the live camera.
 */
class ReviewScreen(
    private val activity: ComponentActivity,
    private val ui: ActivityCameraBinding,
    private val tracker: LocationTracker,
    private val sharedDir: File,
    private val onLeave: () -> Unit,
) {
    private sealed interface State {
        data object Idle : State

        class Waiting(
            val base: Bitmap,
            val data: StampData,
            val settings: AppSettings,
        ) : State

        data object Stamping : State

        class Ready(
            val photo: ProcessedPhoto,
            val caption: String,
            val settings: AppSettings,
        ) : State
    }

    private var state: State = State.Idle

    /** Bumped on every new photo and on leaving, so a stamp that finishes for an abandoned photo is dropped. */
    private var generation = 0

    /** True once the photo was handed to a messenger: coming back to the app then means "next photo". */
    var sentSinceCapture = false
        private set

    val isActive: Boolean get() = state != State.Idle

    init {
        ui.retake.setOnClickListener { leave() }
        ui.useCurrent.setOnClickListener { useCurrent() }
        ui.shareWhatsapp.setOnClickListener { share(ShareTargets.WHATSAPP) }
        ui.shareTelegram.setOnClickListener { share(ShareTargets.TELEGRAM) }
        ui.shareOther.setOnClickListener { share(null) }
    }

    /** Stamp [base] now with [data] as it is. */
    fun stampNow(
        base: Bitmap,
        data: StampData,
        settings: AppSettings,
    ) {
        enter()
        stamp(base, data, settings)
    }

    /** Show [base] unstamped and wait for a precise fix; [data] gets its location when the wait ends. */
    fun waitForLocation(
        base: Bitmap,
        data: StampData,
        settings: AppSettings,
    ) {
        enter()
        state = State.Waiting(base, data, settings)
        // A bitmap of its own for the screen: the stamp step draws on [base] and may recycle it.
        val shown = PhotoDecoder.displayCopy(base, displayEdge())
        ui.reviewImage.setImageBitmap(if (shown === base) base.copy(Bitmap.Config.ARGB_8888, false) else shown)
        ui.pendingRow.isVisible = true
        ui.useCurrent.isVisible = true
        onLocation(tracker.status)
    }

    /** Every tracker update while the screen is up. */
    fun onLocation(status: LocationStatus) {
        val waiting = state as? State.Waiting ?: return
        val now = System.currentTimeMillis()
        if (PendingLocation.settled(status, now)) {
            stamp(waiting.base, waiting.data.with(StampKeys.LOCATION, status.reading()), waiting.settings)
            return
        }
        val r = PendingLocation.readout(status, now, waiting.settings.coordinateFormat, labels())
        ui.pendingStatus.text = activity.getString(R.string.pending_detecting, r.current)
        ui.useCurrent.setText(if (r.hasFix) R.string.use_current else R.string.use_without_location)
        // The user is waiting on this fix: keep the receiver on instead of resting (D008).
        if (!status.updating) tracker.ensureSearching()
    }

    private fun useCurrent() {
        val waiting = state as? State.Waiting ?: return
        stamp(waiting.base, waiting.data.with(StampKeys.LOCATION, tracker.status.reading()), waiting.settings)
    }

    private fun stamp(
        base: Bitmap,
        data: StampData,
        s: AppSettings,
    ) {
        state = State.Stamping
        val token = generation
        ui.pendingRow.isVisible = false
        ui.useCurrent.isVisible = false
        ui.busy.isVisible = true
        val displayEdge = displayEdge()
        activity.lifecycleScope.launch {
            val started = SystemClock.elapsedRealtime()
            val photo =
                try {
                    withContext(Dispatchers.Default) {
                        PhotoPipeline(sharedDir).finish(base, s.stampSpec, data, displayEdge).also {
                            ExifWriter.write(it.file, PhotoMetadata.of(s.stampSpec, data))
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "processing failed", e)
                    null
                }
            if (token != generation) return@launch // the user retook or left meanwhile
            ui.busy.isVisible = false
            if (photo == null) {
                toast(R.string.process_failed)
                leave()
                return@launch
            }
            Log.i(TAG, "photo ${photo.width}x${photo.height} stamped in ${SystemClock.elapsedRealtime() - started}ms")
            state = State.Ready(photo, PhotoMetadata.of(s.stampSpec, data).stampText, s)
            ui.reviewImage.setImageBitmap(photo.display)
            setShareVisible(true)
            if (s.saveToGallery) saveToGallery(photo, data)
        }
    }

    private fun share(targets: List<String>?) {
        val ready = state as? State.Ready ?: return
        val pkg = targets?.let { installed(it) }
        if (targets != null && pkg == null) return
        val caption = if (ready.settings.caption) ready.caption else null
        sentSinceCapture = true
        activity.startActivity(Sharing.sendIntent(activity, ready.photo.file, caption, pkg))
    }

    /** Back to the live camera, dropping the photo under review. */
    fun leave() {
        val wasActive = isActive
        generation++
        state = State.Idle
        ui.busy.isVisible = false
        ui.reviewImage.setImageDrawable(null)
        ui.reviewImage.isVisible = false
        ui.reviewControls.isVisible = false
        ui.cameraControls.isVisible = true
        ui.preview.isVisible = true
        ui.cropGuide.isVisible = true
        if (wasActive) onLeave()
    }

    private fun enter() {
        generation++
        sentSinceCapture = false
        ui.reviewImage.isVisible = true
        ui.reviewControls.isVisible = true
        ui.cameraControls.isVisible = false
        ui.preview.isVisible = false
        ui.cropGuide.isVisible = false
        ui.pendingRow.isVisible = false
        ui.useCurrent.isVisible = false
        setShareVisible(false)
    }

    private fun setShareVisible(on: Boolean) {
        ui.shareOther.isVisible = on
        ui.shareWhatsapp.isVisible = on && installed(ShareTargets.WHATSAPP) != null
        ui.shareTelegram.isVisible = on && installed(ShareTargets.TELEGRAM) != null
    }

    private fun saveToGallery(
        photo: ProcessedPhoto,
        data: StampData,
    ) {
        val takenAt = data[StampKeys.CAPTURE_TIME]?.toInstant()?.toEpochMilli() ?: System.currentTimeMillis()
        activity.lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) { GallerySaver.save(activity.applicationContext, photo.file, takenAt) }
            if (!ok) toast(R.string.gallery_failed)
        }
    }

    private fun installed(candidates: List<String>) = ShareTargets.firstInstalled(candidates) { Sharing.isInstalled(activity, it) }

    private fun displayEdge() = activity.resources.displayMetrics.let { maxOf(it.widthPixels, it.heightPixels) }

    private fun labels() =
        PendingLocation.Labels(
            noFixYet = activity.getString(R.string.pending_no_fix),
            secondsAgo = { activity.getString(R.string.pending_seconds_ago, it) },
            minutesAgo = { activity.getString(R.string.pending_minutes_ago, it) },
        )

    private fun toast(res: Int) = Toast.makeText(activity, res, Toast.LENGTH_LONG).show()

    private companion object {
        const val TAG = "ReportCam"
    }
}
