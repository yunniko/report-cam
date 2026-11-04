package cz.julienika.reportcam.review

import android.graphics.Bitmap
import cz.julienika.reportcam.photo.PhotoMetadata
import cz.julienika.reportcam.photo.ProcessedPhoto
import cz.julienika.reportcam.settings.AppSettings
import cz.julienika.reportcam.stamp.StampData
import java.time.ZonedDateTime

/** Where the photo under review is: none, read back, waiting for a fix (D008), being stamped, or stamped. */
internal sealed interface ReviewState {
    data object Idle : ReviewState

    /** A waiting photo from an earlier run is being read back from the store. */
    data object Restoring : ReviewState

    class Waiting(
        val source: PhotoSource,
        val base: Bitmap,
        val data: StampData,
        val settings: AppSettings,
    ) : ReviewState

    data object Stamping : ReviewState

    class Ready(
        val photo: ProcessedPhoto,
        /** The settings this version was stamped with; its scheme is the one the switch shows. */
        val settings: AppSettings,
        /**
         * The versions of this photo in the gallery, shared by all its versions: neither Save nor sending
         * adds another copy of one (Owner, 2026-11-03).
         */
        val savedFiles: MutableSet<String>,
        val source: PhotoSource,
        /** What this version shows; its `userText` is the note as stamped. */
        val facts: StampFacts,
    ) : ReviewState {
        val captureTime: ZonedDateTime get() = facts.captureTime
        val saved: Boolean get() = photo.file.name in savedFiles

        /** What the stamp says, line by line: the message is filled with it and follows it. */
        val stampText: String by lazy {
            PhotoMetadata.of(settings.stampSpec, facts.toStampData(), settings.preciseWithinM).stampText
        }
    }
}
