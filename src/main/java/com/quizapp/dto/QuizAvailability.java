package com.quizapp.dto;

import com.quizapp.model.Quiz;

/**
 * A published quiz as seen by one student: how many attempts they used and whether one is open.
 */
public record QuizAvailability(Quiz quiz, long attemptsUsed, Long openAttemptId) {

    public long attemptsLeft() {
        return Math.max(0, quiz.getMaxAttempts() - attemptsUsed);
    }

    public boolean canStart() {
        return openAttemptId != null || attemptsLeft() > 0;
    }
}
