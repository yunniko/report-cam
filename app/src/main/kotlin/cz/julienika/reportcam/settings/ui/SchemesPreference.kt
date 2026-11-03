package cz.julienika.reportcam.settings.ui

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import cz.julienika.reportcam.R
import cz.julienika.reportcam.settings.AppSettings
import cz.julienika.reportcam.settings.PrefsSource
import cz.julienika.reportcam.stamp.SchemeId

/**
 * Both stamp schemes side by side, each drawn in its own colours on the preview photo (Owner, G-003).
 * The radio button picks the default for new photos; the card or "Edit colours" opens [SchemeDialog].
 * Stores the default's id under its key (`stamp_scheme`).
 */
class SchemesPreference(
    context: Context,
    attrs: AttributeSet?,
) : Preference(context, attrs) {
    init {
        Controls.useWidgetRow(this)
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val box = Controls.bindRow(holder)
        val c = box.context
        val settings = AppSettings.from(PrefsSource(sharedPreferences!!))
        box.addView(
            LinearLayout(c).apply {
                orientation = LinearLayout.HORIZONTAL
                SchemeId.entries.forEachIndexed { i, id ->
                    val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    if (i > 0) lp.marginStart = c.dp(10)
                    addView(card(c, settings, id), lp)
                }
            },
        )
    }

    private fun card(
        c: Context,
        settings: AppSettings,
        id: SchemeId,
    ): LinearLayout {
        val name = id.label(c)
        val isDefault = settings.scheme == id
        val edit = { SchemeDialog.show(c, sharedPreferences!!, id) { notifyChanged() } }
        return LinearLayout(c).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(c.dp(6), c.dp(6), c.dp(6), c.dp(4))
            background =
                GradientDrawable().apply {
                    cornerRadius = c.dp(10).toFloat()
                    setStroke(c.dp(if (isDefault) 2 else 1), if (isDefault) Controls.accent(c) else Controls.outline(c))
                }
            addView(
                ImageView(c).apply {
                    adjustViewBounds = true
                    setImageBitmap(StampPreview.overlay(c, settings.withScheme(id)))
                    contentDescription = c.getString(R.string.scheme_preview_of, name)
                    setOnClickListener { edit() }
                },
            )
            addView(
                RadioButton(c).apply {
                    text = name
                    isChecked = isDefault
                    isEnabled = this@SchemesPreference.isEnabled
                    setOnClickListener {
                        if (!isDefault && callChangeListener(id.id)) {
                            persistString(id.id)
                            notifyChanged()
                        } else {
                            isChecked = isDefault
                        }
                    }
                },
            )
            addView(
                TextView(c).apply {
                    setText(R.string.scheme_edit)
                    setTextColor(Controls.link(c))
                    setPadding(c.dp(4), c.dp(2), c.dp(4), c.dp(8))
                    setOnClickListener { edit() }
                },
            )
        }
    }
}
