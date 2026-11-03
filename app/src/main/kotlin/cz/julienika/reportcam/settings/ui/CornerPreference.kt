package cz.julienika.reportcam.settings.ui

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import cz.julienika.reportcam.R

/**
 * The stamp position: a sketch of the photo with a button in each corner for an overlay (the 2×2 grid),
 * and beside it the two frame positions, text band on top or below.
 */
class CornerPreference(
    context: Context,
    attrs: AttributeSet?,
) : SegmentedPreference(context, attrs) {
    override fun content(c: Context): View {
        fun chip(
            v: String,
            label: Int,
        ) = Controls.chip(c, c.getString(label), v == value, isEnabled) { pick(v) }.apply {
            // Narrow corner buttons: "Bottom right" must fit on one line.
            textSize = 13f
            setPadding(c.dp(2), paddingTop, c.dp(2), paddingBottom)
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
                Triple("overlay_top_start", R.string.corner_top_start, Gravity.TOP or Gravity.START),
                Triple("overlay_top_end", R.string.corner_top_end, Gravity.TOP or Gravity.END),
                Triple("overlay_bottom_start", R.string.corner_bottom_start, Gravity.BOTTOM or Gravity.START),
                Triple("overlay_bottom_end", R.string.corner_bottom_end, Gravity.BOTTOM or Gravity.END),
            )
        for ((v, label, gravity) in corners) {
            photo.addView(chip(v, label), FrameLayout.LayoutParams(c.dp(CORNER_W), ViewGroup.LayoutParams.WRAP_CONTENT, gravity))
        }
        val frames =
            FrameLayout(c).apply {
                addView(chip("frame_top", R.string.frame_top), FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP))
                addView(chip("frame_bottom", R.string.frame_bottom), FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))
            }
        return LinearLayout(c).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(photo, LinearLayout.LayoutParams(0, c.dp(HEIGHT), 1.6f))
            addView(frames, LinearLayout.LayoutParams(0, c.dp(HEIGHT), 1f).apply { marginStart = c.dp(10) })
        }
    }

    private companion object {
        const val HEIGHT = 128
        const val CORNER_W = 82
    }
}
