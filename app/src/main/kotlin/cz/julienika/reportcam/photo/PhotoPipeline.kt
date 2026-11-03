package cz.julienika.reportcam.photo

import android.graphics.Bitmap
import cz.julienika.reportcam.camera.CapturedJpeg
import cz.julienika.reportcam.stamp.StampData
import cz.julienika.reportcam.stamp.StampKeys
import cz.julienika.reportcam.stamp.StampRenderer
import cz.julienika.reportcam.stamp.StampSpec
import java.io.File
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
     * Stamps [base], writes the JPEG and returns it with a display copy. [base] is consumed: do not use it
     * after. [nameSuffix] tells a redrawn version's file from the first one.
     */
    fun finish(
        base: Bitmap,
        spec: StampSpec,
        data: StampData,
        displayLongEdge: Int,
        nameSuffix: String = "",
    ): ProcessedPhoto {
        val stamped = StampRenderer.render(base, spec, data)
        outputDir.mkdirs()
        val base = "report_" + FILE_TIME.format(data[StampKeys.CAPTURE_TIME]) + nameSuffix
        // A redrawn version never reuses a name: a messenger may already hold that file (D019).
        val file =
            generateSequence(1) { it + 1 }
                .map { n -> File(outputDir, if (n == 1) "$base.jpg" else "$base-$n.jpg") }
                .first { nameSuffix.isEmpty() || !it.exists() }
        file.outputStream().buffered().use { stamped.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        val display = PhotoDecoder.displayCopy(stamped, displayLongEdge)
        val result = ProcessedPhoto(file, stamped.width, stamped.height, display)
        if (display !== stamped) stamped.recycle()
        return result
    }

    companion object {
        /** Messengers recompress anyway; 85 keeps stamp text crisp at a fraction of 95's size. */
        const val JPEG_QUALITY = 85
        private val FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS")
    }
}
