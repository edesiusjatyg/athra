package id.ac.ub.athra

import android.app.Application
import androidx.room.Room
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import id.ac.ub.athra.data.local.AppDatabase
import id.ac.ub.athra.data.auth.AuthRepository
import id.ac.ub.athra.data.preferences.UnitPreferences
import id.ac.ub.athra.data.repository.WorkoutRepository

class AthraApplication : Application() {
    lateinit var database: AppDatabase
        private set
    var firebaseAuth: FirebaseAuth? = null
        private set
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(this, AppDatabase::class.java, "athra.db")
            .addCallback(AppDatabase.validationCallback)
            .build()

        val projectId = BuildConfig.FIREBASE_PROJECT_ID
        val appId = BuildConfig.FIREBASE_APP_ID
        val apiKey = BuildConfig.FIREBASE_API_KEY
        val emulatorEnabled = BuildConfig.DEBUG && BuildConfig.AUTH_EMULATOR_ENABLED
        if (projectId.isNotBlank() && appId.isNotBlank() && apiKey.isNotBlank()) {
            val options = FirebaseOptions.Builder()
                .setProjectId(projectId)
                .setApplicationId(appId)
                .setApiKey(apiKey)
                .build()
            val app = FirebaseApp.initializeApp(this, options)
                ?: FirebaseApp.getInstance()
            firebaseAuth = FirebaseAuth.getInstance(app).also { auth ->
                if (emulatorEnabled) {
                    auth.useEmulator(BuildConfig.AUTH_EMULATOR_HOST, 9099)
                }
            }
        }
        container = AppContainer(
            authRepository = AuthRepository(firebaseAuth),
            workoutRepository = WorkoutRepository(database, assets),
            unitPreferences = UnitPreferences(this),
        )
    }
}

data class AppContainer(
    val authRepository: AuthRepository,
    val workoutRepository: WorkoutRepository,
    val unitPreferences: UnitPreferences,
)
