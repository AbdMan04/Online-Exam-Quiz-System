package com.quizapp.blackbox;

import com.quizapp.model.Attempt;
import com.quizapp.support.ActualOutput;

/**
 * Shared steps for starting attempts (R14-R16).
 */
abstract class AttemptSupport extends QuizBlackBoxSupport {

    /**
     * The student presses "Start". Reports whether a new attempt started or the open one was resumed,
     * and how much time the timer shows.
     *
     * @param openAttemptId the student's attempt that is already open, or null
     */
    protected String start(Long quizId, Long studentId, Long openAttemptId) {
        return ActualOutput.of(() -> {
            Attempt attempt = attemptService.start(quizId, studentId);
            String what = attempt.getId().equals(openAttemptId) ? "Same attempt resumed" : "Attempt started";
            return what + ", " + attemptService.remainingSeconds(attempt) + " s left";
        });
    }

    /** The student uses {@code count} attempts (each started and submitted). */
    protected void useAttempts(Long quizId, Long studentId, int count) {
        for (int i = 0; i < count; i++) {
            Attempt attempt = attemptService.start(quizId, studentId);
            attemptService.submit(attempt.getId(), studentId);
        }
    }
}
