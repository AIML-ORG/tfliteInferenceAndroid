package com.example.conversationclassifier.ui

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.ComponentActivity
import com.example.conversationclassifier.DJLTokenizer
import com.example.conversationclassifier.R
import java.nio.charset.Charset

val TAG ="ntag"
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        setContent {
//            val chatViewModel: ChatViewModel = viewModel()
//            ChatScreen(chatViewModel)
//
//        }
        val button: Button? = findViewById(R.id.button)
        button?.setOnClickListener {
            val tokenizer: HuggingFaceTokenizer = DJLTokenizer.loadTokenizer(this)
            Log.i(TAG, Charset.defaultCharset().toString())
            val ids: LongArray? = tokenizer.encode("Hello how are you ?").ids
            Log.i(TAG, ids.contentToString())
            val decoded = tokenizer.decode(ids)
            Log.i(TAG, decoded)
        }
    }
}
