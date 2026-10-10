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

class ChatActivity : Activity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var friendUid: String
    private lateinit var friendName: String
    private lateinit var roomId: String

    private lateinit var root: LinearLayout
    private lateinit var messagesLayout: LinearLayout
    private lateinit var messageInput: EditText
    private lateinit var scrollView: ScrollView
    private lateinit var sendButton: TextView

    private var messagesListener: ListenerRegistration? = null

    private val backgroundColor = Color.rgb(11, 20, 26)
    private val headerColor = Color.rgb(32, 44, 51)
    private val incomingColor = Color.rgb(32, 44, 51)
    private val outgoingColor = Color.rgb(0, 92, 75)
    private val white = Color.rgb(233, 237, 239)
    private val muted = Color.rgb(174, 186, 193)
    private val green = Color.rgb(0, 168, 132)

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radius: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

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
            if (bold) setTypeface(null, Typeface.BOLD)
        }
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
            Toast.makeText(this, "Invalid friend", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        roomId = listOf(myUid, friendUid).sorted().joinToString("_")

        // Keep system bars separate from the app content.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(true)
        }

        window.statusBarColor = backgroundColor
        window.navigationBarColor = Color.BLACK
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )

        createChatUI()
        prepareChatRoom()
        listenForMessages()
    }

    private fun createChatUI() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(backgroundColor)
            fitsSystemWindows = true
        }

        // TOP HEADER
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(5), dp(5), dp(5))
            setBackgroundColor(headerColor)
        }

        val back = makeText("‹", 38f, white).apply {
            gravity = Gravity.CENTER
            setOnClickListener { finish() }
        }

        header.addView(back, LinearLayout.LayoutParams(dp(38), dp(48)))

        val avatar = makeText(
            friendName.take(1).uppercase(),
            20f,
            white,
            true
        ).apply {
            gravity = Gravity.CENTER
            background = rounded(Color.rgb(82, 97, 107), 50)
        }

        val avatarParams = LinearLayout.LayoutParams(dp(42), dp(42))
        avatarParams.setMargins(dp(3), 0, dp(8), 0)
        header.addView(avatar, avatarParams)

        val titleColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }

        titleColumn.addView(
            makeText(friendName, 16f, white, true).apply {
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }
        )
        titleColumn.addView(makeText("Private chat", 12f, muted))

        header.addView(
            titleColumn,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        val videoButton = makeText("▣", 23f, white).apply {
            gravity = Gravity.CENTER
            setOnClickListener {
                Toast.makeText(
                    this@ChatActivity,
                    "Video calling will be added later",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        header.addView(videoButton, LinearLayout.LayoutParams(dp(38), dp(48)))

        val callButton = makeText("☎", 24f, white).apply {
            gravity = Gravity.CENTER
            setOnClickListener {
                Toast.makeText(
                    this@ChatActivity,
                    "Voice calling will be added later",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        header.addView(callButton, LinearLayout.LayoutParams(dp(38), dp(48)))

        val menuButton = makeText("⋮", 26f, white).apply {
            gravity = Gravity.CENTER
            setOnClickListener { showChatMenu(it) }
        }
        header.addView(menuButton, LinearLayout.LayoutParams(dp(30), dp(48)))

        root.addView(header, LinearLayout.LayoutParams(-1, -2))

        // MESSAGE HISTORY
        scrollView = ScrollView(this).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
            clipToPadding = false
        }

        messagesLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(12), dp(10), dp(12))
        }

        scrollView.addView(
            messagesLayout,
            android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(
            scrollView,
            LinearLayout.LayoutParams(0, 0, 1f)
        )

        // BOTTOM MESSAGE BAR
        val inputRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(5), dp(5), dp(5), dp(5))
            setBackgroundColor(backgroundColor)
        }

        val emojiButton = makeText("☻", 24f, muted).apply {
            gravity = Gravity.CENTER
            setOnClickListener {
                messageInput.requestFocus()
                val imm = getSystemService(INPUT_METHOD_SERVICE)
                    as android.view.inputmethod.InputMethodManager
                imm.showSoftInput(
                    messageInput,
                    android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT
                )
            }
        }
        inputRow.addView(emojiButton, LinearLayout.LayoutParams(dp(36), dp(46)))

        val messageContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(Color.rgb(42, 57, 66), 28)
            setPadding(dp(11), 0, dp(4), 0)
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
            showSoftInputOnFocus = true
            isFocusable = true
            isFocusableInTouchMode = true
            background = null
            setPadding(0, dp(10), dp(4), dp(10))
            minHeight = dp(46)
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    sendMessage()
                    true
                } else {
                    false
                }
            }
        }

        messageContainer.addView(
            messageInput,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        val attachButton = makeText("＋", 27f, muted).apply {
            gravity = Gravity.CENTER
            setOnClickListener {
                Toast.makeText(
                    this@ChatActivity,
                    "Attachments will be added later",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        messageContainer.addView(
            attachButton,
            LinearLayout.LayoutParams(dp(34), dp(46))
        )

        val cameraButton = makeText("▣", 22f, muted).apply {
            gravity = Gravity.CENTER
            setOnClickListener {
                Toast.makeText(
                    this@ChatActivity,
                    "Camera feature will be added later",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        messageContainer.addView(
            cameraButton,
            LinearLayout.LayoutParams(dp(34), dp(46))
        )

        inputRow.addView(
            messageContainer,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        sendButton = makeText("➤", 22f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = rounded(green, 50)
            setOnClickListener { sendMessage() }
        }

        val sendParams = LinearLayout.LayoutParams(dp(44), dp(44))
        sendParams.setMargins(dp(5), 0, dp(1), 0)
        inputRow.addView(sendButton, sendParams)

        root.addView(inputRow, LinearLayout.LayoutParams(-1, -2))

        // Install content only after configuring the complete layout.
        setContentView(root)

        // Keep the latest messages visible when the keyboard resizes the screen.
        messageInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                scrollView.post {
                    scrollView.fullScroll(View.FOCUS_DOWN)
                }
            }
        }
    }

    private fun showChatMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add("Friend details")

        popup.setOnMenuItemClickListener { item ->
            if (item.title.toString() == "Friend details") {
                android.app.AlertDialog.Builder(this)
                    .setTitle(friendName)
                    .setMessage("Private conversation")
                    .setPositiveButton("OK", null)
                    .show()
            }
            true
        }
        popup.show()
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
            .addOnFailureListener { error ->
                Toast.makeText(
                    this,
                    "Chat setup failed: ${error.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
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
                    Toast.makeText(
                        this,
                        "Message load failed: ${error.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addSnapshotListener
                }

                messagesLayout.removeAllViews()
                val myUid = auth.currentUser?.uid ?: return@addSnapshotListener

                snapshot?.documents?.forEach { document ->
                    val senderUid = document.getString("senderUid") ?: ""
                    val text = document.getString("text") ?: ""

                    if (text.isNotBlank()) {
                        addMessageBubble(text, senderUid == myUid)
                    }
                }

                scrollView.post {
                    scrollView.fullScroll(View.FOCUS_DOWN)
                }
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
                if (isMine) outgoingColor else incomingColor,
                12
            )
        }

        bubble.addView(makeText(text, 16f, white))

        val time = java.text.SimpleDateFormat(
            "HH:mm",
            java.util.Locale.getDefault()
        ).format(java.util.Date())

        bubble.addView(
            makeText(
                if (isMine) "$time  ✓✓" else time,
                10f,
                if (isMine) Color.rgb(184, 218, 207) else muted
            ).apply {
                gravity = Gravity.END
                setPadding(dp(12), dp(4), 0, 0)
            }
        )

        row.addView(bubble)
        messagesLayout.addView(row, LinearLayout.LayoutParams(-1, -2))
    }

    private fun sendMessage() {
        val myUid = auth.currentUser?.uid ?: return
        val text = messageInput.text.toString().trim()
        if (text.isEmpty()) return

        sendButton.isEnabled = false

        val data = hashMapOf<String, Any>(
            "senderUid" to myUid,
            "receiverUid" to friendUid,
            "text" to text,
            "timestamp" to FieldValue.serverTimestamp()
        )

        db.collection("privateChats")
            .document(roomId)
            .collection("messages")
            .add(data)
            .addOnSuccessListener {
                messageInput.setText("")
                scrollView.post {
                    scrollView.fullScroll(View.FOCUS_DOWN)
                }

                db.collection("privateChats")
                    .document(roomId)
                    .update("updatedAt", FieldValue.serverTimestamp())
            }
            .addOnFailureListener { error ->
                Toast.makeText(
                    this,
                    "Send failed: ${error.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
            .addOnCompleteListener {
                sendButton.isEnabled = true
            }
    }

    override fun onDestroy() {
        messagesListener?.remove()
        messagesListener = null
        super.onDestroy()
    }
}
