package com.example.athra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.athra.ui.navigation.AthraNavGraph
import com.example.athra.ui.theme.AthraTheme

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