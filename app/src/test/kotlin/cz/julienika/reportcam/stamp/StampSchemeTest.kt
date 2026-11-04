package cz.julienika.reportcam.stamp

import cz.julienika.reportcam.ui.Contrast
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
    fun theDefaultsAreInTheirSchemesPalettesAndThePalettesAreOpaque() {
        for (id in SchemeId.entries) {
            val d = StampScheme.default(id)
            assertTrue(d.background in StampScheme.backgrounds(id))
            assertTrue(d.text in StampScheme.texts(id))
            assertTrue((StampScheme.backgrounds(id) + StampScheme.texts(id)).all { it ushr 24 == 0xFF })
        }
        assertEquals(SchemeId.LIGHT, SchemeId.DARK.other)
    }

    @Test
    fun everyBackgroundAndTextPairingInASchemeIsReadable() {
        for (id in SchemeId.entries) {
            for (bg in StampScheme.backgrounds(id)) {
                for (fg in StampScheme.texts(id)) {
                    val ratio = Contrast.ratio(bg, fg)
                    assertTrue("$id ${StampScheme.formatColor(fg)} on ${StampScheme.formatColor(bg)}: $ratio", ratio >= Contrast.READABLE)
                }
            }
        }
    }

    @Test
    fun theDarkSchemeHasDarkBackgroundsAndTheLightSchemeLightOnes() {
        val darkest = StampScheme.texts(SchemeId.DARK).minOf { Contrast.luminance(it) }
        assertTrue(StampScheme.backgrounds(SchemeId.DARK).all { Contrast.luminance(it) < darkest })
        val lightest = StampScheme.texts(SchemeId.LIGHT).maxOf { Contrast.luminance(it) }
        assertTrue(StampScheme.backgrounds(SchemeId.LIGHT).all { Contrast.luminance(it) > lightest })
    }
}
