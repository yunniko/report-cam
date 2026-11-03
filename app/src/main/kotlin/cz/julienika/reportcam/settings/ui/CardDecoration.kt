package cz.julienika.reportcam.settings.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import cz.julienika.reportcam.R

/**
 * Draws each settings group as one rounded card: the rows between two category headers share a
 * background, with a thin line between rows. [headers] says, per adapter position, whether the row is a
 * category title; it is asked again whenever the adapter reports a change, so rows that appear or hide
 * are followed.
 */
internal class CardDecoration(
    c: Context,
    private val headers: () -> BooleanArray,
) : RecyclerView.ItemDecoration() {
    private val margin = c.dp(12)
    private val gap = c.dp(8)
    private val radius = c.dp(14).toFloat()
    private val card = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = c.getColor(R.color.settings_card) }
    private val line =
        Paint().apply {
            color = c.getColor(R.color.settings_outline)
            strokeWidth = c.resources.displayMetrics.density
        }
    private val path = Path()
    private val rect = RectF()
    private val radii = FloatArray(8)

    private var cached: BooleanArray? = null
    private var observed: RecyclerView.Adapter<*>? = null
    private val invalidate =
        object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                cached = null
            }

            override fun onItemRangeChanged(
                start: Int,
                count: Int,
            ) = onChanged()

            override fun onItemRangeInserted(
                start: Int,
                count: Int,
            ) = onChanged()

            override fun onItemRangeRemoved(
                start: Int,
                count: Int,
            ) = onChanged()

            override fun onItemRangeMoved(
                from: Int,
                to: Int,
                count: Int,
            ) = onChanged()
        }

    private fun headerRows(parent: RecyclerView): BooleanArray {
        val adapter = parent.adapter
        if (adapter !== observed) {
            observed?.unregisterAdapterDataObserver(invalidate)
            adapter?.registerAdapterDataObserver(invalidate)
            observed = adapter
            cached = null
        }
        return cached ?: headers().also { cached = it }
    }

    private fun inCard(
        rows: BooleanArray,
        pos: Int,
        count: Int,
    ) = pos in 0 until count && !rows.getOrElse(pos) { false }

    override fun getItemOffsets(
        out: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        val pos = parent.getChildAdapterPosition(view)
        val count = parent.adapter?.itemCount ?: 0
        val rows = headerRows(parent)
        if (!inCard(rows, pos, count)) return
        out.left = margin
        out.right = margin
        if (!inCard(rows, pos + 1, count)) out.bottom = gap
    }

    override fun onDraw(
        canvas: Canvas,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        val count = parent.adapter?.itemCount ?: return
        val rows = headerRows(parent)
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            val pos = parent.getChildAdapterPosition(child)
            if (!inCard(rows, pos, count)) continue
            val first = !inCard(rows, pos - 1, count)
            val last = !inCard(rows, pos + 1, count)
            val top = child.top + child.translationY
            rect.set(child.left.toFloat(), top, child.right.toFloat(), top + child.height)
            val t = if (first) radius else 0f
            val b = if (last) radius else 0f
            radii.fill(t, 0, 4)
            radii.fill(b, 4, 8)
            path.reset()
            path.addRoundRect(rect, radii, Path.Direction.CW)
            canvas.drawPath(path, card)
            if (!first) canvas.drawLine(rect.left + margin, top, rect.right - margin, top, line)
        }
    }
}
