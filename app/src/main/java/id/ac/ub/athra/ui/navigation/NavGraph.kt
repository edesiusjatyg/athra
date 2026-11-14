package id.ac.ub.athra.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
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
import id.ac.ub.athra.ui.screens.ProgressScreen
import id.ac.ub.athra.ui.screens.HistoryScreen
import id.ac.ub.athra.ui.screens.WorkoutDetailScreen
import id.ac.ub.athra.ui.screens.PreferencesScreen
import id.ac.ub.athra.ui.screens.AthraBottomNav
import id.ac.ub.athra.ui.theme.Background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import id.ac.ub.athra.domain.model.CompletedWorkout
import id.ac.ub.athra.domain.model.WeightUnit
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AthraNavGraph(
    navController: NavHostController,
    userName: String,
    workoutState: WorkoutUiState,
    workoutViewModel: WorkoutViewModel,
    onStartWorkout: suspend () -> String?,
    onResumeWorkout: () -> Unit,
    onSignOut: () -> Unit,
    userEmail: String,
    weightUnit: WeightUnit,
    onSaveUnit: suspend (WeightUnit) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var periodDays by rememberSaveable { mutableIntStateOf(7) }
    var selectedWorkout by remember { mutableStateOf<CompletedWorkout?>(null) }
    NavHost(
        navController = navController,
        startDestination = Routes.Home.route,
    ) {
        composable(Routes.Home.route) {
            HomeScreen(
                userName = userName,
                draft = workoutState.draft,
                recentSessions = workoutState.recentSessions,
                analytics = workoutState.homeAnalytics,
                onStartWorkout = { navController.navigate(Routes.PreWorkout.route) },
                onResumeWorkout = {
                    onResumeWorkout()
                    workoutState.draft?.let { navController.navigate(Routes.LiveWorkout.route) }
                },
                onViewAll = { navController.navigate(Routes.History.route) },
                onNavigate = { tab ->
                    when (tab) {
                        0 -> navController.navigate(Routes.Home.route) { popUpTo(Routes.Home.route) { inclusive = false }; launchSingleTop = true }
                        1 -> navController.navigate(Routes.PreWorkout.route) { launchSingleTop = true }
                        2 -> navController.navigate(Routes.Progress.route) { launchSingleTop = true }
                        3 -> navController.navigate(Routes.Profile.route) { launchSingleTop = true }
                    }
                },
            )
            // History is opened from the Home section action and shares the local completed-session stream.
            //
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
                weightUnit = weightUnit,
            )
        }
        composable(Routes.Progress.route) {
            LaunchedEffect(Unit) { workoutViewModel.refreshAnalytics(periodDays) }
            ProgressScreen(
                analytics = workoutState.analytics,
                periodDays = periodDays,
                onPeriodChange = { days -> periodDays = days; workoutViewModel.refreshAnalytics(days) },
                onNavigate = { tab ->
                    when (tab) {
                        0 -> navController.navigate(Routes.Home.route) { popUpTo(Routes.Home.route) { inclusive = false }; launchSingleTop = true }
                        1 -> navController.navigate(Routes.PreWorkout.route) { launchSingleTop = true }
                        2 -> Unit
                        3 -> navController.navigate(Routes.Profile.route) { launchSingleTop = true }
                    }
                },
            )
        }
        composable(Routes.History.route) {
            HistoryScreen(
                sessions = workoutState.recentSessions,
                onOpen = { sessionId ->
                    scope.launch {
                        selectedWorkout = workoutViewModel.loadWorkoutDetail(sessionId)
                        navController.navigate(Routes.WorkoutDetail.route)
                    }
                },
                onNavigate = { tab ->
                    when (tab) {
                        0 -> navController.navigate(Routes.Home.route) { launchSingleTop = true }
                        1 -> navController.navigate(Routes.PreWorkout.route) { launchSingleTop = true }
                        2 -> navController.navigate(Routes.Progress.route) { launchSingleTop = true }
                        3 -> navController.navigate(Routes.Profile.route) { launchSingleTop = true }
                    }
                },
            )
        }
        composable(Routes.Profile.route) {
            androidx.compose.material3.Scaffold(
                containerColor = Background,
                bottomBar = {
                    AthraBottomNav(selected = 3) { tab ->
                        when (tab) {
                            0 -> navController.navigate(Routes.Home.route) { launchSingleTop = true }
                            1 -> navController.navigate(Routes.PreWorkout.route) { launchSingleTop = true }
                            2 -> navController.navigate(Routes.Progress.route) { launchSingleTop = true }
                            3 -> Unit
                        }
                    }
                },
                topBar = { TopAppBar(title = { Text("Profil") }) },
            ) { insets ->
                Column(Modifier.padding(insets).padding(20.dp)) {
                    Text(userName, style = MaterialTheme.typography.headlineMedium)
                    Text(userEmail, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Data latihan tersimpan di perangkat ini.")
                    TextButton(onClick = { navController.navigate(Routes.Preferences.route) }) { Text("Preferensi satuan") }
                    TextButton(onClick = onSignOut) { Text("Keluar") }
                }
            }
        }
        composable(Routes.WorkoutDetail.route) {
            WorkoutDetailScreen(selectedWorkout, weightUnit) { navController.popBackStack() }
        }
        composable(Routes.Preferences.route) {
            PreferencesScreen(
                initialUnit = weightUnit,
                onBack = { navController.popBackStack() },
                onSave = { unit -> scope.launch { onSaveUnit(unit); navController.popBackStack() } },
            )
        }
    }
}
