package cz.julienika.reportcam.settings.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.preference.MultiSelectListPreference
import androidx.preference.PreferenceViewHolder
import cz.julienika.reportcam.R
import cz.julienika.reportcam.share.Messenger
import cz.julienika.reportcam.share.Sharing
import cz.julienika.reportcam.share.icon

/**
 * The quick-send messengers as a grid of icon tiles to toggle. At most [Messenger.QUICK_MAX] (Owner,
 * 2026-11-03): a further tap is refused with a message, not silently dropped. A messenger that is not
 * installed can still be chosen, and says so; the review screen shows only installed ones.
 */
class MessengerPickerPreference(
    context: Context,
    attrs: AttributeSet?,
) : MultiSelectListPreference(context, attrs) {
    init {
        Controls.useWidgetRow(this)
    }

    override fun onClick() = Unit

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val box = Controls.bindRow(holder)
        box.addView(grid(box.context))
    }

    private fun grid(c: Context) =
        GridLayout(c).apply {
            columnCount = COLUMNS
            for (m in Messenger.entries) {
                addView(
                    tile(c, m),
                    GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, 1f)).apply {
                        width = 0
                        setMargins(c.dp(3), c.dp(3), c.dp(3), c.dp(3))
                    },
                )
            }
        }

    private fun tile(
        c: Context,
        m: Messenger,
    ): LinearLayout {
        val selected = m.id in values
        val installed = m.firstInstalled { Sharing.isInstalled(c, it) } != null
        return LinearLayout(c).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(c.dp(4), c.dp(8), c.dp(4), c.dp(8))
            background = Controls.chipBackground(c, selected)
            isSelected = selected
            contentDescription = m.label
            val ink = if (selected) Color.BLACK else Controls.textColor(c)
            // The icons are white, drawn for the dark review screen; tinted here to the tile's text colour.
            addView(
                ImageView(c).apply {
                    setImageResource(m.icon)
                    imageTintList = ColorStateList.valueOf(ink)
                },
                LinearLayout.LayoutParams(c.dp(28), c.dp(28)),
            )
            addView(
                TextView(c).apply {
                    text = m.label
                    textSize = 12f
                    maxLines = 1
                    gravity = Gravity.CENTER
                    setTextColor(ink)
                },
            )
            // Always laid out, so every tile is the same height.
            addView(
                TextView(c).apply {
                    setText(R.string.messenger_not_installed)
                    textSize = 10f
                    gravity = Gravity.CENTER
                    setTextColor(if (selected) Color.DKGRAY else Controls.secondaryTextColor(c))
                    visibility = if (installed) View.INVISIBLE else View.VISIBLE
                },
            )
            setOnClickListener { toggle(c, m) }
        }
    }

    private fun toggle(
        c: Context,
        m: Messenger,
    ) {
        val next = if (m.id in values) values - m.id else values + m.id
        if (next.size > Messenger.QUICK_MAX) {
            Toast.makeText(c, R.string.pref_quick_too_many, Toast.LENGTH_SHORT).show()
            return
        }
        if (callChangeListener(next)) values = next
    }

    private companion object {
        const val COLUMNS = 4
    }
}
