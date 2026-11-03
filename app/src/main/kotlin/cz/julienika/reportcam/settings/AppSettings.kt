package cz.julienika.reportcam.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import cz.julienika.reportcam.photo.AspectRatio
import cz.julienika.reportcam.share.Messenger
import cz.julienika.reportcam.stamp.CoordinateFormat
import cz.julienika.reportcam.stamp.CoordinatesField
import cz.julienika.reportcam.stamp.Corner
import cz.julienika.reportcam.stamp.DateTimeField
import cz.julienika.reportcam.stamp.FixQuality
import cz.julienika.reportcam.stamp.FramePlacement
import cz.julienika.reportcam.stamp.LocationLabels
import cz.julienika.reportcam.stamp.OverlayPlacement
import cz.julienika.reportcam.stamp.PlaceField
import cz.julienika.reportcam.stamp.PlaceFormat
import cz.julienika.reportcam.stamp.SchemeId
import cz.julienika.reportcam.stamp.StampData
import cz.julienika.reportcam.stamp.StampField
import cz.julienika.reportcam.stamp.StampPlacement
import cz.julienika.reportcam.stamp.StampScheme
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

    fun stringSet(
        key: String,
        default: Set<String>,
    ): Set<String>
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

    override fun stringSet(
        key: String,
        default: Set<String>,
    ): Set<String> = prefs.getStringSet(key, default) ?: default

    companion object {
        fun of(context: Context) = PrefsSource(PreferenceManager.getDefaultSharedPreferences(context))
    }
}

/** Every setting key and default lives here; `res/xml/preferences.xml` uses the same keys and defaults. */
object Keys {
    const val STAMP_TEXT = "stamp_text"
    const val SHOW_DATETIME = "show_datetime"
    const val SHOW_WEEKDAY = "show_weekday"
    const val DATETIME_PATTERN = "datetime_pattern"
    const val SHOW_LOCATION = "show_location"
    const val COORD_FORMAT = "coord_format"
    const val PLACE_FORMAT = "place_format"
    const val SHOW_COORDINATES = "show_coordinates"
    const val WAIT_PRECISE = "wait_precise"
    const val PRECISE_ACCURACY = "precise_accuracy"
    const val APP_THEME = "app_theme"
    const val PLACEMENT = "placement"
    const val FRAME_BORDER = "frame_border"
    const val TEXT_SIZE = "text_size"
    /** Replaced by the schemes in G-003; read once, to carry an old choice over. */
    const val LEGACY_COLORS = "colors"
    const val STAMP_SCHEME = "stamp_scheme"
    const val OUTPUT_SIZE = "output_size"
    const val ASPECT = "aspect_ratio"
    const val DEFAULT_EV = "default_ev"
    const val SAVE_GALLERY = "save_gallery"
    const val CAPTION = "caption"
    const val QUICK_MESSENGERS = "quick_messengers"
    const val QUICK_SIDE = "quick_side"

    const val DEFAULT_PATTERN = "yyyy-MM-dd HH:mm:ss"
    const val DEFAULT_PLACEMENT = "overlay_bottom_start"
    const val DEFAULT_FRAME_BORDER = "medium"

    /** Off until the user turns it on: it sends the fix to Google (D011). */
    const val DEFAULT_PLACE_FORMAT = "off"

    /** Small by default (Owner, 2026-11-03): long edge in px; 0 means the sensor's largest. See D006. */
    const val DEFAULT_OUTPUT_SIZE = "1280"

    /** The two direct buttons the app had before the choice existed (G-001). */
    val DEFAULT_QUICK_MESSENGERS = setOf("whatsapp", "telegram")

    /** Where a single quick messenger sits beside Share; "start" or "end". Either works (D016). */
    const val DEFAULT_QUICK_SIDE = "start"

    /** Off by default: existing stamps keep their look until the user asks for the day (see D018). */
    const val DEFAULT_SHOW_WEEKDAY = false

    fun schemeBackground(id: SchemeId) = "scheme_${id.id}_background"

    fun schemeText(id: SchemeId) = "scheme_${id.id}_text"

    fun schemeOpacity(id: SchemeId) = "scheme_${id.id}_opacity"
}

/** The settings as the app uses them. */
data class AppSettings(
    val stampSpec: StampSpec,
    /** The user's own stamp line from settings; blank prints nothing. */
    val userText: String,
    val showLocation: Boolean,
    val waitForPrecise: Boolean,
    val coordinateFormat: CoordinateFormat,
    /** Null when place names are off; otherwise the stamped fix is looked up just before stamping (D011). */
    val placeFormat: PlaceFormat?,
    val outputLongEdge: Int,
    /** The photo is cropped to this shape before stamping; a frame is added around the cropped photo. */
    val aspect: AspectRatio,
    /** Exposure compensation the camera opens at, in EV; tapping the EV label returns to it. */
    val defaultEv: Float,
    /** Save a copy to the gallery when the photo is sent; the review's Save button saves regardless. */
    val saveToGallery: Boolean,
    /** The review's caption switch starts on: the stamp text goes with the photo (Owner, 2026-11-03). */
    val caption: Boolean,
    /** The quick messenger buttons, in [Messenger] order, at most [Messenger.QUICK_MAX]; not yet filtered by installed. */
    val quickMessengers: List<Messenger>,
    /** A single quick messenger sits before Share (left in left-to-right languages), else after it. */
    val quickOnStart: Boolean,
    /** The scheme new photos are stamped with; [stampSpec] uses it. */
    val scheme: SchemeId = SchemeId.DARK,
    val schemes: Map<SchemeId, StampScheme> = SchemeId.entries.associateWith(StampScheme::default),
    /** A fix within this radius counts as precise: for the marker, the wait and the stamp's mark alike. */
    val preciseWithinM: Float = FixQuality.DEFAULT_PRECISE_M,
    val appTheme: AppTheme = AppTheme.SYSTEM,
) {
    /** These settings stamping with [id] instead: the review's dark/light switch. */
    fun withScheme(id: SchemeId): AppSettings =
        copy(scheme = id, stampSpec = stampSpec.copy(style = schemes.getValue(id).style(stampSpec.style.textSizeFraction)))

    companion object {
        fun from(
            s: SettingsSource,
            labels: LocationLabels = LocationLabels(),
        ): AppSettings {
            val showLocation = s.bool(Keys.SHOW_LOCATION, true)
            val format = if (s.string(Keys.COORD_FORMAT, "decimal") == "dms") CoordinateFormat.DMS else CoordinateFormat.DECIMAL
            val fields = mutableListOf<StampField>(TextField())
            if (s.bool(Keys.SHOW_DATETIME, true)) {
                fields +=
                    DateTimeField(
                        safePattern(s.string(Keys.DATETIME_PATTERN, Keys.DEFAULT_PATTERN)),
                        weekday = s.bool(Keys.SHOW_WEEKDAY, Keys.DEFAULT_SHOW_WEEKDAY),
                    )
            }
            val placeFormat = if (showLocation) PlaceFormat.of(s.string(Keys.PLACE_FORMAT, Keys.DEFAULT_PLACE_FORMAT)) else null
            val preciseWithinM = preciseWithin(s)
            val placeField = placeFormat?.let { PlaceField(it, preciseWithinM) }
            if (placeField != null) fields += placeField
            if (showLocation) {
                // Coordinates off only drops them while a place name prints; without one they still say where (D012).
                val hidden: (StampData) -> Boolean =
                    if (placeField != null && !s.bool(Keys.SHOW_COORDINATES, true)) {
                        { placeField.lines(it).isNotEmpty() }
                    } else {
                        { false }
                    }
                fields += CoordinatesField(format, labels, hiddenWhen = hidden, preciseWithinM = preciseWithinM)
            }
            val schemes = SchemeId.entries.associateWith { scheme(s, it) }
            val scheme = SchemeId.of(s.string(Keys.STAMP_SCHEME, "")) ?: legacyScheme(s)
            val spec = StampSpec(fields, placement(s), schemes.getValue(scheme).style(textSize(s)))
            return AppSettings(
                stampSpec = spec,
                userText = s.string(Keys.STAMP_TEXT, "").trim(),
                showLocation = showLocation,
                waitForPrecise = showLocation && s.bool(Keys.WAIT_PRECISE, false),
                coordinateFormat = format,
                placeFormat = placeFormat,
                outputLongEdge = s.string(Keys.OUTPUT_SIZE, Keys.DEFAULT_OUTPUT_SIZE).toIntOrNull() ?: 1280,
                aspect = AspectRatio.of(s.string(Keys.ASPECT, AspectRatio.FULL.id)),
                defaultEv = s.string(Keys.DEFAULT_EV, "0").toFloatOrNull()?.coerceIn(-MAX_EV, MAX_EV) ?: 0f,
                saveToGallery = s.bool(Keys.SAVE_GALLERY, true),
                caption = s.bool(Keys.CAPTION, true),
                quickMessengers = s.stringSet(Keys.QUICK_MESSENGERS, Keys.DEFAULT_QUICK_MESSENGERS).let { ids ->
                    Messenger.entries.filter { it.id in ids }.take(Messenger.QUICK_MAX)
                },
                quickOnStart = s.string(Keys.QUICK_SIDE, Keys.DEFAULT_QUICK_SIDE) != "end",
                scheme = scheme,
                schemes = schemes,
                preciseWithinM = preciseWithinM,
                appTheme = AppTheme.of(s.string(Keys.APP_THEME, AppTheme.SYSTEM.id)),
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

        /**
         * Frame border width by stored id, as a fraction of the photo's short edge. The Owner found the first
         * scale too thin (2026-11-03): the old thick is now medium, and each step doubles (D013).
         */
        val FRAME_BORDERS: Map<String, Float> = mapOf("none" to 0f, "thin" to 0.025f, "medium" to 0.05f, "thick" to 0.1f)

        private const val MAX_EV = 4f

        private fun preciseWithin(s: SettingsSource): Float =
            s.string(Keys.PRECISE_ACCURACY, "").toFloatOrNull()?.takeIf { it in FixQuality.PRECISE_CHOICES_M }
                ?: FixQuality.DEFAULT_PRECISE_M

        private fun placement(s: SettingsSource): StampPlacement {
            val p = PLACEMENTS[s.string(Keys.PLACEMENT, Keys.DEFAULT_PLACEMENT)] ?: PLACEMENTS.getValue(Keys.DEFAULT_PLACEMENT)
            if (p !is FramePlacement) return p
            val border = FRAME_BORDERS[s.string(Keys.FRAME_BORDER, Keys.DEFAULT_FRAME_BORDER)] ?: FRAME_BORDERS.getValue(Keys.DEFAULT_FRAME_BORDER)
            return p.copy(borderFraction = border)
        }

        private fun textSize(s: SettingsSource) =
            when (s.string(Keys.TEXT_SIZE, "medium")) {
                "small" -> StampStyle.TEXT_SMALL
                "large" -> StampStyle.TEXT_LARGE
                else -> StampStyle.TEXT_MEDIUM
            }

        /** A stored colour that does not parse falls back to the scheme's default, never to a crash. */
        private fun scheme(
            s: SettingsSource,
            id: SchemeId,
        ): StampScheme {
            val d = legacyDefault(s, id)
            return StampScheme(
                background = StampScheme.parseColor(s.string(Keys.schemeBackground(id), "")) ?: d.background,
                text = StampScheme.parseColor(s.string(Keys.schemeText(id), "")) ?: d.text,
                overlayOpacity =
                    s
                        .string(Keys.schemeOpacity(id), "")
                        .toIntOrNull()
                        ?.coerceIn(StampScheme.MIN_OPACITY, StampScheme.MAX_OPACITY) ?: d.overlayOpacity,
            )
        }

        /**
         * What to store once so Settings shows an old colour choice as the scheme it became; empty once a
         * scheme is stored. Written before Settings first shows, so the list and the stamp agree.
         */
        fun legacySeed(s: SettingsSource): Map<String, String> {
            if (s.string(Keys.STAMP_SCHEME, "").isNotEmpty()) return emptyMap()
            val seed = mutableMapOf(Keys.STAMP_SCHEME to legacyScheme(s).id)
            if (s.string(Keys.LEGACY_COLORS, "") == "yellow_on_dark") {
                val dark = legacyDefault(s, SchemeId.DARK)
                seed[Keys.schemeText(SchemeId.DARK)] = StampScheme.formatColor(dark.text)
                seed[Keys.schemeOpacity(SchemeId.DARK)] = dark.overlayOpacity.toString()
            }
            return seed
        }

        /** The old three-way colour choice becomes the default scheme, and yellow text carries over to dark. */
        private fun legacyScheme(s: SettingsSource) =
            if (s.string(Keys.LEGACY_COLORS, "") == "dark_on_light") SchemeId.LIGHT else SchemeId.DARK

        private fun legacyDefault(
            s: SettingsSource,
            id: SchemeId,
        ): StampScheme =
            if (id == SchemeId.DARK && s.string(Keys.LEGACY_COLORS, "") == "yellow_on_dark") {
                StampScheme.DEFAULT_DARK.copy(text = YELLOW, overlayOpacity = 70)
            } else {
                StampScheme.default(id)
            }

        private const val YELLOW = 0xFFFFD600.toInt()

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
