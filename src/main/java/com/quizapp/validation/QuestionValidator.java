package com.quizapp.validation;

import com.quizapp.dto.QuestionInput;
import com.quizapp.dto.QuestionInput.OptionInput;
import com.quizapp.exception.BusinessException;
import com.quizapp.exception.Messages;

import java.util.List;

import static com.quizapp.validation.ValidationRules.inRange;

/**
 * R11: a question needs non-blank text, 1-10 points, 2-5 non-blank options and exactly 1 correct option.
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
        if (input == null || isBlank(input.text())) {
            return false;
        }
        if (!inRange(input.points(), ValidationRules.POINTS_MIN, ValidationRules.POINTS_MAX)) {
            return false;
        }
        List<OptionInput> options = input.options();
        if (options == null || !inRange(options.size(), ValidationRules.OPTIONS_MIN, ValidationRules.OPTIONS_MAX)) {
            return false;
        }
        if (options.stream().anyMatch(option -> isBlank(option.text()))) {
            return false;
        }
        return countCorrect(options) == ValidationRules.REQUIRED_CORRECT_OPTIONS;
    }

    private static long countCorrect(List<OptionInput> options) {
        return options.stream().filter(OptionInput::correct).count();
    }

    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }
}
