package cz.julienika.reportcam.review

import cz.julienika.reportcam.camera.CapturedJpeg
import cz.julienika.reportcam.photo.AspectRatio
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.ZonedDateTime
import java.util.Properties

/** What a waiting photo needs to be stamped later, besides its pixels. */
data class PendingMeta(
    val captureTime: ZonedDateTime,
    val userText: String,
    val rotationDegrees: Int,
    val outputLongEdge: Int,
    val aspect: AspectRatio,
) {
    fun toProperties() =
        Properties().apply {
            setProperty(TIME, captureTime.toString())
            setProperty(TEXT, userText)
            setProperty(ROTATION, rotationDegrees.toString())
            setProperty(LONG_EDGE, outputLongEdge.toString())
            setProperty(ASPECT, aspect.id)
        }

    companion object {
        private const val TIME = "capture_time"
        private const val TEXT = "user_text"
        private const val ROTATION = "rotation"
        private const val LONG_EDGE = "long_edge"
        private const val ASPECT = "aspect"

        /** Null when anything is missing or unreadable: a broken entry is dropped, never half-restored. */
        fun fromProperties(p: Properties): PendingMeta? =
            runCatching {
                PendingMeta(
                    captureTime = ZonedDateTime.parse(p.getProperty(TIME)),
                    userText = p.getProperty(TEXT) ?: "",
                    rotationDegrees = p.getProperty(ROTATION).toInt(),
                    outputLongEdge = p.getProperty(LONG_EDGE).toInt(),
                    aspect = AspectRatio.of(p.getProperty(ASPECT) ?: ""),
                )
            }.getOrNull()
    }
}

/** The kept stamped photo, the camera JPEG it was drawn from, and its review state. */
class KeptReady(
    val stamped: File,
    val source: CapturedJpeg,
    val meta: ReadyMeta,
)

/**
 * Keeps the one photo under review in app-private storage, so leaving the app or a restart does not lose
 * it (Owner, 2026-11-03; D010, D015). Two entries, at most one of them live: a photo waiting for a precise
 * fix (the camera's own JPEG bytes, not a re-encode, so the restored photo is the one that would have been
 * stamped), or a stamped photo not yet sent. Plain JVM I/O: unit-tested.
 */
class PendingStore(
    private val dir: File,
) {
    private val photo get() = File(dir, "photo.jpg")
    private val meta get() = File(dir, "meta.properties")
    private val stamped get() = File(dir, "stamped.jpg")
    private val ready get() = File(dir, "ready.properties")
    private val source get() = File(dir, "source.jpg")

    /** Cheap enough for the main thread: a few file checks. Every method holds the store's lock. */
    @Synchronized
    fun exists(): Boolean = hasWaiting() || hasReady()

    @Synchronized
    fun hasWaiting(): Boolean = meta.isFile && photo.isFile

    @Synchronized
    fun hasReady(): Boolean = ready.isFile && stamped.isFile && source.isFile

    /** Writes the photo, then the metadata last via a rename, so a save cut short never looks complete. */
    @Synchronized
    fun save(
        jpeg: CapturedJpeg,
        m: PendingMeta,
    ) {
        clear()
        dir.mkdirs()
        photo.writeBytes(jpeg.bytes)
        val tmp = File(dir, "meta.tmp")
        tmp.outputStream().use { m.toProperties().store(it, null) }
        check(tmp.renameTo(meta)) { "could not finish saving the waiting photo" }
    }

    /**
     * When the kept photo, stamped or waiting, was taken; null when there is none or its state does not read.
     * Reads the small state file only, so a start can ask before the pixels are loaded (G-007).
     */
    @Synchronized
    fun captureTime(): ZonedDateTime? =
        when {
            hasReady() -> readProperties(ready)?.let(ReadyMeta::fromProperties)?.captureTime
            hasWaiting() -> readProperties(meta)?.let(PendingMeta::fromProperties)?.captureTime
            else -> null
        }

    @Synchronized
    fun load(): Pair<CapturedJpeg, PendingMeta>? {
        if (!hasWaiting()) return null
        val m = readProperties(meta)?.let(PendingMeta::fromProperties)
        if (m == null) {
            clearWaiting()
            return null
        }
        return CapturedJpeg(photo.readBytes(), m.rotationDegrees) to m
    }

    /**
     * Keeps [file] as the stamped photo under review, with the camera JPEG it was drawn from, then drops
     * the waiting entry: a stop in between leaves both, and the stamped one wins on restore. Nothing is
     * written unless [still] holds, checked under the lock, so a photo the user dropped meanwhile is never kept.
     */
    @Synchronized
    fun saveReady(
        file: File,
        jpeg: CapturedJpeg,
        m: ReadyMeta,
        still: () -> Boolean = { true },
    ) {
        if (!still()) return
        clearReady()
        dir.mkdirs()
        source.writeBytes(jpeg.bytes)
        file.copyTo(stamped, overwrite = true)
        writeReady(m)
        clearWaiting()
    }

    /** The same photo stamped again (another scheme or note): its new file and state; the source stays. */
    @Synchronized
    fun replaceStamped(
        file: File,
        m: ReadyMeta,
        still: () -> Boolean = { true },
    ) {
        if (!still() || !hasReady()) return
        file.copyTo(stamped, overwrite = true)
        writeReady(m)
    }

    /** New texts, switches or saved versions for the stamped photo already kept; nothing when there is none. */
    @Synchronized
    fun updateReady(
        m: ReadyMeta,
        still: () -> Boolean = { true },
    ) {
        if (still() && stamped.isFile) writeReady(m)
    }

    /** The kept stamped file, its source and its state; a broken entry is dropped. */
    @Synchronized
    fun loadReady(): KeptReady? {
        if (!hasReady()) return null
        val m = readProperties(ready)?.let(ReadyMeta::fromProperties)
        if (m == null) {
            clearReady()
            return null
        }
        return KeptReady(stamped, CapturedJpeg(source.readBytes(), m.rotationDegrees), m)
    }

    @Synchronized
    fun clear() {
        clearWaiting()
        clearReady()
    }

    @Synchronized
    fun clearWaiting() {
        meta.delete()
        photo.delete()
        File(dir, "meta.tmp").delete()
    }

    private fun clearReady() {
        ready.delete()
        stamped.delete()
        source.delete()
        File(dir, "ready.tmp").delete()
    }

    private fun writeReady(m: ReadyMeta) {
        val tmp = File(dir, "ready.tmp")
        tmp.outputStream().use { m.toProperties().store(it, null) }
        // Replaces the old state in one step, so a stop mid-write keeps the previous texts and flags.
        Files.move(tmp.toPath(), ready.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }

    private fun readProperties(f: File): Properties? =
        runCatching { f.inputStream().use { Properties().apply { load(it) } } }.getOrNull()
}
