package cz.julienika.reportcam.review

import cz.julienika.reportcam.settings.AppSettings
import cz.julienika.reportcam.settings.FakeSource
import cz.julienika.reportcam.settings.Keys
import cz.julienika.reportcam.settings.ui.StampLooks
import cz.julienika.reportcam.stamp.SchemeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The Owner's rules for the review's quick stamp row (G-005). */
class StampChoicesTest {
    private val order = listOf("a", "b", "c")

    @Test
    fun nothingTickedOrOnlyTheDefaultShowsNoGroup() {
        assertEquals(emptyList<String>(), StampChoices.group(order, emptySet(), "a"))
        assertEquals(emptyList<String>(), StampChoices.group(order, setOf("a"), "a"))
    }

    @Test
    fun oneOtherButtonComesWithTheDefault() {
        assertEquals(listOf("a", "c"), StampChoices.group(order, setOf("c"), "a"))
    }

    @Test
    fun twoOrMoreTickedShowAsTickedInTheirOrder() {
        assertEquals(listOf("b", "c"), StampChoices.group(order, setOf("c", "b"), "a"))
        assertEquals(order, StampChoices.group(order, order.toSet(), "b"))
    }

    @Test
    fun everyPlacementHasAPlaceInTheRowAndALook() {
        assertEquals(AppSettings.PLACEMENTS.keys, StampChoices.POSITION_ORDER.toSet())
        assertEquals(AppSettings.PLACEMENTS.keys, StampLooks.POSITIONS.keys)
        assertEquals(SchemeId.entries.toSet(), StampLooks.SCHEMES.keys)
    }

    @Test
    fun byDefaultEveryButtonShowsAsIcons() {
        val c = StampChoices.of(AppSettings.from(FakeSource()))
        assertEquals(StampChoices.POSITION_ORDER, c.positions)
        assertEquals(SchemeId.entries, c.schemes)
        assertFalse(c.named)
    }

    @Test
    fun twoButtonsInAllAreNamed() {
        // Only the colours: the row is Dark and Light with their names, as in 0.4.
        val colours = StampChoices.of(AppSettings.from(FakeSource(Keys.QUICK_POSITIONS to emptySet<String>())))
        assertEquals(emptyList<String>(), colours.positions)
        assertTrue(colours.named)
        // One position that is not the default, colours trimmed to the default only.
        val one =
            StampChoices.of(
                AppSettings.from(FakeSource(Keys.QUICK_POSITIONS to setOf("frame_top"), Keys.QUICK_SCHEMES to setOf("dark"))),
            )
        assertEquals(listOf("overlay_bottom_start", "frame_top"), one.positions)
        assertEquals(emptyList<SchemeId>(), one.schemes)
        assertTrue(one.named)
    }

    @Test
    fun theDefaultsComeFromSettingsAndUnknownIdsAreDropped() {
        val s =
            AppSettings.from(
                FakeSource(
                    Keys.PLACEMENT to "frame_bottom",
                    Keys.QUICK_POSITIONS to setOf("frame_bottom", "nowhere"),
                    Keys.QUICK_SCHEMES to setOf("light", "sepia"),
                    Keys.STAMP_SCHEME to "dark",
                ),
            )
        assertEquals(setOf("frame_bottom"), s.quickPositions)
        assertEquals(setOf(SchemeId.LIGHT), s.quickSchemes)
        val c = StampChoices.of(s)
        assertTrue("only the default position ticked", c.positions.isEmpty())
        assertEquals(listOf(SchemeId.DARK, SchemeId.LIGHT), c.schemes)
        assertFalse(c.isEmpty)
        assertTrue(StampChoices.of(AppSettings.from(FakeSource(Keys.QUICK_POSITIONS to emptySet<String>(), Keys.QUICK_SCHEMES to emptySet<String>()))).isEmpty)
    }
}
