package cz.julienika.reportcam.review

import android.content.ActivityNotFoundException
import android.content.res.Configuration
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
import cz.julienika.reportcam.location.PlaceLookup
import cz.julienika.reportcam.photo.GallerySaver
import cz.julienika.reportcam.photo.PhotoDecoder
import cz.julienika.reportcam.photo.PhotoPipeline
import cz.julienika.reportcam.settings.AppSettings
import cz.julienika.reportcam.share.Messenger
import cz.julienika.reportcam.share.Sharing
import cz.julienika.reportcam.stamp.SchemeId
import cz.julienika.reportcam.stamp.StampData
import cz.julienika.reportcam.stamp.StampKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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
    private var state: ReviewState = ReviewState.Idle

    /**
     * Bumped on every new photo and on leaving, so a stamp that finishes for an abandoned photo is dropped.
     * Read by the store's guards off the main thread.
     */
    @Volatile
    private var generation = 0

    /** True once the photo was handed to a messenger: coming back to the app then means "next photo". */
    var sentSinceCapture = false
        private set

    val isActive: Boolean get() = state != ReviewState.Idle

    /**
     * The settings as they are now, for what only presents the photo: the quick buttons and the theme. Stamping
     * keeps the settings it started with; these follow a change made in Settings while the photo is in review.
     */
    private var current: AppSettings? = null

    /** A scheme or position tapped, and what to do once the photo is drawn: kept while a redraw runs ([redraw]). */
    private var wantedScheme: SchemeId? = null
    private var wantedPlacement: String? = null
    private val afterRedraw = mutableListOf<(ReviewState.Ready) -> Unit>()

    private val versions = PhotoVersions(sharedDir, places)
    private val texts = ReviewTexts(ui) { redraw() }
    private val schemes = SchemeButtons(ui) { redraw(scheme = it) }
    private val quick =
        QuickButtons(ui, activity.resources.displayMetrics.density, { installed(it) != null }) { share(it) }

    init {
        ui.retake.setOnClickListener { leave() }
        ui.useCurrent.setOnClickListener { useCurrent() }
        ui.shareOther.setOnClickListener { share(null) }
        ui.saveGallery.setOnClickListener {
            whenDrawn { ready -> storageAccess { if (state === ready) saveToGallery(ready) } }
        }
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
        state = ReviewState.Waiting(source, base, data, settings)
        // A bitmap of its own for the screen: the stamp step draws on [base] and may recycle it.
        val shown = PhotoDecoder.displayCopy(base, displayEdge())
        ui.reviewImage.setImageBitmap(if (shown === base) base.copy(Bitmap.Config.ARGB_8888, false) else shown)
        ui.pendingRow.isVisible = true
        ui.useCurrent.isVisible = true
        onLocation(tracker.status)
    }

    /** True when a photo from an earlier run is still on disk; [restore] brings it back. */
    val hasSaved: Boolean get() = store.exists()

    /**
     * Reads the kept photo back: a stamped one returns to review as it was left, a waiting one resumes its
     * wait. A broken entry is dropped.
     */
    fun restore(settings: AppSettings) {
        enter(settings)
        state = ReviewState.Restoring
        ui.busy.isVisible = true
        val token = generation
        val displayEdge = displayEdge()
        activity.lifecycleScope.launch {
            val stamped =
                withContext(Dispatchers.IO) {
                    try {
                        store.loadReady()?.let { kept -> versions.restore(kept.stamped, kept.meta.fileName, displayEdge) to kept }
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
                texts.show(m.note, m.noteOn, m.message, m.messageOn)
                val look = settings.withScheme(m.scheme).let { s -> m.placement?.let(s::withPlacement) ?: s }
                showReady(ReviewState.Ready(photo, look, m.savedFiles.toMutableSet(), source, m.facts))
                // A note left mid-edit when the app was stopped is drawn now.
                redraw()
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
        val waiting = state as? ReviewState.Waiting ?: return
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
        val waiting = state as? ReviewState.Waiting ?: return
        stamp(waiting.source, waiting.base, waiting.data.with(StampKeys.LOCATION, PendingLocation.current(tracker.status)), waiting.settings)
    }

    private fun stamp(
        source: PhotoSource,
        base: Bitmap,
        found: StampData,
        s: AppSettings,
    ) {
        state = ReviewState.Stamping
        val token = generation
        ui.pendingRow.isVisible = false
        ui.useCurrent.isVisible = false
        ui.busy.isVisible = true
        val displayEdge = displayEdge()
        activity.lifecycleScope.launch {
            val started = SystemClock.elapsedRealtime()
            val data = versions.withPlace(found, s)
            if (token != generation) return@launch // the user retook or left during the lookup
            val photo =
                try {
                    withContext(Dispatchers.Default) { versions.draw(source, s, data, displayEdge, base) }
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
            val ready = ReviewState.Ready(photo, s, mutableSetOf(), source, StampFacts.of(data))
            texts.show(data[StampKeys.USER_TEXT].orEmpty(), noteOn = true, ready.stampText, s.message)
            // The stamped copy replaces the waiting one on disk before anything can be sent (D015), and
            // not at all if the user left meanwhile.
            val meta = readyMeta(ready)
            withContext(Dispatchers.IO) {
                try {
                    store.saveReady(photo.file, source.jpeg, meta) { token == generation }
                } catch (e: Exception) {
                    Log.e(TAG, "could not keep the stamped photo on disk", e)
                }
            }
            if (token != generation) return@launch
            showReady(ready)
        }
    }

    /** Shows [ready] with its controls; the text rows are filled separately, so a redraw never overwrites typing. */
    private fun showReady(ready: ReviewState.Ready) {
        state = ready
        ui.pendingRow.isVisible = false
        ui.useCurrent.isVisible = false
        ui.reviewImage.setImageBitmap(ready.photo.display)
        showSaved(ready.saved)
        schemes.show(ready.settings)
        if (!ui.shareRow.isVisible) setShareVisible(true)
    }

    /**
     * Draws the photo under review again if its look changed: the scheme tapped (G-003), the note finished
     * (G-004) or the position tapped (G-005). The new version is drawn from the kept camera JPEG (D019), or is the version drawn earlier with
     * the same look (D025). [then] runs on the current version once everything asked for so far is drawn; a
     * call during a redraw is picked up when that one ends. The message follows the note unless edited.
     */
    private fun redraw(
        scheme: SchemeId? = null,
        placement: String? = null,
        then: ((ReviewState.Ready) -> Unit)? = null,
    ) {
        if (scheme != null) wantedScheme = scheme
        if (placement != null) wantedPlacement = placement
        if (then != null) afterRedraw += then
        val ready = state as? ReviewState.Ready ?: return
        val s =
            ready.settings
                .withScheme(wantedScheme ?: ready.settings.scheme)
                .withPlacement(wantedPlacement ?: ready.settings.placementId)
        val facts = ready.facts.copy(userText = texts.stampedNote)
        wantedScheme = null
        wantedPlacement = null
        if (s.scheme == ready.settings.scheme && s.placementId == ready.settings.placementId && facts == ready.facts) {
            val actions = afterRedraw.toList()
            afterRedraw.clear()
            actions.forEach { if (state === ready) it(ready) }
            return
        }
        state = ReviewState.Stamping
        val token = generation
        ui.busy.isVisible = true
        val displayEdge = displayEdge()
        activity.lifecycleScope.launch {
            val data = facts.toStampData()
            val photo =
                try {
                    withContext(Dispatchers.Default) { versions.draw(ready.source, s, data, displayEdge) }
                } catch (e: Exception) {
                    Log.e(TAG, "redrawing failed", e)
                    null
                }
            if (token != generation) return@launch
            ui.busy.isVisible = false
            if (photo == null) {
                toast(R.string.process_failed)
                dropPending()
                showReady(ready)
                return@launch
            }
            val next = ReviewState.Ready(photo, s, ready.savedFiles, ready.source, facts)
            texts.follow(ready.stampText, next.stampText)
            val meta = readyMeta(next)
            withContext(Dispatchers.IO) {
                try {
                    store.replaceStamped(photo.file, meta) { token == generation }
                } catch (e: Exception) {
                    Log.e(TAG, "could not keep the redrawn photo on disk", e)
                }
            }
            if (token != generation) return@launch
            Log.i(TAG, "redrawn as ${photo.file.name}")
            showReady(next)
            redraw() // a change made meanwhile, or the action waiting for this version
        }
    }

    /** Forgets a look asked for and the action waiting for a redraw: the photo they were for is gone. */
    private fun dropPending() {
        wantedScheme = null
        wantedPlacement = null
        afterRedraw.clear()
    }

    /** Finishes a note edit in progress and waits for its version, so what is sent or saved shows the note. */
    private fun whenDrawn(action: (ReviewState.Ready) -> Unit) {
        if (state !is ReviewState.Ready && state !== ReviewState.Stamping) return
        texts.finishEditing()
        redraw(then = action)
    }

    private fun readyMeta(ready: ReviewState.Ready) =
        ReadyMeta(
            fileName = ready.photo.file.name,
            message = texts.message,
            messageOn = texts.messageOn,
            note = texts.note,
            noteOn = texts.noteOn,
            savedFiles = ready.savedFiles.toSet(),
            scheme = ready.settings.scheme,
            placement = ready.settings.placementId,
            rotationDegrees = ready.source.jpeg.rotationDegrees,
            outputLongEdge = ready.source.longEdge,
            aspect = ready.source.aspect,
            facts = ready.facts,
        )

    /**
     * Writes the text rows and saved versions of the photo under review to disk. Called from the
     * activity's onStop, on the main thread: a small file, and done before a restart could read it.
     */
    fun persist() {
        val ready = state as? ReviewState.Ready ?: return
        val token = generation
        try {
            store.updateReady(readyMeta(ready)) { token == generation }
        } catch (e: Exception) {
            Log.e(TAG, "could not keep the review texts on disk", e)
        }
    }

    private fun share(messenger: Messenger?) = whenDrawn { ready -> send(ready, messenger) }

    private fun send(
        ready: ReviewState.Ready,
        messenger: Messenger?,
    ) {
        val pkg = messenger?.let { installed(it) ?: return }
        val intent = Sharing.sendIntent(activity, ready.photo.file, texts.messageToSend, pkg)
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
        state = ReviewState.Idle
        dropPending()
        texts.finishEditing() // closes the keyboard; with no photo in review, nothing is drawn
        ui.busy.isVisible = false
        ui.reviewImage.setImageDrawable(null)
        ui.reviewImage.isVisible = false
        ui.reviewControls.isVisible = false
        ui.cameraControls.isVisible = true
        ui.preview.isVisible = true
        ui.cropGuide.isVisible = true
        if (wasActive) onLeave()
    }

    /** The keyboard opened or closed; closing it ends a compact field's edit. */
    fun keyboardShown(shown: Boolean) = texts.keyboardShown(shown)

    /** Settings changed (back from the Settings screen): the quick buttons and the theme follow at once. */
    fun settingsChanged(settings: AppSettings) {
        current = settings
        if (!isActive) return
        texts.compact = settings.compactTexts
        paintControls(settings)
        if (state is ReviewState.Ready && ui.shareRow.isVisible) setShareVisible(true)
    }

    /** The scheme buttons are drawn in their schemes and the quick buttons in their brands, so the palette skips them. */
    private fun paintControls(settings: AppSettings) {
        val systemDark =
            activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        ReviewPalette
            .of(settings.appTheme.isDark(systemDark))
            .paint(
                ui.reviewControls,
                activity.resources.displayMetrics.density,
                schemes.views + quick.views,
            )
    }

    private fun enter(settings: AppSettings) {
        generation++
        current = settings
        dropPending()
        texts.compact = settings.compactTexts
        paintControls(settings)
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

    /** The stamped-photo rows, with the quick messengers. */
    private fun setShareVisible(on: Boolean) {
        val ready = state as? ReviewState.Ready
        val visible = on && ready != null
        ui.noteRow.isVisible = visible
        ui.messageRow.isVisible = visible
        ui.schemeRow.isVisible = visible
        ui.shareRow.isVisible = visible
        ui.saveGallery.isVisible = visible
        if (ready == null || !visible) quick.hide() else quick.show(current ?: ready.settings)
    }

    private fun showSaved(saved: Boolean) {
        ui.saveGallery.isEnabled = !saved
        ui.saveGallery.setText(if (saved) R.string.saved_gallery else R.string.save_gallery)
    }

    /** At most one gallery copy per photo: marked saved before the write, so a tap or send meanwhile adds none. */
    private fun saveToGallery(ready: ReviewState.Ready) {
        if (ready.saved) return
        val name = ready.photo.file.name
        ready.savedFiles += name
        showSaved(true)
        val takenAt = ready.captureTime.toInstant().toEpochMilli()
        val token = generation
        activity.lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) { GallerySaver.save(activity.applicationContext, ready.photo.file, takenAt) }
            if (!ok) {
                ready.savedFiles -= name
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
    }
}
