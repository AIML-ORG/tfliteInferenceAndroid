package com.example.conversationclassifier.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.conversationclassifier.viewmodel.ChatViewModel

@SuppressLint("UnrememberedMutableState")
@Composable
fun ChatScreen(viewModel: ChatViewModel = viewModel()) {
//    var meMessage by remember { mutableStateOf(TextFieldValue("")) }
//    var unknownMessage by remember { mutableStateOf(TextFieldValue("")) }
    var meMessage by remember { mutableStateOf(TextFieldValue(""))};
    var unknownMessage by remember { mutableStateOf(TextFieldValue("")) };
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Top Buttons
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = { viewModel.checkChat() }) {
                Text("Check")
            }
            Button(onClick = { viewModel.resetChat() }) {
                Text("Reset")
            }
        }

        // Chat Messages List
        LazyColumn(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
            items(viewModel.chatHistory) { (tag, message) ->
                ChatBubble(tag, message)
            }
        }

        // Input Fields & Buttons
        MessageInputField("Me", meMessage, onValueChange = { meMessage = it }) {
            viewModel.sendMessage("Me", meMessage.text)
            meMessage = TextFieldValue("")
        }

        MessageInputField("Unknown", unknownMessage, onValueChange = { unknownMessage = it }) {
            viewModel.sendMessage("Unknown", unknownMessage.text)
            unknownMessage = TextFieldValue("")
        }
    }
}

@Composable
fun MessageInputField(tag: String, message: TextFieldValue, onValueChange: (TextFieldValue) -> Unit, onSend: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        TextField(
            value = message,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Enter $tag message") }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Button(onClick = onSend) {
            Text("Send")
        }
    }
}

@Composable
fun ChatBubble(tag: String, message: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(4.dp),
        horizontalArrangement = if (tag == "Me") Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .background(if (tag == "Me") Color.Blue else Color.Gray)
                .padding(8.dp)
        ) {
            Text(message, color = Color.White, fontSize = 16.sp)
        }
    }
}

