package com.quizapp.whitebox;

/**
 * Turns short input specs used in CSV rows into real test inputs, and back into readable report text.
 * {@code LEN:101} means "a text of 101 characters".
 */
final class Specs {

    private static final String LENGTH_PREFIX = "LEN:";
    private static final String EMAIL_DOMAIN = "@uni.edu";

    private Specs() {
    }

    /** "LEN:n" -> n letters; anything else unchanged. */
    static String text(String spec) {
        return isLength(spec) ? "x".repeat(length(spec)) : spec;
    }

    /** "LEN:n" -> a well-formed email of exactly n characters; anything else unchanged. */
    static String email(String spec) {
        return isLength(spec) ? "a".repeat(length(spec) - EMAIL_DOMAIN.length()) + EMAIL_DOMAIN : spec;
    }

    /** Report text for an input: long texts are shown by their length. */
    static String describe(String value) {
        if (value == null) {
            return "null";
        }
        return value.length() > 30 ? value.length() + " characters" : "\"" + value + "\"";
    }

    private static boolean isLength(String spec) {
        return spec != null && spec.startsWith(LENGTH_PREFIX);
    }

    private static int length(String spec) {
        return Integer.parseInt(spec.substring(LENGTH_PREFIX.length()));
    }
}
