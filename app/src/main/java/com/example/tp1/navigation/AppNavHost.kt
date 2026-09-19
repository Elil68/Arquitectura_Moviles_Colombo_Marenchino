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
                onAddTask = { title, description, startDate, endDate ->
                    taskViewModel.addTask(title, description, startDate, endDate)
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
