package ru.senin.kotlin.wiki

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class StatsTest {

    private val testStats = Stats(hashMapOf("снегири" to 2, "гири" to 3),
        hashMapOf("вополе" to 4, "берёзка" to 9, "стояла" to 5), hashMapOf(0 to 0, 1 to 3), hashMapOf(2016 to 3))

    @Test
    fun testStatsUpdate1() {
        val actual = testStats
        actual.statsUpdate(mutableListOf("снегири", "онигири"), mutableListOf("берёзка", "берёзка", "грёзка"),
            1, 2017)
        val expected = Stats(hashMapOf("снегири" to 3, "гири" to 3, "онигири" to 1),
            hashMapOf("вополе" to 4, "берёзка" to 11, "стояла" to 5, "грёзка" to 1),
            hashMapOf(0 to 0, 1 to 4), hashMapOf(2016 to 3, 2017 to 1))

        statsAssert(expected, actual)
    }

    @Test
    fun testStatsUpdate2() {
        val actual = testStats
        actual.statsUpdate(mutableListOf("лошадка"), mutableListOf("крюк"),
            timeText = 852)
        val expected = Stats(hashMapOf("снегири" to 2, "гири" to 3, "лошадка" to 1),
            hashMapOf("вополе" to 4, "берёзка" to 9, "стояла" to 5, "крюк" to 1),
            hashMapOf(0 to 0, 1 to 3), hashMapOf(2016 to 3, 852 to 1))

        statsAssert(expected, actual)
    }

    @Test
    fun testMerge1() {
        val actual = testStats
        actual.merge(
            Stats(hashMapOf("снегири" to 1, "пасхалка" to 6001),
                hashMapOf("сантехник" to 2, "лемонграсс" to 3, "берёзка" to 4),
                hashMapOf(0 to 2, 2 to 1), hashMapOf(2016 to 1, 2001 to 2)))
        val expected = Stats(hashMapOf("снегири" to 3, "гири" to 3, "пасхалка" to 6001),
            hashMapOf("вополе" to 4, "берёзка" to 13, "стояла" to 5, "сантехник" to 2, "лемонграсс" to 3),
            hashMapOf(0 to 2, 1 to 3, 2 to 1), hashMapOf(2016 to 4, 2001 to 2))

        statsAssert(expected, actual)
    }

    @Test
    fun testMerge2() {
        val actual = testStats
        actual.merge(
            Stats(text = hashMapOf("многословный" to 123456789), bytes = hashMapOf(4 to 1),
                time = hashMapOf(2016 to 1)))
        val expected = Stats(hashMapOf("снегири" to 2, "гири" to 3),
            hashMapOf("вополе" to 4, "берёзка" to 9, "стояла" to 5, "многословный" to 123456789),
            hashMapOf(0 to 0, 1 to 3, 4 to 1), hashMapOf(2016 to 4))

        statsAssert(expected, actual)
    }

    private fun statsAssert(expected : Stats, actual : Stats) {
        assertEquals(expected.title, actual.title, "This function doesn't calculate titles correctly!")
        assertEquals(expected.text, actual.text, "This function doesn't calculate texts correctly!")
        assertEquals(expected.bytes, actual.bytes, "This function doesn't calculate bytes correctly!")
        assertEquals(expected.time, actual.time, "This function doesn't calculate time correctly!")
    }
}