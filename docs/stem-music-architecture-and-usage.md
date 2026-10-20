# Stem Music Generation & Dynamic Mixing Architecture

This document describes the architectural design, workflow, and usage instructions for the Stem Music Generator tool and the Android `MusicDirector` dynamic stem mixer in Arthur.

---

## 1. Overview

Arthur's Stem Music subsystem delivers infinite, local generative music without storing large multi-gigabyte neural weight files on mobile devices.

The system is split into two primary components:
1. **PC Stem Generator Tool (`tools/stem_generator/`)**: Python pipeline that synthesizes high-efficiency Ogg Opus audio stems (Nature FX, Sci-Fi FX, Jazz Chords, Percussion, Pads) and outputs structured JSON arrangement specifications (`arrangement.json`).
2. **Android Music Director (`fr.geoking.arthur.audio.stem`)**: Kotlin & Media3 (`ExoPlayer`) engine running on device that parses arrangement JSONs, synchronizes parallel stem tracks, and dynamically adjusts stem muting/volume levels at loop boundaries based on probabilistic arrangement rules.

---

## 2. PC Stem Generator Pipeline

### Location
`tools/stem_generator/generate_stems.py`

### Capabilities & Synthesis
The PC tool synthesizes loopable audio stems using pure Python DSP (sine oscillators, white noise generators, low-pass filters, ADSR envelopes, beat sequences) and converts the resulting WAV tracks to Ogg Opus using FFmpeg.

Supported stem categories:
- `NATURE_FX`: Procedurally synthesized rain, wind, and ambient natural textures.
- `SCIFI_FX`: Sub-bass drones with binaural LFO modulation.
- `JAZZ_MUSIC`: Soft jazz chord progressions (e.g., Dm7 -> G7 -> Cmaj7 -> Am7).
- `PERCUSSION`: Soft kick and snare beat foundations.
- `CHORDS` / `AMBIENT_PAD`: Harmonic pad layers.

### Command Line Usage
```bash
# Generate stems and output to assets directory
python3 tools/stem_generator/generate_stems.py --outdir androidApp/src/main/assets/stems
```

### Generated Files
Outputs into `androidApp/src/main/assets/stems/`:
- `nature_fx.opus` (~40 KB)
- `scifi_fx.opus` (~50 KB)
- `jazz_chords.opus` (~50 KB)
- `percussion.opus` (~40 KB)
- `arrangement.json` (~1 KB)

Total disk footprint on Android: **< 200 KB** (versus multi-gigabyte neural models).

---

## 3. JSON Arrangement Schema

The arrangement spec (`arrangement.json`) defines track rules, probability weights, default gain, panning, and loop parameters:

```json
{
  "id": "arr_ambient_jazz_pack",
  "title": "Arthur Ambient Jazz Generator",
  "bpm": 90,
  "key": "C Major",
  "loopDurationSeconds": 8.0,
  "tracks": [
    {
      "id": "nature_fx",
      "name": "Rain & Wind",
      "category": "NATURE_FX",
      "filename": "nature_fx.opus",
      "defaultGain": 0.6,
      "defaultPan": 0.0,
      "isOptional": true,
      "activationProbability": 0.7,
      "dynamicRule": "AMBIENT_BACKGROUND"
    },
    {
      "id": "jazz_chords",
      "name": "Lo-Fi Jazz Keys",
      "category": "JAZZ_MUSIC",
      "filename": "jazz_chords.opus",
      "defaultGain": 0.8,
      "defaultPan": 0.1,
      "isOptional": false,
      "activationProbability": 1.0,
      "dynamicRule": "HARMONY_LEAD"
    }
  ]
}
```

---

## 4. Android `MusicDirector` Engine

### Key Classes (`fr.geoking.arthur.audio.stem`)
- **`StemArrangement` & `StemTrack`**: Data classes serialized with `kotlinx.serialization`.
- **`StemArrangementLoader`**: Loads arrangement JSON specs from Android `assets/stems/arrangement.json`.
- **`MusicDirector`**: Core Media3 ExoPlayer orchestrator.
  - Instantiates parallel `ExoPlayer` instances for each stem track.
  - Synchronizes seek position and playback start across all stem players.
  - Executes a Coroutine loop on each arrangement boundary (`loopDurationSeconds`), dynamically muting/unmuting optional stem layers according to their `activationProbability`.
  - Exposes state flows (`directorState`, `trackStatuses`) for real-time UI binding.

---

## 5. UI Integration

The Stem Music Pack tile is registered in `AudioPackCatalog.STEM` (`"stem"`) and displayed in the Sound Pack catalog under `AudioPackTopic.Stem`.

Tapping the tile launches `SoundPlayerScreen`, which initializes `MusicDirector`, starts dynamic stem playback, and provides play/pause controls.
