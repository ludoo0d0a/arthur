package fr.geoking.arthur.audio.markov

import fr.geoking.arthur.audio.MusicPreset
import fr.geoking.arthur.audio.MusicStyle
import fr.geoking.arthur.audio.banks.BassBank
import fr.geoking.arthur.audio.banks.BassStep
import fr.geoking.arthur.audio.banks.GrooveCellPattern
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
 *
 * Jazz/lounge (non-Atmosphere) uses a shared [GrooveTransport] swing grid;
 * other styles keep independent duration-token countdowns.
 */
class MarkovSequencer(
    private val preset: MusicPreset,
    sessionSalt: Long,
    private val character: AmbientAudioCharacter = AmbientAudioCharacter.Melody,
) {
    private val random = Random(preset.artworkSeed xor sessionSalt)

    private val atmosphere: Boolean = character == AmbientAudioCharacter.Atmosphere

    private val motifs = MelodyBank.motifsFor(preset.style)
    private val rhythm = RhythmBank.pick(preset.style, preset.rhythmBankIndex)
    private val groovePatterns = RhythmBank.groovePatternsFor(preset.style, atmosphere)
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
    private val groovePatternChain = MarkovChain.connected(
        states = if (groovePatterns.isEmpty()) listOf(0) else groovePatterns.indices.toList(),
        random = random,
        selfBias = 0.35f,
        initial = Math.floorMod(preset.rhythmBankIndex, groovePatterns.size.coerceAtLeast(1)),
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

    // Phrase planner: theme (A) → variation (A') → contrast (B) → cadence on the theme.
    private var activeDegrees: IntArray = activeMotif.degrees
    private var themeDegrees: IntArray = activeMotif.degrees
    private var phraseStep = 1

    /** Key shift in semitones (occasional modulation to the 4th/5th on melodic styles). */
    private var keyOffset = 0
    private val modulationAllowed: Boolean = !atmosphere && preset.style in setOf(
        MusicStyle.JazzPiano, MusicStyle.BarAmbience, MusicStyle.NightLounge,
        MusicStyle.ClassicalPiano, MusicStyle.PianoBallad, MusicStyle.SoftGuitar,
        MusicStyle.RockBallad, MusicStyle.ViolinLead, MusicStyle.HawaiianUkulele,
    )
    private var leadCenter = 5f
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
    private var lastVoicedPartials: List<Float> = emptyList()
    private var lastHarmonyState: HarmonyState? = null

    // Groove-clock state (jazz/lounge only).
    private var transport: GrooveTransport? = null
    private var grooveEighthIndex = 0L
    private var grooveQuarterIndex = 0L
    private var grooveBarIndex = 0L
    private var grooveCellCursor = 0
    private var activeGroovePattern: GrooveCellPattern? =
        groovePatterns.getOrNull(Math.floorMod(preset.rhythmBankIndex, groovePatterns.size.coerceAtLeast(1)))

    private val melodicStyle: Boolean =
        preset.style in setOf(
            MusicStyle.JazzPiano,
            MusicStyle.Zen,
            MusicStyle.SoftGuitar,
            MusicStyle.BarAmbience,
            MusicStyle.NightLounge,
            MusicStyle.AfricanPulse,
            MusicStyle.ClassicalPiano,
            MusicStyle.ViolinLead,
            MusicStyle.RockBallad,
            MusicStyle.PianoBallad,
            MusicStyle.BassOnly,
            MusicStyle.HawaiianUkulele,
            MusicStyle.Chiptune,
            MusicStyle.ChipArp,
            MusicStyle.ArcadeGlow,
        )

    /**
     * Shared swung-eighth transport for groove styles, plus any Atmosphere session
     * (ultra-sparse cells keep beds calm).
     */
    val useGrooveClock: Boolean =
        atmosphere || preset.style in GROOVE_STYLES

    val harmonyState: HarmonyState get() = currentHarmony
    val currentMelodyHz: Float?
        get() = currentDegree?.let { degreeToHz(it) }
    val currentBassFrequencyHz: Float?
        get() = currentBassHz

    /** True when harmony state changed on the last [advanceHarmonyClock] call. */
    var harmonyChanged: Boolean = true
        private set

    /** Samples remaining before next melody decision (0 = due now). */
    fun samplesUntilMelody(): Int = samplesUntilNextNote

    /** Samples remaining before next bass onset (0 = due now). */
    fun samplesUntilBass(): Int = samplesUntilBass

    /** Samples remaining before next harmony change (0 = due now). */
    fun samplesUntilHarmony(): Int = samplesUntilHarmony

    /**
     * Fast-forward idle countdowns by [frames] when no onset is due.
     * Caller must ensure [frames] ≤ all positive countdowns.
     */
    fun skipSamples(frames: Int) {
        if (frames <= 0) return
        if (samplesUntilNextNote > 0) {
            samplesUntilNextNote = (samplesUntilNextNote - frames).coerceAtLeast(0)
        }
        if (samplesUntilBass > 0) {
            samplesUntilBass = (samplesUntilBass - frames).coerceAtLeast(0)
        }
        if (samplesUntilHarmony > 0) {
            samplesUntilHarmony = (samplesUntilHarmony - frames).coerceAtLeast(0)
        }
        harmonyChanged = false
    }

    fun resetPhrase() {
        pitchChain.reset()
        rhythmChain.reset()
        groovePatternChain.reset()
        activeMotif = motifs[pitchChain.current]
        activeDegrees = activeMotif.degrees
        themeDegrees = activeDegrees
        phraseStep = 1
        motifCursor = 0
        samplesUntilNextNote = 0
        currentDegree = 0
        lastDegree = 0
        grooveCellCursor = 0
        activeGroovePattern = groovePatterns.getOrNull(groovePatternChain.current)
        grooveEighthIndex = 0L
        grooveQuarterIndex = 0L
        grooveBarIndex = 0L
    }

    fun advanceHarmonyClock(sampleRate: Int) {
        if (useGrooveClock) {
            advanceGrooveHarmony(sampleRate)
            return
        }
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

    private fun advanceGrooveHarmony(sampleRate: Int) {
        val t = ensureTransport(sampleRate)
        harmonyChanged = false
        if (samplesUntilHarmony > 0) {
            samplesUntilHarmony--
            return
        }
        advanceHarmonyState()
        harmonyChanged = true
        val bars = when {
            atmosphere -> 4 + random.nextInt(5) // 4–8 bars — long drones
            preset.style in TEXTURE_GROOVE_STYLES -> 3 + random.nextInt(3) // 3–5
            preset.style in AMBIENT_GROOVE_STYLES -> 2 + random.nextInt(3) // 2–4
            else -> 1 + random.nextInt(2) // 1–2 jazz
        }
        // Countdown semantics: N decrements then fire → period N+1; store duration-1.
        samplesUntilHarmony = (t.barsDuration(grooveBarIndex, bars) - 1).coerceAtLeast(0)
        grooveBarIndex += bars
    }

    private fun advanceHarmonyState() {
        if (harmonyPattern.sequential && harmonyPattern.progression.isNotEmpty()) {
            // Occasional jump to refresh refrain feel.
            if (random.nextFloat() < 0.12f) {
                progressionIndex = random.nextInt(harmonyPattern.progression.size)
            } else {
                progressionIndex = (progressionIndex + 1) % harmonyPattern.progression.size
                // After a full cycle, occasionally modulate up a 4th/5th, then come home.
                if (progressionIndex == 0 && modulationAllowed && random.nextFloat() < 0.2f) {
                    keyOffset = if (keyOffset == 0) (if (random.nextBoolean()) 5 else 7) else 0
                }
            }
            currentHarmony = harmonyPattern.progression[progressionIndex]
        } else {
            currentHarmony = harmonyChain.next()
        }
    }

    private fun peekNextHarmonyState(): HarmonyState {
        if (harmonyPattern.sequential && harmonyPattern.progression.isNotEmpty()) {
            val nextIdx = (progressionIndex + 1) % harmonyPattern.progression.size
            return harmonyPattern.progression[nextIdx]
        }
        return currentHarmony
    }

    /** Advance bass clock; returns true when a new bass note should fire. */
    fun tickBass(sampleRate: Int): Boolean {
        if (useGrooveClock) return tickGrooveBass(sampleRate)
        if (samplesUntilBass > 0) {
            samplesUntilBass--
            return false
        }
        val step = bassPattern.steps[bassStepCursor % bassPattern.steps.size]
        bassStepCursor++
        val walk = 1 + random.nextInt(2)
        val chordRootSemi = keyedRootSemis()
        val stepSemi = BassBank.stepSemitones(embellishBassStep(step), walk)
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

    /** Occasionally answer a root with the fifth so the bass line isn't a static pulse. */
    private fun embellishBassStep(step: BassStep): BassStep =
        if (!atmosphere && step == BassStep.Root && random.nextFloat() < 0.2f) BassStep.Fifth else step

    private fun tickGrooveBass(sampleRate: Int): Boolean {
        val t = ensureTransport(sampleRate)
        if (samplesUntilBass > 0) {
            samplesUntilBass--
            return false
        }
        val step = bassPattern.steps[bassStepCursor % bassPattern.steps.size]
        bassStepCursor++
        val chordRootSemi = keyedRootSemis()
        val octaveDown = if (atmosphere && step == BassStep.Pedal) -24 else -12
        val oneBar = t.barsDuration(grooveBarIndex, 1)
        val approachingChange = !atmosphere && samplesUntilHarmony in 1 until oneBar
        val semis = when {
            approachingChange && (step == BassStep.WalkUp || step == BassStep.WalkDown) -> {
                val nextRoot = HarmonyBank.stateSemitones(peekNextHarmonyState()) + keyOffset
                val approach = when {
                    nextRoot > chordRootSemi -> nextRoot - 1
                    nextRoot < chordRootSemi -> nextRoot + 1
                    else -> nextRoot - 1
                }
                approach + octaveDown
            }
            else -> {
                val walk = 1 + random.nextInt(2)
                chordRootSemi + BassBank.stepSemitones(embellishBassStep(step), walk) + octaveDown
            }
        }
        currentBassHz = (preset.rootHz * 2.0.pow(semis / 12.0).toFloat())
            .coerceIn(40f, 140f)
        val quarters = when {
            atmosphere -> 2 + random.nextInt(3) // 2–4
            preset.style in TEXTURE_GROOVE_STYLES -> 2
            else -> 1
        }
        var wait = 0
        repeat(quarters) {
            wait += t.quarterDuration(grooveQuarterIndex)
            grooveQuarterIndex++
        }
        samplesUntilBass = (wait - 1).coerceAtLeast(0)
        return true
    }

    /** Advance melody clock by one sample; returns true when a new note onset fires. */
    fun tickMelody(sampleRate: Int): Boolean {
        if (useGrooveClock) return tickGrooveMelody(sampleRate)
        if (samplesUntilNextNote > 0) {
            samplesUntilNextNote--
            return false
        }
        if (motifCursor >= activeDegrees.size) startNextPhrase()
        var degree = activeDegrees[motifCursor++]
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
            // Prefer stepwise motion; large leaps sound shrill and less singable.
            if (delta > 2 && random.nextFloat() < 0.78f) {
                degree = lastDegree + if (degree > lastDegree) 1 else -1
            }
        }

        // Keep motifs in a mid register (avoid high scale degrees that pierce).
        if (!isRestToken && degree != MelodyMotif.REST) {
            degree = degree.coerceIn(0, maxMelodyDegree())
        }

        // Bias toward current-chord tones for melodic cohesion.
        if (!isRestToken && !atmosphere && random.nextFloat() < 0.78f) {
            degree = snapToChordTone(degree)
        } else if (!isRestToken && atmosphere && random.nextFloat() < 0.85f) {
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

    private fun tickGrooveMelody(sampleRate: Int): Boolean {
        val t = ensureTransport(sampleRate)
        if (samplesUntilNextNote > 0) {
            samplesUntilNextNote--
            return false
        }
        var pattern = activeGroovePattern
        if (pattern == null || pattern.cells.isEmpty()) {
            samplesUntilNextNote = (t.eighthDuration(grooveEighthIndex) - 1).coerceAtLeast(0)
            grooveEighthIndex++
            currentDegree = null
            return false
        }
        if (grooveCellCursor >= pattern.cells.size) {
            activeGroovePattern = groovePatterns[groovePatternChain.next()]
            pattern = activeGroovePattern!!
            grooveCellCursor = 0
        }
        val cellOnset = pattern.cells[grooveCellCursor++]
        val fire = cellOnset && pickGrooveDegree()
        samplesUntilNextNote = (t.eighthDuration(grooveEighthIndex) - 1).coerceAtLeast(0)
        grooveEighthIndex++
        return fire
    }

    /** Selects a melody degree for a groove onset; returns false if treated as rest. */
    private fun pickGrooveDegree(): Boolean {
        if (motifCursor >= activeDegrees.size) startNextPhrase()
        var degree = activeDegrees[motifCursor++]
        if (degree == MelodyMotif.REST) {
            currentDegree = null
            return false
        }
        if (melodicStyle && motifCursor > 1) {
            val delta = kotlin.math.abs(degree - lastDegree)
            if (delta > 2 && random.nextFloat() < 0.78f) {
                degree = lastDegree + if (degree > lastDegree) 1 else -1
            }
        }
        degree = degree.coerceIn(0, maxMelodyDegree())
        if (random.nextFloat() < 0.78f) {
            degree = snapToChordTone(degree)
        }
        val activityFloor = when {
            atmosphere -> 0.16f
            preset.style in TEXTURE_GROOVE_STYLES -> 0.28f
            preset.style in AMBIENT_GROOVE_STYLES -> 0.42f
            else -> 0.68f
        }
        val densityBoost = when {
            atmosphere -> preset.density * 0.10f
            preset.style in TEXTURE_GROOVE_STYLES -> preset.density * 0.14f
            preset.style in AMBIENT_GROOVE_STYLES -> preset.density * 0.18f
            else -> preset.density * 0.28f
        }
        if (random.nextFloat() > (activityFloor + densityBoost)) {
            currentDegree = null
            return false
        }
        currentDegree = degree
        lastDegree = degree
        return true
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

    /** Pad voicing of the current chord: inversion closest to the previous chord. */
    fun voicedPartialsHz(): List<Float> = lastVoicedPartials.ifEmpty { lastHarmonyPartials }

    fun harmonyPartialsHz(): List<Float> {
        if (!harmonyChanged && lastHarmonyPartials.isNotEmpty() && lastHarmonyState == currentHarmony) {
            return lastHarmonyPartials
        }
        val quality = harmonyPattern.qualities[
            Math.floorMod(currentHarmony.ordinal + preset.harmonyBankIndex, harmonyPattern.qualities.size),
        ]
        val rootShift = keyedRootSemis()
        val rootPosition = quality.intervals.map { rootShift + it }
        val partials = rootPosition.map { preset.rootHz * 2.0.pow(it / 12.0).toFloat() }
        lastVoicedPartials = voiceLead(rootPosition).map { preset.rootHz * 2.0.pow(it / 12.0).toFloat() }
        lastHarmonyPartials = partials
        lastHarmonyState = currentHarmony
        return partials
    }

    /**
     * Re-voices a chord with the smallest movement from the previous one (inversions),
     * so pads glide a step instead of jumping. Drone/atmosphere keeps root position.
     */
    private fun voiceLead(semis: List<Int>): List<Int> {
        if (atmosphere || semis.size < 2) return semis
        val moved = semis.map { s -> s + 12 * Math.round((leadCenter - s) / 12f) }.sorted()
        leadCenter = moved.average().toFloat()
        return moved
    }

    /** Shared groove transport when active (for arrangement bar alignment). */
    fun grooveTransportOrNull(): GrooveTransport? = transport

    private fun ensureTransport(sampleRate: Int): GrooveTransport {
        val existing = transport
        if (existing != null) return existing
        val created = GrooveTransport(
            tempoBpm = preset.tempoBpm,
            sampleRate = sampleRate,
            swingRatio = RhythmBank.grooveSwingRatio(preset.style, atmosphere),
        )
        transport = created
        return created
    }

    private fun keyedRootSemis(): Int = HarmonyBank.stateSemitones(currentHarmony) + keyOffset

    private fun snapToChordTone(degree: Int): Int {
        val scale = preset.scaleSemitones
        if (scale.isEmpty()) return degree
        val size = scale.size
        val chordRootSemi = HarmonyBank.stateSemitones(currentHarmony)
        // Triad / soft-7th tones of the *current* chord (semitones from key root).
        val chordTonePcs = intArrayOf(0, 3, 4, 7, 10)
            .map { Math.floorMod(chordRootSemi + it, 12) }
            .toSet()
        // Voice leading: nearest chord tone in absolute scale steps; ties go closer to the last note.
        var best = Int.MIN_VALUE
        var bestDist = Int.MAX_VALUE
        var bestLead = Int.MAX_VALUE
        for (cand in (degree - size)..(degree + size)) {
            if (cand < 0 || cand > maxMelodyDegree()) continue
            val pc = Math.floorMod(scale[Math.floorMod(cand, size)], 12)
            if (pc !in chordTonePcs) continue
            val d = kotlin.math.abs(cand - degree)
            val lead = kotlin.math.abs(cand - lastDegree)
            if (d < bestDist || (d == bestDist && lead < bestLead)) {
                best = cand
                bestDist = d
                bestLead = lead
            }
        }
        if (best == Int.MIN_VALUE) {
            // Fallback: tonic triad degrees in scale space.
            best = intArrayOf(0, 2, 4).firstOrNull { it < size } ?: 0
        }
        return best
    }

    /** Highest absolute scale step: two octaves for melodic styles, one for calm/atmosphere. */
    private fun maxMelodyDegree(): Int {
        val size = preset.scaleSemitones.size.coerceAtLeast(1)
        return if (atmosphere || !melodicStyle) (size - 1).coerceAtMost(4) else size * 2 - 1
    }

    /** Picks the next 4-part phrase unit: A, A' (varied), B (contrast, higher), A cadence. */
    private fun startNextPhrase() {
        motifCursor = 0
        val shifted = melodicStyle && !atmosphere
        when (phraseStep % 4) {
            0 -> {
                activeMotif = motifs[pitchChain.next()]
                themeDegrees = activeMotif.degrees
                activeDegrees = themeDegrees
            }
            1 -> activeDegrees = varyPhrase(themeDegrees)
            2 -> {
                activeMotif = motifs[pitchChain.next()]
                val lift = if (shifted) 2 + random.nextInt(2) else 0
                activeDegrees = transposeDegrees(activeMotif.degrees, lift)
            }
            else -> activeDegrees = cadencePhrase(themeDegrees)
        }
        phraseStep++
    }

    private fun transposeDegrees(src: IntArray, by: Int): IntArray =
        IntArray(src.size) { i -> if (src[i] == MelodyMotif.REST) src[i] else src[i] + by }

    private fun varyPhrase(src: IntArray): IntArray = when (random.nextInt(3)) {
        0 -> transposeDegrees(src, 1 + random.nextInt(2))
        1 -> {
            // Inversion around the first sounding degree.
            val pivot = src.firstOrNull { it != MelodyMotif.REST } ?: 0
            IntArray(src.size) { i -> if (src[i] == MelodyMotif.REST) src[i] else 2 * pivot - src[i] }
        }
        else -> {
            // Echo: same contour, last third reversed.
            val out = src.copyOf()
            val from = out.size * 2 / 3
            out.reverse(from, out.size)
            out
        }
    }

    /** Same theme, but the final sounding notes resolve down to the tonic. */
    private fun cadencePhrase(src: IntArray): IntArray {
        val out = src.copyOf()
        val sounding = out.indices.filter { out[it] != MelodyMotif.REST }
        if (sounding.size >= 2) {
            out[sounding[sounding.size - 1]] = 0
            out[sounding[sounding.size - 2]] = if (random.nextBoolean()) 1 else 2
        }
        return out
    }

    private fun degreeToHz(degreeIndex: Int): Float {
        val scale = preset.scaleSemitones
        if (scale.isEmpty()) return preset.rootHz.coerceIn(MELODY_MIN_HZ, MELODY_MAX_HZ)
        val d = degreeIndex.coerceIn(0, maxMelodyDegree())
        val octave = Math.floorDiv(d, scale.size)
        val idx = Math.floorMod(d, scale.size)
        // Melody sits one octave above root for clarity over bass.
        val semis = scale[idx] + 12 * octave + keyedRootSemis() + 12
        var hz = preset.rootHz * 2.0.pow(semis / 12.0).toFloat()
        // Fold piercing highs into a warm mid register.
        while (hz > MELODY_MAX_HZ) hz *= 0.5f
        return hz.coerceIn(MELODY_MIN_HZ, MELODY_MAX_HZ)
    }

    companion object {
        /** Soft floor so melody stays above muddy bass. */
        const val MELODY_MIN_HZ = 180f
        /** Cap bright tops — above ~G5 ambient piano/chimes get harsh in-car. */
        const val MELODY_MAX_HZ = 784f

        val GROOVE_STYLES: Set<MusicStyle> = setOf(
            MusicStyle.JazzPiano,
            MusicStyle.BarAmbience,
            MusicStyle.NightLounge,
            MusicStyle.BassOnly,
            MusicStyle.Zen,
            MusicStyle.ClassicalPiano,
            MusicStyle.PianoBallad,
            MusicStyle.OceanWaves,
            MusicStyle.SoftRain,
            MusicStyle.WindAmbience,
            MusicStyle.CosmicDrone,
            MusicStyle.OrchestraPads,
            MusicStyle.OrchestraSwell,
        )

        private val AMBIENT_GROOVE_STYLES: Set<MusicStyle> = setOf(
            MusicStyle.Zen,
            MusicStyle.ClassicalPiano,
            MusicStyle.PianoBallad,
        )

        private val TEXTURE_GROOVE_STYLES: Set<MusicStyle> = setOf(
            MusicStyle.OceanWaves,
            MusicStyle.SoftRain,
            MusicStyle.WindAmbience,
            MusicStyle.CosmicDrone,
            MusicStyle.OrchestraPads,
            MusicStyle.OrchestraSwell,
        )
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
