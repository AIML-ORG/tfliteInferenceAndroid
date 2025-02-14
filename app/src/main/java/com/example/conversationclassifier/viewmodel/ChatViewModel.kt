package com.example.conversationclassifier.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import com.example.conversationclassifier.electra.BertTokenizer
import lombok.extern.java.Log

class ChatViewModel(application: Application) : AndroidViewModel(application){
    val chatHistory = mutableStateListOf<Pair<String, String>>() // Stores messages
    private val tokenizer = BertTokenizer(application.applicationContext)
    fun sendMessage(tag: String, message: String) {
        if (message.isNotBlank()) {
            chatHistory.add(tag to message)
        }
    }

    fun resetChat() {
        chatHistory.clear()
    }

    fun checkChat() {
        val chatHistoryString = chatHistory.joinToString(" ") { "${it.first}: ${it.second}" }
        val tokenizedResult = tokenizer.tokenize(chatHistoryString)

        // Handle the result (log it or store it in state)
        android.util.Log.d("ChatCheck", "Tokenized: $tokenizedResult")
    }
}
