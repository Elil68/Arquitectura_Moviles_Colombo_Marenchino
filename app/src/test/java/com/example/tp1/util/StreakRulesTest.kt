package com.example.tp1.util

import com.example.tp1.data.model.RetentionProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class StreakRulesTest {

    private fun day(offsetDays: Int): Long = Calendar.getInstance().apply {
        set(2026, Calendar.NOVEMBER, 10, 12, 0, 0)
        add(Calendar.DAY_OF_YEAR, offsetDays)
    }.timeInMillis

    @Test
    fun firstCompletionStartsStreakAndUnlocksFirstAchievement() {
        val result = StreakRules.onTaskCompleted(RetentionProgress(), day(0))
        assertEquals(1, result.streak)
        assertEquals(1, result.totalCompleted)
        assertTrue(StreakRules.ACH_FIRST_TASK in result.achievements)
    }

    @Test
    fun sameDayDoesNotIncreaseStreak() {
        val first = StreakRules.onTaskCompleted(RetentionProgress(), day(0))
        val second = StreakRules.onTaskCompleted(first, day(0))
        assertEquals(1, second.streak)
        assertEquals(2, second.totalCompleted)
    }

    @Test
    fun consecutiveDaysIncreaseStreakAndUnlockStreakAchievements() {
        var p = RetentionProgress()
        for (i in 0..2) p = StreakRules.onTaskCompleted(p, day(i))
        assertEquals(3, p.streak)
        assertTrue(StreakRules.ACH_STREAK_3 in p.achievements)
    }

    @Test
    fun missingADayResetsStreakButKeepsBest() {
        var p = RetentionProgress()
        for (i in 0..2) p = StreakRules.onTaskCompleted(p, day(i))
        p = StreakRules.onTaskCompleted(p, day(5))
        assertEquals(1, p.streak)
        assertEquals(3, p.bestStreak)
    }

    @Test
    fun tenTasksUnlockAchievement() {
        var p = RetentionProgress()
        repeat(10) { p = StreakRules.onTaskCompleted(p, day(0)) }
        assertTrue(StreakRules.ACH_TEN_TASKS in p.achievements)
    }
}
