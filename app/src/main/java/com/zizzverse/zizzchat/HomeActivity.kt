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
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

// ==========================================
// 🏠 HOME ACTIVITY — মূল Chat Page
// ==========================================

class HomeActivity : Activity() {

    // 🔥 FIREBASE AUTHENTICATION
    private val auth by lazy {
        FirebaseAuth.getInstance()
    }

    // 🔥 FIRESTORE DATABASE
    private val db by lazy {
        FirebaseFirestore.getInstance()
    }

    // 🎨 UI COLORS
    private val bg = Color.rgb(10, 16, 32)
    private val card = Color.rgb(22, 32, 53)
    private val blue = Color.rgb(37, 99, 235)
    private val white = Color.WHITE
    private val muted = Color.rgb(154, 170, 196)
    private val green = Color.rgb(34, 197, 94)

    // 💬 MESSAGE LIST CONTAINER
    private lateinit var messagesLayout: LinearLayout

    // 📜 MESSAGE SCROLL VIEW
    private lateinit var messageScroll: ScrollView

    // 📝 MESSAGE INPUT FIELD
    private lateinit var messageInput: EditText

    // 📤 SEND BUTTON
    private lateinit var sendButton: TextView

    // 🌟 EMPTY CHAT MESSAGE
    private lateinit var emptyState: TextView

    // 📡 FIRESTORE LISTENER
    private var messageListener:
        com.google.firebase.firestore.ListenerRegistration? = null

    // 📏 DP CONVERTER
    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    // 🎨 ROUNDED BACKGROUND
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

    // 🖥️ HOME PAGE UI
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔐 LOGIN না থাকলে Login Page-এ পাঠাও
        if (auth.currentUser == null) {
            startActivity(
                Intent(this, LoginActivity::class.java)
            )
            finish()
            return
        }

        // 🎨 SYSTEM BAR COLORS
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.setSoftInputMode(
            android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )

        // 📱 MAIN LAYOUT
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }

        setContentView(root)

        // ======================================
        // 🔝 TOP HEADER — APP NAME + MENU
        // ======================================

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(14))
            setBackgroundColor(card)
        }

        root.addView(
            header,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        // 🟦 APP LOGO
        val logo = makeText(
            "Z",
            24f,
            white,
            true
        ).apply {
            gravity = Gravity.CENTER
            background = rounded(blue, 14)
        }

        header.addView(
            logo,
            LinearLayout.LayoutParams(dp(48), dp(48))
        )

        // 📛 APP NAME + CHAT STATUS
        val titleColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }

        titleColumn.addView(
            makeText("ZizzChat", 19f, white, true)
        )

        val statusRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        // 🟢 STATUS INDICATOR — শুধু UI indicator
        val statusDot = makeText("●", 10f, green)

        statusRow.addView(statusDot)

        val statusText = makeText(
            "  General Chat",
            12f,
            muted
        )

        statusRow.addView(statusText)
        titleColumn.addView(statusRow)

        header.addView(
            titleColumn,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        // ⋮ MENU BUTTON — Refresh / Logout
        val menuButton = makeText(
            "⋮",
            30f,
            white
        ).apply {
            gravity = Gravity.CENTER
            setPadding(dp(10), 0, dp(4), 0)

            setOnClickListener { view ->
                PopupMenu(this@HomeActivity, view).apply {
                    menu.add("Refresh messages")
                    menu.add("Logout")

                    setOnMenuItemClickListener { item ->
                        when (item.title.toString()) {
                            "Refresh messages" -> {
                                listenForMessages()
                                true
                            }

                            "Logout" -> {
                                logout()
                                true
                            }

                            else -> false
                        }
                    }

                    show()
                }
            }
        }

        header.addView(menuButton)


        // ======================================
        // 👥 FRIEND ACTION BUTTONS
        // ======================================

        val friendActions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val friendActionsParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(dp(16), dp(14), dp(16), dp(4))
        }

        root.addView(friendActions, friendActionsParams)

        // ➕ ADD FRIEND BUTTON — Friend Search খুলবে
        val addFriendButton = TextView(this).apply {
            text = "＋  Add Friends"
            textSize = 14f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(14), dp(10), dp(14))
            background = rounded(blue, 14)

            setOnClickListener {
                startActivity(
                    Intent(
                        this@HomeActivity,
                        FriendsActivity::class.java
                    )
                )
            }
        }

        friendActions.addView(
            addFriendButton,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                setMargins(0, 0, dp(6), 0)
            }
        )

        // 📩 FRIEND REQUESTS BUTTON
        val requestsButton = TextView(this).apply {
            text = "✉  Requests"
            textSize = 14f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(14), dp(10), dp(14))
            background = rounded(card, 14)

            // 📩 FRIEND REQUESTS PAGE OPEN BUTTON
            setOnClickListener {
                startActivity(
                    Intent(
                        this@HomeActivity,
                        FriendRequestsActivity::class.java
                    )
                )
            }
        }

        friendActions.addView(
            requestsButton,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                setMargins(dp(6), 0, 0, 0)
            }
        )
        

        // ======================================
        // 👋 WELCOME CARD
        // ======================================

        val welcomeCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = rounded(card, 18)
        }

        val welcomeParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(dp(16), dp(16), dp(16), dp(10))
        }

        root.addView(welcomeCard, welcomeParams)

        welcomeCard.addView(
            makeText(
                "Welcome to ZizzChat 👋",
                19f,
                white,
                true
            )
        )

        val userEmail =
            auth.currentUser?.email ?: "Chat member"

        welcomeCard.addView(
            makeText(
                userEmail,
                12f,
                muted
            ).apply {
                setPadding(0, dp(6), 0, 0)
            }
        )

        // ======================================
        // 💬 CHAT MESSAGE AREA
        // ======================================

        val chatFrame = FrameLayout(this).apply {
            setBackgroundColor(bg)
        }

        root.addView(
            chatFrame,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        // 📜 SCROLL VIEW FOR MESSAGES
        messageScroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            setPadding(dp(16), dp(8), dp(16), dp(8))
        }

        chatFrame.addView(
            messageScroll,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        // 💬 ALL MESSAGES CONTAINER
        messagesLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        messageScroll.addView(messagesLayout)

        // 🌟 EMPTY CHAT STATE
        emptyState = makeText(
            "✦\n\nNo messages yet\n\nSay hello to start the conversation!",
            15f,
            muted
        ).apply {
            gravity = Gravity.CENTER
            visibility = View.GONE
        }

        chatFrame.addView(
            emptyState,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        // ======================================
        // ⌨️ BOTTOM MESSAGE COMPOSER
        // ======================================

        val composer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundColor(card)
        }

        root.addView(
            composer,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        // 📝 MESSAGE INPUT FIELD
        messageInput = EditText(this).apply {
            hint = "Write a message..."
            setHintTextColor(muted)
            setTextColor(white)
            textSize = 15f
            maxLines = 4
            minLines = 1
            inputType =
                android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
                android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE

            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = rounded(bg, 22)
        }

        composer.addView(
            messageInput,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        // 📤 SEND BUTTON
        sendButton = makeText(
            "➤",
            25f,
            white,
            true
        ).apply {
            gravity = Gravity.CENTER
            background = rounded(blue, 22)

            setOnClickListener {
                sendMessage()
            }
        }

        val sendParams = LinearLayout.LayoutParams(
            dp(52),
            dp(52)
        ).apply {
            setMargins(dp(10), 0, 0, 0)
        }

        composer.addView(sendButton, sendParams)

        // 🔥 START FIRESTORE MESSAGE LISTENER
        listenForMessages()
    }

    // ======================================
    // 📤 SEND MESSAGE TO FIRESTORE
    // ======================================

    private fun sendMessage() {
        val text = messageInput.text.toString().trim()
        val user = auth.currentUser ?: return

        // ⚠️ EMPTY MESSAGE CHECK
        if (text.isEmpty()) {
            return
        }

        // ⚠️ MESSAGE LENGTH CHECK
        if (text.length > 2000) {
            Toast.makeText(
                this,
                "Message is too long (maximum 2000 characters)",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // ⏳ SENDING STATE
        sendButton.isEnabled = false

        // 🗂️ MESSAGE DATA
        val message = hashMapOf(
            "uid" to user.uid,
            "email" to (user.email ?: ""),
            "text" to text,
            "timestamp" to System.currentTimeMillis()
        )

        // 🔥 SAVE MESSAGE IN FIRESTORE
        db.collection("rooms")
            .document("general")
            .collection("messages")
            .add(message)
            .addOnSuccessListener {
                // ✅ MESSAGE SENT
                messageInput.text.clear()

                // ⌨️ HIDE KEYBOARD
                val imm = getSystemService(
                    Context.INPUT_METHOD_SERVICE
                ) as InputMethodManager

                imm.hideSoftInputFromWindow(
                    messageInput.windowToken,
                    0
                )
            }
            .addOnFailureListener { error ->
                // ❌ MESSAGE SEND ERROR
                Toast.makeText(
                    this,
                    error.localizedMessage ?: "Message failed",
                    Toast.LENGTH_LONG
                ).show()
            }
            .addOnCompleteListener {
                sendButton.isEnabled = true
            }
    }

    // ======================================
    // 📥 RECEIVE MESSAGES FROM FIRESTORE
    // ======================================

    private fun listenForMessages() {
        // 🧹 REMOVE OLD LISTENER BEFORE ADDING NEW ONE
        messageListener?.remove()

        messageListener = db.collection("rooms")
            .document("general")
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .limitToLast(100)
            .addSnapshotListener { snapshot, error ->

                // ❌ DATABASE ERROR
                if (error != null) {
                    Toast.makeText(
                        this,
                        "Could not load messages",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addSnapshotListener
                }

                // 📭 NO DATA YET
                if (snapshot == null) return@addSnapshotListener

                // 🧹 CLEAR OLD MESSAGE VIEWS
                messagesLayout.removeAllViews()

                val documents = snapshot.documents

                // 🌟 SHOW EMPTY STATE WHEN THERE ARE NO MESSAGES
                emptyState.visibility =
                    if (documents.isEmpty()) View.VISIBLE
                    else View.GONE

                // 💬 DISPLAY EACH MESSAGE
                for (document in documents) {
                    val email =
                        document.getString("email") ?: "Unknown user"

                    val text =
                        document.getString("text") ?: ""

                    val uid =
                        document.getString("uid") ?: ""

                    addMessageBubble(
                        email,
                        text,
                        uid == auth.currentUser?.uid
                    )
                }

                // 📜 AUTO-SCROLL TO LATEST MESSAGE
                messageScroll.post {
                    messageScroll.fullScroll(View.FOCUS_DOWN)
                }
            }
    }

    // ======================================
    // 💬 MESSAGE BUBBLE UI
    // ======================================

    private fun addMessageBubble(
        email: String,
        text: String,
        isMine: Boolean
    ) {
        // 📦 ONE MESSAGE CONTAINER
        val messageColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))

            background = rounded(
                if (isMine) {
                    Color.rgb(30, 64, 175)
                } else {
                    card
                },
                16
            )
        }

        // 👤 SENDER EMAIL
        val sender = makeText(
            if (isMine) "You" else email,
            11f,
            if (isMine) {
                Color.rgb(191, 219, 254)
            } else {
                Color.rgb(96, 165, 250)
            },
            true
        )

        messageColumn.addView(sender)

        // 📝 MESSAGE TEXT
        val body = makeText(
            text,
            15f,
            white
        ).apply {
            setPadding(0, dp(5), 0, 0)
        }

        messageColumn.addView(body)

        // 📐 ALIGN OWN MESSAGES RIGHT, OTHERS LEFT
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = if (isMine) Gravity.END else Gravity.START
        }

        val bubbleParams = LinearLayout.LayoutParams(
            (resources.displayMetrics.widthPixels * 0.78f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 0, 0, dp(10))
        }

        row.addView(messageColumn, bubbleParams)
        messagesLayout.addView(row)
    }

    // 🚪 LOGOUT FUNCTION
    private fun logout() {
        // 🔥 SIGN OUT FROM FIREBASE
        auth.signOut()

        // 🧹 REMOVE LISTENER
        messageListener?.remove()
        messageListener = null

        // 🔐 RETURN TO LOGIN PAGE
        val intent = Intent(
            this,
            LoginActivity::class.java
        )

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()
    }

    // 🧹 CLEAN UP FIRESTORE LISTENER
    override fun onDestroy() {
        messageListener?.remove()
        messageListener = null
        super.onDestroy()
    }
}
