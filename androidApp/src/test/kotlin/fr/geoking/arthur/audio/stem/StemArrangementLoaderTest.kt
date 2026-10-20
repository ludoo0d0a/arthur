package fr.geoking.arthur.audio.stem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StemArrangementLoaderTest {

    @Test
    fun testJsonDeserialization() {
        val jsonStr = """
            {
              "id": "arr_test",
              "title": "Test Suite Arrangement",
              "bpm": 120,
              "key": "A Minor",
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
                  "name": "Jazz Keys",
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
        """.trimIndent()

        val arrangement = StemArrangementLoader.loadFromString(jsonStr)

        assertEquals("arr_test", arrangement.id)
        assertEquals("Test Suite Arrangement", arrangement.title)
        assertEquals(120, arrangement.bpm)
        assertEquals(2, arrangement.tracks.size)

        val t1 = arrangement.tracks[0]
        assertEquals("nature_fx", t1.id)
        assertEquals(StemCategory.NATURE_FX, t1.category)
        assertTrue(t1.isOptional)
        assertEquals(0.6f, t1.defaultGain, 0.001f)

        val t2 = arrangement.tracks[1]
        assertEquals("jazz_chords", t2.id)
        assertEquals(StemCategory.JAZZ_MUSIC, t2.category)
        assertFalse(t2.isOptional)
    }
}
