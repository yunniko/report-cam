package cz.julienika.reportcam.review

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import androidx.core.view.isVisible
import cz.julienika.reportcam.databinding.ActivityCameraBinding
import cz.julienika.reportcam.settings.AppSettings
import cz.julienika.reportcam.share.Messenger
import cz.julienika.reportcam.share.icon

/**
 * The quick messenger buttons of the review screen, placed by [ShareButtons] and drawn in their brands
 * (D023). [isInstalled] says whether a messenger can be sent to; [onPick] sends to it.
 */
class QuickButtons(
    private val ui: ActivityCameraBinding,
    private val density: Float,
    private val isInstalled: (Messenger) -> Boolean,
    private val onPick: (Messenger) -> Unit,
) {
    /** Drawn in their brand colours, so the review palette leaves them alone. */
    val views: Set<View> get() = setOf(ui.quick1, ui.quick2, ui.quick3, ui.quickStart, ui.quickEnd)

    fun show(s: AppSettings) {
        val rows = ShareButtons.arrange(s.quickMessengers, s.quickOnStart, isInstalled)
        ui.quickRow.isVisible = rows.ownRow.isNotEmpty()
        listOf(ui.quick1, ui.quick2, ui.quick3).forEachIndexed { i, b -> bind(b, rows.ownRow.getOrNull(i)) }
        bindIcon(ui.quickStart, rows.beside?.takeIf { rows.besideOnStart })
        bindIcon(ui.quickEnd, rows.beside?.takeIf { !rows.besideOnStart })
    }

    fun hide() {
        ui.quickRow.isVisible = false
        ui.quickStart.isVisible = false
        ui.quickEnd.isVisible = false
    }

    /** A quick button in its own row: icon and name on the messenger's colour. */
    private fun bind(
        button: Button,
        messenger: Messenger?,
    ) {
        button.isVisible = messenger != null
        if (messenger == null) return
        button.text = messenger.label
        button.setCompoundDrawablesRelativeWithIntrinsicBounds(messenger.icon, 0, 0, 0)
        button.compoundDrawableTintList = ColorStateList.valueOf(messenger.foreground)
        button.setTextColor(messenger.foreground)
        button.background = brandBackground(messenger)
        button.setOnClickListener { onPick(messenger) }
    }

    /** The single quick button beside Share: the icon alone (Owner, G-003 phone test), named for TalkBack. */
    private fun bindIcon(
        button: ImageButton,
        messenger: Messenger?,
    ) {
        button.isVisible = messenger != null
        if (messenger == null) return
        button.setImageResource(messenger.icon)
        button.imageTintList = ColorStateList.valueOf(messenger.foreground)
        button.contentDescription = messenger.label
        button.background = brandBackground(messenger)
        button.setOnClickListener { onPick(messenger) }
    }

    private fun brandBackground(m: Messenger) =
        GradientDrawable().apply {
            cornerRadius = ReviewPalette.CORNER_DP * density
            setColor(m.brandColor)
        }
}
