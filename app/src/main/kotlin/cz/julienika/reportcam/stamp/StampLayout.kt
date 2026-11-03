package cz.julienika.reportcam.stamp

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

/** Text metrics, so layout is plain Kotlin: Android supplies a Paint-backed one, tests a fixed-width one. */
interface TextMeasurer {
    fun width(
        text: String,
        sizePx: Float,
    ): Float

    /** Distance from the top of a line box to its baseline. */
    fun ascent(sizePx: Float): Float

    /** Height of one line box, ascent plus descent plus leading. */
    fun lineHeight(sizePx: Float): Float
}

/** Colours are ARGB ints, as `android.graphics.Color` uses them. */
data class StampStyle(
    /** Text height as a fraction of the photo's short edge, so the stamp reads the same at any output size. */
    val textSizeFraction: Float = TEXT_MEDIUM,
    val textColor: Int = 0xFFFFFFFF.toInt(),
    /** Behind the text for an overlay; the whole band for a frame. */
    val backgroundColor: Int = 0x99000000.toInt(),
) {
    companion object {
        const val TEXT_SMALL = 0.024f
        const val TEXT_MEDIUM = 0.032f
        const val TEXT_LARGE = 0.044f
        const val MIN_TEXT_PX = 12f
    }
}

data class PlacedLine(
    val text: String,
    val x: Float,
    val baseline: Float,
)

data class Box(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

/** Everything the renderer needs: output size, where the photo goes, the background box and each line. */
data class StampGeometry(
    val outputWidth: Int,
    val outputHeight: Int,
    val imageLeft: Int,
    val imageTop: Int,
    val box: Box?,
    val boxCornerRadius: Float,
    val textSizePx: Float,
    val lines: List<PlacedLine>,
)

/** The wrapped, measured lines of a stamp, before they are put anywhere. */
data class TextBlock(
    val lines: List<String>,
    val widths: List<Float>,
    val sizePx: Float,
    val lineHeight: Float,
    val ascent: Float,
) {
    val width: Float get() = widths.maxOrNull() ?: 0f
    val height: Float get() = lineHeight * lines.size

    companion object {
        /** Wraps on spaces; a single word wider than [maxWidth] is broken by characters. */
        fun build(
            raw: List<String>,
            maxWidth: Float,
            sizePx: Float,
            m: TextMeasurer,
        ): TextBlock {
            val out = mutableListOf<String>()
            for (line in raw) out += wrap(line, maxWidth, sizePx, m)
            return TextBlock(out, out.map { m.width(it, sizePx) }, sizePx, m.lineHeight(sizePx), m.ascent(sizePx))
        }

        private fun wrap(
            line: String,
            maxWidth: Float,
            sizePx: Float,
            m: TextMeasurer,
        ): List<String> {
            if (m.width(line, sizePx) <= maxWidth) return listOf(line)
            val result = mutableListOf<String>()
            var current = ""
            for (word in line.split(' ').filter { it.isNotEmpty() }) {
                val candidate = if (current.isEmpty()) word else "$current $word"
                if (m.width(candidate, sizePx) <= maxWidth) {
                    current = candidate
                    continue
                }
                if (current.isNotEmpty()) result += current
                current = word
                while (m.width(current, sizePx) > maxWidth && current.length > 1) {
                    var cut = current.length - 1
                    while (cut > 1 && m.width(current.substring(0, cut), sizePx) > maxWidth) cut--
                    result += current.substring(0, cut)
                    current = current.substring(cut)
                }
            }
            if (current.isNotEmpty()) result += current
            return result
        }
    }
}

/**
 * Where the stamp goes. Overlay and frame are two implementations; a new layout (a side band, a
 * caption strip) is a new class here, and neither fields nor the renderer change.
 */
interface StampPlacement {
    val id: String

    /** Width available to one line of text on an image of this size. */
    fun maxTextWidth(
        imageWidth: Int,
        unit: Float,
    ): Float

    fun place(
        imageWidth: Int,
        imageHeight: Int,
        block: TextBlock,
        unit: Float,
    ): StampGeometry
}

enum class Corner(
    val top: Boolean,
    val start: Boolean,
) {
    TOP_START(true, true),
    TOP_END(true, false),
    BOTTOM_START(false, true),
    BOTTOM_END(false, false),
}

/** A translucent box in one corner of the photo; the output is the photo's own size. */
data class OverlayPlacement(
    val corner: Corner,
) : StampPlacement {
    override val id = "overlay_" + corner.name.lowercase()

    override fun maxTextWidth(
        imageWidth: Int,
        unit: Float,
    ) = imageWidth - 2 * (MARGIN + PADDING) * unit

    override fun place(
        imageWidth: Int,
        imageHeight: Int,
        block: TextBlock,
        unit: Float,
    ): StampGeometry {
        val pad = PADDING * unit
        val margin = MARGIN * unit
        val boxW = block.width + 2 * pad
        val boxH = block.height + 2 * pad
        val left = if (corner.start) margin else imageWidth - margin - boxW
        val top = if (corner.top) margin else imageHeight - margin - boxH
        val lines =
            block.lines.mapIndexed { i, text ->
                // Text aligns to the edge its corner sits on.
                val x = if (corner.start) left + pad else left + boxW - pad - block.widths[i]
                PlacedLine(text, x, top + pad + i * block.lineHeight + block.ascent)
            }
        return StampGeometry(
            outputWidth = imageWidth,
            outputHeight = imageHeight,
            imageLeft = 0,
            imageTop = 0,
            box = Box(left, top, left + boxW, top + boxH),
            boxCornerRadius = pad * 0.6f,
            textSizePx = block.sizePx,
            lines = lines,
        )
    }

    private companion object {
        const val MARGIN = 0.6f
        const val PADDING = 0.5f
    }
}

/**
 * A solid band added above or below the photo, and a border of the band's colour on the other three
 * edges; the photo itself is not covered. [borderFraction] is the border's width as a fraction of the
 * photo's short edge; 0 leaves only the band.
 */
data class FramePlacement(
    val top: Boolean,
    val borderFraction: Float = 0f,
) : StampPlacement {
    override val id = if (top) "frame_top" else "frame_bottom"

    override fun maxTextWidth(
        imageWidth: Int,
        unit: Float,
    ) = imageWidth - 2 * PADDING * unit

    override fun place(
        imageWidth: Int,
        imageHeight: Int,
        block: TextBlock,
        unit: Float,
    ): StampGeometry {
        val pad = PADDING * unit
        val border = (borderFraction.coerceAtLeast(0f) * minOf(imageWidth, imageHeight)).roundToInt()
        val outW = imageWidth + 2 * border
        val bandH = ceil(block.height + 2 * pad).roundToInt()
        // The band replaces the border on its own edge.
        val bandTop = if (top) 0f else (border + imageHeight).toFloat()
        val lines =
            block.lines.mapIndexed { i, text ->
                // Text lines up with the photo's edge, not the border's.
                PlacedLine(text, border + pad, bandTop + pad + i * block.lineHeight + block.ascent)
            }
        return StampGeometry(
            outputWidth = outW,
            outputHeight = imageHeight + border + bandH,
            imageLeft = border,
            imageTop = if (top) bandH else border,
            // The whole output is the frame colour; the photo is drawn over its middle.
            box = Box(0f, 0f, outW.toFloat(), (imageHeight + border + bandH).toFloat()),
            boxCornerRadius = 0f,
            textSizePx = block.sizePx,
            lines = lines,
        )
    }

    private companion object {
        const val PADDING = 0.6f
    }
}

/** What to stamp, where, and how it looks. */
data class StampSpec(
    val fields: List<StampField>,
    val placement: StampPlacement,
    val style: StampStyle = StampStyle(),
)

object StampLayout {
    /** Lays out [spec] for a photo of the given size. Returns null when no field has anything to say. */
    fun compute(
        imageWidth: Int,
        imageHeight: Int,
        spec: StampSpec,
        data: StampData,
        measurer: TextMeasurer,
    ): StampGeometry? {
        val raw = spec.fields.flatMap { it.lines(data) }
        if (raw.isEmpty()) return null
        val sizePx = max(StampStyle.MIN_TEXT_PX, spec.style.textSizeFraction * minOf(imageWidth, imageHeight))
        val maxWidth = spec.placement.maxTextWidth(imageWidth, sizePx).coerceAtLeast(sizePx)
        val block = TextBlock.build(raw, maxWidth, sizePx, measurer)
        return spec.placement.place(imageWidth, imageHeight, block, sizePx)
    }
}
