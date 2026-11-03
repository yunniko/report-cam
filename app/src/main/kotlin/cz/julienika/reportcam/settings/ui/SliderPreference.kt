package cz.julienika.reportcam.settings.ui

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

/** A list setting with ordered values (exposure steps, accuracy radius) as a slider; stored when the thumb is let go. */
class SliderPreference(
    context: Context,
    attrs: AttributeSet?,
) : SegmentedPreference(context, attrs) {
    override fun content(c: Context): View {
        val label =
            TextView(c).apply {
                textSize = 16f
                setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
                minWidth = c.dp(72)
            }
        val bar =
            SeekBar(c).apply {
                max = entries.size - 1
                progress = findIndexOfValue(value).coerceAtLeast(0)
                isEnabled = this@SliderPreference.isEnabled
                contentDescription = title
            }
        label.text = entries[bar.progress]
        bar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    b: SeekBar,
                    p: Int,
                    fromUser: Boolean,
                ) {
                    label.text = entries[p]
                }

                override fun onStartTrackingTouch(b: SeekBar) = Unit

                override fun onStopTrackingTouch(b: SeekBar) = pick(entryValues[b.progress].toString())
            },
        )
        return LinearLayout(c).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(bar, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(label, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
    }
}
