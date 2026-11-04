package cz.julienika.reportcam.review

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import cz.julienika.reportcam.R
import cz.julienika.reportcam.databinding.ActivityCameraBinding
import cz.julienika.reportcam.settings.AppSettings
import cz.julienika.reportcam.stamp.SchemeId

/** The review's Dark and Light buttons (G-003): each drawn in its own scheme, the one in use outlined in the accent. */
class SchemeButtons(
    private val ui: ActivityCameraBinding,
    private val onPick: (SchemeId) -> Unit,
) {
    private val buttons = listOf(ui.schemeDark to SchemeId.DARK, ui.schemeLight to SchemeId.LIGHT)

    /** Drawn in their schemes, so the review palette leaves them alone. */
    val views get() = buttons.map { it.first }.toSet()

    init {
        for ((button, id) in buttons) button.setOnClickListener { onPick(id) }
    }

    fun show(s: AppSettings) {
        val density = ui.root.resources.displayMetrics.density
        val accent = ui.root.context.getColor(R.color.accent)
        for ((button, id) in buttons) {
            val scheme = s.schemes.getValue(id)
            val chosen = id == s.scheme
            button.background =
                GradientDrawable().apply {
                    cornerRadius = CORNER_DP * density
                    setColor(scheme.background)
                    setStroke(((if (chosen) 3 else 1) * density).toInt(), if (chosen) accent else GREY)
                }
            button.setTextColor(scheme.text)
            button.compoundDrawableTintList = ColorStateList.valueOf(scheme.text)
            button.isSelected = chosen
        }
    }

    private companion object {
        const val CORNER_DP = 6f
        const val GREY = 0xFF757575.toInt()
    }
}
