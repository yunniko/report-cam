package cz.julienika.reportcam.settings.ui

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout

/**
 * The stamp position: a sketch of the photo with a button in each corner for an overlay (the 2×2 grid),
 * and beside it the two frame positions, text band on top or below.
 */
class CornerPreference(
    context: Context,
    attrs: AttributeSet?,
) : SegmentedPreference(context, attrs) {
    override fun content(c: Context): View {
        // Each button shows its position as an icon in the chip's text colour. A corner shows only the icon and
        // keeps its name for TalkBack; a frame keeps its label beside the icon.
        fun chip(
            v: String,
            iconOnly: Boolean,
        ): View {
            val look = StampLooks.POSITIONS.getValue(v)
            return Controls.chip(c, if (iconOnly) "" else c.getString(look.label), v == value, isEnabled) { pick(v) }.apply {
            val d = requireNotNull(c.getDrawable(look.icon)).mutate().apply { setTint(currentTextColor) }
            if (iconOnly) {
                contentDescription = c.getString(look.label)
                // Drawn on the background, so the icon is centred in the chip rather than above an empty line.
                background = LayerDrawable(arrayOf(background, d)).apply { setLayerGravity(1, Gravity.CENTER) }
            } else {
                setCompoundDrawablesRelativeWithIntrinsicBounds(d, null, null, null)
                compoundDrawablePadding = c.dp(4)
            }
        }
        }

        val photo =
            FrameLayout(c).apply {
                background =
                    GradientDrawable().apply {
                        cornerRadius = c.dp(6).toFloat()
                        setStroke(c.dp(1), Controls.outline(c))
                    }
                setPadding(c.dp(6), c.dp(6), c.dp(6), c.dp(6))
            }
        val corners =
            listOf(
                "overlay_top_start" to (Gravity.TOP or Gravity.START),
                "overlay_top_end" to (Gravity.TOP or Gravity.END),
                "overlay_bottom_start" to (Gravity.BOTTOM or Gravity.START),
                "overlay_bottom_end" to (Gravity.BOTTOM or Gravity.END),
            )
        for ((id, gravity) in corners) {
            val lp = FrameLayout.LayoutParams(c.dp(CORNER_W), ViewGroup.LayoutParams.WRAP_CONTENT, gravity)
            photo.addView(chip(id, iconOnly = true), lp)
        }
        val frames =
            FrameLayout(c).apply {
                addView(chip("frame_top", iconOnly = false), FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP))
                addView(chip("frame_bottom", iconOnly = false), FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))
            }
        return LinearLayout(c).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(photo, LinearLayout.LayoutParams(0, c.dp(HEIGHT), 1.6f))
            addView(frames, LinearLayout.LayoutParams(0, c.dp(HEIGHT), 1f).apply { marginStart = c.dp(10) })
        }
    }

    private companion object {
        const val HEIGHT = 128
        const val CORNER_W = 64
    }
}
