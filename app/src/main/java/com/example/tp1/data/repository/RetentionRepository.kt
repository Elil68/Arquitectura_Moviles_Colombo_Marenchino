package com.example.tp1.data.repository

import com.example.tp1.data.model.RetentionProgress
import com.example.tp1.util.StreakRules
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging

/**
 * Acceso a los datos de retención: progreso (racha y logros), tokens de
 * dispositivo para notificaciones push y suscripción a temas de campañas.
 */
class RetentionRepository {

    private val database = FirebaseFirestore.getInstance()

    private fun progressDoc(userId: String) =
        database.collection("users").document(userId)
            .collection("retention").document("progress")

    fun observeProgress(
        userId: String,
        onChange: (RetentionProgress) -> Unit
    ): ListenerRegistration {
        return progressDoc(userId).addSnapshotListener { snapshot, _ ->
            onChange(snapshot?.toObject(RetentionProgress::class.java) ?: RetentionProgress())
        }
    }

    /** Actualiza racha y logros en una transacción al completar una tarea. */
    fun recordCompletion(
        userId: String,
        onResult: (Result<RetentionProgress>) -> Unit
    ) {
        val ref = progressDoc(userId)
        database.runTransaction { transaction ->
            val current = transaction.get(ref).toObject(RetentionProgress::class.java)
                ?: RetentionProgress()
            val updated = StreakRules.onTaskCompleted(current, System.currentTimeMillis())
            transaction.set(ref, updated)
            updated
        }.addOnSuccessListener { onResult(Result.success(it)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    /** Marca la última apertura de la app (sirve para segmentar usuarios inactivos). */
    fun touchLastOpen(userId: String) {
        progressDoc(userId).set(
            mapOf("lastOpenAt" to FieldValue.serverTimestamp()),
            SetOptions.merge()
        )
    }

    /** Guarda el token FCM del dispositivo: users/{uid}/devices/{token}. */
    fun saveToken(userId: String, token: String) {
        database.collection("users").document(userId)
            .collection("devices").document(token)
            .set(
                mapOf(
                    "token" to token,
                    "platform" to "android",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
    }

    /** Obtiene el token actual, lo guarda y suscribe al dispositivo a los temas de campañas. */
    fun registerDevice(userId: String) {
        val messaging = FirebaseMessaging.getInstance()
        messaging.token.addOnSuccessListener { token -> saveToken(userId, token) }
        TOPICS.forEach { messaging.subscribeToTopic(it) }
    }

    companion object {
        const val TOPIC_REMINDERS = "recordatorios"
        const val TOPIC_NEWS = "novedades"
        val TOPICS = listOf(TOPIC_REMINDERS, TOPIC_NEWS)
    }
}
