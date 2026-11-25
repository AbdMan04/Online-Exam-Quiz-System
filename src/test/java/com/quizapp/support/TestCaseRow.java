package com.quizapp.support;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One row of a report table (EP, BVA, statement or branch coverage).
 * The id follows the pattern {@code TC-R<nn>-<TABLE>-<nn>}, e.g. {@code TC-R02-BVA-03}.
 */
public record TestCaseRow(String id, String condition, String input, String expected, String actual, String result) {

    private static final Pattern ID_PATTERN = Pattern.compile("TC-(R\\d{2})-(EP|BVA|SC|BC)-\\d{2}");

    /** Report order: requirement, then table (EP, BVA, SC, BC), then id. */
    public static final Comparator<TestCaseRow> REPORT_ORDER = Comparator
            .comparing(TestCaseRow::requirement)
            .thenComparing(row -> Table.valueOf(row.table()).ordinal())
            .thenComparing(TestCaseRow::id);

    public TestCaseRow {
        if (id == null || !ID_PATTERN.matcher(id).matches()) {
            throw new IllegalArgumentException("Test case id must look like TC-R02-BVA-03 but was: " + id);
        }
    }

    public String requirement() {
        return idPart(1);
    }

    public String table() {
        return idPart(2);
    }

    private String idPart(int group) {
        Matcher matcher = ID_PATTERN.matcher(id);
        matcher.matches();
        return matcher.group(group);
    }

    /** The four tables of the report, in document order. */
    public enum Table {
        EP("Equivalent partition test cases"),
        BVA("2-BVA test cases"),
        SC("Statement coverage test cases"),
        BC("Branch coverage test cases");

        private final String title;

        Table(String title) {
            this.title = title;
        }

        public String title() {
            return title;
        }
    }
}
