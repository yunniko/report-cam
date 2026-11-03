package cz.julienika.reportcam.photo

import cz.julienika.reportcam.stamp.FixQuality
import cz.julienika.reportcam.stamp.LocationReading
import cz.julienika.reportcam.stamp.StampData
import cz.julienika.reportcam.stamp.StampKeys
import cz.julienika.reportcam.stamp.StampSpec
import java.text.Normalizer
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Everything on the stamp, also as file metadata (Owner, 2026-11-03). Built from the same fields that
 * draw the stamp, so a new field reaches the metadata with no change here. See D006.
 */
data class PhotoMetadata(
    /** Field id to the lines it printed, in stamp order. */
    val fields: List<Pair<String, List<String>>>,
    val data: StampData,
) {
    val stampText: String get() = fields.flatMap { it.second }.joinToString("\n")

    /** EXIF ImageDescription is ASCII by specification: accents stripped, stamp signs spelled out, the rest '?'. */
    val asciiDescription: String
        get() =
            Normalizer
                .normalize(
                    fields
                        .flatMap { it.second }
                        .joinToString(" | ")
                        .replace("±", "+/-")
                        .replace("≈", "~")
                        .replace(Regex("[‒-―]"), "-"),
                    Normalizer.Form.NFD,
                ).replace(Regex("\\p{Mn}+"), "")
                .map { if (it.code in 32..126) it else '?' }
                .joinToString("")

    /**
     * The XMP packet: dc:description holds the full stamp text, rc:* each field and the raw fix. It is pure
     * ASCII with every other character as an XML character reference, because ExifInterface stores the XMP
     * tag through an ASCII conversion that turns them into '?'. Any XMP reader decodes the references.
     */
    fun xmp(): String {
        val sb = StringBuilder()
        sb.append("<x:xmpmeta xmlns:x=\"adobe:ns:meta/\">")
        sb.append("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">")
        sb.append("<rdf:Description rdf:about=\"\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:rc=\"$NS\">")
        sb.append("<dc:description><rdf:Alt><rdf:li xml:lang=\"x-default\">")
        sb.append(esc(stampText)).append("</rdf:li></rdf:Alt></dc:description>")
        for ((id, lines) in fields) {
            if (lines.isNotEmpty()) sb.append("<rc:").append(id).append('>').append(esc(lines.joinToString("\n"))).append("</rc:").append(id).append('>')
        }
        val reading = data[StampKeys.LOCATION]
        if (reading is LocationReading.Fix) {
            val f = reading.fix
            val at = data[StampKeys.CAPTURE_TIME]?.toInstant()?.toEpochMilli() ?: f.timeMillis
            val q = FixQuality.of(f, at)
            sb.append("<rc:latitude>").append(f.latitude).append("</rc:latitude>")
            sb.append("<rc:longitude>").append(f.longitude).append("</rc:longitude>")
            f.accuracyM?.let { sb.append("<rc:accuracyM>").append(it).append("</rc:accuracyM>") }
            sb.append("<rc:fixTime>").append(ISO.format(Instant.ofEpochMilli(f.timeMillis))).append("</rc:fixTime>")
            sb.append("<rc:fixAgeS>").append(q.ageMillis / 1000).append("</rc:fixAgeS>")
            sb.append("<rc:approximate>").append(q.approximate).append("</rc:approximate>")
        } else if (reading == LocationReading.None) {
            sb.append("<rc:location>unavailable</rc:location>")
        }
        sb.append("</rdf:Description></rdf:RDF></x:xmpmeta>")
        return asciiXml(sb.toString())
    }

    companion object {
        const val NS = "https://julienika.cz/ns/reportcam/1.0/"
        private val ISO = DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC)

        fun of(
            spec: StampSpec,
            data: StampData,
        ) = PhotoMetadata(spec.fields.map { it.id to it.lines(data) }, data)

        /** Replaces every non-ASCII code point with a numeric character reference. */
        private fun asciiXml(s: String): String {
            val out = StringBuilder(s.length)
            s.codePoints().forEach { cp ->
                if (cp in 0..126) {
                    out.append(cp.toChar())
                } else {
                    out.append("&#x").append(Integer.toHexString(cp).uppercase()).append(';')
                }
            }
            return out.toString()
        }

        private fun esc(s: String) =
            s
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
    }
}
