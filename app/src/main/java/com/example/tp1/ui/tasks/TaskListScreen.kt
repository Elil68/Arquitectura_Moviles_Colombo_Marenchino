package com.example.tp1.ui.tasks

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.tp1.data.model.AcademicTask
import com.example.tp1.viewmodel.TaskUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

private fun formatDate(millis: Long): String = dateFormatter.format(Date(millis))

private val ALL_SECTIONS = setOf("overdue", "pending", "completed")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    state: TaskUiState,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onAddTask: (String, String, Long?, Long?) -> Unit,
    onToggleTask: (AcademicTask) -> Unit,
    onDeleteTask: (AcademicTask) -> Unit,
    onReorderTasks: (List<String>) -> Unit,
    onLogout: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedSections by remember { mutableStateOf(ALL_SECTIONS) }
    var onlyWithoutDate by remember { mutableStateOf(false) }
    var rangeStart by remember { mutableStateOf<Long?>(null) }
    var rangeEnd by remember { mutableStateOf<Long?>(null) }
    var showRangeStartPicker by remember { mutableStateOf(false) }
    var showRangeEndPicker by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onStartListening()
        onDispose { onStopListening() }
    }

    val now = System.currentTimeMillis()
    val searched = state.tasks.filter { it.title.contains(searchQuery, ignoreCase = true) }
    val dateFiltered = when {
        onlyWithoutDate -> searched.filter { it.startDate == null && it.endDate == null }
        rangeStart != null || rangeEnd != null -> {
            val start = rangeStart ?: Long.MIN_VALUE
            val end = rangeEnd ?: Long.MAX_VALUE
            searched.filter { task ->
                val startInRange = task.startDate != null && task.startDate in start..end
                val endInRange = task.endDate != null && task.endDate in start..end
                startInRange || endInRange
            }
        }
        else -> searched
    }

    val overdueTasks = if ("overdue" in selectedSections) {
        dateFiltered.filter { !it.completed && it.endDate != null && it.endDate < now }
    } else {
        emptyList()
    }
    val pendingTasks = if ("pending" in selectedSections) {
        dateFiltered.filter { !it.completed && (it.endDate == null || it.endDate >= now) }
    } else {
        emptyList()
    }
    val completedTasks = if ("completed" in selectedSections) {
        dateFiltered.filter { it.completed }
    } else {
        emptyList()
    }

    val pendingDated = pendingTasks.filter { it.endDate != null }
    val pendingUndated = pendingTasks.filter { it.endDate == null }
    val completedDated = completedTasks.filter { it.endDate != null }
    val completedUndated = completedTasks.filter { it.endDate == null }

    val hasAnyResult = overdueTasks.isNotEmpty() || pendingTasks.isNotEmpty() || completedTasks.isNotEmpty()
    val hasActiveFilters = searchQuery.isNotBlank() || onlyWithoutDate ||
        rangeStart != null || rangeEnd != null || selectedSections != ALL_SECTIONS

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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = "overdue" in selectedSections,
                    onClick = {
                        selectedSections = if ("overdue" in selectedSections) {
                            selectedSections - "overdue"
                        } else {
                            selectedSections + "overdue"
                        }
                    },
                    label = { Text("Vencidas") }
                )
                FilterChip(
                    selected = "pending" in selectedSections,
                    onClick = {
                        selectedSections = if ("pending" in selectedSections) {
                            selectedSections - "pending"
                        } else {
                            selectedSections + "pending"
                        }
                    },
                    label = { Text("Pendientes") }
                )
                FilterChip(
                    selected = "completed" in selectedSections,
                    onClick = {
                        selectedSections = if ("completed" in selectedSections) {
                            selectedSections - "completed"
                        } else {
                            selectedSections + "completed"
                        }
                    },
                    label = { Text("Completadas") }
                )
                FilterChip(
                    selected = onlyWithoutDate,
                    onClick = {
                        onlyWithoutDate = !onlyWithoutDate
                        if (onlyWithoutDate) {
                            rangeStart = null
                            rangeEnd = null
                        }
                    },
                    label = { Text("Sin fecha") }
                )

                if (hasActiveFilters) {
                    TextButton(
                        onClick = {
                            searchQuery = ""
                            selectedSections = ALL_SECTIONS
                            onlyWithoutDate = false
                            rangeStart = null
                            rangeEnd = null
                        }
                    ) {
                        Text("Limpiar filtros")
                    }
                }
            }

            if (!onlyWithoutDate) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    DateField(
                        label = "Desde",
                        dateMillis = rangeStart,
                        onPick = { showRangeStartPicker = true },
                        onClear = { rangeStart = null }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    DateField(
                        label = "Hasta",
                        dateMillis = rangeEnd,
                        onPick = { showRangeEndPicker = true },
                        onClear = { rangeEnd = null }
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when {
                    state.isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    !hasAnyResult -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (!hasActiveFilters) {
                                Text(
                                    text = "Todavía no tenés tareas.",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Presioná + para crear la primera.")
                            } else {
                                Text(
                                    text = "No se encontraron tareas con los filtros aplicados.",
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
                                    items = pendingDated,
                                    key = { task -> task.id }
                                ) { task ->
                                    TaskItem(
                                        task = task,
                                        isOverdue = false,
                                        onToggle = { onToggleTask(task) },
                                        onDelete = { onDeleteTask(task) }
                                    )
                                }
                                if (pendingUndated.isNotEmpty()) {
                                    item(key = "undated_pending") {
                                        UndatedTasksHint()
                                        ReorderableUndatedTasks(
                                            tasks = pendingUndated,
                                            onToggle = onToggleTask,
                                            onDelete = onDeleteTask,
                                            onReorder = onReorderTasks
                                        )
                                    }
                                }
                            }

                            if (completedTasks.isNotEmpty()) {
                                item(key = "header_completed") {
                                    SectionHeader("Completadas")
                                }
                                items(
                                    items = completedDated,
                                    key = { task -> task.id }
                                ) { task ->
                                    TaskItem(
                                        task = task,
                                        isOverdue = false,
                                        onToggle = { onToggleTask(task) },
                                        onDelete = { onDeleteTask(task) }
                                    )
                                }
                                if (completedUndated.isNotEmpty()) {
                                    item(key = "undated_completed") {
                                        UndatedTasksHint()
                                        ReorderableUndatedTasks(
                                            tasks = completedUndated,
                                            onToggle = onToggleTask,
                                            onDelete = onDeleteTask,
                                            onReorder = onReorderTasks
                                        )
                                    }
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

    if (showRangeStartPicker) {
        DatePickerModal(
            initialMillis = rangeStart,
            onDismiss = { showRangeStartPicker = false },
            onConfirm = { millis ->
                rangeStart = millis
                showRangeStartPicker = false
            }
        )
    }

    if (showRangeEndPicker) {
        DatePickerModal(
            initialMillis = rangeEnd,
            onDismiss = { showRangeEndPicker = false },
            onConfirm = { millis ->
                rangeEnd = millis
                showRangeEndPicker = false
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
private fun UndatedTasksHint() {
    Text(
        text = "Sin fecha — mantené presionada una tarea y arrastrá para ordenarlas.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

/**
 * Lista arrastrable (long-press + drag) para reordenar a mano las tareas
 * que no tienen fecha de fin. Las que sí tienen fecha se ordenan solas
 * por vencimiento y no pasan por acá.
 */
@Composable
private fun ReorderableUndatedTasks(
    tasks: List<AcademicTask>,
    onToggle: (AcademicTask) -> Unit,
    onDelete: (AcademicTask) -> Unit,
    onReorder: (List<String>) -> Unit
) {
    var items by remember(tasks.map { it.id }) { mutableStateOf(tasks) }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { task ->
            // key() ancla el estado (posición, altura medida, gesto de arrastre) a la
            // identidad de la tarea, no a la posición del forEach. Sin esto, al
            // reordenar en vivo durante el drag, Compose reasigna cada slot a otra
            // tarea y reinicia el pointerInput (que usa task.id como key) a mitad de
            // gesto, cancelando el drag antes de llegar a onDragEnd.
            key(task.id) {
                var itemHeightPx by remember { mutableStateOf(0f) }
                val isDragging = draggingId == task.id

                TaskItem(
                    task = task,
                    isOverdue = false,
                    onToggle = { onToggle(task) },
                    onDelete = { onDelete(task) },
                    modifier = Modifier
                        .onGloballyPositioned { coordinates ->
                            itemHeightPx = coordinates.size.height.toFloat()
                        }
                        .graphicsLayer {
                            translationY = if (isDragging) dragOffset else 0f
                            shadowElevation = if (isDragging) 8f else 0f
                        }
                        .zIndex(if (isDragging) 1f else 0f)
                        .pointerInput(task.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggingId = task.id
                                    dragOffset = 0f
                                },
                                onDragEnd = {
                                    draggingId = null
                                    dragOffset = 0f
                                    onReorder(items.map { it.id })
                                },
                                onDragCancel = {
                                    draggingId = null
                                    dragOffset = 0f
                                },
                                onDrag = { change, delta ->
                                    change.consume()
                                    dragOffset += delta.y

                                    val height = itemHeightPx
                                    if (height <= 0f) return@detectDragGesturesAfterLongPress

                                    val currentIndex = items.indexOfFirst { it.id == task.id }
                                    if (currentIndex == -1) return@detectDragGesturesAfterLongPress

                                    if (dragOffset > height / 2 && currentIndex < items.lastIndex) {
                                        items = items.toMutableList().apply {
                                            add(currentIndex + 1, removeAt(currentIndex))
                                        }
                                        dragOffset -= height
                                    } else if (dragOffset < -height / 2 && currentIndex > 0) {
                                        items = items.toMutableList().apply {
                                            add(currentIndex - 1, removeAt(currentIndex))
                                        }
                                        dragOffset += height
                                    }
                                }
                            )
                        }
                )
            }
        }
    }
}

@Composable
private fun TaskItem(
    task: AcademicTask,
    isOverdue: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
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
