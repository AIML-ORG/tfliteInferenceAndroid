package com.example.conversationclassifier;

import android.content.Context;
import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

public class TFLiteInferenceEngine {

    private Interpreter tflite;

    /**
     * Initialize the TFLite interpreter.
     * @param context Android context
     * @param modelPath Path to TFLite model in assets folder
     */
    public TFLiteInferenceEngine(Context context, String modelPath) {
        try {
            tflite = new Interpreter(loadModelFile(context, modelPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private MappedByteBuffer loadModelFile(Context context, String modelPath) throws IOException {
        FileInputStream inputStream = new FileInputStream(context.getAssets().openFd(modelPath).getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = context.getAssets().openFd(modelPath).getStartOffset();
        long declaredLength = context.getAssets().openFd(modelPath).getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    /**
     * Run inference with input_ids and attention_mask (INT64)
     * @param inputIds long[batch_size][seq_len]
     * @param attentionMask long[batch_size][seq_len]
     * @return float[][] model output
     */
    public float[][] run(long[][] inputIds, long[][] attentionMask) {
        Object[] inputs = new Object[]{inputIds, attentionMask};

        // Dynamically get output tensor shape
        int[] outputShape = tflite.getOutputTensor(0).shape();
        float[][] output = new float[outputShape[0]][outputShape[1]];

        Map<Integer, Object> outputs = new HashMap<>();
        outputs.put(0, output);

        tflite.runForMultipleInputsOutputs(inputs, outputs);

        return output;
    }

    public void close() {
        if (tflite != null) tflite.close();
    }
}
