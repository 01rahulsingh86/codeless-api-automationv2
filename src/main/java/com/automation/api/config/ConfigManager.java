package com.automation.api.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

public class ConfigManager {

    private static final String DEFAULT_CONFIG_FILE = "config.properties";
    private static final Properties PROPERTIES = new Properties();

    static {
        load(DEFAULT_CONFIG_FILE);
    }

    private static void load(String fileName) {
        try (InputStream is = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(fileName)) {
            if (is == null) {
                throw new IllegalStateException("Could not find config file on classpath: " + fileName);
            }
            PROPERTIES.load(is);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config file: " + fileName, e);
        }
    }

    public static String get(String key) {
        String value = PROPERTIES.getProperty(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing config key: " + key);
        }
        return value.trim();
    }

    public static String getOrDefault(String key, String defaultValue) {
        String value = PROPERTIES.getProperty(key);
        return Objects.requireNonNullElseGet(value, () -> defaultValue).trim();
    }

    /**
     * Resolve an endpoint from the properties, allowing aliases.
     * <p>
     * For example, CSV may specify "credit" and properties may contain:
     * credit=/v1/credit
     * or
     * endpoint.credit=/v1/credit
     */
    public static String resolveEndpoint(String aliasOrPath) {
        if (aliasOrPath == null || aliasOrPath.isEmpty()) {
            throw new IllegalArgumentException("Endpoint alias or path must not be empty");
        }
        // Try endpoint.<alias>
        String byEndpointPrefix = PROPERTIES.getProperty("endpoint." + aliasOrPath);
        if (byEndpointPrefix != null) {
            return byEndpointPrefix.trim();
        }
        // Try plain alias key (e.g. credit=/v1/credit)
        String byPlainKey = PROPERTIES.getProperty(aliasOrPath);
        if (byPlainKey != null) {
            return byPlainKey.trim();
        }
        // Fallback: treat as literal path from CSV
        return aliasOrPath;
    }

    public static String getBaseUrl(String key) {
        // Example: baseUrl or baseUrl.credit
        String direct = PROPERTIES.getProperty(key);
        if (direct != null) {
            return direct.trim();
        }
        String withPrefix = PROPERTIES.getProperty("baseUrl." + key);
        if (withPrefix != null) {
            return withPrefix.trim();
        }
        // Fallback to global baseUrl
        return get("baseUrl");
    }
}

