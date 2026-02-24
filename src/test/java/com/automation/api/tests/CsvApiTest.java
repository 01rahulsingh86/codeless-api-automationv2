package com.automation.api.tests;

import com.automation.api.core.CsvTestDataLoader;
import com.automation.api.core.RequestBuilder;
import com.automation.api.core.ResponseChainingProcessor;
import com.automation.api.core.ResponseValidator;
import com.automation.api.model.TestCaseData;
import com.automation.api.reporting.ExtentTestManager;
import com.automation.api.reporting.SummaryReportManager;
import com.aventstack.extentreports.ExtentTest;
import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

public class CsvApiTest {

    private static final Logger logger = LogManager.getLogger(CsvApiTest.class);

    private static final String CSV_PATH = "test-data/sample_api_tests.csv";

    @DataProvider(name = "csvData")
    public Object[][] csvDataProvider() {
        List<TestCaseData> list = CsvTestDataLoader.load(CSV_PATH);
        Object[][] data = new Object[list.size()][1];
        for (int i = 0; i < list.size(); i++) {
            data[i][0] = list.get(i);
        }
        return data;
    }

    @Test(dataProvider = "csvData")
    public void runCsvDrivenApi(TestCaseData data) {
        RequestBuilder.BuiltRequest built = RequestBuilder.build(data);

        // Start collecting data for custom HTML summary report
        SummaryReportManager.start(data, built);

        ExtentTest test = ExtentTestManager.getTest();
        if (test != null) {
            test.info("TestCase ID: " + data.getTestCaseId());
            test.info("TestCase Name: " + data.getTestCaseName());
            test.info("HTTP Method: " + built.getMethod());
            test.info("URL: " + built.getUrl());
            test.info("Headers: " + data.getHeaderFields());
            test.info("Request Fields: " + data.getRequestFields());
            if (built.getBody() != null) {
                test.info("Request Body: <pre>" + built.getBody() + "</pre>");
            }
        }

        logger.info("Executing test [{}] {} - {} {}",
                data.getTestCaseId(),
                data.getTestCaseName(),
                built.getMethod(),
                built.getUrl());
        logger.info("Request headers: {}", data.getHeaderFields());
        if (built.getBody() != null) {
            logger.info("Request body: {}", built.getBody());
        }

        Response response = null;
        try {
            response = execute(built);
            SummaryReportManager.attachResponse(response);

            String responseBody = response.getBody().asString();
            logger.info("Response status: {}", response.getStatusCode());
            logger.info("Response headers: {}", response.getHeaders());
            logger.info("Response body: {}", responseBody);

            if (test != null) {
                test.info("Response Status: " + response.getStatusCode());
                test.info("Response Headers: " + response.getHeaders());
                test.info("Response Body: <pre>" + escapeHtml(responseBody) + "</pre>");
            }

            // Chaining and validation
            ResponseChainingProcessor.capture(response, data);
            ResponseValidator.validate(response, data);

            SummaryReportManager.markResult("PASS", null);
        } catch (Throwable e) {
            // Attach any response we may have and record failure before rethrowing
            if (response != null) {
                SummaryReportManager.attachResponse(response);
            }
            SummaryReportManager.markResult("FAIL", e);
            throw e;
        } finally {
            SummaryReportManager.clearCurrent();
        }
    }

    private Response execute(RequestBuilder.BuiltRequest built) {
        String method = built.getMethod();
        switch (method) {
            case "GET":
                return built.getSpec().get(built.getUrl());
            case "POST":
                return built.getSpec().post(built.getUrl());
            case "PUT":
                return built.getSpec().put(built.getUrl());
            case "PATCH":
                return built.getSpec().patch(built.getUrl());
            case "DELETE":
                return built.getSpec().delete(built.getUrl());
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }
    }

    private String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}

