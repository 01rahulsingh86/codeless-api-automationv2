package com.automation.api.reporting;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Holds per-test execution details for custom HTML summary reporting.
 */
public class TestRunRecord {

    private String testId;
    private String testName;
    private String method;
    private String url;
    private String requestBody;
    private Map<String, String> requestHeaders = new LinkedHashMap<>();
    private Map<String, String> requestQueryParams = new LinkedHashMap<>();
    private String responseStatus;
    private Map<String, String> responseHeaders = new LinkedHashMap<>();
    private String responseBody;
    private String result; // PASS / FAIL / SKIP
    private String errorMessage;

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public Map<String, String> getRequestHeaders() {
        return requestHeaders;
    }

    public void setRequestHeaders(Map<String, String> requestHeaders) {
        this.requestHeaders = requestHeaders;
    }

    public Map<String, String> getRequestQueryParams() {
        return requestQueryParams;
    }

    public void setRequestQueryParams(Map<String, String> requestQueryParams) {
        this.requestQueryParams = requestQueryParams;
    }

    public String getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(String responseStatus) {
        this.responseStatus = responseStatus;
    }

    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }

    public void setResponseHeaders(Map<String, String> responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}

