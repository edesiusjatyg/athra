package id.ac.ub.athra.ui

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToLog
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavHostController
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import id.ac.ub.athra.data.local.AppDatabase
import id.ac.ub.athra.data.repository.WorkoutRepository
import id.ac.ub.athra.domain.model.WeightUnit
import id.ac.ub.athra.ui.navigation.AthraNavGraph
import id.ac.ub.athra.ui.navigation.Routes
import id.ac.ub.athra.ui.theme.AthraTheme
import id.ac.ub.athra.ui.workout.WorkoutViewModel
import id.ac.ub.athra.ui.workout.WorkoutViewModelFactory
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutNavigationFlowTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var database: AppDatabase
    private lateinit var repository: WorkoutRepository
    private lateinit var storeOwner: ViewModelStoreOwner
    private lateinit var workoutViewModel: WorkoutViewModel

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(AppDatabase.validationCallback)
            .allowMainThreadQueries()
            .build()
        repository = WorkoutRepository(database, context.assets)
        runBlocking {
            repository.ensureProfile("nav-test-user", "Atlet Uji", "atlet@example.test")
            repository.seedCatalog()
        }
        storeOwner = compose.activity
        workoutViewModel = ViewModelProvider(
            storeOwner,
            WorkoutViewModelFactory("nav-test-user", repository),
        )[WorkoutViewModel::class.java]
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun workoutCanStartAddSetReviewAndSaveAcrossGraphRoutes() {
        var savedUnit: WeightUnit? = null
        var testNavController: NavHostController? = null
        val graphFactory = WorkoutViewModelFactory(
            "nav-test-user",
            repository,
            savedStateEnabled = true,
        )
        compose.setContent {
            CompositionLocalProvider(LocalViewModelStoreOwner provides storeOwner) {
                AthraTheme {
                    val state by workoutViewModel.state.collectAsStateWithLifecycle()
                    val navController = rememberNavController()
                    SideEffect { testNavController = navController }
                    AthraNavGraph(
                        navController = navController,
                        userId = "nav-test-user",
                        database = database,
                        userName = state.profileName.ifBlank { "Atlet Uji" },
                        workoutState = state,
                        workoutViewModel = workoutViewModel,
                        workoutViewModelFactory = graphFactory,
                        onResumeWorkout = {},
                        onSignOut = {},
                        userEmail = "atlet@example.test",
                        weightUnit = WeightUnit.KG,
                        onSaveUnit = { savedUnit = it },
                    )
                }
            }
        }

        compose.waitUntil(5_000) { !workoutViewModel.state.value.draftLoading }
        compose.onNodeWithText("Mulai workout").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Pilih caramu berlatih hari ini.").fetchSemanticsNodes().isNotEmpty() }
        compose.waitUntil(5_000) {
            runCatching { compose.onNodeWithText("Mulai sesi").assertIsEnabled() }.isSuccess
        }
        compose.onNodeWithText("Mulai sesi").performClick()

        compose.waitUntil(5_000) { compose.onAllNodesWithText("Pustaka gerakan").fetchSemanticsNodes().isNotEmpty() }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Bench Press").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Tambahkan Bench Press").assertIsDisplayed().performClick()

        compose.waitUntil(5_000) { compose.onAllNodesWithText("Repetisi").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Detail").performClick()
        compose.onAllNodesWithText("Detail gerakan").get(0).assertIsDisplayed()
        compose.onNodeWithContentDescription("Kembali").performClick()
        compose.onNodeWithText("Repetisi").assertIsDisplayed()
        compose.onNodeWithText("Repetisi").performTextInput("10")
        compose.onNodeWithText("Beban (kg)").performTextInput("32")
        compose.onNodeWithText("Simpan set").assertIsEnabled()
        compose.onNodeWithText("Simpan set").performClick()
        try {
            compose.waitUntil(5_000) {
                runBlocking { repository.getDraft("nav-test-user")?.exercises?.any { it.sets.isNotEmpty() } == true }
            }
        } catch (error: Throwable) {
            val draft = runBlocking { repository.getDraft("nav-test-user") }
            val graphEntry = testNavController!!.getBackStackEntry(Routes.WorkoutGraph.route)
            val graphViewModel = ViewModelProvider(graphEntry, graphFactory)
                .get("workout_graph_viewmodel", WorkoutViewModel::class.java)
            val graphState = graphViewModel.state.value
            Log.e(
                "WorkoutNavigationFlowTest",
                "Draft after set save: ${draft?.exercises?.map { it.exercise.id to it.sets.size }}; graph form=${graphState.repsInput}/${graphState.weightInput}, selected=${graphState.selectedExerciseId}, busy=${graphState.busy}, error=${graphState.error}",
                error,
            )
            compose.onRoot().printToLog("WorkoutNavigationFlowTest")
            throw error
        }
        compose.onNodeWithText("Repetisi").performImeAction()
        compose.waitForIdle()
        compose.waitUntil(5_000) {
            runCatching { compose.onNodeWithText("Selesai dan tinjau").assertIsEnabled() }.isSuccess
        }

        compose.onNodeWithText("Selesai dan tinjau").performClick()
        compose.waitUntil(5_000) {
            runBlocking { repository.getDraft("nav-test-user")?.session?.status?.name == "REVIEW" }
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Ringkasan").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Simpan sesi").performClick()
        compose.waitUntil(5_000) {
            runBlocking { database.workoutDao().getCompleted("nav-test-user").size == 1 }
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Sesi terbaru").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Sesi terbaru").assertIsDisplayed()
        val completed = runBlocking { database.workoutDao().getCompleted("nav-test-user") }
        assertEquals(1, completed.size)
        assertNotNull(runBlocking { repository.getWorkoutDetail("nav-test-user", completed.single().sessionId) })

        compose.onNodeWithText("Lihat semua").performScrollTo().performClick()
        assertEquals(Routes.History.route, testNavController!!.currentDestination?.route)
        compose.waitUntil(5_000) {
            runCatching { compose.onNodeWithText(completed.single().name).assertIsDisplayed() }.isSuccess
        }
        compose.onNodeWithText(completed.single().name).performClick()
        compose.onNodeWithText("Detail latihan").assertIsDisplayed()
        compose.waitUntil(5_000) {
            runCatching { compose.onNodeWithText("Bench Press").assertIsDisplayed() }.isSuccess
        }
        compose.onNodeWithContentDescription("Kembali").performClick()
        compose.onNodeWithText("Kembali ke beranda").performClick()
        assertEquals(Routes.Home.route, testNavController!!.currentDestination?.route)
        compose.waitUntil(5_000) {
            runCatching { compose.onNodeWithText("Profil").assertIsDisplayed() }.isSuccess
        }
        compose.onNodeWithText("Profil").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Edit profil").fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNodeWithText("Edit profil").performClick()
        compose.onNodeWithContentDescription("Nama tampilan").performTextReplacement("Nama Baru")
        compose.onNodeWithText("Simpan profil").performClick()
        compose.waitUntil(5_000) {
            testNavController!!.currentDestination?.route == Routes.Profile.route &&
                compose.onAllNodesWithText("Nama Baru").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Satuan beban").performScrollTo().performClick()
        compose.onNodeWithText("Pound (lb)").performClick()
        compose.onNodeWithText("Simpan preferensi").performClick()
        compose.waitUntil(5_000) { savedUnit == WeightUnit.LB }
    }

    @Test
    fun recoveredReviewCanReturnToActiveWithoutPoppingWorkoutGraph() {
        runBlocking {
            val draft = repository.startOrResume("nav-test-user", "Workout pulih", now = 1_000L)
            repository.addExercise("nav-test-user", draft.session.id, "bench_press")
            repository.saveSet(
                "nav-test-user",
                draft.session.id,
                "bench_press",
                setId = null,
                repsText = "12",
                weightText = "30",
                unit = WeightUnit.KG,
            )
            repository.enterReview("nav-test-user", draft.session.id, endedAt = 5_000L)
            workoutViewModel.refreshDraft().join()
        }

        compose.setContent {
            CompositionLocalProvider(LocalViewModelStoreOwner provides storeOwner) {
                AthraTheme {
                    val state by workoutViewModel.state.collectAsStateWithLifecycle()
                    AthraNavGraph(
                        navController = rememberNavController(),
                        userId = "nav-test-user",
                        database = database,
                        userName = state.profileName.ifBlank { "Atlet Uji" },
                        workoutState = state,
                        workoutViewModel = workoutViewModel,
                        workoutViewModelFactory = WorkoutViewModelFactory(
                            "nav-test-user",
                            repository,
                            savedStateEnabled = true,
                        ),
                        onResumeWorkout = {},
                        onSignOut = {},
                        userEmail = "atlet@example.test",
                        weightUnit = WeightUnit.KG,
                        onSaveUnit = {},
                    )
                }
            }
        }

        compose.waitUntil(5_000) {
            workoutViewModel.state.value.draft?.session?.status?.name == "REVIEW"
        }
        compose.onNodeWithText("Lanjutkan sesi").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Ringkasan").fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNodeWithContentDescription("Perbaiki sesi").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Workout aktif").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("Set 1: 30 kg × 12. Ketuk untuk edit").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Selesai dan tinjau").assertIsDisplayed()
        assertEquals("ACTIVE", runBlocking { repository.getDraft("nav-test-user")?.session?.status?.name })
    }
}
