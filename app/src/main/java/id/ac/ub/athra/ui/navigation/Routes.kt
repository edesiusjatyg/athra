package id.ac.ub.athra.ui.navigation

sealed class Routes(val route: String) {
    object Home        : Routes("home")
    object PreWorkout  : Routes("pre_workout")
    object WorkoutGraph : Routes("workout_graph")
    object LiveWorkout : Routes("live_workout")
    object QuickStart : Routes("quick_start")
    object ExerciseLibrary : Routes("exercise_library")
    object ExerciseFilters : Routes("exercise_filters")
    object ExerciseDetails : Routes("exercise_details/{exerciseId}") {
        fun create(exerciseId: String) = "exercise_details/$exerciseId"
    }
    object Summary : Routes("workout_summary")
    object Progress : Routes("progress")
    object History : Routes("history")
    object Profile : Routes("profile")
    object WorkoutDetail : Routes("workout_detail/{sessionId}") {
        fun create(sessionId: String) = "workout_detail/$sessionId"
    }
    object Preferences : Routes("preferences")
    object EditProfile : Routes("edit_profile")
}
