package fr.geoking.arthur.shared.source

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RemoteSampleTest {
    @Test
    fun sampleReturnsAllWhenListSmallerThanCount() {
        assertEquals(listOf(1, 2), RemoteSample.sample(listOf(1, 2), 5, ZeroRandom))
    }

    @Test
    fun sampleTakesPrefixWhenZeroRandom() {
        assertEquals(
            listOf(10, 11, 12),
            RemoteSample.sample((10..100).toList(), 3, ZeroRandom),
        )
    }

    @Test
    fun randomPageIsOneBased() {
        assertEquals(1, RemoteSample.randomPage(maxPage = 20, random = ZeroRandom))
        assertTrue(RemoteSample.randomPage(maxPage = 5) in 1..5)
    }

    @Test
    fun randomStartIsAlignedAndZeroForZeroRandom() {
        assertEquals(0, RemoteSample.randomStart(pageSize = 60, random = ZeroRandom))
        val start = RemoteSample.randomStart(pageSize = 20, maxStart = 100)
        assertEquals(0, start % 20)
        assertTrue(start in 0..100)
    }
}
