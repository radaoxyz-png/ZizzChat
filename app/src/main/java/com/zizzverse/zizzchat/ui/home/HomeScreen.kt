package com.zizzverse.zizzchat.ui.home

//========== IMPORTS START ==========
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.zizzverse.zizzchat.data.model.User
//========== IMPORTS END ==========

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {

    //========== STATE VARIABLES START ==========
    var selectedTab by remember { mutableIntStateOf(0) }

    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val myUid = auth.currentUser?.uid.orEmpty()

    var searchText by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(listOf<User>()) }
    var friends by remember { mutableStateOf(listOf<User>()) }
    var incoming by remember {
        mutableStateOf(listOf<Pair<String, User>>())
    }
    var outgoingIds by remember { mutableStateOf(setOf<String>()) }
    var loading by remember { mutableStateOf(false) }

    var vaultText by remember { mutableStateOf("") }
    var savedVault by remember { mutableStateOf(listOf<String>()) }
    var username by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    //========== STATE VARIABLES END ==========


    //========== FIREBASE LISTENERS START ==========
    DisposableEffect(myUid) {
        if (myUid.isBlank()) {
            onDispose { }
        } else {
            val registrations = mutableListOf<ListenerRegistration>()

            //========== MY PROFILE LISTENER START ==========
            registrations += db.collection("users")
                .document(myUid)
                .addSnapshotListener { snapshot, _ ->
                    username = snapshot?.getString("name").orEmpty()
                    status = snapshot?.getString("status").orEmpty()
                }
            //========== MY PROFILE LISTENER END ==========


            //========== INCOMING REQUESTS LISTENER START ==========
            registrations += db.collection("friendRequests")
                .whereEqualTo("receiverId", myUid)
                .whereEqualTo("status", "pending")
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {
                        Toast.makeText(
                            context,
                            error.localizedMessage ?: "Requests failed to load",
                            Toast.LENGTH_LONG
                        ).show()
                        return@addSnapshotListener
                    }

                    val requests = snapshot?.documents.orEmpty()
                    val validIds = requests.map { it.id }.toSet()

                    incoming = incoming.filter { it.first in validIds }

                    requests.forEach { request ->
                        val senderId =
                            request.getString("senderId").orEmpty()

                        if (senderId.isNotBlank()) {
                            db.collection("users")
                                .document(senderId)
                                .get()
                                .addOnSuccessListener { userDoc ->
                                    val user =
                                        userDoc.toObject(User::class.java)

                                    if (user != null) {
                                        incoming = (
                                            incoming.filterNot {
                                                it.first == request.id
                                            } + (request.id to user)
                                        )
                                    }
                                }
                        }
                    }
                }
            //========== INCOMING REQUESTS LISTENER END ==========


            //========== OUTGOING REQUESTS LISTENER START ==========
            registrations += db.collection("friendRequests")
                .whereEqualTo("senderId", myUid)
                .whereEqualTo("status", "pending")
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {
                        Toast.makeText(
                            context,
                            error.localizedMessage
                                ?: "Outgoing requests failed to load",
                            Toast.LENGTH_LONG
                        ).show()
                        return@addSnapshotListener
                    }

                    outgoingIds = snapshot?.documents.orEmpty()
                        .mapNotNull { it.getString("receiverId") }
                        .toSet()
                }
            //========== OUTGOING REQUESTS LISTENER END ==========


            //========== ACCEPTED REQUESTS SENT BY ME START ==========
            registrations += db.collection("friendRequests")
                .whereEqualTo("senderId", myUid)
                .whereEqualTo("status", "accepted")
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {
                        Toast.makeText(
                            context,
                            error.localizedMessage ?: "Friends failed to load",
                            Toast.LENGTH_LONG
                        ).show()
                        return@addSnapshotListener
                    }

                    snapshot?.documents.orEmpty().forEach { doc ->
                        val friendId =
                            doc.getString("receiverId").orEmpty()

                        if (friendId.isNotBlank()) {
                            db.collection("users")
                                .document(friendId)
                                .get()
                                .addOnSuccessListener { userDoc ->
                                    val user =
                                        userDoc.toObject(User::class.java)

                                    if (user != null) {
                                        friends = (
                                            friends.filterNot {
                                                it.uid == user.uid
                                            } + user
                                        ).distinctBy { it.uid }
                                    }
                                }
                        }
                    }
                }
            //========== ACCEPTED REQUESTS SENT BY ME END ==========


            //========== ACCEPTED REQUESTS RECEIVED BY ME START ==========
            registrations += db.collection("friendRequests")
                .whereEqualTo("receiverId", myUid)
                .whereEqualTo("status", "accepted")
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {
                        Toast.makeText(
                            context,
                            error.localizedMessage ?: "Friends failed to load",
                            Toast.LENGTH_LONG
                        ).show()
                        return@addSnapshotListener
                    }

                    snapshot?.documents.orEmpty().forEach { doc ->
                        val friendId =
                            doc.getString("senderId").orEmpty()

                        if (friendId.isNotBlank()) {
                            db.collection("users")
                                .document(friendId)
                                .get()
                                .addOnSuccessListener { userDoc ->
                                    val user =
                                        userDoc.toObject(User::class.java)

                                    if (user != null) {
                                        friends = (
                                            friends.filterNot {
                                                it.uid == user.uid
                                            } + user
                                        ).distinctBy { it.uid }
                                    }
                                }
                        }
                    }
                }
            //========== ACCEPTED REQUESTS RECEIVED BY ME END ==========


            //========== VAULT LISTENER START ==========
            registrations += db.collection("vault")
                .whereEqualTo("ownerId", myUid)
                .addSnapshotListener { snapshot, _ ->
                    savedVault = snapshot?.documents.orEmpty()
                        .mapNotNull { it.getString("text") }
                }
            //========== VAULT LISTENER END ==========


            //========== DO NOT DELETE: CLEANUP LISTENERS ==========
            onDispose {
                registrations.forEach { it.remove() }
            }
            //========== CLEANUP LISTENERS END ==========
        }
    }
    //========== FIREBASE LISTENERS END ==========


    //========== SEARCH USERS FUNCTION START ==========
    fun searchUsers() {
        val query = searchText.trim()

        if (query.isBlank()) {
            searchResults = emptyList()
            return
        }

        loading = true

        db.collection("users")
            .get()
            .addOnSuccessListener { snapshot ->

                searchResults = snapshot.documents
                    .mapNotNull { it.toObject(User::class.java) }
                    .filter { user ->
                        user.uid != myUid && (
                            user.name.contains(query, ignoreCase = true) ||
                            user.email.contains(query, ignoreCase = true) ||
                            user.uid == query
                        )
                    }

                loading = false

                if (searchResults.isEmpty()) {
                    Toast.makeText(
                        context,
                        "No users found",
                        Toast.LENGTH_SHORT
                    ).show()
                }
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
    //========== SEARCH USERS FUNCTION END ==========


    //========== SEND FRIEND REQUEST FUNCTION START ==========
    fun sendRequest(user: User) {
        if (myUid.isBlank() || user.uid.isBlank() || myUid == user.uid) {
            Toast.makeText(
                context,
                "Invalid user",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (friends.any { it.uid == user.uid } ||
            outgoingIds.contains(user.uid)
        ) {
            Toast.makeText(
                context,
                "Request already sent or already friends",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val requestRef = db.collection("friendRequests").document()

        val request = hashMapOf(
            "requestId" to requestRef.id,
            "senderId" to myUid,
            "receiverId" to user.uid,
            "status" to "pending",
            "timestamp" to System.currentTimeMillis()
        )

        requestRef.set(request)
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
                    "Request failed: ${
                        error.localizedMessage ?: "Permission denied"
                    }",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
    //========== SEND FRIEND REQUEST FUNCTION END ==========


    //========== ACCEPT / REJECT REQUEST FUNCTION START ==========
    fun answerRequest(requestId: String, accept: Boolean) {
        db.collection("friendRequests")
            .document(requestId)
            .update(
                "status",
                if (accept) "accepted" else "rejected"
            )
            .addOnSuccessListener {
                incoming = incoming.filterNot { it.first == requestId }

                Toast.makeText(
                    context,
                    if (accept) "Friend request accepted"
                    else "Friend request rejected",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { error ->
                Toast.makeText(
                    context,
                    error.localizedMessage ?: "Action failed",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
    //========== ACCEPT / REJECT REQUEST FUNCTION END ==========


    //========== MAIN SCAFFOLD START ==========
    Scaffold(

        //========== TOP BAR START ==========
        topBar = {
            TopAppBar(
                title = { Text("ZizzChat") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor =
                        MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        //========== TOP BAR END ==========


        //========== BOTTOM NAVIGATION START ==========
        bottomBar = {
            NavigationBar {

                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(Icons.Default.Chat, contentDescription = "Chats")
                    },
                    label = { Text("Chats") }
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(Icons.Default.People, contentDescription = "Friends")
                    },
                    label = { Text("Friends") }
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(Icons.Default.Lock, contentDescription = "Vault")
                    },
                    label = { Text("Vault") }
                )

                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    },
                    label = { Text("Settings") }
                )
            }
        }
        //========== BOTTOM NAVIGATION END ==========

    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {

            when (selectedTab) {

                //========== CHATS TAB START ==========
                0 -> {
                    Text(
                        "Chats",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(Modifier.height(12.dp))

                    if (friends.isEmpty()) {
                        Text("No friends yet. Add friends to start chatting.")
                    } else {
                        LazyColumn {
                            items(friends, key = { it.uid }) { friend ->
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            friend.name.ifBlank { friend.email }
                                        )
                                    },
                                    supportingContent = {
                                        Text("Tap to open chat")
                                    },
                                    leadingContent = {
                                        Icon(
                                            Icons.Default.People,
                                            contentDescription = null
                                        )
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
                //========== CHATS TAB END ==========


                //========== FRIENDS TAB START ==========
                1 -> {
                    Text(
                        "Friends",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(Modifier.height(12.dp))

                    //========== SEARCH INPUT START ==========
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        label = { Text("Search name, email or UID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    //========== SEARCH INPUT END ==========

                    Spacer(Modifier.height(8.dp))

                    //========== SEARCH BUTTON START ==========
                    Button(
                        onClick = { searchUsers() },
                        enabled = !loading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (loading) "Searching..." else "Search Users")
                    }
                    //========== SEARCH BUTTON END ==========

                    //========== SEARCH RESULTS START ==========
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        items(searchResults, key = { it.uid }) { user ->
                            ListItem(
                                headlineContent = {
                                    Text(
                                        user.name.ifBlank { "ZizzChat User" }
                                    )
                                },
                                supportingContent = {
                                    Text(user.email)
                                },
                                trailingContent = {
                                    when {
                                        friends.any { it.uid == user.uid } -> {
                                            Text("Friends")
                                        }

                                        outgoingIds.contains(user.uid) -> {
                                            Text("Sent")
                                        }

                                        else -> {
                                            TextButton(
                                                onClick = {
                                                    sendRequest(user)
                                                }
                                            ) {
                                                Text("Add")
                                            }
                                        }
                                    }
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                    //========== SEARCH RESULTS END ==========

                    Spacer(Modifier.height(12.dp))

                    //========== INCOMING REQUESTS UI START ==========
                    Text(
                        "Friend Requests (${incoming.size})",
                        style = MaterialTheme.typography.titleMedium
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        items(incoming, key = { it.first }) { item ->
                            val (requestId, user) = item

                            ListItem(
                                headlineContent = {
                                    Text(user.name.ifBlank { user.email })
                                },
                                supportingContent = {
                                    Text("Wants to be your friend")
                                },
                                trailingContent = {
                                    Row {
                                        TextButton(
                                            onClick = {
                                                answerRequest(requestId, true)
                                            }
                                        ) {
                                            Text("Accept")
                                        }

                                        TextButton(
                                            onClick = {
                                                answerRequest(requestId, false)
                                            }
                                        ) {
                                            Text("Reject")
                                        }
                                    }
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                    //========== INCOMING REQUESTS UI END ==========

                    Spacer(Modifier.height(12.dp))

                    //========== FRIENDS LIST UI START ==========
                    Text(
                        "Friends List",
                        style = MaterialTheme.typography.titleMedium
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        items(friends, key = { it.uid }) { friend ->
                            ListItem(
                                headlineContent = {
                                    Text(
                                        friend.name.ifBlank { friend.email }
                                    )
                                },
                                trailingContent = {
                                    TextButton(
                                        onClick = {
                                            navController.navigate(
                                                "chat/${friend.uid}"
                                            )
                                        }
                                    ) {
                                        Text("Chat")
                                    }
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                    //========== FRIENDS LIST UI END ==========
                }
                //========== FRIENDS TAB END ==========


                //========== VAULT TAB START ==========
                2 -> {
                    Text(
                        "Vault",
                        style = MaterialTheme.typography.titleLarge
                    )

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
                                db.collection("vault")
                                    .add(
                                        mapOf(
                                            "ownerId" to myUid,
                                            "text" to vaultText.trim(),
                                            "createdAt" to
                                                System.currentTimeMillis()
                                        )
                                    )
                                    .addOnSuccessListener {
                                        vaultText = ""
                                        Toast.makeText(
                                            context,
                                            "Note saved",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    .addOnFailureListener { error ->
                                        Toast.makeText(
                                            context,
                                            error.localizedMessage ?: "Save failed",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Note")
                    }

                    Spacer(Modifier.height(12.dp))

                    LazyColumn {
                        items(savedVault) { note ->
                            ListItem(
                                headlineContent = { Text(note) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
                //========== VAULT TAB END ==========


                //========== SETTINGS TAB START ==========
                3 -> {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(Modifier.height(12.dp))

                    Text("Name: $username")
                    Text("Email: ${auth.currentUser?.email.orEmpty()}")

                    if (status.isNotBlank()) {
                        Text("Status: $status")
                    }

                    Spacer(Modifier.height(16.dp))

                    //========== LOGOUT BUTTON START ==========
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
                    //========== LOGOUT BUTTON END ==========
                }
                //========== SETTINGS TAB END ==========
            }
        }
    }
    //========== MAIN SCAFFOLD END ==========
}
