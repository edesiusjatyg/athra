package id.ac.ub.athra.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import id.ac.ub.athra.domain.model.SessionStatus
import id.ac.ub.athra.domain.model.WeightUnit
import id.ac.ub.athra.domain.model.WorkoutSession
import id.ac.ub.athra.ui.auth.AuthScreen
import id.ac.ub.athra.ui.auth.AuthUiState
import id.ac.ub.athra.ui.screens.HistoryScreen
import id.ac.ub.athra.ui.screens.PreferencesScreen
import id.ac.ub.athra.ui.screens.ProgressScreen
import id.ac.ub.athra.ui.screens.PreWorkoutScreen
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
}
