package cz.julienika.reportcam.settings.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import cz.julienika.reportcam.R
import cz.julienika.reportcam.stamp.SchemeId

internal fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

internal fun SchemeId.label(c: Context): String = c.getString(if (this == SchemeId.DARK) R.string.scheme_dark else R.string.scheme_light)

/** The settings screen's own controls, built in code: the app uses no Material Components (G-001). */
internal object Controls {
    fun accent(c: Context) = c.getColor(R.color.accent)

    fun outline(c: Context) = c.getColor(R.color.settings_outline)

    /** Link text: the accent is too pale on a light card, so each theme has its own. */
    fun link(c: Context) = c.getColor(R.color.settings_link)

    private fun themeColors(
        c: Context,
        attr: Int,
    ): ColorStateList? {
        val a = c.obtainStyledAttributes(intArrayOf(attr))
        return a.getColorStateList(0).also { a.recycle() }
    }

    fun textColor(c: Context): Int = themeColors(c, android.R.attr.textColorPrimary)?.defaultColor ?: Color.BLACK

    fun secondaryTextColor(c: Context): Int = themeColors(c, android.R.attr.textColorSecondary)?.defaultColor ?: Color.GRAY

    /** A row whose control sits under its title and description (`pref_widget_row`); the row itself is not a button. */
    fun useWidgetRow(p: Preference) {
        p.layoutResource = R.layout.pref_widget_row
        p.isSelectable = false
    }

    /**
     * Binds a [useWidgetRow] row and returns the empty box for its control. The library greys the title of a
     * row that is not selectable; these rows are controls, so the title keeps the theme colour (with its
     * disabled state, so a dependent row still greys out).
     */
    fun bindRow(holder: PreferenceViewHolder): FrameLayout {
        val c = holder.itemView.context
        themeColors(c, android.R.attr.textColorPrimary)?.let { (holder.findViewById(android.R.id.title) as? TextView)?.setTextColor(it) }
        return (holder.findViewById(R.id.pref_content) as FrameLayout).apply { removeAllViews() }
    }

    /** Filled with the accent when chosen, outlined otherwise. */
    fun chipBackground(
        c: Context,
        selected: Boolean,
    ) = GradientDrawable().apply {
        cornerRadius = c.dp(8).toFloat()
        if (selected) {
            setColor(accent(c))
        } else {
            setColor(Color.TRANSPARENT)
            setStroke(c.dp(1), outline(c))
        }
    }

    fun chip(
        c: Context,
        label: CharSequence,
        selected: Boolean,
        enabled: Boolean,
        onClick: () -> Unit,
    ): TextView =
        TextView(c).apply {
            text = label
            gravity = Gravity.CENTER
            textSize = 14f
            maxLines = 2
            minHeight = c.dp(40)
            setPadding(c.dp(6), c.dp(6), c.dp(6), c.dp(6))
            background = chipBackground(c, selected)
            // Dark text on the amber accent in both themes.
            setTextColor(if (selected) Color.BLACK else textColor(c))
            if (selected) setTypeface(typeface, Typeface.BOLD)
            isSelected = selected
            isEnabled = enabled
            alpha = if (enabled) 1f else DISABLED_ALPHA
            setOnClickListener { onClick() }
        }

    /** Equal-width chips in one row; [selected] -1 shows none chosen. */
    fun segmentRow(
        c: Context,
        labels: List<CharSequence>,
        selected: Int,
        enabled: Boolean,
        onPick: (Int) -> Unit,
    ): LinearLayout =
        LinearLayout(c).apply {
            orientation = LinearLayout.HORIZONTAL
            labels.forEachIndexed { i, label ->
                val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                if (i > 0) lp.marginStart = c.dp(6)
                addView(chip(c, label, i == selected, enabled) { onPick(i) }, lp)
            }
        }

    private const val DISABLED_ALPHA = 0.4f
}
