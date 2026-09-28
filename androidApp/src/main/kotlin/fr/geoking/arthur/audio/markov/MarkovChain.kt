package fr.geoking.arthur.audio.markov

import kotlin.random.Random

/**
 * Order-1 Markov chain over discrete states.
 * Transition rows are normalized; empty rows fall back to uniform over all states.
 */
class MarkovChain<T>(
    private val states: List<T>,
    private val transitions: Map<T, Map<T, Float>>,
    private val random: Random,
    initial: T? = null,
) {
    init {
        require(states.isNotEmpty()) { "MarkovChain needs states" }
    }

    var current: T = initial ?: states[random.nextInt(states.size)]
        private set

    fun next(): T {
        val row = transitions[current].orEmpty()
        val pick = if (row.isEmpty()) {
            states[random.nextInt(states.size)]
        } else {
            weightedPick(row)
        }
        current = pick
        return pick
    }

    fun reset(state: T? = null) {
        current = state ?: states[random.nextInt(states.size)]
    }

    private fun weightedPick(row: Map<T, Float>): T {
        val total = row.values.sum().coerceAtLeast(1e-6f)
        var r = random.nextFloat() * total
        for ((state, weight) in row) {
            r -= weight
            if (r <= 0f) return state
        }
        return row.keys.last()
    }

    companion object {
        /** Build a fully connected chain with self-loop bias so there are no absorbing traps. */
        fun <T> connected(
            states: List<T>,
            random: Random,
            selfBias: Float = 0.35f,
            initial: T? = null,
        ): MarkovChain<T> {
            val transitions = states.associateWith { from ->
                states.associateWith { to ->
                    if (from == to) selfBias else (1f - selfBias) / (states.size - 1).coerceAtLeast(1)
                }
            }
            return MarkovChain(states, transitions, random, initial)
        }
    }
}
