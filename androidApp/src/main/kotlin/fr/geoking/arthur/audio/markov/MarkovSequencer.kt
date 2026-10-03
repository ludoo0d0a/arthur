package fr.geoking.arthur.audio.markov

import fr.geoking.arthur.audio.MusicPreset
import fr.geoking.arthur.audio.MusicStyle
import fr.geoking.arthur.audio.banks.BassBank
import fr.geoking.arthur.audio.banks.BassStep
import fr.geoking.arthur.audio.banks.HarmonyBank
import fr.geoking.arthur.audio.banks.HarmonyState
import fr.geoking.arthur.audio.banks.MelodyBank
import fr.geoking.arthur.audio.banks.MelodyMotif
import fr.geoking.arthur.audio.banks.RhythmBank
import fr.geoking.arthur.audio.banks.TextureBank
import fr.geoking.arthur.audio.banks.TextureCueKind
import fr.geoking.arthur.source.AmbientAudioCharacter
import kotlin.math.pow
import kotlin.random.Random

enum class OrnamentKind {
    None,
    Grace,
    Roll,
    DoubleStrike,
}

/**
 * Couples pitch / rhythm / harmony / bass / ornament walks for one preset.
 * Session salt varies walks; artwork seed selects banks/matrices.
 */
class MarkovSequencer(
    private val preset: MusicPreset,
    sessionSalt: Long,
    private val character: AmbientAudioCharacter = AmbientAudioCharacter.Melody,
) {
    private val random = Random(preset.artworkSeed xor sessionSalt)

    private val motifs = MelodyBank.motifsFor(preset.style)
    private val rhythm = RhythmBank.pick(preset.style, preset.rhythmBankIndex)
    private val harmonyPattern = HarmonyBank.pick(preset.style, preset.harmonyBankIndex)
    private val bassPattern = BassBank.patternFor(preset.style, preset.harmonyBankIndex)

    private val pitchChain = MarkovChain.connected(
        states = motifs.indices.toList(),
        random = random,
        selfBias = 0.45f,
        initial = Math.floorMod(preset.melodyBankIndex, motifs.size),
    )
    private val rhythmChain = MarkovChain.connected(
        states = rhythm.tokens.indices.toList(),
        random = random,
        selfBias = 0.25f,
    )
    private val harmonyChain = MarkovChain(
        states = HarmonyState.entries.toList(),
        transitions = harmonyTransitions(),
        random = random,
        initial = harmonyPattern.progression.firstOrNull() ?: HarmonyState.I,
    )
    private val ornamentChain = MarkovChain.connected(
        states = OrnamentKind.entries.toList(),
        random = random,
        selfBias = 0.55f,
        initial = OrnamentKind.None,
    )

    private var motifCursor = 0
    private var activeMotif: MelodyMotif = motifs[pitchChain.current]
    private var samplesUntilNextNote = 0
    private var samplesUntilHarmony = 0
    private var samplesUntilBass = 0
    private var progressionIndex = 0
    private var currentHarmony = harmonyChain.current
    private var currentDegree: Int? = 0
    private var lastDegree: Int = 0
    private var bassStepCursor = 0
    private var currentBassHz: Float? = null
    private var lastHarmonyPartials: List<Float> = emptyList()
    private var lastHarmonyState: HarmonyState? = null

    private val melodicStyle: Boolean =
        preset.style in setOf(
            MusicStyle.JazzPiano,
            MusicStyle.Zen,
            MusicStyle.SoftGuitar,
            MusicStyle.BarAmbience,
            MusicStyle.NightLounge,
            MusicStyle.AfricanPulse,
        )

    private val atmosphere: Boolean = character == AmbientAudioCharacter.Atmosphere

    val harmonyState: HarmonyState get() = currentHarmony
    val currentMelodyHz: Float?
        get() = currentDegree?.let { degreeToHz(it) }
    val currentBassFrequencyHz: Float?
        get() = currentBassHz

    /** True when harmony state changed on the last [advanceHarmonyClock] call. */
    var harmonyChanged: Boolean = true
        private set

    fun resetPhrase() {
        pitchChain.reset()
        rhythmChain.reset()
        activeMotif = motifs[pitchChain.current]
        motifCursor = 0
        samplesUntilNextNote = 0
        currentDegree = 0
        lastDegree = 0
    }

    fun advanceHarmonyClock(sampleRate: Int) {
        harmonyChanged = false
        if (samplesUntilHarmony > 0) {
            samplesUntilHarmony--
            return
        }
        advanceHarmonyState()
        harmonyChanged = true
        val bars = if (atmosphere) {
            4 + random.nextInt(5) // 4–8 bars per chord
        } else {
            2 + random.nextInt(3)
        }
        val secondsPerBar = 4.0 * 60.0 / preset.tempoBpm
        samplesUntilHarmony = (bars * secondsPerBar * sampleRate).toInt().coerceAtLeast(sampleRate)
    }

    private fun advanceHarmonyState() {
        if (harmonyPattern.sequential && harmonyPattern.progression.isNotEmpty()) {
            // Occasional jump to refresh refrain feel.
            if (random.nextFloat() < 0.12f) {
                progressionIndex = random.nextInt(harmonyPattern.progression.size)
            } else {
                progressionIndex = (progressionIndex + 1) % harmonyPattern.progression.size
            }
            currentHarmony = harmonyPattern.progression[progressionIndex]
        } else {
            currentHarmony = harmonyChain.next()
        }
    }

    /** Advance bass clock; returns true when a new bass note should fire. */
    fun tickBass(sampleRate: Int): Boolean {
        if (samplesUntilBass > 0) {
            samplesUntilBass--
            return false
        }
        val step = bassPattern.steps[bassStepCursor % bassPattern.steps.size]
        bassStepCursor++
        val walk = 1 + random.nextInt(2)
        val chordRootSemi = HarmonyBank.stateSemitones(currentHarmony)
        val stepSemi = BassBank.stepSemitones(step, walk)
        // One octave below root (or pedal at root/2).
        val octaveDown = if (step == BassStep.Pedal && atmosphere) -24 else -12
        val semis = chordRootSemi + stepSemi + octaveDown
        currentBassHz = (preset.rootHz * 2.0.pow(semis / 12.0).toFloat())
            .coerceIn(40f, 140f)

        val quarters = when {
            atmosphere -> 4f
            step == BassStep.Pedal -> 2f
            else -> 1f
        }
        val seconds = quarters * (60.0 / preset.tempoBpm.coerceAtLeast(30f))
        samplesUntilBass = (seconds * sampleRate).toInt().coerceAtLeast(sampleRate / 8)
        return true
    }

    /** Advance melody clock by one sample; returns true when a new note onset fires. */
    fun tickMelody(sampleRate: Int): Boolean {
        if (samplesUntilNextNote > 0) {
            samplesUntilNextNote--
            return false
        }
        if (motifCursor >= activeMotif.degrees.size) {
            activeMotif = motifs[pitchChain.next()]
            motifCursor = 0
        }
        var degree = activeMotif.degrees[motifCursor++]
        val token = rhythm.tokens[rhythmChain.next()]
        val rawQuarters = kotlin.math.abs(token).coerceAtLeast(0.25f)
        val quarters = when {
            atmosphere -> rawQuarters.coerceAtLeast(1.5f)
            melodicStyle -> rawQuarters.coerceAtMost(1.25f)
            else -> rawQuarters
        }
        val isRestToken = token < 0f || degree == MelodyMotif.REST

        if (!isRestToken && melodicStyle && motifCursor > 1) {
            val delta = kotlin.math.abs(degree - lastDegree)
            if (delta > 3 && random.nextFloat() < 0.65f) {
                degree = lastDegree + if (degree > lastDegree) 1 else -1
            }
        }

        // Bias toward chord tones (scale degrees 0, 2, 4) for melodic cohesion.
        if (!isRestToken && !atmosphere && random.nextFloat() < 0.60f) {
            degree = snapToChordTone(degree)
        } else if (!isRestToken && atmosphere && random.nextFloat() < 0.75f) {
            degree = snapToChordTone(degree)
        }

        val activityFloor = when {
            atmosphere -> 0.18f
            melodicStyle -> 0.72f
            else -> 0.35f
        }
        val densityBoost = if (atmosphere) preset.density * 0.12f else preset.density * 0.28f
        val forceRest = random.nextFloat() > (activityFloor + densityBoost)
        val treatAsRest = when {
            degree == MelodyMotif.REST -> true
            token < 0f && melodicStyle && !atmosphere && random.nextFloat() < 0.45f -> false
            token < 0f -> true
            forceRest -> true
            else -> false
        }
        if (treatAsRest) {
            currentDegree = null
        } else {
            currentDegree = degree
            lastDegree = degree
        }
        val seconds = quarters * (60.0 / preset.tempoBpm.coerceAtLeast(30f))
        samplesUntilNextNote = (seconds * sampleRate).toInt().coerceAtLeast(sampleRate / 32)
        return currentDegree != null
    }

    fun nextOrnament(): OrnamentKind {
        if (atmosphere && random.nextFloat() > preset.ornamentRate * 0.35f) {
            return OrnamentKind.None
        }
        val kind = ornamentChain.next()
        return if (random.nextFloat() > preset.ornamentRate) OrnamentKind.None else kind
    }

    fun transitionCue(): TextureCueKind =
        TextureBank.transitionCue(preset.style, random.nextInt(8))

    fun harmonyPartialsHz(): List<Float> {
        if (!harmonyChanged && lastHarmonyPartials.isNotEmpty() && lastHarmonyState == currentHarmony) {
            return lastHarmonyPartials
        }
        val quality = harmonyPattern.qualities[
            Math.floorMod(currentHarmony.ordinal + preset.harmonyBankIndex, harmonyPattern.qualities.size),
        ]
        val rootShift = HarmonyBank.stateSemitones(currentHarmony)
        val partials = quality.intervals.map { interval ->
            preset.rootHz * 2.0.pow((rootShift + interval) / 12.0).toFloat()
        }
        lastHarmonyPartials = partials
        lastHarmonyState = currentHarmony
        return partials
    }

    private fun snapToChordTone(degree: Int): Int {
        val scale = preset.scaleSemitones
        if (scale.isEmpty()) return degree
        val chordDegrees = intArrayOf(0, 2, 4).filter { it < scale.size }
        if (chordDegrees.isEmpty()) return 0
        // Prefer nearest chord tone in scale-degree space.
        var best = chordDegrees[0]
        var bestDist = Int.MAX_VALUE
        val wrapped = Math.floorMod(degree, scale.size)
        for (cd in chordDegrees) {
            val d = kotlin.math.min(
                kotlin.math.abs(wrapped - cd),
                scale.size - kotlin.math.abs(wrapped - cd),
            )
            if (d < bestDist) {
                bestDist = d
                best = cd
            }
        }
        val octave = degree / scale.size
        return best + octave * scale.size
    }

    private fun degreeToHz(degreeIndex: Int): Float {
        val scale = preset.scaleSemitones
        if (scale.isEmpty()) return preset.rootHz
        val idx = Math.floorMod(degreeIndex, scale.size)
        val octave = degreeIndex / scale.size
        // Melody sits one octave above root for clarity over bass.
        val semis = scale[idx] + HarmonyBank.stateSemitones(currentHarmony) + octave * 12 + 12
        return preset.rootHz * 2.0.pow(semis / 12.0).toFloat()
    }

    private fun harmonyTransitions(): Map<HarmonyState, Map<HarmonyState, Float>> {
        val allowed = harmonyPattern.progression.toSet().ifEmpty { HarmonyState.entries.toSet() }
        return HarmonyState.entries.associateWith { from ->
            val targets = if (from in allowed) allowed else HarmonyState.entries.toSet()
            targets.associateWith { to ->
                when {
                    to == from -> 0.4f
                    to in harmonyPattern.progression -> 1f
                    else -> 0.15f
                }
            }
        }
    }
}
