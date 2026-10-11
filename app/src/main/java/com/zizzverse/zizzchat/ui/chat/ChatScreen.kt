package com.zizzverse.zizzchat.ui.chat

//========== IMPORTS START ==========
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.zizzverse.zizzchat.data.model.Message
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
//========== IMPORTS END ==========

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    userId: String?
) {
    //========== STATE START ==========
    val context = LocalContext.current
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid
    val receiverUid = userId.orEmpty()
    val db = remember { FirebaseFirestore.getInstance() }

    var textState by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var chatError by remember { mutableStateOf<String?>(null) }

    var receiverName by remember { mutableStateOf("Loading...") }
    var isOnline by remember { mutableStateOf(false) }
    var lastSeen by remember { mutableLongStateOf(0L) }
    var menuExpanded by remember { mutableStateOf(false) }

    val messages = remember { mutableStateListOf<Message>() }
    val listState = rememberLazyListState()

    val chatId = remember(currentUid, receiverUid) {
        listOf(currentUid.orEmpty(), receiverUid)
            .sorted()
            .joinToString("_")
    }

    val timeFormat = remember {
        SimpleDateFormat("hh:mm a", Locale.getDefault())
    }
    //========== STATE END ==========

    //========== AUTH CHECK START ==========
    LaunchedEffect(currentUid, receiverUid) {
        if (currentUid == null) {
            navController.navigate("login") {
                popUpTo("home") { inclusive = true }
                launchSingleTop = true
            }
        } else if (
            receiverUid.isBlank() || receiverUid == currentUid
        ) {
            chatError = "Invalid chat user."
        }
    }
    //========== AUTH CHECK END ==========

    //========== USER PROFILE LISTENER START ==========
    DisposableEffect(receiverUid, currentUid) {
        if (
            currentUid == null ||
            receiverUid.isBlank() ||
            receiverUid == currentUid
        ) {
            onDispose { }
        } else {
            val registration = db.collection("users")
                .document(receiverUid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        receiverName = "Chat"
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        receiverName =
                            snapshot.getString("name")
                                ?.takeIf { it.isNotBlank() }
                                ?: snapshot.getString("username")
                                    ?.takeIf { it.isNotBlank() }
                                ?: "ZizzChat User"

                        isOnline =
                            snapshot.getBoolean("isOnline") ?: false
                        lastSeen =
                            snapshot.getLong("lastSeen") ?: 0L
                    }
                }

            onDispose { registration.remove() }
        }
    }
    //========== USER PROFILE LISTENER END ==========

    //========== MESSAGES AND READ RECEIPTS START ==========
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
                            error.localizedMessage
                                ?: "Could not load messages"
                        return@addSnapshotListener
                    }

                    val loadedMessages = snapshot?.documents
                        .orEmpty()
                        .mapNotNull { document ->
                            document.toObject(Message::class.java)
                                ?.let { message ->
                                    if (message.messageId.isBlank()) {
                                        message.copy(
                                            messageId = document.id
                                        )
                                    } else {
                                        message
                                    }
                                }
                        }

                    messages.clear()
                    messages.addAll(loadedMessages)
                    chatError = null

                    // Mark incoming messages as read.
                    snapshot?.documents?.forEach { document ->
                        val senderId =
                            document.getString("senderId")
                        val messageReceiver =
                            document.getString("receiverId")
                        val isRead =
                            document.getBoolean("isRead") ?: false

                        if (
                            senderId == receiverUid &&
                            messageReceiver == currentUid &&
                            !isRead
                        ) {
                            document.reference.update(
                                "isRead", true
                            )
                        }
                    }
                }

            onDispose { registration.remove() }
        }
    }
    //========== MESSAGES AND READ RECEIPTS END ==========

    //========== AUTO SCROLL START ==========
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }
    //========== AUTO SCROLL END ==========

    //========== ONLINE STATUS TEXT START ==========
    val statusText = when {
        isOnline -> "Active now"
        lastSeen > 0L -> {
            val formatted = SimpleDateFormat(
                "dd MMM, hh:mm a",
                Locale.getDefault()
            ).format(Date(lastSeen))
            "Last seen $formatted"
        }
        else -> "Offline"
    }
    //========== ONLINE STATUS TEXT END ==========

    //========== MAIN SCAFFOLD START ==========
    Scaffold(
        topBar = {
            //========== HEADER START ==========
            TopAppBar(
                title = {
                    Column(
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text(
                            text = receiverName,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isOnline) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outline
                                        }
                                    )
                            )

                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOnline) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = 1
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true }
                        ) {
                            Text(
                                "⋮",
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = {
                                menuExpanded = false
                            }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Copy user ID") },
                                onClick = {
                                    menuExpanded = false
                                    val clipboard =
                                        context.getSystemService(
                                            Context.CLIPBOARD_SERVICE
                                        ) as ClipboardManager

                                    clipboard.setPrimaryClip(
                                        ClipData.newPlainText(
                                            "User ID", receiverUid
                                        )
                                    )
                                    Toast.makeText(
                                        context,
                                        "User ID copied",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Close chat") },
                                onClick = {
                                    menuExpanded = false
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            )
            //========== HEADER END ==========
        },

        bottomBar = {
            //========== MESSAGE INPUT START ==========
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
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
                                "timestamp" to System.currentTimeMillis(),
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
            //========== MESSAGE INPUT END ==========
        }
    ) { innerPadding ->

        //========== CHAT BODY START ==========
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

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = if (mine) {
                                Alignment.End
                            } else {
                                Alignment.Start
                            }
                        ) {
                            Text(
                                text = message.messageText,
                                modifier = Modifier
                                    .widthIn(max = 300.dp)
                                    .background(
                                        color = if (mine) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .padding(
                                        horizontal = 14.dp,
                                        vertical = 10.dp
                                    ),
                                color = if (mine) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )

                            //========== MESSAGE TIME AND STATUS START ==========
                            Row(
                                modifier = Modifier.padding(
                                    top = 3.dp,
                                    start = 5.dp,
                                    end = 5.dp
                                ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = if (message.timestamp > 0L) {
                                        timeFormat.format(
                                            Date(message.timestamp)
                                        )
                                    } else {
                                        ""
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (mine) {
    Text(
        text = if (message.isRead) "✓✓" else "✓",
        style = MaterialTheme.typography.labelSmall,
        color = if (message.isRead) {
            androidx.compose.ui.graphics.Color(0xFF1687F8)
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        fontWeight = FontWeight.SemiBold
    )
                                }
                            }
                            //========== MESSAGE TIME AND STATUS END ==========
                        }
                    }
                }
            }
        }
        //========== CHAT BODY END ==========
    }
    //========== MAIN SCAFFOLD END ==========
}
