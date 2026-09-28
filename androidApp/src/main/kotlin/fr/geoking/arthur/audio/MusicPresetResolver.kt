package fr.geoking.arthur.audio

import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.marketplace.GenartPackTopics
import kotlin.math.pow

/** Maps [Artwork] → unique [MusicPreset] via topic/kind heuristics + id hash. */
object MusicPresetResolver {

    fun resolve(artwork: Artwork): MusicPreset {
        val seed = stableHash(artwork.id)
        val style = pickStyle(artwork, seed)
        val scales = ScaleLibrary.forStyle(style)
        val scale = scales[mod(seed, scales.size)]
        val rootMidi = 45 + mod(seed shr 8, 17) // A2..C4-ish
        val rootHz = midiToHz(rootMidi.toFloat())
        val tempo = baseTempo(style) * (0.85f + mod(seed shr 16, 31) / 100f)
        val density = (0.25f + mod(seed shr 4, 50) / 100f).coerceIn(0.15f, 0.85f)
        val ornament = (0.1f + mod(seed shr 12, 40) / 100f).coerceIn(0.05f, 0.7f)
        val mix = defaultMix(style).let { base ->
            base.copy(
                bed = (base.bed + delta(seed, 0)).coerceIn(0.15f, 0.7f),
                harmony = (base.harmony + delta(seed, 1)).coerceIn(0f, 0.5f),
                melody = (base.melody + delta(seed, 2)).coerceIn(0.05f, 0.55f),
                texture = (base.texture + delta(seed, 3)).coerceIn(0f, 0.4f),
                pulse = (base.pulse + delta(seed, 4)).coerceIn(0f, 0.35f),
            ).clamped()
        }
        return MusicPreset(
            artworkId = artwork.id,
            style = style,
            artworkSeed = seed,
            rootHz = rootHz,
            scaleSemitones = scale,
            tempoBpm = tempo,
            density = density,
            ornamentRate = ornament,
            trackMix = mix,
            melodyBankIndex = mod(seed, 8),
            rhythmBankIndex = mod(seed shr 3, 8),
            harmonyBankIndex = mod(seed shr 6, 8),
            textureBankIndex = mod(seed shr 9, 8),
            formSeed = seed xor 0x9E3779B97F4A7C15u.toLong(),
        )
    }

    internal fun pickStyle(artwork: Artwork, seed: Long): MusicStyle {
        val topics = when {
            artwork.kind == ArtworkKind.Genart || artwork.id.startsWith("genart.") ->
                GenartPackTopics.topicsCoveringEngine(artwork.id)
            else -> emptyList()
        }
        // Prefer the most specific ambient-audio topic when an engine sits in several packs.
        val ranked = topics.sortedBy { audioTopicPriority(it) }
        val primaryTopicStyles = ranked.firstOrNull()?.let { topicToStyles(it) }.orEmpty()
        if (primaryTopicStyles.isNotEmpty()) {
            return primaryTopicStyles[mod(seed shr 5, primaryTopicStyles.size)]
        }
        return sourceHeuristics(artwork)
            ?: MusicStyle.entries[mod(seed, MusicStyle.entries.size)]
    }

    /** Lower = more specific for soundtrack mapping. */
    private fun audioTopicPriority(topic: String): Int = when (topic) {
        GenartPackTopics.WATER -> 0
        GenartPackTopics.WEATHER -> 1
        GenartPackTopics.PLANETS, GenartPackTopics.SCIFI -> 2
        GenartPackTopics.LIFE -> 3
        GenartPackTopics.EARTH -> 4
        GenartPackTopics.NATURE -> 5
        GenartPackTopics.ABSTRACT, GenartPackTopics.GEOMETRY, GenartPackTopics.TAPET -> 6
        GenartPackTopics.FRACTAL, GenartPackTopics.CUSTOM -> 7
        else -> 8
    }

    private fun topicToStyles(topic: String): List<MusicStyle> = when (topic) {
        GenartPackTopics.WATER -> listOf(MusicStyle.OceanWaves, MusicStyle.Zen)
        GenartPackTopics.NATURE -> listOf(MusicStyle.Zen, MusicStyle.SoftGuitar, MusicStyle.WindChimes)
        GenartPackTopics.EARTH -> listOf(MusicStyle.SoftGuitar, MusicStyle.Zen, MusicStyle.AfricanPulse)
        GenartPackTopics.LIFE -> listOf(MusicStyle.SoftGuitar, MusicStyle.AfricanPulse)
        GenartPackTopics.WEATHER -> listOf(MusicStyle.WindChimes, MusicStyle.OceanWaves, MusicStyle.Zen)
        GenartPackTopics.ABSTRACT, GenartPackTopics.GEOMETRY, GenartPackTopics.TAPET ->
            listOf(MusicStyle.JazzPiano, MusicStyle.BarAmbience, MusicStyle.WindChimes)
        GenartPackTopics.PLANETS, GenartPackTopics.SCIFI ->
            listOf(MusicStyle.CosmicDrone, MusicStyle.TibetanBowl, MusicStyle.Zen)
        GenartPackTopics.FRACTAL, GenartPackTopics.CUSTOM ->
            listOf(MusicStyle.CosmicDrone, MusicStyle.JazzPiano, MusicStyle.Zen)
        else -> listOf(MusicStyle.Zen)
    }

    private fun sourceHeuristics(artwork: Artwork): MusicStyle? {
        val src = artwork.sourceId.lowercase()
        val title = artwork.title.lowercase()
        val blob = "$src ${artwork.id} $title"
        return when {
            "ocean" in blob || "wave" in blob || "water" in blob || "pexels" in src && "ocean" in title ->
                MusicStyle.OceanWaves
            "city" in blob || "street" in blob || "architecture" in blob ->
                MusicStyle.NightLounge
            "mountain" in blob || "nature" in blob || "forest" in blob ->
                MusicStyle.Zen
            "abstract" in blob -> MusicStyle.JazzPiano
            "sky" in blob || "cloud" in blob -> MusicStyle.WindChimes
            artwork.kind == ArtworkKind.Painting || artwork.kind == ArtworkKind.Sculpture ->
                MusicStyle.BarAmbience
            artwork.kind == ArtworkKind.FractalPreset || artwork.kind == ArtworkKind.CustomFractal ->
                MusicStyle.CosmicDrone
            else -> null
        }
    }

    private fun defaultMix(style: MusicStyle): TrackMix = when (style) {
        MusicStyle.Zen -> TrackMix(bed = 0.50f, harmony = 0.18f, melody = 0.28f, texture = 0.08f, pulse = 0.04f)
        MusicStyle.BarAmbience -> TrackMix(bed = 0.42f, harmony = 0.28f, melody = 0.22f, texture = 0.10f, pulse = 0.14f)
        MusicStyle.JazzPiano -> TrackMix(bed = 0.28f, harmony = 0.35f, melody = 0.40f, texture = 0.05f, pulse = 0.10f)
        MusicStyle.SoftGuitar -> TrackMix(bed = 0.30f, harmony = 0.25f, melody = 0.38f, texture = 0.06f, pulse = 0.08f)
        MusicStyle.TibetanBowl -> TrackMix(bed = 0.55f, harmony = 0.20f, melody = 0.18f, texture = 0.12f, pulse = 0.02f)
        MusicStyle.OceanWaves -> TrackMix(bed = 0.55f, harmony = 0.10f, melody = 0.12f, texture = 0.35f, pulse = 0.02f)
        MusicStyle.AfricanPulse -> TrackMix(bed = 0.28f, harmony = 0.22f, melody = 0.35f, texture = 0.10f, pulse = 0.28f)
        MusicStyle.WindChimes -> TrackMix(bed = 0.25f, harmony = 0.10f, melody = 0.30f, texture = 0.35f, pulse = 0.04f)
        MusicStyle.NightLounge -> TrackMix(bed = 0.40f, harmony = 0.30f, melody = 0.25f, texture = 0.08f, pulse = 0.16f)
        MusicStyle.CosmicDrone -> TrackMix(bed = 0.60f, harmony = 0.22f, melody = 0.12f, texture = 0.18f, pulse = 0.02f)
    }

    private fun baseTempo(style: MusicStyle): Float = when (style) {
        MusicStyle.Zen, MusicStyle.TibetanBowl, MusicStyle.CosmicDrone, MusicStyle.OceanWaves -> 48f
        MusicStyle.WindChimes -> 52f
        MusicStyle.SoftGuitar, MusicStyle.BarAmbience, MusicStyle.NightLounge -> 64f
        MusicStyle.JazzPiano -> 72f
        MusicStyle.AfricanPulse -> 88f
    }

    private fun delta(seed: Long, lane: Int): Float =
        ((mod(seed shr (lane * 3), 21) - 10) / 100f)

    private fun mod(value: Long, modulo: Int): Int {
        if (modulo <= 0) return 0
        val m = (value % modulo).toInt()
        return if (m < 0) m + modulo else m
    }

    /** FNV-1a 64-bit over UTF-8 bytes — stable across processes. */
    fun stableHash(id: String): Long {
        var hash = -0x340d631b7bdddcdbL // FNV offset basis
        for (b in id.encodeToByteArray()) {
            hash = hash xor (b.toLong() and 0xff)
            hash *= 0x100000001b3L
        }
        return hash
    }

    private fun midiToHz(midi: Float): Float =
        (440.0 * 2.0.pow(((midi - 69.0) / 12.0))).toFloat()
}

/** Scale degree libraries keyed by style. */
object ScaleLibrary {
    private val majorPent = intArrayOf(0, 2, 4, 7, 9, 12)
    private val minorPent = intArrayOf(0, 3, 5, 7, 10, 12)
    private val dorian = intArrayOf(0, 2, 3, 5, 7, 9, 10, 12)
    private val mixolydian = intArrayOf(0, 2, 4, 5, 7, 9, 10, 12)
    private val wholeTone = intArrayOf(0, 2, 4, 6, 8, 10, 12)
    private val hexatonic = intArrayOf(0, 2, 3, 7, 8, 10, 12)
    private val bluesLite = intArrayOf(0, 3, 5, 6, 7, 10, 12)
    private val bowlPartials = intArrayOf(0, 5, 7, 12, 17, 19) // approximate inharmonic steps

    fun forStyle(style: MusicStyle): List<IntArray> = when (style) {
        MusicStyle.Zen -> listOf(majorPent, minorPent)
        MusicStyle.BarAmbience, MusicStyle.NightLounge -> listOf(dorian, bluesLite, mixolydian)
        MusicStyle.JazzPiano -> listOf(dorian, mixolydian, bluesLite)
        MusicStyle.SoftGuitar -> listOf(majorPent, mixolydian, dorian)
        MusicStyle.TibetanBowl -> listOf(bowlPartials, minorPent)
        MusicStyle.OceanWaves -> listOf(majorPent, wholeTone)
        MusicStyle.AfricanPulse -> listOf(hexatonic, majorPent, minorPent)
        MusicStyle.WindChimes -> listOf(wholeTone, majorPent, bowlPartials)
        MusicStyle.CosmicDrone -> listOf(wholeTone, minorPent, bowlPartials)
    }
}
