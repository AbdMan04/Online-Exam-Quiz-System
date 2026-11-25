package com.quizapp.validation;

import com.quizapp.dto.QuestionInput;
import com.quizapp.dto.QuestionInput.OptionInput;
import com.quizapp.exception.BusinessException;
import com.quizapp.exception.Messages;

import java.util.List;

import static com.quizapp.validation.ValidationRules.inRange;

/**
 * R11: a question needs text of 1-500 characters, 1-10 points, 2-5 options with text of 1-300 characters,
 * and exactly 1 correct option.
 */
public final class QuestionValidator {

    private QuestionValidator() {
    }

    public static void validate(QuestionInput input) {
        if (!isValid(input)) {
            throw new BusinessException(Messages.INVALID_QUESTION);
        }
    }

    public static boolean isValid(QuestionInput input) {
        if (input == null || !hasValidText(input.text(), ValidationRules.QUESTION_TEXT_MAX_LENGTH)) {
            return false;
        }
        if (!inRange(input.points(), ValidationRules.POINTS_MIN, ValidationRules.POINTS_MAX)) {
            return false;
        }
        List<OptionInput> options = input.options();
        if (options == null || !inRange(options.size(), ValidationRules.OPTIONS_MIN, ValidationRules.OPTIONS_MAX)) {
            return false;
        }
        if (options.stream().anyMatch(option -> !hasValidText(option.text(), ValidationRules.OPTION_TEXT_MAX_LENGTH))) {
            return false;
        }
        return countCorrect(options) == ValidationRules.REQUIRED_CORRECT_OPTIONS;
    }

    private static long countCorrect(List<OptionInput> options) {
        return options.stream().filter(OptionInput::correct).count();
    }

    /** Not blank, and at most {@code maxLength} characters after trimming (texts are stored trimmed). */
    private static boolean hasValidText(String text, int maxLength) {
        return text != null && !text.isBlank() && text.trim().length() <= maxLength;
    }
}
