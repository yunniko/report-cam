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

/** A stamped photo under review that was not sent yet: what the review screen showed for it. */
data class ReadyMeta(
    /** The shared file's name, so the restored copy is the same file a messenger would get. */
    val fileName: String,
    val captureTime: ZonedDateTime,
    /** The caption text as the user left it, edits included. */
    val caption: String,
    val captionOn: Boolean,
    /** True once this version is in the gallery: Save and send-time saving then add no second copy. */
    val saved: Boolean,
) {
    fun toProperties() =
        Properties().apply {
            setProperty(FILE, fileName)
            setProperty(TIME, captureTime.toString())
            setProperty(CAPTION, caption)
            setProperty(CAPTION_ON, captionOn.toString())
            setProperty(SAVED, saved.toString())
        }

    companion object {
        private const val FILE = "file"
        private const val TIME = "capture_time"
        private const val CAPTION = "caption"
        private const val CAPTION_ON = "caption_on"
        private const val SAVED = "saved"

        fun fromProperties(p: Properties): ReadyMeta? =
            runCatching {
                val file = p.getProperty(FILE)
                // A name only, never a path: it is resolved inside the shared folder on restore.
                require(!file.isNullOrBlank() && File(file).name == file)
                ReadyMeta(
                    fileName = file,
                    captureTime = ZonedDateTime.parse(p.getProperty(TIME)),
                    caption = p.getProperty(CAPTION) ?: "",
                    captionOn = p.getProperty(CAPTION_ON).toBooleanStrict(),
                    saved = p.getProperty(SAVED).toBooleanStrict(),
                )
            }.getOrNull()
    }
}

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

    /** Cheap enough for the main thread: a few file checks. Every method holds the store's lock. */
    @Synchronized
    fun exists(): Boolean = hasWaiting() || hasReady()

    @Synchronized
    fun hasWaiting(): Boolean = meta.isFile && photo.isFile

    @Synchronized
    fun hasReady(): Boolean = ready.isFile && stamped.isFile

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
     * Keeps [file] as the stamped photo under review, then drops the waiting entry: a stop in between
     * leaves both, and the stamped one wins on restore. Nothing is written unless [still] holds, checked
     * under the lock, so a photo the user dropped meanwhile is never kept.
     */
    @Synchronized
    fun saveReady(
        file: File,
        m: ReadyMeta,
        still: () -> Boolean = { true },
    ) {
        if (!still()) return
        clearReady()
        dir.mkdirs()
        file.copyTo(stamped, overwrite = true)
        writeReady(m)
        clearWaiting()
    }

    /** New caption, switch or saved flag for the stamped photo already kept; nothing when there is none. */
    @Synchronized
    fun updateReady(
        m: ReadyMeta,
        still: () -> Boolean = { true },
    ) {
        if (still() && stamped.isFile) writeReady(m)
    }

    /** The kept stamped file and its state; a broken entry is dropped. */
    @Synchronized
    fun loadReady(): Pair<File, ReadyMeta>? {
        if (!hasReady()) return null
        val m = readProperties(ready)?.let(ReadyMeta::fromProperties)
        if (m == null) {
            clearReady()
            return null
        }
        return stamped to m
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
        File(dir, "ready.tmp").delete()
    }

    private fun writeReady(m: ReadyMeta) {
        val tmp = File(dir, "ready.tmp")
        tmp.outputStream().use { m.toProperties().store(it, null) }
        // Replaces the old state in one step, so a stop mid-write keeps the previous caption and flags.
        Files.move(tmp.toPath(), ready.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }

    private fun readProperties(f: File): Properties? =
        runCatching { f.inputStream().use { Properties().apply { load(it) } } }.getOrNull()
}
