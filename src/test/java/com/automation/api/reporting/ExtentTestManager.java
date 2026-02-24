package com.automation.api.reporting;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;

public class ExtentTestManager {

    private static final ThreadLocal<ExtentTest> TEST = new ThreadLocal<>();

    private ExtentTestManager() {
    }

    public static ExtentTest startTest(String testName, String description) {
        ExtentReports extent = ExtentManager.getInstance();
        ExtentTest extentTest = extent.createTest(testName, description);
        TEST.set(extentTest);
        return extentTest;
    }

    public static ExtentTest getTest() {
        return TEST.get();
    }

    public static void endTest() {
        TEST.remove();
    }
}

