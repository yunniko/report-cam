package cz.julienika.reportcam.stamp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every character is 0.5 em wide; lines are 1.2 em high with a 1 em ascent. */
private object FixedMeasurer : TextMeasurer {
    override fun width(
        text: String,
        sizePx: Float,
    ) = text.length * sizePx * 0.5f

    override fun ascent(sizePx: Float) = sizePx

    override fun lineHeight(sizePx: Float) = sizePx * 1.2f
}

private class LinesField(
    private vararg val text: String,
    override val describesPlace: Boolean = false,
) : StampField {
    override val id = "fixed"

    override fun lines(data: StampData) = text.toList()
}

/** A where-field: the place name and coordinates stand-in. */
private fun where(vararg text: String) = LinesField(*text, describesPlace = true)

class StampLayoutTest {
    private val style = StampStyle(textSizeFraction = 0.05f) // 50 px on a 1000 px short edge

    private fun layout(
        placement: StampPlacement,
        vararg lines: String,
        w: Int = 1000,
        h: Int = 1500,
    ) = StampLayout.compute(w, h, StampSpec(listOf(LinesField(*lines)), placement, style), StampData.EMPTY, FixedMeasurer)

    /** A 1000 px wide photo (50 px text, 25 px per character): padding 30 px, so lines run from 30 to 970. */
    private fun frame(
        what: List<String>,
        whereLines: List<String>,
        placement: StampPlacement = FramePlacement(top = false),
    ) = StampLayout.compute(
        1000,
        1500,
        StampSpec(listOf(LinesField(*what.toTypedArray()), where(*whereLines.toTypedArray())), placement, style),
        StampData.EMPTY,
        FixedMeasurer,
    )!!

    private fun PlacedLine.right() = x + text.length * 25f

    private fun PlacedLine.atLeft() = kotlin.math.abs(x - 30f) < 0.01f

    @Test
    fun nothingToSayMeansNoStamp() {
        assertNull(layout(OverlayPlacement(Corner.BOTTOM_START)))
    }

    @Test
    fun overlayKeepsPhotoSizeAndSitsInsideItsCorner() {
        val g = layout(OverlayPlacement(Corner.BOTTOM_END), "abcd", "ab")!!
        assertEquals(1000, g.outputWidth)
        assertEquals(1500, g.outputHeight)
        val box = g.box!!
        assertEquals(1000f - 30f, box.right, 0.01f) // margin 0.6 em
        assertEquals(1500f - 30f, box.bottom, 0.01f)
        // End corner: every line ends at the same x, the box's right edge less padding.
        val ends = g.lines.map { it.x + it.text.length * 25f }
        assertEquals(ends[0], ends[1], 0.01f)
        assertEquals(box.right - 25f, ends[0], 0.01f)
    }

    @Test
    fun overlayTopStartAlignsLeft() {
        val g = layout(OverlayPlacement(Corner.TOP_START), "abcd", "ab")!!
        assertEquals(g.lines[0].x, g.lines[1].x, 0.01f)
        assertEquals(30f, g.box!!.left, 0.01f)
        assertEquals(30f + 25f + 50f, g.lines[0].baseline, 0.01f) // margin + padding + ascent
    }

    @Test
    fun frameAddsABandAndDoesNotCoverThePhoto() {
        val bottom = layout(FramePlacement(top = false), "one", "two")!!
        assertEquals(1000, bottom.outputWidth)
        assertTrue(bottom.outputHeight > 1500)
        assertEquals(0, bottom.imageTop)
        assertTrue(bottom.lines.all { it.baseline > 1500f })

        val top = layout(FramePlacement(top = true), "one", "two")!!
        assertEquals(top.outputHeight - 1500, top.imageTop)
        assertTrue(top.lines.all { it.baseline < top.imageTop })
    }

    @Test
    fun frameBorderSurroundsThePhotoOnTheOtherThreeEdges() {
        // 1000x1500 photo, border 2% of the short edge = 20 px.
        val bottom = layout(FramePlacement(top = false, borderFraction = 0.02f), "one")!!
        val plain = layout(FramePlacement(top = false), "one")!!
        val bandH = plain.outputHeight - 1500
        assertEquals(1040, bottom.outputWidth)
        assertEquals(20 + 1500 + bandH, bottom.outputHeight)
        assertEquals(20, bottom.imageLeft)
        assertEquals(20, bottom.imageTop)
        assertEquals(Box(0f, 0f, 1040f, bottom.outputHeight.toFloat()), bottom.box)
        // Text lines up with the photo edge and sits in the band below it.
        assertEquals(20f + plain.lines[0].x, bottom.lines[0].x, 0.01f)
        assertTrue(bottom.lines.all { it.baseline > 20f + 1500f })

        val top = layout(FramePlacement(top = true, borderFraction = 0.02f), "one")!!
        assertEquals(bandH, top.imageTop)
        assertEquals(bandH + 1500 + 20, top.outputHeight)
        assertTrue(top.lines.all { it.baseline < bandH })
    }

    @Test
    fun aFrameWithoutBorderCoversTheWholeOutputBehindThePhoto() {
        val g = layout(FramePlacement(top = false), "one")!!
        assertEquals(Box(0f, 0f, 1000f, g.outputHeight.toFloat()), g.box)
        assertEquals(0, g.imageLeft)
    }

    @Test
    fun longLinesWrapToFitThePhoto() {
        val words = List(30) { "word" }.joinToString(" ")
        val g = layout(OverlayPlacement(Corner.BOTTOM_START), words)!!
        assertTrue(g.lines.size > 1)
        assertTrue(g.box!!.right <= 1000f - 30f + 0.01f)
        assertEquals(words, g.lines.joinToString(" ") { it.text })
    }

    @Test
    fun unbreakableWordIsSplitByCharacters() {
        val g = layout(FramePlacement(false), "x".repeat(100), w = 1000, h = 1000)!!
        assertTrue(g.lines.size >= 3)
        assertEquals("x".repeat(100), g.lines.joinToString("") { it.text })
    }

    @Test
    fun textScalesWithTheShortEdgeNotTheOutputSetting() {
        val small = layout(FramePlacement(false), "a", w = 600, h = 800)!!
        val large = layout(FramePlacement(false), "a", w = 1200, h = 1600)!!
        assertEquals(2f, large.textSizePx / small.textSizePx, 0.001f)
    }

    @Test
    fun frameShowsWhereOnTheRightTopAlignedWithTheNote() {
        val g = frame(listOf("Site 4", "2026-11-04 08:06"), listOf("Prague", "50.1, 14.4 5 m"))
        val (left, right) = g.lines.partition { it.atLeft() }
        assertEquals(listOf("Site 4", "2026-11-04 08:06"), left.map { it.text })
        assertEquals(listOf("Prague", "50.1, 14.4 5 m"), right.map { it.text })
        right.forEach { assertEquals("right-aligned to the photo edge", 970f, it.right(), 0.01f) }
        assertEquals("place above coordinates", right[0].baseline + 60f, right[1].baseline, 0.01f)
        assertEquals("top-aligned", left[0].baseline, right[0].baseline, 0.01f)
    }

    @Test
    fun theBandIsAsTallAsTheTallerColumn() {
        val oneLeft = frame(listOf("Site 4"), listOf("Prague", "50.1, 14.4"))
        val oneRight = frame(listOf("Site 4", "08:06"), listOf("Prague"))
        val stacked = layout(FramePlacement(top = false), "a", "b")!!
        // Two lines tall either way, the same band as two stacked lines.
        assertEquals(stacked.outputHeight, oneLeft.outputHeight)
        assertEquals(stacked.outputHeight, oneRight.outputHeight)
    }

    @Test
    fun aLongNoteWrapsOnTheLeftWhileWhereStaysRight() {
        val note = List(12) { "word" }.joinToString(" ") // 59 characters, 1475 px
        val g = frame(listOf(note), listOf("Prague"))
        val (left, right) = g.lines.partition { it.atLeft() }
        assertTrue(left.size > 1)
        assertEquals(note, left.joinToString(" ") { it.text })
        assertEquals(listOf("Prague"), right.map { it.text })
        assertTrue("the note keeps clear of the place", left.all { it.right() <= right[0].x - 75f + 0.01f })
    }

    @Test
    fun columnsThatDoNotFitSideBySideStackAsBefore() {
        // Both sides need over half the room and neither can be wrapped without splitting a word.
        val g = frame(listOf("x".repeat(20)), listOf("y".repeat(20)))
        assertEquals(listOf("x".repeat(20), "y".repeat(20)), g.lines.map { it.text })
        assertTrue(g.lines.all { it.atLeft() })
    }

    @Test
    fun withOnlyWhereToSayItStillSitsOnTheRight() {
        val g = frame(emptyList(), listOf("Prague"))
        assertEquals(970f, g.lines.single().right(), 0.01f)
    }

    @Test
    fun aFrameTopBandGetsTheSameColumns() {
        val g = frame(listOf("Site 4"), listOf("Prague"), FramePlacement(top = true))
        assertEquals(970f, g.lines.single { it.text == "Prague" }.right(), 0.01f)
        assertTrue(g.lines.all { it.baseline < g.imageTop })
    }

    @Test
    fun overlayKeepsOneColumnWhatBeforeWhere() {
        val spec =
            StampSpec(listOf(where("Prague"), LinesField("Site 4")), OverlayPlacement(Corner.BOTTOM_START), style)
        val g = StampLayout.compute(1000, 1500, spec, StampData.EMPTY, FixedMeasurer)!!
        assertEquals(listOf("Site 4", "Prague"), g.lines.map { it.text })
        assertEquals(g.lines[0].x, g.lines[1].x, 0.01f)
    }

    @Test
    fun aNewFieldAndANewPlacementNeedNoEngineChange() {
        // Stand-ins for future additions (an address field, a side band): plain classes, no engine edits.
        val address = LinesField("Main St 1, Prague")
        val sideBand =
            object : StampPlacement {
                override val id = "side"

                override fun place(
                    imageWidth: Int,
                    imageHeight: Int,
                    text: StampText,
                ): StampGeometry {
                    val block = text.block(text.all, 400f)
                    return StampGeometry(
                        imageWidth + 400,
                        imageHeight,
                        0,
                        0,
                        Box(imageWidth.toFloat(), 0f, imageWidth + 400f, imageHeight.toFloat()),
                        0f,
                        block.sizePx,
                        block.lines.mapIndexed { i, t -> PlacedLine(t, imageWidth + 10f, (i + 1) * block.lineHeight) },
                    )
                }
            }
        val spec = StampSpec(listOf(TextField(), address), sideBand, style)
        val data = StampData.EMPTY.with(StampKeys.USER_TEXT, "Crew B")
        val g = StampLayout.compute(1000, 1500, spec, data, FixedMeasurer)!!
        assertEquals(1400, g.outputWidth)
        assertEquals(listOf("Crew B", "Main St 1,", "Prague"), g.lines.map { it.text })
    }
}
