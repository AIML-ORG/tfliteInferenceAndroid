package com.example.conversationclassifier.utils;

import android.util.Log;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SMS Text Parameterization Pipeline
 * Tags various entities in SMS text to remove noise and help models understand content better.
 *
 * Processing stages:
 * 1. Tag URLs and emails as [[link]]
 * 2. Tag dates as [[date]]
 * 3. Tag currency/money amounts as [[money]]
 * 4. Tag masked identities as [[alpha_numeric]]
 * 5. Tag phone numbers as [[phone]]
 * 6. Tag alpha-numeric words as [[alpha_numeric]] (including account-number patterns)
 * 7. Tag pure numbers as [[numbers]]
 *
 * Usage example:
 * <pre>
 * String input = "Call me at 9898989898. Your bill of ₹3000.00 is due on 08OCT25";
 * String parameterized = SmsTextParameterizer.parameterize(input);
 * // Result: "Call me at [[phone]]. Your bill of [[money]] is due on [[date]]"
 * </pre>
 */
public class SmsTextParameterizer {

    private static final String TAG = "SmsTextParameterizer";

    // Pattern to avoid matching already tagged content
    private static final String NOT_TAGGED_LOOKBEHIND = "(?<!\\[\\[)";
    private static final String NOT_TAGGED_LOOKAHEAD = "(?!\\]\\])";

    // URL patterns
    private static final Pattern PATTERN_URL = Pattern.compile(
            NOT_TAGGED_LOOKBEHIND +
                    "(https?://[\\w\\-\\.]+(?:[\\w\\-\\./?#=&]+)?|www\\.[\\w\\-\\.]+(?:[\\w\\-\\./?#=&]+)?)" +
                    NOT_TAGGED_LOOKAHEAD
    );

    // Email pattern
    private static final Pattern PATTERN_EMAIL = Pattern.compile(
            NOT_TAGGED_LOOKBEHIND +
                    "\\b[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\\b" +
                    NOT_TAGGED_LOOKAHEAD
    );

    // Date formatters for java.time
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            // Standard formats with separators
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("dd-MMM-yyyy"),
            DateTimeFormatter.ofPattern("dd/MMM/yyyy"),
            DateTimeFormatter.ofPattern("dd.MMM.yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yy"),
            DateTimeFormatter.ofPattern("dd/MM/yy"),
            DateTimeFormatter.ofPattern("dd.MM.yy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd"),
            DateTimeFormatter.ofPattern("MMM dd, yyyy"),
            DateTimeFormatter.ofPattern("dd MMM yyyy"),
            DateTimeFormatter.ofPattern("dd MMM yy"),
            // Formats without spaces (edge cases)
            DateTimeFormatter.ofPattern("ddMMMyy"),
            DateTimeFormatter.ofPattern("ddMMMyyyy"),
            DateTimeFormatter.ofPattern("MMMddyy"),
            DateTimeFormatter.ofPattern("MMMddyyyy"),
            // Ordinal date formats (with th, st, nd, rd)
            DateTimeFormatter.ofPattern("d'th' MMM yyyy"),
            DateTimeFormatter.ofPattern("d'th' MMM yy"),
            DateTimeFormatter.ofPattern("d'st' MMM yyyy"),
            DateTimeFormatter.ofPattern("d'st' MMM yy"),
            DateTimeFormatter.ofPattern("d'nd' MMM yyyy"),
            DateTimeFormatter.ofPattern("d'nd' MMM yy"),
            DateTimeFormatter.ofPattern("d'rd' MMM yyyy"),
            DateTimeFormatter.ofPattern("d'rd' MMM yy"),
            DateTimeFormatter.ofPattern("d'th' MMM"),
            DateTimeFormatter.ofPattern("d'st' MMM"),
            DateTimeFormatter.ofPattern("d'nd' MMM"),
            DateTimeFormatter.ofPattern("d'rd' MMM"),
            DateTimeFormatter.ofPattern("dd'th' MMM yyyy"),
            DateTimeFormatter.ofPattern("dd'th' MMM yy"),
            DateTimeFormatter.ofPattern("dd'th' MMM")
    };

    // Regex patterns for misspelled dates (fallback)
    private static final Pattern PATTERN_DATE_MISSPELLED = Pattern.compile(
            NOT_TAGGED_LOOKBEHIND +
                    "\\b(\\d{1,2})([A-Z]{3,4})(\\d{2,4})\\b" +
                    NOT_TAGGED_LOOKAHEAD,
            Pattern.CASE_INSENSITIVE
    );

    // Pattern for ordinal dates (5th Oct, 23rd Jan, etc.) - handles with and without spaces
    private static final Pattern PATTERN_DATE_ORDINAL = Pattern.compile(
            NOT_TAGGED_LOOKBEHIND +
                    "\\b(\\d{1,2})(?:st|nd|rd|th)\\s+([A-Z]{3,9})(?:\\s+(\\d{2,4}))?\\b" +
                    NOT_TAGGED_LOOKAHEAD,
            Pattern.CASE_INSENSITIVE
    );

    // Pattern for relative dates (tomorrow, yesterday, today)
    private static final Pattern PATTERN_DATE_RELATIVE = Pattern.compile(
            NOT_TAGGED_LOOKBEHIND +
                    "\\b(tomorrow|yesterday|today)\\b" +
                    NOT_TAGGED_LOOKAHEAD,
            Pattern.CASE_INSENSITIVE
    );

    // Additional date patterns for common formats
    private static final Pattern PATTERN_DATE_DDMMYYYY = Pattern.compile(
            NOT_TAGGED_LOOKBEHIND +
                    "\\b(0?[1-9]|[12][0-9]|3[01])[-/.\\s](0?[1-9]|1[0-2])[-/.\\s](\\d{4})\\b" +
                    NOT_TAGGED_LOOKAHEAD
    );

    private static final Pattern PATTERN_DATE_DDMMYY = Pattern.compile(
            NOT_TAGGED_LOOKBEHIND +
                    "\\b(0?[1-9]|[12][0-9]|3[01])[-/.\\s](0?[1-9]|1[0-2])[-/.\\s](\\d{2})\\b" +
                    NOT_TAGGED_LOOKAHEAD
    );

    private static final Pattern PATTERN_DATE_YYYYMMDD = Pattern.compile(
            NOT_TAGGED_LOOKBEHIND +
                    "\\b(\\d{4})[-/.\\s](0?[1-9]|1[0-2])[-/.\\s](0?[1-9]|[12][0-9]|3[01])\\b" +
                    NOT_TAGGED_LOOKAHEAD
    );

    private static final Pattern PATTERN_DATE_MMDDYY = Pattern.compile(
            NOT_TAGGED_LOOKBEHIND +
                    "\\b(0?[1-9]|1[0-2])[-/.\\s](0?[1-9]|[12][0-9]|3[01])[-/.\\s](\\d{2})\\b" +
                    NOT_TAGGED_LOOKAHEAD
    );

    private static final Pattern PATTERN_DATE_DDMMMYY = Pattern.compile(
            NOT_TAGGED_LOOKBEHIND +
                    "\\b(0?[1-9]|[12][0-9]|3[01])[-/.\\s]([A-Z]{3,9})[-/.\\s](\\d{2})\\b" +
                    NOT_TAGGED_LOOKAHEAD,
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern PATTERN_DATE_DDMMMYYYY = Pattern.compile(
            NOT_TAGGED_LOOKBEHIND +
                    "\\b(0?[1-9]|[12][0-9]|3[01])[-/.\\s]([A-Z]{3,9})[-/.\\s](\\d{4})\\b" +
                    NOT_TAGGED_LOOKAHEAD,
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern PATTERN_DATE_MMMDDYY = Pattern.compile(
            NOT_TAGGED_LOOKBEHIND +
                    "\\b([A-Z]{3,9})[-/.\\s](0?[1-9]|[12][0-9]|3[01])[-/.\\s](\\d{2})\\b" +
                    NOT_TAGGED_LOOKAHEAD,
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern PATTERN_DATE_MMMDDYYYY = Pattern.compile(
            NOT_TAGGED_LOOKBEHIND +
                    "\\b([A-Z]{3,9})[-/.\\s](0?[1-9]|[12][0-9]|3[01])[-/.\\s](\\d{4})\\b" +
                    NOT_TAGGED_LOOKAHEAD,
            Pattern.CASE_INSENSITIVE
    );

    // Pattern for dates without year (e.g., "01OCT", "15JAN")
    private static final Pattern PATTERN_DATE_NO_YEAR = Pattern.compile(
            NOT_TAGGED_LOOKBEHIND +
                    "\\b(\\d{1,2})([A-Z]{3,4})\\b" +
                    NOT_TAGGED_LOOKAHEAD,
            Pattern.CASE_INSENSITIVE
    );

    // Phone number patterns - standard formats first
    // Use negative lookbehind/lookahead to avoid matching within alphanumeric words
    private static final Pattern PATTERN_PHONE_STANDARD = Pattern.compile(
            NOT_TAGGED_LOOKBEHIND +
                    "(?<![a-zA-Z0-9])(?:\\+?91[-\\s]?)?[6-9]\\d{9}(?![a-zA-Z0-9])|" +
                    "\\(?[0-9]{3}\\)?[-\\s]?[0-9]{3}[-\\s]?[0-9]{4}(?![a-zA-Z0-9])|" +
                    "(?<![a-zA-Z0-9])\\+?[1-9]\\d{7,14}(?![a-zA-Z0-9])" +
                    NOT_TAGGED_LOOKAHEAD
    );

    // Phone number pattern - spaceless (fallback)
    // Avoid matching within longer alphanumeric sequences
    private static final Pattern PATTERN_PHONE_SPACELESS = Pattern.compile(
            NOT_TAGGED_LOOKBEHIND +
                    "(?<![a-zA-Z0-9])[6-9]\\d{9}(?![a-zA-Z0-9])" +
                    NOT_TAGGED_LOOKAHEAD
    );

    // Currency symbols and codes
    private static final Pattern PATTERN_CURRENCY_SYMBOL = Pattern.compile("[₹$€£¥¢]");
    private static final Pattern PATTERN_CURRENCY_CODE = Pattern.compile(
            "\\b(?:inr|rs\\.?|rupees?|rupee|usd|eur|gbp|jpy|cny)\\b",
            Pattern.CASE_INSENSITIVE
    );

    // Money pattern - with currency prefix/postfix
    // Handles cases like "Rs.269.62" where period is part of currency code
    // Also handles commas in numbers like "INR 4,405.41"
    private static final Pattern PATTERN_MONEY_PREFIX = Pattern.compile(
            NOT_TAGGED_LOOKBEHIND +
                    "(?:[₹$€£¥¢]|\\b(?:inr|rs\\.?|rupees?|rupee|usd|eur|gbp|jpy|cny)\\b)[.\\s]*\\d+(?:,\\d{3})*(?:\\.\\d+)?\\b" +
                    NOT_TAGGED_LOOKAHEAD,
            Pattern.CASE_INSENSITIVE
    );

    // Money pattern with currency postfix
    // Handles commas in numbers like "4,405.41 INR"
    private static final Pattern PATTERN_MONEY_POSTFIX = Pattern.compile(
            NOT_TAGGED_LOOKBEHIND +
                    "\\b\\d+(?:,\\d{3})*(?:\\.\\d+)?\\s*(?:[₹$€£¥¢]|\\b(?:inr|rs\\.?|rupees?|rupee|usd|eur|gbp|jpy|cny)\\b)" +
                    NOT_TAGGED_LOOKAHEAD,
            Pattern.CASE_INSENSITIVE
    );

    // Pure numbers pattern
    // Match whole numbers and decimals (but not those already tagged as money)
    // Money amounts have exactly 2 decimal places, so we match any number including decimals
    // Use word boundaries and negative lookahead/lookbehind to ensure we don't match within alphanumeric words
    // Numbers should only contain digits, dots, and commas - not letters
    private static final Pattern PATTERN_NUMBERS = Pattern.compile(
            NOT_TAGGED_LOOKBEHIND +
                    "(?<![a-zA-Z])\\b\\d+(?:,\\d{3})*(?:\\.\\d+)?\\b(?![a-zA-Z])" +
                    NOT_TAGGED_LOOKAHEAD
    );

    // Alpha-numeric pattern - traditional alphanumeric words (letters + digits)
    private static final Pattern PATTERN_ALPHA_NUMERIC_TRADITIONAL = Pattern.compile(
            NOT_TAGGED_LOOKBEHIND +
                    "\\b(?=\\w*\\d)(?=\\w*[a-zA-Z])[a-zA-Z0-9]+\\b" +
                    NOT_TAGGED_LOOKAHEAD
    );

    // Pattern for account-number-like sequences (digits with separators)
    // Matches sequences like: 499-132***-006, 123-456-789, ABC123-456
    // Separators: hyphens, asterisks, slashes, dots
    // Must have at least 2 segments separated by separators
    private static final Pattern PATTERN_ACCOUNT_NUMBER = Pattern.compile(
            NOT_TAGGED_LOOKBEHIND +
                    "\\b(?:[\\d*]+[a-zA-Z]*|[a-zA-Z]*[\\d*]+)[-*/.]+[\\d*]+(?:[-*/.]+[\\d*]+)*\\b" +
                    NOT_TAGGED_LOOKAHEAD
    );

    /**
     * Main method to apply the complete parameterization pipeline.
     *
     * @param text Input text to parameterize
     * @return Parameterized text with all entities tagged
     */
    public static String parameterize(String text) {
        if (text == null || text.isEmpty()) {
            Log.d(TAG, "Input is null or empty");
            return text;
        }

        String originalText = text;
        Log.d(TAG, "=== SMS TEXT PARAMETERIZER ===");
        Log.d(TAG, "Original text: \"" + originalText + "\"");

        // Stage 1: Tag URLs and emails as [[link]]
        text = tagLinks(text);
        Log.d(TAG, "Stage 1: Tagged links | Result: \"" + text + "\"");

        // Stage 2: Tag dates as [[date]]
        text = tagDates(text);
        Log.d(TAG, "Stage 2: Tagged dates | Result: \"" + text + "\"");

        // Stage 3: Tag currency/money as [[money]] (before phone numbers to avoid matching currency amounts as phones)
        text = tagMoney(text);
        Log.d(TAG, "Stage 3: Tagged money | Result: \"" + text + "\"");

        // Stage 4: Tag masked identities as [[alpha_numeric]] (before phone/numbers to prevent splitting)
        text = tagMaskedIdentities(text);
        Log.d(TAG, "Stage 4: Tagged masked identities | Result: \"" + text + "\"");

        // Stage 5: Tag phone numbers as [[phone]]
        text = tagPhoneNumbers(text);
        Log.d(TAG, "Stage 5: Tagged phone numbers | Result: \"" + text + "\"");

        // Stage 6: Tag alpha-numeric as [[alpha_numeric]] (before numbers to catch account-number patterns)
        text = tagAlphaNumeric(text);
        Log.d(TAG, "Stage 6: Tagged alpha-numeric | Result: \"" + text + "\"");

        // Stage 7: Tag pure numbers as [[numbers]]
        text = tagNumbers(text);
        Log.d(TAG, "Stage 7: Tagged numbers | Final: \"" + text + "\"");

        Log.d(TAG, "SUCCESS: Parameterized text | Original: \"" + originalText + "\" | Parameterized: \"" + text + "\"");
        return text;
    }

    /**
     * Stage 1: Tag URLs and emails as [[link]]
     */
    private static String tagLinks(String text) {
        // Tag URLs - replace from end to start to preserve positions
        Matcher urlMatcher = PATTERN_URL.matcher(text);
        List<MatchPosition> urlMatches = new ArrayList<>();
        while (urlMatcher.find()) {
            urlMatches.add(new MatchPosition(urlMatcher.start(), urlMatcher.end()));
        }
        if (!urlMatches.isEmpty()) {
            urlMatches.sort((a, b) -> Integer.compare(b.start, a.start));
            StringBuffer sb = new StringBuffer(text);
            for (MatchPosition m : urlMatches) {
                sb.replace(m.start, m.end, "[[link]]");
            }
            text = sb.toString();
        }

        // Tag emails - replace from end to start to preserve positions
        Matcher emailMatcher = PATTERN_EMAIL.matcher(text);
        List<MatchPosition> emailMatches = new ArrayList<>();
        while (emailMatcher.find()) {
            emailMatches.add(new MatchPosition(emailMatcher.start(), emailMatcher.end()));
        }
        if (!emailMatches.isEmpty()) {
            emailMatches.sort((a, b) -> Integer.compare(b.start, a.start));
            StringBuffer sb = new StringBuffer(text);
            for (MatchPosition m : emailMatches) {
                sb.replace(m.start, m.end, "[[link]]");
            }
            text = sb.toString();
        }
        return text;
    }

    /**
     * Stage 2: Tag dates as [[date]]
     * Uses date parsing with fallback to regex for misspelled dates.
     */
    private static String tagDates(String text) {
        List<DateMatch> dateMatches = new ArrayList<>();

        // Find DD/MM/YYYY matches
        Matcher matcher = PATTERN_DATE_DDMMYYYY.matcher(text);
        while (matcher.find()) {
            String dateStr = matcher.group(0);
            if (isValidDate(dateStr)) {
                // Check for overlaps
                boolean overlaps = false;
                for (DateMatch dm : dateMatches) {
                    if (!(matcher.end() <= dm.start || matcher.start() >= dm.end)) {
                        overlaps = true;
                        break;
                    }
                }
                if (!overlaps) {
                    dateMatches.add(new DateMatch(matcher.start(), matcher.end(), dateStr));
                }
            }
        }

        // Find YYYY/MM/DD matches
        matcher = PATTERN_DATE_YYYYMMDD.matcher(text);
        while (matcher.find()) {
            String dateStr = matcher.group(0);
            if (isValidDate(dateStr)) {
                boolean overlaps = false;
                for (DateMatch dm : dateMatches) {
                    if (!(matcher.end() <= dm.start || matcher.start() >= dm.end)) {
                        overlaps = true;
                        break;
                    }
                }
                if (!overlaps) {
                    dateMatches.add(new DateMatch(matcher.start(), matcher.end(), dateStr));
                }
            }
        }

        // Find DD/MM/YY matches (2-digit year)
        matcher = PATTERN_DATE_DDMMYY.matcher(text);
        while (matcher.find()) {
            String dateStr = matcher.group(0);
            if (isValidDate(dateStr)) {
                boolean overlaps = false;
                for (DateMatch dm : dateMatches) {
                    if (!(matcher.end() <= dm.start || matcher.start() >= dm.end)) {
                        overlaps = true;
                        break;
                    }
                }
                if (!overlaps) {
                    dateMatches.add(new DateMatch(matcher.start(), matcher.end(), dateStr));
                }
            }
        }

        // Find MM/DD/YY matches (US format with 2-digit year)
        matcher = PATTERN_DATE_MMDDYY.matcher(text);
        while (matcher.find()) {
            String dateStr = matcher.group(0);
            if (isValidDate(dateStr)) {
                boolean overlaps = false;
                for (DateMatch dm : dateMatches) {
                    if (!(matcher.end() <= dm.start || matcher.start() >= dm.end)) {
                        overlaps = true;
                        break;
                    }
                }
                if (!overlaps) {
                    dateMatches.add(new DateMatch(matcher.start(), matcher.end(), dateStr));
                }
            }
        }

        // Find DD-MMM-YY matches (e.g., "08-Oct-25")
        matcher = PATTERN_DATE_DDMMMYY.matcher(text);
        while (matcher.find()) {
            String dateStr = matcher.group(0);
            if (isValidDate(dateStr)) {
                boolean overlaps = false;
                for (DateMatch dm : dateMatches) {
                    if (!(matcher.end() <= dm.start || matcher.start() >= dm.end)) {
                        overlaps = true;
                        break;
                    }
                }
                if (!overlaps) {
                    dateMatches.add(new DateMatch(matcher.start(), matcher.end(), dateStr));
                }
            }
        }

        // Find DD-MMM-YYYY matches (e.g., "08-Oct-2025")
        matcher = PATTERN_DATE_DDMMMYYYY.matcher(text);
        while (matcher.find()) {
            String dateStr = matcher.group(0);
            if (isValidDate(dateStr)) {
                boolean overlaps = false;
                for (DateMatch dm : dateMatches) {
                    if (!(matcher.end() <= dm.start || matcher.start() >= dm.end)) {
                        overlaps = true;
                        break;
                    }
                }
                if (!overlaps) {
                    dateMatches.add(new DateMatch(matcher.start(), matcher.end(), dateStr));
                }
            }
        }

        // Find MMM-DD-YY matches (US format, e.g., "Oct-08-25")
        matcher = PATTERN_DATE_MMMDDYY.matcher(text);
        while (matcher.find()) {
            String dateStr = matcher.group(0);
            if (isValidDate(dateStr)) {
                boolean overlaps = false;
                for (DateMatch dm : dateMatches) {
                    if (!(matcher.end() <= dm.start || matcher.start() >= dm.end)) {
                        overlaps = true;
                        break;
                    }
                }
                if (!overlaps) {
                    dateMatches.add(new DateMatch(matcher.start(), matcher.end(), dateStr));
                }
            }
        }

        // Find MMM-DD-YYYY matches (US format, e.g., "Oct-08-2025")
        matcher = PATTERN_DATE_MMMDDYYYY.matcher(text);
        while (matcher.find()) {
            String dateStr = matcher.group(0);
            if (isValidDate(dateStr)) {
                boolean overlaps = false;
                for (DateMatch dm : dateMatches) {
                    if (!(matcher.end() <= dm.start || matcher.start() >= dm.end)) {
                        overlaps = true;
                        break;
                    }
                }
                if (!overlaps) {
                    dateMatches.add(new DateMatch(matcher.start(), matcher.end(), dateStr));
                }
            }
        }

        // Find dates without year (e.g., "01OCT", "15JAN")
        matcher = PATTERN_DATE_NO_YEAR.matcher(text);
        while (matcher.find()) {
            String dateStr = matcher.group(0);
            if (isValidDateNoYear(dateStr)) {
                boolean overlaps = false;
                for (DateMatch dm : dateMatches) {
                    if (!(matcher.end() <= dm.start || matcher.start() >= dm.end)) {
                        overlaps = true;
                        break;
                    }
                }
                if (!overlaps) {
                    dateMatches.add(new DateMatch(matcher.start(), matcher.end(), dateStr));
                }
            }
        }

        // Find misspelled dates (no spaces)
        matcher = PATTERN_DATE_MISSPELLED.matcher(text);
        while (matcher.find()) {
            String dateStr = matcher.group(0);
            if (isValidDate(dateStr)) {
                boolean overlaps = false;
                for (DateMatch dm : dateMatches) {
                    if (!(matcher.end() <= dm.start || matcher.start() >= dm.end)) {
                        overlaps = true;
                        break;
                    }
                }
                if (!overlaps) {
                    dateMatches.add(new DateMatch(matcher.start(), matcher.end(), dateStr));
                }
            }
        }

        // Find ordinal dates
        matcher = PATTERN_DATE_ORDINAL.matcher(text);
        while (matcher.find()) {
            String dateStr = matcher.group(0);
            if (isValidOrdinalDate(dateStr)) {
                boolean overlaps = false;
                for (DateMatch dm : dateMatches) {
                    if (!(matcher.end() <= dm.start || matcher.start() >= dm.end)) {
                        overlaps = true;
                        break;
                    }
                }
                if (!overlaps) {
                    dateMatches.add(new DateMatch(matcher.start(), matcher.end(), dateStr));
                }
            }
        }

        // Find relative dates
        matcher = PATTERN_DATE_RELATIVE.matcher(text);
        while (matcher.find()) {
            String dateStr = matcher.group(0);
            boolean overlaps = false;
            for (DateMatch dm : dateMatches) {
                if (!(matcher.end() <= dm.start || matcher.start() >= dm.end)) {
                    overlaps = true;
                    break;
                }
            }
            if (!overlaps) {
                dateMatches.add(new DateMatch(matcher.start(), matcher.end(), dateStr));
            }
        }

        // Sort matches by start position (descending) to replace from end to start
        dateMatches.sort((a, b) -> Integer.compare(b.start, a.start));

        // Replace dates with [[date]]
        StringBuffer sb = new StringBuffer(text);
        for (DateMatch dm : dateMatches) {
            sb.replace(dm.start, dm.end, "[[date]]");
        }

        return sb.toString();
    }

    /**
     * Check if a date string can be parsed using java.time formatters
     */
    private static boolean isValidDate(String dateStr) {
        // Clean the date string
        String cleaned = dateStr.trim();

        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                // Try parsing with the formatter
                LocalDate.parse(cleaned, formatter);
                return true;
            } catch (DateTimeParseException e) {
                // Try with case-insensitive parsing for month names
                try {
                    LocalDate.parse(cleaned.toUpperCase(), formatter);
                    return true;
                } catch (DateTimeParseException e2) {
                    try {
                        LocalDate.parse(cleaned.toLowerCase(), formatter);
                        return true;
                    } catch (DateTimeParseException e3) {
                        // Try next formatter
                    }
                }
            }
        }
        return false;
    }

    /**
     * Check if a date string without year is valid (e.g., "01OCT", "15JAN")
     */
    private static boolean isValidDateNoYear(String dateStr) {
        String cleaned = dateStr.trim();

        // Try formats without year: day + month abbreviation
        DateTimeFormatter[] noYearFormatters = {
                DateTimeFormatter.ofPattern("dMMM"),
                DateTimeFormatter.ofPattern("ddMMM"),
                DateTimeFormatter.ofPattern("d MMM"),
                DateTimeFormatter.ofPattern("dd MMM"),
                DateTimeFormatter.ofPattern("MMMd"),
                DateTimeFormatter.ofPattern("MMMdd"),
                DateTimeFormatter.ofPattern("MMM d"),
                DateTimeFormatter.ofPattern("MMM dd")
        };

        for (DateTimeFormatter formatter : noYearFormatters) {
            try {
                // Use a default year for parsing (we just need to validate the format)
                LocalDate.parse(cleaned, formatter);
                return true;
            } catch (DateTimeParseException e) {
                try {
                    LocalDate.parse(cleaned.toUpperCase(), formatter);
                    return true;
                } catch (DateTimeParseException e2) {
                    try {
                        LocalDate.parse(cleaned.toLowerCase(), formatter);
                        return true;
                    } catch (DateTimeParseException e3) {
                        // Try next formatter
                    }
                }
            }
        }
        return false;
    }

    /**
     * Check if an ordinal date string is valid (e.g., "5th Oct", "23rd Jan")
     */
    private static boolean isValidOrdinalDate(String dateStr) {
        // Clean the date string
        String cleaned = dateStr.trim();

        // Try parsing with ordinal formatters (those containing 'th', 'st', 'nd', 'rd')
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            String pattern = formatter.toString();
            if (pattern.contains("th") || pattern.contains("st") || pattern.contains("nd") || pattern.contains("rd")) {
                try {
                    LocalDate.parse(cleaned, formatter);
                    return true;
                } catch (DateTimeParseException e) {
                    // Try with case-insensitive parsing for month names
                    try {
                        LocalDate.parse(cleaned.toUpperCase(), formatter);
                        return true;
                    } catch (DateTimeParseException e2) {
                        try {
                            LocalDate.parse(cleaned.toLowerCase(), formatter);
                            return true;
                        } catch (DateTimeParseException e3) {
                            // Try next formatter
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Helper class to store date match information
     */
    private static class DateMatch {
        int start;
        int end;
        String text;

        DateMatch(int start, int end, String text) {
            this.start = start;
            this.end = end;
            this.text = text;
        }
    }

    /**
     * Stage 5: Tag phone numbers as [[phone]]
     * First tries standard formats, then falls back to spaceless numbers
     * Replaces from end to start to preserve positions
     */
    private static String tagPhoneNumbers(String text) {
        // First, tag standard phone formats - replace from end to start
        Matcher standardMatcher = PATTERN_PHONE_STANDARD.matcher(text);
        List<MatchPosition> standardMatches = new ArrayList<>();
        while (standardMatcher.find()) {
            standardMatches.add(new MatchPosition(standardMatcher.start(), standardMatcher.end()));
        }
        if (!standardMatches.isEmpty()) {
            standardMatches.sort((a, b) -> Integer.compare(b.start, a.start));
            StringBuffer sb = new StringBuffer(text);
            for (MatchPosition m : standardMatches) {
                sb.replace(m.start, m.end, "[[phone]]");
            }
            text = sb.toString();
        }

        // Fallback: Tag spaceless phone numbers - replace from end to start
        Matcher spacelessMatcher = PATTERN_PHONE_SPACELESS.matcher(text);
        List<MatchPosition> spacelessMatches = new ArrayList<>();
        while (spacelessMatcher.find()) {
            spacelessMatches.add(new MatchPosition(spacelessMatcher.start(), spacelessMatcher.end()));
        }
        if (!spacelessMatches.isEmpty()) {
            spacelessMatches.sort((a, b) -> Integer.compare(b.start, a.start));
            StringBuffer sb = new StringBuffer(text);
            for (MatchPosition m : spacelessMatches) {
                sb.replace(m.start, m.end, "[[phone]]");
            }
            text = sb.toString();
        }
        return text;
    }

    /**
     * Stage 4: Tag currency/money as [[money]]
     * Validates that floats have exactly 2 decimal places
     */
    private static String tagMoney(String text) {
        List<MoneyMatch> moneyMatches = new ArrayList<>();

        // Find money with currency prefix
        Matcher prefixMatcher = PATTERN_MONEY_PREFIX.matcher(text);
        while (prefixMatcher.find()) {
            String match = prefixMatcher.group(0);
            if (isValidMoneyAmount(match)) {
                moneyMatches.add(new MoneyMatch(prefixMatcher.start(), prefixMatcher.end()));
            }
        }

        // Find money with currency postfix
        Matcher postfixMatcher = PATTERN_MONEY_POSTFIX.matcher(text);
        while (postfixMatcher.find()) {
            String match = postfixMatcher.group(0);
            // Check if this range overlaps with an existing match
            boolean overlaps = false;
            for (MoneyMatch mm : moneyMatches) {
                if (!(postfixMatcher.end() <= mm.start || postfixMatcher.start() >= mm.end)) {
                    overlaps = true;
                    break;
                }
            }
            if (!overlaps && isValidMoneyAmount(match)) {
                moneyMatches.add(new MoneyMatch(postfixMatcher.start(), postfixMatcher.end()));
            }
        }

        // Sort matches by start position (descending) to replace from end to start
        moneyMatches.sort((a, b) -> Integer.compare(b.start, a.start));

        // Replace money with [[money]]
        StringBuffer sb = new StringBuffer(text);
        for (MoneyMatch mm : moneyMatches) {
            sb.replace(mm.start, mm.end, "[[money]]");
        }

        return sb.toString();
    }

    /**
     * Check if a money string is valid.
     * Floats must have exactly 2 decimal places.
     * Handles commas in numbers (e.g., "4,405.41").
     * Removes currency codes/symbols from both beginning and end.
     */
    private static boolean isValidMoneyAmount(String moneyStr) {
        // Extract the number part (remove currency symbols/codes from both positions)
        String numberPart = moneyStr;

        // Remove currency symbols from both beginning and end
        numberPart = numberPart.replaceAll("^[₹$€£¥¢]\\s*|\\s*[₹$€£¥¢]$", "");

        // Remove currency codes from beginning (with optional punctuation and whitespace)
        // Matches: Rs., Rs, RS:, RS: , INR, etc.
        numberPart = numberPart.replaceAll("^(?i)(?:rs|inr|rupees?|rupee|usd|eur|gbp|jpy|cny)[.:]?\\s*", "");

        // Remove currency codes from end (with optional punctuation and whitespace)
        // Matches: Rs., Rs, RS:, RS: , INR, etc.
        numberPart = numberPart.replaceAll("\\s*(?i)(?:rs|inr|rupees?|rupee|usd|eur|gbp|jpy|cny)[.:]?$", "");

        numberPart = numberPart.trim();

        // Remove commas (thousands separators)
        numberPart = numberPart.replace(",", "");

        // Check for multiple decimal points (reject invalid cases like "123.45.67")
        int dotCount = 0;
        for (int i = 0; i < numberPart.length(); i++) {
            if (numberPart.charAt(i) == '.') {
                dotCount++;
            }
        }
        if (dotCount > 1) {
            return false;
        }

        // Check if it contains a decimal point
        if (numberPart.contains(".")) {
            int dotIndex = numberPart.indexOf('.');
            String afterDot = numberPart.substring(dotIndex + 1);

            // Must have exactly 2 digits after decimal
            if (afterDot.length() != 2 || !afterDot.matches("\\d{2}")) {
                return false;
            }
        }

        // Check if it's a valid number
        try {
            Double.parseDouble(numberPart);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Helper class to store money match information
     */
    private static class MoneyMatch {
        int start;
        int end;

        MoneyMatch(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }

    /**
     * Stage 4: Tag masked identities as [[alpha_numeric]]
     *
     * A masked identity is a sequence of words where:
     * 1. At least one word contains only * or X (no other characters)
     * 2. Words are separated by spaces
     * 3. Adjacent words are either mask-only (* / X) or number-only (digits)
     *
             * The entire sequence is replaced with a single [[alpha_numeric]] tag.
     * Examples:
            * - "*** 4280" -> "[[alpha_numeric]]"
            * - "4280 ***" -> "[[alpha_numeric]]"
            * - "4280 *** 4280" -> "[[alpha_numeric]]"
            * - "*** 4280 1234" -> "[[alpha_numeric]]"
            *
            * This should run before phone/number tagging to prevent splitting masked identities.
     */
    private static String tagMaskedIdentities(String text) {
        List<MatchPosition> maskedMatches = new ArrayList<>();

        // Split text into words while preserving positions
        Pattern wordPattern = Pattern.compile("\\S+");
        Matcher wordMatcher = wordPattern.matcher(text);
        List<WordPosition> wordsWithPos = new ArrayList<>();
        while (wordMatcher.find()) {
            wordsWithPos.add(new WordPosition(wordMatcher.group(0), wordMatcher.start(), wordMatcher.end()));
        }

        // Track which words have been processed to avoid duplicates
        Set<Integer> processedIndices = new HashSet<>();

        // Find mask-only words and expand sequences
        for (int i = 0; i < wordsWithPos.size(); i++) {
            WordPosition wp = wordsWithPos.get(i);
            // Skip if already processed or not a mask-only word
            if (processedIndices.contains(i) || !isMaskOnly(wp.word)) {
                continue;
            }

            // Start a sequence from this mask word
            int sequenceStart = wp.start;
            int sequenceEnd = wp.end;
            processedIndices.add(i);

            // Expand backward (to previous words)
            int j = i - 1;
            while (j >= 0) {
                WordPosition prevWp = wordsWithPos.get(j);
                if (isValidAdjacent(prevWp.word)) {
                    sequenceStart = prevWp.start;
                    processedIndices.add(j);
                    j--;
                } else {
                    break;
                }
            }

            // Expand forward (to next words)
            j = i + 1;
            while (j < wordsWithPos.size()) {
                WordPosition nextWp = wordsWithPos.get(j);
                if (isValidAdjacent(nextWp.word)) {
                    sequenceEnd = nextWp.end;
                    processedIndices.add(j);
                    j++;
                } else {
                    break;
                }
            }

            // Add the sequence match
            maskedMatches.add(new MatchPosition(sequenceStart, sequenceEnd));
        }

        // Sort matches by start position (descending) to replace from end to start
        maskedMatches.sort((a, b) -> Integer.compare(b.start, a.start));

        // Replace masked identities with [[alpha_numeric]]
        StringBuffer sb = new StringBuffer(text);
        for (MatchPosition mm : maskedMatches) {
            sb.replace(mm.start, mm.end, "[[alpha_numeric]]");
        }

        return sb.toString();
    }

    /**
     * Check if word contains only * or X (no digits, letters, or other characters).
     * Also excludes already tagged words.
     */
    private static boolean isMaskOnly(String word) {
        if (word.contains("[[") || word.contains("]]")) {
            return false;
        }
        // Word must contain only * and/or X (case insensitive)
        return word.matches("(?i)^[*X]+$");
    }

    /**
     * Check if word contains only digits (no letters, *, X, or other characters).
     * Also excludes already tagged words.
     */
    private static boolean isNumberOnly(String word) {
        if (word.contains("[[") || word.contains("]]")) {
            return false;
        }
        // Word must contain only digits
        return word.matches("^\\d+$");
    }

    /**
     * Check if word is valid for masked identity sequence (mask-only or number-only).
     */
    private static boolean isValidAdjacent(String word) {
        return isMaskOnly(word) || isNumberOnly(word);
    }

    /**
     * Helper class to store word position information
     */
    private static class WordPosition {
        String word;
        int start;
        int end;

        WordPosition(String word, int start, int end) {
            this.word = word;
            this.start = start;
            this.end = end;
        }
    }

    /**
     * Helper class to store match position information
     */
    private static class MatchPosition {
        int start;
        int end;

        MatchPosition(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }

    /**
     * Stage 7: Tag pure numbers as [[numbers]]
     * Includes decimals (numbers with decimals that don't have exactly 2 places)
     * Only matches pure numbers (digits, dots, commas) - excludes alphanumeric content
     * Runs after alpha-numeric tagging to avoid matching numbers that are part of account-number patterns
     */
    private static String tagNumbers(String text) {
        List<MatchPosition> matches = new ArrayList<>();
        Matcher numbersMatcher = PATTERN_NUMBERS.matcher(text);
        while (numbersMatcher.find()) {
            matches.add(new MatchPosition(numbersMatcher.start(), numbersMatcher.end()));
        }

        if (!matches.isEmpty()) {
            // Sort by start position (descending) to replace from end to start
            matches.sort((a, b) -> Integer.compare(b.start, a.start));
            StringBuffer sb = new StringBuffer(text);
            for (MatchPosition m : matches) {
                sb.replace(m.start, m.end, "[[numbers]]");
            }
            return sb.toString();
        }

        return text;
    }

    /**
     * Stage 6: Tag alpha-numeric words as [[alpha_numeric]]
     * Includes:
     * - Traditional alphanumeric words (letters + digits)
     * - Account-number patterns (digits separated by hyphens, asterisks, slashes, dots, etc.)
     * Runs before number tagging to catch account-number patterns like "499-132***-006"
     */
    private static String tagAlphaNumeric(String text) {
        List<MatchPosition> alphaNumericMatches = new ArrayList<>();

        // Find traditional alphanumeric words
        Matcher traditionalMatcher = PATTERN_ALPHA_NUMERIC_TRADITIONAL.matcher(text);
        while (traditionalMatcher.find()) {
            // Check for overlaps
            boolean overlaps = false;
            for (MatchPosition am : alphaNumericMatches) {
                if (!(traditionalMatcher.end() <= am.start || traditionalMatcher.start() >= am.end)) {
                    overlaps = true;
                    break;
                }
            }
            if (!overlaps) {
                alphaNumericMatches.add(new MatchPosition(traditionalMatcher.start(), traditionalMatcher.end()));
            }
        }

        // Find account-number patterns
        Matcher accountMatcher = PATTERN_ACCOUNT_NUMBER.matcher(text);
        while (accountMatcher.find()) {
            // Check for overlaps
            boolean overlaps = false;
            for (MatchPosition am : alphaNumericMatches) {
                if (!(accountMatcher.end() <= am.start || accountMatcher.start() >= am.end)) {
                    overlaps = true;
                    break;
                }
            }
            if (!overlaps) {
                alphaNumericMatches.add(new MatchPosition(accountMatcher.start(), accountMatcher.end()));
            }
        }

        if (!alphaNumericMatches.isEmpty()) {
            // Sort matches by start position (descending) to replace from end to start
            alphaNumericMatches.sort((a, b) -> Integer.compare(b.start, a.start));
            StringBuffer sb = new StringBuffer(text);
            for (MatchPosition am : alphaNumericMatches) {
                sb.replace(am.start, am.end, "[[alpha_numeric]]");
            }
            return sb.toString();
        }

        return text;
    }
}

