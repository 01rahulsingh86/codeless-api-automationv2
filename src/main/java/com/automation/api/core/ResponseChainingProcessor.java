package com.automation.api.core;

import com.automation.api.model.TestCaseData;
import io.restassured.path.json.JsonPath;
import io.restassured.path.json.exception.JsonPathException;
import io.restassured.response.Response;

import java.util.Map;

/**
 * Extracts values from responses and stores them into DataContext
 * based on store_* columns in CSV.
 *
 * Convention:
 * - store_token = data.token   (JSONPath "data.token" used to extract value)
 *   makes ${token} (or {{token}}) available for subsequent rows.
 */
public class ResponseChainingProcessor {

    public static void capture(Response response, TestCaseData data) {
        Map<String, String> storeFields = data.getStoreFields();
        if (storeFields.isEmpty()) {
            return;
        }
        String contentType = response.getContentType();
        if (contentType == null || !contentType.contains("application/json")) {
            // do not attempt JSON extraction if response is not JSON
            return;
        }

        JsonPath jsonPath;
        try {
            jsonPath = response.jsonPath();
        } catch (JsonPathException e) {
            // response is not valid JSON; skip chaining for this step
            return;
        }
        for (Map.Entry<String, String> entry : storeFields.entrySet()) {
            String alias = entry.getKey();      // e.g. token
            String path = entry.getValue();     // e.g. data.token
            if (path == null || path.isEmpty()) {
                continue;
            }
            Object value = jsonPath.get(path);
            if (value != null) {
                DataContext.put(alias, String.valueOf(value));
            }
        }
    }
}

