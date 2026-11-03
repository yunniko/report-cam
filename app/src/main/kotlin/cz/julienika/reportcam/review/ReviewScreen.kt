package cz.julienika.reportcam.review

import android.content.ActivityNotFoundException
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import cz.julienika.reportcam.R
import cz.julienika.reportcam.camera.CapturedJpeg
import cz.julienika.reportcam.databinding.ActivityCameraBinding
import cz.julienika.reportcam.location.LocationStatus
import cz.julienika.reportcam.location.LocationTracker
import cz.julienika.reportcam.location.PlaceLookup
import cz.julienika.reportcam.photo.AspectRatio
import cz.julienika.reportcam.photo.ExifWriter
import cz.julienika.reportcam.photo.GallerySaver
import cz.julienika.reportcam.photo.PhotoDecoder
import cz.julienika.reportcam.photo.PhotoMetadata
import cz.julienika.reportcam.photo.PhotoPipeline
import cz.julienika.reportcam.photo.ProcessedPhoto
import cz.julienika.reportcam.settings.AppSettings
import cz.julienika.reportcam.share.Messenger
import cz.julienika.reportcam.share.icon
import cz.julienika.reportcam.share.Sharing
import cz.julienika.reportcam.stamp.LocationReading
import cz.julienika.reportcam.stamp.SchemeId
import cz.julienika.reportcam.stamp.StampData
import cz.julienika.reportcam.stamp.StampKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.ZonedDateTime

/** The camera JPEG a photo is drawn from, and the shape and size it was taken at: enough to draw it again. */
class PhotoSource(
    val jpeg: CapturedJpeg,
    val longEdge: Int,
    val aspect: AspectRatio,
)

/**
 * The review half of the camera screen. A photo arrives either ready to stamp, or waiting for a precise
 * fix (D008): then it shows unstamped with a live readout, and is stamped when the fix settles or the
 * user picks "use current". Nothing can be sent until the photo is stamped. The photo under review,
 * waiting or stamped and unsent, is also kept in [store] and comes back after a restart (D010, D015).
 * The camera stays closed throughout (D003); [onLeave] hands back to the live camera.
 * [storageAccess] runs its block once a gallery write is allowed (Android 9 and older need a permission).
 */
class ReviewScreen(
    private val activity: ComponentActivity,
    private val ui: ActivityCameraBinding,
    private val tracker: LocationTracker,
    private val sharedDir: File,
    private val store: PendingStore,
    private val places: PlaceLookup,
    private val storageAccess: (() -> Unit) -> Unit,
    private val onLeave: () -> Unit,
) {
    private sealed interface State {
        data object Idle : State

        /** A waiting photo from an earlier run is being read back from the store. */
        data object Restoring : State

        class Waiting(
            val source: PhotoSource,
            val base: Bitmap,
            val data: StampData,
            val settings: AppSettings,
        ) : State

        data object Stamping : State

        class Ready(
            val photo: ProcessedPhoto,
            /** The settings this version was stamped with; its scheme is the one the switch shows. */
            val settings: AppSettings,
            /** This version is in the gallery: neither Save nor sending adds another copy (Owner, 2026-11-03). */
            var saved: Boolean,
            val source: PhotoSource,
            val facts: StampFacts,
        ) : State {
            val captureTime: ZonedDateTime get() = facts.captureTime
        }
    }

    private var state: State = State.Idle

    /**
     * Bumped on every new photo and on leaving, so a stamp that finishes for an abandoned photo is dropped.
     * Read by the store's guards off the main thread.
     */
    @Volatile
    private var generation = 0

    /** True once the photo was handed to a messenger: coming back to the app then means "next photo". */
    var sentSinceCapture = false
        private set

    val isActive: Boolean get() = state != State.Idle

    init {
        ui.retake.setOnClickListener { leave() }
        ui.useCurrent.setOnClickListener { useCurrent() }
        ui.shareOther.setOnClickListener { share(null) }
        ui.saveGallery.setOnClickListener {
            val ready = state as? State.Ready ?: return@setOnClickListener
            storageAccess { if (state === ready) saveToGallery(ready) }
        }
        ui.captionSwitch.setOnCheckedChangeListener { _, on -> ui.captionText.isEnabled = on }
        ui.schemeDark.setOnClickListener { restamp(SchemeId.DARK) }
        ui.schemeLight.setOnClickListener { restamp(SchemeId.LIGHT) }
    }

    /** Stamp [base], decoded from [source], now with [data] as it is. */
    fun stampNow(
        source: PhotoSource,
        base: Bitmap,
        data: StampData,
        settings: AppSettings,
    ) {
        enter(settings)
        stamp(source, base, data, settings)
    }

    /** Show [base] unstamped and wait for a precise fix; [data] gets its location when the wait ends. */
    fun waitForLocation(
        source: PhotoSource,
        base: Bitmap,
        data: StampData,
        settings: AppSettings,
    ) {
        enter(settings)
        state = State.Waiting(source, base, data, settings)
        // A bitmap of its own for the screen: the stamp step draws on [base] and may recycle it.
        val shown = PhotoDecoder.displayCopy(base, displayEdge())
        ui.reviewImage.setImageBitmap(if (shown === base) base.copy(Bitmap.Config.ARGB_8888, false) else shown)
        ui.pendingRow.isVisible = true
        ui.useCurrent.isVisible = true
        onLocation(tracker.status)
    }

    /** The place name of the fix being stamped, and of no other (D011); unchanged when off or not found. */
    private suspend fun withPlace(
        data: StampData,
        s: AppSettings,
    ): StampData {
        if (s.placeFormat == null) return data
        val fix = (data[StampKeys.LOCATION] as? LocationReading.Fix)?.fix ?: return data
        val started = SystemClock.elapsedRealtime()
        val place = places.lookup(fix)
        Log.i(TAG, "place ${if (place == null) "not found" else "found"} in ${SystemClock.elapsedRealtime() - started}ms")
        return data.with(StampKeys.PLACE, place)
    }

    /** True when a photo from an earlier run is still on disk; [restore] brings it back. */
    val hasSaved: Boolean get() = store.exists()

    /**
     * Reads the kept photo back: a stamped one returns to review as it was left, a waiting one resumes its
     * wait. A broken entry is dropped.
     */
    fun restore(settings: AppSettings) {
        enter(settings)
        state = State.Restoring
        ui.busy.isVisible = true
        val token = generation
        val displayEdge = displayEdge()
        activity.lifecycleScope.launch {
            val stamped =
                withContext(Dispatchers.IO) {
                    try {
                        store.loadReady()?.let { kept -> restoreStamped(kept.stamped, kept.meta, displayEdge) to kept }
                    } catch (e: Exception) {
                        Log.e(TAG, "restoring the stamped photo failed", e)
                        store.clear()
                        null
                    }
                }
            if (token != generation) return@launch // the user left meanwhile
            if (stamped != null) {
                ui.busy.isVisible = false
                val (photo, kept) = stamped
                val m = kept.meta
                Log.i(TAG, "restored a stamped photo, taken ${m.captureTime}")
                val source = PhotoSource(kept.source, m.outputLongEdge, m.aspect)
                showReady(State.Ready(photo, settings.withScheme(m.scheme), m.saved, source, m.facts), m.caption, m.captionOn)
                return@launch
            }
            val loaded =
                withContext(Dispatchers.IO) {
                    try {
                        store.load()?.let { (jpeg, m) ->
                            val source = PhotoSource(jpeg, m.outputLongEdge, m.aspect)
                            Triple(source, PhotoPipeline(sharedDir).decode(jpeg, m.outputLongEdge, m.aspect), m)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "restoring the waiting photo failed", e)
                        null
                    }
                }
            if (token != generation) return@launch // the user left meanwhile
            ui.busy.isVisible = false
            if (loaded == null) {
                leave()
                return@launch
            }
            val (source, base, m) = loaded
            val data =
                StampData.EMPTY
                    .with(StampKeys.CAPTURE_TIME, m.captureTime)
                    .with(StampKeys.USER_TEXT, m.userText)
            Log.i(TAG, "restored a photo waiting for location, taken ${m.captureTime}")
            if (settings.showLocation) waitForLocation(source, base, data, settings) else stampNow(source, base, data, settings)
        }
    }

    /** Every tracker update while the screen is up. */
    fun onLocation(status: LocationStatus) {
        val waiting = state as? State.Waiting ?: return
        val now = System.currentTimeMillis()
        if (PendingLocation.settled(status, now, waiting.settings.preciseWithinM)) {
            stamp(waiting.source, waiting.base, waiting.data.with(StampKeys.LOCATION, PendingLocation.current(status)), waiting.settings)
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
        stamp(waiting.source, waiting.base, waiting.data.with(StampKeys.LOCATION, PendingLocation.current(tracker.status)), waiting.settings)
    }

    private fun stamp(
        source: PhotoSource,
        base: Bitmap,
        found: StampData,
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
            val data = withPlace(found, s)
            if (token != generation) return@launch // the user retook or left during the lookup
            val photo =
                try {
                    withContext(Dispatchers.Default) {
                        PhotoPipeline(sharedDir).finish(base, s.stampSpec, data, displayEdge).also {
                            ExifWriter.write(it.file, PhotoMetadata.of(s.stampSpec, data, s.preciseWithinM))
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
                leave() // drops the waiting copy too: it would fail the same way on restore
                return@launch
            }
            Log.i(TAG, "photo ${photo.width}x${photo.height} stamped in ${SystemClock.elapsedRealtime() - started}ms")
            val caption = PhotoMetadata.of(s.stampSpec, data, s.preciseWithinM).stampText
            val ready = State.Ready(photo, s, saved = false, source, StampFacts.of(data))
            // The stamped copy replaces the waiting one on disk before anything can be sent (D015), and
            // not at all if the user left meanwhile.
            val meta = readyMeta(ready, caption, s.caption)
            withContext(Dispatchers.IO) {
                try {
                    store.saveReady(photo.file, source.jpeg, meta) { token == generation }
                } catch (e: Exception) {
                    Log.e(TAG, "could not keep the stamped photo on disk", e)
                }
            }
            if (token != generation) return@launch
            showReady(ready, caption, s.caption)
        }
    }

    private fun showReady(
        ready: State.Ready,
        caption: String,
        captionOn: Boolean,
    ) {
        state = ready
        ui.pendingRow.isVisible = false
        ui.useCurrent.isVisible = false
        ui.reviewImage.setImageBitmap(ready.photo.display)
        ui.captionText.setText(caption)
        ui.captionSwitch.isChecked = captionOn
        ui.captionText.isEnabled = captionOn
        showSaved(ready.saved)
        showScheme(ready.settings)
        setShareVisible(true)
    }

    /**
     * Draws the photo under review again with scheme [id], caption and edits kept. The new version counts
     * as unsaved, so Save adds one copy of it and the old copy stays (Owner, G-003; D017). It gets its own
     * file name, so a version already handed to a messenger is never overwritten.
     */
    private fun restamp(id: SchemeId) {
        val ready = state as? State.Ready ?: return
        if (ready.settings.scheme == id) return
        val s = ready.settings.withScheme(id)
        val caption = ui.captionText.text.toString()
        val captionOn = ui.captionSwitch.isChecked
        state = State.Stamping
        val token = generation
        ui.busy.isVisible = true
        val displayEdge = displayEdge()
        activity.lifecycleScope.launch {
            val data = ready.facts.toStampData()
            val photo =
                try {
                    withContext(Dispatchers.Default) {
                        val pipeline = PhotoPipeline(sharedDir)
                        val base = pipeline.decode(ready.source.jpeg, ready.source.longEdge, ready.source.aspect)
                        pipeline.finish(base, s.stampSpec, data, displayEdge, nameSuffix = "_${id.id}").also {
                            ExifWriter.write(it.file, PhotoMetadata.of(s.stampSpec, data, s.preciseWithinM))
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "restamping failed", e)
                    null
                }
            if (token != generation) return@launch
            ui.busy.isVisible = false
            if (photo == null) {
                toast(R.string.process_failed)
                showReady(ready, caption, captionOn)
                return@launch
            }
            val next = State.Ready(photo, s, saved = false, ready.source, ready.facts)
            val meta = readyMeta(next, caption, captionOn)
            withContext(Dispatchers.IO) {
                try {
                    store.replaceStamped(photo.file, meta) { token == generation }
                } catch (e: Exception) {
                    Log.e(TAG, "could not keep the restamped photo on disk", e)
                }
            }
            if (token != generation) return@launch
            Log.i(TAG, "restamped with the ${id.id} scheme")
            showReady(next, caption, captionOn)
        }
    }

    /** Each scheme button in its own colours; the one in use is outlined in the accent colour. */
    private fun showScheme(s: AppSettings) {
        val density = activity.resources.displayMetrics.density
        for ((button, id) in listOf(ui.schemeDark to SchemeId.DARK, ui.schemeLight to SchemeId.LIGHT)) {
            val scheme = s.schemes.getValue(id)
            val chosen = id == s.scheme
            button.background =
                GradientDrawable().apply {
                    cornerRadius = SCHEME_CORNER_DP * density
                    setColor(scheme.background)
                    setStroke(((if (chosen) 3 else 1) * density).toInt(), if (chosen) activity.getColor(R.color.accent) else GREY)
                }
            button.setTextColor(scheme.text)
            button.isSelected = chosen
        }
    }

    /** The shared copy is written back from the kept one, so the restored file is the one a messenger gets. */
    private fun restoreStamped(
        kept: File,
        m: ReadyMeta,
        displayEdge: Int,
    ): ProcessedPhoto {
        val shared = File(sharedDir, m.fileName)
        sharedDir.mkdirs()
        kept.copyTo(shared, overwrite = true)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(shared.path, bounds)
        check(bounds.outWidth > 0) { "kept photo does not decode" }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= displayEdge) sample *= 2
        val decoded =
            BitmapFactory.decodeFile(shared.path, BitmapFactory.Options().apply { inSampleSize = sample })
                ?: error("kept photo does not decode")
        return ProcessedPhoto(shared, bounds.outWidth, bounds.outHeight, PhotoDecoder.displayCopy(decoded, displayEdge))
    }

    private fun readyMeta(
        ready: State.Ready,
        caption: String = ui.captionText.text.toString(),
        captionOn: Boolean = ui.captionSwitch.isChecked,
    ) = ReadyMeta(
        fileName = ready.photo.file.name,
        caption = caption,
        captionOn = captionOn,
        saved = ready.saved,
        scheme = ready.settings.scheme,
        rotationDegrees = ready.source.jpeg.rotationDegrees,
        outputLongEdge = ready.source.longEdge,
        aspect = ready.source.aspect,
        facts = ready.facts,
    )

    /**
     * Writes the caption, switch and saved flag of the photo under review to disk. Called from the
     * activity's onStop, on the main thread: a small file, and done before a restart could read it.
     */
    fun persist() {
        val ready = state as? State.Ready ?: return
        val token = generation
        try {
            store.updateReady(readyMeta(ready)) { token == generation }
        } catch (e: Exception) {
            Log.e(TAG, "could not keep the caption on disk", e)
        }
    }

    private fun share(messenger: Messenger?) {
        val ready = state as? State.Ready ?: return
        val pkg = messenger?.let { installed(it) ?: return }
        val caption = if (ui.captionSwitch.isChecked) ui.captionText.text.toString().trim().ifEmpty { null } else null
        val intent = Sharing.sendIntent(activity, ready.photo.file, caption, pkg)
        try {
            activity.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "$pkg is gone", e)
            setShareVisible(true)
            return
        }
        // "Save to gallery when sent", at most one copy per photo (Owner, 2026-11-03).
        if (ready.settings.saveToGallery) saveToGallery(ready)
        sentSinceCapture = true
        // Sent: coming back starts the next photo, so a restart must not bring this one back either.
        store.clear()
    }

    /** Back to the live camera, dropping the photo under review. */
    fun leave() {
        val wasActive = isActive
        generation++
        store.clear() // Retake or back: the user dropped this photo
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

    private fun enter(settings: AppSettings) {
        generation++
        val systemDark =
            activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        ReviewPalette
            .of(settings.appTheme.isDark(systemDark))
            .paint(ui.reviewControls, activity.resources.displayMetrics.density, setOf(ui.schemeDark, ui.schemeLight))
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

    /** The stamped-photo rows, with the quick messengers placed by [ShareButtons]. */
    private fun setShareVisible(on: Boolean) {
        val ready = state as? State.Ready
        val visible = on && ready != null
        ui.captionRow.isVisible = visible
        ui.schemeRow.isVisible = visible
        ui.shareRow.isVisible = visible
        ui.saveGallery.isVisible = visible
        if (ready == null || !visible) {
            ui.quickRow.isVisible = false
            return
        }
        val rows = ShareButtons.arrange(ready.settings.quickMessengers, ready.settings.quickOnStart) { installed(it) != null }
        ui.quickRow.isVisible = rows.ownRow.isNotEmpty()
        listOf(ui.quick1, ui.quick2, ui.quick3).forEachIndexed { i, b -> bind(b, rows.ownRow.getOrNull(i)) }
        bind(ui.quickStart, rows.beside?.takeIf { rows.besideOnStart })
        bind(ui.quickEnd, rows.beside?.takeIf { !rows.besideOnStart })
    }

    private fun bind(
        button: Button,
        messenger: Messenger?,
    ) {
        button.isVisible = messenger != null
        if (messenger == null) return
        button.text = messenger.label
        button.setCompoundDrawablesRelativeWithIntrinsicBounds(messenger.icon, 0, 0, 0)
        button.setOnClickListener { share(messenger) }
    }

    private fun showSaved(saved: Boolean) {
        ui.saveGallery.isEnabled = !saved
        ui.saveGallery.setText(if (saved) R.string.saved_gallery else R.string.save_gallery)
    }

    /** At most one gallery copy per photo: marked saved before the write, so a tap or send meanwhile adds none. */
    private fun saveToGallery(ready: State.Ready) {
        if (ready.saved) return
        ready.saved = true
        showSaved(true)
        val takenAt = ready.captureTime.toInstant().toEpochMilli()
        val token = generation
        activity.lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) { GallerySaver.save(activity.applicationContext, ready.photo.file, takenAt) }
            if (!ok) {
                ready.saved = false
                toast(R.string.gallery_failed)
            }
            if (state !== ready) return@launch
            showSaved(ready.saved)
            val meta = readyMeta(ready)
            withContext(Dispatchers.IO) { runCatching { store.updateReady(meta) { token == generation } } }
        }
    }

    private fun installed(m: Messenger) = m.firstInstalled { Sharing.isInstalled(activity, it) }


    private fun displayEdge() = activity.resources.displayMetrics.let { maxOf(it.widthPixels, it.heightPixels) }

    private fun labels() =
        PendingLocation.Labels(
            noFixYet = activity.getString(R.string.pending_no_fix),
            servicesOff = activity.getString(R.string.pending_services_off),
            denied = activity.getString(R.string.pending_denied),
            secondsAgo = { activity.getString(R.string.pending_seconds_ago, it) },
            minutesAgo = { activity.getString(R.string.pending_minutes_ago, it) },
        )

    private fun toast(res: Int) = Toast.makeText(activity, res, Toast.LENGTH_LONG).show()

    private companion object {
        const val TAG = "ReportCam"
        const val SCHEME_CORNER_DP = 6f
        const val GREY = 0xFF757575.toInt()
    }
}
