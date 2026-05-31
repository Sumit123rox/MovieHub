package com.moviehub.core.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class RuntimeParserTest {

    @Test
    fun `parses ISO 8601 duration`() {
        assertEquals(7_980_000, parseRuntime("PT2H13M")) // 2h 13m
        assertEquals(600_000, parseRuntime("PT10M"))
        assertEquals(3_600_000, parseRuntime("PT1H"))
        assertEquals(5_000, parseRuntime("PT5S"))
        assertEquals(3_605_000, parseRuntime("PT1H5S"))
    }

    @Test
    fun `parses ISO 8601 with lowercase prefix`() {
        assertEquals(7_980_000, parseRuntime("pt2h13m"))
    }

    @Test
    fun `parses simple minutes format`() {
        assertEquals(7_980_000, parseRuntime("133 min"))
        assertEquals(7_800_000, parseRuntime("130m"))
        assertEquals(600_000, parseRuntime("10 min"))
    }

    @Test
    fun `parses hours and minutes format`() {
        assertEquals(7_980_000, parseRuntime("2h 13m"))
        assertEquals(7_800_000, parseRuntime("2 hr 10 min"))
        assertEquals(3_600_000, parseRuntime("1h 0m"))
    }

    @Test
    fun `parses plain digits as minutes`() {
        assertEquals(7_980_000, parseRuntime("133"))
        assertEquals(600_000, parseRuntime("10"))
        assertEquals(0, parseRuntime("0"))
    }

    @Test
    fun `handles whitespace around input`() {
        assertEquals(7_980_000, parseRuntime("  133 min  "))
        assertEquals(7_980_000, parseRuntime("\tPT2H13M\n"))
    }

    @Test
    fun `handles mixed case`() {
        assertEquals(7_980_000, parseRuntime("2H 13M"))
        assertEquals(7_980_000, parseRuntime("PT2H13M"))
    }

    @Test
    fun `returns 0 for empty or invalid input`() {
        assertEquals(0L, parseRuntime(""))
        assertEquals(0L, parseRuntime("  "))
        assertEquals(0L, parseRuntime("invalid"))
        assertEquals(0L, parseRuntime("abc123"))
    }

    @Test
    fun `handles various spacing`() {
        assertEquals(7_980_000, parseRuntime("2h13m")) // no space
        assertEquals(7_800_000, parseRuntime("2hr10min")) // no space, hr/min
    }

    @Test
    fun `handles edge case formats`() {
        // Single hour format
        val oneHour = parseRuntime("1h")
        assertNotEquals(0L, oneHour)
        assertEquals(3_600_000, oneHour)

        // "90 mins" variant
        assertEquals(5_400_000, parseRuntime("90 mins"))
    }
}
