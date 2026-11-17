package com.example.tp1.ui.tasks

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tp1.data.model.AcademicTask
import com.example.tp1.viewmodel.TaskUiState
import androidx.compose.runtime.remember
import java.util.UUID

/** Local UI verification only: no authentication, backend writes or persistence. */
@Composable
fun LocalDemoScreen() {
    var tasks by remember {
        mutableStateOf(listOf(
            AcademicTask(id = "demo-1", title = "Revisar entrega TP4",
                description = "Tarea de demostración local"),
            AcademicTask(id = "demo-2", title = "Organizar apuntes", completed = true)
        ))
    }
    var closed by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
            Text(
                "Demostración local · datos temporales · sin Firebase",
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp),
                style = MaterialTheme.typography.labelMedium
            )
        }
        if (closed) {
            androidx.compose.material3.Button(onClick = { closed = false }) {
                Text("Volver a la demostración")
            }
        } else {
            TaskListScreen(
                state = TaskUiState(isLoading = false, tasks = tasks),
                onStartListening = {},
                onStopListening = {},
                onAddTask = { title, description, start, end ->
                    tasks = tasks + AcademicTask(
                        id = UUID.randomUUID().toString(), title = title,
                        description = description, startDate = start, endDate = end
                    )
                },
                onToggleTask = { selected ->
                    tasks = tasks.map {
                        if (it.id == selected.id) it.copy(completed = !it.completed) else it
                    }
                },
                onDeleteTask = { selected -> tasks = tasks.filterNot { it.id == selected.id } },
                onReorderTasks = { ids ->
                    tasks = tasks.map { task ->
                        val index = ids.indexOf(task.id)
                        if (index >= 0) task.copy(order = index.toLong()) else task
                    }.sortedBy { it.order }
                },
                onLogout = { closed = true }
            )
        }
    }
}
