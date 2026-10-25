package fr.geoking.arthur.shared.neural

import kotlinx.serialization.Serializable

/** One generated Neural music take stored for the hub library. */
@Serializable
data class NeuralTake(
    val id: String,
    val packId: String,
    val promptId: String,
    val durationSec: Int,
    val createdAtEpochMs: Long,
    /** Absolute path to the generated audio file on device. */
    val localPath: String,
)

/** Persistence for Neural takes (filesystem impl lives in androidApp). */
interface NeuralTakeLibrary {
    fun listForPack(packId: String): List<NeuralTake>

    fun get(takeId: String): NeuralTake?

    fun save(take: NeuralTake)

    fun delete(takeId: String)
}

/** In-memory library for unit tests. */
class InMemoryNeuralTakeLibrary : NeuralTakeLibrary {
    private val takes = linkedMapOf<String, NeuralTake>()

    override fun listForPack(packId: String): List<NeuralTake> =
        takes.values
            .filter { it.packId == packId }
            .sortedByDescending { it.createdAtEpochMs }

    override fun get(takeId: String): NeuralTake? = takes[takeId]

    override fun save(take: NeuralTake) {
        takes[take.id] = take
    }

    override fun delete(takeId: String) {
        takes.remove(takeId)
    }
}
