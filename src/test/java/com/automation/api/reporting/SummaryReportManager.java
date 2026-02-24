package com.automation.api.reporting;

import com.automation.api.core.RequestBuilder;
import com.automation.api.model.TestCaseData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.restassured.response.Response;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map;

/**
 * Collects per-test data during execution and writes a compact HTML
 * summary report with one row per test and expandable JSON columns.
 */
public class SummaryReportManager {

    private static final List<TestRunRecord> RECORDS =
            Collections.synchronizedList(new ArrayList<>());

    private static final ThreadLocal<TestRunRecord> CURRENT = new ThreadLocal<>();

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private SummaryReportManager() {
    }

    public static void start(TestCaseData data, RequestBuilder.BuiltRequest built) {
        TestRunRecord record = new TestRunRecord();
        record.setTestId(nullToEmpty(data.getTestCaseId()));
        record.setTestName(nullToEmpty(data.getTestCaseName()));
        record.setMethod(nullToEmpty(built.getMethod()));
        record.setUrl(nullToEmpty(built.getUrl()));
        record.setRequestHeaders(new LinkedHashMap<>(data.getHeaderFields()));
        record.setRequestQueryParams(new LinkedHashMap<>(data.getRequestFields()));
        record.setRequestBody(prettyIfJson(nullToEmpty(built.getBody())));
        RECORDS.add(record);
        CURRENT.set(record);
    }

    public static void attachResponse(Response response) {
        TestRunRecord record = CURRENT.get();
        if (record == null || response == null) {
            return;
        }
        record.setResponseStatus(String.valueOf(response.getStatusCode()));
        record.setResponseHeaders(flattenHeaders(response));

        String body = response.getBody().asString();
        record.setResponseBody(truncate(prettyIfJson(body), 4000));
    }

    public static void markResult(String result, Throwable error) {
        TestRunRecord record = CURRENT.get();
        if (record == null) {
            return;
        }
        record.setResult(result);
        if (error != null) {
            record.setErrorMessage(truncate(error.getMessage(), 1000));
        }
    }

    public static void clearCurrent() {
        CURRENT.remove();
    }

    public static void writeHtmlSummary(String outputPath) {
        List<TestRunRecord> snapshot;
        synchronized (RECORDS) {
            snapshot = new ArrayList<>(RECORDS);
        }

        Path path = Paths.get(outputPath);
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            // If we cannot create directories, just bail out quietly.
            return;
        }

        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("<!DOCTYPE html>\n");
            writer.write("<html lang=\"en\">\n");
            writer.write("<head>\n");
            writer.write("<meta charset=\"UTF-8\" />\n");
            writer.write("<title>REST API CSV Automation Summary</title>\n");
            writer.write("<style>\n");
            writer.write("body { font-family: system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; font-size: 14px; margin: 16px; }\n");
            writer.write("h1 { font-size: 20px; margin-bottom: 8px; }\n");
            writer.write("table { border-collapse: collapse; width: 100%; table-layout: auto; }\n");
            writer.write("th, td { border: 1px solid #ddd; padding: 6px 8px; vertical-align: top; word-wrap: break-word; }\n");
            writer.write("th { background-color: #f4f4f4; position: sticky; top: 0; z-index: 1; }\n");
            writer.write(".pass { background-color: #e6ffed; }\n");
            writer.write(".fail { background-color: #ffecec; }\n");
            writer.write(".skip { background-color: #fff8e1; }\n");
            writer.write(".status-badge { font-weight: 600; padding: 2px 8px; border-radius: 999px; color: #ffffff; display: inline-block; }\n");
            writer.write(".status-pass { background-color: #2e7d32; }\n");
            writer.write(".status-fail { background-color: #c62828; }\n");
            writer.write(".status-skip { background-color: #f9a825; }\n");
            writer.write("details > summary { cursor: pointer; color: #1976d2; }\n");
            writer.write("pre { white-space: pre-wrap; margin: 4px 0 0 0; }\n");
            writer.write(".section { margin-top: 4px; }\n");
            writer.write(".section-title { font-weight: 600; margin-top: 4px; margin-bottom: 2px; }\n");
            writer.write(".spacer { height: 4px; }\n");
            writer.write(".kv { width: 100%; border-collapse: collapse; }\n");
            writer.write(".kv td { border: 1px solid #eee; padding: 4px 6px; vertical-align: top; }\n");
            writer.write(".kv td.key { width: 38%; font-weight: 600; background: #fafafa; }\n");
            writer.write("</style>\n");
            writer.write("</head>\n");
            writer.write("<body>\n");
            writer.write("<h1>REST API CSV Automation Summary</h1>\n");
            writer.write("<p>Generated at "
                    + escapeHtml(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    + "</p>\n");

            writer.write("<table>\n");
            writer.write("<thead><tr>");
            writer.write("<th>Test ID</th>");
            writer.write("<th>Description</th>");
            writer.write("<th>Method</th>");
            writer.write("<th>URL</th>");
            writer.write("<th style=\"width: 26%;\">Request</th>");
            writer.write("<th>Status</th>");
            writer.write("<th>Result</th>");
            writer.write("<th>Error</th>");
            writer.write("<th style=\"width: 26%;\">Response</th>");
            writer.write("</tr></thead>\n");
            writer.write("<tbody>\n");

            for (TestRunRecord record : snapshot) {
                String result = nullToEmpty(record.getResult());
                String rowClass = switch (result.toUpperCase()) {
                    case "PASS" -> "pass";
                    case "FAIL" -> "fail";
                    case "SKIP" -> "skip";
                    default -> "";
                };
                writer.write("<tr class=\"" + rowClass + "\">");
                writer.write("<td>" + escapeHtml(record.getTestId()) + "</td>");
                writer.write("<td>" + escapeHtml(record.getTestName()) + "</td>");
                writer.write("<td>" + escapeHtml(record.getMethod()) + "</td>");
                writer.write("<td>" + escapeHtml(record.getUrl()) + "</td>");

                // Request (headers + params/body)
                writer.write("<td>");
                writeRequestCell(writer, record);
                writer.write("</td>");

                // Response status
                writer.write("<td>" + escapeHtml(nullToEmpty(record.getResponseStatus())) + "</td>");

                // Result badge
                String badgeClass = switch (result.toUpperCase()) {
                    case "PASS" -> "status-badge status-pass";
                    case "FAIL" -> "status-badge status-fail";
                    case "SKIP" -> "status-badge status-skip";
                    default -> "status-badge";
                };
                writer.write("<td><span class=\"" + badgeClass + "\">" + escapeHtml(result) + "</span></td>");

                // Error
                writer.write("<td>" + escapeHtml(nullToEmpty(record.getErrorMessage())) + "</td>");

                // Response (headers + body)
                writer.write("<td>");
                writeResponseCell(writer, record);
                writer.write("</td>");

                writer.write("</tr>\n");
            }

            writer.write("</tbody>\n");
            writer.write("</table>\n");
            writer.write("</body>\n");
            writer.write("</html>\n");
        } catch (IOException e) {
            // ignore failure to write report
        }
    }

    private static void writeRequestCell(BufferedWriter writer, TestRunRecord record) throws IOException {
        Map<String, String> headers = record.getRequestHeaders();
        Map<String, String> params = record.getRequestQueryParams();
        String body = record.getRequestBody();
        String method = nullToEmpty(record.getMethod()).toUpperCase();

        boolean hasHeaders = headers != null && !headers.isEmpty();
        boolean hasParams = params != null && !params.isEmpty();
        boolean hasBody = body != null && !body.isEmpty();

        if (!hasHeaders && !hasParams && !hasBody) {
            writer.write("-");
            return;
        }

        writer.write("<details><summary>View request</summary>");
        writer.write("<div class=\"section\">");

        if (hasHeaders) {
            writer.write("<div class=\"section-title\">Headers</div>");
            writeKeyValueTable(writer, headers);
        }

        if ("GET".equals(method)) {
            if (hasParams) {
                if (hasHeaders) {
                    writer.write("<div class=\"spacer\"></div>");
                }
                writer.write("<div class=\"section-title\">Query params</div>");
                writeKeyValueTable(writer, params);
            }
        } else {
            if (hasBody) {
                if (hasHeaders) {
                    writer.write("<div class=\"spacer\"></div>");
                }
                writer.write("<div class=\"section-title\">Body</div><pre>");
                writer.write(escapeHtml(body));
                writer.write("</pre>");
            }
        }

        writer.write("</div></details>");
    }

    private static void writeResponseCell(BufferedWriter writer, TestRunRecord record) throws IOException {
        Map<String, String> headers = record.getResponseHeaders();
        String body = record.getResponseBody();

        boolean hasHeaders = headers != null && !headers.isEmpty();
        boolean hasBody = body != null && !body.isEmpty();

        if (!hasHeaders && !hasBody) {
            writer.write("-");
            return;
        }

        writer.write("<details><summary>View response</summary>");
        writer.write("<div class=\"section\">");

        if (hasHeaders) {
            writer.write("<div class=\"section-title\">Headers</div>");
            writeKeyValueTable(writer, headers);
        }

        if (hasBody) {
            if (hasHeaders) {
                writer.write("<div class=\"spacer\"></div>");
            }
            writer.write("<div class=\"section-title\">Body</div><pre>");
            writer.write(escapeHtml(body));
            writer.write("</pre>");
        }

        writer.write("</div></details>");
    }

    private static void writeKeyValueTable(BufferedWriter writer, Map<String, String> map) throws IOException {
        if (map == null || map.isEmpty()) {
            writer.write("-");
            return;
        }
        writer.write("<table class=\"kv\">");
        for (Map.Entry<String, String> entry : map.entrySet()) {
            writer.write("<tr>");
            writer.write("<td class=\"key\">" + escapeHtml(nullToEmpty(entry.getKey())) + "</td>");
            writer.write("<td>" + escapeHtml(nullToEmpty(entry.getValue())) + "</td>");
            writer.write("</tr>");
        }
        writer.write("</table>");
    }

    private static Map<String, String> flattenHeaders(Response response) {
        Map<String, String> out = new LinkedHashMap<>();
        response.getHeaders().asList().forEach(h -> {
            String name = h.getName();
            String value = h.getValue();
            if (name == null || name.isEmpty()) {
                return;
            }
            if (!out.containsKey(name)) {
                out.put(name, value);
            } else {
                // join multiple values for same header
                String existing = out.get(name);
                out.put(name, existing + ", " + value);
            }
        });
        return out;
    }

    private static String truncate(String input, int maxLength) {
        if (input == null) {
            return "";
        }
        if (input.length() <= maxLength) {
            return input;
        }
        return input.substring(0, maxLength) + "\n... (truncated)";
    }

    private static String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private static String nullToEmpty(String input) {
        return input == null ? "" : input;
    }

    private static String prettyIfJson(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (!(trimmed.startsWith("{") || trimmed.startsWith("["))) {
            return value;
        }
        try {
            Object tree = MAPPER.readTree(trimmed);
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(tree);
        } catch (Exception e) {
            return value;
        }
    }
}

