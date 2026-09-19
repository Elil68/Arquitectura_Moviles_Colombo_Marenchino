package com.example.tp1.data.model

data class AcademicTask(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val completed: Boolean = false,
    val startDate: Long? = null,
    val endDate: Long? = null
)
