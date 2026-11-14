package id.ac.ub.athra.ui.navigation

sealed class Routes(val route: String) {
    object Home        : Routes("home")
    object PreWorkout  : Routes("pre_workout")
    object WorkoutGraph : Routes("workout_graph")
    object LiveWorkout : Routes("live_workout")
    object Progress : Routes("progress")
    object History : Routes("history")
    object Profile : Routes("profile")
    object WorkoutDetail : Routes("workout_detail")
    object Preferences : Routes("preferences")
    object EditProfile : Routes("edit_profile")
}
