package com.zizzverse.zizzchat

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class ChatActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var friendUid: String
    private lateinit var friendName: String
    private lateinit var myUid: String
    private lateinit var roomId: String

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: Button
    private lateinit var chatTitle: TextView

    private val messages = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatAdapter

    private var messageListener: ListenerRegistration? = null
    private var roomReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val currentUser = auth.currentUser

        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        myUid = currentUser.uid

        friendUid = intent.getStringExtra("friendUid") ?: ""
        friendName = intent.getStringExtra("friendName") ?: "Friend"

        if (friendUid.isBlank() || friendUid == myUid) {
            Toast.makeText(this, "Invalid friend account", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Both users must generate exactly the same room ID.
        roomId = listOf(myUid, friendUid).sorted().joinToString("_")

        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        messageInput = findViewById(R.id.messageInput)
        sendButton = findViewById(R.id.sendButton)
        chatTitle = findViewById(R.id.chatTitle)

        chatTitle.text = friendName

        adapter = ChatAdapter(messages)

        chatRecyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        chatRecyclerView.adapter = adapter

        sendButton.isEnabled = false

        prepareChatRoom()

        sendButton.setOnClickListener {
            sendMessage()
        }
    }

    private fun prepareChatRoom() {
        val roomRef = db.collection("privateChats").document(roomId)

        roomRef.get()
            .addOnSuccessListener { document ->

                if (document.exists()) {
                    val participants =
                        document.get("participants") as? List<*>

                    if (participants?.contains(myUid) != true ||
                        participants.contains(friendUid).not()
                    ) {
                        showError("Chat room participants are incorrect")
                        return@addOnSuccessListener
                    }

                    roomReady = true
                    sendButton.isEnabled = true
                    listenForMessages()
                } else {
                    val participants = listOf(myUid, friendUid).sorted()

                    val roomData = hashMapOf(
                        "participants" to participants,
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
                            showError("Chat setup failed: ${error.message}")
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
                    val message = document.toObject(ChatMessage::class.java)
                    if (message != null) {
                        messages.add(message)
                    }
                }

                adapter.notifyDataSetChanged()

                if (messages.isNotEmpty()) {
                    chatRecyclerView.scrollToPosition(messages.size - 1)
                }
            }
    }

    private fun sendMessage() {
        if (!roomReady) {
            showError("Chat is not ready yet")
            return
        }

        val text = messageInput.text.toString().trim()

        if (text.isEmpty()) {
            return
        }

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

        val roomRef = db.collection("privateChats").document(roomId)

        roomRef.collection("messages")
            .add(messageData)
            .addOnSuccessListener {
                messageInput.text.clear()

                roomRef.update(
                    "updatedAt",
                    FieldValue.serverTimestamp()
                ).addOnCompleteListener {
                    sendButton.isEnabled = true
                }
            }
            .addOnFailureListener { error ->
                sendButton.isEnabled = true
                showError("Send failed: ${error.message}")
            }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
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
