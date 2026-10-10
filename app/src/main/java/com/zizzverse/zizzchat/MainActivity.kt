package com.zizzverse.zizzchat

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import android.text.InputType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ListenerRegistration

class MainActivity : Activity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private val bg = Color.rgb(10, 16, 32)
    private val panel = Color.rgb(21, 31, 53)
    private val blue = Color.rgb(37, 99, 235)
    private val white = Color.WHITE
    private val muted = Color.rgb(156, 174, 202)

    private lateinit var root: LinearLayout
    private lateinit var messages: LinearLayout
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var messageInput: EditText
    private var messageListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.setSoftInputMode(
            android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )

        if (auth.currentUser != null) showChat() else showLogin()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radius: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun makeRoot() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(12), dp(18), dp(12))
            setBackgroundColor(bg)
        }
        setContentView(root)
    }

    private fun text(
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

    private fun input(
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
            setPadding(dp(15), dp(12), dp(15), dp(12))
            background = rounded(panel, 14)
        }
    }

    private fun button(
        label: String,
        action: () -> Unit
    ): TextView {
        return TextView(this).apply {
            text = label
            textSize = 16f
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
            setTextColor(white)
            background = rounded(blue, 14)
            setPadding(dp(14), dp(15), dp(14), dp(15))
            setOnClickListener { action() }
        }
    }

    private fun addGap(height: Int) {
        root.addView(View(this), LinearLayout.LayoutParams(1, dp(height)))
    }

    private fun showLogin() {
        messageListener?.remove()
        makeRoot()

        val spacer = View(this)
        root.addView(
            spacer,
            LinearLayout.LayoutParams(1, 0, 1f)
        )

        val logo = TextView(this).apply {
            text = "Z"
            textSize = 38f
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
            setTextColor(white)
            background = rounded(blue, 24)
        }

        val logoRow = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            addView(logo, LinearLayout.LayoutParams(dp(78), dp(78)))
        }
        root.addView(logoRow)

        addGap(20)

        val title = text("ZizzChat", 32f, white, true)
        title.gravity = Gravity.CENTER
        root.addView(title)

        val subtitle = text(
            "Connect with your friends",
            15f,
            muted
        )
        subtitle.gravity = Gravity.CENTER
        root.addView(subtitle)

        addGap(34)

        emailInput = input(
            "Email address",
            InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        )
        root.addView(emailInput, matchParams())

        addGap(12)

        passwordInput = input(
            "Password (6+ characters)",
            InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_PASSWORD
        )
        root.addView(passwordInput, matchParams())

        addGap(22)

        root.addView(
            button("Log In") { authenticate(false) },
            matchParams()
        )

        addGap(10)

        val create = TextView(this).apply {
            text = "Create a new account"
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(96, 165, 250))
            setPadding(10, dp(14), 10, dp(14))
            setOnClickListener { authenticate(true) }
        }
        root.addView(create, matchParams())

        val bottom = text("Private conversations. Real connections.", 12f, muted)
        bottom.gravity = Gravity.CENTER
        root.addView(bottom)

        root.addView(
            View(this),
            LinearLayout.LayoutParams(1, 0, 1f)
        )
    }

    private fun matchParams() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )

    private fun authenticate(create: Boolean) {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString()

        if (email.isBlank() || password.length < 6) {
            toast("Enter a valid email and password (6+ characters).")
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

        val avatar = TextView(this).apply {
            text = "Z"
            textSize = 24f
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
            setTextColor(white)
            background = rounded(blue, 18)
        }
        header.addView(avatar, LinearLayout.LayoutParams(dp(48), dp(48)))

        val titleBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }

        titleBox.addView(text("ZizzChat", 22f, white, true))
        titleBox.addView(text("●  General Chat", 12f, Color.rgb(52, 211, 153)))

        header.addView(
            titleBox,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        val logout = TextView(this).apply {
            text = "⋮"
            textSize = 30f
            gravity = Gravity.CENTER
            setTextColor(white)
            setPadding(dp(12), 0, dp(12), 0)
            setOnClickListener {
                auth.signOut()
                messageListener?.remove()
                showLogin()
            }
        }
        header.addView(logout)
        root.addView(header)

        val divider = View(this).apply {
            setBackgroundColor(Color.rgb(38, 51, 75))
        }
        root.addView(
            divider,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(1)
            )
        )

        val intro = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, dp(14), 0, dp(14))
        }
        intro.addView(text("Welcome to ZizzChat 👋", 16f, white, true))
        intro.addView(text("Messages appear here in real time", 12f, muted))
        root.addView(intro)

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
            textSize = 15f
            maxLines = 3
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            imeOptions = EditorInfo.IME_ACTION_SEND
            setPadding(dp(16), dp(10), dp(16), dp(10))
            background = rounded(panel, 24)
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    sendMessage()
                    true
                } else false
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
            background = rounded(blue, 25)
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
                        background = rounded(
                            if (mine) blue else panel,
                            18
                        )
                    }

                    if (!mine) {
                        bubble.addView(text(email, 11f, Color.rgb(147, 197, 253), true))
                    }

                    bubble.addView(text(body, 15f, white))

                    val row = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = if (mine) Gravity.END else Gravity.START
                        setPadding(dp(2), dp(4), dp(2), dp(4))
                    }

                    row.addView(
                        bubble,
                        LinearLayout.LayoutParams(
                            (resources.displayMetrics.widthPixels * 0.72f).toInt(),
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    )

                    messages.addView(
                        row,
                        LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    )
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
