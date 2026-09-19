package com.example.tp1.data.repository

import com.example.tp1.data.model.AcademicTask
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

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
                    .sortedWith(compareBy(nullsLast()) { task -> task.endDate })

                onChange(tasks)
            }
    }

    fun addTask(
        userId: String,
        title: String,
        description: String,
        startDate: Long?,
        endDate: Long?,
        onResult: (Result<Unit>) -> Unit
    ) {
        val taskData = hashMapOf(
            "title" to title,
            "description" to description,
            "completed" to false,
            "startDate" to startDate,
            "endDate" to endDate,
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
