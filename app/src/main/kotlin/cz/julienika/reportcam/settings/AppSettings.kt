package cz.julienika.reportcam.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import cz.julienika.reportcam.stamp.CoordinateFormat
import cz.julienika.reportcam.stamp.CoordinatesField
import cz.julienika.reportcam.stamp.Corner
import cz.julienika.reportcam.stamp.DateTimeField
import cz.julienika.reportcam.stamp.FramePlacement
import cz.julienika.reportcam.stamp.LocationLabels
import cz.julienika.reportcam.stamp.OverlayPlacement
import cz.julienika.reportcam.stamp.StampField
import cz.julienika.reportcam.stamp.StampPlacement
import cz.julienika.reportcam.stamp.StampSpec
import cz.julienika.reportcam.stamp.StampStyle
import cz.julienika.reportcam.stamp.TextField

/** Read access to stored settings, so the mapping below is unit-tested without Android. */
interface SettingsSource {
    fun string(
        key: String,
        default: String,
    ): String

    fun bool(
        key: String,
        default: Boolean,
    ): Boolean
}

class PrefsSource(
    private val prefs: SharedPreferences,
) : SettingsSource {
    override fun string(
        key: String,
        default: String,
    ) = prefs.getString(key, default) ?: default

    override fun bool(
        key: String,
        default: Boolean,
    ) = prefs.getBoolean(key, default)

    companion object {
        fun of(context: Context) = PrefsSource(PreferenceManager.getDefaultSharedPreferences(context))
    }
}

/** Every setting key and default lives here; `res/xml/preferences.xml` uses the same keys and defaults. */
object Keys {
    const val STAMP_TEXT = "stamp_text"
    const val SHOW_DATETIME = "show_datetime"
    const val DATETIME_PATTERN = "datetime_pattern"
    const val SHOW_LOCATION = "show_location"
    const val COORD_FORMAT = "coord_format"
    const val WAIT_PRECISE = "wait_precise"
    const val PLACEMENT = "placement"
    const val TEXT_SIZE = "text_size"
    const val COLORS = "colors"
    const val OUTPUT_SIZE = "output_size"
    const val SAVE_GALLERY = "save_gallery"
    const val CAPTION = "caption"

    const val DEFAULT_PATTERN = "yyyy-MM-dd HH:mm:ss"
    const val DEFAULT_PLACEMENT = "overlay_bottom_start"

    /** Small by default (Owner, 2026-11-03): long edge in px; 0 means the sensor's largest. See D006. */
    const val DEFAULT_OUTPUT_SIZE = "1280"
}

/** The settings as the app uses them. */
data class AppSettings(
    val stampSpec: StampSpec,
    /** The user's own stamp line from settings; blank prints nothing. */
    val userText: String,
    val showLocation: Boolean,
    val waitForPrecise: Boolean,
    val outputLongEdge: Int,
    val saveToGallery: Boolean,
    /** Send the stamp text as the share's caption (Owner, 2026-11-03). */
    val caption: Boolean,
) {
    companion object {
        fun from(
            s: SettingsSource,
            labels: LocationLabels = LocationLabels(),
        ): AppSettings {
            val showLocation = s.bool(Keys.SHOW_LOCATION, true)
            val fields = mutableListOf<StampField>(TextField())
            if (s.bool(Keys.SHOW_DATETIME, true)) {
                fields += DateTimeField(safePattern(s.string(Keys.DATETIME_PATTERN, Keys.DEFAULT_PATTERN)))
            }
            if (showLocation) {
                val format =
                    if (s.string(Keys.COORD_FORMAT, "decimal") == "dms") CoordinateFormat.DMS else CoordinateFormat.DECIMAL
                fields += CoordinatesField(format, labels)
            }
            val spec = StampSpec(fields, placement(s.string(Keys.PLACEMENT, Keys.DEFAULT_PLACEMENT)), style(s))
            return AppSettings(
                stampSpec = spec,
                userText = s.string(Keys.STAMP_TEXT, "").trim(),
                showLocation = showLocation,
                waitForPrecise = showLocation && s.bool(Keys.WAIT_PRECISE, false),
                outputLongEdge = s.string(Keys.OUTPUT_SIZE, Keys.DEFAULT_OUTPUT_SIZE).toIntOrNull() ?: 1280,
                saveToGallery = s.bool(Keys.SAVE_GALLERY, true),
                caption = s.bool(Keys.CAPTION, true),
            )
        }

        /** Every placement the settings offer, by stored id. A new placement is one more entry. */
        val PLACEMENTS: Map<String, StampPlacement> =
            listOf(
                OverlayPlacement(Corner.BOTTOM_START),
                OverlayPlacement(Corner.BOTTOM_END),
                OverlayPlacement(Corner.TOP_START),
                OverlayPlacement(Corner.TOP_END),
                FramePlacement(top = false),
                FramePlacement(top = true),
            ).associateBy { it.id }

        private fun placement(id: String) = PLACEMENTS[id] ?: PLACEMENTS.getValue(Keys.DEFAULT_PLACEMENT)

        private fun style(s: SettingsSource): StampStyle {
            val size =
                when (s.string(Keys.TEXT_SIZE, "medium")) {
                    "small" -> StampStyle.TEXT_SMALL
                    "large" -> StampStyle.TEXT_LARGE
                    else -> StampStyle.TEXT_MEDIUM
                }
            return when (s.string(Keys.COLORS, "light_on_dark")) {
                "dark_on_light" -> StampStyle(size, 0xFF000000.toInt(), 0xB3FFFFFF.toInt())
                "yellow_on_dark" -> StampStyle(size, 0xFFFFD600.toInt(), 0xB3000000.toInt())
                else -> StampStyle(size, 0xFFFFFFFF.toInt(), 0x99000000.toInt())
            }
        }

        /** A stored pattern that no longer parses falls back to the default instead of crashing a capture. */
        private fun safePattern(p: String): String =
            try {
                java.time.format.DateTimeFormatter
                    .ofPattern(p)
                p
            } catch (e: IllegalArgumentException) {
                Keys.DEFAULT_PATTERN
            }
    }
}
