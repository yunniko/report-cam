package cz.julienika.reportcam.settings.ui

import cz.julienika.reportcam.R
import cz.julienika.reportcam.stamp.SchemeId

/** A stamp choice as a button shows it: its icon, and its name (the button's text, or its TalkBack label). */
data class Look(
    val icon: Int,
    val label: Int,
)

/** The one table of how stamp positions and colour schemes look on buttons, for Settings and the review alike. */
object StampLooks {
    /** By placement id ([cz.julienika.reportcam.settings.AppSettings.PLACEMENTS]). */
    val POSITIONS: Map<String, Look> =
        mapOf(
            "overlay_top_start" to Look(R.drawable.ic_pos_top_start, R.string.corner_top_start),
            "overlay_top_end" to Look(R.drawable.ic_pos_top_end, R.string.corner_top_end),
            "overlay_bottom_start" to Look(R.drawable.ic_pos_bottom_start, R.string.corner_bottom_start),
            "overlay_bottom_end" to Look(R.drawable.ic_pos_bottom_end, R.string.corner_bottom_end),
            "frame_top" to Look(R.drawable.ic_frame_top, R.string.frame_top),
            "frame_bottom" to Look(R.drawable.ic_frame_bottom, R.string.frame_bottom),
        )

    val SCHEMES: Map<SchemeId, Look> =
        mapOf(
            SchemeId.DARK to Look(R.drawable.ic_scheme_dark, R.string.scheme_dark),
            SchemeId.LIGHT to Look(R.drawable.ic_scheme_light, R.string.scheme_light),
        )
}
