package com.quizapp.blackbox;

import com.quizapp.dto.QuizInput;
import com.quizapp.model.Quiz;
import com.quizapp.support.ActualOutput;

/**
 * Shared steps for the "create quiz" form requirements (R6-R9).
 */
abstract class QuizFormSupport extends QuizBlackBoxSupport {

    /** Submits the create-quiz form and returns what the instructor sees. */
    protected String saveQuiz(String title, Integer duration, Integer passingScore, Integer maxAttempts) {
        return ActualOutput.of(() -> {
            Quiz quiz = quizService.createQuiz(instructorId(), new QuizInput(title, duration, passingScore, maxAttempts));
            return "Quiz created with status " + quiz.getStatus();
        });
    }

    /** "LEN:150" in a CSV row stands for a title of 150 characters; anything else is used as written. */
    static String title(String spec) {
        return spec != null && spec.startsWith("LEN:") ? "T".repeat(Integer.parseInt(spec.substring(4))) : spec;
    }

    static String describe(String title, Integer duration, Integer passingScore, Integer maxAttempts) {
        String shownTitle = title == null ? "null" : title.length() > 30 ? title.length() + " characters" : "\"" + title + "\"";
        return "title=" + shownTitle + ", duration=" + duration + ", passing=" + passingScore + ", attempts=" + maxAttempts;
    }
}
