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

        fun of(data: StampData) =
            StampFacts(
                captureTime = data[StampKeys.CAPTURE_TIME] ?: ZonedDateTime.now(),
                userText = data[StampKeys.USER_TEXT],
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
            return StampFacts(ZonedDateTime.parse(p.getProperty(TIME)), p.getProperty(TEXT), location, place)
        }
    }
}

/** A stamped photo under review that was not sent yet: what the review screen showed for it, and how to redraw it. */
data class ReadyMeta(
    /** The shared file's name, so the restored copy is the same file a messenger would get. */
    val fileName: String,
    /** The caption text as the user left it, edits included. */
    val caption: String,
    val captionOn: Boolean,
    /** True once this version is in the gallery: Save and send-time saving then add no second copy. */
    val saved: Boolean,
    val scheme: SchemeId,
    /** How the kept camera JPEG becomes the photo: the same shape and size as at the shutter. */
    val rotationDegrees: Int,
    val outputLongEdge: Int,
    val aspect: AspectRatio,
    val facts: StampFacts,
) {
    val captureTime: ZonedDateTime get() = facts.captureTime

    fun toProperties() =
        Properties().apply {
            setProperty(FILE, fileName)
            setProperty(CAPTION, caption)
            setProperty(CAPTION_ON, captionOn.toString())
            setProperty(SAVED, saved.toString())
            setProperty(SCHEME, scheme.id)
            setProperty(ROTATION, rotationDegrees.toString())
            setProperty(LONG_EDGE, outputLongEdge.toString())
            setProperty(ASPECT, aspect.id)
            facts.writeTo(this)
        }

    companion object {
        private const val FILE = "file"
        private const val CAPTION = "caption"
        private const val CAPTION_ON = "caption_on"
        private const val SAVED = "saved"
        private const val SCHEME = "scheme"
        private const val ROTATION = "rotation"
        private const val LONG_EDGE = "long_edge"
        private const val ASPECT = "aspect"

        /** Null when anything is missing or unreadable: a broken entry is dropped, never half-restored. */
        fun fromProperties(p: Properties): ReadyMeta? =
            runCatching {
                val file = p.getProperty(FILE)
                // A name only, never a path: it is resolved inside the shared folder on restore.
                require(!file.isNullOrBlank() && File(file).name == file)
                ReadyMeta(
                    fileName = file,
                    caption = p.getProperty(CAPTION) ?: "",
                    captionOn = p.getProperty(CAPTION_ON).toBooleanStrict(),
                    saved = p.getProperty(SAVED).toBooleanStrict(),
                    scheme = requireNotNull(SchemeId.of(p.getProperty(SCHEME) ?: "")),
                    rotationDegrees = p.getProperty(ROTATION).toInt(),
                    outputLongEdge = p.getProperty(LONG_EDGE).toInt(),
                    aspect = AspectRatio.of(p.getProperty(ASPECT) ?: ""),
                    facts = StampFacts.readFrom(p),
                )
            }.getOrNull()
    }
}
