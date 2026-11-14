package id.ac.ub.athra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import id.ac.ub.athra.ui.auth.AuthScreen
import id.ac.ub.athra.ui.auth.AuthViewModel
import id.ac.ub.athra.ui.auth.AuthViewModelFactory
import id.ac.ub.athra.ui.navigation.AthraNavGraph
import id.ac.ub.athra.ui.theme.AthraTheme
import id.ac.ub.athra.ui.workout.WorkoutViewModel
import id.ac.ub.athra.ui.workout.WorkoutViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as AthraApplication).container
        val authViewModel = ViewModelProvider(
            this,
            AuthViewModelFactory(container.authRepository, container.workoutRepository),
        )[AuthViewModel::class.java]
        setContent {
            AthraTheme {
                val state by authViewModel.state.collectAsState()
                when {
                    state.restoring -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    state.user == null -> AuthScreen(state, authViewModel::signIn, authViewModel::signUp)
                    else -> {
                        val user = state.user!!
                        val workoutViewModel = remember(user.uid) {
                            ViewModelProvider(
                                this@MainActivity,
                                WorkoutViewModelFactory(user.uid, container.workoutRepository),
                            ).get("workout_${user.uid}", WorkoutViewModel::class.java)
                        }
                        val workoutState by workoutViewModel.state.collectAsState()
                        val navController = rememberNavController()
                        AthraNavGraph(
                            navController = navController,
                            userName = user.displayName.ifBlank { user.email.substringBefore('@') },
                            workoutState = workoutState,
                            workoutViewModel = workoutViewModel,
                            onStartWorkout = { workoutViewModel.start() },
                            onResumeWorkout = { workoutViewModel.refreshDraft() },
                            onSignOut = authViewModel::signOut,
                        )
                    }
                }
            }
        }
    }
}
