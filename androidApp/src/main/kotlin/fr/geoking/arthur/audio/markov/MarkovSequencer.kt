package fr.geoking.arthur.audio.markov

import fr.geoking.arthur.audio.MusicPreset
import fr.geoking.arthur.audio.banks.HarmonyBank
import fr.geoking.arthur.audio.banks.HarmonyState
import fr.geoking.arthur.audio.banks.MelodyBank
import fr.geoking.arthur.audio.banks.MelodyMotif
import fr.geoking.arthur.audio.banks.RhythmBank
import fr.geoking.arthur.audio.banks.TextureBank
import fr.geoking.arthur.audio.banks.TextureCueKind
import kotlin.math.pow
import kotlin.random.Random

enum class OrnamentKind {
    None,
    Grace,
    Roll,
    DoubleStrike,
}

/**
 * Couples pitch / rhythm / harmony / ornament Markov walks for one preset.
 * Session salt varies walks; artwork seed selects banks/matrices.
 */
class MarkovSequencer(
    private val preset: MusicPreset,
    sessionSalt: Long,
) {
    private val random = Random(preset.artworkSeed xor sessionSalt)

    private val motifs = MelodyBank.motifsFor(preset.style)
    private val rhythm = RhythmBank.pick(preset.style, preset.rhythmBankIndex)
    private val harmonyPattern = HarmonyBank.pick(preset.style, preset.harmonyBankIndex)

    private val pitchChain = MarkovChain.connected(
        states = motifs.indices.toList(),
        random = random,
        selfBias = 0.25f,
        initial = Math.floorMod(preset.melodyBankIndex, motifs.size),
    )
    private val rhythmChain = MarkovChain.connected(
        states = rhythm.tokens.indices.toList(),
        random = random,
        selfBias = 0.2f,
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
    private var currentHarmony = harmonyChain.current
    private var currentDegree: Int? = 0

    val harmonyState: HarmonyState get() = currentHarmony
    val currentMelodyHz: Float?
        get() = currentDegree?.let { degreeToHz(it) }

    fun resetPhrase() {
        pitchChain.reset()
        rhythmChain.reset()
        activeMotif = motifs[pitchChain.current]
        motifCursor = 0
        samplesUntilNextNote = 0
        currentDegree = 0
    }

    fun advanceHarmonyClock(sampleRate: Int) {
        if (samplesUntilHarmony > 0) {
            samplesUntilHarmony--
            return
        }
        currentHarmony = harmonyChain.next()
        val bars = 2 + random.nextInt(3)
        val secondsPerBar = 4.0 * 60.0 / preset.tempoBpm
        samplesUntilHarmony = (bars * secondsPerBar * sampleRate).toInt().coerceAtLeast(sampleRate)
    }

    /** Advance melody clock by one sample; returns true when a new note onset fires. */
    fun tickMelody(sampleRate: Int): Boolean {
        if (samplesUntilNextNote > 0) {
            samplesUntilNextNote--
            return false
        }
        // Consume next degree in motif or pick a new motif.
        if (motifCursor >= activeMotif.degrees.size) {
            activeMotif = motifs[pitchChain.next()]
            motifCursor = 0
        }
        val degree = activeMotif.degrees[motifCursor++]
        val token = rhythm.tokens[rhythmChain.next()]
        val quarters = kotlin.math.abs(token).coerceAtLeast(0.25f)
        val isRestToken = token < 0f || degree == MelodyMotif.REST
        // Density thins activity by sometimes forcing rests (never all notes).
        val forceRest = random.nextFloat() > (0.35f + preset.density * 0.65f)
        if (isRestToken || forceRest) {
            currentDegree = null
        } else {
            currentDegree = degree
        }
        val seconds = quarters * (60.0 / preset.tempoBpm.coerceAtLeast(30f))
        samplesUntilNextNote = (seconds * sampleRate).toInt().coerceAtLeast(sampleRate / 32)
        return currentDegree != null
    }

    fun nextOrnament(): OrnamentKind {
        val kind = ornamentChain.next()
        return if (random.nextFloat() > preset.ornamentRate) OrnamentKind.None else kind
    }

    fun transitionCue(): TextureCueKind =
        TextureBank.transitionCue(preset.style, random.nextInt(8))

    fun harmonyPartialsHz(): List<Float> {
        val quality = harmonyPattern.qualities[
            Math.floorMod(currentHarmony.ordinal + preset.harmonyBankIndex, harmonyPattern.qualities.size),
        ]
        val rootShift = HarmonyBank.stateSemitones(currentHarmony)
        return quality.intervals.map { interval ->
            preset.rootHz * 2.0.pow((rootShift + interval) / 12.0).toFloat()
        }
    }

    private fun degreeToHz(degreeIndex: Int): Float {
        val scale = preset.scaleSemitones
        if (scale.isEmpty()) return preset.rootHz
        val idx = Math.floorMod(degreeIndex, scale.size)
        val semis = scale[idx] + HarmonyBank.stateSemitones(currentHarmony)
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
