package com.example.tp1.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.tp1.data.model.AcademicTask
import com.example.tp1.data.repository.AuthRepository
import com.example.tp1.data.repository.TaskRepository
import com.google.firebase.firestore.ListenerRegistration

data class TaskUiState(
    val isLoading: Boolean = true,
    val tasks: List<AcademicTask> = emptyList(),
    val errorMessage: String? = null
)

class TaskViewModel : ViewModel() {

    private val taskRepository = TaskRepository()
    private val authRepository = AuthRepository()
    private var listener: ListenerRegistration? = null

    var uiState by mutableStateOf(TaskUiState())
        private set

    fun startListening() {
        if (listener != null) return

        val userId = authRepository.currentUserId()

        if (userId == null) {
            uiState = TaskUiState(
                isLoading = false,
                errorMessage = "No hay un usuario autenticado."
            )
            return
        }

        uiState = uiState.copy(isLoading = true)

        listener = taskRepository.observeTasks(
            userId = userId,
            onChange = { tasks ->
                uiState = TaskUiState(
                    isLoading = false,
                    tasks = tasks
                )
            },
            onError = { exception ->
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = exception.localizedMessage
                        ?: "No se pudieron cargar las tareas."
                )
            }
        )
    }

    fun addTask(
        title: String,
        description: String,
        startDate: Long?,
        endDate: Long?
    ) {
        val userId = authRepository.currentUserId() ?: return

        if (title.isBlank()) {
            uiState = uiState.copy(
                errorMessage = "La tarea debe tener un título."
            )
            return
        }

        if (startDate != null && endDate != null && startDate > endDate) {
            uiState = uiState.copy(
                errorMessage = "La fecha de inicio debe ser anterior a la fecha de fin."
            )
            return
        }

        taskRepository.addTask(
            userId = userId,
            title = title.trim(),
            description = description.trim(),
            startDate = startDate,
            endDate = endDate
        ) { result ->
            result.onFailure { exception ->
                uiState = uiState.copy(
                    errorMessage = exception.localizedMessage
                        ?: "No se pudo guardar la tarea."
                )
            }
        }
    }

    fun toggleTask(task: AcademicTask) {
        val userId = authRepository.currentUserId() ?: return

        taskRepository.setTaskCompleted(
            userId = userId,
            taskId = task.id,
            completed = !task.completed
        ) { result ->
            result.onFailure { exception ->
                uiState = uiState.copy(
                    errorMessage = exception.localizedMessage
                        ?: "No se pudo actualizar la tarea."
                )
            }
        }
    }

    fun deleteTask(task: AcademicTask) {
        val userId = authRepository.currentUserId() ?: return

        taskRepository.deleteTask(
            userId = userId,
            taskId = task.id
        ) { result ->
            result.onFailure { exception ->
                uiState = uiState.copy(
                    errorMessage = exception.localizedMessage
                        ?: "No se pudo eliminar la tarea."
                )
            }
        }
    }

    fun stopListening() {
        listener?.remove()
        listener = null
        uiState = TaskUiState()
    }

    override fun onCleared() {
        listener?.remove()
        super.onCleared()
    }
}
