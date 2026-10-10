package com.zizzverse.zizzchat

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.app.Activity
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
    private lateinit var sendButton: Button

    private var messagesListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        friendUid = intent.getStringExtra("friendUid") ?: ""
        friendName = intent.getStringExtra("friendName") ?: "Friend"

        val myUid = auth.currentUser?.uid

        if (myUid.isNullOrBlank() ||
            friendUid.isBlank() ||
            friendUid == myUid
        ) {
            Toast.makeText(this, "Invalid chat or friend", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Both users calculate the same room ID.
        roomId = listOf(myUid, friendUid).sorted().joinToString("_")

        createChatUI()
        prepareChatRoom()
        listenForMessages()
    }

    private fun createChatUI() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(245, 247, 250))
        }

        // Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(12, 12, 12, 12)
            setBackgroundColor(Color.rgb(28, 40, 65))
        }

        val backButton = Button(this).apply {
            text = "‹"
            textSize = 28f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { finish() }
        }

        val title = TextView(this).apply {
            text = friendName
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setPadding(12, 0, 0, 0)
        }

        header.addView(
            backButton,
            LinearLayout.LayoutParams(52, 56)
        )
        header.addView(
            title,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        root.addView(
            header,
            LinearLayout.LayoutParams(-1, -2)
        )

        // Messages area
        scrollView = ScrollView(this).apply {
            isFillViewport = true
        }

        messagesLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 12, 12, 12)
        }

        scrollView.addView(
    messagesLayout,
    android.widget.FrameLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT
    )
)

        root.addView(
            scrollView,
            LinearLayout.LayoutParams( -1, 0, 1f)
        )

        // Message input row
        val inputRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(8, 8, 8, 8)
            setBackgroundColor(Color.WHITE)
        }

        messageInput = EditText(this).apply {
            hint = "Write a message..."
            textSize = 16f
            setSingleLine(true)
            imeOptions = EditorInfo.IME_ACTION_SEND
        }

        sendButton = Button(this).apply {
            text = "Send"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(45, 105, 220))
            setOnClickListener { sendMessage() }
        }

        messageInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }

        inputRow.addView(
            messageInput,
            LinearLayout.LayoutParams(0, -2, 1f)
        )
        inputRow.addView(
            sendButton,
            LinearLayout.LayoutParams(-2, -2)
        )

        root.addView(
            inputRow,
            LinearLayout.LayoutParams(-1, -2)
        )

        setContentView(root)
    }

    private fun prepareChatRoom() {
        val myUid = auth.currentUser?.uid ?: return

        val roomData = hashMapOf<String, Any>(
            "participants" to listOf(myUid, friendUid),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        db.collection("privateChats")
            .document(roomId)
            .set(roomData, com.google.firebase.firestore.SetOptions.merge())
    }

    private fun listenForMessages() {
        messagesListener = db.collection("privateChats")
            .document(roomId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .limitToLast(100)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    Toast.makeText(
                        this,
                        "Could not load messages: ${error.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addSnapshotListener
                }

                messagesLayout.removeAllViews()

                val myUid = auth.currentUser?.uid ?: return@addSnapshotListener

                snapshot?.documents?.forEach { document ->
                    val senderUid = document.getString("senderUid") ?: ""
                    val messageText = document.getString("text") ?: ""

                    if (messageText.isNotBlank()) {
                        addMessageBubble(
                            messageText,
                            senderUid == myUid
                        )
                    }
                }

                scrollView.post {
                    scrollView.fullScroll(ScrollView.FOCUS_DOWN)
                }
            }
    }

    private fun addMessageBubble(message: String, isMine: Boolean) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = if (isMine) Gravity.END else Gravity.START
            setPadding(0, 4, 0, 4)
        }

        val bubble = TextView(this).apply {
            text = message
            textSize = 16f
            setTextColor(
                if (isMine) Color.WHITE else Color.rgb(30, 30, 30)
            )
            setPadding(14, 10, 14, 10)

            background = GradientDrawable().apply {
                setColor(
                    if (isMine) Color.rgb(45, 105, 220)
                    else Color.WHITE
                )
                cornerRadius = 22f
            }

            maxWidth = (resources.displayMetrics.widthPixels * 0.75f).toInt()
        }

        row.addView(bubble)
        messagesLayout.addView(
            row,
            LinearLayout.LayoutParams(-1, -2)
        )
    }

    private fun sendMessage() {
        val myUid = auth.currentUser?.uid ?: return
        val messageText = messageInput.text.toString().trim()

        if (messageText.isEmpty()) return

        sendButton.isEnabled = false

        val messageData = hashMapOf<String, Any>(
            "senderUid" to myUid,
            "receiverUid" to friendUid,
            "text" to messageText,
            "timestamp" to FieldValue.serverTimestamp()
        )

        db.collection("privateChats")
            .document(roomId)
            .collection("messages")
            .add(messageData)
            .addOnSuccessListener {
                messageInput.setText("")
                db.collection("privateChats")
                    .document(roomId)
                    .update("updatedAt", FieldValue.serverTimestamp())
            }
            .addOnFailureListener { error ->
                Toast.makeText(
                    this,
                    "Message failed: ${error.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
            .addOnCompleteListener {
                sendButton.isEnabled = true
            }
    }

    override fun onDestroy() {
        messagesListener?.remove()
        super.onDestroy()
    }
}
