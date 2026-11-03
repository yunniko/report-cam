package cz.julienika.reportcam.review

import cz.julienika.reportcam.camera.CapturedJpeg
import cz.julienika.reportcam.photo.AspectRatio
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

    private val ready =
        ReadyMeta(
            fileName = "report_20261103_133847_123.jpg",
            captureTime = meta.captureTime,
            caption = "Stavba č. 4 – kontrola\nupraveno",
            captionOn = false,
            saved = true,
        )

    private fun stampedFile() = tmp.newFile().apply { writeBytes(bytes) }

    @Test
    fun aStampedPhotoComesBackWithItsCaptionAndFlags() {
        store().saveReady(stampedFile(), ready)
        val (file, m) = store().loadReady()!!
        assertArrayEquals(bytes, file.readBytes())
        assertEquals(ready, m)
    }

    @Test
    fun keepingTheStampedPhotoDropsTheWaitingOne() {
        val s = store()
        s.save(CapturedJpeg(bytes, 0), meta)
        s.saveReady(stampedFile(), ready)
        assertFalse(s.hasWaiting())
        assertNull(s.load())
        assertTrue(s.hasReady())
    }

    @Test
    fun anUpdateReplacesTheStateAndNeedsAKeptPhoto() {
        val s = store()
        s.updateReady(ready)
        assertFalse("no photo, nothing to update", s.exists())
        s.saveReady(stampedFile(), ready)
        s.updateReady(ready.copy(caption = "jiný text", captionOn = true, saved = false))
        assertEquals(ready.copy(caption = "jiný text", captionOn = true, saved = false), store().loadReady()!!.second)
    }

    @Test
    fun aNewWaitingPhotoOrClearDropsTheStampedOne() {
        val s = store()
        s.saveReady(stampedFile(), ready)
        s.save(CapturedJpeg(bytes, 0), meta)
        assertFalse(s.hasReady())
        s.saveReady(stampedFile(), ready)
        s.clear()
        assertFalse(s.exists())
    }

    @Test
    fun aStampedEntryNamingAPathIsDropped() {
        val s = store()
        s.saveReady(stampedFile(), ready.copy(fileName = "../../shared_prefs/x.xml"))
        assertNull(s.loadReady())
        assertFalse(s.exists())
    }
}
