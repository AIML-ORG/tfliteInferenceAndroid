package com.example.conversationclassifier.utils;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SMS Text Preprocessing Pipeline
 * Implements all 17 regex rules for bill-due SMS classification preprocessing.
 * Returns empty string if text fails validation, otherwise returns cleaned text.
 *
 * Usage example:
 * <pre>
 * String input = "Your bill of ₹3000 is due! Pay now!!!";
 * String cleaned = SmsTextPreprocessor.preprocess(input);
 * if (!cleaned.isEmpty()) {
 *     // Use cleaned text
 * } else {
 *     // Text failed validation
 * }
 * </pre>
 */
public class SmsTextPreprocessor {

    private static final String TAG = "SmsTextPreprocessor";

    // Pre-compiled patterns for better performance
    private static final Pattern PATTERN_LETTER = Pattern.compile("[a-zA-Z]");
    private static final Pattern PATTERN_DIGIT = Pattern.compile("\\d");
    private static final Pattern PATTERN_ALLOWED_CHARS = Pattern.compile("[^a-zA-Z0-9\\s!\"#$%&'()*+,\\-./:;<=>?@\\[\\\\\\]^_`{|}~$€£¥₹¢]");
    private static final Pattern PATTERN_WHITESPACE = Pattern.compile("[\\t ]+");
    private static final Pattern PATTERN_NEWLINES = Pattern.compile("\\n+");
    private static final Pattern PATTERN_NEWLINE_REPLACE = Pattern.compile("([^\\n]+)\\n");
    private static final Pattern PATTERN_CURRENCY_SYMBOLS = Pattern.compile("[₹$€£¥¢]");
    private static final Pattern PATTERN_CURRENCY_INR = Pattern.compile("\\binr\\b");
    private static final Pattern PATTERN_CURRENCY_RS = Pattern.compile("\\brs\\.?\\b");
    private static final Pattern PATTERN_CURRENCY_RUPEES = Pattern.compile("\\brupees?\\b");
    private static final Pattern PATTERN_CURRENCY_RUPEE = Pattern.compile("\\brupee\\b");
    private static final Pattern PATTERN_RS_DIGIT = Pattern.compile("\\brs\\s*[.:]?\\s*(\\d)");
    private static final Pattern PATTERN_R_S_DIGIT = Pattern.compile("\\br\\s+s\\s*(\\d)");
    private static final Pattern PATTERN_EXCESSIVE_CHARS = Pattern.compile("([a-zA-Z!?.,;:])\\1{3,}");
    private static final Pattern PATTERN_PUNCT_WORD = Pattern.compile("\\b[!?.,;:()\\[\\]{}" + "\"'\\-]{3,}\\b");
    private static final Pattern PATTERN_STANDALONE_PUNCT = Pattern.compile("[!?.,;:()\\[\\]{}" + "\"'\\-]{3,}");
    private static final Pattern PATTERN_SPACED_PUNCT = Pattern.compile("[!?.,;:()\\[\\]{}" + "\"'\\-](?:\\s+[!?.,;:()\\[\\]{}" + "\"'\\-]){2,}");
    private static final Pattern PATTERN_FINAL_WHITESPACE = Pattern.compile("\\s+");

    /**
     * Main method to apply the complete preprocessing pipeline.
     * Returns empty string if text fails validation, otherwise returns cleaned text.
     *
     * @param text Input text to process
     * @return Cleaned text or empty string if validation fails
     */
    public static String preprocess(String text) {
        if (text == null || text.isEmpty()) {
            Log.d(TAG, "REJECTED: Input is null or empty");
            return "";
        }

        String originalText = text;

        // Phase 1: Quick rejection
        text = phase1QuickRejection(text);
        if (text == null) {
            Log.d(TAG, "REJECTED: Phase 1 - Quick rejection failed");
            return "";
        }

        // Phase 2: Character normalization
        text = phase2CharacterNormalization(text);
        if (text == null) {
            Log.d(TAG, "REJECTED: Phase 2 - Character normalization failed (invalid characters)");
            return "";
        }

        // Phase 3: Whitespace normalization
        text = phase3WhitespaceNormalization(text);
        if (text == null) {
            Log.d(TAG, "REJECTED: Phase 3 - Whitespace normalization failed");
            return "";
        }

        // Phase 4: Currency normalization
        text = phase4CurrencyNormalization(text);
        if (text == null) {
            Log.d(TAG, "REJECTED: Phase 4 - Currency normalization failed");
            return "";
        }

        // Phase 5: Noise reduction
        text = phase5NoiseReduction(text);
        if (text == null) {
            Log.d(TAG, "REJECTED: Phase 5 - Noise reduction failed");
            return "";
        }

        // Phase 6: Final cleanup
        text = phase6FinalCleanup(text);
        if (text == null) {
            Log.d(TAG, "REJECTED: Phase 6 - Final validation failed (length < 30 after processing)");
            return "";
        }

        Log.d(TAG, "SUCCESS: Processed text | Original: \"" + originalText + "\" | Cleaned: \"" + text + "\"");
        return text;
    }

    /**
     * Phase 1: Quick Rejection Filters
     * Rule 1: Minimum Length Check (< 30 chars)
     * Rule 2: Essential Content Check (must have letter AND digit)
     */
    private static String phase1QuickRejection(String text) {
        if (text == null || text.isEmpty()) {
            Log.d(TAG, "Rule 1: Min Length Check | REJECTED: null or empty");
            return null;
        }

        String before = text;

        // Rule 1: Minimum Length Check
        if (text.length() < 30) {
            Log.d(TAG, "Rule 1: Min Length Check | REJECTED: length " + text.length() + " < 30");
            return null;
        }
        Log.d(TAG, "Rule 1: Min Length Check | PASSED: length " + text.length() + " >= 30");

        // Rule 2: Essential Content Check - must have at least one letter AND one digit
        boolean hasLetter = PATTERN_LETTER.matcher(text).find();
        boolean hasDigit = PATTERN_DIGIT.matcher(text).find();

        if (!(hasLetter && hasDigit)) {
            Log.d(TAG, "Rule 2: Essential Content Check | REJECTED: hasLetter=" + hasLetter + ", hasDigit=" + hasDigit);
            return null;
        }
        Log.d(TAG, "Rule 2: Essential Content Check | PASSED: hasLetter=" + hasLetter + ", hasDigit=" + hasDigit);

        if (!before.equals(text)) {
            Log.d(TAG, "Phase 1: Quick Rejection | " + before + " -> " + text);
        }

        return text;
    }

    /**
     * Phase 2: Initial Character-Level Normalization & Filtering
     * Rule 3: Emoji and Symbol Removal
     * Rule 4: Character Set Filtering
     * Rule 5: Case Normalization
     */
    private static String phase2CharacterNormalization(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        String before = text;

        // Rule 3: Emoji and Symbol Removal
        // Remove emojis and graphical characters using Unicode ranges
        text = removeEmojis(text);
        if (!before.equals(text)) {
            Log.d(TAG, "Rule 3: Emoji Removal | " + before + " -> " + text);
        }

        before = text;

        // Rule 4: Character Set Filtering
        // Check if any character is outside allowed set
        if (PATTERN_ALLOWED_CHARS.matcher(text).find()) {
            Log.d(TAG, "Rule 4: Character Set Filter | REJECTED: contains invalid characters");
            return null;
        }
        Log.d(TAG, "Rule 4: Character Set Filter | PASSED: all characters valid");

        // Rule 5: Case Normalization
        text = text.toLowerCase();
        if (!before.equals(text)) {
            Log.d(TAG, "Rule 5: Case Normalization | " + before + " -> " + text);
        }

        return text;
    }

    /**
     * Remove emojis and graphical characters using Unicode ranges
     */
    private static String removeEmojis(String text) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            int codePoint = text.codePointAt(i);

            // Skip emoji ranges
            if (isEmoji(codePoint)) {
                // Move to next code point (may be 1 or 2 chars for surrogate pairs)
                i += Character.charCount(codePoint);
                continue;
            }

            result.appendCodePoint(codePoint);
            i += Character.charCount(codePoint);
        }
        return result.toString();
    }

    /**
     * Check if a code point is an emoji
     */
    private static boolean isEmoji(int codePoint) {
        return (codePoint >= 0x1F600 && codePoint <= 0x1F64F) ||  // Emoticons
                (codePoint >= 0x1F300 && codePoint <= 0x1F5FF) ||  // Misc Symbols and Pictographs
                (codePoint >= 0x1F680 && codePoint <= 0x1F6FF) ||  // Transport and Map
                (codePoint >= 0x1F1E0 && codePoint <= 0x1F1FF) ||  // Flags
                (codePoint >= 0x2702 && codePoint <= 0x27B0) ||    // Dingbats
                (codePoint >= 0x24C2 && codePoint <= 0x1F251) ||   // Enclosed characters
                (codePoint >= 0x1F900 && codePoint <= 0x1F9FF) ||  // Supplemental Symbols and Pictographs
                (codePoint >= 0x1FA00 && codePoint <= 0x1FA6F) ||  // Chess Symbols
                (codePoint >= 0x1FA70 && codePoint <= 0x1FAFF) ||  // Symbols and Pictographs Extended-A
                (codePoint >= 0x2600 && codePoint <= 0x26FF) ||    // Miscellaneous Symbols
                (codePoint >= 0x2700 && codePoint <= 0x27BF);      // Dingbats
    }

    /**
     * Phase 3: Structural & Whitespace Normalization
     * Rule 6: Whitespace Unification
     * Rule 7: Newline Consolidation
     * Rule 8: Newline to Sentence Break Conversion
     */
    private static String phase3WhitespaceNormalization(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        String before = text;

        // Rule 6: Whitespace Unification
        // Replace tabs and multiple consecutive spaces with single space
        text = PATTERN_WHITESPACE.matcher(text).replaceAll(" ");
        if (!before.equals(text)) {
            Log.d(TAG, "Rule 6: Whitespace Unification | " + before + " -> " + text);
        }

        before = text;

        // Rule 7: Newline Consolidation
        // Replace multiple consecutive newlines with single newline
        text = PATTERN_NEWLINES.matcher(text).replaceAll("\n");
        if (!before.equals(text)) {
            Log.d(TAG, "Rule 7: Newline Consolidation | " + before + " -> " + text);
        }

        before = text;

        // Rule 8: Newline to Sentence Break Conversion
        // Replace single newlines with " " if preceding char is not sentence-ending punctuation
        Matcher matcher = PATTERN_NEWLINE_REPLACE.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String beforeGroup = matcher.group(1);
            if (beforeGroup != null && beforeGroup.length() > 0) {
                char lastChar = beforeGroup.charAt(beforeGroup.length() - 1);
                if (lastChar != '.' && lastChar != '!' && lastChar != '?') {
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(beforeGroup + " "));
                } else {
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
                }
            } else {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(sb);
        text = sb.toString();
        if (!before.equals(text)) {
            Log.d(TAG, "Rule 8: Newline to Sentence Break | " + before + " -> " + text);
        }

        before = text;

        // Rule 8.1: Replace exact "\n" with whitespace
        // Replace any remaining newline characters with space
        text = text.replace("\n", " ");
        if (!before.equals(text)) {
            Log.d(TAG, "Rule 8.2: Replace Newline with Space | " + before + " -> " + text);
        }

        before = text;

        // Rule 8.2: Whitespace Unification
        // Replace tabs and multiple consecutive spaces with single space
        text = PATTERN_WHITESPACE.matcher(text).replaceAll(" ");
        if (!before.equals(text)) {
            Log.d(TAG, "Rule 8.1: Whitespace Unification | " + before + " -> " + text);
        }

        return text;
    }

    /**
     * Phase 4: Semantic & Domain-Specific Replacements
     * Rule 9: Currency Normalization
     * Rule 10: Currency-Amount Spacing
     */
    private static String phase4CurrencyNormalization(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        String before = text;

        // Rule 9: Currency Normalization
        // Replace currency symbols
        text = PATTERN_CURRENCY_SYMBOLS.matcher(text).replaceAll("rs");
        if (!before.equals(text)) {
            Log.d(TAG, "Rule 9: Currency Symbols | " + before + " -> " + text);
        }

        before = text;

        // Replace currency codes (case-insensitive, but text is already lowercase)
        text = PATTERN_CURRENCY_INR.matcher(text).replaceAll("rs");
        text = PATTERN_CURRENCY_RS.matcher(text).replaceAll("rs");
        text = PATTERN_CURRENCY_RUPEES.matcher(text).replaceAll("rs");
        text = PATTERN_CURRENCY_RUPEE.matcher(text).replaceAll("rs");
        if (!before.equals(text)) {
            Log.d(TAG, "Rule 9: Currency Codes | " + before + " -> " + text);
        }

        before = text;

        // Rule 10: Currency-Amount Spacing
        // Ensure space between "rs" and subsequent digit
        text = PATTERN_RS_DIGIT.matcher(text).replaceAll("rs $1");
        text = PATTERN_R_S_DIGIT.matcher(text).replaceAll("rs $1");
        if (!before.equals(text)) {
            Log.d(TAG, "Rule 10: Currency-Amount Spacing | " + before + " -> " + text);
        }

        return text;
    }

    /**
     * Phase 5: Advanced Noise & Repetition Reduction
     * Rule 11: Excessive Character Reduction
     * Rule 12: Punctuation-Only Word Reduction
     * Rule 13: Spaced Punctuation Collapse
     * Rule 14: Consecutive Word/Phrase Reduction
     * Rule 15: Consecutive Single-Letter Word Merging
     */
    private static String phase5NoiseReduction(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        // Rule 11: Excessive Character Reduction
        // Replace sequences of more than 2 identical consecutive chars with 3
        String before = text;
        Matcher excessiveMatcher = PATTERN_EXCESSIVE_CHARS.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (excessiveMatcher.find()) {
            String charStr = excessiveMatcher.group(1);
            excessiveMatcher.appendReplacement(sb, Matcher.quoteReplacement(charStr + charStr + charStr));
        }
        excessiveMatcher.appendTail(sb);
        text = sb.toString();
        if (!before.equals(text)) {
            Log.d(TAG, "Rule 11: Excessive Char Reduction | " + before + " -> " + text);
        }

        // Rule 12: Punctuation-Only Word Reduction
        // If a word is >2 chars and consists solely of punctuation, replace with first+last
        before = text;
        Matcher punctWordMatcher = PATTERN_PUNCT_WORD.matcher(text);
        sb = new StringBuffer();
        while (punctWordMatcher.find()) {
            String punctWord = punctWordMatcher.group(0);
            if (punctWord.length() > 2) {
                String replacement = punctWord.charAt(0) + "" + punctWord.charAt(punctWord.length() - 1);
                punctWordMatcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            } else {
                punctWordMatcher.appendReplacement(sb, Matcher.quoteReplacement(punctWord));
            }
        }
        punctWordMatcher.appendTail(sb);
        text = sb.toString();
        if (!before.equals(text)) {
            Log.d(TAG, "Rule 12: Punct Word Reduction | " + before + " -> " + text);
        }

        // Also handle standalone punctuation sequences not bounded by word boundaries
        before = text;
        Matcher standalonePunctMatcher = PATTERN_STANDALONE_PUNCT.matcher(text);
        sb = new StringBuffer();
        while (standalonePunctMatcher.find()) {
            String punctSeq = standalonePunctMatcher.group(0);
            if (punctSeq.length() > 2) {
                String replacement = punctSeq.charAt(0) + "" + punctSeq.charAt(punctSeq.length() - 1);
                standalonePunctMatcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            } else {
                standalonePunctMatcher.appendReplacement(sb, Matcher.quoteReplacement(punctSeq));
            }
        }
        standalonePunctMatcher.appendTail(sb);
        text = sb.toString();
        if (!before.equals(text)) {
            Log.d(TAG, "Rule 12: Standalone Punct Reduction | " + before + " -> " + text);
        }

        // Rule 13: Spaced Punctuation Collapse
        // If >2 punctuation marks separated by spaces, replace with first+last+space
        before = text;
        Matcher spacedPunctMatcher = PATTERN_SPACED_PUNCT.matcher(text);
        sb = new StringBuffer();
        while (spacedPunctMatcher.find()) {
            String match = spacedPunctMatcher.group(0);
            List<Character> puncts = new ArrayList<>();
            for (char c : match.toCharArray()) {
                if ("!?.,;:()[]{}'\"-".indexOf(c) >= 0) {
                    puncts.add(c);
                }
            }
            if (puncts.size() > 2) {
                String replacement = puncts.get(0) + " " + puncts.get(puncts.size() - 1) + " ";
                spacedPunctMatcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            } else {
                spacedPunctMatcher.appendReplacement(sb, Matcher.quoteReplacement(match));
            }
        }
        spacedPunctMatcher.appendTail(sb);
        text = sb.toString();
        if (!before.equals(text)) {
            Log.d(TAG, "Rule 13: Spaced Punct Collapse | " + before + " -> " + text);
        }

        // Rule 14: Consecutive Word/Phrase Reduction
        // Find sequences of identical words/phrases repeating >2 times, replace with single instance
        before = text;
        String[] words = text.split("\\s+");
        if (words.length > 0) {
            List<String> resultWords = new ArrayList<>();
            int i = 0;
            while (i < words.length) {
                // Check for repeating sequences starting at position i
                int maxRepeatLen = 0;
                int maxRepeatCount = 0;

                // Try different phrase lengths (1 word, 2 words, 3 words, etc.)
                // Ensure we don't exceed array bounds: can only form phrases of length up to remaining words
                int maxPhraseLen = Math.min(6, words.length - i);
                for (int phraseLen = 1; phraseLen <= maxPhraseLen; phraseLen++) {
                    List<String> phrase = new ArrayList<>();
                    for (int k = 0; k < phraseLen; k++) {
                        if (i + k < words.length) {
                            phrase.add(words[i + k]);
                        } else {
                            break;
                        }
                    }
                    // Skip if phrase couldn't be fully formed
                    if (phrase.size() != phraseLen) {
                        continue;
                    }
                    int count = 1;
                    int j = i + phraseLen;

                    // Count how many times this phrase repeats consecutively
                    while (j + phraseLen <= words.length) {
                        boolean matches = true;
                        for (int k = 0; k < phraseLen; k++) {
                            if (!words[j + k].equals(phrase.get(k))) {
                                matches = false;
                                break;
                            }
                        }
                        if (!matches) {
                            break;
                        }
                        count++;
                        j += phraseLen;
                    }

                    if (count > 2 && phraseLen * count > maxRepeatLen * maxRepeatCount) {
                        maxRepeatLen = phraseLen;
                        maxRepeatCount = count;
                    }
                }

                if (maxRepeatCount > 2) {
                    // Add phrase once
                    for (int k = 0; k < maxRepeatLen && i + k < words.length; k++) {
                        resultWords.add(words[i + k]);
                    }
                    i += maxRepeatLen * maxRepeatCount;
                } else {
                    resultWords.add(words[i]);
                    i++;
                }
            }

            text = String.join(" ", resultWords);
        }
        if (!before.equals(text)) {
            Log.d(TAG, "Rule 14: Word/Phrase Reduction | " + before + " -> " + text);
        }

        // Rule 15: Consecutive Single-Letter Word Merging
        // Find sequences of 3+ single-letter words and merge them
        before = text;
        words = text.split("\\s+");
        if (words.length > 0) {
            List<String> resultWords = new ArrayList<>();
            int i = 0;
            while (i < words.length) {
                // Check if we have a sequence of single-letter words starting at i
                if (words[i].length() == 1 && Character.isLetter(words[i].charAt(0))) {
                    // Count consecutive single-letter words
                    int singleLetterCount = 1;
                    int j = i + 1;
                    while (j < words.length && words[j].length() == 1 &&
                            Character.isLetter(words[j].charAt(0))) {
                        singleLetterCount++;
                        j++;
                    }

                    if (singleLetterCount >= 3) {
                        // Merge them
                        StringBuilder merged = new StringBuilder();
                        for (int k = i; k < j; k++) {
                            merged.append(words[k]);
                        }
                        resultWords.add(merged.toString());
                        i = j;
                    } else {
                        resultWords.add(words[i]);
                        i++;
                    }
                } else {
                    resultWords.add(words[i]);
                    i++;
                }
            }

            text = String.join(" ", resultWords);
        }
        if (!before.equals(text)) {
            Log.d(TAG, "Rule 15: Single-Letter Merging | " + before + " -> " + text);
        }

        return text;
    }

    /**
     * Phase 6: Final Cleanup and Validation
     * Rule 16: Final Whitespace Cleanup
     * Rule 17: Final Length Validation
     */
    private static String phase6FinalCleanup(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        String before = text;

        // Rule 16: Final Whitespace Cleanup
        text = PATTERN_FINAL_WHITESPACE.matcher(text).replaceAll(" ");
        text = text.trim();
        if (!before.equals(text)) {
            Log.d(TAG, "Rule 16: Final Whitespace Cleanup | " + before + " -> " + text);
        }

        // Rule 17: Final Length Validation
        if (text.length() < 30) {
            Log.d(TAG, "Rule 17: Final Length Validation | REJECTED: length " + text.length() + " < 30");
            return null;
        }
        Log.d(TAG, "Rule 17: Final Length Validation | PASSED: length " + text.length() + " >= 30");

        return text;
    }
}

