package fr.geoking.arthur.audio

import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.source.GenartSource
import fr.geoking.arthur.source.AmbientAudioCharacter
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
    fun waterTopicMapsToMelodicOrOcean() {
        val art = Artwork(
            id = GenartSource.WAVES,
            title = "Waves",
            sourceId = GenartSource.ID,
            kind = ArtworkKind.Genart,
        )
        val style = MusicPresetResolver.pickStyle(art, MusicPresetResolver.stableHash(art.id))
        assertTrue(
            style == MusicStyle.OceanWaves ||
                style == MusicStyle.Zen ||
                style == MusicStyle.SoftGuitar,
        )
    }

    @Test
    fun stableHashIsDeterministic() {
        assertEquals(MusicPresetResolver.stableHash("abc"), MusicPresetResolver.stableHash("abc"))
        assertNotEquals(MusicPresetResolver.stableHash("abc"), MusicPresetResolver.stableHash("abd"))
    }

    @Test
    fun photoHeuristicOceanKeepsOceanUnderAtmosphere() {
        val art = Artwork(
            id = "pexels-1",
            title = "Ocean sunset",
            sourceId = "pexels",
            kind = ArtworkKind.Photo,
        )
        val preset = MusicPresetResolver.resolve(
            art,
            MusicUserPrefs(character = AmbientAudioCharacter.Atmosphere),
        )
        assertEquals(MusicStyle.OceanWaves, preset.style)
    }

    @Test
    fun melodyCharacterRebiasesOcean() {
        val art = Artwork(
            id = "pexels-1",
            title = "Ocean sunset",
            sourceId = "pexels",
            kind = ArtworkKind.Photo,
        )
        val preset = MusicPresetResolver.resolve(
            art,
            MusicUserPrefs(character = AmbientAudioCharacter.Melody),
        )
        assertTrue(preset.style == MusicStyle.Zen || preset.style == MusicStyle.SoftGuitar)
        assertEquals(0f, preset.trackMix.texture, 0.001f)
        assertTrue(preset.trackMix.melody >= 0.55f)
    }

    @Test
    fun stylePreferenceOverridesArtwork() {
        val art = Artwork(
            id = "pexels-1",
            title = "Ocean sunset",
            sourceId = "pexels",
            kind = ArtworkKind.Photo,
        )
        val preset = MusicPresetResolver.resolve(
            art,
            MusicUserPrefs(
                character = AmbientAudioCharacter.Melody,
                stylePreference = MusicStyle.JazzPiano,
            ),
        )
        assertEquals(MusicStyle.JazzPiano, preset.style)
    }

    @Test
    fun melodyCharacterClampsBed() {
        val mix = TrackMix(bed = 0.5f, harmony = 0.3f, melody = 0.3f, texture = 0.2f, pulse = 0.1f)
        val applied = MusicPresetResolver.applyCharacterMix(
            mix,
            AmbientAudioCharacter.Melody,
            MusicStyle.JazzPiano,
        )
        assertTrue(applied.bed <= 0.08f)
        assertEquals(0f, applied.texture, 0.001f)
        assertTrue(applied.melody >= 0.55f)
    }
}
