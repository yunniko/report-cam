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
    fun frameBorderSurroundsThePhotoInTheBandColour() {
        // Border 5% of the 900 px short edge = 45 px on the left, right and top.
        val out = StampRenderer.render(photo(), StampSpec(listOf(TextField()), FramePlacement(false, 0.05f)), data)
        assertEquals(1200 + 90, out.width)
        val band = out.getPixel(out.width - 2, out.height - 2)
        for ((x, y) in listOf(2 to 2, out.width - 3 to 450, 20 to 450, 600 to 20)) {
            assertEquals("border at $x,$y", band, out.getPixel(x, y))
        }
        assertEquals(255, Color.alpha(band))
        assertEquals(gray, out.getPixel(45, 45))
        assertEquals(gray, out.getPixel(45 + 1199, 45 + 899))
    }

    @Test
    fun textIsActuallyDrawn() {
        val out = StampRenderer.render(photo(), StampSpec(listOf(TextField()), FramePlacement(top = false)), data)
        var white = 0
        for (y in 900 until out.height) for (x in 0 until 600) if (Color.red(out.getPixel(x, y)) > 200) white++
        assertTrue("expected white text pixels in the band, found $white", white > 200)
    }
}
