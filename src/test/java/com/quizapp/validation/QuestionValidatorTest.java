package com.quizapp.validation;

import com.quizapp.dto.QuestionInput;
import com.quizapp.dto.QuestionInput.OptionInput;
import com.quizapp.exception.BusinessException;
import com.quizapp.exception.Messages;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("QuestionValidator - R10 and R11")
class QuestionValidatorTest {

    /** {@code count} options; the ones at {@code correctIndexes} are marked correct (none given = no correct option). */
    private static List<OptionInput> options(int count, int... correctIndexes) {
        List<OptionInput> options = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            boolean correct = false;
            for (int index : correctIndexes) {
                correct |= index == i;
            }
            options.add(new OptionInput("Option " + (i + 1), correct));
        }
        return options;
    }

    @Test
    @DisplayName("R10 4 options, 1 correct, 5 points is valid")
    void validQuestion() {
        assertDoesNotThrow(() -> QuestionValidator.validate(new QuestionInput("What is the JVM?", 5, options(4, 0))));
    }

    @Test
    @DisplayName("R11 invalid configurations throw the shared message")
    void invalidThrows() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> QuestionValidator.validate(new QuestionInput("Q?", 5, options(1, 0))));
        assertEquals(Messages.INVALID_QUESTION, ex.getMessage());
    }

    @Test
    @DisplayName("R11 each rule rejects its own bad case")
    void eachRule() {
        assertFalse(QuestionValidator.isValid(new QuestionInput("Q?", 5, options(6, 0))), "6 options");
        assertFalse(QuestionValidator.isValid(new QuestionInput("Q?", 5, options(4))), "no correct option");
        assertFalse(QuestionValidator.isValid(new QuestionInput("Q?", 5, options(4, 0, 1))), "two correct options");
        assertFalse(QuestionValidator.isValid(new QuestionInput("Q?", 0, options(4, 0))), "0 points");
        assertFalse(QuestionValidator.isValid(new QuestionInput("Q?", 11, options(4, 0))), "11 points");
        assertFalse(QuestionValidator.isValid(new QuestionInput(" ", 5, options(4, 0))), "blank text");
        assertFalse(QuestionValidator.isValid(new QuestionInput("Q?", 5,
                List.of(new OptionInput("A", true), new OptionInput(" ", false)))), "blank option");
        assertFalse(QuestionValidator.isValid(null), "null input");
    }
}
