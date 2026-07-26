package com.habitloop.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChallengeCatalogTest {
    @Test
    fun challengeIdsAreUniqueAndSchedulesAreValid() {
        assertEquals(ChallengeCatalog.all.size, ChallengeCatalog.all.map { it.id }.distinct().size)
        ChallengeCatalog.all.forEach { challenge ->
            val days = challenge.scheduleDaysCsv.split(",").map { it.toInt() }
            assertTrue(challenge.title.isNotBlank())
            assertTrue(challenge.durationDays > 0)
            assertTrue(days.isNotEmpty())
            assertTrue(days.all { it in 1..7 })
        }
    }
}
