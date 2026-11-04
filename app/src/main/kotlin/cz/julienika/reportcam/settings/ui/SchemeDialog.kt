package cz.julienika.reportcam.settings.ui

import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import cz.julienika.reportcam.R
import cz.julienika.reportcam.settings.AppSettings
import cz.julienika.reportcam.settings.Keys
import cz.julienika.reportcam.settings.PrefsSource
import cz.julienika.reportcam.stamp.SchemeId
import cz.julienika.reportcam.stamp.StampScheme

/**
 * One scheme's colours (Owner, G-003): background and text from the palettes, the overlay's opacity, and two
 * live previews, the overlay on a photo and the frame on its solid background. Saved only on Save.
 */
internal object SchemeDialog {
    private const val OPACITY_STEP = 5

    fun show(
        c: Context,
        prefs: SharedPreferences,
        id: SchemeId,
        onSaved: () -> Unit,
    ): AlertDialog {
        val base = AppSettings.from(PrefsSource(prefs))
        var scheme = base.schemes.getValue(id)

        val overlayView = preview(c, R.string.scheme_preview_overlay)
        val frameView = preview(c, R.string.scheme_preview_frame)
        val backgrounds = GridLayout(c).apply { columnCount = StampScheme.backgrounds(id).size }
        val texts = GridLayout(c).apply { columnCount = StampScheme.texts(id).size }
        val opacityLabel = label(c, "")
        val opacity =
            SeekBar(c).apply {
                max = (StampScheme.MAX_OPACITY - StampScheme.MIN_OPACITY) / OPACITY_STEP
                contentDescription = c.getString(R.string.scheme_opacity_label)
            }

        fun refresh() {
            val s = base.copy(schemes = base.schemes + (id to scheme)).withScheme(id)
            overlayView.setImageBitmap(StampPreview.overlay(c, s))
            frameView.setImageBitmap(StampPreview.frame(c, s))
            fill(backgrounds, StampScheme.backgrounds(id), scheme.background) {
                scheme = scheme.copy(background = it)
                refresh()
            }
            fill(texts, StampScheme.texts(id), scheme.text) {
                scheme = scheme.copy(text = it)
                refresh()
            }
            opacity.progress = (scheme.overlayOpacity - StampScheme.MIN_OPACITY) / OPACITY_STEP
            opacityLabel.text = c.getString(R.string.scheme_opacity, scheme.overlayOpacity)
        }
        opacity.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    b: SeekBar,
                    p: Int,
                    fromUser: Boolean,
                ) {
                    if (!fromUser) return
                    scheme = scheme.copy(overlayOpacity = StampScheme.MIN_OPACITY + p * OPACITY_STEP)
                    refresh()
                }

                override fun onStartTrackingTouch(b: SeekBar) = Unit

                override fun onStopTrackingTouch(b: SeekBar) = Unit
            },
        )

        val content =
            LinearLayout(c).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(c.dp(20), c.dp(8), c.dp(20), c.dp(8))
                // Side by side, so the swatches stay on screen with the previews.
                addView(
                    LinearLayout(c).apply {
                        orientation = LinearLayout.HORIZONTAL
                        addView(column(c, R.string.scheme_preview_overlay, overlayView), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                        addView(
                            column(c, R.string.scheme_preview_frame, frameView),
                            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = c.dp(10) },
                        )
                    },
                )
                addView(label(c, c.getString(R.string.scheme_background)))
                addView(backgrounds)
                addView(label(c, c.getString(R.string.scheme_text)))
                addView(texts)
                addView(opacityLabel)
                addView(opacity)
                addView(
                    TextView(c).apply {
                        setText(R.string.scheme_opacity_note)
                        setTextColor(Controls.secondaryTextColor(c))
                    },
                )
            }
        refresh()
        val name = id.label(c)
        val dialog =
            AlertDialog
                .Builder(c)
                .setTitle(c.getString(R.string.scheme_dialog_title, name))
                .setView(ScrollView(c).apply { addView(content) })
                .setPositiveButton(R.string.scheme_save) { _, _ ->
                    prefs
                        .edit()
                        .putString(Keys.schemeBackground(id), StampScheme.formatColor(scheme.background))
                        .putString(Keys.schemeText(id), StampScheme.formatColor(scheme.text))
                        .putString(Keys.schemeOpacity(id), scheme.overlayOpacity.toString())
                        .apply()
                    onSaved()
                }.setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.scheme_reset, null)
                .show()
        // Reset only resets the dialog; Save still decides.
        dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener {
            scheme = StampScheme.default(id)
            refresh()
        }
        return dialog
    }

    private fun preview(
        c: Context,
        description: Int,
    ) = ImageView(c).apply {
        adjustViewBounds = true
        scaleType = ImageView.ScaleType.FIT_CENTER
        contentDescription = c.getString(description)
    }

    private fun column(
        c: Context,
        title: Int,
        image: ImageView,
    ) = LinearLayout(c).apply {
        orientation = LinearLayout.VERTICAL
        addView(label(c, c.getString(title)))
        addView(image)
    }

    private fun label(
        c: Context,
        text: String,
    ) = TextView(c).apply {
        this.text = text
        textSize = 14f
        setTextColor(Controls.textColor(c))
        setPadding(0, c.dp(12), 0, c.dp(6))
    }

    /** One round swatch per colour; the chosen one has an accent ring. */
    private fun fill(
        grid: GridLayout,
        colors: List<Int>,
        chosen: Int,
        onPick: (Int) -> Unit,
    ) {
        val c = grid.context
        grid.removeAllViews()
        for (color in colors) {
            val selected = color or OPAQUE == chosen or OPAQUE
            val swatch =
                View(c).apply {
                    background =
                        GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(color)
                            if (selected) setStroke(c.dp(4), Controls.accent(c)) else setStroke(c.dp(1), Color.GRAY)
                        }
                    isSelected = selected
                    contentDescription = StampScheme.formatColor(color)
                    setOnClickListener { onPick(color) }
                }
            val size = c.dp(SWATCH)
            grid.addView(
                swatch,
                GridLayout.LayoutParams().apply {
                    width = size
                    height = size
                    setMargins(c.dp(5), c.dp(5), c.dp(5), c.dp(5))
                },
            )
        }
    }

    private const val OPAQUE = 0xFF000000.toInt()
    private const val SWATCH = 36
}
