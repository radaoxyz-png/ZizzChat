
package com.zizzverse.zizzchat.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.zizzverse.zizzchat.data.model.Message

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(navController: NavController, userId: String?) {
    var textState by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<Message>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("User: ${userId ?: "Unknown"}")
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
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textState,
                    onValueChange = { textState = it },
                    placeholder = { Text("Type a message...") },
                    modifier = Modifier.weight(1f),
                    maxLines = 4
                )

                IconButton(
                    onClick = {
                        if (textState.isNotBlank()) {
                            messages.add(
                                Message(
                                    messageId =
                                        System.currentTimeMillis().toString(),
                                    senderId = "me",
                                    receiverId = userId ?: "",
                                    messageText = textState.trim(),
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                            textState = ""
                        }
                    }
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send"
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp),
            reverseLayout = false
        ) {
            items(messages) { message ->
                Text(
                    text = message.messageText,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}
