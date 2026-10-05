package fr.geoking.arthur.audio

import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.marketplace.AudioPackCatalog
import fr.geoking.arthur.shared.marketplace.GenartPackTopics
import fr.geoking.arthur.shared.marketplace.PackOwnership
import fr.geoking.arthur.source.AmbientAudioCharacter
import kotlin.math.pow

/** Optional user overrides applied on top of artwork-derived presets. */
data class MusicUserPrefs(
    val character: AmbientAudioCharacter = AmbientAudioCharacter.Melody,
    val stylePreference: MusicStyle? = null,
    val complexity: Float = 0.55f,
)

/** Maps [Artwork] → unique [MusicPreset] via topic/kind heuristics + id hash. */
object MusicPresetResolver {

    private val freeStyles: List<MusicStyle> = AudioPackCatalog.freeStyleSuffixes.mapNotNull {
        MusicStyleIds.fromSuffix(it)
    }

    fun resolve(
        artwork: Artwork,
        prefs: MusicUserPrefs = MusicUserPrefs(),
        ownership: PackOwnership = PackOwnership.NONE,
    ): MusicPreset {
        val seed = stableHash(artwork.id)
        var style = prefs.stylePreference ?: pickStyle(artwork, seed, ownership)
        style = rebiasStyleForCharacter(style, prefs.character, seed)
        style = gateStyle(style, ownership, seed)
        val scales = ScaleLibrary.forStyle(style, prefs.character)
        val scale = scales[mod(seed, scales.size)]
        // Lower root leaves room for melody above bass (F#2..E3 → ~92–165 Hz),
        // so melody (+1 oct, capped) stays warm rather than piercing.
        val rootMidi = 42 + mod(seed shr 8, 12)
        val rootHz = midiToHz(rootMidi.toFloat())
        val tempo = baseTempo(style) * (0.9f + mod(seed shr 16, 21) / 100f)
        val complexity = prefs.complexity.coerceIn(0f, 1f)
        val density = (0.4f + complexity * 0.45f + mod(seed shr 4, 15) / 100f).coerceIn(0.25f, 0.95f)
        val ornament = (0.08f + complexity * 0.35f + mod(seed shr 12, 20) / 100f).coerceIn(0.05f, 0.65f)
        val mix = applyCharacterMix(
            defaultMix(style).let { base ->
                base.copy(
                    bed = (base.bed + delta(seed, 0)).coerceIn(0f, 0.7f),
                    harmony = (base.harmony + delta(seed, 1)).coerceIn(0f, 0.5f),
                    melody = (base.melody + delta(seed, 2)).coerceIn(0.15f, 0.75f),
                    bass = (base.bass + delta(seed, 5)).coerceIn(0.1f, 0.65f),
                    texture = (base.texture + delta(seed, 3)).coerceIn(0f, 0.4f),
                    pulse = (base.pulse + delta(seed, 4)).coerceIn(0f, 0.35f),
                ).clamped()
            },
            prefs.character,
            style,
        )
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

    internal fun pickStyle(
        artwork: Artwork,
        seed: Long,
        ownership: PackOwnership = PackOwnership.NONE,
    ): MusicStyle {
        val topics = when {
            artwork.kind == ArtworkKind.Genart || artwork.id.startsWith("genart.") ->
                GenartPackTopics.topicsCoveringEngine(artwork.id)
            else -> emptyList()
        }
        val ranked = topics.sortedBy { audioTopicPriority(it) }
        val primaryTopicStyles = ranked.firstOrNull()?.let { topicToStyles(it) }.orEmpty()
            .filter { ownership.allowsMusicStyle(MusicStyleIds.toSuffix(it)) }
        if (primaryTopicStyles.isNotEmpty()) {
            return primaryTopicStyles[mod(seed shr 5, primaryTopicStyles.size)]
        }
        val heuristic = sourceHeuristics(artwork)
        if (heuristic != null && ownership.allowsMusicStyle(MusicStyleIds.toSuffix(heuristic))) {
            return heuristic
        }
        val allowed = MusicStyle.entries.filter {
            ownership.allowsMusicStyle(MusicStyleIds.toSuffix(it))
        }.ifEmpty { freeStyles }
        return allowed[mod(seed, allowed.size)]
    }

    /** Fall back to a free style when the chosen style is locked. */
    internal fun gateStyle(
        style: MusicStyle,
        ownership: PackOwnership,
        seed: Long,
    ): MusicStyle {
        if (ownership.allowsMusicStyle(MusicStyleIds.toSuffix(style))) return style
        val fallback = freeStyles.ifEmpty { listOf(MusicStyle.JazzPiano, MusicStyle.Zen) }
        return fallback[mod(seed shr 11, fallback.size)]
    }

    /** Redirect drone-heavy styles toward melodic ones when character is Melody. */
    internal fun rebiasStyleForCharacter(
        style: MusicStyle,
        character: AmbientAudioCharacter,
        seed: Long,
    ): MusicStyle {
        if (character == AmbientAudioCharacter.Atmosphere) return style
        return when (style) {
            MusicStyle.CosmicDrone, MusicStyle.OrchestraPads, MusicStyle.OrchestraSwell ->
                if (character == AmbientAudioCharacter.Melody) {
                    listOf(MusicStyle.JazzPiano, MusicStyle.Zen, MusicStyle.NightLounge)[mod(seed shr 7, 3)]
                } else {
                    MusicStyle.Zen
                }
            MusicStyle.TibetanBowl ->
                if (character == AmbientAudioCharacter.Melody) MusicStyle.Zen else style
            MusicStyle.OceanWaves, MusicStyle.SoftRain, MusicStyle.WindAmbience,
            MusicStyle.Fireplace, MusicStyle.Songbirds,
            ->
                if (character == AmbientAudioCharacter.Melody) {
                    listOf(MusicStyle.Zen, MusicStyle.SoftGuitar)[mod(seed shr 9, 2)]
                } else {
                    style
                }
            else -> style
        }
    }

    internal fun applyCharacterMix(
        mix: TrackMix,
        character: AmbientAudioCharacter,
        style: MusicStyle,
    ): TrackMix = when (character) {
        AmbientAudioCharacter.Melody -> mix.copy(
            bed = (mix.bed * 0.15f).coerceAtMost(0.08f),
            harmony = (mix.harmony * 0.55f).coerceIn(0.08f, 0.18f),
            melody = mix.melody.coerceAtLeast(0.55f),
            bass = mix.bass.coerceIn(0.35f, 0.55f),
            texture = 0f,
            pulse = 0.02f,
            transition = mix.transition.coerceAtMost(0.25f),
        ).clamped()
        AmbientAudioCharacter.Balanced -> mix.copy(
            bed = mix.bed.coerceIn(0.08f, 0.22f),
            harmony = mix.harmony.coerceIn(0.10f, 0.26f),
            melody = mix.melody.coerceAtLeast(0.42f),
            bass = mix.bass.coerceIn(0.28f, 0.48f),
            texture = when (style) {
                MusicStyle.OceanWaves, MusicStyle.WindChimes, MusicStyle.SoftRain,
                MusicStyle.WindAmbience, MusicStyle.Fireplace, MusicStyle.Songbirds,
                -> mix.texture.coerceAtMost(0.10f)
                else -> 0f
            },
            pulse = mix.pulse.coerceAtMost(0.06f),
            transition = mix.transition.coerceAtMost(0.30f),
        ).clamped()
        AmbientAudioCharacter.Atmosphere -> mix.copy(
            bed = mix.bed.coerceIn(0.28f, 0.55f),
            harmony = mix.harmony.coerceIn(0.12f, 0.28f),
            melody = mix.melody.coerceIn(0.12f, 0.28f),
            bass = mix.bass.coerceIn(0.22f, 0.42f),
            texture = when (style) {
                MusicStyle.OceanWaves, MusicStyle.WindChimes, MusicStyle.SoftRain,
                MusicStyle.WindAmbience, MusicStyle.Fireplace, MusicStyle.Songbirds,
                -> mix.texture.coerceIn(0.18f, 0.38f)
                MusicStyle.TibetanBowl, MusicStyle.CosmicDrone,
                MusicStyle.OrchestraPads, MusicStyle.OrchestraSwell,
                -> mix.texture.coerceIn(0.08f, 0.22f)
                else -> mix.texture.coerceIn(0.06f, 0.20f)
            },
            pulse = when (style) {
                MusicStyle.AfricanPulse -> mix.pulse.coerceAtMost(0.12f)
                else -> 0.02f
            },
            transition = mix.transition.coerceAtMost(0.35f),
        ).clamped()
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
        GenartPackTopics.WATER -> listOf(MusicStyle.Zen, MusicStyle.SoftGuitar, MusicStyle.OceanWaves)
        GenartPackTopics.NATURE ->
            listOf(MusicStyle.Zen, MusicStyle.SoftGuitar, MusicStyle.Songbirds)
        GenartPackTopics.EARTH -> listOf(MusicStyle.SoftGuitar, MusicStyle.Zen, MusicStyle.AfricanPulse)
        GenartPackTopics.LIFE ->
            listOf(MusicStyle.SoftGuitar, MusicStyle.Songbirds, MusicStyle.JazzPiano)
        GenartPackTopics.WEATHER ->
            listOf(MusicStyle.WindChimes, MusicStyle.SoftRain, MusicStyle.WindAmbience)
        GenartPackTopics.ABSTRACT, GenartPackTopics.GEOMETRY, GenartPackTopics.TAPET ->
            listOf(MusicStyle.JazzPiano, MusicStyle.BarAmbience, MusicStyle.NightLounge)
        GenartPackTopics.PLANETS, GenartPackTopics.SCIFI ->
            listOf(MusicStyle.CosmicDrone, MusicStyle.OrchestraPads, MusicStyle.Zen)
        GenartPackTopics.FRACTAL, GenartPackTopics.CUSTOM ->
            listOf(MusicStyle.JazzPiano, MusicStyle.Zen, MusicStyle.OrchestraPads)
        else -> listOf(MusicStyle.JazzPiano, MusicStyle.Zen)
    }

    private fun sourceHeuristics(artwork: Artwork): MusicStyle? {
        val src = artwork.sourceId.lowercase()
        val title = artwork.title.lowercase()
        val blob = "$src ${artwork.id} $title"
        return when {
            "ocean" in blob || "wave" in blob || "water" in blob || "pexels" in src && "ocean" in title ->
                MusicStyle.OceanWaves
            "rain" in blob || "storm" in blob -> MusicStyle.SoftRain
            "fire" in blob || "ember" in blob || "hearth" in blob -> MusicStyle.Fireplace
            "bird" in blob -> MusicStyle.Songbirds
            "city" in blob || "street" in blob || "architecture" in blob ->
                MusicStyle.NightLounge
            "mountain" in blob || "nature" in blob || "forest" in blob ->
                MusicStyle.Zen
            "abstract" in blob -> MusicStyle.JazzPiano
            "sky" in blob || "cloud" in blob -> MusicStyle.WindChimes
            artwork.kind == ArtworkKind.Painting || artwork.kind == ArtworkKind.Sculpture ->
                MusicStyle.ClassicalPiano
            artwork.kind == ArtworkKind.FractalPreset || artwork.kind == ArtworkKind.CustomFractal ->
                MusicStyle.OrchestraPads
            else -> null
        }
    }

    private fun defaultMix(style: MusicStyle): TrackMix = when (style) {
        MusicStyle.Zen ->
            TrackMix(bed = 0.12f, harmony = 0.16f, melody = 0.58f, bass = 0.40f, texture = 0f, pulse = 0.02f)
        MusicStyle.BarAmbience ->
            TrackMix(bed = 0.12f, harmony = 0.20f, melody = 0.52f, bass = 0.42f, texture = 0f, pulse = 0.04f)
        MusicStyle.JazzPiano ->
            TrackMix(bed = 0.10f, harmony = 0.18f, melody = 0.60f, bass = 0.45f, texture = 0f, pulse = 0.04f)
        MusicStyle.SoftGuitar ->
            TrackMix(bed = 0.12f, harmony = 0.16f, melody = 0.56f, bass = 0.38f, texture = 0f, pulse = 0.02f)
        MusicStyle.TibetanBowl ->
            TrackMix(bed = 0.42f, harmony = 0.18f, melody = 0.22f, bass = 0.30f, texture = 0.12f, pulse = 0.02f)
        MusicStyle.OceanWaves ->
            TrackMix(bed = 0.32f, harmony = 0.14f, melody = 0.20f, bass = 0.28f, texture = 0.28f, pulse = 0.02f)
        MusicStyle.SoftRain ->
            TrackMix(bed = 0.28f, harmony = 0.12f, melody = 0.16f, bass = 0.24f, texture = 0.34f, pulse = 0.02f)
        MusicStyle.WindAmbience ->
            TrackMix(bed = 0.30f, harmony = 0.12f, melody = 0.14f, bass = 0.22f, texture = 0.32f, pulse = 0.02f)
        MusicStyle.Fireplace ->
            TrackMix(bed = 0.34f, harmony = 0.14f, melody = 0.18f, bass = 0.30f, texture = 0.30f, pulse = 0.02f)
        MusicStyle.Songbirds ->
            TrackMix(bed = 0.18f, harmony = 0.12f, melody = 0.28f, bass = 0.20f, texture = 0.26f, pulse = 0.02f)
        MusicStyle.AfricanPulse ->
            TrackMix(bed = 0.10f, harmony = 0.14f, melody = 0.52f, bass = 0.40f, texture = 0f, pulse = 0.18f)
        MusicStyle.WindChimes ->
            TrackMix(bed = 0.14f, harmony = 0.12f, melody = 0.40f, bass = 0.22f, texture = 0.22f, pulse = 0.02f)
        MusicStyle.NightLounge ->
            TrackMix(bed = 0.12f, harmony = 0.22f, melody = 0.52f, bass = 0.44f, texture = 0f, pulse = 0.06f)
        MusicStyle.CosmicDrone ->
            TrackMix(bed = 0.48f, harmony = 0.20f, melody = 0.18f, bass = 0.35f, texture = 0.14f, pulse = 0.02f)
        MusicStyle.ClassicalPiano ->
            TrackMix(bed = 0.10f, harmony = 0.20f, melody = 0.58f, bass = 0.38f, texture = 0f, pulse = 0.02f)
        MusicStyle.OrchestraPads ->
            TrackMix(bed = 0.44f, harmony = 0.28f, melody = 0.22f, bass = 0.32f, texture = 0.10f, pulse = 0.02f)
        MusicStyle.OrchestraSwell ->
            TrackMix(bed = 0.40f, harmony = 0.30f, melody = 0.26f, bass = 0.30f, texture = 0.12f, pulse = 0.02f)
        MusicStyle.ViolinLead ->
            TrackMix(bed = 0.16f, harmony = 0.18f, melody = 0.56f, bass = 0.28f, texture = 0.06f, pulse = 0.02f)
        MusicStyle.RockBallad ->
            TrackMix(bed = 0.12f, harmony = 0.18f, melody = 0.50f, bass = 0.48f, texture = 0f, pulse = 0.08f)
        MusicStyle.BassOnly ->
            TrackMix(bed = 0.06f, harmony = 0.04f, melody = 0.08f, bass = 0.72f, texture = 0f, pulse = 0.06f)
        MusicStyle.PianoBallad ->
            TrackMix(bed = 0.18f, harmony = 0.22f, melody = 0.54f, bass = 0.36f, texture = 0f, pulse = 0.02f)
        MusicStyle.HawaiianUkulele ->
            TrackMix(bed = 0.10f, harmony = 0.20f, melody = 0.58f, bass = 0.38f, texture = 0f, pulse = 0.04f)
    }

    private fun baseTempo(style: MusicStyle): Float = when (style) {
        MusicStyle.Zen, MusicStyle.TibetanBowl, MusicStyle.CosmicDrone, MusicStyle.OceanWaves,
        MusicStyle.SoftRain, MusicStyle.WindAmbience, MusicStyle.Fireplace,
        MusicStyle.OrchestraPads, MusicStyle.OrchestraSwell,
        -> 56f
        MusicStyle.WindChimes, MusicStyle.Songbirds, MusicStyle.ClassicalPiano,
        MusicStyle.ViolinLead, MusicStyle.PianoBallad,
        -> 58f
        MusicStyle.SoftGuitar, MusicStyle.BarAmbience, MusicStyle.NightLounge,
        MusicStyle.RockBallad, MusicStyle.HawaiianUkulele,
        -> 68f
        MusicStyle.JazzPiano, MusicStyle.BassOnly -> 76f
        MusicStyle.AfricanPulse -> 92f
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

/** Scale degree libraries keyed by style / character. */
object ScaleLibrary {
    private val majorPent = intArrayOf(0, 2, 4, 7, 9, 12)
    private val minorPent = intArrayOf(0, 3, 5, 7, 10, 12)
    private val major = intArrayOf(0, 2, 4, 5, 7, 9, 11, 12)
    private val naturalMinor = intArrayOf(0, 2, 3, 5, 7, 8, 10, 12)
    private val dorian = intArrayOf(0, 2, 3, 5, 7, 9, 10, 12)
    private val mixolydian = intArrayOf(0, 2, 4, 5, 7, 9, 10, 12)
    private val wholeTone = intArrayOf(0, 2, 4, 6, 8, 10, 12)
    private val hexatonic = intArrayOf(0, 2, 3, 7, 8, 10, 12)
    private val bluesLite = intArrayOf(0, 3, 5, 6, 7, 10, 12)
    private val bowlPartials = intArrayOf(0, 5, 7, 12, 17, 19)

    fun forStyle(
        style: MusicStyle,
        character: AmbientAudioCharacter = AmbientAudioCharacter.Melody,
    ): List<IntArray> {
        val melodic = listOf(majorPent, minorPent, major, naturalMinor, dorian, mixolydian)
        if (character != AmbientAudioCharacter.Atmosphere) {
            return when (style) {
                MusicStyle.Zen -> listOf(majorPent, minorPent, major, naturalMinor)
                MusicStyle.BarAmbience, MusicStyle.NightLounge -> listOf(dorian, bluesLite, mixolydian, major)
                MusicStyle.JazzPiano, MusicStyle.BassOnly -> listOf(dorian, mixolydian, bluesLite, major)
                MusicStyle.SoftGuitar, MusicStyle.RockBallad, MusicStyle.HawaiianUkulele ->
                    listOf(majorPent, major, mixolydian, dorian)
                MusicStyle.TibetanBowl -> listOf(minorPent, majorPent)
                MusicStyle.OceanWaves, MusicStyle.SoftRain, MusicStyle.WindAmbience,
                MusicStyle.Fireplace, MusicStyle.Songbirds,
                -> listOf(majorPent, major, naturalMinor)
                MusicStyle.AfricanPulse -> listOf(hexatonic, majorPent, minorPent)
                MusicStyle.WindChimes -> listOf(majorPent, major, mixolydian)
                MusicStyle.CosmicDrone, MusicStyle.OrchestraPads, MusicStyle.OrchestraSwell ->
                    listOf(minorPent, naturalMinor, dorian)
                MusicStyle.ClassicalPiano, MusicStyle.PianoBallad, MusicStyle.ViolinLead ->
                    listOf(major, naturalMinor, majorPent, dorian)
            }
        }
        return when (style) {
            MusicStyle.Zen -> listOf(majorPent, minorPent, naturalMinor)
            MusicStyle.BarAmbience, MusicStyle.NightLounge -> listOf(dorian, bluesLite, mixolydian)
            MusicStyle.JazzPiano, MusicStyle.BassOnly -> listOf(dorian, mixolydian, bluesLite)
            MusicStyle.SoftGuitar, MusicStyle.RockBallad, MusicStyle.HawaiianUkulele -> listOf(majorPent, mixolydian, dorian)
            MusicStyle.TibetanBowl -> listOf(bowlPartials, minorPent)
            MusicStyle.OceanWaves, MusicStyle.SoftRain, MusicStyle.WindAmbience,
            MusicStyle.Fireplace, MusicStyle.Songbirds,
            -> listOf(majorPent, wholeTone, naturalMinor)
            MusicStyle.AfricanPulse -> listOf(hexatonic, majorPent, minorPent)
            MusicStyle.WindChimes -> listOf(wholeTone, majorPent, bowlPartials)
            MusicStyle.CosmicDrone, MusicStyle.OrchestraPads, MusicStyle.OrchestraSwell ->
                listOf(wholeTone, minorPent, bowlPartials)
            MusicStyle.ClassicalPiano, MusicStyle.PianoBallad, MusicStyle.ViolinLead ->
                listOf(major, naturalMinor, majorPent)
        }.ifEmpty { melodic }
    }
}
