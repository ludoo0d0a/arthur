package fr.geoking.arthur.shared.engine

import fr.geoking.arthur.shared.domain.AmbientRotation
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.domain.FreeTierLimits
import fr.geoking.arthur.shared.domain.PreparedRotation
import fr.geoking.arthur.shared.domain.PremiumEntitlement
import fr.geoking.arthur.shared.domain.Source

/**
 * Aggregates Sources into an Ambient Rotation, applying free-tier caps and Premium gates.
 */
class ContentEngine(
    private val sources: List<Source>,
    private val entitlement: PremiumEntitlement,
    private val limits: FreeTierLimits = FreeTierLimits(),
) {
    suspend fun resolve(prepared: PreparedRotation): AmbientRotation {
        val selectedSources = sources.filter { it.id in prepared.sourceIds }
        val loaded = selectedSources.flatMap { it.load() }
        val byId = loaded.associateBy { it.id }
        val ordered = prepared.artworkIds.mapNotNull { byId[it] }
            .ifEmpty { loaded }
        return AmbientRotation(artworkIds = applyGates(ordered).map { it.id })
    }

    suspend fun catalog(prepared: PreparedRotation): List<Artwork> {
        val selectedSources = sources.filter { it.id in prepared.sourceIds.ifEmpty { sources.map { s -> s.id } } }
        val loaded = selectedSources.flatMap { it.load() }
        return applyGates(loaded)
    }

    private fun applyGates(artworks: List<Artwork>): List<Artwork> {
        if (entitlement.isPremium) {
            return artworks
        }
        var photos = 0
        var fractals = 0
        var genart = 0
        return artworks.filter { art ->
            when (art.kind) {
                ArtworkKind.PersonalPhoto, ArtworkKind.CustomFractal -> false
                ArtworkKind.Photo, ArtworkKind.Painting, ArtworkKind.Sculpture -> {
                    photos++
                    photos <= limits.maxPhotoArtwork
                }
                ArtworkKind.FractalPreset -> {
                    fractals++
                    fractals <= limits.maxFractalPresets
                }
                ArtworkKind.Genart -> {
                    genart++
                    genart <= limits.maxGenart
                }
            }
        }
    }
}
