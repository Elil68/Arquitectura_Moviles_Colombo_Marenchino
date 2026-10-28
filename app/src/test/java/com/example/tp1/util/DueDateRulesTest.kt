package com.example.tp1.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DueDateRulesTest {

    private fun millisFor(year: Int, month: Int, day: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.clear()
        calendar.set(year, month - 1, day, 0, 0, 0)
        return calendar.timeInMillis
    }

    @Test
    fun `convierte milisegundos a fecha ISO`() {
        val millis = millisFor(2026, 12, 25)
        assertEquals("2026-12-25", DueDateRules.toIsoDate(millis))
    }

    @Test
    fun `detecta fin de semana`() {
        val saturday = millisFor(2026, 10, 31)
        val monday = millisFor(2026, 11, 2)

        assertTrue(DueDateRules.isWeekend(saturday))
        assertFalse(DueDateRules.isWeekend(monday))
    }

    @Test
    fun `el feriado tiene prioridad sobre el fin de semana`() {
        val saturdayHoliday = millisFor(2026, 10, 31)
        val holidays = mapOf("2026-10-31" to "Feriado de prueba")

        val warning = DueDateRules.checkDueDate(saturdayHoliday, holidays)

        assertTrue(warning is DueDateWarning.Holiday)
        assertEquals("Feriado de prueba", (warning as DueDateWarning.Holiday).name)
    }

    @Test
    fun `no hay aviso en un dia habil sin feriados`() {
        val monday = millisFor(2026, 11, 2)
        val warning = DueDateRules.checkDueDate(monday, emptyMap())
        assertNull(warning)
    }
}
