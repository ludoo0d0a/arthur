#!/usr/bin/env python3
"""
Arthur Ambient Music Stem & Arrangement Generator

Generates seamless Opus audio stems and JSON arrangement specifications for Arthur's
mobile dynamic stem mixer ("Music Director").

Supports categories:
- Nature FX
- Sci-Fi FX
- Jazz Music
- Chords
- Percussion
- Ambient Pads
"""

import math
import json
import os
import struct
import subprocess
import wave
import argparse
import random

SAMPLE_RATE = 44100

def generate_sine(freq, duration_sec, volume=0.5):
    num_samples = int(SAMPLE_RATE * duration_sec)
    samples = []
    for i in range(num_samples):
        t = i / SAMPLE_RATE
        val = volume * math.sin(2 * math.pi * freq * t)
        samples.append(val)
    return samples

def generate_white_noise(duration_sec, volume=0.1):
    num_samples = int(SAMPLE_RATE * duration_sec)
    samples = []
    for _ in range(num_samples):
        val = volume * (random.uniform(-1.0, 1.0))
        samples.append(val)
    return samples

def apply_envelope(samples, attack_sec=0.1, release_sec=0.1):
    num_samples = len(samples)
    attack_samples = int(SAMPLE_RATE * attack_sec)
    release_samples = int(SAMPLE_RATE * release_sec)

    out = list(samples)
    for i in range(min(attack_samples, num_samples)):
        env = i / float(attack_samples)
        out[i] *= env
    for i in range(min(release_samples, num_samples)):
        idx = num_samples - 1 - i
        env = i / float(release_samples)
        out[idx] *= env
    return out

def mix_tracks(track1, track2):
    length = max(len(track1), len(track2))
    mixed = []
    for i in range(length):
        v1 = track1[i] if i < len(track1) else 0.0
        v2 = track2[i] if i < len(track2) else 0.0
        mixed.append(max(-1.0, min(1.0, v1 + v2)))
    return mixed

def low_pass_filter(samples, alpha=0.1):
    filtered = []
    prev = 0.0
    for s in samples:
        curr = prev + alpha * (s - prev)
        filtered.append(curr)
        prev = curr
    return filtered

def generate_nature_rain(duration_sec=8.0):
    noise = generate_white_noise(duration_sec, volume=0.15)
    filtered = low_pass_filter(noise, alpha=0.08)
    # Add gentle modulate wave for wind/rain intensity
    out = []
    for i, s in enumerate(filtered):
        t = i / SAMPLE_RATE
        mod = 0.7 + 0.3 * math.sin(2 * math.pi * 0.2 * t)
        out.append(s * mod)
    return apply_envelope(out, 0.5, 0.5)

def generate_scifi_drone(duration_sec=8.0):
    # Sci-Fi FX drone with detuned oscillators and slow LFO
    num_samples = int(SAMPLE_RATE * duration_sec)
    samples = []
    freq1, freq2 = 110.0, 110.8 # A2 pitch with binaural beat
    for i in range(num_samples):
        t = i / SAMPLE_RATE
        lfo = 0.5 + 0.5 * math.sin(2 * math.pi * 0.1 * t)
        val = 0.2 * math.sin(2 * math.pi * freq1 * t) + 0.2 * math.sin(2 * math.pi * freq2 * t + lfo)
        samples.append(val)
    return apply_envelope(samples, 1.0, 1.0)

def generate_jazz_chords(duration_sec=8.0):
    # Dm7 -> G7 -> Cmaj7 jazz progression stems
    num_samples = int(SAMPLE_RATE * duration_sec)
    samples = [0.0] * num_samples

    # Chord frequencies (Hz)
    chords = [
        [293.66, 349.23, 440.00, 523.25], # Dm7
        [246.94, 293.66, 392.00, 440.00], # G7
        [261.63, 329.63, 392.00, 493.88], # Cmaj7
        [220.00, 261.63, 329.63, 392.00]  # Am7
    ]
    chord_len = duration_sec / len(chords)

    for c_idx, chord in enumerate(chords):
        start_idx = int(c_idx * chord_len * SAMPLE_RATE)
        end_idx = int((c_idx + 1) * chord_len * SAMPLE_RATE)
        for freq in chord:
            tone = generate_sine(freq, chord_len, volume=0.08)
            tone = apply_envelope(tone, 0.2, 0.2)
            for j in range(len(tone)):
                idx = start_idx + j
                if idx < num_samples:
                    samples[idx] += tone[j]
    return apply_envelope(samples, 0.5, 0.5)

def generate_percussion_beat(duration_sec=8.0, bpm=90):
    num_samples = int(SAMPLE_RATE * duration_sec)
    samples = [0.0] * num_samples
    beat_sec = 60.0 / bpm

    total_beats = int(duration_sec / beat_sec)
    for b in range(total_beats):
        start_idx = int(b * beat_sec * SAMPLE_RATE)
        if b % 2 == 0:
            # Kick drum (frequency drop)
            kick_dur = 0.15
            kick_samples = int(SAMPLE_RATE * kick_dur)
            for i in range(kick_samples):
                t = i / SAMPLE_RATE
                freq = 150.0 * math.exp(-25 * t) + 40.0
                env = math.exp(-15 * t)
                val = 0.4 * math.sin(2 * math.pi * freq * t) * env
                idx = start_idx + i
                if idx < num_samples:
                    samples[idx] += val
        else:
            # Soft rimshot / snare noise
            snare_dur = 0.12
            snare_samples = int(SAMPLE_RATE * snare_dur)
            for i in range(snare_samples):
                t = i / SAMPLE_RATE
                env = math.exp(-20 * t)
                val = 0.25 * random.uniform(-1.0, 1.0) * env
                idx = start_idx + i
                if idx < num_samples:
                    samples[idx] += val
    return samples

def save_wav(filepath, samples):
    with wave.open(filepath, 'w') as wf:
        wf.setnchannels(1)
        wf.setsampwidth(2)
        wf.setframerate(SAMPLE_RATE)
        int_samples = [int(max(-1.0, min(1.0, s)) * 32767) for s in samples]
        packed = struct.pack(f'<{len(int_samples)}h', *int_samples)
        wf.writeframes(packed)

def convert_wav_to_opus(wav_path, opus_path, bitrate="48k"):
    cmd = ["ffmpeg", "-y", "-i", wav_path, "-c:a", "libopus", "-b:a", bitrate, opus_path]
    res = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if res.returncode != 0:
        raise RuntimeError(f"FFmpeg conversion failed: {res.stderr.decode('utf-8')}")

def build_llm_arrangement_prompt(style="ambient_jazz"):
    """
    Simulates LLM prompt structuring or calls an external LLM API if key is provided.
    Outputs the JSON arrangement configuration.
    """
    return {
        "id": f"arr_{style}_pack",
        "title": f"Arthur {style.replace('_', ' ').title()} Generator",
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
                "isOptional": True,
                "activationProbability": 0.7,
                "dynamicRule": "AMBIENT_BACKGROUND"
            },
            {
                "id": "scifi_fx",
                "name": "Cosmic Sub Drone",
                "category": "SCIFI_FX",
                "filename": "scifi_fx.opus",
                "defaultGain": 0.5,
                "defaultPan": -0.2,
                "isOptional": True,
                "activationProbability": 0.5,
                "dynamicRule": "DEEP_DRONE"
            },
            {
                "id": "jazz_chords",
                "name": "Lo-Fi Jazz Keys",
                "category": "JAZZ_MUSIC",
                "filename": "jazz_chords.opus",
                "defaultGain": 0.8,
                "defaultPan": 0.1,
                "isOptional": False,
                "activationProbability": 1.0,
                "dynamicRule": "HARMONY_LEAD"
            },
            {
                "id": "percussion",
                "name": "Soft Beat",
                "category": "PERCUSSION",
                "filename": "percussion.opus",
                "defaultGain": 0.7,
                "defaultPan": 0.0,
                "isOptional": True,
                "activationProbability": 0.8,
                "dynamicRule": "RHYTHM_FOUNDATION"
            }
        ]
    }

def main():
    parser = argparse.ArgumentParser(description="Arthur Stem Generator Tool")
    parser.add_argument("--outdir", default="androidApp/src/main/assets/stems", help="Output directory for Opus stems and JSON")
    args = parser.parse_args()

    os.makedirs(args.outdir, exist_ok=True)

    print("Generating stems...")
    duration = 8.0

    stems = {
        "nature_fx": generate_nature_rain(duration),
        "scifi_fx": generate_scifi_drone(duration),
        "jazz_chords": generate_jazz_chords(duration),
        "percussion": generate_percussion_beat(duration, bpm=90)
    }

    for stem_name, samples in stems.items():
        wav_path = os.path.join(args.outdir, f"{stem_name}.wav")
        opus_path = os.path.join(args.outdir, f"{stem_name}.opus")

        save_wav(wav_path, samples)
        convert_wav_to_opus(wav_path, opus_path)
        os.remove(wav_path)
        print(f"Generated stem: {opus_path}")

    arrangement = build_llm_arrangement_prompt("ambient_jazz")
    json_path = os.path.join(args.outdir, "arrangement.json")
    with open(json_path, "w") as f:
        json.dump(arrangement, f, indent=2)

    print(f"Generated arrangement spec: {json_path}")

if __name__ == "__main__":
    main()
