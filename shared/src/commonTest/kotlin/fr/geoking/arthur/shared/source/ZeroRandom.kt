package fr.geoking.arthur.shared.source

import kotlin.random.Random

/**
 * Deterministic [Random] for Source fixture tests: [nextInt] is always 0 so
 * [RemoteSample.randomPage] → 1, [RemoteSample.randomStart] → 0, and
 * [RemoteSample.sample] keeps list order (always picks remaining index 0).
 */
object ZeroRandom : Random() {
    override fun nextBits(bitCount: Int): Int = 0

    override fun nextInt(until: Int): Int {
        require(until > 0) { "until must be positive" }
        return 0
    }
}
