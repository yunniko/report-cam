package cz.julienika.reportcam.review

import cz.julienika.reportcam.settings.AppSettings
import cz.julienika.reportcam.stamp.SchemeId

/**
 * The quick stamp buttons the review shows (G-005): positions on the left, colour schemes on the right, from the
 * buttons ticked in Settings and the Settings defaults. The Owner's rules, per group: nothing ticked, or only the
 * default ticked, shows no group; one other button ticked shows it with the default, so a shown group always
 * offers a choice. Two buttons in all show their names and fill the row ([named]).
 */
data class StampChoices(
    val positions: List<String>,
    val schemes: List<SchemeId>,
) {
    val isEmpty: Boolean get() = positions.isEmpty() && schemes.isEmpty()

    val named: Boolean get() = positions.size + schemes.size == 2

    companion object {
        /** The positions in the order the row shows them: the corners as they sit on a photo, then the frames. */
        val POSITION_ORDER =
            listOf("overlay_top_start", "overlay_top_end", "overlay_bottom_start", "overlay_bottom_end", "frame_top", "frame_bottom")

        /** [defaults] are the settings as they are in Settings now, not the look of the photo under review. */
        fun of(defaults: AppSettings): StampChoices =
            StampChoices(
                positions = group(POSITION_ORDER, defaults.quickPositions, defaults.placementId),
                schemes = group(SchemeId.entries, defaults.quickSchemes, defaults.scheme),
            )

        internal fun <T> group(
            order: List<T>,
            ticked: Set<T>,
            default: T,
        ): List<T> {
            val shown =
                when {
                    ticked.isEmpty() || ticked == setOf(default) -> emptySet()
                    ticked.size == 1 -> ticked + default
                    else -> ticked
                }
            return order.filter { it in shown }
        }
    }
}
