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
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import id.ac.ub.athra.ui.workout.WorkoutUiState
import id.ac.ub.athra.ui.screens.HomeScreen
import id.ac.ub.athra.ui.screens.PreWorkoutScreen
import id.ac.ub.athra.ui.workout.WorkoutViewModel
import id.ac.ub.athra.ui.workout.WorkoutFlowScreen
import id.ac.ub.athra.ui.workout.QuickStartScreen
import id.ac.ub.athra.ui.workout.ExerciseLibraryScreen
import id.ac.ub.athra.ui.workout.ExerciseFiltersScreen
import id.ac.ub.athra.ui.workout.ExerciseDetailsScreen
import id.ac.ub.athra.ui.workout.WorkoutSummaryScreen
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
import id.ac.ub.athra.domain.model.WeightUnit
import id.ac.ub.athra.domain.model.SessionStatus
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    var periodDays by rememberSaveable { mutableIntStateOf(30) }
    var showFixtureConfirm by rememberSaveable(userId) { mutableStateOf(false) }
    var fixtureBusy by rememberSaveable(userId) { mutableStateOf(false) }
    var fixtureMessage by rememberSaveable(userId) { mutableStateOf<String?>(null) }
    var fixtureMessageIsError by rememberSaveable(userId) { mutableStateOf(false) }
    fun openWorkoutDetail(sessionId: String) {
        navController.navigate(Routes.WorkoutDetail.create(sessionId))
    }
    fun exitWorkoutToHome() {
        scope.launch {
            workoutViewModel.refreshDraft().join()
            workoutViewModel.refreshHomeAnalytics().join()
            withContext(Dispatchers.Main.immediate) {
                navController.popBackStack(Routes.Home.route, false)
            }
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
                loading = workoutState.loading || workoutState.homeAnalyticsLoading || workoutState.historyLoading,
                draftLoading = workoutState.draftLoading,
                draftError = workoutState.draftError,
                error = workoutState.error ?: workoutState.homeAnalyticsError ?: workoutState.historyError,
                onRetry = {
                    workoutViewModel.refreshDraft()
                    workoutViewModel.refreshHomeAnalytics()
                    workoutViewModel.refreshHistory()
                },
                onStartWorkout = { navController.navigate(Routes.WorkoutGraph.route) },
                onResumeWorkout = {
                    scope.launch {
                        workoutViewModel.refreshDraft().join()
                        val draft = workoutViewModel.state.value.draft ?: return@launch
                        withContext(Dispatchers.Main.immediate) {
                            navController.navigate(Routes.WorkoutGraph.route) { launchSingleTop = true }
                            navController.navigate(
                                if (draft.session.status == SessionStatus.REVIEW) Routes.Summary.route else Routes.LiveWorkout.route,
                            ) { launchSingleTop = true }
                        }
                    }
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
                fun continueDraft() {
                    val draft = graphState.draft ?: return
                    val destination = when {
                        draft.session.status == SessionStatus.REVIEW -> Routes.Summary.route
                        draft.exercises.isEmpty() -> Routes.ExerciseLibrary.route
                        else -> Routes.LiveWorkout.route
                    }
                    navController.navigate(destination) { launchSingleTop = true }
                }
                PreWorkoutScreen(
                    hasDraft = graphState.draft != null,
                    busy = graphState.busy,
                    loading = graphState.draftLoading,
                    error = graphState.draftError ?: graphState.error,
                    onRetry = { graphViewModel.refreshDraft() },
                    onBack = { navController.popBackStack() },
                    onBeginSession = {
                        scope.launch {
                            if (graphViewModel.start() != null) {
                                withContext(Dispatchers.Main.immediate) {
                                    navController.navigate(Routes.ExerciseLibrary.route)
                                }
                            }
                        }
                    },
                    onQuickStart = { navController.navigate(Routes.QuickStart.route) },
                    onContinueDraft = ::continueDraft,
                    onDiscardDraft = {
                        scope.launch {
                            val draft = graphState.draft ?: return@launch
                            if (graphViewModel.discardResult(draft.session.id) && graphViewModel.start() != null) {
                                withContext(Dispatchers.Main.immediate) {
                                    navController.navigate(Routes.ExerciseLibrary.route)
                                }
                            }
                        }
                    },
                )
            }
            composable(Routes.QuickStart.route) { backStackEntry ->
                val graphViewModel = rememberWorkoutGraphViewModel(navController, backStackEntry, workoutViewModelFactory)
                val graphState by graphViewModel.state.collectAsStateWithLifecycle()
                QuickStartScreen(
                    name = graphState.quickStartName,
                    busy = graphState.busy,
                    error = graphState.error,
                    onNameChange = graphViewModel::updateQuickStartName,
                    onBack = { navController.popBackStack() },
                    onStart = {
                        scope.launch {
                            if (graphViewModel.start(graphState.quickStartName) != null) {
                                graphViewModel.updateQuickStartName("")
                                withContext(Dispatchers.Main.immediate) {
                                    navController.navigate(Routes.ExerciseLibrary.route)
                                }
                            }
                        }
                    },
                )
            }
            composable(Routes.ExerciseLibrary.route) { backStackEntry ->
                val graphViewModel = rememberWorkoutGraphViewModel(navController, backStackEntry, workoutViewModelFactory)
                val graphState by graphViewModel.state.collectAsStateWithLifecycle()
                ExerciseLibraryScreen(
                    state = graphState,
                    onSearchChange = graphViewModel::updateLibrarySearch,
                    onOpenFilters = { navController.navigate(Routes.ExerciseFilters.route) },
                    onOpenDetails = { exerciseId -> navController.navigate(Routes.ExerciseDetails.create(exerciseId)) },
                    onBack = { navController.popBackStack() },
                    onRetry = { graphViewModel.retryCatalog() },
                )
            }
            composable(Routes.ExerciseFilters.route) { backStackEntry ->
                val graphViewModel = rememberWorkoutGraphViewModel(navController, backStackEntry, workoutViewModelFactory)
                val graphState by graphViewModel.state.collectAsStateWithLifecycle()
                ExerciseFiltersScreen(
                    selected = graphState.pendingLibraryFilter,
                    onSelect = graphViewModel::updatePendingLibraryFilter,
                    onReset = graphViewModel::resetLibraryFilter,
                    onApply = { graphViewModel.applyLibraryFilter(); navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = Routes.ExerciseDetails.route,
                arguments = listOf(navArgument("exerciseId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val graphViewModel = rememberWorkoutGraphViewModel(navController, backStackEntry, workoutViewModelFactory)
                val graphState by graphViewModel.state.collectAsStateWithLifecycle()
                val exerciseId = backStackEntry.arguments?.getString("exerciseId").orEmpty()
                val exercise = graphState.catalog.firstOrNull { it.id == exerciseId }
                ExerciseDetailsScreen(
                    exercise = exercise,
                    reference = graphState.exerciseReferences[exerciseId],
                    weightUnit = weightUnit,
                    busy = graphState.busy,
                    error = graphState.error,
                    onBack = { navController.popBackStack() },
                    onAdd = {
                        scope.launch {
                            val sessionId = graphViewModel.state.value.draft?.session?.id ?: return@launch
                            if (graphViewModel.addExerciseResult(sessionId, exerciseId)) {
                                graphViewModel.selectExercise(exerciseId)
                                withContext(Dispatchers.Main.immediate) {
                                    val returnedToActive = navController.popBackStack(Routes.LiveWorkout.route, inclusive = false)
                                    if (!returnedToActive) {
                                        navController.popBackStack(Routes.ExerciseLibrary.route, inclusive = true)
                                        navController.navigate(Routes.LiveWorkout.route) { launchSingleTop = true }
                                    }
                                }
                            }
                        }
                    },
                )
            }
            composable(Routes.LiveWorkout.route) { backStackEntry ->
                val graphViewModel = rememberWorkoutGraphViewModel(navController, backStackEntry, workoutViewModelFactory)
                val graphState by graphViewModel.state.collectAsStateWithLifecycle()
                val draft = graphState.draft
                if (draft == null) {
                    Column(Modifier.fillMaxSize(), verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center, horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text("Memuat sesi latihan…")
                        graphState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        TextButton(onClick = { graphViewModel.refreshDraft() }) { Text("Coba lagi") }
                    }
                } else {
                    LaunchedEffect(draft.session.status) {
                        if (draft.session.status == SessionStatus.REVIEW) {
                            navController.navigate(Routes.Summary.route) { launchSingleTop = true }
                        }
                    }
                    WorkoutFlowScreen(
                        draft = draft,
                        state = graphState,
                        onSaveSet = graphViewModel::saveSet,
                        onDeleteSet = graphViewModel::deleteSet,
                        onAddExercise = graphViewModel::addExercise,
                        onRemoveExercise = graphViewModel::removeExercise,
                        onFinish = { sessionId ->
                            val finished = graphViewModel.finish(sessionId)
                            if (finished) {
                                withContext(Dispatchers.Main.immediate) {
                                    navController.navigate(Routes.Summary.route) { launchSingleTop = true }
                                }
                            }
                            finished
                        },
                        onResume = graphViewModel::resume,
                        onSave = graphViewModel::save,
                        onDiscard = graphViewModel::discard,
                        onDiscardResult = graphViewModel::discardResult,
                        onExit = ::exitWorkoutToHome,
                        onOpenLibrary = { navController.navigate(Routes.ExerciseLibrary.route) },
                        onOpenExerciseDetails = { id -> navController.navigate(Routes.ExerciseDetails.create(id)) },
                        onUpdateSetInputs = graphViewModel::updateSetInputs,
                        onSelectExercise = graphViewModel::selectExercise,
                        onEditSetInputs = graphViewModel::editSetInputs,
                        onClearSetForm = graphViewModel::clearSetForm,
                        weightUnit = weightUnit,
                    )
                }
            }
            composable(Routes.Summary.route) { backStackEntry ->
                val graphViewModel = rememberWorkoutGraphViewModel(navController, backStackEntry, workoutViewModelFactory)
                val graphState by graphViewModel.state.collectAsStateWithLifecycle()
                val draft = graphState.draft
                if (draft == null) {
                    Column(Modifier.fillMaxSize(), verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center, horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        if (graphState.loading || graphState.busy) CircularProgressIndicator()
                        Text(graphState.error ?: "Memuat ringkasan…")
                        TextButton(onClick = { graphViewModel.refreshDraft() }) { Text("Coba lagi") }
                    }
                } else if (draft.session.status != SessionStatus.REVIEW) {
                    LaunchedEffect(draft.session.status) { navController.popBackStack() }
                } else {
                    WorkoutSummaryScreen(
                        draft = draft,
                        state = graphState,
                        weightUnit = weightUnit,
                        onResume = {
                            scope.launch {
                                if (graphViewModel.resumeResult(draft.session.id)) {
                                    withContext(Dispatchers.Main.immediate) { navController.popBackStack() }
                                }
                            }
                        },
                        onSave = {
                            scope.launch { if (graphViewModel.save(draft.session.id)) exitWorkoutToHome() }
                        },
                        onDiscard = {
                            scope.launch { if (graphViewModel.discardResult(draft.session.id)) exitWorkoutToHome() }
                        },
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
                loading = workoutState.analyticsLoading,
                error = workoutState.analyticsError,
                onRetry = { workoutViewModel.refreshAnalytics(periodDays) },
                onPeriodChange = { days -> periodDays = days; workoutViewModel.refreshAnalytics(days) },
                onNavigate = { tab -> if (tab != 2) navController.navigateTab(tab) },
            )
        }
        composable(Routes.History.route) {
            HistoryScreen(
                sessions = workoutState.recentSessions,
                loading = workoutState.historyLoading,
                error = workoutState.historyError,
                onRetry = workoutViewModel::refreshHistory,
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
                    if (workoutState.profileLoading && workoutState.profileName.isBlank()) CircularProgressIndicator()
                    workoutState.profileError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = { workoutViewModel.refreshProfile() }) { Text("Coba lagi") }
                    }
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
        composable(
            route = Routes.WorkoutDetail.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId").orEmpty()
            LaunchedEffect(sessionId) { workoutViewModel.loadWorkoutDetail(sessionId) }
            WorkoutDetailScreen(
                workout = workoutState.selectedWorkoutDetail,
                weightUnit = weightUnit,
                loading = workoutState.workoutDetailLoading,
                error = workoutState.workoutDetailError,
                onBack = { navController.popBackStack() },
                onRetry = { scope.launch { workoutViewModel.loadWorkoutDetail(sessionId) } },
            )
        }
        composable(Routes.Preferences.route) {
            PreferencesScreen(
                initialUnit = weightUnit,
                onBack = { navController.popBackStack() },
                onSave = {
                    unit -> scope.launch {
                        onSaveUnit(unit)
                        withContext(Dispatchers.Main.immediate) { navController.popBackStack() }
                    }
                },
            )
        }
        composable(Routes.EditProfile.route) {
            EditProfileScreen(
                initialName = workoutState.profileName.ifBlank { userName },
                email = workoutState.profileEmail.ifBlank { userEmail },
                error = workoutState.profileError ?: workoutState.error,
                onBack = { workoutViewModel.refreshProfile(); navController.popBackStack() },
                onSave = {
                    name -> scope.launch {
                        if (workoutViewModel.updateProfileName(name)) {
                            withContext(Dispatchers.Main.immediate) { navController.popBackStack() }
                        }
                    }
                },
                busy = workoutState.busy,
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
