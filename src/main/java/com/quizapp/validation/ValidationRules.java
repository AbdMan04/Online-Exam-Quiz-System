package com.quizapp.validation;

/**
 * All business limits in one place (see docs/requirements.md, section 1).
 * Change a limit here and every validator and test picks it up.
 */
public final class ValidationRules {

    public static final int PASSWORD_MIN_LENGTH = 8;
    public static final int PASSWORD_MAX_LENGTH = 20;

    public static final int TITLE_MIN_LENGTH = 3;
    public static final int TITLE_MAX_LENGTH = 100;

    public static final int DURATION_MIN_MINUTES = 5;
    public static final int DURATION_MAX_MINUTES = 180;

    public static final int PASSING_SCORE_MIN = 0;
    public static final int PASSING_SCORE_MAX = 100;

    public static final int MAX_ATTEMPTS_MIN = 1;
    public static final int MAX_ATTEMPTS_MAX = 5;

    public static final int OPTIONS_MIN = 2;
    public static final int OPTIONS_MAX = 5;
    public static final int REQUIRED_CORRECT_OPTIONS = 1;

    public static final int POINTS_MIN = 1;
    public static final int POINTS_MAX = 10;

    public static final int FAILED_LOGINS_BEFORE_LOCK = 3;

    private ValidationRules() {
    }

    /** True when the value is present and inside the inclusive range [min, max]. */
    public static boolean inRange(Integer value, int min, int max) {
        return value != null && value >= min && value <= max;
    }
}
