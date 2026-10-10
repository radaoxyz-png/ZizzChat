package com.zizzverse.zizzchat.ui.home

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.zizzverse.zizzchat.data.model.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val myUid = auth.currentUser?.uid.orEmpty()

    var searchText by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(listOf<User>()) }
    var friends by remember { mutableStateOf(listOf<User>()) }
    var incoming by remember { mutableStateOf(listOf<Pair<String, User>>()) }
    var outgoingIds by remember { mutableStateOf(setOf<String>()) }
    var loading by remember { mutableStateOf(false) }
    var vaultText by remember { mutableStateOf("") }
    var savedVault by remember { mutableStateOf(listOf<String>()) }
    var username by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }

    DisposableEffect(myUid) {
        if (myUid.isBlank()) {
            onDispose { }
        } else {
            val registrations = mutableListOf<ListenerRegistration>()

            registrations += db.collection("users")
                .document(myUid)
                .addSnapshotListener { snapshot, _ ->
                    username = snapshot?.getString("name").orEmpty()
                    status = snapshot?.getString("status").orEmpty()
                }

            registrations += db.collection("friendRequests")
                .whereEqualTo("receiverId", myUid)
                .whereEqualTo("status", "pending")
                .addSnapshotListener { snapshot, _ ->
                    val requests = snapshot?.documents.orEmpty()
                    requests.forEach { request ->
                        val senderId = request.getString("senderId").orEmpty()
                        if (senderId.isNotBlank()) {
                            db.collection("users").document(senderId)
                                .get()
                                .addOnSuccessListener { userDoc ->
                                    val user = userDoc.toObject(User::class.java)
                                    if (user != null) {
                                        incoming = (incoming.filterNot {
                                            it.first == request.id
                                        } + (request.id to user))
                                    }
                                }
                        }
                    }
                    val validIds = requests.map { it.id }.toSet()
                    incoming = incoming.filter { it.first in validIds }
                }

            registrations += db.collection("friendRequests")
                .whereEqualTo("senderId", myUid)
                .whereEqualTo("status", "pending")
                .addSnapshotListener { snapshot, _ ->
                    outgoingIds = snapshot?.documents.orEmpty()
                        .mapNotNull { it.getString("receiverId") }
                        .toSet()
                }

            registrations += db.collection("friendRequests")
                .whereEqualTo("status", "accepted")
                .addSnapshotListener { snapshot, _ ->
                    val accepted = snapshot?.documents.orEmpty()
                        .filter {
                            it.getString("senderId") == myUid ||
                            it.getString("receiverId") == myUid
                        }

                    accepted.forEach { request ->
                        val otherId = if (
                            request.getString("senderId") == myUid
                        ) {
                            request.getString("receiverId").orEmpty()
                        } else {
                            request.getString("senderId").orEmpty()
                        }

                        if (otherId.isNotBlank()) {
                            db.collection("users").document(otherId)
                                .get()
                                .addOnSuccessListener { userDoc ->
                                    val user = userDoc.toObject(User::class.java)
                                    if (user != null) {
                                        friends = (friends.filterNot {
                                            it.uid == user.uid
                                        } + user).distinctBy { it.uid }
                                    }
                                }
                        }
                    }

                    val ids = accepted.mapNotNull {
                        if (it.getString("senderId") == myUid) {
                            it.getString("receiverId")
                        } else {
                            it.getString("senderId")
                        }
                    }.toSet()

                    friends = friends.filter { it.uid in ids }
                }

            registrations += db.collection("vault")
                .whereEqualTo("ownerId", myUid)
                .addSnapshotListener { snapshot, _ ->
                    savedVault = snapshot?.documents.orEmpty().mapNotNull {
                        it.getString("text")
                    }
                }

            onDispose {
                registrations.forEach { it.remove() }
            }
        }
    }

    fun searchUsers() {
        val query = searchText.trim()
        if (query.isBlank()) {
            searchResults = emptyList()
            return
        }

        loading = true
        db.collection("users").get()
            .addOnSuccessListener { snapshot ->
                searchResults = snapshot.documents.mapNotNull {
                    it.toObject(User::class.java)
                }.filter { user ->
                    user.uid != myUid &&
                    (
                        user.name.contains(query, ignoreCase = true) ||
                        user.email.contains(query, ignoreCase = true) ||
                        user.uid == query
                    )
                }
                loading = false
            }
            .addOnFailureListener { error ->
                loading = false
                Toast.makeText(
                    context,
                    error.localizedMessage ?: "Search failed",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    fun sendRequest(user: User) {
    if (myUid.isBlank() || user.uid.isBlank()) {
        Toast.makeText(
            context,
            "Please log in again",
            Toast.LENGTH_SHORT
        ).show()
        return
    }

    if (myUid == user.uid) {
        Toast.makeText(
            context,
            "You cannot add yourself",
            Toast.LENGTH_SHORT
        ).show()
        return
    }

    val request = hashMapOf(
        "requestId" to "",
        "senderId" to myUid,
        "receiverId" to user.uid,
        "status" to "pending",
        "timestamp" to System.currentTimeMillis()
    )

    db.collection("friendRequests")
        .add(request)
        .addOnSuccessListener { document ->
            document.update("requestId", document.id)
                .addOnSuccessListener {
                    Toast.makeText(
                        context,
                        "Friend request sent!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .addOnFailureListener { error ->
                    Toast.makeText(
                        context,
                        error.localizedMessage
                            ?: "Request created, but update failed",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
        .addOnFailureListener { error ->
            Toast.makeText(
                context,
                error.localizedMessage
                    ?: "Friend request failed",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun answerRequest(requestId: String, accept: Boolean) {
        db.collection("friendRequests").document(requestId)
            .update("status", if (accept) "accepted" else "rejected")
            .addOnSuccessListener {
                incoming = incoming.filterNot { it.first == requestId }
                Toast.makeText(
                    context,
                    if (accept) "Friend added" else "Request rejected",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, it.localizedMessage ?: "Action failed", Toast.LENGTH_LONG).show()
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ZizzChat") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Chat, contentDescription = "Chats") },
                    label = { Text("Chats") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.People, contentDescription = "Friends") },
                    label = { Text("Friends") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Lock, contentDescription = "Vault") },
                    label = { Text("Vault") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    Text("Chats", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(12.dp))

                    if (friends.isEmpty()) {
                        Text("No friends yet. Add friends to start chatting.")
                    } else {
                        LazyColumn {
                            items(friends, key = { it.uid }) { friend ->
                                ListItem(
                                    headlineContent = {
                                        Text(friend.name.ifBlank { friend.email })
                                    },
                                    supportingContent = { Text("Tap to open chat") },
                                    leadingContent = {
                                        Icon(Icons.Default.People, contentDescription = null)
                                    },
                                    modifier = Modifier.clickable {
                                        navController.navigate("chat/${friend.uid}")
                                    }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }

                1 -> {
                    Text("Friends", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        label = { Text("Search name, email or UID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { searchUsers() },
                        enabled = !loading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (loading) "Searching..." else "Search Users")
                    }

                    LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                        items(searchResults, key = { it.uid }) { user ->
                            ListItem(
                                headlineContent = {
                                    Text(user.name.ifBlank { "ZizzChat User" })
                                },
                                supportingContent = { Text(user.email) },
                                trailingContent = {
                                    if (friends.any { it.uid == user.uid }) {
                                        Text("Friends")
                                    } else if (outgoingIds.contains(user.uid)) {
                                        Text("Sent")
                                    } else {
                                        TextButton(onClick = { sendRequest(user) }) {
                                            Text("Add")
                                        }
                                    }
                                }
                            )
                            HorizontalDivider()
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text("Friend Requests (${incoming.size})",
                        style = MaterialTheme.typography.titleMedium)

                    LazyColumn {
                        items(incoming, key = { it.first }) { item ->
                            val (requestId, user) = item
                            ListItem(
                                headlineContent = {
                                    Text(user.name.ifBlank { user.email })
                                },
                                supportingContent = { Text("Wants to be your friend") },
                                trailingContent = {
                                    Row {
                                        TextButton(
                                            onClick = { answerRequest(requestId, true) }
                                        ) { Text("Accept") }
                                        TextButton(
                                            onClick = { answerRequest(requestId, false) }
                                        ) { Text("Reject") }
                                    }
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text("Friends List", style = MaterialTheme.typography.titleMedium)
                    LazyColumn {
                        items(friends, key = { it.uid }) { friend ->
                            ListItem(
                                headlineContent = {
                                    Text(friend.name.ifBlank { friend.email })
                                },
                                trailingContent = {
                                    TextButton(
                                        onClick = {
                                            navController.navigate("chat/${friend.uid}")
                                        }
                                    ) { Text("Chat") }
                                }
                            )
                        }
                    }
                }

                2 -> {
                    Text("Vault", style = MaterialTheme.typography.titleLarge)
                    Text("Private notes saved to your account.")
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = vaultText,
                        onValueChange = { vaultText = it },
                        label = { Text("Write a private note") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (myUid.isNotBlank() && vaultText.isNotBlank()) {
                                db.collection("vault").add(
                                    mapOf(
                                        "ownerId" to myUid,
                                        "text" to vaultText.trim(),
                                        "createdAt" to System.currentTimeMillis()
                                    )
                                ).addOnSuccessListener {
                                    vaultText = ""
                                    Toast.makeText(context, "Note saved", Toast.LENGTH_SHORT).show()
                                }.addOnFailureListener {
                                    Toast.makeText(context, it.localizedMessage ?: "Save failed", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Save Note") }

                    Spacer(Modifier.height(12.dp))
                    LazyColumn {
                        items(savedVault) { note ->
                            ListItem(headlineContent = { Text(note) })
                            HorizontalDivider()
                        }
                    }
                }

                3 -> {
                    Text("Settings", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(12.dp))
                    Text("Name: $username")
                    Text("Email: ${auth.currentUser?.email.orEmpty()}")
                    if (status.isNotBlank()) Text("Status: $status")

                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            auth.signOut()
                            navController.navigate("login") {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Log Out")
                    }
                }
            }
        }
    }
}
