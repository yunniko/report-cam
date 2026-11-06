package cz.julienika.reportcam.about

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** The About card's sources stay in step: the policy the app shows, the page published, the README's attributions. */
class AboutTest {
    // Gradle runs unit tests from the module directory.
    private val policyMd = File("../docs/privacy-policy.md").readText()
    private val readme = File("../README.md").readText()

    @Test
    fun theAppShowsThePolicyThatIsPublished() {
        assertEquals(policyMd, File("src/main/assets/privacy-policy.md").readText())
    }

    @Test
    fun thePublishedPageCarriesEveryHeadingAndTheDate() {
        val html = File("../docs/site/privacy-policy.html").readText()
        val headings = policyMd.lines().filter { it.startsWith("## ") }.map { it.removePrefix("## ") }
        assertTrue(headings.size >= 8)
        for (h in headings) assertTrue("heading '$h' in the page", html.contains("<h2>$h</h2>"))
        val updated = policyMd.lines().first { it.startsWith("Last updated:") }
        assertTrue(html.contains(updated))
    }

    @Test
    fun theNoticeNamesEveryAttributionInTheReadmeAndNothingDropped() {
        val notice = Licences.notice { asset -> "<$asset>" }
        for (name in listOf("AndroidX", "kotlinx.coroutines", "Material Icons", "Material Symbols", "OpenSearch UI", "Boxicons")) {
            assertTrue("$name in README", readme.contains(name))
            assertTrue("$name in the notice", notice.contains(name))
        }
        assertFalse(notice.contains("css.gg"))
        assertFalse(readme.contains("css.gg"))
        assertTrue(notice.contains(Licences.TRADEMARKS))
    }

    @Test
    fun eachLicenceTextAppearsOnceAfterTheComponents() {
        val notice = Licences.notice { asset -> "TEXT OF $asset" }
        for (l in Licences.Licence.entries) {
            assertEquals(l.title, 1, notice.windowed("TEXT OF ${l.asset}".length).count { it == "TEXT OF ${l.asset}" })
            assertTrue(notice.indexOf("TEXT OF ${l.asset}") > notice.indexOf(Licences.components.last().url))
        }
        assertTrue(File("src/main/assets/licenses/apache-2.0.txt").readText().contains("Apache License"))
        assertTrue(File("src/main/assets/licenses/mit-boxicons.txt").readText().contains("Copyright (c) 2015-2021 Aniket Suvarna"))
    }

    @Test
    fun headingsAndBulletsAreMarkedAndTheRestIsPlain() {
        val lines = PlainMarkdown.render("# Title\n\n## Part\n- one\n- two\ntext  \n")
        assertEquals(
            listOf(
                PlainMarkdown.Line("Title", true),
                PlainMarkdown.Line("", false),
                PlainMarkdown.Line("Part", true),
                PlainMarkdown.Line("• one", false),
                PlainMarkdown.Line("• two", false),
                PlainMarkdown.Line("text", false),
                PlainMarkdown.Line("", false),
            ),
            lines,
        )
    }
}
