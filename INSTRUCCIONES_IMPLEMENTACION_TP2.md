# Implementación del TP2 — Mis Tareas Académicas

## Objetivo de esta instrucción

Implementar sobre la rama `tp2-mvp` una aplicación Android nativa desarrollada con Kotlin y Jetpack Compose. La aplicación debe ampliar el TP1 y funcionar como un MVP para gestionar tareas académicas.

No modificar ni eliminar la rama utilizada para entregar el TP1. Antes de editar, verificar que la rama activa sea `tp2-mvp`.

El proyecto existente utiliza el paquete:

```text
com.example.tp1
```

Debe conservarse el tema existente `TP1Theme` y el código que ya se encuentre dentro de `ui.theme`.

## Funcionalidades obligatorias

1. Registrar un usuario con correo electrónico y contraseña.
2. Iniciar sesión.
3. Mantener la sesión iniciada al volver a abrir la aplicación.
4. Mostrar las tareas pertenecientes al usuario autenticado.
5. Crear una tarea con título y descripción.
6. Marcar una tarea como completada o pendiente.
7. Eliminar una tarea.
8. Cerrar sesión.
9. Guardar los datos en Cloud Firestore.
10. Impedir que un usuario acceda a las tareas de otro usuario.

## Arquitectura requerida

Utilizar una arquitectura MVVM con Repository y una única Activity:

```text
Interfaz Jetpack Compose
        ↓
ViewModels
        ↓
Repositories
        ↓
Firebase Authentication / Cloud Firestore
```

## Estructura de archivos

Crear la siguiente estructura dentro de `app/src/main/java/com/example/tp1`:

```text
com.example.tp1
├── MainActivity.kt
├── data
│   ├── model
│   │   └── AcademicTask.kt
│   └── repository
│       ├── AuthRepository.kt
│       └── TaskRepository.kt
├── navigation
│   └── AppNavHost.kt
├── ui
│   ├── auth
│   │   ├── LoginScreen.kt
│   │   └── RegisterScreen.kt
│   └── tasks
│       └── TaskListScreen.kt
└── viewmodel
    ├── AuthViewModel.kt
    └── TaskViewModel.kt
```

No colocar todo el código dentro de `MainActivity.kt`. Cada bloque siguiente corresponde a un archivo distinto.

---

## 1. Configuración Gradle del proyecto

En el archivo `build.gradle.kts` ubicado en la raíz del proyecto, agregar dentro del bloque `plugins`:

```kotlin
id("com.google.gms.google-services") version "4.5.0" apply false
```

No reemplazar las líneas existentes. El resultado será similar a:

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("com.google.gms.google-services") version "4.5.0" apply false
}
```

## 2. Configuración Gradle del módulo app

En `app/build.gradle.kts`, agregar dentro del bloque `plugins`:

```kotlin
id("com.google.gms.google-services")
```

Agregar dentro del bloque `dependencies`:

```kotlin
implementation("androidx.navigation:navigation-compose:2.10.1")
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")

implementation(platform("com.google.firebase:firebase-bom:34.19.0"))
implementation("com.google.firebase:firebase-auth")
implementation("com.google.firebase:firebase-firestore")
```

No utilizar `firebase-auth-ktx` ni `firebase-firestore-ktx`.

Si el proyecto ya contiene alguna de estas dependencias, no duplicarla. Sincronizar Gradle después de realizar los cambios.

## 3. AndroidManifest.xml

En `app/src/main/AndroidManifest.xml`, agregar el permiso de Internet antes de `<application>`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

La aplicación utilizará únicamente `MainActivity`. El Manifest debe conservar el `intent-filter` de inicio:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.TP1">

        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

    </application>

</manifest>
```

Si el Manifest real contiene atributos adicionales generados por Android Studio, conservarlos. Solamente retirar el registro de `SecondActivity` porque la navegación del TP2 se realizará con Compose.

---

## 4. Modelo de datos

Crear `app/src/main/java/com/example/tp1/data/model/AcademicTask.kt`:

```kotlin
package com.example.tp1.data.model

data class AcademicTask(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val completed: Boolean = false
)
```

---

## 5. Repositorio de autenticación

Crear `app/src/main/java/com/example/tp1/data/repository/AuthRepository.kt`:

```kotlin
package com.example.tp1.data.repository

import com.google.firebase.auth.FirebaseAuth

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()

    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    fun currentUserId(): String? {
        return auth.currentUser?.uid
    }

    fun login(
        email: String,
        password: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                onResult(Result.success(Unit))
            }
            .addOnFailureListener { exception ->
                onResult(Result.failure(exception))
            }
    }

    fun register(
        email: String,
        password: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                onResult(Result.success(Unit))
            }
            .addOnFailureListener { exception ->
                onResult(Result.failure(exception))
            }
    }

    fun logout() {
        auth.signOut()
    }
}
```

---

## 6. Repositorio de tareas

Crear `app/src/main/java/com/example/tp1/data/repository/TaskRepository.kt`:

```kotlin
package com.example.tp1.data.repository

import com.example.tp1.data.model.AcademicTask
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class TaskRepository {

    private val database = FirebaseFirestore.getInstance()

    private fun tasksCollection(userId: String) =
        database.collection("users")
            .document(userId)
            .collection("tasks")

    fun observeTasks(
        userId: String,
        onChange: (List<AcademicTask>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return tasksCollection(userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    onError(exception)
                    return@addSnapshotListener
                }

                val tasks = snapshot?.documents
                    ?.mapNotNull { document ->
                        document.toObject(AcademicTask::class.java)
                            ?.copy(id = document.id)
                    }
                    .orEmpty()

                onChange(tasks)
            }
    }

    fun addTask(
        userId: String,
        title: String,
        description: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        val taskData = hashMapOf(
            "title" to title,
            "description" to description,
            "completed" to false,
            "createdAt" to FieldValue.serverTimestamp()
        )

        tasksCollection(userId)
            .add(taskData)
            .addOnSuccessListener {
                onResult(Result.success(Unit))
            }
            .addOnFailureListener { exception ->
                onResult(Result.failure(exception))
            }
    }

    fun setTaskCompleted(
        userId: String,
        taskId: String,
        completed: Boolean,
        onResult: (Result<Unit>) -> Unit
    ) {
        tasksCollection(userId)
            .document(taskId)
            .update("completed", completed)
            .addOnSuccessListener {
                onResult(Result.success(Unit))
            }
            .addOnFailureListener { exception ->
                onResult(Result.failure(exception))
            }
    }

    fun deleteTask(
        userId: String,
        taskId: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        tasksCollection(userId)
            .document(taskId)
            .delete()
            .addOnSuccessListener {
                onResult(Result.success(Unit))
            }
            .addOnFailureListener { exception ->
                onResult(Result.failure(exception))
            }
    }
}
```

---

## 7. ViewModel de autenticación

Crear `app/src/main/java/com/example/tp1/viewmodel/AuthViewModel.kt`:

```kotlin
package com.example.tp1.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.tp1.data.repository.AuthRepository

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()

    var uiState by mutableStateOf(AuthUiState())
        private set

    fun isUserLoggedIn(): Boolean {
        return repository.isUserLoggedIn()
    }

    fun login(
        email: String,
        password: String,
        onSuccess: () -> Unit
    ) {
        if (email.isBlank() || password.isBlank()) {
            uiState = AuthUiState(
                errorMessage = "Completá el correo y la contraseña."
            )
            return
        }

        uiState = AuthUiState(isLoading = true)

        repository.login(email.trim(), password) { result ->
            result.onSuccess {
                uiState = AuthUiState()
                onSuccess()
            }.onFailure { exception ->
                uiState = AuthUiState(
                    errorMessage = exception.localizedMessage
                        ?: "No se pudo iniciar sesión."
                )
            }
        }
    }

    fun register(
        email: String,
        password: String,
        repeatedPassword: String,
        onSuccess: () -> Unit
    ) {
        if (email.isBlank() || password.isBlank()) {
            uiState = AuthUiState(
                errorMessage = "Completá todos los campos."
            )
            return
        }

        if (password.length < 6) {
            uiState = AuthUiState(
                errorMessage = "La contraseña debe tener al menos 6 caracteres."
            )
            return
        }

        if (password != repeatedPassword) {
            uiState = AuthUiState(
                errorMessage = "Las contraseñas no coinciden."
            )
            return
        }

        uiState = AuthUiState(isLoading = true)

        repository.register(email.trim(), password) { result ->
            result.onSuccess {
                uiState = AuthUiState()
                onSuccess()
            }.onFailure { exception ->
                uiState = AuthUiState(
                    errorMessage = exception.localizedMessage
                        ?: "No se pudo crear la cuenta."
                )
            }
        }
    }

    fun logout() {
        repository.logout()
        uiState = AuthUiState()
    }

    fun clearError() {
        uiState = uiState.copy(errorMessage = null)
    }
}
```

---

## 8. ViewModel de tareas

Crear `app/src/main/java/com/example/tp1/viewmodel/TaskViewModel.kt`:

```kotlin
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

    fun addTask(title: String, description: String) {
        val userId = authRepository.currentUserId() ?: return

        if (title.isBlank()) {
            uiState = uiState.copy(
                errorMessage = "La tarea debe tener un título."
            )
            return
        }

        taskRepository.addTask(
            userId = userId,
            title = title.trim(),
            description = description.trim()
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
```

---

## 9. Pantalla de inicio de sesión

Crear `app/src/main/java/com/example/tp1/ui/auth/LoginScreen.kt`:

```kotlin
package com.example.tp1.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.tp1.viewmodel.AuthUiState

@Composable
fun LoginScreen(
    state: AuthUiState,
    onLogin: (String, String) -> Unit,
    onGoToRegister: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Mis Tareas Académicas",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Iniciar sesión")
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo electrónico") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        state.errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = message, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { onLogin(email, password) },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Ingresar")
            }
        }

        TextButton(
            onClick = onGoToRegister,
            enabled = !state.isLoading
        ) {
            Text("No tengo cuenta — Registrarme")
        }
    }
}
```

---

## 10. Pantalla de registro

Crear `app/src/main/java/com/example/tp1/ui/auth/RegisterScreen.kt`:

```kotlin
package com.example.tp1.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.tp1.viewmodel.AuthUiState

@Composable
fun RegisterScreen(
    state: AuthUiState,
    onRegister: (String, String, String) -> Unit,
    onBack: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var repeatedPassword by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Crear cuenta",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo electrónico") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = repeatedPassword,
            onValueChange = { repeatedPassword = it },
            label = { Text("Repetir contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        state.errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = message, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { onRegister(email, password, repeatedPassword) },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Crear cuenta")
            }
        }

        TextButton(
            onClick = onBack,
            enabled = !state.isLoading
        ) {
            Text("Volver al inicio de sesión")
        }
    }
}
```

---

## 11. Pantalla de tareas

Crear `app/src/main/java/com/example/tp1/ui/tasks/TaskListScreen.kt`:

```kotlin
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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    state: TaskUiState,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onAddTask: (String, String) -> Unit,
    onToggleTask: (AcademicTask) -> Unit,
    onDeleteTask: (AcademicTask) -> Unit,
    onLogout: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                state.tasks.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Todavía no tenés tareas.",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Presioná + para crear la primera.")
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = state.tasks,
                            key = { task -> task.id }
                        ) { task ->
                            TaskItem(
                                task = task,
                                onToggle = { onToggleTask(task) },
                                onDelete = { onDeleteTask(task) }
                            )
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

    if (showAddDialog) {
        AddTaskDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, description ->
                onAddTask(title, description)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun TaskItem(
    task: AcademicTask,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.completed,
                onCheckedChange = { onToggle() }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
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
            }

            TextButton(onClick = onDelete) {
                Text("Eliminar")
            }
        }
    }
}

@Composable
private fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title, description)
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
}
```

---

## 12. Navegación

Crear `app/src/main/java/com/example/tp1/navigation/AppNavHost.kt`:

```kotlin
package com.example.tp1.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.tp1.ui.auth.LoginScreen
import com.example.tp1.ui.auth.RegisterScreen
import com.example.tp1.ui.tasks.TaskListScreen
import com.example.tp1.viewmodel.AuthViewModel
import com.example.tp1.viewmodel.TaskViewModel

private object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val TASKS = "tasks"
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val taskViewModel: TaskViewModel = viewModel()

    val startDestination = if (authViewModel.isUserLoggedIn()) {
        Routes.TASKS
    } else {
        Routes.LOGIN
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                state = authViewModel.uiState,
                onLogin = { email, password ->
                    authViewModel.login(email, password) {
                        navController.navigate(Routes.TASKS) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                },
                onGoToRegister = {
                    authViewModel.clearError()
                    navController.navigate(Routes.REGISTER)
                }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                state = authViewModel.uiState,
                onRegister = { email, password, repeatedPassword ->
                    authViewModel.register(
                        email = email,
                        password = password,
                        repeatedPassword = repeatedPassword
                    ) {
                        navController.navigate(Routes.TASKS) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                },
                onBack = {
                    authViewModel.clearError()
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.TASKS) {
            TaskListScreen(
                state = taskViewModel.uiState,
                onStartListening = { taskViewModel.startListening() },
                onStopListening = { taskViewModel.stopListening() },
                onAddTask = { title, description ->
                    taskViewModel.addTask(title, description)
                },
                onToggleTask = { task ->
                    taskViewModel.toggleTask(task)
                },
                onDeleteTask = { task ->
                    taskViewModel.deleteTask(task)
                },
                onLogout = {
                    taskViewModel.stopListening()
                    authViewModel.logout()

                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.TASKS) { inclusive = true }
                    }
                }
            )
        }
    }
}
```

---

## 13. MainActivity

Reemplazar el contenido de `app/src/main/java/com/example/tp1/MainActivity.kt` por:

```kotlin
package com.example.tp1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.tp1.navigation.AppNavHost
import com.example.tp1.ui.theme.TP1Theme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TP1Theme {
                AppNavHost()
            }
        }
    }
}
```

---

## 14. Configuración externa de Firebase

Esta parte puede ser realizada por otro integrante, pero debe quedar terminada antes de probar el funcionamiento completo.

1. Registrar una aplicación Android en Firebase usando exactamente el package `com.example.tp1`.
2. Descargar `google-services.json`.
3. Colocar el archivo en `app/google-services.json`.
4. En Firebase Authentication, habilitar el proveedor `Email/Password`.
5. Crear una base de datos Cloud Firestore.
6. Publicar las siguientes reglas de seguridad:

```javascript
rules_version = '2';

service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId}/tasks/{taskId} {
      allow read, create, update, delete:
        if request.auth != null
        && request.auth.uid == userId;
    }
  }
}
```

No subir claves privadas, archivos de cuentas de servicio ni contraseñas al repositorio. El archivo Android `google-services.json` contiene la configuración del cliente y debe corresponder exactamente con la aplicación registrada.

---

## 15. Verificación final

Una vez implementado todo:

1. Sincronizar el proyecto con Gradle.
2. Corregir solamente errores relacionados con la integración realizada, sin reestructurar innecesariamente el proyecto.
3. Compilar la aplicación.
4. Ejecutarla en el emulador.
5. Crear una cuenta con correo y contraseña.
6. Crear dos tareas.
7. Marcar una como completada.
8. Eliminar una tarea.
9. Cerrar y abrir nuevamente la aplicación y comprobar que la sesión y los datos persisten.
10. Cerrar sesión y comprobar que vuelve al inicio de sesión.
11. Tomar capturas de las pantallas de registro, inicio de sesión, listado y creación de tareas para el informe.

## Criterios de aceptación

La implementación está terminada cuando:

- El proyecto compila sin errores.
- El usuario puede registrarse e iniciar sesión.
- Las tareas se guardan en Firestore.
- Cada usuario ve únicamente sus propias tareas.
- Se puede crear, completar y eliminar una tarea.
- La sesión se conserva al reiniciar la aplicación.
- El botón `Salir` cierra la sesión.
- La arquitectura y la estructura de carpetas coinciden con este documento.

Al finalizar, informar qué archivos fueron creados o modificados y cualquier ajuste necesario debido a diferencias en la versión o configuración previa del proyecto.
