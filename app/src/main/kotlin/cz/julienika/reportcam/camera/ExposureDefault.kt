package cz.julienika.reportcam.camera

import kotlin.math.roundToInt

/** Maps the default EV from settings onto a camera's compensation steps; pure, so it is unit-tested. */
object ExposureDefault {
    /**
     * The compensation index nearest to [ev] for a camera with [stepEv] per index and the index range
     * [lower]..[upper]. A camera that cannot reach [ev] gets its nearest end.
     */
    fun index(
        ev: Float,
        stepEv: Float,
        lower: Int,
        upper: Int,
    ): Int {
        if (stepEv <= 0f || lower > upper) return 0 // no usable compensation: leave it at zero
        return (ev / stepEv).roundToInt().coerceIn(lower, upper)
    }
}
