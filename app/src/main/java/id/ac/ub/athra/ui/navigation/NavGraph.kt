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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.ac.ub.athra.BuildConfig
import id.ac.ub.athra.data.local.AppDatabase
import id.ac.ub.athra.debugfixtures.installH1H2Fixtures
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    userId: String,
    database: AppDatabase,
    userName: String,
    workoutState: WorkoutUiState,
    workoutViewModel: WorkoutViewModel,
    workoutViewModelFactory: ViewModelProvider.Factory,
    onResumeWorkout: () -> Unit,
    onSignOut: () -> Unit,
    userEmail: String,
    weightUnit: WeightUnit,
    onSaveUnit: suspend (WeightUnit) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var periodDays by rememberSaveable { mutableIntStateOf(7) }
    var selectedWorkout by remember { mutableStateOf<CompletedWorkout?>(null) }
    var showFixtureConfirm by rememberSaveable(userId) { mutableStateOf(false) }
    var fixtureBusy by rememberSaveable(userId) { mutableStateOf(false) }
    var fixtureMessage by rememberSaveable(userId) { mutableStateOf<String?>(null) }
    var fixtureMessageIsError by rememberSaveable(userId) { mutableStateOf(false) }
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
            composable(Routes.PreWorkout.route) { backStackEntry ->
                val graphViewModel = rememberWorkoutGraphViewModel(navController, backStackEntry, workoutViewModelFactory)
                val graphState by graphViewModel.state.collectAsStateWithLifecycle()
                PreWorkoutScreen(
                    hasDraft = graphState.draft != null,
                    onBack = { navController.popBackStack() },
                    onBeginSession = {
                        scope.launch {
                            if (graphViewModel.start() != null) navController.navigate(Routes.LiveWorkout.route)
                        }
                    },
                    onQuickStart = {
                        scope.launch {
                            if (graphViewModel.start() != null) navController.navigate(Routes.LiveWorkout.route)
                        }
                    },
                    onContinueDraft = {
                        scope.launch {
                            graphState.draft?.let {
                                graphViewModel.resume(it.session.id)
                                navController.navigate(Routes.LiveWorkout.route)
                            }
                        }
                    },
                    onDiscardDraft = {
                        scope.launch {
                            graphState.draft?.let { graphViewModel.discard(it.session.id) }
                            if (graphViewModel.start() != null) navController.navigate(Routes.LiveWorkout.route)
                        }
                    },
                )
            }
            composable(Routes.LiveWorkout.route) { backStackEntry ->
                val graphViewModel = rememberWorkoutGraphViewModel(navController, backStackEntry, workoutViewModelFactory)
                val graphState by graphViewModel.state.collectAsStateWithLifecycle()
                val draft = graphState.draft
                if (draft == null) {
                    androidx.compose.material3.CircularProgressIndicator()
                } else {
                    val exitWorkout: () -> Unit = {
                        scope.launch {
                            workoutViewModel.refreshDraft().join()
                            workoutViewModel.refreshHomeAnalytics().join()
                            navController.popBackStack(Routes.Home.route, false)
                        }
                    }
                    WorkoutFlowScreen(
                        draft = draft,
                        state = graphState,
                        onSaveSet = graphViewModel::saveSet,
                        onDeleteSet = graphViewModel::deleteSet,
                        onAddExercise = graphViewModel::addExercise,
                        onRemoveExercise = graphViewModel::removeExercise,
                        onFinish = graphViewModel::finish,
                        onResume = graphViewModel::resume,
                        onSave = graphViewModel::save,
                        onDiscard = graphViewModel::discard,
                        onExit = exitWorkout,
                        weightUnit = weightUnit,
                    )
                }
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
                Column(Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(20.dp)) {
                    Text(userName, style = MaterialTheme.typography.headlineMedium)
                    Text(userEmail, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Data latihan tersimpan di perangkat ini.")
                    TextButton(onClick = { navController.navigate(Routes.EditProfile.route) }) { Text("Edit profil") }
                    TextButton(onClick = { navController.navigate(Routes.Preferences.route) }) { Text("Preferensi satuan") }
                    TextButton(onClick = onSignOut) { Text("Keluar") }
                    if (BuildConfig.DEBUG && BuildConfig.AUTH_EMULATOR_ENABLED) {
                        Spacer(Modifier.height(8.dp))
                        Text("Demo Auth Emulator", style = MaterialTheme.typography.titleSmall)
                        Text("Reset hanya mengganti fixture H1/H2 untuk akun ini. Riwayat lain tetap ada.")
                        OutlinedButton(
                            onClick = { showFixtureConfirm = true },
                            enabled = !fixtureBusy,
                        ) {
                            if (fixtureBusy) CircularProgressIndicator()
                            else Text("Reset fixture H1/H2")
                        }
                        fixtureMessage?.let {
                            Text(
                                it,
                                color = if (fixtureMessageIsError) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
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
    if (showFixtureConfirm) {
        AlertDialog(
            onDismissRequest = { if (!fixtureBusy) showFixtureConfirm = false },
            title = { Text("Reset fixture demo?") },
            text = { Text("H1 dan H2 pada akun ini akan dibuat ulang dengan tanggal dan data PRD. Sesi lain tidak dihapus.") },
            confirmButton = {
                TextButton(
                    enabled = !fixtureBusy,
                    onClick = {
                        showFixtureConfirm = false
                        scope.launch {
                            fixtureBusy = true
                            try {
                                installH1H2Fixtures(database, userId)
                                workoutViewModel.refreshDraft().join()
                                workoutViewModel.refreshHomeAnalytics().join()
                                workoutViewModel.refreshReferences().join()
                                workoutViewModel.refreshAnalytics(periodDays)
                                fixtureMessageIsError = false
                                fixtureMessage = "Fixture H1/H2 siap. Buat sesi H3 melalui alur workout biasa."
                            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                                throw cancelled
                            } catch (error: Exception) {
                                fixtureMessageIsError = true
                                fixtureMessage = error.message ?: "Fixture demo gagal disiapkan."
                            } finally {
                                fixtureBusy = false
                            }
                        }
                    },
                ) { Text("Reset fixture") }
            },
            dismissButton = { TextButton(onClick = { showFixtureConfirm = false }, enabled = !fixtureBusy) { Text("Batal") } },
        )
    }
}

@Composable
private fun rememberWorkoutGraphViewModel(
    navController: NavHostController,
    backStackEntry: androidx.navigation.NavBackStackEntry,
    factory: ViewModelProvider.Factory,
): WorkoutViewModel {
    val graphEntry = remember(backStackEntry) {
        navController.getBackStackEntry(Routes.WorkoutGraph.route)
    }
    return remember(graphEntry, factory) {
        ViewModelProvider(graphEntry, factory).get("workout_graph_viewmodel", WorkoutViewModel::class.java)
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
