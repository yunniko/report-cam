package cz.julienika.reportcam.review

import cz.julienika.reportcam.camera.CapturedJpeg
import cz.julienika.reportcam.photo.AspectRatio
import cz.julienika.reportcam.settings.AppSettings
import cz.julienika.reportcam.settings.FakeSource
import cz.julienika.reportcam.settings.Keys
import cz.julienika.reportcam.stamp.SchemeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.Duration
import java.time.ZonedDateTime

class ExpiryTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val taken = ZonedDateTime.parse("2026-11-06T08:00:00+01:00[Europe/Prague]")
    private val takenMillis = taken.toInstant().toEpochMilli()

    @Test
    fun theRuleIsNeverWithoutALimitAndAtTheLimitOtherwise() {
        val hour = Duration.ofHours(1)
        assertFalse("no limit", Expiry.expired(taken, takenMillis + 100 * 3_600_000L, null))
        assertFalse("younger", Expiry.expired(taken, takenMillis + hour.toMillis() - 1, hour))
        assertTrue("exactly the limit", Expiry.expired(taken, takenMillis + hour.toMillis(), hour))
        assertTrue("older", Expiry.expired(taken, takenMillis + 2 * hour.toMillis(), hour))
        // A clock set back since the shot: the photo looks taken in the future and is not dropped.
        assertFalse(Expiry.expired(taken, takenMillis - 10 * hour.toMillis(), Duration.ofMinutes(1)))
    }

    // ---- the discard step, over a real store on disk -------------------------------------------

    // Lazy: the rule creates the folder after the constructor runs.
    private val store by lazy { PendingStore(File(tmp.root, "pending")) }
    private val jpeg = CapturedJpeg(ByteArray(2_000) { (it % 13).toByte() }, 90)
    private val waiting = PendingMeta(taken, "Site 4", 90, 1280, AspectRatio.FULL)
    private val ready =
        ReadyMeta(
            fileName = "report_20261106_080000_000_0a1b2c3d.jpg",
            message = "Site 4",
            messageOn = true,
            note = "Site 4",
            noteOn = true,
            savedFiles = emptySet(),
            scheme = SchemeId.DARK,
            placement = "frame_top",
            rotationDegrees = 90,
            outputLongEdge = 1280,
            aspect = AspectRatio.FULL,
            facts = StampFacts(taken, "Site 4", null, null),
        )

    /** Each gallery write asked for: the bytes written, under the name and time given. */
    private val saved = mutableListOf<Triple<ByteArray, String, Long>>()
    private var stamped = 0
    private var galleryRefuses = false

    private val expiry by lazy {
        Expiry(
            store,
            stamp = { j, m, _ ->
                stamped++
                tmp.newFile("report_stamped_${m.userText.hashCode()}.jpg").apply { writeBytes(j.bytes.reversedArray()) }
            },
            save = { f, name, at ->
                if (!galleryRefuses) saved += Triple(f.readBytes(), name, at)
                !galleryRefuses
            },
        )
    }

    private fun settings(
        minutes: String,
        save: Boolean,
    ) = AppSettings.from(FakeSource(Keys.DISCARD_AFTER to minutes, Keys.SAVE_DISCARDED to save))

    private fun stampedFile() = tmp.newFile().apply { writeBytes(byteArrayOf(7, 7, 7)) }

    @Test
    fun nothingKeptOrNoLimitOrYoungerIsLeftAlone() {
        assertEquals(Expiry.Outcome.KEPT, expiry.discardIfExpired(settings("60", true), takenMillis + 7_200_000L))
        store.saveReady(stampedFile(), jpeg, ready)
        assertEquals(Expiry.Outcome.KEPT, expiry.discardIfExpired(settings("", true), takenMillis + 7_200_000L))
        assertEquals(Expiry.Outcome.KEPT, expiry.discardIfExpired(settings("60", true), takenMillis + 3_599_999L))
        assertTrue("still kept", store.hasReady())
        assertTrue(saved.isEmpty())
    }

    @Test
    fun anOldStampedPhotoIsDroppedAndSavedUnderItsSharedNameWhenAsked() {
        store.saveReady(stampedFile(), jpeg, ready)
        assertEquals(Expiry.Outcome.DROPPED_AND_SAVED, expiry.discardIfExpired(settings("60", true), takenMillis + 3_600_000L))
        assertFalse(store.exists())
        val (bytes, name, at) = saved.single()
        assertArrayEquals(byteArrayOf(7, 7, 7), bytes)
        assertEquals(ready.fileName, name)
        assertEquals(takenMillis, at)
        assertEquals("nothing to stamp", 0, stamped)
    }

    @Test
    fun anOldStampedPhotoIsDroppedWithoutSavingWhenNotAsked() {
        store.saveReady(stampedFile(), jpeg, ready)
        assertEquals(Expiry.Outcome.DROPPED, expiry.discardIfExpired(settings("1", false), takenMillis + 60_000L))
        assertFalse(store.exists())
        assertTrue(saved.isEmpty())
    }

    @Test
    fun aPhotoAlreadyInTheGalleryGetsNoSecondCopy() {
        store.saveReady(stampedFile(), jpeg, ready.copy(savedFiles = setOf(ready.fileName)))
        assertEquals(Expiry.Outcome.DROPPED_AND_SAVED, expiry.discardIfExpired(settings("1", true), takenMillis + 60_000L))
        assertTrue(saved.isEmpty())
        assertFalse(store.exists())
    }

    @Test
    fun anOldWaitingPhotoIsStampedThenSaved() {
        store.save(jpeg, waiting)
        val s = settings("720", true)
        assertEquals(Expiry.Outcome.DROPPED_AND_SAVED, expiry.discardIfExpired(s, takenMillis + 12 * 3_600_000L))
        assertEquals(1, stamped)
        val (bytes, name, at) = saved.single()
        assertArrayEquals("the stamped file, not the camera JPEG", jpeg.bytes.reversedArray(), bytes)
        assertTrue(name.startsWith("report_stamped_"))
        assertEquals(takenMillis, at)
        assertFalse(store.exists())
    }

    @Test
    fun aRefusedGalleryWriteStillDropsThePhoto() {
        galleryRefuses = true
        store.saveReady(stampedFile(), jpeg, ready)
        assertEquals(Expiry.Outcome.DROPPED, expiry.discardIfExpired(settings("1", true), takenMillis + 60_000L))
        assertFalse(store.exists())
    }

    private fun assertArrayEquals(
        message: String,
        expected: ByteArray,
        actual: ByteArray,
    ) = org.junit.Assert.assertArrayEquals(message, expected, actual)

    private fun assertArrayEquals(
        expected: ByteArray,
        actual: ByteArray,
    ) = org.junit.Assert.assertArrayEquals(expected, actual)
}
