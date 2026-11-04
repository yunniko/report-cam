package cz.julienika.reportcam.review

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** When the review's Note and Message rows show, by the two G-005 settings (D031). */
class ReviewTextsRowsTest {
    @Test
    fun withBothSettingsOnEveryRowShows() {
        assertTrue(ReviewTexts.rowShown(editing = true, switches = true, rowOn = true))
        assertTrue("an off row keeps its switch", ReviewTexts.rowShown(editing = true, switches = true, rowOn = false))
    }

    @Test
    fun withoutEditingTheRowsShowReadOnlyWithTheirSwitches() {
        assertTrue(ReviewTexts.rowShown(editing = false, switches = true, rowOn = true))
        assertTrue(ReviewTexts.rowShown(editing = false, switches = true, rowOn = false))
    }

    @Test
    fun withoutSwitchesARowThatStartedOffIsHidden() {
        assertTrue(ReviewTexts.rowShown(editing = true, switches = false, rowOn = true))
        assertFalse(ReviewTexts.rowShown(editing = true, switches = false, rowOn = false))
    }

    @Test
    fun withBothOffNoRowShows() {
        assertFalse(ReviewTexts.rowShown(editing = false, switches = false, rowOn = true))
        assertFalse(ReviewTexts.rowShown(editing = false, switches = false, rowOn = false))
    }
}
