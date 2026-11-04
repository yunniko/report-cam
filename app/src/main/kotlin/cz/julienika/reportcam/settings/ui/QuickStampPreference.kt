package cz.julienika.reportcam.settings.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.widget.GridLayout
import android.widget.ImageView
import androidx.preference.MultiSelectListPreference
import androidx.preference.PreferenceViewHolder
import cz.julienika.reportcam.review.StampChoices
import cz.julienika.reportcam.settings.Keys
import cz.julienika.reportcam.stamp.SchemeId

/**
 * The quick stamp buttons the review offers (G-005), each ticked separately: an icon tile per position for
 * [Keys.QUICK_POSITIONS], per colour scheme for [Keys.QUICK_SCHEMES]. The key chooses which; the rule for what
 * the review then shows is [StampChoices].
 */
class QuickStampPreference(
    context: Context,
    attrs: AttributeSet?,
) : MultiSelectListPreference(context, attrs) {
    init {
        Controls.useWidgetRow(this)
    }

    override fun onClick() = Unit

    /** The stored ids and their looks, in the review row's order. */
    private val looks: List<Pair<String, Look>>
        get() =
            if (key == Keys.QUICK_SCHEMES) {
                SchemeId.entries.map { it.id to StampLooks.SCHEMES.getValue(it) }
            } else {
                StampChoices.POSITION_ORDER.map { it to StampLooks.POSITIONS.getValue(it) }
            }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val box = Controls.bindRow(holder)
        val c = box.context
        box.addView(
            GridLayout(c).apply {
                columnCount = looks.size
                for ((id, look) in looks) {
                    addView(
                        tile(c, id, look),
                        GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, 1f)).apply {
                            width = 0
                            setMargins(c.dp(3), c.dp(3), c.dp(3), c.dp(3))
                        },
                    )
                }
            },
        )
    }

    private fun tile(
        c: Context,
        id: String,
        look: Look,
    ) = ImageView(c).apply {
        val ticked = id in values
        tag = id
        setImageResource(look.icon)
        imageTintList = ColorStateList.valueOf(if (ticked) Color.BLACK else Controls.textColor(c))
        background = Controls.chipBackground(c, ticked)
        isSelected = ticked
        isEnabled = this@QuickStampPreference.isEnabled
        contentDescription = c.getString(look.label)
        setPadding(c.dp(8), c.dp(8), c.dp(8), c.dp(8))
        minimumHeight = c.dp(TILE_DP)
        setOnClickListener {
            val next = if (id in values) values - id else values + id
            if (callChangeListener(next)) values = next
        }
    }

    private companion object {
        const val TILE_DP = 44
    }
}
