package cz.julienika.reportcam.review

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Space
import androidx.core.view.isVisible
import cz.julienika.reportcam.R
import cz.julienika.reportcam.settings.AppSettings
import cz.julienika.reportcam.settings.ui.Look
import cz.julienika.reportcam.settings.ui.StampLooks
import cz.julienika.reportcam.stamp.SchemeId

/**
 * The review's quick stamp row (G-005): the position buttons on the left, the colour buttons on the right, icons
 * only, or icon and name when the row holds just two ([StampChoices.named]). Positions follow the review palette,
 * the one in use filled with the accent; each colour button is drawn in its own scheme (G-003). The row paints
 * itself, so the review palette skips it. Each button is tagged with [tag] of its value, for tests.
 */
class StampButtons(
    private val row: LinearLayout,
    private val onPosition: (String) -> Unit,
    private val onScheme: (SchemeId) -> Unit,
) {
    private val c = row.context
    private val density = c.resources.displayMetrics.density

    /** Whether the last [show] put any button in the row. */
    var hasButtons = false
        private set

    /** Drawn by this class, so the review palette leaves the row alone. */
    val views: Set<View> get() = setOf(row)

    /**
     * Fills the row for the photo whose look is [photo], with the choices [defaults] (Settings as they are now)
     * give. Hidden when there is nothing to choose.
     */
    fun show(
        defaults: AppSettings,
        photo: AppSettings,
        palette: ReviewPalette,
    ) {
        val choices = StampChoices.of(defaults)
        row.removeAllViews()
        hasButtons = !choices.isEmpty
        if (!hasButtons) {
            row.isVisible = false
            return
        }
        val named = choices.named
        val positions =
            choices.positions.map { id ->
                val chosen = id == photo.placementId
                button(StampLooks.POSITIONS.getValue(id), named, tag(id)) { onPosition(id) }.apply {
                    val fg = if (chosen) Color.BLACK else palette.text
                    paint(this, if (chosen) c.getColor(R.color.accent) else palette.button, fg, if (chosen) null else palette.buttonOutline, 1)
                    isSelected = chosen
                }
            }
        val schemes =
            choices.schemes.map { id ->
                val chosen = id == photo.scheme
                val scheme = photo.schemes.getValue(id)
                button(StampLooks.SCHEMES.getValue(id), named, tag(id)) { onScheme(id) }.apply {
                    paint(this, scheme.background, scheme.text, if (chosen) c.getColor(R.color.accent) else GREY, if (chosen) 3 else 1)
                    isSelected = chosen
                }
            }
        if (named) {
            for (b in positions + schemes) row.addView(b, LinearLayout.LayoutParams(0, dp(HEIGHT_DP), 1f).margins())
        } else {
            for (b in positions) row.addView(b, LinearLayout.LayoutParams(dp(ICON_W_DP), dp(HEIGHT_DP)).margins())
            row.addView(Space(c), LinearLayout.LayoutParams(0, 1, 1f))
            for (b in schemes) row.addView(b, LinearLayout.LayoutParams(dp(ICON_W_DP), dp(HEIGHT_DP)).margins())
        }
        row.isVisible = true
    }

    private fun button(
        look: Look,
        named: Boolean,
        tag: String,
        onClick: () -> Unit,
    ) = Button(c).apply {
        this.tag = tag
        isAllCaps = false
        minWidth = 0
        minHeight = 0
        minimumWidth = 0
        minimumHeight = 0
        stateListAnimator = null
        textSize = 14f
        val name = c.getString(look.label)
        if (named) {
            text = name
            setCompoundDrawablesRelativeWithIntrinsicBounds(look.icon, 0, 0, 0)
            compoundDrawablePadding = dp(4)
            setPadding(dp(10), 0, dp(10), 0)
        } else {
            // The icon alone, centred: an empty text line would push a top drawable off centre.
            contentDescription = name
            setCompoundDrawablesRelativeWithIntrinsicBounds(look.icon, 0, 0, 0)
            setPadding(dp((ICON_W_DP - ICON_DP) / 2), 0, 0, 0)
            gravity = Gravity.CENTER_VERTICAL or Gravity.START
        }
        setOnClickListener { onClick() }
    }

    private fun paint(
        b: Button,
        fill: Int,
        fg: Int,
        stroke: Int?,
        strokeDp: Int,
    ) {
        b.background =
            GradientDrawable().apply {
                cornerRadius = CORNER_DP * density
                setColor(fill)
                if (stroke != null) setStroke((strokeDp * density).toInt().coerceAtLeast(1), stroke)
            }
        b.setTextColor(fg)
        b.compoundDrawableTintList = ColorStateList.valueOf(fg)
    }

    private fun LinearLayout.LayoutParams.margins() = apply { setMargins(dp(MARGIN_DP), 0, dp(MARGIN_DP), 0) }

    private fun dp(v: Int) = (v * density + 0.5f).toInt()

    companion object {
        fun tag(position: String) = "stamp:$position"

        fun tag(scheme: SchemeId) = "stamp:scheme_${scheme.id}"

        private const val HEIGHT_DP = 40
        private const val ICON_W_DP = 40
        private const val ICON_DP = 24
        private const val MARGIN_DP = 2
        private const val CORNER_DP = 6f
        private const val GREY = 0xFF757575.toInt()
    }
}
