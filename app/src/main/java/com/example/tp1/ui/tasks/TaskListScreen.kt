package com.example.tp1.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.tp1.data.model.AcademicTask
import com.example.tp1.viewmodel.TaskUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

private fun formatDate(millis: Long): String = dateFormatter.format(Date(millis))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    state: TaskUiState,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onAddTask: (String, String, Long?, Long?) -> Unit,
    onToggleTask: (AcademicTask) -> Unit,
    onDeleteTask: (AcademicTask) -> Unit,
    onLogout: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        onStartListening()
        onDispose { onStopListening() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis tareas") },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text("Salir")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Text("+")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Buscar por título") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Box(modifier = Modifier.weight(1f)) {
                val filteredTasks = remember(state.tasks, searchQuery) {
                    state.tasks.filter { it.title.contains(searchQuery, ignoreCase = true) }
                }
                val now = System.currentTimeMillis()
                val overdueTasks = filteredTasks.filter {
                    !it.completed && it.endDate != null && it.endDate < now
                }
                val pendingTasks = filteredTasks.filter {
                    !it.completed && (it.endDate == null || it.endDate >= now)
                }
                val completedTasks = filteredTasks.filter { it.completed }

                when {
                    state.isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    filteredTasks.isEmpty() -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (searchQuery.isBlank()) {
                                Text(
                                    text = "Todavía no tenés tareas.",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Presioná + para crear la primera.")
                            } else {
                                Text(
                                    text = "No se encontraron tareas para \"$searchQuery\".",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (overdueTasks.isNotEmpty()) {
                                item(key = "header_overdue") {
                                    SectionHeader("Vencidas")
                                }
                                items(
                                    items = overdueTasks,
                                    key = { task -> task.id }
                                ) { task ->
                                    TaskItem(
                                        task = task,
                                        isOverdue = true,
                                        onToggle = { onToggleTask(task) },
                                        onDelete = { onDeleteTask(task) }
                                    )
                                }
                            }

                            if (pendingTasks.isNotEmpty()) {
                                item(key = "header_pending") {
                                    SectionHeader("Pendientes")
                                }
                                items(
                                    items = pendingTasks,
                                    key = { task -> task.id }
                                ) { task ->
                                    TaskItem(
                                        task = task,
                                        isOverdue = false,
                                        onToggle = { onToggleTask(task) },
                                        onDelete = { onDeleteTask(task) }
                                    )
                                }
                            }

                            if (completedTasks.isNotEmpty()) {
                                item(key = "header_completed") {
                                    SectionHeader("Completadas")
                                }
                                items(
                                    items = completedTasks,
                                    key = { task -> task.id }
                                ) { task ->
                                    TaskItem(
                                        task = task,
                                        isOverdue = false,
                                        onToggle = { onToggleTask(task) },
                                        onDelete = { onDeleteTask(task) }
                                    )
                                }
                            }
                        }
                    }
                }

                state.errorMessage?.let { message ->
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddTaskDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, description, startDate, endDate ->
                onAddTask(title, description, startDate, endDate)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun TaskItem(
    task: AcademicTask,
    isOverdue: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (task.completed) {
                    TextDecoration.LineThrough
                } else {
                    TextDecoration.None
                }
            )

            if (task.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (task.startDate != null || task.endDate != null) {
                Spacer(modifier = Modifier.height(4.dp))
                val datesText = listOfNotNull(
                    task.startDate?.let { "Inicio: ${formatDate(it)}" },
                    task.endDate?.let { "Fin: ${formatDate(it)}" }
                ).joinToString(" · ")
                Text(
                    text = datesText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDelete) {
                    Text("Eliminar")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(onClick = onToggle) {
                    Text(if (task.completed) "Pendiente" else "Completada")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long?, Long?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf<Long?>(null) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva tarea") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                DateField(
                    label = "Fecha de inicio",
                    dateMillis = startDate,
                    onPick = { showStartDatePicker = true },
                    onClear = { startDate = null }
                )

                Spacer(modifier = Modifier.height(8.dp))

                DateField(
                    label = "Fecha de fin",
                    dateMillis = endDate,
                    onPick = { showEndDatePicker = true },
                    onClear = { endDate = null }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title, description, startDate, endDate)
                    }
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )

    if (showStartDatePicker) {
        DatePickerModal(
            initialMillis = startDate,
            onDismiss = { showStartDatePicker = false },
            onConfirm = { millis ->
                startDate = millis
                showStartDatePicker = false
            }
        )
    }

    if (showEndDatePicker) {
        DatePickerModal(
            initialMillis = endDate,
            onDismiss = { showEndDatePicker = false },
            onConfirm = { millis ->
                endDate = millis
                showEndDatePicker = false
            }
        )
    }
}

@Composable
private fun DateField(
    label: String,
    dateMillis: Long?,
    onPick: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onPick,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = if (dateMillis != null) "$label: ${formatDate(dateMillis)}" else "$label (sin definir)"
            )
        }

        if (dateMillis != null) {
            TextButton(onClick = onClear) {
                Text("Quitar")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerModal(
    initialMillis: Long?,
    onDismiss: () -> Unit,
    onConfirm: (Long?) -> Unit
) {
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(datePickerState.selectedDateMillis) }) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
