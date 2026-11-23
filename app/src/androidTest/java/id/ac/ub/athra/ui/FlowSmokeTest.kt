package id.ac.ub.athra.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import id.ac.ub.athra.domain.model.Exercise
import id.ac.ub.athra.domain.model.PersonalRecord
import id.ac.ub.athra.domain.model.PersonalRecordStatus
import id.ac.ub.athra.domain.model.SessionStatus
import id.ac.ub.athra.domain.model.WeightUnit
import id.ac.ub.athra.domain.model.WorkoutSession
import id.ac.ub.athra.domain.model.WorkoutDraft
import id.ac.ub.athra.domain.model.WorkoutExercise
import id.ac.ub.athra.domain.model.WorkoutSet
import id.ac.ub.athra.ui.auth.AuthScreen
import id.ac.ub.athra.ui.auth.AuthUiState
import id.ac.ub.athra.ui.screens.EditProfileScreen
import id.ac.ub.athra.ui.screens.HistoryScreen
import id.ac.ub.athra.ui.screens.HomeScreen
import id.ac.ub.athra.ui.screens.PreferencesScreen
import id.ac.ub.athra.ui.screens.ProgressScreen
import id.ac.ub.athra.ui.screens.PreWorkoutScreen
import id.ac.ub.athra.ui.workout.WorkoutFlowScreen
import id.ac.ub.athra.ui.workout.WorkoutSummaryScreen
import id.ac.ub.athra.ui.workout.WorkoutUiState
import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.atomic.AtomicInteger
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FlowSmokeTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun authScreenCanSwitchToAccountCreation() {
        compose.setContent {
            AuthScreen(AuthUiState(restoring = false), onSignIn = { _, _ -> }, onSignUp = { _, _, _, _ -> })
        }

        compose.onNodeWithText("Selamat datang kembali").assertIsDisplayed()
        compose.onNodeWithText("Belum punya akun? Daftar").performClick()
        compose.onNodeWithText("Buat akun").assertIsDisplayed()
        compose.onNodeWithText("Konfirmasi password").assertIsDisplayed()
    }

    @Test
    fun savedWorkoutDraftCanBeResumed() {
        var resumed = false
        compose.setContent {
            PreWorkoutScreen(hasDraft = true, onContinueDraft = { resumed = true })
        }

        compose.onNodeWithText("Sesi latihan belum selesai").assertIsDisplayed()
        compose.onNodeWithText("Lanjutkan sesi").performClick()
        assertTrue(resumed)
    }

    @Test
    fun homeDoesNotOfferStartUntilDraftCheckCompletes() {
        var started = false
        compose.setContent {
            HomeScreen(
                userName = "Uji",
                draft = null,
                recentSessions = listOf(WorkoutSession("done", "user", "Sesi lalu", 1_000, 2_000, SessionStatus.COMPLETED)),
                analytics = null,
                draftLoading = true,
                onStartWorkout = { started = true },
            )
        }

        compose.onNodeWithText("Memuat…").assertIsNotEnabled()
        compose.runOnIdle { assertTrue(!started) }
    }

    @Test
    fun preWorkoutHidesStartActionsUntilDraftCheckCompletes() {
        var started = false
        compose.setContent {
            PreWorkoutScreen(loading = true, onBeginSession = { started = true })
        }

        compose.onNodeWithText("Memeriksa sesi tersimpan…").assertIsDisplayed()
        assertTrue(compose.onAllNodesWithText("Mulai workout").fetchSemanticsNodes().isEmpty())
        compose.runOnIdle { assertTrue(!started) }
    }

    @Test
    fun analyticsPeriodSelectionCallsForTheSelectedRange() {
        var selectedPeriod = 0
        compose.setContent {
            ProgressScreen(
                analytics = null,
                periodDays = 7,
                weightUnit = WeightUnit.KG,
                onPeriodChange = { selectedPeriod = it },
                onNavigate = {},
            )
        }
        compose.onNodeWithText("30 hari").performClick()
        assertTrue(selectedPeriod == 30)
    }

    @Test
    fun historySessionOpensTheSelectedDetail() {
        var openedId = ""
        compose.setContent {
            HistoryScreen(
                sessions = listOf(WorkoutSession("s1", "u1", "Sesi uji", 1_000, 2_000, SessionStatus.COMPLETED)),
                onNavigate = {},
                onOpen = { openedId = it },
            )
        }
        compose.onNodeWithText("Sesi uji").performClick()
        assertTrue(openedId == "s1")
    }

    @Test
    fun summaryShowsSavedMetricsAndPersonalRecordBeforeSave() {
        val exercise = Exercise("bench_press", "Bench Press", "Chest", "Barbell", "")
        val draft = WorkoutDraft(
            WorkoutSession("review", "user", "Latihan sore", 1_000, 61_000, SessionStatus.REVIEW),
            listOf(WorkoutExercise(exercise, 0, listOf(WorkoutSet("set-1", 1, 10, BigDecimal("32"))))),
        )
        var saved = false
        compose.setContent {
            WorkoutSummaryScreen(
                draft = draft,
                state = WorkoutUiState(
                    draft = draft,
                    personalRecords = listOf(
                        PersonalRecord("bench_press", BigDecimal("30"), BigDecimal("32"), PersonalRecordStatus.NEW_RECORD),
                    ),
                ),
                weightUnit = WeightUnit.KG,
                onResume = {},
                onSave = { saved = true },
                onDiscard = {},
            )
        }

        compose.onNodeWithText("Ringkasan workout").assertIsDisplayed()
        compose.onNodeWithText("Rekor baru · 32 kg · sebelumnya 30 kg").assertIsDisplayed()
        compose.onNodeWithText("Simpan sesi").performClick()
        compose.runOnIdle { assertTrue(saved) }
    }

    @Test
    fun editProfileTrimsNameBeforeSaving() {
        var savedName: String? = null
        compose.setContent {
            EditProfileScreen(
                initialName = "",
                email = "athra@example.test",
                error = null,
                onBack = {},
                onSave = { savedName = it },
            )
        }

        compose.onNodeWithText("Nama").performTextInput("  Atlet ATHRA  ")
        compose.onNodeWithText("Simpan").performClick()
        compose.runOnIdle { assertEquals("Atlet ATHRA", savedName) }
    }

    @Test
    fun preferencesSaveUnitOnlyAfterExplicitSave() {
        var savedUnit: WeightUnit? = null
        compose.setContent {
            PreferencesScreen(WeightUnit.KG, onBack = {}, onSave = { savedUnit = it })
        }
        compose.onNodeWithText("Pound (lb)").performClick()
        assertTrue(savedUnit == null)
        compose.onNodeWithText("Simpan").performClick()
        assertTrue(savedUnit == WeightUnit.LB)
    }

    @Test
    fun rapidSaveSetTapsKeepOnlyOneSubmissionInFlight() {
        val exercise = Exercise("bench_press", "Bench Press", "Chest", "Barbell", "")
        val draft = WorkoutDraft(
            WorkoutSession("session-1", "user-1", "Workout", 1_000L, null, SessionStatus.ACTIVE),
            listOf(WorkoutExercise(exercise, 0, emptyList())),
        )
        val pendingSave = CompletableDeferred<Boolean>()
        val saveCalls = AtomicInteger()
        compose.setContent {
            WorkoutFlowScreen(
                draft = draft,
                state = WorkoutUiState(draft = draft, catalog = listOf(exercise)),
                onSaveSet = { _, _, _, _, _, _ ->
                    saveCalls.incrementAndGet()
                    pendingSave.await()
                },
                onDeleteSet = { _, _ -> },
                onAddExercise = { _, _ -> },
                onRemoveExercise = { _, _ -> },
                onFinish = { true },
                onResume = {},
                onSave = { true },
                onDiscard = {},
                onExit = {},
            )
        }

        compose.onNodeWithText("Repetisi").performTextInput("10")
        compose.onNodeWithText("Beban (kg)").performTextInput("32")
        compose.onNodeWithText("Simpan set").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Menyimpan…").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(1, saveCalls.get()) }

        pendingSave.complete(true)
        compose.waitForIdle()
        compose.onNodeWithText("Simpan set").assertIsDisplayed()
    }

    @Test
    fun failedDraftDiscardKeepsWorkoutOpen() {
        val exercise = Exercise("bench_press", "Bench Press", "Chest", "Barbell", "")
        val draft = WorkoutDraft(
            WorkoutSession("session-1", "user-1", "Workout", 1_000L, null, SessionStatus.ACTIVE),
            listOf(WorkoutExercise(exercise, 0, emptyList())),
        )
        var exited = false
        compose.setContent {
            WorkoutFlowScreen(
                draft = draft,
                state = WorkoutUiState(draft = draft, catalog = listOf(exercise)),
                onSaveSet = { _, _, _, _, _, _ -> false },
                onDeleteSet = { _, _ -> },
                onAddExercise = { _, _ -> },
                onRemoveExercise = { _, _ -> },
                onFinish = { false },
                onResume = {},
                onSave = { false },
                onDiscard = {},
                onDiscardResult = { false },
                onExit = { exited = true },
            )
        }

        compose.onNodeWithText("Buang sesi").performClick()
        compose.onAllNodesWithText("Buang sesi").get(1).performClick()
        compose.waitForIdle()
        compose.runOnIdle { assertTrue(!exited) }
    }
}
