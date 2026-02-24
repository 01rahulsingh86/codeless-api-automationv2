package com.automation.api.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Holds data extracted from previous test steps so it can be reused
 * in later rows (API chaining).
 */
public class DataContext {

    private static final Map<String, String> CONTEXT = new ConcurrentHashMap<>();

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)}|\\{\\{([^}]+)}}");

    private DataContext() {
        // utility
    }

    public static void put(String key, String value) {
        if (key != null && value != null) {
            CONTEXT.put(key, value);
        }
    }

    public static String get(String key) {
        return CONTEXT.get(key);
    }

    public static void clear() {
        CONTEXT.clear();
    }

    /**
     * Replace placeholders like ${token} or {{token}} with values
     * previously stored in the context.
     */
    public static String resolvePlaceholders(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            String value = CONTEXT.getOrDefault(key, "");
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Apply placeholder resolution for each value in a row.
     */
    public static Map<String, String> resolveRow(Map<String, String> row) {
        row.replaceAll((k, v) -> v == null ? null : resolvePlaceholders(v));
        return row;
    }
}

