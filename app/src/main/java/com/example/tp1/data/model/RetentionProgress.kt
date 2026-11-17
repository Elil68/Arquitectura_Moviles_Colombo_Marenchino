package com.example.tp1.data.model

/** Progreso de gamificación del usuario: users/{uid}/retention/progress. */
data class RetentionProgress(
    val streak: Int = 0,
    val bestStreak: Int = 0,
    // Día (yyyy-MM-dd) de la última tarea completada.
    val lastCompletionDay: String = "",
    val totalCompleted: Int = 0,
    val achievements: List<String> = emptyList()
)
