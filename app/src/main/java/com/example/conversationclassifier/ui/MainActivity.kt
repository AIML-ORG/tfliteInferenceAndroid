package com.example.conversationclassifier.ui

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.activity.ComponentActivity
import com.example.conversationclassifier.DJLTokenizer
import com.example.conversationclassifier.R
import com.example.conversationclassifier.TFLiteInferenceEngine

val TAG = "ntag"

class MainActivity : ComponentActivity() {

    private var engine: TFLiteInferenceEngine? = null
    private val seqLen = 512  // Set this to your model's expected sequence length

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button: Button? = findViewById(R.id.execute)
        button?.setOnClickListener {

            // Load tokenizer
            val tokenizer: HuggingFaceTokenizer = DJLTokenizer.loadTokenizer(this)

            var etInput: EditText? = findViewById(R.id.etInput)
            val text = etInput?.text.toString()

            val encoded = tokenizer.encode(text)
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
                engine = TFLiteInferenceEngine(this, "model_v6.tflite")
            }

            // Run inference
            val output: Array<FloatArray> = engine!!.run(inputIds2D, inputMask2D)
            Log.i(TAG, "Model output: ${output.contentDeepToString()}")
            val exp0 = kotlin.math.exp(output[0][0])
            val exp1 = kotlin.math.exp(output[0][1])
            val sum = exp0 + exp1

            val p0 = exp0 / sum
            val p1 = exp1 / sum
            var textView2 = findViewById<EditText>(R.id.textView2)
            textView2.setText(p0.toString()+" "+p1.toString())
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
