package cz.julienika.reportcam.photo

import androidx.exifinterface.media.ExifInterface
import cz.julienika.reportcam.BuildConfig
import cz.julienika.reportcam.stamp.LocationReading
import cz.julienika.reportcam.stamp.StampKeys
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.roundToLong

/** Writes [PhotoMetadata] into a JPEG file in place: standard EXIF tags plus the XMP packet. */
object ExifWriter {
    private val EXIF_TIME = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")
    private val GPS_DATE = DateTimeFormatter.ofPattern("yyyy:MM:dd").withZone(ZoneOffset.UTC)
    private val GPS_TIME = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneOffset.UTC)

    fun write(
        file: File,
        meta: PhotoMetadata,
    ) {
        val exif = ExifInterface(file)
        exif.setAttribute(ExifInterface.TAG_SOFTWARE, "Report Cam ${BuildConfig.VERSION_NAME}")
        exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, meta.asciiDescription)
        meta.data[StampKeys.CAPTURE_TIME]?.let { t ->
            val local = EXIF_TIME.format(t)
            val offset = t.offset.id.let { if (it == "Z") "+00:00" else it }
            exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, local)
            exif.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, local)
            exif.setAttribute(ExifInterface.TAG_DATETIME, local)
            exif.setAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL, offset)
            exif.setAttribute(ExifInterface.TAG_OFFSET_TIME, offset)
        }
        val reading = meta.data[StampKeys.LOCATION]
        if (reading is LocationReading.Fix) {
            val f = reading.fix
            exif.setLatLong(f.latitude, f.longitude)
            f.accuracyM?.let {
                exif.setAttribute(ExifInterface.TAG_GPS_H_POSITIONING_ERROR, "${(it * 100).roundToLong()}/100")
            }
            val fixAt = Instant.ofEpochMilli(f.timeMillis)
            exif.setAttribute(ExifInterface.TAG_GPS_DATESTAMP, GPS_DATE.format(fixAt))
            exif.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP, GPS_TIME.format(fixAt))
        }
        exif.setAttribute(ExifInterface.TAG_XMP, meta.xmp())
        exif.saveAttributes()
    }
}
