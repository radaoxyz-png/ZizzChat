package com.zizzverse.zizzchat

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.text.InputType
import android.widget.*
import com.google.firebase.auth.FirebaseAuth

// ==========================================
// LOGIN PAGE ACTIVITY
// এই ফাইল শুধু Login Page-এর জন্য
// ==========================================

class LoginActivity : Activity() {

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

    // 📧 EMAIL INPUT FIELD
    private lateinit var emailInput: EditText

    // 🔒 PASSWORD INPUT FIELD
    private lateinit var passwordInput: EditText

    // 🔵 LOGIN BUTTON
    private lateinit var loginButton: TextView

    // 👁️ PASSWORD SHOW/HIDE BUTTON STATE
    private var passwordVisible = false

    // 📏 DP CONVERTER
    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    // 🎨 ROUNDED CORNER DESIGN
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
                dp(14),
                dp(16),
                dp(14)
            )

            background = rounded(card, 14)
        }
    }

    // 🔵 BLUE GRADIENT BUTTON DESIGN
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
                    Color.rgb(37, 99, 235),
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

    // 🖥️ LOGIN PAGE UI DESIGN
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🎨 STATUS BAR / NAVIGATION BAR COLOR
        window.statusBarColor = bg
        window.navigationBarColor = bg

        // 🔐 ALREADY LOGGED IN → HOME PAGE
        if (auth.currentUser != null) {
            openHomePage()
            return
        }

        // 📱 MAIN LOGIN PAGE LAYOUT
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

        setContentView(root)

        // 🟦 APP LOGO DESIGN
        val logo = makeText(
            "Z",
            38f,
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
                cornerRadius = dp(24).toFloat()
            }
        }

        val logoRow = LinearLayout(this).apply {
            gravity = Gravity.CENTER

            addView(
                logo,
                LinearLayout.LayoutParams(
                    dp(76),
                    dp(76)
                )
            )
        }

        root.addView(logoRow)

        addGap(root, 24)

        // 🏷️ LOGIN PAGE TITLE
        root.addView(
            makeText(
                "Welcome Back",
                30f,
                white,
                true
            ).apply {
                gravity = Gravity.CENTER
            }
        )

        addGap(root, 6)

        // 📝 LOGIN PAGE SUBTITLE
        root.addView(
            makeText(
                "Sign in to continue to ZizzChat",
                14f,
                muted
            ).apply {
                gravity = Gravity.CENTER
            }
        )

        addGap(root, 30)

        // 📧 EMAIL LABEL
        root.addView(
            makeText(
                "Email address",
                13f,
                muted
            )
        )

        addGap(root, 8)

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

        addGap(root, 18)

        // 🔒 PASSWORD LABEL
        root.addView(
            makeText(
                "Password",
                13f,
                muted
            )
        )

        addGap(root, 8)

        // 🔒 PASSWORD INPUT FIELD
        passwordInput = makeInput(
            "Enter your password",
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

        // 👁️ SHOW / HIDE PASSWORD BUTTON
        val passwordToggle = makeText(
            "Show password",
            13f,
            Color.rgb(96, 165, 250)
        ).apply {
            gravity = Gravity.END

            setPadding(
                0,
                dp(12),
                0,
                dp(12)
            )

            setOnClickListener {
                passwordVisible = !passwordVisible

                passwordInput.inputType =
                    if (passwordVisible) {
                        InputType.TYPE_CLASS_TEXT or
                            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    } else {
                        InputType.TYPE_CLASS_TEXT or
                            InputType.TYPE_TEXT_VARIATION_PASSWORD
                    }

                passwordInput.setSelection(
                    passwordInput.text.length
                )

                text = if (passwordVisible) {
                    "Hide password"
                } else {
                    "Show password"
                }
            }
        }

        root.addView(passwordToggle)

        addGap(root, 10)

        // 🔵 LOGIN BUTTON
        loginButton = makeButton("Log In") {
            loginWithFirebase()
        }

        root.addView(
            loginButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        addGap(root, 14)

        // 📝 OPEN SIGN-UP PAGE BUTTON
        val signupLink = makeText(
            "Don't have an account?  Create Account",
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
                openSignUpPage()
            }
        }

        root.addView(signupLink)

        addGap(root, 18)

        // 🏷️ FOOTER TEXT
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

    // 🔥 FIREBASE LOGIN FUNCTION
    private fun loginWithFirebase() {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString()

        // ⚠️ INPUT VALIDATION
        if (email.isBlank() || password.length < 6) {
            Toast.makeText(
                this,
                "Enter a valid email and password (6+ characters)",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // ⏳ LOGIN BUTTON LOADING STATE
        loginButton.text = "Logging in..."
        loginButton.isEnabled = false

        // 🔐 FIREBASE EMAIL / PASSWORD SIGN-IN
        auth.signInWithEmailAndPassword(
            email,
            password
        )
            .addOnSuccessListener {
                // ✅ LOGIN SUCCESS → HOME PAGE
                openHomePage()
            }
            .addOnFailureListener { error ->
                // ❌ LOGIN ERROR
                loginButton.text = "Log In"
                loginButton.isEnabled = true

                Toast.makeText(
                    this,
                    error.localizedMessage ?: "Login failed",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // 📝 OPEN SIGN-UP PAGE
    private fun openSignUpPage() {
        startActivity(
            Intent(
                this,
                SignUpActivity::class.java
            )
        )
    }

    // 🏠 OPEN HOME PAGE AFTER SUCCESSFUL LOGIN
    private fun openHomePage() {
        startActivity(
            Intent(
                this,
                HomeActivity::class.java
            )
        )

        finish()
    }
}
