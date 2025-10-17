package com.example.conversationclassifier.ui

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.ComponentActivity
import com.example.conversationclassifier.DJLTokenizer
import com.example.conversationclassifier.R
import com.example.conversationclassifier.TFLiteInferenceEngine

val TAG = "ntag"

class MainActivity : ComponentActivity() {

    private var engine: TFLiteInferenceEngine? = null
    private val seqLen = 512  // Set this to your model's expected sequence length

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button: Button? = findViewById(R.id.button)
        button?.setOnClickListener {

            // Load tokenizer
            val tokenizer: HuggingFaceTokenizer = DJLTokenizer.loadTokenizer(this)

            // Encode text
            val encoded = tokenizer.encode("Hello how are you ?")
            val ids: LongArray = encoded.ids
            val attentionMask: LongArray = encoded.attentionMask

            Log.i(TAG, "Token IDs: ${ids.contentToString()}")
            Log.i(TAG, "Attention Mask: ${attentionMask.contentToString()}")

            // Pad input_ids and attention_mask to seq_len
            val paddedIds = LongArray(seqLen) { i -> if (i < ids.size) ids[i] else 0L }
            val paddedMask = LongArray(seqLen) { i -> if (i < attentionMask.size) attentionMask[i] else 0L }

            // Wrap in batch dimension
            val inputIds2D = arrayOf(paddedIds)
            val inputMask2D = arrayOf(paddedMask)

            // Initialize engine if needed
            if (engine == null) {
                engine = TFLiteInferenceEngine(this, "model.tflite")
            }

            // Run inference
            val output: Array<FloatArray> = engine!!.run(inputIds2D, inputMask2D)
            Log.i(TAG, "Model output: ${output.contentDeepToString()}")

            // Optional: decode tokens back to text
            val decoded: String = tokenizer.decode(ids)
            Log.i(TAG, "Decoded text: $decoded")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        engine?.close()
    }
}
