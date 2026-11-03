package cz.julienika.reportcam.camera

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import cz.julienika.reportcam.photo.AspectCrop
import cz.julienika.reportcam.photo.AspectRatio

/**
 * Dims the part of the live preview that the aspect-ratio setting will crop away. The preview is shown
 * whole (FIT_CENTER), so the undimmed area is exactly what the photo keeps (D009). It draws only and
 * never takes touches, so tap-to-focus and pinch reach the preview underneath.
 */
class CropGuideView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
    ) : View(context, attrs) {
        var ratio: AspectRatio = AspectRatio.FULL
            set(value) {
                field = value
                invalidate()
            }

        /** Width / height of the preview stream as it appears on this portrait screen. */
        var contentAspect: Float = PORTRAIT_4_3
            set(value) {
                field = value
                invalidate()
            }

        private val dim = Paint().apply { color = DIM }
        private val edge =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = EDGE
                style = Paint.Style.STROKE
                strokeWidth = resources.displayMetrics.density
            }

        override fun onDraw(canvas: Canvas) {
            if (ratio == AspectRatio.FULL || width == 0 || height == 0) return
            // The preview content, fitted and centred in this view.
            val vw = width.toFloat()
            val vh = height.toFloat()
            val (cw, ch) = if (vw / vh > contentAspect) vh * contentAspect to vh else vw to vw / contentAspect
            val cl = (vw - cw) / 2
            val ct = (vh - ch) / 2
            val (kw, kh) = AspectCrop.rect(cw, ch, ratio)
            val kl = cl + (cw - kw) / 2
            val kt = ct + (ch - kh) / 2
            val kr = kl + kw
            val kb = kt + kh
            canvas.drawRect(cl, ct, cl + cw, kt, dim)
            canvas.drawRect(cl, kb, cl + cw, ct + ch, dim)
            canvas.drawRect(cl, kt, kl, kb, dim)
            canvas.drawRect(kr, kt, cl + cw, kb, dim)
            canvas.drawRect(kl, kt, kr, kb, edge)
        }

        private companion object {
            const val PORTRAIT_4_3 = 3f / 4f
            const val DIM = 0x99000000.toInt()
            const val EDGE = 0xCCFFFFFF.toInt()
        }
    }
