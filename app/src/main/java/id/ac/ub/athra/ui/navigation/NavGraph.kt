package id.ac.ub.athra.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavHostController
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import id.ac.ub.athra.ui.workout.WorkoutUiState
import id.ac.ub.athra.ui.screens.HomeScreen
import id.ac.ub.athra.ui.screens.PreWorkoutScreen
import id.ac.ub.athra.ui.workout.WorkoutViewModel
import id.ac.ub.athra.ui.workout.WorkoutFlowScreen

@Composable
fun AthraNavGraph(
    navController: NavHostController,
    userName: String,
    workoutState: WorkoutUiState,
    workoutViewModel: WorkoutViewModel,
    onStartWorkout: suspend () -> String?,
    onResumeWorkout: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    NavHost(
        navController = navController,
        startDestination = Routes.Home.route,
    ) {
        composable(Routes.Home.route) {
            HomeScreen(
                userName = userName,
                draft = workoutState.draft,
                recentSessions = workoutState.recentSessions,
                analytics = workoutState.analytics,
                onStartWorkout = { navController.navigate(Routes.PreWorkout.route) },
                onResumeWorkout = {
                    onResumeWorkout()
                    workoutState.draft?.let { navController.navigate(Routes.LiveWorkout.route) }
                },
                onViewAll = { },
            )
        }
        composable(Routes.PreWorkout.route) {
            PreWorkoutScreen(
                onBack = { navController.popBackStack() },
                onBeginSession = {
                    scope.launch {
                        if (onStartWorkout() != null) navController.navigate(Routes.LiveWorkout.route)
                    }
                },
                onQuickStart = {
                    scope.launch {
                        if (onStartWorkout() != null) navController.navigate(Routes.LiveWorkout.route)
                    }
                },
            )
        }
        composable(Routes.LiveWorkout.route) {
            val draft = workoutState.draft
            if (draft == null) {
                androidx.compose.material3.CircularProgressIndicator()
            } else WorkoutFlowScreen(
                draft = draft,
                state = workoutState,
                onSaveSet = workoutViewModel::saveSet,
                onDeleteSet = workoutViewModel::deleteSet,
                onAddExercise = workoutViewModel::addExercise,
                onFinish = workoutViewModel::finish,
                onResume = workoutViewModel::resume,
                onSave = workoutViewModel::save,
                onDiscard = workoutViewModel::discard,
                onExit = { navController.popBackStack(Routes.Home.route, false) },
            )
        }
    }
}
