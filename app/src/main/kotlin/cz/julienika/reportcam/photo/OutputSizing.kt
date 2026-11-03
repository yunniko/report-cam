package cz.julienika.reportcam.photo

/** Pure size arithmetic for the output photo; no Android types so it is unit-tested on the JVM. */
object OutputSizing {
    /** Largest power-of-two sample size that still leaves the long edge at or above [targetLongEdge]. */
    fun sampleSize(
        srcLongEdge: Int,
        targetLongEdge: Int,
    ): Int {
        if (targetLongEdge <= 0 || srcLongEdge <= targetLongEdge) return 1
        var sample = 1
        while (srcLongEdge / (sample * 2) >= targetLongEdge) sample *= 2
        return sample
    }

    /** Scale factor that brings the long edge down to [targetLongEdge]; never enlarges. */
    fun scale(
        srcLongEdge: Int,
        targetLongEdge: Int,
    ): Float =
        if (targetLongEdge <= 0 || srcLongEdge <= targetLongEdge) {
            1f
        } else {
            targetLongEdge.toFloat() / srcLongEdge
        }
}
