package cz.julienika.reportcam.photo

import kotlin.math.roundToInt

/**
 * The photo's shape as short edge : long edge. It follows the way the phone is held, so 2:3 gives a
 * portrait 2:3 or a landscape 3:2 and never a thin strip across a portrait frame (D009).
 */
enum class AspectRatio(
    val id: String,
    val short: Int,
    val long: Int,
) {
    /** The sensor's own shape, no crop. */
    FULL("full", 0, 0),
    SQUARE("1:1", 1, 1),
    R4_5("4:5", 4, 5),
    R2_3("2:3", 2, 3),
    R9_16("9:16", 9, 16),
    ;

    companion object {
        fun of(id: String) = entries.firstOrNull { it.id == id } ?: FULL
    }
}

data class CropRect(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
)

/** Pure crop arithmetic, shared by the photo pipeline and the preview guide so both cut the same part. */
object AspectCrop {
    /** The largest centred rectangle of [ratio] inside a [width]×[height] image. */
    fun centered(
        width: Int,
        height: Int,
        ratio: AspectRatio,
    ): CropRect {
        val r = rect(width.toFloat(), height.toFloat(), ratio)
        val w = r.first.roundToInt().coerceIn(1, width)
        val h = r.second.roundToInt().coerceIn(1, height)
        return CropRect((width - w) / 2, (height - h) / 2, w, h)
    }

    /** As [centered], unrounded: the size of the kept area, for drawing a guide over a scaled preview. */
    fun rect(
        width: Float,
        height: Float,
        ratio: AspectRatio,
    ): Pair<Float, Float> {
        if (ratio == AspectRatio.FULL || width <= 0f || height <= 0f) return width to height
        val target = ratio.short.toFloat() / ratio.long // short / long, at most 1
        val portrait = height >= width
        val shortEdge = if (portrait) width else height
        val longEdge = if (portrait) height else width
        val (keepShort, keepLong) =
            if (shortEdge / longEdge > target) {
                longEdge * target to longEdge // too wide for the shape: trim the short edge
            } else {
                shortEdge to shortEdge / target // trim the long edge
            }
        return if (portrait) keepShort to keepLong else keepLong to keepShort
    }
}
