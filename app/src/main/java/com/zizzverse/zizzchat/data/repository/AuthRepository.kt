package com.zizzverse.zizzchat.data.repository

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()

    val currentUser get() = auth.currentUser

    // Login function with proper error handling
    suspend fun loginUser(email: String, password: String): Result<Boolean> {
        return try {
            if (email.isBlank() || password.isBlank()) {
                return Result.failure(Exception("Email and password cannot be empty"))
            }
            auth.signInWithEmailAndPassword(email, password).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Signup function
    suspend fun signupUser(email: String, password: String): Result<Boolean> {
        return try {
            if (email.isBlank() || password.isBlank()) {
                return Result.failure(Exception("Email and password cannot be empty"))
            }
            auth.createUserWithEmailAndPassword(email, password).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Logout
    fun logout() {
        auth.signOut()
    }
}
