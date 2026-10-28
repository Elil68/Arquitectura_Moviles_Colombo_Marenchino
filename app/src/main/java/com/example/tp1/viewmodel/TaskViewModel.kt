package com.example.tp1.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tp1.data.model.AcademicTask
import com.example.tp1.data.repository.AuthRepository
import com.example.tp1.data.repository.HolidayRepository
import com.example.tp1.data.repository.TaskRepository
import com.example.tp1.util.AnalyticsLogger
import com.example.tp1.util.DueDateRules
import com.example.tp1.util.DueDateWarning
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import java.util.Calendar

data class TaskUiState(
    val isLoading: Boolean = true,
    val tasks: List<AcademicTask> = emptyList(),
    val errorMessage: String? = null
)

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val taskRepository = TaskRepository()
    private val authRepository = AuthRepository()
    private val holidayRepository = HolidayRepository()
    private val analytics = AnalyticsLogger(application)
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
            result.onSuccess {
                analytics.logTaskCreated()
            }.onFailure { exception ->
                uiState = uiState.copy(
                    errorMessage = exception.localizedMessage
                        ?: "No se pudo guardar la tarea."
                )
            }
        }
    }

    fun toggleTask(task: AcademicTask) {
        val userId = authRepository.currentUserId() ?: return
        val newCompleted = !task.completed

        taskRepository.setTaskCompleted(
            userId = userId,
            taskId = task.id,
            completed = newCompleted
        ) { result ->
            result.onSuccess {
                analytics.logTaskStatusChanged(newCompleted)
            }.onFailure { exception ->
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
            result.onSuccess {
                analytics.logTaskDeleted()
            }.onFailure { exception ->
                uiState = uiState.copy(
                    errorMessage = exception.localizedMessage
                        ?: "No se pudo eliminar la tarea."
                )
            }
        }
    }

    /**
     * Avisa si [dateMillis] cae en un feriado nacional (AR) o en fin de
     * semana. Si la API de feriados no está disponible, solo se omite el
     * aviso de feriado (ver [HolidayRepository]).
     */
    fun checkDueDateWarning(dateMillis: Long, onResult: (DueDateWarning?) -> Unit) {
        viewModelScope.launch {
            val year = Calendar.getInstance().apply { timeInMillis = dateMillis }.get(Calendar.YEAR)
            val holidays = holidayRepository.getHolidaysMap(year)
            onResult(DueDateRules.checkDueDate(dateMillis, holidays))
        }
    }

    fun reorderUndatedTasks(orderedTaskIds: List<String>) {
        val userId = authRepository.currentUserId() ?: return

        taskRepository.updateTaskOrder(
            userId = userId,
            orderedTaskIds = orderedTaskIds
        ) { result ->
            result.onFailure { exception ->
                uiState = uiState.copy(
                    errorMessage = exception.localizedMessage
                        ?: "No se pudo reordenar la tarea."
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
