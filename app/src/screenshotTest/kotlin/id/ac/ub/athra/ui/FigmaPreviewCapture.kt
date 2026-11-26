package id.ac.ub.athra.ui

import androidx.compose.runtime.Composable
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
import java.math.BigDecimal
import java.time.Instant

/** Renders production composables with local fixtures, without Firebase or database setup. */
class FigmaPreviewCapture {
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

    @Composable
    private fun frame(content: @Composable () -> Unit) { AthraTheme(content) }

    @com.android.tools.screenshot.PreviewTest
    @androidx.compose.ui.tooling.preview.Preview(widthDp = 390, heightDp = 844, showSystemUi = true, apiLevel = 36)
    @Composable
    fun capture01Home() = frame {
        HomeScreen(
            userName = "Widodo", draft = null, recentSessions = recent.take(1),
            analytics = analytics.copy(sessionCount = 4, setCount = 12, volumeKg = BigDecimal("24800")),
        )
    }

    @com.android.tools.screenshot.PreviewTest
    @androidx.compose.ui.tooling.preview.Preview(widthDp = 390, heightDp = 844, showSystemUi = true, apiLevel = 36)
    @Composable
    fun capture02PreWorkout() = frame { PreWorkoutScreen() }

    @com.android.tools.screenshot.PreviewTest
    @androidx.compose.ui.tooling.preview.Preview(widthDp = 390, heightDp = 844, showSystemUi = true, apiLevel = 36)
    @Composable
    fun capture03ActiveWorkout() = frame {
        WorkoutFlowScreen(
            draft = active, state = state,
            onSaveSet = { _, _, _, _, _, _ -> true }, onDeleteSet = { _, _ -> },
            onAddExercise = { _, _ -> }, onRemoveExercise = { _, _ -> },
            onFinish = { true }, onResume = {}, onSave = { true }, onDiscard = {}, onExit = {},
            clock = WorkoutClock { now },
        )
    }

    @com.android.tools.screenshot.PreviewTest
    @androidx.compose.ui.tooling.preview.Preview(widthDp = 390, heightDp = 844, showSystemUi = true, apiLevel = 36)
    @Composable
    fun capture04Library() = frame {
        ExerciseLibraryScreen(
            state = state, onSearchChange = {}, onOpenFilters = {}, onOpenDetails = {},
            onBack = {}, onRetry = {}, onNavigate = {},
        )
    }

    @com.android.tools.screenshot.PreviewTest
    @androidx.compose.ui.tooling.preview.Preview(widthDp = 390, heightDp = 844, showSystemUi = true, apiLevel = 36)
    @Composable
    fun capture05Summary() = frame {
        WorkoutSummaryScreen(review, state.copy(draft = review), unit, onResume = {}, onSave = {}, onDiscard = {})
    }

    @com.android.tools.screenshot.PreviewTest
    @androidx.compose.ui.tooling.preview.Preview(widthDp = 390, heightDp = 844, showSystemUi = true, apiLevel = 36)
    @Composable
    fun capture06Progress() = frame {
        ProgressScreen(analytics, 30, unit, onPeriodChange = {}, onNavigate = {})
    }

    @com.android.tools.screenshot.PreviewTest
    @androidx.compose.ui.tooling.preview.Preview(widthDp = 390, heightDp = 844, showSystemUi = true, apiLevel = 36)
    @Composable
    fun capture07Profile() = frame {
        ProfileScreen(
            "Widodo", "widodo@example.com", analytics, unit,
            loading = false, error = null, onRetry = {}, onNavigate = {}, onEdit = {},
            onPreferences = {}, onHistory = {}, onSignOut = {},
        )
    }

    @com.android.tools.screenshot.PreviewTest
    @androidx.compose.ui.tooling.preview.Preview(widthDp = 390, heightDp = 844, showSystemUi = true, apiLevel = 36)
    @Composable
    fun capture00Login() = frame {
        AuthScreen(AuthUiState(restoring = false), onSignIn = { _, _ -> }, onSignUp = { _, _, _, _ -> })
    }

    @com.android.tools.screenshot.PreviewTest
    @androidx.compose.ui.tooling.preview.Preview(widthDp = 390, heightDp = 844, showSystemUi = true, apiLevel = 36)
    @Composable
    fun capture08History() = frame { HistoryScreen(recent, onNavigate = {}, onOpen = {}) }

    @com.android.tools.screenshot.PreviewTest
    @androidx.compose.ui.tooling.preview.Preview(widthDp = 390, heightDp = 844, showSystemUi = true, apiLevel = 36)
    @Composable
    fun capture09EditProfile() = frame {
        EditProfileScreen("Widodo", "widodo@example.com", null, onBack = {}, onSave = {})
    }

    @com.android.tools.screenshot.PreviewTest
    @androidx.compose.ui.tooling.preview.Preview(widthDp = 390, heightDp = 844, showSystemUi = true, apiLevel = 36)
    @Composable
    fun capture10Preferences() = frame { PreferencesScreen(unit, onBack = {}, onSave = {}) }

    @com.android.tools.screenshot.PreviewTest
    @androidx.compose.ui.tooling.preview.Preview(widthDp = 390, heightDp = 844, showSystemUi = true, apiLevel = 36)
    @Composable
    fun capture11QuickStart() = frame {
        QuickStartScreen("Evening Pull Day", busy = false, error = null, onNameChange = {}, onStart = {}, onBack = {})
    }

    @com.android.tools.screenshot.PreviewTest
    @androidx.compose.ui.tooling.preview.Preview(widthDp = 390, heightDp = 844, showSystemUi = true, apiLevel = 36)
    @Composable
    fun capture12Filters() = frame {
        ExerciseFiltersScreen("Back", onSelect = {}, onReset = {}, onApply = {}, onBack = {})
    }

    @com.android.tools.screenshot.PreviewTest
    @androidx.compose.ui.tooling.preview.Preview(widthDp = 390, heightDp = 844, showSystemUi = true, apiLevel = 36)
    @Composable
    fun capture13ExerciseDetail() = frame {
        ExerciseDetailsScreen(pullUp, reference, unit, busy = false, error = null, onAdd = {}, onBack = {})
    }

    @com.android.tools.screenshot.PreviewTest
    @androidx.compose.ui.tooling.preview.Preview(widthDp = 390, heightDp = 844, showSystemUi = true, apiLevel = 36)
    @Composable
    fun capture14SessionDetail() = frame {
        WorkoutDetailScreen(CompletedWorkout(review.copy(session = review.session.copy(status = SessionStatus.COMPLETED)), records), unit, onBack = {})
    }
}
