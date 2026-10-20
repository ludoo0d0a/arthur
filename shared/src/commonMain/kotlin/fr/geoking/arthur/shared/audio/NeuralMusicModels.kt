package fr.geoking.arthur.shared.audio

/** Supported local AI music generation engines. */
enum class NeuralEngineType {
    SOUNDGEN,
    MUSICGEN_SMALL,
    STABLE_AUDIO_3_SMALL,
    EXECUTORCH_MIDI,
}

/**
 * Public FluidR3 GM SoundFont (no auth). Used until dedicated GeoKing soundfonts are published.
 * Source: https://github.com/urish/cinto (FluidR3 GM).
 */
const val PUBLIC_FLUIDR3_GM_SF2_URL =
    "https://raw.githubusercontent.com/urish/cinto/master/media/FluidR3%20GM.sf2"
const val PUBLIC_FLUIDR3_GM_SF2_BYTES = 148_358_590L

/** SoundFont and ExecuTorch model configuration presets for MIDI synthesis. */
data class SoundFontPreset(
    val id: String,
    val displayName: String,
    val soundFontFileName: String,
    val downloadUrl: String,
    val expectedSizeBytes: Long,
    val modelFileName: String = "executorch_midi_gen.pte",
    /** Optional on-device .pte; empty = procedural MIDI tokens (no remote ExecuTorch weights). */
    val modelDownloadUrl: String = "",
)

object ExecuTorchMidiPresets {
    val AMBIENT_PIANO = SoundFontPreset(
        id = "ambient_piano",
        displayName = "Ambient Piano (FluidR3 GM)",
        soundFontFileName = "ambient_piano.sf2",
        downloadUrl = PUBLIC_FLUIDR3_GM_SF2_URL,
        expectedSizeBytes = PUBLIC_FLUIDR3_GM_SF2_BYTES,
    )
    val CHAMBER_STRINGS = SoundFontPreset(
        id = "chamber_strings",
        displayName = "Chamber Strings (FluidR3 GM)",
        soundFontFileName = "chamber_strings.sf2",
        downloadUrl = PUBLIC_FLUIDR3_GM_SF2_URL,
        expectedSizeBytes = PUBLIC_FLUIDR3_GM_SF2_BYTES,
    )
    val LOFI_SYNTH = SoundFontPreset(
        id = "lofi_synth",
        displayName = "Lo-Fi / Synth Ambient (FluidR3 GM)",
        soundFontFileName = "lofi_synth.sf2",
        downloadUrl = PUBLIC_FLUIDR3_GM_SF2_URL,
        expectedSizeBytes = PUBLIC_FLUIDR3_GM_SF2_BYTES,
    )
    val NATURE_ATMOSPHERES = SoundFontPreset(
        id = "nature_atmospheres",
        displayName = "Nature Atmospheres (FluidR3 GM)",
        soundFontFileName = "nature_atmospheres.sf2",
        downloadUrl = PUBLIC_FLUIDR3_GM_SF2_URL,
        expectedSizeBytes = PUBLIC_FLUIDR3_GM_SF2_BYTES,
    )

    val allPresets = listOf(AMBIENT_PIANO, CHAMBER_STRINGS, LOFI_SYNTH, NATURE_ATMOSPHERES)

    /** Default SoundFont used by the ExecuTorch MIDI engine download path. */
    val DEFAULT_SOUNDFONT = AMBIENT_PIANO
}

/** Specific pack mapping to local neural music engines. */
enum class NeuralAudioPack(
    val id: String,
    val displayName: String,
    val engineType: NeuralEngineType,
) {
    ATMOSPHERES_NATURE("pack_atmospheres_nature", "SoundGen Small", NeuralEngineType.SOUNDGEN),
    MUSEUMS_CLASSICAL("pack_museums_classical", "MusicGen Small", NeuralEngineType.MUSICGEN_SMALL),
    GENART_DIGITAL("pack_genart_digital", "Stable Audio 3", NeuralEngineType.STABLE_AUDIO_3_SMALL),
    EXECUTORCH_MIDI("pack_executorch_midi", "ExecuTorch MIDI", NeuralEngineType.EXECUTORCH_MIDI),
}

/** Prompt definition for local neural music generation (2 min max, no lyrics). */
data class NeuralPromptDef(
    val id: String,
    val pack: NeuralAudioPack,
    val title: String,
    val promptText: String,
    val targetDurationSeconds: Int = 120,
)

/** Catalog of 10 predefined prompts per neural music engine/pack (30 prompts total). */
object NeuralPromptCatalog {
    val soundGenPrompts: List<NeuralPromptDef> = listOf(
        NeuralPromptDef(
            "sg_01", NeuralAudioPack.ATMOSPHERES_NATURE,
            "Pluie Douce & Caverne",
            "Soft rain falling on forest leaves with gentle cavern reverb, organic ambient, no lyrics, instrumental",
        ),
        NeuralPromptDef(
            "sg_02", NeuralAudioPack.ATMOSPHERES_NATURE,
            "Vagues Célestes",
            "Ocean waves washing over pebble beach with atmospheric low drone, deep peaceful ambient, no vocals",
        ),
        NeuralPromptDef(
            "sg_03", NeuralAudioPack.ATMOSPHERES_NATURE,
            "Chimes du Vent",
            "Wind chimes swaying gently in a mountain breeze, meditative, shimmering harmonic textures, instrumental",
        ),
        NeuralPromptDef(
            "sg_04", NeuralAudioPack.ATMOSPHERES_NATURE,
            "Feu de Bois Cosmique",
            "Warm fireplace crackling sound layered with soft subtle cosmic drone, relaxing ambience, no lyrics",
        ),
        NeuralPromptDef(
            "sg_05", NeuralAudioPack.ATMOSPHERES_NATURE,
            "Oiseaux de l'Aube",
            "Gentle songbirds at dawn with misty forest wind, calm environmental soundscape, instrumental",
        ),
        NeuralPromptDef(
            "sg_06", NeuralAudioPack.ATMOSPHERES_NATURE,
            "Bol Tibétain & Cristal",
            "Resonant Tibetan singing bowl bowls with subtle crystal chime, deep meditation, peaceful sound, no vocals",
        ),
        NeuralPromptDef(
            "sg_07", NeuralAudioPack.ATMOSPHERES_NATURE,
            "Ruisseau de Montagne",
            "Flowing mountain stream water with distant soft wind through pines, zen ambient texture, instrumental",
        ),
        NeuralPromptDef(
            "sg_08", NeuralAudioPack.ATMOSPHERES_NATURE,
            "Brumes Nocturnes",
            "Night wind rustling bamboo trees, deep serene nocturnal soundscape, calm ambient, no lyrics",
        ),
        NeuralPromptDef(
            "sg_09", NeuralAudioPack.ATMOSPHERES_NATURE,
            "Baleines Éthérées",
            "Deep ocean hydrophone ambience with underwater echo swells, mysterious, pure instrumental, no vocals",
        ),
        NeuralPromptDef(
            "sg_10", NeuralAudioPack.ATMOSPHERES_NATURE,
            "Aurore Boréale",
            "Shimmering atmospheric ice crystals texture, ethereal winter wind soundscape, gentle ambient, no vocals",
        ),
    )

    val musicGenPrompts: List<NeuralPromptDef> = listOf(
        NeuralPromptDef(
            "mg_01", NeuralAudioPack.MUSEUMS_CLASSICAL,
            "Piano Classique Impressionniste",
            "Soft impressionist piano solo in Debussy style, gentle dynamics, museum gallery ambience, no lyrics, instrumental",
        ),
        NeuralPromptDef(
            "mg_02", NeuralAudioPack.MUSEUMS_CLASSICAL,
            "Quatuor à Cordes Méditatif",
            "Slow baroque string quartet, warm cellos and violins in lush acoustics, classical museum atmosphere, instrumental",
        ),
        NeuralPromptDef(
            "mg_03", NeuralAudioPack.MUSEUMS_CLASSICAL,
            "Harpe Éthérée Louvre",
            "Gentle acoustic harp arpeggios with soft room ambience, serene art gallery sound, no vocals, instrumental",
        ),
        NeuralPromptDef(
            "mg_04", NeuralAudioPack.MUSEUMS_CLASSICAL,
            "Guitare Classique Espagnole",
            "Mellow nylon string acoustic guitar solo, peaceful adagio tempo, expressive and warm, no vocals",
        ),
        NeuralPromptDef(
            "mg_05", NeuralAudioPack.MUSEUMS_CLASSICAL,
            "Flûte & Clavecin Doux",
            "Delicate renaissance wooden flute and harpsichord duet, slow movement, historical museum music, instrumental",
        ),
        NeuralPromptDef(
            "mg_06", NeuralAudioPack.MUSEUMS_CLASSICAL,
            "Adagio pour Violoncelle",
            "Solitary cello melody with rich warm reverb, emotional classical piece, museum artwork soundtrack, no vocals",
        ),
        NeuralPromptDef(
            "mg_07", NeuralAudioPack.MUSEUMS_CLASSICAL,
            "Nocturne au Piano",
            "Relaxing romantic piano nocturne, Chopin inspired melody, warm acoustic recording, purely instrumental",
        ),
        NeuralPromptDef(
            "mg_08", NeuralAudioPack.MUSEUMS_CLASSICAL,
            "Ensemble de Luth & Viole",
            "Soft renaissance lute and viola de gamba, tranquil courtly melody, historical art setting, no lyrics",
        ),
        NeuralPromptDef(
            "mg_09", NeuralAudioPack.MUSEUMS_CLASSICAL,
            "Chœur de Cordes Neoclassique",
            "Sustained string orchestra chord swells, modern classical ambient soundscape, deep and reverent, instrumental",
        ),
        NeuralPromptDef(
            "mg_10", NeuralAudioPack.MUSEUMS_CLASSICAL,
            "Solo Clavecin Rijksmuseum",
            "Gentle classical harpsichord fantasia, golden age dutch master paintings ambience, no lyrics, instrumental",
        ),
    )

    val stableAudioPrompts: List<NeuralPromptDef> = listOf(
        NeuralPromptDef(
            "sa_01", NeuralAudioPack.GENART_DIGITAL,
            "Synthé Modulaire Rétro",
            "Warm analog modular synth pad swells with gentle arpeggio filter sweeps, 44kHz stereo genart music, no vocals",
        ),
        NeuralPromptDef(
            "sa_02", NeuralAudioPack.GENART_DIGITAL,
            "Vortex Algorithmique",
            "Deep futuristic ambient synth drone, evolving harmonic overtones, procedural generative art sound, instrumental",
        ),
        NeuralPromptDef(
            "sa_03", NeuralAudioPack.GENART_DIGITAL,
            "Ondes Cybernétiques",
            "Chilled synthwave pads with soft tape warmth, relaxing digital art gallery music, pure instrumental, no lyrics",
        ),
        NeuralPromptDef(
            "sa_04", NeuralAudioPack.GENART_DIGITAL,
            "Clifford Wash Ambient",
            "Ethereal shimmering electronic textures, spatial stereo reverb, generative geometry soundtrack, no vocals",
        ),
        NeuralPromptDef(
            "sa_05", NeuralAudioPack.GENART_DIGITAL,
            "Chiptune Rétro Chill",
            "Soft 8-bit game console sine wave arpeggios, nostalgic lo-fi chiptune ambient, gentle tempo, instrumental",
        ),
        NeuralPromptDef(
            "sa_06", NeuralAudioPack.GENART_DIGITAL,
            "Fractal Flow",
            "Pulsing ambient sub bass with shimmering high frequency bell tones, complex generative soundscape, no vocals",
        ),
        NeuralPromptDef(
            "sa_07", NeuralAudioPack.GENART_DIGITAL,
            "Nébuleuse Synthé",
            "Lush polyphonic synthesizer pads, slow chords drifting in space, high quality 2-minute stereo track, instrumental",
        ),
        NeuralPromptDef(
            "sa_08", NeuralAudioPack.GENART_DIGITAL,
            "Matrix Rain Ambient",
            "Gentle electronic rain impulses with deep warm ambient synthesizer bed, digital glitch art music, no lyrics",
        ),
        NeuralPromptDef(
            "sa_09", NeuralAudioPack.GENART_DIGITAL,
            "Aura Plasma",
            "Cosmic ambient soundscape with organic analog filter movement, relaxing spatial audio, pure instrumental",
        ),
        NeuralPromptDef(
            "sa_10", NeuralAudioPack.GENART_DIGITAL,
            "Hypercube Dream",
            "Glassy FM synthesizer chords with lush delay, modern digital art museum ambient, peaceful, no vocals",
        ),
    )

    val executorchPrompts: List<NeuralPromptDef> = listOf(
        NeuralPromptDef(
            "em_01", NeuralAudioPack.EXECUTORCH_MIDI,
            "Symphonie MIDI Ambient Piano",
            "ExecuTorch generated MIDI sequence rendered with Ambient Piano SoundFont, gentle adagio tempo, peaceful harmonics",
        ),
        NeuralPromptDef(
            "em_02", NeuralAudioPack.EXECUTORCH_MIDI,
            "Quatuor MIDI Cordes Célestes",
            "ExecuTorch neural MIDI composition synthesized through Chamber Strings SoundFont, warm legato strings",
        ),
        NeuralPromptDef(
            "em_03", NeuralAudioPack.EXECUTORCH_MIDI,
            "Lo-Fi Neural Wave",
            "Generative MIDI arpeggios rendered with Lo-Fi Synth SoundFont, relaxing ambient chillout",
        ),
        NeuralPromptDef(
            "em_04", NeuralAudioPack.EXECUTORCH_MIDI,
            "Brise de Nature Synthétisée",
            "ExecuTorch algorithmic MIDI texture rendered with Nature Atmospheres SoundFont, organic serene soundscape",
        ),
    )

    val allPrompts: List<NeuralPromptDef> = soundGenPrompts + musicGenPrompts + stableAudioPrompts + executorchPrompts

    fun promptsForPack(pack: NeuralAudioPack): List<NeuralPromptDef> = when (pack) {
        NeuralAudioPack.ATMOSPHERES_NATURE -> soundGenPrompts
        NeuralAudioPack.MUSEUMS_CLASSICAL -> musicGenPrompts
        NeuralAudioPack.GENART_DIGITAL -> stableAudioPrompts
        NeuralAudioPack.EXECUTORCH_MIDI -> executorchPrompts
    }

    fun promptById(id: String): NeuralPromptDef? = allPrompts.firstOrNull { it.id == id }
}
