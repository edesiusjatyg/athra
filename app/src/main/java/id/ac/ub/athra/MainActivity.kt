package id.ac.ub.athra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
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
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
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
                    state.user != null && state.error != null -> Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(24.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
                        Text("Data akun belum siap dimuat.")
                        Text(state.error.orEmpty())
                        Button(onClick = authViewModel::retryLocalSetup) { Text("Coba lagi") }
                        Button(onClick = authViewModel::signOut) { Text("Keluar") }
                    }
                    state.user == null -> AuthNavigation(
                        state,
                        authViewModel::signIn,
                        authViewModel::signUp,
                        authViewModel::clearError,
                    )
                    else -> {
                        val user = state.user!!
                        key(user.uid) {
                        val workoutStoreOwner = remember(user.uid) { UserWorkoutStoreOwner() }
                        DisposableEffect(workoutStoreOwner) {
                            onDispose { workoutStoreOwner.clear() }
                        }
                        val workoutFactory = remember(user.uid) {
                            WorkoutViewModelFactory(user.uid, container.workoutRepository)
                        }
                        val workoutGraphFactory = remember(user.uid) {
                            WorkoutViewModelFactory(user.uid, container.workoutRepository, savedStateEnabled = true)
                        }
                        val workoutViewModel = remember(user.uid) {
                            ViewModelProvider(
                                workoutStoreOwner,
                                workoutFactory,
                            ).get("workout", WorkoutViewModel::class.java)
                        }
                        val workoutState by workoutViewModel.state.collectAsStateWithLifecycle()
                        val unit by remember(user.uid) { container.unitPreferences.observe(user.uid) }
                            .collectAsStateWithLifecycle(initialValue = WeightUnit.KG)
                        val navController = rememberNavController()
                        CompositionLocalProvider(LocalViewModelStoreOwner provides workoutStoreOwner) {
                            AthraNavGraph(
                                navController = navController,
                                userId = user.uid,
                                database = athraApplication.database,
                                userName = workoutState.profileName.ifBlank { user.displayName.ifBlank { user.email.substringBefore('@') } },
                                workoutState = workoutState,
                                workoutViewModel = workoutViewModel,
                                workoutViewModelFactory = workoutGraphFactory,
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
    }
}

private class UserWorkoutStoreOwner : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()

    fun clear() = viewModelStore.clear()
}
