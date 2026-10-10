package com.zizzverse.zizzchat

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.*
import android.text.InputType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : Activity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private lateinit var root: LinearLayout
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var messagesLayout: LinearLayout
    private lateinit var messageInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(15, 23, 42)
        window.navigationBarColor = Color.rgb(15, 23, 42)

        if (auth.currentUser != null) {
            showChat()
        } else {
            showLogin()
        }
    }

    private fun makeRoot(): LinearLayout {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
            setBackgroundColor(Color.rgb(15, 23, 42))
        }
        setContentView(root)
        return root
    }

    private fun addText(
        parent: LinearLayout,
        value: String,
        size: Float
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(Color.WHITE)
            setPadding(4, 12, 4, 12)
            parent.addView(this)
        }
    }

    private fun makeInput(
        hintText: String,
        inputType: Int
    ): EditText {
        return EditText(this).apply {
            hint = hintText
            hintTextColor = Color.LTGRAY
            setTextColor(Color.WHITE)
            this.inputType = inputType
            setSingleLine(true)
            setPadding(14, 10, 14, 10)
            backgroundTintList =
                android.content.res.ColorStateList.valueOf(
                    Color.rgb(100, 116, 139)
                )
        }
    }

    private fun makeButton(
        parent: LinearLayout,
        label: String,
        action: () -> Unit
    ) {
        parent.addView(Button(this).apply {
            text = label
            setOnClickListener { action() }
        })
    }

    private fun showLogin() {
        makeRoot()

        addText(root, "ZizzChat", 32f)
        addText(root, "Connect with your friends", 16f)

        emailInput = makeInput(
            "Email address",
            InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        )
        root.addView(emailInput)

        passwordInput = makeInput(
            "Password (at least 6 characters)",
            InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_PASSWORD
        )
        root.addView(passwordInput)

        makeButton(root, "Log In") {
            authenticate(false)
        }

        makeButton(root, "Create Account") {
            authenticate(true)
        }
    }

    private fun authenticate(createAccount: Boolean) {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString()

        if (email.isEmpty() || password.length < 6) {
            toast("Enter a valid email and password (6+ characters).")
            return
        }

        val task = if (createAccount) {
            auth.createUserWithEmailAndPassword(email, password)
        } else {
            auth.signInWithEmailAndPassword(email, password)
        }

        task.addOnSuccessListener {
            showChat()
        }.addOnFailureListener { error ->
            toast(error.localizedMessage ?: "Authentication failed")
        }
    }

    private fun showChat() {
        makeRoot()

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        header.addView(TextView(this).apply {
            text = "ZizzChat"
            textSize = 25f
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        })

        header.addView(Button(this).apply {
            text = "Logout"
            setOnClickListener {
                auth.signOut()
                showLogin()
            }
        })

        root.addView(header)

        addText(
            root,
            "Signed in: ${auth.currentUser?.email ?: ""}",
            13f
        )

        val scrollView = ScrollView(this)
        messagesLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scrollView.addView(messagesLayout)

        root.addView(
            scrollView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        val composer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        messageInput = makeInput(
            "Write a message...",
            InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE
        )
        messageInput.maxLines = 3

        composer.addView(
            messageInput,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        composer.addView(Button(this).apply {
            text = "Send"
            setOnClickListener { sendMessage() }
        })

        root.addView(composer)
        listenForMessages()
    }

    private fun sendMessage() {
        val user = auth.currentUser ?: return
        val message = messageInput.text.toString().trim()

        if (message.isEmpty()) return

        val data = hashMapOf(
            "uid" to user.uid,
            "email" to (user.email ?: ""),
            "text" to message,
            "createdAt" to FieldValue.serverTimestamp()
        )

        db.collection("rooms")
            .document("general")
            .collection("messages")
            .add(data)
            .addOnSuccessListener {
                messageInput.text.clear()
            }
            .addOnFailureListener { error ->
                toast(error.localizedMessage ?: "Message failed")
            }
    }

    private fun listenForMessages() {
        db.collection("rooms")
            .document("general")
            .collection("messages")
            .orderBy("createdAt")
            .addSnapshotListener(this) { snapshot, error ->

                if (error != null) {
                    toast("Chat error: ${error.localizedMessage}")
                    return@addSnapshotListener
                }

                messagesLayout.removeAllViews()

                snapshot?.documents?.forEach { document ->
                    val email = document.getString("email") ?: "User"
                    val message = document.getString("text") ?: ""

                    addText(
                        messagesLayout,
                        "$email\n$message",
                        15f
                    )
                }
            }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
