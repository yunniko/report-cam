package cz.julienika.reportcam.about

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer

/** The Play listing under docs/play/ stays within Play's limits: text lengths and image sizes (G-006 M3). */
class PlayListingTest {
    // Gradle runs unit tests from the module directory.
    private val play = File("../docs/play")
    private val listing = play.resolve("listing.md").readText()

    /** The text under a `### <heading>` inside a `## <language>` block, trimmed. */
    private fun text(
        language: String,
        heading: String,
    ): String {
        val block = listing.substringAfter("\n## $language").substringBefore("\n## ")
        return block.substringAfter("\n### $heading").substringAfter("\n\n").substringBefore("\n### ").trim()
    }

    @Test
    fun everyTextFitsItsLimitInBothLanguages() {
        for (language in listOf("English", "Czech")) {
            val title = text(language, "Title")
            val short = text(language, "Short description")
            val full = text(language, "Full description")
            assertTrue("$language title ${title.length} > 30", title.length in 1..30)
            assertTrue("$language short ${short.length} > 80", short.length in 1..80)
            assertTrue("$language full ${full.length} > 4000", full.length in 1..4000)
            assertTrue(title.lines().size == 1 && short.lines().size == 1)
        }
    }

    @Test
    fun theGraphicsHavePlaysSizes() {
        assertEquals(512 to 512, pngSize(play.resolve("icon-512.png")))
        assertEquals(1024 to 500, pngSize(play.resolve("feature-graphic.png")))
        val shots = play.resolve("screenshots").listFiles { f -> f.extension == "png" }!!.sortedBy { it.name }
        assertTrue("2 to 8 phone screenshots, found ${shots.size}", shots.size in 2..8)
        for (s in shots) assertEquals(s.name, 1080 to 2220, pngSize(s))
    }

    /** Width and height from the IHDR chunk, the first chunk of every PNG. */
    private fun pngSize(f: File): Pair<Int, Int> {
        val head = f.inputStream().use { it.readNBytes(24) }
        assertEquals("${f.name} is a PNG", 0x89, head[0].toInt() and 0xFF)
        val b = ByteBuffer.wrap(head, 16, 8)
        return b.int to b.int
    }
}
