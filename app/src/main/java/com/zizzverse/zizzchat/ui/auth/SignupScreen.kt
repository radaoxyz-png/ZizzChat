
package com.zizzverse.zizzchat.ui.auth

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun SignupScreen(navController: NavController) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Create your account",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password (minimum 6 characters)") },
            visualTransformation =
                PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))

        Button(
            enabled = !loading,
            onClick = {
                val cleanName = name.trim()
                val cleanEmail = email.trim().lowercase()

                when {
                    cleanName.isBlank() ||
                    cleanEmail.isBlank() ||
                    password.isBlank() -> {
                        Toast.makeText(
                            context,
                            "Fill in all fields",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    password.length < 6 -> {
                        Toast.makeText(
                            context,
                            "Password must be at least 6 characters",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    else -> {
                        loading = true

                        auth.createUserWithEmailAndPassword(
                            cleanEmail,
                            password
                        ).addOnSuccessListener { result ->

                            val user = result.user

                            if (user == null) {
                                loading = false
                                Toast.makeText(
                                    context,
                                    "Account creation failed",
                                    Toast.LENGTH_LONG
                                ).show()
                                return@addOnSuccessListener
                            }

                            val profile = hashMapOf(
                                "uid" to user.uid,
                                "name" to cleanName,
                                "email" to cleanEmail,
                                "profilePic" to "",
                                "status" to
                                    "Hey there! I am using ZizzChat",
                                "isOnline" to true,
                                "lastSeen" to
                                    System.currentTimeMillis()
                            )

                            db.collection("users")
                                .document(user.uid)
                                .set(profile)
                                .addOnSuccessListener {
                                    loading = false

                                    navController.navigate("home") {
                                        popUpTo("login") {
                                            inclusive = true
                                        }
                                        launchSingleTop = true
                                    }
                                }
                                .addOnFailureListener { error ->
                                    loading = false
                                    Toast.makeText(
                                        context,
                                        "Profile save failed: " +
                                            (error.localizedMessage ?: ""),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        }.addOnFailureListener { error ->
                            loading = false
                            Toast.makeText(
                                context,
                                error.localizedMessage
                                    ?: "Sign up failed",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Sign Up")
            }
        }

        Spacer(Modifier.height(12.dp))

        TextButton(
            enabled = !loading,
            onClick = { navController.popBackStack() }
        ) {
            Text("Already have an account? Log in")
        }
    }
}
