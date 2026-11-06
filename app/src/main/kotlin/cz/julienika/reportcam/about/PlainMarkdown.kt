package cz.julienika.reportcam.about

/** The little Markdown the privacy policy uses (headings and bullets), as lines a dialog can style. */
object PlainMarkdown {
    data class Line(
        val text: String,
        val heading: Boolean,
    )

    fun render(markdown: String): List<Line> =
        markdown.lines().map { raw ->
            val line = raw.trimEnd()
            when {
                line.startsWith("#") -> Line(line.trimStart('#').trim(), heading = true)
                line.startsWith("- ") -> Line("• " + line.removePrefix("- "), heading = false)
                else -> Line(line, heading = false)
            }
        }
}
