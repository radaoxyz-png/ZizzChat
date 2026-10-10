
package com.zizzverse.zizzchat.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.zizzverse.zizzchat.data.model.User
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    val currentUser get() = auth.currentUser

    // Signup Function
    suspend fun signup(name: String, email: String, pass: String): Result<Boolean> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, pass).await()
            val userId = result.user?.uid ?: throw Exception("User ID is null")

            // Save user info to Firestore
            val user = User(
                uid = userId,
                name = name,
                email = email,
                isOnline = true
            )
            firestore.collection("users").document(userId).set(user).await()

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Login Function
    suspend fun login(email: String, pass: String): Result<Boolean> {
        return try {
            auth.signInWithEmailAndPassword(email, pass).await()
            val userId = auth.currentUser?.uid
            if (userId != null) {
                // Update online status
                firestore.collection("users").document(userId).update("isOnline", true).await()
            }
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Logout Function
    fun logout() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            firestore.collection("users").document(userId).update("isOnline", false)
        }
        auth.signOut()
    }
}
