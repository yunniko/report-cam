package cz.julienika.reportcam.stamp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StampSchemeTest {
    @Test
    fun opacityBecomesTheBoxAlphaAndTextStaysOpaque() {
        val style = StampScheme(0xFF102030.toInt(), 0x00FFD600, 50).style(0.03f)
        assertEquals(0x80102030.toInt(), style.backgroundColor)
        assertEquals(0xFFFFD600.toInt(), style.textColor)
        assertEquals(0.03f, style.textSizeFraction, 0f)
    }

    @Test
    fun opacityIsClampedToItsRange() {
        assertEquals(0xFF, StampScheme(0, 0, 150).style(0.03f).backgroundColor ushr 24)
        assertEquals((StampScheme.MIN_OPACITY * 255 + 50) / 100, StampScheme(0, 0, 0).style(0.03f).backgroundColor ushr 24)
    }

    @Test
    fun colorsParseOnlyAsHashAndSixHexDigits() {
        assertEquals(0xFFFFD600.toInt(), StampScheme.parseColor("#ffd600"))
        for (bad in listOf("", "FFD600", "#FFD60", "#FFD6000", "#-12345", "#GGGGGG")) assertNull(bad, StampScheme.parseColor(bad))
        assertEquals("#0D2B45", StampScheme.formatColor(0xFF0D2B45.toInt()))
    }

    @Test
    fun theDefaultsAreInThePalettesAndThePalettesAreOpaque() {
        for (id in SchemeId.entries) {
            val d = StampScheme.default(id)
            assertTrue(d.background in StampScheme.BACKGROUNDS)
            assertTrue(d.text in StampScheme.TEXTS)
        }
        assertTrue((StampScheme.BACKGROUNDS + StampScheme.TEXTS).all { it ushr 24 == 0xFF })
        assertEquals(SchemeId.LIGHT, SchemeId.DARK.other)
    }
}
