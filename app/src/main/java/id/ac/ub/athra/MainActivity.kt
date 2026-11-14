package id.ac.ub.athra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import id.ac.ub.athra.ui.auth.AuthNavigation
import id.ac.ub.athra.ui.auth.AuthViewModel
import id.ac.ub.athra.ui.auth.AuthViewModelFactory
import id.ac.ub.athra.ui.navigation.AthraNavGraph
import id.ac.ub.athra.ui.theme.AthraTheme
import id.ac.ub.athra.ui.workout.WorkoutViewModel
import id.ac.ub.athra.ui.workout.WorkoutViewModelFactory
import id.ac.ub.athra.domain.model.WeightUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val athraApplication = application as AthraApplication
        val container = athraApplication.container
        val authViewModel = ViewModelProvider(
            this,
            AuthViewModelFactory(container.authRepository, container.workoutRepository),
        )[AuthViewModel::class.java]
        setContent {
            AthraTheme {
                val state by authViewModel.state.collectAsStateWithLifecycle()
                when {
                    state.restoring -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    state.user != null && state.error != null -> Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
                        Text("Data akun belum siap dimuat.")
                        Text(state.error.orEmpty())
                        Button(onClick = authViewModel::retryLocalSetup) { Text("Coba lagi") }
                        Button(onClick = authViewModel::signOut) { Text("Keluar") }
                    }
                    state.user == null -> AuthNavigation(state, authViewModel::signIn, authViewModel::signUp)
                    else -> {
                        val user = state.user!!
                        val workoutFactory = remember(user.uid) {
                            WorkoutViewModelFactory(user.uid, container.workoutRepository)
                        }
                        val workoutViewModel = remember(user.uid) {
                            ViewModelProvider(
                                this@MainActivity,
                                workoutFactory,
                            ).get("workout_${user.uid}", WorkoutViewModel::class.java)
                        }
                        val workoutState by workoutViewModel.state.collectAsStateWithLifecycle()
                        val unit by remember(user.uid) { container.unitPreferences.observe(user.uid) }
                            .collectAsStateWithLifecycle(initialValue = WeightUnit.KG)
                        val navController = rememberNavController()
                        AthraNavGraph(
                            navController = navController,
                            userId = user.uid,
                            database = athraApplication.database,
                            userName = workoutState.profileName.ifBlank { user.displayName.ifBlank { user.email.substringBefore('@') } },
                            workoutState = workoutState,
                            workoutViewModel = workoutViewModel,
                            workoutViewModelFactory = workoutFactory,
                            onResumeWorkout = { workoutViewModel.refreshDraft() },
                            onSignOut = authViewModel::signOut,
                            userEmail = user.email,
                            weightUnit = unit,
                            onSaveUnit = { next -> container.unitPreferences.set(user.uid, next) },
                        )
                    }
                }
            }
        }
    }
}
