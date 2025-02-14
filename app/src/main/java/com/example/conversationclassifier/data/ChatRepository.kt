package com.example.conversationclassifier.data


import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val DATASTORE_NAME = "chat_prefs"

val Context.dataStore by preferencesDataStore(name = DATASTORE_NAME)

class ChatRepository(private val context: Context) {
    companion object {
        private val CHAT_HISTORY_KEY = stringPreferencesKey("chat_history")
    }

    private val dataStoreScope = CoroutineScope(Dispatchers.IO)

    // Get chat history as a Flow
    fun getChatHistory(): Flow<List<Pair<String, String>>> {
        return context.dataStore.data.map { preferences ->
            val historyString = preferences[CHAT_HISTORY_KEY] ?: ""
            historyString.split("||").mapNotNull {
                val parts = it.split(": ", limit = 2)
                if (parts.size == 2) parts[0] to parts[1] else null
            }
        }
    }

    // Save chat history
    fun saveChatHistory(history: List<Pair<String, String>>) {
        val historyString = history.joinToString("||") { "${it.first}: ${it.second}" }
        dataStoreScope.launch {
            context.dataStore.edit { preferences ->
                preferences[CHAT_HISTORY_KEY] = historyString
            }
        }
    }

    // Clear chat history
    fun clearChatHistory() {
        dataStoreScope.launch {
            context.dataStore.edit { preferences ->
                preferences.remove(CHAT_HISTORY_KEY)
            }
        }
    }
}
