package cz.julienika.reportcam.review

import cz.julienika.reportcam.share.Messenger

/**
 * Where the quick messenger buttons go on the review screen (Owner, 2026-11-03): one shares the row with
 * the general Share button, on the side the user picked; two or three get their own row above it.
 */
data class ShareRows(
    /** The row above Share, evenly spaced; empty when there are fewer than two. */
    val ownRow: List<Messenger>,
    /** The single messenger beside Share, or null. */
    val beside: Messenger?,
    val besideOnStart: Boolean,
)

object ShareButtons {
    /** [chosen] in settings order; only installed ones count, so a missing app never leaves a dead button. */
    fun arrange(
        chosen: List<Messenger>,
        besideOnStart: Boolean,
        isInstalled: (Messenger) -> Boolean,
    ): ShareRows {
        val shown = chosen.distinct().filter(isInstalled).take(Messenger.QUICK_MAX)
        return if (shown.size == 1) {
            ShareRows(emptyList(), shown.single(), besideOnStart)
        } else {
            ShareRows(shown, null, besideOnStart)
        }
    }
}
