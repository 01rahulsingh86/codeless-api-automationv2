package com.automation.api.reporting;

import com.automation.api.core.DataContext;
import com.automation.api.model.TestCaseData;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import org.testng.*;

public class TestNGListener implements ITestListener, IExecutionListener {

    @Override
    public void onExecutionStart() {
        // Initialize report and clear any existing chained data
        ExtentManager.getInstance();
        DataContext.clear();
    }

    @Override
    public void onExecutionFinish() {
        ExtentManager.flush();
        // Write custom summary HTML with one row per test
        SummaryReportManager.writeHtmlSummary("target/api-summary.html");
    }

    @Override
    public void onTestStart(ITestResult result) {
        Object[] params = result.getParameters();
        String testName = result.getMethod().getMethodName();
        String description = "";

        if (params != null && params.length > 0 && params[0] instanceof TestCaseData data) {
            String id = data.getTestCaseId();
            String name = data.getTestCaseName();
            testName = String.format("[%s] %s", id, name);
            description = "HTTP " + data.getMethod() + " for endpoint alias '" + data.getEndpointKey() + "'";
        }

        ExtentTestManager.startTest(testName, description);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        ExtentTest test = ExtentTestManager.getTest();
        if (test != null) {
            test.log(Status.PASS, "Test passed");
        }
        ExtentTestManager.endTest();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        ExtentTest test = ExtentTestManager.getTest();
        if (test != null) {
            test.log(Status.FAIL, result.getThrowable());
        }
        ExtentTestManager.endTest();
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        ExtentTest test = ExtentTestManager.getTest();
        if (test != null) {
            test.log(Status.SKIP, "Test skipped");
        }
        ExtentTestManager.endTest();
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        // no-op
    }

    @Override
    public void onStart(ITestContext context) {
        // no-op
    }

    @Override
    public void onFinish(ITestContext context) {
        // no-op
    }
}

