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
import com.example.conversationclassifier.utils.BillerLookup
import com.example.conversationclassifier.utils.SmsTextPreprocessor
import com.example.conversationclassifier.utils.SmsTextParameterizer

val TAG = "ntag"

class MainActivity : ComponentActivity() {

    private var engine: TFLiteInferenceEngine? = null
    private val seqLen = 512  // Set this to your model's expected sequence length

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize BillerLookup
        initializeBillerLookup()

        val button: Button? = findViewById(R.id.execute)
        button?.setOnClickListener {

            var etInput: EditText? = findViewById(R.id.etInput)
            val rawText = etInput?.text.toString()

            Log.i(TAG, "=== SMS CLASSIFICATION PIPELINE ===")
            Log.i(TAG, "Raw input: \"$rawText\"")

            // STEP 1: Preprocessing - Clean and normalize the SMS text
            val preprocessedText = SmsTextPreprocessor.preprocess(rawText)
            if (preprocessedText.isEmpty()) {
                Log.w(TAG, "Preprocessing failed - text rejected")
                var textView2 = findViewById<EditText>(R.id.textView2)
                textView2.setText("Text rejected by preprocessor (too short or invalid)")
                return@setOnClickListener
            }
            Log.i(TAG, "Preprocessed text: \"$preprocessedText\"")

            // STEP 2: Parameterization - Tag entities (dates, money, phone numbers, etc.)
            val parameterizedText = SmsTextParameterizer.parameterize(preprocessedText)
            Log.i(TAG, "Parameterized text: \"$parameterizedText\"")

            // STEP 3: Tokenization - Convert to model input format
            val tokenizer: HuggingFaceTokenizer = DJLTokenizer.loadTokenizer(this)
            val encoded = tokenizer.encode(parameterizedText)
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

            // STEP 4: TFLite Inference - Run the model
            if (engine == null) {
                engine = TFLiteInferenceEngine(this, "model_v6.tflite")
            }

            val output: Array<FloatArray> = engine!!.run(inputIds2D, inputMask2D)
            Log.i(TAG, "Model output (logits): ${output.contentDeepToString()}")

            // STEP 5: Post-processing - Calculate probabilities
            val exp0 = kotlin.math.exp(output[0][0])
            val exp1 = kotlin.math.exp(output[0][1])
            val sum = exp0 + exp1

            val p0 = exp0 / sum  // Probability of class 0 (not bill-due)
            val p1 = exp1 / sum  // Probability of class 1 (bill-due)

            Log.i(TAG, "Probabilities: P(not-bill-due)=$p0, P(bill-due)=$p1")

            // STEP 6: Biller Type Lookup - Identify the biller
            val billerType = BillerLookup.getInstance(this).findBillerType(rawText)
            Log.i(TAG, "Biller type: $billerType")

            // Display results
            var textView2 = findViewById<EditText>(R.id.textView2)
            val resultText = """
                Probabilities:
                Not Bill-Due: ${String.format("%.4f", p0)}
                Bill-Due: ${String.format("%.4f", p1)}
                
                Predicted: ${if (p1 > p0) "BILL-DUE" else "NOT BILL-DUE"}
                Biller Type: $billerType
            """.trimIndent()
            textView2.setText(resultText)

            Log.i(TAG, "=== PIPELINE COMPLETE ===")
        }
    }

    private fun initializeBillerLookup() {
        val billerLookup = BillerLookup.getInstance(this)
        if (!billerLookup.isInitialized) {
            val success = billerLookup.initialize()
            if (success) {
                Log.i(TAG, "BillerLookup initialized with ${billerLookup.entryCount} entries")
            } else {
                Log.e(TAG, "Failed to initialize BillerLookup")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        engine?.close()
    }
}
