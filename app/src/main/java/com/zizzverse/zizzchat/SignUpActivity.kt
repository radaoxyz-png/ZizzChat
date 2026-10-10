package com.zizzverse.zizzchat

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// ==========================================
// 📝 SIGN UP — ACCOUNT + USERNAME SYSTEM
// ==========================================

class SignUpActivity : Activity() {

    // 🔥 FIREBASE AUTHENTICATION
    private val auth by lazy { FirebaseAuth.getInstance() }

    // 🔥 FIRESTORE DATABASE
    private val db by lazy { FirebaseFirestore.getInstance() }

    // 🎨 UI COLORS
    private val bg = Color.rgb(10, 16, 32)
    private val card = Color.rgb(22, 32, 53)
    private val blue = Color.rgb(37, 99, 235)
    private val white = Color.WHITE
    private val muted = Color.rgb(154, 170, 196)

    // 👤 USERNAME INPUT
    private lateinit var usernameInput: EditText

    // 📧 EMAIL INPUT
    private lateinit var emailInput: EditText

    // 🔒 PASSWORD INPUT
    private lateinit var passwordInput: EditText

    // 🔒 CONFIRM PASSWORD INPUT
    private lateinit var confirmPasswordInput: EditText

    // 🔵 CREATE ACCOUNT BUTTON
    private lateinit var signupButton: TextView

    // 📏 DP CONVERTER
    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    // 🎨 ROUNDED BACKGROUND
    private fun rounded(color: Int, radius: Int) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }

    // 📝 TEXT DESIGN
    private fun makeText(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean = false
    ) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (bold) setTypeface(null, Typeface.BOLD)
    }

    // ✏️ INPUT FIELD DESIGN
    private fun makeInput(
        hintText: String,
        type: Int
    ) = EditText(this).apply {
        hint = hintText
        setHintTextColor(muted)
        setTextColor(white)
        textSize = 15f
        inputType = type
        setSingleLine(true)
        setPadding(dp(16), dp(12), dp(16), dp(12))
        background = rounded(card, 14)
    }

    // 🔵 BUTTON DESIGN
    private fun makeButton(
        value: String,
        action: () -> Unit
    ) = TextView(this).apply {
        text = value
        textSize = 16f
        gravity = Gravity.CENTER
        setTextColor(white)
        setTypeface(null, Typeface.BOLD)

        background = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(blue, Color.rgb(79, 70, 229))
        ).apply {
            cornerRadius = dp(15).toFloat()
        }

        setPadding(dp(14), dp(16), dp(14), dp(16))

        // 🔘 BUTTON CLICK
        setOnClickListener { action() }
    }

    // 📏 VERTICAL SPACING
    private fun addGap(parent: LinearLayout, height: Int) {
        parent.addView(
            View(this),
            LinearLayout.LayoutParams(1, dp(height))
        )
    }

    // 🖥️ SIGN UP PAGE UI
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = bg
        window.navigationBarColor = bg

        // 📱 MAIN LAYOUT
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(24), dp(20), dp(24), dp(20))
            setBackgroundColor(bg)
        }

        // 📜 SCROLL VIEW FOR SMALL SCREENS
        val scroll = ScrollView(this).apply {
            setFillViewport(true)
            isVerticalScrollBarEnabled = false
            addView(root)
        }

        setContentView(scroll)

        // 🟦 APP LOGO
        val logo = makeText("Z", 34f, white, true).apply {
            gravity = Gravity.CENTER
            background = rounded(blue, 22)
        }

        val logoRow = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            addView(logo, LinearLayout.LayoutParams(dp(68), dp(68)))
        }

        root.addView(logoRow)
        addGap(root, 18)

        // 🏷️ PAGE TITLE
        root.addView(
            makeText("Create Account", 28f, white, true).apply {
                gravity = Gravity.CENTER
            }
        )

        addGap(root, 6)

        // 📝 PAGE SUBTITLE
        root.addView(
            makeText("Join ZizzChat and meet your friends", 14f, muted).apply {
                gravity = Gravity.CENTER
            }
        )

        addGap(root, 24)

        // 👤 USERNAME LABEL
        root.addView(makeText("Username", 13f, muted))
        addGap(root, 7)

        // 👤 USERNAME INPUT FIELD
        usernameInput = makeInput(
            "Choose a username",
            InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_NORMAL
        )

        root.addView(
            usernameInput,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        addGap(root, 15)

        // 📧 EMAIL LABEL
        root.addView(makeText("Email address", 13f, muted))
        addGap(root, 7)

        // 📧 EMAIL INPUT FIELD
        emailInput = makeInput(
            "Enter your email",
            InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        )

        root.addView(
            emailInput,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        addGap(root, 15)

        // 🔒 PASSWORD LABEL
        root.addView(makeText("Password", 13f, muted))
        addGap(root, 7)

        // 🔒 PASSWORD INPUT FIELD
        passwordInput = makeInput(
            "At least 6 characters",
            InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_PASSWORD
        )

        root.addView(
            passwordInput,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        addGap(root, 15)

        // 🔒 CONFIRM PASSWORD LABEL
        root.addView(makeText("Confirm password", 13f, muted))
        addGap(root, 7)

        // 🔒 CONFIRM PASSWORD INPUT FIELD
        confirmPasswordInput = makeInput(
            "Enter password again",
            InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_PASSWORD
        )

        root.addView(
            confirmPasswordInput,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        addGap(root, 22)

        // 🔵 CREATE ACCOUNT BUTTON
        signupButton = makeButton("Create Account") {
            createAccount()
        }

        root.addView(
            signupButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        addGap(root, 14)

        // 🔙 BACK TO LOGIN
        root.addView(
            makeText(
                "Already have an account?  Log In",
                14f,
                Color.rgb(96, 165, 250)
            ).apply {
                gravity = Gravity.CENTER
                setPadding(0, dp(12), 0, dp(12))
                setOnClickListener { finish() }
            }
        )
    }

    // 🔥 CREATE ACCOUNT + SAVE USER PROFILE
    private fun createAccount() {
        val username = usernameInput.text.toString()
            .trim().removePrefix("@")

        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString()
        val confirm = confirmPasswordInput.text.toString()

        // ⚠️ USERNAME VALIDATION
        if (!Regex("^[A-Za-z0-9_]{3,20}$").matches(username)) {
            usernameInput.error =
                "Use 3-20 letters, numbers or underscores"
            usernameInput.requestFocus()
            return
        }

        // ⚠️ EMAIL VALIDATION
        if (!android.util.Patterns.EMAIL_ADDRESS
                .matcher(email).matches()) {
            emailInput.error = "Enter a valid email"
            emailInput.requestFocus()
            return
        }

        // ⚠️ PASSWORD VALIDATION
        if (password.length < 6) {
            passwordInput.error = "Minimum 6 characters"
            passwordInput.requestFocus()
            return
        }

        // ⚠️ CONFIRM PASSWORD VALIDATION
        if (password != confirm) {
            confirmPasswordInput.error = "Passwords do not match"
            confirmPasswordInput.requestFocus()
            return
        }

        signupButton.isEnabled = false
        signupButton.text = "Creating account..."

        // 🔥 CREATE FIREBASE AUTH USER
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->

                val user = result.user

                if (user == null) {
                    signupButton.isEnabled = true
                    signupButton.text = "Create Account"
                    Toast.makeText(
                        this,
                        "Could not create user",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addOnSuccessListener
                }

                // 👤 PROFILE DATA FOR FRIEND SEARCH
                val profile = hashMapOf(
                    "uid" to user.uid,
                    "username" to username,
                    "usernameLower" to username.lowercase(),
                    "email" to email.lowercase(),
                    "displayName" to username,
                    "createdAt" to System.currentTimeMillis()
                )

                // 🔥 SAVE PROFILE IN FIRESTORE
                // Collection: users / Document ID: Firebase UID
                db.collection("users")
                    .document(user.uid)
                    .set(profile)
                    .addOnSuccessListener {

                        // ✅ PROFILE SAVED — OPEN HOME
                        Toast.makeText(
                            this,
                            "Account created!",
                            Toast.LENGTH_SHORT
                        ).show()

                        startActivity(
                            Intent(this, HomeActivity::class.java)
                        )
                        finish()
                    }
                    .addOnFailureListener { error ->

                        // ⚠️ AUTH ACCOUNT CREATED, PROFILE SAVE FAILED
                        // Keep user signed in; retry profile save later.
                        signupButton.isEnabled = true
                        signupButton.text = "Retry Profile Save"

                        Toast.makeText(
                            this,
                            "Account created, but profile save failed: " +
                                (error.localizedMessage ?: "Unknown error"),
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { error ->
                // ❌ ACCOUNT CREATION ERROR
                signupButton.isEnabled = true
                signupButton.text = "Create Account"

                Toast.makeText(
                    this,
                    error.localizedMessage ?: "Sign up failed",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}
