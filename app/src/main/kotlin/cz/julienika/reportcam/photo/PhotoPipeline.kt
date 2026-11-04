package cz.julienika.reportcam.photo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import cz.julienika.reportcam.camera.CapturedJpeg
import cz.julienika.reportcam.stamp.StampData
import cz.julienika.reportcam.stamp.StampKeys
import cz.julienika.reportcam.stamp.StampRenderer
import cz.julienika.reportcam.stamp.StampSpec
import java.io.File
import java.security.MessageDigest
import java.time.format.DateTimeFormatter

/** The finished photo: the JPEG file that is shared, and a screen-sized bitmap for review. */
class ProcessedPhoto(
    val file: File,
    val width: Int,
    val height: Int,
    val display: Bitmap,
)

/**
 * Camera bytes in, a stamped JPEG file out, in two steps so the stamp can wait for its data (D008):
 * [decode] right after the shutter, [finish] once the stamp data is final. Runs off the main thread.
 */
class PhotoPipeline(
    private val outputDir: File,
) {
    /** The upright photo at output size, cropped to [aspect], not yet stamped. */
    fun decode(
        jpeg: CapturedJpeg,
        longEdge: Int,
        aspect: AspectRatio,
    ): Bitmap = PhotoDecoder.cropTo(PhotoDecoder.decodeUpright(jpeg.bytes, jpeg.rotationDegrees, longEdge), aspect)

    /**
     * Stamps [base], writes it as [fileName] (from [fileName] in the companion) and returns it with a display
     * copy. [base] is consumed: do not use it after.
     */
    fun finish(
        base: Bitmap,
        spec: StampSpec,
        data: StampData,
        displayLongEdge: Int,
        fileName: String,
    ): ProcessedPhoto {
        val stamped = StampRenderer.render(base, spec, data)
        outputDir.mkdirs()
        val file = File(outputDir, fileName)
        file.outputStream().buffered().use { stamped.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        val display = PhotoDecoder.displayCopy(stamped, displayLongEdge)
        val result = ProcessedPhoto(file, stamped.width, stamped.height, display)
        if (display !== stamped) stamped.recycle()
        return result
    }

    /** A version already written, read back with a display copy: nothing is drawn again. */
    fun load(
        file: File,
        displayLongEdge: Int,
    ): ProcessedPhoto {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        check(bounds.outWidth > 0) { "${file.name} does not decode" }
        val sample = OutputSizing.sampleSize(maxOf(bounds.outWidth, bounds.outHeight), displayLongEdge)
        val decoded =
            BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
                ?: error("${file.name} does not decode")
        return ProcessedPhoto(file, bounds.outWidth, bounds.outHeight, PhotoDecoder.displayCopy(decoded, displayLongEdge))
    }

    companion object {
        /** Messengers recompress anyway; 85 keeps stamp text crisp at a fraction of 95's size. */
        const val JPEG_QUALITY = 85
        private val FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS")
        private const val KEY_BYTES = 4

        /**
         * `report_<capture time>_<key>.jpg`, the key a short hash of everything the version shows: the same
         * stamp always gets the same name and a different stamp never does, so a file a messenger holds is
         * never changed, and going back to an earlier look finds that version again (Owner, G-004; D025).
         */
        fun fileName(
            spec: StampSpec,
            data: StampData,
            preciseWithinM: Float,
            metadata: PhotoMetadata = PhotoMetadata.of(spec, data, preciseWithinM),
        ): String {
            val key = "${spec.placement}|${spec.style}|$preciseWithinM|${metadata.stampText}"
            val hash = MessageDigest.getInstance("SHA-256").digest(key.toByteArray()).take(KEY_BYTES)
            return "report_" + FILE_TIME.format(data[StampKeys.CAPTURE_TIME]) + "_" +
                hash.joinToString("") { "%02x".format(it) } + ".jpg"
        }
    }
}
