package com.zizzverse.zizzchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ZizzChatApp()
        }
    }
}

@Composable
fun ZizzChatApp() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AppNavigation()
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // Screen Routes Setup
    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            // LoginScreen(navController)
        }
        composable("signup") {
            // SignupScreen(navController)
        }
        composable("home") {
            // HomeScreen(navController)
        }
        composable("chat/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")
            // ChatScreen(navController, userId)
        }
        composable("profile") {
            // ProfileScreen(navController)
        }
    }
}
