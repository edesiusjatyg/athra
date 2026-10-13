package com.example.athra.ui.navigation

sealed class Routes(val route: String) {
    object Home        : Routes("home")
    object PreWorkout  : Routes("pre_workout")
    object LiveWorkout : Routes("live_workout")
    // tambah screen baru di sini aja
}