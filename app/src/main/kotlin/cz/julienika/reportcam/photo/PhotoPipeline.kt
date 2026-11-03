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

/** Camera bytes in, a stamped JPEG file out. Runs off the main thread; touches no UI. */
class PhotoPipeline(
    private val outputDir: File,
) {
    fun process(
        jpeg: CapturedJpeg,
        longEdge: Int,
        spec: StampSpec,
        data: StampData,
        displayLongEdge: Int,
    ): ProcessedPhoto {
        val upright = PhotoDecoder.decodeUpright(jpeg.bytes, jpeg.rotationDegrees, longEdge)
        val stamped = StampRenderer.render(upright, spec, data)
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
