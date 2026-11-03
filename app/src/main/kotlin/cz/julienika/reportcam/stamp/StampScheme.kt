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

        /** Backgrounds on offer: dark ones first, then light ones. */
        val BACKGROUNDS: List<Int> =
            listOf(0x000000, 0x37474F, 0x0D2B45, 0x1B3A26, 0x5D1A1A, 0xFFFFFF, 0xE0E0E0, 0xFFF3D6, 0xFFD600, 0xCDE8F6)
                .map { it or OPAQUE }

        /** Text colours on offer, each readable on at least one background above. */
        val TEXTS: List<Int> =
            listOf(0xFFFFFF, 0x000000, 0xFFD600, 0xF2A33A, 0xB9F6CA, 0x80D8FF, 0xFF8A80, 0x0D2B45, 0x8B0000, 0x1B5E20)
                .map { it or OPAQUE }

        /** "#RRGGBB" as stored in settings; null for anything else. */
        fun parseColor(s: String): Int? {
            val hex = s.removePrefix("#")
            if (s.length != 7 || hex.length != 6 || !hex.all { it in '0'..'9' || it.lowercaseChar() in 'a'..'f' }) return null
            return hex.toInt(16) or OPAQUE
        }

        fun formatColor(argb: Int): String = "#%06X".format(argb and RGB)
    }
}
