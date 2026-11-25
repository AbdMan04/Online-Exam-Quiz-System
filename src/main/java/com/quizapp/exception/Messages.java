package com.quizapp.exception;

/**
 * Exact user-facing messages (see docs/requirements.md, section 2).
 * Tests assert on these constants, never on copied strings.
 */
public final class Messages {

    public static final String REGISTER_OK = "Registration successful";
    public static final String NAME_REQUIRED = "Name is required";
    public static final String INVALID_EMAIL = "Invalid email";
    public static final String INVALID_PASSWORD = "Password must be 8-20 characters with a letter and a digit";
    public static final String EMAIL_TAKEN = "Email already registered";
    public static final String BAD_CREDENTIALS = "Invalid email or password";
    public static final String ACCOUNT_LOCKED = "Account locked";

    public static final String INVALID_TITLE = "Invalid title length";
    public static final String INVALID_DURATION = "Duration must be 5-180 minutes";
    public static final String INVALID_ATTEMPTS = "Max attempts must be 1-5";
    public static final String INVALID_PASSING_SCORE = "Passing score must be 0-100";
    public static final String INVALID_QUESTION = "Invalid question configuration";
    public static final String QUIZ_NEEDS_QUESTION = "Quiz needs at least 1 question";
    public static final String QUIZ_LOCKED = "Quiz cannot be modified after attempts exist";
    public static final String QUIZ_ALREADY_PUBLISHED = "Quiz is already published";
    public static final String QUIZ_NOT_FOUND = "Quiz not found";
    public static final String QUESTION_NOT_FOUND = "Question not found";

    public static final String QUIZ_CREATED = "Quiz created";
    public static final String QUESTION_ADDED = "Question added";
    public static final String QUESTION_UPDATED = "Question updated";
    public static final String QUESTION_DELETED = "Question deleted";
    public static final String QUIZ_PUBLISHED = "Quiz published";

    public static final String NO_ATTEMPTS_LEFT = "No attempts remaining";
    public static final String QUIZ_NOT_AVAILABLE = "Quiz not available";
    public static final String TIME_EXPIRED = "Time is up - attempt already submitted";

    private Messages() {
    }
}
