package id.ac.ub.athra.ui.auth

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

private object AuthRoutes {
    const val SIGN_IN = "auth/sign_in"
    const val SIGN_UP = "auth/sign_up"
}

@Composable
fun AuthNavigation(
    state: AuthUiState,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String, String, String) -> Unit,
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = AuthRoutes.SIGN_IN) {
        composable(AuthRoutes.SIGN_IN) {
            AuthScreen(
                state = state,
                onSignIn = onSignIn,
                onSignUp = onSignUp,
                startInRegistration = false,
                onSwitchMode = { registering ->
                    if (registering) navController.navigate(AuthRoutes.SIGN_UP) {
                        popUpTo(AuthRoutes.SIGN_IN) { inclusive = false }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(AuthRoutes.SIGN_UP) {
            AuthScreen(
                state = state,
                onSignIn = onSignIn,
                onSignUp = onSignUp,
                startInRegistration = true,
                onSwitchMode = { registering ->
                    if (!registering) navController.popBackStack(AuthRoutes.SIGN_IN, inclusive = false)
                },
            )
        }
    }
}
