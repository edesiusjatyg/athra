package id.ac.ub.athra.ui

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import id.ac.ub.athra.data.local.AppDatabase
import id.ac.ub.athra.data.repository.WorkoutRepository
import id.ac.ub.athra.domain.model.WeightUnit
import id.ac.ub.athra.ui.navigation.AthraNavGraph
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
                        onSaveUnit = { savedUnit = it },
                    )
                }
            }
        }

        compose.waitUntil(5_000) { !workoutViewModel.state.value.draftLoading }
        compose.onNodeWithText("Mulai workout").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Siap berlatih?").fetchSemanticsNodes().isNotEmpty() }
        compose.waitUntil(5_000) { !workoutViewModel.state.value.draftLoading }
        compose.onAllNodesWithText("Mulai workout").get(1).performClick()

        compose.waitUntil(5_000) { compose.onAllNodesWithText("Pilih gerakan").fetchSemanticsNodes().isNotEmpty() }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Bench Press").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Bench Press").performClick()
        compose.onNodeWithText("Tambah ke sesi").assertIsDisplayed().performClick()

        compose.waitUntil(5_000) { compose.onAllNodesWithText("Repetisi").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Repetisi").performTextInput("10")
        compose.onNodeWithText("Beban (kg)").performTextInput("32")
        compose.onNodeWithText("Simpan set").performClick()
        compose.waitUntil(5_000) {
            runBlocking { repository.getDraft("nav-test-user")?.exercises?.any { it.sets.isNotEmpty() } == true }
        }
        compose.onNodeWithText("Beban (kg)").performImeAction()
        compose.waitForIdle()
        compose.waitUntil(5_000) {
            runCatching { compose.onNodeWithText("Selesai dan tinjau").assertIsEnabled() }.isSuccess
        }

        compose.onNodeWithText("Selesai dan tinjau").performClick()
        compose.waitUntil(5_000) {
            runBlocking { repository.getDraft("nav-test-user")?.session?.status?.name == "REVIEW" }
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Ringkasan workout").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Simpan sesi").performClick()
        compose.waitUntil(5_000) {
            runBlocking { database.workoutDao().getCompleted("nav-test-user").size == 1 }
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Sesi terbaru").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Sesi terbaru").assertIsDisplayed()
        val completed = runBlocking { database.workoutDao().getCompleted("nav-test-user") }
        assertEquals(1, completed.size)
        assertNotNull(runBlocking { repository.getWorkoutDetail("nav-test-user", completed.single().sessionId) })

        compose.onNodeWithText("Lihat semua").performClick()
        compose.onNodeWithText("Riwayat latihan").assertIsDisplayed()
        compose.onNodeWithText(completed.single().name).performClick()
        compose.onNodeWithText("Detail latihan").assertIsDisplayed()
        compose.onNodeWithText("Bench Press").assertIsDisplayed()
        compose.onNodeWithText("Kembali").performClick()
        compose.onNodeWithText("Profil").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Edit profil").fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNodeWithText("Edit profil").performClick()
        compose.onNodeWithText("Nama").performTextReplacement("Nama Baru")
        compose.onNodeWithText("Simpan").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Nama Baru").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Preferensi satuan").performClick()
        compose.onNodeWithText("Pound (lb)").performClick()
        compose.onNodeWithText("Simpan").performClick()
        compose.waitUntil(5_000) { savedUnit == WeightUnit.LB }
    }
}
