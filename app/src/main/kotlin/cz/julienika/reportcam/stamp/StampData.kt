package cz.julienika.reportcam.stamp

import java.time.ZonedDateTime

/** A typed key into [StampData]. A new kind of data is a new key plus whatever fills it. */
class StampKey<T : Any>(
    val name: String,
) {
    override fun toString() = "StampKey($name)"
}

/**
 * The facts collected at capture time. Fields read from it; data sources fill it. Absent means
 * "not collected" (for location: permission denied), which a field renders as nothing at all.
 */
class StampData private constructor(
    private val values: Map<StampKey<*>, Any>,
) {
    @Suppress("UNCHECKED_CAST")
    operator fun <T : Any> get(key: StampKey<T>): T? = values[key] as T?

    fun <T : Any> with(
        key: StampKey<T>,
        value: T?,
    ): StampData = if (value == null) StampData(values - key) else StampData(values + (key to value))

    companion object {
        val EMPTY = StampData(emptyMap())
    }
}

object StampKeys {
    val CAPTURE_TIME = StampKey<ZonedDateTime>("captureTime")
    val USER_TEXT = StampKey<String>("userText")

    /** Present whenever location permission is granted; [LocationReading.None] if no fix arrived. */
    val LOCATION = StampKey<LocationReading>("location")
}
