package com.zizzverse.zizzchat

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.text.InputType
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class MainActivity : Activity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private val bg = Color.rgb(10, 16, 32)
    private val card = Color.rgb(22, 32, 53)
    private val blue = Color.rgb(37, 99, 235)
    private val blueLight = Color.rgb(96, 165, 250)
    private val muted = Color.rgb(154, 170, 196)
    private val white = Color.WHITE
    private val green = Color.rgb(52, 211, 153)

    private lateinit var root: LinearLayout
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var confirmInput: EditText
    private lateinit var messages: LinearLayout
    private lateinit var messageInput: EditText

    private var messageListener: ListenerRegistration? = null
    private var showPassword = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )

        if (auth.currentUser != null) showChat() else showLogin()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun shape(
        color: Int,
        radius: Int,
        strokeColor: Int = color
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (strokeColor != color) {
                setStroke(dp(1), strokeColor)
            }
        }
    }

    private fun gradientButton(): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(
                Color.rgb(37, 99, 235),
                Color.rgb(79, 70, 229)
            )
        ).apply {
            cornerRadius = dp(15).toFloat()
        }
    }

    private fun label(
        value: String,
        size: Float,
        color: Int = white,
        bold: Boolean = false
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(null, Typeface.BOLD)
        }
    }

    private fun makeRoot() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(14), dp(22), dp(12))
            setBackgroundColor(bg)
        }
        setContentView(root)
    }

    private fun params() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )

    private fun gap(height: Int) {
        root.addView(
            View(this),
            LinearLayout.LayoutParams(1, dp(height))
        )
    }

    private fun makeInput(
        hintText: String,
        type: Int
    ): EditText {
        return EditText(this).apply {
            hint = hintText
            setHintTextColor(muted)
            setTextColor(white)
            textSize = 15f
            inputType = type
            setSingleLine(true)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = shape(card, 15, Color.rgb(44, 59, 83))
        }
    }

    private fun makeButton(
        textValue: String,
        action: () -> Unit
    ): TextView {
        return TextView(this).apply {
            text = textValue
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(white)
            setTypeface(null, Typeface.BOLD)
            background = gradientButton()
            setPadding(dp(16), dp(16), dp(16), dp(16))
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
        }
    }

    private fun addLogo() {
        val logo = TextView(this).apply {
            text = "Z"
            textSize = 38f
            gravity = Gravity.CENTER
            setTextColor(white)
            setTypeface(null, Typeface.BOLD)
            background = gradientButton()
            elevation = dp(8).toFloat()
        }

        val logoRow = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            addView(logo, LinearLayout.LayoutParams(dp(76), dp(76)))
        }

        root.addView(logoRow)
    }

    private fun addLink(
        textValue: String,
        action: () -> Unit
    ) {
        val link = label(textValue, 14f, blueLight)
        link.gravity = Gravity.CENTER
        link.setPadding(0, dp(16), 0, dp(16))
        link.setOnClickListener { action() }
        root.addView(link, params())
    }

    private fun showLogin() {
        messageListener?.remove()
        showPassword = false
        makeRoot()

        root.addView(
            View(this),
            LinearLayout.LayoutParams(1, 0, 1f)
        )

        addLogo()
        gap(22)

        root.addView(
            label("Welcome Back", 30f, white, true).apply {
                gravity = Gravity.CENTER
            },
            params()
        )

        gap(6)

        root.addView(
            label("Sign in to continue to ZizzChat", 14f, muted).apply {
                gravity = Gravity.CENTER
            },
            params()
        )

        gap(34)

        root.addView(label("Email address", 13f, muted), params())
        gap(7)

        emailInput = makeInput(
            "Enter your email",
            InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        )
        root.addView(emailInput, params())

        gap(18)

        root.addView(label("Password", 13f, muted), params())
        gap(7)

        passwordInput = makeInput(
            "Enter your password",
            InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_PASSWORD
        )
        root.addView(passwordInput, params())

        addLink("Show / hide password") {
            showPassword = !showPassword
            passwordInput.inputType =
                if (showPassword) {
                    InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                } else {
                    InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_VARIATION_PASSWORD
                }
            passwordInput.setSelection(passwordInput.text.length)
        }

        root.addView(
            makeButton("Log In") { authenticate(false) },
            params()
        )

        gap(10)

        addLink("Don't have an account?  Create Account") {
            showCreateAccount()
        }

        root.addView(
            label("PRIVATE MESSAGES  •  REAL CONNECTIONS", 10f, muted).apply {
                gravity = Gravity.CENTER
            },
            params()
        )

        root.addView(
            View(this),
            LinearLayout.LayoutParams(1, 0, 1f)
        )
    }

    private fun showCreateAccount() {
        messageListener?.remove()
        showPassword = false
        makeRoot()

        root.addView(
            View(this),
            LinearLayout.LayoutParams(1, 0, 1f)
        )

        addLogo()
        gap(20)

        root.addView(
            label("Create Account", 29f, white, true).apply {
                gravity = Gravity.CENTER
            },
            params()
        )

        gap(6)

        root.addView(
            label("Your new conversations start here", 14f, muted).apply {
                gravity = Gravity.CENTER
            },
            params()
        )

        gap(28)

        root.addView(label("Email address", 13f, muted), params())
        gap(7)

        emailInput = makeInput(
            "Enter your email",
            InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        )
        root.addView(emailInput, params())

        gap(16)

        root.addView(label("Password", 13f, muted), params())
        gap(7)

        passwordInput = makeInput(
            "At least 6 characters",
            InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_PASSWORD
        )
        root.addView(passwordInput, params())

        gap(16)

        root.addView(label("Confirm password", 13f, muted), params())
        gap(7)

        confirmInput = makeInput(
            "Re-enter your password",
            InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_PASSWORD
        )
        root.addView(confirmInput, params())

        gap(24)

        root.addView(
            makeButton("Create Account") {
                val password = passwordInput.text.toString()
                val confirm = confirmInput.text.toString()

                if (password != confirm) {
                    toast("Passwords do not match")
                } else {
                    authenticate(true)
                }
            },
            params()
        )

        addLink("Already have an account?  Log In") {
            showLogin()
        }

        root.addView(
            View(this),
            LinearLayout.LayoutParams(1, 0, 1f)
        )
    }

    private fun authenticate(create: Boolean) {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString()

        if (email.isBlank() || password.length < 6) {
            toast("Enter your email and a password of at least 6 characters")
            return
        }

        val task = if (create) {
            auth.createUserWithEmailAndPassword(email, password)
        } else {
            auth.signInWithEmailAndPassword(email, password)
        }

        task.addOnSuccessListener {
            showChat()
        }.addOnFailureListener {
            toast(it.localizedMessage ?: "Authentication failed")
        }
    }

    private fun showChat() {
        messageListener?.remove()
        makeRoot()

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(6), 0, dp(16))
        }

        val logo = TextView(this).apply {
            text = "Z"
            textSize = 25f
            gravity = Gravity.CENTER
            setTextColor(white)
            setTypeface(null, Typeface.BOLD)
            background = gradientButton()
        }

        header.addView(logo, LinearLayout.LayoutParams(dp(50), dp(50)))

        val titleBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }

        titleBox.addView(label("ZizzChat", 21f, white, true))
        titleBox.addView(
            label("●  General Chat", 12f, green)
        )

        header.addView(
            titleBox,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        val logout = TextView(this).apply {
            text = "Logout"
            textSize = 12f
            setTextColor(blueLight)
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(10), dp(10), dp(10))
            setOnClickListener {
                auth.signOut()
                messageListener?.remove()
                showLogin()
            }
        }

        header.addView(logout)
        root.addView(header)

        val divider = View(this).apply {
            setBackgroundColor(Color.rgb(39, 52, 76))
        }
        root.addView(
            divider,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(1)
            )
        )

        val welcome = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, dp(18), 0, dp(18))
        }

        welcome.addView(
            label("Welcome to ZizzChat 👋", 17f, white, true)
        )
        welcome.addView(
            label("Say hello and start a conversation", 12f, muted)
        )

        root.addView(welcome)

        val scroll = ScrollView(this).apply {
            isFillViewport = true
        }

        messages = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(8))
        }

        scroll.addView(messages)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        val composer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(12), 0, dp(4))
        }

        messageInput = EditText(this).apply {
            hint = "Write a message..."
            setHintTextColor(muted)
            setTextColor(white)
            textSize = 14f
            maxLines = 3
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            imeOptions = EditorInfo.IME_ACTION_SEND
            setPadding(dp(16), dp(10), dp(16), dp(10))
            background = shape(card, 25)

            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    sendMessage()
                    true
                } else {
                    false
                }
            }
        }

        composer.addView(
            messageInput,
            LinearLayout.LayoutParams(0, dp(52), 1f)
        )

        val send = TextView(this).apply {
            text = "➤"
            textSize = 23f
            gravity = Gravity.CENTER
            setTextColor(white)
            background = gradientButton()
            setOnClickListener { sendMessage() }
        }

        val sendParams = LinearLayout.LayoutParams(dp(52), dp(52))
        sendParams.setMargins(dp(10), 0, 0, 0)
        composer.addView(send, sendParams)

        root.addView(composer)
        listenForMessages()
    }

    private fun sendMessage() {
        val user = auth.currentUser ?: return
        val value = messageInput.text.toString().trim()

        if (value.isEmpty()) return

        val data = hashMapOf(
            "uid" to user.uid,
            "email" to (user.email ?: ""),
            "text" to value,
            "createdAt" to FieldValue.serverTimestamp()
        )

        db.collection("rooms")
            .document("general")
            .collection("messages")
            .add(data)
            .addOnSuccessListener {
                messageInput.text.clear()
            }
            .addOnFailureListener {
                toast(it.localizedMessage ?: "Message could not be sent")
            }
    }

    private fun listenForMessages() {
        messageListener = db.collection("rooms")
            .document("general")
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener(this) { snapshot, error ->

                if (error != null) {
                    toast(error.localizedMessage ?: "Unable to load messages")
                    return@addSnapshotListener
                }

                messages.removeAllViews()

                snapshot?.documents?.forEach { doc ->
                    val uid = doc.getString("uid") ?: ""
                    val email = doc.getString("email") ?: "User"
                    val body = doc.getString("text") ?: ""
                    val mine = uid == auth.currentUser?.uid

                    val bubble = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(dp(14), dp(10), dp(14), dp(10))
                        background = shape(
                            if (mine) blue else card,
                            18
                        )
                    }

                    if (!mine) {
                        bubble.addView(
                            label(email, 11f, blueLight, true)
                        )
                    }

                    bubble.addView(label(body, 15f, white))

                    val row = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = if (mine) Gravity.END else Gravity.START
                        setPadding(0, dp(4), 0, dp(4))
                    }

                    val bubbleParams = LinearLayout.LayoutParams(
                        (resources.displayMetrics.widthPixels * 0.70f).toInt(),
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )

                    row.addView(bubble, bubbleParams)
                    messages.addView(row, params())
                }
            }
    }

    private fun toast(value: String) {
        Toast.makeText(this, value, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        messageListener?.remove()
        super.onDestroy()
    }
}
