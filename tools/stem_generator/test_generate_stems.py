#!/usr/bin/env python3
import unittest
import os
import json
import tempfile
import shutil
from tools.stem_generator.generate_stems import (
    generate_sine,
    generate_white_noise,
    generate_nature_rain,
    generate_scifi_drone,
    generate_jazz_chords,
    generate_percussion_beat,
    build_llm_arrangement_prompt,
    save_wav,
    convert_wav_to_opus,
    SAMPLE_RATE
)

class TestStemGenerator(unittest.TestCase):

    def test_audio_generators_length(self):
        duration = 2.0
        expected_samples = int(SAMPLE_RATE * duration)

        self.assertEqual(len(generate_sine(440, duration)), expected_samples)
        self.assertEqual(len(generate_white_noise(duration)), expected_samples)
        self.assertEqual(len(generate_nature_rain(duration)), expected_samples)
        self.assertEqual(len(generate_scifi_drone(duration)), expected_samples)
        self.assertEqual(len(generate_jazz_chords(duration)), expected_samples)
        self.assertEqual(len(generate_percussion_beat(duration)), expected_samples)

    def test_arrangement_schema(self):
        arr = build_llm_arrangement_prompt("ambient_jazz")
        self.assertIn("id", arr)
        self.assertIn("bpm", arr)
        self.assertIn("tracks", arr)
        self.assertGreater(len(arr["tracks"]), 0)

        for track in arr["tracks"]:
            self.assertIn("id", track)
            self.assertIn("category", track)
            self.assertIn("filename", track)
            self.assertIn("defaultGain", track)

    def test_wav_and_opus_export(self):
        temp_dir = tempfile.mkdtemp()
        try:
            samples = generate_sine(440, 1.0)
            wav_path = os.path.join(temp_dir, "test.wav")
            opus_path = os.path.join(temp_dir, "test.opus")

            save_wav(wav_path, samples)
            self.assertTrue(os.path.exists(wav_path))

            convert_wav_to_opus(wav_path, opus_path)
            self.assertTrue(os.path.exists(opus_path))
            self.assertGreater(os.path.getsize(opus_path), 0)
        finally:
            shutil.rmtree(temp_dir)

if __name__ == "__main__":
    unittest.main()
