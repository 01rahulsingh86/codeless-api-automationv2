package com.automation.api.core;

import com.automation.api.model.TestCaseData;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CsvTestDataLoader {

    public static List<TestCaseData> load(String classpathLocation) {
        List<TestCaseData> result = new ArrayList<>();
        try (InputStream is = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(classpathLocation)) {
            if (is == null) {
                throw new IllegalStateException("CSV file not found on classpath: " + classpathLocation);
            }
            try (CSVReader reader = new CSVReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                List<String[]> all = reader.readAll();
                if (all.isEmpty()) {
                    return result;
                }
                String[] header = all.get(0);
                for (int i = 1; i < all.size(); i++) {
                    String[] rowValues = all.get(i);
                    Map<String, String> row = new HashMap<>();
                    for (int j = 0; j < header.length; j++) {
                        String key = header[j];
                        String value = j < rowValues.length ? rowValues[j] : "";
                        row.put(key, value);
                    }
                    // Resolve chaining placeholders using DataContext
                    DataContext.resolveRow(row);
                    result.add(new TestCaseData(row));
                }
            }
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to read CSV from: " + classpathLocation, e);
        }
        return result;
    }
}

