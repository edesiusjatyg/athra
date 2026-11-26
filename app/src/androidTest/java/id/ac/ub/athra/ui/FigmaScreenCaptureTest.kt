package id.ac.ub.athra.ui

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.platform.app.InstrumentationRegistry
import id.ac.ub.athra.domain.model.AnalyticsSummary
import id.ac.ub.athra.domain.model.CompletedWorkout
import id.ac.ub.athra.domain.model.Exercise
import id.ac.ub.athra.domain.model.ExerciseReference
import id.ac.ub.athra.domain.model.PersonalRecord
import id.ac.ub.athra.domain.model.PersonalRecordStatus
import id.ac.ub.athra.domain.model.SessionStatus
import id.ac.ub.athra.domain.model.WeightUnit
import id.ac.ub.athra.domain.model.WorkoutDraft
import id.ac.ub.athra.domain.model.WorkoutExercise
import id.ac.ub.athra.domain.model.WorkoutSession
import id.ac.ub.athra.domain.model.WorkoutSet
import id.ac.ub.athra.domain.time.WorkoutClock
import id.ac.ub.athra.ui.auth.AuthScreen
import id.ac.ub.athra.ui.auth.AuthUiState
import id.ac.ub.athra.ui.screens.EditProfileScreen
import id.ac.ub.athra.ui.screens.HistoryScreen
import id.ac.ub.athra.ui.screens.HomeScreen
import id.ac.ub.athra.ui.screens.PreferencesScreen
import id.ac.ub.athra.ui.screens.PreWorkoutScreen
import id.ac.ub.athra.ui.screens.ProfileScreen
import id.ac.ub.athra.ui.screens.ProgressScreen
import id.ac.ub.athra.ui.screens.WorkoutDetailScreen
import id.ac.ub.athra.ui.theme.AthraTheme
import id.ac.ub.athra.ui.workout.ExerciseDetailsScreen
import id.ac.ub.athra.ui.workout.ExerciseFiltersScreen
import id.ac.ub.athra.ui.workout.ExerciseLibraryScreen
import id.ac.ub.athra.ui.workout.QuickStartScreen
import id.ac.ub.athra.ui.workout.WorkoutFlowScreen
import id.ac.ub.athra.ui.workout.WorkoutSummaryScreen
import id.ac.ub.athra.ui.workout.WorkoutUiState
import java.io.File
import java.math.BigDecimal
import java.time.Instant
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Renders production composables with local fixtures, without Firebase or database setup. */
class FigmaScreenCaptureTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val unit = WeightUnit.KG
    private val now = Instant.parse("2026-10-13T12:42:18Z").toEpochMilli()
    private val day = 86_400_000L
    private val pullUp = Exercise("pull_up", "Pull up", "Back · Biceps", "Bodyweight", "Tarik tubuh hingga dagu melewati palang. Turunkan tubuh dengan terkontrol.")
    private val deadlift = Exercise("deadlift", "Barbell Deadlift", "Posterior chain", "Barbell", "Jaga punggung netral. Dorong lantai dengan kaki dan angkat beban dekat tubuh.")
    private val catalog = listOf(
        pullUp,
        Exercise("lat_pulldown", "Lat pulldown", "Back", "Cable", "Tarik pegangan ke dada bagian atas dengan gerakan terkontrol."),
        Exercise("barbell_row", "Barbell row", "Back", "Barbell", "Tekuk pinggul dan tarik barbell ke arah pinggang."),
        Exercise("bench_press", "Bench press", "Chest", "Barbell", "Turunkan barbell ke dada dan dorong kembali ke atas."),
        deadlift,
        Exercise("squat", "Squat", "Legs", "Barbell", "Turunkan pinggul sambil menjaga punggung tetap netral."),
    )
    private val sets = listOf(
        WorkoutSet("set-1", 1, 12, BigDecimal.ZERO),
        WorkoutSet("set-2", 2, 10, BigDecimal("5")),
        WorkoutSet("set-3", 3, 8, BigDecimal("10")),
    )
    private val active = WorkoutDraft(
        WorkoutSession("active", "fixture-user", "Evening Pull Day", now - 2_538_000L, null, SessionStatus.ACTIVE),
        listOf(WorkoutExercise(pullUp, 0, sets), WorkoutExercise(deadlift, 1, emptyList())),
    )
    private val review = active.copy(
        session = active.session.copy(id = "review", endedAt = now, status = SessionStatus.REVIEW),
        exercises = listOf(
            WorkoutExercise(pullUp, 0, sets),
            WorkoutExercise(deadlift, 1, listOf(
                WorkoutSet("deadlift-1", 1, 10, BigDecimal("60")),
                WorkoutSet("deadlift-2", 2, 8, BigDecimal("70")),
                WorkoutSet("deadlift-3", 3, 8, BigDecimal("75")),
            )),
        ),
    )
    private val records = listOf(PersonalRecord(deadlift.id, BigDecimal("70"), BigDecimal("75"), PersonalRecordStatus.NEW_RECORD))
    private val recent = listOf(
        WorkoutSession("history-1", "fixture-user", "Evening Pull Day", now - 4_320_000L, now, SessionStatus.COMPLETED),
        WorkoutSession("history-2", "fixture-user", "Morning Push Day", now - day - 3_600_000L, now - day, SessionStatus.COMPLETED),
        WorkoutSession("history-3", "fixture-user", "Lower Body Strength", now - 3 * day - 3_900_000L, now - 3 * day, SessionStatus.COMPLETED),
    )
    private val analytics = AnalyticsSummary(
        startEpochMs = now - 6 * day,
        endEpochMs = now,
        sessionCount = 18,
        setCount = 72,
        repCount = 684,
        volumeKg = BigDecimal("214800"),
        favoriteExercise = pullUp,
        favoriteSessionCount = 8,
        dailyVolumeKg = listOf("18000", "23000", "20000", "30000", "27000", "36000", "33000").mapIndexed { index, volume ->
            (now - (6 - index) * day) to BigDecimal(volume)
        },
    )
    private val reference = ExerciseReference(recent[1], sets, BigDecimal("130"))
    private val state = WorkoutUiState(
        draft = active,
        catalog = catalog,
        catalogLoading = false,
        draftLoading = false,
        loading = false,
        personalRecords = records,
        selectedExerciseId = pullUp.id,
        exerciseReferences = mapOf(pullUp.id to reference),
    )

    @Before
    fun edgeToEdge() {
        compose.activityRule.scenario.onActivity { it.enableEdgeToEdge() }
    }

    private fun capture(name: String, content: @Composable () -> Unit) {
        compose.setContent { AthraTheme(content) }
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(100)
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val directory = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "ui-rebuild")
        check(directory.isDirectory || directory.mkdirs()) { "Cannot create screenshot directory" }
        File(directory, "$name.png").outputStream().use { stream ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) { "Cannot write screenshot $name" }
        }
    }

    @Test
    fun capture01Home() = capture("01-home") {
        HomeScreen(
            userName = "Widodo", draft = null, recentSessions = recent.take(1),
            analytics = analytics.copy(sessionCount = 4, setCount = 12, volumeKg = BigDecimal("24800")),
        )
    }

    @Test
    fun capture02PreWorkout() = capture("02-pre-workout") { PreWorkoutScreen() }

    @Test
    fun capture03ActiveWorkout() = capture("03-active-workout") {
        WorkoutFlowScreen(
            draft = active, state = state,
            onSaveSet = { _, _, _, _, _, _ -> true }, onDeleteSet = { _, _ -> },
            onAddExercise = { _, _ -> }, onRemoveExercise = { _, _ -> },
            onFinish = { true }, onResume = {}, onSave = { true }, onDiscard = {}, onExit = {},
            clock = WorkoutClock { now },
        )
    }

    @Test
    fun capture04Library() = capture("04-exercise-library") {
        ExerciseLibraryScreen(
            state = state, onSearchChange = {}, onOpenFilters = {}, onOpenDetails = {},
            onBack = {}, onRetry = {}, onNavigate = {},
        )
    }

    @Test
    fun capture05Summary() = capture("05-workout-summary") {
        WorkoutSummaryScreen(review, state.copy(draft = review), unit, onResume = {}, onSave = {}, onDiscard = {})
    }

    @Test
    fun capture06Progress() = capture("06-progress") {
        ProgressScreen(analytics, 30, unit, onPeriodChange = {}, onNavigate = {})
    }

    @Test
    fun capture07Profile() = capture("07-profile") {
        ProfileScreen(
            "Widodo", "widodo@example.com", analytics, unit,
            loading = false, error = null, onRetry = {}, onNavigate = {}, onEdit = {},
            onPreferences = {}, onHistory = {}, onSignOut = {},
        )
    }

    @Test
    fun capture00Login() = capture("00-login") {
        AuthScreen(AuthUiState(restoring = false), onSignIn = { _, _ -> }, onSignUp = { _, _, _, _ -> })
    }

    @Test
    fun capture08History() = capture("08-history") { HistoryScreen(recent, onNavigate = {}, onOpen = {}) }

    @Test
    fun capture09EditProfile() = capture("09-edit-profile") {
        EditProfileScreen("Widodo", "widodo@example.com", null, onBack = {}, onSave = {})
    }

    @Test
    fun capture10Preferences() = capture("10-preferences") { PreferencesScreen(unit, onBack = {}, onSave = {}) }

    @Test
    fun capture11QuickStart() = capture("11-quick-start") {
        QuickStartScreen("Evening Pull Day", busy = false, error = null, onNameChange = {}, onStart = {}, onBack = {})
    }

    @Test
    fun capture12Filters() = capture("12-filters") {
        ExerciseFiltersScreen("Back", onSelect = {}, onReset = {}, onApply = {}, onBack = {})
    }

    @Test
    fun capture13ExerciseDetail() = capture("13-exercise-detail") {
        ExerciseDetailsScreen(pullUp, reference, unit, busy = false, error = null, onAdd = {}, onBack = {})
    }

    @Test
    fun capture14SessionDetail() = capture("14-session-detail") {
        WorkoutDetailScreen(CompletedWorkout(review.copy(session = review.session.copy(status = SessionStatus.COMPLETED)), records), unit, onBack = {})
    }
}
