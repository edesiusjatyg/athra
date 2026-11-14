package id.ac.ub.athra.ui.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import id.ac.ub.athra.data.auth.AuthRepository
import id.ac.ub.athra.data.local.AppDatabase
import id.ac.ub.athra.data.repository.WorkoutRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthValidationTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var database: AppDatabase
    private lateinit var viewModelStore: ViewModelStore

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(AppDatabase.validationCallback)
            .allowMainThreadQueries()
            .build()
        viewModelStore = ViewModelStore()
    }

    @After
    fun tearDown() {
        viewModelStore.clear()
        database.close()
    }

    @Test
    fun signInShowsFieldSpecificErrors() {
        compose.setContent {
            AuthScreen(
                state = AuthUiState(
                    restoring = false,
                    emailError = "Masukkan email yang valid.",
                    passwordError = "Password wajib diisi.",
                ),
                onSignIn = { _, _ -> },
                onSignUp = { _, _, _, _ -> },
            )
        }

        compose.onNodeWithText("Masukkan email yang valid.").assertIsDisplayed()
        compose.onNodeWithText("Password wajib diisi.").assertIsDisplayed()
    }

    @Test
    fun signUpShowsErrorsForEachInvalidField() {
        compose.setContent {
            AuthScreen(
                state = AuthUiState(
                    restoring = false,
                    nameError = "Nama wajib diisi.",
                    emailError = "Masukkan email yang valid.",
                    passwordError = "Password minimal 6 karakter.",
                    confirmationError = "Konfirmasi password belum cocok.",
                ),
                onSignIn = { _, _ -> },
                onSignUp = { _, _, _, _ -> },
                startInRegistration = true,
            )
        }

        compose.onNodeWithText("Nama wajib diisi.").assertIsDisplayed()
        compose.onNodeWithText("Masukkan email yang valid.").assertIsDisplayed()
        compose.onNodeWithText("Password minimal 6 karakter.").assertIsDisplayed()
        compose.onNodeWithText("Konfirmasi password belum cocok.").assertIsDisplayed()
    }

    @Test
    fun busySubmitCannotDispatchAuthCallbacks() {
        var submitCalls = 0
        compose.setContent {
            AuthScreen(
                state = AuthUiState(restoring = false, busy = true),
                onSignIn = { _, _ -> submitCalls++ },
                onSignUp = { _, _, _, _ -> submitCalls++ },
            )
        }

        compose.onNodeWithText("Memproses…").assertIsNotEnabled().performClick()
        compose.runOnIdle { assertEquals(0, submitCalls) }
    }

    @Test
    fun invalidSignInStopsBeforeAuthRepositoryRequest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val viewModel = ViewModelProvider(
            viewModelStore,
            AuthViewModelFactory(
                AuthRepository(null),
                WorkoutRepository(database, context.assets),
            ),
        )[AuthViewModel::class.java]
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        viewModel.signIn("invalid-email", "")
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        assertEquals("Masukkan email yang valid.", viewModel.state.value.emailError)
        assertEquals("Password wajib diisi.", viewModel.state.value.passwordError)
        assertNull("Invalid input must not surface the missing-Firebase error", viewModel.state.value.error)
        assertTrue(!viewModel.state.value.busy)
    }
}
