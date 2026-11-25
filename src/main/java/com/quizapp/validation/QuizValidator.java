package com.quizapp.validation;

import com.quizapp.dto.QuizInput;
import com.quizapp.exception.BusinessException;
import com.quizapp.exception.Messages;

import static com.quizapp.validation.ValidationRules.inRange;

/**
 * R7-R9: quiz settings. Checked in order title, duration, passing score, max attempts;
 * the first failure is reported.
 */
public final class QuizValidator {

    private QuizValidator() {
    }

    public static void validate(QuizInput input) {
        if (!isValidTitle(input.title())) {
            throw new BusinessException(Messages.INVALID_TITLE);
        }
        if (!inRange(input.durationMinutes(), ValidationRules.DURATION_MIN_MINUTES, ValidationRules.DURATION_MAX_MINUTES)) {
            throw new BusinessException(Messages.INVALID_DURATION);
        }
        if (!inRange(input.passingScore(), ValidationRules.PASSING_SCORE_MIN, ValidationRules.PASSING_SCORE_MAX)) {
            throw new BusinessException(Messages.INVALID_PASSING_SCORE);
        }
        if (!inRange(input.maxAttempts(), ValidationRules.MAX_ATTEMPTS_MIN, ValidationRules.MAX_ATTEMPTS_MAX)) {
            throw new BusinessException(Messages.INVALID_ATTEMPTS);
        }
    }

    /** R7: the trimmed title must be 3-100 characters. */
    public static boolean isValidTitle(String title) {
        if (title == null) {
            return false;
        }
        return inRange(title.trim().length(), ValidationRules.TITLE_MIN_LENGTH, ValidationRules.TITLE_MAX_LENGTH);
    }
}
