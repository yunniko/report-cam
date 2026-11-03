package cz.julienika.reportcam.stamp

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface

/** [TextMeasurer] over an Android [Paint]; the renderer draws with the same paint settings. */
class PaintTextMeasurer : TextMeasurer {
    private val paint = newTextPaint()

    override fun width(
        text: String,
        sizePx: Float,
    ): Float {
        paint.textSize = sizePx
        return paint.measureText(text)
    }

    override fun ascent(sizePx: Float): Float {
        paint.textSize = sizePx
        return -paint.fontMetrics.ascent
    }

    override fun lineHeight(sizePx: Float): Float {
        paint.textSize = sizePx
        val fm = paint.fontMetrics
        return (fm.descent - fm.ascent) * LINE_SPACING
    }

    private companion object {
        const val LINE_SPACING = 1.1f
    }
}

internal fun newTextPaint() =
    Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

/** Draws a [StampGeometry]. It knows boxes and lines only, never which field or placement made them. */
object StampRenderer {
    /**
     * Returns the stamped photo. When the output is the photo's size (overlay) it draws in place on
     * [photo], which must be mutable; otherwise it makes a new bitmap and recycles [photo].
     */
    fun render(
        photo: Bitmap,
        spec: StampSpec,
        data: StampData,
        measurer: TextMeasurer = PaintTextMeasurer(),
    ): Bitmap {
        val g = StampLayout.compute(photo.width, photo.height, spec, data, measurer) ?: return photo
        val inPlace = g.outputWidth == photo.width && g.outputHeight == photo.height && g.imageLeft == 0 && g.imageTop == 0
        val out =
            if (inPlace) {
                photo
            } else {
                Bitmap.createBitmap(g.outputWidth, g.outputHeight, Bitmap.Config.ARGB_8888)
            }
        val canvas = Canvas(out)
        if (inPlace) {
            g.box?.let { drawBox(canvas, it, g.boxCornerRadius, spec.style.backgroundColor) } // translucent, over the photo
        } else {
            // Added area has nothing behind it, so its box is opaque, and drawn first: a frame's box spans
            // the whole output and the photo goes over its middle.
            g.box?.let { drawBox(canvas, it, g.boxCornerRadius, spec.style.backgroundColor or OPAQUE) }
            canvas.drawBitmap(photo, g.imageLeft.toFloat(), g.imageTop.toFloat(), null)
            photo.recycle()
        }
        val text =
            newTextPaint().apply {
                color = spec.style.textColor
                textSize = g.textSizePx
            }
        for (line in g.lines) canvas.drawText(line.text, line.x, line.baseline, text)
        return out
    }

    private fun drawBox(
        canvas: Canvas,
        b: Box,
        radius: Float,
        color: Int,
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        canvas.drawRoundRect(b.left, b.top, b.right, b.bottom, radius, radius, paint)
    }

    private const val OPAQUE = 0xFF000000.toInt()
}
