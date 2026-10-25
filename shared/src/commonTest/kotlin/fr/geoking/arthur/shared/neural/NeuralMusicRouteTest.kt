package fr.geoking.arthur.shared.neural

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NeuralMusicRouteTest {
    private val packId = "pack_museums_classical"

    @Test
    fun back_fromHub_exitsStack() {
        assertNull(NeuralMusicRoute.Hub(packId).back())
    }

    @Test
    fun back_fromCreate_returnsHub() {
        assertEquals(NeuralMusicRoute.Hub(packId), NeuralMusicRoute.Create(packId).back())
    }

    @Test
    fun back_fromModelSetup_returnsCreateByDefault() {
        val setup = NeuralMusicRoute.ModelSetup(packId, promptId = "mg_01")
        assertEquals(NeuralMusicRoute.Create(packId), setup.back())
    }

    @Test
    fun back_fromGenerating_returnsCreate() {
        val gen = NeuralMusicRoute.Generating(packId, promptId = "mg_01", durationSec = 60)
        assertEquals(NeuralMusicRoute.Create(packId), gen.back())
    }

    @Test
    fun back_fromResult_returnsHub() {
        assertEquals(
            NeuralMusicRoute.Hub(packId),
            NeuralMusicRoute.Result(packId, takeId = "t1").back(),
        )
    }

    @Test
    fun routeAfterGenerateRequest_whenModelReady_goesGenerating() {
        val next = routeAfterGenerateRequest(packId, "mg_01", 60, modelReady = true)
            as NeuralMusicRoute.Generating
        assertEquals(60, next.durationSec)
    }

    @Test
    fun routeAfterGenerateRequest_whenModelMissing_goesSetup() {
        val next = routeAfterGenerateRequest(packId, "mg_01", 120, modelReady = false)
            as NeuralMusicRoute.ModelSetup
        assertEquals("mg_01", next.promptId)
    }


    @Test
    fun modelSetup_continueToGenerating_keepsBrief() {
        val setup = NeuralMusicRoute.ModelSetup(packId, "mg_02", durationSec = 30)
        val gen = setup.continueToGenerating()
        assertEquals(packId, gen.packId)
        assertEquals("mg_02", gen.promptId)
        assertEquals(30, gen.durationSec)
    }

    @Test
    fun inMemoryLibrary_listsNewestFirst() {
        val lib = InMemoryNeuralTakeLibrary()
        lib.save(
            NeuralTake("a", packId, "mg_01", 60, createdAtEpochMs = 1L, localPath = "/a"),
        )
        lib.save(
            NeuralTake("b", packId, "mg_02", 60, createdAtEpochMs = 2L, localPath = "/b"),
        )
        assertEquals(listOf("b", "a"), lib.listForPack(packId).map { it.id })
        assertEquals("b", lib.get("b")?.id)
        lib.delete("b")
        assertNull(lib.get("b"))
    }
}
