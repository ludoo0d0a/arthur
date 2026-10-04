package fr.geoking.arthur.audio

import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.marketplace.FakePackOwnership
import fr.geoking.arthur.shared.marketplace.MarketplaceCatalog
import fr.geoking.arthur.shared.marketplace.PackOwnership
import fr.geoking.arthur.shared.source.GenartSource
import fr.geoking.arthur.source.AmbientAudioCharacter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicPresetResolverTest {

    private fun ownedAll(): PackOwnership = FakePackOwnership().also { it.unlockAll() }

    @Test
    fun sameIdYieldsSamePreset() {
        val art = Artwork(
            id = GenartSource.WAVES,
            title = "Waves",
            sourceId = GenartSource.ID,
            kind = ArtworkKind.Genart,
        )
        val a = MusicPresetResolver.resolve(art, ownership = ownedAll())
        val b = MusicPresetResolver.resolve(art, ownership = ownedAll())
        assertEquals(a, b)
        assertEquals(a.artworkSeed, b.artworkSeed)
        assertEquals(a.style, b.style)
        assertEquals(a.rootHz, b.rootHz)
    }

    @Test
    fun differentIdsYieldDifferentSeeds() {
        val a = MusicPresetResolver.resolve(
            Artwork("genart.snow", "Snow", sourceId = GenartSource.ID, kind = ArtworkKind.Genart),
            ownership = ownedAll(),
        )
        val b = MusicPresetResolver.resolve(
            Artwork(GenartSource.FIRE, "Fire", sourceId = GenartSource.ID, kind = ArtworkKind.Genart),
            ownership = ownedAll(),
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
        val style = MusicPresetResolver.pickStyle(
            art,
            MusicPresetResolver.stableHash(art.id),
            ownedAll(),
        )
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
            ownedAll(),
        )
        assertEquals(MusicStyle.OceanWaves, preset.style)
    }

    @Test
    fun lockedOceanFallsBackToFreeStyle() {
        val art = Artwork(
            id = "pexels-1",
            title = "Ocean sunset",
            sourceId = "pexels",
            kind = ArtworkKind.Photo,
        )
        val preset = MusicPresetResolver.resolve(
            art,
            MusicUserPrefs(character = AmbientAudioCharacter.Atmosphere),
            PackOwnership.NONE,
        )
        assertTrue(
            preset.style == MusicStyle.JazzPiano ||
                preset.style == MusicStyle.Zen ||
                preset.style == MusicStyle.SoftGuitar,
        )
        assertFalse(preset.style == MusicStyle.OceanWaves)
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
            ownedAll(),
        )
        assertTrue(preset.style == MusicStyle.Zen || preset.style == MusicStyle.SoftGuitar)
        assertEquals(0f, preset.trackMix.texture, 0.001f)
        assertTrue(preset.trackMix.melody >= 0.55f)
        assertTrue(preset.trackMix.bass >= 0.35f)
        assertTrue(preset.trackMix.pulse <= 0.05f)
    }

    @Test
    fun atmosphereMixKeepsBedTextureBass() {
        val art = Artwork(
            id = "pexels-1",
            title = "Ocean sunset",
            sourceId = "pexels",
            kind = ArtworkKind.Photo,
        )
        val preset = MusicPresetResolver.resolve(
            art,
            MusicUserPrefs(character = AmbientAudioCharacter.Atmosphere),
            ownedAll(),
        )
        assertTrue(preset.trackMix.bed >= 0.28f)
        assertTrue(preset.trackMix.texture >= 0.18f)
        assertTrue(preset.trackMix.bass >= 0.22f)
        assertTrue(preset.trackMix.melody <= 0.28f)
        assertTrue(preset.trackMix.pulse <= 0.05f)
    }

    @Test
    fun owningHearthWeatherAllowsOcean() {
        val ownership = FakePackOwnership(
            owned = setOf(MarketplaceCatalog.audioPackId("hearth_weather")),
        )
        assertTrue(ownership.allowsMusicStyle("ocean_waves"))
        assertFalse(ownership.allowsMusicStyle("violin_lead"))
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
        val mix = TrackMix(
            bed = 0.5f,
            harmony = 0.3f,
            melody = 0.3f,
            bass = 0.4f,
            texture = 0.2f,
            pulse = 0.1f,
        )
        val applied = MusicPresetResolver.applyCharacterMix(
            mix,
            AmbientAudioCharacter.Melody,
            MusicStyle.JazzPiano,
        )
        assertTrue(applied.bed <= 0.08f)
        assertEquals(0f, applied.texture, 0.001f)
        assertTrue(applied.melody >= 0.55f)
        assertTrue(applied.bass >= 0.35f)
    }
}
