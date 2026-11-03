package cz.julienika.reportcam.photo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import kotlin.math.max
import kotlin.math.roundToInt

object PhotoDecoder {
    /**
     * Decodes camera JPEG bytes into an upright, mutable bitmap whose long edge is at most [maxLongEdge]
     * (0 = keep). Subsampling during decode keeps peak memory near the output size, not the sensor size.
     */
    fun decodeUpright(
        bytes: ByteArray,
        rotationDegrees: Int,
        maxLongEdge: Int,
    ): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val srcLong = max(bounds.outWidth, bounds.outHeight)
        require(srcLong > 0) { "camera returned bytes that are not a decodable JPEG (${bytes.size} bytes)" }

        val opts =
            BitmapFactory.Options().apply {
                inSampleSize = OutputSizing.sampleSize(srcLong, maxLongEdge)
                inMutable = true
            }
        val decoded =
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                ?: error("JPEG decode failed after bounds succeeded")

        val scale = OutputSizing.scale(max(decoded.width, decoded.height), maxLongEdge)
        if (scale == 1f && rotationDegrees == 0) return decoded

        val m =
            Matrix().apply {
                if (scale != 1f) postScale(scale, scale)
                if (rotationDegrees != 0) postRotate(rotationDegrees.toFloat())
            }
        val out = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, m, true)
        if (out !== decoded) decoded.recycle()
        return if (out.isMutable) out else out.copy(Bitmap.Config.ARGB_8888, true).also { out.recycle() }
    }

    /** The centred [ratio] part of [src] as a new mutable bitmap; [src] is recycled. FULL returns [src]. */
    fun cropTo(
        src: Bitmap,
        ratio: AspectRatio,
    ): Bitmap {
        val r = AspectCrop.centered(src.width, src.height, ratio)
        if (r.width == src.width && r.height == src.height) return src
        // Drawn into a fresh bitmap: createBitmap(src, x, y, w, h) may return an immutable one.
        val out = Bitmap.createBitmap(r.width, r.height, Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(src, -r.left.toFloat(), -r.top.toFloat(), null)
        src.recycle()
        return out
    }

    /** A display-sized copy for the review screen, so a full-size photo never goes to the GPU whole. */
    fun displayCopy(
        src: Bitmap,
        maxLongEdge: Int,
    ): Bitmap {
        val scale = OutputSizing.scale(max(src.width, src.height), maxLongEdge)
        if (scale == 1f) return src
        return Bitmap.createScaledBitmap(src, (src.width * scale).roundToInt(), (src.height * scale).roundToInt(), true)
    }
}
