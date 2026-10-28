package com.example.tp1.util

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Punto único para registrar los eventos de Firebase Analytics de la app
 * (fase Measure del ciclo Build-Measure-Learn).
 */
class AnalyticsLogger(context: Context) {

    private val analytics = FirebaseAnalytics.getInstance(context)

    fun logLogin() {
        analytics.logEvent(FirebaseAnalytics.Event.LOGIN, null)
    }

    fun logSignUp() {
        analytics.logEvent(FirebaseAnalytics.Event.SIGN_UP, null)
    }

    fun logTaskCreated() {
        analytics.logEvent("task_created", null)
    }

    fun logTaskStatusChanged(completed: Boolean) {
        val params = Bundle().apply {
            putBoolean("completed", completed)
        }
        analytics.logEvent("task_status_changed", params)
    }

    fun logTaskDeleted() {
        analytics.logEvent("task_deleted", null)
    }

    fun logTaskExportedToCalendar() {
        analytics.logEvent("task_exported_to_calendar", null)
    }

    fun logDueDateWarning(type: String) {
        val params = Bundle().apply {
            putString("warning_type", type)
        }
        analytics.logEvent("due_date_warning_shown", params)
    }
}
