package cz.julienika.reportcam.ui

import kotlin.math.pow

/** WCAG 2 contrast between opaque colours (alpha ignored); 4.5:1 is the AA level for normal text. */
object Contrast {
    const val READABLE = 4.5

    private const val WHITE = 0xFFFFFFFF.toInt()
    private const val BLACK = 0xFF000000.toInt()

    fun ratio(
        a: Int,
        b: Int,
    ): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    /** White when it is readable on [background], otherwise black. */
    fun foregroundOn(background: Int): Int = if (ratio(WHITE, background) >= READABLE) WHITE else BLACK

    fun luminance(c: Int): Double {
        fun channel(shift: Int): Double {
            val v = (c shr shift and 0xFF) / 255.0
            return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }
}
