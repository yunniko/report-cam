package cz.julienika.reportcam.settings.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.preference.ListPreference
import androidx.preference.PreferenceViewHolder
import cz.julienika.reportcam.R

/**
 * A list setting shown as buttons under its description instead of a dialog. Keeps ListPreference's
 * entries, values, default and dependency handling; only the control differs.
 */
open class SegmentedPreference(
    context: Context,
    attrs: AttributeSet?,
) : ListPreference(context, attrs) {
    init {
        Controls.useWidgetRow(this)
    }

    // The buttons choose; the row itself opens no dialog.
    override fun onClick() = Unit

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val box = Controls.bindRow(holder)
        box.addView(content(box.context))
    }

    protected open fun content(c: Context): View =
        Controls.segmentRow(c, entries.toList(), findIndexOfValue(value), isEnabled) { pick(entryValues[it].toString()) }

    protected fun pick(v: String) {
        if (v != value && callChangeListener(v)) value = v
    }
}
