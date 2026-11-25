package com.quizapp.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Collects all recorded rows and, when JUnit closes it at the end of the run, writes:
 * <ul>
 *   <li>{@code target/test-tables/test-cases.md}  - the tables grouped per requirement (readable)</li>
 *   <li>{@code target/test-tables/test-cases.csv} - every row (input for the final PDF)</li>
 * </ul>
 */
public class TestTableWriter implements AutoCloseable {

    static final Path DEFAULT_OUTPUT_DIR = Path.of("target", "test-tables");
    private static final String HEADER = "| Test Case ID | Input condition | Input | Actual output | Expected output | Pass/Fail |";
    private static final String DIVIDER = "|---|---|---|---|---|---|";

    private final Path outputDir;
    private final List<TestCaseRow> rows = new ArrayList<>();

    public TestTableWriter() {
        this(DEFAULT_OUTPUT_DIR);
    }

    TestTableWriter(Path outputDir) {
        this.outputDir = outputDir;
    }

    synchronized void add(TestCaseRow row) {
        rows.add(row);
    }

    @Override
    public synchronized void close() {
        List<TestCaseRow> sorted = rows.stream().sorted(TestCaseRow.REPORT_ORDER).toList();
        try {
            Files.createDirectories(outputDir);
            Files.writeString(outputDir.resolve("test-cases.md"), toMarkdown(sorted), StandardCharsets.UTF_8);
            Files.writeString(outputDir.resolve("test-cases.csv"), toCsv(sorted), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write test tables", e);
        }
    }

    static String toMarkdown(List<TestCaseRow> sorted) {
        Map<String, List<TestCaseRow>> byRequirement = sorted.stream()
                .collect(Collectors.groupingBy(TestCaseRow::requirement, TreeMap::new, Collectors.toList()));
        StringBuilder md = new StringBuilder("# Test case tables\n");
        byRequirement.forEach((requirement, requirementRows) -> {
            md.append("\n## ").append(requirement).append('\n');
            for (TestCaseRow.Table table : TestCaseRow.Table.values()) {
                List<TestCaseRow> tableRows = requirementRows.stream()
                        .filter(row -> row.table().equals(table.name())).toList();
                if (!tableRows.isEmpty()) {
                    md.append("\n### ").append(table.title()).append("\n\n").append(HEADER).append('\n').append(DIVIDER).append('\n');
                    tableRows.forEach(row -> md.append(markdownLine(row)).append('\n'));
                }
            }
        });
        return md.toString();
    }

    private static String markdownLine(TestCaseRow row) {
        return Stream.of(row.id(), row.condition(), row.input(), row.actual(), row.expected(), row.result())
                .map(cell -> cell == null ? "" : cell.replace("|", "\\|").replace("\n", " "))
                .collect(Collectors.joining(" | ", "| ", " |"));
    }

    static String toCsv(List<TestCaseRow> sorted) {
        StringBuilder csv = new StringBuilder("requirement,table,id,condition,input,actual,expected,result\n");
        for (TestCaseRow row : sorted) {
            csv.append(Stream.of(row.requirement(), row.table(), row.id(), row.condition(), row.input(),
                            row.actual(), row.expected(), row.result())
                    .map(TestTableWriter::csvCell)
                    .collect(Collectors.joining(","))).append('\n');
        }
        return csv.toString();
    }

    private static String csvCell(String value) {
        String text = value == null ? "" : value;
        return '"' + text.replace("\"", "\"\"") + '"';
    }
}
