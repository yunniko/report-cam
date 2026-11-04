package cz.julienika.reportcam.review

import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import cz.julienika.reportcam.camera.CapturedJpeg
import cz.julienika.reportcam.location.PlaceLookup
import cz.julienika.reportcam.photo.AspectRatio
import cz.julienika.reportcam.photo.ExifWriter
import cz.julienika.reportcam.photo.PhotoMetadata
import cz.julienika.reportcam.photo.PhotoPipeline
import cz.julienika.reportcam.photo.ProcessedPhoto
import cz.julienika.reportcam.settings.AppSettings
import cz.julienika.reportcam.stamp.LocationReading
import cz.julienika.reportcam.stamp.StampData
import cz.julienika.reportcam.stamp.StampKeys
import java.io.File

/** The camera JPEG a photo is drawn from, and the shape and size it was taken at: enough to draw it again. */
class PhotoSource(
    val jpeg: CapturedJpeg,
    val longEdge: Int,
    val aspect: AspectRatio,
)

/**
 * The stamped versions of the photo under review, as files in [sharedDir], the folder messengers read from.
 * Each look of the photo is one file, named for what it shows (D025). Everything here runs off the main thread.
 */
class PhotoVersions(
    private val sharedDir: File,
    private val places: PlaceLookup,
) {
    /** The place name of the fix being stamped, and of no other (D011); unchanged when off or not found. */
    suspend fun withPlace(
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

    /**
     * The version of [source] stamped with [s] and [data]: the file on disk when this look was drawn before,
     * else drawn now (D019), from [base] when one is decoded already. [base] is consumed either way.
     */
    fun draw(
        source: PhotoSource,
        s: AppSettings,
        data: StampData,
        displayEdge: Int,
        base: Bitmap? = null,
    ): ProcessedPhoto {
        val pipeline = PhotoPipeline(sharedDir)
        val metadata = PhotoMetadata.of(s.stampSpec, data, s.preciseWithinM)
        val name = PhotoPipeline.fileName(s.stampSpec, data, s.preciseWithinM, metadata)
        val existing = File(sharedDir, name)
        if (existing.isFile) {
            val loaded = runCatching { pipeline.load(existing, displayEdge) }.getOrNull()
            if (loaded != null) {
                base?.recycle()
                // In review again: the cache prune counts its age from now.
                existing.setLastModified(System.currentTimeMillis())
                return loaded
            }
        }
        val photo = base ?: pipeline.decode(source.jpeg, source.longEdge, source.aspect)
        // Written under a temporary name and renamed once complete, so a file found by name above is never
        // one cut short by a crash (it would be reused as it is).
        val part = pipeline.finish(photo, s.stampSpec, data, displayEdge, "$name.part")
        ExifWriter.write(part.file, metadata)
        check(part.file.renameTo(existing)) { "could not name ${existing.name}" }
        return ProcessedPhoto(existing, part.width, part.height, part.display)
    }

    /** The kept stamped file is copied back as [fileName], so the restored file is the one a messenger gets. */
    fun restore(
        kept: File,
        fileName: String,
        displayEdge: Int,
    ): ProcessedPhoto {
        val shared = File(sharedDir, fileName)
        sharedDir.mkdirs()
        kept.copyTo(shared, overwrite = true)
        return PhotoPipeline(sharedDir).load(shared, displayEdge)
    }

    companion object {
        private const val TAG = "ReportCam"
    }
}
