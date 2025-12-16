package com.example.conversationclassifier.utils;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BillerLookup - Fast lookup utility for matching SMS headers and principal entity names
 * to determine biller types.
 *
 * Usage:
 * <pre>
 * BillerLookup.getInstance(context).initialize();
 * String billerType = BillerLookup.getInstance(context).findBillerType(inputString);
 * </pre>
 */
public class BillerLookup {

    private static final String TAG = "BillerLookup";
    private static final String JSON_FILE_NAME = "billers_classified_V1.json";

    private static BillerLookup instance;
    private Context context;

    // Data structure for fast lookup
    // Key: composite key "principal_entity_name|sms_header"
    // Value: biller_type
    private Map<String, String> lookupMap;

    // List of all biller entries for iteration
    private List<BillerEntry> billerEntries;

    // Flag to track if data is loaded
    private boolean isInitialized = false;

    /**
     * Inner class representing a biller entry
     */
    public static class BillerEntry {
        public String biller_type;
        public String principal_entity_name;
        public String sms_header;

        public BillerEntry(String billerType, String principalEntityName, String smsHeader) {
            this.biller_type = billerType;
            this.principal_entity_name = principalEntityName;
            this.sms_header = smsHeader;
        }
    }

    /**
     * Private constructor for singleton pattern
     */
    private BillerLookup(Context context) {
        this.context = context.getApplicationContext();
        this.lookupMap = new HashMap<>();
        this.billerEntries = new ArrayList<>();
    }

    /**
     * Get singleton instance
     */
    public static synchronized BillerLookup getInstance(Context context) {
        if (instance == null) {
            instance = new BillerLookup(context);
        }
        return instance;
    }

    /**
     * Initialize the lookup by loading JSON file and building the lookup map.
     * This should be called once, preferably in Application class or onCreate of main activity.
     *
     * @return true if initialization successful, false otherwise
     */
    public boolean initialize() {
        if (isInitialized) {
            Log.d(TAG, "Already initialized");
            return true;
        }

        try {
            // Read JSON file from assets folder
            InputStream inputStream = context.getAssets().open(JSON_FILE_NAME);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

            // Read entire file content
            StringBuilder jsonString = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonString.append(line);
            }
            reader.close();
            inputStream.close();

            // Parse JSON array
            JSONArray jsonArray = new JSONArray(jsonString.toString());

            // Process each entry
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);

                String billerType = jsonObject.optString("biller_type", "");
                String principalEntityName = jsonObject.optString("principal_entity_name", "");
                String smsHeader = jsonObject.optString("sms_header", "");

                if (!principalEntityName.isEmpty() && !smsHeader.isEmpty()) {
                    BillerEntry entry = new BillerEntry(billerType, principalEntityName, smsHeader);
                    billerEntries.add(entry);

                    // Build lookup map with composite key
                    String compositeKey = principalEntityName + "|" + smsHeader;
                    lookupMap.put(compositeKey, billerType);
                }
            }

            isInitialized = true;
            Log.d(TAG, "Initialized successfully. Loaded " + billerEntries.size() + " biller entries");
            return true;

        } catch (IOException e) {
            Log.e(TAG, "Error reading JSON file: " + e.getMessage(), e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error initializing BillerLookup: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Find biller type by matching principal_entity_name and sms_header in the input string.
     *
     * Matching logic:
     * 1. First, find exact match for principal_entity_name in the input string
     * 2. Then, find exact match for sms_header in the input string
     * 3. If both found, return the corresponding biller_type
     * 4. Else return "unsupported"
     *
     * @param inputString The string to search in
     * @return biller_type if match found, "unsupported" otherwise
     */
    public String findBillerType(String inputString) {
        if (!isInitialized) {
            Log.w(TAG, "Not initialized. Call initialize() first.");
            return "unsupported";
        }

        if (inputString == null || inputString.isEmpty()) {
            return "unsupported";
        }

        // Iterate through all biller entries
        for (BillerEntry entry : billerEntries) {
            if (entry.principal_entity_name == null || entry.sms_header == null) {
                continue;
            }

            // Check if input string contains exact match for principal_entity_name
            boolean hasPrincipalEntity = inputString.contains(entry.principal_entity_name);

            // Check if input string contains exact match for sms_header
            boolean hasSmsHeader = inputString.contains(entry.sms_header);

            // If both found, return the biller_type
            if (hasPrincipalEntity && hasSmsHeader) {
                Log.d(TAG, "Match found: " + entry.principal_entity_name + " | " + entry.sms_header + " -> " + entry.biller_type);
                return entry.biller_type;
            }
        }

        // No match found
        return "unsupported";
    }

    /**
     * Get the number of biller entries loaded
     */
    public int getEntryCount() {
        return billerEntries.size();
    }

    /**
     * Check if the lookup is initialized
     */
    public boolean isInitialized() {
        return isInitialized;
    }

    /**
     * Reset the instance (useful for testing or re-initialization)
     */
    public void reset() {
        lookupMap.clear();
        billerEntries.clear();
        isInitialized = false;
    }
}
