package cz.julienika.reportcam.stamp

/** The two stamp colour schemes (Owner, G-003). Stored by [id]. */
enum class SchemeId(
    val id: String,
) {
    DARK("dark"),
    LIGHT("light"),
    ;

    val other: SchemeId get() = if (this == DARK) LIGHT else DARK

    companion object {
        fun of(id: String): SchemeId? = entries.firstOrNull { it.id == id }
    }
}

/**
 * One scheme's colours. [background] and [text] are opaque RGB colours from the palettes below;
 * [overlayOpacity] (percent) applies to an overlay's box only: a frame is always opaque.
 */
data class StampScheme(
    val background: Int,
    val text: Int,
    val overlayOpacity: Int,
) {
    fun style(textSizeFraction: Float): StampStyle {
        val alpha = (overlayOpacity.coerceIn(MIN_OPACITY, MAX_OPACITY) * 255 + 50) / 100
        return StampStyle(
            textSizeFraction = textSizeFraction,
            textColor = text or OPAQUE,
            backgroundColor = (alpha shl 24) or (background and RGB),
        )
    }

    companion object {
        private const val OPAQUE = 0xFF000000.toInt()
        private const val RGB = 0x00FFFFFF

        /** Below this the box stops separating text from a busy photo; at 100 it is a solid label. */
        const val MIN_OPACITY = 20
        const val MAX_OPACITY = 100

        /** The colours the app had before schemes existed: white on 60 % black, black on 70 % white. */
        val DEFAULT_DARK = StampScheme(0x000000 or OPAQUE, 0xFFFFFF or OPAQUE, 60)
        val DEFAULT_LIGHT = StampScheme(0xFFFFFF or OPAQUE, 0x000000 or OPAQUE, 70)

        fun default(id: SchemeId) = if (id == SchemeId.DARK) DEFAULT_DARK else DEFAULT_LIGHT

        /*
         * Each scheme's own palettes, from the Owner's colour chart plus black and white (G-003 phone test): the
         * dark scheme has dark backgrounds and light text, the light scheme the reverse, and every pairing within a
         * scheme is readable (D024). Order: black or white, then slate/spruce/navy/charcoal/auburn and their kin.
         */
        private val DARK_BACKGROUNDS = opaque(0x000000, 0x2B3135, 0x1B2720, 0x1A212E, 0x2A2725, 0x281F26)
        private val DARK_TEXTS = opaque(0xFFFFFF, 0xFFF9EB, 0xDFF7EB, 0xFFEBAE, 0xFFDDD1, 0xE1F4FC)
        private val LIGHT_BACKGROUNDS = opaque(0xFFFFFF, 0xD8E2DA, 0xEBE6DC, 0xEADECE, 0xDFE2EB, 0xE6E1D5)
        private val LIGHT_TEXTS = opaque(0x000000, 0x101B30, 0x1C1E21, 0x0F2B18, 0x3F2E24, 0x391629)

        fun backgrounds(id: SchemeId): List<Int> = if (id == SchemeId.DARK) DARK_BACKGROUNDS else LIGHT_BACKGROUNDS

        fun texts(id: SchemeId): List<Int> = if (id == SchemeId.DARK) DARK_TEXTS else LIGHT_TEXTS

        private fun opaque(vararg rgb: Int) = rgb.map { it or OPAQUE }

        /** "#RRGGBB" as stored in settings; null for anything else. */
        fun parseColor(s: String): Int? {
            val hex = s.removePrefix("#")
            if (s.length != 7 || hex.length != 6 || !hex.all { it in '0'..'9' || it.lowercaseChar() in 'a'..'f' }) return null
            return hex.toInt(16) or OPAQUE
        }

        fun formatColor(argb: Int): String = "#%06X".format(argb and RGB)
    }
}
