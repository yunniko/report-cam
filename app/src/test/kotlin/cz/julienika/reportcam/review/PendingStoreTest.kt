package cz.julienika.reportcam.review

import cz.julienika.reportcam.camera.CapturedJpeg
import cz.julienika.reportcam.photo.AspectRatio
import cz.julienika.reportcam.stamp.GeoFix
import cz.julienika.reportcam.stamp.LocationReading
import cz.julienika.reportcam.stamp.Place
import cz.julienika.reportcam.stamp.SchemeId
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.ZonedDateTime

class PendingStoreTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val meta =
        PendingMeta(
            captureTime = ZonedDateTime.parse("2026-11-03T13:38:47.123+01:00[Europe/Prague]"),
            userText = "Stavba č. 4 – kontrola",
            rotationDegrees = 90,
            outputLongEdge = 1920,
            aspect = AspectRatio.R4_5,
        )
    private val bytes = ByteArray(5_000) { (it % 251).toByte() }

    private fun store() = PendingStore(File(tmp.root, "pending"))

    @Test
    fun aSavedPhotoComesBackWhole() {
        store().save(CapturedJpeg(bytes, 90), meta)
        // A fresh instance, as after a restart.
        val (jpeg, m) = store().load()!!
        assertArrayEquals(bytes, jpeg.bytes)
        assertEquals(90, jpeg.rotationDegrees)
        assertEquals(meta, m)
    }

    @Test
    fun clearLeavesNothingToRestore() {
        val s = store()
        s.save(CapturedJpeg(bytes, 0), meta)
        assertTrue(s.exists())
        s.clear()
        assertFalse(s.exists())
        assertNull(s.load())
    }

    @Test
    fun aSaveCutShortBeforeItsMetadataIsNotRestored() {
        val dir = File(tmp.root, "pending").apply { mkdirs() }
        File(dir, "photo.jpg").writeBytes(bytes)
        File(dir, "meta.tmp").writeText("capture_time=2026")
        assertFalse(store().exists())
        assertNull(store().load())
    }

    @Test
    fun brokenMetadataIsDroppedNotHalfRestored() {
        val dir = File(tmp.root, "pending").apply { mkdirs() }
        File(dir, "photo.jpg").writeBytes(bytes)
        File(dir, "meta.properties").writeText("capture_time=not a time\nrotation=x\n")
        assertNull(store().load())
        assertFalse(File(dir, "photo.jpg").exists())
    }

    @Test
    fun aNewSaveReplacesTheOldOne() {
        val s = store()
        s.save(CapturedJpeg(bytes, 0), meta)
        s.save(CapturedJpeg(byteArrayOf(1, 2, 3), 180), meta.copy(userText = "second"))
        val (jpeg, m) = s.load()!!
        assertArrayEquals(byteArrayOf(1, 2, 3), jpeg.bytes)
        assertEquals("second", m.userText)
    }

    private val facts =
        StampFacts(
            captureTime = meta.captureTime,
            userText = "Stavba č. 4 – kontrola",
            location = LocationReading.Fix(GeoFix(50.08765, 14.42133, 4.5f, 1_762_173_527_000, coarseOnly = false)),
            place = Place(street = "Karlova", houseNumber = "12", city = "Praha", fullAddress = null),
        )

    private val ready =
        ReadyMeta(
            fileName = "report_20261103_133847_123_0a1b2c3d.jpg",
            message = "Stavba č. 4 – kontrola\nupraveno",
            messageOn = false,
            note = "Stavba č. 4 – kontrola",
            noteOn = true,
            savedFiles = setOf("report_20261103_133847_123_0a1b2c3d.jpg", "report_20261103_133847_123_99ff0011.jpg"),
            scheme = SchemeId.LIGHT,
            rotationDegrees = 90,
            outputLongEdge = 1280,
            aspect = AspectRatio.R4_5,
            facts = facts,
        )

    private val source = CapturedJpeg(ByteArray(3_000) { (it % 7).toByte() }, 90)

    private fun stampedFile() = tmp.newFile().apply { writeBytes(bytes) }

    @Test
    fun aStampedPhotoComesBackWithItsTextsAndSavedVersions() {
        store().saveReady(stampedFile(), source, ready)
        val kept = store().loadReady()!!
        assertArrayEquals(bytes, kept.stamped.readBytes())
        assertArrayEquals(source.bytes, kept.source.bytes)
        assertEquals(90, kept.source.rotationDegrees)
        assertEquals(ready, kept.meta)
    }

    @Test
    fun keepingTheStampedPhotoDropsTheWaitingOne() {
        val s = store()
        s.save(CapturedJpeg(bytes, 0), meta)
        s.saveReady(stampedFile(), source, ready)
        assertFalse(s.hasWaiting())
        assertNull(s.load())
        assertTrue(s.hasReady())
    }

    @Test
    fun anUpdateReplacesTheStateAndNeedsAKeptPhoto() {
        val s = store()
        s.updateReady(ready)
        assertFalse("no photo, nothing to update", s.exists())
        s.saveReady(stampedFile(), source, ready)
        val edited = ready.copy(message = "jiný text", messageOn = true, note = "", noteOn = false, savedFiles = emptySet())
        s.updateReady(edited)
        assertEquals(edited, store().loadReady()!!.meta)
    }

    @Test
    fun aNewWaitingPhotoOrClearDropsTheStampedOne() {
        val s = store()
        s.saveReady(stampedFile(), source, ready)
        s.save(CapturedJpeg(bytes, 0), meta)
        assertFalse(s.hasReady())
        s.saveReady(stampedFile(), source, ready)
        s.clear()
        assertFalse(s.exists())
    }

    @Test
    fun aStampedEntryNamingAPathIsDropped() {
        val s = store()
        s.saveReady(stampedFile(), source, ready.copy(fileName = "../../shared_prefs/x.xml"))
        assertNull(s.loadReady())
        assertFalse(s.exists())
    }

    @Test
    fun factsWithoutLocationOrPlaceRoundTrip() {
        for (location in listOf(null, LocationReading.None)) {
            val m = ready.copy(facts = facts.copy(userText = null, location = location, place = null))
            store().saveReady(stampedFile(), source, m)
            assertEquals(m, store().loadReady()!!.meta)
        }
        assertEquals(facts, StampFacts.of(facts.toStampData()))
    }

    @Test
    fun aRestampReplacesTheFileAndStateButKeepsTheSource() {
        val s = store()
        s.replaceStamped(stampedFile(), ready)
        assertFalse("nothing kept, nothing to replace", s.exists())
        s.saveReady(stampedFile(), source, ready)
        val redrawn = tmp.newFile().apply { writeBytes(byteArrayOf(9, 9, 9)) }
        s.replaceStamped(redrawn, ready.copy(fileName = "report_20261103_133847_123_77aa55cc.jpg", scheme = SchemeId.DARK))
        val kept = store().loadReady()!!
        assertArrayEquals(byteArrayOf(9, 9, 9), kept.stamped.readBytes())
        assertArrayEquals(source.bytes, kept.source.bytes)
        assertEquals(SchemeId.DARK, kept.meta.scheme)
        assertFalse("this version is not among the saved ones", kept.meta.saved)
    }

    @Test
    fun anEntryFrom03ComesBackWithTheNoteSwitchedOnAndItsSavedFlag() {
        val dir = File(tmp.root, "pending").apply { mkdirs() }
        File(dir, "stamped.jpg").writeBytes(bytes)
        File(dir, "source.jpg").writeBytes(source.bytes)
        val old =
            ready.toProperties().apply {
                remove("note")
                remove("note_on")
                remove("saved_files")
                setProperty("saved", "true")
            }
        File(dir, "ready.properties").outputStream().use { old.store(it, null) }
        val m = store().loadReady()!!.meta
        assertEquals("the note is what was stamped", facts.userText, m.note)
        assertTrue(m.noteOn)
        assertEquals(setOf(ready.fileName), m.savedFiles)
        assertTrue(m.saved)
        assertEquals(ready.message, m.message)
    }

    @Test
    fun aStampedEntryFromBeforeSchemesIsDroppedNotHalfRestored() {
        val dir = File(tmp.root, "pending").apply { mkdirs() }
        File(dir, "stamped.jpg").writeBytes(bytes)
        File(dir, "source.jpg").writeBytes(bytes)
        File(dir, "ready.properties").writeText("file=report.jpg\ncapture_time=2026-11-03T13:38:47+01:00\ncaption=x\ncaption_on=true\nsaved=false\n")
        assertNull(store().loadReady())
        assertFalse(store().exists())
    }
}
