package id.ac.ub.athra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import id.ac.ub.athra.ui.navigation.AthraNavGraph
import id.ac.ub.athra.ui.theme.AthraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AthraTheme {
                val navController = rememberNavController()
                AthraNavGraph(navController = navController)
            }
        }
    }
}