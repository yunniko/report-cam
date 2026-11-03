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

    /** Stamps [base], writes the JPEG and returns it with a display copy. [base] is consumed: do not use it after. */
    fun finish(
        base: Bitmap,
        spec: StampSpec,
        data: StampData,
        displayLongEdge: Int,
    ): ProcessedPhoto {
        val stamped = StampRenderer.render(base, spec, data)
        outputDir.mkdirs()
        val name = "report_" + FILE_TIME.format(data[StampKeys.CAPTURE_TIME]) + ".jpg"
        val file = File(outputDir, name)
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
