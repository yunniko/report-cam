package cz.julienika.reportcam.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SharedFilesTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun prunesOnlyFilesOlderThanADay() {
        val now = 1_800_000_000_000L
        val old = tmp.newFile("old.jpg").apply { setLastModified(now - SharedFiles.MAX_AGE_MS - 1) }
        val fresh = tmp.newFile("fresh.jpg").apply { setLastModified(now - 60_000) }
        assertEquals(1, SharedFiles.prune(tmp.root, now))
        assertFalse(old.exists())
        assertTrue(fresh.exists())
    }

    @Test
    fun missingDirectoryIsNotAnError() {
        assertEquals(0, SharedFiles.prune(tmp.root.resolve("none"), 0))
    }

    @Test
    fun firstInstalledPrefersListOrder() {
        val installed = setOf("com.whatsapp.w4b", "com.whatsapp")
        assertEquals("com.whatsapp", Messenger.WHATSAPP.firstInstalled { it in installed })
        assertNull(Messenger.TELEGRAM.firstInstalled { it in installed })
    }

    @Test
    fun messengerIdsAreUniqueAndFoundById() {
        assertEquals(Messenger.entries.size, Messenger.entries.map { it.id }.toSet().size)
        for (m in Messenger.entries) assertEquals(m, Messenger.of(m.id))
        assertNull(Messenger.of("icq"))
    }
}
