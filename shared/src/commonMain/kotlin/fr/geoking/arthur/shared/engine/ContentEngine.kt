package fr.geoking.arthur.shared.engine

import fr.geoking.arthur.shared.domain.AmbientRotation
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.domain.FreeTierLimits
import fr.geoking.arthur.shared.domain.PreparedRotation
import fr.geoking.arthur.shared.domain.PremiumEntitlement
import fr.geoking.arthur.shared.domain.Source
import fr.geoking.arthur.shared.domain.isGenerative
import fr.geoking.arthur.shared.source.BundledPackSource

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
        // Drop stills with no image — they only fill free-tier slots with placeholders.
        val candidates = artworks.filter { art ->
            art.isGenerative || hasDisplayableStill(art)
        }
        if (entitlement.isPremium) {
            return candidates
        }
        var fractals = 0
        var genart = 0
        val stills = ArrayList<Artwork>()
        val kept = ArrayList<Artwork>(candidates.size)
        for (art in candidates) {
            when (art.kind) {
                ArtworkKind.PersonalPhoto, ArtworkKind.CustomFractal -> Unit
                ArtworkKind.Photo, ArtworkKind.Video, ArtworkKind.Painting, ArtworkKind.Sculpture -> {
                    // Bundled pack (incl. Photo suggestions) always available on free tier.
                    if (art.sourceId == BundledPackSource.ID) {
                        kept.add(art)
                    } else {
                        stills.add(art)
                    }
                }
                ArtworkKind.FractalPreset -> {
                    fractals++
                    if (fractals <= limits.maxFractalPresets) kept.add(art)
                }
                ArtworkKind.Genart -> {
                    genart++
                    if (genart <= limits.maxGenart) kept.add(art)
                }
            }
        }
        kept.addAll(fairStillSample(stills, limits.maxPhotoArtwork))
        return kept
    }

    /**
     * Round-robin stills by [Artwork.sourceId] so Painting/Museum **All** does not
     * starve later museums (prefix-cap used to keep only Rijks/Met).
     */
    private fun fairStillSample(stills: List<Artwork>, limit: Int): List<Artwork> {
        if (limit <= 0 || stills.isEmpty()) return emptyList()
        if (stills.size <= limit) return stills
        val bySource = LinkedHashMap<String, ArrayDeque<Artwork>>()
        for (art in stills) {
            bySource.getOrPut(art.sourceId) { ArrayDeque() }.add(art)
        }
        val out = ArrayList<Artwork>(limit)
        while (out.size < limit) {
            var progressed = false
            for (queue in bySource.values) {
                if (queue.isNotEmpty()) {
                    out.add(queue.removeFirst())
                    progressed = true
                    if (out.size >= limit) break
                }
            }
            if (!progressed) break
        }
        return out
    }

    private fun hasDisplayableStill(artwork: Artwork): Boolean =
        !artwork.remoteUrl.isNullOrBlank() || !artwork.localPath.isNullOrBlank()
}
