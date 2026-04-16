package com.ims.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ims.app.ui.IMSViewModel
import com.ims.app.ui.Screen
import com.ims.app.ui.screens.attendance.AdminAttendanceScreen
import com.ims.app.ui.screens.attendance.AttendanceScreen
import com.ims.app.ui.screens.attendance.MonthlyAttendanceScreen
import com.ims.app.ui.screens.auth.LoginScreen
import com.ims.app.ui.screens.dashboard.DashboardScreen
import com.ims.app.ui.screens.timetable.AdminTimetableScreen
import com.ims.app.ui.screens.timetable.TimetableScreen
import com.ims.app.ui.theme.IMSTheme

class MainActivity : ComponentActivity() {

    private val viewModel: IMSViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IMSTheme {
                IMSApp(viewModel)
            }
        }
    }
}

@Composable
fun IMSApp(viewModel: IMSViewModel) {
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route ?: Screen.Login.route

    // Central navigate helper — also used by bottom nav
    val navigate: (String) -> Unit = { route ->
        navController.navigate(route) {
            // Avoid building up a huge back stack for bottom nav tabs
            launchSingleTop = true
            restoreState    = true
        }
    }

    NavHost(
        navController    = navController,
        startDestination = Screen.Login.route
    ) {

        // ── Auth ─────────────────────────────────────────────────────────────
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel          = viewModel,
                onLoginSuccess     = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToSignUp = { }
            )
        }



        // ── Dashboard ─────────────────────────────────────────────────────────
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                viewModel    = viewModel,
                currentRoute = currentRoute,
                onNavigate   = navigate,
                onLogout     = {
                    viewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ── Attendance ────────────────────────────────────────────────────────
        composable(Screen.Attendance.route) {
            AttendanceScreen(
                viewModel    = viewModel,
                currentRoute = currentRoute,
                onNavigate   = navigate
            )
        }

        composable(Screen.AdminAttendance.route) {
            AdminAttendanceScreen(
                viewModel    = viewModel,
                currentRoute = currentRoute,
                onNavigate   = navigate
            )
        }

        composable(Screen.MonthlyAttendance.route) {
            MonthlyAttendanceScreen(
                viewModel    = viewModel,
                currentRoute = currentRoute,
                onNavigate   = navigate
            )
        }

        // ── Timetable ─────────────────────────────────────────────────────────
        composable(Screen.Timetable.route) {
            TimetableScreen(
                viewModel    = viewModel,
                currentRoute = currentRoute,
                onNavigate   = navigate
            )
        }

        composable(Screen.AdminTimetable.route) {
            AdminTimetableScreen(
                viewModel    = viewModel,
                currentRoute = currentRoute,
                onNavigate   = navigate
            )
        }
    }
}
