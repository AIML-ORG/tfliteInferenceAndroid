package com.example.conversationclassifier;

import android.content.Context;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Paths;

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;

//import org.jetbrains.tokenizers.Tokenizer;

public class DJLTokenizer {
    public static HuggingFaceTokenizer loadTokenizer(Context context) throws Exception {
        // Copy from assets to internal storage
        File file = new File(context.getFilesDir(), "all_conversations_tokenizer.json");
        if (!file.exists()) {
            try (InputStream is = context.getAssets().open("all_conversations_tokenizer.json");
                 FileOutputStream fos = new FileOutputStream(file)) {
                byte[] buffer = new byte[512];
                int length;
                while ((length = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, length);
                }
            }
        }

        // Load tokenizer
        return HuggingFaceTokenizer.newInstance(Paths.get(file.toPath().toUri()));
    }
//        File file = new File(context.getFilesDir(), "all_conversations_tokenizer.json");
//
//        // Copy tokenizer.json from assets if it does not exist yet
//        if (!file.exists()) {
//            try (InputStream input = context.getAssets().open("all_conversations_tokenizer.json");
//                 FileOutputStream output = new FileOutputStream(file)) {
//
//                byte[] buffer = new byte[1024];
//                int length;
//                while ((length = input.read(buffer)) > 0) {
//                    output.write(buffer, 0, length);
//                }
//            }
//        }
//
//        // Load tokenizer from file path
//        return Tokenizer.fromFile(file.getAbsolutePath());
//        }
}

