package com.example.myapplication.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapplication.data.remote.Session
import com.example.myapplication.ui.screens.MainScreen
import com.example.myapplication.ui.screens.auth.LoginScreen
import com.example.myapplication.ui.screens.student.GradesScreen
import com.example.myapplication.ui.screens.student.ScheduleScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Main : Screen("main")
    object Grades : Screen("grades")
    object Schedule : Screen("schedule")
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    // NAVEGACIÓN SEGURA
    // 1) Si hay una sesión vigente (token sin vencer) se entra directo a la app; si no, al login.
    val inicio = remember {
        if (Session.vigente()) "${Screen.Main.route}/${Session.rol}" else Screen.Login.route
    }

    // 2) Si la sesión se cierra (logout, token vencido o rechazado por el servidor) se vuelve al login
    //    y se borra todo el historial de pantallas, para que "atrás" no deje volver.
    val sesionActiva = Session.activa
    LaunchedEffect(sesionActiva) {
        val rutaActual = navController.currentDestination?.route
        if (!sesionActiva && rutaActual != null && rutaActual != Screen.Login.route) {
            navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
        }
    }

    NavHost(
        navController = navController,
        startDestination = inicio
    ) {
        // 1. Login (pública)
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { role ->
                    navController.navigate("${Screen.Main.route}/$role") {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // 2. Pantalla principal: el rol sale SIEMPRE de la sesión (token), no del parámetro de la ruta
        composable(
            route = "${Screen.Main.route}/{role}",
            arguments = listOf(navArgument("role") { type = NavType.StringType })
        ) {
            RutaProtegida(navController, roles = setOf("ALUMNO", "DOCENTE", "JEFATURA")) {
                MainScreen(
                    userRole = Session.rol ?: "ALUMNO",
                    onLogout = { Session.cerrar() }
                )
            }
        }

        // 3. Vistas adicionales del estudiante
        composable(Screen.Grades.route) {
            RutaProtegida(navController, roles = setOf("ALUMNO")) {
                GradesScreen(onBack = { navController.popBackStack() })
            }
        }

        composable(Screen.Schedule.route) {
            RutaProtegida(navController, roles = setOf("ALUMNO")) {
                ScheduleScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

/**
 * Muestra la pantalla solo si hay sesión activa y el rol está permitido.
 * Si no, cierra la sesión y manda al login.
 */
@Composable
private fun RutaProtegida(
    navController: NavHostController,
    roles: Set<String>,
    contenido: @Composable () -> Unit
) {
    val permitido = Session.vigente() && Session.rol in roles
    if (permitido) {
        contenido()
    } else {
        LaunchedEffect(Unit) {
            Session.cerrar()
            navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
        }
    }
}
