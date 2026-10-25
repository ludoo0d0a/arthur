package fr.geoking.arthur.audio.neural

import android.content.Context
import fr.geoking.arthur.shared.neural.NeuralTake
import fr.geoking.arthur.shared.neural.NeuralTakeLibrary
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

/** Persists Neural takes as JSON under app filesDir. */
class FileNeuralTakeLibrary(
    context: Context,
    private val json: Json = Json { ignoreUnknownKeys = true; prettyPrint = false },
) : NeuralTakeLibrary {
    private val storeFile = File(context.filesDir, "neural_takes.json")
    private val lock = Any()

    override fun listForPack(packId: String): List<NeuralTake> =
        loadAll()
            .filter { it.packId == packId }
            .sortedByDescending { it.createdAtEpochMs }

    override fun get(takeId: String): NeuralTake? =
        loadAll().firstOrNull { it.id == takeId }

    override fun save(take: NeuralTake) {
        synchronized(lock) {
            val next = loadAll().filterNot { it.id == take.id } + take
            writeAll(next)
        }
    }

    override fun delete(takeId: String) {
        synchronized(lock) {
            writeAll(loadAll().filterNot { it.id == takeId })
        }
    }

    private fun loadAll(): List<NeuralTake> {
        if (!storeFile.exists()) return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(NeuralTake.serializer()), storeFile.readText())
        }.getOrDefault(emptyList())
    }

    private fun writeAll(takes: List<NeuralTake>) {
        storeFile.parentFile?.mkdirs()
        storeFile.writeText(json.encodeToString(ListSerializer(NeuralTake.serializer()), takes))
    }
}
