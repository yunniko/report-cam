package cz.julienika.reportcam.review

import android.content.Context
import android.util.Log
import cz.julienika.reportcam.camera.CapturedJpeg
import cz.julienika.reportcam.photo.GallerySaver
import cz.julienika.reportcam.settings.AppSettings
import cz.julienika.reportcam.stamp.LocationReading
import cz.julienika.reportcam.stamp.StampData
import cz.julienika.reportcam.stamp.StampKeys
import java.io.File
import java.time.Duration
import java.time.ZonedDateTime

/**
 * Drops the kept photo once it is older than the limit from Settings (G-007), at a start of the app: a photo
 * forgotten in review is not the next day's report. With "Save discarded photo" on, the photo goes to the
 * gallery first: a stamped one as it is, a waiting one stamped without location. Runs off the main thread;
 * nothing here is on a timer (no background work, G-001).
 */
class Expiry(
    private val store: PendingStore,
    /** Stamps a waiting photo with the settings given and no location, returning its file. */
    private val stamp: (CapturedJpeg, PendingMeta, AppSettings) -> File,
    /** Copies a file into the gallery under the name given, taken at the time given; false when refused. */
    private val save: (file: File, name: String, takenAtMillis: Long) -> Boolean,
) {
    enum class Outcome {
        /** No kept photo, no limit, or the photo is younger than it: review goes on. */
        KEPT,
        DROPPED,

        /** Dropped, and in the gallery: saved now, or earlier by the user. */
        DROPPED_AND_SAVED,
    }

    fun discardIfExpired(
        settings: AppSettings,
        nowMillis: Long,
    ): Outcome {
        val keepFor = settings.keepFor ?: return Outcome.KEPT
        val taken = store.captureTime() ?: return Outcome.KEPT
        if (!expired(taken, nowMillis, keepFor)) return Outcome.KEPT
        Log.i(TAG, "dropping the photo taken $taken: older than $keepFor")
        val inGallery =
            settings.saveDiscarded &&
                try {
                    saveKept(settings)
                } catch (e: Exception) {
                    Log.e(TAG, "could not save the discarded photo", e)
                    false
                }
        store.clear()
        return if (inGallery) Outcome.DROPPED_AND_SAVED else Outcome.DROPPED
    }

    /** True once the kept photo is in the gallery. */
    private fun saveKept(settings: AppSettings): Boolean {
        val kept = store.loadReady()
        if (kept != null) {
            // Already in the gallery under the same name: at most one copy per photo (D017).
            if (kept.meta.saved) return true
            return save(kept.stamped, kept.meta.fileName, kept.meta.captureTime.toInstant().toEpochMilli())
        }
        val (jpeg, meta) = store.load() ?: return false
        val file = stamp(jpeg, meta, settings)
        return save(file, file.name, meta.captureTime.toInstant().toEpochMilli())
    }

    companion object {
        private const val TAG = "ReportCam"

        /**
         * The review's expiry: a waiting photo is drawn through [versions] with "Location unavailable" when the
         * stamp shows location at all, at the [displayEdge] of the screen; saves go through [GallerySaver].
         */
        fun forReview(
            store: PendingStore,
            versions: PhotoVersions,
            context: Context,
            displayEdge: () -> Int,
        ) = Expiry(
            store,
            stamp = { jpeg, m, s ->
                val data =
                    StampData.EMPTY
                        .with(StampKeys.CAPTURE_TIME, m.captureTime)
                        .with(StampKeys.USER_TEXT, m.userText)
                        .with(StampKeys.LOCATION, if (s.showLocation) LocationReading.None else null)
                versions.draw(PhotoSource(jpeg, m.outputLongEdge, m.aspect), s, data, displayEdge()).also { it.display.recycle() }.file
            },
            save = { file, name, takenAt -> GallerySaver.save(context, file, takenAt, name) },
        )

        /** Whether a photo taken at [captureTime] has been in review for [keepFor] or longer by [nowMillis]; never with no limit. */
        fun expired(
            captureTime: ZonedDateTime,
            nowMillis: Long,
            keepFor: Duration?,
        ): Boolean = keepFor != null && nowMillis - captureTime.toInstant().toEpochMilli() >= keepFor.toMillis()
    }
}
