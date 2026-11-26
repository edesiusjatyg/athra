package id.ac.ub.athra.ui.auth

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import id.ac.ub.athra.AthraApplication
import id.ac.ub.athra.BuildConfig
import id.ac.ub.athra.data.auth.AuthRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AuthEmulatorIntegrationTest {
    @Test
    fun applicationInitializesAuthFromTheCurrentBuildConfiguration() {
        assumeTrue(BuildConfig.DEBUG && BuildConfig.AUTH_EMULATOR_ENABLED)
        val application = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as AthraApplication

        // Regression: BuildConfig used to say "enabled" while stale inlined empty
        // strings in AthraApplication left its FirebaseAuth null after a flag change.
        assertNotNull("FirebaseAuth must initialize for an emulator-enabled APK", application.firebaseAuth)
        assertEquals(BuildConfig.FIREBASE_PROJECT_ID, application.firebaseAuth!!.app.options.projectId)
        assertEquals(BuildConfig.FIREBASE_APP_ID, application.firebaseAuth!!.app.options.applicationId)
        assertEquals(BuildConfig.FIREBASE_API_KEY, application.firebaseAuth!!.app.options.apiKey)
    }

    @Test
    fun signUpProfileAndSignInWorkAgainstTheAuthEmulator() = runBlocking {
        assumeTrue(BuildConfig.DEBUG && BuildConfig.AUTH_EMULATOR_ENABLED)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val application = context.applicationContext as AthraApplication
        assertNotNull(application.firebaseAuth)
        // A named FirebaseApp keeps the user's default-app session untouched.
        val testApp = FirebaseApp.initializeApp(context, application.firebaseAuth!!.app.options, "auth-test-${UUID.randomUUID()}")
        val auth = FirebaseAuth.getInstance(testApp)
        auth.useEmulator(BuildConfig.AUTH_EMULATOR_HOST, 9099)
        val repository = AuthRepository(auth)
        val email = "auth-regression-${UUID.randomUUID()}@example.test"
        val password = "Athra-local-test-123"
        try {
            val created = repository.signUp("Atlet Uji", email, password)
            assertEquals(email, created.email)
            assertEquals("Atlet Uji", created.displayName)
            repository.signOut()
            val signedIn = repository.signIn(email, password)
            assertEquals(created.uid, signedIn.uid)
            assertEquals("Atlet Uji", signedIn.displayName)
        } finally {
            auth.signOut()
            testApp.delete()
        }
    }
}
