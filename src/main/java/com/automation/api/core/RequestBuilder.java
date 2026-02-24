package com.automation.api.core;

import com.automation.api.config.ConfigManager;
import com.automation.api.model.TestCaseData;
import io.restassured.specification.RequestSpecification;
import io.restassured.RestAssured;

import java.util.Map;

/**
 * Builds REST Assured requests from a TestCaseData row.
 */
public class RequestBuilder {

    public static class BuiltRequest {
        private final String url;
        private final String method;
        private final RequestSpecification spec;
        private final String body;

        public BuiltRequest(String url, String method, RequestSpecification spec, String body) {
            this.url = url;
            this.method = method;
            this.spec = spec;
            this.body = body;
        }

        public String getUrl() {
            return url;
        }

        public String getMethod() {
            return method;
        }

        public RequestSpecification getSpec() {
            return spec;
        }

        public String getBody() {
            return body;
        }
    }

    public static BuiltRequest build(TestCaseData data) {
        String method = data.getMethod();

        String urlOverride = data.getUrlOverride();
        String baseUrl = ConfigManager.getBaseUrl(data.getBaseUrlKey());
        String endpointAliasOrPath = data.getEndpointKey();
        String endpointPath = endpointAliasOrPath.isEmpty()
                ? ""
                : ConfigManager.resolveEndpoint(endpointAliasOrPath);

        String url = !urlOverride.isEmpty()
                ? urlOverride
                : baseUrl + endpointPath;

        RequestSpecification spec = RestAssured.given();

        // Headers
        for (Map.Entry<String, String> header : data.getHeaderFields().entrySet()) {
            if (header.getValue() != null && !header.getValue().isEmpty()) {
                spec.header(header.getKey(), header.getValue());
            }
        }

        // Build body (and/or query params) from request_ fields
        Map<String, String> requestFields = data.getRequestFields();
        String body = null;
        if (!requestFields.isEmpty()) {
            if ("GET".equalsIgnoreCase(method)) {
                requestFields.forEach((k, v) -> {
                    if (v != null && !v.isEmpty()) {
                        spec.queryParam(k, v);
                    }
                });
            } else {
                String templateAlias = data.getBodyTemplateAlias();
                body = JsonTemplateResolver.buildBody(templateAlias, requestFields);
                spec.body(body);
                spec.contentType("application/json");
            }
        }

        return new BuiltRequest(url, method, spec, body);
    }
}

