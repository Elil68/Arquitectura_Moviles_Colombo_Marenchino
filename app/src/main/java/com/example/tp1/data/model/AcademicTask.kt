package com.example.tp1.data.model

data class AcademicTask(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val completed: Boolean = false,
    val startDate: Long? = null,
    val endDate: Long? = null,
    // Orden manual (arrastrar) entre las tareas sin fecha de fin.
    // Por defecto es la fecha de creación: así las tareas nuevas quedan
    // al final de la lista manual sin tener que conocer el máximo actual.
    val order: Long = 0
)
