package com.zizzverse.zizzchat

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.text.InputType
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class ChatActivity : Activity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var friendUid: String
    private lateinit var friendName: String
    private lateinit var roomId: String

    private lateinit var messagesLayout: LinearLayout
    private lateinit var messageInput: EditText
    private lateinit var scrollView: ScrollView
    private lateinit var sendButton: TextView

    private var messagesListener: ListenerRegistration? = null
    private var isSending = false

    private val bg = Color.rgb(11, 20, 26)
    private val headerBg = Color.rgb(32, 44, 51)
    private val incomingBg = Color.rgb(32, 44, 51)
    private val outgoingBg = Color.rgb(0, 92, 75)
    private val white = Color.rgb(233, 237, 239)
    private val muted = Color.rgb(174, 186, 193)
    private val green = Color.rgb(0, 168, 132)

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radius: Int = 12) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }

    private fun label(
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        friendUid = intent.getStringExtra("friendUid") ?: ""
        friendName = intent.getStringExtra("friendName") ?: "Friend"

        val myUid = auth.currentUser?.uid

        if (myUid.isNullOrBlank() ||
            friendUid.isBlank() ||
            friendUid == myUid
        ) {
            Toast.makeText(this, "Invalid friend account", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        roomId = listOf(myUid, friendUid).sorted().joinToString("_")

        if (android.os.Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(true)
        }

        window.statusBarColor = bg
        window.navigationBarColor = Color.BLACK
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )

        createUI()
        listenForMessages()
    }

    private fun createUI() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            fitsSystemWindows = true
        }

        // Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(5), dp(5), dp(5))
            setBackgroundColor(headerBg)
        }

        header.addView(label("‹", 38f, white).apply {
            gravity = Gravity.CENTER
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(dp(38), dp(48)))

        header.addView(label(
            friendName.take(1).uppercase(),
            20f,
            white,
            true
        ).apply {
            gravity = Gravity.CENTER
            background = rounded(Color.rgb(82, 97, 107), 50)
        }, LinearLayout.LayoutParams(dp(42), dp(42)).apply {
            setMargins(dp(3), 0, dp(8), 0)
        })

        val titleColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }

        titleColumn.addView(label(friendName, 16f, white, true).apply {
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        })
        titleColumn.addView(label("Private chat", 12f, muted))

        header.addView(
            titleColumn,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        header.addView(label("⋮", 26f, white).apply {
            gravity = Gravity.CENTER
            setOnClickListener {
                AlertDialog.Builder(this@ChatActivity)
                    .setTitle(friendName)
                    .setMessage("Private conversation")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }, LinearLayout.LayoutParams(dp(38), dp(48)))

        root.addView(header)

        // Messages
        scrollView = ScrollView(this).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
        }

        messagesLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(12), dp(10), dp(12))
        }

        scrollView.addView(messagesLayout)
        root.addView(
            scrollView,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        // Message input row
        val inputRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(5), dp(5), dp(5), dp(5))
            setBackgroundColor(bg)
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(Color.rgb(42, 57, 66), 26)
            setPadding(dp(12), 0, dp(6), 0)
        }

        messageInput = EditText(this).apply {
            hint = "Message"
            setHintTextColor(muted)
            setTextColor(white)
            textSize = 16f
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            imeOptions = EditorInfo.IME_ACTION_SEND
            background = null
            showSoftInputOnFocus = true
            setPadding(0, dp(10), dp(4), dp(10))

            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    sendMessage()
                    true
                } else false
            }
        }

        container.addView(
            messageInput,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        container.addView(label("＋", 25f, muted).apply {
            gravity = Gravity.CENTER
            setPadding(dp(5), 0, dp(5), 0)
            setOnClickListener {
                Toast.makeText(
                    this@ChatActivity,
                    "Attachment feature coming soon",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        inputRow.addView(
            container,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        sendButton = label("➤", 22f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = rounded(green, 50)
            setOnClickListener { sendMessage() }
        }

        inputRow.addView(
            sendButton,
            LinearLayout.LayoutParams(dp(44), dp(44)).apply {
                setMargins(dp(5), 0, 0, 0)
            }
        )

        root.addView(inputRow)
        setContentView(root)

        messageInput.setOnFocusChangeListener { _, focused ->
            if (focused) scrollToBottom()
        }
    }

    private fun scrollToBottom() {
        scrollView.post {
            scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun listenForMessages() {
        messagesListener = db.collection("privateChats")
            .document(roomId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .limitToLast(100)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    showError("Message load failed", error)
                    return@addSnapshotListener
                }

                messagesLayout.removeAllViews()

                val myUid = auth.currentUser?.uid
                    ?: return@addSnapshotListener

                snapshot?.documents?.forEach { document ->
                    val text = document.getString("text") ?: ""
                    val senderUid = document.getString("senderUid") ?: ""

                    if (text.isNotBlank()) {
                        addMessageBubble(text, senderUid == myUid)
                    }
                }

                scrollToBottom()
            }
    }

    private fun addMessageBubble(text: String, isMine: Boolean) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = if (isMine) Gravity.END else Gravity.START
            setPadding(0, dp(3), 0, dp(3))
        }

        val bubble = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(6))
            background = rounded(
                if (isMine) outgoingBg else incomingBg
            )
        }

        bubble.addView(label(text, 16f, white))

        val time = java.text.SimpleDateFormat(
            "HH:mm",
            java.util.Locale.getDefault()
        ).format(java.util.Date())

        bubble.addView(label(
            time + if (isMine) "  ✓" else "",
            10f,
            if (isMine) Color.rgb(184, 218, 207) else muted
        ).apply {
            gravity = Gravity.END
            setPadding(dp(12), dp(4), 0, 0)
        })

        row.addView(bubble)
        messagesLayout.addView(
            row,
            LinearLayout.LayoutParams(-1, -2)
        )
    }

    private fun sendMessage() {
        val myUid = auth.currentUser?.uid
        val text = messageInput.text.toString().trim()

        if (myUid.isNullOrBlank()) {
            Toast.makeText(this, "Please sign in again", Toast.LENGTH_LONG).show()
            return
        }

        if (text.isEmpty() || isSending) return

        isSending = true
        sendButton.isEnabled = false

        val roomRef = db.collection("privateChats").document(roomId)

        val messageData = hashMapOf<String, Any>(
            "senderUid" to myUid,
            "receiverUid" to friendUid,
            "text" to text,
            "timestamp" to FieldValue.serverTimestamp()
        )

        // Create the message only after the room is available.
        roomRef.get()
            .addOnSuccessListener { roomSnapshot ->
                if (!roomSnapshot.exists()) {
                    val roomData = hashMapOf<String, Any>(
                        "participants" to listOf(myUid, friendUid).sorted(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )

                    roomRef.set(roomData)
                        .addOnSuccessListener {
                            writeMessage(roomRef, messageData, text)
                        }
                        .addOnFailureListener { error ->
                            showError("Chat setup failed", error)
                            finishSending()
                        }
                } else {
                    writeMessage(roomRef, messageData, text)
                }
            }
            .addOnFailureListener { error ->
                showError("Chat access failed", error)
                finishSending()
            }
    }

    private fun writeMessage(
        roomRef: com.google.firebase.firestore.DocumentReference,
        messageData: HashMap<String, Any>,
        originalText: String
    ) {
        roomRef.collection("messages")
            .add(messageData)
            .addOnSuccessListener {
                messageInput.setText("")
                scrollToBottom()

                roomRef.update(
                    "updatedAt",
                    FieldValue.serverTimestamp()
                )
            }
            .addOnFailureListener { error ->
                showError("Send failed", error)
            }
            .addOnCompleteListener {
                finishSending()
            }
    }

    private fun finishSending() {
        isSending = false
        sendButton.isEnabled = true
    }

    private fun showError(prefix: String, error: Exception) {
        Toast.makeText(
            this,
            "$prefix: ${error.localizedMessage ?: "Unknown error"}",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onDestroy() {
        messagesListener?.remove()
        messagesListener = null
        super.onDestroy()
    }
}
