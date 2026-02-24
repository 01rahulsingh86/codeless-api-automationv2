package com.automation.api.core;

import com.automation.api.model.TestCaseData;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.Assert;

import java.util.Map;

/**
 * Applies assertions based on expected_ fields in CSV.
 *
 * Supported conventions:
 * - expected_status: HTTP status code (e.g. 200)
 * - expected_body_contains: substring that must appear in response body
 */
public class ResponseValidator {

    public static void validate(Response response, TestCaseData data) {
        Map<String, String> expected = data.getExpectedFields();

        String status = expected.get("status");
        if (status != null && !status.isEmpty()) {
            int expectedStatus = Integer.parseInt(status.trim());
            Assert.assertEquals(response.statusCode(), expectedStatus,
                    "HTTP status code mismatch for test " + data.getTestCaseId());
        }

        String bodyContains = expected.get("body_contains");
        if (bodyContains != null && !bodyContains.isEmpty()) {
            String body = response.getBody().asString();
            Assert.assertTrue(body.contains(bodyContains),
                    "Response body does not contain expected text for test "
                            + data.getTestCaseId() + ": " + bodyContains);
        }

        // Additional JSONPath-based expectations:
        // expected_json_<path> = value  -> path is dotted, e.g. user.id
        String contentType = response.getContentType();
        if (contentType != null && contentType.contains("application/json")) {
            JsonPath jsonPath = response.jsonPath();
            for (Map.Entry<String, String> entry : expected.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("json_")) {
                    String jsonPathExpr = key.substring("json_".length()); // e.g. user.id
                    String expectedValue = entry.getValue();
                    if (expectedValue == null || expectedValue.isEmpty()) {
                        continue;
                    }
                    Object actual = jsonPath.get(jsonPathExpr);
                    Assert.assertNotNull(actual,
                            "JSONPath " + jsonPathExpr + " not found in response for test " + data.getTestCaseId());
                    Assert.assertEquals(String.valueOf(actual), expectedValue,
                            "Mismatch for JSONPath " + jsonPathExpr + " in test " + data.getTestCaseId());
                }
            }
        }
    }
}

