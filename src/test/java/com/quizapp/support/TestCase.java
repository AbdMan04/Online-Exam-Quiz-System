package com.quizapp.support;

/**
 * Handed to a test method by {@link TestCaseRecorder}. The test describes the case it runs and the
 * actual output it observed; the recorder adds Pass/Fail once the test has finished.
 */
public final class TestCase {

    private String id;
    private String condition;
    private String input;
    private String expected;
    private String actual;

    TestCase() {
    }

    /** Records the row; call it before asserting so failing cases are reported too. */
    public void record(String id, String condition, String input, String expected, String actual) {
        this.id = id;
        this.condition = condition;
        this.input = input;
        this.expected = expected;
        this.actual = actual;
    }

    boolean isRecorded() {
        return id != null;
    }

    TestCaseRow toRow(boolean passed) {
        return new TestCaseRow(id, condition, input, expected, actual, passed ? "Pass" : "Fail");
    }
}
