package com.zizzverse.zizzchat

//========== IMPORTS START ==========
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.zizzverse.zizzchat.ui.auth.LoginScreen
import com.zizzverse.zizzchat.ui.auth.SignupScreen
import com.zizzverse.zizzchat.ui.chat.ChatScreen
import com.zizzverse.zizzchat.ui.home.HomeScreen
//========== IMPORTS END ==========


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ZizzChatApp()
        }
    }
}


//========== APP UI START ==========
@Composable
fun ZizzChatApp() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            PresenceTracker()
            AppNavigation()
        }
    }
}
//========== APP UI END ==========


//========== REAL PRESENCE TRACKER START ==========
@Composable
private fun PresenceTracker() {

    val lifecycleOwner = LocalLifecycleOwner.current

    val auth = remember {
        FirebaseAuth.getInstance()
    }

    val db = remember {
        FirebaseFirestore.getInstance()
    }

    val currentUid = remember {
        mutableStateOf(auth.currentUser?.uid)
    }

    DisposableEffect(auth, db, lifecycleOwner) {

        var observedUid = auth.currentUser?.uid

        //========== PRESENCE UPDATE FUNCTION START ==========
        fun updatePresence(uid: String?, online: Boolean) {
            if (uid.isNullOrBlank()) return

            val profileRef = db.collection("users")
                .document(uid)

            if (online) {
                profileRef.update(
                    mapOf("isOnline" to true)
                )
            } else {
                profileRef.update(
                    mapOf(
                        "isOnline" to false,
                        "lastSeen" to System.currentTimeMillis()
                    )
                )
            }
        }
        //========== PRESENCE UPDATE FUNCTION END ==========


        //========== AUTH STATE LISTENER START ==========
        val authListener =
            FirebaseAuth.AuthStateListener { firebaseAuth ->

                val newUid = firebaseAuth.currentUser?.uid

                if (observedUid != newUid) {
                    updatePresence(observedUid, false)
                    observedUid = newUid
                    currentUid.value = newUid
                }

                val appIsVisible =
                    lifecycleOwner.lifecycle.currentState
                        .isAtLeast(Lifecycle.State.STARTED)

                if (appIsVisible) {
                    updatePresence(newUid, true)
                }
            }

        auth.addAuthStateListener(authListener)
        //========== AUTH STATE LISTENER END ==========


        //========== APP LIFECYCLE LISTENER START ==========
        val lifecycleObserver =
            LifecycleEventObserver { _, event ->

                when (event) {
                    Lifecycle.Event.ON_START -> {
                        updatePresence(observedUid, true)
                    }

                    Lifecycle.Event.ON_STOP -> {
                        updatePresence(observedUid, false)
                    }

                    else -> Unit
                }
            }

        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        //========== APP LIFECYCLE LISTENER END ==========


        onDispose {
            lifecycleOwner.lifecycle.removeObserver(
                lifecycleObserver
            )

            auth.removeAuthStateListener(authListener)
        }
    }
}
//========== REAL PRESENCE TRACKER END ==========


//========== NAVIGATION START ==========
@Composable
fun AppNavigation() {

    val navController = rememberNavController()

    val currentUser = FirebaseAuth.getInstance().currentUser

    val startDestination =
        if (currentUser != null) "home" else "login"

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {

        composable("login") {
            LoginScreen(navController)
        }

        composable("signup") {
            SignupScreen(navController)
        }

        composable("home") {
            HomeScreen(navController)
        }

        composable(
            route = "chat/{userId}",
            arguments = listOf(
                navArgument("userId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->

            val userId =
                backStackEntry.arguments?.getString("userId")

            ChatScreen(navController, userId)
        }
    }
}
//========== NAVIGATION END ==========
