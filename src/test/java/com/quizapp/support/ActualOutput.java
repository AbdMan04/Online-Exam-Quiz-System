package com.quizapp.support;

import com.quizapp.exception.BusinessException;

import java.util.function.Supplier;

/**
 * Runs an action and returns what the user would see: the success text, or the business error message.
 * Any other exception is a defect; it is returned as "Unhandled error: ..." so the row still appears in the
 * report (and fails) instead of disappearing from the tables.
 */
public final class ActualOutput {

    private ActualOutput() {
    }

    public static String of(Supplier<String> action) {
        try {
            return action.get();
        } catch (BusinessException e) {
            return e.getMessage();
        } catch (RuntimeException e) {
            return "Unhandled error: " + e.getClass().getSimpleName();
        }
    }

    /** Formats an input value for the report: {@code null} stays null, text is quoted. */
    public static String quoted(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }
}
