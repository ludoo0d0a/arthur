package fr.geoking.arthur.shared.engine

import fr.geoking.arthur.shared.domain.AmbientRotation
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.domain.FreeTierLimits
import fr.geoking.arthur.shared.domain.PreparedRotation
import fr.geoking.arthur.shared.domain.Source
import fr.geoking.arthur.shared.domain.isGenerative
import fr.geoking.arthur.shared.marketplace.PackOwnership
import fr.geoking.arthur.shared.source.BundledPackSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlin.math.ceil

/**
 * Aggregates Sources into an Ambient Rotation, applying free-tier caps and Marketplace pack gates.
 * Premium does not unlock content packs — see [PackOwnership].
 */
class ContentEngine(
    private val sources: List<Source>,
    private val packOwnership: PackOwnership = PackOwnership.NONE,
    private val limits: FreeTierLimits = FreeTierLimits(),
) {
    private fun calculatePerSourceLimit(sourceCount: Int): Int? =
        if (sourceCount > 1) maxOf(3, ceil(limits.maxPhotoArtwork.toDouble() / sourceCount).toInt()) else null

    suspend fun resolve(prepared: PreparedRotation): AmbientRotation = coroutineScope {
        val selectedSources = sources.filter { it.id in prepared.sourceIds }
        val perSourceLimit = calculatePerSourceLimit(selectedSources.size)
        val loaded = selectedSources.map { source ->
            async {
                try {
                    if (perSourceLimit != null) source.load(perSourceLimit) else source.load()
                } catch (e: Throwable) {
                    if (e is CancellationException) throw e
                    emptyList()
                }
            }
        }.awaitAll().flatten()
        val byId = loaded.associateBy { it.id }
        val ordered = prepared.artworkIds.mapNotNull { byId[it] }
            .ifEmpty { loaded }
        AmbientRotation(artworkIds = applyGates(ordered).map { it.id })
    }

    suspend fun catalog(prepared: PreparedRotation): List<Artwork> = coroutineScope {
        val selectedSources = sources.filter { it.id in prepared.sourceIds.ifEmpty { sources.map { s -> s.id } } }
        val perSourceLimit = calculatePerSourceLimit(selectedSources.size)
        val loaded = selectedSources.map { source ->
            async {
                try {
                    if (perSourceLimit != null) source.load(perSourceLimit) else source.load()
                } catch (e: Throwable) {
                    if (e is CancellationException) throw e
                    emptyList()
                }
            }
        }.awaitAll().flatten()
        applyGates(loaded)
    }

    fun catalogFlow(prepared: PreparedRotation): Flow<List<Artwork>> = channelFlow {
        val selectedSources = sources.filter { it.id in prepared.sourceIds.ifEmpty { sources.map { s -> s.id } } }
        if (selectedSources.isEmpty()) {
            send(emptyList())
            return@channelFlow
        }
        val perSourceLimit = calculatePerSourceLimit(selectedSources.size)
        val accumulated = mutableListOf<Artwork>()
        val lock = Any()
        coroutineScope {
            selectedSources.forEach { source ->
                launch {
                    val loaded = try {
                        if (perSourceLimit != null) source.load(perSourceLimit) else source.load()
                    } catch (e: Throwable) {
                        if (e is CancellationException) throw e
                        emptyList()
                    }
                    if (loaded.isNotEmpty()) {
                        val gated = synchronized(lock) {
                            accumulated.addAll(loaded)
                            applyGates(accumulated.toList())
                        }
                        send(gated)
                    }
                }
            }
        }
    }

    private fun applyGates(artworks: List<Artwork>): List<Artwork> {
        // Drop stills with no image — they only fill free-tier slots with placeholders.
        val candidates = artworks.filter { art ->
            art.isGenerative || hasDisplayableStill(art)
        }
        var fractals = 0
        val stills = ArrayList<Artwork>()
        val kept = ArrayList<Artwork>(candidates.size)
        for (art in candidates) {
            when (art.kind) {
                ArtworkKind.PersonalPhoto -> {
                    if (packOwnership.ownsPersonalPhotos()) kept.add(art)
                }
                ArtworkKind.CustomFractal -> {
                    if (packOwnership.ownsCustomFractal()) kept.add(art)
                }
                ArtworkKind.Photo, ArtworkKind.Video, ArtworkKind.Painting, ArtworkKind.Sculpture -> {
                    // Bundled pack (incl. Photo suggestions) always available on free tier.
                    if (art.sourceId == BundledPackSource.ID) {
                        kept.add(art)
                    } else {
                        stills.add(art)
                    }
                }
                ArtworkKind.FractalPreset -> {
                    if (packOwnership.ownsAllFractalPresets()) {
                        kept.add(art)
                    } else {
                        fractals++
                        if (fractals <= limits.maxFractalPresets) kept.add(art)
                    }
                }
                ArtworkKind.Genart -> {
                    if (packOwnership.allowsGenartEngine(art.id)) kept.add(art)
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
