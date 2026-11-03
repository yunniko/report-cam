package cz.julienika.reportcam.stamp

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StampRendererTest {
    private val gray = Color.rgb(128, 128, 128)
    private val data = StampData.EMPTY.with(StampKeys.USER_TEXT, "Site 4 inspection")

    private fun photo() = Bitmap.createBitmap(1200, 900, Bitmap.Config.ARGB_8888).apply { eraseColor(gray) }

    @Test
    fun overlayDrawsInTheCornerAndLeavesTheRestAlone() {
        val src = photo()
        val out = StampRenderer.render(src, StampSpec(listOf(TextField()), OverlayPlacement(Corner.BOTTOM_START)), data)
        assertSame(src, out)
        assertEquals(1200, out.width)
        assertEquals(900, out.height)
        val g = StampLayout.compute(1200, 900, StampSpec(listOf(TextField()), OverlayPlacement(Corner.BOTTOM_START)), data, PaintTextMeasurer())!!
        val box = g.box!!
        // Inside the box, the translucent black darkens the gray.
        val inside = out.getPixel((box.left + 3).toInt(), (box.top + 3).toInt())
        assertTrue(Color.red(inside) < 100)
        // Far away, the photo is untouched.
        assertEquals(gray, out.getPixel(1150, 50))
    }

    @Test
    fun frameAddsAnOpaqueBandBelowThePhoto() {
        val out = StampRenderer.render(photo(), StampSpec(listOf(TextField()), FramePlacement(top = false)), data)
        assertEquals(1200, out.width)
        assertTrue(out.height > 900)
        assertEquals(gray, out.getPixel(600, 450))
        val band = out.getPixel(1195, out.height - 2)
        assertEquals(255, Color.alpha(band))
        assertNotEquals(gray, band)
    }

    @Test
    fun textIsActuallyDrawn() {
        val out = StampRenderer.render(photo(), StampSpec(listOf(TextField()), FramePlacement(top = false)), data)
        var white = 0
        for (y in 900 until out.height) for (x in 0 until 600) if (Color.red(out.getPixel(x, y)) > 200) white++
        assertTrue("expected white text pixels in the band, found $white", white > 200)
    }
}
