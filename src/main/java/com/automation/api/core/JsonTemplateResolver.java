package com.automation.api.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Resolves JSON templates from resources and applies overrides based on request_ fields.
 *
 * CSV convention:
 * - body_template column contains alias, e.g. "createCredit"
 * - request_* columns represent JSON field paths, e.g. request_user.id, request_amount
 *   which will be applied into the JSON template.
 */
public class JsonTemplateResolver {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonTemplateResolver() {
    }

    public static String buildBody(String templateAlias, Map<String, String> requestFields) {
        if (templateAlias == null || templateAlias.isEmpty()) {
            // no template, simply build a flat JSON object from request fields
            ObjectNode root = MAPPER.createObjectNode();
            requestFields.forEach(root::put);
            try {
                return MAPPER.writeValueAsString(root);
            } catch (IOException e) {
                throw new RuntimeException("Failed to serialize simple JSON body", e);
            }
        }
        String resourcePath = "json-templates/" + templateAlias + ".json";
        try (InputStream is = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalStateException("JSON template not found: " + resourcePath);
            }
            JsonNode root = MAPPER.readTree(is);
            if (!(root instanceof ObjectNode objectNode)) {
                throw new IllegalStateException("JSON template must be an object at root: " + resourcePath);
            }
            applyOverrides(objectNode, requestFields);
            return MAPPER.writeValueAsString(objectNode);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read/modify JSON template: " + resourcePath, e);
        }
    }

    private static void applyOverrides(ObjectNode root, Map<String, String> requestFields) {
        for (Map.Entry<String, String> entry : requestFields.entrySet()) {
            String path = entry.getKey();
            String value = entry.getValue();
            if (value == null) {
                continue;
            }
            setPath(root, path, value);
        }
    }

    /**
     * Apply a simple dotted-path override, e.g. "user.id" or "amount".
     */
    private static void setPath(ObjectNode root, String path, String value) {
        String[] parts = path.split("\\.");
        ObjectNode current = root;
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i == parts.length - 1) {
                current.put(part, value);
            } else {
                JsonNode child = current.get(part);
                if (child == null || !child.isObject()) {
                    child = current.putObject(part);
                }
                current = (ObjectNode) child;
            }
        }
    }
}

