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
