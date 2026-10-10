package fr.geoking.arthur.audio.markov

/**
 * Shared swing eighth-note grid for jazz/lounge styles.
 *
 * Straight eighth index [n] maps to sample time with delayed offbeats:
 * - even (onbeat): `n * eighthStraight`
 * - odd (offbeat): `(n - 1) * eighthStraight + swingRatio * eighthStraight`
 *
 * Default [swingRatio] 0.67 ≈ classic 2:1 long–short feel.
 */
class GrooveTransport(
    tempoBpm: Float,
    sampleRate: Int,
    val swingRatio: Float = DEFAULT_SWING,
    val beatsPerBar: Int = 4,
) {
    private val bpm = tempoBpm.coerceAtLeast(30f)
    private val sr = sampleRate.coerceAtLeast(8_000)
    private val secondsPerQuarter = 60.0 / bpm
    private val eighthStraight = secondsPerQuarter * 0.5 * sr

    /** Absolute sample time of swung eighth [index] (0-based). */
    fun sampleAtEighth(index: Long): Long {
        if (index <= 0L) return 0L
        val even = index and 1L == 0L
        return if (even) {
            (index * eighthStraight).toLong()
        } else {
            ((index - 1) * eighthStraight + swingRatio * eighthStraight).toLong()
        }
    }

    /** Sample time of quarter [index] (aligned to even eighths). */
    fun sampleAtQuarter(index: Long): Long = sampleAtEighth(index.coerceAtLeast(0L) * 2)

    /** Sample time of bar [index] (bar = [beatsPerBar] quarters). */
    fun sampleAtBar(index: Long): Long =
        sampleAtQuarter(index.coerceAtLeast(0L) * beatsPerBar)

    fun eighthDuration(fromEighthIndex: Long): Int {
        val a = sampleAtEighth(fromEighthIndex)
        val b = sampleAtEighth(fromEighthIndex + 1)
        return (b - a).toInt().coerceAtLeast(1)
    }

    fun quarterDuration(fromQuarterIndex: Long): Int {
        val a = sampleAtQuarter(fromQuarterIndex)
        val b = sampleAtQuarter(fromQuarterIndex + 1)
        return (b - a).toInt().coerceAtLeast(1)
    }

    fun barsDuration(fromBarIndex: Long, bars: Int): Int {
        val n = bars.coerceAtLeast(1)
        val a = sampleAtBar(fromBarIndex)
        val b = sampleAtBar(fromBarIndex + n)
        return (b - a).toInt().coerceAtLeast(1)
    }

    fun samplesUntilNextEighth(fromSample: Long): Int {
        var i = 0L
        while (sampleAtEighth(i) < fromSample && i < 1_000_000L) i++
        return (sampleAtEighth(i) - fromSample).toInt().coerceAtLeast(0)
    }

    fun samplesUntilNextQuarter(fromSample: Long): Int {
        var i = 0L
        while (sampleAtQuarter(i) < fromSample && i < 1_000_000L) i++
        return (sampleAtQuarter(i) - fromSample).toInt().coerceAtLeast(0)
    }

    fun samplesUntilBar(fromSample: Long): Int {
        var i = 0L
        while (sampleAtBar(i) < fromSample && i < 1_000_000L) i++
        return (sampleAtBar(i) - fromSample).toInt().coerceAtLeast(0)
    }

    fun eighthIndexAt(sample: Long): Long {
        var i = 0L
        while (sampleAtEighth(i + 1) <= sample && i < 1_000_000L) i++
        return i
    }

    fun quarterIndexAt(sample: Long): Long = eighthIndexAt(sample) / 2

    fun barIndexAt(sample: Long): Long = quarterIndexAt(sample) / beatsPerBar

    companion object {
        const val DEFAULT_SWING = 0.67f
    }
}
