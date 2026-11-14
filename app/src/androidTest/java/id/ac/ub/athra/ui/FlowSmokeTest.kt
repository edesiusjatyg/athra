package id.ac.ub.athra.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import id.ac.ub.athra.ui.auth.AuthScreen
import id.ac.ub.athra.ui.auth.AuthUiState
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
}
