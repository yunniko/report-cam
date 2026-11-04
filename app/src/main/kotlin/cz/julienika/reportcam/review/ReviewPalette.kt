package cz.julienika.reportcam.review

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.TextView

/**
 * The review controls' colours in the app theme (Owner, G-003): the panel, its text and its buttons follow
 * light or dark, while the photo and the live camera view around it stay on black. See D022.
 */
data class ReviewPalette(
    val panel: Int,
    val text: Int,
    val hint: Int,
    val button: Int,
    val buttonOutline: Int,
) {
    /**
     * Colours every text and button under [root]. Views in [skip] keep their own colours: the scheme buttons
     * are each drawn in their scheme.
     */
    fun paint(
        root: ViewGroup,
        density: Float,
        skip: Set<View>,
    ) {
        root.setBackgroundColor(panel)
        walk(root, density, skip, textColors())
    }

    private fun walk(
        group: ViewGroup,
        density: Float,
        skip: Set<View>,
        colors: ColorStateList,
    ) {
        for (i in 0 until group.childCount) {
            val v = group.getChildAt(i)
            if (v in skip) continue
            when (v) {
                is ViewGroup -> walk(v, density, skip, colors)
                // CompoundButton (the caption switch) keeps the framework's switch track; only its label is coloured.
                is Button ->
                    if (v !is CompoundButton) {
                        v.background = buttonBackground(density)
                        v.setTextColor(colors)
                        v.compoundDrawableTintList = colors
                    } else {
                        v.setTextColor(colors)
                    }
                is EditText -> {
                    v.setTextColor(colors)
                    v.setHintTextColor(hint)
                }
                is TextView -> v.setTextColor(colors)
            }
        }
    }

    /** The text colour, dimmed to [hint] while a view is disabled ("Saved to gallery", a caption switched off). */
    private fun textColors() =
        ColorStateList(
            arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()),
            intArrayOf(hint, text),
        )

    private fun buttonBackground(density: Float) =
        GradientDrawable().apply {
            cornerRadius = CORNER_DP * density
            setColor(button)
            setStroke(density.toInt().coerceAtLeast(1), buttonOutline)
        }

    companion object {
        const val CORNER_DP = 8f

        val DARK = ReviewPalette(0x99000000.toInt(), 0xFFFFFFFF.toInt(), 0xB3FFFFFF.toInt(), 0xFF2A2E33.toInt(), 0xFF4A5058.toInt())
        val LIGHT = ReviewPalette(0xEBEEF1F4.toInt(), 0xFF1B1F23.toInt(), 0x991B1F23.toInt(), 0xFFFFFFFF.toInt(), 0xFFC9CFD6.toInt())

        fun of(dark: Boolean) = if (dark) DARK else LIGHT
    }
}
