package com.example.e_clinic.ui.activities.user_screens.user_activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun AiAssistantChatScreen() {
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<Pair<Boolean, String>>() } // (isUser, text)

    Column(Modifier.fillMaxSize().padding(16.dp)) {

        /* message list */
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            reverseLayout = true          // newest at bottom
        ) {
            // reverse list for natural chat order
            items(messages.asReversed()) { (isUser, text) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        color = if (isUser)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(
                            text,
                            modifier = Modifier.padding(12.dp),
                            color = if (isUser)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        /* input row */
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Describe your symptoms…") },
                singleLine = true
            )
            IconButton(
                onClick = {
                    val prompt = input.trim()
                    if (prompt.isNotEmpty()) {
                        messages.add(true to prompt)   // show user msg
                        input = ""
                        scope.launch {
                            val reply = generateSpecializationSuggestions(prompt)
                            messages.add(false to reply) // show AI reply
                        }
                    }
                }
            ) { Icon(Icons.Default.Send, contentDescription = "Send") }
        }
    }
}
