package com.quizapp.validation;

import com.quizapp.dto.QuizInput;
import com.quizapp.exception.BusinessException;
import com.quizapp.exception.Messages;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("QuizValidator - R6 to R9")
class QuizValidatorTest {

    @Test
    @DisplayName("R6 valid settings are accepted")
    void validQuiz() {
        assertDoesNotThrow(() -> QuizValidator.validate(new QuizInput("Java Basics", 30, 60, 2)));
    }

    @ParameterizedTest(name = "[{index}] title=''{0}'' duration={1} passing={2} attempts={3} -> {4}")
    @CsvSource({
            "Ja,          30,  60, 2, " + Messages.INVALID_TITLE,
            "Java Basics, 4,   60, 2, " + Messages.INVALID_DURATION,
            "Java Basics, 181, 60, 2, " + Messages.INVALID_DURATION,
            "Java Basics, 30,  -1, 2, " + Messages.INVALID_PASSING_SCORE,
            "Java Basics, 30, 101, 2, " + Messages.INVALID_PASSING_SCORE,
            "Java Basics, 30,  60, 0, " + Messages.INVALID_ATTEMPTS,
            "Java Basics, 30,  60, 6, " + Messages.INVALID_ATTEMPTS
    })
    @DisplayName("R7-R9 each invalid field gives its own message")
    void invalidFields(String title, int duration, int passing, int attempts, String expected) {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> QuizValidator.validate(new QuizInput(title, duration, passing, attempts)));
        assertEquals(expected, ex.getMessage());
    }

    @Test
    @DisplayName("R7 title longer than 100 characters is rejected")
    void titleTooLong() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> QuizValidator.validate(new QuizInput("x".repeat(101), 30, 60, 2)));
        assertEquals(Messages.INVALID_TITLE, ex.getMessage());
    }

    @Test
    @DisplayName("R8 an empty (null) duration counts as out of range")
    void missingDuration() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> QuizValidator.validate(new QuizInput("Java Basics", null, 60, 2)));
        assertEquals(Messages.INVALID_DURATION, ex.getMessage());
    }
}
