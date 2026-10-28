package com.example.tp1.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import com.example.tp1.data.model.AcademicTask

/**
 * Crea un evento en la app de calendario del usuario (Intent.ACTION_INSERT)
 * a partir de una tarea. No requiere permisos ni cuenta de Google: el
 * usuario confirma el guardado en la app de calendario que elija.
 */
object CalendarIntentHelper {

    fun exportTaskToCalendar(context: Context, task: AcademicTask): Boolean {
        val start = task.startDate ?: task.endDate ?: System.currentTimeMillis()
        val end = task.endDate ?: start

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, task.title)
            putExtra(CalendarContract.Events.DESCRIPTION, task.description)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end)
        }

        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }
}
