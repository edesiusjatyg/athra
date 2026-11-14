package id.ac.ub.athra

import android.app.Application
import androidx.room.Room
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import id.ac.ub.athra.data.local.AppDatabase

class AthraApplication : Application() {
    lateinit var database: AppDatabase
        private set
    var firebaseAuth: FirebaseAuth? = null
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(this, AppDatabase::class.java, "athra.db").build()

        val projectId = BuildConfig.FIREBASE_PROJECT_ID
        val appId = BuildConfig.FIREBASE_APP_ID
        val apiKey = BuildConfig.FIREBASE_API_KEY
        if (projectId.isNotBlank() && appId.isNotBlank() && apiKey.isNotBlank()) {
            val options = FirebaseOptions.Builder()
                .setProjectId(projectId)
                .setApplicationId(appId)
                .setApiKey(apiKey)
                .build()
            val app = FirebaseApp.initializeApp(this, options)
                ?: FirebaseApp.getInstance()
            firebaseAuth = FirebaseAuth.getInstance(app).also { auth ->
                if (BuildConfig.AUTH_EMULATOR_ENABLED) {
                    auth.useEmulator(BuildConfig.AUTH_EMULATOR_HOST, 9099)
                }
            }
        }
    }
}
