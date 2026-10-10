package com.zizzverse.zizzchat

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth

// ==========================================
// 🚀 MAIN ACTIVITY — APP STARTING POINT
// ==========================================

class MainActivity : Activity() {

    // 🔥 FIREBASE LOGIN STATUS
    private val auth by lazy {
        FirebaseAuth.getInstance()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔐 USER LOGIN করা থাকলে Home Page
        if (auth.currentUser != null) {
            startActivity(
                Intent(this, HomeActivity::class.java)
            )
        } else {
            // 🔓 LOGIN না থাকলে Login Page
            startActivity(
                Intent(this, LoginActivity::class.java)
            )
        }

        // 🧹 MainActivity-তে ফিরে আসা বন্ধ
        finish()
    }
}
