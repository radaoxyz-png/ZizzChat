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

// ==========================================
// SIGN UP ACTIVITY — নতুন অ্যাকাউন্ট তৈরি
// ==========================================

class SignUpActivity : Activity() {

    // 🔥 FIREBASE AUTHENTICATION
    private val auth by lazy {
        FirebaseAuth.getInstance()
    }

    // 🎨 DESIGN COLORS
    private val bg = Color.rgb(10, 16, 32)
    private val card = Color.rgb(22, 32, 53)
    private val blue = Color.rgb(37, 99, 235)
    private val white = Color.WHITE
    private val muted = Color.rgb(154, 170, 196)

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

    // 🎨 ROUNDED BACKGROUND DESIGN
    private fun rounded(
        color: Int,
        radius: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

    // 📝 TEXT DESIGN
    private fun makeText(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean = false
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)

            if (bold) {
                setTypeface(null, Typeface.BOLD)
            }
        }
    }

    // 📧 EMAIL / PASSWORD INPUT DESIGN
    private fun makeInput(
        hintText: String,
        inputTypeValue: Int
    ): EditText {
        return EditText(this).apply {
            hint = hintText
            setHintTextColor(muted)
            setTextColor(white)
            textSize = 15f
            inputType = inputTypeValue
            setSingleLine(true)

            setPadding(
                dp(16),
                dp(13),
                dp(16),
                dp(13)
            )

            background = rounded(card, 14)
        }
    }

    // 🔵 BUTTON DESIGN
    private fun makeButton(
        value: String,
        action: () -> Unit
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(white)
            setTypeface(null, Typeface.BOLD)

            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(
                    blue,
                    Color.rgb(79, 70, 229)
                )
            ).apply {
                cornerRadius = dp(15).toFloat()
            }

            setPadding(
                dp(14),
                dp(16),
                dp(14),
                dp(16)
            )

            // BUTTON CLICK ACTION
            setOnClickListener {
                action()
            }
        }
    }

    // 📏 GAP / SPACING
    private fun addGap(
        parent: LinearLayout,
        height: Int
    ) {
        parent.addView(
            View(this),
            LinearLayout.LayoutParams(
                1,
                dp(height)
            )
        )
    }

    // 🖥️ SIGN UP PAGE UI
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🎨 SYSTEM BAR COLORS
        window.statusBarColor = bg
        window.navigationBarColor = bg

        // 📱 MAIN PAGE LAYOUT
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL

            setPadding(
                dp(24),
                dp(20),
                dp(24),
                dp(20)
            )

            setBackgroundColor(bg)
        }

        // 📜 SCROLL VIEW — ছোট স্ক্রিনে UI স্ক্রল হবে
        val scrollView = ScrollView(this).apply {
            fillViewport = true
            isVerticalScrollBarEnabled = false
            addView(root)
        }

        setContentView(scrollView)

        // 🟦 APP LOGO
        val logo = makeText(
            "Z",
            34f,
            white,
            true
        ).apply {
            gravity = Gravity.CENTER

            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(
                    blue,
                    Color.rgb(79, 70, 229)
                )
            ).apply {
                cornerRadius = dp(22).toFloat()
            }
        }

        // LOGO CENTER ALIGNMENT
        val logoRow = LinearLayout(this).apply {
            gravity = Gravity.CENTER

            addView(
                logo,
                LinearLayout.LayoutParams(
                    dp(68),
                    dp(68)
                )
            )
        }

        root.addView(logoRow)

        addGap(root, 20)

        // 🏷️ PAGE TITLE
        root.addView(
            makeText(
                "Create Account",
                28f,
                white,
                true
            ).apply {
                gravity = Gravity.CENTER
            }
        )

        addGap(root, 6)

        // 📝 PAGE SUBTITLE
        root.addView(
            makeText(
                "Join ZizzChat and start connecting",
                14f,
                muted
            ).apply {
                gravity = Gravity.CENTER
            }
        )

        addGap(root, 26)

        // 📧 EMAIL LABEL
        root.addView(
            makeText(
                "Email address",
                13f,
                muted
            )
        )

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

        addGap(root, 16)

        // 🔒 PASSWORD LABEL
        root.addView(
            makeText(
                "Password",
                13f,
                muted
            )
        )

        addGap(root, 7)

        // 🔒 PASSWORD INPUT FIELD
        passwordInput = makeInput(
            "Create a password (6+ characters)",
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

        addGap(root, 16)

        // 🔒 CONFIRM PASSWORD LABEL
        root.addView(
            makeText(
                "Confirm password",
                13f,
                muted
            )
        )

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

        addGap(root, 24)

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

        addGap(root, 16)

        // 🔙 BACK TO LOGIN BUTTON
        val loginLink = makeText(
            "Already have an account?  Log In",
            14f,
            Color.rgb(96, 165, 250)
        ).apply {
            gravity = Gravity.CENTER

            setPadding(
                0,
                dp(12),
                0,
                dp(12)
            )

            setOnClickListener {
                // LOGIN PAGE-এ ফিরে যাবে
                finish()
            }
        }

        root.addView(loginLink)

        addGap(root, 16)

        // 🏷️ FOOTER
        root.addView(
            makeText(
                "PRIVATE MESSAGES  •  REAL CONNECTIONS",
                10f,
                muted
            ).apply {
                gravity = Gravity.CENTER
            }
        )
    }

    // 🔥 FIREBASE ACCOUNT CREATION
    private fun createAccount() {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString()
        val confirmPassword =
            confirmPasswordInput.text.toString()

        // ⚠️ EMAIL VALIDATION
        if (
            email.isBlank() ||
            !android.util.Patterns.EMAIL_ADDRESS
                .matcher(email)
                .matches()
        ) {
            emailInput.error = "Enter a valid email"
            emailInput.requestFocus()
            return
        }

        // ⚠️ PASSWORD LENGTH CHECK
        if (password.length < 6) {
            passwordInput.error =
                "Password must be at least 6 characters"
            passwordInput.requestFocus()
            return
        }

        // ⚠️ CONFIRM PASSWORD CHECK
        if (password != confirmPassword) {
            confirmPasswordInput.error =
                "Passwords do not match"
            confirmPasswordInput.requestFocus()
            return
        }

        // ⏳ LOADING STATE
        signupButton.isEnabled = false
        signupButton.text = "Creating account..."

        // 🔐 CREATE USER IN FIREBASE AUTHENTICATION
        auth.createUserWithEmailAndPassword(
            email,
            password
        )
            .addOnSuccessListener {
                // ✅ ACCOUNT CREATED — OPEN HOME PAGE
                Toast.makeText(
                    this,
                    "Account created successfully!",
                    Toast.LENGTH_SHORT
                ).show()

                startActivity(
                    Intent(
                        this,
                        HomeActivity::class.java
                    )
                )

                finish()
            }
            .addOnFailureListener { error ->
                // ❌ ACCOUNT CREATION ERROR
                signupButton.isEnabled = true
                signupButton.text = "Create Account"

                Toast.makeText(
                    this,
                    error.localizedMessage
                        ?: "Account creation failed",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}
