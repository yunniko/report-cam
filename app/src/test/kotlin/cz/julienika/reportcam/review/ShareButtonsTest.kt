package cz.julienika.reportcam.review

import cz.julienika.reportcam.share.Messenger
import cz.julienika.reportcam.share.Messenger.DISCORD
import cz.julienika.reportcam.share.Messenger.INSTAGRAM
import cz.julienika.reportcam.share.Messenger.SLACK
import cz.julienika.reportcam.share.Messenger.TELEGRAM
import cz.julienika.reportcam.share.Messenger.WHATSAPP
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShareButtonsTest {
    private fun arrange(
        chosen: List<Messenger>,
        installed: Set<Messenger> = Messenger.entries.toSet(),
        onStart: Boolean = true,
    ) = ShareButtons.arrange(chosen, onStart) { it in installed }

    @Test
    fun noneChosenLeavesShareAlone() {
        assertEquals(ShareRows(emptyList(), null, true), arrange(emptyList()))
    }

    @Test
    fun oneSitsBesideShareOnTheChosenSide() {
        assertEquals(ShareRows(emptyList(), SLACK, true), arrange(listOf(SLACK)))
        assertEquals(ShareRows(emptyList(), SLACK, false), arrange(listOf(SLACK), onStart = false))
    }

    @Test
    fun twoOrThreeGetTheirOwnRowInOrder() {
        assertEquals(listOf(WHATSAPP, TELEGRAM), arrange(listOf(WHATSAPP, TELEGRAM)).ownRow)
        val three = arrange(listOf(WHATSAPP, TELEGRAM, DISCORD))
        assertEquals(listOf(WHATSAPP, TELEGRAM, DISCORD), three.ownRow)
        assertNull(three.beside)
    }

    @Test
    fun onlyInstalledOnesCountSoTwoChosenWithOneInstalledSitsBesideShare() {
        val r = arrange(listOf(WHATSAPP, TELEGRAM), installed = setOf(TELEGRAM))
        assertEquals(ShareRows(emptyList(), TELEGRAM, true), r)
        assertEquals(ShareRows(emptyList(), null, true), arrange(listOf(WHATSAPP), installed = emptySet()))
    }

    @Test
    fun neverMoreThanThreeAndNoDuplicates() {
        assertEquals(listOf(WHATSAPP, TELEGRAM, DISCORD), arrange(listOf(WHATSAPP, WHATSAPP, TELEGRAM, DISCORD, INSTAGRAM)).ownRow)
    }
}
