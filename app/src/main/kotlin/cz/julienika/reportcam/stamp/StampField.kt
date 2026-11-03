package cz.julienika.reportcam.stamp

import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * One piece of the stamp. A field turns [StampData] into zero or more text lines and knows nothing
 * about layout or drawing, so a new field (an address, a project code) is one class and a registry entry.
 */
interface StampField {
    val id: String

    fun lines(data: StampData): List<String>
}

/** The free text from Settings; each line of it is a stamp line. */
class TextField : StampField {
    override val id = "text"

    override fun lines(data: StampData): List<String> =
        data[StampKeys.USER_TEXT]
            ?.lines()
            ?.map { it.trimEnd() }
            ?.filter { it.isNotBlank() }
            .orEmpty()
}

class DateTimeField(
    pattern: String,
    locale: Locale = Locale.getDefault(),
) : StampField {
    override val id = "datetime"
    private val formatter = DateTimeFormatter.ofPattern(pattern, locale)

    override fun lines(data: StampData): List<String> =
        data[StampKeys.CAPTURE_TIME]?.let { listOf(formatter.format(it)) }.orEmpty()
}

enum class CoordinateFormat { DECIMAL, DMS }

/** Words the location line needs, supplied from string resources so the engine stays plain Kotlin. */
data class LocationLabels(
    val unavailable: String = "Location unavailable",
    val approximate: String = "approximate",
    /** Takes the age in whole minutes. */
    val minutesOld: (Int) -> String = { "$it min old" },
    /** Takes the age in whole hours. */
    val hoursOld: (Int) -> String = { "$it h old" },
)

/**
 * Coordinates, with accuracy, and marks for approximate and old fixes (Owner, 2026-11-03). With no
 * location permission there is no LOCATION entry and this field prints nothing.
 */
class CoordinatesField(
    private val format: CoordinateFormat = CoordinateFormat.DECIMAL,
    private val labels: LocationLabels = LocationLabels(),
    private val nowMillis: () -> Long = System::currentTimeMillis,
    /** True when another line already says where the photo was, so coordinates are left off the stamp (D012). */
    private val hiddenWhen: (StampData) -> Boolean = { false },
) : StampField {
    override val id = "coordinates"

    override fun lines(data: StampData): List<String> =
        if (hiddenWhen(data)) emptyList() else describeReading(data)

    private fun describeReading(data: StampData): List<String> =
        when (val reading = data[StampKeys.LOCATION]) {
            null -> emptyList()
            LocationReading.None -> listOf(labels.unavailable)
            is LocationReading.Fix -> listOf(describe(reading.fix, data))
        }

    private fun describe(
        fix: GeoFix,
        data: StampData,
    ): String {
        // Age is measured against the capture time, not the moment of rendering.
        val at = data[StampKeys.CAPTURE_TIME]?.toInstant()?.toEpochMilli() ?: nowMillis()
        val q = FixQuality.of(fix, at)
        val sb = StringBuilder()
        if (q.approximate) sb.append(APPROX_SIGN)
        sb.append(position(fix.latitude, fix.longitude, format))
        fix.accuracyM?.let { sb.append(" ").append(PLUS_MINUS).append(formatDistance(it)) }
        val notes = mutableListOf<String>()
        if (q.approximate) notes += labels.approximate
        if (q.stale) notes += formatAge(q.ageMillis)
        if (notes.isNotEmpty()) sb.append(" (").append(notes.joinToString(", ")).append(')')
        return sb.toString()
    }

    private fun formatAge(ms: Long): String {
        val minutes = (ms / 60_000).toInt()
        return if (minutes < 120) labels.minutesOld(minutes) else labels.hoursOld(minutes / 60)
    }

    companion object {
        const val APPROX_SIGN = '≈'
        const val PLUS_MINUS = '±'

        /** The coordinates alone, in the chosen format; shared with the review screen's live readout. */
        fun position(
            lat: Double,
            lon: Double,
            format: CoordinateFormat,
        ): String =
            when (format) {
                CoordinateFormat.DECIMAL -> String.format(Locale.ROOT, "%.5f, %.5f", lat, lon)
                CoordinateFormat.DMS -> "${dms(lat, 'N', 'S')} ${dms(lon, 'E', 'W')}"
            }

        fun formatDistance(m: Float): String =
            if (m < 1000f) "${m.roundToInt()} m" else String.format(Locale.ROOT, "%.1f km", m / 1000f)

        /** Degrees, minutes and seconds to a tenth, e.g. 50°05'15.5"N. */
        fun dms(
            value: Double,
            positive: Char,
            negative: Char,
        ): String {
            val hemi = if (value >= 0) positive else negative
            var tenths = (abs(value) * 36_000).roundToInt() // tenths of an arc-second
            val deg = tenths / 36_000
            tenths -= deg * 36_000
            val min = tenths / 600
            val secTenths = tenths - min * 600
            return String.format(Locale.ROOT, "%d°%02d'%04.1f\"%c", deg, min, secTenths / 10.0, hemi)
        }
    }
}
