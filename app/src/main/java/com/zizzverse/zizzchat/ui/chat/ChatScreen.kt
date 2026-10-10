
package com.zizzverse.zizzchat.ui.chat

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.zizzverse.zizzchat.data.model.Message

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    userId: String?
) {
    val context = LocalContext.current
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid
    val receiverUid = userId.orEmpty()

    var textState by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var chatError by remember { mutableStateOf<String?>(null) }

    val messages = remember { mutableStateListOf<Message>() }
    val listState = rememberLazyListState()
    val db = remember { FirebaseFirestore.getInstance() }

    val chatId = remember(currentUid, receiverUid) {
        listOf(currentUid.orEmpty(), receiverUid)
            .sorted()
            .joinToString("_")
    }

    LaunchedEffect(currentUid, receiverUid) {
        if (currentUid == null) {
            navController.navigate("login") {
                popUpTo("home") { inclusive = true }
                launchSingleTop = true
            }
        } else if (
            receiverUid.isBlank() ||
            receiverUid == currentUid
        ) {
            chatError = "Invalid chat user."
        }
    }

    DisposableEffect(chatId, currentUid, receiverUid) {
        if (
            currentUid == null ||
            receiverUid.isBlank() ||
            receiverUid == currentUid
        ) {
            onDispose { }
        } else {
            val registration = db.collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {
                        chatError =
                            error.localizedMessage ?: "Could not load messages"
                        return@addSnapshotListener
                    }

                    messages.clear()

                    snapshot?.documents?.forEach { document ->
                        document.toObject(Message::class.java)
                            ?.let { messages.add(it) }
                    }

                    chatError = null
                }

            onDispose {
                registration.remove()
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ZizzChat") },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = textState,
                    onValueChange = { textState = it },
                    placeholder = { Text("Message...") },
                    modifier = Modifier.weight(1f),
                    maxLines = 4,
                    shape = RoundedCornerShape(24.dp)
                )

                Spacer(Modifier.width(8.dp))

                IconButton(
                    enabled = !sending &&
                        textState.isNotBlank() &&
                        currentUid != null &&
                        receiverUid.isNotBlank() &&
                        receiverUid != currentUid,
                    onClick = {
                        val messageText = textState.trim()

                        if (
                            messageText.isNotEmpty() &&
                            currentUid != null
                        ) {
                            sending = true

                            val messageRef = db.collection("chats")
                                .document(chatId)
                                .collection("messages")
                                .document()

                            val message = hashMapOf(
                                "messageId" to messageRef.id,
                                "senderId" to currentUid,
                                "receiverId" to receiverUid,
                                "messageText" to messageText,
                                "timestamp" to
                                    System.currentTimeMillis(),
                                "isRead" to false
                            )

                            messageRef.set(message)
                                .addOnSuccessListener {
                                    textState = ""
                                    sending = false
                                }
                                .addOnFailureListener { error ->
                                    sending = false

                                    Toast.makeText(
                                        context,
                                        error.localizedMessage
                                            ?: "Message failed to send",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        }
                    }
                ) {
                    if (sending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send"
                        )
                    }
                }
            }
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            chatError?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(12.dp)
                )
            }

            if (messages.isEmpty() && chatError == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No messages yet. Say hello!")
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = messages,
                        key = { it.messageId }
                    ) { message ->

                        val mine = message.senderId == currentUid

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (mine) {
                                Arrangement.End
                            } else {
                                Arrangement.Start
                            }
                        ) {
                            Text(
                                text = message.messageText,
                                modifier = Modifier
                                    .widthIn(max = 300.dp)
                                    .background(
                                        color = if (mine) {
                                            MaterialTheme.colorScheme
                                                .primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme
                                                .surfaceVariant
                                        },
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .padding(
                                        horizontal = 14.dp,
                                        vertical = 10.dp
                                    ),
                                color = if (mine) {
                                    MaterialTheme.colorScheme
                                        .onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme
                                        .onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
