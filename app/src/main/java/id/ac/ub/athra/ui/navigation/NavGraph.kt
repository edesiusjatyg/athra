package id.ac.ub.athra.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
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
import id.ac.ub.athra.ui.screens.EditProfileScreen
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
    fun openWorkoutDetail(sessionId: String) {
        scope.launch {
            selectedWorkout = workoutViewModel.loadWorkoutDetail(sessionId)
            navController.navigate(Routes.WorkoutDetail.route)
        }
    }
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
                onStartWorkout = { navController.navigate(Routes.WorkoutGraph.route) },
                onResumeWorkout = {
                    onResumeWorkout()
                    workoutState.draft?.let { navController.navigate(Routes.LiveWorkout.route) }
                },
                onViewAll = { navController.navigate(Routes.History.route) },
                onOpenSession = ::openWorkoutDetail,
                weightUnit = weightUnit,
                onNavigate = { tab -> navController.navigateTab(tab) },
            )
            // History is opened from the Home section action and shares the local completed-session stream.
            //
        }
        navigation(startDestination = Routes.PreWorkout.route, route = Routes.WorkoutGraph.route) {
        composable(Routes.PreWorkout.route) {
            PreWorkoutScreen(
                hasDraft = workoutState.draft != null,
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
                onContinueDraft = {
                    onResumeWorkout()
                    if (workoutState.draft != null) navController.navigate(Routes.LiveWorkout.route)
                },
                onDiscardDraft = {
                    scope.launch {
                        workoutState.draft?.let { workoutViewModel.discard(it.session.id) }
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
                onRemoveExercise = workoutViewModel::removeExercise,
                onFinish = workoutViewModel::finish,
                onResume = workoutViewModel::resume,
                onSave = workoutViewModel::save,
                onDiscard = workoutViewModel::discard,
                onExit = { navController.popBackStack(Routes.Home.route, false) },
                weightUnit = weightUnit,
            )
        }
        }
        composable(Routes.Progress.route) {
            LaunchedEffect(Unit) { workoutViewModel.refreshAnalytics(periodDays) }
            ProgressScreen(
                analytics = workoutState.analytics,
                periodDays = periodDays,
                weightUnit = weightUnit,
                onPeriodChange = { days -> periodDays = days; workoutViewModel.refreshAnalytics(days) },
                onNavigate = { tab -> if (tab != 2) navController.navigateTab(tab) },
            )
        }
        composable(Routes.History.route) {
            HistoryScreen(
                sessions = workoutState.recentSessions,
                onOpen = ::openWorkoutDetail,
                onNavigate = { tab -> navController.navigateTab(tab) },
            )
        }
        composable(Routes.Profile.route) {
            androidx.compose.material3.Scaffold(
                containerColor = Background,
                bottomBar = {
                    AthraBottomNav(selected = 3) { tab -> if (tab != 3) navController.navigateTab(tab) }
                },
                topBar = { TopAppBar(title = { Text("Profil") }) },
            ) { insets ->
                Column(Modifier.padding(insets).padding(20.dp)) {
                    Text(userName, style = MaterialTheme.typography.headlineMedium)
                    Text(userEmail, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Data latihan tersimpan di perangkat ini.")
                    TextButton(onClick = { navController.navigate(Routes.EditProfile.route) }) { Text("Edit profil") }
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
        composable(Routes.EditProfile.route) {
            EditProfileScreen(
                initialName = workoutState.profileName.ifBlank { userName },
                email = workoutState.profileEmail.ifBlank { userEmail },
                error = workoutState.error,
                onBack = { workoutViewModel.refreshProfile(); navController.popBackStack() },
                onSave = { name -> scope.launch { if (workoutViewModel.updateProfileName(name)) navController.popBackStack() } },
            )
        }
    }
}

private fun NavHostController.navigateTab(tab: Int) {
    val route = when (tab) {
        0 -> Routes.Home.route
        1 -> Routes.WorkoutGraph.route
        2 -> Routes.Progress.route
        3 -> Routes.Profile.route
        else -> return
    }
    navigate(route) {
        popUpTo(Routes.Home.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
