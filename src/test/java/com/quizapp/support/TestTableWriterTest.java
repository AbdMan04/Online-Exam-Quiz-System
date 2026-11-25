package com.quizapp.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the report-table generator itself, so the 80 tables in the PDF can be trusted.
 * JUnit features: {@code @TempDir}, {@code assertLinesMatch} (with regex lines), {@code assertIterableEquals},
 * {@code assertInstanceOf}.
 */
@DisplayName("Report table writer")
class TestTableWriterTest {

    private static TestCaseRow row(String id, String result) {
        return new TestCaseRow(id, "condition " + id, "input", "expected", "actual", result);
    }

    @Test
    @DisplayName("rows are sorted by requirement, then table (EP, BVA, SC, BC), then id")
    void reportOrder() {
        List<TestCaseRow> unsorted = List.of(row("TC-R02-BC-01", "Pass"), row("TC-R01-SC-01", "Pass"),
                row("TC-R01-EP-02", "Pass"), row("TC-R01-BVA-01", "Pass"), row("TC-R01-EP-01", "Pass"));

        List<String> ids = unsorted.stream().sorted(TestCaseRow.REPORT_ORDER).map(TestCaseRow::id).toList();

        assertIterableEquals(List.of("TC-R01-EP-01", "TC-R01-EP-02", "TC-R01-BVA-01", "TC-R01-SC-01", "TC-R02-BC-01"), ids);
    }

    @Test
    @DisplayName("closing the writer creates the Markdown tables and the CSV file")
    void writesFiles(@TempDir Path outputDir) throws IOException {
        TestTableWriter writer = new TestTableWriter(outputDir);
        writer.add(new TestCaseRow("TC-R01-EP-01", "a | b", "x", "ok", "ok", "Pass"));
        writer.add(new TestCaseRow("TC-R01-BVA-01", "quote \"q\"", "y", "ok", "bad", "Fail"));

        writer.close();

        List<String> markdown = Files.readAllLines(outputDir.resolve("test-cases.md"));
        assertLinesMatch(List.of(
                "# Test case tables",
                "",
                "## R01",
                "",
                "### Equivalent partition test cases",
                "",
                "\\| Test Case ID .*",
                "\\|---.*",
                "| TC-R01-EP-01 | a \\| b | x | ok | ok | Pass |",
                "",
                "### 2-BVA test cases",
                ">> 3 >>", // skip exactly 3 lines: blank line, table header, divider
                "| TC-R01-BVA-01 | quote \"q\" | y | bad | ok | Fail |"), markdown,
                () -> "actual file:\n" + String.join("\n", markdown));

        List<String> csv = Files.readAllLines(outputDir.resolve("test-cases.csv"));
        assertAll(
                () -> assertEquals("requirement,table,id,condition,input,actual,expected,result", csv.get(0)),
                () -> assertEquals(3, csv.size()),
                () -> assertTrue(csv.get(2).contains("\"quote \"\"q\"\"\""), "quotes inside a CSV cell are doubled"));
    }

    @Test
    @DisplayName("a test case id that does not follow TC-Rnn-TABLE-nn is rejected")
    void rejectsBadIds() {
        Exception error = assertThrows(Exception.class, () -> row("R1-EP-1", "Pass"));
        assertInstanceOf(IllegalArgumentException.class, error);
    }
}
