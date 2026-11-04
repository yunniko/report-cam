package cz.julienika.reportcam.review

import cz.julienika.reportcam.photo.AspectRatio
import cz.julienika.reportcam.stamp.GeoFix
import cz.julienika.reportcam.stamp.LocationReading
import cz.julienika.reportcam.stamp.Place
import cz.julienika.reportcam.stamp.SchemeId
import cz.julienika.reportcam.stamp.StampData
import cz.julienika.reportcam.stamp.StampKeys
import java.io.File
import java.time.ZonedDateTime
import java.util.Properties

/**
 * The facts a stamp was drawn from, as plain values, so the photo can be stamped again with the other
 * scheme (G-003), after a restart too. Absent location means "no permission", as in [StampData].
 */
data class StampFacts(
    val captureTime: ZonedDateTime,
    val userText: String?,
    val location: LocationReading?,
    val place: Place?,
) {
    fun toStampData(): StampData =
        StampData.EMPTY
            .with(StampKeys.CAPTURE_TIME, captureTime)
            .with(StampKeys.USER_TEXT, userText)
            .with(StampKeys.LOCATION, location)
            .with(StampKeys.PLACE, place)

    internal fun writeTo(p: Properties) {
        p.setProperty(TIME, captureTime.toString())
        userText?.let { p.setProperty(TEXT, it) }
        when (location) {
            null -> Unit
            LocationReading.None -> p.setProperty(LOCATION, NONE)
            is LocationReading.Fix -> {
                val f = location.fix
                p.setProperty(LOCATION, FIX)
                p.setProperty(LAT, f.latitude.toString())
                p.setProperty(LON, f.longitude.toString())
                f.accuracyM?.let { p.setProperty(ACCURACY, it.toString()) }
                p.setProperty(FIX_TIME, f.timeMillis.toString())
                p.setProperty(COARSE, f.coarseOnly.toString())
            }
        }
        place?.let {
            p.setProperty(PLACE, "1")
            it.street?.let { v -> p.setProperty(STREET, v) }
            it.houseNumber?.let { v -> p.setProperty(NUMBER, v) }
            it.city?.let { v -> p.setProperty(CITY, v) }
            it.fullAddress?.let { v -> p.setProperty(ADDRESS, v) }
        }
    }

    companion object {
        private const val TIME = "capture_time"
        private const val TEXT = "data.user_text"
        private const val LOCATION = "data.location"
        private const val NONE = "none"
        private const val FIX = "fix"
        private const val LAT = "data.lat"
        private const val LON = "data.lon"
        private const val ACCURACY = "data.accuracy"
        private const val FIX_TIME = "data.fix_time"
        private const val COARSE = "data.coarse"
        private const val PLACE = "data.place"
        private const val STREET = "data.street"
        private const val NUMBER = "data.number"
        private const val CITY = "data.city"
        private const val ADDRESS = "data.address"

        /** The note as the stamp carries it: trimmed, and null when there is none. */
        fun stampedNote(text: String?): String? = text?.trim()?.ifEmpty { null }

        fun of(data: StampData) =
            StampFacts(
                captureTime = data[StampKeys.CAPTURE_TIME] ?: ZonedDateTime.now(),
                userText = stampedNote(data[StampKeys.USER_TEXT]),
                location = data[StampKeys.LOCATION],
                place = data[StampKeys.PLACE],
            )

        /** Throws on anything missing or malformed; the caller drops the entry. */
        internal fun readFrom(p: Properties): StampFacts {
            val location =
                when (p.getProperty(LOCATION)) {
                    null -> null
                    NONE -> LocationReading.None
                    FIX ->
                        LocationReading.Fix(
                            GeoFix(
                                latitude = p.getProperty(LAT).toDouble(),
                                longitude = p.getProperty(LON).toDouble(),
                                accuracyM = p.getProperty(ACCURACY)?.toFloat(),
                                timeMillis = p.getProperty(FIX_TIME).toLong(),
                                coarseOnly = p.getProperty(COARSE).toBooleanStrict(),
                            ),
                        )
                    else -> error("unknown location kind")
                }
            val place =
                p.getProperty(PLACE)?.let {
                    Place(p.getProperty(STREET), p.getProperty(NUMBER), p.getProperty(CITY), p.getProperty(ADDRESS))
                }
            return StampFacts(ZonedDateTime.parse(p.getProperty(TIME)), stampedNote(p.getProperty(TEXT)), location, place)
        }
    }
}

/** A stamped photo under review that was not sent yet: what the review screen showed for it, and how to redraw it. */
data class ReadyMeta(
    /** The shared file's name, so the restored copy is the same file a messenger would get. */
    val fileName: String,
    /** The message (text sent with the photo) as the user left it, edits included. */
    val message: String,
    val messageOn: Boolean,
    /** The note field as the user left it; what is stamped is in [facts] (`userText`). */
    val note: String,
    val noteOn: Boolean,
    /** The versions of this photo already in the gallery: Save and send-time saving add no second copy of them. */
    val savedFiles: Set<String>,
    val scheme: SchemeId,
    /** Where the stamp sits on this photo (G-005); null for an entry from 0.4, which used the Settings one. */
    val placement: String? = null,
    /** How the kept camera JPEG becomes the photo: the same shape and size as at the shutter. */
    val rotationDegrees: Int,
    val outputLongEdge: Int,
    val aspect: AspectRatio,
    val facts: StampFacts,
) {
    val captureTime: ZonedDateTime get() = facts.captureTime
    val saved: Boolean get() = fileName in savedFiles

    fun toProperties() =
        Properties().apply {
            setProperty(FILE, fileName)
            setProperty(MESSAGE, message)
            setProperty(MESSAGE_ON, messageOn.toString())
            setProperty(NOTE, note)
            setProperty(NOTE_ON, noteOn.toString())
            setProperty(SAVED_FILES, savedFiles.joinToString("/"))
            setProperty(SCHEME, scheme.id)
            placement?.let { setProperty(PLACEMENT, it) }
            setProperty(ROTATION, rotationDegrees.toString())
            setProperty(LONG_EDGE, outputLongEdge.toString())
            setProperty(ASPECT, aspect.id)
            facts.writeTo(this)
        }

    companion object {
        private const val FILE = "file"
        // The message keys keep their 0.3 names, so a photo left in review survives the update.
        private const val MESSAGE = "caption"
        private const val MESSAGE_ON = "caption_on"
        private const val NOTE = "note"
        private const val NOTE_ON = "note_on"
        private const val SAVED_FILES = "saved_files"

        /** 0.3's flag for the one version kept; read when [SAVED_FILES] is absent. */
        private const val SAVED = "saved"
        private const val SCHEME = "scheme"
        private const val PLACEMENT = "placement"
        private const val ROTATION = "rotation"
        private const val LONG_EDGE = "long_edge"
        private const val ASPECT = "aspect"

        /** Null when anything is missing or unreadable: a broken entry is dropped, never half-restored. */
        fun fromProperties(p: Properties): ReadyMeta? =
            runCatching {
                val file = p.getProperty(FILE)
                // A name only, never a path: it is resolved inside the shared folder on restore.
                require(!file.isNullOrBlank() && File(file).name == file)
                val facts = StampFacts.readFrom(p)
                val savedFiles =
                    p.getProperty(SAVED_FILES)?.split('/')?.filter { it.isNotEmpty() }?.toSet()
                        ?: if (p.getProperty(SAVED).toBooleanStrict()) setOf(file) else emptySet()
                ReadyMeta(
                    fileName = file,
                    message = p.getProperty(MESSAGE) ?: "",
                    messageOn = p.getProperty(MESSAGE_ON).toBooleanStrict(),
                    // A 0.3 entry had no note row: the note is what was stamped, switched on.
                    note = p.getProperty(NOTE) ?: facts.userText.orEmpty(),
                    noteOn = p.getProperty(NOTE_ON)?.toBooleanStrict() ?: true,
                    savedFiles = savedFiles,
                    scheme = requireNotNull(SchemeId.of(p.getProperty(SCHEME) ?: "")),
                    placement = p.getProperty(PLACEMENT),
                    rotationDegrees = p.getProperty(ROTATION).toInt(),
                    outputLongEdge = p.getProperty(LONG_EDGE).toInt(),
                    aspect = AspectRatio.of(p.getProperty(ASPECT) ?: ""),
                    facts = facts,
                )
            }.getOrNull()
    }
}
