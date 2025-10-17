package com.example.conversationclassifier.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.conversationclassifier.viewmodel.ChatViewModel

@Composable
fun ChatScreen(viewModel: ChatViewModel = viewModel()) {
    var meMessage by remember { mutableStateOf(TextFieldValue("")) }
    var unknownMessage by remember { mutableStateOf(TextFieldValue("")) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Bar
        TopAppBar(
            title = { Text("Chat") },
            backgroundColor = Color(0xFF075E54),
            contentColor = Color.White
        )

        // Chat Messages List
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                reverseLayout = true, // Scroll from bottom to top like WhatsApp
                modifier = Modifier.fillMaxSize()
            ) {
                items(viewModel.chatHistory) { (tag, message) ->
                    ChatBubble(tag, message)
                }
            }
        }

        // Sticky Message Input Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(8.dp)
        ) {
            MessageInputField("Me", meMessage, onValueChange = { meMessage = it }) {
                viewModel.sendMessage("Me", meMessage.text)
                meMessage = TextFieldValue("")
            }
        }
    }
}

@Composable
fun MessageInputField(tag: String, message: TextFieldValue, onValueChange: (TextFieldValue) -> Unit, onSend: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(Color.White, RoundedCornerShape(50))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = message,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = {onSend()})
        )

        Button(
            onClick = onSend,
            modifier = Modifier.padding(start = 8.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF25D366))
        ) {
            Text("Send", color = Color.White)
        }
    }
}

@Composable
fun ChatBubble(tag: String, message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        horizontalArrangement = if (tag == "me") Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (tag == "meLocalConfiguration") Color(0xFF526943) else Color(0xFFECECEC))
                .padding(10.dp)
        ) {
            Text(
                message,
                color = Color.Black,
                fontSize = 16.sp
            )
        }
    }
}
