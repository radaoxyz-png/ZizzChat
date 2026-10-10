package com.zizzverse.zizzchat

import android.app.Activity
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class ChatActivity : Activity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var myUid: String
    private lateinit var friendUid: String
    private lateinit var friendName: String
    private lateinit var roomId: String

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: Button
    private lateinit var chatTitle: TextView
    private lateinit var chatAvatar: TextView

    private val messages = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatAdapter

    private var messageListener: ListenerRegistration? = null
    private var roomReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val user = auth.currentUser

        if (user == null) {
            showError("Please login first")
            finish()
            return
        }

        myUid = user.uid
        friendUid = intent.getStringExtra("friendUid") ?: ""
        friendName = intent.getStringExtra("friendName") ?: "Friend"

        if (friendUid.isBlank() || friendUid == myUid) {
            showError("Invalid friend account")
            finish()
            return
        }

        roomId = listOf(myUid, friendUid)
            .sorted()
            .joinToString("_")

        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        messageInput = findViewById(R.id.messageInput)
        sendButton = findViewById(R.id.sendButton)
        chatTitle = findViewById(R.id.chatTitle)
        chatAvatar = findViewById(R.id.chatAvatar)

        chatTitle.text = friendName
        chatAvatar.text = friendName
            .firstOrNull()
            ?.uppercaseChar()
            ?.toString() ?: "A"

        findViewById<ImageButton>(R.id.backButton)
            .setOnClickListener {
                finish()
            }

        adapter = ChatAdapter(messages)

        chatRecyclerView.layoutManager =
            LinearLayoutManager(this).apply {
                stackFromEnd = true
            }

        chatRecyclerView.adapter = adapter

        sendButton.isEnabled = false

        sendButton.setOnClickListener {
            sendMessage()
        }

        messageInput.setOnEditorActionListener {
                _, actionId, event ->

            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }

        prepareChatRoom()
    }

    private fun prepareChatRoom() {
        val roomRef = db.collection("privateChats")
            .document(roomId)

        roomRef.get()
            .addOnSuccessListener { document ->

                if (document.exists()) {
                    val participants =
                        document.get("participants") as? List<*>

                    if (
                        participants == null ||
                        !participants.contains(myUid) ||
                        !participants.contains(friendUid)
                    ) {
                        showError("Chat room participants are incorrect")
                        finish()
                        return@addOnSuccessListener
                    }

                    roomReady = true
                    sendButton.isEnabled = true
                    listenForMessages()

                } else {
                    val roomData = hashMapOf(
                        "participants" to
                            listOf(myUid, friendUid).sorted(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )

                    roomRef.set(roomData)
                        .addOnSuccessListener {
                            roomReady = true
                            sendButton.isEnabled = true
                            listenForMessages()
                        }
                        .addOnFailureListener { error ->
                            showError(
                                "Chat setup failed: ${error.message}"
                            )
                        }
                }
            }
            .addOnFailureListener { error ->
                showError("Chat access failed: ${error.message}")
            }
    }

    private fun listenForMessages() {
        messageListener?.remove()

        messageListener = db.collection("privateChats")
            .document(roomId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .limitToLast(100)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    showError("Message load failed: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot == null) return@addSnapshotListener

                messages.clear()

                for (document in snapshot.documents) {
                    val message =
                        document.toObject(ChatMessage::class.java)

                    if (message != null) {
                        messages.add(message)
                    }
                }

                adapter.notifyDataSetChanged()

                if (messages.isNotEmpty()) {
                    chatRecyclerView.scrollToPosition(
                        messages.size - 1
                    )
                }
            }
    }

    private fun sendMessage() {
        if (!roomReady) {
            showError("Chat is not ready yet")
            return
        }

        val text = messageInput.text.toString().trim()

        if (text.isEmpty()) return

        if (text.length > 2000) {
            showError("Message is too long")
            return
        }

        sendButton.isEnabled = false

        val messageData = hashMapOf(
            "senderUid" to myUid,
            "receiverUid" to friendUid,
            "text" to text,
            "timestamp" to FieldValue.serverTimestamp()
        )

        db.collection("privateChats")
            .document(roomId)
            .collection("messages")
            .add(messageData)
            .addOnSuccessListener {
                messageInput.text.clear()
                sendButton.isEnabled = true
            }
            .addOnFailureListener { error ->
                sendButton.isEnabled = true
                showError("Send failed: ${error.message}")
            }
    }

    private fun showError(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onDestroy() {
        messageListener?.remove()
        super.onDestroy()
    }
}

data class ChatMessage(
    val senderUid: String = "",
    val receiverUid: String = "",
    val text: String = "",
    val timestamp: Timestamp? = null
)
