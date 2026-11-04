package cz.julienika.reportcam.share

import cz.julienika.reportcam.ui.Contrast
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MessengerTest {
    @Test
    fun eachButtonsIconAndNameAreReadableOnItsBrandColour() {
        for (m in Messenger.entries) {
            val ratio = Contrast.ratio(m.foreground, m.brandColor)
            assertTrue("$m: $ratio", ratio >= Contrast.READABLE)
        }
    }

    @Test
    fun whiteIsUsedWhereItIsReadable() {
        assertEquals(0xFFFFFFFF.toInt(), Messenger.SLACK.foreground)
        assertEquals(0xFF000000.toInt(), Messenger.SNAPCHAT.foreground)
    }

    @Test
    fun contrastMatchesTheWcagExtremes() {
        assertEquals(21.0, Contrast.ratio(0xFF000000.toInt(), 0xFFFFFFFF.toInt()), 0.01)
        assertEquals(1.0, Contrast.ratio(0xFF777777.toInt(), 0xFF777777.toInt()), 0.0)
    }
}
