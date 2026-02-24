package com.automation.api.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a single CSV row of test data.
 *
 * Column conventions:
 * - testcase_id, testcase_name: metadata
 * - method: HTTP method (GET, POST, etc.)
 * - base_url_key: key in properties for base URL (defaults to baseUrl)
 * - endpoint_key: alias for endpoint path from properties
 * - url: optional full URL override
 * - request_*: request fields (body fields or query params depending on method)
 * - header_*: HTTP headers
 * - expected_*: expected values for assertions
 * - store_*: JSONPath expressions whose extracted values should be stored for chaining
 * - body_template: alias of a JSON template file under src/test/resources/json-templates
 */
public class TestCaseData {

    private final Map<String, String> raw;
    private final Map<String, String> requestFields = new HashMap<>();
    private final Map<String, String> headerFields = new HashMap<>();
    private final Map<String, String> expectedFields = new HashMap<>();
    private final Map<String, String> storeFields = new HashMap<>();

    public TestCaseData(Map<String, String> row) {
        this.raw = new HashMap<>(row);
        row.forEach((k, v) -> {
            if (k == null) {
                return;
            }
            if (v == null || v.trim().isEmpty()) {
                return;
            }
            String key = k.trim();
            String value = v.trim();
            if (key.startsWith("request_")) {
                requestFields.put(key.substring("request_".length()), value);
            } else if (key.startsWith("header_")) {
                headerFields.put(key.substring("header_".length()), value);
            } else if (key.startsWith("expected_")) {
                expectedFields.put(key.substring("expected_".length()), value);
            } else if (key.startsWith("store_")) {
                storeFields.put(key.substring("store_".length()), value);
            }
        });
    }

    public String get(String key) {
        return raw.get(key);
    }

    public String getTestCaseId() {
        return raw.getOrDefault("testcase_id", "");
    }

    public String getTestCaseName() {
        return raw.getOrDefault("testcase_name", "");
    }

    public String getMethod() {
        String value = raw.get("method");
        if (value == null || value.trim().isEmpty()) {
            return "GET";
        }
        return value.trim().toUpperCase();
    }

    public String getBaseUrlKey() {
        return raw.getOrDefault("base_url_key", "baseUrl");
    }

    public String getEndpointKey() {
        return raw.getOrDefault("endpoint_key", "");
    }

    public String getUrlOverride() {
        return raw.getOrDefault("url", "");
    }

    public String getBodyTemplateAlias() {
        return raw.getOrDefault("body_template", "");
    }

    public Map<String, String> getRequestFields() {
        return Collections.unmodifiableMap(requestFields);
    }

    public Map<String, String> getHeaderFields() {
        return Collections.unmodifiableMap(headerFields);
    }

    public Map<String, String> getExpectedFields() {
        return Collections.unmodifiableMap(expectedFields);
    }

    public Map<String, String> getStoreFields() {
        return Collections.unmodifiableMap(storeFields);
    }

    public Map<String, String> getRaw() {
        return Collections.unmodifiableMap(raw);
    }
}

