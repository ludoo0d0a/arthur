package fr.geoking.arthur.audio

import kotlin.random.Random

/**
 * Small performance imperfections so repeated notes don't sound machine-identical:
 * velocity jitter and a short late-only onset delay (a note can't fire before the grid tick).
 */
internal class Humanizer(private val rng: Random) {

    private class Pending(val dueSample: Long, val action: () -> Unit)

    private val pending = ArrayList<Pending>(8)

    /** Base velocity with ± [spread] relative jitter, clamped to a sane range. */
    fun velocity(base: Float, spread: Float = 0.14f): Float =
        (base * (1f + (rng.nextFloat() * 2f - 1f) * spread)).coerceIn(0.05f, 1f)

    /** Onset delay in samples, 0..[maxMs]; skewed small so most notes stay tight. */
    fun delaySamples(sampleRate: Int, maxMs: Float = 24f): Int {
        val r = rng.nextFloat()
        return (r * r * maxMs * sampleRate / 1000f).toInt()
    }

    fun schedule(nowSample: Long, delaySamples: Int, action: () -> Unit) {
        if (delaySamples <= 0) action() else pending.add(Pending(nowSample + delaySamples, action))
    }

    /** Fire every action that is due at [nowSample]. */
    fun tick(nowSample: Long) {
        if (pending.isEmpty()) return
        var i = 0
        while (i < pending.size) {
            val p = pending[i]
            if (p.dueSample <= nowSample) {
                pending.removeAt(i)
                p.action()
            } else {
                i++
            }
        }
    }

    /** Samples until the next pending onset, or [Int.MAX_VALUE] when idle. */
    fun samplesUntilNext(nowSample: Long): Int {
        if (pending.isEmpty()) return Int.MAX_VALUE
        var min = Long.MAX_VALUE
        for (p in pending) if (p.dueSample < min) min = p.dueSample
        return (min - nowSample).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
    }

    fun clear() = pending.clear()
}
