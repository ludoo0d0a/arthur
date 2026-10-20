# Audit Moteur Audio : Génération Procédurale et Algorithmique

Cet audit documente le fonctionnement détaillé du moteur audio procédural de l'application Arthur (`ProceduralMusicEngine`). Il confirme que la musique est générée à 100 % de manière algorithmique à partir de zéro, sans aucun échantillon audio préenregistré.

---

## Vue d'ensemble du Pipeline Audio Procédural

```
[ Artwork + Session Salt ]
           │
           ▼ (1. Seed / Hash FNV-1a)
   [ MusicPreset ] ── (Root Hz, Tempo, Density, Scales, Banks)
           │
           ├──────────────────────────┐
           ▼ (2. Chord Progression)   ▼ (3. Melody Generator)
     [ HarmonyBank ]            [ MelodyBank / RhythmBank ]
     [ MarkovChain ]            [ MarkovChain Interconnectées ]
           │                          │
           └───────────┬──────────────┘
                       ▼ (4. MIDI / Note Events)
           [ NoteOn / Velocity / Timing ]
           [ Humanizer (Micro-jitter)  ]
                       │
                       ▼ (5. Synth DSP)
           [ Sine, Wavetable, Pluck, Lead, Noise ]
           [ RoomBus, Reverb, Chorus, Delay, Pan ]
                       │
                       ▼ (6. Audio Output Stream)
           [ Ping-Pong Float/Short PCM Buffers ]
           [ Android AudioTrack @ 48kHz ]
```

---

## Description Technique des Composants

### 1. Seed (Génération Déterministe)
* **Fichiers** : `MusicPresetResolver.kt`, `ProceduralMusicEngine.kt`
* **Mécanisme** :
  - Un hash FNV-1a 64-bit déterministe (`stableHash`) est calculé à partir de l'identifiant unique de chaque œuvre (`artwork.id`).
  - Ce hash dérive l'ensemble des paramètres musicaux de base dans `MusicPresetResolver.resolve()` :
    - **Fondamentale** : `rootHz` (convertie depuis un pitch MIDI $42..53 \rightarrow 92..165\text{ Hz}$).
    - **Gamme** : `scaleSemitones` extraite de `ScaleLibrary`.
    - **Tempo** : `tempoBpm` adapté au style.
    - **Densité & Ornementation** : `density`, `ornamentRate`.
    - **Indices de banques** : `melodyBankIndex`, `rhythmBankIndex`, `harmonyBankIndex`, `textureBankIndex`.
    - **Graine d'arrangement** : `formSeed`.
  - Dans `ProceduralMusicEngine.kt`, la graine d'œuvre est combinée avec `sessionSalt` (`preset.artworkSeed xor sessionSalt`) pour garantir la variabilité d'exécution entre sessions tout en maintenant l'identité musicale liée à l'œuvre.

### 2. Chord Progression (Progression d'Accords Algorithmique)
* **Fichiers** : `HarmonyBank.kt`, `MarkovChain.kt`, `MarkovSequencer.kt`
* **Mécanisme** :
  - **États d'harmonie** : `HarmonyState` (`I`, `ii`, `iii`, `IV`, `V`, `vi`, `viiDim`).
  - **Chaîne de Markov** : L'état d'harmonie évolue selon une matrice de probabilités (`harmonyTransitions()`) ou une séquence prédéfinie selon le style dans `HarmonyBank`.
  - **Voisement (*Voice Leading*)** : Les méthodes `harmonyPartialsHz()` et `voicedPartialsHz()` dérivent les fréquences des partielles de l'accord en temps réel. La fonction `voiceLead()` ré-ordonne les inversions pour minimiser la distance en fréquences entre les accords successifs.

### 3. Melody Generator (Générateur Algorithmique de Mélodies)
* **Fichiers** : `MelodyBank.kt`, `RhythmBank.kt`, `MarkovSequencer.kt`, `GrooveTransport.kt`
* **Mécanisme** :
  - **Séquençage Markovien** : `pitchChain` gère la séquence de motifs mélodiques (`MelodyMotif`) tandis que `rhythmChain` et `groovePatternChain` gèrent les jetons et motifs rythmiques.
  - **Planification de phrases (4 étapes)** :
    1. **Thème ($A$)** : Motif principal.
    2. **Variation ($A'$)** : Inversion, transposition ou rétrogradation partielle.
    3. **Contraste ($B$)** : Transposition plus élevée.
    4. **Cadence ($A$)** : Résolution sur la tonique.
  - **Conduite mélodique** :
    - `snapToChordTone()` : Attraction probabiliste des notes de la gamme vers les partielles de l'accord en cours.
    - Limitation des sauts d'intervalles (*stepwise motion*) pour privilégier le mouvement conjoint.
    - `degreeToHz()` : Conversion des degrés en Hertz avec encadrement de registre ($180\text{ Hz} \le f \le 784\text{ Hz}$).
  - **Transport Groove** : `GrooveTransport` assure une grille à croche balancée (*swing*) pour les styles Jazz, Lounge et amples.

### 4. MIDI Events / Note Events (Événements de Notes & Humanisation)
* **Fichiers** : `Humanizer.kt`, `MarkovSequencer.kt`, `ProceduralMusicEngine.kt`
* **Mécanisme** :
  - Événements de déclenchement générés lors de la progression temporelle du moteur :
    - `tickMelody()` : Note-on pour la mélodie principale.
    - `tickBass()` : Note-on pour la ligne de basse.
    - `fireJazzComping()` : Déclenchement des voicings d'accompagnement (ex: $3^{\text{e}} + 7^{\text{e}}$).
    - `nextOrnament()` : Déclenchement d'ornements (Grace notes, Rolls, Double strikes).
  - Chaque événement transporte : fréquence (Hz), vélocité ($0.0 .. 1.0$), durée et timing.
  - **Humanisation** : `Humanizer.kt` applique un modèle probabiliste avec micro-jitter de timing et légères variations de vélocité pour reproduire les imperfections naturelles d'une interprétation humaine.

### 5. Synth (Voix de Synthèse Numérique & DSP)
* **Fichiers** : Répertoire `fr.geoking.arthur.audio.voices` et `fr.geoking.arthur.audio.fx`
* **Mécanisme** :
  - **Pads & Drones** : `SinePadVoice` (synthèse sinusoïdale polyphonique à glissement/portamento).
  - **Pianos** : `WavetablePianoPool` et `SoftPianoPool` (synthèse par table d'ondes avec enveloppe ADSR).
  - **Cordes & Guitares** : `PluckGuitarVoice`, `KalimbaPluckVoice` (modélisation Karplus-Strong / résonateur).
  - **Leads & Chiptune** : `SquareLeadVoice` (générateur d'onde carrée à rapport cyclique variable).
  - **Basses** : `SoftBassVoice` (synthèse de basse fondamentale avec sous-harmonique).
  - **Textures & Percussions** : `WindTextureVoice`, `WaveNoiseVoice`, `RainNoiseVoice`, `SoftKitVoice` (bruits filtrés et résonateurs).
  - **Chine & Bols** : `ChimeClusterVoice`, `BowlVoice`.
  - **Traitements DSP & Effets** :
    - `RoomBus` : Filtrage passe-bas et bus acoustique.
    - `StereoReverb` : Réverbération stéréo algorithmique.
    - `StereoChorus` : Chorus stéréo pour l'élargissement des pads.
    - `PingPongDelay` : Délai ping-pong stéréo synchronisé au tempo.
    - `softLimit()` : Limiteur d'amplitude anti-clipping.

### 6. Audio Output (Rendu PCM & AudioTrack)
* **Fichiers** : `ProceduralMusicEngine.kt`, `CpuLoadTracker.kt`
* **Mécanisme** :
  - Thread audio dédié à priorité urgente (`Process.THREAD_PRIORITY_URGENT_AUDIO`) exécutant `renderAudioLoop()`.
  - Calcul continu des échantillons stereo entrelacés en Float PCM 32-bit ou Short PCM 16-bit.
  - Tamponnage double-buffer (*Ping-Pong buffers* de 2048 échantillons) pour éviter tout craquement.
  - Écriture bloquante dans le stream `android.media.AudioTrack`.
  - `CpuLoadTracker` : Gestionnaire de charge CPU adaptatif ajustant dynamiquement la qualité de synthèse (`Full`, `Reduced`, `Minimal`).

---

## Validation et Tests

Les tests unitaires couvrant l'ensemble du pipeline procédural sont exécutés via la commande Gradle suivante :

```bash
./gradlew :androidApp:testDebugUnitTest
```

Ces tests vérifient notamment :
- L'étalement de la gamme mélodique et le nombre de piques de fréquences distinctes (`PhraseCompositionTest`).
- Le respect des limites d'octave et de registre médium (`MELODY_MIN_HZ` et `MELODY_MAX_HZ`).
- La continuité du voisement des accords (`voicedPartialsHz`).
- La cohérence rythmique du `GrooveTransport` et des chaînes de Markov.
- La stabilité des synthétiseurs sous différentes charges CPU.
