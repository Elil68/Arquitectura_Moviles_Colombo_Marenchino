package com.example.tp1.util

import com.example.tp1.data.model.RetentionProgress
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Reglas de gamificación (estrategia de retención): racha de días seguidos
 * completando al menos una tarea y logros por hitos. Es lógica pura para
 * poder probarla con tests unitarios.
 */
object StreakRules {

    const val ACH_FIRST_TASK = "first_task"
    const val ACH_TEN_TASKS = "ten_tasks"
    const val ACH_STREAK_3 = "streak_3"
    const val ACH_STREAK_7 = "streak_7"

    val ACHIEVEMENT_TITLES = mapOf(
        ACH_FIRST_TASK to "Primera tarea completada",
        ACH_TEN_TASKS to "10 tareas completadas",
        ACH_STREAK_3 to "Racha de 3 días",
        ACH_STREAK_7 to "Racha de 7 días"
    )

    fun dayKey(millis: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis))

    fun previousDayKey(millis: Long): String {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            add(Calendar.DAY_OF_YEAR, -1)
        }
        return dayKey(cal.timeInMillis)
    }

    /** Devuelve el progreso actualizado al completar una tarea en [nowMillis]. */
    fun onTaskCompleted(current: RetentionProgress, nowMillis: Long): RetentionProgress {
        val today = dayKey(nowMillis)
        val yesterday = previousDayKey(nowMillis)

        val newStreak = when (current.lastCompletionDay) {
            today -> maxOf(current.streak, 1)
            yesterday -> current.streak + 1
            else -> 1
        }
        val total = current.totalCompleted + 1

        val achievements = current.achievements.toMutableSet()
        if (total >= 1) achievements += ACH_FIRST_TASK
        if (total >= 10) achievements += ACH_TEN_TASKS
        if (newStreak >= 3) achievements += ACH_STREAK_3
        if (newStreak >= 7) achievements += ACH_STREAK_7

        return current.copy(
            streak = newStreak,
            bestStreak = maxOf(current.bestStreak, newStreak),
            lastCompletionDay = today,
            totalCompleted = total,
            achievements = achievements.toList().sorted()
        )
    }
}
