package id.ac.ub.athra.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavHostController
import id.ac.ub.athra.ui.screens.HomeScreen
import id.ac.ub.athra.ui.screens.LiveWorkoutScreen
import id.ac.ub.athra.ui.screens.PreWorkoutScreen

@Composable
fun AthraNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.Home.route,
    ) {
        composable(Routes.Home.route) {
            HomeScreen(
                onStartWorkout = { navController.navigate(Routes.PreWorkout.route) },
                onViewAll = { },
            )
        }
        composable(Routes.PreWorkout.route) {
            PreWorkoutScreen(
                onBack = { navController.popBackStack() },
                onBeginSession = { navController.navigate(Routes.LiveWorkout.route) },
            )
        }
        composable(Routes.LiveWorkout.route) {
            LiveWorkoutScreen(
                onBack = { navController.popBackStack() },
                onFinishWorkout = {
                    navController.popBackStack(
                        route = Routes.Home.route,
                        inclusive = false,
                    )
                },
            )
        }
    }
}