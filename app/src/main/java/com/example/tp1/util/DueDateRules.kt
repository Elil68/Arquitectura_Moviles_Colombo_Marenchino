package com.example.tp1.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed class DueDateWarning {
    data class Holiday(val name: String) : DueDateWarning()
    object Weekend : DueDateWarning()
}

/**
 * Reglas puras (sin red ni Android) para avisar sobre la fecha de
 * vencimiento de una tarea: fin de semana o feriado nacional (AR).
 * El feriado tiene prioridad sobre el fin de semana.
 */
object DueDateRules {

    fun toIsoDate(millis: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis))

    fun isWeekend(millis: Long): Boolean {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = millis
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY, Calendar.SUNDAY -> true
            else -> false
        }
    }

    fun checkDueDate(millis: Long, holidays: Map<String, String>): DueDateWarning? {
        val holidayName = holidays[toIsoDate(millis)]
        return when {
            holidayName != null -> DueDateWarning.Holiday(holidayName)
            isWeekend(millis) -> DueDateWarning.Weekend
            else -> null
        }
    }
}
