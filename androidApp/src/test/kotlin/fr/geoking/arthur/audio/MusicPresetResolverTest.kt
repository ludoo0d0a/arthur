package fr.geoking.arthur.audio

import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.source.GenartSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicPresetResolverTest {

    @Test
    fun sameIdYieldsSamePreset() {
        val art = Artwork(
            id = GenartSource.WAVES,
            title = "Waves",
            sourceId = GenartSource.ID,
            kind = ArtworkKind.Genart,
        )
        val a = MusicPresetResolver.resolve(art)
        val b = MusicPresetResolver.resolve(art)
        assertEquals(a, b)
        assertEquals(a.artworkSeed, b.artworkSeed)
        assertEquals(a.style, b.style)
        assertEquals(a.rootHz, b.rootHz)
    }

    @Test
    fun differentIdsYieldDifferentSeeds() {
        val a = MusicPresetResolver.resolve(
            Artwork("genart.snow", "Snow", sourceId = GenartSource.ID, kind = ArtworkKind.Genart),
        )
        val b = MusicPresetResolver.resolve(
            Artwork(GenartSource.FIRE, "Fire", sourceId = GenartSource.ID, kind = ArtworkKind.Genart),
        )
        assertNotEquals(a.artworkSeed, b.artworkSeed)
    }

    @Test
    fun waterTopicMapsToOceanOrZen() {
        val art = Artwork(
            id = GenartSource.WAVES,
            title = "Waves",
            sourceId = GenartSource.ID,
            kind = ArtworkKind.Genart,
        )
        val style = MusicPresetResolver.pickStyle(art, MusicPresetResolver.stableHash(art.id))
        assertTrue(style == MusicStyle.OceanWaves || style == MusicStyle.Zen)
    }

    @Test
    fun stableHashIsDeterministic() {
        assertEquals(MusicPresetResolver.stableHash("abc"), MusicPresetResolver.stableHash("abc"))
        assertNotEquals(MusicPresetResolver.stableHash("abc"), MusicPresetResolver.stableHash("abd"))
    }

    @Test
    fun photoHeuristicOcean() {
        val art = Artwork(
            id = "pexels-1",
            title = "Ocean sunset",
            sourceId = "pexels",
            kind = ArtworkKind.Photo,
        )
        val preset = MusicPresetResolver.resolve(art)
        assertEquals(MusicStyle.OceanWaves, preset.style)
    }
}
