package cz.julienika.reportcam.review

import cz.julienika.reportcam.camera.CapturedJpeg
import cz.julienika.reportcam.photo.AspectRatio
import java.io.File
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

/**
 * Keeps the one photo waiting for a precise fix in app-private storage, so leaving the app or a restart
 * does not lose it (Owner, 2026-11-03; D010). The camera's own JPEG bytes are kept, not a re-encode, so
 * the restored photo is the same as the one that would have been stamped. Plain JVM I/O: unit-tested.
 */
class PendingStore(
    private val dir: File,
) {
    private val photo get() = File(dir, "photo.jpg")
    private val meta get() = File(dir, "meta.properties")

    /** Cheap enough for the main thread: one file check. */
    fun exists(): Boolean = meta.isFile && photo.isFile

    /** Writes the photo, then the metadata last via a rename, so a save cut short never looks complete. */
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

    fun load(): Pair<CapturedJpeg, PendingMeta>? {
        if (!exists()) return null
        val m =
            runCatching { meta.inputStream().use { Properties().apply { load(it) } } }
                .getOrNull()
                ?.let(PendingMeta::fromProperties)
        if (m == null) {
            clear()
            return null
        }
        return CapturedJpeg(photo.readBytes(), m.rotationDegrees) to m
    }

    fun clear() {
        meta.delete()
        photo.delete()
        File(dir, "meta.tmp").delete()
    }
}
